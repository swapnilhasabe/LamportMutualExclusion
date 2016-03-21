import java.util.*;

public class PrivilegeBasedTOBProvider implements TOBProvider{
	MESProvider mesProvider;
	Messenger messenger;
	int nodeId;
	int N;
	int K;

	//The id of the next broadcast message.
	long nextDeliver;

	//The messages that the node needs to broadcast.
	LinkedList<String> messagesToSend = new LinkedList<String>();

	//Contains a mapping of broadcast id to messages.
	Hashtable<Long, String> receivedMessages = new Hashtable<Long, String>();

	public PrivilegeBasedTOBProvider(MESProvider mesProvider, Messenger messenger, int nodeId, int N, int K) {
		this.mesProvider = mesProvider;
		this.messenger = messenger;
		this.nodeId = nodeId;
		this.N = N;
		this.K = K;
		//Every node is expecting the broadcast with id 1 initially.
		nextDeliver = 1;
		if(this.nodeId == 0){
			messenger.send(new PrivilegeTOBToken(1), (this.nodeId + 1) % this.N);
		}
	}

	public void tobSend(String m) {
		synchronized(this){
			messagesToSend.add(m);
		}
	}

	public String tobReceive() {
		synchronized(this){
			String ret = null;
			while(!receivedMessages.containsKey(nextDeliver)){
				try{
//					System.out.println("About to wait");
					wait();
//					System.out.println("Out of wait");
				} catch(InterruptedException iex){
					iex.printStackTrace();
				}
			}
//			System.out.println("Now changing nextDeliver!");
			ret = receivedMessages.get(nextDeliver);
			receivedMessages.remove(nextDeliver);
			nextDeliver++;
			return ret;
		}
	}

	public void process(Object message, int nodeId, long sendTime, long receiveTime){
		if(message instanceof PrivilegeTOBMessage){
			synchronized(this){
				PrivilegeTOBMessage tobMessage = (PrivilegeTOBMessage)message;
				
				receivedMessages.put(tobMessage.broadcastId, tobMessage.content);
//				System.out.println("Received a broadcast message with id "+tobMessage.broadcastId + " from node id, next delivery :" 
//						+ nextDeliver + "nextDeliver:" + receivedMessages.containsKey(nextDeliver) + " received messages:" + receivedMessages.size());
				//Notify the waiting tobSend in case that is required.
				if(receivedMessages.containsKey(nextDeliver)){
//					System.out.println("About to notify!");
					this.notify();
				}
			}
		} else if (message instanceof PrivilegeTOBToken) {
//			System.out.println("Asking for the critical section!!!");
			new CSThread(this, (PrivilegeTOBToken) message).start();
		}
	}
}

class CSThread extends Thread{
	PrivilegeBasedTOBProvider tobProvider;
	PrivilegeTOBToken token;

	CSThread(PrivilegeBasedTOBProvider tobProvider, PrivilegeTOBToken token){
		this.tobProvider = tobProvider;
		this.token = token;
	}

	public void run(){
		tobProvider.mesProvider.csEnter();
		synchronized(tobProvider){
			for(String m: tobProvider.messagesToSend){
				tobProvider.messenger.broadcast(new PrivilegeTOBMessage(token.seqNum, m), true);
				token.seqNum++;
			}
			tobProvider.messagesToSend.clear();
			//Once N*K  messages are sent, don't bother to send any further messages.
			if(token.seqNum <= (tobProvider.N * tobProvider.K) ){
				tobProvider.messenger.send(token, (tobProvider.nodeId + 1) % tobProvider.N);
			}
		}
		tobProvider.mesProvider.csExit();
	}
}
