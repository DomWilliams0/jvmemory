package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.Monitor
import ms.domwillia.jvmemory.monitor.definition.ClassDefinition
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Opcodes

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

    override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor {
        currentClass.registerField(access, name, desc)
        return super.visitField(access, name, desc, signature, value)
    }

    override fun visitEnd() {
        Monitor.logger.logClassDefinition(currentClass)
    }
}
