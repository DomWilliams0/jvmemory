package ms.domwillia.specimen;

import java.util.ArrayList;

public class Threads implements Specimen {

	private static void doThings() {
		ArrayList<Object> list = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			list.add(new Object());
		}
	}

	@Override
	public void go() {
		int count = 10;
		for (int i = 0; i < count; i++) {
			// TODO avoid invokeDynamic!
			new Thread(new Runnable() {
				@Override
				public void run() {
					doThings();
				}
			}).start();
		}

	}
}
