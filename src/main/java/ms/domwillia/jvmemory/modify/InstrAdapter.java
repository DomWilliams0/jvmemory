package ms.domwillia.jvmemory.modify;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class InstrAdapter extends InstructionAdapter {

	private final boolean isConstructor;
	public LocalVariablesSorter localVars;

	public InstrAdapter(MethodVisitor mv, boolean isConstructor) {
		super(Opcodes.ASM6, mv);
		this.isConstructor = isConstructor;
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
		super.load(var, type);
	}

	@Override
	public void putfield(String owner, String name, String desc) {
		// `this` is uninitialised in constructor
		// TODO find a way to deal with this
		if (isConstructor) {
			super.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			super.visitLdcInsn(String.format("skipping `putfield %s %s` in %s constructor", name, desc, owner));
			super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

		} else {
			String typePrefix = getTypePrefix(Type.getType(desc));
			if (typePrefix != null) {
				String funcName = typePrefix + "getfieldPrint";
				String sig = "(Ljava/lang/Object;" + desc + ")V";

				// TODO if desc is long/double dup3, otherwise dup2
				// TODO dont be a messy savage and extract all this into a separate class
				if (desc.equals("J") || desc.equals("D"))
					dup3(desc.equals("J") ? Type.LONG_TYPE : Type.DOUBLE_TYPE);
				else
					super.dup2();
				super.visitMethodInsn(INVOKESTATIC, "ms/domwillia/jvmemory/modify/DebugPrinter", funcName, sig, false);

			}

		}
		super.putfield(owner, name, desc);
	}

	// top of stack must be a wide type (double or long)
	// TODO similar for dup4
	private void dup3(Type type) {
		// original:   obj l1 l2 <-- HEAD
		// dup2_x1:    l1 l2 obj l1 l2
		// lstore tmp: l1 l2 obj
		// dup_x2:     obj l1 l2 obj
		// lload tmp:  obj l1 l2 obj l1 l2

		int tmp = localVars.newLocal(type);

		super.dup2X1();
		super.store(tmp, type);
		super.dupX2();
		super.load(tmp, type);
	}

	@Override
	public void anew(Type type) {
//		System.out.printf("new %s\n", type);
		super.anew(type);
	}

	@Override
	public void astore(Type type) {
		// TODO astore and aload have 3 params - how to dup3?
		super.astore(type);
	}

	@Override
	public void aload(Type type) {
//		System.out.printf("aload %s\n", type);
		super.aload(type);
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		// TODO dont hardcode, count the added instructions instead
		super.visitMaxs(maxStack + 4, maxLocals);
	}
}
