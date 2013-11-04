package dan.dit.gameMemo.appCore.statistics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import android.content.Context;
import android.graphics.Paint.Align;
import android.view.View;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.statistics.AcceptorIterator;
import dan.dit.gameMemo.gameData.statistics.GameStatistic;
import dan.dit.gameMemo.util.ColorPickerView;

public class StatisticFactoryChrono extends StatisticFactory {
    public static final double DEFAULT_ALPHA = 1.0;
    public static final boolean DEFAULT_CHRONO_MODE_SINGLE_VALUES = true;
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();;
    private GraphicalView mResultView;
    private double mAlpha = DEFAULT_ALPHA; // factor for the exponential smoothing (http://de.wikipedia.org/wiki/Exponentielle_Gl%C3%A4ttung), default 1 = no smoothing
    private List<List<Double>> values;
    private boolean mModeSingleValues = DEFAULT_CHRONO_MODE_SINGLE_VALUES;
    private List<AbstractPlayerTeam> mTeams;
     
    public StatisticFactoryChrono(GameStatistic stat, GameStatistic refStat, double alpha, boolean modeSingleValues) {
        super(stat, refStat);
        mAlpha = alpha;
        mModeSingleValues = modeSingleValues;
        if (alpha < 0 || alpha > 1) {
            throw new IllegalArgumentException("Illegal alpha: " + alpha);
        }
    }
    
    public View build(Context context) {
        mTeams = mStat.getTeams();
        mResultView = ChartFactory.getLineChartView(context, buildDataset(context, mTeams), buildRenderer(context, mTeams));
        return mResultView;
    }
    
