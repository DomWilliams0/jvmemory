package ms.domwillia.jvmemory.specimen.helper;

public class EmptyChildBean extends EmptyBean {
	public EmptyChildBean() {
	}

	public static class InnerBean extends EmptyChildBean {
		@Override
		protected void finalize() throws Throwable {
			System.out.println("finalize");
		}
	}
}
