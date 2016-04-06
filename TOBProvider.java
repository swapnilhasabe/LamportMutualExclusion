/**
 * Any class which provides the functionality of Total Order Broadcast *must* implement this interface.
 */
 //Third layer in the application
public interface TOBProvider extends MessageConsumer {
	/**
	 * Totally ordered broadcast the message m!
	 * 
	 * @param m		a String to be broadcast
	 */
	public void tobSend(String m);
	
	/**
	 * Receive the next totally ordered broadcasted message and return it (may block).
	 * 
	 * @return		the String which was received by the broadcast.
	 */
	public String tobReceive();
	

}