    private XYMultipleSeriesRenderer buildRenderer(Context context, List<AbstractPlayerTeam> teams) {
        Iterator<AbstractPlayerTeam> it = teams.iterator();
        int[] colors = new int[teams.size() == 0 ? 1 : (teams.size() + 1) / 2 ];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = SERIES_COLOR;
            if (it.hasNext()) {
                AbstractPlayerTeam team = it.next();
                if (team != null) {
                    colors[i] = ColorPickerView.tintColor(team.getColor(), 0.5);
                }
                if (it.hasNext()) {
                    it.next(); // skip the enemy team
                }
            }
        }
        XYMultipleSeriesRenderer renderer = super.buildLineRenderer(colors, new PointStyle[] {PointStyle.POINT});
        renderer.setZoomButtonsVisible(true);
        renderer.setPanEnabled(true, true);
        renderer.setBackgroundColor(BACKGROUND_COLOR);
        renderer.setApplyBackgroundColor(true);
        renderer.setShowLegend(true);
        renderer.setLabelsColor(LABEL_COLOR);
        renderer.setXLabelsColor(LABEL_COLOR);
        renderer.setYLabelsColor(0, LABEL_COLOR);
        renderer.setYLabelsAlign(Align.RIGHT);
        renderer.setXTitle(context.getResources().getString(R.string.statistics_x_axis_label));
        renderer.setChartTitle(mStat.getName(context.getResources()).toString());
        return renderer;
    }
    
    private String getLegend(Context context) {
        if (mStat.getTeamsCount() == 1 && mStat.getTeam(0) != null) {
            return mStat.getTeam(0).getShortenedName(Player.LONG_NAME_LENGTH);
        } else if (mStat.getTeamsCount() == 2 && mStat.getTeam(0) != null ) {
            if (mStat.getTeam(1) != null) {
                return mStat.getTeam(0).getShortenedName(Player.LONG_NAME_LENGTH) + mStat.getTeam(1).getShortenedName(Player.LONG_NAME_LENGTH);
            } else {
                return mStat.getTeam(0).getShortenedName(Player.LONG_NAME_LENGTH);
            }
        } else {
            return context.getResources().getString(R.string.statistics_mode_chrono_no_team);
        }
    }
    
    private void applyAlpha() {
        int index = 0;
        for (XYSeries serie : mDataset.getSeries()) {
            serie.clear();
            List<Double> data = values.get(index++);
            if (data.size() > 0) {
                if (mModeSingleValues) {
                    double sum = 0;
                    for (Double d : data) {
                        sum += d;
                    }
                    serie.add(1, sum / data.size()); // first item is set to the median to have its influence reduced in case there is a high smoothing
                } else {
                    serie.add(1, data.get(0));
                }
                for (int i = 1; i < data.size(); i++) {
                    serie.add(i + 1, mAlpha * data.get(i) + (1.0 - mAlpha) * serie.getY(i - 1));
                }
            }
        }
    }
    
    public void setAlphaAndMode(Context context, double alpha, boolean modeSingleValues) {
        if (alpha < 0 || alpha > 1) {
            throw new IllegalArgumentException("Illegal alpha: " + alpha);
        }
        if (modeSingleValues != mModeSingleValues) {
            mModeSingleValues = modeSingleValues;
            buildDataset(context, mTeams);
        } else if (Math.abs(alpha - mAlpha) < 10E-10) {
            return; // nothing changed
        }
        mAlpha = alpha;
        applyAlpha();
        mResultView.repaint();
    }
    
    private XYMultipleSeriesDataset buildDataset(Context context, List<AbstractPlayerTeam> teams) {
        mDataset.clear();
        values = new ArrayList<List<Double>>((teams.size() + 1)/ 2);
        Iterator<AbstractPlayerTeam> teamsIt = teams.iterator();
        ArrayList<AbstractPlayerTeam> playersAndEnemys = new ArrayList<AbstractPlayerTeam>(2);
        boolean useReference = useReference();
        do {
            // set up the teams
            AbstractPlayerTeam players = teamsIt.hasNext() ? teamsIt.next() : null;
            AbstractPlayerTeam enemys = teamsIt.hasNext() ? teamsIt.next() : null;
            playersAndEnemys.clear();
            if (players != null && enemys == null) {
                playersAndEnemys.add(players);
            } else if (players != null) {
                playersAndEnemys.add(players);
                playersAndEnemys.add(enemys);
            }
            mStat.setTeams(playersAndEnemys);
            // calculate the series
            XYSeries series = new XYSeries(getLegend(context));
            List<Double> currValues = new ArrayList<Double>();
            values.add(currValues);
            // stat
            mStat.initCalculation();
            AcceptorIterator it = mStat.iterator();
            // ref stat
            AcceptorIterator refIt = null;
            if (useReference && mRefStat != null) {
                mRefStat.initCalculation();
                refIt = mRefStat.iterator();
            }
            double index = 0;
            double nextSum = 0;
            double nextValue = 0;
            double nextRefSum = 0;
            double lastRefValue = 0;
            while (it.hasNextGame()) {
                if (mModeSingleValues) {
                    nextSum = nextGameSum(mStat, it); // no more sum, show games individual value
                } else {
                    nextSum += nextGameSum(mStat, it);
                }
                if (useReference) {
                    if (refIt != null) {
                        if (mModeSingleValues) {
                            while (refIt.hasNextGame() && refIt.getNextGameIndex() < it.getCurrentGameIndex()) {
                                refIt.nextGame();
                            }
                            if (refIt.hasNextGame() && refIt.getNextGameIndex() == it.getCurrentGameIndex()) {
                                nextRefSum = nextGameSum(mRefStat, refIt); // no more sum, show games individual value
                            } else {
                                nextRefSum = Double.NaN;
                            }
                        } else {
                            while (refIt.hasNextGame() && refIt.getNextGameIndex() <= it.getCurrentGameIndex()) {
                                nextRefSum += nextGameSum(mRefStat, refIt);
                            }
                        }
                    } else {
                        nextRefSum = it.getAcceptedRoundsCount() > 0 ? it.getAcceptedRoundsCount() : it.getAcceptedGamesCount();
                        if (mModeSingleValues) {
                            nextRefSum -= lastRefValue;
                            lastRefValue = nextRefSum;
                        }
                    }
                }
                nextValue = nextValue(nextSum, nextRefSum);
                if (!Double.isNaN(nextValue)) {
                    currValues.add(nextValue);
                }
            }
            if (series.getItemCount() > 0) {
                series.addAnnotation(mStat.getFormat().format(nextValue), index, nextValue);
            }
            mDataset.addSeries(series);
        } while (teamsIt.hasNext());
        applyAlpha();
        return mDataset;
    }
}
