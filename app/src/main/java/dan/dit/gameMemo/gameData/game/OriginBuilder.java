package dan.dit.gameMemo.gameData.game;

import java.util.ArrayList;
import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.os.Build;

public class OriginBuilder {
    private static final OriginBuilder INSTANCE = new OriginBuilder();
    
    public static OriginBuilder getInstance() {
        return INSTANCE;
    }
    
    public List<String> getOriginHints() {
        ArrayList<String> hints = new ArrayList<String>(2);
        hints.add(getBluetoothDeviceName());
        hints.add(Build.MODEL);
        return hints;
    }
    
    private String getBluetoothDeviceName() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            return adapter.getName();
        } else {
            return "";
        }
    }   
}
