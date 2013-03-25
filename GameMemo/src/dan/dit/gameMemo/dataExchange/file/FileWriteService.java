package dan.dit.gameMemo.dataExchange.file;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.content.ContentResolver;
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;
import dan.dit.gameMemo.dataExchange.DataExchangeActivity;
import dan.dit.gameMemo.dataExchange.ExchangeService;
import dan.dit.gameMemo.dataExchange.GameDataExchangerEgoistic;
import dan.dit.gameMemo.dataExchange.Postman.PostRecipient;
import dan.dit.gameMemo.dataExchange.bluetooth.BluetoothDataExchangeActivity;

public class FileWriteService implements ExchangeService {
    private SparseArray<PostRecipient> mOriginalRecipients;	
    private SparseArray<PostRecipient> mRecipients;	
    private ExchangeService mDirectAnswer;
	private ContentResolver mResolver;
	private FileWriter mFileWriter;
	private Handler mHandler;
	
	public FileWriteService(Handler handler, ContentResolver resolver, File outputFile) throws IOException {
		mResolver = resolver;
		mOriginalRecipients = new SparseArray<PostRecipient>();
		mRecipients = new SparseArray<PostRecipient>();
		mDirectAnswer = new DirectAnswerService();
		mFileWriter = new FileWriter(outputFile);
		mHandler = handler;
		// we have a new connection 
		Message msg = mHandler.obtainMessage(BluetoothDataExchangeActivity.MESSAGE_NEW_CONNECTION);
        mHandler.sendMessage(msg);
	}
	
	public void close() throws IOException {
		mFileWriter.close();
	}
	
	@Override
	public void sendData(byte[] data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized void sendPost(int connectionId, int messageId, String message) {
		try {
			mFileWriter.write(message); //TODO somehow seperate two different messages, probably best would be to use a Compressor, though this takes more space 
			mFileWriter.flush();
		} catch (IOException e) {
			//TODO tell this the handler? Abort?
		}
		// send the post to the dummy exchanger
		PostRecipient dummyRecipient = mRecipients.get(connectionId);
		dummyRecipient.receivePost(messageId, message);
	}

	@Override
	public void onNewPost(int connectionId, int messageId, String message) {
		throw new UnsupportedOperationException(); // this service only writes and does not receive
	}

	@Override
	public synchronized void registerRecipient(int connectionId, PostRecipient receiver) {
		// the regular post recipient sents to this exchange service but the dummy is registered as a recipient
		// the dummy sents to the direct answer service
		// the given receiver is registered at the direct answer service
		GameDataExchangerEgoistic dummyReceiver = new GameDataExchangerEgoistic(mResolver, mDirectAnswer, connectionId);
		dummyReceiver.setIgnoreReceivedGames(true);
		mRecipients.put(connectionId, dummyReceiver);
		mOriginalRecipients.put(connectionId, receiver);
		mDirectAnswer.registerRecipient(connectionId, receiver);
		dummyReceiver.startExchange(0);
	}

	@Override
	public synchronized void unregisterRecipient(PostRecipient receiver) {
		int index = mOriginalRecipients.indexOfValue(receiver);
		if (index >= 0) {
			int key = mOriginalRecipients.keyAt(index);
			mRecipients.remove(key); // remove the dummy recipient
			PostRecipient originalRecipient = mOriginalRecipients.get(key);
			mOriginalRecipients.remove(key);
			mDirectAnswer.unregisterRecipient(originalRecipient);
			Message msg = mHandler.obtainMessage(DataExchangeActivity.MESSAGE_DATA_EXCHANGER_CLOSED, key, -1);
	        mHandler.sendMessage(msg);
	        if (mRecipients.size() == 0) {
	        	// we are finished, connection is now 'lost'
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
