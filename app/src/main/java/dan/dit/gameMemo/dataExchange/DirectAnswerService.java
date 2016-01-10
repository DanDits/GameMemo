package dan.dit.gameMemo.dataExchange;

import android.util.SparseArray;
import dan.dit.gameMemo.dataExchange.Postman.PostRecipient;

/**
 * All send messages will be directly sent to the according registered PostRecipient.
 * So it does not make much sense to use this service for a GameDataExchanger since it would send
 * messages to itself.<br>
 * Does not support sending of binary data. Flushing will do nothing. Does not time out.
 * @author Daniel
 *
 */
public class DirectAnswerService implements ExchangeService {

    private SparseArray<PostRecipient> mRecipients = new SparseArray<PostRecipient>();	
    
	@Override
	public void sendData(byte[] data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendPost(int connectionId, int messageId, String message) {
		onNewPost(connectionId, messageId, message);
	}

	@Override
	public void onNewPost(int connectionId, final int messageId, final String message) {
		final PostRecipient receiver = mRecipients.get(connectionId);
		if (receiver != null) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					receiver.receivePost(messageId, message);
				}
			}).start();
		}
	}

	@Override
	public void registerRecipient(int connectionId, PostRecipient receiver) {
		mRecipients.put(connectionId, receiver);
	}

	@Override
	public void unregisterRecipient(PostRecipient receiver) {
		int index = mRecipients.indexOfValue(receiver);
		if (index >= 0) {
			int key = mRecipients.keyAt(index);
			mRecipients.remove(key);
		}
	}

	@Override
	public void flush() {}

	@Override
	public void startTimeoutTimer(long defaultTimeout) {}

}
