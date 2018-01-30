package ms.domwillia.specimen;

public class Arrays implements Specimen {

	@Override
	public void go() {
		Arrays[] arr = new Arrays[10];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = new Arrays();
		}
	}

}
