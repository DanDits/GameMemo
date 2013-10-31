package dan.dit.gameMemo.appCore.statistics;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.statistics.GameStatistic;
import dan.dit.gameMemo.gameData.statistics.StatisticAttribute;


public class AdvancedStatisticsAdapter extends ArrayAdapter<StatisticAttribute> {
    private Context mContext;
    private boolean mAllDisabled;
    private StatisticAttribute mAttr;
    
    public AdvancedStatisticsAdapter(Context context, List<StatisticAttribute> attrs) {
        super(context, 0, attrs);
        mContext = context;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.statistic_list_item, parent, false);
        }
        TextView text = (TextView)view.findViewById(android.R.id.text1);
        StatisticAttribute attr = getItem(position);
        if (attr == null) {
            text.setText("");
            return view;
        }
        if (attr instanceof GameStatistic) {
            text.setTextColor(Color.GREEN);
        } else {
            text.setTextColor(Color.BLUE);
        }
        if (isLocked(position)) {
            ((ImageView) view.findViewById(R.id.locked_state)).setImageResource(R.drawable.locked);
        } else {
            ((ImageView) view.findViewById(R.id.locked_state)).setImageDrawable(null);
        }
        CharSequence name = attr.getName(mContext.getResources());
        text.setText(name);
        return view;

    }
    
    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }
    
    public void setAttribute(StatisticAttribute attr) {
        mAttr = attr;
        notifyDataSetChanged();
    }
    
    private boolean isLocked(int position) {
        if (mAllDisabled) {
            return true;
        } else if (mAttr == null) {
            return false;
        }
        StatisticAttribute itemAtPos = getItem(position);
        return !mAttr.getOwnAttributes().contains(itemAtPos);
    }
    
    @Override
    public boolean isEnabled(int position) {
        return !isLocked(position);
    }
    
    public void setAllItemsDisabled(boolean disable) {
        if (disable != mAllDisabled) {
            mAllDisabled = disable;
            notifyDataSetChanged();
        }        
    }
}
