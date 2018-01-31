package ms.domwillia.jvmemory.modify.patcher

import ms.domwillia.jvmemory.monitor.Monitor
import ms.domwillia.jvmemory.monitor.definition.MethodDefinition
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.InstructionAdapter

class MethodPatcher(
        delegate: MethodVisitor?,
        private val definition: MethodDefinition
) : InstructionAdapter(Opcodes.ASM6, delegate) {

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
            functionDesc = "(JI)V"
        } else {
            // stack: value
            functionName = "onStoreLocalVarPrimitive"
            functionDesc = "(I)V"
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
        val size = type.size

        // avoid uninitialisedThis
        if (name != "this$0") {

            // object:
            // Monitor.onPutFieldObject(Monitor.getTag(obj), field, Monitor.getTag(value))
            // Monitor.onPutFieldObject(obj, value, field)
            // others:
            // Monitor.onPutFieldPrimitive(Monitor.getTag(obj), field)
            // Monitor.onPutFieldPrimitive(obj, field)
            // TODO
            var functionName = "onPutFieldPrimitive"
            var functionDesc = "(Ljava/lang/Object;Ljava/lang/String;)V"

            // obj: dup2
            // size1: dup top to 3
            // size2:

            if (size == 1) {
                // stack: obj value

                super.dup2()

                // stack: obj value obj value

                if (type.sort != Type.OBJECT) {
                    super.pop()

                    // stack: obj value obj <no value>

                } else {
                    functionName = "onPutFieldObject"
                    functionDesc = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)V"
                }

                super.visitLdcInsn(name)

                // stack: obj value obj <value> field

            } else {
                // stack: obj value
                super.dup2X1()

                // stack: value obj value
                super.pop2()

                // stack: value obj
                super.dupX2()

                // stack: obj value obj
                super.visitLdcInsn(name)

                // stack: obj value obj name
            }

            // log
            super.invokestatic(
                    Monitor.internalName,
                    functionName,
                    functionDesc,
                    false
            )
        }

        super.putfield(owner, name, desc)
    }

    override fun newarray(type: Type) {
        // stack: size
        super.dup()

        // stack: size size
        super.newarray(type)

        // stack: size array
        super.dupX1()

        // stack: array size array
        super.visitLdcInsn(type.className)

        // stack: array size array clazz
        super.invokestatic(
                Monitor.internalName,
                "allocateTagForArray",
                "(ILjava/lang/Object;Ljava/lang/String;)V",
                false
        )

        // stack: array
    }

    // TODO deal with sizes
    override fun multianewarray(desc: String, dims: Int) {
        super.multianewarray(desc, dims)

        // stack: array
        super.dup()

        // stack: array array
        super.iconst(dims)

        // stack: array array dims
        super.visitLdcInsn(Type.getType(desc).className)

        // stack: array array dims clazz
        super.invokestatic(
                Monitor.internalName,
                "allocateTagForMultiDimArray",
                "(Ljava/lang/Object;ILjava/lang/String;)V",
                false
        )

        // stack: array
    }

    override fun astore(type: Type) {
        // stack: array index value

        when {
            type.size == 2 -> {
                super.dup2X2()

                // stack: value array index value
                super.pop2()

                // stack: value array index
                super.dup2X2()

                // stack: array index value array index
                super.invokestatic(
                        Monitor.internalName,
                        "onStorePrimitiveInArray",
                        "(Ljava/lang/Object;I)V",
                        false
                )

                // stack: array index value

            }
            type.sort != Type.OBJECT -> {
                super.dupX2()

                // stack: value array index value
                super.pop()

                // stack: value array index
                super.dup2X1()

                // stack: array index value array index
                super.invokestatic(
                        Monitor.internalName,
                        "onStorePrimitiveInArray",
                        "(Ljava/lang/Object;I)V",
                        false
                )

                // stack: array index value
            }
            else -> {
                super.dup()

                // stack: array index value value
                super.dup2X2()

                // stack: value value array index value value
                super.pop2()

                // stack: value value array index
                super.dup2X2()

                // stack: array index value value array index
                super.invokestatic(
                        Monitor.internalName,
                        "onStoreObjectInArray",
                        "(Ljava/lang/Object;Ljava/lang/Object;I)V",
                        false
                )

                // stack: array index value
            }
        }

        super.astore(type)
    }

    override fun aload(type: Type) {
        // stack: array index
        super.dup2()

        // stack: array index array index
        super.invokestatic(
                Monitor.internalName,
                "onLoadFromArray",
                "(Ljava/lang/Object;I)V",
                false
        )

        // stack: array index

        super.aload(type)
    }

    override fun arraylength() {
        // stack: array
        super.dup()

        // stack: array array
        super.iconst(-1)

        // stack: array array dummy_index
        super.invokestatic(
                Monitor.internalName,
                "onLoadFromArray",
                "(Ljava/lang/Object;I)V",
                false
        )

        // stack: array

        super.arraylength()
    }

    override fun visitLocalVariable(name: String, desc: String, signature: String?, start: Label?, end: Label?, index: Int) {
        // TODO what if no debugging symbols? is desc null
        definition.registerLocalVariable(name, desc, index)
        super.visitLocalVariable(name, desc, signature, start, end, index)
    }
}