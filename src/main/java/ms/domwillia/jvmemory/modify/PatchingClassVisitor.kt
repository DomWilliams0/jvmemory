package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.Monitor
import ms.domwillia.jvmemory.monitor.definition.ClassDefinition
import ms.domwillia.jvmemory.monitor.definition.ClassType
import org.objectweb.asm.*
import org.objectweb.asm.commons.LocalVariablesSorter

class PatchingClassVisitor(writer: ClassWriter) : ClassVisitor(Opcodes.ASM6, writer) {

    private lateinit var currentClass: ClassDefinition

    override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<String>?
    ) {
        currentClass = ClassDefinition(name, access, superName, interfaces)
        super.visit(version, access, name, signature, superName, interfaces)
    }

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

        // instruction patching
        val instr = MethodPatcher(mv, name, currentClass.registerMethod(access, name, desc))
        val localVarSorter = LocalVariablesSorter(access, desc, instr)
        instr.localVarSorter = localVarSorter

        // call tracing
        // TODO check this at the method level instead of class level
        if (currentClass.flags.type != ClassType.INTERFACE)
            mv = CallTracer(currentClass.name, localVarSorter, access, name, desc)

        // constructor patching
        if ("<init>" == name)
            mv = ConstructorPatcher(mv, currentClass.name)

        return mv
    }
}
