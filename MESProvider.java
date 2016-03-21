public interface MESProvider extends MessageConsumer {
	/**
	 * Gives the caller of the message access to the critical section 
	 * when the method returns.
	 * 
	 * IMPORTANT: This call is blocking - it will return only when the object gets access
	 * to the critical section.  
	 */
	public void csEnter();
	
	/**
	 * Leave the critical section.
	 */
	public void csExit();
	
}