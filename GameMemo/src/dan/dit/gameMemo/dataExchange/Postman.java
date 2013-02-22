package dan.dit.gameMemo.dataExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.util.Log;
/**
 * A man-in-the-middle class to simplify sending and receiving of messages over a ExchangeService.
 * A Postman coordinates the connection between a {@link GameDataExchanger} and an {@link ExchangeService}
 * by offering a simple way to send a message over the service to the target. This target needs to listen
 * for the same exchange service and use the same connection id, else the message will be lost.<br>
 * Sending of <code>null</code> or empty messages is possible and results in the receiving of an empty message
 * String in either case.<br>
 * A message will be received in its own thread, so a PostRecipient must be 
 * thread safe, this can be easily achieved by synchronizing the receivePost method.<br>
 * Always close a Postman from the PostRecipient after using to free resources and references, the ExchangeService
 * does not need to care about closing a Postman. After being closed the Postman is unusable and all methods will not
 * perform any action when invoked.
 * @author Daniel
 *
 */
public class Postman {
	private static final String TAG = "GameDataExchange";
	// states how incoming data is expected for a single message (HEADER=(KEY, ID, SIZE)[, DATA])
	private static final int HEADER_SIZE = 12; // in bytes, usually a multiple of 4 since data is stored from ints
	private static final int DATA_INCOME_STATE_EXPECT_HEADER = 1;
	private static final int DATA_INCOME_STATE_EXPECT_DATA = 2;
	
	// curr message fields
	private ByteArrayOutputStream mBytesReceived;
	private int mDataIncomeState;
	private int mCurrMessageConnectionId;
	private int mCurrMessageId;
	private int mCurrMessageSize;
	private byte[] mCurrMessage;
	
	// member fields
	private final int mConnectionId;
	private PostRecipient mReceiver;
	private ExchangeService mTarget;
	
	public interface PostRecipient {
		void receivePost(int messageId, String message);
	}
	
	public Postman(int connectionId, PostRecipient receiver, ExchangeService target) {
		mConnectionId = connectionId;
		mReceiver = receiver;
		mTarget = target;
		if (receiver == null || target == null) {
			throw new NullPointerException("PostRecipient or ExchangeService null.");
		}
		target.addInputReceiver(this);
		mDataIncomeState = DATA_INCOME_STATE_EXPECT_HEADER;
		mBytesReceived = new ByteArrayOutputStream(512);
	}
	
	public synchronized void close() {
		// free resources, this makes the Postman unusable
		if (mTarget != null) {
			mTarget.removeInputReceiver(this, true);
		}
		mTarget = null;
		mReceiver = null;
		mBytesReceived = null;
		mCurrMessage = null;
	}
	
	public synchronized void onReceiveData(byte[] data, int dataCount) {
		if (mReceiver == null) {
			return; // ignore gracefully, the Postman is not usable anymore, but there could still be some data around
		}
		if (dataCount <= 0) {
			return; // no data received, you lied!
		}
		//Log.d(TAG, "Received " + dataSize + " bytes via bluetooth.");
		saveReceivedBytes(data, 0, dataCount);
		new Thread() {
			@Override
			public void run() {
				while (processData()) {
					//Log.d(TAG, "Data processed. Waiting for " + ((mDataIncomeState == DATA_INCOME_STATE_EXPECT_DATA) ? "DATA" : "HEADER"));
				}
			}
		}.start();
	}
	
	public synchronized void sendPost(int messageId, String message) {
		if (mTarget == null) {
			Log.d(TAG, "Attempting to send message " + messageId + " message = " + message + " over closed postman (target is null)");
			return; // closed
		}
		byte[] header = new byte[HEADER_SIZE];
		intToByte(mConnectionId, header, 0);
		intToByte(messageId, header, 4);
		intToByte(0, header, 8); // default message size is 0
		if (message != null && message.length() > 0) {
			//Log.d(TAG, "Sending message : key=" + mGameKey + " and id=" + messageId + ", message size =" + message.length);
			int messageSize = message.length();
			intToByte(messageSize, header, 8); // updating actual message size which is >0
			mTarget.sendData(header);
			mTarget.sendData(message.getBytes());
		} else {
			// sending an empty message (only the header)
			mTarget.sendData(header); 
		}
		mTarget.flush();
	}
	
	private byte[] processReceivedBytes() {
		try {
			mBytesReceived.flush();
		} catch (IOException e) {
			// "this implementation does nothing", though http://www.gregbugaj.com/?p=283 recommends to flush the stream? well then there should be no exception..
		}
		byte[] temp = mBytesReceived.toByteArray();
		mBytesReceived.reset();
		return temp;
	}
	
