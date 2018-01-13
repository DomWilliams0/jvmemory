package ms.domwillia.jvmemory.modify.visitor

import ms.domwillia.jvmemory.modify.MethodPatcher
import ms.domwillia.jvmemory.modify.tracer.CallTracer
import ms.domwillia.jvmemory.monitor.Monitor
import ms.domwillia.jvmemory.monitor.definition.ClassType
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.LocalVariablesSorter

class UserClassVisitor(writer: ClassWriter) : BaseClassVisitor(writer) {

    override fun visitEnd() {
        super.visitEnd()
        Monitor.onDefineClass(currentClass.toProtoBuf().toByteArray())
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
        mv = run {
            val instr = MethodPatcher(mv, currentClass.registerMethod(access, name, desc))
            val localVarSorter = LocalVariablesSorter(access, desc, instr)
            instr.localVarSorter = localVarSorter
            instr
        }

        // call tracing
        // TODO check this at the method level instead of class level
        if (currentClass.flags.type != ClassType.INTERFACE)
            mv = CallTracer(currentClass.name, mv, access, name, desc)

        return mv
    }
}
