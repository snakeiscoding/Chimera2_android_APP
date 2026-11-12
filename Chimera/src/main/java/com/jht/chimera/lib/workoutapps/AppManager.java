package com.jht.chimera.lib.workoutapps;


import static com.jht.chimera.lib.workoutapps.AppInfo.IGNORE_APP_ANALYTICS_ID;
import static com.jht.chimera.service.ui.panels.PanelBehaviorKt.PANEL_OVERRIDES_FORCE_HIDE_PAUSE_PANEL_KEY;
import static com.jht.chimera.service.ui.panels.PanelBehaviorKt.PANEL_OVERRIDES_INTENT_KEY;
import static com.jht.progression.MmProgramConstantsKt.PROGRESSION_APP_UUID;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.appstore.service.AppStoreConfigManager;
import com.jht.appstore.ui.AppStoreActivity;
import com.jht.appstore.interfaces.AppStoreAppInfo;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.ClubComKeys;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.lib.common.permission.GrantPermission;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.chimera.lib.workout.WorkoutInput;
import com.jht.chimera.service.ui.UiConfiguration;
import com.jht.chimera.service.ui.panels.ExtraBarOptions;
import com.jht.chimera.service.ui.panels.PanelBehavior;
import com.jht.chimera.service.ui.panels.PanelConfig;
import com.jht.chimera.service.ui.panels.PanelIds;
import com.jht.chimera.service.ui.panels.WorkoutPanelMode;
import com.jht.chimera.service.workout.WorkoutControllerService;
import com.jht.chimera.ui.browser.WebBrowserActivity;
import com.jht.chimera.ui.mainapp.ClubComActivity;
import com.jht.chimera.ui.mainapp.ConstantWattsActivity;
import com.jht.chimera.ui.mainapp.EGymActivity;
import com.jht.chimera.ui.mainapp.HDMIActivity;
import com.jht.chimera.ui.mainapp.HeartRateIIActivity;
import com.jht.chimera.ui.mainapp.RemoteTVActivity;
import com.jht.chimera.ui.mainapp.TVActivity;
import com.jht.chimera.ui.mainapp.TVActivityNew;
import com.jht.chimera.ui.mainapp.WebAppActivity;
import com.jht.chimera.ui.mainapp.WebAppSSOActivity;
import com.jht.chimera.ui.mymedia.MyMediaActivity;
import com.jht.clubcom.interfaces.ClubComApp;
import com.jht.clubcom.interfaces.ClubComOptions;
import com.jht.data.intent.DynamicExtraValue;
import com.jht.data.intent.IntentExtras;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.oauthlib.auth.BksCredentialProvider;
import com.jht.progression.MmActivity;
import com.jht.trackapp.TrackAppMode;
import com.jht.trackapp.TrackAppViewModelKt;
import com.jht.virtualactive.activity.VAActivity;
import com.jht.workoutcontrollerlib.workoutcardio.WorkoutSettings;
import com.jht.workoutcontrollerlib.workoutdefinition.statements.SettingValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/*
    This manages app meta data, not app icons, see AppIcon/AppIconManager for icons.
    The filtering of app available should probably be at this level to keep icon show logic purely related to icons?
 */
public class AppManager {
    private static final String TAG = "AppManager";

    public static final String TEAM_VIEWER_APP_NAME = "com.teamviewer.quicksupport.market"; // Team viewer is not treated as a traditional app
    private static final String mainAppPackage = "com.jht.chimera"; // this corresponds to entry in sso_settings.json - or use context.getPackageName()?

    // https://jhtnaee.atlassian.net/wiki/spaces/IT/pages/3916202019/Console+Auto-Login
    public static final String WORKOUT_HISTORY_URL_BASE = "matrixworkouts.jfit.co/console"; // apparently WebAppActivity has a special path for this (consider using flags instead?)

    boolean smallScreen = (ProductConfig.getConsoleType().heightPx < 768); // not sure if the vert or horz or both is the issue, so this may need adjustment as more displays are added?
    boolean isCommercial = ProductConfig.isCommercial();
    boolean isRetail = ProductConfig.isRetail();
    boolean isLightCommercial = ProductConfig.isLightCommercial();
    boolean hasTV = BuildConfig.HAS_TV;
    boolean hasHDMI = ProductConfig.hasHDMIInput();

    //region panels
    private static final PanelConfig[] WorkoutNoBottomPanels = new PanelConfig[]{
            new PanelConfig(PanelIds.TOP_PANEL),
            new PanelConfig(PanelIds.LEFT_PANEL), new PanelConfig(PanelIds.RIGHT_PANEL)};
    private static final PanelConfig[] BorderPanels = new PanelConfig[]{
            new PanelConfig(PanelIds.BORDER_LEFT_PANEL), new PanelConfig(PanelIds.BORDER_RIGHT_PANEL),
            new PanelConfig(PanelIds.TOP_PANEL)};
    private static final PanelConfig[] TopOnlyPanels = new PanelConfig[]{
            new PanelConfig(PanelIds.TOP_PANEL)};
    private static final PanelConfig[] LandmarkFullPanels = new PanelConfig[]{
            new PanelConfig(PanelIds.TOP_PANEL, WorkoutPanelMode.OVERLAY),
            new PanelConfig(PanelIds.LEFT_PANEL, WorkoutPanelMode.OVERLAY),
            new PanelConfig(PanelIds.RIGHT_PANEL, WorkoutPanelMode.OVERLAY)};

    private static final PanelBehavior SimplePanelBehavior = new PanelBehavior(null, WorkoutNoBottomPanels, null, null);
    private static final PanelBehavior LandmarkPanelBehavior = new PanelBehavior(null, PanelBehavior.getNormalRegularPanels(), LandmarkFullPanels, null);
    private static final PanelBehavior YmcaPanelBehavior = new PanelBehavior(
            BorderPanels,
            PanelBehavior.getNormalRegularPanels(),
            new PanelConfig[]{new PanelConfig(PanelIds.FLOATING_MINIMIZE_PANEL)},
            null
    );

    // Old ifit needs to maintain aspect ratio, so we add borders
    private static final PanelBehavior IfitPanelBehavior = new PanelBehavior(
            BorderPanels,
            PanelBehavior.getNormalRegularPanels(),
            new PanelConfig[]{new PanelConfig(PanelIds.FLOATING_MINIMIZE_PANEL)},
            BorderPanels, // ifit uses its own summary page, so just go back to borders
            new ExtraBarOptions(false, false, false, false, false) // ifit needs the bottom to be spacer with no stats
    );
    // New ifit does not require a particular aspect, so does not need letterbox borders
    private static final PanelBehavior Ifit3PanelBehavior = new PanelBehavior(
            TopOnlyPanels,
            WorkoutNoBottomPanels,
            new PanelConfig[]{new PanelConfig(PanelIds.FLOATING_MINIMIZE_PANEL)},
            TopOnlyPanels,
            new ExtraBarOptions(false, false, false, false, false) // ifit3 does NOT need the bottom to be spacer
    );
    // AUTO HIDE COMPOSE POC UI PANELS
    private static final PanelConfig[] PocRegularPanels = new PanelConfig[]{
            new PanelConfig(PanelIds.BOTTOM_PANEL2, WorkoutPanelMode.OVERLAY),
            new PanelConfig(PanelIds.TOP_PANEL2, WorkoutPanelMode.OVERLAY),
    };
    public static final PanelBehavior PocNormalPanelBehavior = new PanelBehavior(null, PocRegularPanels, PocRegularPanels, null); // fullscreen is no longer null but regular panels
    private static final PanelBehavior PocNormalExtraPanelBehavior = new PanelBehavior(null, PocRegularPanels, PocRegularPanels, null); // NewExtraFullPanels replaced by just the regular panels
    private static final PanelBehavior PocWebBehavior = PocNormalExtraPanelBehavior.copy(PanelBehavior.getWebExtraBar());
    private static final PanelBehavior PocBackBehavior = PocNormalExtraPanelBehavior.copy(PanelBehavior.getBackExtraBar());
    private static final PanelBehavior PocRefreshBehavior = PocNormalExtraPanelBehavior.copy(PanelBehavior.getRefreshExtraBar());
    //endregion panels


