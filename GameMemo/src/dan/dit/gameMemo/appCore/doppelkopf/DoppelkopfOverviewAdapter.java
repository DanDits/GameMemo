package dan.dit.gameMemo.appCore.doppelkopf;

import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameOverviewAdapter;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfGame;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

public class DoppelkopfOverviewAdapter extends GameOverviewAdapter {
	public static final String[] REQUIRED_COLUMNS = new String[] { GameStorageHelper.COLUMN_PLAYERS, GameStorageHelper.COLUMN_STARTTIME,
				GameStorageHelper.COLUMN_WINNER, GameStorageHelper.COLUMN_ID, GameStorageHelper.COLUMN_METADATA};
	private static final int[] COLUMN_MAPPING = new int[REQUIRED_COLUMNS.length];
	
	private static final int COLOR_PLAYER_IS_WINNER = 0xFFDF7401;
	private static int COLOR_PLAYER_IS_NO_WINNER = 0xFF000000;

	public DoppelkopfOverviewAdapter(Context context, int layout, Cursor c, int flag) {
		super(context, layout, c, REQUIRED_COLUMNS, COLUMN_MAPPING, flag);
	}
	
	public DoppelkopfOverviewAdapter(Context context, int layout, Cursor c) {
		super(context, layout, c, REQUIRED_COLUMNS, COLUMN_MAPPING);
	}

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        Cursor c = getCursor();

        final LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(layout, parent, false);
        ViewHolder holder = new ViewHolder();
        holder.extraInfo = v.findViewById(R.id.doppelkopf_overview_extra_info);
        holder.dutySoli = (TextView) v.findViewById(R.id.duty_soli_info);
        holder.limit = (TextView) v.findViewById(R.id.rounds_info);
        holder.player = new TextView[DoppelkopfGame.MAX_PLAYERS];
        holder.player[0] = (TextView) v.findViewById(R.id.player1);
        holder.player[1] = (TextView) v.findViewById(R.id.player2);
        holder.player[2] = (TextView) v.findViewById(R.id.player3);
        holder.player[3] = (TextView) v.findViewById(R.id.player4);
        holder.player[4] = (TextView) v.findViewById(R.id.player5);
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
    protected void updateViewInfo(View row, Cursor c) {
        int playersCol = c.getColumnIndex(GameStorageHelper.COLUMN_PLAYERS);
        int starttimeCol = c.getColumnIndex(GameStorageHelper.COLUMN_STARTTIME);
        int winnerCol = c.getColumnIndex(GameStorageHelper.COLUMN_WINNER);
        int idCol = c.getColumnIndex(GameStorageHelper.COLUMN_ID);
        int metadataCol = c.getColumnIndex(GameStorageHelper.COLUMN_METADATA);

        // fetching raw information
        String players = c.getString(playersCol);
        long startTime = c.getLong(starttimeCol);
        int winner = c.getInt(winnerCol);
        long id = c.getLong(idCol);
        Compacter metaData = new Compacter(c.getString(metadataCol));
        
        // prepare information
        Compacter cmp = new Compacter(players);
        String[] playerName = new String[cmp.getSize()];
        for (int index = 0; index < cmp.getSize(); index++) {
        	playerName[index] = cmp.getData(index);
        }

        Date startDate = new Date(startTime);
        ViewHolder holder = (ViewHolder) row.getTag();
        
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
        for (int index = 0; index < holder.player.length; index++) {
        	holder.player[index].setPaintFlags(holder.player[index].getPaintFlags() & (~Paint.FAKE_BOLD_TEXT_FLAG));	
    		holder.player[index].setTextColor(COLOR_PLAYER_IS_NO_WINNER);
        	if (index < playerName.length && Player.isValidPlayerName(playerName[index])) {
        		holder.player[index].setText(playerName[index]);
            	holder.player[index].setVisibility(View.VISIBLE);
            	if (DoppelkopfGame.isPlayerWinner(winner, index)) {
            		holder.player[index].setPaintFlags(holder.player[index].getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
            		holder.player[index].setTextColor(COLOR_PLAYER_IS_WINNER);
            	}
        	} else {
            	holder.player[index].setVisibility(View.GONE);        		
        	}
        }
        int dutySoli = 0;
        int roundLimit = DoppelkopfGame.NO_LIMIT;
		try {
			dutySoli = DoppelkopfGame.extractDutySoliOfMetadata(metaData);
			roundLimit = DoppelkopfGame.extractRoundLimitOfMetadata(metaData);
		} catch (CompactedDataCorruptException e) {
		}
        if (dutySoli > 0 | roundLimit > 0) {
        	holder.extraInfo.setVisibility(View.VISIBLE);
        	if (roundLimit == DoppelkopfGame.NO_LIMIT) {
        		holder.limit.setText(Character.toString('\u221E'));
        	} else {
        		holder.limit.setText(Integer.toString(roundLimit));
        	}
    		holder.dutySoli.setVisibility(dutySoli > 0 ? View.VISIBLE : View.GONE);
        	if (dutySoli > 0) {
        		holder.dutySoli.setText(Integer.toString(dutySoli));
        	}
        } else {
        	holder.extraInfo.setVisibility(View.GONE);
        }
    }
	
	private static class ViewHolder {
		 TextView[] player;
		 TextView date;
		 TextView time;
		 CheckBox checker;
		 View extraInfo;
		 TextView limit;
		 TextView dutySoli;
	}

}
