package dan.dit.gameMemo.dataExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Bridge class to allow sending ExchangeMessages over a binary
 * stream. An ExchangeService will usually only require one Postman. Concurrency
 * is not handled by this class but must be done by the using class. A message
 * is built by passing data read from an InputStream to onReceiveData and will
 * trigger onNewPost() of the ExchangeService as soon as a new post arrived completely.
 * Requires the ExchangeService's binary sendData(data) method.
 * @author Daniel
 *
 */
public class Postman {
	// states how incoming data is expected for a single message (HEADER=(KEY, ID, SIZE)[, DATA])
	private static final int HEADER_SIZE = 12; // in bytes, here a multiple of 4 since data is stored in ints
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
	private ExchangeService mTarget;
	
	public interface PostRecipient {
		void receivePost(int messageId, String message);
	}
	
	public Postman(ExchangeService target) {
		mTarget = target;
		if (target == null) {
			throw new NullPointerException("ExchangeService null.");
		}
		mDataIncomeState = DATA_INCOME_STATE_EXPECT_HEADER;
		mBytesReceived = new ByteArrayOutputStream(512);
	}
	
	public void onReceiveData(byte[] data, int dataCount) {
		if (dataCount <= 0) {
			return; // no data received, you lied!
		}
		//Log.d(TAG, "Received " + dataSize + " bytes via bluetooth.");
		saveReceivedBytes(data, 0, dataCount);
		while (processData()) {
			//Log.d(TAG, "Data processed. Waiting for " + ((mDataIncomeState == DATA_INCOME_STATE_EXPECT_DATA) ? "DATA" : "HEADER"));
		}
	}
	
	public void sendPost(int connectionId, int messageId, String message) {
		byte[] header = new byte[HEADER_SIZE];
		intToByte(connectionId, header, 0);
		intToByte(messageId, header, 4);
		intToByte(0, header, 8); // default message size is 0
		if (message != null && message.length() > 0) {
			//Log.d(TAG, "Sending message : key=" + mGameKey + " and id=" + messageId + ", message size =" + message.length);
			byte[] messageInBytes = message.getBytes();
			int messageSize = messageInBytes.length;
			intToByte(messageSize, header, 8); // updating actual message size which is >0
			mTarget.sendData(header); 
			mTarget.sendData(messageInBytes);
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
	
	private boolean processData() {
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
			throw new IllegalStateException("Expecting state invalid for postman with message state " + mDataIncomeState);
		}
	}
	
	private void processHeader() {
		mDataIncomeState = DATA_INCOME_STATE_EXPECT_DATA;
		if (mCurrMessageSize > 0) {
			mCurrMessage = null; // not really important, but this signals the old message is over and we expect a new one to come
		} else {
			mDataIncomeState = DATA_INCOME_STATE_EXPECT_HEADER;//back to first state, there will no data be sent
			mTarget.onNewPost(mCurrMessageConnectionId, mCurrMessageId, "");
			//Log.d(TAG, "Switching back to first state: EXPECT_HEADER since no data is expected, (size=" + mCurrMessageSize + ")");
		}
	}
	
	private void handleCurrentMessage() {
		mDataIncomeState = DATA_INCOME_STATE_EXPECT_HEADER; // back to the first state if message is finished
		byte[] messageRaw = mCurrMessage;
		String message = new String(messageRaw);
		mTarget.onNewPost(mCurrMessageConnectionId, mCurrMessageId, message);
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
}
