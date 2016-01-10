package dan.dit.gameMemo.dataExchange;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.dataExchange.GamesOverviewDialog.GamesOverviewDialogCallback;
import dan.dit.gameMemo.gameData.game.GameKey;

/**
 * An abstract activity for {@link ExchangeService}s to exchange game data. Offers various methods
 * that may be hooked for different events of the exchange. The way to establish and present the connection is left
 * for the subclass.
 * @author Daniel
 *
 */
public abstract class DataExchangeActivity extends FragmentActivity implements
		GamesOverviewDialogCallback {
	private static final String TAG = DataExchangeActivity.class.getName();
	protected static final String EXTRA_ALL_GAMES = "dan.dit.gameMemo.ALL_GAMES"; //int[] with at least one gamekey
	protected static final String EXTRA_SINGLE_GAME_OFFERS = "dan.dit.gameMemo.SINGLE_GAME_OFFERS"; // long[] if only one game suggested these starttimes are used to limit the offer of this game, others will send all
	private static final String STORAGE_SINGLE_GAME_ID = "dan.dit.gameMemo.SINGLE_GAME_KEY"; // key of the single game
	// handler message constants
	public static final int MESSAGE_CONNECTION_STATE_CHANGE = 1; // used to update the text that displays the state, new state id in arg1
	public static final int MESSAGE_TOAST = 2; // if arg1 != -1 loads string resource with this id, else displays obj as CharSequence 
	public static final int MESSAGE_DATA_EXCHANGER_CLOSED = 3; // if a single data exchanger finished (successfully or not), game key in arg1
	public static final int MESSAGE_CONNECTION_LOST = 4; // exchange service lost connection
	public static final int MESSAGE_NEW_CONNECTION = 5; // exchange service started a new connection
	
	protected GamesExchangeManager mManager;
	protected Handler mHandler;
	private int mSingleGameKey = GameKey.NO_GAME;
	private long[] mSingleGameOfferedStarttimes;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras = getIntent().getExtras();

		int[] gameKeySuggestions = null;
		if (savedInstanceState != null) {
			gameKeySuggestions = savedInstanceState
					.getIntArray(GameKey.EXTRA_GAMEKEY);
			mSingleGameOfferedStarttimes = savedInstanceState.getLongArray(EXTRA_SINGLE_GAME_OFFERS);
			if (mSingleGameOfferedStarttimes != null && mSingleGameOfferedStarttimes.length > 0 && gameKeySuggestions != null 
					&& gameKeySuggestions.length == 1 && GameKey.isGameSupported(gameKeySuggestions[0])) {
				mSingleGameKey = savedInstanceState.getInt(STORAGE_SINGLE_GAME_ID, GameKey.NO_GAME);
			}
		} else if (extras != null) {
			gameKeySuggestions = extras.getIntArray(GameKey.EXTRA_GAMEKEY);
			mSingleGameOfferedStarttimes = extras.getLongArray(EXTRA_SINGLE_GAME_OFFERS);
			if (mSingleGameOfferedStarttimes != null && mSingleGameOfferedStarttimes.length > 0 && gameKeySuggestions != null 
					&& gameKeySuggestions.length == 1 && GameKey.isGameSupported(gameKeySuggestions[0])) {
				mSingleGameKey = gameKeySuggestions[0];
			}
		}
		mManager = new GamesExchangeManager(getSupportFragmentManager(),
				gameKeySuggestions, extras != null ? extras.getIntArray(EXTRA_ALL_GAMES) : null, getResources());
		if (GameKey.isGameSupported(mSingleGameKey)) {
			List<Long> asList = new ArrayList<Long>(mSingleGameOfferedStarttimes.length);
			for (long starttime : mSingleGameOfferedStarttimes) {
				asList.add(Long.valueOf(starttime));
			}
			mManager.setOffer(mSingleGameKey, asList);
		}
		mHandler = new DataExchangeHandler(this);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		mManager.setGamesExchangeView((GamesExchangeView) findViewById(R.id.gamesExchangeView));
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putIntArray(GameKey.EXTRA_GAMEKEY, mManager.getSelectedGames());
		outState.putLongArray(EXTRA_SINGLE_GAME_OFFERS, mSingleGameOfferedStarttimes);
		outState.putInt(STORAGE_SINGLE_GAME_ID, mSingleGameKey);
	}

	// The Handler that gets information back from the ExchangeService
	private static class DataExchangeHandler extends Handler {
		WeakReference<DataExchangeActivity> mAct;

		public DataExchangeHandler(DataExchangeActivity pAct) {
			mAct = new WeakReference<DataExchangeActivity>(pAct);
		}

		@Override
		public void handleMessage(Message msg) {
			DataExchangeActivity act = mAct.get();
			if (act == null) {
				Log.e(TAG, "REFERENCE TO ACTIVITY GOT LOST FOR MESSAGE " + msg);
				return;
			}
			// handle messages from the exchange service
			switch (msg.what) {
			case MESSAGE_DATA_EXCHANGER_CLOSED:
				act.onDataExchangerClosed(msg.arg1);
				break;
			case MESSAGE_CONNECTION_STATE_CHANGE:
				act.setConnectionStatusText(msg.arg1);
				break;
			case MESSAGE_CONNECTION_LOST:
				act.onConnectionTerminated(act.mManager.getSuccessfullyExchangedGames());
				break;
			case MESSAGE_NEW_CONNECTION:
				act.onNewConnection(msg.obj);
				break;
			case MESSAGE_TOAST:
				CharSequence text = null;
				if (msg.arg1 != -1) {
					text = act.getApplicationContext().getResources()
							.getString(msg.arg1);
				} else if (msg.obj instanceof CharSequence) {
					text = (CharSequence) msg.obj;
				}
				if (text != null) {
					Toast.makeText(
							act.getApplicationContext(),
							text, Toast.LENGTH_SHORT).show();
				}
				break;
			}
		}
	};

	/**
	 * A data exchanger was closed and unregistered from the ExchangeService. This can
	 * happen during the connection or afterwards. Offers visual feedback for this progress.
	 * @param gameKey The game key of the closed data exchanger.
	 */
	protected void onDataExchangerClosed(int gameKey) {
		mManager.finishedDataExchanger(); // for visual feedback only
	}

	/**
	 * The connection got terminated. This will close all pending data exchangers. Presentation
	 * of this termination is left to the subclass. Do not forget to call the parent method.
	 * @param successfullyExchanged The amount of successfully exchanged game types.
	 */
	protected void onConnectionTerminated(int successfullyExchanged) {
		mManager.closeAll();
	}

	/**
	 * There is a new connection with the given connection object. The interpretation of the
	 * object and if there really can be a connection with this object is left to the subclass.
	 * If this is not the case, do not invoke the parent method.
	 * @param connectionObject The object (a device,...) a new connection is established with.
	 */
	protected void onNewConnection(Object connectionObject) {
		mManager.startExchange(getExchangeService(), getContentResolver());
	}

	/**
	 * Returns the exchange service.
	 * @return The exchange service.
	 */
	protected abstract ExchangeService getExchangeService();
	
	/**
	 * The connection state changed. The visual representation and interpretation of the state code
	 * is left to the subclass.
	 * @param newState The state id of the new state.
	 */
	protected abstract void setConnectionStatusText(int newState);

	@Override
	public GamesExchangeManager getManager() {
		return mManager;
	}
	
}
