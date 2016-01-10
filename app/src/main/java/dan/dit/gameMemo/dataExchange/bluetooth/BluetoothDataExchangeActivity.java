package dan.dit.gameMemo.dataExchange.bluetooth;

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
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.dataExchange.DataExchangeActivity;
import dan.dit.gameMemo.dataExchange.ExchangeService;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.util.ActivityUtil;

/**
 * A special implementation of a {@link DataExchangeActivity}.
 * Offers an interface to the bluetooth adapter to discover devices, make oneself
 * discoverable and connect to discovered or bounded devices.
 * This uses a {@link BluetoothExchangeService} for wireless communication over bluetooth.
 * <br>On startup if bluetooth is not enabled the user is asked to do so, also if wanted he
 * is asked to become discoverable to other devices. This can also be done in the menu manually.<br>
 * Also offers a possibility to refresh the devices list, fetching already bonded devices and discovering new
 * ones.
 * @author Daniel
 *
 */
public class BluetoothDataExchangeActivity extends DataExchangeActivity {
	private static final String STORAGE_ACTIVITY_ENABLED_BLUETOOTH = "ACTIVITY_ENABLED_BLUETOOTH";
	private static final String PREFERENCES_ALWAYS_REQUEST_DISCOVERABLE_ON_START= "dan.dit.gameMemo.PREFERENCE_REQUEST_DISCOVERABLE_ON_START";
	private static final int REQUEST_ENABLE_BT = 1;
	private static final int REQUEST_DISCOVERABLE_BT = 2;
	
	// the default option for the preference to make discoverable when enabling bluetooth on start
	public static final boolean DEFAULT_OPTION_MAKE_DISCOVERABLE_ON_START = true; 
	// insecure bluetooth connections require API level 10!
	public static final boolean DEFAULT_CONNECTIVITY_IS_SECURE = true; 
	

	// a hack that the user is not asked twice on activity start to enable discoverability if preference to do so is set
	// the problem is that the check for discoverability is done too fast and the adapter is not yet discoverable so it is asked again
	private boolean mDoNotAskForDiscoverability; 
	
	// ui component references
	private MenuItem mOptionToggleDiscoverableOnStart;
	private BluetoothExchangeService mExchangeService;
	private TextView mConnectionStatusText;
	private BluetoothAdapter mBtAdapter;
	private ArrayAdapter<BluetoothDevice> mDevicesArrayAdapter;
	private String mLastConnectedDeviceName;
	private boolean mIsStopped;
	private boolean mActivityEnabledBluetooth;
	
