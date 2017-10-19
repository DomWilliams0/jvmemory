package ms.domwillia.jvmemory;

import java.util.Arrays;

public class SimpleJava {

	private int[] array;

	SimpleJava() {
		array = new int[16];
		for (int i = 0; i < array.length; i++) {
			array[i] = i;
		}
	}

	private void ping() {
		for (int i = 0; i < array.length; i++) {
			array[i] += 1;
		}

		System.out.println("array = " + Arrays.toString(array));
	}

	public static void main(String[] args) {
		SimpleJava simple = new SimpleJava();

		while (true) {
			simple.ping();

			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
