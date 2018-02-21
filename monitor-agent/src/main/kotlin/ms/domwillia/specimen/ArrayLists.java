package ms.domwillia.specimen;

import ms.domwillia.jvmemory.monitor.Monitor;

import java.util.Arrays;

public class ArrayLists implements Specimen {

	@Override
	public void go() {
		int[] arr = new int[20];
		int[] other = Arrays.copyOf(arr, 20);
		System.out.printf("%d -> %d\n", Monitor.getTag(arr), Monitor.getTag(other));
	}
}
