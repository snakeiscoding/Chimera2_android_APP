package com.jht.chimera.lib.common.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.log.ConsoleEvent;
import com.jht.chimera.lib.workoutapps.AppIconManager;
import com.jht.chimera.lib.workoutapps.AppIds;
import com.jht.chimera.lib.workoutapps.AppInfo;
import com.jht.chimera.lib.workoutapps.AppManager;
import com.jht.chimera.service.ui.CommonScreens;
import com.jht.chimera.service.ui.LaunchAppCondition;
import com.jht.chimera.service.ui.LaunchAppData;
import com.jht.chimera.service.ui.LaunchScreenData;
import com.jht.chimera.service.ui.UiConfiguration;
import com.jht.chimera.service.ui.UiLauncherServiceHelperKt;
import com.jht.chimera.ui.machinesettings.dialogs.ProjectDialogFactory;
import com.jht.chimera.ui.mainapp.createaccount.LoginRegisterActivity;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.interfaces.models.LaunchWelcomeScreenOptions;
import com.jht.interfaces.models.PostWorkoutSurveyExtras;
import com.jht.workoutsummary.survey.PostWorkoutSurveyActivity;
import com.jhtp.dapiservice.an.types.ButtonPressType;

import org.jetbrains.annotations.NonNls;

/*
    Provides for a unified launching of "apps" and common screens.
    ALL launching of UI modules or "apps" should come through here.
    No code in here should manipulate panels or other UI subcomponents, that is the UI service's job!
    (Internal screens or tightly coupled activities do not necessarily need to go through here unless it makes sense)
 */

public class LaunchAppUtil {
    @NonNls private static final String TAG = "LaunchAppUtil";

    private LaunchAppUtil() {
        // prevent instantiation of utility class.
    }

