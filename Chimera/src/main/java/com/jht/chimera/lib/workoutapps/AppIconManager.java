package com.jht.chimera.lib.workoutapps;

import static com.jht.progression.MmProgramConstantsKt.PROGRESSION_APP_UUID;

import android.content.Context;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.appstore.interfaces.AppStoreAppInfo;
import com.jht.appstore.service.AppStoreConfigManager;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.lib.common.datamatrixkeys.ClubComKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TiersKeys;
import com.jht.chimera.lib.common.datamatrixkeys.VaKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.chimera.service.tiers.PremiumFeature;
import com.jht.chimera.service.tiers.PremiumFeatureSet;
import com.jht.chimera.ui.machinesettings.JHTTypes;
import com.jht.clubcom.interfaces.ClubComApp;
import com.jht.clubcom.interfaces.ClubComOptions;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.enums.WorkoutId;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*
    AppIconManager just manages the icons for apps and the logic associated with that.
    Whether or not an app should be added at all is responsibility of AppManager.
    Conditions under which an icon should be show fall under AppIconManager.
 */
@SuppressWarnings("HardCodedStringLiteral")
public class AppIconManager {
    private static final String TAG = "AppIconManager";

    private enum ICON_TYPE {
        STANDARD,
        SPECIAL // special appears at the start of icon order, usually used to get back into a started workout app
    }

    // Base app list.
    private final HashMap<ICON_TYPE, List<AppIcon>> mIconTypeMap = new HashMap<>();

    // A list of apps with their UUID
    private final HashMap<String, AppIcon> mAllIcons = new HashMap<>();

    // The saved ordering of the standard apps
    private List<AppIcon> mAppOrder = null;

    // The current list of hidden apps
    private List<AppIcon> mHiddenApps = null;

    public static int APPS_PER_PAGE = 18;
    private static final String APP_NAME = "app_name";

    private DataMatrix mDataMatrix;
    private boolean mHasAssetManagement;
    private boolean mHasWorkoutTracking;

    boolean isCommercial = ProductConfig.isCommercial();
    boolean isRetail = ProductConfig.isRetail();
    boolean isLightCommercial = ProductConfig.isLightCommercial();

    private static class SingletonHolder {
        private static final AppIconManager Instance = new AppIconManager();
    }

    public static AppIconManager getInstance() {
        return AppIconManager.SingletonHolder.Instance;
    }

    private AppIconManager() {
        APPS_PER_PAGE = ProductConfig.getConsoleType().appGridColCount * ProductConfig.getConsoleType().appGridRowCount;
        initApps();
    }

    public void reset() {
        initApps();
    }

