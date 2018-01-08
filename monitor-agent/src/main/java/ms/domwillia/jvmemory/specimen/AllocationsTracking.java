package ms.domwillia.jvmemory.specimen;

public class AllocationsTracking implements Specimen {

	@Override
	public void go() {
		Baggage baggageA = new Baggage("rather heavy", 100);
		Baggage baggageB = new Baggage("quite light", 20);

		TestBean bean = new TestBean(20, 5.5, baggageA);
		bean.baggage = baggageB;
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
