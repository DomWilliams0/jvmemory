package ms.domwillia.jvmemory;

import ms.domwillia.jvmemory.specimen.ArrayManipulation;
import ms.domwillia.jvmemory.specimen.Specimen;

public class SpecimenRunner {

	public static void main(String[] args) {
		Specimen specimen;

		specimen = new ArrayManipulation();

		specimen.go();

	}
}
