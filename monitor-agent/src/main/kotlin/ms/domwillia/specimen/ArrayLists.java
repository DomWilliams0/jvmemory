package ms.domwillia.specimen;

import ms.domwillia.jvmemory.monitor.Monitor;

import java.util.ArrayList;
import java.util.LinkedList;

public class ArrayLists implements Specimen {

	@Override
	public void go() {
		ArrayList<Object> list = new ArrayList<>(1);
		list.add(new Object());
		list.add(new Object());
		int x = list.size();
	}
}
