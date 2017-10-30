package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.monitor.LocalVarTracker
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
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

}