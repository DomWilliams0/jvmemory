package ms.domwillia.specimen;

import java.util.ArrayList;

public class ArrayLists implements Specimen {

	@Override
	public void go() {
		ArrayList<Object> list = new ArrayList<>();
		list.add(new Object());
		list.add(new Object());
		list.add(new Object());
		list.add(new Object());
		list.add(new Object());
		int x = list.size();
	}
}
