package ms.domwillia.jvmemory.modify.tracer

import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Object only
 */
class ObjectConstructorTracer(mv: MethodVisitor?) : MethodVisitor(Opcodes.ASM6, mv) {

    override fun visitInsn(opcode: Int) {
        if (opcode != Opcodes.RETURN) {
            super.visitInsn(opcode)
            return
        }

        super.visitVarInsn(Opcodes.ALOAD, 0)
        super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                Monitor.internalName,
                "allocateTag",
                "(Ljava/lang/Object;)V",
                false
        )

        super.visitInsn(opcode)
    }
}
