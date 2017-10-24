package ms.domwillia.jvmemory.modify;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TestMethodVisitor extends MethodVisitor {
	private final String name;

	public TestMethodVisitor(MethodVisitor mv, String name) {
		super(Opcodes.ASM4, mv);
		this.name = name;
	}

	@Override
	public void visitCode() {
		System.out.printf("visit %s\n", name);
		super.visitCode();
	}

	@Override
	public void visitEnd() {
		System.out.println("done");
		super.visitEnd();
	}

	@Override
	public void visitInsn(int opcode) {
		System.out.println("opcode = " + opcode);
		super.visitInsn(opcode);
	}
}
