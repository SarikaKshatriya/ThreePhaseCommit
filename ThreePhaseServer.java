
// This is a server program for 3 phase commit
/*import all the required packages*/
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

//create a server to access chat messages 
public class ThreePhaseServer extends JFrame {
	ArrayList clients;// create a arraylist to maintain number of clients for
						// output stream
	ArrayList<String> username; // create a arraylist for String data to
								// maintain name of the users
	private JTextArea textarea = new JTextArea(5, 20);// Create the textarea to
														// display all the input
														// and output messages
	Socket socket; // initialize socket variable to create socket stream
	JFrame frame = new JFrame("Three Phase Commit ");// Create the frame
														
	/*
	 * this is a class to handle communication among multiple clients one
	 * instance of this thread will run for each client
	 */
	public class multiUsers implements Runnable {
		BufferedReader in;// create variables for input stream
		PrintWriter out;// create variables for output stream
		/* get reference to the sockets input and output stream */

		public multiUsers(Socket s, PrintWriter w) throws Exception {
			out = w;
			socket = s;
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		}

		// override run method
		@Override
		public void run() {
			/*
			 * line is a string array to take input line and currentuser is a
			 * variable to store name of a client who has sent the message
			 */
			String[] line;
			String currentUser;
			try {
				String msg = "";
				/*
				 * check for the whole message line and split it with : the
				 * first element is username the second is message all the
				 * messages are splitted with : read the inputstream
				 */
				while ((msg = in.readLine()) != null) {
					line = msg.split(":");// split the line with :
					currentUser = line[1];// get the name of current client who
											// sent the message
					/*
					 * if the incoming message last field is Connect then add a
					 * username to the arraylist and display username as a
					 * connected
					 */
					if (line[11].equals("Connect")) {
						textarea.append(currentUser + " Connected.\n");
						username.add(currentUser);// add the current users name
													// to arraylist username
					}
					/*
					 * if it's VoteRequest means coordinator sent a request with
					 * a arbitary string then send msg to all participants
					 */
					else if (line[11].equals("VoteRequest")) {
						textarea.append("\n" + msg + "\n");
						String doGET = msg.replace("POST", "GET");
						sendMsgParticipant(doGET);// send message to all
													// participants
					}
					/*
					 * if it's Abort means participant aborted then send msg to
					 * Coordinator
					 */
					else if (line[11].equals("Abort")) {
						textarea.append("\n" + msg + "\n");
						String doGET = msg.replace("POST", "GET");
						sendMsgCoordinator(doGET);// send message to coordinator
					}
					/*
					 * if it's commit means participant aborted then send msg to
					 * Coordinator
					 */
					else if (line[11].equals("PreCommit")) {
						textarea.append("\n" + msg + "\n");
						String doGET = msg.replace("POST", "GET");
						sendMsgCoordinator(doGET);// send message to coordinator
					}
					/*
					 * if coordinator sent a acknowledgment request then send
					 * msg to all participants
					 */
					else if (line[11].equals("CoordinatorPreCommit")) {
						textarea.append("\n" + msg + "\n");
						String doGET = msg.replace("POST", "GET");
						sendMsgParticipant(doGET);// send message to all
													// participants
					}
					/*
					 * if it's ack to commit means participant ackd for commit
					 * then send msg to Coordinator
					 */
					else if (line[11].equals("AckCommit")) {
						textarea.append("\n" + msg + "\n");
						String doGET = msg.replace("POST", "GET");
						sendMsgCoordinator(doGET);// send message to coordinator
					}
					/*
					 * if coordinator sent a global commit then send msg to all
					 * participants
					 */
					else if (line[11].equals("CoordinatorCommit")) {
						textarea.append("\n" + msg + "\n");
						String doGET = msg.replace("POST", "GET");
						sendMsgParticipant(doGET);// send message to all
													// participants
					}
					/*
					 * if coordinator crashes/we close the coordinator window
					 * then send msg to all participants
					 */
					else if (line[11].equals("Crashed")) {
						textarea.append("\n" + msg + "\n");
						String doGET = msg.replace("POST", "GET");
						sendMsgParticipant(doGET);// send message to all
													// participants
					}
					/*
					 * if coordinator sent global abort then send msg to all
					 * participants
					 */
					else if (line[11].equals("CoordinatorAbort")) {
						textarea.append("\n" + msg + "\n");
						String doGET = msg.replace("POST", "GET");
						sendMsgParticipant(doGET);// send message to all
													// participants
					}
					/*
					 * if coordinator does not send any vote request in 20
					 * seconds then participant aborts locally.server just print
					 * that message
					 */
					else if (line[11].equals("voteRequestTimeout")) {
						textarea.append("\n" + msg + "\n");
					}
					/*
					 * if coordinator times out then notify that to all
					 * participants
					 */
					else if (line[11].equals("TimeoutAbort")) {
						textarea.append("\n" + msg + "\n");
						String doGET = msg.replace("POST", "GET");
						sendMsgParticipant(doGET);// send message to all
													// participants
					}
					/*
					 * if coordinator doesn't vote in 20 seconds then
					 * participants who commited asks other participants about
					 * there state
					 */
					else if (line[11].equals("DECISION_REQUEST")) {
						textarea.append("\n" + msg + "\n");
						String doGET = msg.replace("POST", "GET");
						sendMsgParticipant(doGET);// send message to all
													// participants
					}

				} // while
			} // try
			catch (Exception e) {
				System.out.println(e);
			}
		} // run
	}// multiuser

	// create the GUI and display it
	private void createGUI() {
		JScrollPane scrollPane = new JScrollPane(textarea);
		textarea.setEditable(false);// set both textarea's as not editable
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		frame.add(scrollPane);// add scrollpane to the frame
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		/*
		 * add listener to frame when frame opens start the server window
		 */
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent we) {
				Thread thread = new Thread(new multiThread());
				thread.start();// start the thread
				textarea.append("Server started...\n");
			}
		});
		frame.setVisible(true);// set frame as visible
		frame.setSize(600, 600); // set frame size
		frame.setResizable(false);// make the size not resizable to avoid
									// inconsistency among all the frames
	}// createGUI()
	// this class is to allow multithreading

	public class multiThread extends Thread {
		@Override
		public void run() {
			clients = new ArrayList();
			username = new ArrayList();
			try {
				ServerSocket serversocket = new ServerSocket(7879);
				while (true) {
					Socket clientSock = serversocket.accept(); // Listening for
																// a connection
																// request.
					PrintWriter pw = new PrintWriter(clientSock.getOutputStream());
					clients.add(pw);// add the client output stream
					Thread thread = new Thread(new multiUsers(clientSock, pw));
					thread.start();// start the thread
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// broadcast message to all participants
	public void sendMsgParticipant(String message) {
		textarea.append("\n" + message);
		Iterator it = clients.iterator();// index just before the first element
											// in arraylist
		for (int k = 1; k < 4; k++) {
			try {
				PrintWriter output = (PrintWriter) clients.get(k);
				output.println(message);
				output.flush();// flush the outputstream
				textarea.setCaretPosition(textarea.getDocument().getLength());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// broadcast message to Coordinator,first client is always coordinator
	public void sendMsgCoordinator(String message) {
		textarea.append("\n" + message);
		try {
			PrintWriter output = (PrintWriter) clients.get(0);
			output.println(message);
			output.flush();// flush the outputstream
			textarea.setCaretPosition(textarea.getDocument().getLength());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) {
		ThreePhaseServer s = new ThreePhaseServer();// create a new instance of
													// the server class
		s.createGUI();// call createGUI() method
	}
}
