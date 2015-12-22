package znapi.simplefileiohelper;

import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public final class Main extends NanoHTTPD {

	private String rootDir;
	private boolean hasConsole;
	private static final int port = 8080;

	// the following properties are used if not in console mode
	private JFrame frame;
	private JTextArea textArea;
	private PlainDocument document;

	private static final String usageMsg = "Usage:\n\tjava -jar simple-file-io-helper-app.jar [-gui] /root/directory/\n\to  -gui is an optional argument. It causes the app to run with\n\t    a GUI rather in console mode.";

	public static void main(String[] args) {
		// if running from console
		if(System.console() != null) { 
			if(args.length == 0)
				System.out.println("You must specify a root directory.\nExample:\n\tsimple-file-io-helper-app /directory/you/want/as/root/\n"+usageMsg);
			else if(args.length == 1) {
				if(args[0].equalsIgnoreCase("-gui"))
					new Main(null, false);
				else
					new Main(args[0], true);
			}
			else if(args.length == 2) {
				if(args[0].equalsIgnoreCase("-gui"))
					new Main(args[1], false);
				else if(args[1].equalsIgnoreCase("-gui"))
					new Main(args[0], false);
				else
					System.out.println("Invalid argument.\n"+usageMsg);
			}
			else
				System.out.println("Invalid argument. There are too many.\n"+usageMsg);
		}
		else
			new Main(null, false);
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

	public Main(String rootDir, boolean hasConsole) {
		super(port);
		this.hasConsole = hasConsole;
		if(hasConsole)
			this.rootDir = rootDir;
		else {
			frame = new JFrame("ScratchX Simple File I/O Helper App");
			document = new PlainDocument();
			textArea = new JTextArea(document);
			textArea.setFont(Font.decode(Font.MONOSPACED));
			textArea.setEditable(false);
			frame.setContentPane(textArea);
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(500, 300);
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
		Response r = newFixedLengthResponse(null);//new Response(null);
		r.addHeader("Access-Control-Allow-Origin", "*");
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
			if(f.exists())	{
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
			else
				r.setStatus(createFile(f));
			break;

		// create/empty the specified file
		case POST:
			log("POST " + uri);
			f = new File(rootDir+uri);
			if(f.exists())
				r.setStatus(writeFile(f, ""));
			else
				r.setStatus(createFile(f));
			break;

		// set contents of file
		case PUT:
			log("PUT " + session.getUri());
			f = new File(rootDir+session.getUri());
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
			break;

		default:
			r.setStatus(Response.Status.METHOD_NOT_ALLOWED);
			break;

		}
		return r;
	}

}


