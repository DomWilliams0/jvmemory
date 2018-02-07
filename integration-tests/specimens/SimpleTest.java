package specimens;

class SimpleTest {

	private static int anInt;
	private String aString;
	public Object anObject;

	private void a() {

	}

	private int b(int x) {
		a();
		return x;
	}

	private String c(String a, Long b, int[][] c) {
		b(10);
		return null;
	}

	public static void d() {

	}

	public static void main(String[] args) {
		d();
		new SimpleTest().c(null, 20L, null);
	}
}
