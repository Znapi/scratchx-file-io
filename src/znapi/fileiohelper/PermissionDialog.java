package znapi.fileiohelper;

import java.awt.Button;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

@SuppressWarnings("serial")
public final class PermissionDialog extends JDialog implements ActionListener {

	private Button allowButton;

	private boolean isAllowed;

	public static boolean askPermission(JFrame frame, String dir, boolean isAskingRead) {
		PermissionDialog d = new PermissionDialog(frame, dir, isAskingRead);
		try {
			synchronized(d) {
				d.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			d.dispose();
			return false;
		}
		d.dispose();
		return d.isAllowed;
	}

	private PermissionDialog(JFrame frame, String dir, boolean isAskingRead) {
		super(frame);
		if(isAskingRead)
			this.setTitle("New Read Permission");
		else
			this.setTitle("New Write Permission");

		this.setAutoRequestFocus(true);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		allowButton = new Button("Allow");
		Button denyButton = new Button("Don't allow");
		allowButton.addActionListener(this);
		denyButton.addActionListener(this);

		JTextArea message = new JTextArea();
		message.setEditable(false);
		message.setBackground(frame.getBackground());
		if(isAskingRead)
			message.setText("The Scratch project wants to read the file " + dir);
		else
			message.setText("The Scratch project wants to write the file " + dir);

		JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
		buttonPanel.add(allowButton);
		buttonPanel.add(denyButton);
		JPanel p = new JPanel(new GridLayout(2, 1));
		p.add(message);
		p.add(buttonPanel);

		this.setContentPane(p);
		this.pack();
		this.setVisible(true);
		this.requestFocus();
	}

	// called when the ok button is pressed
	public void actionPerformed(ActionEvent e) {
		this.setVisible(false);

		if(e.getSource() == allowButton)
			isAllowed = true;
		else
			isAllowed = false;

		synchronized(this) {
			this.notifyAll();
		}
	}

}
