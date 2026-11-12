package com.jht.chimera.service.matrixcomm.messages.events;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.datamatrixlib.DataMatrix;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Used to notify third parties of workout state events.
 */
public class WorkoutStateEvent extends EventBase {
    public enum WorkoutState {
        RUN,
        PAUSE,
        STOP,
        UNKNOWN;
        public static WorkoutState fromString(String value) {
            for(WorkoutState state : WorkoutState.values()) {
                if(state.toString().toLowerCase(Locale.ENGLISH).equals(value.toLowerCase(Locale.ENGLISH))) {
                    return state;
                }
            }
            return UNKNOWN;
        }

        public static WorkoutState fromInt(int value) {
            if(value == WorkoutKeys.Constants.RUN_STATE_RUNNING)
                return RUN;
            else if(value == WorkoutKeys.Constants.RUN_STATE_PAUSE)
                return PAUSE;
            else if(value == WorkoutKeys.Constants.RUN_STATE_STOP)
                return STOP;
            return UNKNOWN;
        }
    }

    public enum WorkoutStage {
        WARMUP,
        COOLDOWN,
        WORKOUT,
        UNKNOWN;
        public static WorkoutStage fromString(String value) {
            for(WorkoutStage stage : WorkoutStage.values()) {
                if(stage.toString().toLowerCase(Locale.ENGLISH).equals(value.toLowerCase(Locale.ENGLISH))) {
                    return stage;
                }
            }
            return UNKNOWN;
        }

        public static WorkoutStage fromInt(int value) {
            if(value == WorkoutKeys.Constants.WORKOUT_STAGE_WARMUP)
                return WARMUP;
            else if(value == WorkoutKeys.Constants.WORKOUT_STAGE_COOLDOWN)
                return COOLDOWN;
            else if(value == WorkoutKeys.Constants.WORKOUT_STAGE_WORKOUT)
                return WORKOUT;
            return UNKNOWN;
        }
    }

    private final WorkoutState workoutState;
    private final WorkoutStage workoutStage;
    private final int time;
    private final int pauseTime;
    private final double distance;
    private final int calories;
    private final @Nullable Integer activeCalories; // treat null as unavailable
    private final int heartrate;
    private final int rpm;
    private final double speed;
    private final double incline;
    private final int resistance;
    private final int floors;
    private final int spm;
    private final int watts;
    private final double elevation;

