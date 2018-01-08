package ms.domwillia.jvmemory.modify

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

class BytecodeTransformer : ClassFileTransformer {

    private fun createVisitor(className: String): ((ClassWriter) -> ClassVisitor)? {
        // TODO decide between system and user classes
        return if (className.startsWith("ms/domwillia") &&
                (!className.startsWith("ms/domwillia/jvmemory/") || className.startsWith("ms/domwillia/specimen"))) {
            ::UserClassVisitor
        } else {
            null
        }
    }

    override fun transform(loader: ClassLoader, className: String,
                           classBeingRedefined: Class<*>?, protectionDomain: ProtectionDomain,
                           classfileBuffer: ByteArray): ByteArray? {

        val visitor = createVisitor(className)
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

        rewritten?.let {
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
            inst.addTransformer(BytecodeTransformer())
        }
    }
}
