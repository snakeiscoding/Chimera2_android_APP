package com.jht.chimera.lib.graph.workoutgraph;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;

import com.jht.chimera.lib.graph.graphview.PlotStyle;
import com.jht.data.DataPoint;
import com.jht.data.GraphDataParcelableKt;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the necessary information to draw a channel of information on the graph. This class also
 * contains the necessary methods to draw the line on a canvas.
 */
public class GraphSeries {
    /**
     * A single point on a graph. When connected together they should form a line.
     */
    public static class GraphPoint {
        public final float pixelX;
        public final float pixelY;

        GraphPoint(float pixelX, float pixelY) {
            this.pixelX = pixelX;
            this.pixelY = pixelY;
        }

        public boolean equals(GraphPoint point) {
            return pixelX == point.pixelX &&
                    pixelY == point.pixelY;
        }
    }
    private static final String TAG = "GraphSeries";
    public static final float INVALID_PIXEL = GraphDataParcelableKt.INVALID_GRAPH_DATA;

    // Drawing parameters for graph lines
    private static final float STROKE_WIDTH = 2.0f;
    private Path mLinePath;
    private Paint mLinePaint;
    private Paint mFillPaint;

    private PlotStyle.FillType mHistoryFillType;
    private int mHistoryLineColor;
    private int mHistoryFillColor;

    private PlotStyle.FillType mFutureFillType;
    private int mFutureLineColor;
    private int mFutureFillColor;

    // Whether future drawing enabled
    private boolean mDrawableFuture;

    // List of data samples to be plotted.
    List<GraphPoint> mLinePoints = new ArrayList<>();
    List<GraphPoint> mHistoryLinePoints = new ArrayList<>();
    List<GraphPoint> mFutureLinePoints = new ArrayList<>();

    // Min & max values for each data sample.
    float mRangeMin;
    float mRangeMax;

    public GraphSeries(float rangeMin, float rangeMax) {
        mLinePath = new Path();
        mLinePaint = new Paint();
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(STROKE_WIDTH);
        mLinePaint.setStrokeCap(Paint.Cap.SQUARE);
        mLinePaint.setStrokeJoin(Paint.Join.ROUND);
        mLinePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setStrokeCap(Paint.Cap.BUTT);
        mFillPaint = new Paint();
        mFillPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mFillPaint.setStrokeWidth(0f);
        mFillPaint.setStrokeCap(Paint.Cap.BUTT);

        mHistoryLineColor = mHistoryFillColor = mFutureLineColor = mFutureFillColor = 0xffffffff;

        mRangeMin = rangeMin;
        mRangeMax = rangeMax;

        mDrawableFuture = false;
    }

    public void setRangeMin(float value) {
        mRangeMin = value;
    }

    public void setRangeMax(float value) {
        mRangeMax = value;
    }

    public void setHistoryPlotStyle(PlotStyle.FillType fillType, int lineColor, int fillColor) {
        mHistoryFillType = fillType;
        mHistoryLineColor = lineColor;
        mHistoryFillColor = fillColor;
    }

    public void setFuturePlotStyle(PlotStyle.FillType fillType, int lineColor, int fillColor) {
        Log.d(TAG, "setFuturePlotStyle 2 " + lineColor + " " + fillColor);
        mFutureFillType = fillType;
        mFutureLineColor = lineColor;
        mFutureFillColor = fillColor;
        mDrawableFuture = true;
    }

    public boolean hasDrawableFuture() {
        return mDrawableFuture;
    }

    public void clearData() {
        mLinePoints.clear();
        mHistoryLinePoints.clear();
        mFutureLinePoints.clear();
    }


