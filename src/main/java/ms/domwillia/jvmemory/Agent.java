package ms.domwillia.jvmemory;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class Agent implements ClassFileTransformer {
	public static void premain(String agentArgs, Instrumentation inst) {
		System.out.println("agentArgs = " + agentArgs);
		inst.addTransformer(new Agent());
	}

	public byte[] transform(final ClassLoader loader, String className,
	                        final Class classBeingRedefined, final ProtectionDomain protectionDomain,
	                        final byte[] classfileBuffer) throws IllegalClassFormatException {

		System.out.println("className = " + className);
		return null;
	}
}
