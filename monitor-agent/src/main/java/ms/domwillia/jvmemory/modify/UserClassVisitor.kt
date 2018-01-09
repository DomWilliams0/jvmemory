package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.Monitor
import ms.domwillia.jvmemory.monitor.definition.ClassType
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.LocalVariablesSorter
import org.objectweb.asm.tree.FieldNode

class UserClassVisitor(writer: ClassWriter) : SystemClassVisitor(writer) {

    private var hasDoneFinalize = false

    override fun visitEnd() {
        super.visitEnd()
        currentClass.debugPrint()

        // add unique id field
        FieldNode(
                Opcodes.ACC_FINAL + Opcodes.ACC_PUBLIC,
                Monitor.instanceIdFieldName,
                "J",
                null,
                null
        ).accept(cv)
    }

    override fun visitMethod(
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            exceptions: Array<String>?
    ): MethodVisitor? {

        var mv = super.visitMethod(access, name, desc, signature, exceptions)

        when (name) {
        // constructor
            "<init>" -> mv = ConstructorPatcher(mv, currentClass.name, currentClass.superName)

        // destructor
            "finalize" -> {
                mv = FinalizePatcher(mv, currentClass.name)
                hasDoneFinalize = true
            }
        }

        // instruction patching
        val instr = MethodPatcher(mv, name, currentClass.registerMethod(access, name, desc))
        val localVarSorter = LocalVariablesSorter(access, desc, instr)
        instr.localVarSorter = localVarSorter

        // call tracing
        // TODO check this at the method level instead of class level
        if (currentClass.flags.type != ClassType.INTERFACE)
            mv = CallTracer(currentClass.name, localVarSorter, access, name, desc)

        return mv
    }
}
