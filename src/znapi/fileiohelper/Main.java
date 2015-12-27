package znapi.fileiohelper;

import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import znapi.fileiohelper.TableModel;
import znapi.fileiohelper.NewEntryDialog;

public final class Main extends NanoHTTPD implements TableModelListener {

	private String rootDir;
	private boolean hasConsole;
	private static final int port = 8080;

	private HashMap<String, Boolean[]> permissions;

	private TableModel tableModel;

	private JFrame frame;
	private JTable table;
	private JTextArea textArea;
	private PlainDocument document;

	private static final String usageMsg = "Usage:\n\tjava -jar simple-file-io-helper-app.jar [-gui] /root/directory/\n\to  -gui is an optional argument. It causes the app to run with\n\t    a GUI rather in console mode.";

	public static void main(String[] args) {
		String rootDir = null;
		boolean useGui;

		if(args.length > 2) {
			System.out.println("Invalid arguments. There are too many.\n" + usageMsg);
		}
		if(System.console() != null) {
			useGui = false;
			if(args.length == 0) {
				System.out.println("You must specify a root directory.\nExample:\n\tsimple-file-io-helper-app /directory/you/want/as/root/\n"+usageMsg);
				return;
			}
		}
		else
			useGui = true;

		for(String arg : args) {
			if(arg.equalsIgnoreCase("-gui"))
				useGui = true;
			else
				rootDir = arg;
		}

		if(System.console() != null && rootDir == null) {
			System.out.println("You must specify a root directory.\nExample:\n\tsimple-file-io-helper-app /directory/you/want/as/root/\n"+usageMsg);
			return;
		}

		new Main(rootDir, !useGui);
	}

