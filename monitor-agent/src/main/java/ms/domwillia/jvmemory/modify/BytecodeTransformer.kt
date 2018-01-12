package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.modify.visitor.SystemClassVisitor
import ms.domwillia.jvmemory.modify.visitor.UserClassVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain
import java.util.jar.JarFile

class BytecodeTransformer : ClassFileTransformer {

    enum class PatcherType {
        USER,
        SYSTEM,
        NONE
    }

    private fun createVisitor(className: String): ((ClassWriter) -> ClassVisitor)? {
        val type = when {
        // blacklist jvmemory classes
            className.startsWith("ms/domwillia/jvmemory") -> PatcherType.NONE
            className.startsWith("com/google/protobuf") -> PatcherType.NONE

        // user classes
        // TODO controlled by user
            className.startsWith("ms/domwillia/specimen") -> PatcherType.USER

        // system
            className == "java/lang/Object" -> PatcherType.SYSTEM

        // no need to instrument any other classes
            else -> PatcherType.NONE
        }

        return when (type) {
            BytecodeTransformer.PatcherType.USER -> ::UserClassVisitor
            BytecodeTransformer.PatcherType.SYSTEM -> ::SystemClassVisitor
            BytecodeTransformer.PatcherType.NONE -> null
        }
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

            bytes
        }
    }

    companion object {
        @JvmStatic
        fun premain(agentArgs: String?, inst: Instrumentation) {
            inst.appendToBootstrapClassLoaderSearch(JarFile("out/artifacts/bootstrap/bootstrap.jar"))
            inst.addTransformer(BytecodeTransformer(), true)
            inst.retransformClasses(java.lang.Object::class.java)
        }
    }
}
