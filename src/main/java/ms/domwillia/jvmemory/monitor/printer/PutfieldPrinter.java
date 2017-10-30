package ms.domwillia.jvmemory.monitor.printer;

import ms.domwillia.jvmemory.monitor.StackTracker;

@SuppressWarnings("unused")
public class PutfieldPrinter {
	private static void putField(String clazz, Object obj, String field, String type, Object val) {
		System.out.printf("%s > putfield %s %s->%s on object %s = %s\n", StackTracker.getHead(), type, clazz, field, obj.hashCode(), val);
	}

	public static void booleanDo(Object obj, boolean val, String clazz, String field) {
		putField(clazz, obj, field, "boolean", val);
	}

	public static void charDo(Object obj, char val, String clazz, String field) {
		putField(clazz, obj, field, "char", val);
	}

	public static void byteDo(Object obj, byte val, String clazz, String field) {
		putField(clazz, obj, field, "byte", val);
	}

	public static void shortDo(Object obj, short val, String clazz, String field) {
		putField(clazz, obj, field, "short", val);
	}

	public static void intDo(Object obj, int val, String clazz, String field) {
		putField(clazz, obj, field, "int", val);
	}

	public static void floatDo(Object obj, float val, String clazz, String field) {
		putField(clazz, obj, field, "float", val);
	}

	public static void longDo(Object obj, long val, String clazz, String field) {
		putField(clazz, obj, field, "long", val);
	}

	public static void doubleDo(Object obj, double val, String clazz, String field) {
		putField(clazz, obj, field, "double", val);
	}

	public static void objectDo(Object obj, Object val, String clazz, String field) {
		putField(clazz, obj, field, "object", val);
	}
}
