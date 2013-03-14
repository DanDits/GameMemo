package dan.dit.gameMemo.dataExchange;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import dan.dit.gameMemo.dataExchange.Postman.PostRecipient;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameBuilder;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.storage.database.GameSQLiteHelper;
import dan.dit.gameMemo.util.compression.CompressedDataCorruptException;
import dan.dit.gameMemo.util.compression.Compressor;

// class made for one time use only, after closing the instance must be deleted and cannot be used anymore
// always close to free resources
// Exchange protocol:
//	-offering its own data for the given gameKey to the given service
//	-on offer, sending needed part of data that is needed back to service, requesting service to send it
//	-on request, sending all requested data to service
//	-on send: simply store given data permanently
//	-if on request and on send fulfilled or otherwise finished or cannot continue: unregister from exchanger

public class GameDataExchanger implements PostRecipient {
	private static final String TAG = "GameDataExchange";

	// message ids 
	private static final int MESSAGE_ID_OFFERING_DATA = 1;
	private static final int MESSAGE_ID_REQUESTING_DATA = 2;
	private static final int MESSAGE_ID_SENDING_DATA = 3;
	
	// member vars	
	private ContentResolver mContentResolver;
	private ExchangeService mService; 
	
	private int mGameKey;
	private int mGamesSentCount;
	private int mGamesReceivedCount;
	private List<Long> mOwnStarttimes;
	private List<Long> mOwnUnfinishedGamesStarttimes;
	private boolean mPartnerOfferReceived;
	private boolean mPartnerRequestSatisfied;
	private boolean mOwnRequestDataReceived;

	
	public GameDataExchanger(ContentResolver resolver, ExchangeService service, int gameKey) {
		mContentResolver = resolver;
		mPartnerRequestSatisfied = mOwnRequestDataReceived = mPartnerOfferReceived = false;
		mGameKey = gameKey;
		mService = service;
		mService.registerRecipient(mGameKey, this);
		if (service == null || resolver == null) {
			throw new NullPointerException();
		}
		if (!GameKey.isGameSupported(gameKey)) {
			throw new IllegalArgumentException("Game key not supported " + gameKey);
		}
	}
	
	public final int getGameKey() {
		return mGameKey;
	}

	public boolean isClosed() {
		return mService == null;
	}
	
	public synchronized void receivePost(int messageId, String message) {
		if (isClosed()) {
			assert false;
			return; // i am closed
		}
		// receivePost is always invoked in an extra thread
		if (message == null || message.length() == 0) {
			// handle receiving of no data messages
			switch(messageId) {
			case MESSAGE_ID_OFFERING_DATA:
				// other got nothing to offer, so we will not receive anything
				mOwnRequestDataReceived = true;
				mPartnerOfferReceived = true;
				break;
			case MESSAGE_ID_REQUESTING_DATA:
				// other does not want anything
				mPartnerRequestSatisfied = true;
				break;
			case MESSAGE_ID_SENDING_DATA:
				// well... theoretically my requested data could not be received, but since other will not sent more than this, we will say ok
				mOwnRequestDataReceived = true;
				break;
			default:
				// I don't even understand what you tell me
				close();
				break;
			}
		} else {
			// there is data available
			switch (messageId) {
			case MESSAGE_ID_OFFERING_DATA:
				if (!mOwnRequestDataReceived && !mPartnerOfferReceived) {
					onReceiveOffer(message);
				} else {
					close(); // I already got your offer or I got everything I wanted, leave me alone
				}
				break;
			case MESSAGE_ID_REQUESTING_DATA:
				if (!mPartnerRequestSatisfied) {
					onReceiveRequest(message);
				} else {
					close(); // already satisfied, what else do you want from me?
				}
				break;
			case MESSAGE_ID_SENDING_DATA:
				if (!mOwnRequestDataReceived) {
					onReceiveGames(message);
				} else {
					close(); // I got everything I wanted, thanks, thats enough
				}
				break;
			default:
				// I don't even understand what you tell me
				close();
				break;
			}
		}
		if (isExchangeCompleteCondition()) {
			close();
		}
	}

	
	private void sendMessage(int messageId, String message) {
		if (message == null || message.length() == 0) {
			//Log.d(TAG, "Sending empty message : key = " + mGameKey + " and id = " + messageId);
			// handle sending of no data messages
			switch(messageId) {
			case MESSAGE_ID_OFFERING_DATA:
				// we got nothing to offer, so partner will not request anything and we will not send anything and he is 'satisfied'
				mPartnerRequestSatisfied = true;
				break;
			case MESSAGE_ID_REQUESTING_DATA:
				// we do not want anything, so we will not get anything and we are 'satisfied'
				mOwnRequestDataReceived = true;
				break;
			case MESSAGE_ID_SENDING_DATA:
				// we are sending no data, this satisfies our partners needs from our point of view
				mPartnerRequestSatisfied = true;
				break;
			default:
				// I don't even understand what I'm doing, critical program error
				close();
				Log.e(TAG, "Sending message even I myself do not understand: id=" + messageId);
				break;
			}
		}
		mService.sendPost(mGameKey, messageId, message);
		if (isExchangeCompleteCondition()) {
			close();
		}
	}
	
