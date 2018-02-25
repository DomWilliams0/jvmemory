package ms.domwillia.specimen;

public class Threads implements Specimen {

	private static void allocate() {
		for (int i = 0; i < 2; i++) {
			long tid = Thread.currentThread().getId();
			switch (Math.toIntExact(tid % 3)) {
				case 0:
					new Enums();
					break;
				case 1:
					new FieldManipulation();
					break;
				case 2:
					new ArrayLists();
					break;
			}

		}
	}

	@Override
	public void go() {
		int count = 3;
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