    // A list of apps with their UUID
    private final HashMap<String, AppInfo> mAllApps = new HashMap<>();

    private static class SingletonHolder {
        private static final AppManager Instance = new AppManager();
    }

    public static AppManager getInstance() {
        return AppManager.SingletonHolder.Instance;
    }

    private AppManager() {
        initApps();
    }

    public void reset() {
        initApps();
    }

    private void add(AppInfo info) {
        if (mAllApps.containsKey(info.uuid)) {
            // ignore duplicate but log an error
            for (AppInfo app : mAllApps.values()) Log.d(TAG, app.uuid + " " + app.getAppPackage() + " " + app.getAppActivity());
            Log.e(TAG, "DUPLICATE APP INFO KEY " + info.uuid);
        } else {
            mAllApps.put(info.uuid, info);
        }
    }

    public @Nullable AppInfo getApp(@Nullable String uuid) {
        return mAllApps.get(uuid);
    }

    public AppInfo getAppFromPackage(String packageName) {
        if (mainAppPackage.equals(packageName)) return null; // this package is not an individual app
        for (AppInfo appInfo : mAllApps.values()) {
            if (appInfo.appPackage.equalsIgnoreCase(packageName)) return appInfo;
        }
        Log.e(TAG, "Could not find icon from package name " + packageName);
        return null;
    }


    // maybe try to slim down code that needs to only run if app is found
    public interface IAppInfoAction {
        void doForApp(AppInfo info);
    }
    public void doForApp(String uuid, IAppInfoAction action) {
        AppInfo info = getApp(uuid);
        if (info != null) action.doForApp(info);
    }

    public static void grantPermissions(AppInfo app) {
        // or does it use less memory to use a settable callback on AppIcon than having a string[] ?
        Context context = ContextUtil.getApplicationContext();
        if (app != null && app.permissions != null) {
            for (String perm : app.permissions) {
                GrantPermission.grant(context, app.appPackage, perm);
            }
        }
    }
    /**
     * Enable the app with 'pm enable'
     *
     * @param app
     */
    public static void enableApp(AppInfo app) {
        if (app == null) return;

        if (!shouldPackageBeToggled(app.appPackage)) return; // make sure conditions match up with the disabling code !

        // we would cache the state of if an app is disabled/enabled if we wanted to do more optimization and not make this call all the time
        // NOTE: We cannot use the "pm enable" command here since it doesn't have additional parameters to not kill the process if
        // an app is being installed. When the AppStore installs an app, Android will kill processes that has changed via 'pm enable'.
        // A one second delay does not help since the change is sent via broadcast so there is a delay with the processing of this broadcast.
        try {
            PackageManager packageManager = ContextUtil.getApplicationContext().getPackageManager();
            int state = packageManager.getApplicationEnabledSetting(app.appPackage);
            // skip if already enabled (there appear to be 2 states where it means the app is enabled)
            if (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT &&
                    packageManager.getApplicationInfo(app.appPackage, 0).enabled) return;
            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) return;
            packageManager.setApplicationEnabledSetting(app.appPackage, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        } catch (Exception e) {
            Log.e(TAG, "enableApp exception", e);
        }
    }

    /** Returns true if given package name should be disabled/enabled as needed */
    public static boolean shouldPackageBeToggled(String packageName) {
        // assume we are not disabling our apps (ie starts with com.jht or we could alternately we just ignore com.jht.chimera)
        return !packageName.contains("com.jht.");
    }

    /*public static String getCurrentApp() { // moved from team viewer panel
        Context context = ContextUtil.getApplicationContext();
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        // Query events for that occurred over a certain period of time
        long timeBackMs = 1000 * 3600; // 1 hour
        long time = System.currentTimeMillis();
        List<UsageStats> queryUsageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - timeBackMs, time);

        // Find the event that occurred last, that's the app that's visible. If no events occurred return null
        long latestTime = 0;
        if (queryUsageStats.size()>0) {
            latestTime = queryUsageStats.get(0).getLastTimeStamp();
        }
        String currentApp = null;
        for (int i = 0; i < queryUsageStats.size(); i++) {
            UsageStats stats = queryUsageStats.get(i);
            if (stats.getLastTimeUsed() > latestTime) {
                latestTime = stats.getLastTimeStamp();
                currentApp = stats.getPackageName();
            }
        }
        return currentApp;
    }*/

    /** Setup activity to run LG750E app */
    private AppInfo setupStbStreamApp(Context context, @Nullable String appKeyword, String appUUID, int AnalyticsId) {
        // compose video input seems to be working now, note it does enforce aspect ratio so app extra bar will throw it off!
        String streamingAppActivity = "com.jht.streamingapps.StreamingAppsActivity";
        AppInfo streamApps = new AppInfo(context.getPackageName(), streamingAppActivity, appUUID, AnalyticsId);
        streamApps.category = AppInfo.Category.Entertainment;
        streamApps.audioInput = AudioSourceInput.HDMI;
        streamApps.urlExtra = appKeyword;
        //streamApps.specialPanelBehavior = backBehavior;
        add(streamApps);
        return streamApps;
    }

    private void addDynamicTpaPackages(List<String> list) {
        // CGC-20324 workaround this being dynamic, need to review our options
        if (!list.contains("com.netflix.mediaclient")) list.add("com.netflix.mediaclient");
    }

    /** Return list of all third party app (TPA) packages */
    public List<String> getTpaPackages() {
        // filter by category or just check if in our package?
        Context context = ContextUtil.getApplicationContext();
        if (context == null) {
            Log.e(TAG, "Error getting context! Returning empty TPA list");
            return Collections.emptyList();
        }
        String thisPackage = context.getPackageName();
        List<String> list = new ArrayList<>();
        for (AppInfo appInfo : mAllApps.values()) {
            if (!appInfo.appPackage.equals(thisPackage) && !list.contains(appInfo.appPackage)) list.add(appInfo.appPackage);
        }
        addDynamicTpaPackages(list);
        return list;
    }

    /** Return list of apk packages (used for version lists) */
    public List<String> getApkPackages() {
        List<String> list = new ArrayList<>();
        // our apk
        list.add("com.jht.chimera");
        list.add("com.jht.updateservice");
        // TPA
        List<String> tpaPackages = getTpaPackages();
        list.addAll(tpaPackages);
        addDynamicTpaPackages(list);
        // third party services not managed by this manager
        list.add("com.myzone.myzoneble.console_service");
        /* CGC-20971
        list.add("com.teamviewer.quicksupport.addon.aosp4");
        list.add("com.teamviewer.quicksupport.market");*/
        // keyboard apk
        list.add("com.android.inputmethod.pinyin");
        list.add("com.jht.inputmethod.pinyin");
        list.add("com.jht.googlecode.tcime.unofficial");
        list.add("com.android.inputmethod.latin");
        list.add("jp.co.omronsoft.openwnn");
        list.add("com.johnsonfit.jp.co.omronsoft.openwnn");
        list.add("org.kandroid.app.hangulkeyboard");
        list.add("helium314.keyboard");
        return list;
    }