	private synchronized boolean isExchangeCompleteCondition() {
		return mPartnerRequestSatisfied && mOwnRequestDataReceived;
	}
	
	public synchronized void close() {
		if (mService != null) {
			mService.unregisterRecipient(this);
			mService = null;
		}
	}

	private void onReceiveRequest(String message) {	
		List<Long> timestamps = timesFromString(message);
		// load the games that are requested and that we actually have
		List<Game> gamesToSend = GameKey.loadGames(mGameKey, mContentResolver, timestamps);
		sendGames(gamesToSend);
		// sending data complete
		mPartnerRequestSatisfied = true;
	}
	
	private void onReceiveGames(String message) {
		String compressedGames = new String(message);
		Compressor gamesCmp = new Compressor(compressedGames);
		buildOwnStarttimes(); // even though I can assert that they are already built at this point (we sent a request to receive data)
		Game curr;
		for (String compressedGameString : gamesCmp) {
			curr = null;
			GameBuilder builder = GameKey.getBuilder(mGameKey);
			try {
				builder.loadAll(new Compressor(compressedGameString));
				curr = builder.build();
			} catch (CompressedDataCorruptException e) {
				// received data corrupt, no game
				Log.d(TAG, "Error creating game from received data : " + e.getMessage() + "\nCorruptData=" + e.getCorruptData());
			}
			// make sure the game could be created and we do not already have it
			if (curr != null) {
				if (!mOwnStarttimes.contains(Long.valueOf(curr.getStartTime()))) {
					// we do not yet have this game, save it
					curr.saveGame(mContentResolver);
					Log.d(TAG, "Saving game newly." + curr.toString());
					mGamesReceivedCount++;
				} else if (mOwnUnfinishedGamesStarttimes.contains(Long.valueOf(curr.getStartTime()))) {
					// already has the game, but unfinished, so overwrite the own game instead of newly saving it (chances are it is now finished)
					long unfinishedId = Game.getUnfinishedGame(mGameKey, mContentResolver, curr.getStartTime());
					if (Game.isValidId(unfinishedId)) { // should always be the case since unfinished games is a subset of all own start times
						Log.d(TAG, "Overwriting game." + curr.toString());
						if (curr.saveGame(mContentResolver, unfinishedId)) {
							mGamesReceivedCount++; // saving is always successful since the id is valid and curr game does not yet have an id
						}
					} else {
						Log.e(TAG, "Received already existing and finished game, do not save: " + curr.toString());
					}
				}
			}
		}
		mOwnRequestDataReceived = true;
	}
	
