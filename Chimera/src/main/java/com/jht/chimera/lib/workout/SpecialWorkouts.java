package com.jht.chimera.lib.workout;

import static com.jht.chimera.lib.common.config.frames.FrameConfigEnum.CardioSpinBike;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DMCallback;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SpecialWorkouts {
    public static final String MIGROS_KEYCODE = "644767";

    /**
     * Separate key for iFIT for Athena that defaults to on (CHIM-7636)
     */
    public static final String IFIT_KEYCODE = "4348";
    public static final String YMCA360_KEYCODE = "9622";

    public static List<SpecialWorkout> getSpecialWorkouts() {
        List<SpecialWorkout> specialWorkouts = new ArrayList<>();

        SpecialWorkout ifitWorkout = getIfitWorkout();
        if (ifitWorkout != null) {
            specialWorkouts.add(ifitWorkout);
        }

        //Note: This is where app gets added. This is not for the carousel.
        SpecialWorkout migros = new SpecialWorkout(R.string.fitnesspark_tc, SettingKeys.Workout.DATA_MIGROS_ENABLED_BOOL, MIGROS_KEYCODE, true, WorkoutIds.MIGROS_POTENTIAL);
        specialWorkouts.add(migros);

        SpecialWorkout ymca360 = new SpecialWorkout(R.string.ymca_360_tc, SettingKeys.Experimental.DATA_ENABLE_YMCA_360_BOOL, YMCA360_KEYCODE, true, WorkoutIds.YMCA_360);
        specialWorkouts.add(ymca360);

        return specialWorkouts;
    }

    /**
     * Comment from Brenden Capps, 04/05/2021: "CHIM-7702 Make sure it works on Athena and nowhere
     * else. Also cleaned up the code to make sure we send the correct value to DAPI."
     * @return
     */
    public static boolean isIfitEnabled() {
        SpecialWorkout workout = getIfitWorkout();
        return workout != null && DataMatrix.getBoolean(workout.key, false);
    }

    public static void monitorIfitStatus(DataMatrix dm, DMCallback<Boolean> cb) {
        SpecialWorkout ifitWorkout = getIfitWorkout();
        if(ifitWorkout != null) {
            dm.createBoolAdapter(ifitWorkout.key, cb, false);
        }
    }

    /**
     * Athena defaults to on (CHIM-7636)
     * @return
     */
    private static SpecialWorkout getIfitWorkout() {
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();

        Object ifitKey = getIfitKey(frameConfig);
        if (ifitKey != null) {
            return new SpecialWorkout(R.string.ifit_tc, ifitKey, IFIT_KEYCODE,
                    FrameConfigManager.getInstance().getFrameType() != null
                            && FrameConfigManager.getInstance().getFrameType()
                            .equals(CardioSpinBike), WorkoutIds.IFIT);
        }
        return null;
    }

    /**
     * If product has IFIT, return the DM key. Athena defaults to on (CHIM-7636).
     *
     * @param frameConfig   determine value based on modality
     * @return              returns modality-specific DM key
     */
    @Nullable
    public static Object getIfitKey(FrameConfig frameConfig) {
        if(!ProductConfig.hasIFIT()) {
            return null;
        }

        if (ProductConfig.isProjectOne()) return SettingKeys.Workout.DATA_IFIT_ONYX_ENABLED_BOOL;
        if (ProductConfig.ifitByDefault()) {
            // For non LCG consoles, use original logic
            switch (frameConfig.machineType) {
                case Treadmill:
                    return SettingKeys.Workout.DATA_IFIT_TREADMILL_ENABLED_BOOL;
                case Elliptical:
                    return SettingKeys.Workout.DATA_IFIT_ELLIPTICAL_ENABLED_BOOL;
                case HybridBike:
                    return SettingKeys.Workout.DATA_IFIT_HYBRID_BIKE_ENABLED_BOOL;
                case UprightBike:
                    return SettingKeys.Workout.DATA_IFIT_UPRIGHT_BIKE_ENABLED_BOOL;
                case RecumbentBike:
                case RecumbentElliptical:
                    return SettingKeys.Workout.DATA_IFIT_RECUMBENT_BIKE_ENABLED_BOOL;
                case Ascent:
                    return SettingKeys.Workout.DATA_IFIT_ASCENT_ENABLED_BOOL;
                case LowerBodyAscent:
                    return SettingKeys.Workout.DATA_IFIT_LOWER_BODY_ASCENT_ENABLED_BOOL;
                case Stepper:
                    return SettingKeys.Workout.DATA_IFIT_STEPPER_ENABLED_BOOL;
                case Climbmill:
                    return SettingKeys.Workout.DATA_IFIT_CLIMBMILL_ENABLED_BOOL;
                case CardioSpinBike:
                    // If it's for Athena we need to default to on for Ifit, otherwise use the default setting.
                    return SettingKeys.Workout.DATA_IFIT_ENABLED_DEFAULT_ON_BOOL;
            }
        } else {
            // LCG only logic (see CGC-15581)
            switch (frameConfig.machineType) {
                case Treadmill:
                    return SettingKeys.Workout.DATA_IFIT_TREADMILL_ENABLED_TB_BOOL;
                case Elliptical:
                    return SettingKeys.Workout.DATA_IFIT_ELLIPTICAL_ENABLED_TB_BOOL;
                case HybridBike:
                    return SettingKeys.Workout.DATA_IFIT_HYBRID_BIKE_ENABLED_TB_BOOL;
                case UprightBike:
                    return SettingKeys.Workout.DATA_IFIT_UPRIGHT_BIKE_ENABLED_TB_BOOL;
                case RecumbentBike:
                case RecumbentElliptical:
                    return SettingKeys.Workout.DATA_IFIT_RECUMBENT_BIKE_ENABLED_TB_BOOL;
                case Ascent:
                    return SettingKeys.Workout.DATA_IFIT_ASCENT_ENABLED_TB_BOOL;
                case LowerBodyAscent:
                    return SettingKeys.Workout.DATA_IFIT_LOWER_BODY_ASCENT_ENABLED_TB_BOOL;
                case Stepper:
                    return SettingKeys.Workout.DATA_IFIT_STEPPER_ENABLED_TB_BOOL;
                case Climbmill:
                    return SettingKeys.Workout.DATA_IFIT_CLIMBMILL_ENABLED_TB_BOOL;
            }
        }
        return null;
    }

    public static class SpecialWorkout {
        private final int stringId;
        private final String keycode;
        private final Object key;
        private final boolean active;
        private final int workoutId;

        public SpecialWorkout(int stringId, Object key, String keycode, boolean active, int workoutId) {
            this.stringId = stringId;
            this.keycode = keycode;
            this.key = key;
            this.active = active;
            this.workoutId = workoutId;
        }

        public int stringId() { return stringId; }
        public Object key() { return key; }
        public String keycode() { return keycode; }
        public boolean active() { return active; }
        public int workoutId() { return workoutId; }
    }
}
