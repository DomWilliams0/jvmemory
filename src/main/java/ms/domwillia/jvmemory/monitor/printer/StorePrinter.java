package ms.domwillia.jvmemory.monitor.printer;

import ms.domwillia.jvmemory.monitor.StackTracker;

public class StorePrinter {

	private static void storePrint(String type, Object val, int var) {
		System.out.printf("%s > store %s \"%s\" in local var %d\n", StackTracker.getHead(), type, val.toString(), var);
	}

	public static void booleanDo(boolean val, int var) {
		storePrint("bool", val, var);
	}

	public static void charDo(char val, int var) {
		storePrint("char", val, var);
	}

	public static void byteDo(byte val, int var) {
		storePrint("byte", val, var);
	}

	public static void shortDo(short val, int var) {
		storePrint("short", val, var);
	}

	public static void intDo(int val, int var) {
		storePrint("int", val, var);
	}

	public static void floatDo(float val, int var) {
		storePrint("float", val, var);
	}

	public static void longDo(long val, int var) {
		storePrint("long", val, var);
	}

	public static void doubleDo(double val, int var) {
		storePrint("double", val, var);
	}

	public static void objectDo(Object val, int var) {
		storePrint("object", val, var);
	}
}
