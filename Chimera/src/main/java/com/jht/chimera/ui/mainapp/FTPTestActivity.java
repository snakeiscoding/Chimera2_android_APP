package com.jht.chimera.ui.mainapp;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jht.chimera.lib.widget.activity.RestringActivity;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.conversionlib.StringFormatting;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.widget.athena.WattsGraph;
import com.jht.datamatrixlib.DataMatrix;

public class FTPTestActivity extends RestringActivity {
    private final String TAG = "FTPTestActivity";
    private DataMatrix dataMatrix = new DataMatrix();
    private WattsGraph wattsGraph;
    private float runningWorkoutTime = 0;
    private TextView numericTimeTV;
    private TextView numericWattsTV;
    private TextView timeLabel;
    private int currentWorkoutStage = 0;
    private float warmupGoalTime = 0;
    private float lastGoalValueSent = warmupGoalTime;
    private float lastGoalRemainingSent = 0;
    private float lastWattsValue = 0;
    private float timeCounter = 0;
    final private int ZONE_BAND_TOLERANCE = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.watts_graph_include);

        wattsGraph = findViewById (R.id.wattsGraph);
        numericTimeTV = findViewById (R.id.numericTimeTV);
        numericWattsTV = findViewById (R.id.numericWattsTV);
        timeLabel = findViewById (R.id.timeLabel);

        wattsGraph.setFTPTestLogic(true);
        wattsGraph.setZoneRangeNoRounding(150 - ZONE_BAND_TOLERANCE, 150 + ZONE_BAND_TOLERANCE);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        wattsGraph.setZoneBandOn(false);
        //wattsGraph.setZoneRangeNoRounding(125, 150);

        // Handle cases where the user + - the warmup time
        dataMatrix.createFloatAdapter(WorkoutKeys.DATA_GOAL_VALUE_FLOAT, value -> {
            // Filter out wierd low values
            if (value > 5800) {
                lastGoalValueSent = value;
            }
        }, false);

        dataMatrix.createFloatAdapter(HALKeys.DATA_WATTS_FLOAT, value -> lastWattsValue = value);

        dataMatrix.createFloatAdapter(WorkoutKeys.DATA_GOAL_REMAINING_FLOAT, value ->
                lastGoalRemainingSent = value);

        // graph has logic changes based on being in warmup state or not.   Track changes
        dataMatrix.createIntAdapter(WorkoutKeys.DATA_WORKOUT_STAGE_INT, value -> currentWorkoutStage = value, false);

        dataMatrix.createEventAdapter(WorkoutKeys.ACTION_WORKOUT_STATS_UPDATED, value -> {
            if (currentWorkoutStage == WorkoutKeys.Constants.WORKOUT_STAGE_WARMUP) {
//                Log.d(TAG, "FTPY Warmup Time Change from " + warmupGoalTime + " To " + lastGoalValueSent);
                warmupGoalTime = lastGoalValueSent;
                wattsGraph.setWarmUpDuration(warmupGoalTime / 1000);
            } else if (currentWorkoutStage == WorkoutKeys.Constants.WORKOUT_STAGE_WORKOUT && warmupGoalTime > 5900) {
                wattsGraph.setIsInWorkoutStage(true);
//                Log.d(TAG, "In WorkoutStage TRUE");
            }
            numericTimeTV.setText(StringFormatting.formatSecondsToTimeString(lastGoalRemainingSent / 1000));

            wattsGraph.addWattSample(lastWattsValue, timeCounter);
            numericWattsTV.setText("" + Math.round(lastWattsValue));
            timeCounter += 1;

//            Log.d (TAG, "FTPY ----------------------------------------------");
//            Log.d (TAG, "FTPY Workout Stage =" + currentWorkoutStage);
//            Log.d (TAG, "FTPY Goal Remaining =" + lastGoalRemainingSent);
//            Log.d (TAG, "FTPY Watts =" + lastWattsValue);
//            Log.d (TAG, "FTPY Time Counter =" + timeCounter);
//            Log.d (TAG, "FTPY Warmup Time Goal =" + warmupGoalTime);
        });

        //int targetWattsZone = (int) DataMatrix.getFloat(WorkoutKeys.DATA_TARGET_FTP_FLOAT, 50);
        dataMatrix.createFloatAdapter(WorkoutKeys.DATA_TARGET_FTP_FLOAT, targetWattsZone -> {
            wattsGraph.setZoneRangeNoRounding(targetWattsZone - ZONE_BAND_TOLERANCE, targetWattsZone + ZONE_BAND_TOLERANCE);
        });
    }

    @Override
    public void onPause ()
    {
        super.onPause();
        dataMatrix.finish();
    }
}
