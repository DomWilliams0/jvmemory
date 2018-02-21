package ms.domwillia.jvmemory.modify.visitor

import ms.domwillia.jvmemory.modify.patcher.ArrayNativeMethodPatcher
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class ArrayNativeClassVisitor(api: Int, writer: ClassWriter) : ClassVisitor(api, writer) {

    override fun visitMethod(
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            exceptions: Array<String>?
    ): MethodVisitor? {

        // TODO multidim too
        val multiDim = when (name) {
            "newArray" -> false
            "multiNewArray" -> true
            else -> return super.visitMethod(access, name, desc, signature, exceptions)
        }

        val newAccess = access.and(Opcodes.ACC_NATIVE.inv())
        val mv = super.visitMethod(newAccess, name, desc, signature, exceptions)
        return ArrayNativeMethodPatcher(api, mv, multiDim)
    }

}
