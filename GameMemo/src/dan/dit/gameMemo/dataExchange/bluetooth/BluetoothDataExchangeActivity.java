package dan.dit.gameMemo.dataExchange.bluetooth;

import java.lang.ref.WeakReference;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.dataExchange.GameDataExchanger;
import dan.dit.gameMemo.gameData.game.GameKey;

/**
 * Offers an interface to the bluetooth adapter to discover devices, make oneself
 * discoverable and connect to discovered or bounded devices to start a {@link GameDataExchanger}.
 * This uses a {@link BluetoothExchangeService}.
 * @author Daniel
 *
 */
public class BluetoothDataExchangeActivity extends Activity {
	private static final String TAG = "GameDataExchange";
	private static final String PREFERENCES_ALWAYS_REQUEST_DISCOVERABLE_ON_START= "dan.dit.gameMemo.PREFERENCE_REQUEST_DISCOVERABLE_ON_START";
	private static final int REQUEST_ENABLE_BT = 1;
	private static final int REQUEST_DISCOVERABLE_BT = 2;
	
	// options that could later maybe be made configurable by the user in the context menu
	public static final boolean DEFAULT_OPTION_MAKE_DISCOVERABLE_ON_START = true;
	public static final boolean DEFAULT_CONNECTIVITY_IS_SECURE = true;
	
	// handler message constants
	public static final int MESSAGE_CONNECTION_STATE_CHANGE = 1;
	public static final int MESSAGE_TOAST = 2;
	private static final long DEFAULT_EXCHANGE_START_DELAY = 1500; //ms, smaller than timeout of exchange service

	// a hack that the user is not asked twice on activity start to enable discoverability if preference to do so is set
	// the problem is that the check for discoverability is done too fast and the adapter is not yet discoverable so it is asked again
	private boolean mDoNotAskForDiscoverability; 
	private int[] mGameKeys;
	private MenuItem mOptionToggleDiscoverableOnStart;
	private Button mScanButton;
	private BluetoothExchangeService mExchangeService;
	private TextView mConnectionStatusText;
	private BluetoothAdapter mBtAdapter;
	private ArrayAdapter<BluetoothDevice> mDevicesArrayAdapter;
	private String mLastConnectedDeviceName;
	private GameDataExchanger[] mDataExchangers;
	private boolean mIsStopped;
	
