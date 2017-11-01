package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.InjectedMonitor
import ms.domwillia.jvmemory.monitor.LocalVarTracker
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.InstructionAdapter
import org.objectweb.asm.commons.LocalVariablesSorter

// TODO helper data class for class+method
class MethodPatcher(
        delegate: MethodVisitor,
        private val className: String,
        private val methodName: String,
        private val localVarTracker: LocalVarTracker
) : InstructionAdapter(Opcodes.ASM6, delegate) {

    lateinit var localVarSorter: LocalVariablesSorter

    val isConstructor
        get() = this.methodName == "<init>"

    override fun store(index: Int, type: Type) {
        InjectedMonitor.getTypeSpecificLocalVarFuncName(true, type)?.let { handler ->
            // dup value and store in a tmp var
            // TODO we can definitely reuse this tmpvar, should not make a new one for every single store!!
            dupTypeSpecific(type)
            val tmpVar = localVarSorter.newLocal(type)
            super.store(tmpVar, type)

            super.visitVarInsn(Opcodes.ALOAD, 0)
            super.visitFieldInsn(
                    Opcodes.GETFIELD,
                    className,
                    InjectedMonitor.fieldName,
                    InjectedMonitor.descriptor
            )

            super.load(tmpVar, type)
            super.iconst(index)
            super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    InjectedMonitor.internalName,
                    handler,
                    "(${type.descriptor}I)V",
                    false
            )
        }

        super.store(index, type)
    }

    // TODO when injecting monitored instructions (eg. aload_0 and getfield), set a flag
    // TODO to ignore it so it isnt logged! this.__injectedMonitor__ is being logged here!

    override fun load(index: Int, type: Type) {
        // TODO doesnt seem possible to inject more loads when `this` isnt initialised
        // but store manages it?!
        if (!isConstructor) {
            InjectedMonitor.getTypeSpecificLocalVarFuncName(false, type)?.let { handler ->
                super.load(0, OBJECT_TYPE)
                super.visitFieldInsn(
                        Opcodes.GETFIELD,
                        className,
                        InjectedMonitor.fieldName,
                        InjectedMonitor.descriptor
                )

                super.iconst(index)
                super.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        InjectedMonitor.internalName,
                        handler,
                        "(I)V",
                        false
                )
            }
        }

        super.load(index, type)
    }

    override fun getfield(owner: String, name: String, desc: String) {
        // TODO uninitialisedThis causes problems again
        // TODO use flag wrapper instead of this check
        if (!isConstructor && desc != InjectedMonitor.descriptor) {
            super.dup()

            // get injected monitor
            super.load(0, OBJECT_TYPE)
            super.getfield(
                    className,
                    InjectedMonitor.fieldName,
                    InjectedMonitor.descriptor
            )

            // swap monitor and object
            super.swap()

            // calculate hashcode
            super.invokevirtual(
                    "Ljava/lang/Object;",
                    "hashCode",
                    "()I",
                    false
            )
            // add other args
            super.visitLdcInsn(owner)
            super.visitLdcInsn(name)
            super.visitLdcInsn(desc)

            super.invokevirtual(
                    InjectedMonitor.internalName,
                    "onGetField",
                    "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                    false
            )
        }

        super.getfield(owner, name, desc)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        super.visitMaxs(maxStack + 8, maxLocals)
    }

    // does this depend on architecture/implementation?
    private fun dupTypeSpecific(type: Type) {
        when (type.sort) {
            Type.LONG, Type.DOUBLE -> super.dup2()
            else -> super.dup()
        }
    }

    private fun getPrinterClass(what: String): String {
        return "ms/domwillia/jvmemory/monitor/printer/${what.capitalize()}Printer"
    }

}