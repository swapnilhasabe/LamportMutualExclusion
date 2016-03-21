
public interface MessageConsumer {
	/**
	 * Consume a message.
	 * 
	 * @param message 		- the message that was received.
	 * @param nodeId		- the node that send the message.
	 * @param sendTime		- the time the message was sent, as per the sender's Lamport clock.
	 * @param receiveTime	- the time the message was received, as per the receiver's Lamport clock.
	 */
	public void process(Object message, int nodeId, long sendTime, long receiveTime);
}
