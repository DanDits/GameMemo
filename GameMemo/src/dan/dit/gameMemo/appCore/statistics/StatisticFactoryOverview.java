package dan.dit.gameMemo.appCore.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.achartengine.chart.BarChart.Type;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import android.content.Context;
import android.graphics.Paint.Align;
import android.view.View;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.statistics.AdvancedRangeBarChart;
import dan.dit.gameMemo.gameData.statistics.GameStatistic;
import dan.dit.gameMemo.gameData.statistics.StatisticAttribute;

public class StatisticFactoryOverview extends StatisticFactory {

    public StatisticFactoryOverview(GameStatistic stat, GameStatistic refStat) {
        super(stat, refStat);
    }
    
    private XYMultipleSeriesDataset buildDataset(List<Double> values, Context context) {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        XYSeries series = new XYSeries(mStat.getName(context.getResources()).toString());
        int index = 0;
        for (Double val : values) {
            series.add(++index, StatisticAttribute.makeShorter(val)); 
        }
        dataset.addSeries(series);
        return dataset;
    }
    
    private List<Double> calculateValues(List<AbstractPlayerTeam> allTeams) {
        List<Double> list = new ArrayList<Double>(allTeams.size());
        Iterator<AbstractPlayerTeam> it = allTeams.iterator();
        while (it.hasNext()) {
            AbstractPlayerTeam team = it.next();
            double value = getTeamValueForCurrentStatistic(team);
            if (!Double.isNaN(value)) {
                list.add(value);                
            } else {
                it.remove();
            }
        }
        return list;
    }
    
    private void sortParallel(List<AbstractPlayerTeam> teams, List<Double> values) {
        boolean sorted = false;
        int endIndex = values.size() - 1;
        // bubble sort descending
        while (!sorted) {
            sorted = true;
            for (int i = 0; i < endIndex; i++) {
                Double old = values.get(i);
                if (old.compareTo(values.get(i + 1)) < 0) {
                    // swap
                    values.set(i, values.get(i + 1));
                    values.set(i + 1, old);
                    AbstractPlayerTeam oldTeam = teams.get(i);
                    teams.set(i, teams.get(i + 1));
                    teams.set(i + 1, oldTeam);
                    sorted = false;
                }
            }
            endIndex--;
        }
    }
    
    private XYMultipleSeriesRenderer buildRenderer(Context context, List<AbstractPlayerTeam> allTeams, List<Double> values) {
        XYMultipleSeriesRenderer renderer = super.buildBarRenderer(new int[] {0xFF000000});
        renderer.getSeriesRendererAt(0).setDisplayChartValues(true);
        renderer.setPanEnabled(true, true);
        renderer.setZoomRate(1.1f);
        renderer.setBarSpacing(0.1f);
        renderer.setZoomButtonsVisible(true);
        renderer.setChartTitle(mStat.getName(context.getResources()).toString());
        renderer.setShowGridX(true);
        renderer.setXLabelsAlign(Align.CENTER);
        renderer.setXLabels(0);
        renderer.setXAxisMin(-1);
        renderer.setXAxisMax(15);
        renderer.setXLabelsAngle(290);
        renderer.setXLabelsAlign(Align.RIGHT);
        final int COLOR = 0xFF4CEB20;
        renderer.setLabelsColor(COLOR);
        renderer.setXLabelsColor(COLOR);
        renderer.setYLabelsColor(0, COLOR);
        renderer.setYLabelsAlign(Align.RIGHT);
        int index = 0;
        for (AbstractPlayerTeam team : allTeams) {
            renderer.addXTextLabel(++index, team.getShortenedName(Player.LONG_NAME_LENGTH));
        }
        return renderer;
    }
    
    public View build(Context context) {
        List<AbstractPlayerTeam> allTeams = getAllTeams();
        List<Double> values = calculateValues(allTeams);
        sortParallel(allTeams, values);
        return AdvancedRangeBarChart.getBarChartView(context, buildDataset(values, context), buildRenderer(context, allTeams, values),
                Type.DEFAULT, getColors(allTeams));
    }
    
    private int[] getColors(List<AbstractPlayerTeam> allTeams) {
        int[] colors = new int[allTeams.size()];
        int index = 0;
        for (AbstractPlayerTeam team : allTeams) {
            colors[index] = team.getColor();
            index++;
        }
        return colors;
    }
    
    private List<AbstractPlayerTeam> getAllTeams() {
        List<AbstractPlayerTeam> allTeams = new ArrayList<AbstractPlayerTeam>();
        Collection<? extends AbstractPlayerTeam> statTeams = mStat.getTeams();
        if (statTeams != null) {
            for (AbstractPlayerTeam team : statTeams) {
                if (team.getPlayerCount() > 0) {
                    allTeams.add(team);
                }
            }
        }
        for (Player p : GameKey.getPool(mStat.getGameKey()).getAll()) {
            allTeams.add(p);
        }
        return allTeams;
    }
}
