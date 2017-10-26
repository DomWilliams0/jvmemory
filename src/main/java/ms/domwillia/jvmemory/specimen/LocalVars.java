package ms.domwillia.jvmemory.specimen;

public class LocalVars implements Specimen {

	private Bean createStevo() {
		return new Bean("Stevo", 16);
	}

	@Override
	public void go() {
		Bean a = new Bean("Bob", 40);
		Bean b = createStevo();

		System.out.println("a = " + a);
		System.out.println("b = " + b);

		int c = 25;
		byte d = 6;
		char e = 's';
		double k = 5.5;
		float f = 0.25f;
		long l = 40000;
		short s = 2000;
		boolean g = true;

	}

	class Bean {
		String name;
		int age;

		Bean(String name, int age) {
			this.name = name;
			this.age = age;
		}

		@Override
		public String toString() {
			return "Bean{" +
					"name='" + name + '\'' +
					", age=" + age +
					'}';
		}
	}
}
