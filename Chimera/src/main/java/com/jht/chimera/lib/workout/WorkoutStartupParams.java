package com.jht.chimera.lib.workout;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import com.jht.chimera.lib.common.ObjectUtil;
import com.jht.chimera.lib.workout.generator.BaseSprint8Config;
import com.jhtp.dapiservice.wt.DapiProgramType;

import java.util.UUID;

/**
 * Attributes of a workout as it was started,
 * sufficient for starting a similar workout again.
 *
 * Does *not* record the actual workout (user input, stats, etc),
 *
 * Field names are used for serialization, so don't change them!
 */
public class WorkoutStartupParams
{
    public static class Builder {
        public @Nullable Integer programTypeId; // 18 is Sprint 8, etc.  See the DapiProgramTypes enum.
        public @Nullable String featureUuid;
        public @Nullable String appUuid;
        public @Nullable Integer workoutLevel; // The value of the "LEVEL" parameter, whatever it means
        /** For Sprint 8 workouts, name of media type enum value (MEDIA_AUDIO, etc) */
        public @Nullable String mediaTypeName;
        /** For Sprint8 custom workouts, the custom Sprint8 workout config */
        public @Nullable BaseSprint8Config sprint8Config;
        /** For Sprint8 custom workouts, the number of sprints in the workout */
        public @Nullable Integer numberOfSprints;
        /** For custom workouts, the UUID identifying the custom workout */
        public @Nullable String customWorkoutUuid;
        /** For landmark workouts, the location string identifying the landmark */
        public @Nullable String landmarkTitle;
    }

    public static WorkoutStartupParams forProgramType(int programTypeId) {
        Builder b = new Builder();
        b.programTypeId = programTypeId;
        return new WorkoutStartupParams(b);
    }
    public static WorkoutStartupParams forProgramType(DapiProgramType programType) {
        return forProgramType(programType.Value);
    }
    public static WorkoutStartupParams forProgramTypeAndApp(int programTypeId, String appUuid, String customWorkoutUuid) {
        Builder b = new Builder();
        b.programTypeId = programTypeId;
        b.appUuid = appUuid;
        b.customWorkoutUuid = customWorkoutUuid;
        return new WorkoutStartupParams(b);
    }
    public static WorkoutStartupParams forProgramTypeAndApp(int programTypeId, String appUuid) {
        return forProgramTypeAndApp(programTypeId, appUuid, null);
    }
    public static WorkoutStartupParams forCustomWorkout(String customWorkoutUuid) {
        Builder b = new Builder();
        b.programTypeId = DapiProgramType.CardioCustomWorkout.Value;
        b.customWorkoutUuid = customWorkoutUuid;
        return new WorkoutStartupParams(b);
    }
    public static WorkoutStartupParams forCustomWorkout(UUID customWorkoutUuid) {
        return forCustomWorkout(customWorkoutUuid == null ? null : customWorkoutUuid.toString());
    }

    public final @Keep @Nullable Integer programTypeId; // 18 is Sprint 8, etc.  See the DapiProgramTypes enum.
    public final @Keep @Nullable String featureUuid;
    public final @Keep @Nullable String appUuid;
    public final @Keep @Nullable Integer workoutLevel; // The value of the "LEVEL" parameter, whatever it means
    /** For Sprint 8 workouts, name of media type enum value (MEDIA_AUDIO, etc) */
    public final @Keep @Nullable String mediaTypeName;
    /** For Sprint 8 custom workouts, custom workout config */
    public final @Keep @Nullable BaseSprint8Config sprint8Config;
    /** For Sprint8 custom workouts, the number of sprints in the workout */
    public @Nullable Integer numberOfSprints;
    /** For custom workouts, the UUID identifying the custom workout */
    public final @Keep @Nullable String customWorkoutUuid;
    /** For landmark workouts, the title string identifying the landmark */
    public final @Keep @Nullable String landmarkTitle;

    public WorkoutStartupParams(WorkoutStartupParams params) {
        this.programTypeId = params.programTypeId;
        this.featureUuid = params.featureUuid;
        this.appUuid = params.appUuid;
        this.workoutLevel = params.workoutLevel;
        this.mediaTypeName = params.mediaTypeName;
        this.sprint8Config = params.sprint8Config;
        this.numberOfSprints = params.numberOfSprints;
        this.customWorkoutUuid = params.customWorkoutUuid;
        this.landmarkTitle = params.landmarkTitle;
    }

    public WorkoutStartupParams(Builder params) {
        this.programTypeId = params.programTypeId;
        this.featureUuid = params.featureUuid;
        this.appUuid = params.appUuid;
        this.workoutLevel = params.workoutLevel;
        this.mediaTypeName = params.mediaTypeName;
        this.sprint8Config = params.sprint8Config;
        this.numberOfSprints = params.numberOfSprints;
        this.customWorkoutUuid = params.customWorkoutUuid;
        this.landmarkTitle = params.landmarkTitle;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WorkoutStartupParams)) return false;
        WorkoutStartupParams other = (WorkoutStartupParams) obj;
        return programTypeId == other.programTypeId &&
                ObjectUtil.equals(featureUuid, other.featureUuid) &&
                ObjectUtil.equals(appUuid, other.appUuid) &&
                ObjectUtil.equals(workoutLevel, other.workoutLevel) &&
                ObjectUtil.equals(mediaTypeName, other.mediaTypeName) &&
                ObjectUtil.equals(sprint8Config, other.sprint8Config) &&
                ObjectUtil.equals(numberOfSprints, other.numberOfSprints) &&
                ObjectUtil.equals(customWorkoutUuid, other.customWorkoutUuid) &&
                ObjectUtil.equals(landmarkTitle, other.landmarkTitle);
    }

    // this will treat null and empty strings as equal
    public boolean equalsIgnoreEmptyString(Object obj) {
        if (!(obj instanceof WorkoutStartupParams)) return false;
        WorkoutStartupParams other = (WorkoutStartupParams) obj;
        return programTypeId == other.programTypeId &&
                ObjectUtil.equalsIgnoreEmptyString(featureUuid, other.featureUuid) &&
                ObjectUtil.equalsIgnoreEmptyString(appUuid, other.appUuid) &&
                ObjectUtil.equalsIgnoreEmptyString(workoutLevel, other.workoutLevel) &&
                ObjectUtil.equalsIgnoreEmptyString(mediaTypeName, other.mediaTypeName) &&
                ObjectUtil.equalsIgnoreEmptyString(sprint8Config, other.sprint8Config) &&
                ObjectUtil.equalsIgnoreEmptyString(numberOfSprints, other.numberOfSprints) &&
                ObjectUtil.equalsIgnoreEmptyString(customWorkoutUuid, other.customWorkoutUuid) &&
                ObjectUtil.equalsIgnoreEmptyString(landmarkTitle, other.landmarkTitle);
    }
}
