package ms.domwillia.jvmemory.modify;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

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

		if (className.startsWith("ms/domwillia")) {
			ClassReader reader = new ClassReader(classfileBuffer);
			ClassWriter writer = new ClassWriter(reader, 0);
			TestVisitor printer = new TestVisitor(writer);
			reader.accept(printer, 0);
			return writer.toByteArray();
		}
		return null;
	}
}
