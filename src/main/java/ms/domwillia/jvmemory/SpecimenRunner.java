package ms.domwillia.jvmemory;

import ms.domwillia.jvmemory.specimen.FieldManipulation;
import ms.domwillia.jvmemory.specimen.LocalVars;

public class SpecimenRunner {

	public static void main(String[] args) {
		new LocalVars().go();
		new FieldManipulation().go();

	}
}
