package ms.domwillia.specimen;

public class AllocationsTracking implements Specimen {

	@Override
	public void go() {
		Link last = new Link("initial", null);
		for (int i = 1; i <= 5; i++) {
			Link l = new Link(Integer.toString(i), null);
			last.next = l;
			last = l;
		}
	}

	private class Link {
		String name;
		Link next;

		Link(String name, Link next) {
			this.name = name;
			this.next = next;
		}
	}
}
