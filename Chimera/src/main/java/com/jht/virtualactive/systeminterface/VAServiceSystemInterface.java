package com.jht.virtualactive.systeminterface;

import static com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.RUN_STATE_PAUSE;
import static com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.RUN_STATE_RUNNING;
import static com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.RUN_STATE_STOP;
import static com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.RUN_STATE_SWITCHING;

import android.annotation.SuppressLint;
import android.media.AudioManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.ProfileValue;
import com.jht.chimera.lib.common.WorkoutFuture;
import com.jht.chimera.lib.common.WorkoutFutureProfile;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.conversionlib.DistanceConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.VaKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.log.ConsoleEvent;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.lib.va.VAWorkoutList;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.chimera.lib.workoutapps.AppIds;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.virtualactive.common.VaConfiguration;
import com.jht.virtualactive.common.handlers.InclineChangeHandler;
import com.jht.virtualactive.common.handlers.MachineTypeChangeHandler;
import com.jht.virtualactive.common.handlers.ResistanceChangeHandler;
import com.jht.virtualactive.common.handlers.RpmChangeHandler;
import com.jht.virtualactive.common.handlers.SelectedWorkoutChangeHandler;
import com.jht.virtualactive.common.handlers.SpeedChangeHandler;
import com.jht.virtualactive.common.handlers.WorkoutStateChangeHandler;
import com.jht.virtualactive.common.models.VAProfileEntryParcel;
import com.jht.virtualactive.service.IVAServiceSystemInterface;
import com.jht.virtualactive.service.VAWorkoutState;
import com.jht.virtualactive.service.datamodels.Workout;
import com.jht.workoutcontrollerlib.WorkoutController;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;

/**
 * Virtual Active System Interface consumed by the Virtual Active Service <strong>only</strong>.
 * This is included by the Virtual Active artifact using reflection.
 *
 * @author Andy Rosene (2019.11.15)
 */
@SuppressLint("LongLogTag")
public class VAServiceSystemInterface implements IVAServiceSystemInterface {
    private final VaConfiguration config = new VaConfiguration();
    private boolean hasVaAppLaunched = false;

