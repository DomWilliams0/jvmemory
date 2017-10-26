package ms.domwillia.jvmemory;

import ms.domwillia.jvmemory.specimen.ManyDups;
import ms.domwillia.jvmemory.specimen.Specimen;

public class SpecimenRunner {

	public static void main(String[] args) {
		Specimen specimen;

		specimen = new ManyDups();

		specimen.go();

	}
}