	public static Intent newInstance(Context packageContext, int[] gameKeys) {
		Intent i = new Intent(packageContext, BluetoothDataExchangeActivity.class);
		i.putExtra(GameKey.EXTRA_GAMEKEY, gameKeys);
		return i;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceData) {
		super.onCreate(savedInstanceData);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        ActivityUtil.applyUncaughtExceptionHandler(this);
		setContentView(R.layout.data_exchange_bluetooth);
		setProgressBarIndeterminateVisibility(false);
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		
        // If the adapter is null, then Bluetooth is not supported
        if (mBtAdapter == null) {
            Toast.makeText(this, getResources().getString(R.string.bluetooth_not_available), Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        if (savedInstanceData != null) {
        	mActivityEnabledBluetooth = savedInstanceData.getBoolean(STORAGE_ACTIVITY_ENABLED_BLUETOOTH);
        }
        mConnectionStatusText = (TextView) findViewById(R.id.data_exchange_connection_status_text);
        try {
            initDeviceList();
        } catch (SecurityException se) {
            // user managed to remove bluetooth permission, well, cannot do my job then
            Toast.makeText(this, getResources().getString(R.string.bluetooth_permission_required), Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
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
		if (mBtAdapter != null && mActivityEnabledBluetooth
				&& (mBtAdapter.getState() == BluetoothAdapter.STATE_ON || mBtAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON)) {
			// ask user to turn off bluetooth, so he does not forget and can save energy (for planet earth!)
			// but in any case invoke the default back pressed behavior and let the user leave
			new AlertDialog.Builder(this)
 			.setTitle(getResources().getString(R.string.bluetooth_confirm_turn_off_title))
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
		} else {
			super.onBackPressed();
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
	    
	private void setupExchangeService() {
        // Initialize the BluetoothExchangeService to perform bluetooth connections
		if (mExchangeService != null) {
			return;
		}
		mExchangeService = new BluetoothExchangeService(mHandler);
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

    @Override
	protected void setConnectionStatusText(int status) {
		switch (status) {
		case BluetoothExchangeService.STATE_NONE:
		    mConnectionStatusText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			mConnectionStatusText.setText(getResources().getString(R.string.data_exchange_connection_status_none));
			break;
		case BluetoothExchangeService.STATE_CONNECTING:
            mConnectionStatusText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.bluetooth_connected, 0, 0, 0);
			mConnectionStatusText.setText(getResources().getString(R.string.data_exchange_connection_status_connecting));
			break;
		case BluetoothExchangeService.STATE_CONNECTED:
            mConnectionStatusText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.bluetooth_connected, 0, 0, 0);
			mConnectionStatusText.setText(getResources().getString(R.string.data_exchange_connection_status_connected));
			break;
		case BluetoothExchangeService.STATE_LISTEN:
            mConnectionStatusText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.bluetooth, 0, 0, 0);
			mConnectionStatusText.setText(getResources().getString(R.string.data_exchange_connection_status_listen));
			break;
		}
	}
	
    @Override
    protected void onNewConnection(Object device) {
    	if (!(device instanceof BluetoothDevice)) {
    		return; // not connected to a bluetooth device, so this is not a new connection
    	}
        mBtAdapter.cancelDiscovery();
    	super.onNewConnection(device);
    	BluetoothDevice btDevice = (BluetoothDevice) device;
    	 // save the connected device's name
        mLastConnectedDeviceName = btDevice.getName();
        if (mLastConnectedDeviceName == null) {
            mLastConnectedDeviceName = "Unknown device";
        }
        Toast.makeText(this, 
        		getResources().getString(R.string.data_exchange_connected_to, 
        				mLastConnectedDeviceName), Toast.LENGTH_SHORT).show();
       
    }
    
    @Override
    protected void onConnectionTerminated(int successfullyExchanged) {
    	super.onConnectionTerminated(successfullyExchanged);
    	Toast.makeText(this, getResources().getQuantityString(R.plurals.data_exchange_finished, successfullyExchanged,
    			successfullyExchanged, mLastConnectedDeviceName), 
    			Toast.LENGTH_LONG).show();
		if (mIsStopped && mExchangeService != null) {
			// connection finished and activity is stopped, stop exchange service
			mExchangeService.stop();
		}
    }
    
    private static class DevicesAdapter extends ArrayAdapter<BluetoothDevice> {
    	private Context context;
    	public DevicesAdapter(Context context, int textViewResourceId) {
    		super(context, textViewResourceId);
    		this.context = context;
    	}

    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		View row = super.getView(position, convertView, parent);
    		BluetoothDevice device = (BluetoothDevice) getItem(position);
    		String bondState = device.getBondState() == BluetoothDevice.BOND_NONE ? "" 
    				: (" (" + context.getResources().getString(R.string.bluetooth_devices_state_bonded) + ")");
    		((TextView) row).setText(device.getAddress() + " " + device.getName() +  bondState);
    		return row;
    	}
    }
    
    private void initDeviceList() {
    	  // Initialize array adapter for paired and newly discovered devices
    	mDevicesArrayAdapter = new DevicesAdapter(this, R.layout.data_exchange_bluetooth_device);

        // Find and set up the ListView for devices
        ListView devicesListView = (ListView) findViewById(R.id.bluetooth_devices_list); 
        
        // if no devices found show this in a text
    	TextView emptyView = new TextView(this);
    	emptyView.setText(getResources().getString(R.string.data_exchange_bluetooth_no_devices));
    	devicesListView.setEmptyView(emptyView);
    	
    	devicesListView.setAdapter(mDevicesArrayAdapter);
    	devicesListView.setOnItemClickListener(mDeviceClickListener);

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
        if (discover && mExchangeService != null 
        		&& mExchangeService.getState() != BluetoothExchangeService.STATE_CONNECTED 
        		&& mExchangeService.getState() != BluetoothExchangeService.STATE_CONNECTING) {
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
                setConnectionStatusText(mExchangeService.getState());
            // Bluetooth adapter changes state
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            	switch (intent.getExtras().getInt(BluetoothAdapter.EXTRA_STATE)) {
            	case BluetoothAdapter.STATE_TURNING_OFF:
            		finish();
            		break;
            	case BluetoothAdapter.STATE_ON:
            		mActivityEnabledBluetooth = true;
        			handleDiscoverability();
                	refreshDeviceList(true);
        			setupExchangeService();
        			break;
            	}
            }
        }
    };

	private void doDiscovery() {
        // Indicate scanning in the title and by icon
        setProgressBarIndeterminateVisibility(true);
        mConnectionStatusText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.bluetooth_searching, 0, 0, 0);
        
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
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	outState.putBoolean(STORAGE_ACTIVITY_ENABLED_BLUETOOTH, mActivityEnabledBluetooth);
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
		case R.id.refresh:
        	refreshDeviceList(true);
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
	        ActivityUtil.commitOrApplySharedPreferencesEditor(editor);
		}
	}
	
	private boolean getPreferencesMakeDiscoverableOnStart() {
		SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
		return sharedPref == null ? DEFAULT_OPTION_MAKE_DISCOVERABLE_ON_START : 
			sharedPref.getBoolean(PREFERENCES_ALWAYS_REQUEST_DISCOVERABLE_ON_START, DEFAULT_OPTION_MAKE_DISCOVERABLE_ON_START);
	}

	@Override
	protected ExchangeService getExchangeService() {
		return mExchangeService;
	}
}
