package ms.domwillia.jvmemory;

public class ProtoBufTester {

	public static void main(String[] args) {
		MemoryAccess.Test.newBuilder()
				.setMessage("hiya")
				.build();
	}
}
