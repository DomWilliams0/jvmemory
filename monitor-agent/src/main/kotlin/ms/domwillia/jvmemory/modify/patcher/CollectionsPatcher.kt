package ms.domwillia.jvmemory.modify.patcher

import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.AdviceAdapter

class CollectionsPatcher(
        api: Int,
        mv: MethodVisitor?,
        access: Int,
        val methodName: String,
        desc: String?
) : AdviceAdapter(
        api,
        mv,
        access,
        methodName,
        desc
) {

    override fun onMethodEnter() {
        super.visitLdcInsn(methodName)
        callMonitor(Monitor::enterSystemMethod)
    }

}