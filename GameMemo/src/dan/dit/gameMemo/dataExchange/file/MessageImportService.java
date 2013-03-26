package dan.dit.gameMemo.dataExchange.file;

import java.util.List;

import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;
import dan.dit.gameMemo.dataExchange.DataExchangeActivity;
import dan.dit.gameMemo.dataExchange.ExchangeService;
import dan.dit.gameMemo.dataExchange.GameDataExchanger;
import dan.dit.gameMemo.dataExchange.Postman.PostRecipient;

public class MessageImportService implements ExchangeService {
    private SparseArray<PostRecipient> mRecipients;	
	private Handler mHandler;
	private List<ExchangeMessage> mMessages;
	
	public MessageImportService(Handler handler, List<ExchangeMessage> messages) {
		mHandler = handler;
		mMessages = messages;
		mRecipients = new SparseArray<PostRecipient>();
		// we have a new connection 
		Message msg = mHandler.obtainMessage(DataExchangeActivity.MESSAGE_NEW_CONNECTION);
        mHandler.sendMessage(msg);
	}
	
	@Override
	public void sendData(byte[] data) {
		throw new UnsupportedOperationException();
	}
	
	public void startImport() {
		for (ExchangeMessage msg : mMessages) {
			onNewPost(msg.getConnectionId(), msg.getMessageId(), msg.getMessage());
		}
	}

	@Override
	public void sendPost(int connectionId, int messageId, String message) {
		//ignore their posts, only sent back empty request for the protocol
		if (messageId == GameDataExchanger.MESSAGE_ID_OFFERING_DATA) {
			onNewPost(connectionId, GameDataExchanger.MESSAGE_ID_REQUESTING_DATA, "");
		}
	}

	@Override
	public void onNewPost(int connectionId, int messageId, String message) {
		final PostRecipient receiver = mRecipients.get(connectionId);
		if (receiver != null) {
			receiver.receivePost(messageId, message);
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
			Message msg = mHandler.obtainMessage(DataExchangeActivity.MESSAGE_DATA_EXCHANGER_CLOSED, key, -1);
	        mHandler.sendMessage(msg);
			mRecipients.remove(key);
			if (mRecipients.size() == 0) {
	        	Message lostConnMsg = mHandler.obtainMessage(DataExchangeActivity.MESSAGE_CONNECTION_LOST);
	        	mHandler.sendMessage(lostConnMsg);
			}
		}
	}

	@Override
	public void flush() {}

	@Override
	public void startTimeoutTimer(long defaultTimeout) {}

}
