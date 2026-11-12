package com.jht.chimera.ui.mainapp.graph;

import android.database.ContentObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.graph.GraphLegendWidget;
import com.jht.chimera.lib.graph.graphview.PlotStyle;
import com.jht.chimera.lib.graph.workoutgraph.ChannelDataSource;
import com.jht.chimera.lib.graph.workoutgraph.GraphSeries;
import com.jht.chimera.lib.graph.workoutgraph.WorkoutGraphThreaded;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.chimera.ui.mainapp.workouttracking.DistanceMarkers;
import com.jht.data.DataPoint;
import com.jht.data.GraphChannelChange;
import com.jht.data.GraphInfoChannel;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.interfaces.models.WorkoutGraphConfig;
import com.jht.workoutcontrollerlib.WorkoutController;
import com.jht.workoutdata.client.WorkoutDataClient;
import com.jht.workoutdata.model.FutureProfile;
import com.jht.workoutdata.model.FutureProfilePoint;
import com.jht.workoutdata.model.WorkoutDataPoint;
import com.jht.workoutdata.model.WorkoutFutureData;
import com.jht.workoutdata.model.WorkoutMetric;
import com.jht.workoutdata.query.WorkoutDataQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sets up a graph view and displays the correct relevant information on the graph relative to the current frame type.
 * <p></p>
 * Adapted from the Passport 2.0 graph setup.
 * <p></p>
 * Updated by Andy Rosene
 */
public class GraphSetup {
    private static final String TAG = "GraphSetup";
    private static final int COLOR_GRAPH_LIGHT_YELLOW = 0xfff2a61a;
    private static final int COLOR_GRAPH_DARK_YELLOW = 0xffa5751c;
    private static final int COLOR_GRAPH_BLUE = 0xff00a0d0;
    private static final int COLOR_GRAPH_DARK_BLUE = 0xff0060a0;
    private static final int COLOR_GRAPH_RED = 0xffff0000;
    private static final int COLOR_GRAPH_GREEN = 0xff00cc00;
    private static final int COLOR_GRAPH_GRAY_ALPHA_20 = 0x30909090;
    private static final int COLOR_GRAPH_GRAY_ALPHA_40 = 0x60909090;
    //private static final int COLOR_TEXT_GRAY = 0x80a0a0a0;
    //private static final int COLOR_TEXT_LIGHT_GRAY = 0xffb5b5b5;

    @NonNull
    private ChannelDataSource mInclineChannel;

    @NonNull
    private ChannelDataSource mSpeedChannel;

    @NonNull
    private ChannelDataSource mResistanceChannel;

    @NonNull
    private ChannelDataSource mRpmChannel;

    @NonNull
    private ChannelDataSource mSpmChannel;

    @NonNull
    private ChannelDataSource mHeartRateChannel;

    @NonNull
    private ChannelDataSource mWattsChannel;

    @NonNull
    private WorkoutGraphThreaded mWorkoutGraph;

    @NonNull
    private GraphLegendWidget mGraphLegendWidget;

    @NonNull
    private final DataMatrix mDataMatrix = new DataMatrix();

    // Cached workout future, fetched from the WorkoutData ContentProvider.
    // Refreshed only when the provider fires its future-change notification (setFuture/clear),
    // instead of deserializing a parcelable on every graph tick for every metric.
    @Nullable
    private volatile WorkoutFutureData mProviderFuture;
    // Observer for the provider's future-change notifications (separate URI from history).
    @Nullable
    private ContentObserver mFutureObserver;
    // Per-metric projected future points, invalidated whenever mProviderFuture updates.
    // NO_FUTURE marks "projected, nothing for this metric" (ConcurrentHashMap can't hold null).
    private final ConcurrentHashMap<WorkoutMetric, DataPoint[]> mFutureCache = new ConcurrentHashMap<>();
    private static final DataPoint[] NO_FUTURE = new DataPoint[0];

    @Nullable
    private ContentObserver mProviderObserver;
    @Nullable
    private HandlerThread mObserverThread;

    // Client for reading history from the ContentProvider.
    @Nullable
    private WorkoutDataClient mWorkoutDataClient;

    @NonNull
    private WorkoutDataClient getWorkoutDataClient() {
        if (mWorkoutDataClient == null) {
            mWorkoutDataClient = new WorkoutDataClient(ContextUtil.getApplicationContext());
        }
        return mWorkoutDataClient;
    }


