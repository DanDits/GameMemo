package dan.dit.gameMemo.dataExchange;

import dan.dit.gameMemo.dataExchange.bluetooth.BluetoothExchangeService;

/**
 * An ExchangeService wraps a way to exchange data, for example
 * via bluetooth (see {@link BluetoothExchangeService}) or wlan.
 * The exchange service must be able to send data.<br>
 * Received data is sent to a GameDataExchanger.
 * @author Daniel
 *
 */
public interface ExchangeService {
	
	/**
	 * Sends the given data.
	 * @param data Data to send.
	 */
	void sendData(byte[] data);
	
	/**
	 * Flushes the OutputStream. Call this after a coherent block of data
	 * was sent to the exchange service to make sure it is sent to the target.
	 */
	void flush();
	
	/**
	 * Adds a receiver of incoming data. If <code>null</code> or already contained this method does nothing.
	 * @param postman The receiver of incoming data that will deliver the message to the
	 * actual target.
	 * @return If the postman was successfully added.
	 */
	boolean addInputReceiver(Postman postman);
	
	/**
	 * Removes the receiver of incoming data. If <code>null</code> or not contained this method does
	 * nothing.
	 * @param postman The receiver that is to be removed from the recipient list.
	 * @param closeIfLastReceiver If there is no more receiver registered with this exchange service after the
	 * given was (if possible) removed, the ExchangeService's connection is terminated and closed.
	 * @return If the postman was successfully removed.
	 */
	boolean removeInputReceiver(Postman postman, boolean closeIfLastReceiver);
	

}
