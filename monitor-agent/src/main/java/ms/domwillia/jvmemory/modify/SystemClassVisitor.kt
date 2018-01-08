package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.Monitor
import ms.domwillia.jvmemory.monitor.definition.ClassDefinition
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FieldNode

open class SystemClassVisitor(writer: ClassWriter) : ClassVisitor(Opcodes.ASM6, writer) {

    protected lateinit var currentClass: ClassDefinition

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
        // add unique id field
        FieldNode(
                Opcodes.ACC_FINAL + Opcodes.ACC_PUBLIC,
                Monitor.instanceIdFieldName,
                "J",
                null,
                null
        ).accept(cv)

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

        // constructor patching
        if ("<init>" == name)
            mv = ConstructorPatcher(mv, currentClass.name, currentClass.superName)

        return mv
    }
}
