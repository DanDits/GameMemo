package dan.dit.gameMemo.dataExchange;

import java.lang.ref.WeakReference;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;
import dan.dit.gameMemo.dataExchange.GamesOverviewDialog.GamesOverviewDialogCallback;
import dan.dit.gameMemo.gameData.game.GameKey;

public abstract class DataExchangeActivity extends FragmentActivity implements
		GamesOverviewDialogCallback {
	private static final String TAG = DataExchangeActivity.class.getName();

	// handler message constants
	public static final int MESSAGE_CONNECTION_STATE_CHANGE = 1; // used to update the text that displays the state, new state id in arg1
	public static final int MESSAGE_TOAST = 2; // if arg1 != -1 loads string resource with this id, else displays obj as CharSequence 
	public static final int MESSAGE_DATA_EXCHANGER_CLOSED = 3; // if a single data exchanger finished (successfully or not), game key in arg1
	public static final int MESSAGE_CONNECTION_LOST = 4; // exchange service lost connection
	public static final int MESSAGE_NEW_CONNECTION = 5; // exchange service started a new connection
	
	protected GamesExchangeManager mManager;
	protected ExchangeService mExchangeService;
	protected Handler mHandler;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras = getIntent().getExtras();

		int[] gameKeySuggestions = null;
		if (savedInstanceState != null) {
			gameKeySuggestions = savedInstanceState
					.getIntArray(GameKey.EXTRA_GAMEKEY);
		} else if (extras != null) {
			gameKeySuggestions = extras.getIntArray(GameKey.EXTRA_GAMEKEY);
		}
		mManager = new GamesExchangeManager(getSupportFragmentManager(),
				gameKeySuggestions);
		mHandler = new DataExchangeHandler(this);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putIntArray(GameKey.EXTRA_GAMEKEY, mManager.getSelectedGames());
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
				act.onConnectionTerminated();
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

	protected void onDataExchangerClosed(int gameKey) {
		mManager.finishedDataExchanger(); // for visual feedback only
	}

	protected void onConnectionTerminated() {
		mManager.closeAll();
	}

	protected void onNewConnection(Object connectionObject) {
		mManager.startExchange(mExchangeService, getContentResolver());
	}

	protected abstract void setConnectionStatusText(int newState);

	@Override
	public GamesExchangeManager getManager() {
		return mManager;
	}
	
	public void setGamesExchangeView(GamesExchangeView view) {
		mManager.setGamesExchangeView(view);
	}
}
