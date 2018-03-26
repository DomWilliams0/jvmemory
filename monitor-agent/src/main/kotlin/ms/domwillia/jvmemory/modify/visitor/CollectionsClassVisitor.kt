package ms.domwillia.jvmemory.modify.visitor

import ms.domwillia.jvmemory.modify.patcher.CollectionsPatcher
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor

class CollectionsClassVisitor(api: Int, writer: ClassWriter) : ClassVisitor(api, writer) {
    private lateinit var className: String

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        className = name
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            exceptions: Array<String>?
    ): MethodVisitor? {

        var mv = super.visitMethod(access, name, desc, signature, exceptions)


        if (BuiltinMethod.parse(name, desc) != BuiltinMethod.TO_STRING)
            mv = CollectionsPatcher(api, mv, className, access, name, desc)

        return mv
    }
}