	public void log(String s) {
		if(hasConsole)
			System.out.println(s);
		else {
			try {
				document.insertString(document.getLength(), s + '\n', null);
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
	}

	public Main(String rootDir, boolean useConsole) {
		super(port);
		this.hasConsole = hasConsole;
		this.permissions = new HashMap<String, Boolean[]>();
		if(hasConsole)
			this.rootDir = rootDir;
		else {
			frame = new JFrame("ScratchX File I/O Helper App");

			// create space to write log messages to
			document = new PlainDocument();
			textArea = new JTextArea(document);
			textArea.setFont(Font.decode(Font.MONOSPACED));
			textArea.setEditable(false);

			// create table of file permissions
			tableModel = new TableModel();
			tableModel.addTableModelListener(this);
			table = new JTable(tableModel);
			table.setFillsViewportHeight(true);
			JScrollPane tableScrollPane = new JScrollPane(table);

			// put log space and table in panel
			GridLayout grid = new GridLayout(2, 1);
			JPanel panel = new JPanel(grid);
			panel.add(tableScrollPane);
			panel.add(textArea);

			// make window/frame
			frame.setContentPane(panel);
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(500, 600);
			frame.setVisible(true);

			if(rootDir == null) {
				log("Please select a directory to use as root");
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setDialogTitle("Select a directory to use as root");
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				
				switch(fileChooser.showOpenDialog(frame)) {
				case JFileChooser.APPROVE_OPTION:
					this.rootDir = fileChooser.getSelectedFile().getAbsolutePath();
					break;
				case JFileChooser.CANCEL_OPTION:
					log("No directory selected.\nStopped. You may close the window to exit.");
					return;
				}

			}
			else
				this.rootDir = rootDir;
		}

		log("Starting server with root at `" + this.rootDir + "` on port " + port + "\n");
		try {
			this.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
		} catch (IOException ioe) {
			log("Couldn't start server:\n" + ioe + "\nStopped. You may close the window to exit");
			return;
		}

		if(hasConsole) {
			System.out.println("Server started. Hit Enter to stop.\n");

			try {
				System.in.read();
			} catch (Throwable ignored) {
			}

			this.stop();
			System.out.println("Stopped.\n");
		}
		else
			log("Server started. Close the window to exit.");
	}

	/*public Main(String rootDir) {
		super(port);
		this.rootDir = rootDir;
		permissions = new HashMap<String, Boolean[]>();

		frame = new JFrame("ScratchX File Helper App");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		JPanel panel = new JPanel(new GridLayout(1, 0));

		tableModel = new TableModel();
		tableModel.addTableModelListener(this);
		table = new JTable(tableModel);
        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		table.setFillsViewportHeight(true);

		JScrollPane scrollPane = new JScrollPane(table);
		panel.add(scrollPane);

		frame.setContentPane(panel);
		frame.pack();
		frame.setVisible(true);

		//addPermission("/todo.txt", true, false);

		ServerRunner.executeInstance(this);
	}*/

	public Boolean[] checkPermissions(String dir) {
		if(!permissions.containsKey(dir)) {
			NewEntryDialog d = new NewEntryDialog(frame, dir);
			try {
				log("checking permission");
				synchronized(d) {
					d.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			addPermission(dir, d.canReadBox.getState(), d.canWriteBox.getState());
			d.dispose();
		}
		return permissions.get(dir);
	}

	public Status createFile(File f) {
		log("Creating file: " + f.getName());
		try {
			f.getParentFile().mkdirs();
			f.createNewFile();
			return Status.OK;
		} catch(SecurityException e) {
			log("Denied by security manager");
			return Status.UNAUTHORIZED;
		} catch(FileNotFoundException e) {
			log("ERROR: FILE DOES NOT EXIST"); // the file was just created
			return Status.INTERNAL_ERROR;
		} catch (IOException e) {
			log("I/O Error");
			return Status.INTERNAL_ERROR;
		}
	}

	public Status writeFile(File f, String contents) {
		log("Writing file: " + f.getName());
		try {
			PrintWriter writer = new PrintWriter(f);
			writer.print(contents);
			writer.close();
			return Status.OK;
		} catch (SecurityException e) {
			log("Denied by security manager");
			return Status.UNAUTHORIZED;
		} catch (FileNotFoundException e) {
			log("ERROR: FILE DOES NOT EXIST"); // the caller ensures that the file exists
			return Status.INTERNAL_ERROR;
		}
	}

	@Override
	public Response serve(IHTTPSession session) {
		Response r = newFixedLengthResponse(null);
		r.addHeader("Access-Control-Allow-Origin", "*");
		r.addHeader("Access-Control-Expose-Headers", "X-Is-ScratchX-File-IO-Helper-App");
		r.addHeader("X-Is-ScratchX-File-IO-Helper-App", "yes");

		File f;
		String uri = session.getUri();
		switch(session.getMethod()) {

		case OPTIONS:
			r.setStatus(Response.Status.OK);
			r.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
			break;

		// send the contents of the specified file to the Scratch extension
		case GET:
			log("GET " + uri);
			f = new File(rootDir + uri);
			if(f.exists()) {
				log("FILE EXISTS");
				if(!checkPermissions(uri)[0]) {
					log("No read permission");
					r.setStatus(Status.UNAUTHORIZED);
				}
				else {
					try { // this could not be abstracted into another method because no pass by reference and only one return type
						log("Reading file: " + uri);
						FileInputStream s = new FileInputStream(f);

						r.setStatus(Status.OK);
						r.setData(s);
					} catch (FileNotFoundException e) {
						log("File not found");
						r.setStatus(Status.NOT_FOUND);
					}
				}
			}
			else {
				if(!checkPermissions(uri)[1]) {
					log("No write permission");
					r.setStatus(Status.UNAUTHORIZED);
				}
				else {
					r.setStatus(createFile(f));
				}
			}
			break;

		// create/empty the specified file
		case POST:
			log("POST " + uri);
			f = new File(rootDir+uri);
			if(!checkPermissions(uri)[1]) {
				log("No write permission");
				r.setStatus(Status.UNAUTHORIZED);
			}
			else { 
				if(f.exists())
					r.setStatus(writeFile(f, ""));
				else
					r.setStatus(createFile(f));
			}
			break;

		// set contents of file
		case PUT:
			log("PUT " + session.getUri());
			f = new File(rootDir+session.getUri());
			if(!checkPermissions(uri)[1]) {
				log("No write permission");
				r.setStatus(Status.UNAUTHORIZED);
			}
			else {
				if(!f.exists()) {
					r.setStatus(createFile(f));
					if(r.getStatus() != Status.OK) break;
				}
				int contentLength = Integer.parseInt(session.getHeaders().get("content-length"));
				byte[] buffer = new byte[contentLength];
				try {
					session.getInputStream().read(buffer, 0, contentLength);
					r.setStatus(writeFile(f, new String(buffer)));
				} catch (IOException e) {
					log("I/O error");
					r.setStatus(Status.INTERNAL_ERROR);
				}
			}
			break;

		default:
			r.setStatus(Response.Status.METHOD_NOT_ALLOWED);
			break;

		}
		return r;
	}

	// changes the hashmap in event that the user changed the table
	public void tableChanged(TableModelEvent e) {
		int row = e.getFirstRow();
		int column = e.getColumn();
		String dir = (String)table.getValueAt(row, 0);

		Boolean[] p = permissions.get(dir);
		p[column-1] = (Boolean)table.getValueAt(row, column);
		permissions.replace(dir, p); 
	}

	// adds to both the hashmap and the table
	public void addPermission(String dir, boolean allowRead, boolean allowWrite) {
		permissions.put(dir, new Boolean[]{allowRead, allowWrite});
		tableModel.addRow(dir, allowRead, allowWrite);
	}

}
