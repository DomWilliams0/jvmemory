package ms.domwillia.jvmemory.modify.visitor

import ms.domwillia.jvmemory.modify.patcher.callMonitor
import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class ObjectClassVisitor(api: Int, writer: ClassWriter) : ClassVisitor(api, writer) {

    override fun visitMethod(
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            exceptions: Array<String>?
    ): MethodVisitor? {
        var mv = super.visitMethod(access, name, desc, signature, exceptions)

        // constructor only
        if (name == "<init>") {
            mv = ObjectConstructorTracer(api, mv)
        }

        return mv
    }

    private class ObjectConstructorTracer(api: Int, mv: MethodVisitor?) : MethodVisitor(api, mv) {

        override fun visitInsn(opcode: Int) {
            if (opcode != Opcodes.RETURN) {
                super.visitInsn(opcode)
                return
            }

            super.visitVarInsn(Opcodes.ALOAD, 0)

            // stack: this
            super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Object",
                    "getClass",
                    "()Ljava/lang/Class;",
                    false
            )

            // stack: class
            super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Class",
                    "getTypeName",
                    "()Ljava/lang/String;",
                    false
            )

            // stack: name
            super.visitVarInsn(Opcodes.ALOAD, 0)

            // stack: name this
            callMonitor(Monitor::allocateTag)

            super.visitInsn(opcode)
        }
    }
}
