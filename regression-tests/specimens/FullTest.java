package specimens;

public class FullTest {

	// fields with different visibilities
	private int smallPrimitive;
	private long largePrimitive;
	protected Object object;
	public String constant;
	Object nil;

	// statics
	static int smallPrimitiveStatic;
	static long largePrimitiveStatic;
	static Object objectStatic;
	static String constantStatic;
	static Object nilStatic;


	static {
		smallPrimitiveStatic = 5;
		largePrimitiveStatic = 5000;
		objectStatic = new Object();
		constantStatic = "hello";
		nilStatic = null;
	}

	// putfield
	public FullTest() {
		this.smallPrimitive = 2;
		this.largePrimitive = 2000;
		this.object = new Object();
		this.constant = "alright";
		this.nil = null;
	}

	static void aStaticMethod() {

	}

	void recursive(int x) {
		if (x > 0)
			recursive(x - 1);
	}

	void testMethodCalls() {
        recursive(5);
	}

	// getfield, store and load 0 (this)
	void testGetField() {
		int i = this.smallPrimitive;
		long l = this.largePrimitive;
		Object o = this.object;
		String s = this.constant;
		Object n = this.nil;
	}

	// store/load
	void testLocalVars() {
		int smallPrimA = 5;
		int smallPrimB = smallPrimA;
		long doublePrimA = 2L;
		long doublePrimB = doublePrimA;
		Object objA = new Object();
		Object objB = objA;
		String constA = "gday";
		String constB = constA;
		Object nilA = null;
		Object nilB = nilA;
	}

	// load from/store in single/multidim arrays
	void testArrays() {
		Object[] objSingle = new Object[2];
		int[] primSingle = new int[2];
		Object[][] objMulti = new Object[2][2];
		int[][][] primMulti = new int[2][2][2];

		Object o = objSingle[0];
		objSingle[1] = null;

		int i = primSingle[0];
		primSingle[1] = 1;

		o = objMulti[0][0];
		objMulti[1][1] = null;

		i = primMulti[0][0][0];
		primMulti[1][1][1] = 1;

		int size = objSingle.length + primSingle.length + objMulti.length + primMulti.length;
	}

	// alloc obj and array
	void testAllocations() {
		new Object();
		new Integer(50);

		Object[] objSingle = new Object[2];
		int[] primSingle = new int[2];
		Object[][] objMulti = new Object[2][2];
		int[][][] primMulti = new int[2][2][2];
	}

	// deallocations by garbage collector
	void testDeallocations() {
		final int arrLen = 1024 * 1024; // 1mb
		final int arrSize = arrLen * 8; // at least long size
		int n = 2;
		while (n-- > 0) {
			long[] arr = new long[arrLen];
		}
	}

	public static void main(String[] args) {
		FullTest test = new FullTest();
		test.testMethodCalls();
		test.testGetField();
		test.testLocalVars();
		test.testArrays();
		test.testAllocations();
		test.testDeallocations();
	}
}
