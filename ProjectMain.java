/******************************************************
	 * NOTE TO TA: Implement the interfaces and replace with your own object.
	 * The only required method apart from csEnter() and csLeave() in MESProvider and
	 * tobSend() and tobReceive() in TOBProvider is a message to process the incoming message:
	 * 
	 * 	public void process(Object message, int nodeId, long sendTime, long receiveTime);
	 * 
	 * This is as per the discussion via email between Bhavya Natarajan and Kenneth Alex Mills
	 * on the morning of Dec 3rd: our application class receives the every message and passes it on to both the layers.
	 * 
	 * We provide functionality to both the MES and TOB layers to send an messages/ broadcast 
	 * messages. The same can be extended to you if you pass an instance of ProjectMain to your constructors.
	 * 
	 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;


//The node also acts as a messenger and implements the messenger interface.
public class ProjectMain implements Messenger{
	//Constants
	public static final int WRITE_OUTPUT_CHUNK_SIZE = 100; 	
	public static final int MAX_RANDOM_NUMBER = 10000000;
	
	//Instance variables
	static String outputFileName;
	int id;
	int N,K,tobSendDelay;
	//ArrayList which holds the nodes part of the distributed system 
	ArrayList<Node> nodes = new ArrayList<Node>();
	String configurationFileName;
	//HashMap which has node number as keys and <id,host,port> as value
	HashMap<Integer,Node> store = new HashMap<Integer,Node>();
	// Create all the channels in the beginning and keep it open till the end
	HashMap<Integer,Socket> channels = new HashMap<Integer,Socket>();
	// Create all the output streams associated with each socket 
	HashMap<Integer,ObjectOutputStream> oStream = new HashMap<Integer,ObjectOutputStream>();
	//The Lamport's logical clock.
	long clock;
	
	/******************************************************
	 * NOTE TO TA: Implement the interfaces and replace with your own object.
	 * The only required method apart from csEnter() and csLeave() in MESProvider and
	 * tobSend() and tobReceive() in TOBProvider is a message to process the incoming message:
	 * 
	 * 	public void process(Object message, int nodeId, long sendTime, long receiveTime);
	 * 
	 * This is as per the discussion via email between Bhavya Natarajan and Kenneth Alex Mills
	 * on the morning of Dec 3rd.
	 * 
	 * We provide functionality to both the MES and TOB layers to send an messages/ broadcast 
	 * messages. The same can be extended to you if you pass an instance of ProjectMain to your constructors.
	 */
	
	MESProvider mesProvider;
	TOBProvider tobProvider;
	
	ArrayList<String> output = new ArrayList<String>();	
	
	public static void main(String[] args) throws IOException, InterruptedException {
		//Read the values for all variables from the configuration file
		ProjectMain mainObj = ConfigParser.readConfigFile(args[1]);
		//HashMap which has node number as keys and <id,host,port> as value
		HashMap<Integer,Node> store = new HashMap<Integer,Node>();
		// Get the node number of the current Node
		mainObj.id = Integer.parseInt(args[0]);
		int curNode = mainObj.id;
		//Get the configuration file from command line
		mainObj.configurationFileName = args[1];
		ProjectMain.outputFileName = mainObj.configurationFileName.substring(0, mainObj.configurationFileName.lastIndexOf('.'));
		// Transfer the collection of nodes from ArrayList to hash map which has node id as key since  
		// we need to get and node as value ,it returns <id,host,port> when queried with node Id.
		for(int i=0;i<mainObj.nodes.size();i++){
			mainObj.store.put(mainObj.nodes.get(i).nodeId, mainObj.nodes.get(i));
		}
		// Get the port number on which this node should listen 
		int serverPort = mainObj.nodes.get(mainObj.id).port;
		// Start server on this node's assigned port
//		System.out.println("Starting server on port:"+serverPort);
		ServerSocket listener = new ServerSocket(serverPort);
		Thread.sleep(10000);
		//Create channels and keep it till the end
		for(int i=0;i<mainObj.N;i++){
			String hostName = mainObj.store.get(i).host;
//			InetAddress hostName = InetAddress.getLocalHost();
			int port = mainObj.store.get(i).port;
			InetAddress address = InetAddress.getByName(hostName);
			Socket client = new Socket(address,port);
			// Get the sockets for all neighbors
//			System.out.println("Attempting to connect to port:"+port);
//			Socket client = new Socket(hostName,port);
			// Put the neighbor sockets in hash map called channels indexed by their node id's
			mainObj.channels.put(i, client);
			// Get an output stream associated with each socket and put it in a hashmap oStream
			ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
			mainObj.oStream.put(i, oos);		
		}
		mainObj.mesProvider = new LamportMESProvider(mainObj, mainObj.id, mainObj.N);
		mainObj.tobProvider = new PrivilegeBasedTOBProvider(mainObj.mesProvider, mainObj, mainObj.id, mainObj.N, mainObj.K);
		
				//Listen for incoming connections and then start the listener thread for each connection.
		try {
			for(int i=0;i<mainObj.N;i++){
//			while (true) {
				// This node listens as a Server for the clients requests
//				System.out.println("about to get a client");
				Socket socket = listener.accept();
//				System.out.println("got a client");
				// For every client request start a new thread 
				new ClientThread(socket,mainObj).start();
			}
		}
		finally {
			listener.close();
		}
		

		//Start this node's sending of application messages.
		new ApplicationSendThread(mainObj).start();
		//Start this node's receiving of application messages.
		new ApplicationReceiveThread(mainObj).start();
	}
	
	
	// Function to generate random number in a given range
	int getRandomNumber(int min,int max){
		// Usually this can be a field rather than a method variable
		Random rand = new Random();
		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;
		return randomNum;
	}


	/**
	 * Messenger interface method implementations.
	 */
	
	//Send a message as an object to the specified nodeId
	//Locking on mainObj - that is the key data structure here. It simulates the fact that at a time,
	//a process does only one thing.
	public long send(Object message, int nodeId) {
//		System.out.println("normal send called!!");
		long sendTime = -1;
		synchronized(this){
			MessageContainer piggybackedMessage = new MessageContainer(message, this.clock, this.id);
			try{
				this.oStream.get(nodeId).writeObject(piggybackedMessage);
			} catch (IOException ioex){
				ioex.printStackTrace();
			}
			sendTime = this.clock++;
		}
		return sendTime;
	}
	
	public long broadcast(Object message, boolean includeSelf) {
//		System.out.println("broadcast called!!");
		long sendTime = -1;
		synchronized(this){
			MessageContainer piggybackedMessage = new MessageContainer(message, this.clock, this.id);
			for(int i=0;i<this.N;i++){
				if(includeSelf || (i != this.id)){
					try{	
						this.oStream.get(i).writeObject(piggybackedMessage);
					} catch (IOException ioex){
						ioex.printStackTrace();
					}
				}
			}
			sendTime = this.clock++;
		}
//		System.out.println("broadcast call OVER!!");
		return sendTime;
	}
}

