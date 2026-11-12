package com.jht.chimera.lib.graph.workoutgraph;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.Log;

import com.jht.chimera.R;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class GraphRenderer
{
    static class DistanceMarker {
        public double xPosition = 0;
        public int seconds;
        public double startBoundary = 0;
        public double endBoundary = 0;
        String text1;
        String text2;
        public DistanceMarker(int seconds) {
            this.seconds = seconds;
        }

        void setLabel(int index, String label, Paint paint) {
            text1 = index + " " + label;
            text2 = secToTime(seconds);
            double width = Math.max(paint.measureText(text1), paint.measureText(text2));
            startBoundary = xPosition - width / 2;
            endBoundary = xPosition + width / 2;

        }

        private String secToTime(int sec) {
            int seconds = sec % 60;
            int minutes = sec / 60;
            if (minutes >= 60) {
                int hours = minutes / 60;
                minutes %= 60;
                if (hours >= 24) {
                    int days = hours / 24;
                    return String.format(Locale.ENGLISH, "%d days %02d:%02d:%02d", days, hours % 24, minutes, seconds);
                }
                return String.format(Locale.ENGLISH, "%d:%02d:%02d", hours, minutes, seconds);

            }
            return String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds);
        }

    }
    private static final String TAG = "GraphRender";
    private final Paint mCursorPaint;
    private final Paint mBackgroundPaint;

    private final List<GraphSeries> mDataChannels;

    private float mCursorPos;
    private Bitmap mBackground;
    private boolean isMetric;

    public GraphRenderer(Bitmap background) {
        mCursorPaint = new Paint();
        mCursorPaint.setStrokeWidth(1);
        mCursorPaint.setColor(0xffffffff);
        mCursorPaint.setTextAlign(Paint.Align.CENTER);
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(0xffffffff);
        mDataChannels = new ArrayList<>();
        mCursorPos = 0;
        mBackground = background;
    }

    public void reset()
    {
        mDataChannels.clear();
    }

    public int addDataChannel(float min, float max) {
        mDataChannels.add(new GraphSeries(min, max));
        return mDataChannels.size() - 1;
    }

    public GraphSeries getChannel(int channelIndex) {
        if (channelIndex < mDataChannels.size()) {
            return mDataChannels.get(channelIndex);
        }
        return null;
    }

    public int getSize() {
        return mDataChannels.size();
    }

    public void setCursorPos(float cursorPos) {
        mCursorPos = cursorPos;
    }

    public void setIsMetric(boolean isMetric) {
        this.isMetric = isMetric;
    }

    private void drawBackground(Canvas canvas, float alpha, int canvasHeight, int canvasWidth) {
        Rect destRect = new Rect(0, 0, canvasWidth, canvasHeight);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mBackgroundPaint.setAlpha((int) (alpha * 0xff));
        canvas.drawBitmap(mBackground, null, destRect, mBackgroundPaint);
    }

    // Plot the different data channels
    private void drawDataChannels(Canvas canvas) {
        float highestXValue = mCursorPos;
        for (GraphSeries channel : mDataChannels) {
            try {
                for (int i = 0; i < channel.mHistoryLinePoints.size(); i++) {
                    if (channel.mHistoryLinePoints.get(i).pixelX > highestXValue) {
                        highestXValue = channel.mHistoryLinePoints.get(i).pixelX;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error", e);
            }
        }
        for (GraphSeries channel : mDataChannels) {
            try {
                if (highestXValue > channel.mHistoryLinePoints.get(channel.mHistoryLinePoints.size() - 1).pixelX) {
                    GraphSeries.GraphPoint graphPoint = new GraphSeries.GraphPoint(highestXValue, channel.mHistoryLinePoints.get(channel.mHistoryLinePoints.size() - 1).pixelY);
                    channel.mHistoryLinePoints.remove(channel.mHistoryLinePoints.size() - 1);
                    channel.mHistoryLinePoints.add(graphPoint);
                    //Log.d("GRAPH", "New Last Point Added");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error", e);
            }
            channel.drawSeries(canvas);
        }
    }

    private void drawCursor(Canvas canvas, float alpha, int canvasHeight, float cursorPosition){
        mCursorPaint.setAlpha((int) (alpha * 0xff));
        canvas.drawLine(cursorPosition, canvasHeight, cursorPosition, 0, mCursorPaint);
    }
    private void drawMarkers(Canvas canvas, List<DistanceMarker> markers, String label) {
        DistanceMarker previousMarker = null;
        for(int i = markers.size() - 2; i >= 0; i--) {
            DistanceMarker marker = markers.get(i);
            if (marker.xPosition != 0) {
                marker.setLabel(i + 1, label, mCursorPaint);
                drawMarkerLine(canvas, marker);
                if (previousMarker == null || previousMarker.startBoundary > marker.endBoundary) {
                    drawMarker(canvas, marker);
                    previousMarker = marker;
                }
            }
        }
    }

    //Draw on the canvas
    public void draw(Canvas canvas, float alpha, int goalType, List<DistanceMarker> markers) {
        final int canvasHeight = canvas.getHeight();
        final int canvasWidth = canvas.getWidth();
        // Draw the background.
        drawBackground(canvas, alpha, canvasHeight, canvasWidth);
        // Plot the different data channels
        drawDataChannels(canvas);
        float cursorPosition = mCursorPos;
        for (GraphSeries channel : mDataChannels) {
            //Log.d("GRAPH", "----------------------------------------");
            //for ( int i = 0; i < channel.mHistoryLinePoints.size(); i++){
            //  Log.d("GRAPH", "index "+i+":  x value = "+channel.mHistoryLinePoints.get(i).pixelX);
            //}
            //Log.d("GRAPH", "cursor Pos = "+cursorPosition);
            //If for some reason a graph data point's x value is larger than the cursor position,
            // this will set the cursor x pixel position equal to the largest x pixel value of the
            // graph data to prevent it from going past the cursor line.
            for (int j = 0; j < channel.mHistoryLinePoints.size(); j++) {
                if (channel.mHistoryLinePoints.get(j).pixelX > cursorPosition) {
                    cursorPosition = channel.mHistoryLinePoints.get(j).pixelX;
                }
            }
        }
        // Draw cursor at the given cursor position
        drawCursor(canvas, alpha, canvasHeight, cursorPosition);
        // Draw the distance markers
        drawDistanceMarkers(canvas, goalType, markers);
    }

    public void draw_CoolDown(Canvas canvas, float alpha, int goalType, List<DistanceMarker> markers){
        final int canvasHeight = canvas.getHeight();
        final int canvasWidth = canvas.getWidth();
        // Draw the background.
        drawBackground(canvas, alpha, canvasHeight, canvasWidth);
        // Plot the different data channels
        drawDataChannels(canvas);
        // Draw the distance markers
        drawDistanceMarkers(canvas,goalType,markers);
    }

    private void drawDistanceMarkers(Canvas canvas, int goalType, List<DistanceMarker> markers) {
        switch (goalType) {
            case ConsoleWorkoutVariables.GOAL_TYPE_TIME:
            case ConsoleWorkoutVariables.GOAL_TYPE_TIME_STAGE:
                drawMarkers(canvas, markers, isMetric ? StringLookup.get(R.string.km_lc) : StringLookup.get(R.string.mi_lc));
                break;
            default:
                break;
        }
    }

    private void drawMarker(Canvas canvas, DistanceMarker marker) {
        canvas.drawText(marker.text1, (float) marker.xPosition, 15, mCursorPaint);//Top Text Label
        canvas.drawText(marker.text2, (float) marker.xPosition, 30, mCursorPaint);//Bottom Text Label
    }

    private void drawMarkerLine(Canvas canvas, DistanceMarker marker) {
        canvas.drawLine((float) marker.xPosition, canvas.getHeight(), (float) marker.xPosition, (mCursorPaint.getTextSize() * 2) + 10, mCursorPaint);//Short Line
    }

}
