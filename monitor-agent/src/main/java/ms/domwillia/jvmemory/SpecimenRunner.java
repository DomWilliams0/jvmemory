package ms.domwillia.jvmemory;

import ms.domwillia.jvmemory.specimen.AllocationsTracking;

public class SpecimenRunner {

	public static void main(String[] args) {
		new AllocationsTracking().go();
	}
}
