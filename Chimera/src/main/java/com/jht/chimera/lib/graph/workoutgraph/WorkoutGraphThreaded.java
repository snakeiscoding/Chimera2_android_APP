package com.jht.chimera.lib.graph.workoutgraph;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.graph.graphview.PlotStyle;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.workoutcontrollerlib.WorkoutController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class WorkoutGraphThreaded extends SurfaceView implements IWorkoutGraph {
    private static final String TAG = "WorkoutGraphThreaded";
    private final GraphRenderer mGraphRenderer;
    private final List<ChannelDef> mChannelDefs;
    private final RenderParams mRenderParams;

    private float mCursorPos = 0;

    private RenderThread mRenderThread;
    private AtomicInteger mRedrawRequest;
    private AtomicInteger mFadeIn;
    private AtomicInteger mFadeOut;

    private List<GraphRenderer.DistanceMarker> mileMarkers = new ArrayList<>();
    private List<GraphRenderer.DistanceMarker> kmMarkers = new ArrayList<>();


    private float mGoalAdjusted = 0;
    private float mGoalRemaining = 0;
    private float mStartGoal = 0;
    private int secondsElapsed = 0;

    private int goalType = ConsoleWorkoutVariables.GOAL_TYPE_TIME;

    private int graphWidthInPixels = 0;

    private boolean isMetric;//to determine if we show the mile markers or km markers.

    Paint mCursorPaint;

    DataMatrix mDataMatrix = new DataMatrix();

    public void finish(){
        mDataMatrix.finish();
    }

    public WorkoutGraphThreaded(Context context, AttributeSet attrs) {
        super(context, attrs);

        mCursorPaint = new Paint();
        mCursorPaint.setStrokeWidth(1);
        mCursorPaint.setColor(0xffffffff);

        Bitmap backgroundBitmap;
        Drawable backgroundDrawable = getBackground();

        if ((backgroundDrawable instanceof BitmapDrawable)) {
            backgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();
            // This interferes with our drawing into our own surface (not sure how!) so clear it.
            setBackgroundResource(0);
        } else {
            backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_graph_background);
        }

        mGraphRenderer = new GraphRenderer(backgroundBitmap);
        mChannelDefs = new ArrayList<>();
        mRenderParams = new RenderParams();
        mRedrawRequest = new AtomicInteger(0);
        mFadeIn = new AtomicInteger(0);
        mFadeOut = new AtomicInteger(0);
        reset();

        // We want the graph to be composited over the background SurfaceView layer and a pixel format that supports alpha.
        setZOrderOnTop(false);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);

        // Install the callback that will fire every time the Window containing the underlying surface comes into view.
        getHolder().addCallback(new SurfaceHolderCallback());

    }

    private static class RenderParams {
        float mAlpha;
        boolean mModifyCurrentInterval;
        int mRebuildGraphData;

        RenderParams() {
            mModifyCurrentInterval = false;
            mAlpha = 1.0f;
            mRebuildGraphData = 0;
        }

        synchronized void setAlpha(float alpha) {
            mAlpha = alpha;
        }

        synchronized void copy(RenderParams src) {
            mAlpha = src.mAlpha;
            mModifyCurrentInterval = src.mModifyCurrentInterval;
            mRebuildGraphData = src.mRebuildGraphData;
        }
    }

    private class RenderThread extends Thread {
        // Limit to ~10fps max.
        long mMinFrametimeNanos = 50000000;

        // Thread lifetime.
        private volatile boolean mRun;

        // Provides the Canvas object we draw too.
        private final SurfaceHolder mSurfaceHolder;

        // Local copy of all data needed during drawing.
        private RenderParams mLocalRenderParms;

        private Bitmap drawBitmap;
        private Canvas drawCanvas;

        public RenderThread(SurfaceHolder surfaceHolder, int width, int height) {
            mRun = false;
            mSurfaceHolder = surfaceHolder;
            mLocalRenderParms = new RenderParams();
            setWillNotDraw(false);

            Bitmap.Config conf = Bitmap.Config.ARGB_8888;
            drawBitmap = Bitmap.createBitmap(width, height, conf);

            drawCanvas = new Canvas(drawBitmap);
        }

        public void setRunning(boolean bRun) {
            mRun = bRun;
        }

        @Override
        public void run() {
            super.run();

            while (mRun) {
                long startTime = System.nanoTime();
                if (mRedrawRequest.get() > 0) {
                    boolean animate = checkAnimation();

                    // Make a local copy of RenderParms from the main thread.
                    synchronized (mRenderParams) {
                        mLocalRenderParms.copy(mRenderParams);
                    }

                    // Attempt to lock the underlying surface for drawing.
                    Canvas canvas = null;
                    try {

                        if (drawCanvas != null) {
                            drawCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                            if (animate) {
                                //mGraphRenderer.draw(canvas, mLocalRenderParms.mAlpha,goalType);//
                                doDraw(drawCanvas,mLocalRenderParms);
                            } else {
                                // Actually do the drawing.
                                doDraw(drawCanvas, mLocalRenderParms);
                            }

                            if(mRun) {
                                synchronized (mSurfaceHolder) {
                                    canvas = mSurfaceHolder.lockCanvas();
                                }
                                if (canvas != null) {
                                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                                    canvas.drawBitmap(drawBitmap, 0, 0, null);
                                }
                            }
                            // Signal that a draw request has been satisfied.
                            mRedrawRequest.decrementAndGet();
                        }
                    } finally {
                        if (canvas != null) {
                            try {
                                // Show the contents of the Canvas and unlock the surface.
                                synchronized (mSurfaceHolder) {
                                    mSurfaceHolder.unlockCanvasAndPost(canvas);
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "WorkoutGraphThreaded unlockCanvasAndPost()", e);
                            }
                        }
                    }
                }

                // Throttle the frame-rate.
                long actualFrameTime = System.nanoTime() - startTime;
                //Log.d("DEBUG", "Drawtime(ms) = " + actualFrameTime / 1000000);
                if (actualFrameTime < mMinFrametimeNanos) {
                    // Sleep until it's time for the next frame.
                    try {
                        Thread.sleep((mMinFrametimeNanos - actualFrameTime) / 1000000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            drawBitmap.recycle();
        }

        private boolean checkAnimation() {
            if (mFadeIn.get() > 0) {
                mMinFrametimeNanos = 20000000;
                if (mRenderParams.mAlpha < 1) {
                    setAlpha(mRenderParams.mAlpha + 0.1f);
                    if (mRenderParams.mAlpha > 1) {
                        setAlpha(1);
                    }
                } else {
                    mFadeIn.getAndDecrement();
                    mMinFrametimeNanos = 50000000;
                }
                return true;
            } else if (mFadeOut.get() > 0) {

                mMinFrametimeNanos = 20000000;
                if (mRenderParams.mAlpha > 0) {
                    setAlpha(mRenderParams.mAlpha - 0.1f);
                    if (mRenderParams.mAlpha < 0) {
                        setAlpha(0);
                    }
                } else {
                    mFadeOut.getAndDecrement();
                    mMinFrametimeNanos = 50000000;
                }
                return true;
            }
            return false;
        }
    }

    private static class ChannelDef {
        ChannelDataSource mChannelDataSource;

        ChannelDef(ChannelDataSource channelDataSource) {
            mChannelDataSource = channelDataSource;
        }
    }

    private class SurfaceHolderCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            mRenderThread = new RenderThread(surfaceHolder, getMeasuredWidth(), getMeasuredHeight());
            mRenderThread.setRunning(true);
            mRenderThread.start();
            Log.d(TAG, "WorkoutGraphData render thread started");
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mRenderThread.setRunning(false);
            boolean retry = true;
            while (retry) {
                try {
                    mRenderThread.join();
                    retry = false;
                } catch (Exception e) {
                    Log.w(TAG, e);
                }
            }
            // This must happen after the thread has exited.
            mRedrawRequest.set(0);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            surfaceDestroyed(holder);
            surfaceCreated(holder);
        }
    }

    /**
     * Sets the goal value. The graph uses the ratio of goalRemaining/goal to determine the cursor
     * position.
     *
     * @param value The value for the goal
     */
    public void setWorkoutGoalAdjusted(float value) {
        mGoalAdjusted = value;
    }

    public void setWorkoutStartGoal(float goalValue) {
        mStartGoal = goalValue;
    }

    public void setSecondsElapsed(int seconds) {
        secondsElapsed = seconds;
    }

    /**
     * Sets the goalRemaining value. The graph uses the ratio of goalRemaining/goal to determine
     * the cursor position.
     *
     * @param value The value for the goal remaining
     */
    public void setGoalRemaining(float value) {
        mGoalRemaining = value;
    }

    public void setGoalTypeInt ( int goalType){
        //Log.d("GRAPH", "goal type set to "+goalType);
        this.goalType = goalType;
    }

    public double getXPos(int time) {
        if ( time > 0){
            float cursorPos = 0;
            if (mGoalAdjusted != 0) {
                cursorPos = graphWidthInPixels * ((mGoalAdjusted - mStartGoal) - getGoalRemaining()) / (mGoalAdjusted - mStartGoal);
            }
            if (cursorPos <= 0) cursorPos = 0;
            return (cursorPos*time)/secondsElapsed;
        }
        return 0;
    }

    public void setIsMetric(boolean isMetric){this.isMetric = isMetric;}

    public synchronized void setMileMarkerSeconds(List<Integer> mileMarkerSeconds){
        for (int i = 0; i < mileMarkerSeconds.size(); i++) {
            if (i < mileMarkers.size()) {
                mileMarkers.get(i).seconds = mileMarkerSeconds.get(i);
            } else {
                mileMarkers.add(new GraphRenderer.DistanceMarker(mileMarkerSeconds.get(i)));
            }
        }
    }

    public synchronized void setKmMarkerSeconds(List<Integer> kmMakerSeconds){
        for (int i = 0; i < kmMakerSeconds.size(); i++) {
            if (i < kmMarkers.size()) {
                kmMarkers.get(i).seconds = kmMakerSeconds.get(i);
            } else {
                kmMarkers.add(new GraphRenderer.DistanceMarker(kmMakerSeconds.get(i)));
            }
        }
    }

    private void updateCursorPosition() {
        float goalRemaining = getGoalRemaining();
        float cursorPos = 0;
        if (mGoalAdjusted != 0) {
            cursorPos = graphWidthInPixels * ((mGoalAdjusted - mStartGoal) - goalRemaining) / (mGoalAdjusted - mStartGoal);
        }
        //Log.d("GRAPH", "mGoalRemaining:: "+goalRemaining);
        if (cursorPos <= 0) cursorPos = 0;
        //mCursorPos = (int) cursorPos;
        mCursorPos = cursorPos;
        mGraphRenderer.setCursorPos(mCursorPos);
    }

    public void updateDistanceMarkers(){

        if ( isMetric ) {//KM Marker Stuff
            for(GraphRenderer.DistanceMarker marker : kmMarkers) {
                marker.xPosition = getXPos(marker.seconds);
            }
        }
        else {//Mile Marker Stuff
            for(GraphRenderer.DistanceMarker marker : mileMarkers) {
                marker.xPosition = getXPos(marker.seconds);
            }
        }
    }


    private synchronized void doDraw(Canvas canvas, RenderParams renderParams) {

        graphWidthInPixels = getWidth();// Retrieve width of SurfaceView.
        updateCursorPosition ();
        updateDistanceMarkers();

        mGraphRenderer.setIsMetric(isMetric);

        int workoutState = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT);
        if ( workoutState == WorkoutController.WORKOUT_STAGE_COOLDOWN ) {
            mGraphRenderer.draw_CoolDown(canvas, renderParams.mAlpha, goalType, isMetric ? kmMarkers : mileMarkers);
        }
        else {
            mGraphRenderer.draw(canvas, renderParams.mAlpha, goalType, isMetric ? kmMarkers : mileMarkers);
        }
    }

    /**
     * Resets the graph to the default state
     */
    @Override
    public synchronized void reset() {
        mChannelDefs.clear();
        mGraphRenderer.reset();
        mCursorPos = 0;
        mRedrawRequest.set(0);

        mileMarkers.clear();
        kmMarkers.clear();
    }

    /**
     * Adds a channel that the graph uses for data.
     *
     * @param dataSource The data the graph will use for a single channel
     * @return The ID for the channel
     */
    @Override
    public synchronized int addChannel(ChannelDataSource dataSource) {
        mChannelDefs.add(new ChannelDef(dataSource));
        float min = dataSource.getMin();
        float max = dataSource.getMax();
        return mGraphRenderer.addDataChannel(min, max);
    }

    @Override
    public synchronized boolean setChannelHistoryPlotStyle(int channelIndex, PlotStyle plotStyle) {
        if (channelIndex < mChannelDefs.size()) {
            mGraphRenderer.getChannel(channelIndex).setHistoryPlotStyle(plotStyle.fillType, plotStyle.lineColor, plotStyle.fillColor);
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean setChannelFuturePlotStyle(int channelIndex, PlotStyle plotStyle) {
        if (channelIndex < mChannelDefs.size()) {
            mGraphRenderer.getChannel(channelIndex).setFuturePlotStyle(plotStyle.fillType, plotStyle.lineColor, plotStyle.fillColor);
            return true;
        }
        return false;
    }

    @Override
    public void update() {
        updateGraphPoints();
        mRedrawRequest.incrementAndGet();
    }

    private void updateGraphPoints() {
        for (int i = 0; i < mChannelDefs.size(); i++) {
        ChannelDataSource channel = mChannelDefs.get(i).mChannelDataSource;
        GraphSeries graphLine = mGraphRenderer.getChannel(i);
        //for ( int j = 0; j < channel.getHistoryData().length; j++){
        //    Log.d("GRAPH_123", "updateGraphPoints: ["+channel.getHistoryData()[j].goalValue+"]["+channel.getHistoryData()[j].data+"]");
        //}
        //Log.d("GRAPH_12345", "updateGraphPoints: mGoalAdjusted:"+mGoalAdjusted);
        graphLine.loadSeriesPoints(getHeight(), getWidth(), channel.getHistoryData(),
                channel.getFutureData(), mStartGoal, getGoalRemaining(), mGoalAdjusted);
        }
    }

    @Override
    public void setAlpha(float alpha) {
        mRenderParams.setAlpha(alpha);
        mRedrawRequest.incrementAndGet();
    }

    public float getGoalRemaining(){
        int stageInt = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT);
        if (stageInt == WorkoutController.WORKOUT_STAGE_COOLDOWN) {
            boolean completed_full_workout = DataMatrix.getBoolean(WorkoutKeys.DATA_REACHED_FULL_WORKOUT_BOOL);
            if (completed_full_workout){
                return 0;
            }
        }
        return mGoalRemaining;
    }

}
