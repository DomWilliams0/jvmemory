package ms.domwillia.jvmemory.monitor.printer;

import ms.domwillia.jvmemory.monitor.StackTracker;

@SuppressWarnings("unused")
public class GetfieldPrinter {
	private static void getField(String clazz, Object obj, String field, String type) {
		System.out.printf("%s > getfield %s %s->%s on object %s\n", StackTracker.getHead(), type, clazz, field, obj.hashCode());
	}

	public static void booleanDo(Object obj, String clazz, String field) {
		getField(clazz, obj, field, "boolean");
	}

	public static void charDo(Object obj, String clazz, String field) {
		getField(clazz, obj, field, "char");
	}

	public static void byteDo(Object obj, String clazz, String field) {
		getField(clazz, obj, field, "byte");
	}

	public static void shortDo(Object obj, String clazz, String field) {
		getField(clazz, obj, field, "short");
	}

	public static void intDo(Object obj, String clazz, String field) {
		getField(clazz, obj, field, "int");
	}

	public static void floatDo(Object obj, String clazz, String field) {
		getField(clazz, obj, field, "float");
	}

	public static void longDo(Object obj, String clazz, String field) {
		getField(clazz, obj, field, "long");
	}

	public static void doubleDo(Object obj, String clazz, String field) {
		getField(clazz, obj, field, "double");
	}

	public static void objectDo(Object obj, String clazz, String field) {
		getField(clazz, obj, field, "object");
	}
}
