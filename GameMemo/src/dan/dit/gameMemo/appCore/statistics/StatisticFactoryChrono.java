package dan.dit.gameMemo.appCore.statistics;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import android.content.Context;
import android.graphics.Paint.Align;
import android.view.View;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.statistics.AcceptorIterator;
import dan.dit.gameMemo.gameData.statistics.GameStatistic;

public class StatisticFactoryChrono extends StatisticFactory {
    
    public StatisticFactoryChrono(GameStatistic stat, GameStatistic refStat) {
        super(stat, refStat);
    }
    
    public View build(Context context) {
        return ChartFactory.getLineChartView(context, buildDataset(context), buildRenderer(context));
    }
    
    private XYMultipleSeriesRenderer buildRenderer(Context context) {
        XYMultipleSeriesRenderer renderer = super.buildLineRenderer(new int[] {0xFF08088A}, new PointStyle[] {PointStyle.POINT});
        renderer.setZoomButtonsVisible(true);
        renderer.setPanEnabled(true, true);
        renderer.setShowLegend(true);
        final int COLOR = 0xFF4CEB20;
        renderer.setLabelsColor(COLOR);
        renderer.setXLabelsColor(COLOR);
        renderer.setYLabelsColor(0, COLOR);
        renderer.setYLabelsAlign(Align.RIGHT);
        renderer.setXTitle(context.getResources().getString(R.string.statistics_x_axis_label));
        renderer.setChartTitle(mStat.getName(context.getResources()).toString());
        return renderer;
    }
    
    private String getLegend(Context context) {
        if (mStat.getTeamsCount() == 0) {
            return context.getResources().getString(R.string.statistics_mode_chrono_no_team);
        } else if (mStat.getTeamsCount() == 1) {
            return mStat.getTeam(0).getShortenedName(Player.LONG_NAME_LENGTH);
        } else {
            return mStat.getTeam(0).getShortenedName(Player.LONG_NAME_LENGTH) + mStat.getTeam(1).getShortenedName(Player.LONG_NAME_LENGTH);
        }
    }
    
    private XYMultipleSeriesDataset buildDataset(Context context) {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYSeries series = new XYSeries(getLegend(context));
        AcceptorIterator it = mStat.iterator();
        boolean useReference = useReference();
        AcceptorIterator refIt = useReference && mRefStat != null ? mRefStat.iterator() : null;
        mStat.initCalculation();
        double index = 0;
        double nextSum = 0;
        double nextRefSum = 0;
        double nextValue = 0;
        while (it.hasNextGame() && (refIt == null || refIt.hasNextGame())) {
            nextSum += nextGameSum(it);
            if (useReference) {
                if (refIt != null) {
                    nextRefSum += nextGameSum(refIt);
                } else {
                    nextRefSum = it.getAcceptedRoundsCount() > 0 ? it.getAcceptedRoundsCount() : it.getAcceptedGamesCount();
                }
            }
            nextValue = nextValue(nextSum, nextRefSum);
            if (!Double.isNaN(nextValue)) {
                series.add(++index, nextValue);
            }
        }
        if (series.getItemCount() > 0) {
            series.addAnnotation(Double.toString(nextValue), index, nextValue);
        }
        dataset.addSeries(series);
        return dataset;
    }
    
}
