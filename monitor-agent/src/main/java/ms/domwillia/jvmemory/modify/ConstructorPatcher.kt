package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

class ConstructorPatcher(mv: MethodVisitor?, private val className: String, private val superName: String?) :
        MethodVisitor(Opcodes.ASM6, mv) {

    private fun visitShouldLogConstructor() {

        super.visitVarInsn(Opcodes.ALOAD, 0)

        super.visitLdcInsn(Type.getObjectType(className).className)
        super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/Class",
                "forName",
                "(Ljava/lang/String;)Ljava/lang/Class;",
                false
        )

        // stack: this clazz
        super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                Monitor.internalName,
                "allocateTag",
                "(Ljava/lang/Object;Ljava/lang/Class;)J",
                false
        )
    }

    override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, desc: String?, itf: Boolean) {
        super.visitMethodInsn(opcode, owner, name, desc, itf)

        if (opcode == Opcodes.INVOKESPECIAL &&
                (owner == "java/lang/Object" || owner == superName) &&
                name == "<init>") {
            visitShouldLogConstructor()
        }
    }
}