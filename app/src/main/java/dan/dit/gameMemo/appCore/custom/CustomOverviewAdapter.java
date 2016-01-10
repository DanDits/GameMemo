package dan.dit.gameMemo.appCore.custom;

import java.util.Date;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameOverviewAdapter;
import dan.dit.gameMemo.gameData.game.custom.CustomGame;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

public class CustomOverviewAdapter extends GameOverviewAdapter {
    public static final String[] REQUIRED_COLUMNS = new String[] { GameStorageHelper.COLUMN_PLAYERS, GameStorageHelper.COLUMN_STARTTIME,
        GameStorageHelper.COLUMN_WINNER, GameStorageHelper.COLUMN_ID, GameStorageHelper.COLUMN_METADATA};
    private static final int[] COLUMN_MAPPING = new int[REQUIRED_COLUMNS.length];
    private String mFilterGameName;

    public CustomOverviewAdapter(Context context, int layout, Cursor c, int flag) {
        super(context, layout, c, REQUIRED_COLUMNS, COLUMN_MAPPING, flag);
    }
    
    public CustomOverviewAdapter(Context context, int layout, Cursor c) {
        super(context, layout, c, REQUIRED_COLUMNS, COLUMN_MAPPING);
    }

    public void setFilterGameName(String filterGameName) {
        if ((filterGameName == null && mFilterGameName == null)
                || (filterGameName != null && filterGameName.equals(mFilterGameName))) {
            return; // nothing changed
        }
        mFilterGameName = filterGameName;
        if (TextUtils.isEmpty(mFilterGameName)) {
            mFilterGameName = null;
        }
        notifyDataSetChanged();
    }
    
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        Cursor c = getCursor();

        final LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(layout, parent, false);
        ViewHolder holder = new ViewHolder();
        holder.players = (TextView) v.findViewById(R.id.custom_players);
        holder.gameName = (TextView) v.findViewById(R.id.custom_game_name);
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
        String metaData = c.getString(metadataCol);
        Compacter metaDataCmp = new Compacter(metaData);
        String gameName = CustomGame.getGameNameOfMetadata(metaDataCmp);
        ViewHolder holder = (ViewHolder) row.getTag();
        // prepare information
        Compacter playerData = new Compacter(players);
        List<Integer> teamSizeData = null;
        List<String> teamNameData = null;
        try {
            teamSizeData = CustomGame.getTeamSizeDataOfMetadata(metaDataCmp);
            teamNameData = CustomGame.getTeamNameDataOfMetadata(metaDataCmp);
        } catch (CompactedDataCorruptException e) {
        }
        if (teamSizeData != null) {
            SpannableStringBuilder content = new SpannableStringBuilder();
            int playerIndex = 0;
            for (int index = 0; index < teamSizeData.size(); index++) {
                int boldStartIndex = content.length();
                int currTeamSize = teamSizeData.get(index);
                if (teamNameData != null && currTeamSize > 1 && !TextUtils.isEmpty(teamNameData.get(index))) {
                    content.append(teamNameData.get(index)).append(": ");
                }
                for (int i = 0; i < currTeamSize; i++) {
                    content.append(playerData.getData(playerIndex));
                    if (i < currTeamSize - 1) {
                        content.append(", ");
                    }
                    playerIndex++;
                }
                if (CustomGame.isTeamWinner(winner, index)) {
                    StyleSpan style = new StyleSpan(Typeface.BOLD);
                    content.setSpan(style, boldStartIndex, content.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                }
                if (index < teamSizeData.size() - 1) {
                    content.append('\n');
                }
            }          
            holder.players.setText(content);  
        }

        Date startDate = new Date(startTime);
        
        if (mFilterGameName == null || !mFilterGameName.equals(gameName)) {
            holder.gameName.setVisibility(View.VISIBLE);
            holder.gameName.setText(gameName);
        } else {
            holder.gameName.setVisibility(View.GONE);
        }
        
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
    }
    
    private static class ViewHolder {
         TextView players;
         TextView date;
         TextView time;
         CheckBox checker;
         TextView gameName;
    }
}
