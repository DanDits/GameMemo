package dan.dit.gameMemo.appCore.statistics;

import java.util.ArrayList;
import java.util.List;

import org.achartengine.chart.PointStyle;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.view.View;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.statistics.AcceptorIterator;
import dan.dit.gameMemo.gameData.statistics.GameStatistic;

public abstract class StatisticFactory {
    protected GameStatistic mStat;
    protected GameStatistic mRefStat;
    
    public StatisticFactory() {}
    
    public StatisticFactory(GameStatistic stat, GameStatistic refStat) {
        setStatistic(stat, refStat);
    }
    
    protected void setStatistic(GameStatistic stat, GameStatistic refStat) {
        mStat = stat;
        if (mStat == null) {
            throw new IllegalArgumentException("Given statistic is null for DatasetBuilder.");
        }
        mRefStat = refStat;
    }
    
    public abstract View build(Context context);
    
    /**
     * Builds a bar multiple series renderer to use the provided colors.
     * 
     * @param colors the series renderers colors
     * @return the bar multiple series renderer
     */
    protected XYMultipleSeriesRenderer buildBarRenderer(int[] colors) {
      XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
      renderer.setAxisTitleTextSize(16);
      renderer.setChartTitleTextSize(20);
      renderer.setLabelsTextSize(15);
      renderer.setLegendTextSize(15);
      int length = colors.length;
      for (int i = 0; i < length; i++) {
        SimpleSeriesRenderer r = new SimpleSeriesRenderer();
        r.setColor(colors[i]);
        renderer.addSeriesRenderer(r);
      }
      return renderer;
    }

    public XYMultipleSeriesRenderer buildLineRenderer(int[] colors,
            PointStyle[] styles) {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setAxisTitleTextSize(16);
        renderer.setChartTitleTextSize(20);
        renderer.setLabelsTextSize(15);
        renderer.setLegendTextSize(15);
        renderer.setPointSize(5f);
        renderer.setMargins(new int[] { 20, 30, 15, 20 });
        int length = colors.length;
        for (int i = 0; i < length; i++) {
          XYSeriesRenderer r = new XYSeriesRenderer();
          r.setColor(colors[i]);
          r.setPointStyle(styles[i]);
          renderer.addSeriesRenderer(r);
        }
        return renderer;
    }
    
    protected boolean useReference() {
        return mStat.getPresentationType() == GameStatistic.PRESENTATION_TYPE_PERCENTAGE || mStat.getPresentationType() == GameStatistic.PRESENTATION_TYPE_PROPORTION;
    }
    
    protected double nextGameSum(AcceptorIterator it) {
        Game game;
        GameRound round;
        double gameValue;
        double roundValue;
        double sum = 0;
        game = it.nextGame();
        gameValue = mStat.calculateValue(game);
        if (!Double.isNaN(gameValue)) {
            sum += gameValue;
        }
        while (it.hasNextRound()) {
            round = it.nextRound();
            roundValue = mStat.calculateValue(game, round);
            if (Double.isNaN(roundValue)) {
                throw new IllegalStateException("Calculated NaN for an accepted round for game " + game + " and round " + round + " and stat " + mStat);
            }
            sum += roundValue;
        }
        return sum;
    }
    
    protected double nextValue(double sum, double refSum) {
        double factor = 1;
        switch (mStat.getPresentationType()) {
        case GameStatistic.PRESENTATION_TYPE_ABSOLUTE:
            return sum;
        case GameStatistic.PRESENTATION_TYPE_PERCENTAGE:
            factor = 100;
            /* fall through */
        case GameStatistic.PRESENTATION_TYPE_PROPORTION:
            if (sum == Double.NaN || refSum == Double.NaN || refSum == 0) {
                return Double.NaN;
            }
            return factor * sum / refSum;
        default:
            throw new IllegalStateException("Illegal presentation type for stat " + mStat + " type= " + mStat.getPresentationType());
        }
    }

    protected double getTeamValueForCurrentStatistic(AbstractPlayerTeam team) {
        if (team != null) {
            List<AbstractPlayerTeam> teamList = new ArrayList<AbstractPlayerTeam>(1);
            teamList.add(team);
            mStat.setTeams(teamList);
        }
        
        AcceptorIterator it = mStat.iterator();
        boolean useReference = useReference();
        AcceptorIterator refIt = useReference && mRefStat != null ? mRefStat.iterator() : null;

        mStat.initCalculation();
        double totalSum = 0;
        double totalRefSum = 0;
        double nextSum;
        double nextRefSum = Double.NaN;
        while (it.hasNextGame() && (refIt == null || refIt.hasNextGame())) {
            nextSum = nextGameSum(it);
            totalSum += nextSum;
            if (useReference) {
                if (refIt != null) {
                    nextRefSum = nextGameSum(refIt);
                    totalRefSum += nextRefSum;
                }
            }
        }
        if (useReference && refIt == null) {
            totalRefSum = it.getAcceptedRoundsCount() > 0 ? it.getAcceptedRoundsCount() : it.getAcceptedGamesCount();
        }
        double result = nextValue(totalSum, totalRefSum);
        return result;
    }
}
