package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.Monitor
import ms.domwillia.jvmemory.monitor.definition.MethodDefinition
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.InstructionAdapter
import org.objectweb.asm.commons.LocalVariablesSorter

// TODO helper data class for class+method
class MethodPatcher(
        delegate: MethodVisitor?,
        private val methodName: String,
        private val definition: MethodDefinition
) : InstructionAdapter(Opcodes.ASM6, delegate) {

    lateinit var localVarSorter: LocalVariablesSorter

    override fun store(index: Int, type: Type) {

        // object:
        // Monitor.onStoreLocalVar(Monitor.getTag(value), index)
        // others:
        // Monitor.onStoreLocalVar(0, index)

        // stack: value

        if (type.sort == Type.OBJECT) {
            // dup value
            super.dup()

            // stack: value value

            // get tag
            super.invokestatic(
                    Monitor.internalName,
                    "getTag",
                    "(Ljava/lang/Object;)J",
                    false
            )

            // stack: value tag_long
        } else {
            super.load(0, Type.LONG_TYPE)
            // stack: value tag_long
        }

        // stack: value tag_long

        // push index
        super.iconst(index)

        // stack: value tag_long index

        // log
        super.invokestatic(
                Monitor.internalName,
                "onStoreLocalVar",
                "(JI)V",
                false
        )

        // stack: value
        super.store(index, type)
    }
    override fun load(index: Int, type: Type) {
        // Monitor.onLoadLocalVar(index)

        super.iconst(index)
        super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                Monitor.internalName,
                "onLoadLocalVar",
                "(I)V",
                false
        )

        super.load(index, type)
    }

    /*
    override fun getfield(owner: String, name: String, desc: String) {
        // TODO uninitialisedThis causes problems again
        super.dup()

        super.getstatic(
                Monitor.internalName,
                Monitor.instanceName,
                Monitor.descriptor
        )

        // swap monitor and object
        super.swap()

        // get id
        super.invokestatic(Monitor.internalName, "getTag", "(Ljava/lang/Object;)J", false)

        // add other args
        super.visitLdcInsn(owner)
        super.visitLdcInsn(name)
        super.visitLdcInsn(desc)

        super.invokevirtual(
                Monitor.internalName,
                "onGetField",
                "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                false
        )

        super.getfield(owner, name, desc)
    }

    override fun putfield(owner: String, name: String, desc: String) {
        if (!(methodName == "<init>" && name == "this$0")) {
            val type = Type.getType(desc)
            getHandler(type, TypeSpecificOperation.PUTFIELD)?.let { handler ->

                // stack: obj value
                // store value in tmp var
                val tmp = localVarSorter.newLocal(type)
                super.store(tmp, type)

                // stack: obj
                super.dup()

                // stack: obj obj
                super.invokestatic(Monitor.internalName, "getTag", "(Ljava/lang/Object;)J", false)

                // stack: obj id1 id2
                // get monitor
                super.getstatic(
                        Monitor.internalName,
                        Monitor.instanceName,
                        Monitor.descriptor
                )

                // stack: obj id1 id2 monitor
                super.dupX2()
                super.pop()

                // stack: obj monitor id1 id2
                // push other args
                super.visitLdcInsn(owner)
                super.visitLdcInsn(name)
                super.visitLdcInsn(desc)

                // stack: obj monitor id1 id2 owner name desc
                // pop value
                super.load(tmp, type)

                // stack: obj monitor id1 id2 owner name desc value
                val typeDescriptor = if (type.sort == Type.OBJECT) "Ljava/lang/Object;" else type.descriptor
                super.invokevirtual(
                        Monitor.internalName,
                        handler,
                        "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;$typeDescriptor)V",
                        false
                )

                // pop value again for original call
                super.load(tmp, type)
            }
        }
        super.putfield(owner, name, desc)
    }

*/
    override fun visitLocalVariable(name: String, desc: String, signature: String?, start: Label?, end: Label?, index: Int) {
        // TODO what if no debugging symbols? is desc null
        definition.registerLocalVariable(name, desc, index)
        super.visitLocalVariable(name, desc, signature, start, end, index)
    }

    // does this depend on architecture/implementation?
    private fun dupTypeSpecific(type: Type) {
        when (type.sort) {
            Type.LONG, Type.DOUBLE -> super.dup2()
            else -> super.dup()
        }
    }
}