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

        // stack: value
        val func = if (type.sort == Type.OBJECT) {
            // dup value
            super.dup()

            // stack: value value

            // get tag
            callMonitor(Monitor::getTag)

            // stack: value tag_long
            Monitor::onStoreLocalVarObject
        } else {
            // stack: value
            Monitor::onStoreLocalVarPrimitive
        }

        // stack: value (tag_long)

        // push index
        super.iconst(index)

        // stack: value (tag_long) index

        // log
        callMonitor(func)

        // stack: value
        super.store(index, type)
    }

    override fun load(index: Int, type: Type) {
        // Monitor.onLoadLocalVar(index)

        super.iconst(index)
        callMonitor(Monitor::onLoadLocalVar)

        super.load(index, type)
    }

    override fun getfield(owner: String, name: String, desc: String) {

        // stack: object

        // TODO uninitialisedThis causes problems again
        // dup object
        super.dup()

        // stack: object object

        // get tag
        callMonitor(Monitor::getTag)

        // stack: object tag

        // push field
        super.visitLdcInsn(name)

        // stack: object tag fieldName

        // log
        callMonitor(Monitor::onGetField)

        // stack: object
        super.getfield(owner, name, desc)
    }

    override fun putfield(owner: String, name: String, desc: String) {
        // TODO there are extra onLoadLocalVars before every putfield - remove these!
        val type = Type.getType(desc)
        val size = type.size

        // avoid uninitialisedThis
        if (name != "this$0") {
            var func: MonitorMethod = Monitor::onPutFieldPrimitive

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
                    func = Monitor::onPutFieldObject
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
            callMonitor(func)
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
        callMonitor(Monitor::allocateTagForArray)

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
        callMonitor(Monitor::allocateTagForMultiDimArray)

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
                callMonitor(Monitor::onStorePrimitiveInArray)

                // stack: array index value

            }
            type.sort != Type.OBJECT -> {
                super.dupX2()

                // stack: value array index value
                super.pop()

                // stack: value array index
                super.dup2X1()

                // stack: array index value array index
                callMonitor(Monitor::onStorePrimitiveInArray)

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
                callMonitor(Monitor::onStoreObjectInArray)

                // stack: array index value
            }
        }

        super.astore(type)
    }

    override fun aload(type: Type) {
        // stack: array index
        super.dup2()

        // stack: array index array index
        callMonitor(Monitor::onLoadFromArray)

        // stack: array index

        super.aload(type)
    }

    override fun arraylength() {
        // stack: array
        super.dup()

        // stack: array array
        super.iconst(-1)

        // stack: array array dummy_index
        callMonitor(Monitor::onLoadFromArray)

        // stack: array

        super.arraylength()
    }

    override fun visitLdcInsn(cst: Any) {
        super.visitLdcInsn(cst)

        // stack: value
        if (cst is String || (cst is Type && cst.sort == Type.OBJECT)) {
            super.dup()

            // stack: value value
            super.visitLdcInsn(cst::class.java.typeName)

            // stack: value value name
            callMonitor(Monitor::allocateTagForConstant)

            // stack: value
        }
    }

    override fun visitLocalVariable(name: String, desc: String, signature: String?, start: Label?, end: Label?, index: Int) {
        // TODO what if no debugging symbols? is desc null
        definition.registerLocalVariable(name, desc, index)
        super.visitLocalVariable(name, desc, signature, start, end, index)
    }
}