package com.jht.chimera.ui.mainapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.jht.chimera.lib.widget.activity.RestringActivity;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.ProfileValue;
import com.jht.chimera.lib.common.WorkoutFuture;
import com.jht.chimera.lib.common.WorkoutFutureProfile;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.datamatrixlists.DataMatrixListFactory;
import com.jht.chimera.lib.common.datamatrixlists.WorkoutTrackingSnapshot;
import com.jht.chimera.lib.graph.GraphLegendWidget;
import com.jht.chimera.lib.widget.athena.NewWorkoutGraph;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.workoutcontrollerlib.WorkoutController;

import java.util.List;

public class BaseWorkoutProfileAltActivity extends RestringActivity {
    private static final String TAG = "BaseWorkoutProfileAl...";
    private GraphLegendWidget mLeftLegend;
    private GraphLegendWidget mTopLegend;
    private GraphMaker graphMaker;
    private DataMatrix dataMatrix = new DataMatrix();
    private ViewGroup graphLayout;
    private NewWorkoutGraph newWorkoutGraph;
    private WorkoutFuture mFuture;
    private TextView stageLabel;
    private TextView showHideGraphButton;
    private DataMatrixListFactory.WorkoutSnapshotConsumer snapshots = DataMatrixListFactory.snapshotListConsumer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setGraphAndLegend (ViewGroup viewGroup, NewWorkoutGraph nwg, GraphLegendWidget leftLegend, GraphLegendWidget topLegend)
    {
        graphLayout = viewGroup;
        newWorkoutGraph = nwg;
        mLeftLegend = leftLegend;
        mTopLegend = topLegend;
        setupDataMatrix();
        setupGraph();
        newWorkoutGraph.setWorkoutTime(30 * 60);
    }

