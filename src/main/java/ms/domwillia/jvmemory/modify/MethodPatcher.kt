package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.InjectedMonitor
import ms.domwillia.jvmemory.monitor.LocalVarTracker
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.InstructionAdapter
import org.objectweb.asm.commons.LocalVariablesSorter

// TODO helper data class for class+method
class MethodPatcher(
        delegate: MethodVisitor,
        private val methodName: String,
        private val localVarTracker: LocalVarTracker
) : InstructionAdapter(Opcodes.ASM6, delegate) {

    lateinit var localVarSorter: LocalVariablesSorter

    override fun store(index: Int, type: Type) {
        InjectedMonitor.getHandler(type, InjectedMonitor.TypeSpecificOperation.STORE)?.let { handler ->
            // dup value and store in a tmp var
            // TODO we can definitely reuse this tmpvar, should not make a new one for every single store!!
            // TODO caching localvarsorter
            dupTypeSpecific(type)
            val tmpVar = localVarSorter.newLocal(type)
            super.store(tmpVar, type)

            super.visitFieldInsn(
                    Opcodes.GETSTATIC,
                    InjectedMonitor.internalName,
                    InjectedMonitor.instanceName,
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

    override fun load(index: Int, type: Type) {
        super.visitFieldInsn(
                Opcodes.GETSTATIC,
                InjectedMonitor.internalName,
                InjectedMonitor.instanceName,
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

        super.load(index, type)
    }

    override fun getfield(owner: String, name: String, desc: String) {
        // TODO uninitialisedThis causes problems again
        super.dup()

        super.getstatic(
                InjectedMonitor.internalName,
                InjectedMonitor.instanceName,
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

        super.getfield(owner, name, desc)
    }

    override fun putfield(owner: String, name: String, desc: String) {
        if (!(methodName == "<init>" && name == "this$0")) {
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
                // get monitor
                super.getstatic(
                        InjectedMonitor.internalName,
                        InjectedMonitor.instanceName,
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

    override fun visitLocalVariable(name: String, desc: String, signature: String?, start: Label?, end: Label?, index: Int) {
        // TODO what if no debugging symbols? is desc null
        localVarTracker.registerLocalVar(methodName, name, desc, index)
        super.visitLocalVariable(name, desc, signature, start, end, index)
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
}