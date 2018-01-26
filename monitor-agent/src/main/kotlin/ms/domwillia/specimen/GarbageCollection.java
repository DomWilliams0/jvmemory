package ms.domwillia.specimen;

public class GarbageCollection implements Specimen {

	@Override
	public void go() {

		for (int i = 0; i < 10000; i++) {
			new WasteOfSpace(String.valueOf(i));
		}
		System.out.println("waste of spaces done");
	}

	public static class WasteOfSpace {
		double[] doubles = new double[1024];
		String s;
		Empty e = new Empty();


		public WasteOfSpace(String s) {
			this.s = s;
		}
	}
	private static class Empty {

	}
}
