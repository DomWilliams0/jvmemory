package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.InjectedMonitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.InstructionAdapter

class ConstructorPatcher(
        private val className: String,
        mv: MethodVisitor
) : InstructionAdapter(Opcodes.ASM6, mv) {

    override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, desc: String?, itf: Boolean) {
        if (opcode == Opcodes.INVOKESPECIAL && owner == "java/lang/Object" && name == "<init>")
            initialiseInjectedMonitor()

        super.visitMethodInsn(opcode, owner, name, desc, itf)
    }

    private fun initialiseInjectedMonitor() {
        val monitorType = Type.getType(InjectedMonitor::class.java)

        // 4: aload_0
        super.visitVarInsn(Opcodes.ALOAD, 0)

        // 5: new           #2                  // class Bean
        super.visitTypeInsn(Opcodes.NEW, monitorType.internalName)

        // 8: dup
        super.dup()

        // 9: invokespecial #3                  // Method Bean."<init>":()V
        super.visitMethodInsn(Opcodes.INVOKESPECIAL,
                monitorType.internalName,
                "<init>",
                "()V",
                false
        )

        // 12: putfield      #4                  // Field bean:LBean;
        super.visitFieldInsn(Opcodes.PUTFIELD, className, InjectedMonitor.fieldName, monitorType.descriptor)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        super.visitMaxs(maxStack + 4, maxLocals)
    }

}
