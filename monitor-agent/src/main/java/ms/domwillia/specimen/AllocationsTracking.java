package ms.domwillia.specimen;

import java.util.ArrayList;
import java.util.List;

public class AllocationsTracking implements Specimen {

	@Override
	public void go() {
		Baggage baggageA = new Baggage("rather heavy", 100);
		Baggage baggageB = new Baggage("quite light", 20);

		TestBean bean = new TestBean(20, 5.5, baggageA);
		Baggage tmp = bean.baggage;
		bean.baggage = baggageB;

		List<Baggage> baggages = new ArrayList<>();
		baggages.add(new Baggage("first", 5));
		baggages.add(new Baggage("second", 10));

		for (Baggage b : baggages) {
			bean.baggage = b;
		}
		bean.baggage = baggageA;
	}

	private class TestBean {
		int anInt;
		double aDouble;
		Baggage baggage;

		public TestBean(int anInt, double aDouble, Baggage baggage) {
			this.anInt = anInt;
			this.aDouble = aDouble;
			this.baggage = baggage;
		}
	}

	private class Baggage extends BaggageParent {
		int anInt;

		public Baggage(String name, int anInt) {
			super(name);
			this.anInt = anInt;
		}
	}

	private class BaggageParent {
		String name;

		public BaggageParent(String name) {
			this.name = name;
		}
	}
}
