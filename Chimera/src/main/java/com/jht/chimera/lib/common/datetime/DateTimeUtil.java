package com.jht.chimera.lib.common.datetime;

import android.app.AlarmManager;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.i18n.DateFormat;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class DateTimeUtil {
    @NonNls private static final String TAG = "DateTimeUtil";
    public static final long MS_PER_DAY =86400000L;

    private DateTimeUtil() {
        // prevent instantiation
    }

    public static void setTimezone(String timezone) {
        try {
            AlarmManager alarm = (AlarmManager) ContextUtil.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            alarm.setTimeZone(timezone);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public static void setAutoTime(boolean useAutoTime) {
        try {
            Settings.Global.putInt(ContextUtil.getApplicationContext().getContentResolver(), Settings.Global.AUTO_TIME, useAutoTime ? 1 : 0);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public static void setTimeFormat(boolean useTwelveHour) {
        try {
            Settings.System.putString(ContextUtil.getApplicationContext().getContentResolver(), Settings.System.TIME_12_24, useTwelveHour ? "12" : "24");
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public static void setDateFormat(DateFormat dateFormat) {
        try {
            if (dateFormat == DateFormat.MONTH_DAY_YEAR) {
                Settings.System.putString(ContextUtil.getApplicationContext().getContentResolver(), Settings.System.DATE_FORMAT, "MM-dd-yyyy"); //NON-NLS
            } else if (dateFormat == DateFormat.DAY_MONTH_YEAR) {
                Settings.System.putString(ContextUtil.getApplicationContext().getContentResolver(), Settings.System.DATE_FORMAT, "dd-MM-yyyy"); //NON-NLS
            } else if (dateFormat == DateFormat.YEAR_MONTH_DAY) {
                Settings.System.putString(ContextUtil.getApplicationContext().getContentResolver(), Settings.System.DATE_FORMAT, "yyyy-MM-dd"); //NON-NLS
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public static void setDate(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        int hourOfDay = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        c.set(year, month, day, hourOfDay, minute, 0);
        AlarmManager am = (AlarmManager) ContextUtil.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        try {
            am.setTime(c.getTimeInMillis());
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public static void setTime(int hour, int minute) {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        c.set(year, month, day, hour, minute, 0);
        AlarmManager am = (AlarmManager) ContextUtil.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        try {
            am.setTime(c.getTimeInMillis());
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public static int dateToDays(Date date){
        return (int) (date.getTime() / MS_PER_DAY);
    }

    public static int timestampToDays(long timestamp) {
        return (int) (timestamp / MS_PER_DAY);
    }

    public static String getTime(Calendar calendar) {
        return getTime(calendar.getTime());
    }

    public static String getTime(Calendar calendar, JHTCommonTypes.TimeFormat format) {
        return getTime(calendar.getTime(), format == JHTCommonTypes.TimeFormat.Twenty_Four_Hour);
    }

    public static String getTime(java.util.Date date) {
        return getTime(date, DataMatrix.getBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL));
    }

    public static String getTime(java.util.Date date, boolean twentyFourHours) {
        if (twentyFourHours) {
            return new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(date);
        } else {
            return new SimpleDateFormat("hh:mm:ss a", Locale.ENGLISH).format(date);
        }
    }

    public static String getDate(Calendar calendar) {
        return getDate(calendar.getTime());
    }

    public static String getDate(Calendar calendar, JHTCommonTypes.DateFormat format) {
        return getDate(calendar.getTime(), format);
    }

    public static String getDate(java.util.Date date) {
        return getDate(date, JHTCommonTypes.DateFormat.values()[DataMatrix.getInt(SettingKeys.General.DATA_DATE_FORMAT_ENUM_INT)]);
    }

    public static String getDate(java.util.Date date, JHTCommonTypes.DateFormat format) {
        SimpleDateFormat dateFormat;
        switch (format) {

            case YearMonthDay:
                dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                break;
            case MonthDayYear:
                dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                break;
            case DayMonthYear:
                dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                break;
            case DayMonthAbrYear:
                dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                break;
            default:
                return "";
        }
        return dateFormat.format(date);
    }

    public static String getDateTime(Calendar calendar) {
        return getDateTime(calendar.getTime());
    }

    public static String getDateTime(Calendar calendar, JHTCommonTypes.DateFormat dateFormat, JHTCommonTypes.TimeFormat timeFormat) {
        return getDateTime(calendar.getTime(), dateFormat, timeFormat == JHTCommonTypes.TimeFormat.Twenty_Four_Hour);
    }

    public static String getDateTime(java.util.Date date) {
        return getDate(date) + " " + getTime(date);
    }

    public static String getDateTime(java.util.Date date, JHTCommonTypes.DateFormat dateFormat, boolean twentyFourHours) {
        return getDate(date, dateFormat) + " " + getTime(date, twentyFourHours);
    }
}
