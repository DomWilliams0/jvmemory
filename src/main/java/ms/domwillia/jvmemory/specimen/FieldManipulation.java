package ms.domwillia.jvmemory.specimen;

public class FieldManipulation implements Specimen {

	static int staticI = 0;
	int i = 0;
	String s = "hiya";

	@Override
	public void go() {
		this.s = "potato";
		String x = s;

		staticI = 40;
		int s = staticI;
	}
}