    /**
     * Initializes the icons and some logic for all console apps. App feature UUIDs can be found at
     * https://jhtnaee.atlassian.net/wiki/spaces/PFTS/pages/121370905/Tiered+Services+Web+to+Console+API
     */
    private void initApps() {
        final Context context = ContextUtil.getApplicationContext();
        if (context == null) {
            Log.e(TAG, "Error getting context!");
            return;
        }
        mIconTypeMap.clear();
        mAllIcons.clear();

        final ICON_TYPE standardForCommercial = isCommercial ? ICON_TYPE.STANDARD : ICON_TYPE.SPECIAL;

        mDataMatrix = new DataMatrix();
        mDataMatrix.createBoolAdapter(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL, value -> mHasAssetManagement = value);
        mDataMatrix.createBoolAdapter(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL, value -> mHasWorkoutTracking = value);

        for (ICON_TYPE appType : ICON_TYPE.values()) {
            mIconTypeMap.put(appType, new ArrayList<>());
        }

        AppManager.getInstance().doForApp(AppIds.EGYM_UUID, info -> {
            //egym - uuid was generated since it was not available
            AppIcon egym = new AppIcon("egym", "eGym", R.drawable.egym_btn, info);
            egym.setAppShownLogic(showIfStartingApp);
            addApp(egym, ICON_TYPE.SPECIAL);
        });

        AppManager.getInstance().doForApp(AppIds.SPRINT8_UUID, info -> {
            AppIcon sprint8 = new AppIcon("sprint8", StringLookup.getString(R.string.sprint8_tc, context), R.drawable.ic_app_button_runscreen_sp8, info);
            sprint8.setAppShownLogic(showIfStartingApp);
            addApp(sprint8, ICON_TYPE.SPECIAL);
        });

        AppManager.getInstance().doForApp(AppIds.LANDMARKS_UUID, info -> {
            AppIcon landmarks = new AppIcon("landmarks", StringLookup.getString(R.string.landmarks_tc, context), R.drawable.ic_app_button_runscreen_landmarks, info);
            landmarks.setAppShownLogic(showIfStartingApp);
            addApp(landmarks, ICON_TYPE.SPECIAL);
        });

        AppManager.getInstance().doForApp(AppIds.TARGET_HEART_RATE_UUID, info -> {
            //Target Heart Rate
            AppIcon targetHr = new AppIcon("targetHeartRate", StringLookup.getString(R.string.target_heart_rate_tc, context), R.drawable.ic_app_button_runscreen_hr, info);
            targetHr.setAppShownLogic((appInfo, config, workoutId, startingAppId) -> WorkoutIds.isTargetHeartRate(workoutId));
            addApp(targetHr, ICON_TYPE.SPECIAL);
        });

        AppManager.getInstance().doForApp(AppIds.CONSTANT_WATTS_UUID, info -> {
            //Constant Watts
            AppIcon constantWatts = new AppIcon("constantWatts", StringLookup.getString(R.string.constant_watts_tc, context), R.drawable.ic_app_button_runscreen_watts, info);
            constantWatts.setAppShownLogic(showIfStartingApp);
            addApp(constantWatts, ICON_TYPE.SPECIAL);
        });

        AppManager.getInstance().doForApp(AppIds.SIMPLE_UUID, info -> {
            //Simple
            AppIcon simpleApp = new AppIcon("simple", StringLookup.getString(R.string.simple_tc, context), R.drawable.ic_app_button_runscreen_simple, info);
            //simpleApp.setAppShownLogic((appInfo, config, workoutId, startingAppId) -> isCommercial || WorkoutIds.CPAT_FITNESS_TEST != workoutId);
            simpleApp.setAppShownLogic(showIfNotStartingApp);
            addApp(simpleApp, standardForCommercial);

            // Provide special for simple for CPAT instead of trying to get fancy with it
            AppIcon simpleAppSpecial = new AppIcon("simple", StringLookup.getString(R.string.simple_tc, context), R.drawable.ic_app_button_runscreen_simple, info);
            simpleAppSpecial.setAppShownLogic(showIfStartingApp);
            addApp(simpleAppSpecial, ICON_TYPE.SPECIAL);
        });

        AppManager.getInstance().doForApp(PROGRESSION_APP_UUID, info -> {
            AppIcon progressionSpecial = new AppIcon("progression",
                    StringLookup.getString(R.string.matrix_method_tc, context),
                    R.drawable.ic_app_button_runscreen_matrix_method, info);
            progressionSpecial.setAppShownLogic(showIfStartingApp);
            addApp(progressionSpecial, ICON_TYPE.SPECIAL);
        });

        // if CPAT is just launching simple app
        /*AppManager.getInstance().doForApp(AppIds.CPAT_UUID, info -> {
            //Simple app special icon (only for CPAT workout)
            AppIcon simpleAppSpecial = new AppIcon("simple", StringLookup.getString(R.string.simple_tc, context), R.drawable.ic_app_button_runscreen_simple, info);
            simpleAppSpecial.setAppShownLogic(showIfStartingApp);
            addApp(simpleAppSpecial, ICON_TYPE.SPECIAL);
        });*/

        //region /////////////////////   ATHENA  /////////////////////

        AppManager.getInstance().doForApp(AppIds.TARGET_TRAINING, info -> {
           AppIcon targetTraining = new AppIcon("targetTraining", StringLookup.getString(R.string.target_training_tc, context), R.drawable.ic_app_target_training, info);
           // CGC-25017: spin bike only, it was appearing on every frame type (reported on an ascent). Checked here
           // rather than in AppManager so the app stays launchable and the frame is read live rather than at init.
           targetTraining.setAppShownLogic((appInfo, config, workoutId, startingAppId) ->
                   config.machineClassType == JHTCommonTypes.MachineClassificationType.SpinBike
                           && ((workoutId == WorkoutId.MANUAL.getId()) || appInfo.uuid.equals(startingAppId)));
           addApp(targetTraining, ICON_TYPE.SPECIAL);
        });

        AppManager.getInstance().doForApp(AppIds.PACE_UUID, info -> {
            // Bike Pace
            AppIcon bikePace = new AppIcon("bikepace", StringLookup.getString(R.string.pace_tc, context), R.drawable.ic_app_button_runscreen_pace, info);
            bikePace.setAppShownLogic(showIfStartingApp);
            addApp(bikePace, standardForCommercial);
        });

        AppManager.getInstance().doForApp(AppIds.INTERVALS_UUID, info -> {
            AppIcon bikeIntervals = new AppIcon("bikeintervals", StringLookup.getString(R.string.intervals_tc, context), R.drawable.ic_app_button_runscreen_intervals, info);
            bikeIntervals.setAppShownLogic(showIfStartingApp);
            addApp(bikeIntervals, standardForCommercial);
        });

        AppManager.getInstance().doForApp(AppIds.SPIN_BIKE_INTERVALS_UUID, info -> {
            AppIcon spinBikeIntervals = new AppIcon("spinBikeIntervals", StringLookup.getString(R.string.intervals_tc, context), R.drawable.ic_app_button_runscreen_intervals, info);
            spinBikeIntervals.setAppShownLogic(showIfStartingApp);
            addApp(spinBikeIntervals, standardForCommercial);
        });

        AppManager.getInstance().doForApp(AppIds.FTP_UUID, info -> {
            AppIcon bikeFtp = new AppIcon("bikeftptest", StringLookup.getString(R.string.ftp_watts_tc, context), R.drawable.ic_app_button_runscreen_watts_ftp, info);
            bikeFtp.setAppShownLogic(showIfStartingApp);
            addApp(bikeFtp, ICON_TYPE.SPECIAL);
        });

        AppManager.getInstance().doForApp(AppIds.MAX_HR_TEST_UUID, info -> {
            // Add MaxHRTest app to homescreen
            AppIcon bikeMaxHr = new AppIcon("bikemaxhrtest", StringLookup.getString(R.string.maxhrtest_tc, context), R.drawable.ic_app_button_runscreen_fitness_test, info);
            bikeMaxHr.setAppShownLogic(showIfStartingApp);
            addApp(bikeMaxHr, ICON_TYPE.SPECIAL);
        });

        AppManager.getInstance().doForApp(AppIds.TARGET_HEART_RATE_UUID, info -> {
            //Target Heart Rate
            AppIcon fitnesspark = new AppIcon("fitnesspark_tc", StringLookup.getString(R.string.fitnesspark_tc, context), R.drawable.button_app_fitnesspark, info);
            fitnesspark.setAppShownLogic((appInfo, config, workoutId, startingAppId) -> WorkoutIds.isMigros(workoutId));
            addApp(fitnesspark, ICON_TYPE.SPECIAL);
        });

        //endregion /////////////////////   ATHENA  /////////////////////


        // Note: Multiple icons might be added as "trackApp" which has side effect of the first one "winning" when applying app orders
        AppManager.getInstance().doForApp(AppIds.MILE_HIGH_UUID, info -> {
            AppIcon mile = new AppIcon("trackApp", StringLookup.getString(R.string.mile_high_run_club_tc, context), R.drawable.ic_app_button_mile_high_run_club, info);
            mile.setAppShownLogic(showIfNotStartingApp);
            addApp(mile, standardForCommercial);
        });
        AppManager.getInstance().doForApp(AppIds.TRACK_UUID, info -> {
            AppIcon track = new AppIcon("trackApp", StringLookup.getString(R.string.track_tc, context), R.drawable.ic_app_button_runscreen_track, info);
            track.setAppShownLogic(showIfNotStartingApp);
            addApp(track, standardForCommercial);

            //Track app special icon (for certain fitness tests)
            AppIcon trackSpecial = new AppIcon("trackApp", StringLookup.getString(R.string.track_tc, context), R.drawable.ic_app_button_runscreen_track, info);
            trackSpecial.setAppShownLogic(showIfStartingApp);
            addApp(trackSpecial, ICON_TYPE.SPECIAL);
        });

        AppManager.getInstance().doForApp(AppIds.HIIT_APP_UUID, info -> {
            AppIcon customHIIT = new AppIcon("customHIIT", StringLookup.getString(R.string.hiit_tc, context), R.drawable.ic_app_button_hiit, info);
            customHIIT.setAppShownLogic(showIfStartingApp);
            addApp(customHIIT, ICON_TYPE.SPECIAL);
        });

        AppManager.getInstance().doForApp(AppIds.VA_UUID, info -> {
            //Virtual Active standard icon
            AppIcon vaApp = new AppIcon("virtualActive", StringLookup.getString(R.string.virtual_active_tc, context), R.drawable.ic_app_button_runscreen_va, info);
            vaApp.setAppShownLogic((appInfo, config, workoutId, startingAppId) ->
                    !appInfo.uuid.equals(startingAppId) &&
                            DataMatrix.getBoolean(VaKeys.DATA_VA_CAN_DISPLAY_APP_ICON_BOOL, true)
            );
            addApp(vaApp, standardForCommercial);

            //Virtual Active special icon (for when in a VA workout)
            AppIcon specialVaApp = new AppIcon("virtualActive", StringLookup.getString(R.string.virtual_active_tc, context), R.drawable.ic_app_button_runscreen_va, info);
            specialVaApp.setAppShownLogic((appInfo, config, workoutId, startingAppId) ->
                    appInfo.uuid.equals(startingAppId) &&
                            DataMatrix.getBoolean(VaKeys.DATA_VA_CAN_DISPLAY_APP_ICON_BOOL, true));
            addApp(specialVaApp, ICON_TYPE.SPECIAL);
        });

        AppManager.getInstance().doForApp(AppIds.TARGET_HEART_RATE_UUID, info -> {
            //Heart Rate
            AppIcon heartRate = new AppIcon("heartRate", StringLookup.getString(R.string.heart_rate_tc, context), R.drawable.ic_app_button_runscreen_hr, info);
            heartRate.setAppShownLogic((appInfo, config, workoutId, startingAppId) -> !usesTargetTrainingOnly(config) && ! (WorkoutIds.isMigros(workoutId) || WorkoutIds.isTargetHeartRate(workoutId)));
            addApp(heartRate, standardForCommercial);
        });

        //My Media
        AppManager.getInstance().doForApp(AppIds.MY_MEDIA_UUID, info -> {
            AppIcon myMedia = new AppIcon("myMedia", StringLookup.getString(R.string.my_media_tc, context), R.drawable.ic_app_button_my_media, info);
            addApp(myMedia, ICON_TYPE.STANDARD);
        });

        //Facebook
        AppManager.getInstance().doForApp(AppIds.FACEBOOK_UUID, info -> {
            AppIcon facebook;
            facebook = new AppIcon("facebook", StringLookup.getString(R.string.facebook_tc, context),
                    R.drawable.ic_app_button_facebook, info);
            facebook.setAppShownLogic(showRequireAmOrWt);
            addApp(facebook, ICON_TYPE.STANDARD);
        });

        //Treo
        AppManager.getInstance().doForApp(AppIds.TREO_UUID, info -> {
            AppIcon treo = new AppIcon(
                    "treo",
                    StringLookup.getString(R.string.treo_tc, context),
                    R.drawable.ic_app_button_treo, info
            );
            addApp(treo, ICON_TYPE.STANDARD);
        });

        //Custom Web App
        AppManager.getInstance().doForApp(AppIds.CUSTOM_APP_UUID, info -> {
            AppIcon customApp = new AppIcon("custom_app", DataMatrix.getString(SettingKeys.AssetManagement.DATA_CUSTOM_WEB_APP_NAME_STRING), info);
            customApp.setAppShownLogic(showRequireAmOrWt);
            addApp(customApp, ICON_TYPE.STANDARD);
        });

        //Twitter or "X"
        AppManager.getInstance().doForApp(AppIds.X_UUID, info -> {
            AppIcon twitter;
            twitter = new AppIcon("x", StringLookup.getString(R.string.x_tc, context), R.drawable.ic_app_button_x, info);
            twitter.setAppShownLogic(showRequireAmOrWt);
            addApp(twitter, ICON_TYPE.STANDARD);
        });

        //My Fitness Pal
        AppManager.getInstance().doForApp(AppIds.MY_FITNESS_PAL_UUID, info -> {
            AppIcon myFitnessPal;
            myFitnessPal = new AppIcon("myFitnessPal", StringLookup.getString(R.string.my_fitness_pal_tc, context), R.drawable.ic_app_button_myfitnesspal, info);
            myFitnessPal.setAppShownLogic(showRequireAmOrWt);
            addApp(myFitnessPal, ICON_TYPE.STANDARD);
        });

        //Youtube
        AppManager.getInstance().doForApp(AppIds.YOUTUBE_UUID, info -> {
            AppIcon youtube;
            youtube = new AppIcon("youtube", StringLookup.getString(R.string.youtube_tc, context), R.drawable.ic_app_button_youtube, info);
            youtube.setAppShownLogic(showRequireAmOrWt);
            addApp(youtube, ICON_TYPE.STANDARD);
        });

        //Weather
        AppManager.getInstance().doForApp(AppIds.WEATHER_UUID, info -> {
            AppIcon weather = new AppIcon("weather", StringLookup.getString(R.string.weather_tc, context), R.drawable.ic_app_button_weather, info);
            weather.setAppShownLogic(showRequireAmOrWt);
            addApp(weather, ICON_TYPE.STANDARD);
        });

        //HDMI
        AppManager.getInstance().doForApp(AppIds.HDMI_UUID, info -> {
            AppIcon hdmi = new AppIcon("hdmi", StringLookup.getString(R.string.hdmi_uc, context), R.drawable.ic_app_button_hdmi, info);
            addApp(hdmi, ICON_TYPE.STANDARD);
        });

        //TV
        AppManager.getInstance().doForApp(AppIds.TV_UUID, info -> {
            AppIcon tv = new AppIcon("tv", StringLookup.getString(R.string.tv_uc, context), R.drawable.ic_app_button_tv, info);
            //AppIcon tv = new AppIcon("tv", StringLookup.getString(R.string.tv_uc, context), R.drawable.ic_app_button_tv, "com.jht.test.iptvtest", "com.jht.test.iptvtest.MainActivity", "0d1039bc-fd2f-11e6-bc64-92361f002671");
            addApp(tv, ICON_TYPE.STANDARD);
        });

        //Remote TV
        AppManager.getInstance().doForApp(AppIds.REMOTE_TV_UUID, info -> {
            AppIcon remoteTv = new AppIcon("remoteTv", StringLookup.get(R.string.remote_tv_tc), R.drawable.ic_remote_tv_app_icon, info);
            addApp(remoteTv, ICON_TYPE.STANDARD);
        });

        //Web Browser
        AppManager.getInstance().doForApp(AppIds.WEB_BROWSER_UUID, info -> {
            AppIcon webBrowser = new AppIcon("webBrowser", StringLookup.getString(R.string.web_browser_tc, context), R.drawable.ic_app_button_web_browser, info);
            webBrowser.setAppShownLogic(showRequireAmOrWt);
            webBrowser.setAppIconLogic(() -> {
                boolean useBaidu = DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_USE_BAIDU_BOOL);
                if (useBaidu)
                    return ContextCompat.getDrawable(context, R.drawable.ic_app_button_baidu);
                else
                    return ContextCompat.getDrawable(context, R.drawable.ic_app_button_web_browser);
            });
            webBrowser.setAppNameLogic(() -> {
                boolean useBaidu = DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_USE_BAIDU_BOOL);
                if (useBaidu) return StringLookup.getString(R.string.baidu_tc, context);
                else return StringLookup.getString(R.string.web_browser_tc, context);
            });
            addApp(webBrowser, ICON_TYPE.STANDARD);
        });

        //Hulu
        AppManager.getInstance().doForApp(AppIds.HULU_UUID, info -> {
            AppIcon hulu = new AppIcon("hulu", StringLookup.getString(R.string.hulu_tc, context), R.drawable.ic_app_button_hulu, info);
            hulu.setAppShownLogic(showRequireAmOrWt);
            addApp(hulu, ICON_TYPE.STANDARD);
        });

        //Netflix
        AppManager.getInstance().doForApp(AppIds.NETFLIX_UUID, info -> {
            AppIcon netflix = new AppIcon("netflix", StringLookup.getString(R.string.netflix_tc, context), R.drawable.ic_app_button_netflix, info);
            netflix.setAppShownLogic(showRequireAmOrWt);
            addApp(netflix, ICON_TYPE.STANDARD);
        });

        //Disney plus
        AppManager.getInstance().doForApp(AppIds.DISNEY_UUID, info -> {
            AppIcon disney = new AppIcon("disney", StringLookup.getString(R.string.disney_plus_tc, context), R.drawable.ic_app_button_disney, info);
            disney.setAppShownLogic(showRequireAmOrWt);
            addApp(disney, ICON_TYPE.STANDARD);
        });

        //Flipboard
        AppManager.getInstance().doForApp(AppIds.FLIPBOARD_UUID, info -> {
            AppIcon flipboard = new AppIcon("flipboard", StringLookup.get(R.string.flipboard_tc), R.drawable.ic_app_button_flipboard, info);
            flipboard.setAppShownLogic(showRequireAmOrWt);
            addApp(flipboard, ICON_TYPE.STANDARD);
        });

        //PressReader
        AppManager.getInstance().doForApp(AppIds.PRESS_READER_UUID, info -> {
            AppIcon pressreader = new AppIcon("pressReader", StringLookup.get(R.string.press_reader_tc), R.drawable.ic_app_button_pressreader, info);
            pressreader.setAppShownLogic(showRequireAmOrWt);
            addApp(pressreader, ICON_TYPE.STANDARD);
        });

        //Solitaire
        AppManager.getInstance().doForApp(AppIds.SOLITAIRE_UUID, info -> {
            AppIcon solitaire = new AppIcon("solitaire", StringLookup.get(R.string.matrix_solitaire_app_name_tc), R.drawable.ic_app_button_solitaire_suite, info);
            solitaire.setAppShownLogic(showRequireAmOrWt);
            addApp(solitaire, ICON_TYPE.STANDARD);
        });

        //Switch Stride
        AppManager.getInstance().doForApp(AppIds.HYBRID_TD_SWITCH_STRIDE_UUID, info -> {
            AppIcon switchStride = new AppIcon("switchStride", StringLookup.getString(R.string.switch_stride_tc, context), com.jht.switchStride.R.drawable.ic_matrix_workout_hybrid, info);
            switchStride.setAppShownLogic(showIfStartingApp);
            addApp(switchStride, ICON_TYPE.STANDARD);
        });

        // Third Party Solitaire
        AppManager.getInstance().doForApp(AppIds.TPA_SOLITAIRE_UUID, info -> {
            AppIcon thirdPartySolitaire = new AppIcon("thirdPartySolitaire", StringLookup.get(R.string.cards_tc), R.drawable.ic_app_button_third_party_solitaire_suite, info);
            thirdPartySolitaire.setAppShownLogic(showRequireAmOrWt);
            addApp(thirdPartySolitaire, ICON_TYPE.STANDARD);
        });

        //Kindle
        AppManager.getInstance().doForApp(AppIds.KINDLE_UUID, info -> {
            AppIcon kindle = new AppIcon("kindle_reader", StringLookup.get(R.string.kindle_reader_tc), R.drawable.ic_app_button_kindle, info);
            /* kindle on android currently only works with wifi connection (unless kindle finally fixes it or we find a clever hack)
            kindle.setAppShownLogic((appInfo, config, workoutId, startingAppId) -> {
                boolean ethernetConnected = false;
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivityManager != null) {
                    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    ethernetConnected = activeNetworkInfo != null && activeNetworkInfo.getType() == ConnectivityManager.TYPE_ETHERNET;
                }
                return !ethernetConnected && showRequireAmOrWt.getAppShown(appInfo, config, workoutId, startingAppId);
            });*/
            kindle.setAppShownLogic(showRequireAmOrWt);
            addApp(kindle, ICON_TYPE.STANDARD);
        });

        //Instagram
        AppManager.getInstance().doForApp(AppIds.INSTAGRAM_UUID, info -> {
            AppIcon instagram = new AppIcon("instagram", StringLookup.getString(R.string.instagram_tc, context), R.drawable.ic_app_button_instagram, info);
            instagram.setAppShownLogic(showRequireAmOrWt);
            addApp(instagram, ICON_TYPE.STANDARD);
        });

        //Spotify
        AppManager.getInstance().doForApp(AppIds.SPOTIFY_UUID, info -> {
            AppIcon spotify = new AppIcon("spotify", StringLookup.get(R.string.spotify_tc), R.drawable.ic_app_button_spotify, info);
            spotify.setAppShownLogic(showRequireAmOrWt);
            addApp(spotify, ICON_TYPE.STANDARD);
        });

        //WeChat
        AppManager.getInstance().doForApp(AppIds.WECHAT_UUID, info -> {
            AppIcon wechat = new AppIcon("weChat", StringLookup.getString(R.string.we_chat_tc, context), R.drawable.ic_app_button_wechat, info);
            wechat.setAppShownLogic(showRequireAmOrWt);
            addApp(wechat, ICON_TYPE.STANDARD);
        });
