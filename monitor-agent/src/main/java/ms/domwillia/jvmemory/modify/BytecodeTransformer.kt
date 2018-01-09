package ms.domwillia.jvmemory.modify

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

class BytecodeTransformer : ClassFileTransformer {

    private enum class PatcherType {
        USER,
        SYSTEM,
        NONE
    }

    private fun createVisitor(className: String): Pair<PatcherType, ((ClassWriter) -> ClassVisitor)?> {
        val type = when {
        // blacklist jvmemory classes
            className.startsWith("ms/domwillia/jvmemory") -> PatcherType.NONE

        // user classes
        // TODO controlled by user
            className.startsWith("ms/domwillia/specimen") -> PatcherType.USER

        // ide
            className.startsWith("com/intellij") -> PatcherType.NONE

        // system
            else -> PatcherType.SYSTEM
        }

        val func = when (type) {
            BytecodeTransformer.PatcherType.USER -> ::UserClassVisitor
            BytecodeTransformer.PatcherType.SYSTEM -> ::SystemClassVisitor
            BytecodeTransformer.PatcherType.NONE -> null
        }

        return Pair(type, func)
    }

    override fun transform(loader: ClassLoader?, className: String,
                           classBeingRedefined: Class<*>?, protectionDomain: ProtectionDomain?,
                           classfileBuffer: ByteArray): ByteArray? {

        val (type, visitor) = createVisitor(className)
        val rewritten = visitor?.run {

            val reader = ClassReader(classfileBuffer)
            val writer = ClassWriter(reader, 0)

            try {
                // teehee
                reader.accept(this(writer), ClassReader.EXPAND_FRAMES)
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }

            writer.toByteArray()
        }

        if (type == PatcherType.USER) {
            val outPath = "/tmp/modified_" + className.replace('/', '.') + ".class"
            File(outPath).outputStream().use { file ->
                file.write(rewritten)
            }
            println("Wrote modified class to $outPath")
        }

        return rewritten
    }

    companion object {
        @JvmStatic
        fun premain(agentArgs: String?, inst: Instrumentation) {
            inst.addTransformer(BytecodeTransformer(), true)

            inst.allLoadedClasses
                    .filter(inst::isModifiableClass)
                    .forEach {
                        try {
                            inst.retransformClasses(it)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
        }
    }
}
