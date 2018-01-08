package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.Monitor
import ms.domwillia.jvmemory.monitor.definition.ClassType
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.LocalVariablesSorter

class UserClassVisitor(writer: ClassWriter) : SystemClassVisitor(writer) {

    private var hasDoneFinalize = false

    override fun visitEnd() {
        currentClass.debugPrint()
        Monitor.logger.logClassDefinition(currentClass)
        super.visitEnd()
    }

    override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor {
        currentClass.registerField(access, name, desc)
        return super.visitField(access, name, desc, signature, value)
    }

    override fun visitMethod(
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            exceptions: Array<String>?
    ): MethodVisitor? {

        var mv = super.visitMethod(access, name, desc, signature, exceptions)

        // dealloc
        if ("finalize" == name) {
            mv = FinalizePatcher(mv, currentClass.name)
            hasDoneFinalize = true
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
