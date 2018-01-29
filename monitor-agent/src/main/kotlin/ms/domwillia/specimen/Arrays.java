package ms.domwillia.specimen;

public class Arrays implements Specimen {

	void args(int... sizes) {
		for (int size : sizes) {

		}
	}

	@Override
	public void go() {
		int[] a = new int[20];
		double[] d = new double[50];
		String[] s = new String[8];

		int[][] heh = new int[20][10];
		Object[][][] hoh = new Object[1][2][3];
	}

}
