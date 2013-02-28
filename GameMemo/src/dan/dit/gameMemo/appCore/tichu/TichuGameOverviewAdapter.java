package dan.dit.gameMemo.appCore.tichu;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;
import dan.dit.gameMemo.storage.database.GameSQLiteHelper;
import dan.dit.gameMemo.util.compression.Compressor;

/**
 * This adapter is used display tichu games in a ListView, showing
 * the start time, the two teams and underlining the winner team in case there is a winner.
 * @author Daniel
 *
 */
public class TichuGameOverviewAdapter extends SimpleCursorAdapter {
	private static final DateFormat TIME_FORMAT = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT);
	private static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM);
	private static final Calendar CALENDAR_CHECKER1 = Calendar.getInstance();
	private static final Calendar CALENDAR_CHECKER2 = Calendar.getInstance();
	private static final Random IMAGE_RANDOM = new Random();
	private long highlightedGameId = Game.NO_ID;
    private int layout;

    @SuppressWarnings("deprecation") // needed for support library (cursor loader)
	public TichuGameOverviewAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
    	super(context, layout, c, from, to);
    	this.layout = layout;
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public TichuGameOverviewAdapter (Context context, int layout, Cursor c, String[] from, int[] to, int flag) {
        super(context, layout, c, from, to, flag);
        this.layout = layout;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        Cursor c = getCursor();

        final LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(layout, parent, false);

        updateViewInfo(v, c);

        return v;
    }
    
    @Override
    public void bindView(View v, Context context, Cursor c) {
       updateViewInfo(v, c);
    }
    
    public void setHighlightedGameId(long gameId) {
    	if (gameId != highlightedGameId) {
    		highlightedGameId = gameId;
    		notifyDataSetChanged();
    	}
    }
    
	public long getHighlightedGameId() {
		return highlightedGameId;
	}
    
    private void updateViewInfo(View tichuRow, Cursor c) {
        int playersCol = c.getColumnIndex(GameSQLiteHelper.COLUMN_PLAYERS);
        int starttimeCol = c.getColumnIndex(GameSQLiteHelper.COLUMN_STARTTIME);
        int winnerCol = c.getColumnIndex(GameSQLiteHelper.COLUMN_WINNER);
        int idCol = c.getColumnIndex(GameSQLiteHelper.COLUMN_ID);

        // fetching raw information
        String players = c.getString(playersCol);
        long startTime = c.getLong(starttimeCol);
        int winner = c.getInt(winnerCol);

        // prepare information
        Compressor cmp = new Compressor(players);
        String playerOne = cmp.getData(0);
        String playerTwo = cmp.getData(1);
        String playerThree = cmp.getData(2);
        String playerFour = cmp.getData(3);
       
        Date startDate = new Date(startTime);
        boolean hasWinner = winner != Game.WINNER_NONE;
        boolean team1Wins = winner == TichuGame.WINNER_TEAM1;
           
        TextView time = (TextView) tichuRow.findViewById(R.id.time);
        time.setText(TIME_FORMAT.format(startDate));

        if (!c.isFirst() && c.moveToPrevious()) {
        	Date prevDate = new Date(c.getLong(starttimeCol));
        	CALENDAR_CHECKER1.setTime(startDate);
        	CALENDAR_CHECKER2.setTime(prevDate);
        	boolean sameDay = CALENDAR_CHECKER1.get(Calendar.DAY_OF_YEAR) == CALENDAR_CHECKER2.get(Calendar.DAY_OF_YEAR)
        			&& CALENDAR_CHECKER1.get(Calendar.YEAR) == CALENDAR_CHECKER2.get(Calendar.YEAR);
            TextView date = (TextView) tichuRow.findViewById(R.id.date);
        	if (!sameDay) {
                date.setText(DATE_FORMAT.format(startDate));
        	} else {
        		date.setText("");
        	}
        	c.moveToNext();
        }
        
        TextView team1 = (TextView) tichuRow.findViewById(R.id.tichu_overview_team1);
        team1.setText(new StringBuilder(20).append(playerOne).append('\n').append(playerTwo).toString());

        TextView team2 = (TextView) tichuRow.findViewById(R.id.tichu_overview_team2);
        team2.setText(new StringBuilder(20).append(playerThree).append('\n').append(playerFour).toString());
        
        int team1Image;
        int team2Image;
        if (hasWinner) {
        	team1Image = team1Wins ? R.drawable.tichu_mahjong_raw : R.drawable.tichu_dog_raw;
        	team2Image = team1Wins ? R.drawable.tichu_dog_raw : R.drawable.tichu_mahjong_raw;
        } else {
        	// maybe load the game if not yet finished (since that will never be too many games) and check wo is currently leading in score and use dragon for leader
        	boolean dragonLeft = IMAGE_RANDOM.nextBoolean();
        	team1Image = dragonLeft ? R.drawable.tichu_dragon_translucent : R.drawable.tichu_dragon_translucent;
        	team2Image = dragonLeft ? R.drawable.tichu_dragon_translucent : R.drawable.tichu_dragon_translucent;
        }
		team1.setCompoundDrawablesWithIntrinsicBounds(team1Image, 0, 0, 0);
		team2.setCompoundDrawablesWithIntrinsicBounds(0, 0, team2Image, 0);
        
        long id = c.getLong(idCol);
        if (id == highlightedGameId) {
        	tichuRow.setBackgroundResource(R.drawable.tichu_overview_game_selection);
        } else {
        	tichuRow.setBackgroundResource(0);
        }
        
    }
}
