package ms.domwillia.jvmemory.modify.patcher

import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class CollectionsPatcher(
        api: Int,
        mv: MethodVisitor?,
        access: Int,
        name: String,
        desc: String?
) : AdviceAdapter(
        api,
        mv,
        access,
        name,
        desc
) {

    override fun onMethodExit(opcode: Int) {
        super.visitVarInsn(Opcodes.ALOAD, 0)
        callMonitor(Monitor::enterSystemMethod)
    }

}