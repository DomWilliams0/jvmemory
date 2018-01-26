package ms.domwillia.jvmemory.modify.visitor

import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class ObjectClassVisitor(writer: ClassWriter) : ClassVisitor(Opcodes.ASM6, writer) {

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
            mv = ObjectConstructorTracer(mv)
        }

        return mv
    }

    private class ObjectConstructorTracer(mv: MethodVisitor?) : MethodVisitor(Opcodes.ASM6, mv) {

        override fun visitInsn(opcode: Int) {
            if (opcode != Opcodes.RETURN) {
                super.visitInsn(opcode)
                return
            }

            super.visitVarInsn(Opcodes.ALOAD, 0)
            super.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    Monitor.internalName,
                    "allocateTag",
                    "(Ljava/lang/Object;)V",
                    false
            )

            super.visitInsn(opcode)
        }
    }
}