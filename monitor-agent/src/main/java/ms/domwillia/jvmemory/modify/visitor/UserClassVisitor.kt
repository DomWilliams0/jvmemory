package ms.domwillia.jvmemory.modify.visitor

import ms.domwillia.jvmemory.modify.tracer.CallTracer
import ms.domwillia.jvmemory.monitor.definition.ClassType
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor

class UserClassVisitor(writer: ClassWriter) : BaseClassVisitor(writer) {

    override fun visitEnd() {
        super.visitEnd()
        currentClass.debugPrint()
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
        // TODO temporarily disabled while constructors are sorted out
//        val instr = MethodPatcher(mv, name, currentClass.registerMethod(access, name, desc))
//        val localVarSorter = LocalVariablesSorter(access, desc, instr)
//        instr.localVarSorter = localVarSorter

        // call tracing
        // TODO check this at the method level instead of class level
        if (currentClass.flags.type != ClassType.INTERFACE)
            mv = CallTracer(currentClass.name, mv, access, name, desc)

        return mv
    }
}
