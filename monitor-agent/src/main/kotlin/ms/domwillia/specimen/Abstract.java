package ms.domwillia.specimen;

public class Abstract implements Specimen {

	@Override
	public void go() {
		AbstractClass c = new Implementor();
		Implementor i = new Implementor();

		c.doSomething();
		i.doSomething();

		Splinterface x = new Splimplementor();
		Splimplementor y = new Splimplementor();
		x.splinter();
		y.splinter();

	}
}

abstract class AbstractClass {
	Object[] o = new Object[2];

	void doSomething() {
		o[0] = new Object();
	}
}

class Implementor extends AbstractClass {

	@Override
	void doSomething() {
		super.doSomething();
		o[1] = new Object();
	}
}

interface Splinterface {
	void splinter();
}

class Splimplementor implements Splinterface {

	@Override
	public void splinter() {
		int[] ints = new int[20];
	}
}
