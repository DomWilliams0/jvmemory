package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

/**
 * All system classes except Object
 */
class SystemConstructorTracer(
        private val className: String,
        mv: MethodVisitor?,
        access: Int,
        desc: String?
) : AdviceAdapter(
        Opcodes.ASM6,
        mv,
        access,
        "<init>",
        desc
) {

    override fun onMethodEnter() {
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
                "enterConstructor",
                "(Ljava/lang/String;)V",
                false
        )

        super.onMethodEnter()
    }

    override fun onMethodExit(opcode: Int) {
        super.visitFieldInsn(
                Opcodes.GETSTATIC,
                Monitor.internalName,
                Monitor.instanceName,
                Monitor.descriptor
        )

        super.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                Monitor.internalName,
                "exitMethod",
                "()V",
                false
        )

        super.onMethodExit(opcode)
    }
}
