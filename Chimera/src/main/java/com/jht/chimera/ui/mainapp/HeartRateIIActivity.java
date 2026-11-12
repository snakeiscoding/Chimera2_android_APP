package com.jht.chimera.ui.mainapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.conversionlib.DefaultEquations;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.formatting.FloorFormat;
import com.jht.chimera.lib.common.formatting.TimeFormat;
import com.jht.chimera.lib.graph.GraphLegendWidget;
import com.jht.chimera.lib.graph.workoutgraph.WorkoutGraphThreaded;
import com.jht.chimera.lib.ptp.HeartRateZones;
import com.jht.chimera.lib.widget.streamingimageview.StreamingImageView;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.workoutcontrollerlib.WorkoutController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The activity for the heart rate activity
 * <p>
 * Created by alex.nguyen on 8/23/2017.
 */
public class HeartRateIIActivity extends BaseWorkoutProfileActivity {
    private final String TAG = "HeartRateIIActivity";

    private StreamingImageView mHeartImageView;
    private StreamingImageView mSineWavesImageView;
    private TextView mCurrentBPMText;
    private TextView targetBPMValue;
    private TextView targetBPMTitle;
    private TextView currentMaxHRText;
    private ConstraintLayout mBluetoothLayout;
    private TextView mTotalTimeText;
    private ImageView mBpmCursor;
    private ImageView mTargetHRBackground;
    private ImageView mHRBackground;
    private final List<ImageView> mZones = new ArrayList<>();
    private ImageView mFiveZoneLayout;
    private ImageView mThreeZoneLayout;
    private LinearLayout subMaxValuesLayout;
    private TextView minBPMText;
    private TextView maxBPMText;

    private int mLastHeartRate = -1;
    private int mLastZone = -1;
    private int mMaxHr = 175;
    private boolean showMaxHR = false;
    private boolean isMyzone = false;

    private final Handler mHandler = new Handler();

    private final DataMatrix mDataMatrix = new DataMatrix();

    private enum HEARTRATE_SCREEN_TYPE {SIMPLE_BPM_SCREEN, TARGET_TRAINING_3_ZONE, ZONE_5_BPM, SUBMAX}
    private HEARTRATE_SCREEN_TYPE heartrateScreenType = HEARTRATE_SCREEN_TYPE.SIMPLE_BPM_SCREEN;

