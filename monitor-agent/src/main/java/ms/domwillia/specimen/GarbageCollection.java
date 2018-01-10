package ms.domwillia.specimen;

public class GarbageCollection implements Specimen {

	@Override
	public void go() {

		for (int i = 0; i < 10000; i++) {
			double[] x = new WasteOfSpace().doubles;

		}
	}

	private static class WasteOfSpace {
		double[] doubles = new double[1024];
	}
}
