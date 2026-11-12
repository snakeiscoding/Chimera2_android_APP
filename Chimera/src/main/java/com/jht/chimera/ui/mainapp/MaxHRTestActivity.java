package com.jht.chimera.ui.mainapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jht.chimera.lib.widget.activity.RestringActivity;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.conversionlib.StringFormatting;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.string.StringUtil;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.widget.animation.AnimationHelper;
import com.jht.chimera.lib.widget.athena.MaxBPMTestGraph;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.workoutcontrollerlib.WorkoutController;


/**
 * Created by Megan.Quinlan on 8/7/2019.
 */
public class MaxHRTestActivity extends RestringActivity {
    private DataMatrix dataMatrix = new DataMatrix();
    private MaxBPMTestGraph bpmGraph;
    private float runningWorkoutTime = 0;
    private TextView numericTimeTV;
    private TextView numericBPMTV;
    private TextView stageHeader;
    private View headerCover;

    private int easyColor = Color.parseColor("#39ABE2");
    private int mediumColor = Color.parseColor("#2C9F19");
    private int hardColor = Color.parseColor("#F50B05");
    private int intervalCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.watts_max_hr_graph);

        bpmGraph = findViewById (R.id.wattsGraph);
        numericTimeTV = findViewById (R.id.numericTimeTV);
        numericBPMTV = findViewById (R.id.numericBPMTV);
        stageHeader = findViewById (R.id.sectionHeader);
        headerCover = findViewById(R.id.sectionHeaderOverlay);

        bpmGraph.setMaxHRTestLogic(true);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        bpmGraph.setZoneBandOn(true);
        bpmGraph.setStagesOn(true);
        bpmGraph.setZoneRangeNoRounding(125, 150);

        dataMatrix.createIntAdapter(HALKeys.DATA_HEART_RATE_INT, value -> numericBPMTV.setText(StringUtil.formatHeartRate(value)));

        dataMatrix.createFloatAdapter(WorkoutKeys.DATA_GOAL_REMAINING_FLOAT, value ->
        {
            numericTimeTV.setText(StringFormatting.formatSecondsToTimeString(value / 1000));
        });

        dataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, value ->{
            runningWorkoutTime = value / 1000f;
            bpmGraph.addWattSample(DataMatrix.getFloat(HALKeys.DATA_WATTS_FLOAT), runningWorkoutTime);
            if (runningWorkoutTime > 114 && runningWorkoutTime < 120) {
                headerCover.setVisibility(View.VISIBLE);
            } else if (runningWorkoutTime > 234 && runningWorkoutTime < 240) {
                headerCover.setVisibility(View.VISIBLE);
            } else {
                headerCover.setVisibility(View.GONE);
            }
        });

        dataMatrix.createIntAdapter(WorkoutKeys.DATA_STAGE_INTERVAL_NUM_INT, value ->{
            intervalCounter = value + 1;
            AnimationHelper.showView(stageHeader);
            switch (value) {
                case 0:
                    stageHeader.setText(stageHeader.getContext().getString(R.string.stage_tc).toUpperCase() + " " + intervalCounter + " - " + StringLookup.get(R.string.easy_uc));
                    stageHeader.setBackgroundColor(easyColor);
                    break;
                case 1:
                    stageHeader.setText(stageHeader.getContext().getString(R.string.stage_tc).toUpperCase() + " " + intervalCounter + " - " + StringLookup.get(R.string.medium_uc));
                    stageHeader.setBackgroundColor(mediumColor);
                    break;
                case 2:
                    stageHeader.setText(stageHeader.getContext().getString(R.string.stage_tc).toUpperCase() + " " + intervalCounter + " - " + StringLookup.get(R.string.hard_uc));
                    stageHeader.setBackgroundColor(hardColor);
                    break;
            }
        });

        dataMatrix.createIntAdapter(WorkoutKeys.DATA_WORKOUT_STAGE_INT, value ->
        {
            if (value == WorkoutController.WORKOUT_STAGE_WORKOUT) {
                bpmGraph.setIsInWorkoutStage(true);
            } else {
                bpmGraph.setIsInWorkoutStage(false);
            }
        });
    }

    @Override
    public void onPause ()
    {
        super.onPause();
        dataMatrix.finish();
    }
}