    private TextView graphExpandContractButton;
    private GraphLegendWidget topWorkoutGraphLegend;
    private WorkoutGraphThreaded mWorkoutGraph;
    private float percentMax = -1;
    private String forMaxHRDebugging = "AGE";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.heart_app_activity_athena);

        mCurrentBPMText = findViewById(R.id.currentBPMValueText);
        mBluetoothLayout = findViewById(R.id.bluetooth_pair_layout);
        targetBPMValue = findViewById(R.id.targetBPMValue);
        targetBPMTitle = findViewById(R.id.targetBPMTitle);
        mTotalTimeText = findViewById(R.id.heartRateTotalTime);
        mFiveZoneLayout = findViewById(R.id.fiveZones);
        mThreeZoneLayout = findViewById(R.id.threeZones);
        mTargetHRBackground = findViewById(R.id.threeZonesBackground);
        mHRBackground = findViewById(R.id.fiveZonesBackground);
        currentMaxHRText = findViewById(R.id.currentMaxHRText);
        subMaxValuesLayout = findViewById(R.id.submaximal_bpm_layout);
        minBPMText = findViewById(R.id.minBPMValueText);
        maxBPMText = findViewById(R.id.maxBPMValueText);
        mWorkoutGraph = findViewById(R.id.workout_graph);
        GraphLegendWidget leftLegend = findViewById(R.id.graph_left_legend_widget);
        topWorkoutGraphLegend = findViewById(R.id.graph_top_legend_widget);
        ConstraintLayout graphLayout = findViewById(R.id.graphLayout);
        graphExpandContractButton = findViewById (R.id.graphExpandContractButton);
        mBpmCursor = findViewById(R.id.bpmView);
        mHeartImageView = findViewById(R.id.heartImageView);
        mSineWavesImageView = findViewById(R.id.wavyLinesImageView);
        Button mBluetoothPairButton = findViewById(R.id.bt_pair_button);

        mBluetoothPairButton.setOnClickListener(v -> LaunchAppUtil.launchBluetoothPairing("heart_rate", findViewById(R.id.heart_rate_layout_root_new)));
        graphExpandContractButton.setOnClickListener(this::handleGraphExpandContractButton);

        mZones.add(findViewById(R.id.zone0));
        mZones.add(findViewById(R.id.zone1));
        mZones.add(findViewById(R.id.zone2));
        mZones.add(findViewById(R.id.zone3));
        mZones.add(findViewById(R.id.zone4));

        setGraphAndLegend(graphLayout, mWorkoutGraph, leftLegend, topWorkoutGraphLegend);

        graphExpandContractButton.setVisibility(View.GONE);
        ConstraintSet set = new ConstraintSet();
        set.constrainPercentWidth(R.id.graphLayout, 0.99f);
        set.applyTo(graphLayout);

        if (DataMatrix.getInt (SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT) != DAPIKeys.Constants.DAPI_DEPLOYMENT_PROD) showMaxHR = true;

        isMyzone = DataMatrix.getBoolean(HALKeys.DATA_MY_ZONE_CONNECTED_BOOL, false);
        setup5ZoneColorWheel (isMyzone);

        if (showMaxHR) {
            currentMaxHRText.setText(getString(R.string.max_hr_tc).toUpperCase());
            currentMaxHRText.setVisibility(View.VISIBLE);
        } else {
            currentMaxHRText.setVisibility(View.GONE);
        }

        updateMaxHR();
    }

    @SuppressLint("SetTextI18n")
    public void setupDataMatrix() {

        mDataMatrix.createIntAdapter(HALKeys.DATA_HEART_RATE_INT, this::updateHeartRate);

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_CS_THR_ZONE_INT, zone -> {
            int workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);
            if (workoutId == WorkoutIds.CARDIO_SCAN) {
                updateTargetZone(zone);
            }
        });

        mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_TARGET_HEART_RATE_PERCENT_FLOAT, percentMax -> {
            int workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);
            if (WorkoutIds.isTargetHeartRate(workoutId)) {
                this.percentMax = percentMax;
                int zone;

                if (percentMax < 60) zone = 0;
                else if (percentMax < 70) zone = 1;
                else if (percentMax < 80) zone = 2;
                else if (percentMax < 90) zone = 3;
                else zone = 4;

                updateTargetZone(zone);
                setTargetBPM ();
            }
        });

        mDataMatrix.createIntAdapter(DAPIKeys.DATA_USER_AGE_INT, age -> mHandler.post(() -> {
            int workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);

            switch (workoutId) {
                case WorkoutIds.GERKIN_FITNESS_TEST:
                    targetBPMTitle.setText(R.string.target_bpm_uc);
                    mMaxHr = Math.round(211f - 0.64f * age);
                    targetBPMValue.setText(String.valueOf(Math.round(mMaxHr * 0.85f)));
                    forMaxHRDebugging = "AGE";
                    updateMaxHR();
                    break;
                case WorkoutIds.CARDIO_SCAN:
                    HeartRateZones heartRateZones = DataMatrix.getParcelable(WorkoutKeys.DATA_CS_THR_ZONES_PARCELABLE, HeartRateZones.class);
                    mMaxHr = heartRateZones != null ? heartRateZones.getPeakMax() : 0;
                    forMaxHRDebugging = "mescan";
                    updateMaxHR();
                    break;
                default:
                    int maxHrFromAge = DefaultEquations.getDefaultMaxHr(age);
                    mMaxHr = DataMatrix.getInt (DAPIKeys.DATA_USER_MAXHR_INT, maxHrFromAge);
                    if (mMaxHr < 0) {
                        mMaxHr = maxHrFromAge;
                        forMaxHRDebugging = "AGE";
                    } else {
                        forMaxHRDebugging = "Fitness Test";
                    }
                    Log.d (TAG, "Age callback, age=" + age + "  maxHR value now=" + mMaxHr + " default eq max=" + maxHrFromAge);
                    updateMaxHR();
                    setTargetBPM ();
                    break;
            }
        }));

        mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_MAX_HEART_RATE_FLOAT, value -> mHandler.post(() -> {
            int workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);
            if (workoutId == WorkoutIds.CARDIO_SCAN || workoutId == WorkoutIds.GERKIN_FITNESS_TEST) return;
            targetBPMValue.setText(String.valueOf(Math.round(value)));
        }));

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

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_WORKOUT_ID_INT, value -> showHideTargetBPM());

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_WORKOUT_STAGE_INT, value -> showHideTargetBPM());

        mDataMatrix.createBoolAdapter(HALKeys.DATA_MY_ZONE_CONNECTED_BOOL, aBoolean -> { isMyzone = aBoolean;
            setup5ZoneColorWheel (aBoolean);});

        mDataMatrix.createIntAdapter(HALKeys.DATA_MYZONE_CALCULATED_AVG_MAX_HR_INT, calculated_max_hr -> {
            mMaxHr = calculated_max_hr;
            Log.d (TAG, "Max HR=" + mMaxHr);
            /* if (BuildConfig.DEBUG) */ currentMaxHRText.post(() -> currentMaxHRText.setText("MAX HR: " + mMaxHr));
            forMaxHRDebugging = "Myzone ";
            updateMaxHR();
            setTargetBPM();
        }, false);

        mDataMatrix.createIntAdapter(HALKeys.DATA_MYZONE_EFFORT_INT, percentEffort -> {
            int workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);
            if (workoutId == WorkoutIds.CARDIO_SCAN) return;
            if (heartrateScreenType == HEARTRATE_SCREEN_TYPE.TARGET_TRAINING_3_ZONE) return;
            setFiveZoneBPMCursors(Math.max ((percentEffort-50), 0) * 2 / 100f);
        });

        mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_SUBMAX_MIN_HEART_RATE_FLOAT, value -> mHandler.post(() ->
                minBPMText.setText(value <= 0 ? "---" : FloorFormat.format(value, 0))));

        mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_SUBMAX_MAX_HEART_RATE_FLOAT, value -> mHandler.post(() ->
                maxBPMText.setText(value <= 0 ? "---" : FloorFormat.format(value, 0))
        ));

    }

    private void handleGraphExpandContractButton(View v){
        if (mWorkoutGraph.getVisibility() == View.VISIBLE) {
            setGraphVisibility(View.GONE);
            graphExpandContractButton.setBackgroundResource(R.drawable.icon_chart_show);
        }
        else {
            setGraphVisibility(View.VISIBLE);
            graphExpandContractButton.setBackgroundResource(R.drawable.icon_chart_hide);
        }
    }

    private void updateHeartRate(int heartRate) {
        int workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);
        if (heartRate == mLastHeartRate) return;
        mLastHeartRate = heartRate;
        boolean isVisible = heartRate > 0;
        mHeartImageView.setSpeed(Math.max(heartRate / 40.0f, 1));
        mBluetoothLayout.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        mHeartImageView.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
        mSineWavesImageView.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
        mBpmCursor.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
        mCurrentBPMText.setText(isVisible ? String.format(Locale.ENGLISH, "%d", heartRate) : "- - -");
        if (heartrateScreenType == HEARTRATE_SCREEN_TYPE.SIMPLE_BPM_SCREEN && !isMyzone) {
            float percent = workoutId == WorkoutIds.CARDIO_SCAN ? getCardioScanBPMCursorLocation(heartRate) : (clampHeartRate(heartRate) - 60) / 150f;
            setFiveZoneBPMCursors(percent);
        }
    }

    private void setTargetBPM ()
    {
        if (percentMax>=0) {
            int newTarget = Math.round(mMaxHr * percentMax / 100f);
            targetBPMValue.post(() -> targetBPMValue.setText(String.valueOf(newTarget)));
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateMaxHR ()
    {
        if (showMaxHR)
            currentMaxHRText.post(() -> currentMaxHRText.setText ("MAX BPM: "+ mMaxHr + " from " + forMaxHRDebugging));
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void setup5ZoneColorWheel (boolean isMyzone) {
        if (isMyzone) {
            mZones.get(0).setImageDrawable(getResources().getDrawable(R.drawable.heart_app_zone0_myzone));
            mZones.get(1).setImageDrawable(getResources().getDrawable(R.drawable.heart_app_zone1_myzone));
            mZones.get(2).setImageDrawable(getResources().getDrawable(R.drawable.heart_app_zone2_myzone));
            mZones.get(3).setImageDrawable(getResources().getDrawable(R.drawable.heart_app_zone3_myzone));
            mZones.get(4).setImageDrawable(getResources().getDrawable(R.drawable.heart_app_zone4_myzone));
            mFiveZoneLayout.setImageDrawable(getResources().getDrawable(R.drawable.heart_app_zones_myzone));
        } else {
            mZones.get(0).setImageDrawable(getResources().getDrawable(R.drawable.heart_app_zone0_new));
            mZones.get(1).setImageDrawable(getResources().getDrawable(R.drawable.heart_app_zone1_new));
            mZones.get(2).setImageDrawable(getResources().getDrawable(R.drawable.heart_app_zone2_new));
            mZones.get(3).setImageDrawable(getResources().getDrawable(R.drawable.heart_app_zone3_new));
            mZones.get(4).setImageDrawable(getResources().getDrawable(R.drawable.heart_app_zone4_new));
            mFiveZoneLayout.setImageDrawable(getResources().getDrawable(R.drawable.heart_app_zones_new));
        }

        if (heartrateScreenType != HEARTRATE_SCREEN_TYPE.TARGET_TRAINING_3_ZONE) {
            set5ZoneWorkoutGraphLegend();
        }
    }

    // Logic to see if we should show the target BPM or not
    private void showHideTargetBPM ()
    {
        boolean showTargetBPMValue = false;

        int workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);
        switch (workoutId) {
            case WorkoutIds.GERKIN_FITNESS_TEST:
            case WorkoutIds.THR_SPEED:
            case WorkoutIds.THR_INCLINE:
            case WorkoutIds.WFI_FITNESS_TEST:
            case WorkoutIds.THR_RESISTANCE:
                showTargetBPMValue = true;
                break;
            case WorkoutIds.SUBMAX_FITNESS_TEST:
                subMaxValuesLayout.setVisibility(View.VISIBLE);
                break;
        }

        int workoutID = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);

        graphExpandContractButton.setVisibility(View.INVISIBLE);

        if (workoutID == WorkoutIds.THR_RESISTANCE)  showTargetBPMValue = true;

        int workoutStage = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT);
        if (workoutStage == WorkoutController.WORKOUT_STAGE_WARMUP)
        {
            showTargetBPMValue = false;
        }
        else  if (workoutStage == WorkoutController.WORKOUT_STAGE_COOLDOWN)
        {
            showTargetBPMValue = false;
        }

        if (showTargetBPMValue)
        {
            targetBPMTitle.setVisibility(View.VISIBLE);
            targetBPMValue.setVisibility(View.VISIBLE);
            setTargetBPM();
        }
        else
        {
            targetBPMTitle.setVisibility(View.INVISIBLE);
            targetBPMValue.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDataMatrix.finish();

        mHeartImageView.pause();
        if (heartrateScreenType == HEARTRATE_SCREEN_TYPE.SIMPLE_BPM_SCREEN) {
            mFiveZoneLayout.setVisibility(View.VISIBLE);
            mThreeZoneLayout.setVisibility(View.INVISIBLE);
            set5ZoneWorkoutGraphLegend();
        } else if (heartrateScreenType == HEARTRATE_SCREEN_TYPE.TARGET_TRAINING_3_ZONE) {
            mThreeZoneLayout.setVisibility(View.VISIBLE);
            mZones.get(mLastZone).setVisibility(View.INVISIBLE);
            mFiveZoneLayout.setVisibility(View.INVISIBLE);
            set3ZoneWorkoutGraphLegend();
        }
        mSineWavesImageView.pause();



        // Make each zone invisible & reset last zone on pause in case we changing workouts
        for (ImageView zone: mZones) zone.setVisibility(View.INVISIBLE);
        mLastZone = -1;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHeartImageView.resume();
        mSineWavesImageView.resume();
        mLastHeartRate = -1;
        setupDataMatrix();

        setupTargetHrScreen();
    }

    private void set5ZoneWorkoutGraphLegend ()
    {
        int workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);
        HeartRateZones heartRateZones = DataMatrix.getParcelable(WorkoutKeys.DATA_CS_THR_ZONES_PARCELABLE, HeartRateZones.class);
        String[] titles = new String[5];

        if (workoutId == WorkoutIds.CARDIO_SCAN && heartRateZones != null) {
            List<String> heartRateTitles = heartRateZones.toList();
            titles[0] = heartRateTitles.get(0);
            titles[1] = heartRateTitles.get(1);
            titles[2] = heartRateTitles.get(2);
            titles[3] = heartRateTitles.get(3);
            titles[4] = heartRateTitles.get(4);
        } else {
            titles[0] = ContextUtil.getString(R.string.very_light_uc);
            titles[1] = ContextUtil.getString(R.string.light_uc);
            titles[2] = ContextUtil.getString(R.string.moderate_uc);
            titles[3] = ContextUtil.getString(R.string.hard_uc);
            titles[4] = ContextUtil.getString(R.string.very_hard_uc);
        }

        topWorkoutGraphLegend.removeAllViews();

        if (isMyzone) {
            String myZoneGray = "#6E6E6F";
            topWorkoutGraphLegend.addItem(titles[0], myZoneGray, myZoneGray);
            String myZoneBlue = "#335AF9";
            topWorkoutGraphLegend.addItem(titles[1], myZoneBlue, myZoneBlue);
            String myZoneGreen = "#33A169";
            topWorkoutGraphLegend.addItem(titles[2], myZoneGreen, myZoneGreen);
            String myZoneYellow = "#EFE84E";
            topWorkoutGraphLegend.addItem(titles[3], myZoneYellow, myZoneYellow);
            String myZoneRed = "#FE3339";
            topWorkoutGraphLegend.addItem(titles[4], myZoneRed, myZoneRed);
        } else {
            topWorkoutGraphLegend.addItem(titles[0], "#29ABE2", "#DD29ABE2");
            topWorkoutGraphLegend.addItem(titles[1], "#0071FF", "#DD0071FF");
            topWorkoutGraphLegend.addItem(titles[2], "#009245", "#DD009245");
            topWorkoutGraphLegend.addItem(titles[3], "#EF7122", "#DDEF7122");
            topWorkoutGraphLegend.addItem(titles[4], "#C81C2D", "#DDC81C2D");
        }
    }

    private void set3ZoneWorkoutGraphLegend ()
    {
        topWorkoutGraphLegend.removeAllViews();

        topWorkoutGraphLegend.addItem(ContextUtil.getString(R.string.below_target_uc), "#29ABE2", "#DD29ABE2");
        topWorkoutGraphLegend.addItem(ContextUtil.getString(R.string.on_target_uc), "#009245", "#DD009245");
        topWorkoutGraphLegend.addItem(ContextUtil.getString(R.string.above_target_uc), "#C81C2D", "#DDC81C2D");
    }

    private void setupTargetHrScreen() {
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_WORKOUT_ID_INT, workoutId -> showHideTargetBPM ());

        mThreeZoneLayout.setVisibility(View.INVISIBLE);
        mTargetHRBackground.setVisibility(View.INVISIBLE);
        mFiveZoneLayout.setVisibility(View.VISIBLE);
        mHRBackground.setVisibility(View.VISIBLE);

        set5ZoneWorkoutGraphLegend();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHeartImageView.shutdown();
        mSineWavesImageView.shutdown();
    }

    private void updateTargetZone(int zone) {
        if (zone == mLastZone) return;
        if (zone < 0 || zone >= mZones.size()) return;

        AlphaAnimation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(300);
        AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setDuration(300);

        mZones.get(zone).startAnimation(fadeIn);
        mZones.get(zone).setVisibility(View.VISIBLE);

        if (mLastZone >= 0 && mLastZone < mZones.size()){
            mZones.get(mLastZone).startAnimation(fadeOut);
            mZones.get(mLastZone).setVisibility(View.INVISIBLE);
        }

        mLastZone = zone;
    }

    private int clampHeartRate(int bpm) {
        if (bpm < 60) {
            bpm = 60;
        } else if (bpm > mMaxHr) {
            bpm = mMaxHr;
        }
        return bpm;
    }


    private void setFiveZoneBPMCursors(float percent) {
        Log.d (TAG, "set 5 zone percent " + percent);
        mBpmCursor.setDuplicateParentStateEnabled(true);
        // These values can be adjusted to try to better fit the curve
        float yAdjustmentMultiplier = 0.50f;
        float cursorYShift = 31f;

        // Get positions of view
        float xStartPos = mFiveZoneLayout.getLeft();
        float yStandardRange = 70.26f; // Approximate value from equation
        float width = mFiveZoneLayout.getWidth();
        float yStartPos = mFiveZoneLayout.getTop();
        float yStopPos = mFiveZoneLayout.getBottom();

        // Rescale the y values to the correct size, multiply by scale factor to get correct size
        float yMultiplier = (yStopPos - yStartPos) / yStandardRange * yAdjustmentMultiplier;
        //Function for the curve of the heart rate zones
        double x = percent * width + xStartPos;
        double yXInput = (percent * (945.684 - 128.316) + 128.316); // Equation for arc required this
        double y = ((-1.65081887278 * Math.pow(10, -9) * Math.pow(yXInput, 4) + 0.00000354113177036726 * Math.pow(yXInput, 3)
                - 0.0029954520823457565 * Math.pow(yXInput, 2) + 1.1761803919124185 * yXInput) - 108) * yMultiplier + yStartPos + cursorYShift;

        mBpmCursor.setX((float) x);
        mBpmCursor.setY((float) y);
    }
    private void setTargetZoneBPMCursors(float percent) {
        mBpmCursor.setDuplicateParentStateEnabled(true);
        // These values can be adjusted to try to better fit the curve
        float yAdjustmentMultiplier = 0.50f;
        float cursorYShift = 31f;

        // Get positions of view
        float xStartPos = mThreeZoneLayout.getLeft();
        float xStopPos = mThreeZoneLayout.getRight();
        float yStandardRange = 70.26f; // Approximate value from equation
        float width = mThreeZoneLayout.getWidth();
        float yStartPos = mThreeZoneLayout.getTop();
        float yStopPos = mThreeZoneLayout.getBottom();

        // Rescale the y values to the correct size, multiply by scale factor to get correct size
        float yMultiplier = (yStopPos - yStartPos) / yStandardRange * yAdjustmentMultiplier;
        //Function for the curve of the heart rate zones
        double x = percent * width + xStartPos;
        double yXInput = (percent * (945.684 - 128.316) + 128.316); // Equation for arc required this
        double y = ((-1.65081887278 * Math.pow(10, -9) * Math.pow(yXInput, 4) + 0.00000354113177036726 * Math.pow(yXInput, 3)
                - 0.0029954520823457565 * Math.pow(yXInput, 2) + 1.1761803919124185 * yXInput) - 108) * yMultiplier + yStartPos + cursorYShift;

        if (x < xStartPos) {
            x = xStartPos;
            y = yStartPos + cursorYShift;
        } else if (x > xStopPos) {
            x = xStopPos;
            y = yStartPos + cursorYShift;
        }
        mBpmCursor.setX((float) x);
        mBpmCursor.setY((float) y);
    }

    private float getCardioScanBPMCursorLocation(int heartRate) {
        HeartRateZones heartRateZones = DataMatrix.getParcelable(WorkoutKeys.DATA_CS_THR_ZONES_PARCELABLE, HeartRateZones.class);

        if (heartRateZones == null) return -1;
        if (!heartRateZones.isValid()) return -1;

        heartRate = Math.max(heartRate, heartRateZones.getRegenerativeMin());
        heartRate = Math.min(heartRate, heartRateZones.getPeakMax());

        int zoneHRRange;
        int zoneLeftBoundary;
        int zoneRightBoundary;

        if (heartRate <= heartRateZones.getRegenerativeMax()) {
            heartRate -= heartRateZones.getRegenerativeMin();
            zoneHRRange = heartRateZones.getRegenerativeMax() - heartRateZones.getRegenerativeMin();
            zoneLeftBoundary = 0;
            zoneRightBoundary = 14;
        }
        else if (heartRate <= heartRateZones.getBasicExtensiveMax()) {
            heartRate -= heartRateZones.getRegenerativeMax();
            zoneHRRange = heartRateZones.getBasicExtensiveMax() - heartRateZones.getRegenerativeMax();
            zoneLeftBoundary = 14;
            zoneRightBoundary = 37;
        }
        else if (heartRate <= heartRateZones.getBasicIntensiveMax()) {
            heartRate -= heartRateZones.getBasicExtensiveMax();
            zoneHRRange = heartRateZones.getBasicIntensiveMax() - heartRateZones.getBasicExtensiveMax();
            zoneLeftBoundary = 37;
            zoneRightBoundary = 61;
        }
        else if (heartRate <= heartRateZones.getDevelopmentMax()) {
            heartRate -= heartRateZones.getBasicIntensiveMax();
            zoneHRRange = heartRateZones.getDevelopmentMax() - heartRateZones.getBasicIntensiveMax();
            zoneLeftBoundary = 61;
            zoneRightBoundary = 84;
        }
        else {
            heartRate -= heartRateZones.getDevelopmentMax();
            zoneHRRange = heartRateZones.getPeakMax() - heartRateZones.getDevelopmentMax();
            zoneLeftBoundary = 84;
            zoneRightBoundary = 100;
        }

        return (((((float) heartRate / zoneHRRange)) * (zoneRightBoundary - zoneLeftBoundary)) + zoneLeftBoundary) / 100;
    }
}