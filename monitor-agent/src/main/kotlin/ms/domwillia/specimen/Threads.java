package ms.domwillia.specimen;

public class Threads implements Specimen {

	private static void allocate() {
		for (int i = 0; i < 1000; i++) {
			new Object();
		}
	}

	@Override
	public void go() {
		int count = 1;
		for (int i = 0; i < count; i++) {
			new Thread(Threads::allocate).start();
		}

	}
}