    /**
     * Loads a set of points and creates data points to draw lines between to create the graph
     *
     * @param canvasHeight  The height of the canvas to draw on
     * @param canvasWidth   The width of the canvas to draw on
     * @param historyData   The points of data for the graph
     * @param futureData    The points of data for the future profile
     * @param goalRemaining The goal remaining for the current workout
     * @param workoutGoal   The current workout goal
     */
    public void loadSeriesPoints(float canvasHeight, float canvasWidth, DataPoint[] historyData,
                                 DataPoint[] futureData, float startGoal, float goalRemaining, float workoutGoal) {
        float goalAdjusted = workoutGoal - startGoal;
        float goalProgress = goalAdjusted - goalRemaining;

        // Remove excess data points
        List<DataPoint> historyListCleaned = cleanHistoryPoints(historyData, 0);
        List<DataPoint> futureListCleaned = cleanFutureData(futureData, goalProgress);

        List<GraphPoint> historyGraphPoints = new ArrayList<>();
        List<GraphPoint> futureGraphPoints = new ArrayList<>();
        float pixelsPerGoalUnit = canvasWidth / goalAdjusted;
        float pixelsPerValueUnit = canvasHeight / (mRangeMax - mRangeMin);

        if (historyListCleaned != null) {
            for (int i = 0; i < historyListCleaned.size(); i++) {
                DataPoint data = historyListCleaned.get(i);
                float pixelX1 = (data.getGoalValue() - startGoal) * pixelsPerGoalUnit;
                float pixelY1 = canvasHeight - (data.getData() - mRangeMin) * pixelsPerValueUnit;

                // we have received a lost data so don't add flat line and make the y pixel invalid
                if (data.getData() == INVALID_PIXEL) {
                    GraphPoint graphPoint = new GraphPoint(pixelX1, INVALID_PIXEL);
                    historyGraphPoints.add(graphPoint);
                    continue;
                }

                float pixelX2;
                float pixelY2;
                if (i + 1 < historyListCleaned.size()) {
                    // Create a point at the current height at the next point to create a flat line to the next point
                    DataPoint nextData = historyListCleaned.get(i + 1);
                    pixelX2 = (nextData.getGoalValue() - startGoal) * pixelsPerGoalUnit;
                    pixelY2 = pixelY1;
                } else {
                    // Create a point at the current height until the cursor
                    pixelX2 = goalProgress * pixelsPerGoalUnit;
                    pixelY2 = pixelY1;
                }
                GraphPoint graphPoint = new GraphPoint(pixelX1, pixelY1);
                GraphPoint graphPoint2 = new GraphPoint(pixelX2, pixelY2);
                historyGraphPoints.add(graphPoint);
                historyGraphPoints.add(graphPoint2);
            }


            mHistoryLinePoints = historyGraphPoints;
        }

        if (hasDrawableFuture() && futureListCleaned != null && mHistoryLinePoints.size() > 0) {
            // Needs an initial point to draw from where we currently are
            GraphPoint firstPoint = mHistoryLinePoints.get(mHistoryLinePoints.size() - 1);

            for (int i = 0; i < futureListCleaned.size(); i++) {
                DataPoint data = futureListCleaned.get(i);
                float pixelX1 = (data.getGoalValue() - startGoal) * pixelsPerGoalUnit;
                float pixelY1 = canvasHeight - (data.getData() - mRangeMin) * pixelsPerValueUnit;

                // Setup initial line to the fist change
                if (i == 0) {

                    if (firstPoint.pixelX != pixelX1) {
                        futureGraphPoints.add(firstPoint);

                        if (firstPoint.pixelY != pixelY1) {
                            GraphPoint secondPoint = new GraphPoint(pixelX1, firstPoint.pixelY);
                            futureGraphPoints.add(secondPoint);
                        }
                    }
                }

                float pixelX2;
                float pixelY2;
                if (i + 1 < futureListCleaned.size()) {
                    // Create a point at the current height at the next point to create a flat line to the next point
                    DataPoint nextData = futureListCleaned.get(i + 1);
                    pixelX2 = (nextData.getGoalValue() - startGoal) * pixelsPerGoalUnit;
                    pixelY2 = pixelY1;
                } else {
                    // Create a point at the current height until the end of the workout
                    pixelX2 = goalAdjusted * pixelsPerGoalUnit;
                    pixelY2 = pixelY1;
                }
                GraphPoint graphPoint = new GraphPoint(pixelX1, pixelY1);
                GraphPoint graphPoint2 = new GraphPoint(pixelX2, pixelY2);
                futureGraphPoints.add(graphPoint);
                futureGraphPoints.add(graphPoint2);
            }

            mFutureLinePoints = futureGraphPoints;
        }
    }

    /**
     * Removes unnecessary elements from the list to prevent issues when trying to build the graph
     * from the list of data points. Points are unnecessary if:
     * <p>
     * 1. The previous data point value was equal to the current point
     * 2. The point has the same X value as a subsequent point
     *
     * @param listToClean The list to clean
     * @return the cleaned list
     */
    private List<DataPoint> cleanFutureData(DataPoint[] listToClean, float startValue) {
        if (listToClean == null) return null;
        List<DataPoint> cleanList = new ArrayList<>();
        for (int i = 0; i < listToClean.length; i++) {
            DataPoint point = listToClean[i];
            boolean add = true;

            if (cleanList.size() > 0) {
                DataPoint lastPoint = cleanList.get(cleanList.size() - 1);
                if (point.getData() == lastPoint.getData()) add = false;
            }

            if (i + 1 < listToClean.length) {
                DataPoint nextPoint = listToClean[i + 1];
                if (point.getGoalValue() == nextPoint.getGoalValue()) add = false;
            }

            if (add) cleanList.add(point);
        }

        return cleanList;
    }

