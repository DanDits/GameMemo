package dan.dit.gameMemo.dataExchange.file;

public class ExchangeMessage {
	private int mConnectionId;
	private int mMessageId;
	private String mMessage;
	
	public ExchangeMessage(int connectionId, int messageId, String message) {
		mConnectionId = connectionId;
		mMessageId = messageId;
		mMessage = message;
	}	
	
	public int getConnectionId() {
		return mConnectionId;
	}

	public int getMessageId() {
		return mMessageId;
	}

	public String getMessage() {
		return mMessage;
	}
}
