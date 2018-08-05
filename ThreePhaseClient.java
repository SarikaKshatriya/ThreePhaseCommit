
// This is 3 Phase Commit Protocol participant Program

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ThreePhaseClient extends JFrame {
	Thread thread = new Thread(new ListenServer());// thread instance of a
													// thread class
	String local_log = "";// declare a local_log variable which keeps track of a
							// current state of a participant
	/*
	 * declare names of files for 3 participants which will store a arbitary
	 * string to the file
	 */
	String file1 = "participant1.txt";
	String file2 = "participant2.txt";
	String file3 = "participant3.txt";
	Boolean globalCommitFlag = false;// declare a variable to indicate
										// coordinator send global commit request
	Boolean isCoordinatorVote = false;// declare a variable to indicate
										// coordinator voted
	Boolean isCoordinatorGlobalVote = false;// declare a variable to indicate
											// coordinator voted commit
	Boolean voteRequest = false;// declare a variable to indicate coordinator
								// sent a arb. string
	Timer timer, timer1;
	/*
	 * declare a variable filename to save filename and use it to display
	 * string/file content when a participant connects to server
	 */
	String filename = "participant1.txt";
	static JTextArea textarea = new JTextArea();// declre textarea to display msg
	static JTextArea state = new JTextArea();// declre textarea to display local log
	/*
	 * Create the buttons PREPARE_COMMIT to ack coordinator PREPARE_COMMIT,abort
	 * to vote abort,LOCAL_COMMIT to vote commit
	 */
	JButton ack = new JButton("ACK_COMMIT");
	JButton commit = new JButton("PRE_COMMIT");
	JButton abort = new JButton("Abort");
	String username;// declare a string username to save a name of a current user
	String voteMsg; // declare a variable to save arbString which will save later to 3 files
	Boolean isConnected = false;// to check whether the user is connected or not
	/* create instances for socket,bufferreader,printwriter and date */
	Socket socket;
	BufferedReader in;
	PrintWriter out;
	JFrame frame = new JFrame("Participant");
	// user-agent displays name of the browsers,this list shows this application
	// is independent of a browser.
	String useragent = " User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36";
	String host = " Host:localhost";// get host name
	String localAbort = "LOCAL_ABORT";
	String localCommit = "LOCAL_COMMIT";
	String DecisionRequest = "DECISION_REQUEST";
	String AckCommit = "ACK_COMMIT";// get length of the GLOBAL_COMMIT

	String ContentLengthLocalAbort = " Content-length:" + localAbort.length();
	String ContentLengthLocalCommit = " Content-length:" + localCommit.length();
	String ContentLengthAckCommit = " Content-length:" + AckCommit.length();
	String drl = " Content-length:" + DecisionRequest.length();
	String contentType = " Conent-Type:text/plain";
	/*
	 * To print current date in the HTTP date format explicitly adding the time
	 * zone to the formatter
	 */
	Instant i = Instant.now();
	String dateFormatted = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC).format(i);
	String currentDate = "Date:" + dateFormatted;
	String httpmsgLocalAbort = useragent + host + ContentLengthLocalAbort + contentType + currentDate;
	String httpmsgLocalCommit = useragent + host + ContentLengthLocalCommit + contentType + currentDate;
	String httpmsgDecisionRequest = useragent + host + drl + contentType + currentDate;
	String httpmsgAckCommit = useragent + host + ContentLengthAckCommit + contentType + currentDate;

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
			String[] splitMsg;
			try {
				while ((msg = in.readLine()) != null) // read the input stream
				{

					line = msg.split(":");// split the line with :
					String usern = line[1];// get username
					/*
					 * if the request is timeout,that is coordinator is timeout
					 * then change local_log as GLOBAL_ABORT
					 */
					if (line[11].equals("Timeout")) {
						local_log = "GLOBAL_ABORT";
						state.setText("LOCAL_LOG:GLOBAL_ABORT");
					}
					/*
					 * if the coordinator or participant crashes then change
					 * local_log as LOCAL_ABORT
					 */
					else if (line[11].equals("Crashed")) {
						local_log = "LOCAL_ABORT";
						state.setText("LOCAL_LOG:LOCAL_ABORT");
						textarea.append(usern + " crashed\n");
						abort.setEnabled(false);
						commit.setEnabled(false);
						ack.setEnabled(false);
					}
					/*
					 * if coordinator sends vote request with a arb. string then
					 * save that string to a variable voteMsg
					 */
					else if (line[11].equals("VoteRequest")) {
						abort.setEnabled(true);
						commit.setEnabled(true);
						isCoordinatorVote = false;
						local_log = "INIT";
						isCoordinatorGlobalVote = false;
						state.setText("LOCAL_LOG:INIT");
						textarea.append("Vote in 10 Seconds for the String- ");
						String arbs = line[2];
						String[] l = arbs.split("-");
						voteMsg = l[0];
						textarea.append(l[0] + "\n");
						voteRequest = true;// set voteRequest to true indicating
											// coordinator has send a vote request
					}
					/*
					 * when a participant sends a decision request to other
					 * participants then give 10 seconds time to all the
					 * participants to response to a decision request or send a
					 * decision
					 */
					else if (line[11].equals("DECISION_REQUEST")) {
						timer1 = new Timer();
						TimerTask tt = new decisionTimeOut();
						timer1.schedule(tt, 10 * 1000);
					}

					/*
					 * if coordinator aborts then change local log to
					 * GLOBAL_ABORT it means coordinator voted so make
					 * isCoordinatorVote flag true
					 */
					else if (line[11].equals("CoordinatorAbort")) {
						isCoordinatorVote = true;
						textarea.append("Coordinator:GLOBAL_ABORT\n");
						local_log = "GLOBAL_ABORT";
						state.setText("LOCAL_LOG:GLOBAL_ABORT");
					}
					/*
					 * if coordinator aborts then change local log to
					 * GLOBAL_COMMIT it means coordinator voted so make
					 * isCoordinatorVote flag true
					 */
					else if (line[11].equals("CoordinatorPreCommit")) {
						isCoordinatorVote = true;
						// local_log="PRE_COMMIT";
						// state.setText("LOCAL_LOG:PREPARE_COMMIT");
						textarea.append("Coordinator" + " - If you really want to commit ACK it in 20 sec.\n");

					} else if (line[11].equals("TimeoutAbort")) {
						isCoordinatorVote = true;
						local_log = "GLOBAL_ABORT";
						state.setText("LOCAL_LOG:GLOBAL_ABORT");
						textarea.append("Coordinator:GLOBAL_ABORT\n");
					}

					/*
					 * when coordinator sends commit that is global commit then
					 * save the string to file
					 */
					else if (line[11].equals("CoordinatorCommit")) {
						isCoordinatorGlobalVote = true;
						local_log = "GLOBAL_COMMIT";
						state.setText("LOCAL_LOG:GLOBAL_COMMIT");
						textarea.append("Coordinator" + " - GLOBAL_COMMIT\n");
						/*
						 * save the arbitary string in 3 different files for 3
						 * participants
						 */
						File fp1 = new File(file1);
						File fp2 = new File(file2);
						File fp3 = new File(file3);
						try {
							/*
							 * open filewriter instance to write on a file,it
							 * saves all the arbitary string into the file*
							 */
							FileWriter f1 = new FileWriter(file1);
							f1.flush();
							FileWriter f2 = new FileWriter(file2);
							f2.flush();
							FileWriter f3 = new FileWriter(file3);
							f3.flush();
							f1.write(voteMsg);// write arb str to file 1
							f2.write(voteMsg);// write arb str to file 2
							f3.write(voteMsg);// write arb str to file 3
							// close all filewriters
							f1.close();
							f2.close();
							f3.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					} // if
				} // while
			} catch (Exception e) {
				e.printStackTrace();
			}
		}// run
	}

	// create the GUI and display it
	private void createGUI() {
		frame.setLayout(null);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		JLabel userlabel = new JLabel("Enter Username");// label for username input
		JTextField usertext = new JTextField();// usertext is to input username from user
		usertext.setEditable(true); // set textfield as editable
		userlabel.setBounds(10, 10, 100, 20);
		usertext.setBounds(120, 10, 120, 30);
		state.setBounds(250, 10, 230, 27);
		textarea.setBounds(10, 50, 480, 370);
		abort.setBounds(10, 430, 150, 30);
		commit.setBounds(180, 430, 150, 30);
		ack.setBounds(350, 430, 150, 30);
		JScrollPane scrollPane = new JScrollPane(textarea);
		textarea.setEditable(false);// set textareas as not editable
		state.setEditable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// put created components in the frame.
		frame.add(userlabel);// add userlabel to the frame
		frame.add(usertext);// add usertext to the frame
		frame.add(state);
		frame.add(textarea);// add textarea to the frame
		frame.add(abort);// add abort button to the frame
		frame.add(commit);// add commit button to the frame
		frame.add(ack);// add connect button to the frame
		/*
		 * when user enter participant name then connect it to the system wait
		 * for 20 seconds for coordinators vote request
		 */
		usertext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent action) {
				username = usertext.getText();// take username
				try {
					socket = new Socket("localhost", 7879);// server is at localhost and
															// port 7879 so use same port number
															
					InputStreamReader streamreader = new InputStreamReader(socket.getInputStream()); 
					in = new BufferedReader(streamreader);
					out = new PrintWriter(socket.getOutputStream());
					out.println("POST:" + username + ": has connected.:::::::::Connect");
					out.flush();
					local_log = "INIT";
					state.setText("LOCAL_LOG:INIT");
					usertext.setEnabled(false);
					frame.setTitle("Participant " + username);
					if (voteRequest == false)// if coordinator not sent any request in 20 seconds then
												// call voteRequestTimeOut method
					{
						timer1 = new Timer();
						TimerTask timert = new voteRequestTimeOut();
						timer1.schedule(timert, 20 * 1000);
					}
					try {
						/*
						 * create a new filereader with the filename as a
						 * participant1 as declared before.This file contaions
						 * arbitary string saved by the participant also a
						 * create bufferreader to read content of the file
						 */
						FileReader reader = new FileReader(filename);
						BufferedReader br = new BufferedReader(reader);
						textarea.read(br, null);// read the content of the file and display it on server
												
						textarea.append("- is the arbitary String from file.\n");
						br.close();// close bufferreader
					} catch (Exception e) {
						e.printStackTrace();
					}

				} // try
				catch (Exception e) {
					e.printStackTrace();
				}
				thread.start();
			}
		});
		/* add a listener for ack button to ack for coordinators ack request */
		ack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent action) {
				String ab = "POST:" + username + ":- PRE_COMMIT:" + httpmsgLocalAbort + ":AckCommit";
				sendToStream(ab);
				local_log = "PRE_COMMIT";
				state.setText("LOCAL_LOG:PRE_COMMIT"); // change local_log to
														// ready
				timer = new Timer();
				TimerTask rt = new AckRequest();
				timer.schedule(rt, 20 * 1000);
			}
		});
		/*
		 * add listner to abort button when participant votes abort then send
		 * the msg to server
		 */
		abort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent action) {
				String ab = "POST:" + username + ":- LOCAL_ABORT:" + httpmsgLocalAbort + ":Abort";
				sendToStream(ab);
				local_log = "READY";
				state.setText("LOCAL_LOG:READY"); // change local_log to ready
				commit.setEnabled(false);
			}
		});
		/*
		 * add listner to precommit button when participant votes abort then
		 * send the msg to server
		 */
		commit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent action) {
				String lcWhole = "POST:" + username + ":-PRE_COMMIT:" + httpmsgLocalCommit + ":PreCommit";
				sendToStream(lcWhole);
				local_log = "READY"; /* set local_log to ready */
				state.setText("LOCAL_LOG:READY");
				abort.setEnabled(false);
				timer = new Timer();
				TimerTask rt = new DecisionRequest();
				timer.schedule(rt, 20 * 1000);
			}
		});
		/*
		 * if participant crashes set local_log to Local_abort and msg to
		 * coordinator
		 */
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				String participantCrash = "POST:" + username + ": has Crashed:" + httpmsgLocalAbort + ":Crashed";
				sendToStream(participantCrash);
			}

			public void windowClosed(WindowEvent we) {
				setVisible(false);
			}
		});
		frame.setVisible(true); // setting frame as visible
		frame.setSize(520, 500);
		frame.setResizable(false);
	}

	public static void main(String args[]) {
		ThreePhaseClient c = new ThreePhaseClient();// create a new instance of
													// the client class
		c.createGUI();// call createGUI() method
	}

	/*
	 * the participants who sent a decision request to all other participants
	 * checks current state if the state is precommit then participant sends
	 * precommit if the state is init,ready or other than precommit then it
	 * sends local abort.
	 */
	public class decisionTimeOut extends TimerTask {
		public void run() {

			String decisionabort = "POST:" + username + ": - LOCAL_ABORT:" + httpmsgLocalAbort
					+ ":ParticipantsDecision";
			String decisionCommit = "POST:" + username + ": - PRE_COMMIT:" + httpmsgLocalCommit
					+ ":ParticipantsDecision";
			String decision = "";
			if (local_log == "PRE_COMMIT") {
				local_log = "PRE_COMMIT";
				state.setText("LOCAL_LOG: PRE_COMMIT");
				decision = decisionCommit;
			} else {
				local_log = "LOCAL_ABORT";
				state.setText("LOCAL_LOG: LOCAL_ABORT");
				decision = decisionabort;
			}
			sendToStream(decision);
			textarea.append(username + local_log);
			timer1.cancel();
		}// run
	}

	/*
	 * if participant is waiting for the coordinator to wait and then after 20
	 * seconds ask other participant for decision set local_log to DECISION
	 */
	public class DecisionRequest extends TimerTask {
		public void run() {
			if (isCoordinatorVote == false) {
				state.setText("LOCAL_LOG: DECISION_REQUEST");
				String decisionRequest = "POST:" + username + ": - DECISION_REQUEST:" + httpmsgDecisionRequest
						+ ":DECISION_REQUEST";
				sendToStream(decisionRequest);
				textarea.append("request sent");
				timer.cancel();
			} // if
			else {
				System.out.println("Coordinated voted in 20 seconds!");
			}
		}// run
	}

	/*
	 * if participant is waiting for the coordinator to send arb str and then
	 * after 20 seconds consider that coordinator has crashed set local_log to
	 * LOCAL_ABORT
	 */
	public class voteRequestTimeOut extends TimerTask {
		public void run() {
			if (voteRequest == false) {
				textarea.append("Not recieved any vote request in 20 Seconds.\n");
				local_log = "LOCAL_ABORT";
				state.setText("LOCAL_LOG:LOCAL_ABORT");
				String voteRequestTimeout = "POST:" + username + ": - LOCAL_ABORT:" + httpmsgLocalAbort
						+ ":voteRequestTimeout";
				sendToStream(voteRequestTimeout);
				timer.cancel();
			}
		}// run
	}

	public class AckRequest extends TimerTask {
		public void run() {
			if (isCoordinatorGlobalVote == false) {
				textarea.append("Not recieved global_Commit from coordinator in 20 Seconds.\n");
				local_log = "GLOBAL_COMMIT";
				state.setText("LOCAL_LOG:GLOBAL_COMMIT");
				String allgc = "POST:" + username + ": - GLOBAL_COMMIT:" + httpmsgLocalCommit + ":CoordinatorCommit";
				sendToStream(allgc);
				timer.cancel();
			}
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
