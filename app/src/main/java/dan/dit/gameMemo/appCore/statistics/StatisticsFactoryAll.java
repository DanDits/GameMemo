package dan.dit.gameMemo.appCore.statistics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
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

    private CharSequence resultHeaderText;
    @Override
    public void executeBuild(Context context, StatisticFactory.StatisticBuildTask task) {
        if (!calculateValues(context, task)) {
            return;
        }
    }
    
    @Override
    public View finishBuild(Context context) {
        mHeader.setText(resultHeaderText);
        mAdapter = new AllStatisticsAdapter(context);
        mListView.setAdapter(mAdapter);
        mListView.setSelector(GameKey.getSelectorResource(mManager.getGameKey()));
        mListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position,
                    long id) {
                GameStatistic stat = mAdapter.getItem(position);
                stat.setPresentationType(stat.nextPresentationType());
                mValues.set(position, initAndCalculateForStatistic(stat));
                mAdapter.notifyDataSetChanged();
            }
        });
        mAdapter.notifyDataSetChanged();
        return mListView;
    }
    
    private double initAndCalculateForStatistic(GameStatistic stat) {
        stat.setTeams(mTeams);
        stat.setGameList(mGames);
        GameStatistic refStat = mManager.getStatistic(stat.getReference());
        if (refStat != null) {
            refStat.setTeams(mTeams);
            refStat.setGameList(mGames);
        }
        setStatistic(stat, refStat);
        double value = getTeamValueForCurrentStatistic(null);
        return value;
    }
    
    private boolean calculateValues(Context context, StatisticBuildTask task) {
        mStatistics = mManager.getStatistics(false);
        mValues = new ArrayList<Double>(mStatistics.size());
        double progress = 0;
        double progressPerStat = 100.0 / mStatistics.size();
        for (GameStatistic stat : mStatistics) {
            progress += progressPerStat;
            task.buildProgress(progress);
            mValues.add(initAndCalculateForStatistic(stat));   
            if (task.isCancelled()) {
                return false;
            }
        }
        if (mTeams == null || mTeams.size() == 0) {
            resultHeaderText = context.getResources().getString(R.string.statistics_mode_all_no_team);
        } else {
            StringBuilder builder = new StringBuilder();
            Iterator<AbstractPlayerTeam> it = mTeams.iterator();
            while (it.hasNext()) {
                builder.append(it.next().getShortenedName(Player.LONG_NAME_LENGTH));
                if (it.hasNext()) {
                    builder.append(" <> ");
                }
            }
            resultHeaderText = builder.toString();
        }
        return true;
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
            ((TextView) row.findViewById(R.id.statistic_value)).setText(mStatistics.get(position).getFormat().format(mValues.get(position)));
            return row;
        }
    }

    public void notifyDataSetChanged() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }
}
