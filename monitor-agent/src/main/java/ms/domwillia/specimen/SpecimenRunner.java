package ms.domwillia.specimen;

public class SpecimenRunner {

	public static void main(String[] args) {
		new AllocationsTracking().go();
		System.out.println("Exiting cleanly");
	}
}
