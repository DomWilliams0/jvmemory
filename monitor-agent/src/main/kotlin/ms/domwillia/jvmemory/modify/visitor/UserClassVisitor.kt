package ms.domwillia.jvmemory.modify.visitor

import ms.domwillia.jvmemory.modify.patcher.CallTracer
import ms.domwillia.jvmemory.modify.patcher.MainPatcher
import ms.domwillia.jvmemory.modify.patcher.MethodPatcher
import ms.domwillia.jvmemory.monitor.Monitor
import ms.domwillia.jvmemory.monitor.definition.ClassDefinition
import ms.domwillia.jvmemory.monitor.definition.ClassType
import org.objectweb.asm.*

class UserClassVisitor(writer: ClassWriter) : ClassVisitor(Opcodes.ASM6, writer) {

    private lateinit var currentClass: ClassDefinition

    override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<String>?
    ) {
        fun tidyClassName(name: String?): String? = name?.replace('/', '.')
        currentClass = ClassDefinition(tidyClassName(name)!!, access, tidyClassName(superName), interfaces)
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

        // instruction patching
        mv = MethodPatcher(mv, currentClass.registerMethod(access, name, desc))

        // call tracing
        // TODO check this at the method level instead of class level
        if (currentClass.flags.type != ClassType.INTERFACE)
            mv = CallTracer(currentClass.name, mv, access, name, desc)

        // main
        if (name == "main" && desc == "([Ljava/lang/String;)V")
            mv = MainPatcher(mv, access, name, desc)

        return mv
    }

    override fun visitEnd() {
        super.visitEnd()
        Monitor.onDefineClass(currentClass.toProtoBuf().toByteArray())
    }
}
