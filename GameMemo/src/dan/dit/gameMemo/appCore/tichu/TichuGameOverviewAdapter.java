package dan.dit.gameMemo.appCore.tichu;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.Game.GamesDeletionListener;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

/**
 * This adapter is used display tichu games in a ListView, showing
 * the start time, the two teams and underlining the winner team in case there is a winner.
 * @author Daniel
 *
 */
public class TichuGameOverviewAdapter extends SimpleCursorAdapter implements GamesDeletionListener {
	private static final DateFormat TIME_FORMAT = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT);
	private static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM);
	private static final Calendar CALENDAR_CHECKER1 = Calendar.getInstance();
	private static final Calendar CALENDAR_CHECKER2 = Calendar.getInstance();
    private int layout;
    private Context context;
    private Map<Long, Long> checked; // mapping id to starttime
    private GameCheckedChangeListener mListener;
    
    public interface GameCheckedChangeListener {
    	void onGameCheckedChange(Collection<Long> checkedIds);
    }

    @SuppressWarnings("deprecation") // needed for support library (cursor loader)
	public TichuGameOverviewAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
    	super(context, layout, c, from, to);
    	this.context = context;
    	this.layout = layout;
    	checked = new HashMap<Long, Long>();
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public TichuGameOverviewAdapter (Context context, int layout, Cursor c, String[] from, int[] to, int flag) {
        super(context, layout, c, from, to, flag);
        this.layout = layout;
        this.context = context;
    	checked = new HashMap<Long, Long>();
    }

    public void setOnGameCheckedChangeListener(GameCheckedChangeListener listener) {
    	mListener = listener;
    }
    
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        Cursor c = getCursor();

        final LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(layout, parent, false);
        ViewHolder holder = new ViewHolder();
        holder.team1_1 = (TextView) v.findViewById(R.id.tichu_overview_team1_1);
        holder.team1_2 = (TextView) v.findViewById(R.id.tichu_overview_team1_2);
        holder.team2_1 = (TextView) v.findViewById(R.id.tichu_overview_team2_1);
        holder.team2_2 = (TextView) v.findViewById(R.id.tichu_overview_team2_2);
        holder.team1 = (ImageView) v.findViewById(R.id.tichu_overview_team1);
        holder.team2 = (ImageView) v.findViewById(R.id.tichu_overview_team2);
        holder.time = (TextView) v.findViewById(R.id.time);
        holder.date = (TextView) v.findViewById(R.id.date);
        holder.checker = (CheckBox) v.findViewById(R.id.selected_checker);
        holder.checker.setOnClickListener(new OnClickListener() {
			
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
		});
        holder.checker.setOnLongClickListener(new OnLongClickListener() {
			
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
 		});
        v.setTag(holder);
        updateViewInfo(v, c);

        return v;
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
    
    private void updateViewInfo(View tichuRow, Cursor c) {
        int playersCol = c.getColumnIndex(GameStorageHelper.COLUMN_PLAYERS);
        int starttimeCol = c.getColumnIndex(GameStorageHelper.COLUMN_STARTTIME);
        int winnerCol = c.getColumnIndex(GameStorageHelper.COLUMN_WINNER);
        int idCol = c.getColumnIndex(GameStorageHelper.COLUMN_ID);

        // fetching raw information
        String players = c.getString(playersCol);
        long startTime = c.getLong(starttimeCol);
        int winner = c.getInt(winnerCol);
        long id = c.getLong(idCol);

        // prepare information
        Compacter cmp = new Compacter(players);
        String playerOne = cmp.getData(0);
        String playerTwo = cmp.getData(1);
        String playerThree = cmp.getData(2);
        String playerFour = cmp.getData(3);
       
        Date startDate = new Date(startTime);
        boolean hasWinner = winner != Game.WINNER_NONE;
        boolean team1Wins = winner == TichuGame.WINNER_TEAM1;
        ViewHolder holder = (ViewHolder) tichuRow.getTag();
        
        // checked state
        holder.checker.setChecked(checked.containsKey(id));
        holder.checker.setTag(c.getPosition());
        
        // show the start time
        holder.time.setText(TIME_FORMAT.format(startDate));

        // display the start date if not on the same day like the previous game, display 'today' or 'yesterday' instead of the current/previous date
        if (!c.isFirst() && c.moveToPrevious()) {
        	Date prevDate = new Date(c.getLong(starttimeCol));
        	if (!isSameDate(startDate, prevDate)) {
        		applyDate(holder.date , startDate);
        	} else {
        		holder.date.setText("");
        	}
        	c.moveToNext();
        } else {
        	applyDate(holder.date, startDate);
        }
        
        // show players names
        holder.team1_1.setText(playerOne);
        holder.team1_2.setText(playerTwo);
        holder.team2_1.setText(playerThree);
        holder.team2_2.setText(playerFour);
        
        // show images giving an indication of the game, like the score leader or winner
        int team1Image = 0;
        int team2Image = 0;
        if (hasWinner) {
        	team1Image = team1Wins ? R.drawable.tichu_mahjong : R.drawable.tichu_dog;
        	team2Image = team1Wins ? R.drawable.tichu_dog : R.drawable.tichu_mahjong;
        } else {
        	List<Game> game = null;
			try {
				game = TichuGame.loadGames(context.getContentResolver(), GameStorageHelper.getUri(GameKey.TICHU, id), false);
			} catch (CompactedDataCorruptException e) {
				assert false; // will not throw
			}
        	if (game != null && game.size() > 0) {
        		TichuGame tichuGame = (TichuGame) game.get(0);
        		if (tichuGame.getScoreTeam1() > tichuGame.getScoreTeam2()) {
                	team1Image =  R.drawable.tichu_dragon;
                	team2Image = R.drawable.tichu_phoenix;
        		} else if (tichuGame.getScoreTeam1() < tichuGame.getScoreTeam2()) {
                	team1Image =  R.drawable.tichu_phoenix;
                	team2Image = R.drawable.tichu_dragon;
        		} else {
                	team1Image = R.drawable.tichu_dragon;        		
                	team2Image = R.drawable.tichu_dragon;	
        		}
        	}
        }
        holder.team1.setImageResource(team1Image);
        holder.team2.setImageResource(team2Image);     
    }
    
    private void applyDate(TextView date, Date startDate) {
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

	private static boolean isSameDate(Date first, Date second) {
    	CALENDAR_CHECKER1.setTime(first);
    	CALENDAR_CHECKER2.setTime(second);
    	return CALENDAR_CHECKER1.get(Calendar.DAY_OF_YEAR) == CALENDAR_CHECKER2.get(Calendar.DAY_OF_YEAR)
    			&& CALENDAR_CHECKER1.get(Calendar.YEAR) == CALENDAR_CHECKER2.get(Calendar.YEAR);
    }
	
	private static class ViewHolder {
		 TextView team1_1;
		 TextView team1_2;
		 TextView team2_1;
		 TextView team2_2;
		 ImageView team1;
		 ImageView team2;
		 TextView date;
		 TextView time;
		 CheckBox checker;
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
}