    // ONLY for launching internal activities that have no app meta data (use launch app for "apps")
    private static void launchInternalScreen(String fullActivityName, @NonNull Intent intent) { launchInternalScreen(fullActivityName, intent, true); }
    private static void launchInternalScreen(String fullActivityName, @NonNull Intent intent, boolean newRootTask) {
        try {
            Context context = ContextUtil.getApplicationContext();
            LaunchScreenData data = new LaunchScreenData(CommonScreens.Unknown, intent, newRootTask);
            data.setStartIntent(context, fullActivityName);
            DataMatrix.putParcelable(UiServiceKeys.ACTION_LAUNCH_UI_PARCELABLE, data);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }
    public static void launchInternalScreen(String fullActivityName, @NonNls String mode) {
        Intent intent = new Intent();
        if (mode != null) {
            intent.putExtra("mode", mode);
        }
        launchInternalScreen(fullActivityName, intent);
    }
    public static void launchInternalScreen(String fullActivityName) {
        launchInternalScreen(fullActivityName,  new Intent());
    }


    public static void launchSplashScreen() {
        // since splash is usually invoked when something goes wrong, we want to be sure it can launch even if UI service is down
        // tried posting this but when it gets spammed things get weird
        try {
            Context context = ContextUtil.getApplicationContext();
            Intent intent = new Intent();
            // UI service does react to splash reset, so it will reset any panels
            // splash screen launch could be called while it is already up, so it should be prepared for resume/pause cycling
            // don't think makeMainActivity adds anything here
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK); // if sharing a stack, clear it out
            UiConfiguration uiConfiguration = UiConfiguration.getCurrent();
            // (we could make EVENT_UI_RESUMED_PARCELABLE a DATA key and check "intent?.component?.className" to determine if already resumed? though could there be a race between launch and resume?)
            String activity = uiConfiguration.getActivitySplash();
            intent.setComponent(new ComponentName(context.getPackageName(), activity));
            ConsoleEvent.logEvent("Launching Screen: " + activity);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    static public void launchWelcomeScreen() { launchWelcomeScreen(null); }
    static public void launchWelcomeScreen(LaunchWelcomeScreenOptions options) {
        Intent intent = new Intent();
        if (options != null) intent.putExtra(LaunchWelcomeScreenOptions.key, options);
        LaunchScreenData data = new LaunchScreenData(CommonScreens.Welcome, intent);
        DataMatrix.putParcelable(UiServiceKeys.ACTION_LAUNCH_UI_PARCELABLE, data);
    }

    static public void launchOutOfOrderScreen() {
        LaunchScreenData data = new LaunchScreenData(CommonScreens.OutOfOrder);
        DataMatrix.putParcelable(UiServiceKeys.ACTION_LAUNCH_UI_PARCELABLE, data);
    }

    static public void launchGoScreen() {
        LaunchScreenData data = new LaunchScreenData(CommonScreens.WorkoutSelection);
        DataMatrix.putParcelable(UiServiceKeys.ACTION_LAUNCH_UI_PARCELABLE, data);
    }

    static public void launchWorkoutScreen() {
        // this function is typically used for home button, so we don't reset the root in order to avoid flashing the screen if already up, workout start will force new
        LaunchScreenData data = new LaunchScreenData(CommonScreens.Workout, new Intent(), false);
        DataMatrix.putParcelable(UiServiceKeys.ACTION_LAUNCH_UI_PARCELABLE, data);
    }

    static public void launchMachineSetup() {
        LaunchScreenData data = new LaunchScreenData(CommonScreens.MachineSetup, new Intent(), false);
        DataMatrix.putParcelable(UiServiceKeys.ACTION_LAUNCH_UI_PARCELABLE, data);
    }

    static public void launchBluetoothPairing() {
        launchBluetoothPairing("", null);
    }
    static public void launchBluetoothPairing(@NonNull @NonNls String mode, View view) {
        //Stores device pairing button press events for Dapi Analytics
        DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.DEVICE_PAIRING_BTN_PRESS.name);
        // Stop any active background bluetooth scan, if one is running (copied from old UI)
        DataMatrix.sendEvent(DAPIKeys.ACTION_STOP_BACKGROUND_BLUETOOTH_EVENT);
        LaunchScreenData data = new LaunchScreenData(CommonScreens.DevicePairing);
        DataMatrix.putParcelable(UiServiceKeys.ACTION_LAUNCH_UI_PARCELABLE, data);
    }

    static public void launchSystemSetup() {
        launchSystemSetup(null);
    }
    static public void launchSystemSetup(@Nullable String mode) {
        LaunchScreenData data = new LaunchScreenData(CommonScreens.SystemSettings);
        if (mode != null) data.setMode(mode);
        DataMatrix.putParcelable(UiServiceKeys.ACTION_LAUNCH_UI_PARCELABLE, data);
    }

    static public void launchWifiSetup() {
        LaunchScreenData data = new LaunchScreenData(CommonScreens.SystemSettings);
        data.setMode("wifi");
        DataMatrix.putParcelable(UiServiceKeys.ACTION_LAUNCH_UI_PARCELABLE, data);
    }

    // launch either the default workout summary screen or an alternate summary screen
    static public void launchWorkoutSummary() { launchWorkoutSummary(null); }
    static public void launchWorkoutSummary(String workoutSummaryActivity) {
        launchWorkoutSummaryDestination(PostWorkoutSurveyGate.fromDataMatrix().route(workoutSummaryActivity, false));
    }
    /** Called by the post workout survey when it closes; marks the launch so the survey cannot loop */
    static public void launchWorkoutSummaryAfterSurvey(String workoutSummaryActivity) {
        launchWorkoutSummaryDestination(new WorkoutSummaryDestination.Summary(workoutSummaryActivity == null ? "" : workoutSummaryActivity, true));
    }
    private static void launchWorkoutSummaryDestination(WorkoutSummaryDestination destination) {
        boolean isSurvey = destination instanceof WorkoutSummaryDestination.Survey;
        LaunchScreenData data = new LaunchScreenData(isSurvey ? CommonScreens.PostWorkoutSurvey : CommonScreens.WorkoutSummary);
        Context context = ContextUtil.getApplicationContext();
        String activity;
        if (isSurvey) {
            String target = ((WorkoutSummaryDestination.Survey) destination).getTarget();
            data.getIntent().putExtra(PostWorkoutSurveyExtras.TARGET_ACTIVITY, target);
            activity = PostWorkoutSurveyActivity.class.getCanonicalName();
        } else {
            WorkoutSummaryDestination.Summary summary = (WorkoutSummaryDestination.Summary) destination;
            if (summary.getSurveyShown()) data.getIntent().putExtra(PostWorkoutSurveyExtras.SURVEY_SHOWN, true);
            activity = summary.getTarget();
        }
        // if override summary activity is given, preset it in the data
        if (activity != null && !activity.isEmpty() && context != null) {
            data.setStartIntent(context, activity);
        }
        DataMatrix.putParcelable(UiServiceKeys.ACTION_LAUNCH_UI_PARCELABLE, data);
    }
    // as an alternative to workout summary, some apps will either remain up and IDLE, or launch into another non-summary screen
    static public void attemptLaunchNonWorkoutSummary(String activity) {
        Context context = ContextUtil.getApplicationContext();
        if (activity != null && !activity.isEmpty() && context != null) {
            // do not clear activity stack as we may be going back to same activity (egym)
            LaunchScreenData data = new LaunchScreenData(CommonScreens.GenericIdleScreen, new Intent(), false);
            data.setStartIntent(context, activity);
            DataMatrix.putParcelable(UiServiceKeys.ACTION_LAUNCH_UI_PARCELABLE, data);
        } else {
            // if not launching any kind workout summary, we need to at least clear workout panels
            DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.IDLE);
        }
    }