    private void initApps() {
        Context context = ContextUtil.getApplicationContext();
        if (context == null) {
            Log.e(TAG, "Error getting context!");
            return;
        }

        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        mAllApps.clear();

        // TODO - should only add workout apps that are in the workout map? use addWorkout() to handle the checking?

        final UiConfiguration uiConfig = UiConfiguration.getCurrent();
        final boolean useAutoHide = ProductConfig.hasPremiumUi() && DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_AUTO_HIDE_PANELS_BOOL);
        final PanelBehavior webBehavior = useAutoHide ? PocWebBehavior : PanelBehavior.getNormalPanelWebBehavior();
        final PanelBehavior backBehavior = useAutoHide ? PocBackBehavior : PanelBehavior.getNormalPanelBackBehavior();
        final PanelBehavior refreshBehavior = useAutoHide ? PocRefreshBehavior : PanelBehavior.getNormalPanelRefreshBehavior();
        final boolean useStbStreamApps = DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_STB_STREAM_APPS_BOOL);
        final PanelBehavior quickKeyBehavior = PanelBehavior.getNormalPanelBehavior().copy(new ExtraBarOptions(false, false, false, true, true));

        AppInfo egym = new AppInfo(context.getPackageName(), EGymActivity.class.getCanonicalName(), AppIds.EGYM_UUID, 38);
        egym.launchesFullscreen = true;
        egym.bypassWorkoutUiProcess = true;
        egym.workoutEndAction = AppInfo.WorkoutEndAction.NoWorkoutSummary;
        egym.workoutEndActivity = EGymActivity.class.getCanonicalName();
        egym.workoutEndResetSafetyPin = false;
        egym.audioInput = AudioSourceInput.Default;
        egym.suppressCoolDownControls = true;
        egym.workoutEndAction = AppInfo.WorkoutEndAction.NoWorkoutSummary;
        //egym.uiEventName = UiEventKeys.EVENT.EGYM_LOADING.name(); // EGYM handles its own LCD grips
        add(egym);


        // Clubbed sprint8 and HIIT under single condition check for frame type and then add app
        if (frameConfig.machineClassType != JHTCommonTypes.MachineClassificationType.Climbmill) {
            AppInfo sprint8 = new AppInfo(context.getPackageName(), uiConfig.getActivitySprint8(), AppIds.SPRINT8_UUID, 39);
            sprint8.workoutEndActivity = uiConfig.getActivitySprint8End();
            sprint8.audioInput = AudioSourceInput.Foreground;
            add(sprint8);

            AppInfo customHIIT = new AppInfo(context.getPackageName(), uiConfig.getActivityCustomHIIT(), AppIds.HIIT_APP_UUID, 3);
            customHIIT.specialPanelBehavior = quickKeyBehavior;
            add(customHIIT);
        }

        if (frameConfig.TM_SUPPORTS_REVERSE_BELT) {
            AppInfo hybridTdSwitchStride = new AppInfo(context.getPackageName(), uiConfig.getActivitySwitchStride(), AppIds.HYBRID_TD_SWITCH_STRIDE_UUID, 107);
            hybridTdSwitchStride.specialPanelBehavior = SimplePanelBehavior;
            hybridTdSwitchStride.suppressCoolDownControls = true;
            hybridTdSwitchStride.startExtras = IntentExtras.of(
                IntentExtras.bundle(PANEL_OVERRIDES_INTENT_KEY, IntentExtras.bool(PANEL_OVERRIDES_FORCE_HIDE_PAUSE_PANEL_KEY, true))
            );
            add(hybridTdSwitchStride);
        }

        AppInfo landmarks = new AppInfo(context.getPackageName(), uiConfig.getActivityLandmarks(), AppIds.LANDMARKS_UUID, 40);
        landmarks.specialPanelBehavior = ProductConfig.hasPremiumUi() ? null : LandmarkPanelBehavior;
        add(landmarks);

        AppInfo targetHr = new AppInfo(context.getPackageName(), HeartRateIIActivity.class.getCanonicalName(), AppIds.TARGET_HEART_RATE_UUID, 15);
        add(targetHr);

        AppInfo constantWatts = new AppInfo(context.getPackageName(), ConstantWattsActivity.class.getCanonicalName(), AppIds.CONSTANT_WATTS_UUID, 41);
        add(constantWatts);

        AppInfo targetTraining = new AppInfo(context.getPackageName(),uiConfig.getActivityTargetTraining(), AppIds.TARGET_TRAINING, 79);
        add(targetTraining);

        AppInfo progressionApp = new AppInfo(context.getPackageName(), MmActivity.class.getCanonicalName(), PROGRESSION_APP_UUID, 68);
        progressionApp.workoutEndActivity = MmActivity.class.getCanonicalName();
        progressionApp.workoutEndResetSafetyPin = false;
        progressionApp.category = AppInfo.Category.Workout;
        progressionApp.uiEventName = UiEventKeys.EVENT.IFIT_LOADING.name(); // CGC-22767: Should use same toggles as ifit
        add(progressionApp);

        AppInfo simpleApp = new AppInfo(context.getPackageName(), uiConfig.getActivitySimpleApp(), AppIds.SIMPLE_UUID, 2);
        simpleApp.specialPanelBehavior = useAutoHide ? null : SimplePanelBehavior;
        add(simpleApp);

