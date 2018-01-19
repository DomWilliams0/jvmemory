package ms.domwillia.specimen;

import java.util.ArrayList;
import java.util.List;

public class Threads implements Specimen {

	private static void wasteMemory() {
		for (int i = 0; i < 1000; i++) {
			new GarbageCollection.WasteOfSpace(String.valueOf(i));
		}
	}

	@Override
	public void go() {
		int count = 20;
		List<Thread> ts = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			ts.add(new Thread(Threads::wasteMemory));
		}

		for (Thread t : ts)
			t.start();

		for (int i = 0; i < ts.size(); i++) {
			Thread t = ts.get(i);
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