//Server reading objects sent by other clients in the system in a thread 
class ClientThread extends Thread {
	Socket cSocket;
	ProjectMain mainObj;

	public ClientThread(Socket csocket,ProjectMain mainObj) {
		this.cSocket = csocket;
		this.mainObj = mainObj;
	}

	public void run() {
		//Create an ObjectInputStream to read from the socket.
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(cSocket.getInputStream());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		while(true){
			try {
				MessageContainer piggyBackedMessage = (MessageContainer)ois.readObject();
				if(piggyBackedMessage instanceof FinishMessageContainer){
					break;
				}
				long receiveTime = -1;
				synchronized(mainObj){
					mainObj.clock = Math.max(mainObj.clock, piggyBackedMessage.clock) + 1;
					receiveTime = mainObj.clock;
				}
				mainObj.mesProvider.process(piggyBackedMessage.message, piggyBackedMessage.senderId, piggyBackedMessage.clock, receiveTime);
				mainObj.tobProvider.process(piggyBackedMessage.message, piggyBackedMessage.senderId, piggyBackedMessage.clock, mainObj.clock);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} 
		}
	}
}


//An object of this class send the application messages for the node represented 
//by its mainObj.
class ApplicationSendThread extends Thread{
	ProjectMain mainObj;
	
	public ApplicationSendThread(ProjectMain mainObj) {
		this.mainObj = mainObj;
	}
	
	public void run() {
		//Broadcast K messages!!
//		System.out.println("Started sending!");
		for(int i=0;i<mainObj.K;i++){
//			System.out.println("About to sleep!");
			try{
				Thread.sleep(mainObj.tobSendDelay);
			} catch (InterruptedException iex){
				iex.printStackTrace();
			}
//			System.out.println("Woke up!");
			mainObj.tobProvider.tobSend(""+mainObj.getRandomNumber(0, ProjectMain.MAX_RANDOM_NUMBER));
//			System.out.println("Sent message"+ i);
		}
		//The end of the run() message here is reached surely before the end of the run() 
		//message in ApplicationReceiveThread.
	}
}

//An object of this class receive the application messages for the node represented 
//by its mainObj.
class ApplicationReceiveThread extends Thread{
	ProjectMain mainObj;
	
	ApplicationReceiveThread(ProjectMain mainObj){
		this.mainObj = mainObj;
	}
	
	public void run() {
//		System.out.println("Started receiving!");
		for(int i=0;i<mainObj.K * mainObj.N;i++){
//			System.out.println("About to call tobReceive!!");
			String messageReceived = mainObj.tobProvider.tobReceive();
//			System.out.println("got a message from tobReceive!");
			synchronized(mainObj.output){
				mainObj.output.add(messageReceived);
			}
		}
//		System.out.println("Received the last message!");
//		At this point of time, all application messages will have already been received =>
//		the application has no more messages to send either. At a deeper level, even the 
//		messages releasing the distributed lock will have been released. Therefore, we exit.
		synchronized(mainObj){
			for(int i=0;i<mainObj.N;i++){
				//Tell all other nodes that we are done!
				try{
					mainObj.oStream.get(i).writeObject(new FinishMessageContainer());
					mainObj.oStream.get(i).close();
					mainObj.channels.get(i).close();
				} catch (IOException ioex){
					;
				}
			}
			new OutputWriter(mainObj).writeToFile();
			System.exit(0);
		}
	}
}


//Print the output to the output File
class OutputWriter {
	ProjectMain mainObj;

	public OutputWriter(ProjectMain mainObj) {
		this.mainObj = mainObj;
	}


	public void writeToFile() {
		String fileName = ProjectMain.outputFileName+"-"+mainObj.id+".out";
		synchronized(mainObj.output){
			try {
				File file = new File(fileName);
				FileWriter fileWriter;
				if(file.exists()){
					fileWriter = new FileWriter(file,true);
				}
				else
				{
					fileWriter = new FileWriter(file);
				}
				BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
				for(int i=0;i<mainObj.output.size();i++){
					bufferedWriter.write(mainObj.output.get(i) + "\n");
				}			
				mainObj.output.clear();
				// Always close files.
				bufferedWriter.close();
			}
			catch(IOException ex) {
				System.out.println("Error writing to file '" + fileName + "'");
				// Or we could just do this: ex.printStackTrace();
			}
		}
	}
}