	@Override
	public void onCreate(Bundle savedInstanceData) {
		super.onCreate(savedInstanceData);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.data_exchange);
		setProgressBarIndeterminateVisibility(false);
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		Bundle extras = getIntent().getExtras();
		//TODO only use the given game key(s) as a suggestion and mark them, give possibility to check and uncheck all games in a drop down
		// and only built data exchangers for checked games
		if (savedInstanceData != null) {
			this.mGameKeys = savedInstanceData.getIntArray(GameKey.EXTRA_GAMEKEY);
			if (mGameKeys == null) {
				this.mGameKeys = new int[] {savedInstanceData.getInt(GameKey.EXTRA_GAMEKEY)};
			}
		} else if (extras != null) {
			this.mGameKeys = extras.getIntArray(GameKey.EXTRA_GAMEKEY);
			if (mGameKeys == null) {
				this.mGameKeys = new int[] {extras.getInt(GameKey.EXTRA_GAMEKEY)};
			}
		} else {
			// no game key, what do you want to exchange dude?
			mReceiver = null;
			setResult(RESULT_CANCELED);
			finish();
			return;
		}
        mDataExchangers = new GameDataExchanger[mGameKeys.length];
        // If the adapter is null, then Bluetooth is not supported
        if (mBtAdapter == null) {
            Toast.makeText(this, getResources().getString(R.string.bluetooth_not_available), Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        
        
		// Initialize the button to perform device discovery
        mScanButton = (Button) findViewById(R.id.data_exchange_refresh);
        mScanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	refreshDeviceList(true);
            }
        });
        mConnectionStatusText = (TextView) findViewById(R.id.data_exchange_connection_status_text);
        
        initDeviceList();
	}
	
	@Override
	public void onStart() {
		super.onStart();
		mIsStopped = false;
		// If BT is not on, request that it be enabled.
		// setupExchangeService() will then be called if successfully enabled
		if (!mBtAdapter.isEnabled()) {
			// if handle discoverability returns true bluetooth is already request to be enabled
			if (!handleDiscoverability()) {
				requestEnableBluetooth();
			}
			// Otherwise, setup the exchange service directly
		} else {
			handleDiscoverability();
        	refreshDeviceList(true);
			setupExchangeService();
		}
	}
	
	@Override
	public void onStop() {
		super.onStop();
		mIsStopped = true;
		mDoNotAskForDiscoverability = false;
		
        // Stop the Bluetooth exchange services if not currently connected or connecting
        if (mExchangeService != null 
        		&& mExchangeService.getState() != BluetoothExchangeService.STATE_CONNECTED
        		&& mExchangeService.getState() != BluetoothExchangeService.STATE_CONNECTING) {
        	mExchangeService.stop();
        }
	}
	
	@Override
	public void onBackPressed() {
		if (mBtAdapter != null 
				&& (mBtAdapter.getState() == BluetoothAdapter.STATE_ON || mBtAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON)) {
			// ask user to turn off bluetooth, so he does not forget and can save energy (for planet earth!)
			// but in any case invoke the default back pressed behavior and let the user leave
			new AlertDialog.Builder(this)
 			.setTitle(getResources().getString(R.string.bluetooth_confirm_turn_off_title))
 			.setMessage(getResources().getString(R.string.bluetooth_confirm_turn_off))
 			.setIcon(android.R.drawable.ic_dialog_alert)
 			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

 			    public void onClick(DialogInterface dialog, int whichButton) {
 		 			mBtAdapter.disable();
 					BluetoothDataExchangeActivity.super.onBackPressed();
 			    }})
 			 .setNegativeButton(getResources().getString(R.string.bluetooth_leave_enabled), new DialogInterface.OnClickListener() {
  			    public void onClick(DialogInterface dialog, int whichButton) {
  					BluetoothDataExchangeActivity.super.onBackPressed();
  			    }})
  			 .setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface i) {
  					BluetoothDataExchangeActivity.super.onBackPressed();
				}
  				 
  			 })
  			  .show();
		}
	}
	
	private void requestEnableBluetooth() {
		Intent enableIntent = new Intent(
				BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	}

	private boolean handleDiscoverability() {
		if (!mDoNotAskForDiscoverability && getPreferencesMakeDiscoverableOnStart()) {
			return ensureDiscoverable();
		}
		return false;
	}

	@Override
	public synchronized void onResume() {
		super.onResume();

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity
		// returns.
		if (mExchangeService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't
			// started already
			if (mExchangeService.getState() == BluetoothExchangeService.STATE_NONE) {
				// Start the Bluetooth exchange services
				mExchangeService.start();
			}
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putIntArray(GameKey.EXTRA_GAMEKEY, mGameKeys);
	}
	    
	private void setupExchangeService() {
        // Initialize the BluetoothExchangeService to perform bluetooth connections
		if (mExchangeService != null) {
			return;
		}
		mExchangeService = new BluetoothExchangeService(this, new DataExchangeHandler(this));
		setConnectionStatusText(mExchangeService.getState());
	}
	
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        if (mBtAdapter != null && mReceiver != null) {
        	this.unregisterReceiver(mReceiver);
        }
        
        // Stop the Bluetooth exchange services
        if (mExchangeService != null) mExchangeService.stop();
    }

	private void setConnectionStatusText(int status) {
		switch (status) {
		case BluetoothExchangeService.STATE_NONE:
			mConnectionStatusText.setText(getResources().getString(R.string.data_exchange_connection_status_none));
			break;
		case BluetoothExchangeService.STATE_CONNECTING:
			mConnectionStatusText.setText(getResources().getString(R.string.data_exchange_connection_status_connecting));
			break;
		case BluetoothExchangeService.STATE_CONNECTED:
			mConnectionStatusText.setText(getResources().getString(R.string.data_exchange_connection_status_connected));
			break;
		case BluetoothExchangeService.STATE_LISTEN:
			mConnectionStatusText.setText(getResources().getString(R.string.data_exchange_connection_status_listen));
			break;
		}
	}
	
	 // The Handler that gets information back from the BluetoothChatService
    private static class DataExchangeHandler extends Handler {
		WeakReference<BluetoothDataExchangeActivity> mAct;
    	public DataExchangeHandler(BluetoothDataExchangeActivity pAct) {
    		mAct = new WeakReference<BluetoothDataExchangeActivity>(pAct);
    	}
        @Override
        public void handleMessage(Message msg) {
        	BluetoothDataExchangeActivity act = mAct.get();
        	if (act == null) {
        		Log.e(TAG, "REFERENCE TO ACTIVITY GOT LOST FOR MESSAGE " + msg);
        		return;
        	}
        	// handle messages from the exchange service
            switch (msg.what) {
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
    
    private void onNewConnection(BluetoothDevice device) {
    	 // save the connected device's name
        mLastConnectedDeviceName = device.getName();
        Toast.makeText(this, 
        		getResources().getString(R.string.data_exchange_connected_to)
                       + mLastConnectedDeviceName, Toast.LENGTH_SHORT).show();

        for (int i = 0; i < mGameKeys.length; i++) {
        	mDataExchangers[i] = new GameDataExchanger(getContentResolver(), mExchangeService, mGameKeys[i]);
        	mDataExchangers[i].startExchange(DEFAULT_EXCHANGE_START_DELAY);
        }
        mExchangeService.startTimeoutTimer(BluetoothExchangeService.DEFAULT_TIMEOUT);
    }
    
    private void onConnectionTerminated() {
       	//TODO make a textview that shows a list of all games that are being exchanged:
    	// Tichu: failed  oder Tichu: Sent 10 Received 5
    	// Poker: Sent 5 Received 0
    	//...
		/*GameDataExchanger exch = mDataExchanger;
		mDataExchanger = null;
     	Resources res = getResources();        
		if (exch.exchangeFinishedSuccessfully()) {
         	mSynchStatusText.setText(res.getString(R.string.data_exchange_status_synchronized_with) 
            		+ mLastConnectedDeviceName);
              Toast.makeText(this, 
              		res.getString(R.string.data_exchange_complete) + " " 
                             + res.getString(R.string.data_exchange_games_received) + " " + exch.getGamesReceivedCount()
                             + ", "
                             + res.getString(R.string.data_exchange_games_sent) + " " + exch.getGamesSentCount(), 
                             Toast.LENGTH_LONG).show();
    	} else {
    		String failedSynch = res.getString(R.string.data_exchange_status_synchronizing_failed);
    		mSynchStatusText.setText(failedSynch + mLastConnectedDeviceName);
    		if (exch.getGamesReceivedCount() > 0 || exch.getGamesSentCount() > 0) {
	                Toast.makeText(this, 
	                		failedSynch + " " 
	                               + res.getString(R.string.data_exchange_games_received) + " " + exch.getGamesReceivedCount()
	                               + ", "
	                               + res.getString(R.string.data_exchange_games_sent) + " " + exch.getGamesSentCount(), 
	                               Toast.LENGTH_LONG).show();
    		}
    	}*/
		if (mIsStopped && mExchangeService != null) {
			// connection finished and activity is stopped, stop exchange service
			mExchangeService.stop();
		}
    }
    
    private void initDeviceList() {
    	  // Initialize array adapter for paired and newly discovered devices
    	mDevicesArrayAdapter = new DevicesAdapter(this, R.layout.data_exchange_bluetooth_device);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.data_exchange_devices);
        
        // if no devices found show this in a text
    	TextView emptyView = new TextView(this);
    	emptyView.setText(getResources().getString(R.string.data_exchange_bluetooth_no_devices));
    	pairedListView.setEmptyView(emptyView);
    	
        pairedListView.setAdapter(mDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
        
        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(mReceiver, filter);
    }
    
    private void refreshDeviceList(boolean discover) {
    	mDevicesArrayAdapter.clear();
    	
    	// Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices != null && pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
            	mDevicesArrayAdapter.add(device);
            }
        }
        if (discover) {
        	doDiscovery();
        }
    }
    
    
    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
        	if (mExchangeService.getState() != BluetoothExchangeService.STATE_CONNECTED 
        			&& mExchangeService.getState() != BluetoothExchangeService.STATE_CONNECTING) {
	            // Cancel discovery because it's costly and we're about to connect
	            mBtAdapter.cancelDiscovery();
	            connectDevice(mDevicesArrayAdapter.getItem(pos));
        	}
        }
    };

    // The BroadcastReceiver that listens for discovered devices, for discovery finished and bluetooth state changes
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mDevicesArrayAdapter.add(device);
                }
            // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                Button scanButton = (Button) findViewById(R.id.data_exchange_refresh);
                scanButton.setEnabled(true);
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            	switch (intent.getExtras().getInt(BluetoothAdapter.EXTRA_STATE)) {
            	case BluetoothAdapter.STATE_TURNING_OFF:
            		finish();
            		break;
            	case BluetoothAdapter.STATE_ON:
        			handleDiscoverability();
                	refreshDeviceList(true);
        			setupExchangeService();
        			break;
            	}
            }
        }
    };

	private void doDiscovery() {
		mScanButton.setEnabled(false);
        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
	}
	
    private boolean ensureDiscoverable() {
    	if (mBtAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
    		mDoNotAskForDiscoverability = true;
			Intent enableDiscoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			startActivityForResult(enableDiscoverable, REQUEST_DISCOVERABLE_BT);
			return true;
    	}
    	return false;
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode != Activity.RESULT_OK) {
                // User did not enable Bluetooth or an error occurred
                Toast.makeText(this, getResources().getString(R.string.data_exchange_bluetooth_not_enabled_leaving),
                		Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                finish();
            }
            break;
        case REQUEST_DISCOVERABLE_BT:
        	if (resultCode != Activity.RESULT_CANCELED) {
        		setupExchangeService();
        	} else {
    			setPreferencesMakeDiscoverableOnStart(false); 
        		if (!mBtAdapter.isEnabled()) {
        			// at least request bluetooth if user does not want to be discovered
    				requestEnableBluetooth();
        		}
        	}
        	break;
        }
    }

    private void connectDevice(BluetoothDevice device) {
    	if (device == null 
    			||mExchangeService.getState() == BluetoothExchangeService.STATE_CONNECTING
    			||mExchangeService.getState() == BluetoothExchangeService.STATE_CONNECTED) {
    		return; // already connected or connecting
    	}
        // Attempt to connect to the device
        mExchangeService.connect(device, DEFAULT_CONNECTIVITY_IS_SECURE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.data_exchange_bluetooth, menu);
		mOptionToggleDiscoverableOnStart = menu.findItem(R.id.data_exchange_bluetooth_make_discoverable_on_start);
		mOptionToggleDiscoverableOnStart.setChecked(getPreferencesMakeDiscoverableOnStart());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.data_exchange_bluetooth_ensure_discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
		case R.id.data_exchange_bluetooth_make_discoverable_on_start:
			boolean newState = !item.isChecked();
			setPreferencesMakeDiscoverableOnStart(newState);
			return true;
        }
        return false;
    }
    
	private void setPreferencesMakeDiscoverableOnStart(boolean makeDiscoverable) {
		SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
		if (editor != null) {
			editor.putBoolean(PREFERENCES_ALWAYS_REQUEST_DISCOVERABLE_ON_START, makeDiscoverable);
			if (mOptionToggleDiscoverableOnStart != null) {
				mOptionToggleDiscoverableOnStart.setCheckable(true);
				mOptionToggleDiscoverableOnStart.setChecked(makeDiscoverable);
			}
			editor.commit();
		}
	}
	
	private boolean getPreferencesMakeDiscoverableOnStart() {
		SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
		return sharedPref == null ? DEFAULT_OPTION_MAKE_DISCOVERABLE_ON_START : 
			sharedPref.getBoolean(PREFERENCES_ALWAYS_REQUEST_DISCOVERABLE_ON_START, DEFAULT_OPTION_MAKE_DISCOVERABLE_ON_START);
	}
}
