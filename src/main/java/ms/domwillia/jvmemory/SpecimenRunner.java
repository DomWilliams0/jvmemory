package ms.domwillia.jvmemory;

import ms.domwillia.jvmemory.specimen.LocalVars;
import ms.domwillia.jvmemory.specimen.Specimen;

public class SpecimenRunner {

	public static void main(String[] args) {
		Specimen specimen;

		specimen = new LocalVars();

		specimen.go();

	}
}
