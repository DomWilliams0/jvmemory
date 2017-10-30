package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.LocalVarTracker
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.LocalVariablesSorter

class PatchingClassVisitor(writer: ClassWriter) : ClassVisitor(Opcodes.ASM6, writer) {

    private lateinit var currentClass: String
    private lateinit var localVars: LocalVarTracker

    override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<String>?
    ) {
        currentClass = name
        localVars = LocalVarTracker()
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitEnd() {
        println("==== $currentClass")
        localVars.debugPrint()

        // TODO write out to file
        super.visitEnd()
    }

    override fun visitMethod(
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            exceptions: Array<String>?
    ): MethodVisitor? {
        val mv = super.visitMethod(access, name, desc, signature, exceptions)
        val instr = InstructionPatcher(mv, currentClass, name, localVars)
        val localVarSorter = LocalVariablesSorter(access, desc, instr)
        instr.localVarSorter = localVarSorter

        return localVarSorter
    }
}