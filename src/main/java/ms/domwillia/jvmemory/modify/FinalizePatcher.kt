package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class FinalizePatcher(mv: MethodVisitor?, private val className: String) : MethodVisitor(Opcodes.ASM6, mv) {
    fun generate() {
        visitFieldInsn(
                Opcodes.GETSTATIC,
                Monitor.internalName,
                Monitor.instanceName,
                Monitor.descriptor
        )

        visitVarInsn(Opcodes.ALOAD, 0)
        visitFieldInsn(
                Opcodes.GETFIELD,
                className,
                Monitor.instanceIdFieldName,
                "J"
        )

        visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                Monitor.internalName,
                "onDealloc",
                "(J)V",
                false
        )
    }


    override fun visitInsn(opcode: Int) {
        if (opcode == Opcodes.RETURN) generate()

        super.visitInsn(opcode)
    }

}