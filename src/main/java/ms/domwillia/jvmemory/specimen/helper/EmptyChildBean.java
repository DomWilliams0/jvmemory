package ms.domwillia.jvmemory.specimen.helper;

public class EmptyChildBean extends EmptyBean {
	public EmptyChildBean() {
		System.out.println("EmptyChildBean.EmptyChildBean: " + getClass().getName());
	}

	public static class InnerBean extends EmptyChildBean {

		public InnerBean() {

			System.out.println("InnerBean.InnerBean: " + getClass().getName());
		}
	}
}
