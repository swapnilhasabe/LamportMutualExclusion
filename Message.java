import java.io.Serializable;

public class Message implements Serializable{
	
}


class LamportRequestMessage extends Message implements Serializable{
	int nodeId;
}

class LamportReplyMessage extends Message implements Serializable{
	
}

class LamportReleaseMessage extends Message implements Serializable{
	
}


class PrivilegeTOBToken extends Message implements Serializable{
	long seqNum;
	
	PrivilegeTOBToken(long seqNum){
		this.seqNum = seqNum;
	}
}

class PrivilegeTOBMessage extends Message implements Serializable{
	long broadcastId;
	String content;
	
	PrivilegeTOBMessage(long broadcastId, String content){
		this.broadcastId = broadcastId; 
		this.content = content;
	}
}

