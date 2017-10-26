package ms.domwillia.jvmemory.modify;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;

public class TestVisitor extends ClassVisitor {

	private String currentClass;

	public TestVisitor(ClassWriter writer) {
		super(Opcodes.ASM6, writer);
		currentClass = null;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		currentClass = name;
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public void visitEnd() {
		currentClass = null;
		super.visitEnd();
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		InstrAdapter instr = new InstrAdapter(mv, name.equals("<init>"));
		LocalVariablesSorter localVarSorter = new LocalVariablesSorter(access, desc, instr);
		instr.localVars = localVarSorter;
		System.out.printf("visiting %s : %s\n", currentClass, name);
		return localVarSorter;
	}
}
