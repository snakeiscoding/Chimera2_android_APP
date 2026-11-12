package com.jht.chimera.lib.widget.statarray.formatting;

import android.content.Context;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.conversionlib.ConversionHelper;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.formatting.BasicNumberFormat;
import com.jht.chimera.lib.common.formatting.DistanceFormat;
import com.jht.chimera.lib.common.formatting.TimeFormat;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.widget.statarray.StatValueUpdater;
import com.jht.datamatrixlib.DataMatrix;

/**
 * Formats goal values that are passed into this class into correct time formats for the stat bar
 * <p>
 * Created by chris.foyer on 8/23/2017.
 */
public class GoalFormatter implements IStatFormattingLogic, IStatTitles {
    private StatValueUpdater.IStatValueUpdaterCallback mCallback = null;
    private int mGoalType;
    private DataMatrix mDataMatrix = new DataMatrix();

    private Context mContext;
    private boolean mPreferMetric;

    // Whether this is for the remaining goal or total goal
    private boolean mIsRemaining = false;

    public GoalFormatter(Context context, boolean isRemaining) {
        mIsRemaining = isRemaining;
        mContext = context;
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_GOAL_TYPE_INT, value -> {
            mGoalType = value;
            if (mCallback != null) {
                mCallback.forceStatUpdate();
            }
        },true);

        mDataMatrix.createBoolAdapter(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, value -> {
            mPreferMetric = value;
            if (mCallback != null) {
                mCallback.forceStatUpdate();
            }
        },true);

    }

    @Override
    public String format(String value) {
        throw new RuntimeException("Goal remaining cannot be formatted from string");
    }

    @Override
    public String format(Integer value) {
        throw new RuntimeException("Goal remaining cannot be formatted from string");
    }

    @Override
    public String format(Double value) {
        throw new RuntimeException("Goal remaining cannot be formatted from double");
    }

    @Override
    public String format(Float value) {
        return formatGoalValue(value);
    }

    @Override
    public void setStatUpdaterCallback(StatValueUpdater.IStatValueUpdaterCallback statUpdaterCallback) {
        mCallback = statUpdaterCallback;
    }

    @Override
    public String getStatTitle() {
        return getTitle();
    }

    @Override
    public String getStatDefaultValue() {
        return "";
    }

    private String formatGoalValue(float value) {
        // Do not show goal values below 0
        if (value < 0) value = 0;

        switch (mGoalType) {
            case WorkoutKeys.Constants.GOAL_TYPE_DISTANCE:
            case WorkoutKeys.Constants.GOAL_TYPE_DISTANCE_STAGE:
                return DistanceFormat.formatWorkoutDistanceGoal(value, mPreferMetric);
            case WorkoutKeys.Constants.GOAL_TYPE_CALORIES:
            case WorkoutKeys.Constants.GOAL_TYPE_CALORIES_STAGE:
            case WorkoutKeys.Constants.GOAL_TYPE_FLOORS:
            case WorkoutKeys.Constants.GOAL_TYPE_FLOORS_STAGE:
            case WorkoutKeys.Constants.GOAL_TYPE_ELEVATION:
            case WorkoutKeys.Constants.GOAL_TYPE_ELEVATION_STAGE:
                value = ConversionHelper.roundCeil(value, 0);
                return BasicNumberFormat.format(value, 0);
            case WorkoutKeys.Constants.GOAL_TYPE_TIME:
            case WorkoutKeys.Constants.GOAL_TYPE_TIME_STAGE:
                if (mIsRemaining) return TimeFormat.formatTimeRemaining(value);
                return TimeFormat.formatTime(value);
            default:
                return BasicNumberFormat.format(value, 0);
        }
    }

    private String getTitle() {
        switch (mGoalType) {
            case WorkoutKeys.Constants.GOAL_TYPE_DISTANCE:
            case WorkoutKeys.Constants.GOAL_TYPE_DISTANCE_STAGE:
                if (!mIsRemaining)
                    return StringLookup.getString(R.string.distance_goal_uc, mContext);
                else
                    return StringLookup.getString(R.string.distance_remaining_uc, mContext);
            case WorkoutKeys.Constants.GOAL_TYPE_CALORIES:
            case WorkoutKeys.Constants.GOAL_TYPE_CALORIES_STAGE:
                if (!mIsRemaining)
                    return StringLookup.getString(R.string.calorie_goal_uc, mContext);
                else
                    return StringLookup.getString(R.string.calories_remaining_uc, mContext);
            case WorkoutKeys.Constants.GOAL_TYPE_FLOORS:
            case WorkoutKeys.Constants.GOAL_TYPE_FLOORS_STAGE:
                if (!mIsRemaining)
                    return StringLookup.getString(R.string.floor_goal_uc, mContext);
                else
                    return StringLookup.getString(R.string.floors_remaining_uc, mContext);
            case WorkoutKeys.Constants.GOAL_TYPE_TIME:
            case WorkoutKeys.Constants.GOAL_TYPE_TIME_STAGE:
                if (!mIsRemaining)
                    return StringLookup.getString(R.string.time_goal_uc, mContext);
                else
                    return StringLookup.getString(R.string.time_remaining_uc, mContext);
            default:
                if (!mIsRemaining)
                    return StringLookup.getString(R.string.time_goal_uc, mContext);
                else
                    return StringLookup.getString(R.string.time_remaining_uc, mContext);
        }
    }
}
