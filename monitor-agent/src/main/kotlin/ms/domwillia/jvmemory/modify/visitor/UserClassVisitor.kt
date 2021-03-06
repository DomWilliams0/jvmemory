package ms.domwillia.jvmemory.modify.visitor

import ms.domwillia.jvmemory.modify.patcher.CallTracer
import ms.domwillia.jvmemory.modify.patcher.MainPatcher
import ms.domwillia.jvmemory.modify.patcher.MethodPatcher
import ms.domwillia.jvmemory.modify.patcher.tidyClassName
import ms.domwillia.jvmemory.monitor.Monitor
import ms.domwillia.jvmemory.monitor.definition.ClassDefinition
import ms.domwillia.jvmemory.monitor.definition.ClassType
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor

class UserClassVisitor(api: Int, writer: ClassWriter) : ClassVisitor(api, writer) {

    private lateinit var currentClass: ClassDefinition

    override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<String>?
    ) {
        currentClass = ClassDefinition(name.tidyClassName(), access, superName?.tidyClassName(), interfaces)
        super.visit(version, access, name, signature, superName, interfaces)
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

        val builtin = BuiltinMethod.parse(name, desc)

        // call tracing
        // TODO check this at the method level instead of class level
        if (currentClass.flags.type != ClassType.INTERFACE && builtin != BuiltinMethod.TO_STRING)
            mv = CallTracer(api, currentClass.name, mv, access, name, desc)

        // main
        if (builtin == BuiltinMethod.MAIN)
            mv = MainPatcher(api, mv, access, name, desc)

        // instruction patching
        if (builtin != BuiltinMethod.TO_STRING)
            mv = MethodPatcher(api, mv, currentClass.registerMethod(access, name, desc))

        return mv
    }

    override fun visitEnd() {
        super.visitEnd()
        Monitor.onDefineClass(currentClass.toProtoBuf().toByteArray())
    }
}
