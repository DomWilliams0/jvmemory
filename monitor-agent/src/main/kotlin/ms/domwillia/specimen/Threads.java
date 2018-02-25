package ms.domwillia.specimen;

public class Threads implements Specimen {

	private static void allocate() {
		for (int i = 0; i < 2000; i++) {
			new GarbageCollection.WasteOfSpace("hiy");

		}
	}

	@Override
	public void go() {
		int count = 1;
		for (int i = 0; i < count; i++) {
			// TODO avoid invokeDynamic!
			new Thread(new Runnable() {
				@Override
				public void run() {
					allocate();
				}
			}).start();
		}

	}
}
