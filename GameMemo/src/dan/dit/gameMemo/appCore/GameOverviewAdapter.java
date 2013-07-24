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
    
    /**
     * A helper DateFormat for subclasses that display the time, e.g. the starttime of a game.
     * Use for a consistent look.
     */
	protected static final DateFormat TIME_FORMAT = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT);
	
	 /**
     * A helper DateFormat for subclasses that display a date, e.g. the startdate of a game.
     * Use for a consistent look.
     */
	protected static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM);
	private static final Calendar CALENDAR_CHECKER1 = Calendar.getInstance();
	private static final Calendar CALENDAR_CHECKER2 = Calendar.getInstance();
	
    protected int layout;
    protected Context context;
    private Map<Long, Long> checked; // mapping id to starttime
    private GameCheckedChangeListener mListener;
    
    /**
     * A listener interface to get notified if a game (or multiple) get(s) checked or unchecked by the user.
     * @author Daniel
     *
     */
    public interface GameCheckedChangeListener {
        /**
         * The user checked or unchecked a game.
         * @param checkedIds A collection of all ids of the checked games. Can be empty.
         */
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

    /**
     * Sets the GameCheckedChangeListener that is invoked when the user checks or unchecks games.
     * @param listener The listener, can be <code>null</code>.
     */
    public void setOnGameCheckedChangeListener(GameCheckedChangeListener listener) {
    	mListener = listener;
    }
    
    private void notifyGameCheckedChanged() {
		if (mListener != null) {
			mListener.onGameCheckedChange(getChecked());
		}
    }
    
    /**
     * Checks all games available.
     */
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
    
    /**
     * Returns all currently checked games' ids.
     * @return The ids of all currently checked games.
     */
    public Set<Long> getChecked() {
    	return checked.keySet();
    }

    /**
     * Returns all currently checked games' starttimes.
     * @return The starttimes of all currently checked games.
     */
	public Collection<Long> getCheckedStarttimes() {
		return checked.values();
	}
	
	/**
	 * Unchecks all currently checked games.
	 */
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

    /**
     * Invoked by the system when a view for a row needs to update.
     * @param row The row view to update. Can be recycled or <code>null</code>, handle with care.
     * @param c The cursor pointing to the data of the row to update.
     */
	protected abstract void updateViewInfo(View row, Cursor c);
	
	/**
	 * Helper method to find out if the given dates are on the same day.
	 * @param first The first Date.
	 * @param second The second Date.
	 * @return If <code>true</code> then the dates are on the same day of the year in the same year.
	 */
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
	
	/**
	 * Returns <code>true</code> if the game with the given id is checked.
	 * @param id The id to test.
	 * @return If the game with the given id is checked.
	 */
	protected boolean isChecked(long id) {
		return checked.containsKey(id);
	}
	
	/**
	 * Helper method for the usual format: When the given date is at the current
	 * day, then 'today' is displayed, or 'yesterday' accordingly, else the date according
	 * to the DATE_FORMAT.
	 * @param date The view that displays the date.
	 * @param startDate The startdate of the game.
	 */
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
	
    /**
     * Returns a new OnLongClickListener that -on a long click-
     * checks all available games if the current game is unchecked
     * or clears all checked if the current game is checked. Requires the clicked
     * CheckBox to hold an Integer as a tag, which is the position of the game in the cursor.
     * @return The listener.
     */
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
	
    /**
     * Returns a new OnClickListener that -on a click-
     * checks the clicked game if it is unchecked
     * or removes it from the checked games if it is checked. Requires the clicked
     * CheckBox to hold an Integer as a tag, which is the position of the game in the cursor.
     * @return The listener.
     */
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
