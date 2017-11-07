package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

class ConstructorPatcher(mv: MethodVisitor?, private val className: String) : MethodVisitor(Opcodes.ASM6, mv) {

    private fun visitShouldLogConstructor() {
        val retLabel = Label()

        // stack: this
        super.visitVarInsn(Opcodes.ALOAD, 0)
        super.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/Object",
                "getClass",
                "()Ljava/lang/Class;",
                false
        )

        // stack: clazz
        super.visitLdcInsn(Type.getObjectType(className).className)
        super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/Class",
                "forName",
                "(Ljava/lang/String;)Ljava/lang/Class;",
                false
        )

        // stack: clazz clazz
        super.visitJumpInsn(
                Opcodes.IF_ACMPNE,
                retLabel
        )

        // stack:
        super.visitVarInsn(Opcodes.ALOAD, 0)

        // stack: this
        super.visitFieldInsn(
                Opcodes.GETSTATIC,
                Monitor.internalName,
                Monitor.instanceName,
                Monitor.descriptor
        )

        // stack: this monitor
        super.visitLdcInsn(className)
        super.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                Monitor.internalName,
                "onAlloc",
                "(Ljava/lang/String;)J",
                false
        )

        // stack: this id
        super.visitFieldInsn(
                Opcodes.PUTFIELD,
                className,
                Monitor.instanceIdFieldName,
                "J"
        )

        super.visitLabel(retLabel)
    }

    override fun visitInsn(opcode: Int) {

        if (opcode == Opcodes.RETURN) {
            visitShouldLogConstructor()
        }

        super.visitInsn(opcode)
    }
}