package ms.domwillia.jvmemory.modify.visitor

import ms.domwillia.jvmemory.modify.patcher.CollectionsPatcher
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class CollectionsClassVisitor(api: Int, writer: ClassWriter) : ClassVisitor(api, writer) {

    override fun visitMethod(
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            exceptions: Array<String>?
    ): MethodVisitor? {

        var mv = super.visitMethod(access, name, desc, signature, exceptions)

        mv = CollectionsPatcher(api, mv, access, name, desc)

        return mv
    }
}
