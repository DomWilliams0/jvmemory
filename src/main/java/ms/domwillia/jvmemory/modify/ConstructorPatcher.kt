package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

class ConstructorPatcher(mv: MethodVisitor?, private val className: String) : MethodVisitor(Opcodes.ASM6, mv) {

    private fun visitShouldLogConstructor() {
        val retLabel = Label()
        super.visitVarInsn(Opcodes.ALOAD, 0)
        super.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/Object",
                "getClass",
                "()Ljava/lang/Class;",
                false
                )
        super.visitLdcInsn(Type.getObjectType(className).className)
        super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/Class",
                "forName",
                "(Ljava/lang/String;)Ljava/lang/Class;",
                false
        )

        super.visitJumpInsn(
                Opcodes.IF_ACMPNE,
                retLabel
        )

        super.visitFieldInsn(
                Opcodes.GETSTATIC,
                Monitor.internalName,
                Monitor.instanceName,
                Monitor.descriptor
        )

        super.visitLdcInsn(className)
        super.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                Monitor.internalName,
                "onAlloc",
                "(Ljava/lang/String;)J",
                false
        )
        // TODO use return value
        super.visitInsn(Opcodes.POP2)

        super.visitLabel(retLabel)
    }

    override fun visitInsn(opcode: Int) {

        if (opcode == Opcodes.RETURN) {
            visitShouldLogConstructor()
        }

        super.visitInsn(opcode)
    }
}