    public GraphSetup(@NonNull final WorkoutGraphThreaded workoutGraph,
                      @NonNull final GraphLegendWidget graphLegendWidget) {
        mWorkoutGraph = workoutGraph;
        mGraphLegendWidget = graphLegendWidget;

        // Init the channels
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        mInclineChannel = new ChannelDataSource(config.incline.getMin(), config.incline.getMax());
        mSpeedChannel = new ChannelDataSource(config.speedKph.getMin(), config.speedKph.getMax()); //TODO: does this need to based on current unit?
        mResistanceChannel = new ChannelDataSource(config.resistance.getMin(), config.resistance.getMax());
        mRpmChannel = new ChannelDataSource(0, 200); //TODO: get actual RPM max value
        mSpmChannel = new ChannelDataSource(0, 200); //TODO: get actual RPM max value
        mHeartRateChannel = new ChannelDataSource(0, 220); //TODO: Get actual max heart rate
        mWattsChannel = new ChannelDataSource(0, 525);

    }

    /**
     * Starts updating the graph from the service
     */
    public void startGraphUpdates() {
        configureWorkoutGraph();
        registerCallbacks();
    }

    /**
     * Stops data matrix and service updates
     */
    public void stopGraphUpdates() {
        mDataMatrix.finish();
        mWorkoutGraph.finish();

        if (mProviderObserver != null) {
            getWorkoutDataClient().unregisterChangeListener(mProviderObserver);
            mProviderObserver = null;
        }
        if (mFutureObserver != null) {
            getWorkoutDataClient().unregisterChangeListener(mFutureObserver);
            mFutureObserver = null;
        }
        if (mObserverThread != null) {
            mObserverThread.quitSafely();
            mObserverThread = null;
        }
    }


    private void configureWorkoutGraph() {
        mGraphLegendWidget.reset();

        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        int workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);

        mGraphLegendWidget.addItem(ContextUtil.getString(R.string.heart_rate_uc), COLOR_GRAPH_RED);

