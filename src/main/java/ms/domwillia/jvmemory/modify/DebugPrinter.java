package ms.domwillia.jvmemory.modify;

public class DebugPrinter {

	public static void iloadPrint(int var) {
		System.out.println("=== iload " + var);
	}

	public static void istorePrint(int var) {
		System.out.println("=== istore " + var);
	}
}
