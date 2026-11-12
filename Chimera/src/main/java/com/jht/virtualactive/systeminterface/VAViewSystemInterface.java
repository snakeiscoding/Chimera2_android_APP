package com.jht.virtualactive.systeminterface;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.jht.chimera.R;
import com.jht.chimera.lib.graph.GraphLegendWidget;
import com.jht.chimera.lib.graph.workoutgraph.WorkoutGraphThreaded;
import com.jht.chimera.lib.widget.statarray.StatManager;
import com.jht.chimera.lib.widget.statbar.StatOverlay;
import com.jht.chimera.ui.mainapp.graph.GraphSetup;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.virtualactive.activity.IVAViewSystemInterface;

/**
 * Virtual Active System Interface consumed by the Virtual Active View <strong>only</strong>. This
 * is included by the Virtual Active artifact using reflection.
 *
 * @author Andy Rosene (2019.11.15)
 */
@SuppressWarnings("unused")
public class VAViewSystemInterface implements IVAViewSystemInterface {
    private StatOverlay mStatOverlay;
    private GraphSetup mGraphSetup;
    private WorkoutGraphThreaded mWorkoutGraph;
    private GraphLegendWidget mLegend;

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public View inflatedProfileGraph(LayoutInflater inflater) {
        return inflater.inflate(R.layout.va_graph_layout, null);
    }

    @Override
    public void onCreate(Context context, DataMatrix dataMatrix, View profileGraphContainer) {
        mWorkoutGraph = profileGraphContainer.getRootView().findViewById(R.id.va_workout_graph);
        mLegend = profileGraphContainer.getRootView().findViewById(R.id.va_graph_legend_widget);
        mGraphSetup = new GraphSetup(mWorkoutGraph, mLegend);
        if (mStatOverlay != null) {
            mStatOverlay.closeStats();
            mStatOverlay.finish();
            mStatOverlay = null;
        }
    }

    @Override
    public void onResume(Context context, DataMatrix dataMatrix, View profileGraphContainer) {
        mGraphSetup.startGraphUpdates();
    }

    @Override
    public void onPause(Context context, DataMatrix dataMatrix) {
        // unbind from the WorkoutTrackingService
        mGraphSetup.stopGraphUpdates();

        if (mWorkoutGraph != null)
            mWorkoutGraph.reset();
    }

    @Override
    public void pauseWorkout() {

    }

    @Override
    public void resumeWorkout() {

    }

    @Override
    public void hideStatBar(View view) {
        if (mStatOverlay != null)
            mStatOverlay.closeStats();
    }

    @Override
    public void showStatBar(LayoutInflater inflater, View container) {
        if (mStatOverlay == null) {
            // Inflate the necessary stats view.
            inflater.inflate(R.layout.va_stat_layout, (ViewGroup) container);

            // initialize the StatOverlay
            mStatOverlay = container.findViewById(R.id.stat_overlay);
            mStatOverlay.start(new StatManager.IStatManagerNotifications() {
                /**
                 * Calls a function required so that the stat columns will update visibility
                 * correctly. SurfaceView will not display overlays correctly otherwise.
                 */
                @Override public void columnOpened() {
                    mStatOverlay.getParent().requestTransparentRegion(mStatOverlay);
                }

                @Override public void columnClosed() { /* do nothing */ }
            });
        }
    }

    @Override
    public void showProfileGraph() {
        if (mWorkoutGraph != null) mWorkoutGraph.setVisibility(VISIBLE);
        if (mLegend != null) mLegend.setVisibility(VISIBLE);
    }

    @Override
    public void hideProfileGraph() {
        if (mWorkoutGraph != null) mWorkoutGraph.setVisibility(GONE);
        if (mLegend != null) mLegend.setVisibility(GONE);
    }
}
