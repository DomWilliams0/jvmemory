package ms.domwillia.jvmemory.modify;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class TestInstructionVisitor extends InstructionAdapter {
	private final String name;

	public TestInstructionVisitor(MethodVisitor mv, String name) {
		super(Opcodes.ASM6, mv);
		this.name = name;
		System.out.printf("visiting %s\n", name);
	}

	@Override
	public void visitEnd() {
		System.out.println("--------");
		super.visitEnd();
	}

	@Override
	public void store(int var, Type type) {
		System.out.printf("store %d - %s\n", var, type);
		super.store(var, type);
	}

	@Override
	public void load(int var, Type type) {
		System.out.printf("load %d - %s\n", var, type);
		super.load(var, type);
	}

	@Override
	public void anew(Type type) {
		System.out.printf("new %s\n", type);
		super.anew(type);
	}

	@Override
	public void astore(Type type) {

		// TODO String.format to print 2 previous items on stack?
		// theres got to be a better way

		// load format string ldc
		// iconst2, anewarray (2 size), dup

		// dup2 to dupe original args
		// iconst0 aastore,
		// dup, iconst1, aastore
//		super.visitMethodInsn(INVOKESTATIC, "java/lang/String", "format", Type.getObjectType("String").getDescriptor(), );


		// println
		super.visitFieldInsn(GETSTATIC, "java/lang/System", "out",
				"Ljava/io/PrintStream;");
		super.visitLdcInsn("wahey, astore");
		super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

		super.astore(type);
	}

	@Override
	public void aload(Type type) {
		System.out.printf("aload %s\n", type);
		super.aload(type);
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		System.out.println("maxStack = " + maxStack);
		System.out.println("maxLocals = " + maxLocals);
		super.visitMaxs(maxStack + 2, maxLocals);
	}
}
