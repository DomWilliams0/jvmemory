package ms.domwillia.jvmemory.modify;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

public class InstrAdapter extends InstructionAdapter {

	public InstrAdapter(MethodVisitor mv) {
		super(Opcodes.ASM6, mv);
	}

	private String getTypePrefix(Type type) {
		switch (type.getSort()) {
			case Type.BOOLEAN:
				return "bool";
			case Type.CHAR:
				return "char";
			case Type.BYTE:
				return "byte";
			case Type.SHORT:
				return "short";
			case Type.INT:
				return "int";
			case Type.FLOAT:
				return "float";
			case Type.LONG:
				return "long";
			case Type.DOUBLE:
				return "double";
			case Type.OBJECT:
				return "object";
			// TODO arrays just complicate things at this stage
//			case Type.ARRAY:
//				return "array";
			default:
				return null;
		}
	}

	// does this depend on architecture/implementation?
	private void dupTypeSpecific(Type type) {
		switch (type.getSort()) {
			case Type.LONG:
			case Type.DOUBLE:
				super.dup2();
				break;
			default:
				super.dup();
				break;
		}
	}

	@Override
	public void store(int var, Type type) {
		String typePrefix = getTypePrefix(type);
		if (typePrefix != null) {
			String funcName = typePrefix + "storePrint";
			String sig = String.format("(%sI)V", type.getDescriptor());

			dupTypeSpecific(type);
			super.iconst(var);
			super.visitMethodInsn(INVOKESTATIC, "ms/domwillia/jvmemory/modify/DebugPrinter", funcName, sig, false);
		}
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