	private synchronized void buildOwnStarttimes() {
		if (mOwnStarttimes == null) {
			mOwnStarttimes = new ArrayList<Long>();
			mOwnUnfinishedGamesStarttimes = new ArrayList<Long>();
			String[] projection = {GameSQLiteHelper.COLUMN_STARTTIME, GameSQLiteHelper.COLUMN_WINNER };
			Uri uri = GameStorageHelper.getUriAllItems(mGameKey);
			if (uri == null) {
				close(); // game key not supported
				return;
			}
			Cursor cursor = mContentResolver.query(uri, projection, null, null,
					null);
			if (cursor != null) {
				cursor.moveToFirst();
				while (!cursor.isAfterLast()) {
					long startTime = cursor.getLong(cursor
							.getColumnIndexOrThrow(GameSQLiteHelper.COLUMN_STARTTIME));
					mOwnStarttimes.add(startTime);
					if (cursor.getInt(cursor.getColumnIndexOrThrow(GameSQLiteHelper.COLUMN_WINNER)) == Game.WINNER_NONE) {
						mOwnUnfinishedGamesStarttimes.add(startTime);
					}
					cursor.moveToNext();
				}
				cursor.close();
			}
		}
	}
	
	private void onReceiveOffer(String message) {
		mPartnerOfferReceived = true;
		List<Long> receivedStarttimes = timesFromString(message);
		// now check which of the received start times are not contained in the own start times or if that game is not yet finished and request those
		buildOwnStarttimes(); // if not yet build, get them
		List<Long> wantedStartTimes = new LinkedList<Long>();
		for (Long received : receivedStarttimes) {
			if ((!mOwnStarttimes.contains(received) || mOwnUnfinishedGamesStarttimes.contains(received))) {
				wantedStartTimes.add(received);
			}
		}
		requestData(wantedStartTimes);
	}
	
	private void requestData(List<Long> requestedStartTimes) {
		String message = timesToString(requestedStartTimes);
		sendMessage(MESSAGE_ID_REQUESTING_DATA, message);
	}

	private synchronized void offerOwnData() {
		buildOwnStarttimes();
		String message = timesToString(mOwnStarttimes);
		sendMessage(MESSAGE_ID_OFFERING_DATA, message);
	}
	
	private void sendGames(List<Game> games) {
		Compressor gamesCompressor = new Compressor(games.size());
		for (Game g : games) {
			gamesCompressor.appendData(g.compress());
		}
		mGamesSentCount += games.size();
		String gamesCompressed = gamesCompressor.compress();
		String message = null;
		if (gamesCompressed.length() > 0) {
			message = gamesCompressed;
		}
		sendMessage(MESSAGE_ID_SENDING_DATA, message);
		mPartnerRequestSatisfied = true; // even though we do not know if partner received data, it is ok to expect it
	}

	public void startExchange(long delay) {
		Timer delayedStart = new Timer();
		delayedStart.schedule(new TimerTask() {

			@Override
			public void run() {
				offerOwnData();
			}
			
		}, delay);
	}
	
	public int getGamesSentCount() {
		return mGamesSentCount;
	}
	
	public int getGamesReceivedCount() {
		return mGamesReceivedCount;
	}
	
	public boolean exchangeFinishedSuccessfully() {
		return isExchangeCompleteCondition();
	}
	
	public static List<Long> timesFromString(String compressedTimes) {
		Compressor cmp = new Compressor(compressedTimes);
		List<Long> startTimes = new ArrayList<Long>(cmp.getSize());
		for (String s : cmp) {
			Long l = null;
			try {
				l = Long.parseLong(s);
			} catch (NumberFormatException nfe) {}
			if (l != null) {
				startTimes.add(l);
			}
		}
		return startTimes;
	}

	public static String timesToString(List<Long> startTimes) {
		if (startTimes == null || startTimes.size() == 0) {
			return "";
		}
		Compressor starttimesCompressor = new Compressor(startTimes.size());
		for (Long l : startTimes) {
			starttimesCompressor.appendData(l.toString());
		}
		return starttimesCompressor.compress();
	}

}
