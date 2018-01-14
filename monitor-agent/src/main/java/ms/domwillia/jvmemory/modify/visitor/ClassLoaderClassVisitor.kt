package ms.domwillia.jvmemory.modify.visitor

import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter


class ClassLoaderClassVisitor(cv: ClassVisitor?) : ClassVisitor(Opcodes.ASM6, cv) {

    override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
        var mv = super.visitMethod(access, name, desc, signature, exceptions)

        if (name == "loadClass" && desc == "(Ljava/lang/String;Z)Ljava/lang/Class;")
            mv = CallLoadingTracer(mv, access, name, desc)

        return mv
    }

    private class CallLoadingTracer(
            mv: MethodVisitor?,
            access: Int,
            methodName: String,
            desc: String?
    ) : AdviceAdapter(Opcodes.ASM6, mv, access, methodName, desc) {

        override fun onMethodEnter() {
            super.visitInsn(Opcodes.ICONST_1)
            super.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    Monitor.internalName,
                    "onClassLoad",
                    "(Z)V",
                    false
            )

            super.onMethodEnter()
        }

        override fun onMethodExit(opcode: Int) {
            super.visitInsn(Opcodes.ICONST_0)
            super.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    Monitor.internalName,
                    "onClassLoad",
                    "(Z)V",
                    false
            )

            super.onMethodExit(opcode)
        }
    }
}