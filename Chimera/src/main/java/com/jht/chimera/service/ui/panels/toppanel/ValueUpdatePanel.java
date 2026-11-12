package com.jht.chimera.service.ui.panels.toppanel;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.ProfileValue;
import com.jht.chimera.lib.common.WorkoutFuture;
import com.jht.chimera.lib.common.WorkoutFutureProfile;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.formatting.BasicNumberFormat;
import com.jht.chimera.lib.common.formatting.SpeedFormat;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.chimera.lib.workoutapps.AppIds;
import com.jht.chimera.service.ui.panels.IPanel;
import com.jht.chimera.service.ui.panels.IWorkoutPanelManager;
import com.jht.chimera.service.ui.panels.PanelIds;
import com.jht.chimera.service.ui.panels.WorkoutPanelManager;
import com.jht.chimera.service.ui.panels.WorkoutPanelMode;
import com.jht.datamatrixlib.DataMatrix;

import java.util.List;

public class ValueUpdatePanel extends ConstraintLayout implements IPanel {
    private static final int NEXT_VALUES_SHOWN_TIME_MS = 5000;
    private View mLeftBackground;
    private TextView mLeftValue;
    private TextView mLeftUnit;
    private TextView mLeftLabel;

    private View mRightBackground;
    private TextView mRightValue;
    private TextView mRightUnit;
    private TextView mRightLabel;

    private TextView mCountDownValue;

    private final DataMatrix mDataMatrix = new DataMatrix();

    private FrameConfig mFrameConfig;

    // Future profile events
    private List<ProfileValue> mSpeedChanges = null;
    private List<ProfileValue> mInclineChanges = null;
    private List<ProfileValue> mResistanceChanges = null;

    public ValueUpdatePanel(Context context) {
        super(context);
        init(context);
    }

