package ms.domwillia.jvmemory.modify.patcher

import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class ArrayNativeMethodPatcher(api: Int, mv: MethodVisitor) : MethodVisitor(api, mv) {

    init {
        // visitCode() is never actually called because the original method is native
        // and thus has no method body - this is a hack to add a method body
        super.visitCode()
        super.visitVarInsn(Opcodes.ALOAD, 0)
        super.visitVarInsn(Opcodes.ALOAD, 1)
        callMonitor(Monitor::newArrayWrapper)
        super.visitInsn(Opcodes.ARETURN)
    }
}
