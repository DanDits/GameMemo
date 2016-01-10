package dan.dit.gameMemo.dataExchange.file;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;
import dan.dit.gameMemo.dataExchange.DataExchangeActivity;
import dan.dit.gameMemo.dataExchange.DirectAnswerService;
import dan.dit.gameMemo.dataExchange.ExchangeService;
import dan.dit.gameMemo.dataExchange.GameDataRequester;
import dan.dit.gameMemo.dataExchange.Postman.PostRecipient;
import dan.dit.gameMemo.util.compaction.Compacter;

public class FileWriteService implements ExchangeService {
	private static final String HEADER_START = "START_MESSAGE_HEADER" + Compacter.SEPARATOR;
	private static final String HEADER_MID = "_MID_";
	private static final String HEADER_END = Compacter.SEPARATOR + "HEADER_END";
	
	/**
	 * Pattern to match a header for a message. The message is followed directly by the header until the next header or the end.
	 * group(1) holds connection id, group(2) holds message id.
	 * Pattern is made mostly unique so that a message cannot accidentially or by user purpose match the pattern, though this is not enforced or checked.
	 */
	public static final Pattern MESSAGE_HEADER_PATTERN = Pattern.compile(HEADER_START + "([0-9]+)" + HEADER_MID + "([0-9]+)" + HEADER_END);
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
		Message msg = mHandler.obtainMessage(DataExchangeActivity.MESSAGE_NEW_CONNECTION);
        mHandler.sendMessage(msg);
	}
	
	public synchronized void close() throws IOException {
		if (mFileWriter != null) {
        	Message lostConnMsg = mHandler.obtainMessage(DataExchangeActivity.MESSAGE_CONNECTION_LOST);
        	mHandler.sendMessage(lostConnMsg);
			mFileWriter.close();
			mFileWriter = null;
		}
	}
	
	@Override
	public void sendData(byte[] data) {
		throw new UnsupportedOperationException();
	}

	private String buildHeader(int connectionId, int messageId) {
		StringBuilder builder = new StringBuilder(52); // 52 estimated length of header 
		builder.append(HEADER_START);
		builder.append(connectionId);
		builder.append(HEADER_MID);
		builder.append(messageId);
		builder.append(HEADER_END);
		return builder.toString();
	}
	
	@Override
	public synchronized void sendPost(int connectionId, int messageId, String message) {
		if (mFileWriter == null) {
			return; // closed
		}
		String header = buildHeader(connectionId, messageId);
		try {
			mFileWriter.write(header);
			if (message != null) {
			    mFileWriter.write(message);
			}
			mFileWriter.flush();
		} catch (IOException e) {
        	try {
				close();
			} catch (IOException e1) {
				// ignore second failure
			}
        	return;
		}
		// send the post to the requester exchanger
		PostRecipient dummyRecipient = mRecipients.get(connectionId);
		dummyRecipient.receivePost(messageId, message);
	}

	@Override
	public void onNewPost(int connectionId, int messageId, String message) {
		throw new UnsupportedOperationException(); // this service only writes and does not receive
	}

	@Override
	public synchronized void registerRecipient(int connectionId, PostRecipient receiver) {
		// the regular post recipient sents to this exchange service but the requester is registered as a recipient
		// the requester sents to the direct answer service
		// the given receiver is registered at the direct answer service, this closes the communication circle
		GameDataRequester requesterReceiver = new GameDataRequester(mResolver, mDirectAnswer, connectionId);
		mRecipients.put(connectionId, requesterReceiver);
		mOriginalRecipients.put(connectionId, receiver);
		mDirectAnswer.registerRecipient(connectionId, receiver);
		requesterReceiver.startExchange(0);
	}

	@Override
	public synchronized void unregisterRecipient(PostRecipient receiver) {
		int index = mOriginalRecipients.indexOfValue(receiver);
		if (index >= 0) {
			int key = mOriginalRecipients.keyAt(index);
			mRecipients.remove(key); // remove the requester recipient
			PostRecipient originalRecipient = mOriginalRecipients.get(key);
			mOriginalRecipients.remove(key);
			mDirectAnswer.unregisterRecipient(originalRecipient);
			Message msg = mHandler.obtainMessage(DataExchangeActivity.MESSAGE_DATA_EXCHANGER_CLOSED, key, -1);
	        mHandler.sendMessage(msg);
	        if (mRecipients.size() == 0) {
	        	try {
					close();
				} catch (IOException e) {
					//ignore
				}
	        }
		}
	}

	@Override
	public void flush() {}

	@Override
	public void startTimeoutTimer(long defaultTimeout) {}

}
