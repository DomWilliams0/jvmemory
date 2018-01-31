package ms.domwillia.jvmemory.modify.patcher

import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class MainPatcher(
        mv: MethodVisitor?,
        access: Int,
        name: String?,
        desc: String?
) : AdviceAdapter(Opcodes.ASM6, mv, access, name, desc) {
    private fun setRunning(running: Boolean) {
        val opcode = if (running) {
            Opcodes.ICONST_1
        } else {
            Opcodes.ICONST_0
        }

        visitInsn(opcode)
        callMonitor(Monitor::setProgramInProgress)
    }

    override fun onMethodEnter() {
        setRunning(true)
        super.onMethodEnter()
    }

    // TODO other threads might carry on after main ends
    override fun onMethodExit(opcode: Int) {
        setRunning(false)
        super.onMethodExit(opcode)
    }
}