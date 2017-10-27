package ms.domwillia.jvmemory;

import ms.domwillia.jvmemory.specimen.FieldManipulation;
import ms.domwillia.jvmemory.specimen.Specimen;

public class SpecimenRunner {

	public static void main(String[] args) {
		Specimen specimen;

		specimen = new FieldManipulation();

		specimen.go();

	}
}
