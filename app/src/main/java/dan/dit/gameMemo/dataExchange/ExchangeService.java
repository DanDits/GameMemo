package dan.dit.gameMemo.dataExchange;

import dan.dit.gameMemo.dataExchange.Postman.PostRecipient;
import dan.dit.gameMemo.dataExchange.bluetooth.BluetoothExchangeService;

/**
 * An ExchangeService wraps a way to exchange data, for example
 * via bluetooth (see {@link BluetoothExchangeService}) or wlan.
 * The exchange service must be able to send data binary data.<br>
 * Received data is sent to a PostRecipient registered for the connection
 * id of the received message. So the protocol being used for ExchangeService is
 * that of a {@link Postman}. Methods must be thread safe!
 * @author Daniel
 *
 */
public interface ExchangeService  {
	
	/**
	 * Sends the given data over the stream. Do not use directly but only by
	 * the Postman so that data is structured.
	 * @param data Data to send.
	 */
	void sendData(byte[] data);
	
	/**
	 * Sends a post with this exchange service.
	 * @param connectionId The connection id of this post.
	 * @param messageId The message id of this post.
	 * @param message The actual message. Can be empty.
	 */
	void sendPost(int connectionId, int messageId, String message);
	
	/**
	 * A new post is received. This post is then sent to the registered
	 * recipient, if any for this connection id.
	 * @param connectionId The connection id of the message.
	 * @param messageId The message id.
	 * @param message The actual message.
	 */
	void onNewPost(int connectionId, int messageId, String message);
	
	/**
	 * Registers a PostRecipient for a connection id. This will overwrite any previous
	 * recipient for this connection id.
	 * @param connectionId The connection id the receiver is interested in.
	 * @param receiver The receiver.
	 */
	void registerRecipient(int connectionId, PostRecipient receiver);
	
	/**
	 * Unregisters the receiver from the exchange service. No further messages will be received.
	 * Does nothing if not registered.
	 * @param receiver The receiver to unregister.
	 */
	void unregisterRecipient(PostRecipient receiver);
	
	/**
	 * Flushes the OutputStream. Call this after a coherent block of data
	 * was sent to the exchange service to make sure it is sent to the target.
	 */
	void flush();

	/**
	 * Starts the timeout timer of the exchange service. If there is no in or out
	 * activity, the connection is terminated after the given duration.
	 * @param defaultTimeout The timeout duration >0 in ms.
	 */
	void startTimeoutTimer(long defaultTimeout);	

}