    static public void launchTeamviewer() {
        // should this be treated as an app launch or just keep it as special screen?
        LaunchScreenData data = new LaunchScreenData(CommonScreens.TeamViewer);
        DataMatrix.putParcelable(UiServiceKeys.ACTION_LAUNCH_UI_PARCELABLE, data);
    }

    public static void launchAndroidLauncher() {
        // This needs to work when the UI service is down, so keeping it out of UI service for now
        try {
            Context context = ContextUtil.getApplicationContext();
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                intent.setComponent(new ComponentName("com.android.launcher3", "com.android.launcher3.uioverrides.QuickstepLauncher"));
            }
            else {
                intent.setComponent(new ComponentName("com.android.launcher3", "com.android.launcher3.Launcher"));
            }
            ConsoleEvent.logEvent("Launching Android Launcher");
            context.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "launchAndroidLauncher", e);
        }
    }

    // should we consider this a unique common screen or is this a tighter UI element?
    static public void launchLoginScreen() {
        launchInternalScreen(LoginRegisterActivity.class.getCanonicalName());
    }

    // EGym and CustomHIIT only launched from splash as alternate home screens?
    public static void launchEGym() {
        DataMatrix.putBoolean(UiServiceKeys.DATA_HOME_BUTTON_ENABLED_BOOL, true); // TODO - revisit these keys?
        Context context = ContextUtil.getApplicationContext();
        initialLaunchApp(context, AppIds.EGYM_UUID, null); // this is actually a PreWorkout app launch?
    }

    // "initial launch" registers the given app as the "starting app" before launching the app
    public static void initialLaunchApp(Context context, String appUUID, View button) {
        LaunchAppData data = new LaunchAppData(appUUID, LaunchAppCondition.PreWorkout);
        launchApp(context, data, appUUID, button);
    }
    public static void initialLaunchAppByIcon(Context context, AppInfo appInfo, View button) {
        LaunchAppData data = new LaunchAppData(appInfo.uuid, LaunchAppCondition.PreWorkout);
        launchApp(context, data, appInfo, button);
    }

    public static void launchApp(Context context, String appUUID, View button) {
        LaunchAppData data = new LaunchAppData(appUUID, LaunchAppCondition.None);
        launchApp(context, data, appUUID, button);
    }
    public static void launchAppByIcon(Context context, AppInfo appInfo, View button) {
        LaunchAppData data = new LaunchAppData(appInfo.uuid, LaunchAppCondition.None);
        launchApp(context, data, appInfo, button);
    }

    private static void launchApp(Context context, LaunchAppData data, String appId, View parent) {
        AppInfo appInfo = AppManager.getInstance().getApp(appId);
        launchApp(context, data, appInfo, parent);
    }
    private static void launchApp(Context context, LaunchAppData data, AppInfo appInfo, View parent) {
        checkPromptSso(context, appInfo, parent, ()-> DataMatrix.putParcelable(UiServiceKeys.ACTION_LAUNCH_APP_PARCELABLE, data));
    }

    /** Old UI Helper to pre-screen apps for pending SSO before proceeding forward. Newer UI uses 2-step core interface process. */
    public static void checkPromptSso(Context context, AppInfo appInfo, View parent, Runnable nextAction) {
        if (UiLauncherServiceHelperKt.appWaitingOnSso(appInfo)) {
            // Should the user facing name be part of AppInfo, or should all user facing presentation be with AppIcon?
            String appName = AppIconManager.getInstance().findAppName(appInfo.getUuid());
            String message = context.getResources().getString(R.string.sync_pg).replace("app_name", appName);
            if (parent == null) {
                Log.e(TAG, "SSO sync not done, bailing on " + appInfo.appPackage);
            } else {
                //noinspection HardCodedStringLiteral
                ProjectDialogFactory.confirm(parent, message, nextAction::run, () -> {});
            }
        } else {
            nextAction.run();
        }
    }
}
