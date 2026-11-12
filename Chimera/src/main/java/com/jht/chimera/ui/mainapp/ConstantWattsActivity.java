package com.jht.chimera.ui.mainapp;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.formatting.TimeFormat;
import com.jht.chimera.lib.graph.GraphLegendWidget;
import com.jht.chimera.lib.graph.workoutgraph.WorkoutGraphThreaded;
import com.jht.chimera.lib.widget.ScaleView;
import com.jht.chimera.lib.widget.dial.MultiZoneCircleGauge;
import com.jht.chimera.lib.widget.streamingimageview.StreamingImageView;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.workoutcontrollerlib.WorkoutController;

public class ConstantWattsActivity extends BaseWorkoutProfileActivity {

    private static final int LIGHTNING_LIGHT_BLUE = 0xff1e9cd7;
    private static final int LIGHTNING_BLUE = 0xff0075be;
    private static final int LIGHTNING_GREEN = 0xff009245;
    private static final int LIGHTNING_ORANGE = 0xffef7122;
    private static final int LIGHTNING_RED = 0xffc81c2d;
    private StreamingImageView mLightning;
    private final DataMatrix mDataMatrix = new DataMatrix();
    private MultiZoneCircleGauge wattsDial;
    private TextView mTotalTimeText;

    private int mWorkoutId;
    private final Handler mHandler = new Handler();

