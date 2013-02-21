package dan.dit.gameMemo.dataExchange.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.dataExchange.ExchangeService;
import dan.dit.gameMemo.dataExchange.Postman;
/*
 * for debugging (multiple logcats)
 * in console window 1: adb -s <device01_serial> logcat -v time TAG:D *:S
 * in console window 2: adb -s <device02_serial> logcat -v time TAG:D *:S
 */
@SuppressLint("NewApi")
public final class BluetoothExchangeService  implements ExchangeService {
    private static final String TAG = "GameDataExchange";
    
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
	

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothExchangeSecure";
    private static final String NAME_INSECURE = "BluetoothExchangeInsecure";

    // Unique UUID for this application, created by http://www.famkruithof.net/uuid/uuidgen
    private static final UUID MY_UUID_SECURE =
        UUID.fromString("c354cf50-6033-11e2-bcfd-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
        UUID.fromString("c354cf51-6033-11e2-bcfd-0800200c9a66");

    // Member fields
    private List<Postman> mDataReceiver;
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

	
    /**
     * Constructor. Prepares a new BluetoothExchangeService session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothExchangeService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
        mDataReceiver = new LinkedList<Postman>();
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
    	int oldState = mState;
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        Message msg = mHandler.obtainMessage(BluetoothDataExchangeActivity.MESSAGE_CONNECTION_STATE_CHANGE, state, oldState);
        if (mConnectedThread != null && mState == STATE_CONNECTED) {
        	msg.obj = mConnectedThread.mmSocket.getRemoteDevice();
        }
        mHandler.sendMessage(msg);
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BluetoothDataExchangeActivity.MESSAGE_TOAST, R.string.bluetooth_unable_to_connect, -1);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothExchangeService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity. Restarts the bluetooth service.
     */
    private void connectionLost() {
    	if (mState == STATE_CONNECTED || mState == STATE_CONNECTING) {
	        Log.d(TAG, "Connection to device lost.");
	        // Start the service over to restart listening mode
	        BluetoothExchangeService.this.start();
    	}
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure":"Insecure";

            // Create a new listening server socket
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD_MR1 || secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                        MY_UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED && mmServerSocket != null) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothExchangeService.this) {
                        switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                            connected(socket, socket.getRemoteDevice(),
                                    mSocketType);
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }

        }

        public void cancel() {
        	if (mmServerSocket != null) {
	            try {
	                mmServerSocket.close();
	            } catch (IOException e) {
	                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
	            }
        	}
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD_MR1 || secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothExchangeService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            if (mmInStream == null || mmOutStream == null) {
            	Log.d(TAG, "Streams not successfully created for connection, closing. (" + mmInStream + ", " + mmOutStream);
            	BluetoothExchangeService.this.connectionLost();
            }
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes = -1;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    onReadData(buffer, bytes);
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    BluetoothExchangeService.this.connectionLost();
                    break;
                }
            	if (bytes <= 0) {
            		SystemClock.sleep(100);
            	}
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
        
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

	@Override
	public void sendData(byte[] data) {
		write(data);
	}

	private void onReadData(byte[] buffer, int bytes) {
		for (Postman receiver : mDataReceiver) {
			receiver.onReceiveData(buffer, bytes);
		}
	}

	private void terminateConnection() {
		if (mState == STATE_CONNECTED || mState == STATE_CONNECTING) {
			BluetoothExchangeService.this.stop(); // stops threads
		} // else there is no connection to terminate this service knows about
	}

	@Override
	public void flush() {
		if (mConnectedThread != null) {
			try {
				mConnectedThread.mmOutStream.flush();
			} catch (IOException e) {
				Log.d(TAG, "IOExeption flushing connected output stream: " + e);
			}
		}
	}

	@Override
	public synchronized boolean addInputReceiver(Postman postman) {
		if (postman != null && !mDataReceiver.contains(postman)) {
			return mDataReceiver.add(postman);
		}
		return false;
	}
	
	@Override
	public synchronized boolean removeInputReceiver(Postman postman, boolean closeIfLastReceiver) {
		boolean result = false;
		if (postman != null) {
			result = mDataReceiver.remove(postman);
			if (result) {
				mHandler.obtainMessage(BluetoothDataExchangeActivity.MESSAGE_EXCHANGE_COMPLETE, postman.getConnectionId(), -1, null).sendToTarget();
			} else {
				return false;
			}
		}
		assert result == true;
		if (closeIfLastReceiver && mDataReceiver.size() == 0) {
			terminateConnection();
		}
		return true;
	}
}
