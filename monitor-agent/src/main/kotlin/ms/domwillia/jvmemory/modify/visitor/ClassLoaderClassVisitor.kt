package ms.domwillia.jvmemory.modify.visitor

import ms.domwillia.jvmemory.modify.patcher.callMonitor
import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter


class ClassLoaderClassVisitor(api: Int, cv: ClassVisitor?) : ClassVisitor(api, cv) {

    override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
        var mv = super.visitMethod(access, name, desc, signature, exceptions)

        if (name == "loadClass" && desc == "(Ljava/lang/String;Z)Ljava/lang/Class;")
            mv = CallLoadingTracer(api, mv, access, name, desc)

        return mv
    }

    private class CallLoadingTracer(
            api: Int,
            mv: MethodVisitor?,
            access: Int,
            methodName: String,
            desc: String?
    ) : AdviceAdapter(api, mv, access, methodName, desc) {

        override fun onMethodEnter() {
            super.visitInsn(Opcodes.ICONST_1)
            callMonitor(Monitor::enterIgnoreRegion)

            super.onMethodEnter()
        }

        override fun onMethodExit(opcode: Int) {
            super.visitInsn(Opcodes.ICONST_0)
            callMonitor(Monitor::enterIgnoreRegion)

            super.onMethodExit(opcode)
        }
    }
}