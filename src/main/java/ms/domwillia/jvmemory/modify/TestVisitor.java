package ms.domwillia.jvmemory.modify;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TestVisitor extends ClassVisitor {

	public TestVisitor(ClassWriter writer) {
		super(Opcodes.ASM6, writer);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		System.out.printf("visiting class %s\n", name);
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public void visitEnd() {
		System.out.println("================");
		super.visitEnd();
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		return new TestInstructionVisitor(mv, name);
	}


}
