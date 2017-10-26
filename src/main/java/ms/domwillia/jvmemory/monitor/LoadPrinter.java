package ms.domwillia.jvmemory.monitor;

public class LoadPrinter {

	private static void loadPrint(String type, int var) {
		System.out.printf("%s > load %s local var %d\n", StackTracker.getHead(), type, var);
	}
	public static void booleanDo(int var) {
		loadPrint("bool", var);
	}

	public static void charDo(int var) {
		loadPrint("char", var);
	}

	public static void byteDo(int var) {
		loadPrint("byte", var);
	}

	public static void shortDo(int var) {
		loadPrint("short", var);
	}

	public static void intDo(int var) {
		loadPrint("int", var);
	}

	public static void floatDo(int var) {
		loadPrint("float", var);
	}

	public static void longDo(int var) {
		loadPrint("long", var);
	}

	public static void doubleDo(int var) {
		loadPrint("double", var);
	}

	public static void objectDo(int var) {
		loadPrint("object", var);
	}
}
