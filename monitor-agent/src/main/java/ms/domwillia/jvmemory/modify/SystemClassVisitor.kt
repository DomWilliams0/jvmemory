package ms.domwillia.jvmemory.modify

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor

class SystemClassVisitor(writer: ClassWriter) : BaseClassVisitor(writer) {

    override fun visitMethod(
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            exceptions: Array<String>?
    ): MethodVisitor? {
        var mv = super.visitMethod(access, name, desc, signature, exceptions)

        // constructor
        if (name == "<init>") {
            // not object
            mv = if (currentClass.superName != null)
                SystemConstructorTracer(currentClass.name, mv, access, desc)
            // object
            else
                ObjectConstructorTracer(mv)
        }

        return mv
    }
}
