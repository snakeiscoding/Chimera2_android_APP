package com.jht.chimera.ui.mainapp.egym;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;

import com.jht.androidcommonalgorithms.math.Resolution;
import com.jht.androidcommonalgorithms.math.ValueStepRange;
import com.jht.androidcommonalgorithms.network.AddressHelper;
import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.conversionlib.DefaultEquations;
import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SSOKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.formatting.FloorFormat;
import com.jht.chimera.lib.common.i18n.LanguageProperty;
import com.jht.chimera.lib.common.i18n.LocaleUtil;
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.chimera.lib.workout.WorkoutInput;
import com.jht.chimera.lib.workout.WorkoutManager;
import com.jht.chimera.lib.workout.workoutlist.workoutmap.ChimeraWorkoutMap;
import com.jht.chimera.lib.workoutapps.AppIds;
import com.jht.chimera.service.hal.lib.hardware.RfidReaderManager;
import com.jht.chimera.ui.mainapp.EGymActivity;
import com.jht.data.RfidOperatingMode;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.data.sprint8.Sprint8SweatScores;
import com.jht.rfidlib.ReaderFeatures;
import com.jhtp.dapiservice.um.GenderIndentity;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EGymWebAppInterface {

    final String TAG = "EGymWebAppInterface";

    public interface OnEGymSateChange {
        void onChange(EGymState state);
    }

    public enum EGymWorkoutState {
        PRE,
        RUN,
        PAUSE,
        STOP
    }

    public enum EGymState {
        HOME,
        SELECTION,
        WORKOUT,
        SUMMARY,
        ERROR,
    }

    private final Context context = ContextUtil.getApplicationContext();
    private EGymState egymState = EGymState.HOME;
    private EGymWorkoutState eGymWorkoutState = EGymWorkoutState.PRE;
    private final EGymActivity eGymActivity;
    private String clientSecret;
    private String clientID;
    private boolean isLoggingIn = false;
    private OnEGymSateChange onEGymSateChange;

    public EGymWebAppInterface(@Nullable EGymActivity eGymActivity) {
        this.eGymActivity = eGymActivity;
    }

    @JavascriptInterface
    public void gainAudioFocus() {
        DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.Foreground.ordinal());
    }

    public void setOnEGymSateChange(OnEGymSateChange onEGymSateChange) {
        this.onEGymSateChange = onEGymSateChange;
    }

    private boolean wasEgymLaunched() {
        return AppIds.EGYM_UUID.equals(DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING));
    }

    // At the end of the workout this is called once more.  At that time the elapsed time is 0.
    // We store that value so that we send the total workout elapsed time in that case.
    private int previousElapsedTime = 0;
    @JavascriptInterface
    public String onMetricsUpdated() {
        Log.d(TAG, "onMetricsUpdated");
        String finalSnapshot = (DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == WorkoutKeys.Constants.RUN_STATE_STOP) ? "true": "false"; //NON-NLS
        FrameConfigManager.getInstance().refreshFrame();
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();

        //Initialize value variables
        int heartRate = DataMatrix.getInt(HALKeys.DATA_HEART_RATE_INT);
        if(heartRate < 0) heartRate = 0;
        int elapsedTime = getElapsedTime();
        if(elapsedTime == 0 && finalSnapshot.equalsIgnoreCase("true")) {
            if(previousElapsedTime == 0) {
                finalSnapshot = "false";
            }
            elapsedTime = previousElapsedTime;
            previousElapsedTime = 0;
        }
        else {
            previousElapsedTime = elapsedTime;
        }

        @NonNls
        String returnJson = "{\"distance\":" + Resolution.getDecimalFormat(0.01).format(DataMatrix.getFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT)) + "," + //NON-NLS
                "\"elapsedTime\":" + elapsedTime + "," + // NON-NLS
                "\"kcal\":" + Resolution.getDecimalFormat(1).format(DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT)) + "," + // NON-NLS
                "\"heartRate\":" + heartRate + "," + // NON-NLS
                "\"averageHeartRate\":" + DataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_HEART_RATE_FLOAT) + "," + // NON-NLS
                "\"heartRateStatus\":" + "\"" + ((heartRate > 0) ? "VALID" : "INVALID") + "\"," + // NON-NLS
                "\"rpm\":" + DataMatrix.getInt(HALKeys.DATA_RPM_INT) + "," + // NON-NLS
                "\"spm\":" + DataMatrix.getInt(HALKeys.DATA_SPM_INT) + "," + // NON-NLS
                (frameConfig.machineType.equals(JHTCommonTypes.MachineType.Treadmill) ? "\"speed\":" + DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT) : "\"speed\":" + DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT)) + "," + // NON-NLS
                "\"incline\":" + Float.parseFloat(Resolution.getDecimalFormat(0.1).format(DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT))) + "," + // NON-NLS
                "\"resistance\":" + DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT) + "," + // NON-NLS
                "\"floors\":" + DataMatrix.getFloat(WorkoutKeys.DATA_FLOORS_FLOAT) + "," + // NON-NLS
                "\"avgSpeed\":" + Resolution.getDecimalFormat(0.1).format(DataMatrix.getFloat(WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT)) + "," + // NON-NLS
                "\"avgIncline\":" + Float.parseFloat(Resolution.getDecimalFormat(frameConfig.incline.stepSize >= 1 ? 1 : 0.1).format(DataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_INCLINE_FLOAT))) + "," + // NON-NLS
                "\"avgResistance\":" + Resolution.getDecimalFormat(1).format(DataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_RESISTANCE_FLOAT)) + "," + // NON-NLS
                "\"avgRPM\":" + Resolution.getDecimalFormat(1).format(DataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_RPM_FLOAT)) + "," + // NON-NLS
                "\"avgSPM\":" + Float.parseFloat(Resolution.getDecimalFormat(1).format(DataMatrix.getInt(WorkoutKeys.DATA_AVERAGE_SPM_INT))) + "," + // NON-NLS
                "\"avgKcalPerMin\":" + Resolution.getDecimalFormat(1).format(getCalories() / elapsedTime) + "," + // NON-NLS
                "\"minResistanceLevel\":" + frameConfig.resistance.getMin() + "," + // NON-NLS
                "\"maxResistanceLevel\":" + frameConfig.resistance.getMax() + "," + // NON-NLS
                "\"minSpeed\":" + frameConfig.speedKph.getMin() + "," + // NON-NLS
                "\"maxSpeed\":" + frameConfig.speedKph.getMax() + "," + // NON-NLS
                "\"minIncline\":" + frameConfig.incline.getMin() + "," + // NON-NLS
                "\"maxIncline\":" + frameConfig.incline.getMax() + "," + // NON-NLS
                "\"macAddress\":" + "\"" + AddressHelper.getMACAddressStr("eth0", false) + "\"," + // NON-NLS
                "\"finalSnapshot\":\"" + finalSnapshot + "\"";// NON-NLS

        //workout ended and we want to know what the sprint scores are
        if(finalSnapshot.equals("true")) {

            //Is it actually Sprint 8?
            if(DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT) == WorkoutIds.SPRINT8) {
                Sprint8SweatScores resultParcel = DataMatrix.getParcelable(WorkoutKeys.DATA_SPRINT8_SWEAT_SCORES_PARCELABLE, Sprint8SweatScores.class);

                //Sprint scores saved correctly
                if(resultParcel != null) {
                    returnJson += ",\"sprint8ProgramLevel\":" + getSprintLevel(DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_LEVEL_INT)) + ",";
                    int totalScore = 0;

                    //for each sprint
                    for(int i = 0; i < resultParcel.sweatScores.size(); i++) {

                        //if there actually is a score (user did not skip/end early)
                        if(resultParcel.sweatScores.get(i) != null) {

                            //obtain sprint's score
                            float sprintIScore = Math.round(resultParcel.sweatScores.get(i));
                            //FloorFormat.format(currentProgress, 0);
                            //Math.round(sprintIScore)
                            returnJson += "\"sprint8Sprint" + (i + 1) + "SweatScore\":" + FloorFormat.format(sprintIScore, 0) + ","; //add to JSON
                            totalScore += Math.round(sprintIScore);
                        }
                    }
                    returnJson += "\"sprint8TotalSweatScore\":" + totalScore + "}";
                } else {
                    returnJson += "}";
                }
            } else {
                returnJson += "}";
            }
        } else {
            returnJson += "}";
        }
        Log.d(TAG, returnJson);
        return returnJson;
    }

    @JavascriptInterface
    public void resetStates() {

        Log.d(TAG, "webappinterface resetStates " + egymState + " " + eGymWorkoutState);
        if((eGymActivity != null && !eGymActivity.shown()) || DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP) { return; }

        // in practice the state seems to transition from SUMMARY to HOME before this is called? it's usually HOME PRE or HOME STOP coming into here?
        // may need to check for HOME STOP, or catch the actual transition of SUMMARY to HOME, or both?
        if(egymState.equals(EGymState.SUMMARY)) {
            Log.d(TAG, "webappinterface resetStates in SUMMARY, launch splash");
            LaunchAppUtil.launchSplashScreen();
            egymState = EGymState.HOME;
        }
        else {
            if (!DataMatrix.getBoolean(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL)) {
                Log.w(TAG, "Reset states: Lost Network Connection EGYM");
                String messageText = String.format((context.getString(R.string.connection_error_tc)));
                MessageCenterMessage message = new MessageCenterMessage("", messageText, 10, 30);
                DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, message);
            }
            egymState = EGymState.HOME;
            eGymWorkoutState = EGymWorkoutState.PRE;
            DataMatrix.putBoolean(HALKeys.DATA_SCAN_RFID_TAGS_BOOL, true);

            // Reset the user data.
            // (Wonder if at some point we could just emit a splash reset event here for services to handle?)
            DataMatrix.putBoolean(SettingKeys.General.DATA_IS_PTP_USER_BOOL, false);
            DapiUser user = new DapiUser();
            DataMatrix.putInt(SettingKeys.Application.DATA_USER_PROFILE_SELECTED_SLOT_INT, DAPIKeys.Constants.DAPI_GUEST_INDEX);
            user.sync();
            user.syncDisplayUnits();
            DataMatrix.putBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL, true);
            DataMatrix.putInt(DAPIKeys.DATA_USER_FTP_INT, DefaultEquations.getDefaultFtp(user.genderIdentity));
            DataMatrix.putInt(DAPIKeys.DATA_USER_MAXHR_INT, -1);

            if (onEGymSateChange != null) onEGymSateChange.onChange(egymState);
        }
    }

    @JavascriptInterface
    public void setEgymState(@NonNls String state) {
        Log.d(TAG, "webappinterface setting egymstate to " + state);
        switch (state.toLowerCase()) {
            case "home": // NON-NLS
                egymState = EGymState.HOME;
                break;
            case "selection": // NON-NLS
                egymState = EGymState.SELECTION;
                break;
            case "workout": // NON-NLS
                egymState = EGymState.WORKOUT;
                break;
            case "summary": // NON-NLS
                egymState = EGymState.SUMMARY;
                break;
            case "error": // NON-NLS
                egymState = EGymState.ERROR;
                break;
        }

        if (onEGymSateChange != null) onEGymSateChange.onChange(egymState);
    }

    @JavascriptInterface
    public String getMachineInformation() {
        Log.d(TAG, "getMachineInformation");
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        String rfidLocation = "";
        switch(ProductConfig.getConsoleType().rfidLocation) {

            case TOP_LEFT:
                rfidLocation = "Top Left"; break; // NON-NLS
            case TOP_MIDDLE:
                rfidLocation = "Top Middle"; break; // NON-NLS
            case TOP_RIGHT:
                rfidLocation = "Top Right"; break; // NON-NLS
            case LEFT_TOP:
                rfidLocation = "Left Top"; break; // NON-NLS
            case LEFT_MIDDLE:
                rfidLocation = "Left Middle"; break; // NON-NLS
            case LEFT_BOTTOM:
                rfidLocation = "Left Bottom"; break; // NON-NLS
            case RIGHT_TOP:
                rfidLocation = "Right Top"; break; // NON-NLS
            case RIGHT_MIDDLE:
                rfidLocation = "Right Middle"; break; // NON-NLS
            case RIGHT_BOTTOM:
                rfidLocation = "Right Bottom"; break; // NON-NLS
            case BOTTOM_LEFT:
                rfidLocation = "Bottom Left"; break; // NON-NLS
            case BOTTOM_MIDDLE:
                rfidLocation = "Bottom Middle"; break; // NON-NLS
            case BOTTOM_RIGHT:
                rfidLocation = "Bottom Right"; break; // NON-NLS
            case NONE:
                rfidLocation = "N/A"; break; // NON-NLS
        }
        String supportsWalletValue = RfidReaderManager.getReaderFeatures().contains(ReaderFeatures.WalletSupport) ? "true" : "false";
        @NonNls String returnJson =
                "{" +
                        "\"minResistanceLevel\":" + frameConfig.resistance.getMin() + "," + // NON-NLS
                        "\"maxResistanceLevel\":" + frameConfig.resistance.getMax() + "," + // NON-NLS
                        "\"minSpeed\":" + frameConfig.speedKph.getMin() + "," + // NON-NLS
                        "\"maxSpeed\":" + frameConfig.speedKph.getMax() + "," + // NON-NLS
                        "\"minIncline\":" + frameConfig.incline.getMin() + "," + // NON-NLS
                        "\"maxIncline\":" + frameConfig.incline.getMax() + "," + // NON-NLS
                        // Review if we need the macAddress here.
                        "\"macAddress\":" + "\"" + AddressHelper.getMACAddressStr("eth0", false) + "\"," + // NON-NLS
                        "\"rfidLocation\":\"" + rfidLocation + "\"" + "," + // NON-NLS
                        "\"version\":\"" + BuildConfig.VERSION_NAME + "\"," + // NON-NLS
                        "\"consoleSerialNumber\":\""+DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING)+ "\"," + // NON-NLS
                        "\"frameSerialNumber\":\""+DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING)+ "\","+ // NON-NLS
                        "\"supportsWallet\":" + supportsWalletValue + // NON-NLS (boolean so doesn't need extra quotes)
                        "}";
        Log.i(TAG, "getMachineInformation: " + returnJson);
        return returnJson;
    }

    @JavascriptInterface
    public void disablePedalChanges() {
        Log.d(TAG, "lock input");
        DataMatrix.putBoolean(WorkoutKeys.DATA_DISABLE_PEDAL_CHANGES_BOOL, true);
    }

    @JavascriptInterface
    public void enablePedalChanges() {
        Log.d(TAG, "unlock input");
        DataMatrix.putBoolean(WorkoutKeys.DATA_DISABLE_PEDAL_CHANGES_BOOL, false);
    }

    @JavascriptInterface
    @Deprecated
    public boolean StartEgymWorkout() {
        return startEgymWorkout();
    }

    @JavascriptInterface
    public boolean startEgymWorkout() {
        Log.d(TAG, "WebAppInterface.StartEgymWorkout");
        if(eGymActivity != null) {
            eGymActivity.fadeInWebView();
        }
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.WORKOUT.name());
        // if saftey pin is enabled, disable the prompt for egym since egym takes over the ui
        if (DataMatrix.getBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL)) {
            DataMatrix.putBoolean(HALKeys.DATA_DIGITAL_SAFETY_PIN_PROMPT_NEEDED_BOOL, false);
        }

        List<WorkoutInput> initialConditions = new ArrayList<>();
        initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.TIME_GOAL_START, 120));
        initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.START_SPEED, DataMatrix.getFloat(SettingKeys.Workout.DATA_START_SPEED_KPH_FLOAT)));
        initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.START_INCLINE, FrameConfigManager.getInstance().getFrameConfig().incline.getMin()));
        initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.START_RESISTANCE, DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_LEVEL_INT)));

        // We expect the EGYM workout to be there, we know initial conditions so we don't need to wait for callback
        WorkoutManager workoutManager = new WorkoutManager(new ChimeraWorkoutMap());
        workoutManager.loadWorkoutWithId(WorkoutIds.EGYM, null);
        return startWorkoutAndAwaitInitialization(workoutManager, initialConditions);
    }

    /*
    * We must wait for WorkoutControllerService to set the default resistance, speed, and incline
    * before letting EGYM set its values. This prevents the EGYM default values from being
    * overwritten by the WorkoutControllerService. This happens very quick -> no delay
    * can be seen by the user.
    * */
    private static boolean startWorkoutAndAwaitInitialization(WorkoutManager manager, List<WorkoutInput> initialConditions){
        DataMatrix dataMatrix = new DataMatrix();
        JHTCommonTypes.MachineClassificationType machineType = FrameConfigManager.getInstance().getFrameConfig().machineClassType;
        List<CompletableFuture<Void>> completableFuturesList;
        Map<Object, CompletableFuture<Void>> dataMatrixIntKeysMap = new HashMap<>();
        Map<Object, CompletableFuture<Void>> dataMatrixFloatKeysMap = new HashMap<>();
        JHTTimer timeout;
        boolean result;

        // Specify (by frame type) the DataMatrix Adapters and their associated CompletableFuture objects.
        dataMatrixIntKeysMap.put(WorkoutKeys.DATA_RUN_STATE_INT, new CompletableFuture<>());
        switch (machineType) {
            case Bike:
            case Elliptical:
            case RecumbentElliptical:
            case Stepper:
            case Climbmill:
                dataMatrixIntKeysMap.put(HALKeys.ACTION_SET_RESISTANCE_INT, new CompletableFuture<>());
                break;
            case Ascent:
                dataMatrixIntKeysMap.put(HALKeys.ACTION_SET_RESISTANCE_INT, new CompletableFuture<>());
                dataMatrixFloatKeysMap.put(HALKeys.ACTION_SET_INCLINE_FLOAT, new CompletableFuture<>());
                break;
            case Treadmill:
                dataMatrixFloatKeysMap.put(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, new CompletableFuture<>());
                dataMatrixFloatKeysMap.put(HALKeys.ACTION_SET_INCLINE_FLOAT, new CompletableFuture<>());
                break;
        }

        // Create specified DataMatrix adapters for integer values
        dataMatrixIntKeysMap.forEach((key, future) -> {
            if (key == WorkoutKeys.DATA_RUN_STATE_INT){
                dataMatrix.createIntAdapter(key, (runState) -> {
                    if (runState == WorkoutKeys.Constants.RUN_STATE_RUNNING) future.complete(null);
                }, false);
            }

            else {
                dataMatrix.createIntAdapter(key, (e) -> future.complete(null), false);
            }
        });

        // Create specified DataMatrix adapters for float values
        dataMatrixFloatKeysMap.forEach((key, future) -> dataMatrix.createFloatAdapter(key, (e) -> future.complete(null), false));

        // Get list of DataMatrix futures to be used in timeout and blocking
        completableFuturesList = new ArrayList<>(dataMatrixIntKeysMap.values());
        completableFuturesList.addAll(dataMatrixFloatKeysMap.values());

        // Create a timeout in case a future DataMatrix value is somehow not completed.
        timeout = new JHTTimer(1500, ()-> {
            Log.w("EGymWebAppInterface", "Timeout occurred waiting for Datamatrix Futures.");
            completableFuturesList.forEach((future) -> future.complete(null));
        });

        // Start the workout
        result = manager.startLoadedWorkout(initialConditions, false);

        timeout.start(); // Start timeout
        // Wait for all specified DataMatrix adapters to be launched
        CompletableFuture.allOf(completableFuturesList.toArray(new CompletableFuture[0])).join();
        timeout.stop(); // Stop timeout

        dataMatrix.finish(); // Cleanup dataMatrix object
        return result;
    }

    @JavascriptInterface
    @Deprecated
    public int getCurrentResistance() {
        return DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT);
    }

    @JavascriptInterface
    @Deprecated
    public double getCurrentIncline() {
        return DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT);
    }

    @JavascriptInterface
    @Deprecated
    public double getCurrentSpeed() {
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        return frameConfig.machineType.equals(JHTCommonTypes.MachineType.Treadmill) ?  DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT) : DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT);
    }

    @JavascriptInterface
    @Deprecated
    public void goToWelcomePage() {
        Log.d(TAG, "goToWelcomePage");
        egymState = EGymState.SELECTION;
        eGymWorkoutState = EGymWorkoutState.PRE;
        if(eGymActivity != null) {
            eGymActivity.reload();
        }
    }

    @JavascriptInterface
    public String getMeasurementUnit() {
        Log.d(TAG, "getMeasurementUnit");
        return DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL) ? "METRIC" : "IMPERIAL"; //NON-NLS
    }

    @JavascriptInterface
    public void hideWebApp() {
        Log.d(TAG, "call from web to hide app");
        boolean hasWorkoutStarted = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP;
        if (hasWorkoutStarted) {
            LaunchAppUtil.launchWorkoutScreen();
        }
    }

    @JavascriptInterface
    public boolean getIsFullscreen() {
        Log.d(TAG, "getIsFullscreen");
        return DataMatrix.getInt(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT) == UiServiceKeys.WorkoutPanelState.FULLSCREEN;
    }

    @JavascriptInterface
    public String getLocale() {
        Log.d(TAG, "getLocale");
        int languageIndex = DataMatrix.getInt(SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT);
        return LanguageProperty.getLanguagePropertyFromIndex(languageIndex).locale;
    }

    @JavascriptInterface
    public void setLocale(String locale) {
        Log.d(TAG, "received locale code: " + locale);
        if(context != null && (eGymActivity == null || eGymActivity.shown())) {
            for(LanguageProperty languageProperty : LanguageProperty.values()) {
                if(languageProperty.locale.toLowerCase(Locale.ENGLISH).startsWith(locale.toLowerCase(Locale.ENGLISH))) {
                    LocaleUtil.changeLanguage(context, languageProperty, false);
                    Log.d(TAG, "setting language locale code: " + languageProperty.locale);
                    return;
                }
            }
        }
    }

    @JavascriptInterface
    public void setUnit(String unit) {
        Log.d(TAG, "received unit " + unit);
        if (unit.toLowerCase().equals("metric")) {
            Log.d(TAG, "RECEIVED METRIC UNIT CHANGE REQUEST");
            DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, true);
        } else if (unit.toLowerCase().equals("imperial")) {
            Log.d(TAG, "RECEIVED IMPERIAL UNIT CHANGE REQUEST");
            DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, false);
        } else {
            Log.d(TAG, "INVALID UNIT RECEIVED " + unit);
        }
    }

    @JavascriptInterface
    @Deprecated
    public void changeResistance(boolean increase) {
        Log.d("EGymWebAppInterface","####CS changeResistnace, increase: "+increase);
        if (!wasEgymLaunched()) {
            return;
        }
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        if (increase) {
            DataMatrix.putInt(HALKeys.ACTION_SET_RESISTANCE_INT, (int)MathUtils.clamp(DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT) + frameConfig.resistance.stepSize, frameConfig.resistance.getMin(), frameConfig.resistance.getMax()));
        } else {
            DataMatrix.putInt(HALKeys.ACTION_SET_RESISTANCE_INT, (int)MathUtils.clamp(DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT) - frameConfig.resistance.stepSize, frameConfig.resistance.getMin(), frameConfig.resistance.getMax()));
        }
    }

    @JavascriptInterface
    @Deprecated
    public void changeIncline(boolean increase) {
        Log.d(TAG, "1 changeIncline: " + increase);
        if (!wasEgymLaunched()) {
            return;
        }
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        Log.d(TAG, "change incline " + increase + " " + DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT) + " " + frameConfig.incline.stepSize + " " + frameConfig.incline.getMin() + " " + frameConfig.incline.getMax());
        if (increase) {
            DataMatrix.putFloat(HALKeys.ACTION_SET_INCLINE_FLOAT, MathUtils.clamp(DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT) + frameConfig.incline.stepSize, frameConfig.incline.getMin(), frameConfig.incline.getMax()));
        } else {
            DataMatrix.putFloat(HALKeys.ACTION_SET_INCLINE_FLOAT, MathUtils.clamp(DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT) - frameConfig.incline.stepSize, frameConfig.incline.getMin(), frameConfig.incline.getMax()));
        }
    }

    @JavascriptInterface
    public void setInclineValue(double value) {
        Log.d(TAG, "setInclineValue");
        if (!wasEgymLaunched()) {
            return;
        }
        Log.d(TAG, "1 setInclineValue: " + value);
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        value = new ValueStepRange(frameConfig.incline.getMin(), frameConfig.incline.getMax(), frameConfig.incline.stepSize).fix(value);
        Log.d(TAG, "2 setInclineValue: " + value);
        DataMatrix.putFloat(HALKeys.ACTION_SET_INCLINE_FLOAT, (float) value);
    }

    @JavascriptInterface
    public void setResistanceValue(int value) {
        Log.d(TAG, "setResistanceValue");
        if (!wasEgymLaunched()) {
            return;
        }
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        Log.d(TAG, "setResistanceValue: " + value);
        DataMatrix.putInt(HALKeys.ACTION_SET_RESISTANCE_INT, (int)MathUtils.clamp(value, frameConfig.resistance.getMin(), frameConfig.resistance.getMax()));
    }

    @JavascriptInterface
    public void setSpeedValue(double value) {
        Log.d(TAG, "setSpeedValue");
        if (!wasEgymLaunched()) {
            return;
        }
        Log.d(TAG, "setSpeedValue: " + value);
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        DataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, (float)MathUtils.clamp(value, frameConfig.speedKph.getMin(), frameConfig.speedKph.getMax()));
    }

    @JavascriptInterface
    public void setScanMode(int mode) {
        Log.d(TAG, "setScanMode");
        if (!wasEgymLaunched()) {
            return;
        }
        Log.d(TAG, "setScanMode: " + mode);
        if (mode == 1)
            DataMatrix.putInt(HALKeys.ACTION_RFID_OPERATING_MODE_INT, RfidOperatingMode.Wallet.ordinal());
        else
            DataMatrix.putInt(HALKeys.ACTION_RFID_OPERATING_MODE_INT, RfidOperatingMode.Normal.ordinal());
    }

    @JavascriptInterface
    public void showMessage(String text, int displayTime) {
        Log.d(TAG, "Display time: " + displayTime + ", text: " + text);
        if (text != null && !text.isEmpty() && displayTime > 0) {
            MessageCenterMessage msg = new MessageCenterMessage("", text, 30);
            msg.setHideTime(displayTime / 1000);
            DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, msg);
        }
    }

    @JavascriptInterface
    public void showMatrixExperienceWelcomeScreen() {
        Log.d(TAG, "showMatrixExperienceWelcomeScreen");
        LaunchAppUtil.launchWelcomeScreen();
    }

    @JavascriptInterface
    public void showMatrixExperienceGoScreen() {
        Log.d(TAG, "showMatrixExperienceGoScreen");
        if(!DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ALLOW_GUEST_BOOL)) {
            LaunchAppUtil.launchLoginScreen();
        } else {
            LaunchAppUtil.launchGoScreen();
        }
    }

    @JavascriptInterface
    public boolean resizeScreen() {
        Log.d(TAG, "resizeScreen");
        return eGymActivity.resizeApp();
    }

    @JavascriptInterface
    public String getMachineSerialNumber(){
        Log.d(TAG, "getMachineSerialNumber");
        return DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING);
    }

    @JavascriptInterface
    public boolean pauseWorkout() {
        Log.d(TAG, "pauseWorkout");
        DataMatrix.sendEvent(WorkoutKeys.ACTION_PAUSE_WORKOUT);
        return true;
    }

    @JavascriptInterface
    public boolean resumeWorkout() {
        Log.d(TAG, "resumeWorkout");
        DataMatrix.sendEvent(WorkoutKeys.ACTION_RESUME_WORKOUT);
        return true;
    }

    @JavascriptInterface
    public boolean stopWorkout() {
        Log.d(TAG, "stopWorkout");
        DataMatrix.sendEvent(WorkoutKeys.ACTION_STOP_WORKOUT);
        return true;
    }

    @JavascriptInterface
    public String getWorkoutState() {
        Log.d(TAG, "getWorkoutState");
        Log.d(TAG, "WebAppInterface#getWorkoutState: " + eGymWorkoutState.name());
        return eGymWorkoutState.name();
    }

    @JavascriptInterface
    public void setWorkoutState(@NonNls String state) {
        Log.d(TAG, "Settings workoutstate from: " + eGymWorkoutState + " to: " + state);
        switch (state.toLowerCase()) {
            case "pre": // NON-NLS
                eGymWorkoutState = EGymWorkoutState.PRE;
                break;
            case "run": // NON-NLS
                eGymWorkoutState = EGymWorkoutState.RUN;
                break;
            case "pause": // NON-NLS
                eGymWorkoutState = EGymWorkoutState.PAUSE;
                break;
            case "stop": // NON-NLS
                eGymWorkoutState = EGymWorkoutState.STOP;
                break;
            default:
                Log.d(TAG, "Invalid state received");
                break;
        }
    }

    @JavascriptInterface
    public String getEgymState() {
        Log.d(TAG, "getEgymState");
        Log.d(TAG, "ControlPoint#getEgymState returning: " + egymState.name());
        return egymState.name().toLowerCase(Locale.ENGLISH);
    }

    public EGymState getEgymStateEnum() {
        return egymState;
    }

    @JavascriptInterface
    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @JavascriptInterface
    public String getClientID() {
        return clientID;
    }

    public void setClientID(String id) {
        this.clientID = id;
    }

    public boolean getIsLoginInProgress() {
        return isLoggingIn;
    }

    public void setLoginInProgress(boolean isLoggingIn) {
        this.isLoggingIn = isLoggingIn;
    }

    @JavascriptInterface
    public void notifyLoginComplete() {
        Log.d(TAG, "notifyLoginComplete");
        setLoginInProgress(false);
    }

    private int getElapsedTime() {
        Log.d(TAG, "getElapsedTime");
        return DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000;
    }

    private double getCalories() {
        Log.d(TAG, "getCalories");
        return DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT);
    }

    //Used to determine which Sprint 8 level workout this is
    private int getSprintLevel(int workoutLevel) {
        int sprintLevelToReturn = 0;
        if(workoutLevel < 1 || workoutLevel > 21) {
            sprintLevelToReturn = 1;
        } else {
            sprintLevelToReturn = workoutLevel;
        }
        return sprintLevelToReturn;
    }

    @JavascriptInterface
    public String getMachineType() {
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        Log.d(TAG, "getMachineType: " + frameConfig.machineClassType.name());
        return frameConfig.machineClassType.name();
    }

    @Deprecated
    @JavascriptInterface
    public void setUserProperties(double kg, int age, String gender, String id) {
        // Note the following log should not be in for release builds because it contains sensitive user information.
        if(BuildConfig.DEBUG) {
            Log.i(TAG, "setUserProperties " + kg + " " + age + " " + gender + " " + id);
        }
        DataMatrix.putBoolean(SSOKeys.DATA_USE_SSO_FOR_WEIGHT_BOOL, false);
        DataMatrix.putBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL, id.isEmpty());
        DataMatrix.putInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT, gender.equalsIgnoreCase("male") ? GenderIndentity.Male.Value : GenderIndentity.Female.Value);
        DataMatrix.putInt(DAPIKeys.DATA_USER_AGE_INT, age);
        DataMatrix.putFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT, (float)kg);
        DataMatrix.putString(DAPIKeys.DATA_USER_DAPI_UUID_STRING, id.isEmpty() ? DapiUser.GUEST_USER_UUID : "egym-" + id); // NON-NLS
    }

    @JavascriptInterface
    @Deprecated
    public void onMachineControl(String type, double value) {
        Log.d(TAG, "onMachineControl");
        //Larger method to handle any control input
        boolean hasWorkoutStarted = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP;
        if (hasWorkoutStarted) {
            switch (type.toLowerCase()) {
                case "incline": // NON-NLS
                    setInclineValue(value);
                    break;
                case "resistance": // NON-NLS
                    setResistanceValue((int) value);
                    break;
                case "speed": // NON-NLS
                    setSpeedValue(value);
                    break;
                case "seconds": // NON-NLS
                    DataMatrix.putInt(WorkoutKeys.ACTION_SET_TIME_GOAL_SECONDS_INT, (int) value);
                    break;
                case "pause": // NON-NLS
                    pauseWorkout();
                    break;
                case "stop": // NON-NLS
                    stopWorkout();
                    break;
                case "resume": // NON-NLS
                    resumeWorkout();
                    break;
                default:
                    showToast("Invalid control type sent!"); //NON-NLS
                    break;
            }
        } else {
            showToast("WORKOUT NOT STARTED! - No control point given!"); //NON-NLS
        }
    }

    private void showToast(String toast) {
        try {
            if (context != null) {
                Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
            }
        }
        catch(Exception ex) {
            Log.e(TAG, "Cannot do toast " + toast);
        }
    }

    @JavascriptInterface
    public void setUserInformation(String userName, String gender, String units, String userId, double weightInKilo, int age){
        // Note the following log should not be in for release builds because it contains sensitive user information.
        if(BuildConfig.DEBUG) {
            Log.i(TAG, "setUserInformation " + userName + " " + gender + " " + units + " " + userId + " " + weightInKilo + " " + age);
        }
        Log.d(TAG, "setUserInformation was called");

        // Handle in unit tests and when activity is shown.
        if(eGymActivity == null || eGymActivity.shown()) {
            FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
            weightInKilo = config.weightKg.clamp(weightInKilo);
            age = config.age.clamp(age);
            DataMatrix.putBoolean(SSOKeys.DATA_USE_SSO_FOR_WEIGHT_BOOL, false);
            DataMatrix.putBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL, userId.isEmpty());
            DataMatrix.putString(DAPIKeys.DATA_USER_DAPI_UUID_STRING, userId.isEmpty() ? DapiUser.GUEST_USER_UUID : "egym-" + userId); // NON-NLS
            switch (gender.toLowerCase(Locale.ENGLISH)) {
                case "female":
                    DataMatrix.putInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT, GenderIndentity.Female.Value);
                    break;
                case "other":
                    DataMatrix.putInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT, GenderIndentity.NonBinary.Value);
                    break;
                case "male":
                default:
                    DataMatrix.putInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT, GenderIndentity.Male.Value);
                    break;
            }
            DataMatrix.putInt(DAPIKeys.DATA_USER_AGE_INT, age);
            DataMatrix.putFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT, (float) weightInKilo);
            DataMatrix.putString(DAPIKeys.DATA_USER_FIRST_NAME_STRING, userName);
            DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, units.equalsIgnoreCase("metric"));
        }
    }
}