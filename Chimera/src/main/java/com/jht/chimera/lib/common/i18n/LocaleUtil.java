package com.jht.chimera.lib.common.i18n;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.IBinder;
import android.os.LocaleList;
import android.provider.Settings;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.ImportExportKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

public class LocaleUtil {
    @NonNls private static final String TAG = "LocaleUtil";

    private LocaleUtil() {
        // prevent instantiation.
    }

    public static void changeLanguageAndUpdateDefault(Context context, LanguageProperty languageProperty) {
        changeLanguage(context, languageProperty, true);
    }

    /**
     * Units a fresh (guest) user should start with: the operator's configured default, which they
     * set independently of the language - unless the exerciser has switched the console to another
     * language, in which case that language's default units win.
     */
    public static boolean getDefaultUnitIsMetric() {
        int selectedLanguage = DataMatrix.getInt(SettingKeys.General.DATA_SELECTED_LANGUAGE_ENUM_INT);
        int consoleLanguage = DataMatrix.getInt(SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT);
        if (selectedLanguage != consoleLanguage) {
            LanguageProperty selected = LanguageProperty.getLanguagePropertyFromIndex(selectedLanguage);
            if (selected != null) return selected.defaultUnits == Unit.METRIC;
        }
        return DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL);
    }

    public static void changeLanguage(Context context, LanguageProperty languageProperty, boolean updateDefault) {
        try {
            Locale locale = getLocaleFromString(languageProperty.locale);
            Class<?> ActivityManagerNative = Class.forName("android.app.ActivityManagerNative");
            Class<?> IActivityManager = Class.forName("android.app.IActivityManager");

            // update the locale
            Method getDefault = ActivityManagerNative.getMethod("getDefault");
            Object am = IActivityManager.cast(getDefault.invoke(ActivityManagerNative));
            if (am != null) {
                Method getConfiguration = am.getClass().getMethod("getConfiguration");
                Configuration systemConfig = (Configuration) getConfiguration.invoke(am);

                if (locale != null) {
                    // update the keyboard before the locale change
                    changeKeyboard(context, locale.getLanguage() + "_" + locale.getCountry());
                }

                systemConfig.setLocale(locale);

                LocaleList localeList = new LocaleList(locale);
                LocaleList.setDefault(localeList);
                systemConfig.setLocales(localeList);
                Field userSetLocale = Configuration.class.getField("userSetLocale");
                userSetLocale.setAccessible(true);
                userSetLocale.set(systemConfig, true);

                Method updatePersistentConfiguration = am.getClass().getMethod("updatePersistentConfiguration", Configuration.class);
                updatePersistentConfiguration.invoke(am, systemConfig);

                DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, languageProperty.defaultUnits == Unit.METRIC);
                DataMatrix.putBoolean(ImportExportKeys.DATA_CURRENT_CLOCK_FORMAT_IS_24_HOURS_BOOL, languageProperty.timeFormat.equals(JHTCommonTypes.TimeFormat.Twenty_Four_Hour));
                DataMatrix.putInt(ImportExportKeys.DATA_CURRENT_DATE_FORMAT_INT, languageProperty.dateFormat.ordinal());
                if (!ProductConfig.hasUserProfiles()) {
                    // TODO - don't think changeLanguage() should be altering the current user, seems like this should be somewhere else?
                    if (DapiUser.isGuestUser()) {
                        DataMatrix.putString(DAPIKeys.DATA_USER_FIRST_NAME_STRING, context.getString(R.string.guest_tc));
                    }
                }
                if (updateDefault) {
                    DataMatrix.putBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL, languageProperty.defaultUnits == Unit.METRIC);
                    DataMatrix.putInt(SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT, languageProperty.ordinal());
                    DataMatrix.putInt(SettingKeys.General.DATA_DATE_FORMAT_ENUM_INT, languageProperty.dateFormat.ordinal());
                    DataMatrix.putBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL, languageProperty.timeFormat.equals(JHTCommonTypes.TimeFormat.Twenty_Four_Hour));
                }
                DataMatrix.putInt(SettingKeys.General.DATA_SELECTED_LANGUAGE_ENUM_INT, languageProperty.ordinal());
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    private static Locale getLocaleFromString(String locale) {
        String[] list = locale.split("_");
        if (list.length == 1) {
            return new Locale(locale);
        } else if (list.length == 2) {
            return new Locale(list[0], list[1]);
        }
        return null;
    }

    public static void changeKeyboard(Context context, LanguageProperty lp) {
        Locale locale = getLocaleFromString(lp.locale);
        if (locale != null) {
            changeKeyboard(context, locale.getLanguage() + "_" + locale.getCountry());
        }
    }

    @NonNls private static final String LATIN_IME_ID        = "com.android.inputmethod.latin/.LatinIME";
    @NonNls private static final String ZHUYIN_IME_ID        = "com.jht.googlecode.tcime.unofficial/com.googlecode.tcime.unofficial.ZhuyinIME";
    @NonNls private static final String PINYIN_IME_ID        = "com.jht.inputmethod.pinyin/com.android.inputmethod.pinyin.PinyinIME";
    @NonNls private static final String OPENWNN_JAJP_IME_ID  = "com.johnsonfit.jp.co.omronsoft.openwnn/jp.co.omronsoft.openwnn.OpenWnnJAJP";
    @NonNls private static final String HELIBOARD_IME_ID     = "helium314.keyboard/.latin.LatinIME";

    public static void changeKeyboard(Context context, String locale) {
        try {
            InputMethodManager mgr = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            String imeId = resolveImeId(locale);
            if (imeId != null) {
                setDefaultInputMethod(context, mgr, imeId);
            } else if (!locale.isEmpty()) {
                applyLatinSubtypeForLocale(context, mgr, locale);
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    private static String resolveImeId(String locale) {
        String lower = locale.toLowerCase(Locale.ENGLISH);
        if (lower.contains("zh_tw")) {
            return ZHUYIN_IME_ID;
        } else if (lower.contains("zh")) {
            return PINYIN_IME_ID;
        } else if (lower.contains("ja")) {
            return OPENWNN_JAJP_IME_ID;
        } else if (lower.contains("ko")) {
            return HELIBOARD_IME_ID;
        }
        return null;
    }

    // Switching the active IME requires a different API depending on SDK level: writing directly to Settings.
    private static void setDefaultInputMethod(Context context, InputMethodManager mgr, String imeId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Settings.Secure.putString(context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD, imeId);
        } else {
            invokeSetInputMethod(mgr, imeId);
        }
    }


    private static void invokeSetInputMethod(InputMethodManager mgr, String imeId) {
        try {
            Method method = InputMethodManager.class.getMethod("setInputMethod", IBinder.class, String.class); //NON-NLS
            method.invoke(mgr, null, imeId);
        } catch (InvocationTargetException e) {
            Log.w(TAG, e.getCause());
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    private static void invokeSetInputMethodAndSubtype(InputMethodManager mgr, String imeId, InputMethodSubtype subtype) {
        try {
            Method method = InputMethodManager.class.getMethod("setInputMethodAndSubtype", IBinder.class, String.class, InputMethodSubtype.class); //NON-NLS
            method.invoke(mgr, null, imeId, subtype);
        } catch (InvocationTargetException e) {
            Log.w(TAG, e.getCause());
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    // All other (Latin-script) languages are subtypes of the single LatinIME app rather than
    // separate IME packages, so this finds and selects the matching subtype instead of switching
    // the whole IME.
    @SuppressWarnings("deprecation")
    private static void applyLatinSubtypeForLocale(Context context, InputMethodManager mgr, String locale) {
        setDefaultInputMethod(context, mgr, LATIN_IME_ID);

        InputMethodInfo latinIme = findLatinIme(mgr);
        if (latinIme == null) {
            Log.w(TAG, "LatinIME not found, cannot apply subtype for locale: " + locale);
            return;
        }

        String lowerLocale = locale.toLowerCase(Locale.ENGLISH);

        // First pass: match by full locale string
        for (int i = 0; i < latinIme.getSubtypeCount(); i++) {
            InputMethodSubtype subtype = latinIme.getSubtypeAt(i);
            String subtypeLocale = subtype.getLocale().toLowerCase(Locale.ENGLISH);
            if (subtypeLocale.isEmpty()) {
                continue; // skip automatic/no-locale subtypes to avoid false matches
            }
            if (lowerLocale.contains(subtypeLocale)) {
                applyInputMethodSubtype(context, mgr, subtype);
                return;
            }
        }

        // Second pass: match by language code only, for cases where the exact country variant isn't defined.
        String localeLanguage = getLanguageCode(lowerLocale);
        for (int i = 0; i < latinIme.getSubtypeCount(); i++) {
            InputMethodSubtype subtype = latinIme.getSubtypeAt(i);
            String subtypeLocale = subtype.getLocale().toLowerCase(Locale.ENGLISH);
            if (subtypeLocale.isEmpty()) {
                continue;
            }
            if (getLanguageCode(subtypeLocale).equals(localeLanguage)) {
                applyInputMethodSubtype(context, mgr, subtype);
                return;
            }
        }

        // No match found in either pass - fall back to English US.
        // The cy_GB (Cymraeg) and so_SO (Soomaali) that have no subtype
        // defined in LatinIME on the consoles(Android 9, 11, 14).
        for (int i = 0; i < latinIme.getSubtypeCount(); i++) {
            InputMethodSubtype subtype = latinIme.getSubtypeAt(i);
            if ("en_US".equalsIgnoreCase(subtype.getLocale())) {
                Log.w(TAG, "No matching subtype found for locale: " + locale + ", falling back to en_US");
                applyInputMethodSubtype(context, mgr, subtype);
                return;
            }
        }
    }

    private static InputMethodInfo findLatinIme(InputMethodManager mgr) {
        for (InputMethodInfo info : mgr.getInputMethodList()) {
            if (info.getId().equalsIgnoreCase(LATIN_IME_ID)) {
                return info;
            }
        }
        return null;
    }

    // Returns the language code portion of a locale string (e.g. "en" from "en_us").
    private static String getLanguageCode(String locale) {
        int underscore = locale.indexOf('_');
        return underscore >= 0 ? locale.substring(0, underscore) : locale;
    }

    // changes which language/layout within the currently active and tells LatinIME which of its internal subtypes to use
    private static void applyInputMethodSubtype(Context context, InputMethodManager mgr, InputMethodSubtype subtype) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.SELECTED_INPUT_METHOD_SUBTYPE, subtype.hashCode());
        } else {
            invokeSetInputMethodAndSubtype(mgr, LATIN_IME_ID, subtype);
        }
    }
}