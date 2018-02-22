package ms.domwillia.specimen;

import ms.domwillia.jvmemory.monitor.Monitor;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Arrays;

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

		ArrayList<Object> list = new ArrayList<>(2);
		list.add(new Object());
		list.add(new Object());
		list.add(new Object());
//
//		System.out.println("---------- set");
//		System.out.flush();
//		HashSet<Object> set = new HashSet<>(1);
//		set.add(new Object());
//		set.add(new Object());
//
//		System.out.println("---------- map");
//		System.out.flush();
		LinkedHashMap<Object, Object> map = new LinkedHashMap<>(1);
		map.put(new Object(), new Object());
	}
}
