package ms.domwillia.specimen;

public class Enums implements Specimen {

	public enum AnEnum {
		A, B, C
	}

	@Override
	public void go() {
		AnEnum e = AnEnum.A;
	}
}
