# ThreePhaseCommit
Coordinator-Participant Using JavaSwing,Socket Programming

3PC  System:
A 3PC protocol System has three java classes one for server side socket connections ,one for Coordinator and for one for 3 Participants. All the Participants are getting registered before starting. When a Coordinator/Participants sends a message then it sends the message in HTTP format, the server then broadcasts the message Coordinator/Participants to all the using GET method.  The date is in HTTP format and the application is independent of the browser. Coordinator sends a arbitrary string to the participants and the participants votes abort or commit. The communication and timeout between Coordinator/Participants is according to 3PC protocol.   
Following are the steps to execute programs:
1.Run ThreePhaseServer. It display’s name of the application “3PC Protocol”. Displays message Server Strated.

2. Run Coordinator: The Coordinator window displays a vote button for global voting. A textfield accepts arbitrary string and sends it to all participants. It also shows local log.
 
3.Run ThreePhaseClient. The Client window shows 3 buttons Abort, PRE-Commit and AckCommit. When user enters username it connects to the system. The user’s name is now a title for that client window. The participant also shows a arbitrary String saved in a text file. When participant receives GLOBAL_COMMIT, it saves the arbitrary string in to a text file. 3 text files are used as we have 3 participants. It also shows current LOCAL_LOG value as INIT.

4. The participant waits for 20 seconds to get vote request from user. If it didn’t get voteRequest  in 20 seconds from coordinator, then it sets LOCAL_LOG to LOCAL_ABORT
 
5. The Coordinator sends a VoteRequest then it waits for 10 seconds for participants to vote, If any participant did not vote in 10 seconds then coordinator sends GLOBAL_ABORT and all participant changes the LOCAL_LOG accordingly.

6. If the Coordinator sends a VoteRequest then it waits for 10 seconds for participants to wait, if all participants votes in 10 seconds then Coordinator can vote. If all participants votes commit, then it sends acknowledgment request to participants or if any participant votes abort it sends GLOBAL_ABORT message.  

7. If participant votes commit but did not receive any message from coordinator then it sends decision request to all other participants. If LOCAL_LOG of any participant is PRE_COMMIT then it commits else aborts locally. 

8. If Coordinator sends acknowledgment request to participants and did not receive any message from participants then it sends GLOBAL_COMMIT to all participants. 

9. If all participants votes commit and ack the commit request then Coordinator sends GLOBAL_COMMIT to participants. And strings gets saved to text file.
 
10.When Coordinator crashes all participants gets the message that coordinator crashed.

