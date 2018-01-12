package ms.domwillia.jvmemory.modify.tracer

import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class CallTracer(
        private val className: String,
        mv: MethodVisitor?,
        access: Int,
        private val methodName: String,
        desc: String?
) : AdviceAdapter(
        Opcodes.ASM6,
        mv,
        access,
        methodName,
        desc
) {

    override fun onMethodEnter() {
        super.visitLdcInsn(className)
        super.visitLdcInsn(methodName)
        super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                Monitor.internalName,
                "enterMethod",
                "(Ljava/lang/String;Ljava/lang/String;)V",
                false
        )

        super.onMethodEnter()
    }

    override fun onMethodExit(opcode: Int) {
        super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                Monitor.internalName,
                "exitMethod",
                "()V",
                false
        )

        super.onMethodExit(opcode)
    }
}