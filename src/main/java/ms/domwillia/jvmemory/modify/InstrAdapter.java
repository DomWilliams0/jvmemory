package ms.domwillia.jvmemory.modify;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import static org.objectweb.asm.Opcodes.*;

public class InstrAdapter extends InstructionAdapter {

	public InstrAdapter(MethodVisitor mv) {
		super(Opcodes.ASM6, mv);
	}

	@Override
	public void store(int var, Type type) {
		super.iconst(var);
		super.visitMethodInsn(INVOKESTATIC, "ms/domwillia/jvmemory/modify/DebugPrinter", "istorePrint", "(I)V", false);
		super.store(var, type);
	}

	@Override
	public void load(int var, Type type) {
		super.iconst(var);
		super.visitMethodInsn(INVOKESTATIC, "ms/domwillia/jvmemory/modify/DebugPrinter", "iloadPrint", "(I)V", false);
		super.load(var, type);
	}

	@Override
	public void anew(Type type) {
//		System.out.printf("new %s\n", type);
		super.anew(type);
	}

	@Override
	public void astore(Type type) {
		// TODO astore and aload have 3 params - how to dup3?
		// println
//		super.visitFieldInsn(GETSTATIC, "java/lang/System", "out",
//				"Ljava/io/PrintStream;");
//		super.visitLdcInsn("wahey, astore");
//		super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

		super.astore(type);
	}

	@Override
	public void aload(Type type) {
//		System.out.printf("aload %s\n", type);
		super.aload(type);
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		super.visitMaxs(maxStack + 4, maxLocals);
	}
}
