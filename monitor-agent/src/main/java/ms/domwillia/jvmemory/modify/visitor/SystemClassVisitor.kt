package ms.domwillia.jvmemory.modify.visitor

import ms.domwillia.jvmemory.modify.tracer.ObjectConstructorTracer
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

        // java/lang/Object constructor only
        if (currentClass.superName == null && name == "<init>") {
            mv = ObjectConstructorTracer(mv)
        }

        return mv
    }
}
