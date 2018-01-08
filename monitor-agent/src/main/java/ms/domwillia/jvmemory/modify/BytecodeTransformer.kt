package ms.domwillia.jvmemory.modify

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

class BytecodeTransformer : ClassFileTransformer {

    override fun transform(loader: ClassLoader, className: String,
                           classBeingRedefined: Class<*>, protectionDomain: ProtectionDomain,
                           classfileBuffer: ByteArray): ByteArray? {

        if (className.startsWith("ms/domwillia") &&
                (!className.startsWith("ms/domwillia/jvmemory/") || className.startsWith("ms/domwillia/jvmemory/specimen"))) {

            val rewritten = run {
                val reader = ClassReader(classfileBuffer)
                val writer = ClassWriter(reader, 0)

                PatchingClassVisitor(writer).apply {
                    try {
                        reader.accept(this, ClassReader.EXPAND_FRAMES)
                    } catch (e: RuntimeException) {
                        e.printStackTrace()
                    }
                }

                writer.toByteArray()
            }

            val outPath = "/tmp/modified_" + className.replace('/', '.') + ".class"
            File(outPath).outputStream().use { file ->
                file.write(rewritten)
            }
            println("Wrote modified class to $outPath")

            return rewritten
        }

        return null
    }

    companion object {
        @JvmStatic
        fun premain(agentArgs: String?, inst: Instrumentation) {
            inst.addTransformer(BytecodeTransformer())
        }
    }
}
