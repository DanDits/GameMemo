package dan.dit.gameMemo.appCore.tichu;

import java.util.Date;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameOverviewAdapter;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compaction.Compacter;

/**
 * This adapter is used display tichu games in a ListView, showing
 * the start time, the two teams and underlining the winner team in case there is a winner.
 * @author Daniel
 *
 */
public class TichuGameOverviewAdapter extends GameOverviewAdapter {
	public static final String[] REQUIRED_COLUMNS = new String[] { GameStorageHelper.COLUMN_PLAYERS, GameStorageHelper.COLUMN_STARTTIME,
		GameStorageHelper.COLUMN_WINNER, GameStorageHelper.COLUMN_ID};
	private static final int[] COLUMN_MAPPING = new int[REQUIRED_COLUMNS.length];
	
	public TichuGameOverviewAdapter(Context context, int layout, Cursor c) {
    	super(context, layout, c, REQUIRED_COLUMNS, COLUMN_MAPPING);
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public TichuGameOverviewAdapter (Context context, int layout, Cursor c,  int flag) {
        super(context, layout, c, REQUIRED_COLUMNS, COLUMN_MAPPING, flag);
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
        holder.checker.setOnClickListener(getNewCheckedClickListener());
        holder.checker.setOnLongClickListener(getNewCheckedLongClickListener());
        v.setTag(holder);
        updateViewInfo(v, c);

        return v;
    }
    
    @Override
    protected void updateViewInfo(View tichuRow, Cursor c) {
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
        holder.checker.setChecked(isChecked(id));
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
            team1Image = R.drawable.tichu_phoenix;        		
            team2Image = R.drawable.tichu_phoenix;	
        }
        holder.team1.setImageResource(team1Image);
        holder.team2.setImageResource(team2Image);     
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
}
