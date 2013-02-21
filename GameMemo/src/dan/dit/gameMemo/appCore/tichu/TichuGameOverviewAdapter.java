package dan.dit.gameMemo.appCore.tichu;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
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
        DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);
        boolean hasWinner = winner != Game.WINNER_NONE;
        boolean team1Wins = winner == TichuGame.WINNER_TEAM1;
           
        TextView team1_text = (TextView) tichuRow.findViewById(R.id.tichu_row_team1);
        if (team1_text != null) {
        	team1_text.setText(playerOne + ", " + playerTwo);
        }
        TextView team2_text = (TextView) tichuRow.findViewById(R.id.tichu_row_team2);
        if (team2_text != null) {
        	team2_text.setText(playerThree + ", " + playerFour);
        }
        makeUnderlined(team1Wins ? team1_text : team2_text, hasWinner);
        TextView startTime_text = (TextView) tichuRow.findViewById(R.id.tichu_row_starttime);
        if (startTime_text != null) {
        	startTime_text.setText(dateFormat.format(startDate));
        }
        
        long id = c.getLong(idCol);
        if (id == highlightedGameId) {
        	tichuRow.setBackgroundColor(Color.BLUE);
        } else {
        	tichuRow.setBackgroundColor(Color.TRANSPARENT);
        }
        
    }
    
    private void makeUnderlined(TextView view, boolean underlined) {
    	if (underlined) {
	    	CharSequence viewText = view.getText();
	    	SpannableString content = new SpannableString(viewText);
	    	content.setSpan(new UnderlineSpan(), 0, viewText.length(), 0);
	    	view.setText(content);
    	} else {
    		view.setText(view.getText().toString());
    	}
    }
}
