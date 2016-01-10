package dan.dit.gameMemo.appCore.numberInput;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.GameKey;


public class OperationAdapter extends ArrayAdapter<Operation> {
    private int mGameKey;
    
    public OperationAdapter(Context context, int gameKey, List<Operation> ops) {
        super(context, R.layout.number_input_operation, R.id.op_name, ops);
        mGameKey = gameKey;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        TextView textView = null;
        if (v == null) {
            v = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.number_input_operation, parent, false);
            GameKey.applyTheme(mGameKey, getContext().getResources(), v);
            TextView temp = (TextView) v.findViewById(R.id.op_name);
            GameKey.applyThemeTextColor(mGameKey, getContext().getResources(), temp);
        }
        textView = (TextView) v.findViewById(R.id.op_name);
        Operation op = getItem(position);
        ImageView iconView = (ImageView) v.findViewById(R.id.op_icon);
        if (op.getIconResId() != 0) {
            iconView.setImageResource(op.getIconResId());
            iconView.setVisibility(View.VISIBLE);
        } else {
            iconView.setVisibility(View.GONE);
        }
        textView.setText(op.getName(getContext().getResources()));
        
        return v;
    }
}
