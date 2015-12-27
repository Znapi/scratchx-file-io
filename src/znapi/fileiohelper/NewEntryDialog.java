package znapi.fileiohelper;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public final class NewEntryDialog extends JDialog implements ActionListener {

	@SuppressWarnings("unused")
	private String dir;
	public Checkbox canReadBox, canWriteBox;

	public NewEntryDialog(JFrame frame, String dir) {
		super(frame, "New Entry");
		this.dir = dir;

		this.setAutoRequestFocus(true);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		JPanel p = new JPanel(new GridLayout(0, 1));
		canReadBox = new Checkbox("Allow reading", false);
		canWriteBox = new Checkbox("Allow writing", false);
		p.add(canReadBox);
		p.add(canWriteBox);
		Button button = new Button("OK");
		button.addActionListener(this);
		p.add(button);

		this.setContentPane(p);
		this.pack();
		this.setVisible(true);
		this.requestFocus();
	}

	// called when the ok button is pressed
	public void actionPerformed(ActionEvent e) {
		this.setVisible(false);
		synchronized(this) {
			this.notifyAll();
		}
		/*main.addPermission(dir, canReadBox.getState(), canWriteBox.getState());
		this.dispose();*/
	}

}
