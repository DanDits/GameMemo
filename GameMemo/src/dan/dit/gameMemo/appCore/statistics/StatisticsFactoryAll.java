package dan.dit.gameMemo.appCore.statistics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.statistics.GameStatistic;
import dan.dit.gameMemo.gameData.statistics.GameStatisticAttributeManager;
import dan.dit.gameMemo.gameData.statistics.StatisticAttribute;

public class StatisticsFactoryAll extends StatisticFactory {
    private GameStatisticAttributeManager mManager;
    private List<GameStatistic> mStatistics;
    private List<Double> mValues;
    private List<AbstractPlayerTeam> mTeams;
    private List<Game> mGames;
    private AllStatisticsAdapter mAdapter;
    private ListView mListView;
    private TextView mHeader;
    
    public StatisticsFactoryAll(int gameKey, List<AbstractPlayerTeam> teams, List<Game> games, ListView listView, TextView header) {
        mManager = GameKey.getGameStatisticAttributeManager(gameKey);
        mTeams = teams;
        mGames = games;
        mListView = listView;
        mHeader = header;
    }

    @Override
    public View build(Context context) {
        calculateValues(context);
        mAdapter = new AllStatisticsAdapter(context);
        mListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
        return mListView;
    }
    
    private void calculateValues(Context context) {
        mStatistics = mManager.getStatistics();
        mValues = new ArrayList<Double>(mStatistics.size());
        for (GameStatistic stat : mStatistics) {
            stat.setTeams(mTeams);
            stat.setGameList(mGames);
            setStatistic(stat, mManager.getStatistic(stat.getReference()));
            double value = getTeamValueForCurrentStatistic(null);
            mValues.add(value);                
        }
        if (mTeams == null || mTeams.size() == 0) {
            mHeader.setText(context.getResources().getString(R.string.statistics_mode_all_no_team));
        } else {
            StringBuilder builder = new StringBuilder();
            Iterator<AbstractPlayerTeam> it = mTeams.iterator();
            while (it.hasNext()) {
                builder.append(it.next().getShortenedName(Player.LONG_NAME_LENGTH));
                if (it.hasNext()) {
                    builder.append(" <> ");
                }
            }
            mHeader.setText(builder.toString());
        }
    }

    private class AllStatisticsAdapter extends ArrayAdapter<GameStatistic> {
        private LayoutInflater inflater;
        private Resources res;
        public AllStatisticsAdapter(Context context) {
            super(context, dan.dit.gameMemo.R.layout.statistics_all_row, mStatistics);
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            res = context.getResources();
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                // if row layout not yet created, create with default layout
                row = inflater.inflate(dan.dit.gameMemo.R.layout.statistics_all_row, parent, false);
            }
            ((TextView) row.findViewById(R.id.statistic_name)).setText(getItem(position).getName(res));
            int presTypeTitleResId = 0;
            switch (getItem(position).getPresentationType()) {
            case GameStatistic.PRESENTATION_TYPE_ABSOLUTE:
                presTypeTitleResId = R.string.statistics_menu_pres_type_absolute; break;
            case GameStatistic.PRESENTATION_TYPE_PERCENTAGE:
                presTypeTitleResId = R.string.statistics_menu_pres_type_percentual; break;
            case GameStatistic.PRESENTATION_TYPE_PROPORTION:
                presTypeTitleResId = R.string.statistics_menu_pres_type_proportional; break;
            }
            ((TextView) row.findViewById(R.id.statistic_value)).setText(Double.toString(StatisticAttribute.makeShorter(mValues.get(position))) +
                    "\n(" + res.getString(presTypeTitleResId) + ")");
            Log.d("GameMemo", "GetView for position " + position);
            return row;
        }
    }

    public void notifyDataSetChanged() {
        mAdapter.notifyDataSetChanged();
    }
}