	private void saveReceivedBytes(byte[] data, int offset, int count) {
		//Log.d(TAG, "SAVING " + count + " bytes to buffer, old buffer size = " + mBytesReceived.size());
		mBytesReceived.write(data, offset, count); 
	}
	
	private synchronized boolean processData() {
		if (mBytesReceived == null) {
			return false; // is closed, cannot process data anymore
		}
		if (mDataIncomeState == DATA_INCOME_STATE_EXPECT_HEADER) {
			// expecting 12 bytes, are they available?
			
			if (mBytesReceived.size() >= HEADER_SIZE) {
				byte[] received = processReceivedBytes();
				assert received.length == mBytesReceived.size();
				mCurrMessageConnectionId = byteToInt(received, 0);
				mCurrMessageId = byteToInt(received, 4);
				mCurrMessageSize = byteToInt(received, 8);
				if (received.length > HEADER_SIZE) {
					// we processed too much data that did not belong here, rewrite it to buffer
					saveReceivedBytes(received, HEADER_SIZE, received.length - HEADER_SIZE);
				}
				processHeader();
				return true;
			} else {
				return false; // not enough bytes available, wait for more data
			}
		} else if (mDataIncomeState == DATA_INCOME_STATE_EXPECT_DATA) {
			// expecting any byte data
			// lets see if we already fully received the message
			int missingBytes = mCurrMessageSize - mBytesReceived.size();
			if (missingBytes <= 0) {
				// we received all bytes (and more)
				byte[] received = processReceivedBytes();
				assert received.length == mCurrMessageSize;
				if (received.length > mCurrMessageSize) {
					mCurrMessage = new byte[mCurrMessageSize];
					System.arraycopy(received, 0, mCurrMessage, 0, mCurrMessageSize);
					saveReceivedBytes(received, mCurrMessageSize, received.length - mCurrMessageSize); // there were some bytes received that did not belong to the message, save them
				} else {
					// here received bytes is greater than or equal message size and also lower than or equal message size, so they are equal
					mCurrMessage = received;
				}
				// message is satisfied and completely received, handle it and start waiting for a new message
				handleCurrentMessage();
				return true;
			}
			return false;
		} else {
			throw new IllegalStateException("Expecting state invalid for postman with connectionId " + mConnectionId + " and message state " + mDataIncomeState);
		}
	}
	
	private void processHeader() {
		mDataIncomeState = DATA_INCOME_STATE_EXPECT_DATA;
		if (mConnectionId != mCurrMessageConnectionId) {
			Log.d(TAG, "Received message header for connection id=" + mCurrMessageConnectionId + ", expected " + mConnectionId);
			return;
		}
		//Log.d(TAG, "Handling currently received header: key=" + mGameKey + " and id=" + mCurrMessageId + ", message size =" + mCurrMessageSize);

		if (mCurrMessageSize > 0) {
			mCurrMessage = null; // not really important, but this signals the old message is over and we expect a new one to come
		} else {
			mDataIncomeState = DATA_INCOME_STATE_EXPECT_HEADER;//back to first state, there will no data be sent
			mReceiver.receivePost(mCurrMessageId, "");
			//Log.d(TAG, "Switching back to first state: EXPECT_HEADER since no data is expected, (size=" + mCurrMessageSize + ")");
		}
	}
	
	protected void handleCurrentMessage() {
		mDataIncomeState = DATA_INCOME_STATE_EXPECT_HEADER; // back to the first state if message is finished
		if (mConnectionId != mCurrMessageConnectionId) {
			Log.d(TAG, "Received message for connection id=" + mCurrMessageConnectionId + ", expected " + mConnectionId);
			return; // not for me
		}

		byte[] messageRaw = mCurrMessage;
		String message = new String(messageRaw);
		mReceiver.receivePost(mCurrMessageId, message);
		//Log.d(TAG, "Handling currently received message: key=" + mGameKey + " and id=" + mCurrMessageId + ", message size =" + mCurrMessageSize);
		
	}
	
	private static void intToByte(int i, byte[] buffer, int offset) {
		int intVal = i;
		buffer[offset + 3] = (byte) (intVal & 0xFF);
		intVal >>= 8;
		buffer[offset + 2] = (byte) (intVal & 0xFF);
		intVal >>= 8;
		buffer[offset + 1] = (byte) (intVal & 0xFF);
		intVal >>= 8;
		buffer[offset] = (byte) (intVal & 0xFF);
		intVal >>= 8;
	}

	private static int byteToInt(byte[] buffer, int offset) {
		int i = ((buffer[offset] & 0xFF) << 24);
		i |= ((buffer[offset + 1] & 0xFF) << 16);
		i |= ((buffer[offset + 2] & 0xFF) << 8);
		i |= ((buffer[offset + 3] & 0xFF));
		return i;
	}

	public int getConnectionId() {
		return mConnectionId;
	}
}
