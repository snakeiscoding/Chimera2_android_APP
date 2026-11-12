//package com.jht.chimera.ui.mainapp;
//
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.Intent;
//import android.content.ServiceConnection;
//import android.os.Handler;
//import android.os.IBinder;
//import android.util.Log;
//import android.view.View;
//import android.view.ViewGroup;
//
//import androidx.annotation.NonNull;
//
//import com.jht.chimera.R;
//import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
//import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
//import com.jht.chimera.lib.graph.GraphLegendWidget;
//import com.jht.chimera.lib.graph.workoutgraph.WorkoutGraphThreaded;
//import com.jht.chimera.lib.widget.statarray.StatManager;
//import com.jht.chimera.lib.widget.statbar.StatOverlay;
//import com.jht.chimera.service.va.VAWrapper;
//import com.jht.chimera.ui.mainapp.graph.GraphSetup;
//import com.jht.chimera.ui.mainapp.workouttracking.WorkoutTrackingService;
//import com.jht.datamatrixlib.DataMatrix;
//import com.jht.virtualactive.VABaseActivity;
//import com.jht.virtualactive.VirtualActiveViewSystemInterface;
//import com.jht.virtualactive.IVirtualActiveWrapperCallback;
//
//public class VAWorkoutActivity extends VABaseActivity<VAWrapper> {
//
//    @Override public VAWrapper newWrapperInstance(@NonNull final Context context, @NonNull final IVirtualActiveWrapperCallback wrapperCallbacks) {
//        return new VAWrapper(context, wrapperCallbacks);
//    }
//
//    @Override protected void onResume() {
//        super.onResume();
//
//        //
//        // Setup the stats
//        //
//        inflater.inflate(R.layout.va_stat_layout, (ViewGroup) statView);
//        mStatOverlay = findViewById(R.id.stat_overlay);
//        mStatOverlay.start(new StatManager.IStatManagerNotifications() {
//            /**
//             * Calls a function required so that the stat columns will update visibility correctly.
//             * SurfaceView will not display overlays correctly otherwise.
//             */
//            @Override public void columnOpened() {
//                mStatOverlay.getParent().requestTransparentRegion(mStatOverlay);
//            }
//            @Override public void columnClosed() {}
//        });
//
//
//        //
//        // Setup the graph
//        //
//        inflater.inflate(R.layout.va_graph_layout, (ViewGroup) graphView);
//        mWorkoutGraph = graphView.findViewById(R.id.va_workout_graph);
//        mLegend = graphView.findViewById(R.id.va_graph_legend_widget);
//        bindService(new Intent(this, WorkoutTrackingService.class), mConnection, Context.BIND_AUTO_CREATE);
//        setupGraph();
//    }
//
//
//    @Override protected void onPause() {
//        super.onPause();
//
//        //
//        // Stop the stats
//        //
//        if (mStatOverlay != null) {
//            mStatOverlay.finish();
//            mStatOverlay = null;
//        }
//
//        ((ViewGroup) statView).removeAllViews();
//
//        //
//        // Cleanup the graph
//        //
//        unbindService(mConnection);
//        ((ViewGroup) graphView).removeAllViews();
//        mWorkoutGraph = null;
//        mLegend = null;
//    }
//
//
//    @NonNull @Override public VirtualActiveViewSystemInterface systemInterface() { return SYSTEM_INTERFACE; }
//}
