package ms.domwillia.jvmemory.specimen;

public class FullExample implements Specimen {

	private int anInt;
	private double aDouble;
	private String aString;

	private void testVariableAccesses() {
		int intCopy = anInt;
		double doubleCopy = aDouble;

		aString = "stringAssignment";

		int localInt = 20;
		float localFloat = 0.25f;
	}

	private void onOddNumber(int i) {

	}

	private void onEvenNumber(int i) {

	}

	private void recurse(int i) {
		if (i != 0)
			recurse(i - 1);
	}

	private void testFunctionCalls() {
		for (int i = 0; i < 7; i++) {
			if (i % 2 == 0)
				onEvenNumber(i);
			else
				onOddNumber(i);
		}

		recurse(5);
	}

	private void testAbstractClass() {
		Dog d = new Dog();
		d.speak();

		Animal a = d;
	}

	private void testGarbageCollection() {
		for (int i = 0; i < 100; i++) {
			new BigObject();
		}
	}


	@Override
	public void go() {
		testFunctionCalls();
		testVariableAccesses();
		testAbstractClass();
		testGarbageCollection();
	}


	private static abstract class Animal {
		protected final String name;

		Animal(String name) {
			this.name = name;
		}

		public abstract void speak();
	}

	private static class Dog extends Animal {

		protected Dog() {
			super("dog");
		}

		@Override
		public void speak() {
			System.out.println("woof");
		}
	}

	private static class LittleObject {
		double d;
	}
	private static class BigObject {
		private LittleObject[] lotsOfLittleUns;

		BigObject() {
			lotsOfLittleUns = new LittleObject[1];
			for (int i = 0; i < lotsOfLittleUns.length; i++) {
				lotsOfLittleUns[i] = new LittleObject();
			}
		}
	}
}
