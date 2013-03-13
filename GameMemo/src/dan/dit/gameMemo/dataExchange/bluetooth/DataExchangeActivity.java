package dan.dit.gameMemo.dataExchange.bluetooth;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import dan.dit.gameMemo.dataExchange.ExchangeService;
import dan.dit.gameMemo.dataExchange.GameDataExchanger;
import dan.dit.gameMemo.gameData.game.GameKey;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

public abstract class DataExchangeActivity extends Activity {
	private static final String TAG = DataExchangeActivity.class.getName();
	private static final long DEFAULT_EXCHANGE_START_DELAY = 1500; //ms, must be smaller than any service timout duration TODO ensure this by DOC or sth
	
	// handler message constants
	public static final int MESSAGE_CONNECTION_STATE_CHANGE = 1;
	public static final int MESSAGE_TOAST = 2;
	public static final int MESSAGE_DATA_EXCHANGER_CLOSED = 3;
	
	protected ExchangeService mExchangeService;
	protected int[] mGameKeySuggestions; 
	protected List<Integer> mSelectedGames;
	protected SparseArray<GameDataExchanger> mDataExchangers;
	protected Handler mHandler;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras = getIntent().getExtras();

		if (savedInstanceState != null) {
			mGameKeySuggestions = savedInstanceState.getIntArray(GameKey.EXTRA_GAMEKEY);
		} else if (extras != null) {
			mGameKeySuggestions = extras.getIntArray(GameKey.EXTRA_GAMEKEY);
		}
        int[] allGames = GameKey.ALL_GAMES;
        if (mGameKeySuggestions == null) {
        	mGameKeySuggestions = new int[allGames.length];
        	System.arraycopy(allGames, 0, mGameKeySuggestions, 0, allGames.length);
        }
		//TODO only use the given game key(s) as a suggestion and mark them, give possibility to check and uncheck all games in a drop down
		// and only built data exchangers for checked games
        mDataExchangers = new SparseArray<GameDataExchanger>(allGames.length);
        mSelectedGames = new LinkedList<Integer>();
        for (int gameKey : mGameKeySuggestions) {
        	mSelectedGames.add(gameKey);
        }
        mHandler = new DataExchangeHandler(this);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putIntArray(GameKey.EXTRA_GAMEKEY, mGameKeySuggestions);
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
            	if (act.mExchangeService != null 
            			&& msg.arg2 == BluetoothExchangeService.STATE_CONNECTED ) {
            		// we lost a previous connection 
            		act.onConnectionTerminated();
            	}
            	if (act.mExchangeService != null && msg.arg1 == BluetoothExchangeService.STATE_CONNECTED) {
            		// we are now connected to a remote device
            		BluetoothDevice connectedTo = (BluetoothDevice) msg.obj;
                   act.onNewConnection(connectedTo);
            	}
            	act.setConnectionStatusText(msg.arg1);
                break;
            case MESSAGE_TOAST:
                Toast.makeText(act.getApplicationContext(),act.getApplicationContext().getResources().getString(msg.arg1),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    protected abstract void onDataExchangerClosed(int gameKey);
    protected abstract void onConnectionTerminated();
    protected  void onNewConnection(Object connectionObject) {
    	 mDataExchangers.clear();
         for (int gameKey : mSelectedGames) {
         	GameDataExchanger exchanger = new GameDataExchanger(getContentResolver(), mExchangeService, gameKey);
         	mDataExchangers.put(gameKey, exchanger);
         	exchanger.startExchange(DEFAULT_EXCHANGE_START_DELAY);
         }
         mExchangeService.startTimeoutTimer(BluetoothExchangeService.DEFAULT_TIMEOUT);
    }
    
    protected abstract void setConnectionStatusText(int newState);
}
