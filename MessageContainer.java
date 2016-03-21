import java.io.Serializable;

public class MessageContainer implements Serializable {
	//This is a basic message container - which contains the clock and an arbitrary message
	//to be sent.
	Object message;
	long clock;
	int senderId;
	
	MessageContainer(){
	}
	
	MessageContainer(Object message, long clock, int senderId){
		this.message = message;
		this.clock = clock;
		this.senderId = senderId;
	}
}

class FinishMessageContainer extends MessageContainer implements Serializable{
	
}