    /**
     * Create a workout state event.
     */
    public WorkoutStateEvent() {
        super("workoutstate", 1);

        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        this.workoutState = WorkoutState.fromInt(DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT));
        this.workoutStage = WorkoutStage.fromInt(DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT));
        time = DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000;
        pauseTime = this.workoutState.equals(WorkoutState.PAUSE) ?
            DataMatrix.getInt(UiServiceKeys.DATA_PAUSE_TIME_INT) - DataMatrix.getInt(UiServiceKeys.DATA_PAUSE_TIME_REMAINING_INT) :
            0;
        if(frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Climbmill) || frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Stepper)) {
            distance = DataMatrix.getInt(WorkoutKeys.DATA_STEPS_INT) * frameConfig.getDistancePerStep();
        }
        else {
            distance = DataMatrix.getFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT);
        }
        calories = (int)Math.floor(DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT));
        // if apple gym kit is connected, report the active calories
        int gymKitState = DataMatrix.getInt(HALKeys.DATA_GYM_KIT_STATUS_INT);
        if (gymKitState == HALKeys.STATUS_GYM_KIT_CONNECTED || gymKitState == HALKeys.STATUS_GYM_KIT_OUT_OF_RANGE) {
            activeCalories = (int) Math.floor(DataMatrix.getFloat(WorkoutKeys.DATA_ACTIVE_CALORIES_FLOAT));
        } else {
            activeCalories = null;
        }
        heartrate = DataMatrix.getInt(HALKeys.DATA_HEART_RATE_INT);

        switch (frameConfig.machineClassType) {
            case Climbmill:
            case Stepper:
                rpm = DataMatrix.getInt(HALKeys.DATA_SPM_INT);
                speed = rpm * frameConfig.getDistancePerStep() * 60;
                break;
            case Treadmill:
                rpm = DataMatrix.getInt(HALKeys.DATA_RPM_INT);
                speed = DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT);
                break;
            default:
                rpm = DataMatrix.getInt(HALKeys.DATA_RPM_INT);
                speed = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT);
                break;
        }
        incline = DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT);
        resistance = DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT);
        floors = (int)Math.floor(DataMatrix.getFloat(WorkoutKeys.DATA_FLOORS_FLOAT));
        spm = DataMatrix.getInt(HALKeys.DATA_SPM_INT);
        watts = (int)Math.floor(DataMatrix.getFloat(HALKeys.DATA_WATTS_FLOAT));
        elevation = DataMatrix.getFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT);
    }

    /**
     * Used for testing.
     * @param workoutState  Expected state.
     * @param workoutStage  Expected stage.
     */
    @VisibleForTesting
    public WorkoutStateEvent(
            WorkoutState workoutState,
            WorkoutStage workoutStage
    ) {
        super("workoutstate", 1);
        this.workoutState = workoutState;
        this.workoutStage = workoutStage;
        this.time = 0;
        this.pauseTime = 0;
        this.distance = 0;
        this.calories = 0;
        this.activeCalories = null;
        this.heartrate = 0;
        this.rpm = 0;
        this.speed = 0;
        this.incline = 0;
        this.resistance = 0;
        this.floors = 0;
        this.spm = 0;
        this.watts = 0;
        this.elevation = 0;
    }

    /**
     * Parse the event we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    @VisibleForTesting
    public WorkoutStateEvent(JSONObject object) throws JSONException {
        super(object);
        workoutState = WorkoutState.fromString(object.getString("state"));
        workoutStage = WorkoutStage.fromString(object.getString("stage"));
        time = object.getInt("time");
        pauseTime = object.getInt("pausetime");
        distance = object.getDouble("distance");
        calories = object.getInt("calories");
        activeCalories = object.has("activeCalories") ? object.getInt("activeCalories") : null;
        heartrate = object.getInt("heartrate");
        rpm = object.getInt("rpm");
        speed = object.getDouble("speed");
        incline = object.getDouble("incline");
        resistance = object.getInt("resistance");
        floors = object.getInt("floors");
        spm = object.getInt("spm");
        watts = object.getInt("watts");
        elevation = object.getDouble("elevation");
    }

    /**
     * @return Number of fields the event should contain.  Useful for testing.
     */
    @VisibleForTesting
    public int expectedFields() { return 15 + super.expectedFields(); }

    /**
     * @return  The event in JSON form.
     */
    public JSONObject getRequest() {
        JSONObject object = super.getRequest();
        try {
            object.put("state", workoutState.toString().toLowerCase(Locale.ENGLISH));
            object.put("stage", workoutStage.toString().toLowerCase(Locale.ENGLISH));
            object.put("time", time);
            object.put("pausetime", pauseTime);
            object.put("distance", distance);
            object.put("calories", calories);
            object.put("heartrate", heartrate);
            object.put("rpm", rpm);
            object.put("speed", speed);
            object.put("incline", incline);
            object.put("resistance", resistance);
            object.put("floors", floors);
            object.put("spm", spm);
            object.put("watts", watts);
            object.put("elevation", elevation);
            if (activeCalories != null) object.put("activeCalories", activeCalories);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }


    @Override
    public boolean equals(Object other) {
        if(other instanceof WorkoutStateEvent) {
            WorkoutStateEvent otherResponse = (WorkoutStateEvent)other;
            // Note we can't fully predict the event so just checking the state and stage.
            return super.equals(other) &&
                    otherResponse.workoutState.equals(workoutState) &&
                    otherResponse.workoutStage.equals(workoutStage);
        }
        return false;
    }

    @Override
    public String toString() {
        return "ErrorEvent: workoutState " + workoutState +
                ", workoutStage " +  workoutStage +
                ", time " +  time +
                ", pauseTime " +  pauseTime +
                ", distance " +  distance +
                ", calories " +  calories +
                (activeCalories != null ? ", activeCalories " +  activeCalories : "") +
                ", heartrate " +  heartrate +
                ", rpm " +  rpm +
                ", speed " +  speed +
                ", incline " +  incline +
                ", resistance " +  resistance +
                ", floors " +  floors +
                ", spm " +  spm +
                ", watts " +  watts +
                ", elevation " +  elevation + ", " + super.toString();
    }


    // The following are useful for testing.

    @VisibleForTesting public int time() { return time; }
    @VisibleForTesting public int pauseTime() { return pauseTime; }
    @VisibleForTesting public double distance() { return distance; }
    @VisibleForTesting public int calories() { return calories; }
    @VisibleForTesting public Integer activeCalories() { return activeCalories; }
    @VisibleForTesting public int heartrate() { return heartrate; }
    @VisibleForTesting public int rpm() { return rpm; }
    @VisibleForTesting public double speed() { return speed; }
    @VisibleForTesting public double incline() { return incline; }
    @VisibleForTesting public int resistance() { return resistance; }
    @VisibleForTesting public int floors() { return floors; }
    @VisibleForTesting public int spm() { return spm; }
    @VisibleForTesting public int watts() { return watts; }
    @VisibleForTesting public double elevation() { return elevation; }

}
