package ms.domwillia.jvmemory.modify;

public class DebugPrinter {

	public static void storePrint(String type, Object val, int var) {
		System.out.printf("=== store %s \"%s\" in local var %d\n", type, val.toString(), var);
	}

	// store
	public static void boolstorePrint(boolean val, int var) {
		storePrint("bool", val, var);
	}

	public static void charstorePrint(char val, int var) {
		storePrint("char", val, var);
	}

	public static void bytestorePrint(byte val, int var) {
		storePrint("byte", val, var);
	}

	public static void shortstorePrint(short val, int var) {
		storePrint("short", val, var);
	}

	public static void intstorePrint(int val, int var) {
		storePrint("int", val, var);
	}

	public static void floatstorePrint(float val, int var) {
		storePrint("float", val, var);
	}

	public static void longstorePrint(long val, int var) {
		storePrint("long", val, var);
	}

	public static void doublestorePrint(double val, int var) {
		storePrint("double", val, var);
	}

	public static void objectstorePrint(Object val, int var) {
		storePrint("object", val, var);
	}

	// getfield
	public static void longgetfieldPrint(Object obj, long val) {
		System.out.println("DebugPrinter.longgetfieldPrint");
		System.out.println("obj = [" + obj + "], val = [" + val + "]");
	}
	public static void doublegetfieldPrint(Object obj, double val) {
		System.out.println("DebugPrinter.doublegetfieldPrint");
		System.out.println("obj = [" + obj + "], val = [" + val + "]");
		System.out.println("=================");
	}
	public static void intgetfieldPrint(Object obj, int val) {
		System.out.println("DebugPrinter.intgetfieldPrint");
		System.out.println("obj = [" + obj + "], val = [" + val + "]");
	}
}
