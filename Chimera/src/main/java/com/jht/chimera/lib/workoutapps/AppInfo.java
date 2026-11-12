package com.jht.chimera.lib.workoutapps;

import androidx.annotation.Nullable;

import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.workout.WorkoutInput;
import com.jht.chimera.service.ui.panels.PanelBehavior;
import com.jht.chimera.ui.machinesettings.JHTTypes;
import com.jht.data.intent.IntentExtra;
import com.jht.workoutcontrollerlib.workoutdefinition.statements.SettingValue;

import org.jetbrains.annotations.NonNls;

import java.util.List;


public class AppInfo {
    public enum Category {
        Workout,    // 1st party workout ui (has a workout selection associated) - simple, track, target, constant watts
        Entertainment,  // 3rd party non-workout - netflix, facebook (can be shown outside a workout, retail go screen)
        Integration,    // 3rd party integration - ifit, ymca
        FullIntegration, // 3rd party integration that owns the full user flow incl. post-workout (hero's journey, custom override) - system should not idle-timeout back to splash
        System, // internal systems apps - basically the "change" icons
    }
    public Category category = Category.Workout;

    /** app bypasses normal workout UI launch flow (basically app does all the launch UI except for panels) */
    public boolean bypassWorkoutUiProcess = false;
    /** uses 3rd party interface version of workout panels (such as no home button on top bar) */
    public boolean useThirdPartyInterface = false;

    // third party interfaces have can some overrides (could just do string/float pair for what we use these for)
    public SettingValue[] settingOverrides = null;
    public WorkoutInput[] workoutInputs = null;

    /** Events for old P1/Onyx LCD system until we can explore newer system */
    public String uiEventName = null;

    // initial attempt to get workout end actions as meta data rather than specific app checks?
    // maybe this should be nested class to better organize? or does that make serialization harder later?
    public enum WorkoutEndAction { // could be work summary bool but what if we need more granularity than that?
        NoWorkoutSummary, // at workout end, IDLE panels and launch workoutEndActivity if it is defined (is it always Integration apps? can we just check that?)
        WorkoutSummary, // at workout end, SUMMARY panels and launch workoutEndActivity or default summary if undefined
    }
    /** Indicates what action to take at the end of the workout. Default is to launch a workout summary with summary panel state. */
    public WorkoutEndAction workoutEndAction = WorkoutEndAction.WorkoutSummary;
    /** If defined, the given activity should be launched at the end of the workout. */
    public String workoutEndActivity = null; // null will use default workout summary if action is summary, otherwise won't launch anything
    /** If true, indicates that the safety PIN should be required before the next workout. */
    public boolean workoutEndResetSafetyPin = true; // normally at end of workout pin needs re-entering but some integrations do their own thing

    /** if true, this app requires the LG 750E tuner to be connected before it can be launched */
    public boolean tunerDependent = false;

    /** unique internal identifier for an app */
    public String uuid;
    /** fully qualified app package as used by android, usually either ours or a third party */
    public String appPackage;
    /** if app is internal, this is the fully qualified activity name */
    public String appActivity;
    /** array of permission strings needed by the app */
    public String[] permissions = null;
    /** specify audio source for sound mixer */
    public AudioSourceInput audioInput = AudioSourceInput.Default;
    /** if defined, overrides workout panel behavior */
    public PanelBehavior specialPanelBehavior = null;
    public boolean supportsSSO;
    /**
     * Can be parsed from XML when using Tiers. DISABLED is used on GoScreenActivity with getWorkoutStateMap to
     * hide/show. FEATURED_HIDDEN used with isShown and Tiers. FEATURE_LIMITED used when parsed from XML with Tiers.
     */
    public JHTTypes.FEATURE_STATE state; // used by Tiers to enable/disable app
    /** Used to determine default order, and all apps to screen when init in workout. */
    public boolean enabledByDefault = true; // determines if the app needs to be manually add to list in MES
    /** if true, app should be launched full screen */
    public boolean launchesFullscreen = false;
    /** some app might required a delayed kill like iFit so make sure the app is killed before next launch */
    public boolean requiresPrelaunchKill = false; // ifit is not killed right away so it can upload data (might not be needed anymore since we do not exist ifit at workout end anymore)
    //public boolean clearDataOnKill = false; // does not seem to used anywhere anymore?
    public boolean suppressCoolDownControls = false; // TODO - is this app data or workout data or panel data?

    // is more about the icon than the app?
    /** Some apps can be enabled/disabled explicitly in settings. This seems to be parallel to and overriding of MES app order editing. */
    public String experimentalKey = null; // probably need another way to do this, maybe just a list of disabled uuids we can maintain for getVisibleApps?

    /** if defined, limits app to certain country codes */
    @NonNls
    public String[] allowedCountyCodes = null;
    /** if defined, limits app to certain weather locations */
    @NonNls
    public String[] allowedWeatherLocations = null;

    /** URL to use for web apps */
    public String urlExtra;
    /** if defined, overrides the user agent for a web app (like hiding we are a mobile device) */
    public String userAgent = "";
    /** if defined, web app should only block domains containing strings in the semicolon delimited list */
    public String webAppDomainBan = "";
    /** if defined, web app should only allow domains containing strings in the semicolon delimited list */
    public String webAppDomainInclude = "";
    /** If true, clears the web view cache periodically */
    public boolean clearWebViewCachePeriodically = false;

    /**
     * If defined, these extras are copied into the app's start intent at launch. Declared as data rather
     * than a Bundle so app meta data stays serializable/comparable - see {@link IntentExtra}.
     * Extras that are known dynamic values will be resolved by the app launch code.
     */
    @Nullable public List<IntentExtra> startExtras = null;

    /** Use this value to ignore app analytics. */
    public static final int IGNORE_APP_ANALYTICS_ID = 0;

    /** ID number used to app usage analytics. Be sure check/update web documentation for values. */
    public final int appAnalyticsId;

    AppInfo(String appPackage, String appActivity, String uuid, int appAnalyticsId) {
        this.appPackage = appPackage;
        this.appActivity = appActivity;
        this.uuid = uuid;
        this.appAnalyticsId = appAnalyticsId;
    }

    // do we want to use accessors or just public fields?
    public String getAppPackage() {
        return appPackage;
    }
    public String getAppActivity() {
        return appActivity;
    }
    public String getUrl() {
        return urlExtra;
    }
    public String getUuid() {
        return uuid;
    }
    public int getAppAnalyticsId() {
        return appAnalyticsId;
    }

    // if weather override is not allowed, simply pass NULL for weatherCityName (empty string is unknown default and therefore still checked)
    public boolean isLocationAllowed(String countryCode, String weatherCityName) {
        boolean isAllowed = true; // default is no location restriction
        if (allowedCountyCodes != null) {
            for (String code : allowedCountyCodes) {
                if (code.equals(countryCode)) return true;
            }
            isAllowed = false;
        }
        if (allowedWeatherLocations != null && weatherCityName != null) {
            for (String location : allowedWeatherLocations) {
                if (weatherCityName.endsWith(location)) return true;
            }
            isAllowed = false;
        }
        return isAllowed;
    }
}
