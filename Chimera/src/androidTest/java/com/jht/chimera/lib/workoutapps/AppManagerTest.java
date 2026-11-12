package com.jht.chimera.lib.workoutapps;


import static com.jht.chimera.lib.common.dapi.DapiUser.GUEST_USER_UUID;
import static com.jht.chimera.lib.workout.WorkoutIds.MANUAL;
import static com.jht.chimera.lib.workout.WorkoutIds.THR_SPEED;

import android.os.SystemClock;
import android.util.Log;

import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TiersKeys;
import com.jht.chimera.lib.common.datamatrixkeys.VaKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.chimera.service.tiers.PremiumFeature;
import com.jht.chimera.service.tiers.PremiumFeatureSet;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.Default;
import com.jht.chimera.ui.machinesettings.JHTTypes;
import com.jht.datamatrixlib.DataMatrix;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AppManagerTest {

    private static final String TAG = "AppManagerTest";

    /**
     * FrameConfigManager.setFrame() propagates asynchronously (DataMatrix is a cross-process
     * store), so the list has to be built only once the new frame has actually landed. The old
     * code used a fixed sleep, which loses the race under load and silently builds the app
     * list against the PREVIOUS frame.
     */
    private static final long FRAME_CHANGE_TIMEOUT_MS = 3000;
    private static final long FRAME_POLL_MS = 10;
    private static final long FRAME_SETTLE_MS = 20;

    List<AppIcon> appList;

    /**
     * Collected mismatches. Each test method checks ~18 app lists; with plain assertEquals the
     * first failure hides all the others, which turns fixing this suite into one-token-per-run
     * whack-a-mole. Collecting means a single run reports every failing case with its full
     * (untruncated) actual list.
     */
    private final List<String> mismatches = new ArrayList<>();

    /**
     * Last frame successfully applied in THIS process. Static on purpose: the singletons and the
     * device state are per-process, not per-test, so the tracking has to match that lifetime.
     * Lets us skip a no-op frame change and verify every real one.
     */
    private static FrameConfigEnum sAppliedFrame = null;

    private static String describeConfig() {
        FrameConfig cfg = FrameConfigManager.getInstance().getFrameConfig();
        return cfg == null ? "config=null" : "machineClassType=" + cfg.machineClassType;
    }

    /**
     * AppManagerBuilder to set values used to test the app manager
     * <p>
     * defaults:
     * - treadmill
     * - commercial
     * - small screen false
     * - view type default
     * - egym disabled
     * - AM/WT tracking enabled
     * - hasTV true
     * - hasHDMI false
     * - RemoteTV false
     */
    static class AppManagerBuilder {

        private AppManager appManager = AppManager.getInstance();
        private AppIconManager appIconManager = AppIconManager.getInstance();

        public AppManagerBuilder(FrameConfigEnum frame) {
            applyFrame(frame);
            DataMatrix.putString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, AppIds.DEFAULT_APP_UUID);
            DataMatrix.putInt(WorkoutKeys.DATA_WORKOUT_ID_INT, MANUAL);
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL, true);
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL, true);
            DataMatrix.putBoolean(SettingKeys.Experimental.DATA_EGYM_ENABLED_BOOL, false);
            DataMatrix.putBoolean(VaKeys.DATA_VA_CAN_DISPLAY_APP_ICON_BOOL, true);
            DataMatrix.putBoolean(SettingKeys.Experimental.DATA_ENABLE_MILE_HIGH_RUN_CLUB_BOOL, false);
            DataMatrix.putString(DAPIKeys.DATA_USER_DAPI_UUID_STRING, GUEST_USER_UUID);
            DataMatrix.putBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL, true);
            DataMatrix.putBoolean(SettingKeys.TV.DATA_TV_REMOTE_TV_BOOL, false);
            DataMatrix.putBoolean(TiersKeys.DATA_USING_TIERS_BOOL, false);
            // tiers() writes these and nothing ever cleared them, so a tiers case leaked into
            // every later case in the run. Reset to an empty set alongside the USING_TIERS flag.
            DataMatrix.putParcelable(TiersKeys.DATA_GUEST_FEATURE_SET_PARCELABLE, new PremiumFeatureSet());
            DataMatrix.putParcelable(TiersKeys.DATA_FEATURE_SET_PARCELABLE, new PremiumFeatureSet());

            appManager.smallScreen = false;
            appManager.isCommercial = true;
            appManager.isRetail = false;
            appManager.isLightCommercial = false;
            appManager.hasTV = true;
            appManager.hasHDMI = false;
            appIconManager.isCommercial = true;
            appIconManager.isRetail = false;
            appIconManager.isLightCommercial = false;
        }

        private static final long FRAME_SETTLE_MS = 100;
        private static void applyFrame(FrameConfigEnum frame) {
            // Re-check the manager, not just our own bookkeeping - if something else changed the
            // frame behind us, sAppliedFrame is a lie.
            if (frame == sAppliedFrame && FrameConfigManager.getInstance().getFrameType() == frame) {
                return;
            }

            FrameConfigManager.getInstance().setFrame(frame);

            long deadline = SystemClock.uptimeMillis() + FRAME_CHANGE_TIMEOUT_MS;
            while (SystemClock.uptimeMillis() < deadline) {
                if (FrameConfigManager.getInstance().getFrameType() == frame) {
                    SystemClock.sleep(FRAME_SETTLE_MS);
                    sAppliedFrame = frame;
                    Log.i(TAG, "frame now " + frame + " / " + describeConfig());
                    return;
                }
                SystemClock.sleep(FRAME_POLL_MS);
            }
            Assert.fail("Frame did not become " + frame + " within " + FRAME_CHANGE_TIMEOUT_MS
                    + "ms (getFrameType()=" + FrameConfigManager.getInstance().getFrameType() + ")");
        }

        public AppManagerBuilder largeScreen() {
            appManager.smallScreen = false;
            return this;
        }

        public AppManagerBuilder smallScreen() {
            appManager.smallScreen = true;
            return this;
        }

        public AppManagerBuilder commercial() {
            appManager.isCommercial = true;
            appManager.isRetail = false;
            appManager.isLightCommercial = false;
            appIconManager.isCommercial = true;
            appIconManager.isRetail = false;
            appIconManager.isLightCommercial = false;
            // commercial has disney disabled by default. Set per-builder rather than once at the
            // top of the test method so the value can't depend on which test ran first.
            DataMatrix.putBoolean(SettingKeys.Application.DATA_DISNEY_PLUS_ENABLED_BOOL, false);
            return this;
        }

        public AppManagerBuilder retail() {
            appManager.isCommercial = false;
            appManager.isRetail = true;
            appManager.isLightCommercial = false;
            appIconManager.isCommercial = false;
            appIconManager.isRetail = true;
            appIconManager.isLightCommercial = false;
            // retail has disney enabled by default
            DataMatrix.putBoolean(SettingKeys.Application.DATA_DISNEY_PLUS_ENABLED_BOOL, true);
            return this;
        }

        public AppManagerBuilder lightCommercial() {
            appManager.isCommercial = false;
            appManager.isRetail = false;
            appManager.isLightCommercial = true;
            appIconManager.isCommercial = false;
            appIconManager.isRetail = false;
            appIconManager.isLightCommercial = true;
            return this;
        }

        public AppManagerBuilder tv() {
            appManager.hasTV = true;
            return this;
        }

        public AppManagerBuilder noTv() {
            appManager.hasTV = false;
            return this;
        }

        public AppManagerBuilder hdmi() {
            appManager.hasHDMI = true;
            return this;
        }

        public AppManagerBuilder noHdmi() {
            appManager.hasHDMI = false;
            return this;
        }

        public AppManagerBuilder remoteTV() {
            DataMatrix.putBoolean(SettingKeys.TV.DATA_TV_REMOTE_TV_BOOL, true);
            return this;
        }

        public AppManagerBuilder disableAMandWT() {
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL, false);
            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL, false);
            return this;
        }

        public AppManagerBuilder disableVA() {
            DataMatrix.putBoolean(VaKeys.DATA_VA_CAN_DISPLAY_APP_ICON_BOOL, false);
            return this;
        }

        public AppManagerBuilder enableMileHighRun() {
            DataMatrix.putBoolean(SettingKeys.Experimental.DATA_ENABLE_MILE_HIGH_RUN_CLUB_BOOL, true);
            return this;
        }

        public AppManagerBuilder enableEgym() {
            DataMatrix.putBoolean(SettingKeys.Experimental.DATA_EGYM_ENABLED_BOOL, true);
            return this;
        }

        public AppManagerBuilder fakeUserLogin() {
            DataMatrix.putString(DAPIKeys.DATA_USER_DAPI_UUID_STRING, "00000000-0000-0000-0000-000000000001");
            DataMatrix.putBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL, false);
            return this;
        }

        public AppManagerBuilder startAppUuid(String viewType) {
            DataMatrix.putString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, viewType);
            return this;
        }

        public AppManagerBuilder workoutID(int workoutId) {
            DataMatrix.putInt(WorkoutKeys.DATA_WORKOUT_ID_INT, workoutId);
            return this;
        }

        List<String> enabledAppUuids = new LinkedList<>();

        public AppManagerBuilder enableApp(String appUuid) {
            enabledAppUuids.add(appUuid);
            return this;
        }

        public AppManagerBuilder tiers(String json) {
            DataMatrix.putBoolean(TiersKeys.DATA_USING_TIERS_BOOL, true);
            PremiumFeatureSet set = new PremiumFeatureSet();
            set.addFeature(new PremiumFeature("simple", AppIds.SIMPLE_UUID, JHTTypes.FEATURE_TYPE.FEATURE_TYPE_WORKOUT, JHTTypes.FEATURE_STATE.FEATURE_HIDDEN));
            DataMatrix.putParcelable(TiersKeys.DATA_GUEST_FEATURE_SET_PARCELABLE, set);
            DataMatrix.putParcelable(TiersKeys.DATA_FEATURE_SET_PARCELABLE, set);
            return this;
        }

        public AppIconManager build() {
            // AppManager wasn't designed for testing so clear the app order so the default ordering can load on a reset
            appManager.reset();
            appIconManager.finishAppManager();
            DataMatrix.putString(SettingKeys.Application.DATA_APP_ORDER_STRING, "[]");
            appIconManager.reset();
            appIconManager.finishAppManager();

            // enable any apps that was enabled
            if (!enabledAppUuids.isEmpty()) {
                for (String appUuid : enabledAppUuids) {
                    appIconManager.getAppOrder().add(appIconManager.getAppIcon(appUuid));
                }
                appIconManager.saveAppOrdering();
            }

            System.out.println(appIconManager.getVisibleApps().toString());
            return appIconManager;
        }
    }


    /**
     * Records a mismatch instead of throwing, so every case in the method gets exercised and
     * reported in one run. Always logs the actual list so the full string is recoverable from
     * logcat (adb logcat -s AppManagerTest) without JUnit's "..." elision.
     */
    private void check(String label, String expected, List<AppIcon> actual) {
        String actualStr = actual.toString();
        String frame = FrameConfigManager.getInstance().getFrameType() + " / " + describeConfig();
        Log.i(TAG, label + " [" + frame + "] -> " + actualStr);
        if (!expected.equals(actualStr)) {
            String entry = "[" + label + "] frame=" + frame + " expected=" + expected + " actual=" + actualStr;
            Log.e(TAG, "MISMATCH " + entry);
            mismatches.add(entry);
        }
    }

    private void assertNoMismatches() {
        if (mismatches.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(mismatches.size()).append(" app list mismatch(es): ");
        for (int i = 0; i < mismatches.size(); i++) {
            if (i > 0) {
                sb.append("  ||  ");
            }
            sb.append(mismatches.get(i));
        }
        Assert.fail(sb.toString());
    }

    @Before
    public void setup() {
        mismatches.clear();
        Log.i(TAG, "running in process=" + android.app.Application.getProcessName() + " pid=" + android.os.Process.myPid());
    }

    @After
    public void destroy() {
        // Leave the console on a predictable footing for whatever runs next. The builder resets
        // every key it writes on construction, so the singletons are the part worth clearing.
        AppManager.getInstance().reset();
        AppIconManager.getInstance().reset();
    }

    @Test
    @Emulator
    @Default
    public void testVisibleAppListsCommercial() {

        // default commercial app list for treadmill
        appList = new AppManagerBuilder(FrameConfigEnum.Treadmill_Endurance)
                .commercial()
                .largeScreen().tv().noHdmi()
                .startAppUuid(AppIds.DEFAULT_APP_UUID)
                .build().getVisibleApps();
        check("commercial / treadmill / default",
                "[simple, trackApp, heartRate, facebook, x, myFitnessPal, youtube, weather, tv, netflix, flipboard, pressReader, thirdPartySolitaire, kindle_reader, instagram, spotify, virtualActive, facilityCalendar, appStore]",
                appList);

        // default commercial app list for spin bike - removes tv, and CGC-25017 removes distance, calories, watts, rpm, heartRate in favor of target training
        appList = new AppManagerBuilder(FrameConfigEnum.CardioSpinBike)
                .commercial()
                .largeScreen().tv().noHdmi()
                .startAppUuid(AppIds.DEFAULT_APP_UUID)
                .build().getVisibleApps();
        check("commercial / spinbike / default",
                "[targetTraining, simple, trackApp, facebook, x, myFitnessPal, youtube, weather, netflix, flipboard, pressReader, thirdPartySolitaire, kindle_reader, instagram, spotify, virtualActive, facilityCalendar, appStore]",
                appList);

        // default commercial app list for climbmill - removes trackApp
        appList = new AppManagerBuilder(FrameConfigEnum.Climbmill_Endurance)
                .commercial()
                .largeScreen().tv().noHdmi()
                .startAppUuid(AppIds.DEFAULT_APP_UUID)
                .build().getVisibleApps();
        check("commercial / climbmill / default",
                "[simple, heartRate, facebook, x, myFitnessPal, youtube, weather, tv, netflix, flipboard, pressReader, thirdPartySolitaire, kindle_reader, instagram, spotify, virtualActive, facilityCalendar, appStore]",
                appList);

        // test egym feature app - adds egym
        appList = new AppManagerBuilder(FrameConfigEnum.Treadmill_Endurance)
                .commercial()
                .largeScreen().tv().noHdmi()
                .enableEgym()
                .startAppUuid(AppIds.EGYM_UUID)
                .build().getVisibleApps();
        check("commercial / treadmill / egym featured",
                "[egym, simple, trackApp, heartRate, facebook, x, myFitnessPal, youtube, weather, tv, netflix, flipboard, pressReader, thirdPartySolitaire, kindle_reader, instagram, spotify, virtualActive, facilityCalendar, appStore]",
                appList);

        // test Sprint8 feature app - adds sprint8
        // NOTE: you can't start a Sprint8 workout on climbmill so not sure why the setAppShownLogic has a climbmill check
        appList = new AppManagerBuilder(FrameConfigEnum.Treadmill_Endurance)
                .commercial()
                .largeScreen().tv().noHdmi()
                .startAppUuid(AppIds.SPRINT8_UUID)
                .build().getVisibleApps();
        check("commercial / treadmill / sprint8 featured",
                "[sprint8, simple, trackApp, heartRate, facebook, x, myFitnessPal, youtube, weather, tv, netflix, flipboard, pressReader, thirdPartySolitaire, kindle_reader, instagram, spotify, virtualActive, facilityCalendar, appStore]",
                appList);

        // test Landmark feature app - adds landmark and removes trackApp
        appList = new AppManagerBuilder(FrameConfigEnum.Climbmill_Endurance)
                .commercial()
                .largeScreen().tv().noHdmi()
                .startAppUuid(AppIds.LANDMARKS_UUID)
                .build().getVisibleApps();
        check("commercial / climbmill / landmarks featured",
                "[landmarks, simple, heartRate, facebook, x, myFitnessPal, youtube, weather, tv, netflix, flipboard, pressReader, thirdPartySolitaire, kindle_reader, instagram, spotify, virtualActive, facilityCalendar, appStore]",
                appList);

        // test Target Heart Rate feature app - adds targetHeartRate and removes heartRate
        appList = new AppManagerBuilder(FrameConfigEnum.Treadmill_Endurance)
                .commercial()
                .largeScreen().tv().noHdmi()
                .startAppUuid(AppIds.TARGET_HEART_RATE_UUID)
                .workoutID(THR_SPEED)
                .build().getVisibleApps();
        check("commercial / treadmill / target heart rate featured",
                "[targetHeartRate, simple, trackApp, facebook, x, myFitnessPal, youtube, weather, tv, netflix, flipboard, pressReader, thirdPartySolitaire, kindle_reader, instagram, spotify, virtualActive, facilityCalendar, appStore]",
                appList);

        // test Constant Watts feature app - adds constantWatts
        appList = new AppManagerBuilder(FrameConfigEnum.RecumbentBike_Endurance)
                .commercial()
                .largeScreen().tv().noHdmi()
                .startAppUuid(AppIds.CONSTANT_WATTS_UUID)
                .build().getVisibleApps();
        check("commercial / recumbent bike / constant watts featured",
                "[constantWatts, simple, trackApp, heartRate, facebook, x, myFitnessPal, youtube, weather, tv, netflix, flipboard, pressReader, thirdPartySolitaire, kindle_reader, instagram, spotify, virtualActive, facilityCalendar, appStore]",
                appList);

        // test CPAT which makes simpleApp a featured app
        // NOTE: old code seems to launch simple app fullscreen for CPAT but cannot find record of that requirement
        appList = new AppManagerBuilder(FrameConfigEnum.Climbmill_Endurance)
                .commercial()
                .largeScreen().tv().noHdmi()
                .workoutID(WorkoutIds.CPAT_FITNESS_TEST)
                .startAppUuid(AppIds.SIMPLE_UUID)
                //.startAppUuid(AppIds.CPAT_UUID)
                .build().getVisibleApps();
        check("commercial / climbmill / CPAT",
                "[simple, heartRate, facebook, x, myFitnessPal, youtube, weather, tv, netflix, flipboard, pressReader, thirdPartySolitaire, kindle_reader, instagram, spotify, virtualActive, facilityCalendar, appStore]",
                appList);

        // athena FTP test - adds bikeftptest
        appList = new AppManagerBuilder(FrameConfigEnum.CardioSpinBike)
                .commercial()
                .largeScreen().tv().noHdmi() // console is tv capable but frame should block tv app?
                .startAppUuid(AppIds.FTP_UUID)
                .build().getVisibleApps();
        check("commercial / spinbike / FTP test featured",
                "[targetTraining, bikeftptest, simple, trackApp, facebook, x, myFitnessPal, youtube, weather, netflix, flipboard, pressReader, thirdPartySolitaire, kindle_reader, instagram, spotify, virtualActive, facilityCalendar, appStore]",
                appList);

        // athena max HR test - adds bikemaxhrtest
        appList = new AppManagerBuilder(FrameConfigEnum.CardioSpinBike)
                .commercial()
                .largeScreen().tv().noHdmi()
                .startAppUuid(AppIds.MAX_HR_TEST_UUID)
                .build().getVisibleApps();
        check("commercial / spinbike / max HR test featured",
                "[targetTraining, bikemaxhrtest, simple, trackApp, facebook, x, myFitnessPal, youtube, weather, netflix, flipboard, pressReader, thirdPartySolitaire, kindle_reader, instagram, spotify, virtualActive, facilityCalendar, appStore]",
                appList);

        // test army test - trackApp becomes the featured app so it's first
        appList = new AppManagerBuilder(FrameConfigEnum.Treadmill_Endurance)
                .commercial()
                .largeScreen().tv().noHdmi()
                .startAppUuid(AppIds.TRACK_UUID)
                .build().getVisibleApps();
        check("commercial / treadmill / track featured (army test)",
                "[trackApp, simple, heartRate, facebook, x, myFitnessPal, youtube, weather, tv, netflix, flipboard, pressReader, thirdPartySolitaire, kindle_reader, instagram, spotify, virtualActive, facilityCalendar, appStore]",
                appList);

        // test disable VA
        appList = new AppManagerBuilder(FrameConfigEnum.Treadmill_Endurance)
                .commercial()
                .largeScreen().tv().noHdmi()
                .startAppUuid(AppIds.DEFAULT_APP_UUID)
                .disableVA()
                .build().getVisibleApps();
        check("commercial / treadmill / VA disabled",
                "[simple, trackApp, heartRate, facebook, x, myFitnessPal, youtube, weather, tv, netflix, flipboard, pressReader, thirdPartySolitaire, kindle_reader, instagram, spotify, facilityCalendar, appStore]",
                appList);

        // test VA feature app - VA should be first
        appList = new AppManagerBuilder(FrameConfigEnum.Treadmill_Endurance)
                .commercial()
                .largeScreen().tv().noHdmi()
                .startAppUuid(AppIds.VA_UUID)
                .build().getVisibleApps();
        check("commercial / treadmill / VA featured",
                "[virtualActive, simple, trackApp, heartRate, facebook, x, myFitnessPal, youtube, weather, tv, netflix, flipboard, pressReader, thirdPartySolitaire, kindle_reader, instagram, spotify, facilityCalendar, appStore]",
                appList);

        // test user login - adds workoutHistory
        appList = new AppManagerBuilder(FrameConfigEnum.Treadmill_Endurance)
                .commercial()
                .largeScreen().tv().noHdmi()
                .startAppUuid(AppIds.DEFAULT_APP_UUID)
                .fakeUserLogin()
                .build().getVisibleApps();
        check("commercial / treadmill / user logged in",
                "[simple, trackApp, heartRate, facebook, x, myFitnessPal, youtube, weather, tv, netflix, flipboard, pressReader, thirdPartySolitaire, kindle_reader, instagram, spotify, virtualActive, facilityCalendar, workoutHistory, appStore]",
                appList);

        // test remote TV - adds remoteTV
        appList = new AppManagerBuilder(FrameConfigEnum.Treadmill_Endurance)
                .commercial()
                .largeScreen().tv().noHdmi()
                .startAppUuid(AppIds.DEFAULT_APP_UUID)
                .remoteTV()
                .build().getVisibleApps();
        check("commercial / treadmill / remote TV",
                "[simple, trackApp, heartRate, facebook, x, myFitnessPal, youtube, weather, tv, netflix, flipboard, pressReader, thirdPartySolitaire, kindle_reader, instagram, spotify, virtualActive, remoteTv, facilityCalendar, appStore]",
                appList);

        // test no TV - removes tv
        appList = new AppManagerBuilder(FrameConfigEnum.Treadmill_Endurance)
                .commercial()
                .largeScreen().noTv().noHdmi()
                .startAppUuid(AppIds.DEFAULT_APP_UUID)
                .build().getVisibleApps();
        check("commercial / treadmill / no TV",
                "[simple, trackApp, heartRate, facebook, x, myFitnessPal, youtube, weather, netflix, flipboard, pressReader, thirdPartySolitaire, kindle_reader, instagram, spotify, virtualActive, facilityCalendar, appStore]",
                appList);

        // test enable web browser which is disabled by default
        appList = new AppManagerBuilder(FrameConfigEnum.Treadmill_Endurance)
                .commercial()
                .largeScreen().tv().noHdmi()
                .startAppUuid(AppIds.DEFAULT_APP_UUID)
                .enableApp(AppIds.WEB_BROWSER_UUID)
                .build().getVisibleApps();
        check("commercial / treadmill / web browser enabled",
                "[simple, trackApp, heartRate, facebook, x, myFitnessPal, youtube, weather, tv, netflix, flipboard, pressReader, thirdPartySolitaire, kindle_reader, instagram, spotify, virtualActive, facilityCalendar, appStore, webBrowser]",
                appList);

        // tiers - hide simple
        appList = new AppManagerBuilder(FrameConfigEnum.Treadmill_Endurance)
                .commercial()
                .largeScreen().tv().noHdmi()
                .startAppUuid(AppIds.DEFAULT_APP_UUID)
                .tiers(AppIds.WEB_BROWSER_UUID)
                .build().getVisibleApps();
        check("commercial / treadmill / tiers hides simple",
                "[trackApp, heartRate, facebook, x, myFitnessPal, youtube, weather, tv, netflix, flipboard, pressReader, thirdPartySolitaire, kindle_reader, instagram, spotify, virtualActive, facilityCalendar, appStore]",
                appList);

        assertNoMismatches();
    }


    @Test
    @Emulator
    @Default
    public void testVisibleAppListsRetail() {

        // default retail app list for treadmill - no tv, but adds webBrowser, hdmi
        appList = new AppManagerBuilder(FrameConfigEnum.T70_02)
                .retail().hdmi().noTv()
                .largeScreen()
                .startAppUuid(AppIds.DEFAULT_APP_UUID)
                .build().getVisibleApps();
        check("retail / T70_02 / default",
                "[simple, trackApp, virtualActive, heartRate, hdmi, netflix, spotify, youtube, facebook, x, instagram, myFitnessPal, weather, webBrowser, kindle_reader, flipboard, pressReader, thirdPartySolitaire, appStore]",
                appList);

        // default retail app list for treadmill for small screen - removes kindle_reader
        appList = new AppManagerBuilder(FrameConfigEnum.T70_02)
                .retail().hdmi().noTv()
                .smallScreen()
                .startAppUuid(AppIds.DEFAULT_APP_UUID)
                .build().getVisibleApps();
        check("retail / T70_02 / small screen",
                "[simple, trackApp, virtualActive, heartRate, hdmi, netflix, spotify, youtube, facebook, x, instagram, myFitnessPal, weather, webBrowser, flipboard, pressReader, thirdPartySolitaire, appStore]",
                appList);

        // TODO add ICR70 tests????
//        appList = new AppManagerBuilder(FrameConfigEnum.ICR70)
//                .retail().hdmi().noTv()
//                .largeScreen()
//                .viewType(AppIds.NO_APP_UUID)
//                .build().getVisibleApps();
//        Assert.assertEquals("[simple, trackApp, heartRate, facebook, x, myFitnessPal, youtube, weather, netflix, flipboard, pressReader, kindle_reader, instagram, spotify, distance, calories, watts, rpm, virtualActive, facilityCalendar, appStore]", appList.toString());

        // default retail app list for climbmill - removes trackApp
        appList = new AppManagerBuilder(FrameConfigEnum.C50)
                .retail().hdmi().noTv()
                .largeScreen()
                .startAppUuid(AppIds.DEFAULT_APP_UUID)
                .build().getVisibleApps();
        check("retail / C50 climbmill / default",
                "[simple, virtualActive, heartRate, hdmi, netflix, spotify, youtube, facebook, x, instagram, myFitnessPal, weather, webBrowser, kindle_reader, flipboard, pressReader, thirdPartySolitaire, appStore]",
                appList);

        // test Sprint8 feature app - adds sprint8
        // NOTE: you can't start a Sprint8 workout on climbmill so not sure why the setAppShownLogic has a climbmill check
        appList = new AppManagerBuilder(FrameConfigEnum.T70_02)
                .retail().hdmi().noTv()
                .largeScreen()
                .startAppUuid(AppIds.SPRINT8_UUID)
                .build().getVisibleApps();
        check("retail / T70_02 / sprint8 featured",
                "[sprint8, simple, trackApp, virtualActive, heartRate, hdmi, netflix, spotify, youtube, facebook, x, instagram, myFitnessPal, weather, webBrowser, kindle_reader, flipboard, pressReader, thirdPartySolitaire, appStore]",
                appList);

        // test Landmark feature app - adds landmark
        appList = new AppManagerBuilder(FrameConfigEnum.C50)
                .retail().hdmi().noTv()
                .largeScreen()
                .startAppUuid(AppIds.LANDMARKS_UUID)
                .build().getVisibleApps();
        check("retail / C50 climbmill / landmarks featured",
                "[landmarks, simple, virtualActive, heartRate, hdmi, netflix, spotify, youtube, facebook, x, instagram, myFitnessPal, weather, webBrowser, kindle_reader, flipboard, pressReader, thirdPartySolitaire, appStore]",
                appList);

        // test Target Heart Rate feature app - adds targetHeartRate and removes heartRate
        appList = new AppManagerBuilder(FrameConfigEnum.T70_02)
                .retail().hdmi().noTv()
                .largeScreen()
                .startAppUuid(AppIds.TARGET_HEART_RATE_UUID)
                .workoutID(THR_SPEED)
                .build().getVisibleApps();
        check("retail / T70_02 / target heart rate featured",
                "[targetHeartRate, simple, trackApp, virtualActive, hdmi, netflix, spotify, youtube, facebook, x, instagram, myFitnessPal, weather, webBrowser, kindle_reader, flipboard, pressReader, thirdPartySolitaire, appStore]",
                appList);

        // test Constant Watts feature app - adds constantWatts
        appList = new AppManagerBuilder(FrameConfigEnum.R50)
                .retail().hdmi().noTv()
                .largeScreen()
                .startAppUuid(AppIds.CONSTANT_WATTS_UUID)
                .build().getVisibleApps();
        check("retail / R50 / constant watts featured",
                "[constantWatts, simple, trackApp, virtualActive, heartRate, hdmi, netflix, spotify, youtube, facebook, x, instagram, myFitnessPal, weather, webBrowser, kindle_reader, flipboard, pressReader, thirdPartySolitaire, appStore]",
                appList);

        // test CPAT which makes simpleApp a featured app - adds another simple app???
        // NOTE: the simple app is full screen so the user cannot access the workout app screen which has 2 simple apps
//        appList = new AppManagerBuilder(FrameConfigEnum.C50)
//                .retail().hdmi().noTv()
//                .largeScreen()
//                .startAppUuid(AppIds.CPAT_UUID)
//                .build().getVisibleApps();
//        Assert.assertEquals("[simple, virtualActive, heartRate, hdmi, netflix, spotify, youtube, facebook, x, instagram, myFitnessPal, weather, webBrowser, kindle_reader, flipboard, pressReader, thirdPartySolitaire]", appList.toString());

        // test army test - trackApp doesn't becomes the featured app on retail??? Seems like there's no army test on retail
        appList = new AppManagerBuilder(FrameConfigEnum.T70_02)
                .retail().hdmi().noTv()
                .largeScreen()
                .startAppUuid(AppIds.TRACK_UUID)
                .build().getVisibleApps();
        check("retail / T70_02 / track featured (army test)",
                "[simple, trackApp, virtualActive, heartRate, hdmi, netflix, spotify, youtube, facebook, x, instagram, myFitnessPal, weather, webBrowser, kindle_reader, flipboard, pressReader, thirdPartySolitaire, appStore]",
                appList);

        // test disable VA
        appList = new AppManagerBuilder(FrameConfigEnum.T70_02)
                .retail().hdmi().noTv()
                .largeScreen()
                .startAppUuid(AppIds.DEFAULT_APP_UUID)
                .disableVA()
                .build().getVisibleApps();
        check("retail / T70_02 / VA disabled",
                "[simple, trackApp, heartRate, hdmi, netflix, spotify, youtube, facebook, x, instagram, myFitnessPal, weather, webBrowser, kindle_reader, flipboard, pressReader, thirdPartySolitaire, appStore]",
                appList);

        // test VA feature app - VA doesn't becomes the featured app on retail so doesn't move???
        appList = new AppManagerBuilder(FrameConfigEnum.T70_02)
                .retail().hdmi().noTv()
                .largeScreen()
                .startAppUuid(AppIds.VA_UUID)
                .build().getVisibleApps();
        check("retail / T70_02 / VA featured",
                "[simple, trackApp, virtualActive, heartRate, hdmi, netflix, spotify, youtube, facebook, x, instagram, myFitnessPal, weather, webBrowser, kindle_reader, flipboard, pressReader, thirdPartySolitaire, appStore]",
                appList);

        // test no HDMI - removes hdmi
        appList = new AppManagerBuilder(FrameConfigEnum.T70_02)
                .retail().noTv()
                .largeScreen()
                .startAppUuid(AppIds.DEFAULT_APP_UUID)
                .build().getVisibleApps();
        check("retail / T70_02 / no HDMI",
                "[simple, trackApp, virtualActive, heartRate, netflix, spotify, youtube, facebook, x, instagram, myFitnessPal, weather, webBrowser, kindle_reader, flipboard, pressReader, thirdPartySolitaire, appStore]",
                appList);

        // test enable WeChat which is disabled by default
        appList = new AppManagerBuilder(FrameConfigEnum.T70_02)
                .retail().hdmi().noTv()
                .largeScreen()
                .startAppUuid(AppIds.DEFAULT_APP_UUID)
                .enableApp(AppIds.WECHAT_UUID)
                .build().getVisibleApps();
        check("retail / T70_02 / weChat enabled",
                "[simple, trackApp, virtualActive, heartRate, hdmi, netflix, spotify, youtube, facebook, x, instagram, myFitnessPal, weather, webBrowser, kindle_reader, flipboard, pressReader, thirdPartySolitaire, appStore, weChat]",
                appList);

        assertNoMismatches();
    }
}