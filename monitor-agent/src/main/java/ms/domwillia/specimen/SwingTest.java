package ms.domwillia.specimen;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class SwingTest extends JFrame implements Specimen {

	public SwingTest() {

		JPanel panel = new JPanel();
		getContentPane().add(panel);

		JButton hiya = new JButton("Hiya");
		hiya.addActionListener(new ActionListener() {
			int count = 0;

			@Override
			public void actionPerformed(ActionEvent event) {
				System.out.printf("hiya %d\n", count++);
			}
		});

		panel.add(hiya);

		setSize(200, 200);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
	}

	@Override
	public void go() {
	}
}
