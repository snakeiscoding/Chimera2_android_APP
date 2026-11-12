package com.jht.chimera.ui.mainapp;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jht.chimera.lib.widget.activity.RestringActivity;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.conversionlib.DistanceConvertUtil;
import com.jht.chimera.lib.common.conversionlib.StringFormatting;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.formatting.FloorFormat;
import com.jht.chimera.lib.widget.athena.BarGraphWidget;
import com.jht.chimera.lib.widget.athena.BikePaceWidget;
import com.jht.chimera.lib.widget.streamingimageview.StreamingImageView;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.workoutcontrollerlib.WorkoutController;

import java.util.Locale;

public class BikePaceActivity extends RestringActivity {

    private static final String TAG = "BikePaceActivity";
    private DataMatrix dataMatrix = new DataMatrix();
    private StreamingImageView animatedBikeView;
    private BikePaceWidget bikePaceWidget;
    private TextView distanceNumeric;
    private TextView currentPosition;
    private TextView speedNumeric;
    private TextView distanceLabel;
    private TextView speedLabel;
    private BarGraphWidget paceBarGraph;
    private float runningWorkoutTime = 0;
    private boolean isMetric;
    private int lastAnimateSpeed = 1;
    private TextView targetSpeedNumeric;
    private TextView currentAvgSpeedNumeric;
    private String speedUnit = "";
    private int goalType = WorkoutKeys.Constants.GOAL_TYPE_NONE;
    private float goalTarget = 1;
    private float unitConversion = 1;
    private float targetSpeedInKph = 15;
    private float lastDistanceInKm = 0;
    private float lastTime = 0;
    private String distanceUnit;
    private String distanceUnitLong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bike_pace_widget_include);

        bikePaceWidget = findViewById(R.id.bikePaceWidget);
        animatedBikeView = findViewById (R.id.animatedBikeView);
        distanceNumeric = findViewById (R.id.distanceNumeric);
        paceBarGraph = findViewById(R.id.paceBarGraph);
        currentPosition = findViewById(R.id.currentPosition);
        targetSpeedNumeric = findViewById(R.id.targetSpeedNumeric);
        speedNumeric = findViewById(R.id.speedNumeric);
        currentAvgSpeedNumeric = findViewById(R.id.currentAvgSpeedNumeric);
        distanceLabel = findViewById(R.id.distanceLabel);
        speedLabel = findViewById(R.id.speedLabel);

        paceBarGraph.updatePercentageFilled(.5f);
        paceBarGraph.setMarkerLineAsDecimalPercent(.5f);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        animatedBikeView.resume();


        dataMatrix.createIntAdapter(WorkoutKeys.DATA_WORKOUT_STAGE_INT, value ->
        {
            if (value == WorkoutController.WORKOUT_STAGE_COOLDOWN)
            {
                paceBarGraph.setVisibility(View.GONE);
                currentPosition.setVisibility(View.GONE);
            }
            else
            {
                paceBarGraph.setVisibility(View.VISIBLE);
                currentPosition.setVisibility(View.VISIBLE);
            }
        });

        dataMatrix.createBoolAdapter(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, isMetric -> {
            this.isMetric = isMetric;
            if (isMetric)
            {
                unitConversion = 1;
                speedUnit = getString(R.string.kph_uc);
                distanceUnit = getString(R.string.km_uc);
                distanceUnitLong = getString(R.string.km_uc);
            }
            else
            {
                unitConversion = DistanceConvertUtil.convertKilometersToMiles(1);
                speedUnit = getString(R.string.mph_uc);
                distanceUnitLong = getString(R.string.miles_uc);
                distanceUnit = getString(R.string.mi_uc);
            }

            //  All distance and speed come in as metric, flip the numbers to imperial/metric using the conversion factor.
            bikePaceWidget.changeUnits(distanceUnit, lastDistanceInKm * unitConversion, targetSpeedInKph * unitConversion);
            // Next line for debugging
            // targetSpeedNumeric.setText (String.format(Locale.ENGLISH, "%.1f %s", targetSpeedInKph * unitConversion, speedUnit));

            speedLabel.setText (getString(R.string.current_tc) + " " + speedUnit);
            distanceLabel.setText (getString(R.string.current_tc) + " " + distanceUnitLong);
            bikePaceWidget.updateWidget (lastDistanceInKm * unitConversion, runningWorkoutTime);
            String distanceString = FloorFormat.format (lastDistanceInKm * unitConversion, 2);
            distanceNumeric.setText (distanceString);
        });

        dataMatrix.createFloatAdapter(WorkoutKeys.DATA_DISTANCE_KM_FLOAT, value -> {
            Log.d (TAG, "Distance Value=" + value);

            lastDistanceInKm = value;
            bikePaceWidget.updateWidget (value * unitConversion, runningWorkoutTime);
        });

        dataMatrix.createFloatAdapter(WorkoutKeys.DATA_CURRENT_AVG_TARGET_SPEED_KPH_FLOAT, value -> {
            Log.d (TAG, "Target Speed KPH=" + value);
            targetSpeedInKph = value;
            bikePaceWidget.changeUnits(distanceUnit, lastDistanceInKm * unitConversion, targetSpeedInKph * unitConversion);
            //targetSpeedNumeric.setText (String.format(Locale.ENGLISH, "%.1f %s", targetSpeedInKph * unitConversion, speedUnit));
            speedNumeric.setText (String.format (Locale.ENGLISH,"%.1f", value*unitConversion));
        });

        dataMatrix.createFloatAdapter(WorkoutKeys.DATA_WORKOUT_SPEED_KPH_FLOAT, value -> {
            speedNumeric.setText (String.format (Locale.ENGLISH,"%.1f", value*unitConversion));
        });

        dataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, value -> {
            runningWorkoutTime = value/1000f;
            Log.d (TAG, "Time Value=" + value);
            lastTime = value/1000f;
        });

        dataMatrix.createEventAdapter(WorkoutKeys.ACTION_WORKOUT_STATS_UPDATED, value ->{
            if (lastTime > 0 && targetSpeedInKph > 0)
            {
                float currentAvgSpeedInKph = lastDistanceInKm / (lastTime / 3600);
                float avgSpeedPercentDifference = (currentAvgSpeedInKph/targetSpeedInKph) - 1f;
                float timeDiffInSeconds = avgSpeedPercentDifference * lastTime;
                Log.d (TAG, "avg%SpeedDiff=" + avgSpeedPercentDifference);
                Log.d (TAG, "timeDiffInSeconds=" + timeDiffInSeconds);

                paceBarGraph.updatePercentageFilled(avgSpeedPercentDifference + .5f);

                String distanceString = FloorFormat.format (lastDistanceInKm * unitConversion, 2);
                distanceNumeric.setText (distanceString);

                if (timeDiffInSeconds <= -1){
                    paceBarGraph.updateLabel("-" + StringFormatting.formatSecondsToTimeString(Math.abs(timeDiffInSeconds)));
                }
                else {
                    paceBarGraph.updateLabel(StringFormatting.formatSecondsToTimeString(timeDiffInSeconds));
                }

                // For debugging, next line
                //currentAvgSpeedNumeric.setText(String.format(Locale.ENGLISH,"%.2f  %.3f", currentAvgSpeedInKph*unitConversion, avgSpeedPercentDifference));

                if (avgSpeedPercentDifference > .05f)
                {
                    paceBarGraph.changeBarColor(BarGraphWidget.TTCOLOR.RED);
                }
                else if (avgSpeedPercentDifference < -.05f)
                {
                    paceBarGraph.changeBarColor(BarGraphWidget.TTCOLOR.BLUE);
                }
                else
                {
                    paceBarGraph.changeBarColor(BarGraphWidget.TTCOLOR.GREEN);
                }
            }
        });

        dataMatrix.createIntAdapter(HALKeys.DATA_RPM_INT, value -> {
            int newSpeed = value/5;
            if (newSpeed != lastAnimateSpeed)
            {
                animatedBikeView.setSpeed(newSpeed/5f);
                lastAnimateSpeed = newSpeed;
            }
        });
    }

    private SpannableStringBuilder createDistanceUnitString (String distanceString, String unit)
    {
        SpannableString ssDistance=  new SpannableString(distanceString);
        ssDistance.setSpan(new RelativeSizeSpan(1.0f), 0, ssDistance.length(), 0);
        SpannableString ssUnit=  new SpannableString(unit);
        ssUnit.setSpan(new RelativeSizeSpan(.30f), 0, ssUnit.length(), 0);
        ssUnit.setSpan(new ForegroundColorSpan(Color.parseColor("#a3a3a3")), 0, ssUnit.length(), 0);

        SpannableStringBuilder ssb = new SpannableStringBuilder();

        ssb.append (ssDistance);
        ssb.append (ssUnit);
        return ssb;
    }

    private SpannableStringBuilder createCurrentSpeedLabel (String unit)
    {
        SpannableString ssDistance=  new SpannableString(getString(R.string.current_tc) + "  ");
        ssDistance.setSpan(new RelativeSizeSpan(1.0f), 0, ssDistance.length(), 0);
        SpannableString ssUnit=  new SpannableString(unit);
        ssUnit.setSpan(new RelativeSizeSpan(.80f), 0, ssUnit.length(), 0);
        ssUnit.setSpan(new ForegroundColorSpan(Color.parseColor("#a3a3a3")), 0, ssUnit.length(), 0);

        SpannableStringBuilder ssb = new SpannableStringBuilder();

        ssb.append (ssDistance);
        ssb.append (ssUnit);
        return ssb;
    }

    private SpannableStringBuilder createCurrentDistanceLabel (String unit)
    {
        SpannableString ssDistance=  new SpannableString(getString(R.string.current_tc) + "  ");
        ssDistance.setSpan(new RelativeSizeSpan(1.0f), 0, ssDistance.length(), 0);
        SpannableString ssUnit=  new SpannableString(unit);
        ssUnit.setSpan(new RelativeSizeSpan(.80f), 0, ssUnit.length(), 0);
        ssUnit.setSpan(new ForegroundColorSpan(Color.parseColor("#a3a3a3")), 0, ssUnit.length(), 0);

        SpannableStringBuilder ssb = new SpannableStringBuilder();

        ssb.append (ssDistance);
        ssb.append (ssUnit);
        return ssb;
    }

    private SpannableStringBuilder createSpeedUnitString (float distance, String unit)
    {
        String distanceString = String.format(Locale.ENGLISH,"%.1f", distance);
        SpannableString ssDistance=  new SpannableString(distanceString);
        ssDistance.setSpan(new RelativeSizeSpan(1.0f), 0, ssDistance.length(), 0);
        SpannableString ssUnit=  new SpannableString(unit);
        ssUnit.setSpan(new RelativeSizeSpan(.30f), 0, ssUnit.length(), 0);
        ssUnit.setSpan(new ForegroundColorSpan(Color.parseColor("#a3a3a3")), 0, ssUnit.length(), 0);

        SpannableStringBuilder ssb = new SpannableStringBuilder();

        ssb.append (ssDistance);
        ssb.append (ssUnit);
        return ssb;
    }

    @Override
    public void onPause ()
    {
        super.onPause();

        animatedBikeView.pause();
        dataMatrix.finish();
    }
}
