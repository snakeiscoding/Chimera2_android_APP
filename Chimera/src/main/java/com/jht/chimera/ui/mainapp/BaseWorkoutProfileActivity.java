package com.jht.chimera.ui.mainapp;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.jht.chimera.lib.widget.activity.RestringActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.datamatrixlists.DataMatrixListFactory;
import com.jht.chimera.lib.graph.GraphLegendWidget;
import com.jht.chimera.lib.graph.workoutgraph.WorkoutGraphThreaded;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.chimera.ui.mainapp.graph.GraphSetup;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.workoutcontrollerlib.WorkoutController;

public class BaseWorkoutProfileActivity extends RestringActivity {
    private static final String TAG = "BaseWorkoutProfileAc...";
    private WorkoutGraphThreaded mWorkoutGraph;
    private GraphLegendWidget mLeftLegend;

    private GraphSetup mGraphSetup;
    private DataMatrix dataMatrix = new DataMatrix();
    private ViewGroup graphLayout;
    private GraphLegendWidget mTopLegend;
    private TextView stageLabel;
    private TextView showHideGraphButton;
    private DataMatrixListFactory.WorkoutSnapshotConsumer snapshots = DataMatrixListFactory.snapshotListConsumer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setGraphAndLegend (ViewGroup viewGroup, WorkoutGraphThreaded wgt, GraphLegendWidget leftLegend, GraphLegendWidget topLegend)
    {
        graphLayout = viewGroup;
        mWorkoutGraph = wgt;
        mLeftLegend = leftLegend;
        mTopLegend = topLegend;
        setupDataMatrix();
        setupGraph();
    }

    public void setGraphVisibility (int visibility)
    {
        if (graphLayout != null) graphLayout.setVisibility (visibility);
        if (mWorkoutGraph != null) mWorkoutGraph.setVisibility(visibility);
        if (mLeftLegend != null) mLeftLegend.setVisibility(visibility);
        if (mTopLegend != null) mTopLegend.setVisibility (visibility);
    }

    public void setStageMessageVisbility(int visibility)
    {
        stageLabel = findViewById(R.id.workoutModeText);
        if (stageLabel != null) stageLabel.setVisibility(visibility);
    }

    private void setStageLabelText (int stringID)
    {
        if (stageLabel != null) stageLabel.post(() -> stageLabel.setText (stringID));
    }

    private void setupDataMatrix() {
        dataMatrix.createIntAdapter(WorkoutKeys.DATA_WORKOUT_STAGE_INT, value -> {
            boolean goalTypeNone = DataMatrix.getInt(WorkoutKeys.DATA_GOAL_TYPE_INT) == WorkoutKeys.Constants.GOAL_TYPE_NONE;
            showHideGraphButton = findViewById(R.id.graphExpandContractButton);

            if (value == WorkoutController.WORKOUT_STAGE_WARMUP) {
                setGraphVisibility (View.INVISIBLE);
                setStageMessageVisbility(View.VISIBLE);
                if (goalTypeNone) {
                    setStageLabelText (R.string.marathon_tc);
                }
                else {
                    setStageLabelText (R.string.warmup_uc);
                }
                if (showHideGraphButton != null) showHideGraphButton.setVisibility(View.INVISIBLE);
            } else if (value == WorkoutController.WORKOUT_STAGE_COOLDOWN) {
                setGraphVisibility(View.INVISIBLE);
                setStageMessageVisbility(View.VISIBLE);
                setStageLabelText (R.string.cool_down_uc);
                if (showHideGraphButton != null) showHideGraphButton.setVisibility(View.INVISIBLE);
            }
            else {
                // order is important - always do the gone/invisible first
                setStageMessageVisbility(View.GONE);
                if (DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT) == WorkoutIds.TARGET_TRAINING) {
                    setGraphVisibility(View.GONE);
                    if (showHideGraphButton != null) showHideGraphButton.setVisibility(View.GONE);
                } else {
                    setGraphVisibility(View.VISIBLE);
                    if (showHideGraphButton != null) showHideGraphButton.setVisibility(View.VISIBLE);
                }
                setStageLabelText (R.string.question_mark_tc);
            }
        });

        /*
        snapshots.getList(dataMatrix, snapshots -> {
            Log.d(TAG, "Got workout history list");
            for(WorkoutTrackingSnapshot snapshot : snapshots) {
                Log.d(TAG, "\t" + snapshot.toString());
            }
        }, snapshot -> Log.d(TAG, "Got workout snapshot " + snapshot.toString()));
         */
    }






    private void setupGraph() {
        if (mWorkoutGraph != null && mLeftLegend != null) {
            mGraphSetup = new GraphSetup(mWorkoutGraph, mLeftLegend);
            mGraphSetup.startGraphUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        dataMatrix.finish();

        if (mGraphSetup != null) mGraphSetup.stopGraphUpdates();
        mWorkoutGraph.reset();
        snapshots.clearList();
    }



    @Override
    protected void onResume() {
        super.onResume();

        setupDataMatrix();
        setupGraph();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


//    public void setMiniGraph(NewWorkoutGraph newgGraph) {
//        newWorkoutGraph = newgGraph;
//    }
}
