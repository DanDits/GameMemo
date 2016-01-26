package dan.dit.gameMemo.appCore.binokel;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameOverviewAdapter;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.binokel.BinokelGame;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compaction.Compacter;

public class BinokelGameOverviewAdapter extends GameOverviewAdapter {
	public static final String[] REQUIRED_COLUMNS = new String[] { GameStorageHelper.COLUMN_PLAYERS, GameStorageHelper.COLUMN_STARTTIME,
		GameStorageHelper.COLUMN_WINNER, GameStorageHelper.COLUMN_ID};
	private static final int[] COLUMN_MAPPING = new int[REQUIRED_COLUMNS.length];

	public BinokelGameOverviewAdapter(Context context, int layout, Cursor c) {
    	super(context, layout, c, REQUIRED_COLUMNS, COLUMN_MAPPING);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public BinokelGameOverviewAdapter(Context context, int layout, Cursor c, int flag) {
        super(context, layout, c, REQUIRED_COLUMNS, COLUMN_MAPPING, flag);
    }
    
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        Cursor c = getCursor();

        final LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(layout, parent, false);
        ViewHolder holder = new ViewHolder();
        holder.team1_1 = (TextView) v.findViewById(R.id.binokel_overview_team1_1);
        holder.team1_2 = (TextView) v.findViewById(R.id.binokel_overview_team1_2);
        holder.team2_1 = (TextView) v.findViewById(R.id.binokel_overview_team2_1);
        holder.team2_2 = (TextView) v.findViewById(R.id.binokel_overview_team2_2);
        holder.team3_1 = (TextView) v.findViewById(R.id.binokel_overview_team3_1);
        holder.team3_2 = (TextView) v.findViewById(R.id.binokel_overview_team3_2);
        holder.team1Won = (ImageView) v.findViewById(R.id.binokel_team1_won);
        holder.team2Won = (ImageView) v.findViewById(R.id.binokel_team2_won);
        holder.team3Won = (ImageView) v.findViewById(R.id.binokel_team3_won);
        holder.team3Container = v.findViewById(R.id.team3_container);
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
    protected void updateViewInfo(View binokelRow, Cursor c) {
        int playersCol = c.getColumnIndex(GameStorageHelper.COLUMN_PLAYERS);
        int starttimeCol = c.getColumnIndex(GameStorageHelper.COLUMN_STARTTIME);
        int winnerCol = c.getColumnIndex(GameStorageHelper.COLUMN_WINNER);
        int idCol = c.getColumnIndex(GameStorageHelper.COLUMN_ID);

        // fetching raw information
        String playersRaw = c.getString(playersCol);
        long startTime = c.getLong(starttimeCol);
        int winner = c.getInt(winnerCol);
        long id = c.getLong(idCol);

        // prepare information
        Compacter playersCmp = new Compacter(playersRaw);
        Iterator<String> playerNamesIt = playersCmp.iterator();
        int playersPerTeam = BinokelGame.getPlayersPerTeam(playersCmp.getSize());
        List<String> team1 = new ArrayList<>(playersPerTeam);
        for (int i = 0; i < playersPerTeam && playerNamesIt.hasNext(); i++) {
            team1.add(playerNamesIt.next());
        }
        List<String> team2 = new ArrayList<>(playersPerTeam);
        for (int i = 0; i < playersPerTeam && playerNamesIt.hasNext(); i++) {
            team2.add(playerNamesIt.next());
        }
        List<String> team3 = new ArrayList<>(playersPerTeam);
        for (int i = 0; i < playersPerTeam && playerNamesIt.hasNext(); i++) {
            team3.add(playerNamesIt.next());
        }
        String player1_1 = team1.get(0);
        String player1_2 = team1.size() > 1 ? team1.get(1) : null;
        String player2_1 = team2.get(0);
        String player2_2 = team2.size() > 1 ? team2.get(1) : null;
        String player3_1 = team3.size() > 0 ? team3.get(0) : null;
        String player3_2 = team3.size() > 1 ? team3.get(1) : null;

        Date startDate = new Date(startTime);
        boolean hasWinner = winner != Game.WINNER_NONE;
        boolean team1Wins = winner == 1;
        boolean team2Wins = winner == 2;
        boolean team3Wins = winner == 3;

        ViewHolder holder = (ViewHolder) binokelRow.getTag();
        holder.team3Container.setVisibility(team3.size() == 0 ? View.GONE : View.VISIBLE);
        
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

        showPlayerName(holder.team1_1, player1_1);
        showPlayerName(holder.team1_2, player1_2);
        showPlayerName(holder.team2_1, player2_1);
        showPlayerName(holder.team2_2, player2_2);
        showPlayerName(holder.team3_1, player3_1);
        showPlayerName(holder.team3_2, player3_2);


        showWinner(holder.team1Won, team1Wins, hasWinner);
        showWinner(holder.team2Won, team2Wins, hasWinner);
        showWinner(holder.team3Won, team3Wins, hasWinner);
    }

    private void showWinner(ImageView view, boolean teamWins, boolean hasWinner) {
        if (!hasWinner) {
            view.setVisibility(View.GONE);
            return;
        }
        view.setVisibility(teamWins ? View.VISIBLE : View.INVISIBLE);
    }

    private void showPlayerName(TextView view, String playerName) {
        if (TextUtils.isEmpty(playerName)) {
            view.setVisibility(View.GONE);
            return;
        }
        view.setVisibility(View.VISIBLE);
        Player p = BinokelGame.PLAYERS.populatePlayer(playerName);
        view.setText(p.getShortenedName(Player.SHORT_NAME_LENGTH));
    }

	private static class ViewHolder {
		 TextView team1_1;
		 TextView team1_2;
		 TextView team2_1;
		 TextView team2_2;
         TextView team3_1;
        TextView team3_2;
		 ImageView team1Won;
		 ImageView team2Won;
         ImageView team3Won;
         View team3Container;
		 TextView date;
		 TextView time;
		 CheckBox checker;
    }
}
