package ms.domwillia.jvmemory.modify.patcher

import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class CallTracer(
        api: Int,
        private val className: String,
        mv: MethodVisitor?,
        access: Int,
        private val methodName: String,
        desc: String?
) : AdviceAdapter(
        api,
        mv,
        access,
        methodName,
        desc
) {

    override fun onMethodEnter() {
        super.visitLdcInsn(className)
        super.visitLdcInsn(methodName)
        callMonitor(Monitor::enterMethod)

        super.onMethodEnter()
    }

    override fun onMethodExit(opcode: Int) {
        callMonitor(Monitor::exitMethod)

        super.onMethodExit(opcode)
    }
}