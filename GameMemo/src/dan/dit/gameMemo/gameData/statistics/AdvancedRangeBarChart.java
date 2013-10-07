package dan.dit.gameMemo.gameData.statistics;
import java.util.List;

import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;

public class AdvancedRangeBarChart extends BarChart
{
    private static final long serialVersionUID = -103440304447377027L;
    private int []  barChartColors;

    
    public AdvancedRangeBarChart ( XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer, Type type )
    {
        super ( dataset, renderer, type );
    }
    
    public static final GraphicalView getBarChartView(Context context, XYMultipleSeriesDataset dataset, XYMultipleSeriesRenderer renderer, Type type, int[] colors) {
        AdvancedRangeBarChart barChart = new AdvancedRangeBarChart(dataset, renderer, type);
        barChart.setColors(colors);
        GraphicalView view = new GraphicalView(context, barChart);
        return view;
    }
    
    public void setColors ( int [] colorsIn )
    {
        barChartColors = colorsIn;
    }

    @Override
    public void drawSeries ( Canvas canvas, Paint paint, List<Float> points, SimpleSeriesRenderer seriesRenderer, float yAxisValue, int seriesIndex, int startIndex )
    {
        /* source code originally from achartengine, modified to be able to have different colors */
        int seriesNr = mDataset.getSeriesCount();
        int length = points.size();
        paint.setStyle(Style.FILL);
        float halfDiffX = getHalfDiffX(points, length, seriesNr);
        for (int i = 0; i < length; i += 2) {
          float x = points.get(i);
          float y = points.get(i + 1);
          paint.setColor(barChartColors[startIndex + i / 2]);
          drawBar(canvas, x, yAxisValue, x, y, halfDiffX, seriesNr, seriesIndex, paint);
        }
        paint.setColor(seriesRenderer.getColor());
    }
}