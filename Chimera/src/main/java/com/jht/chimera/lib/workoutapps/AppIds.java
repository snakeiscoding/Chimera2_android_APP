package com.jht.chimera.lib.workoutapps;

// TODO - does this make sense to move here? or is it likely that if you reference these IDs you need AppManager anyway?

import com.jht.progression.MmProgramConstantsKt;

import java.util.UUID;

public final class AppIds {
    /** This ID defers to the default workout app used in absence of launching a specific app */
    public static final String DEFAULT_APP_UUID = (new UUID(0, 0)).toString(); // NIL - should we just use null for no app or that creates other problems?
    /** if workout launched with this ID, it should go through motions of launching app but not actually launch an app */
    public static final String SKIP_APP_UUID          = "00098158-9cd6-4d5e-b0e0-c1909d64a987"; // Replace SKIP_APP_UUID with HIIT_APP_UUID
    public static final String HIIT_APP_UUID          = "67ae3963-bc1a-4914-a0c1-9f7e2962872c";
    public static final String MATRIX_TEST_UUID       = "bce31488-3930-4207-9d8a-687a5ef87a96";
    public static final String HEROS_JOURNEY_UUID     = "e2d78b9f-9c04-4c4f-b673-9a4082260f89";
    public static final String CUSTOM_OVERRIDE_APP_UUID = "392cc99f-9cdc-42d2-82be-08384be8c9a0";
    public static final String EGYM_UUID              = "cec6792f-ff09-4593-a33c-3530fcb3442f";
    public static final String SPRINT8_UUID           = "4a53c8b8-c7fc-429f-913e-076960a6ea0e";
    public static final String LANDMARKS_UUID         = "ba67f41b-6498-438b-a529-b811e5d6f815";
    public static final String TARGET_HEART_RATE_UUID = "082d7e46-fd2f-11e6-bc64-92361f002671";
    public static final String CONSTANT_WATTS_UUID    = "0d102cce-fd2f-11e6-bc64-92361f002671";
    public static final String SIMPLE_UUID            = "0d103174-fd2f-11e6-bc64-92361f002671";
    public static final String PACE_UUID              = "de3fe748-81ae-11e9-9015-27a50ff90bd5";
    public static final String INTERVALS_UUID         = "40adf4ce-81af-11e9-baba-db4e125ec11b";
    public static final String FTP_UUID               = "74b6c048-81af-11e9-a421-2bb237268a89";
    public static final String MAX_HR_TEST_UUID       = "01fb6e86-ef65-11e9-81b4-2a2ae2dbcce4";
    public static final String TRACK_UUID             = "0d10337c-fd2f-11e6-bc64-92361f002671";
    public static final String MILE_HIGH_UUID         = "73d157f5-da1c-4b4b-b05a-45fd62dedc00";
    public static final String VA_UUID                = "0d10353e-fd2f-11e6-bc64-92361f002671";
    public static final String MY_MEDIA_UUID          = "0d104010-fd2f-11e6-bc64-92361f002671";
    public static final String FACEBOOK_UUID          = "0d1043c6-fd2f-11e6-bc64-92361f002671";
    public static final String TREO_UUID              = "d8cd3c40-e517-11ed-b5ea-0242ac120002";
    public static final String X_UUID                 = "0d10456a-fd2f-11e6-bc64-92361f002671";
    public static final String MY_FITNESS_PAL_UUID    = "0d1047ae-fd2f-11e6-bc64-92361f002671";
    public static final String YOUTUBE_UUID           = "0d104a56-fd2f-11e6-bc64-92361f002671";
    public static final String WEATHER_UUID           = "0d104bf0-fd2f-11e6-bc64-92361f002671";
    public static final String HDMI_UUID              = "00c4b718-1b64-4437-92b9-6d33e597fdb4";
    public static final String CASTING_UUID           = "3e714e01-b319-4a94-af2e-18fad6e17005";
    public static final String TV_UUID                = "0d1039bc-fd2f-11e6-bc64-92361f002671";
    public static final String REMOTE_TV_UUID         = "0d103b2e-fd2f-11e6-bc64-92361f002671";
    public static final String WEB_BROWSER_UUID       = "0d105078-fd2f-11e6-bc64-92361f002671";
    public static final String HULU_UUID              = "0d1051fe-fd2f-11e6-bc64-92361f002671";
    public static final String NETFLIX_UUID           = "0d105370-fd2f-11e6-bc64-92361f002671";
    public static final String DISNEY_UUID            = "c6e82ae3-785f-4aa4-8926-3dc136932dbb";
    public static final String FLIPBOARD_UUID         = "638a89dd-f160-4a80-9d47-94055561d2d0"; // did 5bb903e5-6846-4d5f-b1fa-613ad3bd7d7f come from anywhere? it did not match tiers
    public static final String PRESS_READER_UUID      = "e364c038-aecd-461d-bd34-44e79167da8b";
    public static final String SOLITAIRE_UUID         = "5174e220-a44c-450e-ae94-9b76f37f7d90";
    public static final String TPA_SOLITAIRE_UUID     = "a79c2a5e-1983-4380-800d-83c1fda0c04d";
    public static final String KINDLE_UUID            = "a639ae8a-eeb7-4584-beaa-9bfc1209553a";
    public static final String INSTAGRAM_UUID         = "f0a84443-3c06-46e4-9a16-40fc2482c296";
    public static final String SPOTIFY_UUID           = "bfc68d34-2ff2-4161-affd-3732dad3cccb";
    public static final String WECHAT_UUID            = "0f59d410-a2ac-449f-961e-f70e8339560f";
    public static final String FACILITY_CALENDAR_UUID = "0d10385e-fd2f-11e6-bc64-92361f002671";
    public static final String WORKOUT_HISTORY_UUID   = "f43a4c31-a078-4e9f-9976-30182005bb87";
    public static final String SURVEY_MONKEY_UUID     = "cbda1932-853f-11e8-adc0-fa7ae01bbebc";
    public static final String YOUKU_UUID             = "147973a7-d275-4407-a47c-edff2bcfa160";
    public static final String TENCENT_UUID           = "0b27088f-03ed-4ff2-9b2c-c2cfc9d98e5c";
    public static final String IQIYI_UUID             = "a432e2fd-e8aa-451e-a854-a4560f07a553";
    public static final String IFIT_UUID              = "b9380004-56f7-4c48-b35a-e667f1c105eb";
    public static final String NETEASE_CLOUD_MUSIC    = "f9390e5e-e5cd-4976-a24b-9f789db69514";
    public static final String WEIBO_UUID             = "8bd233e8-02b1-43cd-ba02-741b8c7d47a5";
    public static final String YMCA_360_UUID          = "ad2605db-2d3d-42e6-9544-8f6e685bf498";
    public static final String YMCA_360_FEATURED_UUID = "c61cb72c-154d-4810-b53c-3c61e4cec295";
    public static final String PROGRESSION_UUID       = MmProgramConstantsKt.PROGRESSION_APP_UUID;
    public static final String CUSTOM_APP_UUID        = "a46f21eb-6711-4ffb-b63f-a755c9d32a8e";
    public static final String AMAZON_FREEVEE_UUID    = "ccaa4a84-882d-467f-9d72-b9a7ca2ca80e";
    public static final String PLUTO_TV_UUID          = "82181ca9-4062-4011-8444-49ff1fdce9ed";
    public static final String SECA_UUID              = "92018237-a347-4414-9f33-98d7fabb71d4";
    public static final String APP_STORE_UUID         = "34394306-0e4d-49e8-af56-20e9287e3a99";
    public static final String TARGET_TRAINING         = "4580241a-d1f9-4729-8efe-46af52a3b0ba";
    public static final String HYBRID_TD_SWITCH_STRIDE_UUID    = "f8d5c7be-a1e0-414d-b446-a1d13febb502";
    public static final String SPIN_BIKE_INTERVALS_UUID = "fc07d435-4586-4113-8b1e-61f6fae71aba";

}
