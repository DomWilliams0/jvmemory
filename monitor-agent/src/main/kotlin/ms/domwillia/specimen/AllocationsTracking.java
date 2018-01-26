package ms.domwillia.specimen;

public class AllocationsTracking implements Specimen {

	IntLink a;

	@Override
	public void go() {
		IntLink firstInt;

		IntLink linkInt = firstInt = new IntLink(0);
		for (int i = 0; i < 5; i++) {
			IntLink x = new IntLink(i);
			linkInt.next = x;
			linkInt = x;
		}


		a = firstInt;

		System.out.println(a);
	}

	public class IntLink {
		int value;
		IntLink next;

		public IntLink(int value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return "IntLink{" +
					"value=" + value +
					", next=" + next +
					'}';
		}
	}


}