    public VAServiceSystemInterface() {
        // CGC-22468 Chimera2 with exo player seems to have issues with both SurfaceView and TextureView freezing up.
        // CGC-25258 Chimera3 has the same issue as Chimera2
        config.recreateExoPlayerOnSurfaceChange = ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_2) ||
                ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_3);
        config.minBufferMs = 40_000;
        config.maxBufferMs = 80_000;
        config.bufferForPlaybackMs = 2500;
        config.bufferForPlaybackAfterRebufferMs = 1000;
        config.maxPlayRate = 1.25f;
    }

    @NonNull
    @Override
    public VaConfiguration getConfiguration() {
        return config;
    }

    /**
     * The tag to use when outputting log messages.
     */
    @NonNls private static final String TAG = "VAServiceSystemInterface";
    private final DataMatrix dataMatrix = new DataMatrix();

    @Override
    public int getAudioStreamType() {
        return AudioManager.STREAM_MUSIC;
    }

    @Override
    public double getIncline() {
        return Float.valueOf(DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT)).doubleValue();
    }

    @Override
    public int getLevel() {
        return DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_LEVEL_INT);
    }

    @Override
    public int getResistance() {
        return DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT);
    }

    @Override
    public int getRpm() {
        return DataMatrix.getInt(HALKeys.DATA_RPM_INT);
    }

    @Override
    public double getSpeedMPH() {
        if (isTreadmill()) {
            float targetMPH = DistanceConvertUtil.convertKilometersToMiles(DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT));
            return Float.valueOf(targetMPH).doubleValue();
        } else {
            float actualMPH = DistanceConvertUtil.convertKilometersToMiles(DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT));
            return Float.valueOf(actualMPH).doubleValue();
        }
    }

    @Override
    public boolean isClimbmill() {
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        return config.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill ;
    }

    @Override
    public boolean isInCooldown() {
        return DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT) == WorkoutController.WORKOUT_STAGE_COOLDOWN;
    }

    @Override
    public boolean isMaximized() {
        return UiServiceKeys.WorkoutPanelState.FULLSCREEN == DataMatrix.getInt(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT);
    }

    @Override
    public boolean isPaused() {
        return DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == RUN_STATE_PAUSE;
    }

    @Override
    public boolean isTreadmill() {
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        return config.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill;
    }

    @SuppressLint("SdCardPath")
    @NonNull
    @Override
    public List<String> getKnownVAContentLocations() {
        // fixme - this needs to be modified so that instead of having all of the locations bunched
        //         like this, we have one main, and all the rest are optional that have timeouts
        //         for the availability.
        return new ArrayList<String>() {{
            add(StorageUtils.getSdcardPath()); //NON-NLS
            add(StorageUtils.getUdiskPath()); //NON-NLS
            add(StorageUtils.getUdisk2Path()); //NON-NLS
            add("/sdcard/va/"); //NON-NLS
            add("/mnt/backup/va"); //NON-NLS
        }};
    }

    @SuppressLint("SdCardPath")
    public String getPrioritizedVAContentLocation() {
        return "/sdcard/jht/virtual_active";
    }

    @Override
    public void maximizeView() {
        DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.FULLSCREEN);
    }

    @Override
    public void minimizeView() {
        DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.REGULAR);
    }

    @Override
    public void registerInclineChangeListener(InclineChangeHandler handler) {
        dataMatrix.createFloatAdapter(HALKeys.DATA_TARGET_INCLINE_FLOAT, value -> handler.run(value.doubleValue()));
    }

    @Override
    public void registerMachineTypeChangeListener(MachineTypeChangeHandler handler) {
        dataMatrix.createStringAdapter(SettingKeys.General.DATA_FRAME_TYPE_STRING, s -> handler.run());
    }

    @Override
    public void registerResistanceChangeListener(ResistanceChangeHandler handler) {
        dataMatrix.createIntAdapter(HALKeys.DATA_RESISTANCE_INT, handler::run);
    }

    @Override
    public void registerRpmChangeListener(RpmChangeHandler handler) {
        // fixme - need to create unregister for this callback.
        dataMatrix.createIntAdapter(HALKeys.DATA_RPM_INT, handler::run);
    }

    @Override
    public void registerSelectedWorkoutChangeListener(SelectedWorkoutChangeHandler handler) {
        dataMatrix.createStringAdapter(VaKeys.ACTION_SELECT_WORKOUT_UNIQUEID_STRING, handler::run);
    }

    @Override
    public void registerSpeedChangeListener(SpeedChangeHandler handler) {
        // use only if treadmill
        dataMatrix.createFloatAdapter(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, targetSpeedKph -> {
            if (isTreadmill()) {
                final float targetSpeedMph = DistanceConvertUtil.convertKilometersToMiles(targetSpeedKph);
                handler.run(Float.valueOf(targetSpeedMph).doubleValue());
            }
        });

        // use only if !treadmill
        dataMatrix.createFloatAdapter(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT, actualSpeedKph -> {
            if (!isTreadmill()) {
                final float actualSpeedMph = DistanceConvertUtil.convertKilometersToMiles(actualSpeedKph);
                handler.run(Float.valueOf(actualSpeedMph).doubleValue());
            }
        });
    }

    @Override
    public void registerWorkoutStateChangeListener(WorkoutStateChangeHandler handler) {
        // hasVaAppLaunched will be set to true when VA App launch is launched and
        // will reset when RUN_STATE_STOP is triggered
        dataMatrix.createStringAdapter(UiServiceKeys.EVENT_LAUNCHING_APP_UUID_STRING, value -> {
            if (value.equals(AppIds.VA_UUID)) {
                // If this is the first VA app launch and the workout is already running, set the VA workout
                // state to running; otherwise, just set hasVaAppLaunched to true.
                if (!hasVaAppLaunched && DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == RUN_STATE_RUNNING) {
                    hasVaAppLaunched = true;
                    if (canStartVirtualActive()) handler.run(VAWorkoutState.RUNNING);
                } else {
                    hasVaAppLaunched = true;
                }
            }
        });

        dataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, value -> {
            if (!canStartVirtualActive()) return; // return if VA cannot be started

            switch (value) {
                case RUN_STATE_PAUSE:
                    handler.run(VAWorkoutState.PAUSED);
                    break;

                case RUN_STATE_RUNNING:
                    handler.run(VAWorkoutState.RUNNING);
                    break;

                case RUN_STATE_STOP:
                    // Clear the previously selected workout value then handle the state.
                    hasVaAppLaunched = false; // reset flag that tracks if VA app has launched
                    DataMatrix.putString(VaKeys.DATA_SELECTED_WORKOUT_UNIQUEID_STRING, ""); // fixme - this should be in the artifact
                    handler.run(VAWorkoutState.STOPPED);
                    break;

                case RUN_STATE_SWITCHING:
                    handler.run(VAWorkoutState.CHANGING);
                    break;
            }
        });
    }

    @Override
    public void setIncline(double incline) {
        DataMatrix.putFloat(WorkoutKeys.ACTION_SET_INCLINE_WORKOUT_FLOAT, Double.valueOf(incline).floatValue());
    }

    @Override
    public void setResistance(int resistance) {
        DataMatrix.putInt(WorkoutKeys.ACTION_SET_RESISTANCE_WORKOUT_INT, resistance);
    }

    @Override
    public void unregisterListeners() {
        dataMatrix.finish();
    }

    @Override
    public void updateAvailableWorkouts(@NonNull List<Workout> availableWorkouts) {
        if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_DELAY_LOAD_VA_BOOL)) {
            // This is useful for testing a boot that is too fast for VA.
            new JHTTimer(30000, () -> DataMatrix.putParcelable(VaKeys.DATA_VA_ITEMS_PARCELABLE, new VAWorkoutList(availableWorkouts))).start();
        }
        else {
            ConsoleEvent.logEvent("VA Content Found, Size: " + availableWorkouts.size());
            DataMatrix.putParcelable(VaKeys.DATA_VA_ITEMS_PARCELABLE, new VAWorkoutList(availableWorkouts));
        }
    }

    @Override
    public void updateCanDisplayAppIcon(final boolean canDisplayAppIcon) {
        DataMatrix.putBoolean(VaKeys.DATA_VA_CAN_DISPLAY_APP_ICON_BOOL, canDisplayAppIcon);
    }

    @Override
    public void updateFutureProfile(@NonNull List<VAProfileEntryParcel> resistanceProfile, @NonNull List<VAProfileEntryParcel> inclineProfile) {
        final int currentTimeMs = DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT);
        final List<ProfileValue> inclineValues = new ArrayList<>();
        final List<ProfileValue> resistanceValues = new ArrayList<>();

        for (final VAProfileEntryParcel entry : inclineProfile) {
            inclineValues.add(new ProfileValue(
                    (float) (entry.vaProfileEntry.getTimeMs() + currentTimeMs),
                    entry.vaProfileEntry.getValue()
            ));
        }

        for (final VAProfileEntryParcel entry : resistanceProfile) {
            resistanceValues.add(new ProfileValue(
                    (float) (entry.vaProfileEntry.getTimeMs() + currentTimeMs),
                    entry.vaProfileEntry.getValue()
            ));
        }

        DataMatrix.putParcelable(WorkoutKeys.DATA_WORKOUT_FUTURE_PARCELABLE, new WorkoutFuture(new ArrayList<WorkoutFutureProfile>() {{
            add(new WorkoutFutureProfile(ConsoleWorkoutVariables.INCLINE, inclineValues));
            add(new WorkoutFutureProfile(ConsoleWorkoutVariables.RESISTANCE, resistanceValues));
        }}));
    }

    @Override
    public boolean canDisplayProfileGraph() {
        // The profile graph can be displayed as long as the goal type is not NONE.
        return DataMatrix.getInt(WorkoutKeys.DATA_GOAL_TYPE_INT) != WorkoutKeys.Constants.GOAL_TYPE_NONE;
    }

    @Override
    public void closeApp() {
        LaunchAppUtil.launchWorkoutScreen();
    }

    /**
     * Check if Virtual Active is capable of starting in the background. Virtual Active should be
     * started in all but the following scenarios:
     * <p></p>
     *
     * <table>
     *     <thead>
     *         <tr>
     *             <th>Scenario</th>
     *             <th>Reason(s)</th>
     *         </tr>
     *     </thead>
     *     <tbody>
     *         <tr>
     *             <td>The iFit app is running.</td>
     *             <td>You cannot switch from iFit to Virtual Active. Therefore, it does not make
     *             sense to support the "random" workout scenario which will consume additional
     *             resources.</td>
     *         </tr>
     *     </tbody>
     * </table>
     *
     * @return true if virtual active can be started, else false.
     */
    private boolean canStartVirtualActive() {
        if (ProductConfig.hasIFIT() && AppIds.IFIT_UUID.equals(DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING))) {
            Log.d(TAG, "VirtualActive cannot be started because iFit is being started.");
            return false;
        }

        // return false if VA app has not launched
        if (!hasVaAppLaunched) return false;

        Log.d(TAG, "VirtualActive can be started.");
        return true;
    }
}
