package ms.domwillia.specimen;

public class Arrays implements Specimen {

	@Override
	public void go() {
		int[] a = new int[20];
		double[] d = new double[50];
		Object[] o = new Object[9];

		a[6] = 66;
		d[7] = 7.77;
		o[8] = new Object();

		int aa = a[1];
		double dd = d[2];
		Object oo =o[3];
	}

}
