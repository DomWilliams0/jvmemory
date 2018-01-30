package ms.domwillia.specimen;

import ms.domwillia.jvmemory.monitor.Monitor;

import java.lang.reflect.Array;

public class Arrays implements Specimen {

	@Override
	public void go() {
		int[][] arrint = new int[10][20];
		double[][][] arrdouble = new double[3][4][2];
		Object[][][][] arrobj = new Object[5][2][3][3];

		printArray(arrint, "ints");
		printArray(arrdouble, "doubles");
		printArray(arrobj, "objs");
	}

	static void printArray(Object array, String prefix) {
		System.out.printf("%s = %d\n", prefix, Monitor.getTag(array));
		if (!array.getClass().getComponentType().isArray()) return;

		int length = Array.getLength(array);
		for (int i = 0; i < length; i++) {
			Object o = Array.get(array, i);
			String newPrefix = prefix + "[" + i + "]";
			printArray(o, newPrefix);
		}
	}

}
