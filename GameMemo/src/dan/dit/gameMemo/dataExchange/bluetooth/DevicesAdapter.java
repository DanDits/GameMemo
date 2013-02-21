package dan.dit.gameMemo.dataExchange.bluetooth;

import dan.dit.gameMemo.R;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class DevicesAdapter extends ArrayAdapter<BluetoothDevice> {
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
