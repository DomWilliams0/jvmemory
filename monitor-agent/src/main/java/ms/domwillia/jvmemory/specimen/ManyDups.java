package ms.domwillia.jvmemory.specimen;

public class ManyDups implements Specimen{
	private int i;
	private long l;
	private double d;

	@Override
	public void go() {
		// putfield [int, object] - top 2 of stack must be duped, easy with dup2
		this.i = 40;

		// putfield [long1, long2, object] - top 3 of stack must be duped
		this.l = 40;

		// putfield [double1, double1, object] - top 3 of stack must be duped
		this.d = 98.2323;

		// TODO lastore [long1, long2, index, array] - top 4 of stack must be duped
	}
}
