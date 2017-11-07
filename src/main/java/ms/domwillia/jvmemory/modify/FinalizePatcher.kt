package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.InstructionAdapter

class FinalizePatcher(mv: MethodVisitor?, private val className: String) : InstructionAdapter(Opcodes.ASM6, mv) {
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


    override fun areturn(t: Type?) {
        generate()
        super.areturn(t)
    }

}