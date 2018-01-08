package ms.domwillia.specimen;

import ms.domwillia.specimen.helper.EmptyChildBean;

public class ObjectLifecycle implements Specimen {

	@Override
	public void go() {

		for (int i = 0; i < 20; i++) {
			new EmptyChildBean.InnerBean();
		}
	}
}
