package ms.domwillia.jvmemory.modify.patcher

import ms.domwillia.jvmemory.monitor.Monitor
import ms.domwillia.jvmemory.monitor.definition.MethodDefinition
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.InstructionAdapter
import org.objectweb.asm.commons.LocalVariablesSorter

class MethodPatcher(
        delegate: MethodVisitor?,
        private val definition: MethodDefinition
) : InstructionAdapter(Opcodes.ASM6, delegate) {

    lateinit var localVarSorter: LocalVariablesSorter

    override fun store(index: Int, type: Type) {

        // object:
        // Monitor.onStoreLocalVarObject(Monitor.getTag(value), index)
        // others:
        // Monitor.onStoreLocalVarPrimitive(index)
        val functionName: String
        val functionDesc: String

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
            functionName = "onStoreLocalVarObject"
            functionDesc ="(JI)V"
        } else {
            // stack: value
            functionName = "onStoreLocalVarPrimitive"
            functionDesc ="(I)V"
        }

        // stack: value (tag_long)

        // push index
        super.iconst(index)

        // stack: value (tag_long) index

        // log
        super.invokestatic(
                Monitor.internalName,
                functionName,
                functionDesc,
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

    override fun getfield(owner: String, name: String, desc: String) {

        // stack: object

        // TODO uninitialisedThis causes problems again
        // dup object
        super.dup()

        // stack: object object

        // get tag
        super.invokestatic(
                Monitor.internalName,
                "getTag",
                "(Ljava/lang/Object;)J",
                false
        )

        // stack: object tag

        // push field
        super.visitLdcInsn(name)

        // stack: object tag fieldName

        // log
        super.invokestatic(
                Monitor.internalName,
                "onGetField",
                "(JLjava/lang/String;)V",
                false
        )

        // stack: object
        super.getfield(owner, name, desc)
    }

    override fun putfield(owner: String, name: String, desc: String) {
        // TODO there are extra onLoadLocalVars before every putfield - remove these!
        val type = Type.getType(desc)

        // avoid uninitialisedThis
        if (name != "this$0") {

            // object:
            // Monitor.onPutFieldObject(Monitor.getTag(obj), field, Monitor.getTag(value))
            // others:
            // Monitor.onPutFieldPrimitive(Monitor.getTag(obj), field)
            val functionName: String
            val functionDesc: String

            // stack: obj value

            // store value in tmp
            val tmp = localVarSorter.newLocal(type)
            super.store(tmp, type)

            // stack: obj

            // dup
            super.dup()

            // stack: obj obj

            // get tag
            super.invokestatic(
                    Monitor.internalName,
                    "getTag",
                    "(Ljava/lang/Object;)J",
                    false
            )

            // stack: obj obj_id

            // push field
            super.visitLdcInsn(name)

            // stack: obj obj_id field

            if (type.sort == Type.OBJECT) {
                // pop value
                super.load(tmp, type)

                // stack: obj obj_id field value

                // get tag
                super.invokestatic(
                        Monitor.internalName,
                        "getTag",
                        "(Ljava/lang/Object;)J",
                        false
                )

                // stack: obj obj_id field value_id
                functionName = "onPutFieldObject"
                functionDesc = "(JLjava/lang/String;J)V"
            } else {
                // stack: obj obj_id field
                functionName = "onPutFieldPrimitive"
                functionDesc = "(JLjava/lang/String;)V"
            }

            // log
            super.invokestatic(
                    Monitor.internalName,
                    functionName,
                    functionDesc,
                    false
            )
            // stack: obj

            // pop value
            super.load(tmp, type)

            // stack: obj value
        }

        super.putfield(owner, name, desc)
    }

    override fun visitLocalVariable(name: String, desc: String, signature: String?, start: Label?, end: Label?, index: Int) {
        // TODO what if no debugging symbols? is desc null
        definition.registerLocalVariable(name, desc, index)
        super.visitLocalVariable(name, desc, signature, start, end, index)
    }
}