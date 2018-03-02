package ms.domwillia.specimen;

public class StaticField implements Specimen {
/*	static Object[] objects = new Object[10];
	static int dontIgnoreMe = 0;
	static String s;

	Object[] localArr = new Object[5];
	String localStr = "evening";
	int localInt = 5;*/

	// TODO static arrays are fetched into local vars first

	static class Element {
		static Object shared = new Object();
		Object own = new Object();
	}

	@Override
	public void go() {

//		Element[] arr = new Element[3];
//		arr[0] = new Element();
//		arr[1] = new Element();
//		arr[2] = new Element();

		new Element();
		new Element();

		for (int i = 0; i < 3; i++) {
			Element.shared = null;
			Element.shared = new Object();
		}

/*
		Object a;
		String b;
		int c;

		// getstatic
		a = objects[0];
		b = s;
		c = dontIgnoreMe;

		a = localArr;
		// getfield
		a = localArr[0];
		b = localStr;
		c = localInt;

		// putstatic
		objects[0] = new Object(); // puts array in local var first!
		s = "hiya";
		dontIgnoreMe = 50;


		// putfield
		localArr[0] = new Object();
		localStr = "woo";
		localInt = 5;*/
	}
}
