package ms.domwillia.jvmemory.modify.patcher

import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

class CollectionsPatcher(
        api: Int,
        mv: MethodVisitor?,
        access: Int,
        name: String,
        desc: String?
) : AdviceAdapter(
        api,
        mv,
        access,
        name,
        desc
) {

    override fun onMethodExit(opcode: Int) {
        if (methodAccess.and(Opcodes.ACC_STATIC) == 0) {
            super.visitVarInsn(Opcodes.ALOAD, 0)
            callMonitor(Monitor::exitSystemMethod)
        }
    }

    override fun visitMultiANewArrayInsn(desc: String, dims: Int) {
        super.visitMultiANewArrayInsn(desc, dims)

        // stack: array
        super.dup()

        // stack: array array
        super.push(dims)
        super.push(desc)

        // stack: array array dims clazz
        callMonitor(Monitor::allocateTagForMultiDimArray)

        // stack: array
    }

    private fun primitiveToType(operand: Int) = when (operand) {
        Opcodes.T_BOOLEAN -> "boolean[]"
        Opcodes.T_CHAR -> "char[]"
        Opcodes.T_FLOAT -> "float[]"
        Opcodes.T_DOUBLE -> "double[]"
        Opcodes.T_BYTE -> "byte[]"
        Opcodes.T_SHORT -> "short[]"
        Opcodes.T_INT -> "int[]"
        Opcodes.T_LONG -> "long[]"
        else -> throw IllegalArgumentException("bad primitive operand $operand")
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        if (opcode != Opcodes.NEWARRAY) {
            super.visitIntInsn(opcode, operand)
            return
        }

        // stack: size
        super.dup()

        // stack: size size
        super.visitIntInsn(opcode, operand)

        // stack: size array
        super.dupX1()

        // stack: array size array
        super.visitLdcInsn(primitiveToType(operand))

        // stack: array size array type
        callMonitor(Monitor::allocateTagForArray)

        // stack: array
    }

    override fun visitTypeInsn(opcode: Int, type: String) =
            when (opcode) {
                Opcodes.ANEWARRAY -> {
                    // stack: size
                    super.dup()

                    // stack: size size
                    super.visitTypeInsn(opcode, type)

                    // stack: size array
                    super.dupX1()

                    // stack: array size array
                    super.visitLdcInsn(Type.getObjectType(type).className + "[]")

                    // stack: array size array type
                    callMonitor(Monitor::allocateTagForArray)
                }
                else -> super.visitTypeInsn(opcode, type)
            }
}