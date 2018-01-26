package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.modify.visitor.ClassLoaderClassVisitor
import ms.domwillia.jvmemory.modify.visitor.ObjectClassVisitor
import ms.domwillia.jvmemory.modify.visitor.UserClassVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain
import java.util.jar.JarFile
import kotlin.system.exitProcess

class BytecodeTransformer(private val userClassPrefixes: List<String>) : ClassFileTransformer {

    private fun isUserClass(className: String): Boolean {
        return userClassPrefixes.find { className.startsWith(it) } != null
    }

    private fun createVisitor(className: String): ((ClassWriter) -> ClassVisitor)? = when {
    // user classes
        isUserClass(className) -> ::UserClassVisitor

    // system
        className == "java/lang/Object" -> ::ObjectClassVisitor
        className == "java/lang/ClassLoader" -> ::ClassLoaderClassVisitor

    // no need to instrument any other classes
        else -> null
    }

    override fun transform(loader: ClassLoader?, className: String,
                           classBeingRedefined: Class<*>?, protectionDomain: ProtectionDomain?,
                           classfileBuffer: ByteArray): ByteArray? {

        return createVisitor(className)?.run {

            val reader = ClassReader(classfileBuffer)
            val writer = ClassWriter(reader, ClassWriter.COMPUTE_FRAMES)

            try {
                // teehee
                reader.accept(this(writer), ClassReader.EXPAND_FRAMES)
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }

            val bytes = writer.toByteArray()
            val classDir = "/tmp/classes"
            File(classDir).mkdir()

            val outPath = "$classDir/mod_${className.replace('/', '.')}.class"
            File(outPath).outputStream().use { file ->
                file.write(bytes)
            }
            println("Wrote modified class to $outPath")
            System.out.flush()

            bytes
        }
    }

    companion object {
        private fun bail(msg: String = ""): Nothing {
            if (msg.isNotEmpty())
                System.err.println("ERROR: $msg")

            System.err.println("Usage: <bootstrap.jar>,<user packages...>")
            exitProcess(1)
        }

        @JvmStatic
        fun premain(agentArgs: String?, inst: Instrumentation) {
            val args = agentArgs ?: bail("No args provided")
            val split = args.splitToSequence(',')

            val bootstrapPath = split.take(1).elementAtOrNull(0) ?: bail()
            val classes = split.drop(1)
                    .filter(String::isNotEmpty)
                    .map { it.replace('.', '/') }
                    .toList()

            if (!File(bootstrapPath).isFile)
                bail("Bad bootstrap.jar argument '$bootstrapPath'")

            inst.appendToBootstrapClassLoaderSearch(JarFile(bootstrapPath))
            println("Adding to bootstrap path: $bootstrapPath")
            inst.addTransformer(BytecodeTransformer(classes), true)
            inst.retransformClasses(java.lang.Object::class.java)
            inst.retransformClasses(java.lang.ClassLoader::class.java)
        }
    }
}