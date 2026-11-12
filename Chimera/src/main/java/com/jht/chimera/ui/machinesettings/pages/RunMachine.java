package com.jht.chimera.ui.machinesettings.pages;

import static android.view.View.VISIBLE;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.data.RangeInfo;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.service.hal.lib.lcb.ReceiveTreadmillCadence;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.widget.CommonRoutines;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.workout.math.CadenceFormulas;
import com.jht.chimera.ui.machinesettings.EngineeringIdleTimerManager;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;

import java.util.Locale;


/**
 * Implements the run machine page gluing the page to the model layer.
 */
public class RunMachine extends SettingPageBase {
    private final DataMatrix dataMatrix = new DataMatrix();
    private final FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();

    private Button btnRunMachine;
    private View view;
    private UserTextInput timeText;
    private final PeriodicTimer timer = new PeriodicTimer();
    private int timeCounter;
    private TextView calPoints;
    private TextView calPointsTitle;
    @Override
    protected View onCreate(Context context, ViewGroup container) {
        view = LayoutInflater.from(context).inflate(R.layout.run_machine, null);
        container.addView(view);
        return view;
    }


    @SuppressLint("SetTextI18n")
    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);

        btnRunMachine = view.findViewById(R.id.eng_run_machine_start);
        timeText = view.findViewById(R.id.eng_run_machine_time);
        stopMachine();
        calPointsTitle = view.findViewById(R.id.eng_run_machine_cal_points_title);
        calPoints = view.findViewById(R.id.eng_run_machine_cal_points);
        if(config.supportsLcbType(LcbType.LCB_Electromagnetic_brake_Incline_LCB.Value)
                ||config.supportsLcbType(LcbType.LCB_BIKE.Value)
                ||config.supportsLcbType(LcbType.LCB_Global_Induction.Value)
                ||config.supportsLcbType(LcbType.LCB_Global_Full_Function.Value)
                ||config.supportsLcbType(LcbType.LCB_Global_Induction_Incline.Value)
        ) {
            calPointsTitle.setVisibility(VISIBLE);
            calPoints.setVisibility(VISIBLE);
            calPoints.setText(DataMatrix.getString(HALKeys.DATA_PWM_CAL_POINTS_STRING));
        }
        else {
            calPointsTitle.setVisibility(View.GONE);
            calPoints.setVisibility(View.GONE);
        }
        timer.setOnTimer(() -> {
            // TODO format this???
            timeText.post(() -> timeText.setText("" + (++timeCounter)));
        });

        findViewById(R.id.eng_run_machine_tm_controls).setVisibility(View.GONE);
        findViewById(R.id.eng_run_machine_ascent_controls).setVisibility(View.GONE);
        findViewById(R.id.eng_run_machine_cardio_controls).setVisibility(View.GONE);

        switch (config.machineClassType) {

            case Treadmill:
                findViewById(R.id.eng_run_machine_incline_value).setVisibility(View.INVISIBLE);
                findViewById(R.id.eng_run_machine_speed_value).setVisibility(View.INVISIBLE);
                break;
            case Bike:
            case Elliptical:
            case RecumbentElliptical:
                findViewById(R.id.eng_run_machine_rpm_value).setVisibility(VISIBLE);
                break;
            case Stepper:
            case Climbmill:
                findViewById(R.id.eng_run_machine_spm_value).setVisibility(VISIBLE);
                break;
            case Ascent:
                findViewById(R.id.eng_run_machine_incline_value).setVisibility(View.INVISIBLE);
                findViewById(R.id.eng_run_machine_rpm_value).setVisibility(View.INVISIBLE);
                break;
            default:
                break;
        }
        boolean isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);


        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_run_machine_heart_rate);
            dataMatrix.createIntAdapter(HALKeys.DATA_HEART_RATE_INT, integer -> {
                if (integer < 0)
                    integer = 0;
                userTextInput.setText("" + integer);
            });
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_run_machine_incline_value);
            dataMatrix.createFloatAdapter(HALKeys.DATA_ACTUAL_INCLINE_FLOAT, value -> userTextInput.setText("" + value));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_run_machine_speed_value);
            userTextInput.setInputProperties(userTextInput.getInputProperties()
                    .units(CommonRoutines.getUnit())
            );
            dataMatrix.createFloatAdapter(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT, value -> {
                String text = "" + CommonRoutines.round(value * CommonRoutines.getConvertFactorSpeed(), 1);
                userTextInput.setText(text);
            });
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_run_machine_rpm_value);
            dataMatrix.createIntAdapter(HALKeys.DATA_RPM_INT, integer -> userTextInput.setText("" + integer));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_run_machine_spm_value);
            dataMatrix.createIntAdapter(HALKeys.DATA_SPM_INT, integer -> userTextInput.setText("" + integer));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_run_machine_speed_setting);
            RangeInfo speedRange = isMetric ? config.speedKph : config.speedMph;
            userTextInput.setInputProperties(userTextInput.getInputProperties()
                    .resolution(0.1)
                    .min(speedRange.getMin())
                    .max(speedRange.getMax())
                    .maxChars(4)
                    .units(CommonRoutines.getUnit())
            );

            userTextInput.setOnTextChanged(s -> {
                if (s != null && !s.isEmpty()) {
                    try {
                        float value = Float.parseFloat(s);
                        if (!isMetric) {
                            value = SpeedConvertUtil.convertMphToKph(value);
                        }
                        DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, value);
                    } catch (Exception ignored) {
                    }
                }
            });
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_run_machine_tm_set_rpm);
            userTextInput.setInputProperties(userTextInput.getInputProperties()
                    .resolution(1)
                    .min(0)
                    .max(6000)
            );

            userTextInput.setOnTextChanged(s -> {
                if (s != null && !s.isEmpty()) {
                    try {
                        int value = Integer.parseInt(s);
                        DataMatrix.putInt(HALKeys.ACTION_SET_SPEED_RPM_INT, value);
                    } catch (Exception ignored) {
                    }
                }
            });
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_run_machine_tm_incline_setting);
            RangeInfo inclineRange = config.incline;
            userTextInput.setInputProperties(userTextInput.getInputProperties()
                    .resolution(inclineRange.stepSize)
                    .min(inclineRange.getMin())
                    .max(inclineRange.getMax())
                    .maxChars(4)
                    .units(CommonRoutines.getUnit())
            );
            userTextInput.setOnTextChanged(s -> {
                if (s != null && !s.isEmpty()) {
                    try {
                        float value = Float.parseFloat(s);
                        DataMatrix.putFloat(HALKeys.ACTION_SET_INCLINE_FLOAT, value);
                    } catch (Exception ignored) {
                    }
                }
            });
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_run_machine_ascent_incline_setting);
            RangeInfo inclineRange = config.incline;
            userTextInput.setInputProperties(userTextInput.getInputProperties()
                    .resolution(inclineRange.stepSize)
                    .min(inclineRange.getMin())
                    .max(inclineRange.getMax())
                    .maxChars(3)
                    .units(CommonRoutines.getUnit())
            );
            userTextInput.setOnTextChanged(s -> {
                if (s != null && !s.isEmpty()) {
                    try {
                        float value = Float.parseFloat(s);
                        DataMatrix.putFloat(HALKeys.ACTION_SET_INCLINE_FLOAT, value);
                    } catch (Exception ignored) {
                    }
                }
            });
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_run_machine_cardio_resistance_setting);
            RangeInfo resistanceRange = config.resistance;
            userTextInput.setInputProperties(userTextInput.getInputProperties()
                    .resolution(1)
                    .min(resistanceRange.getMin())
                    .max(resistanceRange.getMax())
                    .maxChars(2)
                    .units(CommonRoutines.getUnit())
            );
            userTextInput.setOnTextChanged(s -> {
                try {
                    int value = Integer.parseInt(s);
                    DataMatrix.putInt(HALKeys.ACTION_SET_RESISTANCE_INT, value);
                } catch (Exception ignored) {
                }
            });
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_run_machine_ascent_resistance_setting);
            RangeInfo resistanceRange = config.resistance;
            userTextInput.setInputProperties(userTextInput.getInputProperties()
                    .resolution(1)
                    .min(resistanceRange.getMin())
                    .max(resistanceRange.getMax())
                    .maxChars(2)
                    .units(CommonRoutines.getUnit())
            );
            userTextInput.setOnTextChanged(s -> {
                try {
                    int value = Integer.parseInt(s);
                    DataMatrix.putInt(HALKeys.ACTION_SET_RESISTANCE_INT, value);
                } catch (Exception ignored) {
                }
            });
        }

        // only show cadence if frame supports it
        if (config.HAS_CADENCE) {
            View cadenceContainer = view.findViewById(R.id.eng_run_machine_tm_cadence);
            cadenceContainer.setVisibility(VISIBLE);

            TextView cadenceSpmView = view.findViewById(R.id.eng_run_machine_tm_cadence_spm_value);
            TextView cadenceAverageSpmView = view.findViewById(R.id.eng_run_machine_tm_cadence_average_spm_value);
            TextView accumulateStepsView = view.findViewById(R.id.eng_run_machine_tm_accumulate_steps_value);
            TextView strideLengthView = view.findViewById(R.id.eng_run_machine_tm_stride_length_value);
            TextView averageStrideLengthView = view.findViewById(R.id.eng_run_machine_tm_average_stride_length_value);

            String unitSuffix = isMetric ? " m" : " yd";

            dataMatrix.createParcelableAdapter(HALKeys.EVENT_RECEIVED_TREADMILL_CADENCE_PARCELABLE,
                    ReceiveTreadmillCadence.class, rxCadence -> {

                        float speedKph = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT, 0f);
                        float averageCadence = CadenceFormulas.averageCadence(rxCadence.getAccumulateSteps(), timeCounter);

                        float strideLength = CadenceFormulas.strideLength(speedKph, (float)rxCadence.getCadenceSpm(), isMetric);
                        //There isn't a "workout" in MES so there's no average velocity tracking. Use current speedKph instead
                        float averageStrideLength = CadenceFormulas.strideLength(speedKph, averageCadence, isMetric);

                        cadenceSpmView.setText(String.format(Locale.getDefault(), "%s: %d",
                                ContextUtil.getString(R.string.spm_uc), rxCadence.getCadenceSpm()));

                        cadenceAverageSpmView.setText(String.format(Locale.getDefault(), "%s: %.2f",
                                ContextUtil.getString(R.string.average_spm_uc), averageCadence));

                        accumulateStepsView.setText(String.format(Locale.getDefault(), "%s: %d",
                                ContextUtil.getString(R.string.steps_uc), rxCadence.getAccumulateSteps()));

                        strideLengthView.setText(String.format(Locale.getDefault(), "%s: %.2f%s",
                                ContextUtil.getString(R.string.stride_length_uc), strideLength, unitSuffix));

                        averageStrideLengthView.setText(String.format(Locale.getDefault(), "%s: %.2f%s",
                                ContextUtil.getString(R.string.average_stride_length_uc), averageStrideLength, unitSuffix));
                    });
        }

        btnRunMachine.setOnClickListener(v -> toggleMachine());
    }

    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        stopMachine();
        dataMatrix.finish();
    }


    private void toggleMachine() {
        if (btnRunMachine.getText().toString().equalsIgnoreCase(StringLookup.get(R.string.stop_machine_tc))) {
            stopMachine();

            if(config.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
                //TM548 control ECB brake
                DataMatrix.sendEvent(HALKeys.EVENT_TM548_MES_ECB_RUN_BRAKE);
            }

        } else {
            runMachine();

            if(config.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
                //TM548 control ECB release
                DataMatrix.sendEvent(HALKeys.EVENT_TM548_MES_ECB_RUN_RELEASE);
            }

        }
    }


    private void runMachine() {
        EngineeringIdleTimerManager.idleTimerManagement.stopMonitoring();
        timer.start(1000, 1000);
        DataMatrix.sendEvent(HALKeys.ACTION_START_MACHINE);
        DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, config.speedKph.getMin());
        DataMatrix.putInt(HALKeys.ACTION_SET_RESISTANCE_INT, 1);
        DataMatrix.putFloat(HALKeys.ACTION_SET_INCLINE_FLOAT, 0f);

        btnRunMachine.setText(StringLookup.get(R.string.stop_machine_tc));

        switch (config.machineClassType) {

            case Treadmill:
                findViewById(R.id.eng_run_machine_tm_controls).setVisibility(VISIBLE);
                break;
            case Bike:
            case Elliptical:
            case RecumbentElliptical:
                findViewById(R.id.eng_run_machine_cardio_controls).setVisibility(VISIBLE);
                break;
            case Stepper:
                break;
            case Ascent:
                findViewById(R.id.eng_run_machine_ascent_controls).setVisibility(VISIBLE);
                break;
            case Climbmill:
                int minResistance = Math.round(config.resistance.getMin());
                DataMatrix.putInt(HALKeys.ACTION_SET_RESISTANCE_INT, minResistance);
                findViewById(R.id.eng_run_machine_cardio_controls).setVisibility(VISIBLE);
                break;
            default:
                break;
        }
    }

    private void stopMachine() {
        EngineeringIdleTimerManager.idleTimerManagement.monitorTimers();
        timeCounter = 0;
        timeText.setText("" + timeCounter);
        timer.stop();

        DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, 0f);
        DataMatrix.putInt(HALKeys.ACTION_SET_RESISTANCE_INT, 0);
        DataMatrix.putFloat(HALKeys.ACTION_SET_INCLINE_FLOAT, 0f);
        DataMatrix.sendEvent(HALKeys.ACTION_STOP_MACHINE);

        btnRunMachine.setText(StringLookup.get(R.string.run_machine_tc));

        //reset those inputted text value
        UserTextInput tmSpeedTextInput = view.findViewById(R.id.eng_run_machine_speed_setting);
        UserTextInput tmInclineTextInput = view.findViewById(R.id.eng_run_machine_tm_incline_setting);
        UserTextInput tmRpmTextInput = view.findViewById(R.id.eng_run_machine_tm_set_rpm);

        UserTextInput ascentInclineTextInput = view.findViewById(R.id.eng_run_machine_ascent_incline_setting);
        UserTextInput ascentResistanceTextInput = view.findViewById(R.id.eng_run_machine_ascent_resistance_setting);

        UserTextInput cardioResistanceTextInput = view.findViewById(R.id.eng_run_machine_cardio_resistance_setting);

        tmSpeedTextInput.setText("");
        tmInclineTextInput.setText("");
        tmRpmTextInput.setText("");

        ascentInclineTextInput.setText("");
        ascentResistanceTextInput.setText("");

        cardioResistanceTextInput.setText("");

        findViewById(R.id.eng_run_machine_tm_controls).setVisibility(View.GONE);
        findViewById(R.id.eng_run_machine_ascent_controls).setVisibility(View.GONE);
        findViewById(R.id.eng_run_machine_cardio_controls).setVisibility(View.GONE);
    }
}
