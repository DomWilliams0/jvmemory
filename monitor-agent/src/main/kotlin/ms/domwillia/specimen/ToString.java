package ms.domwillia.specimen;

import java.util.ArrayList;

public class ToString implements Specimen {
	static class Value {
		int me;

		public Value(int me) {
			this.me = me;
		}

		@Override
		public String toString() {
			return "Value{" +
					"me=" + me +
					'}';
		}
	}

	static class Element {
		static int staticInt = 20;
		Value value = new Value(0);
		double wide;

		@Override
		public String toString() {
			return "Element{" +
					"value=" + value +
					", wide=" + wide +
					", STATIC=" + staticInt +
					'}';
		}

		void methodWhatDoStuff() {
			value = new Value(999);
			wide = 999.999;
		}
	}

	@Override
	public void go() {
		Element e = new Element();
		e.wide = 125.0;
		e.value = new Value(125);
		Element.staticInt = 500;
		e.methodWhatDoStuff();
	}

	@Override
	public String toString() {
		return "ToString{}";
	}
}
