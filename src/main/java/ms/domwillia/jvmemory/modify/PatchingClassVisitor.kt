package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.InjectedMonitor
import ms.domwillia.jvmemory.monitor.LocalVarTracker
import org.objectweb.asm.*
import org.objectweb.asm.commons.LocalVariablesSorter
import org.objectweb.asm.tree.FieldNode

class PatchingClassVisitor(writer: ClassWriter) : ClassVisitor(Opcodes.ASM6, writer) {

    private lateinit var currentClass: String
    private var isInterface: Boolean = false
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
        isInterface = access.and(Opcodes.ACC_INTERFACE) != 0
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitEnd() {
        if (!isInterface) {
            // add injected field
            val injectField = FieldNode(
                    Opcodes.ACC_FINAL + Opcodes.ACC_PRIVATE,
                    InjectedMonitor.fieldName,
                    Type.getDescriptor(InjectedMonitor::class.java),
                    null,
                    null
            )
            injectField.accept(cv)
        }

        // TODO write out to log instead of printing
        println("==== $currentClass")
        localVars.debugPrint()

        super.visitEnd()
    }

    override fun visitMethod(
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            exceptions: Array<String>?
    ): MethodVisitor? {
        var mv = super.visitMethod(access, name, desc, signature, exceptions)

        // instruction patching
        val instr = MethodPatcher(mv, currentClass, name, localVars)
        val localVarSorter = LocalVariablesSorter(access, desc, instr)
        instr.localVarSorter = localVarSorter

        // special constructor patcher
        mv = if (instr.isConstructor) {
            ConstructorPatcher(currentClass, localVarSorter)
        } else {
            localVarSorter
        }

        // call tracing
        if (!isInterface)
            mv = CallTracer(currentClass, mv, access, name, desc)

        return mv
    }
}
