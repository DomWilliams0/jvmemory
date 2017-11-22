package ms.domwillia.jvmemory.specimen;

import ms.domwillia.jvmemory.specimen.helper.EmptyChildBean;

public class ObjectLifecycle implements Specimen {

	@Override
	public void go() {

		for (int i = 0; i < 20; i++) {
			new EmptyChildBean.InnerBean();
		}
	}
}
