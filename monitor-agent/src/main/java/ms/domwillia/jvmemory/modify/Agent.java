package ms.domwillia.jvmemory.modify;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class Agent implements ClassFileTransformer {
	public static void premain(String agentArgs, Instrumentation inst) {
		inst.addTransformer(new Agent());
	}

	public byte[] transform(final ClassLoader loader, String className,
	                        final Class classBeingRedefined, final ProtectionDomain protectionDomain,
	                        final byte[] classfileBuffer) throws IllegalClassFormatException {

		if (className.startsWith("ms/domwillia/jvmemory/specimen")) {
			ClassReader reader = new ClassReader(classfileBuffer);
			ClassWriter writer = new ClassWriter(reader, 0);
			PatchingClassVisitor visitor = new PatchingClassVisitor(writer);
			try {
				reader.accept(visitor, ClassReader.EXPAND_FRAMES);
			} catch(RuntimeException e) {
				e.printStackTrace();
			}

			byte[] rewritten = writer.toByteArray();
			File f = new File("/tmp/modified_" + className.replace('/', '.') + ".class");
			try {
				FileOutputStream x = new FileOutputStream(f);
				x.write(rewritten);
				x.close();
				System.out.println("Wrote modified class to " + f.getAbsolutePath());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return rewritten;
		}
		return null;
	}
}
