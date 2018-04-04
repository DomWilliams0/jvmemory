package ms.domwillia.jvmemory.modify

import ms.domwillia.jvmemory.modify.patcher.tidyClassName
import ms.domwillia.jvmemory.modify.visitor.*
import org.objectweb.asm.*
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.net.URL
import java.net.URLClassLoader
import java.security.ProtectionDomain
import java.util.jar.JarFile
import kotlin.coroutines.experimental.buildSequence
import kotlin.system.exitProcess

typealias VisitorConstructor = ((Int, ClassWriter) -> ClassVisitor)

class BytecodeTransformer : ClassFileTransformer {

    private val apiVersion = Opcodes.ASM6

    private fun createVisitor(className: String): VisitorConstructor? = when {
    // user classes
        isUserClass(className) -> ::UserClassVisitor

    // special system
        className == "java/lang/Object" -> ::ObjectClassVisitor
        className == "java/lang/ClassLoader" -> ::ClassLoaderClassVisitor

    // system
        systemClassesDescriptors.containsKey(className) -> {
            systemClassesDescriptors[className]
        }


    // no need to instrument any other classes
        else -> null
    }

    override fun transform(loader: ClassLoader?, classNamePerhaps: String?,
                           classBeingRedefined: Class<*>?, protectionDomain: ProtectionDomain?,
                           classfileBuffer: ByteArray): ByteArray? {

        val className = classNamePerhaps ?: return null
        return createVisitor(className)?.let { visitor ->

            val reader = ClassReader(classfileBuffer)
            val writer = ClassWriter(reader, ClassWriter.COMPUTE_FRAMES)

            try {
                reader.accept(visitor(apiVersion, writer), ClassReader.EXPAND_FRAMES)
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }

            val bytes = writer.toByteArray()
            val classDir = "/tmp/classes"
            File(classDir).mkdir()

            val outPath = "$classDir/mod_${className.tidyClassName()}.class"
            File(outPath).outputStream().use { file ->
                file.write(bytes)
            }
            println("Wrote modified class to $outPath")
            System.out.flush()

            bytes
        }
    }

    companion object {
        private val systemClasses: Map<Class<out Any>, VisitorConstructor> = run {
            val baseClasses = mapOf(
                    java.util.ArrayList::class.java to ::CollectionsClassVisitor,
                    java.util.HashSet::class.java to ::CollectionsClassVisitor,
                    java.util.LinkedHashMap::class.java to ::CollectionsClassVisitor,
                    java.util.Arrays::class.java to ::CollectionsClassVisitor,
                    java.lang.reflect.Array::class.java to ::ArrayNativeClassVisitor,
                    java.lang.String::class.java to ::StringClassVisitor,
                    java.lang.StringBuilder::class.java to ::StringClassVisitor
            )

            fun getSuperClasses(clazz: Class<out Any>) = buildSequence {
                var c: Class<out Any> = clazz
                while (c != java.lang.Object::class.java) {
                    yield(c)
                    c.superclass.let { sup ->
                        c = (sup ?: return@buildSequence)
                    }
                }
            }

            // collects all superclasses of system classes and associates them with the same
            // VisitorConstructor as the listed subclass
            baseClasses
                    .map { getSuperClasses(it.key) to it.value } // -> [([supers], value)]
                    .flatMap { it.first.map { c -> c to it.second }.asIterable() } // -> [(cls, value)]
                    .toMap()
        }


        private val systemClassesDescriptors: Map<String, VisitorConstructor> by lazy {
            // if not lazy, LinkageError ooer
            systemClasses.mapKeys { Type.getType(it.key).internalName }
        }

        fun isSpecialSystemClass(clazz: String): Boolean = systemClassesDescriptors.containsKey(clazz)

        private lateinit var userPackages: List<String>

        fun isUserClass(className: String) = userPackages.find { className.startsWith(it) } != null

        fun isMonitorClass(clazz: String) = clazz.startsWith("ms/domwillia/jvmemory/monitor")

        private fun forceLoadSystemClasses() {
            // the native method multiNewArray isn't automatically loaded without this? bizarre
            java.lang.reflect.Array.newInstance(Int::class.java, 1, 1)
        }

        private fun bail(msg: String = ""): Nothing {
            if (msg.isNotEmpty())
                System.err.println("ERROR: $msg")

            System.err.println("Usage: <bootstrap.jar>;<user packages,...>;<classpath:...>")
            exitProcess(1)
        }

        @JvmStatic
        private fun appendToClassPath(path: String) {
            val f = File(path).toURI().toURL()
            val cl = ClassLoader.getSystemClassLoader()
            val m = URLClassLoader::class.java.getDeclaredMethod("addURL", java.net.URL::class.java)
            m.isAccessible = true
            m.invoke(cl, f)
            println("Added $path to classpath")
}


        @JvmStatic
        fun premain(agentArgs: String?, inst: Instrumentation) {
            val args = agentArgs ?: bail("No args provided")
            val split = args.split(';')

            val bootstrapPath = split.take(1).elementAtOrNull(0) ?: bail()
            userPackages = split.drop(1).firstOrNull()?.let {
                        it.splitToSequence(',')
                                .filter(String::isNotEmpty)
                                .map { it.replace('.', '/') } // cannot use String#tidyClassName here because JVMTI_ERROR_WRONG_PHASE
                                .toList()
                    } ?: bail()

            val classPath = split.drop(2)
                    .firstOrNull()
                    ?.split(':') ?: emptyList()
            classPath.forEach(this::appendToClassPath)


            if (!File(bootstrapPath).isFile)
                bail("Bad bootstrap.jar argument '$bootstrapPath'")

            inst.appendToBootstrapClassLoaderSearch(JarFile(bootstrapPath))
            println("Adding to bootstrap path: $bootstrapPath")
            inst.addTransformer(BytecodeTransformer(), true)
            inst.retransformClasses(java.lang.Object::class.java)
            inst.retransformClasses(java.lang.ClassLoader::class.java)

            forceLoadSystemClasses()
            systemClasses.keys.forEach { inst.retransformClasses(it) }
        }
    }
}
