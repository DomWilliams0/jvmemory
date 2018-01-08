package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

class ConstructorPatcher(mv: MethodVisitor?, private val className: String, private val superName: String?) :
        MethodVisitor(Opcodes.ASM6, mv) {

    private fun visitShouldLogConstructor() {
        // TODO this could do with some deduplication
        fun generateAndAssignId() {
            // stack:
            super.visitVarInsn(Opcodes.ALOAD, 0)

            // stack: this
            super.visitFieldInsn(
                    Opcodes.GETSTATIC,
                    Monitor.internalName,
                    Monitor.instanceName,
                    Monitor.descriptor
            )

            // stack: this monitor
            super.visitLdcInsn(className)
            super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    Monitor.internalName,
                    "onAlloc",
                    "(Ljava/lang/String;)J",
                    false
            )

            // stack: this id
            super.visitFieldInsn(
                    Opcodes.PUTFIELD,
                    className,
                    Monitor.instanceIdFieldName,
                    "J"
            )
        }

        fun assignCurrentEffectiveId() {
            // stack:
            super.visitVarInsn(Opcodes.ALOAD, 0)

            // stack: this
            super.visitFieldInsn(
                    Opcodes.GETSTATIC,
                    Monitor.internalName,
                    Monitor.instanceName,
                    Monitor.descriptor
            )

            // stack: this monitor
            // getter is used here because accessing the field directly throws a NoSuchFieldError
            super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    Monitor.internalName,
                    "getEffectiveId",
                    "()J",
                    false
            )

            // stack: this effectiveId
            super.visitFieldInsn(
                    Opcodes.PUTFIELD,
                    className,
                    Monitor.instanceIdFieldName,
                    "J"
            )
        }

        val retLabel = Label()
        val elseLabel = Label()

        // stack: this
        super.visitVarInsn(Opcodes.ALOAD, 0)
        super.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/Object",
                "getClass",
                "()Ljava/lang/Class;",
                false
        )

        // stack: clazz
        super.visitLdcInsn(Type.getObjectType(className).className)
        super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/Class",
                "forName",
                "(Ljava/lang/String;)Ljava/lang/Class;",
                false
        )

        // stack: clazz clazz
        super.visitJumpInsn(
                Opcodes.IF_ACMPNE,
                elseLabel
        )

        // if getClass() == Class.forName(...)
        generateAndAssignId()
        super.visitJumpInsn(Opcodes.GOTO, retLabel)

        // else
        super.visitLabel(elseLabel)
        assignCurrentEffectiveId()

        super.visitLabel(retLabel)
    }

    override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, desc: String?, itf: Boolean) {
        if (opcode == Opcodes.INVOKESPECIAL &&
                (owner == "java/lang/Object" || owner == superName) &&
                name == "<init>") {
            visitShouldLogConstructor()
        }

        super.visitMethodInsn(opcode, owner, name, desc, itf)
    }
}