import java.util.Arrays;

public class SimpleJava {

	public static void main(String[] args) {
		System.out.println("Hello World!");

		int[] array = new int[16];

		for (int i = 0; i < array.length; i++) {
			array[i] = i;
		}

		while (true) {
			for (int i = 0; i < array.length; i++) {
				array[i] += 1;
			}

			System.out.println("array = " + Arrays.toString(array));

			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
