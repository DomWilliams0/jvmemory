package ms.domwillia.jvmemory.modify;

import ms.domwillia.jvmemory.monitor.LocalVarTracker;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;

public class PatchingClassVisitor extends ClassVisitor {

	private String currentClass;

	public PatchingClassVisitor(ClassWriter writer) {
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
		LocalVarTracker.debugPrint();
		LocalVarTracker.clear();
		super.visitEnd();
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		InstructionPatcher instr = new InstructionPatcher(mv, currentClass, name);
		LocalVariablesSorter localVarSorter = new LocalVariablesSorter(access, desc, instr);
		instr.localVars = localVarSorter;
		return localVarSorter;
	}
}
