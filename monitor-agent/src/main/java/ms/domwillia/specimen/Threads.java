package ms.domwillia.specimen;

public class Threads implements Specimen {

	private static void staticFunction() {
		otherStaticFunction();
	}

	private static void otherStaticFunction() {

	}

	@Override
	public void go() {
		Thread[] ts = new Thread[8];
		for (int i = 0; i < ts.length; i++) {
			ts[i] = new Thread(Threads::staticFunction);
		}

		for (Thread t : ts)
			t.start();

		for (Thread t : ts)
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

	}
}
