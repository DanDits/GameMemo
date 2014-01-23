package dan.dit.gameMemo.appCore.minigolf;

import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameOverviewAdapter;
import dan.dit.gameMemo.gameData.game.minigolf.MinigolfGame;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.storage.database.SportGameTable;
import dan.dit.gameMemo.util.compaction.Compacter;

public class MinigolfOverviewAdapter extends GameOverviewAdapter {
    public static final String[] REQUIRED_COLUMNS = new String[] { GameStorageHelper.COLUMN_PLAYERS, GameStorageHelper.COLUMN_STARTTIME,
        GameStorageHelper.COLUMN_WINNER, GameStorageHelper.COLUMN_ID, SportGameTable.COLUMN_LOCATION};
    private static final int[] COLUMN_MAPPING = new int[REQUIRED_COLUMNS.length];

    public MinigolfOverviewAdapter(Context context, int layout, Cursor c, int flag) {
        super(context, layout, c, REQUIRED_COLUMNS, COLUMN_MAPPING, flag);
    }
    
    public MinigolfOverviewAdapter(Context context, int layout, Cursor c) {
        super(context, layout, c, REQUIRED_COLUMNS, COLUMN_MAPPING);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        Cursor c = getCursor();

        final LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(layout, parent, false);
        ViewHolder holder = new ViewHolder();
        holder.players = (TextView) v.findViewById(R.id.minigolf_players);
        holder.location = (TextView) v.findViewById(R.id.location);
        holder.winner = (TextView) v.findViewById(R.id.winner);
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
        int locationCol = c.getColumnIndex(SportGameTable.COLUMN_LOCATION);

        // fetching raw information
        String players = c.getString(playersCol);
        long startTime = c.getLong(starttimeCol);
        int winner = c.getInt(winnerCol);
        long id = c.getLong(idCol);
        String location = c.getString(locationCol);
        
        // prepare information
        Compacter playerData = new Compacter(players);
        StringBuilder playersText = new StringBuilder();
        for (int index = 0; index < playerData.getSize(); index++) {
            playersText.append(playerData.getData(index));
            if (index < playerData.getSize() - 1) {
                playersText.append(", ");
            }
        }

        Date startDate = new Date(startTime);
        ViewHolder holder = (ViewHolder) row.getTag();
        
        holder.players.setText(playersText.toString());
        holder.location.setText(location);
        
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
        // show winner
        StringBuilder winnerText = new StringBuilder();
        for (int i = 0; i < playerData.getSize(); i++) {
            if (MinigolfGame.isPlayerWinner(winner, i)) {
                if (winnerText.length() != 0) {
                    winnerText.append(", ");
                }
                winnerText.append(playerData.getData(i));
            }
        }
        if  (winnerText.length() > 0) {
            holder.winner.setVisibility(View.VISIBLE);
            holder.winner.setText(winnerText.toString());
        } else {
            holder.winner.setVisibility(View.INVISIBLE);
        }
    }
    
    private static class ViewHolder {
         TextView players;
         TextView date;
         TextView time;
         CheckBox checker;
         TextView location;
         TextView winner;
    }
}
