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
    public static final int LABEL_COLOR = 0xFFFFFFFF;// light green: 0xFF90F490;
    public static final int SERIES_COLOR = 0xFFFFFFFF;
    public static final int BACKGROUND_COLOR = 0xFF000000;
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
        r.setChartValuesFormat(mStat.getFormat());
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
          r.setPointStyle(styles.length > i ? styles[i] : styles[0]);
          r.setChartValuesFormat(mStat.getFormat());
          renderer.addSeriesRenderer(r);
        }
        return renderer;
    }
    
    protected boolean useReference() {
        return mStat.getPresentationType() == GameStatistic.PRESENTATION_TYPE_PERCENTAGE || mStat.getPresentationType() == GameStatistic.PRESENTATION_TYPE_PROPORTION;
    }
    
    protected static double nextGameSum(GameStatistic stat, AcceptorIterator it) {
        Game game;
        GameRound round;
        double gameValue;
        double roundValue;
        double sum = 0;
        game = it.nextGame();
        gameValue = stat.calculateValue(game);
        if (!Double.isNaN(gameValue)) {
            sum += gameValue;
        }
        while (it.hasNextRound()) {
            round = it.nextRound();
            roundValue = stat.calculateValue(game, round);
            if (Double.isNaN(roundValue)) {
                throw new IllegalStateException("Calculated NaN for an accepted round for game " + game + " and round " + round + " and stat " + stat);
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
            if (Double.isNaN(sum) || Double.isNaN(refSum) || refSum == 0) {
                return Double.NaN;
            }
            return factor * sum / refSum;
        default:
            throw new IllegalStateException("Illegal presentation type for stat " + mStat + " type= " + mStat.getPresentationType());
        }
    }

    protected double getTeamValueForCurrentStatistic(AbstractPlayerTeam team) {
        boolean useReference = useReference();
        if (team != null) {
            List<AbstractPlayerTeam> teamList = new ArrayList<AbstractPlayerTeam>(1);
            teamList.add(team);
            mStat.setTeams(teamList);
            if (mRefStat != null && useReference) {
                mRefStat.setTeams(teamList);
            }
        }

        mStat.initCalculation();
        AcceptorIterator it = mStat.iterator();
        double totalSum = 0;
        while (it.hasNextGame()) {
            totalSum += nextGameSum(mStat, it);
        }
        AcceptorIterator refIt = null;
        if (useReference && mRefStat != null) {
            mRefStat.initCalculation();
            refIt = mRefStat.iterator();
        }
        double totalRefSum = 0;
        if (refIt != null) {
            while (refIt.hasNextGame()) {
                totalRefSum += nextGameSum(mRefStat, refIt);
            }
        } else if (useReference) {
            totalRefSum = it.getAcceptedRoundsCount() > 0 ? it.getAcceptedRoundsCount() : it.getAcceptedGamesCount();
        }
        return nextValue(totalSum, totalRefSum);
    }
}