    public void setGraphVisibility (int visibility)
    {
        if (newWorkoutGraph != null) newWorkoutGraph.setVisibility(visibility);
        if (mLeftLegend != null) mLeftLegend.setVisibility(visibility);
        if (graphLayout != null) graphLayout.setVisibility (visibility);
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
                setGraphVisibility(View.VISIBLE);
                setStageLabelText (R.string.question_mark_tc);

                if (showHideGraphButton != null) showHideGraphButton.setVisibility(View.VISIBLE);
            }
        });

        dataMatrix.createParcelableAdapter(WorkoutKeys.DATA_WORKOUT_FUTURE_PARCELABLE, WorkoutFuture.class, workoutFuture -> {
            if (!WorkoutFuture.isValid(workoutFuture)) return;
            mFuture = workoutFuture;
            Log.d(TAG, "Data - New Future ");
            Toast.makeText(getApplicationContext(), "Got Future Graph", Toast.LENGTH_LONG).show(); //NON-NLS
            for (WorkoutFutureProfile wp : mFuture.profiles) {
                switch (wp.parameter) {
                    case ConsoleWorkoutVariables.SPEED: {
                        Log.d(TAG, "Data - New Future Speed");
                        addFutureData(graphMaker.speedLine, wp.profileValues);
                    }
                    break;

                    case ConsoleWorkoutVariables.INCLINE: {
                        Log.d(TAG, "Data - New Future Incline");
                        addFutureData(graphMaker.inclineLine, wp.profileValues);
                    }
                    break;

                    case ConsoleWorkoutVariables.RESISTANCE: {
                        Log.d(TAG, "Data - New Future Resistance");
                        addFutureData(graphMaker.resistanceLine, wp.profileValues);
                    }
                    break;

                    case ConsoleWorkoutVariables.TARGET_WATTS: {
                        Log.d(TAG, "Data - New Future Watts");
                        addFutureData(graphMaker.wattsLine, wp.profileValues);
                    }
                    break;

                    default:
                        break;
                }
            }
        });

        dataMatrix.createBoolAdapter(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, isMetric -> newWorkoutGraph.setAsMetric(isMetric));
        newWorkoutGraph.setDistanceStrings (getString (R.string.km_lc), getString(R.string.miles_tc));


        // Grab all snapshots from the workout tracking service and then listen for snapshot updates.
        // There may be a race condition here as the workout tracking service sends all data and then
        // it may send another snapshot before we register for the updates.  Need to think about how to
        // best handle that.


        snapshots.getList(dataMatrix, snapshots -> {
            for (WorkoutTrackingSnapshot snapshot : snapshots) {
                Log.d(TAG, "Data snapshot list " + snapshot.distance);

                if (graphMaker.inclineLine != null)
                    graphMaker.inclineLine.addData(snapshot.incline, snapshot.timeInState);
                if (graphMaker.rpmLine != null)
                    graphMaker.rpmLine.addData(snapshot.rpm, snapshot.timeInState);
                if (graphMaker.wattsLine != null)
                    graphMaker.wattsLine.addData(snapshot.watts, snapshot.timeInState);
                if (graphMaker.speedLine != null)
                    graphMaker.speedLine.addData(snapshot.kph, snapshot.timeInState);
                if (graphMaker.resistanceLine != null)
                    graphMaker.resistanceLine.addData(snapshot.resistance, snapshot.timeInState);
                if (graphMaker.spmLine != null)
                    graphMaker.spmLine.addData(snapshot.spm, snapshot.timeInState);
                if (graphMaker.bpmLine != null)
                    graphMaker.bpmLine.addData(snapshot.heartRate, snapshot.timeInState);
                newWorkoutGraph.addDistanceData(snapshot.distance, snapshot.timeInState);
                newWorkoutGraph.updateScreen();
            }
        }, snapshot -> {
            if (newWorkoutGraph != null) {
                Log.d(TAG, "Data snapshot");

                if (graphMaker.inclineLine != null)
                    graphMaker.inclineLine.addData(snapshot.incline, snapshot.timeInState);
                if (graphMaker.rpmLine != null)
                    graphMaker.rpmLine.addData(snapshot.rpm, snapshot.timeInState);
                if (graphMaker.wattsLine != null)
                    graphMaker.wattsLine.addData(snapshot.watts, snapshot.timeInState);
                if (graphMaker.speedLine != null)
                    graphMaker.speedLine.addData(snapshot.kph, snapshot.timeInState);
                if (graphMaker.resistanceLine != null)
                    graphMaker.resistanceLine.addData(snapshot.resistance, snapshot.timeInState);
                if (graphMaker.spmLine != null)
                    graphMaker.spmLine.addData(snapshot.spm, snapshot.timeInState);
                if (graphMaker.bpmLine != null)
                    graphMaker.bpmLine.addData(snapshot.heartRate, snapshot.timeInState);

                newWorkoutGraph.addDistanceData(snapshot.distance, snapshot.timeInState);
                newWorkoutGraph.updateScreen();
            }
        });

    }

    private void setupGraph() {
        if (newWorkoutGraph != null && mLeftLegend != null) {
            graphMaker = new GraphMaker(newWorkoutGraph, mLeftLegend);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        dataMatrix.finish();
        snapshots.clearList();
    }

    @Override
    protected void onResume() {
        super.onResume();

        setupDataMatrix();
        setupGraph();

        // Handle sizing the graph to the time goal given.
        dataMatrix.createIntAdapter(WorkoutKeys.DATA_GOAL_TYPE_INT, value ->
        {
            Log.d (TAG, "Workout type=" + DataMatrix.getInt(WorkoutKeys.DATA_GOAL_TYPE_INT));

            if (value == WorkoutKeys.Constants.GOAL_TYPE_TIME || value == WorkoutKeys.Constants.GOAL_TYPE_TIME_STAGE)
            {
                dataMatrix.createFloatAdapter(WorkoutKeys.DATA_GOAL_VALUE_FLOAT, value2 -> newWorkoutGraph.setWorkoutTime(value2.intValue()/1000));
            }
        });

    }

    private void addFutureData (NewWorkoutGraph.LineGroup lineData, List<ProfileValue> newData)
    {
        float lastValue = 0;

        lineData.clearFutureData();

        lineData.addFutureData (lastValue, 0);

        for (ProfileValue pv : newData)
        {
            //Log.d (TAG, "Data - add future " + pv.value + " " + pv.timeMs);
            // Create Plateau's instead of mountains - visually
            lineData.addFutureData(lastValue, pv.timeMs);
            lineData.addFutureData (pv.value, pv.timeMs);

            lastValue = pv.value;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void setMiniGraph(NewWorkoutGraph newgGraph) {
        newWorkoutGraph = newgGraph;
    }
}