    public ValueUpdatePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ValueUpdatePanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.next_value_top_bar, this, true);

        mFrameConfig = FrameConfigManager.getInstance().getFrameConfig();

        mLeftBackground = findViewById(R.id.left_background_view);
        mLeftValue = findViewById(R.id.left_value_text_view);
        mLeftUnit = findViewById(R.id.left_unit_text_view);
        mLeftLabel = findViewById(R.id.left_label_text_view);

        mRightBackground = findViewById(R.id.right_background_view);
        mRightValue = findViewById(R.id.right_value_text_view);
        mRightUnit = findViewById(R.id.right_unit_text_view);
        mRightLabel = findViewById(R.id.right_label_text_view);

        mCountDownValue = findViewById(R.id.countdown_text_view);

        mDataMatrix.createParcelableAdapter(WorkoutKeys.DATA_WORKOUT_FUTURE_PARCELABLE, WorkoutFuture.class, this::saveUpcomingEvents, false);

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, this::showIfNeeded);

        // Reset when the workout is complete
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, runState -> {
            if (runState == WorkoutKeys.Constants.RUN_STATE_STOP) {
                hideAndCancelFutureUpdates();
            }
        });

        // Reset when we are changing the workout
        mDataMatrix.createEventAdapter(WorkoutKeys.EVENT_WORKOUT_SWITCHED, key -> hideAndCancelFutureUpdates());

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_WORKOUT_STAGE_INT, stage -> {
            if (stage == WorkoutKeys.Constants.WORKOUT_STAGE_COOLDOWN) {
                hideAndCancelFutureUpdates();
            }
        });
    }
    @Override
    public void dispose() {
        mDataMatrix.finish();
    }

    private void hideAndCancelFutureUpdates() {
        WorkoutPanelManager.getInstance().showPanel(PanelIds.VALUE_UPDATE_PANEL, false);
        mSpeedChanges = null;
        mInclineChanges = null;
        mResistanceChanges = null;
    }

    private void saveUpcomingEvents(WorkoutFuture data) {
        if (!WorkoutFuture.isValid(data)) return;
        for (WorkoutFutureProfile profile : data.profiles) {
            switch (profile.parameter) {
                case ConsoleWorkoutVariables.SPEED:
                    mSpeedChanges = profile.profileValues;
                    break;
                case ConsoleWorkoutVariables.INCLINE:
                    mInclineChanges = profile.profileValues;
                    break;
                case ConsoleWorkoutVariables.RESISTANCE:
                    mResistanceChanges = profile.profileValues;
                    break;
            }
        }
    }

    /**
     * Finds next change and displays the change if there are 5 seconds or less before changing and
     * the change is to a new value.
     *
     * @param elapsedWorkoutTime The time elapsed in the current workout
     */
    private void showIfNeeded(int elapsedWorkoutTime) {
        IWorkoutPanelManager panelManager = WorkoutPanelManager.getInstance();

        // Do not show in VA, Sprint8, or Matrix Method
        String appId = DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING);
        if (AppIds.SPRINT8_UUID.equals(appId)
                || AppIds.VA_UUID.equals(appId)) return;

        float nextChangeTime = getNextChangeTime(elapsedWorkoutTime);
        int diff = (int) nextChangeTime - elapsedWorkoutTime;
        if (diff <= NEXT_VALUES_SHOWN_TIME_MS) {
            // show if 5 seconds or less and value has changed
            Float leftValue = getLeftNextValue(nextChangeTime);
            Float rightValue = getRightNextValue(nextChangeTime);
            if (valuesAreChanged(leftValue, rightValue)) {
                displayLeftValue(leftValue);
                displayRightValue(rightValue);
                // Display the countdown time
                float time = (float) diff / 1000f;
                mCountDownValue.setText(BasicNumberFormat.format(time, 0));
                panelManager.showPanel(PanelIds.VALUE_UPDATE_PANEL, true);
                return;
            }
        }
        if (panelManager.isPanelShown(PanelIds.VALUE_UPDATE_PANEL)) {
            WorkoutPanelManager.getInstance().showPanel(PanelIds.VALUE_UPDATE_PANEL, false);
        }
    }

    private float getNextChangeTime(int currentTime) {
        float nextTime = Float.MAX_VALUE;
        // Get the lowest time change event that occurs after the current time
        if (mSpeedChanges != null) {
            for (ProfileValue profileValue : mSpeedChanges) {
                if (profileValue.timeMs > currentTime && profileValue.timeMs < nextTime) {
                    nextTime = profileValue.timeMs;
                    break;
                }
            }
        }

        if (mInclineChanges != null) {
            for (ProfileValue profileValue : mInclineChanges) {
                if (profileValue.timeMs > currentTime && profileValue.timeMs < nextTime) {
                    nextTime = profileValue.timeMs;
                    break;
                }
            }
        }

        if (mResistanceChanges != null) {
            for (ProfileValue profileValue : mResistanceChanges) {
                if (profileValue.timeMs > currentTime && profileValue.timeMs < nextTime) {
                    nextTime = profileValue.timeMs;
                    break;
                }
            }
        }

        return nextTime;
    }

    private Float getLeftNextValue(float changeTime) {
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        switch (config.machineClassType) {
            case Treadmill:
            case Ascent:
                if (mInclineChanges != null) {
                    float inclineValue = getFloatNextValue(changeTime, mInclineChanges, HALKeys.DATA_TARGET_INCLINE_FLOAT.name());
                    return Math.min(inclineValue, mFrameConfig.incline.getMax()); // We won't set over the max value, so cap this
                }
                else {
                    return DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT);
                }
            case Bike:
            case Climbmill:
            case SpinBike:
            case Stepper:
            case Elliptical:
            case RecumbentElliptical:
            default:
                return null;
        }
    }

    private Float getRightNextValue(float changeTime) {
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        switch (config.machineClassType) {
            case Treadmill:
                if (mSpeedChanges != null) {
                    boolean isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
                    float maxValue = isMetric ? mFrameConfig.speedKph.getMax() : SpeedConvertUtil.convertMphToKph(mFrameConfig.speedMph.getMax());
                    float speedValue = getFloatNextValue(changeTime, mSpeedChanges, HALKeys.DATA_TARGET_SPEED_KPH_FLOAT.name());
                    return Math.min(speedValue, maxValue);
                }
                else {
                    return null;
                }
            case Ascent:
            case Bike:
            case Climbmill:
            case SpinBike:
            case Stepper:
            case Elliptical:
            case RecumbentElliptical:
                if (mResistanceChanges != null) {
                    int resistanceValue = getIntNextValue(changeTime, mResistanceChanges, HALKeys.DATA_RESISTANCE_INT.name());
                    return Math.min(resistanceValue, mFrameConfig.resistance.getMax());
                }
                else {
                    return null;
                }
            default:
                return null;
        }
    }

    /**
     * Gets the correct value for the next value from the
     *
     * @param changeTime the time the next change occurs at
     * @param key        The key for the current value of the parameter
     * @return The next value
     */
    private float getFloatNextValue(float changeTime, List<ProfileValue> valueList, String key) {
        for (ProfileValue value : valueList) {
            if (value.timeMs < changeTime) {
                continue;
            } else if (Math.round(value.timeMs) == changeTime) {
                return value.value;
            } else if (Math.round(value.timeMs) > changeTime) {
                break;
            }
        }

        return DataMatrix.getFloat(key);
    }

    /**
     * Gets the correct value for the next value from the
     *
     * @param changeTime the time the next change occurs at
     * @param key        The key for the current value of the parameter
     * @return The next value
     */
    private int getIntNextValue(float changeTime, List<ProfileValue> valueList, String key) {
        for (ProfileValue value : valueList) {
            if (value.timeMs < changeTime) {
                continue;
            } else if (Math.round(value.timeMs) == changeTime) {
                return Math.round(value.value);
            } else if (Math.round(value.timeMs) > changeTime) {
                break;
            }
        }

        return DataMatrix.getInt(key);
    }

    private boolean valuesAreChanged(Float leftValue, Float rightValue) {
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        switch (config.machineClassType) {
            case Treadmill:
                float speed = DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT);
                float inclineTm = DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT);
                boolean nextLeftDifferent = leftValue != null && Math.abs(speed - leftValue) > 0.00005;
                boolean nextRightDifferent = rightValue != null && Math.abs(inclineTm - rightValue) > 0.00005;
                return nextLeftDifferent || nextRightDifferent;
            case Ascent:
                float inclineAscent = DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT);
                int resistanceAscent = DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT);
                return leftValue != null && rightValue != null &&
                        (Math.abs(inclineAscent - leftValue) > 0.00005 || resistanceAscent != rightValue);
            case Bike:
            case Climbmill:
            case SpinBike:
            case Stepper:
            case Elliptical:
            case RecumbentElliptical:
                int resistance = DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT);
                return leftValue == null && rightValue != null && resistance != rightValue;
            default:
                return false;
        }
    }

    private void displayLeftValue(Float value) {
        if (value != null) {
            FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
            switch (config.machineClassType) {
                case Treadmill:
                    mLeftValue.setText(BasicNumberFormat.format(value, 1));
                    mLeftUnit.setText("%");
                    mLeftLabel.setText(StringLookup.get(R.string.next_incline_uc));
                    mLeftValue.setVisibility(View.VISIBLE);
                    mLeftUnit.setVisibility(View.VISIBLE);
                    mLeftLabel.setVisibility(View.VISIBLE);
                    mLeftBackground.setVisibility(View.VISIBLE);
                    break;
                case Ascent:
                    mLeftValue.setText(BasicNumberFormat.format(value, 0));
                    mLeftUnit.setText("%");
                    mLeftLabel.setText(StringLookup.get(R.string.next_incline_uc));
                    mLeftValue.setVisibility(View.VISIBLE);
                    mLeftUnit.setVisibility(View.VISIBLE);
                    mLeftLabel.setVisibility(View.VISIBLE);
                    mLeftBackground.setVisibility(View.VISIBLE);
                    break;
            }
        } else {
            mLeftValue.setVisibility(View.INVISIBLE);
            mLeftUnit.setVisibility(View.INVISIBLE);
            mLeftLabel.setVisibility(View.INVISIBLE);
            mLeftBackground.setVisibility(View.INVISIBLE);
        }
    }

    private void displayRightValue(Float value) {
        if (value != null) {
            FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
            switch (config.machineClassType) {
                case Treadmill:
                    boolean isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
                    mRightValue.setText(SpeedFormat.formatSpeed(value, isMetric));
                    mRightUnit.setText(isMetric ? StringLookup.get(R.string.kph_lc) : StringLookup.get(R.string.mph_lc));
                    mRightLabel.setText(StringLookup.get(R.string.next_speed_uc));
                    mRightValue.setVisibility(View.VISIBLE);
                    mRightUnit.setVisibility(View.VISIBLE);
                    mRightLabel.setVisibility(View.VISIBLE);
                    mRightBackground.setVisibility(View.VISIBLE);
                    break;
                case Ascent:
                case Bike:
                case Elliptical:
                case RecumbentElliptical:
                case Stepper:
                case SpinBike:
                case Climbmill:
                    mRightValue.setText(BasicNumberFormat.format(value, 0));
                    mRightLabel.setText(StringLookup.get(R.string.next_resistance_uc));
                    mRightValue.setVisibility(View.VISIBLE);
                    mRightUnit.setVisibility(View.GONE);
                    mRightLabel.setVisibility(View.VISIBLE);
                    mRightBackground.setVisibility(View.VISIBLE);
                    break;
            }
        } else {
            mRightValue.setVisibility(View.INVISIBLE);
            mRightUnit.setVisibility(View.INVISIBLE);
            mRightLabel.setVisibility(View.INVISIBLE);
            mRightBackground.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void panelAdded() {
        // Not handled
    }

    @Override
    public void panelUpdated() {}

    @Override
    public void panelRemoved() {
        // Not handled
    }

    @Override
    public boolean setPanelMode(WorkoutPanelMode mode) { return false; }

    @Override
    public WindowManager.LayoutParams getPanelParams() {
        boolean isFullscreen = DataMatrix.getInt(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT) == UiServiceKeys.WorkoutPanelState.FULLSCREEN;
        if (isFullscreen) {
            int topBarHeight = (int) getResources().getDimension(R.dimen.top_bar_height);
            WindowManager.LayoutParams viewParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, topBarHeight,
                    ChimeraEnums.TYPE_SYSTEM_ERROR, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT);
            viewParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            viewParams.x = 0;
            viewParams.y = 0;
            return viewParams;
        } else {
            int topBarHeight = (int) getResources().getDimension(R.dimen.top_bar_height);
            int width = (int) getResources().getDimension(R.dimen.app_container_width);
            WindowManager.LayoutParams viewParams = new WindowManager.LayoutParams(width, topBarHeight,
                    ChimeraEnums.TYPE_SYSTEM_ERROR, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT);
            viewParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            viewParams.x = 0;
            viewParams.y = 0;
            return viewParams;
        }
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public String getDebugName() {
        return getClass().getName();
    }
}
