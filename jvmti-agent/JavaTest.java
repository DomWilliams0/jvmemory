public class JavaTest {

	static native void tagMe(Object o);

	int i;
	String s;
	double[] waste;

	JavaTest(int i, String s) {
		this.i = i;
		this.s = s;
		this.waste = new double[1024*1024];
	}

	public static void main(String[] args) {
		//JavaTest a = new JavaTest(1, "hello");
		//JavaTest b = new JavaTest(2, "hello");
		//b.s = "there";

		for (int i = 0; i < 500; i++) {
			JavaTest x = new JavaTest(5, "hiya");
			tagMe(x);
		}
	}
}