/*
        AppIcon wechat = new AppIcon("weChat", StringLookup.getString(R.string.we_chat_tc, context),
                R.drawable.ic_app_button_wechat, context.getPackageName(), WebAppSSOActivity.class.getCanonicalName(), WECHAT_UUID);
        wechat.urlExtra = "https://web.wechat.com";
        wechat.userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/11.1.1 Safari/605.1.15";
        wechat.specialPanelBehavior = PanelSetBehavior.getNormalPanelRefreshBehavior();
        wechat.supportsSSO = true;
        wechat.setAppShownLogic(mRequireAmOrWt);
        wechat.webAppDomainInclude = "wechat.com";
        wechat.audioInput = AudioSourceInput.Foreground;
        addApp(wechat, APP_TYPE.STANDARD_APP);
*/
        // Facility calendar
        AppManager.getInstance().doForApp(AppIds.FACILITY_CALENDAR_UUID, info -> {
            AppIcon facilityCalendar = new CalendarAppIcon("facilityCalendar", StringLookup.getString(R.string.facility_calendar_tc, context),
                    R.drawable.ic_app_button_facility_calendar, info);
            facilityCalendar.setAppShownLogic((appInfo, config, workoutId, startingAppId) -> mHasAssetManagement);
            addApp(facilityCalendar, ICON_TYPE.STANDARD);
        });

        // Workout history
        AppManager.getInstance().doForApp(AppIds.WORKOUT_HISTORY_UUID, info -> {
            AppIcon workoutHistory = new AppIcon("workoutHistory", StringLookup.getString(R.string.jwtn_tc, context), R.drawable.ic_app_button_wtn, info);
            workoutHistory.setAppShownLogic((appInfo, config, workoutId, startingAppId) -> mHasAssetManagement && !DapiUser.isGuestUser());
            addApp(workoutHistory, ICON_TYPE.STANDARD);
        });


        // SurveyMonkey - Mid Workout App.
        AppManager.getInstance().doForApp(AppIds.SURVEY_MONKEY_UUID, info -> {
            AppIcon surveyMonkey = new AppIcon("survey_monkey", StringLookup.get(R.string.survey_monkey_tc), R.drawable.ic_app_button_survey_pf, info);
            surveyMonkey.setAppShownLogic(showRequireAmOrWt);
            addApp(surveyMonkey, ICON_TYPE.STANDARD);
        });

        // Youku
        /*
        AppIcon youku = new AppIcon("youku", "", R.drawable.ic_app_button_youku_launcher, "com.youku.phone", "", YOUKU_UUID);
        youku.requiresExtraBar = true;
        youku.requiresExtraBarBackBtn = true;
        youku.supportsSSO = true;
        youku.setAppShownLogic(mRequireAmOrWt);
        youku.enabledByDefault = false;
        addApp(youku, APP_TYPE.STANDARD_APP);
*/
        AppManager.getInstance().doForApp(AppIds.YOUKU_UUID, info -> {
            AppIcon youku = new AppIcon("youku", StringLookup.get(R.string.youku_tc), R.drawable.ic_app_button_youku_launcher, info);
            youku.setAppShownLogic(showRequireAmOrWt);
            addApp(youku, ICON_TYPE.STANDARD);
        });

        AppManager.getInstance().doForApp(AppIds.TENCENT_UUID, info -> {
            AppIcon tencent = new AppIcon("tencent", StringLookup.get(R.string.tencent_video_tc), R.drawable.ic_app_button_tencent_video, info);
            tencent.setAppShownLogic(showRequireAmOrWt);
            addApp(tencent, ICON_TYPE.STANDARD);
        });

        // Iqiyi
        /*
        AppIcon iqiyi = new AppIcon("iqiyi", "",
                0, "com.iqiyi.i18n", "", "a432e2fd-e8aa-451e-a854-a4560f07a553");
        iqiyi.requiresExtraBar = true;
        iqiyi.requiresExtraBarBackBtn = true;
        iqiyi.supportsSSO = true;
        iqiyi.setAppShownLogic(mRequireAmOrWt);
        iqiyi.audioInput = AudioSourceInput.Foreground;
        iqiyi.enabledByDefault = false;
        addApp(iqiyi, APP_TYPE.STANDARD_APP);
*/
        AppManager.getInstance().doForApp(AppIds.IQIYI_UUID, info -> {
            AppIcon iqiyi = new AppIcon("iqiyi", StringLookup.get(R.string.iqiyi_tc), R.drawable.ic_app_button_iqiyi, info);
            iqiyi.setAppShownLogic(showRequireAmOrWt);
            addApp(iqiyi, ICON_TYPE.STANDARD);
        });

        AppManager.getInstance().doForApp(AppIds.IFIT_UUID, info -> {
            // iFit as normal app (PRKS-1659 retail should not show it, does not provide a means to back out to workout screen)
            AppIcon ifit = new AppIcon("ifit", StringLookup.get(R.string.ifit_tc), R.drawable.ic_app_button_ifit, info);
            ifit.setAppShownLogic(showIfStartingApp);
            addApp(ifit, ICON_TYPE.SPECIAL);
        });

        // Weibo
        AppManager.getInstance().doForApp(AppIds.WEIBO_UUID, info -> {
            AppIcon weibo = new AppIcon("weibo", StringLookup.get(R.string.weibo_tc), info);
            weibo.setAppShownLogic(showRequireAmOrWt);
            addApp(weibo, ICON_TYPE.STANDARD);
        });

        // Netease Cloud Music
        AppManager.getInstance().doForApp(AppIds.NETEASE_CLOUD_MUSIC, info -> {
            AppIcon netease = new AppIcon("netease", StringLookup.get(R.string.netease_tc), R.drawable.ic_app_button_netease_music, info);
            netease.setAppShownLogic(showRequireAmOrWt);
            addApp(netease, ICON_TYPE.STANDARD);
        });

        // Add clubcom app if any are defined
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_CLUB_COM_BOOL)) {
            try {
                ClubComOptions options = DataMatrix.getParcelable(ClubComKeys.DATA_CLUB_COM_OPTIONS_PARCELABLE, ClubComOptions.class);
                if (options != null && options.getApps() != null) for (ClubComApp app : options.getApps()) {
                    AppManager.getInstance().doForApp(app.getId(), info -> {
                        AppIcon appIcon = new AppIcon(app.getId(), app.getName(), R.drawable.ic_app_button_web_browser, info);
                        addApp(appIcon, ICON_TYPE.STANDARD);
                    });
                }
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }


        // YMCA 360
        AppManager.getInstance().doForApp(AppIds.YMCA_360_FEATURED_UUID, info -> {
            AppIcon ymca360FeaturedWorkout = new AppIcon("ymca360Featured", StringLookup.get(R.string.ymca_360_tc), R.drawable.ic_app_button_ymca360, info);
            ymca360FeaturedWorkout.setAppShownLogic(showIfStartingApp);
            addApp(ymca360FeaturedWorkout, ICON_TYPE.SPECIAL);
        });
        AppManager.getInstance().doForApp(AppIds.YMCA_360_UUID, info -> {
            AppIcon ymca360WorkoutApp = new AppIcon("ymca360", StringLookup.get(R.string.ymca_360_tc), R.drawable.ic_app_button_ymca360, info);
            ymca360WorkoutApp.setAppShownLogic(showIfNotStartingApp);
            addApp(ymca360WorkoutApp, ICON_TYPE.STANDARD);
        });

        AppManager.getInstance().doForApp(AppIds.AMAZON_FREEVEE_UUID, info -> {
            AppIcon amazonFreeveeApp = new AppIcon("amazonFreevee", StringLookup.get(R.string.amazon_freevee_tc), R.drawable.ic_app_button_freevee, info);
            amazonFreeveeApp.setAppShownLogic(showRequireAmOrWt);
            addApp(amazonFreeveeApp, ICON_TYPE.STANDARD);
        });

        AppManager.getInstance().doForApp(AppIds.PLUTO_TV_UUID, info -> {
            AppIcon plutoTVApp = new AppIcon("plutoTV", StringLookup.get(R.string.pluto_tv_tc), R.drawable.ic_app_button_pluto_tv, info);
            plutoTVApp.setAppShownLogic(showRequireAmOrWt);
            addApp(plutoTVApp, ICON_TYPE.STANDARD);
        });

        // <------ MAKE SURE TO KEEP APPSTORE AT THE END OF THE LIST UNTIL WE FIGURE A WAY TO ORDER LIST ------>
        // App Store
        if (ProductConfig.hasAppStore() && DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_APPSTORE_BOOL)) {
            AppManager.getInstance().doForApp(AppIds.APP_STORE_UUID, info -> {
                AppIcon appStore = new AppIcon("appStore", StringLookup.get(R.string.appstore_tc), R.drawable.ic_app_button_appstore, info);
                appStore.setAppShownLogic(showIfNotStartingApp);
                addApp(appStore, ICON_TYPE.STANDARD);
            });

            // App Store - iterate through app list and add it to make sure app icon exist
            List<AppStoreAppInfo> appStoreAppInfoList = AppStoreConfigManager.INSTANCE.getAppList();
            for (AppStoreAppInfo app : appStoreAppInfoList) {
                if (app.getInstalled()) {
                    AppManager.getInstance().doForApp(app.getUuid(), info -> {
                        AppIcon appStore = new AppIcon(app.getName(), app.getName(), 0, info);
                        appStore.setAppShownLogic(showIfNotStartingApp);
                        addApp(appStore, ICON_TYPE.STANDARD);
                    });
                }
            }
        }
        // <------ END OF APP LIST ------>

        mDataMatrix.createStringAdapter(SettingKeys.Application.DATA_APP_INACTIVE_LIST_STRING, this::setHiddenAppsList);
        mDataMatrix.createStringAdapter(SettingKeys.Application.DATA_APP_ORDER_STRING, appOrderJson -> mAppOrder = getAppOrdering(appOrderJson));
        mDataMatrix.createBoolAdapter(SettingKeys.Application.DATA_HULU_PLUS_ENABLED_BOOL, (bool) -> enableApp(bool, "hulu"));
        mDataMatrix.createBoolAdapter(SettingKeys.Application.DATA_DISNEY_PLUS_ENABLED_BOOL, (bool) -> enableApp(bool, "disney"));

        if (DataMatrix.getBoolean(SettingKeys.Application.DATA_HIDE_WEB_AND_SURVEY_APPS_BOOL)) {
            hideWebAndSurvey();
        }

        addNewApps();

        if (!TiersKeys.usingTiers()) {
            for (Map.Entry<String, AppIcon> stringAppIconEntry : mAllIcons.entrySet()) {
                AppIcon icon = (AppIcon) ((Map.Entry) stringAppIconEntry).getValue();
                icon.appInfo.state = JHTTypes.FEATURE_STATE.FEATURE_ENABLED;
            }
        } else {

            if (DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL)) {

                PremiumFeatureSet set = DataMatrix.getParcelable(TiersKeys.DATA_GUEST_FEATURE_SET_PARCELABLE, PremiumFeatureSet.class);
                if (set != null) {

                    if (!set.isEmpty()) {

                        for (PremiumFeature feature : set.getFeatures()) {
                            AppIcon app = mAllIcons.get(feature.id());
                            if (app != null) {
                                app.appInfo.state = feature.getFeatureState();
                            }

                        }
                    }
                } else {
                    Log.e(TAG, "bytes is null");
                }
            } else {
                PremiumFeatureSet set = DataMatrix.getParcelable(TiersKeys.DATA_FEATURE_SET_PARCELABLE, PremiumFeatureSet.class);
                if (set != null) {
                    if (!set.isEmpty()) {
                        for (PremiumFeature feature : set.getFeatures()) {
                            AppIcon app = mAllIcons.get(feature.id());
                            if (app != null) {
                                app.appInfo.state = feature.getFeatureState();
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "bytes is null");
                }
            }
        }
    }

    // Does user facing app name belong to AppInfo or AppIcon? would need to evaluate some of our special name logic? or does different name warrant different app?
    public String findAppName(String uuid) {
        AppInfo appInfo = AppManager.getInstance().getApp(uuid);
        return appInfo == null ? null : findAppName(appInfo);
    }

    public String findAppName(AppInfo appInfo) {
        // use first icon you can find and fall back to formal names?
        AppIcon appIcon = getAppIcon(appInfo.getUuid());
        return appIcon != null ? appIcon.getAppName() : appInfo.getAppPackage() + appInfo.getAppActivity();
    }

    /**
     * We were requested to hide the the web and survey apps by default. This method will hide them
     * after an update to a version with this logic, but will not run again on the same console.
     */
    private void hideWebAndSurvey() {
        DataMatrix.putBoolean(SettingKeys.Application.DATA_HIDE_WEB_AND_SURVEY_APPS_BOOL, false);
        boolean appOrderChanged = false;
        List<AppIcon> appOrderCopy = new ArrayList<>(mAppOrder);
        for (AppIcon appIcon : appOrderCopy) {
            if (AppIds.WEB_BROWSER_UUID.equals(appIcon.appInfo.uuid) || AppIds.SURVEY_MONKEY_UUID.equals(appIcon.appInfo.uuid)) {
                mAppOrder.remove(appIcon);
                mHiddenApps.add(appIcon);
                appOrderChanged = true;
            }
        }
        if (appOrderChanged) {
            saveAppOrdering();
        }
    }

    private void enableApp(boolean enabled, String appId) {
        List<AppIcon> appOrderCopy = new ArrayList<>(mAppOrder);
        boolean appOrderChanged = false;
        for (AppIcon appIcon : appOrderCopy) {
            if (appIcon.iconId.equals(appId)) {
                if (!enabled) {
                    mAppOrder.remove(appIcon);
                    mHiddenApps.remove(appIcon);
                    appOrderChanged = true;
                } else if (!mHiddenApps.contains(appIcon) && !mAppOrder.contains(appIcon)) {
                    mHiddenApps.add(appIcon);
                    appOrderChanged = true;
                }
            }
        }
        if (appOrderChanged) {
            saveAppOrdering();
        }
    }

    private final AppIcon.IAppShown showIfStartingApp = (info, config, workoutId, startingAppId) -> info.uuid.equals(startingAppId);
    private final AppIcon.IAppShown showIfNotStartingApp = (info, config, workoutId, startingAppId) -> !info.uuid.equals(startingAppId);
    private final AppIcon.IAppShown showRequireAmOrWt = (info, config, workoutId, startingAppId) -> {
        // Most third party apps are only allowed when either asset management or workout tracking are enabled
        if (isLightCommercial) {
            return true;
        }
        return mHasAssetManagement || mHasWorkoutTracking;
    };

    /**
     * CGC-25017: spin bikes use the combined target training app only. Just heart rate needs this - the other target
     * apps are gone, but heart rate is shared by every frame type so it cannot be dropped in AppManager.
     */
    private boolean usesTargetTrainingOnly(FrameConfig config) {
        return config.machineClassType == JHTCommonTypes.MachineClassificationType.SpinBike;
    }

    private void setHiddenAppsList(String arrayString) {
        String[] hiddenApps = arrayString.split(",");
        List<AppIcon> apps = new ArrayList<>();
        for (String appString : hiddenApps) {
            AppIcon app = getAppWithName(appString);
            if (app != null && (app.appInfo.experimentalKey == null || DataMatrix.getBoolean(app.appInfo.experimentalKey, false))) {
                apps.add(app);
            }
        }
        mHiddenApps = apps;
    }

    /**
     * Stops the data matrix callbacks in this instance of the app manager
     */
    public void finishAppManager() {
        mDataMatrix.finish();
    }

    static final String[] PARKER_DEFAULT_APP_ORDER = {
            AppIds.SIMPLE_UUID,
            AppIds.TRACK_UUID, AppIds.MILE_HIGH_UUID,
            AppIds.VA_UUID,
            AppIds.TARGET_HEART_RATE_UUID,
            AppIds.MY_MEDIA_UUID,
            AppIds.HDMI_UUID,
            AppIds.NETFLIX_UUID,
            AppIds.HULU_UUID,
            AppIds.DISNEY_UUID,
            AppIds.SPOTIFY_UUID,
            AppIds.YOUTUBE_UUID,
            AppIds.FACEBOOK_UUID,
            AppIds.TREO_UUID,
            AppIds.X_UUID,
            AppIds.INSTAGRAM_UUID,
            AppIds.MY_FITNESS_PAL_UUID,
            AppIds.WEATHER_UUID,
            AppIds.WEB_BROWSER_UUID,
            AppIds.KINDLE_UUID,
            AppIds.FLIPBOARD_UUID,
            AppIds.PRESS_READER_UUID,
            AppIds.SOLITAIRE_UUID,
            AppIds.TPA_SOLITAIRE_UUID,
            AppIds.WECHAT_UUID,
            AppIds.YOUKU_UUID,
            AppIds.WEIBO_UUID,
            AppIds.IQIYI_UUID,
            AppIds.NETEASE_CLOUD_MUSIC,
    };

    static final String[] CHIMERA_DEFAULT_APP_ORDER = {
            AppIds.SIMPLE_UUID,
            AppIds.TRACK_UUID, AppIds.MILE_HIGH_UUID,
            AppIds.TARGET_HEART_RATE_UUID,
            AppIds.MY_MEDIA_UUID,
            AppIds.FACEBOOK_UUID,
            AppIds.X_UUID,
            AppIds.MY_FITNESS_PAL_UUID,
            AppIds.YOUTUBE_UUID,
            AppIds.WEATHER_UUID,
            AppIds.HDMI_UUID,
            AppIds.TV_UUID,
            AppIds.WEB_BROWSER_UUID,
            AppIds.HULU_UUID,
            AppIds.NETFLIX_UUID,
            AppIds.DISNEY_UUID,
            AppIds.FLIPBOARD_UUID,
            AppIds.PRESS_READER_UUID,
            AppIds.SOLITAIRE_UUID,
            AppIds.HYBRID_TD_SWITCH_STRIDE_UUID,
            AppIds.TPA_SOLITAIRE_UUID,
            AppIds.KINDLE_UUID,
            AppIds.INSTAGRAM_UUID,
            AppIds.SPOTIFY_UUID,
            AppIds.WECHAT_UUID,
            AppIds.CUSTOM_APP_UUID,
            AppIds.AMAZON_FREEVEE_UUID,
            AppIds.PLUTO_TV_UUID,
    };

    protected String[] getDefaultAppOrder() {
        return isRetail ? PARKER_DEFAULT_APP_ORDER : CHIMERA_DEFAULT_APP_ORDER;
    }

    private List<AppIcon> createDefaultOrdering() {
        List<AppIcon> defaultAppOrder = new ArrayList<>();
        List<AppIcon> hiddenApps = new ArrayList<>();

        for (String appUuid : getDefaultAppOrder()) {
            AppIcon app = mAllIcons.get(appUuid);
            if (app != null && (app.appInfo.experimentalKey == null || DataMatrix.getBoolean(app.appInfo.experimentalKey, false))) {
                if (app.appInfo.enabledByDefault) {
                    defaultAppOrder.add(app);
                } else {
                    hiddenApps.add(app);
                }
            }
        }

        setAppOrderInDataMatrix(defaultAppOrder);
        updateHiddenApps(hiddenApps);

        return defaultAppOrder;
    }

    private void addNewApps() {
        boolean appOrderChanged = false;
        List<AppIcon> standardApps = mIconTypeMap.get(ICON_TYPE.STANDARD);
        if (standardApps != null) {
            for (AppIcon app : standardApps) {
                if (!appIsInOrderingOrHidden(app) && (app.appInfo.experimentalKey == null || DataMatrix.getBoolean(app.appInfo.experimentalKey, false))) {
                    if (app.appInfo.enabledByDefault) {
                        addAsOrdered(mAppOrder, app); //mAppOrder.add(app);
                        appOrderChanged = true;
                    } else {
                        mHiddenApps.add(app);
                        appOrderChanged = true;
                    }
                }
            }
        }
        if (appOrderChanged) {
            saveAppOrdering();
        }
    }

    private void addAsOrdered(List<AppIcon> list, AppIcon appToAdd) {
        // if adding an app, try to insert it according to the default position
        String[] defaultOrder = getDefaultAppOrder();
        for (int i = 0; i < list.size(); ++i) {
            AppIcon app = list.get(i);
            for (String item : defaultOrder) {
                // did we find the current app in the order list
                if (item.equals(app.getUuid())) {
                    break; // skip to next app in current list
                }
                // did we reach the app we want to add in the order list
                if (item.equals(appToAdd.getUuid())) {
                    list.add(i, appToAdd);
                    return; // made it to the app being added
                }
            }
        }
        list.add(appToAdd); // just add it to the end
    }

    private boolean appIsInOrderingOrHidden(AppIcon appToFind) {
        // Check the ordering
        for (AppIcon app : mAppOrder) {
            if (appToFind.getIconId().equals(app.getIconId())) return true;
        }

        // Check the hidden apps
        for (AppIcon app : mHiddenApps) {
            if (appToFind.getIconId().equals(app.getIconId())) return true;
        }

        return false;
    }

    private List<AppIcon> getAppOrdering(String appOrderJson) {
        List<AppIcon> appOrder = getAppOrderFromDataMatrix(appOrderJson);
        if (appOrder.size() == 0) {
            appOrder = createDefaultOrdering();
        }

        // ignore special apps in case they were previously defined as standard
        if (!isCommercial) {
            List<AppIcon> specialApps = mIconTypeMap.get(ICON_TYPE.SPECIAL);
            Iterator<AppIcon> iterator = appOrder.iterator();
            while (iterator.hasNext()) {
                AppIcon app = iterator.next();
                if (specialApps != null && specialApps.contains(app)) iterator.remove();
            }
        }

        return appOrder;
    }

    /**
     * Gets the matching app for the string ID that is passed in
     *
     * @param iconName The string id of the app
     * @return The matching app icon
     */
    public AppIcon getAppWithName(String iconName) {
        for (ICON_TYPE appType : ICON_TYPE.values()) {
            List<AppIcon> apps = mIconTypeMap.get(appType);
            if (apps != null) {
                for (AppIcon icon : apps) {
                    if (icon.getIconId().equals(iconName)) return icon;
                }
            }
        }
        Log.e(TAG, "Could not find icon " + iconName);
        return null;
    }

    private void addApp(AppIcon app, ICON_TYPE appType) {
        if (app.getUuid() != null && !app.getUuid().isEmpty()) {
            if (!(mAllIcons.containsKey(app.getUuid()) && appType.equals(ICON_TYPE.SPECIAL))) {
                mAllIcons.put(app.getUuid(), app);
            }
        }

        List<AppIcon> apps = mIconTypeMap.get(appType);
        if (apps != null) {
            apps.add(app);
        }
    }

    public List<AppIcon> getAppOrder() {
        return mAppOrder;
    }

    public List<AppIcon> getVisibleApps() {
        List<AppIcon> displayedApps = new ArrayList<>();
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        int workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);
        String startingAppId = DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING);
        String countryCode = DataMatrix.getString(SettingKeys.DATA_IP_LOCATION_IP_OVERRIDE_STRING, "");
        if (countryCode.isEmpty()) countryCode = DataMatrix.getString(SettingKeys.DATA_IP_LOCATION_COUNTRY_CODE_STRING, "");
        String cityName = (ProductConfig.allowWeatherLocationOverride ? DataMatrix.getString(SettingKeys.Weather.DATA_WEATHER_CITY_STRING, "") : null);

        List<AppIcon> specialApps = mIconTypeMap.get(ICON_TYPE.SPECIAL);
        if (specialApps != null) {
            for (AppIcon specialApp : specialApps) {
                if (specialApp.appInfo.isLocationAllowed(countryCode, cityName)) {
                    if (specialApp.isShown(config, workoutId, startingAppId)) {
                        displayedApps.add(specialApp);
                    }
                }
            }
        }

        for (AppIcon app : mAppOrder) {
            if (app != null && app.appInfo.isLocationAllowed(countryCode, cityName)) {
                if (app.isShown(config, workoutId, startingAppId)) {
                    displayedApps.add(app);
                }
            }
        }

        return displayedApps;
    }

    /**
     * Parses and loads the app pages into a usable format from the settings
     *
     * @return The intended app pages
     */
    private List<AppIcon> getAppOrderFromDataMatrix(String appOrderJson) {
        List<AppIcon> appOrder = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(appOrderJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String name = jsonObject.getString(APP_NAME);


                // The JSON is saved in the correct order so we just need to add to the page in order
                AppIcon appIcon = getAppWithName(name);
                if (appIcon != null && (appIcon.appInfo.experimentalKey == null || DataMatrix.getBoolean(appIcon.appInfo.experimentalKey, false))) {
                    appOrder.add(appIcon);
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, e);
        }

        return appOrder;
    }

    /**
     * Saves the order of apps and the hidden apps to what they are currently set to
     */
    public void saveAppOrdering() {
        setAppOrderInDataMatrix(mAppOrder);
        updateHiddenApps(mHiddenApps);
    }

    /**
     * Creates a json string and pushes to the settings based on the provided app ordering and pages.
     *
     * @param appOrder The ordering and pages to for apps
     */
    private void setAppOrderInDataMatrix(List<AppIcon> appOrder) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (AppIcon app : appOrder) {
                JSONObject appJson = new JSONObject();
                appJson.put(APP_NAME, app.iconId);
                jsonArray.put(appJson);
            }

            String jsonString = jsonArray.toString();
            DataMatrix.putString(SettingKeys.Application.DATA_APP_ORDER_STRING, jsonString);
        } catch (JSONException e) {
            Log.e(TAG, "Error encoding app ordering!", e);
        }
    }

    public List<AppIcon> getHiddenApps() {
        return mHiddenApps;
    }

    /**
     * Updates the hidden apps data matrix key and array
     *
     * @param hiddenApps The array to save
     */
    private void updateHiddenApps(List<AppIcon> hiddenApps) {
        setHiddenApps(hiddenApps);
        mHiddenApps = hiddenApps;
    }

    private void setHiddenApps(List<AppIcon> hiddenApps) {
        StringBuilder hiddenAppsString = new StringBuilder();
        for (AppIcon app : hiddenApps) {
            hiddenAppsString.append(app.getIconId()).append(",");
        }
        DataMatrix.putString(SettingKeys.Application.DATA_APP_INACTIVE_LIST_STRING, hiddenAppsString.toString());
    }

    private List<AppIcon> setHiddenApps() {
        String hiddenAppsString = DataMatrix.getString(SettingKeys.Application.DATA_APP_INACTIVE_LIST_STRING);
        String[] hiddenAppsArray = hiddenAppsString.split(",");
        List<AppIcon> hiddenAppIcons = new ArrayList<>();
        for (String value : hiddenAppsArray) {
            AppIcon icon = getAppWithName(value);
            if (icon != null) {
                hiddenAppIcons.add(icon);
            }
        }
        return hiddenAppIcons;
    }

    public Map<String, AppIcon> getAllApps() {
        return mAllIcons;
    }

    /**
     * Gets the apps on the page in the app ordering
     *
     * @param page The page to check
     * @return The apps on the current page
     */
    public List<AppIcon> getAppsOnPage(int page) {
        return getAppsForPage(mAppOrder, page);
    }

    /**
     * Gets the visible apps on the designated page
     *
     * @param page The page to check
     * @return The apps visible on the page
     */
    public List<AppIcon> getAppsForPage(List<AppIcon> apps, int page) {
        int startOfPage = page * AppIconManager.APPS_PER_PAGE;
        int endOfPage = (page + 1) * AppIconManager.APPS_PER_PAGE;
        if (endOfPage > apps.size() - 1) endOfPage = apps.size();

        if (startOfPage < endOfPage) {
            return apps.subList(startOfPage, endOfPage);
        } else {
            return new ArrayList<>();
        }
    }

    // These are apps shown for pre-launching on parker apps section of go screen
    public List<AppIcon> getEntertainmentApps() {
        List<AppIcon> apps = getVisibleApps();
        // remove anything not entertainment
        apps.removeIf(app -> app == null || app.appInfo == null || !app.appInfo.category.equals(AppInfo.Category.Entertainment));
        return apps;
    }

    public AppIcon getAppIcon(String uuid) {
        return mAllIcons.get(uuid);
    }

}
