package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.InjectedMonitor
import ms.domwillia.jvmemory.monitor.LocalVarTracker
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.InstructionAdapter
import org.objectweb.asm.commons.LocalVariablesSorter

// TODO helper data class for class+method
class MethodPatcher(
        delegate: MethodVisitor,
        private val className: String,
        private val methodName: String,
        private val localVarTracker: LocalVarTracker
) : InstructionAdapter(Opcodes.ASM6, delegate) {

    lateinit var localVarSorter: LocalVariablesSorter

    val isConstructor
        get() = this.methodName == "<init>"

    override fun store(index: Int, type: Type) {
        InjectedMonitor.getTypeSpecificLocalVarFuncName(true, type)?.let { handler ->
            // dup value and store in a tmp var
            // TODO we can definitely reuse this tmpvar, should not make a new one for every single store!!
            dupTypeSpecific(type)
            val tmpVar = localVarSorter.newLocal(type)
            super.store(tmpVar, type)

            super.visitVarInsn(Opcodes.ALOAD, 0)
            super.visitFieldInsn(
                    Opcodes.GETFIELD,
                    className,
                    InjectedMonitor.fieldName,
                    InjectedMonitor.descriptor
            )

            super.load(tmpVar, type)
            super.iconst(index)
            super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    InjectedMonitor.internalName,
                    handler,
                    "(${type.descriptor}I)V",
                    false
            )
        }

        super.store(index, type)
    }

//    override fun load(index: Int, type: Type) {
//        getTypeSpecificHandlerName(type)?.let { handler ->
//            super.iconst(index)
//            super.visitMethodInsn(
//                    INVOKESTATIC,
//                    getPrinterClass("load"),
//                    handler,
//                    "(I)V",
//                    false
//            )
//        }
//
//        super.load(index, type)
//    }

    // does this depend on architecture/implementation?
    private fun dupTypeSpecific(type: Type) {
        when (type.sort) {
            Type.LONG, Type.DOUBLE -> super.dup2()
            else -> super.dup()
        }
    }

    private fun getPrinterClass(what: String): String {
        return "ms/domwillia/jvmemory/monitor/printer/${what.capitalize()}Printer"
    }

}