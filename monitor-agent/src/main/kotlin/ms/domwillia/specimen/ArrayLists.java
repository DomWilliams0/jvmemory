package ms.domwillia.specimen;

import ms.domwillia.jvmemory.monitor.Monitor;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class ArrayLists implements Specimen {

	static class Element {
		int ignoreMe;
		Object o = new Object();
		int[] dontIgnoreMe = new int[5];
		String lookAtMe = "hello";
	}

	@Override
	public void go() {
		int[] arr = new int[20];
		int[] other = Arrays.copyOf(arr, 20);
		System.out.printf("%d -> %d\n", Monitor.getTag(arr), Monitor.getTag(other));

		String[] s = (String[]) Array.newInstance(String.class, 2);
		System.out.printf("single dim %d\n", Monitor.getTag(s));

		String[][][] m = (String[][][]) Array.newInstance(String.class, 2, 2, 2);
		System.out.printf("multi dim %d\n", Monitor.getTag(m));

		ArrayList<Element> list = new ArrayList<>(2);
		list.add(new Element());
		list.add(new Element());
		list.add(new Element());

		HashSet<Object> set = new HashSet<>(1);
		set.add(new Object());
		set.add(new Object());

		LinkedHashMap<Object, Object> map = new LinkedHashMap<>(1);
		map.put(new Object(), new Object());
		map.put(new Object(), new Object());
		map.put(new Object(), new Object());
	}
}
