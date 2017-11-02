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
        InjectedMonitor.getHandler(type, InjectedMonitor.TypeSpecificOperation.STORE)?.let { handler ->
            // dup value and store in a tmp var
            // TODO we can definitely reuse this tmpvar, should not make a new one for every single store!!
            dupTypeSpecific(type)
            val tmpVar = localVarSorter.newLocal(type)
            super.store(tmpVar, type)

            super.load(0, OBJECT_TYPE)
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

    private var skipNextLoad = false
    fun skipNextLoad() {
        this.skipNextLoad = true
    }

    override fun load(index: Int, type: Type) {
        // TODO doesnt seem possible to inject more loads when `this` isnt initialised
        // but store manages it?!
        if (!isConstructor) {

            // skip loading `this` for call tracing
            if (skipNextLoad) {
                println("skipping load")
                skipNextLoad = false
            } else {
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
                        "onLoadLocalVar",
                        "(I)V",
                        false
                )
            }
        }

        super.load(index, type)
    }

    override fun getfield(owner: String, name: String, desc: String) {
        // TODO uninitialisedThis causes problems again
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

    override fun putfield(owner: String, name: String, desc: String) {
        if (!isConstructor) {
            val type = Type.getType(desc)
            InjectedMonitor.getHandler(type, InjectedMonitor.TypeSpecificOperation.PUTFIELD)?.let { handler ->

                // store value in tmp var
                val tmp = localVarSorter.newLocal(type)
                super.store(tmp, type)

                // dup obj
                super.dup()

                // calculate hashcode
                super.invokevirtual(
                        "Ljava/lang/Object;",
                        "hashCode",
                        "()I",
                        false
                )
                // get injected monitor
                super.load(0, OBJECT_TYPE)
                super.getfield(
                        className,
                        InjectedMonitor.fieldName,
                        InjectedMonitor.descriptor
                )

                // swap monitor and hashcode
                super.swap()

                // push others
                super.visitLdcInsn(owner)
                super.visitLdcInsn(name)
                super.visitLdcInsn(desc)

                // pop value
                super.load(tmp, type)

                val typeDescriptor = if (type.sort == Type.OBJECT) "Ljava/lang/Object;" else type.descriptor
                super.invokevirtual(
                        InjectedMonitor.internalName,
                        handler,
                        "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;$typeDescriptor)V",
                        false
                )

                // pop value again for original call
                super.load(tmp, type)
            }
        }
        super.putfield(owner, name, desc)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        // TODO this is arbitrary. look at yourself, what a mess!
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