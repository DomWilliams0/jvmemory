package ms.domwillia.jvmemory.modify

import jdk.nashorn.internal.codegen.types.Type
import ms.domwillia.jvmemory.monitor.InjectedMonitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class CallTracer(private val className: String, mv: MethodVisitor?, access: Int, private val methodName: String, desc: String?) : AdviceAdapter(
        Opcodes.ASM6,
        mv,
        access,
        methodName,
        desc
) {

    override fun onMethodEnter() {
        super.visitVarInsn(Opcodes.ALOAD, 0)

        val injectType = Type.typeFor(InjectedMonitor::class.java)
        super.visitFieldInsn(
                Opcodes.GETFIELD,
                className,
                InjectedMonitor.fieldName,
                injectType.descriptor
        )

        super.visitLdcInsn(className)
        super.visitLdcInsn(methodName)
        super.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                injectType.internalName,
                "enterMethod",
                "(Ljava/lang/String;Ljava/lang/String;)V",
                false
        )

        super.onMethodEnter()
    }

    override fun onMethodExit(opcode: Int) {
        super.visitVarInsn(Opcodes.ALOAD, 0)

        val injectType = Type.typeFor(InjectedMonitor::class.java)
        super.visitFieldInsn(
                Opcodes.GETFIELD,
                className,
                InjectedMonitor.fieldName,
                injectType.descriptor
        )

        super.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                injectType.internalName,
                "exitMethod",
                "()V",
                false
        )
        super.onMethodExit(opcode)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        super.visitMaxs(maxStack + 2, maxLocals)
    }
}