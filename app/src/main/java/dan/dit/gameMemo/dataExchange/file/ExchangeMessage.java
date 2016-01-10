package dan.dit.gameMemo.dataExchange.file;
/**
 * A simple structure holding a connection id, a message id and a message.
 * Does not enforce or check any value. This is as stupid as any C-structure.
 * @author Daniel
 *
 */
public class ExchangeMessage {
	private int mConnectionId;
	private int mMessageId;
	private String mMessage;
	
	/**
	 * Creates a new ExchangeMessage.
	 * @param connectionId The connection id.
	 * @param messageId The message id.
	 * @param message The message.
	 */
	public ExchangeMessage(int connectionId, int messageId, String message) {
		mConnectionId = connectionId;
		mMessageId = messageId;
		mMessage = message;
	}	
	
	/**
	 * Returns the connection id.
	 * @return The connection id.
	 */
	public int getConnectionId() {
		return mConnectionId;
	}

	
	/**
	 * Returns the message id.
	 * @return The message id.
	 */
	public int getMessageId() {
		return mMessageId;
	}

	/**
	 * Returns the message.
	 * @return The message.
	 */
	public String getMessage() {
		return mMessage;
	}
}
