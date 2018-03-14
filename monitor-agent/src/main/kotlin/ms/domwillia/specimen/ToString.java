package ms.domwillia.specimen;

public class ToString implements Specimen {
	static class Value {
	}

	static class Element {
		static int staticInt = 20;
		Value value = new Value();
		double wide;

		@Override
		public String toString() {
			return "Element{" +
					"value=" + value +
					", wide=" + wide +
					'}';
		}
	}

	@Override
	public void go() {
		Element e = new Element();
		e.wide = 125.0;
		e.value = new Value();
	}

	@Override
	public String toString() {
		return "ToString{}";
	}
}
