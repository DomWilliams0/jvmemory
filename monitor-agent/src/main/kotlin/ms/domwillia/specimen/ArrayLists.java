package ms.domwillia.specimen;

import ms.domwillia.jvmemory.monitor.Monitor;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class ArrayLists implements Specimen {

	@Override
	public void go() {
		int[] arr = new int[20];
		int[] other = Arrays.copyOf(arr, 20);
		System.out.printf("%d -> %d\n", Monitor.getTag(arr), Monitor.getTag(other));

		String[] s = (String[]) Array.newInstance(String.class, 2);
		System.out.printf("single dim %d\n", Monitor.getTag(s));

		String[][][] m = (String[][][]) Array.newInstance(String.class, 2, 2, 2);
		System.out.printf("multi dim %d\n", Monitor.getTag(m));

		ArrayList<Object> list = new ArrayList<>(1);
		list.add(new Object());
		list.add(new Object());
//		int x = list.size();

	}
}
