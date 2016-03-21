public interface Messenger {
	/**
	 * Send a message to a specific node
	 * 
	 * @param message  	-	the message to send.
	 * @param nodeId	- 	the nodeId to send to.
	 * @return			- 	the time at which the message was sent - this is the time at which send was invoked.
	 */
	public long send(Object message, int nodeId);
	
	/**
	 * Broadcast a message.
	 * 
	 * @param message  	-	the message to broadcast.
	 * @param includeSelf -	whether the broadcast should include the node itself.	
	 * @return			- 	the time at which the message was sent - this is the time at which send was invoked.
	 */
	public long broadcast(Object message, boolean includeSelf);
}
