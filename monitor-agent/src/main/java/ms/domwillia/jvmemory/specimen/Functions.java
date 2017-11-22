package ms.domwillia.jvmemory.specimen;

import javax.swing.*;


public class Functions extends JFrame implements Specimen {

	void repeatMe() {
	}
	void callMe() {
	}

	void nestMe() {
		repeatMe();
		callMe();
	}

	void imEven() {
	}
	void imOdd() {
	}


	@Override
	public void go() {

		for (int i = 0; i < 5; i++) {
			repeatMe();
		}

		nestMe();

		for (int i = 0; i < 6; i++) {
			if (i % 2 == 0)
				imEven();
			else
				imOdd();
		}

	}
}


