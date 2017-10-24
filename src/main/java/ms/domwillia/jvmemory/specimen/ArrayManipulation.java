package ms.domwillia.jvmemory.specimen;

import java.util.Arrays;

public class ArrayManipulation implements Specimen {

	private int[] array;

	public ArrayManipulation() {
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

	@Override
	public void go() {
		while (true) {
			this.ping();

			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
