
// This is 3  Phase Commit Protocol Coordinator  Program

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Coordinator extends JFrame {
	Thread thread = new Thread(new ListenServer());// thread instance of a
													// thread class
	String local_log = "INIT";// declare a variable which keeps track of a
								// current state of a participant
	int voteCount = 0;// declare a variable which keeps track of a voted participants
	int voteAbort = 0;// declare a variable which keeps track of a aborted participants
	int voteCommit = 0;// declare a variable which keeps track of commited participants
	int ackCommit = 0;// declare a variable which keeps track of participants c
	Boolean participantTimeout = false;
	Boolean globalCommitFlag = false;// declare a variable which indicates coordinator has voted global_commit
	Boolean isCoordinatorVote = false;// declare a variable which indicates coordinator has voted
	Boolean voteRequest = false;// declare a variable which indicates coordinator has sent arb string
	Timer timer, timer1;
	static JTextArea textarea = new JTextArea(5, 20);// Create the textarea for message display
	static JTextArea state = new JTextArea(3, 10); // state textarea displays the state of a client as a Local_log
	String username = "Coordinator";// declare a string username to save a name of a current user
	Boolean isConnected = false;// to check whether the user is connected or not
	/* create instances for socket,bufferreader,printwriter and date */
	Socket socket;
	BufferedReader in;
	PrintWriter out;
	JFrame frame = new JFrame("Coordinator");

	/*
	 * user-agent displays name of the browsers,this list shows this application
	 * is independent of a browser.
	 */
	String useragent = " User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36";
	String host = " Host:localhost";// get host name
	String globalAbort = "GLOBAL_ABORT";// get length of the GLOBAL_ABORT
	String globalCommit = "GLOBAL_COMMIT";// get length of the GLOBAL_COMMIT
	String PrepareCommit = "GLOBAL_COMMIT";// get length of the GLOBAL_COMMIT
	String ContentLengthGlobalAbort = " Content-length:" + globalAbort.length();
	String ContentLengthGlobalCommit = " Content-length:" + globalCommit.length();
	String ContentLengthPrepareCommit = " Content-length:" + PrepareCommit.length();
	String contentType = " Conent-Type:text/plain";

	/*
	 * To print current date in the HTTP date format explicitly adding the time
	 * zone to the formatter
	 */
	Instant i = Instant.now();
	String dateFormatted = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC).format(i);
	String currentDate = "Date:" + dateFormatted;
	/*
	 * append useragent host contentType contentLength and date to a single
	 * string
	 */
	String httpmsgGlobalAbort = useragent + host + ContentLengthGlobalAbort + contentType + currentDate;
	String httpmsgGlobalCommit = useragent + host + ContentLengthGlobalCommit + contentType + currentDate;
	String httpmsgPrepareCommit = useragent + host + ContentLengthPrepareCommit + contentType + currentDate;
	String httpmsgVoteRequest = useragent + host + ContentLengthGlobalCommit + contentType + currentDate;

	/* read data from the servers */
	public class ListenServer implements Runnable {
		@Override
		public void run() {
			/*
			 * line is a string array to take input line and usern is a variable
			 * to store name of a client who has sent the message
			 */
			String[] line;
			String msg = "";
			try {
				while ((msg = in.readLine()) != null) {
					line = msg.split(":");// split the line with :
					String usern = line[1];// get username

					/*
					 * if the coordinator or participant crashes then change
					 * local_log as LOCAL_ABORT
					 */
					if (line[11].equals("Crashed")) {
						local_log = "LOCAL_ABORT";
						state.setText("LOCAL_LOG:LOCAL_ABORT");
						textarea.append(usern + " - LOCAL_ABORT\n");

					}
					/*
					 * if the last element is Abort then count numberr of aborts
					 * and number of votes
					 */
					else if (line[11].equals("Abort")) {
						voteAbort++;// indicates a participant voted abort
						voteCount++;// indicates a participant voted
						textarea.append(usern + " - LOCAL_ABORT\n ");
					}
					/*
					 * if the last element is Precommit then count number of
					 * commits and number of votes
					 */
					else if (line[11].equals("PreCommit")) {
						voteCommit++;// indicates a participant voted commit
						voteCount++;// indicates a participant voted
						textarea.append(usern + " - PRE_COMMIT\n");
					}
					/*
					 * if the last element is ack to commit then count number of
					 * participants ackd if all 3 participant votes ack then
					 * send GLOBAL_COMMIT to all participants
					 */
					else if (line[11].equals("AckCommit")) {
						ackCommit++;// indicates a participant voted for
									// precommit
						textarea.append(usern + " - ACK_COMMIT\n");
						if (ackCommit >= 3) {
							JOptionPane.showMessageDialog(textarea, "Ready to GLOBAL_COMMIT");
							state.setText("LOCAL_LOG:GLOBAL_COMMIT");
							textarea.append("GLOBAL_COMMIT\n");
							String gc = "POST:" + username + ": GLOBAL_COMMIT:" + httpmsgGlobalCommit
									+ ":CoordinatorCommit";
							sendToStream(gc);
						}
					}

				} // while
			} catch (Exception e) {
				e.printStackTrace();
			}
		}// run
	}

	// create the GUI and display it
	private void createGUI() {
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JButton vote = new JButton("Vote");// create vote button to give global
											// vote
		JTextField inputfield = new JTextField("Enter Arb. string here");
		inputfield.setEditable(true);// set textfield as editable
		JScrollPane scrollPane = new JScrollPane(textarea);
		textarea.setEditable(false);// set textareas as not editable
		state.setEditable(false);
		// put created components in the frame.
		frame.getContentPane().add(vote, BorderLayout.PAGE_END);
		frame.getContentPane().add(state, BorderLayout.LINE_START);

		state.append("LOCAL_LOG:\n");
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		frame.getContentPane().add(inputfield, BorderLayout.PAGE_START);
		frame.add(scrollPane);
		/*
		 * add listener to frame when frame opens start the coordinator window
		 */
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent we) {
				try {
					socket = new Socket("localhost", 7879);
					InputStreamReader streamreader = new InputStreamReader(socket.getInputStream());
					in = new BufferedReader(streamreader);
					out = new PrintWriter(socket.getOutputStream());
					out.println("POST:" + username + ": has connected.:::::::::Connect");
					out.flush();
				} // try
				catch (Exception e) {
					e.printStackTrace();
				}
				thread.start();
				textarea.append("Coordinator started...");
				state.setText("LOCAL_LOG:INIT");
			}
		});

		inputfield.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent action) {
				try {

					String voteReq = "POST:" + username + ":  " + inputfield.getText() + " - VOTE_REQUEST" + ":"
							+ httpmsgVoteRequest + ":VoteRequest";
					// set voteRequest to true indicating coordinator has send a
					// vote request
					voteRequest = true;
					voteCount = 0;
					voteAbort = 0;
					voteCommit = 0;
					ackCommit = 0;
					out.println(voteReq);

					/*
					 * when coordinator sends a voterequest then start a timer
					 * then create a timertask which calls participantTimeout
					 * method coordinator waits for a participant to be voted
					 * for 10 seconds if any participant does not vote in 10
					 * seconds then coordinator sends global_abort to all
					 * participants
					 */
					timer = new Timer();
					TimerTask rt = new patricipantTimeOut();
					timer.schedule(rt, 10 * 1000);
					textarea.append("Sent Vote request....\n");
					out.flush(); // flushes the buffer
				} catch (Exception e) {
					e.printStackTrace();
				}
				/*
				 * After sending message to output stream clear the textfield
				 * and set focus to inputfield to take another messages input
				 */
				inputfield.setText("");
				inputfield.requestFocus();

			}
		});

		vote.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent action) {
				isCoordinatorVote = true;
				String abort = "POST:" + username + ": - GLOBAL_ABORT:" + httpmsgGlobalAbort + ":CoordinatorAbort";
				String abortTimeout = "POST:" + username + ": - GLOBAL_ABORT:" + httpmsgGlobalAbort + ":Timeout";
				String commit = "POST:" + username + ": - PRE_COMMIT:" + httpmsgPrepareCommit + ":CoordinatorPreCommit";
				/*
				 * if all participants voted it means count is 3,then check
				 * whether any participant voted abort if voted then send
				 * global_abort otherwise check if all 3 participants voted
				 * commit if so then send ack_commit to all participants
				 */
				String cvote = "";
				if (voteCount >= 3) {
					if (voteAbort > 0) {
						cvote = abort;
						local_log = "GLOBAL_ABORT";
						state.setText("LOCAL_LOG:GLOBAL_ABORT");
						textarea.append(" GLOBAL_ABORT\n ");
					} else {
						cvote = commit;
						local_log = "PRE_COMMIT";
						state.setText("LOCAL_LOG:PREPARE_COMMIT");
						textarea.append("\nsent ACK request\n");
						/*
						 * send ack commit request to all participants if any
						 * participant does not send ack then after 20 seconds
						 * it considers one of the participant crashed and sends
						 * global commit to all other participants
						 */
						timer1 = new Timer();
						TimerTask rt = new patricipantPrepareTimeOut();
						timer1.schedule(rt, 20 * 1000);
					}
				}
				/*
				 * if voteCount is less than 3 means all participants have not
				 * voted and one of the participant timed out then send
				 * global_abort to all participants
				 */
				else if (voteCount < 3) {
					cvote = abortTimeout;
					local_log = "GLOBAL_ABORT";
					state.setText("LOCAL_LOG:GLOBAL_ABORT");
					textarea.append("One of the participant not voted.....\n");
				}
				sendToStream(cvote);
			}
		});
		/*
		 * if the coordinator crashes then it sets the local_log to abort and
		 * sends the message to all other participants
		 */
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				String coordinatorCrash = "POST:" + username + ": Coordinator Crashed:" + httpmsgGlobalAbort
						+ ":Crashed";
				sendToStream(coordinatorCrash);
			}

			public void windowClosed(WindowEvent we) {
				setVisible(false);
			}
		});
		frame.setVisible(true);// setting frame as visible
		frame.setSize(600, 600);
		frame.setResizable(false);
	}

	public static void main(String args[]) {
		Coordinator c = new Coordinator();// create a new instance of the client
											// class
		c.createGUI();// call createGUI() method
	}

	/*
	 * when coordinator sends a voterequest then start a timer then create a
	 * timertask which calls participantTimeout method coordinator waits for a
	 * participant to be voted for 10 seconds if participant does not vote in 10
	 * seconds then coordinator sends globalo_abort to all participants
	 */
	public class patricipantTimeOut extends TimerTask {
		public void run() {
			String gAbort = "POST:" + username + ": - GLOBAL_ABORT:" + httpmsgGlobalAbort + ":TimeoutAbort";
			if (voteCount < 3)// checks voteCount is 3 or not,if its less than 3
								// it means all participant have not voted.
			{
				textarea.append("One of the participant not voted in 10 seconds...\n");
				participantTimeout = true; // set participantTimeout to true
				local_log = "GLOBAL_ABORT";
				state.setText("LOCAL_LOG:GLOBAL_ABORT");
				sendToStream(gAbort);
				timer.cancel();
			} // if

		}// run
	}

	/*
	 * send ack commit request to all participants if any participant does not
	 * send ack then after 20 seconds it considers one of the participant
	 * crashed and sends global commit to all other participants
	 */
	public class patricipantPrepareTimeOut extends TimerTask {
		public void run() {
			String gcommit = "POST:" + username + ": GLOBAL_COMMIT:" + httpmsgGlobalCommit + ":CoordinatorCommit";
			if (ackCommit < 3)// if its less than 3 it means all participant
								// have not voted.
			{
				textarea.append("One of the participant not acknowledged in 20 seconds...");
				state.setText("LOCAL_LOG:GLOBAL_COMMIT");
				textarea.append("GLOBAL_COMMIT\n");
				local_log = "GLOBAL_COMMIT";
				sendToStream(gcommit);
				timer.cancel();
			} // if
		}// run
	}

	/* this method just sends a incoming string to output stream */
	public void sendToStream(String s) {
		String sout = s;
		try {
			out.println(sout);
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