        if (workoutId == WorkoutIds.COOPER_FITNESS_TEST) {
            configureCooperWattsGraph();
        }
        else if(workoutId == WorkoutIds.SUBMAX_FITNESS_TEST) {
            // Add heart rate to every frame
            mWorkoutGraph.setChannelHistoryPlotStyle(mWorkoutGraph.addChannel(mHeartRateChannel), PlotStyle.PlotStyleLine(COLOR_GRAPH_RED));
        }
        else {
            switch (frameConfig.machineClassType) {
                case Treadmill:
                    configureTreadmillGraph();
                    break;

                case Ascent:
                    configureAscentGraph();
                    break;

                case Elliptical:
                case RecumbentElliptical:
                case Bike:
                case SpinBike:
                    configureBikeEllipticalGraph(workoutId);
                    break;
                case Climbmill:
                case Stepper:
                    configureClimbmillStepperGraph();
                    break;
            }

            // Add heart rate to every frame
            mWorkoutGraph.setChannelHistoryPlotStyle(mWorkoutGraph.addChannel(mHeartRateChannel), PlotStyle.PlotStyleLine(COLOR_GRAPH_RED));
        }

    }

    private void updateChannel(@Nullable GraphInfoChannel channel, @NonNull ChannelDataSource channelSource) {
        if(channel != null) {
            channelSource.setHistoryProfileData(channel.getHistoryDataPoints());
            if (channel.getFutureDataPoints() != null) {
                channelSource.setFutureProfileData(channel.getFutureDataPoints());
            }
            mWorkoutGraph.setWorkoutStartGoal(channel.getGoalOffset());
        }
    }

    private void updateChannelData() {
        // Query the history once (shared configurable window, same as the Compose graph) and
        // project each metric into a GraphInfoChannel, instead of reading a per-metric
        // DATA_*_GRAPH_DATA_PARCELABLE from the DataMatrix.
        // NOTE: queryPointsBlocking does cross-process I/O on the calling (graph update) thread.
        final List<WorkoutDataPoint> points = getWorkoutDataClient()
                .queryPointsBlocking(WorkoutDataQuery.lastMinutes(WorkoutGraphConfig.getWindowMinutes()));
        final int goalType = DataMatrix.getInt(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_TYPE_INT);

        switch (FrameConfigManager.getInstance().getFrameConfig().machineClassType) {
            case Treadmill:
                updateChannel(buildChannel(points, WorkoutMetric.SPEED, goalType), mSpeedChannel);
                updateChannel(buildChannel(points, WorkoutMetric.INCLINE, goalType), mInclineChannel);
                break;
            case Elliptical:
            case RecumbentElliptical:
            case Bike:
            case SpinBike:
                updateChannel(buildChannel(points, WorkoutMetric.SPEED, goalType), mSpeedChannel);
                updateChannel(buildChannel(points, WorkoutMetric.RESISTANCE, goalType), mResistanceChannel);
                updateChannel(buildChannel(points, WorkoutMetric.RPM, goalType), mRpmChannel);
                break;
            case Ascent:
                updateChannel(buildChannel(points, WorkoutMetric.SPEED, goalType), mSpeedChannel);
                updateChannel(buildChannel(points, WorkoutMetric.INCLINE, goalType), mInclineChannel);
                updateChannel(buildChannel(points, WorkoutMetric.RESISTANCE, goalType), mResistanceChannel);
                updateChannel(buildChannel(points, WorkoutMetric.RPM, goalType), mRpmChannel);
                break;
            case Stepper:
            case Climbmill:
                updateChannel(buildChannel(points, WorkoutMetric.RESISTANCE, goalType), mResistanceChannel);
                updateChannel(buildChannel(points, WorkoutMetric.SPM, goalType), mSpmChannel);
                break;
            case NUM_MACHINE_CLASSIFICATION_TYPES:
                break;
        }
        updateChannel(buildChannel(points, WorkoutMetric.HEART_RATE, goalType), mHeartRateChannel);
        updateChannel(buildChannel(points, WorkoutMetric.WATTS, goalType), mWattsChannel);
    }

    /**
     * Projects a single metric from the provider point history into a
     * {@link GraphInfoChannel}. Goal axis mirrors the old GraphManager; future-profile data is
     * unused so it is left null.
     */
    private GraphInfoChannel buildChannel(@NonNull List<WorkoutDataPoint> points,
                                          @NonNull WorkoutMetric metric, int goalType) {
        final ArrayList<Float> timeMs = new ArrayList<>(points.size());
        final ArrayList<Float> goalValues = new ArrayList<>(points.size());
        final ArrayList<GraphChannelChange> dataValues = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            final WorkoutDataPoint p = points.get(i);
            timeMs.add(p.getElapsedTimeSec() * 1000f);
            goalValues.add(goalAxisValue(goalType, p));
            // The provider stores raw values (heartRate 0 = no strap).
            // Map invalid heart-rate readings to the pen-up sentinel like GraphManager did, so
            // GraphSeries draws a gap instead of a line along zero.
            final float raw = p.valueOf(metric);
            final float value = (metric == WorkoutMetric.HEART_RATE && raw <= 0f)
                    ? GraphSeries.INVALID_PIXEL
                    : raw;
            dataValues.add(new GraphChannelChange(i, value));
        }
        // History comes from the provider; the future profile comes from the provider's
        // future endpoint, cached per metric (see refreshProviderFuture / futureDataFor).
        return new GraphInfoChannel(timeMs, goalValues, dataValues, futureDataFor(metric));
    }

    /**
     * Builds the future-profile points for a metric from the provider-mirrored workout
     * future, mirroring GraphManager's projection (time goals only, skipped during cooldown,
     * one DataPoint per future point with timeMs on the goal axis).
     * <p>
     * We intentionally do NOT read the DATA_*_GRAPH_DATA_PARCELABLE channels here: those reads
     * return null in the Virtual Active process, and the provider only stores past history.
     */
    @Nullable
    private DataPoint[] futureDataFor(@NonNull WorkoutMetric metric) {
        // Future data only applies to time goals (mirrors GraphManager.getGraphInfo).
        final int goalType = DataMatrix.getInt(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_TYPE_INT);
        if (goalType != WorkoutKeys.Constants.GOAL_TYPE_TIME
                && goalType != WorkoutKeys.Constants.GOAL_TYPE_TIME_STAGE) {
            return null;
        }
        // The future should not change while in cooldown (mirrors GraphManager).
        if (DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT) == WorkoutKeys.Constants.WORKOUT_STAGE_COOLDOWN) {
            return null;
        }

        // Cached projection; only recomputed after the workout future changes (cache cleared by
        // refreshProviderFuture when the provider's future-change notification fires).
        DataPoint[] cached = mFutureCache.get(metric);
        if (cached == null) {
            cached = projectFuture(metric);
            mFutureCache.put(metric, cached);
        }
        if (cached.length == 0) return null;
        // Clamp per tick, NOT in the cache — the cursor advances every
        // second between future updates, so cached points steadily fall behind it.
        return clampFutureToCursor(cached);
    }

    /**
     * Prevents the future from drawing behind the current cursor.
     * Points whose time has already passed are dropped and replaced with a single synthetic
     * point AT the cursor carrying the in-effect profile value, so the gray line starts at the
     * cursor at the height the profile currently commands (GraphSeries then extends it forward).
     * Mirrors the in-effect/upcoming split used by the Compose graph.
     */
    @NonNull
    private DataPoint[] clampFutureToCursor(@NonNull DataPoint[] future) {
        final float nowMs = DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT);
        int firstUpcoming = 0;
        while (firstUpcoming < future.length && future[firstUpcoming].getGoalValue() <= nowMs) {
            firstUpcoming++;
        }
        if (firstUpcoming == 0) return future; // nothing behind the cursor

        final DataPoint inEffect = future[firstUpcoming - 1];
        final DataPoint[] clamped = new DataPoint[future.length - firstUpcoming + 1];
        clamped[0] = new DataPoint(nowMs, nowMs, inEffect.getData());
        System.arraycopy(future, firstUpcoming, clamped, 1, future.length - firstUpcoming);
        return clamped;
    }

    /**
     * Re-fetches the future profiles from the WorkoutData ContentProvider,
     * invalidates the per-metric projections, and redraws. Runs on the observer thread; called
     * when the provider's future-change notification fires (setFuture/clear) and once at startup.
     */
    private void refreshProviderFuture() {
        try {
            mProviderFuture = getWorkoutDataClient().getFutureBlocking();
        } catch (Exception e) {
            Log.e(TAG, "Failed to fetch workout future from provider", e);
            mProviderFuture = null; // keep drawing history; future re-fetched on next change
        }
        mFutureCache.clear();
        polledWorkout(); // redraw with the new future
    }

    /**
     * Projects the provider-cached future profile for [metric] into
     * DataPoints (one per point, timeMs on the goal axis). Returns {@link #NO_FUTURE} when
     * there is no future or no profile for the metric (e.g. RPM/SPM/HR are never scripted).
     */
    @NonNull
    private DataPoint[] projectFuture(@NonNull WorkoutMetric metric) {
        final WorkoutFutureData future = mProviderFuture;
        final FutureProfile profile = future != null ? future.profileFor(metric) : null;
        if (profile == null) return NO_FUTURE;

        final List<FutureProfilePoint> points = profile.getPoints();
        final DataPoint[] dataPoints = new DataPoint[points.size()];
        for (int i = 0; i < points.size(); i++) {
            final FutureProfilePoint point = points.get(i);
            final float timeMs = point.getTimeSec() * 1000f;
            dataPoints[i] = new DataPoint(timeMs, timeMs, point.getValue());
        }
        return dataPoints;
    }

    /** Goal-axis value for a point, matching the old GraphManager mapping. */
    private float goalAxisValue(int goalType, @NonNull WorkoutDataPoint p) {
        switch (goalType) {
            case WorkoutKeys.Constants.GOAL_TYPE_DISTANCE:
            case WorkoutKeys.Constants.GOAL_TYPE_DISTANCE_STAGE:
                return p.getDistanceKm();
            case WorkoutKeys.Constants.GOAL_TYPE_CALORIES:
            case WorkoutKeys.Constants.GOAL_TYPE_CALORIES_STAGE:
                return p.getTotalCalories();
            case WorkoutKeys.Constants.GOAL_TYPE_FLOORS:
            case WorkoutKeys.Constants.GOAL_TYPE_FLOORS_STAGE:
                return p.getTotalFloors();
            case WorkoutKeys.Constants.GOAL_TYPE_NONE:
            default:
                return p.getElapsedTimeSec() * 1000f; // time goals (and default)
        }
    }

    public void polledWorkout() {
        updateChannelData();
        int seconds = DataMatrix.getInt(WorkoutKeys.DATA_PRE_COOL_DOWN_TIME_ELAPSED_MS_INT) / 1000;
        int goalType = DataMatrix.getInt(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_TYPE_INT);

        if (DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT) == WorkoutKeys.Constants.WORKOUT_STAGE_WORKOUT) {
            boolean isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);

            mWorkoutGraph.setIsMetric(isMetric);

            mWorkoutGraph.setSecondsElapsed(seconds);
            //mWorkoutGraph.setTimeGoalSeconds(goalSeconds);//
            mWorkoutGraph.setGoalTypeInt(goalType);
        }
        mWorkoutGraph.update();
    }



    private void configureTreadmillGraph() {
        mGraphLegendWidget.addItem(ContextUtil.getString(R.string.speed_uc), COLOR_GRAPH_BLUE);
        mGraphLegendWidget.addItem(ContextUtil.getString(R.string.incline_uc), COLOR_GRAPH_LIGHT_YELLOW);
        int inclineIndex = mWorkoutGraph.addChannel(mInclineChannel);
        Log.d(TAG, "adding incline future plot style");
        mWorkoutGraph.setChannelHistoryPlotStyle(inclineIndex, PlotStyle.PlotStyleLinePlusFilled(COLOR_GRAPH_LIGHT_YELLOW, COLOR_GRAPH_DARK_YELLOW));
        mWorkoutGraph.setChannelFuturePlotStyle(inclineIndex, PlotStyle.PlotStyleFilled(COLOR_GRAPH_GRAY_ALPHA_40));

        int speedIndex = mWorkoutGraph.addChannel(mSpeedChannel);
        mWorkoutGraph.setChannelHistoryPlotStyle(speedIndex, PlotStyle.PlotStyleLine(COLOR_GRAPH_BLUE));
        mWorkoutGraph.setChannelFuturePlotStyle(speedIndex, PlotStyle.PlotStyleLine(COLOR_GRAPH_DARK_BLUE));
    }

    private void configureAscentGraph() {
        mGraphLegendWidget.addItem(ContextUtil.getString(R.string.rpm_uc), COLOR_GRAPH_GREEN);
        mGraphLegendWidget.addItem(ContextUtil.getString(R.string.resistance_uc), COLOR_GRAPH_LIGHT_YELLOW);
        int resistanceIndex = mWorkoutGraph.addChannel(mResistanceChannel);
        mWorkoutGraph.setChannelHistoryPlotStyle(resistanceIndex, PlotStyle.PlotStyleLinePlusFilled(COLOR_GRAPH_LIGHT_YELLOW, COLOR_GRAPH_DARK_YELLOW));
        mWorkoutGraph.setChannelFuturePlotStyle(resistanceIndex, PlotStyle.PlotStyleFilled(COLOR_GRAPH_GRAY_ALPHA_40));

        mGraphLegendWidget.addItem(ContextUtil.getString(R.string.incline_uc), COLOR_GRAPH_BLUE);
        int inclineIndex = mWorkoutGraph.addChannel(mInclineChannel);
        mWorkoutGraph.setChannelHistoryPlotStyle(inclineIndex, PlotStyle.PlotStyleLine(COLOR_GRAPH_BLUE));
        mWorkoutGraph.setChannelFuturePlotStyle(inclineIndex, PlotStyle.PlotStyleLine(COLOR_GRAPH_DARK_BLUE));

        int rpmIndex = mWorkoutGraph.addChannel(mRpmChannel);
        mWorkoutGraph.setChannelHistoryPlotStyle(rpmIndex, PlotStyle.PlotStyleLine(COLOR_GRAPH_GREEN));
    }

    private void configureBikeEllipticalGraph(int workoutId) {
        mGraphLegendWidget.addItem(ContextUtil.getString(R.string.rpm_uc), COLOR_GRAPH_GREEN);
        if(workoutId != WorkoutIds.CONSTANT_WATTS && !WorkoutIds.isMigros(workoutId)) {
            mGraphLegendWidget.addItem(ContextUtil.getString(R.string.resistance_uc), COLOR_GRAPH_LIGHT_YELLOW);
            int resistanceIndex = mWorkoutGraph.addChannel(mResistanceChannel);
            mWorkoutGraph.setChannelHistoryPlotStyle(resistanceIndex, PlotStyle.PlotStyleLinePlusFilled(COLOR_GRAPH_LIGHT_YELLOW, COLOR_GRAPH_DARK_YELLOW));
            mWorkoutGraph.setChannelFuturePlotStyle(resistanceIndex, PlotStyle.PlotStyleFilled(COLOR_GRAPH_GRAY_ALPHA_40));
        }

        int rpmIndex = mWorkoutGraph.addChannel(mRpmChannel);
        mWorkoutGraph.setChannelHistoryPlotStyle(rpmIndex, PlotStyle.PlotStyleLine(COLOR_GRAPH_GREEN));
    }

    private void configureCooperWattsGraph() {
        mGraphLegendWidget.addItem(ContextUtil.getString(R.string.watts_uc), COLOR_GRAPH_LIGHT_YELLOW);
        int wattsIndex = mWorkoutGraph.addChannel(mWattsChannel);
        mWorkoutGraph.setChannelHistoryPlotStyle(wattsIndex, PlotStyle.PlotStyleLinePlusFilled(COLOR_GRAPH_LIGHT_YELLOW, COLOR_GRAPH_DARK_YELLOW));
        mWorkoutGraph.setChannelFuturePlotStyle(wattsIndex, PlotStyle.PlotStyleFilled(COLOR_GRAPH_GRAY_ALPHA_40));
    }

    private void configureClimbmillStepperGraph() {
        mGraphLegendWidget.addItem(ContextUtil.getString(R.string.spm_uc), COLOR_GRAPH_BLUE);
        mGraphLegendWidget.addItem(ContextUtil.getString(R.string.resistance_uc), COLOR_GRAPH_LIGHT_YELLOW);
        int resistanceIndex = mWorkoutGraph.addChannel(mResistanceChannel);
        mWorkoutGraph.setChannelHistoryPlotStyle(resistanceIndex, PlotStyle.PlotStyleLinePlusFilled(COLOR_GRAPH_LIGHT_YELLOW, COLOR_GRAPH_DARK_YELLOW));
        mWorkoutGraph.setChannelFuturePlotStyle(resistanceIndex, PlotStyle.PlotStyleFilled(COLOR_GRAPH_GRAY_ALPHA_40));

        int spmIndex = mWorkoutGraph.addChannel(mSpmChannel);
        mWorkoutGraph.setChannelHistoryPlotStyle(spmIndex, PlotStyle.PlotStyleLine(COLOR_GRAPH_BLUE));
        mWorkoutGraph.setChannelFuturePlotStyle(spmIndex, PlotStyle.PlotStyleLine(COLOR_GRAPH_DARK_BLUE));
    }

    Handler handler = new Handler();

    private void registerCallbacks() {
        mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_FLOAT, value -> {
            // Needs to be the absolute goal value, not the stage one
            // Note - Adding a small delay because the DATA_GOAL_TYPE_INT and DATA_GOAL_REMAINING_FLOAT might be incorrect
            //        so we might consider a better way to solve this if many more places have this issue
            handler.postDelayed(() -> {
                //int goalType = DataMatrix.getInt(WorkoutKeys.DATA_GOAL_TYPE_INT);
                int goalType = DataMatrix.getInt(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_TYPE_INT);

                //float goalRemaining = DataMatrix.getFloat(WorkoutKeys.DATA_GOAL_REMAINING_FLOAT);
                float goalRemaining = DataMatrix.getFloat(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_REMAINING_FLOAT);
                //Log.d("GRAPH", "value: "+value);
                //Log.d("GRAPH", "goalRemaining: "+goalRemaining);
                //Log.d("GRAPH", "goalType: "+goalType);

                float goalAdjusted = getAdjustedGoal(goalType, value, goalRemaining);
                //Log.d("GRAPH", "goalAdjusted: "+goalAdjusted);

                mWorkoutGraph.setWorkoutGoalAdjusted(goalAdjusted);
//                mWorkoutGraph.setGoalRemaining(goalRemaining);

//                int workoutState = mDataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT);
//                if ( workoutState == WorkoutController.WORKOUT_STAGE_WORKOUT ) {
//                    int secondsElapsed = DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT)/1000;
//                    mWorkoutGraph.setSecondsElapsed(secondsElapsed);
//                }

            }, 200);
        });

        mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_REMAINING_FLOAT, mWorkoutGraph::setGoalRemaining);

        // Drive graph refresh from the ContentProvider's change notifications
        // (fired on addPoint/clear, ~1/sec during a workout) instead of EVENT_GRAPH_DATA_UPDATED.
        // Delivered on a background thread since polledWorkout does cross-process I/O.
        mObserverThread = new HandlerThread("WorkoutDataGraphObserver");
        mObserverThread.start();
        final Handler observerHandler = new Handler(mObserverThread.getLooper());
        mProviderObserver = getWorkoutDataClient().registerChangeListener(observerHandler, this::polledWorkout);

        // Separate trigger for future-profile changes (fires only when the
        // future is replaced or cleared, not per history point). Refetch, invalidate, redraw.
        mFutureObserver = getWorkoutDataClient().registerFutureChangeListener(observerHandler, this::refreshProviderFuture);
        observerHandler.post(this::refreshProviderFuture); // seed the current future off this thread
        mDataMatrix.createParcelableAdapter(WorkoutKeys.EVENT_DISTANCE_MARKERS_PARCELABLE, DistanceMarkers.class, markers -> {

            mWorkoutGraph.setMileMarkerSeconds(markers.mileMarkers);
            mWorkoutGraph.setKmMarkerSeconds(markers.kmMarkers);
        });
    }

    /**
     * Adjusts the goal value so that is based on absolute workout values. This is useful for graphing so that the start
     * and end of the graph line up correctly.
     *
     * @param goalType      The goal type for the workout
     * @param goalValue     The current goal value
     * @param goalRemaining The remaining value left before goal
     * @return The adjusted goal value
     */
    private synchronized float getAdjustedGoal(final int goalType, final float goalValue, float goalRemaining) {
        float adjustedGoal;
        boolean isCoolDown = false;
        int workoutStage = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT);
        if (workoutStage == WorkoutController.WORKOUT_STAGE_COOLDOWN){
            isCoolDown = true;
        }
        // seems there is situation where goal may update before goal remaining, assume if zero remaining we can use the goal itself
        if (goalRemaining == 0) {
            goalRemaining = goalValue;
        }

        switch (goalType) {
            case WorkoutKeys.Constants.GOAL_TYPE_DISTANCE_STAGE:
                float currentDistance = DataMatrix.getFloat(isCoolDown ? WorkoutKeys.DATA_COOL_DOWN_VARIABLE_PROGRESS_FLOAT : WorkoutKeys.DATA_DISTANCE_KM_FLOAT);
                //Log.d("GRAPH", "currentDistance = "+currentDistance);
                adjustedGoal = currentDistance + goalRemaining;
                break;

            case WorkoutKeys.Constants.GOAL_TYPE_CALORIES_STAGE:
                //TODO: This is likely not accurate, test and fix by getting the float version from the workout controller if needed
                float currentCalories = DataMatrix.getFloat(isCoolDown ? WorkoutKeys.DATA_COOL_DOWN_VARIABLE_PROGRESS_FLOAT : WorkoutKeys.DATA_CALORIES_FLOAT);
                //Log.d("GRAPH", "currentCalories = "+currentCalories);
                adjustedGoal = currentCalories + goalRemaining;
                break;

            case WorkoutKeys.Constants.GOAL_TYPE_FLOORS_STAGE:
                //TODO: This is likely not accurate, test and fix by getting the float version from the workout controller if needed!
                float currentFloors = DataMatrix.getFloat(isCoolDown ? WorkoutKeys.DATA_COOL_DOWN_VARIABLE_PROGRESS_FLOAT : WorkoutKeys.DATA_FLOORS_FLOAT);
                //Log.d("GRAPH", "currentFloors = "+currentFloors);
                adjustedGoal = currentFloors + goalRemaining;
                break;

            case WorkoutKeys.Constants.GOAL_TYPE_TIME_STAGE:
                float currentTime;
                if(isCoolDown) {
                    currentTime = DataMatrix.getFloat(WorkoutKeys.DATA_COOL_DOWN_VARIABLE_PROGRESS_FLOAT);
                }
                else {
                    currentTime = DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT);
                }
                //Log.d("GRAPH", "currentTime = "+currentTime);
                adjustedGoal = currentTime + goalRemaining;
                Log.d(TAG, "adjusted goal " + adjustedGoal + " " + currentTime + " " + goalRemaining);
                break;

            case WorkoutKeys.Constants.GOAL_TYPE_DISTANCE:
            case WorkoutKeys.Constants.GOAL_TYPE_CALORIES:
            case WorkoutKeys.Constants.GOAL_TYPE_FLOORS:
            case WorkoutKeys.Constants.GOAL_TYPE_TIME:
            default:
                adjustedGoal = goalValue; // If already based on workout do nothing
                break;
        }

        return adjustedGoal;
    }
}
