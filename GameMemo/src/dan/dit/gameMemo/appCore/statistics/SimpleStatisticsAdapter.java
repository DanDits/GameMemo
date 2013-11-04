package dan.dit.gameMemo.appCore.statistics;

import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import dan.dit.gameMemo.gameData.statistics.StatisticAttribute;

public class SimpleStatisticsAdapter extends ArrayAdapter<StatisticAttribute> {
    private List<StatisticAttribute> mAll;
    private Context mContext;
    
    public SimpleStatisticsAdapter(Context context, List<StatisticAttribute> attrs) {
        super(context, android.R.layout.simple_spinner_dropdown_item,
                android.R.id.text1, attrs);
        mAll = attrs;
        mContext = context;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView text = (TextView)view.findViewById(android.R.id.text1);
        text.setTextColor(Color.BLACK);
        StatisticAttribute attr = mAll.get(position);
        if (attr == null) {
            text.setText("");
        } else {
            CharSequence name = attr.getName(mContext.getResources());
            if (attr.isUserAttribute()) {
                text.setText(name);
            } else {
                text.setText("<" + name + ">");
            }
        }
        return view;
    }
    
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    public void sort() {
        Collections.sort(mAll);
        notifyDataSetChanged();
    }
}
