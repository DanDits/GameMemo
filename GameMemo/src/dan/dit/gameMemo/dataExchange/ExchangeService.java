package dan.dit.gameMemo.dataExchange;

import dan.dit.gameMemo.dataExchange.Postman.PostRecipient;
import dan.dit.gameMemo.dataExchange.bluetooth.BluetoothExchangeService;

/**
 * An ExchangeService wraps a way to exchange data, for example
 * via bluetooth (see {@link BluetoothExchangeService}) or wlan.
 * The exchange service must be able to send data.<br>
 * Received data is sent to a GameDataExchanger.
 * @author Daniel
 *
 */
public interface ExchangeService  {
	
	/**
	 * Sends the given data.
	 * @param data Data to send.
	 */
	void sendData(byte[] data);
	
	void sendPost(int connectionId, int messageId, String message);
	
	void onNewPost(int connectionId, int messageId, String message);
	
	void registerRecipient(int connectionId, PostRecipient receiver);
	
	void unregisterRecipient(PostRecipient receiver);
	
	/**
	 * Flushes the OutputStream. Call this after a coherent block of data
	 * was sent to the exchange service to make sure it is sent to the target.
	 */
	void flush();

	void startTimeoutTimer(long defaultTimeout);	

}
