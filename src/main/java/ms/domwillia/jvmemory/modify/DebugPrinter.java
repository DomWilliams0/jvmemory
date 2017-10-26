package ms.domwillia.jvmemory.modify;

public class DebugPrinter {

	public static void loadPrint(String type, int var) {
		System.out.printf("%s > load %s local var %d\n", StackTracker.getHead(), type, var);
	}

	public static void storePrint(String type, Object val, int var) {
		System.out.printf("%s > store %s \"%s\" in local var %d\n", StackTracker.getHead(), type, val.toString(), var);
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

	// load
	public static void boolloadPrint(int var) {
		loadPrint("bool", var);
	}

	public static void charloadPrint(int var) {
		loadPrint("char", var);
	}

	public static void byteloadPrint(int var) {
		loadPrint("byte", var);
	}

	public static void shortloadPrint(int var) {
		loadPrint("short", var);
	}

	public static void intloadPrint(int var) {
		loadPrint("int", var);
	}

	public static void floatloadPrint(int var) {
		loadPrint("float", var);
	}

	public static void longloadPrint(int var) {
		loadPrint("long", var);
	}

	public static void doubleloadPrint(int var) {
		loadPrint("double", var);
	}

	public static void objectloadPrint(int var) {
		loadPrint("object", var);
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
