package ms.domwillia.jvmemory.modify.patcher

import ms.domwillia.jvmemory.monitor.Monitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import kotlin.reflect.KFunction

typealias MonitorMethod = KFunction<Unit>

private val monitorName = Type.getType(Monitor::class.java).internalName!!

fun <R> MethodVisitor.callMonitor(method: KFunction<R>) {
    val javaMethod = Monitor::class.java.methods.find { it.name == method.name } ?:
            throw IllegalStateException("method doesn't exist")

    visitMethodInsn(
            Opcodes.INVOKESTATIC,
            monitorName,
            method.name,
            Type.getMethodDescriptor(javaMethod),
            false
    )
}

fun String.tidyClassName(): String = replace('/', '.')
