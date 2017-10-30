package ms.domwillia.jvmemory.modify;

import ms.domwillia.jvmemory.monitor.LocalVarTracker;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class InstructionPatcher extends InstructionAdapter {
	private static final String STACK_TRACKER = "ms/domwillia/jvmemory/monitor/StackTracker";
	private static final String VAR_TRACKER = "ms/domwillia/jvmemory/monitor/LocalVarTracker";

	private final String className;
	private final String methodName;
	private final boolean isConstructor;
	private int addedStackSpace;

	LocalVariablesSorter localVars;

	InstructionPatcher(MethodVisitor mv, String className, String methodName) {
		super(Opcodes.ASM6, mv);
		this.className = className;
		this.methodName = methodName;
		this.isConstructor = methodName.equals("<init>");
	}

	@Override
	public void visitCode() {
		addedStackSpace = 0;

		String sig = "(Ljava/lang/String;Ljava/lang/String;)V";
		super.visitLdcInsn(className);
		super.visitLdcInsn(methodName);
		super.visitMethodInsn(INVOKESTATIC, STACK_TRACKER, "push", sig, false);
		super.visitCode();
		addedStackSpace += 2;

		// for visitLocalVariable
		addedStackSpace += 4;
	}

	private String getTypeSpecificHandlerName(Type type) {
		String name;
		switch (type.getSort()) {
			case Type.BOOLEAN:
				name =  "boolean";
				break;
			case Type.CHAR:
				name =  "char";
				break;
			case Type.BYTE:
				name =  "byte";
				break;
			case Type.SHORT:
				name =  "short";
				break;
			case Type.INT:
				name =  "int";
				break;
			case Type.FLOAT:
				name =  "float";
				break;
			case Type.LONG:
				name =  "long";
				break;
			case Type.DOUBLE:
				name =  "double";
				break;
			case Type.OBJECT:
				name =  "object";
				break;
			// TODO arrays just complicate things at this stage
//			case Type.ARRAY:
//				return "array";
			default:
				return null;
		}

		return name + "Do";
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
		String handler = getTypeSpecificHandlerName(type);
		if (handler != null) {
			dupTypeSpecific(type);
			super.iconst(var);
			addedStackSpace += 2;

			String sig = String.format("(%sI)V", type.getDescriptor());
			super.visitMethodInsn(INVOKESTATIC, getPrinterClass("store"), handler, sig, false);
		}
		super.store(var, type);
	}

	@Override
	public void load(int var, Type type) {
		String handler = getTypeSpecificHandlerName(type);
		if (handler != null) {
			super.iconst(var);
			super.visitMethodInsn(INVOKESTATIC, getPrinterClass("load"), handler, "(I)V", false);
			addedStackSpace += 1;
		}
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
			addedStackSpace += 2;

		} else {
			Type type = Type.getType(desc);
			String handler = getTypeSpecificHandlerName(type);
			if (handler != null) {
				// if desc is long/double dup3, otherwise dup2
				// TODO dont be a messy savage and extract all this into a separate class
				if (type.getSize() == 2) {
					dup3(type);
					addedStackSpace += 3;
				} else {
					super.dup2();
					addedStackSpace += 2;
				}

				// push on extra args
				super.visitLdcInsn(owner);
				super.visitLdcInsn(name);
				addedStackSpace += 2;

				String typeDescriptor = type.getSort() == Type.OBJECT ? "Ljava/lang/Object;" : type.getDescriptor();
				String sig = String.format("(Ljava/lang/Object;%sLjava/lang/String;Ljava/lang/String;)V", typeDescriptor);
				super.visitMethodInsn(INVOKESTATIC, getPrinterClass("putfield"), handler, sig, false);
			}

		}
		super.putfield(owner, name, desc);
	}

	@Override
	public void getfield(String owner, String name, String desc) {
		// `this` is uninitialised in constructor
		// TODO find a way to deal with this
		if (isConstructor) {
			super.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			super.visitLdcInsn(String.format("skipping `getfield %s %s` in %s constructor", name, desc, owner));
			super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
			addedStackSpace += 2;

		} else {
			Type type = Type.getType(desc);
			String handler = getTypeSpecificHandlerName(type);
			if (handler != null) {
				super.dup();

				// push on extra args
				super.visitLdcInsn(owner);
				super.visitLdcInsn(name);
				addedStackSpace += 3;

				String sig = "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V";
				super.visitMethodInsn(INVOKESTATIC, getPrinterClass("getfield"), handler, sig, false);
			}

		}
		super.getfield(owner, name, desc);
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
	// track
	private void doReturn() {
		super.visitMethodInsn(INVOKESTATIC, STACK_TRACKER, "pop", "()V", false);
	}

	@Override
	public void ret(int var) {
		doReturn();
		super.ret(var);
	}

	@Override
	public void areturn(Type t) {
		doReturn();
		super.areturn(t);
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		super.visitMaxs(maxStack + addedStackSpace, maxLocals);
	}

	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		LocalVarTracker.registerLocalVar(className, methodName, name, index);
		super.visitLocalVariable(name, desc, signature, start, end, index);
	}

	private static String getPrinterClass(String what) {
		String cap = Character.toUpperCase(what.charAt(0)) + what.substring(1);
		return String.format("ms/domwillia/jvmemory/monitor/%sPrinter", cap);
	}
}
