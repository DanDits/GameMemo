package dan.dit.gameMemo.appCore.statistics;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import android.content.Context;
import android.view.View;
import dan.dit.gameMemo.gameData.statistics.AcceptorIterator;
import dan.dit.gameMemo.gameData.statistics.GameStatistic;

public class StatisticFactoryChrono extends StatisticFactory {
    
    public StatisticFactoryChrono(GameStatistic stat) {
        super(stat);
    }
    
    public StatisticFactoryChrono(GameStatistic stat, GameStatistic refStat) {
        super(stat, refStat);
    }
    
    public View build(Context context) {
        return ChartFactory.getLineChartView(context, buildDataset(), buildRenderer(context));
    }
    
    private XYMultipleSeriesRenderer buildRenderer(Context context) {
        XYMultipleSeriesRenderer renderer = super.buildLineRenderer(new int[] {0xFF08088A}, new PointStyle[] {PointStyle.POINT});
        renderer.setZoomButtonsVisible(true);
        renderer.setPanEnabled(true, true);
        renderer.setShowLegend(false);
        renderer.setChartTitle(mStat.getName(context.getResources()).toString());
        return renderer;
    }
    
    private XYMultipleSeriesDataset buildDataset() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYSeries series = new XYSeries(mStat.getIdentifier());
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
            if (nextValue != Double.NaN) {
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
