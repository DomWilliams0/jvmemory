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

        super.visitFieldInsn(
                Opcodes.GETSTATIC,
                Monitor.internalName,
                Monitor.instanceName,
                Monitor.descriptor
        )

        super.visitLdcInsn("java/lang/Object")
        super.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                Monitor.internalName,
                "enterConstructor",
                "(Ljava/lang/String;)V",
                false
        )
        // TODO should call allocateTag instead

        super.visitInsn(opcode)
    }
}