//        AppInfo seca = new AppInfo(context.getPackageName(), SecaActivity.class.getCanonicalName(), AppIds.SECA_UUID, 106);
//        seca.workoutEndActivity = SecaActivity.class.getCanonicalName();
//        seca.workoutEndResetSafetyPin = false;
//        seca.category = AppInfo.Category.Workout;
//        add(seca);

        // CPAT is really just simple app in full screen, but it sounds like blocking regular panels might not be a requirement, so maybe we can drop this?
        /*AppInfo cpat = new AppInfo(context.getPackageName(), simpleAppActivity, AppIds.CPAT_UUID, 2);
        cpat.specialPanelBehavior = SimplePanelBehavior;
        cpat.launchesFullscreen = true;
        add(cpat);*/

        AppInfo bikePace = new AppInfo(context.getPackageName(), "com.jht.chimera.ui.mainapp.BikePaceActivity", AppIds.PACE_UUID, 46);
        add(bikePace);

        AppInfo intervals = new AppInfo(context.getPackageName(), "com.jht.chimera.ui.mainapp.BikeIntervalsActivity", AppIds.INTERVALS_UUID, 47);
        add(intervals);

        AppInfo ftp = new AppInfo(context.getPackageName(), "com.jht.chimera.ui.mainapp.FTPTestActivity", AppIds.FTP_UUID, 48);
        add(ftp);

        AppInfo maxHr = new AppInfo(context.getPackageName(), "com.jht.chimera.ui.mainapp.MaxHRTestActivity", AppIds.MAX_HR_TEST_UUID, 49);
        add(maxHr);

        AppInfo spinBikeIntervals = new AppInfo(context.getPackageName(), "com.jht.spinbikeapps.intervals.SpinBikeIntervalsActivity", AppIds.SPIN_BIKE_INTERVALS_UUID, 108);
        spinBikeIntervals.suppressCoolDownControls = true;
        add(spinBikeIntervals);

        // Mfit is just target HR, does it need its own entry ?
        if (frameConfig.machineClassType != JHTCommonTypes.MachineClassificationType.Climbmill && frameConfig.machineClassType != JHTCommonTypes.MachineClassificationType.Stepper) {
            if (DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_MILE_HIGH_RUN_CLUB_BOOL) && frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                AppInfo mileHigh = new AppInfo(context.getPackageName(), uiConfig.getActivityTrackApp(), AppIds.MILE_HIGH_UUID, 62);
                mileHigh.startExtras = IntentExtras.of(
                    IntentExtras.string(TrackAppViewModelKt.TrackAppModeKey, TrackAppMode.MileHighClub.name())
                );
                add(mileHigh);
            }
        }

        if (frameConfig.machineClassType != JHTCommonTypes.MachineClassificationType.Climbmill && frameConfig.machineClassType != JHTCommonTypes.MachineClassificationType.Stepper) {
            // A number of fitness tests are associated with TRACK_UUID
            AppInfo track = new AppInfo(context.getPackageName(), uiConfig.getActivityTrackApp(), AppIds.TRACK_UUID, 14);
            add(track);
        }

        AppInfo vaApp = new AppInfo(context.getPackageName(), VAActivity.class.getCanonicalName(), AppIds.VA_UUID, 1);
        vaApp.audioInput = AudioSourceInput.ForegroundLite;
        add(vaApp);

        if (ProductConfig.isRetail()) { // CGC-23466 remove MyMedia from everything but retail
            // MyMedia app (needed for marketing photo/video shoots)
            AppInfo myMedia = new AppInfo(context.getPackageName(), MyMediaActivity.class.getCanonicalName(), AppIds.MY_MEDIA_UUID, 7);
            myMedia.category = AppInfo.Category.Entertainment;
            myMedia.audioInput = AudioSourceInput.Default;
            add(myMedia);
        }

        AppInfo facebook = new AppInfo(context.getPackageName(), WebAppSSOActivity.class.getCanonicalName(), AppIds.FACEBOOK_UUID, 9);
        facebook.category = AppInfo.Category.Entertainment;
        if (smallScreen) {
            facebook.urlExtra = "https://m.facebook.com"; // use mobile site for 10" parker
        } else {
            // without a user agent this seems to redirect to the mobile site anyway
            //facebook.userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.132 Safari/537.36";
            //facebook.userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/11.1.1 Safari/605.1.15";
            facebook.userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.66 Safari/537.36";
            facebook.urlExtra = "https://facebook.com";
        }
        facebook.specialPanelBehavior = refreshBehavior;
        facebook.supportsSSO = true;
        facebook.webAppDomainInclude = "facebook.com";
        facebook.audioInput = AudioSourceInput.Foreground;
        add(facebook);

        if (isRetail) {
            AppInfo treo = new AppInfo(context.getPackageName(), WebAppSSOActivity.class.getCanonicalName(), AppIds.TREO_UUID, 69);
            treo.category = AppInfo.Category.Entertainment;
            treo.urlExtra = DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_TREO_URL_STRING);
            treo.specialPanelBehavior = refreshBehavior;
            treo.supportsSSO = true;
            treo.audioInput = AudioSourceInput.Foreground;
            treo.allowedCountyCodes = new String[] {"US", "CA", "MX",};
            treo.allowedWeatherLocations = new String[] {"USA", "Canada", "Mexico"};
            add(treo);
        }

        AppInfo customApp = new AppInfo(context.getPackageName(), WebAppSSOActivity.class.getCanonicalName(), AppIds.CUSTOM_APP_UUID, 66);
        customApp.urlExtra = DataMatrix.getString(SettingKeys.AssetManagement.DATA_CUSTOM_WEB_APP_URL_STRING);
        if(!customApp.urlExtra.startsWith("http://") && !customApp.urlExtra.startsWith("https://")){
            customApp.urlExtra = "http://" + customApp.urlExtra;
        }
        customApp.supportsSSO = true;
        customApp.specialPanelBehavior = webBehavior;
        customApp.audioInput = AudioSourceInput.Foreground;
        if(!DataMatrix.getString(SettingKeys.AssetManagement.DATA_CUSTOM_WEB_APP_NAME_STRING).isEmpty() &&
                !DataMatrix.getString(SettingKeys.AssetManagement.DATA_CUSTOM_WEB_APP_URL_STRING).isEmpty()) {
            add(customApp);
        }

        AppInfo x = new AppInfo(context.getPackageName(), WebAppSSOActivity.class.getCanonicalName(), AppIds.X_UUID, 10);
        x.category = AppInfo.Category.Entertainment;
        x.urlExtra = "https://m.x.com";
        x.supportsSSO = true;
        x.specialPanelBehavior = refreshBehavior;
        x.webAppDomainInclude = "twitter.com;x.com";
        x.audioInput = AudioSourceInput.Foreground;
        add(x);

        AppInfo myFitnessPal = new AppInfo(context.getPackageName(), isRetail ? WebAppSSOActivity.class.getCanonicalName() : WebAppActivity.class.getCanonicalName(), AppIds.MY_FITNESS_PAL_UUID, 12);
        myFitnessPal.category = AppInfo.Category.Entertainment;
        myFitnessPal.urlExtra = "myfitnesspal.jfit.co";
        myFitnessPal.supportsSSO = isRetail;
        //myFitnessPal.clearDataOnKill = true;
        myFitnessPal.specialPanelBehavior = refreshBehavior;
        add(myFitnessPal);

        AppInfo youtube = new AppInfo(context.getPackageName(), WebAppSSOActivity.class.getCanonicalName(), AppIds.YOUTUBE_UUID, 8);
        youtube.urlExtra = "https://m.youtube.com";
        youtube.supportsSSO = true;
        youtube.specialPanelBehavior = webBehavior;
        youtube.category = AppInfo.Category.Entertainment;
        //youtube.clearDataOnKill = true;
        youtube.audioInput = AudioSourceInput.Foreground;
        add(youtube);

        AppInfo weather;
        SettingKeys.WeatherApplication weatherApplication = SettingKeys.WeatherApplication.values()[DataMatrix.getInt(SettingKeys.Application.DATA_WEATHER_APP_ENUM_INT)];
        switch (weatherApplication) {
            case MATRIX_WEB_APP:
            default:
                weather = new AppInfo(context.getPackageName(), WebAppActivity.class.getCanonicalName(), AppIds.WEATHER_UUID, 11);
                weather.specialPanelBehavior = refreshBehavior;
                weather.urlExtra = "weather.jfit.co";
                break;
            case MY_WEATHER_APP:
                weather = new AppInfo("com.clover.myweather", "", AppIds.WEATHER_UUID, 11);
                weather.specialPanelBehavior = backBehavior;
                break;
            case MOJI_WEATHER_APP:
                weather = new AppInfo("com.moji.mjweather", "", AppIds.WEATHER_UUID, 11);
                weather.specialPanelBehavior = backBehavior;
                break;
        }
        weather.category = AppInfo.Category.System;
        //weather.clearDataOnKill = true;
        add(weather);

        if (hasHDMI) {
            AppInfo hdmi = new AppInfo(context.getPackageName(), HDMIActivity.class.getCanonicalName(), AppIds.HDMI_UUID, 50);
            hdmi.category = AppInfo.Category.Entertainment;
            //hdmi.clearDataOnKill = false;
            hdmi.audioInput = AudioSourceInput.HDMI;
            add(hdmi);
        }

        if (hasTV && !frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.SpinBike)) {
            SettingKeys.TVSource source = SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING));

            AppInfo tv = new AppInfo(context.getPackageName(), source.useNewLib() ? TVActivityNew.class.getCanonicalName() : TVActivity.class.getCanonicalName(), AppIds.TV_UUID, 4);
            tv.category = AppInfo.Category.Entertainment;
            //tv.clearDataOnKill = false;
            tv.audioInput = AudioSourceInput.Default;
            tv.tunerDependent = true;
            add(tv);

            if (DataMatrix.getBoolean(SettingKeys.TV.DATA_TV_REMOTE_TV_BOOL)) {
                AppInfo remoteTv = new AppInfo(context.getPackageName(), RemoteTVActivity.class.getCanonicalName(), AppIds.REMOTE_TV_UUID, 29);
                remoteTv.category = AppInfo.Category.Entertainment;
                //remoteTv.clearDataOnKill = false;
                remoteTv.audioInput = AudioSourceInput.RemoteTV;
                add(remoteTv);
            }
        }

        AppInfo webBrowser = new AppInfo(context.getPackageName(), WebBrowserActivity.class.getCanonicalName(), AppIds.WEB_BROWSER_UUID, 13);
        webBrowser.category = AppInfo.Category.Entertainment;
        webBrowser.audioInput = AudioSourceInput.Foreground;
        webBrowser.enabledByDefault = isRetail; // web browser is on by default for retail but not commercial
        add(webBrowser);

        if (!isRetail) {
            AppInfo hulu = new AppInfo(context.getPackageName(), WebAppSSOActivity.class.getCanonicalName(), AppIds.HULU_UUID, 16);
            hulu.category = AppInfo.Category.Entertainment;
            hulu.urlExtra = "https://www.hulu.com";
            // if hulu web app sees android, it will direct to their app
            hulu.userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.66 Safari/537.36";
            hulu.specialPanelBehavior = webBehavior;
            hulu.supportsSSO = true;
            hulu.webAppDomainInclude = "hulu.com";
            hulu.audioInput = AudioSourceInput.Foreground;
            hulu.enabledByDefault = false;
            if (isCommercial) {
                hulu.experimentalKey = SettingKeys.Application.DATA_HULU_PLUS_ENABLED_BOOL.name();
            }
            add(hulu);
        }

        // Currently include this app in the list, but conditionally show the button for it
        setupStbStreamApp(context, "casting", AppIds.CASTING_UUID, 0);

        if (useStbStreamApps) {
            AppInfo netflixStb = setupStbStreamApp(context, "netflix", AppIds.NETFLIX_UUID, 17);
            netflixStb.tunerDependent = true;
        } else {
            AppInfo netflix = new AppInfo("com.netflix.mediaclient", "", AppIds.NETFLIX_UUID, 17);
            netflix.category = AppInfo.Category.Entertainment;
            netflix.specialPanelBehavior = backBehavior;
            netflix.supportsSSO = true;
            netflix.audioInput = AudioSourceInput.Foreground;
            add(netflix);
        }

        if (!isRetail) {
            AppInfo disney = new AppInfo("com.disney.disneyplus", "", AppIds.DISNEY_UUID, 67);
            disney.category = AppInfo.Category.Entertainment;
            disney.specialPanelBehavior = backBehavior;
            disney.supportsSSO = true;
            disney.audioInput = AudioSourceInput.Foreground;
            disney.experimentalKey = SettingKeys.Application.DATA_DISNEY_PLUS_ENABLED_BOOL.name();
            add(disney);
        }

        AppInfo flipboard = new AppInfo("flipboard.app", "", AppIds.FLIPBOARD_UUID, 21);
        flipboard.category = AppInfo.Category.Entertainment;
        flipboard.specialPanelBehavior = backBehavior;
        flipboard.supportsSSO = true;
        flipboard.audioInput = AudioSourceInput.Foreground;
        add(flipboard);

        AppInfo pressReader;
        if (smallScreen) {
            pressReader = new AppInfo(context.getPackageName(), WebAppSSOActivity.class.getCanonicalName(), AppIds.PRESS_READER_UUID, 30);
            pressReader.urlExtra = "https://www.pressreader.com";
            pressReader.webAppDomainInclude = "pressreader.com";
            pressReader.specialPanelBehavior = refreshBehavior;
        } else {
            pressReader = new AppInfo("com.newspaperdirect.pressreader.android", "", AppIds.PRESS_READER_UUID, 30);
            pressReader.specialPanelBehavior = backBehavior;
        }
        pressReader.category = AppInfo.Category.Entertainment;
        pressReader.supportsSSO = true;
        pressReader.audioInput = AudioSourceInput.Foreground;
        add(pressReader);

        /* Disable the Matrix solitaire since it will be available via AppStore
        AppInfo solitaire = new AppInfo("com.jht.solitaire", "", AppIds.SOLITAIRE_UUID, 51);
        solitaire.category = AppInfo.Category.Entertainment;
        solitaire.specialPanelBehavior = backBehavior;
        add(solitaire);
        */

        AppInfo thirdPartySolitaire = new AppInfo("com.rikkigames.solsuite", "", AppIds.TPA_SOLITAIRE_UUID, 57);
        thirdPartySolitaire.category = AppInfo.Category.Entertainment;
        thirdPartySolitaire.specialPanelBehavior = backBehavior;
        add(thirdPartySolitaire);

        if (smallScreen) {
            // Parker 10" kindle experience not good, do not include
            Log.w(TAG, "Kindle blocked due to small screen issues");
        } else {
            AppInfo kindle = new AppInfo("com.amazon.kindle", "", AppIds.KINDLE_UUID, 22);
            kindle.category = AppInfo.Category.Entertainment;
            kindle.specialPanelBehavior = backBehavior;
            kindle.supportsSSO = true;
            kindle.permissions = new String[]{ // permissions from app manifest, prevents android.content.pm.action.REQUEST_PERMISSIONS not found on vortex
                    "android.permission.READ_PHONE_STATE", "com.amazon.dcp.metrics.permission.METRICS_PERMISSION",
                    "com.amazon.kindle.permission.RECEIVE_ADM_MESSAGE", "android.permission.ACCESS_FINE_LOCATION",
                    "com.amazon.device.messaging.permission.RECEIVE", "android.permission.ACCESS_COARSE_LOCATION",
                    "android.permission.WAKE_LOCK", "android.permission.BLUETOOTH_SCAN",
                    "com.amazon.dcp.messaging.permission.HANDLE_DEVICE_MESSAGE", "android.permission.BLUETOOTH_ADVERTISE",
                    "com.amazon.STORE_ACCESS", "com.google.android.gms.permission.AD_ID",
                    "android.permission.ACCESS_NETWORK_STATE", "android.permission.CHANGE_WIFI_STATE",
                    "android.permission.ACCESS_WIFI_STATE", "com.amazon.kindle.cms.CMS_ACCESS",
                    "android.permission.INTERNET", "android.permission.RECEIVE_BOOT_COMPLETED",
                    "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.BLUETOOTH_ADMIN",
                    "android.permission.AUTHENTICATE_ACCOUNTS", "com.amazon.whisperjoin.deviceprovisioningservice.AllowListPolicyUpdate.Permission",
                    "android.permission.MANAGE_ACCOUNTS", "com.amazon.whisperjoin.deviceprovisioningservice.ArcusUpdate.Permission",
                    "android.permission.USE_CREDENTIALS", "com.amazon.whisperjoin.deviceprovisioningservice.FFSServiceAction.permission",
                    "com.amazon.dcp.sso.permission.AMAZON_ACCOUNT_PROPERTY", "com.google.android.c2dm.permission.RECEIVE",
                    "com.amazon.dcp.sso.permission.account.changed", "android.permission.CHANGE_WIFI_MULTICAST_STATE",
                    "com.amazon.identity.permission.CAN_CALL_MAP_INFORMATION_PROVIDER", "com.android.vending.BILLING",
                    "com.samsung.android.providers.context.permission.WRITE_USE_APP_FEATURE_SURVEY", "android.permission.REORDER_TASKS",
                    "android.permission.POST_NOTIFICATIONS", "com.amazon.kindle.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
                    "android.permission.READ_EXTERNAL_STORAGE", "android.permission.SEND_DOWNLOAD_COMPLETED_INTENTS",
                    "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK", "android.permission.DOWNLOAD_WITHOUT_NOTIFICATION",
                    "com.amazon.kindle.socialsharing.PERMISSION_SHARE", "com.amazon.dcp.config.permission.DYN_CONFIG_VALUES_UPDATED",
                    "com.amazon.kindle.otter.settings.RECEIVE_GOODREADS_UNLINK", "com.amazon.identity.auth.device.perm.AUTH_SDK",
                    "android.permission.FOREGROUND_SERVICE", "com.amazon.identity.permission.GENERIC_IPC",
                    "android.permission.FOREGROUND_SERVICE_DATA_SYNC", "com.amazon.dcp.sso.permission.USE_DEVICE_CREDENTIALS",
                    "android.permission.GET_TASKS", "com.amazon.dcp.sso.permission.MANAGE_COR_PFM",
                    "android.permission.BROADCAST_STICKY", "com.amazon.dcp.sso.permission.CUSTOMER_ATTRIBUTE_SERVICE",
                    "com.audible.hushpuppy.tablet.permission.COMPANION_MAPPING", "com.amazon.dcp.sso.permission.AmazonAccountPropertyService.property.changed",
                    "android.permission.BLUETOOTH", "com.amazon.identity.permission.CALL_AMAZON_DEVICE_INFORMATION_PROVIDER",
                    "android.permission.READ_BASIC_PHONE_STATE", "com.amazon.appmanager.preload.permission.READ_PRELOAD_DEVICE_INFO_PROVIDER",
                    "amazon.permission.COLLECT_METRICS", "android.permission.CREDENTIAL_MANAGER_QUERY_CANDIDATE_CREDENTIALS",
                    "android.permission.BLUETOOTH_CONNECT", "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
            };
            add(kindle);
        }

        AppInfo instagram = new AppInfo(context.getPackageName(), WebAppSSOActivity.class.getCanonicalName(), AppIds.INSTAGRAM_UUID, 31);
        instagram.category = AppInfo.Category.Entertainment;
        instagram.urlExtra = "https://instagram.com";
        instagram.supportsSSO = true;
        instagram.specialPanelBehavior = refreshBehavior;
        instagram.webAppDomainInclude = "instagram.com;facebook.com/";
        instagram.audioInput = AudioSourceInput.Foreground;
        add(instagram);

        AppInfo spotify = new AppInfo("com.spotify.music", "", AppIds.SPOTIFY_UUID, 20);
        spotify.category = AppInfo.Category.Entertainment;
        spotify.specialPanelBehavior = backBehavior;
        spotify.supportsSSO = true;
        spotify.audioInput = AudioSourceInput.Foreground;
        add(spotify);

        // CGC-14771 Remove support for WeChat on P1
        if (!ProductConfig.isProjectOne()) {
            AppInfo wechat = new AppInfo("com.tencent.mm", "", AppIds.WECHAT_UUID, 34);
            wechat.category = AppInfo.Category.Entertainment;
            wechat.specialPanelBehavior = backBehavior;
            wechat.supportsSSO = true;
            wechat.enabledByDefault = false; //Always default off now
            wechat.audioInput = AudioSourceInput.Foreground;
            wechat.permissions = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE,
            };
            add(wechat);
        }

        if (isCommercial || isLightCommercial) {
            AppInfo facilityCalendar = new AppInfo(context.getPackageName(), WebAppActivity.class.getCanonicalName(), AppIds.FACILITY_CALENDAR_UUID, 5);
            facilityCalendar.category = AppInfo.Category.System;
            facilityCalendar.urlExtra = "facilitycalendar.jfit.co";
            facilityCalendar.specialPanelBehavior = refreshBehavior;
            add(facilityCalendar);
        }

        if (isCommercial || isLightCommercial) {
            AppInfo workoutHistory = new AppInfo(context.getPackageName(), WebAppActivity.class.getCanonicalName(), AppIds.WORKOUT_HISTORY_UUID, 32);
            workoutHistory.category = AppInfo.Category.System;
            workoutHistory.specialPanelBehavior = refreshBehavior;
            add(workoutHistory);

            if (DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_DAPI_FALLBACK_BOOL)) {
                // https://jhtnaee.atlassian.net/wiki/spaces/IT/pages/3916202019/Console+Auto-Login
                String uuid = DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING);
                String xid = DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_USER_XID_STRING);
                String session = DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_USER_TOKEN_SESSION_STRING);
                BksCredentialProvider credentialProvider = new BksCredentialProvider(context.getApplicationContext());
                String jwtSecret = credentialProvider.getString("WORKOUT_HISTORY_JWT_SECRET");
                Algorithm alg = Algorithm.HMAC256(jwtSecret);
                String jwt = "";
                try {
                    jwt = JWT.create()
                            .withClaim("uuid", uuid)
                            .withClaim("xid", xid)
                            .withClaim("session", session)
                            .sign(alg);
                } catch(Exception e) {
                    Log.e(TAG, "Workout History JWT creation FAILED", e);
                }
                workoutHistory.urlExtra = WORKOUT_HISTORY_URL_BASE + "?token=" + jwt.trim();
            } else {
                // https://jhtnaee.atlassian.net/wiki/spaces/IT/pages/3916202019/Console+Auto-Login#JUMP-Token-Embedding
                workoutHistory.urlExtra = WORKOUT_HISTORY_URL_BASE + "?embedded=1";
            }
        }

        if (isCommercial) {
            AppInfo surveyMonkey = new AppInfo(context.getPackageName(), WebAppActivity.class.getCanonicalName(), AppIds.SURVEY_MONKEY_UUID, 33);
            surveyMonkey.category = AppInfo.Category.Entertainment;
            surveyMonkey.urlExtra = "surveymonkey";
            surveyMonkey.specialPanelBehavior = refreshBehavior;
            surveyMonkey.enabledByDefault = false;
            add(surveyMonkey);
        }

        AppInfo youku = new AppInfo(context.getPackageName(), WebAppSSOActivity.class.getCanonicalName(), AppIds.YOUKU_UUID, 36);
        youku.category = AppInfo.Category.Entertainment;
        youku.urlExtra = "https://youku.com";
        youku.userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/11.1.1 Safari/605.1.15";
        youku.specialPanelBehavior = webBehavior;
        youku.supportsSSO = true;
        youku.webAppDomainInclude = "youku.com";
        if (!isLightCommercial) {
            youku.enabledByDefault = false;
        }
        youku.audioInput = AudioSourceInput.Foreground;
        youku.permissions = new String[] {
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE,
        };
        add(youku);

        if (!isRetail) { // Parker currently does not have tencent video app per pps
            AppInfo tencent = new AppInfo(context.getPackageName(), WebAppSSOActivity.class.getCanonicalName(), AppIds.TENCENT_UUID, 35);
            tencent.category = AppInfo.Category.Entertainment;
            tencent.urlExtra = "https://v.qq.com/";
            //tencent.userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/11.1.1 Safari/605.1.15";
            tencent.userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36";
            tencent.specialPanelBehavior = webBehavior;
            tencent.supportsSSO = true;
            tencent.webAppDomainInclude = "v.qq.com";
            if (!isLightCommercial) {
                tencent.enabledByDefault = false;
            }
            tencent.audioInput = AudioSourceInput.Foreground;
            add(tencent);
        }

        AppInfo iqiyi = new AppInfo("com.iqiyi.i18n", "", AppIds.IQIYI_UUID, 37);
        iqiyi.category = AppInfo.Category.Entertainment;
        iqiyi.specialPanelBehavior = backBehavior;
        iqiyi.clearWebViewCachePeriodically = BuildConfig.CLEAR_IQIYI_CACHE_PERIODICALLY;
        iqiyi.supportsSSO = false; // no longer supporting SSO
        if (!isLightCommercial) {
            iqiyi.enabledByDefault = false;
        }
        iqiyi.audioInput = AudioSourceInput.Foreground;
        iqiyi.permissions = new String[]{ // permissions from app manifest, prevents android.content.pm.action.REQUEST_PERMISSIONS not found on vortex
                "android.permission.INTERNET", "com.android.launcher2.permission.READ_SETTINGS",
                "android.permission.ACCESS_NETWORK_STATE", "com.android.launcher.permission.READ_SETTINGS",
                "com.android.launcher.permission.INSTALL_SHORTCUT", "com.bbk.launcher2.permission.READ_SETTINGS",
                "com.android.launcher.permission.UNINSTALL_SHORTCUT", "com.meizu.flyme.launcher.permission.READ_SETTINGS",
                "android.permission.ACCESS_WIFI_STATE", "com.iqiyi.i18n.permission.MIPUSH_RECEIVE",
                "android.permission.CHANGE_NETWORK_STATE", "com.coloros.mcs.permission.RECIEVE_MCS_MESSAGE",
                "android.permission.CHANGE_WIFI_STATE", "com.heytap.mcs.permission.RECIEVE_MCS_MESSAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_CALENDAR",
                "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_CALENDAR",
                "com.android.vending.BILLING", "com.google.android.gms.permission.AD_ID",
                "android.permission.READ_BASIC_PHONE_STATE", "android.permission.POST_NOTIFICATIONS",
                "android.permission.CHANGE_WIFI_MULTICAST_STATE", "android.permission.BLUETOOTH_CONNECT",
                "android.permission.REORDER_TASKS", "android.permission.BLUETOOTH",
                "android.permission.WAKE_LOCK", "android.permission.KILL_BACKGROUND_PROCESSES",
                "android.permission.RECEIVE_BOOT_COMPLETED", "android.permission.DOWNLOAD_WITHOUT_NOTIFICATION",
                "android.permission.VIBRATE", "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
                "android.permission.DISABLE_KEYGUARD", "android.permission.ACCESS_ADSERVICES_AD_ID",
                "com.xiaomi.permission.AUTH_SERVICE", "android.permission.AD_SERVICES_CONFIG",
                "android.permission.CAMERA", "android.permission.SYSTEM_ALERT_WINDOW",
                "android.permission.FLASHLIGHT", "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
                "android.permission.MANAGE_ACCOUNTS", "com.google.android.c2dm.permission.RECEIVE",
                "android.permission.AUTHENTICATE_ACCOUNTS", "android.permission.ACCESS_ADSERVICES_TOPICS",
                "com.google.android.launcher.permission.READ_SETTINGS", "android.permission.GET_TASKS",
                "com.android.launcher3.permission.READ_SETTINGS", "com.huawei.appmarket.service.commondata.permission.GET_COMMON_DATA",
        };
        add(iqiyi);

        if(ProductConfig.hasIFIT()) {
            AppInfo ifit = new AppInfo("com.ifit.standalone", "", AppIds.IFIT_UUID, 52);
            ifit.category = AppInfo.Category.Integration;
            ifit.bypassWorkoutUiProcess = true;
            ifit.useThirdPartyInterface = true;
            ifit.suppressCoolDownControls = true;
            ifit.workoutEndAction = AppInfo.WorkoutEndAction.NoWorkoutSummary;
            ifit.workoutEndResetSafetyPin = false; // NOT for ifit, see https://jhtnaee.atlassian.net/browse/CGC-12655 for details
            ifit.settingOverrides = new SettingValue[] {
                    new SettingValue(WorkoutSettings.WORKOUT_ID, WorkoutIds.IFIT),
                    new SettingValue(WorkoutControllerService.CAN_MODIFY_GOAL_SETTING, 0)
            };
            ifit.specialPanelBehavior = Ifit3PanelBehavior;
            ifit.supportsSSO = true;
            ifit.audioInput = AudioSourceInput.Foreground;
            ifit.enabledByDefault = true;
            ifit.requiresPrelaunchKill = true;
            if (isCommercial) {
                //ifit.clearDataOnKill = true;
            }
            ifit.permissions = new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            };
            ifit.uiEventName = UiEventKeys.EVENT.IFIT_LOADING.name();
            // extras the ifit app expects - IS_MATRIX is constant, the rest are filled in at launch (see LaunchExtraResolver)
            ifit.startExtras = IntentExtras.of(
                    IntentExtras.bool("com.ifit.extra.IS_MATRIX", true),
                    IntentExtras.dynamic("com.ifit.extra.IS_DEFAULT_UNIT_IMPERIAL", DynamicExtraValue.UnitsAreImperial),
                    IntentExtras.dynamic("is_commercial", DynamicExtraValue.CommercialFlag),
                    IntentExtras.dynamic("com.ifit.extra.IS_CLUB", DynamicExtraValue.CommercialFlag)
            );
            add(ifit);
        }

        if (isRetail) {
            AppInfo weibo = new AppInfo("com.weico.international", "", AppIds.WEIBO_UUID, 53);
            weibo.category = AppInfo.Category.Entertainment;
            weibo.specialPanelBehavior = backBehavior;
            weibo.audioInput = AudioSourceInput.Foreground;
            weibo.enabledByDefault = false;
            weibo.permissions = new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            };
            add(weibo);

            AppInfo netease = new AppInfo("com.netease.cloudmusic", "", AppIds.NETEASE_CLOUD_MUSIC, 54);
            netease.category = AppInfo.Category.Entertainment;
            netease.specialPanelBehavior = backBehavior;
            netease.audioInput = AudioSourceInput.Foreground;
            netease.enabledByDefault = false;
            netease.permissions = new String[] {
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE,
            };
            add(netease);
        }

        // NOTE: club com handled in AppIconManager

        SettingValue[] ymcaSettingValues  = new SettingValue[] {
                new SettingValue(WorkoutSettings.WORKOUT_ID, WorkoutIds.YMCA_360),
                new SettingValue(WorkoutControllerService.CAN_MODIFY_GOAL_SETTING, 0)
        };
        WorkoutInput[] ymcaWorkoutInputs = new WorkoutInput[] {new WorkoutInput(ConsoleWorkoutVariables.COOLDOWN_TIME_START, 0)};
        // YMCA has same package but different options depending on how it is launched? may need to dive deeper to understand what YMCA actually needs?
        AppInfo ymca360FeaturedWorkout = new AppInfo("org.ymca360.omni", "", AppIds.YMCA_360_FEATURED_UUID, 61);
        ymca360FeaturedWorkout.specialPanelBehavior = YmcaPanelBehavior;
        ymca360FeaturedWorkout.supportsSSO = true;
        ymca360FeaturedWorkout.bypassWorkoutUiProcess = false; // YMCA does run the 3-2-1 countdown
        ymca360FeaturedWorkout.useThirdPartyInterface = true;
        ymca360FeaturedWorkout.suppressCoolDownControls = true;
        ymca360FeaturedWorkout.category = AppInfo.Category.Integration;
        ymca360FeaturedWorkout.settingOverrides = ymcaSettingValues;
        ymca360FeaturedWorkout.workoutInputs = ymcaWorkoutInputs;
        ymca360FeaturedWorkout.audioInput = AudioSourceInput.Foreground;
        ymca360FeaturedWorkout.uiEventName = UiEventKeys.EVENT.YMCA360_LOADING.name();
        add(ymca360FeaturedWorkout);

        AppInfo ymca360WorkoutApp = new AppInfo("org.ymca360.omni", "", AppIds.YMCA_360_UUID, 61);
        ymca360WorkoutApp.experimentalKey = SettingKeys.Experimental.DATA_ENABLE_YMCA_360_BOOL.name();
        ymca360WorkoutApp.specialPanelBehavior = backBehavior;
        ymca360WorkoutApp.category = AppInfo.Category.Integration; // technically an integration but still operates in our normal app flow
        ymca360WorkoutApp.settingOverrides = ymcaSettingValues;
        ymca360WorkoutApp.workoutInputs = ymcaWorkoutInputs;
        ymca360WorkoutApp.audioInput = AudioSourceInput.Foreground;
        add(ymca360WorkoutApp);

        //Amazon Freevee
        AppInfo amazonFreeveeApp = new AppInfo("com.amazon.imdb.tv.mobile.app", "", AppIds.AMAZON_FREEVEE_UUID, 100);
        amazonFreeveeApp.category = AppInfo.Category.Entertainment;
        amazonFreeveeApp.supportsSSO = false;
        amazonFreeveeApp.experimentalKey = SettingKeys.Experimental.DATA_AMAZON_FREEVEE_ENABLE_BOOL.name();
        amazonFreeveeApp.audioInput = AudioSourceInput.Foreground;
        add(amazonFreeveeApp);

        //Pluto TV
        AppInfo plutoTVApp = new AppInfo("tv.pluto.android", "", AppIds.PLUTO_TV_UUID, 101);
        plutoTVApp.category = AppInfo.Category.Entertainment;
        plutoTVApp.supportsSSO = true;
        plutoTVApp.specialPanelBehavior = backBehavior;
        plutoTVApp.audioInput = AudioSourceInput.Foreground;
        plutoTVApp.permissions = new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };
        plutoTVApp.allowedCountyCodes = new String[] {
                "US", "CA", "GB", "DE", "FR",
                "IT",  "ES",  "CH", "AT",  "SE",  "DK",
                "NO",  "BR",  "AR", "BO",  "CL",  "CO",
                "CR",  "DO",  "EC", "GT",  "HN",  "MX",
                "NI",  "PA",  "PE", "PY",  "SV",  "UY", "VE",
        };
        plutoTVApp.allowedWeatherLocations = new String[] {
                "Mexico", "USA", "Canada", "UK",
                "Germany", "France", "Italy", "Spain",
                "Switzerland", "Austria", "Sweden", "Denmark",
                "Norway", "Brazil", "Argentina", "Bolivia",
                "Chile", "Colombia", "Costa Rica", "Dominican Republic",
                "Ecuador", "Guatemala", "Honduras", "Nicaragua",
                "Panama", "Peru", "Paraguay", "El Salvador", "Uruguay", "Venezuela",
        };
        add(plutoTVApp);

        // launched from splash screen
        AppInfo test = new AppInfo("com.jht.matrixcomm.testapp", "", AppIds.MATRIX_TEST_UUID, IGNORE_APP_ANALYTICS_ID);
        test.category = AppInfo.Category.FullIntegration;
        test.workoutEndAction = AppInfo.WorkoutEndAction.NoWorkoutSummary;
        test.settingOverrides = new SettingValue[] {
                new SettingValue(WorkoutSettings.WORKOUT_ID, WorkoutIds.MATRIX_TEST_APP)
        };
        test.specialPanelBehavior = PanelBehavior.getNoPanelBehavior();
        add(test);

        //Hero's Journey
        AppInfo herosJourney = new AppInfo("com.fitnesslabs.gymstationrn", "", AppIds.HEROS_JOURNEY_UUID, IGNORE_APP_ANALYTICS_ID);
        herosJourney.category = AppInfo.Category.FullIntegration; //com.fitnesslabs.gymstationrn
        herosJourney.bypassWorkoutUiProcess = true; //Prevents TaskStackBuilder from creating hidden backstack WorkoutActivity
        herosJourney.useThirdPartyInterface = true; //Treats Hero's Journey as a 3rd-party integration interface
        herosJourney.workoutEndAction = AppInfo.WorkoutEndAction.NoWorkoutSummary;
        herosJourney.specialPanelBehavior = PanelBehavior.getNoPanelBehavior();
        add(herosJourney);

        AppInfo custom = new AppInfo(DataMatrix.getString(SettingKeys.Experimental.DATA_CUSTOM_APP_OVERRIDE_PACKAGE_NAME_STRING), "", AppIds.CUSTOM_OVERRIDE_APP_UUID, IGNORE_APP_ANALYTICS_ID);
        custom.category = AppInfo.Category.FullIntegration;
        custom.bypassWorkoutUiProcess = true; //Prevents TaskStackBuilder from creating hidden backstack WorkoutActivity
        custom.useThirdPartyInterface = true; //Treats app as a 3rd-party integration interface
        custom.workoutEndAction = AppInfo.WorkoutEndAction.NoWorkoutSummary;
        custom.specialPanelBehavior = PanelBehavior.getNoPanelBehavior();
        add(custom);

        // Add clubcom app if any are defined
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_CLUB_COM_BOOL)) {
            try {
                ClubComOptions options = DataMatrix.getParcelable(ClubComKeys.DATA_CLUB_COM_OPTIONS_PARCELABLE, ClubComOptions.class);
                if (options != null && options.getApps() != null) for (ClubComApp app : options.getApps()) {
                    Log.d(TAG, "ClubCom app> " + app.getId() + " " + app.getAppUrl());
                    AppInfo appInfo = new AppInfo(context.getPackageName(), ClubComActivity.class.getCanonicalName(), app.getId(), 55);
                    appInfo.urlExtra = app.getAppUrl();
                    appInfo.audioInput = AudioSourceInput.Foreground;
                    appInfo.specialPanelBehavior = PanelBehavior.getNormalPanelRefreshBehavior();
                    add(appInfo);
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }

        // Add App Store
        if (ProductConfig.hasAppStore() && DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_APPSTORE_BOOL)) {
            AppInfo.Category category = AppInfo.Category.Entertainment;
            if (isRetail) category = AppInfo.Category.Workout; // using Workout so it doesn't show up for Parker Welcome app list
            AppInfo appStore = new AppInfo(context.getPackageName(), AppStoreActivity.class.getCanonicalName(), AppIds.APP_STORE_UUID, IGNORE_APP_ANALYTICS_ID);
            appStore.category = category;
            add(appStore);

            // Add custom list of installed apps via App Store
            List<AppStoreAppInfo> appStoreAppInfoList = AppStoreConfigManager.INSTANCE.getAppList();
            for (AppStoreAppInfo app : appStoreAppInfoList) {
                AppInfo appInfo = new AppInfo(app.getPackageName(), "", app.getUuid(), IGNORE_APP_ANALYTICS_ID);
                appInfo.permissions = app.getPermissions().toArray(new String[0]);
                appInfo.category = category;
                appInfo.specialPanelBehavior = backBehavior;
                if (!app.getUuid().isEmpty()) add(appInfo);
            }
        }
    }

}