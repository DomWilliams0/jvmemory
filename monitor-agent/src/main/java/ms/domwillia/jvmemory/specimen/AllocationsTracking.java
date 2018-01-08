package ms.domwillia.jvmemory.specimen;

public class AllocationsTracking implements Specimen {

	@Override
	public void go() {
		new TestBean(20, 5.5, new Baggage("rather heavy", 100));
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
