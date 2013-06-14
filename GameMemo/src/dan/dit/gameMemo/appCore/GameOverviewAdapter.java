package dan.dit.gameMemo.appCore;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.CheckBox;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.Game.GamesDeletionListener;
import dan.dit.gameMemo.storage.GameStorageHelper;

public abstract class GameOverviewAdapter extends SimpleCursorAdapter implements
		GamesDeletionListener {
	protected static final DateFormat TIME_FORMAT = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT);
	protected static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM);
	private static final Calendar CALENDAR_CHECKER1 = Calendar.getInstance();
	private static final Calendar CALENDAR_CHECKER2 = Calendar.getInstance();
    protected int layout;
    protected Context context;
    private Map<Long, Long> checked; // mapping id to starttime
    private GameCheckedChangeListener mListener;
    
    public interface GameCheckedChangeListener {
    	void onGameCheckedChange(Collection<Long> checkedIds);
    }

    @SuppressWarnings("deprecation") // needed for support library (cursor loader)
	public GameOverviewAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
    	super(context, layout, c, from, to);
    	this.context = context;
    	this.layout = layout;
    	checked = new HashMap<Long, Long>();
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public GameOverviewAdapter (Context context, int layout, Cursor c, String[] from, int[] to, int flag) {
        super(context, layout, c, from, to, flag);
        this.layout = layout;
        this.context = context;
    	checked = new HashMap<Long, Long>();
    }

    public void setOnGameCheckedChangeListener(GameCheckedChangeListener listener) {
    	mListener = listener;
    }
    
    private void notifyGameCheckedChanged() {
		if (mListener != null) {
			mListener.onGameCheckedChange(getChecked());
		}
    }
    
    
    public void checkAll() {
    	Cursor data = getCursor();
    	int pos = data.getPosition();
    	data.moveToFirst();
    	while (!data.isAfterLast()) {
    		checked.put(data.getLong(data.getColumnIndex(GameStorageHelper.COLUMN_ID)), 
    				data.getLong(data.getColumnIndex(GameStorageHelper.COLUMN_STARTTIME)));
    		data.moveToNext();
    	}
    	data.moveToPosition(pos);
		notifyDataSetChanged();
    }
    
    public Set<Long> getChecked() {
    	return checked.keySet();
    }

	public Collection<Long> getCheckedStarttimes() {
		return checked.values();
	}
	
    public void clearChecked() {
    	checked.clear();
    	notifyDataSetChanged();
    }
    
    @Override
    public void bindView(View v, Context context, Cursor c) {
       updateViewInfo(v, c);
    }
    
    @Override
    public boolean hasStableIds() {
    	return true;
    }

	protected abstract void updateViewInfo(View row, Cursor c);
	
	protected static boolean isSameDate(Date first, Date second) {
    	CALENDAR_CHECKER1.setTime(first);
    	CALENDAR_CHECKER2.setTime(second);
    	return CALENDAR_CHECKER1.get(Calendar.DAY_OF_YEAR) == CALENDAR_CHECKER2.get(Calendar.DAY_OF_YEAR)
    			&& CALENDAR_CHECKER1.get(Calendar.YEAR) == CALENDAR_CHECKER2.get(Calendar.YEAR);
    }
	
	@Override
	public void deletedGames(Collection<Long> deletedIds) {
		boolean oneWasChecked = false;
		for (long id : deletedIds) {
			oneWasChecked |= checked.remove(Long.valueOf(id)) != null;
		}
		if (oneWasChecked) {
			notifyDataSetChanged();
		}
	}
	
	protected boolean isChecked(long id) {
		return checked.containsKey(id);
	}
	
    protected void applyDate(TextView date, Date startDate) {
    	Date today = new Date();
		if (isSameDate(today, startDate)) {
			date.setText(context.getResources().getString(R.string.today));
		} else {
			Calendar yesterday = Calendar.getInstance();
			yesterday.add(Calendar.DATE, -1);
			if (isSameDate(yesterday.getTime(), startDate)) {
    			date.setText(context.getResources().getString(R.string.yesterday));        				
			} else {
                date.setText(DATE_FORMAT.format(startDate));        				
			}
		}
	}
	
	protected OnLongClickListener getNewCheckedLongClickListener() {
		return new OnLongClickListener() {
		
			@Override
			public boolean onLongClick(View v) {
				CheckBox box = (CheckBox) v;
				Integer pos = (Integer) box.getTag();
			Cursor cursor = getCursor();
			int oldPos = cursor.getPosition();
			cursor.moveToPosition(pos);
			Long id = cursor.getLong(cursor.getColumnIndex(GameStorageHelper.COLUMN_ID));
				if (checked.containsKey(id)) {
					clearChecked();
				} else {
					checkAll();
				}
			notifyGameCheckedChanged();
				cursor.moveToPosition(oldPos);
				return true;
			}
		};
	}
	
	protected OnClickListener getNewCheckedClickListener() {
		return new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				CheckBox box = (CheckBox) v;
				Integer pos = (Integer) box.getTag();
				Cursor cursor = getCursor();
				int oldPos = cursor.getPosition();
				cursor.moveToPosition(pos);
				Long id = cursor.getLong(cursor.getColumnIndex(GameStorageHelper.COLUMN_ID));
				if (checked.containsKey(id)) {
					checked.remove(id);
					box.setChecked(false);
				} else {
					Long starttime = cursor.getLong(cursor.getColumnIndex(GameStorageHelper.COLUMN_STARTTIME));
					checked.put(id, starttime);
					box.setChecked(true);
				}
				notifyGameCheckedChanged();
				cursor.moveToPosition(oldPos);
			}
		};
	}
}
