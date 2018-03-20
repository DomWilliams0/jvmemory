package ms.domwillia.jvmemory.modify.visitor

import ms.domwillia.jvmemory.modify.patcher.callMonitor
import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type


class StringClassVisitor(api: Int, cv: ClassVisitor?) : ClassVisitor(api, cv) {

    override fun visitMethod(access: Int, name: String?, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor {
        var mv = super.visitMethod(access, name, desc, signature, exceptions)

        if (Type.getType(desc).returnType.className == "java.lang.String" || name == "<init>") {
            mv = StringMethodPatcher(api, mv, access)
        }

        return mv
    }
}

private class StringMethodPatcher(api: Int, mv: MethodVisitor?, val access: Int) : MethodVisitor(api, mv) {

    override fun visitInsn(opcode: Int) {
        when (opcode) {
            Opcodes.ARETURN -> {
                super.visitInsn(Opcodes.DUP)
                callMonitor(Monitor::toStringObject)
            }
            Opcodes.RETURN -> if (access.and(Opcodes.ACC_STATIC) == 0) {
                super.visitVarInsn(Opcodes.ALOAD, 0)
                callMonitor(Monitor::toStringObject)
            }
        }

        super.visitInsn(opcode)
    }
}