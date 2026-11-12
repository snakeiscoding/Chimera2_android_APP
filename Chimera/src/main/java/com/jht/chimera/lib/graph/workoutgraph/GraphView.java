package com.jht.chimera.lib.graph.workoutgraph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

import com.jht.chimera.lib.graph.graphview.PlotStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a graph on an image view to simplify displaying animations and motion.
 */
public class GraphView extends AppCompatImageView implements IWorkoutGraph
{
    private final Paint mCursorPaint;
    private final List<GraphSeries> mSeries = new ArrayList<>();
    private List<ChannelDataSource> mChannels = new ArrayList<>();
    private float mGoal;
    private float mGoalRemaining;
    private float mGoalStart = 0;

    // Tracks the last x coordinate that was drawn so we don't redraw the whole graph
    private float mLastDrawnPixel;

    public GraphView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        mCursorPaint = new Paint();
        mCursorPaint.setStrokeWidth(1);
        mCursorPaint.setColor(0xffffffff);
        mGoal = 0;
        mGoalRemaining = 0;
    }

    @Override
    public void reset()
    {
        mSeries.clear();
        mChannels.clear();
        mGoal = 0;
        mGoalRemaining = 0;
    }

    @Override
    public int addChannel(ChannelDataSource dataSource)
    {
        mChannels.add(dataSource);
        mSeries.add(new GraphSeries(dataSource.getMin(), dataSource.getMax()));
        return mSeries.size() - 1;
    }

    @Override
    public boolean setChannelHistoryPlotStyle(int channelIndex, PlotStyle plotStyle)
    {
        if (channelIndex < mSeries.size())
        {
            GraphSeries series = mSeries.get(channelIndex);
            series.setHistoryPlotStyle(plotStyle.fillType, plotStyle.lineColor, plotStyle.fillColor);
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean setChannelFuturePlotStyle(int channelIndex, PlotStyle plotStyle)
    {
        if (channelIndex < mSeries.size())
        {
            GraphSeries series = mSeries.get(channelIndex);
            series.setFuturePlotStyle(plotStyle.fillType, plotStyle.lineColor, plotStyle.fillColor);
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public void update()
    {
        for (int i = 0; i < mSeries.size(); i++)
        {
            GraphSeries series = mSeries.get(i);
            ChannelDataSource dataSource = mChannels.get(i);

            series.loadSeriesPoints(getHeight(), getWidth(), dataSource.getHistoryData(),
                    dataSource.getFutureData(), mGoalStart, mGoalRemaining, mGoal);
        }

        // Trigger android to redraw this view
        invalidate();
    }

    public void clearDataFromChannels()
    {
        for (GraphSeries channel : mSeries)
        {
            channel.clearData();
        }
    }

    public GraphSeries getChannel(int channelIndex)
    {
        if (channelIndex < mSeries.size())
        {
            return mSeries.get(channelIndex);
        }
        return null;
    }

    public void setGoalRemaining(float goalRemaining)
    {
        mGoalRemaining = goalRemaining;
    }

    public void setGoal(float goal)
    {
        mGoal = goal;
    }

    public void setGoalStart(float goalStart)
    {
        mGoalStart = goalStart;
    }

    private float getCursorPosition()
    {
        return (mGoal - mGoalRemaining) / mGoal * getWidth();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        for (GraphSeries series : mSeries)
        {
            series.drawSeries(canvas);
        }

        // Draw cursor
        float cursorPosition = getCursorPosition();
        canvas.drawLine(cursorPosition, getHeight(), cursorPosition, 0, mCursorPaint);
    }
}