    private List<DataPoint> cleanHistoryPoints(DataPoint[] listToClean, float startValue) {
        if (listToClean == null) return null;
        List<DataPoint> cleanList = new ArrayList<>();
        for (int i = 0; i < listToClean.length; i++) {

            DataPoint point = listToClean[i];
            boolean add = true;

            // If the point is from before the start change the goal of that point to be the start
            if (point.getGoalValue() < startValue) {
                DataPoint nextPoint = listToClean[i + 1];
                // If there's another change closer to the start value then ignore this one
                if (nextPoint.getGoalValue() < startValue) {
                    add = false;
                } else {
                    point.copy(startValue, startValue, point.getData());
                }
            }

            if (cleanList.size() > 0) {
                DataPoint lastPoint = cleanList.get(cleanList.size() - 1);
                if (point.getData() == lastPoint.getData()) add = false;
            }

            if (i + 1 < listToClean.length) {
                DataPoint nextPoint = listToClean[i + 1];
                if (point.getGoalValue() == nextPoint.getGoalValue()) add = false;
            }

            if (add) cleanList.add(point);
        }

        return cleanList;
    }

    /**
     * Draws the graph line on the provided canvas using the loaded points
     *
     * @param canvas The canvas to draw on
     */
    public void drawSeries(Canvas canvas) {
        drawHistory(canvas);

        if (hasDrawableFuture()) {
            drawFuture(canvas);
        }
    }

    private void drawHistory(Canvas canvas) {

        if (mHistoryFillType == null){
            Log.e(TAG, "drawHistory: mHistoryFillType is null!" );
            return;
        }

        switch (mHistoryFillType) {
            case Line:
                mLinePaint.setColor(mHistoryLineColor);
                drawLine(canvas, mHistoryLinePoints, mLinePaint);
                break;
            case Filled:
                mFillPaint.setColor(mHistoryFillColor);
                drawFill(canvas, mHistoryLinePoints, mFillPaint);
                break;
            case LinePlusFilled:
                mLinePaint.setColor(mHistoryLineColor);
                mFillPaint.setColor(mHistoryFillColor);
                drawLineAndFill(canvas, mHistoryLinePoints, mLinePaint, mFillPaint);
                break;
        }
    }

    private void drawFuture(Canvas canvas) {

        //Log.d(TAG, "drawFuture " + mFutureFillType + " " + Integer.toHexString(mFutureLineColor) + " " + Integer.toHexString(mFutureFillColor) + " " + mFutureLinePoints.size());
        switch (mFutureFillType) {
            case Line:
                mLinePaint.setColor(mFutureLineColor);
                drawLine(canvas, mFutureLinePoints, mLinePaint);
                break;
            case Filled:
                mFillPaint.setColor(mFutureFillColor);
                drawFill(canvas, mFutureLinePoints, mFillPaint);
                break;
            case LinePlusFilled:
                mLinePaint.setColor(mFutureLineColor);
                mFillPaint.setColor(mFutureFillColor);
                drawLineAndFill(canvas, mFutureLinePoints, mLinePaint, mFillPaint);
                break;
        }
    }

    private void drawLine(Canvas canvas, List<GraphPoint> points, Paint linePaint) {
        GraphPoint lastPoint = null;

        for (int i = 0; i < points.size(); i++) {
            GraphPoint point = points.get(i);

            if (lastPoint != null && !lastPoint.equals(point)) {
                if (lastPoint.pixelY != INVALID_PIXEL && point.pixelY != INVALID_PIXEL) {
                    // Draw line
                    canvas.drawLine(lastPoint.pixelX, lastPoint.pixelY, point.pixelX, point.pixelY, linePaint);
                }
            }

            lastPoint = point;
        }
    }

    private void drawFill(Canvas canvas, List<GraphPoint> points, Paint fillPaint) {
        GraphPoint lastPoint = null;

        for (int i = 0; i < points.size(); i++) {
            GraphPoint point = points.get(i);

            if (lastPoint != null && !lastPoint.equals(point) && lastPoint.pixelX != point.pixelX) {
                // Draw fill
                RectF rectangle = new RectF(lastPoint.pixelX, lastPoint.pixelY, point.pixelX, canvas.getHeight());
                canvas.drawRect(rectangle, fillPaint);
            }

            lastPoint = point;
        }
    }

    private void drawLineAndFill(Canvas canvas, List<GraphPoint> points, Paint linePaint, Paint fillPaint) {
        GraphPoint lastPoint = null;

        for (int i = 0; i < points.size(); i++) {
            GraphPoint point = points.get(i);

            if (lastPoint != null && !lastPoint.equals(point)) {
                
                if (lastPoint.pixelX != point.pixelX) {
                    // Draw fill
                    RectF rectangle = new RectF(lastPoint.pixelX, lastPoint.pixelY, point.pixelX, canvas.getHeight());
                    canvas.drawRect(rectangle, fillPaint);
                }

                // Draw line
                canvas.drawLine(lastPoint.pixelX, lastPoint.pixelY, point.pixelX, point.pixelY, linePaint);
            }

            lastPoint = point;
        }
    }
}