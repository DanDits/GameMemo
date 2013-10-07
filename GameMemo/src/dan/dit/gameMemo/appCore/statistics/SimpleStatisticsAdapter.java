package dan.dit.gameMemo.appCore.statistics;

import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import dan.dit.gameMemo.gameData.statistics.GameStatistic;

public class SimpleStatisticsAdapter extends ArrayAdapter<GameStatistic> {
    private List<GameStatistic> mAll;
    private Context mContext;
    
    public SimpleStatisticsAdapter(Context context, List<GameStatistic> attrs) {
        super(context, android.R.layout.simple_dropdown_item_1line, 
                android.R.id.text1, attrs);
        mAll = attrs;
        mContext = context;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView text = (TextView)view.findViewById(android.R.id.text1);
        text.setTextColor(Color.BLACK);
        CharSequence name = mAll.get(position).getName(mContext.getResources());
        text.setText(name);
        return view;

    }
    
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = getView(position, convertView, parent);
        return view;

    }

    public void sort() {
        Collections.sort(mAll);
        notifyDataSetChanged();
    }
}
