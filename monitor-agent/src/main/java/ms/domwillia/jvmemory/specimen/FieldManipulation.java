package ms.domwillia.jvmemory.specimen;

public class FieldManipulation implements Specimen {

	static int staticI = 0;
	static double staticD = 0;
	static String staticS = "hiya";

	int i = 0;
	double d = 0;
	String s = "hiya";

	@Override
	public void go() {
		int a = i;
		double b = d;
		String c = s;

		i = 10;
		d = 40.0;
		s = "hiya";

//		int e = staticI;
//		double f = staticD;
//		String g = staticS;
//
//		staticI = 10;
//		staticD = 40.0;
//		staticS = "hiya";
	}
}