    private boolean dialSetUp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.constant_watts_layout);
        ScaleView.scaleContents(findViewById(R.id.constantWattsLayout));
        mLightning = findViewById(R.id.lightningImageView);
        wattsDial = findViewById(R.id.wattsDial);
        mTotalTimeText = findViewById(R.id.heartRateTotalTime);
        WorkoutGraphThreaded mWorkoutGraph = findViewById(R.id.workout_graph);
        GraphLegendWidget leftLegend = findViewById(R.id.graph_left_legend_widget);
        GraphLegendWidget topWorkoutGraphLegend = findViewById(R.id.graph_top_legend_widget);
        ConstraintLayout graphLayout = findViewById(R.id.graphLayout);
        setGraphAndLegend(graphLayout, mWorkoutGraph, leftLegend, topWorkoutGraphLegend);
    }

    @Override
    public void onResume() {
        super.onResume();
        mLightning.resume();
        mWorkoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);
        if (mWorkoutId == WorkoutIds.COOPER_FITNESS_TEST) {
            if (wattsDial != null ){
                wattsDial.setLargeTextUnits(getResources().getString(R.string.rpm_uc));
            }
            findViewById(R.id.rpmIcon).setVisibility(View.VISIBLE);
            findViewById(R.id.wattsIcon).setVisibility(View.GONE);
        } else {
            if (wattsDial != null ){
                wattsDial.setLargeTextUnits(getResources().getString(R.string.watts_uc));
            }
            findViewById(R.id.rpmIcon).setVisibility(View.GONE);
            findViewById(R.id.wattsIcon).setVisibility(View.VISIBLE);
        }
        if (!dialSetUp){
            setupDial();
        }
        setupDataMatrix();
    }

    private void setupDial() {
        dialSetUp = true;
        if (mWorkoutId == WorkoutIds.COOPER_FITNESS_TEST) {
            //mDialWidget.setMinMax(0f, 140f, 0);
            //mDialWidget.setUnit(R.string.rpm_uc);
            //mDialWidget.hideNumeric();
            //mDialWidget.showTargetZone();
            //mDialWidget.setTargetZone(60, 80);
            if (wattsDial != null) {
                String blueLineColor = "#0288A8";//dropper color 2 NON-NLS
                //String blueLineBlurColor = "#0288A8";
                String blueLineBlurColor = "#ffffff"; //NON-NLS
                String blueMiddleBlurColor = "#012A36"; //NON-NLS
                float rangeMin = 0;
                float rangeMax = 100f;
                float targetMin = 60;
                float targetMax = 80;
                wattsDial.setGaugeRange(rangeMin, rangeMax);
                //wattsDial.createZone(rangeMin, rangeMax, blueLineColor, blueLineBlurColor, blueMiddleBlurColor);//Add this line to keep constant color around ring
                wattsDial.addZone(targetMin, targetMax, blueLineColor, blueLineBlurColor, blueMiddleBlurColor);//Target Zone
                wattsDial.setShowBottomText(false);
                wattsDial.setShowLargeText(true);
            }
        } else {
            //mDialWidget.setMinMax(0f, 220f, 0);
            //mDialWidget.setUnit(R.string.rpm_uc);
            //mDialWidget.showNumeric();
            if (wattsDial != null) {
                String blueLineColor = "#0288A8";//dropper color 2 NON-NLS
                //String blueLineBlurColor = "#0288A8";
                String blueLineBlurColor = "#ffffff"; //NON-NLS
                String blueMiddleBlurColor = "#012A36"; //NON-NLS
                float rangeMin = 0;
                float rangeMax = 220f;
                wattsDial.setGaugeRange(rangeMin, rangeMax);
                //wattsDial.createZone(rangeMin, rangeMax, blueLineColor, blueLineBlurColor, blueMiddleBlurColor);//Add this line to keep constant color around ring
                wattsDial.addZone(rangeMin, rangeMax, blueLineColor, blueLineBlurColor, blueMiddleBlurColor);//"Target" Zone
                wattsDial.setShowBottomText(true);
                wattsDial.setShowLargeText(true);
            }
        }
    }

    private void setupDataMatrix() {
        if (mWorkoutId == WorkoutIds.COOPER_FITNESS_TEST) {
            setupCooperDataMatrix();
        } else {
            setupConstantWattsDataMatrix();
        }

        mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_GOAL_VALUE_FLOAT, value -> {
            final String formattedGoal = TimeFormat.formatTime(value);

            mHandler.post(() -> mTotalTimeText.setText(formattedGoal));
        });

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_GOAL_TYPE_INT, value -> {
            switch (value) {
                case ConsoleWorkoutVariables.GOAL_TYPE_TIME:
                case ConsoleWorkoutVariables.GOAL_TYPE_TIME_STAGE:
                    mHandler.post(() -> mTotalTimeText.setVisibility(View.VISIBLE));
                    break;
                default:
                    mHandler.post(() -> mTotalTimeText.setVisibility(View.INVISIBLE));
                    break;
            }
        });

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_WORKOUT_STAGE_INT, value -> {
            if (value == WorkoutController.WORKOUT_STAGE_WARMUP) {
                mTotalTimeText.setVisibility(View.INVISIBLE);
            } else if (value == WorkoutController.WORKOUT_STAGE_COOLDOWN) {
                mTotalTimeText.setVisibility(View.INVISIBLE);
            }
            else {
                mTotalTimeText.setVisibility(View.VISIBLE);
            }
        });
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_WORKOUT_ID_INT, value -> setupGraph(), true);
    }

    private void setupCooperDataMatrix() {

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, ms -> {
            int rpm = DataMatrix.getInt(HALKeys.DATA_RPM_INT);
            int targetRpm = 80;
            //mDialNumeric.setText(getCenterString(targetRpm, actualRpm));
            //mDialWidget.setValue(value);
            updateLightningForCooperTest(rpm);
            if (wattsDial != null){
                wattsDial.setLargeTextValue(getCenterString(targetRpm, rpm));
                wattsDial.setGaugeValue (rpm);
            }
        });
    }

    public void setupConstantWattsDataMatrix() {
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, ms -> {
            int watts = (int)DataMatrix.getFloat(HALKeys.DATA_WATTS_FLOAT);
            int targetWatts = DataMatrix.getInt(WorkoutKeys.DATA_TARGET_WATTS_INT);
            int rpm = DataMatrix.getInt(HALKeys.DATA_RPM_INT);

            //mDialNumeric.setText(getCenterString(targetWatts, actualWatts));
            if (wattsDial != null){
                wattsDial.setLargeTextValue(getCenterString(targetWatts, watts));
                wattsDial.invalidate();
            }
            updateLightningColor(targetWatts, watts);

            //mDialNumeric.setText(getCenterString(targetWatts, actualWatts));
            if (wattsDial != null){
                wattsDial.setLargeTextValue(getCenterString(targetWatts, watts));
                wattsDial.invalidate();
            }
            updateLightningColor(targetWatts, watts);

            wattsDial.setGaugeValue(rpm);
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        mLightning.pause();
        mDataMatrix.finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLightning.shutdown();
    }

    private void setupGraph() {
        //mWorkoutGraph.reset();
    }

    private String getCenterString(int targetValue, int actualValue) {
        return actualValue + "/" + targetValue;
    }

    private void updateLightningColor(int targetWatts, int actualWatts) {
        int color = calculateLightningColor(targetWatts, actualWatts);
        mLightning.setTintColor(color);
    }

    private void updateLightningForCooperTest(int rpm) {
        int rpmDifference = rpm - 70;
        int color;
        if (rpmDifference < -20) {
            // Far below zone [-50, -inf].
            color = LIGHTNING_LIGHT_BLUE;
        } else if (rpmDifference < -10) {
            // Below zone [-10, -50].
            color = LIGHTNING_BLUE;
        } else if (rpmDifference > 20) {
            // Far above zone [50, inf].
            color = LIGHTNING_RED;
        } else if (rpmDifference > 10) {
            // Above zone [10, 50].
            color = LIGHTNING_ORANGE;
        } else {
            // Target Zone [-9, +9]
            color = LIGHTNING_GREEN;
        }

        mLightning.setTintColor(color);
    }

    private int calculateLightningColor(int targetWatts, int actualWatts) {
        int difference = actualWatts - targetWatts;
        int color;
        if (difference < -50) {
            // Far below zone [-50, -inf].
            color = LIGHTNING_LIGHT_BLUE;
        } else if (difference <= -10) {
            // Below zone [-10, -50].
            color = LIGHTNING_BLUE;
        } else if (difference > 50) {
            // Far above zone [50, inf].
            color = LIGHTNING_RED;
        } else if (difference >= 10) {
            // Above zone [10, 50].
            color = LIGHTNING_ORANGE;
        } else {
            // Target Zone [-9, +9]
            color = LIGHTNING_GREEN;
        }
        return color;
    }
}
