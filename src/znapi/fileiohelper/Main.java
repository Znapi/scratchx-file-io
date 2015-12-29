package znapi.fileiohelper;

import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public final class Main extends NanoHTTPD implements TableModelListener {

	private String rootDir;
	private boolean hasConsole;
	private static final int port = 8080;

	public Hashtable<String, Boolean> readPermissions;
	public Hashtable<String, Boolean> writePermissions;

	private PermissionsTableModel tableModel;

	private JFrame frame;
	private JTable table;
	private JTextArea textArea;
	private PlainDocument document;

	private static final String usageMsg = "Usage:\n\tjava -jar simple-file-io-helper-app.jar [-gui] /root/directory\n\to  -gui is an optional argument. It causes the app to run with\n\t    a GUI rather in console mode.";

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
		this.hasConsole = useConsole;
		readPermissions = new Hashtable<String, Boolean>();
		writePermissions = new Hashtable<String, Boolean>();

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
			tableModel = new PermissionsTableModel();
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

	public void checkRead(String dir) {
		if(readPermissions.containsKey(dir)) {
			if(!readPermissions.get(dir))
				throw new SecurityException();
		}
		else {
			boolean allow = PermissionDialog.askPermission(frame, dir, true);
			readPermissions.put(dir, allow);
			tableModel.addReadPermission(dir, allow);
			if(!allow)
				throw new SecurityException();
		}
	}

	public void checkWrite(String dir) {
		if(writePermissions.containsKey(dir)) {
			if(!writePermissions.get(dir))
				throw new SecurityException();
		}
		else {
			boolean allow = PermissionDialog.askPermission(frame, dir, false);
			writePermissions.put(dir, allow);
			tableModel.addWritePermission(dir, allow);
			if(!allow)
				throw new SecurityException();
		}
	}

	public Status createFile(File f, String dir) {
		log("Creating file: " + f.getName());
		try {
			checkWrite(dir);
			f.getParentFile().mkdirs();
			f.createNewFile();
			return Status.OK;
		} catch(SecurityException e) {
			log("Unauthorized");
			return Status.UNAUTHORIZED;
		} catch(FileNotFoundException e) {
			log("ERROR: FILE DOES NOT EXIST"); // the file was just created
			return Status.INTERNAL_ERROR;
		} catch (IOException e) {
			log("I/O Error");
			return Status.INTERNAL_ERROR;
		}
	}

	public Status writeFile(File f, String dir, String contents) {
		log("Writing file: " + f.getName());
		try {
			checkWrite(dir);
			PrintWriter writer = new PrintWriter(f);
			writer.print(contents);
			writer.close();
			return Status.OK;
		} catch (SecurityException e) {
			log("Unauthorized");
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
				try { // this could not be abstracted into another method because no pass by reference and only one return type
					checkRead(uri);
					log("Reading file: " + uri);
					FileInputStream s = new FileInputStream(f);
					r.setStatus(Status.OK);
					r.setData(s);
				} catch(SecurityException e) {
					log("Unauthorized");
					r.setStatus(Status.UNAUTHORIZED);
				} catch (FileNotFoundException e) {
					log("File not found");
					r.setStatus(Status.NOT_FOUND);
				}
			}
			else
				r.setStatus(createFile(f, uri));
			break;

		// create/empty the specified file
		case POST:
			log("POST " + uri);
			f = new File(rootDir+uri);
			if(f.exists())
				r.setStatus(writeFile(f, uri, ""));
			else
				r.setStatus(createFile(f, uri));
			break;

		// set contents of file
		case PUT:
			log("PUT " + session.getUri());
			f = new File(rootDir+session.getUri());
			if(!f.exists()) {
				r.setStatus(createFile(f, uri));
				if(r.getStatus() != Status.OK) break;
			}
			int contentLength = Integer.parseInt(session.getHeaders().get("content-length"));
			byte[] buffer = new byte[contentLength];
			try {
				session.getInputStream().read(buffer, 0, contentLength);
				r.setStatus(writeFile(f, uri, new String(buffer)));
			} catch (IOException e) {
				log("I/O error");
				r.setStatus(Status.INTERNAL_ERROR);
			}
			break;

		default:
			r.setStatus(Response.Status.METHOD_NOT_ALLOWED);
			break;

		}
		return r;
	}

	public JFrame getFrame() {
		return frame;
	}

	// changes a hashtable in event that the user changed the table
	public void tableChanged(TableModelEvent e) {
		int r = e.getFirstRow();
		boolean isReadPerm = e.getColumn() == 1;

		String dir = (String)table.getValueAt(r, 0);
		boolean newValue;
		if(isReadPerm) {
			newValue = (boolean)table.getValueAt(r, 1);
			readPermissions.replace(dir, newValue);
		}
		else {
			newValue = (boolean)table.getValueAt(r, 2);
			writePermissions.replace(dir, newValue);
		}
	}

}
