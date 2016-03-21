import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.ArrayList;


public class LamportMESProvider implements MESProvider {
	Messenger messenger;
	//Stores the last message times that we have seen from a particular node id.
	HashMap<Integer, Long> lastMessageTimes = new HashMap<Integer, Long>();
	//Stores the next elements that need to enter the critical section
	PriorityQueue<CSRequest> pq = new PriorityQueue<CSRequest>();
	int N;
	int nodeId; 

	Object lock = new Object();

	//This is the boolean variable that tells whether the node has access to the critical section.
	boolean inCriticalSection = false;
	//The currentRequest variable holds the object related to the current request.
	CSRequest currentRequest = null;
	ArrayList<Integer> requestsWhenInCS = new ArrayList<Integer>();

	public LamportMESProvider(Messenger messenger, int nodeId,  int N){
		this.messenger = messenger;
		this.nodeId = nodeId;
		this.N = N;
	}

	public void initialize(){
		currentRequest = null;
		inCriticalSection = false;
		lastMessageTimes.clear();
		requestsWhenInCS.clear();
	}


	public void process(Object message, int senderId, long sendTime, long receiveTime){
		synchronized(this){
			if(inCriticalSection){
				if(message instanceof LamportRequestMessage){
//					System.out.println("received a request message in cs");
					//1. Add the process to the priority queue.
					pq.add(new CSRequest(senderId, sendTime));
					//2. Also add the process to a temporary buffer to hold processes which we need to reply to.
					requestsWhenInCS.add(senderId);
				} else if (message instanceof LamportReplyMessage){
//					System.out.println("received a reply message in Cs");
				} else if (message instanceof LamportReleaseMessage){
//					System.out.println("received a release message in Cs");
					//This should not happen.
					;
				} else {
					;
				}
			} else {
				if(message instanceof LamportRequestMessage){
					CSRequest nextRequest = new CSRequest(senderId, sendTime);
					//					if the process already has its own request message in the priority queue AND 
					//the process is in a better position than the requesting process:
					if(currentRequest != null &&  (currentRequest.compareTo(nextRequest) < 0)){
//						System.out.println("Received a request of lower prioirity from "+ senderId +" with time " + sendTime);
						//						Add the process to the priority queue.
						pq.add(nextRequest);
						//						Don’t send back a reply.
					} else {
						//						Add the process to the priority queue.
//						System.out.println("Received a request of higher prioirity from "+ senderId+" with time " + sendTime);
						pq.add(nextRequest);

						//Send back a reply.
//						System.out.println("About to send a reply to "+ senderId);
						messenger.send(new LamportReplyMessage(), senderId);
						//						
					}
				} else if (message instanceof LamportReplyMessage){
//					System.out.println("Received a reply message not in CS from "+ senderId + " with timestamp "+ sendTime);
					;
				} else if (message instanceof LamportReleaseMessage){
//					System.out.println("received a release message not in Cs from "+ senderId + " with timestamp "+ sendTime);
					//Dequeue the priority queue.
					pq.poll();					
				} else {
					;
				}
				if(currentRequest != null){
					lastMessageTimes.put(senderId, sendTime);
//					System.out.println("l1->"+l1());
//					System.out.println("l2->"+l2());
					if(l1() && l2()){
//						System.out.println("About to enter the critical section for real");
						inCriticalSection = true;
						notify();
					}
				} 
			}
		}

	}

	public void csEnter() {
		synchronized(this){
			initialize();
			//			Put its own request into priority queue.
			currentRequest = new CSRequest(nodeId, -1);
			//			Send out request messages - set the clock field to be the time it was sent out.
			currentRequest.clock = messenger.broadcast(new LamportRequestMessage(), false);
			//Add the current request also to the priority queue
			pq.add(currentRequest);
			//			wait() indefinitely until notified - this is the blocking part.
			while(!inCriticalSection){
				try{
//					System.out.println("About to wait!");
					wait();
//					System.out.println("done waiting!!" + inCriticalSection);
				} catch (InterruptedException iex){
					;
				}
			} 

			//			return - the application can enter the critical section.
			return;
		}
	}

	public void csExit() {
		//			Send a release message to every other process in the critical section.
		synchronized(this){
//			System.out.println("got the lock!");
			messenger.broadcast(new LamportReleaseMessage(), false);
			//			Dequeue oneself from the Priority Queue.
			pq.poll();
			//			Send out a reply message to every process in the temporary buffer.
			for(int i:requestsWhenInCS){
				messenger.send(new LamportReplyMessage(), i);
			}
			initialize();
			requestsWhenInCS.clear();
			inCriticalSection = false;
		}

	}

	//	l1():
	//		if the process has received a message from every process with a timestamp greater than its request:
	//		return true;
	//		Else:
	//		return false;
	public boolean l1(){
		if(currentRequest!=null && lastMessageTimes.size() >= (N - 1)){
			for(Integer i: lastMessageTimes.keySet()){
				if(i!=nodeId){
					if(lastMessageTimes.get(i) <= currentRequest.clock){
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}

	//	l2():
	//	if the process has its own request at the front of the priority queue:
	//		return true:
	//	else:
	//		return false;
	public boolean l2(){
		if(!pq.isEmpty()){
			CSRequest next = pq.peek();
			if(next.nodeId == this.nodeId){
				return true;
			} else {
//				System.out.println("l2 returning false because it is not at the head of the queue but "+ next.nodeId+"is in a q of size"
//						+ pq.size());
				return false;
			}
		}
		return false;
	} 
}


class CSRequest implements Comparable<CSRequest>{
	int nodeId;
	long clock;

	CSRequest(int nodeId, long clock){
		this.nodeId = nodeId;
		this.clock = clock;
	}

	public int compareTo(CSRequest o) {
		if(this.clock == o.clock){
			//if the clocks are equal, the smaller node id should come to the front of the queue.
			return this.nodeId - o.nodeId;
		} else if(this.clock > o.clock){
			return 1;
		} else {
			return -1;
		}
	}
}

