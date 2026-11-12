package com.jht.chimera.lib.common.i18n;

import android.util.Log;

import androidx.annotation.NonNull;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.BuildConfig;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum LanguageProperty {

    English("en_US", Unit.IMPERIAL, DateFormat.MONTH_DAY_YEAR, JHTCommonTypes.TimeFormat.Twelve_Hour),
    English_GB("en_GB", Unit.METRIC, DateFormat.DAY_MONTH_YEAR, JHTCommonTypes.TimeFormat.Twelve_Hour),
    English_AU("en_AU", Unit.METRIC, DateFormat.DAY_MONTH_YEAR, JHTCommonTypes.TimeFormat.Twelve_Hour),
    German("de_DE", Unit.METRIC, DateFormat.DAY_MONTH_YEAR, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    French("fr_FR", Unit.METRIC, DateFormat.DAY_MONTH_YEAR, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Italian("it_IT", Unit.METRIC, DateFormat.DAY_MONTH_YEAR, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Spanish("es_ES", Unit.METRIC, DateFormat.DAY_MONTH_YEAR, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Dutch("nl_NL", Unit.METRIC, DateFormat.DAY_MONTH_YEAR, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Portuguese("pt_PT", Unit.METRIC, DateFormat.DAY_MONTH_YEAR, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Korean("ko_KR", Unit.METRIC, DateFormat.YEAR_MONTH_DAY, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Arabic("ar_AE", Unit.METRIC, DateFormat.DAY_MONTH_YEAR, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Swedish("sv_SE", Unit.METRIC, DateFormat.YEAR_MONTH_DAY, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Russian("ru_RU", Unit.METRIC, DateFormat.YEAR_MONTH_DAY, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Finnish("fi_FI", Unit.METRIC, DateFormat.DAY_MONTH_YEAR, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Chinese_CN("zh_CN", Unit.METRIC, DateFormat.DAY_MONTH_YEAR, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Chinese_TW("zh_TW", Unit.METRIC, DateFormat.DAY_MONTH_YEAR, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Japanese("ja_JP", Unit.METRIC, DateFormat.DAY_MONTH_YEAR, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Turkish("tr_TR", Unit.METRIC, DateFormat.DAY_MONTH_YEAR, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Polish("pl_PL", Unit.METRIC, DateFormat.DAY_MONTH_YEAR, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Welsh("cy_GB", Unit.METRIC, DateFormat.DAY_MONTH_YEAR, JHTCommonTypes.TimeFormat.Twelve_Hour),
    Basque("eu_EU", Unit.METRIC, DateFormat.YEAR_MONTH_DAY, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Vietnamese("vi_VI", Unit.METRIC, DateFormat.YEAR_MONTH_DAY, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Somali("so_SO", Unit.METRIC, DateFormat.YEAR_MONTH_DAY, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Danish("da_DK", Unit.METRIC, DateFormat.YEAR_MONTH_DAY, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Thai("th_TH", Unit.METRIC, DateFormat.YEAR_MONTH_DAY, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Malay("ms_MY", Unit.METRIC, DateFormat.YEAR_MONTH_DAY, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Catalan("ca_ES", Unit.METRIC, DateFormat.YEAR_MONTH_DAY, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Greek("el_GR", Unit.METRIC, DateFormat.YEAR_MONTH_DAY, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Slovak("sk_SK", Unit.METRIC, DateFormat.YEAR_MONTH_DAY, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Czech("cs_CZ", Unit.METRIC, DateFormat.YEAR_MONTH_DAY, JHTCommonTypes.TimeFormat.Twenty_Four_Hour),
    Hebrew("iw_IL", Unit.METRIC, DateFormat.DAY_MONTH_YEAR, JHTCommonTypes.TimeFormat.Twenty_Four_Hour), // note old android pre-API-24 used "iw" instead of "he" (some of our consoles seem to prefer "iw")

    //Expecting the debug language to be last in the enum list
    DebugLongestString("gr_ZZ", Unit.METRIC, DateFormat.YEAR_MONTH_DAY, JHTCommonTypes.TimeFormat.Twenty_Four_Hour);

    private static final String TAG = "LanguageProperty";
    public final String locale;
    public final Unit defaultUnits;
    public final DateFormat dateFormat;
    public final JHTCommonTypes.TimeFormat timeFormat;

    private static final LanguageProperty[] sLanguageList;

    static {
        // only add the longest string id to DEBUG builds
        LanguageProperty[] list = LanguageProperty.values();
        if (BuildConfig.DEBUG) {
            sLanguageList = list;
        } else {
            sLanguageList = Arrays.copyOf(list, list.length - 1);
        }
    }

    LanguageProperty(@NonNls String locale, Unit defaultUnits, DateFormat dateFormat, JHTCommonTypes.TimeFormat timeFormat) {
        this.locale = locale;
        this.defaultUnits = defaultUnits;
        this.dateFormat = dateFormat;
        this.timeFormat = timeFormat;
    }

    public static List<String> getSpinnerList() {
        List<String> list = new ArrayList<>();
        for (LanguageProperty languageProperty : sLanguageList) {
            String[] language = languageProperty.locale.split("_");
            Locale locale = new Locale(language[0], language[1]);
            // Due to China not recognizing Taiwan as a country we cannot display the country for
            // chinese thus the hack below.
            switch (languageProperty.locale) {
                case "zh_CN":
                    list.add("中文 - 简体"); //NON-NLS
                    break;
                case "zh_TW":
                    list.add("中文 - 繁體"); //NON-NLS
                    break;
                case "gr_ZZ":
                    list.add("Longest String (DEBUG)"); //NON-NLS
                    break;
                default:
                    list.add(locale.getDisplayLanguage(locale) + " - " + locale.getDisplayCountry(locale));
                    break;
            }
        }
        return list;
    }

    public static LanguageProperty getLanguagePropertyFromIndex(int languageIndex) {
        LanguageProperty languageProperty = null;
        try {
            languageProperty = sLanguageList[languageIndex];
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return languageProperty;
    }

    public String getLanguageAcronym() {
        return locale.substring(0, 2);
    }

    @NonNull
    @Override
    public String toString() {
        String retVal = "";
        retVal += "locale: " + locale + ", ";
        retVal += "defaultUnits: " + defaultUnits;
        return retVal;
    }
}
