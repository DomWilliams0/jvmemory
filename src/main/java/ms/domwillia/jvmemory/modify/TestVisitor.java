package ms.domwillia.jvmemory.modify;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TestVisitor extends ClassVisitor {

	public TestVisitor(ClassWriter writer) {
		super(Opcodes.ASM4, writer);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		return new TestMethodVisitor(mv, name);
	}


}
