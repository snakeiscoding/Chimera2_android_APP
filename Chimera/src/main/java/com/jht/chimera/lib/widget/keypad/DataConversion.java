package com.jht.chimera.lib.widget.keypad;

import android.util.Log;

import com.jht.chimera.lib.common.i18n.ClockFormat;
import com.jht.chimera.lib.common.i18n.DateFormat;

import org.jetbrains.annotations.NonNls;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;


public class DataConversion {
    @NonNls private static final String TAG = "DataConversion";
    public enum DATA_TYPE {
        INTEGER,
        DECIMAL,
        TIME_MINUTE,
        TIME_SECONDS,
        TIME_MINUTE_SECONDS,
        DATE,
    }

    @NonNls public static final String[] DATE_FORMAT_PATTERN =
            {
                    "yyyy/MM/dd",
                    "MM/dd/yyyy",
                    "dd/MM/yyyy",
                    "dd MMM yyyy"
            };

    @NonNls static final String[] CLOCK_FORMAT_PATTERN =
            {
                    "hh:mma",
                    "H:mm"
            };

    @NonNls static final String[] CLOCK_FORMAT_PATTERN_NO_SUFFIX =
            {
                    "h:mm",
                    "H:mm"
            };

    final static int SECONDS_PER_MINUTE = 60;

    static public String timeInSecondToMinuteSecond(double value) {
        final String mTimeFormat = "%d:%02d"; //NON-NLS
        return String.format(Locale.ENGLISH, mTimeFormat,
                (int) (value + 0.5) / SECONDS_PER_MINUTE,
                (int) (value + 0.5) % SECONDS_PER_MINUTE);
    }

    static public String timeInMinuteToMinuteSecond(double value) {
        final String mTimeFormat = "%d:%02d"; //NON-NLS
        return String.format(Locale.ENGLISH, mTimeFormat,
                (int) (value + 0.5), 0);
    }

    // Convert mm:ss to seconds
    static public double timeInMinuteSecondToDouble(String value) {
        int indexColon = value.indexOf(":");
        double minutes = 0.0;
        double seconds = 0.0;

        if ((-1 < indexColon) && (value.length() - 1 > indexColon)) {
            minutes = Double.parseDouble(value.substring(0, indexColon));
            seconds = Double.parseDouble(value.substring(indexColon + 1, value.length()));
        }

        return minutes * 60.0 + seconds;
    }

    static public String calendarDateToString(Calendar calendarDate, String displayFormat) {
        SimpleDateFormat format1 = new SimpleDateFormat(displayFormat, Locale.ENGLISH);
        return format1.format(calendarDate.getTime());
    }

    static public Calendar stringToCalendarDate(String date, String displayFormat) {
        SimpleDateFormat format1 = new SimpleDateFormat(displayFormat, Locale.ENGLISH);
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(format1.parse(date));
        } catch (ParseException e) {
            Log.w(TAG, e);
        }
        return cal;
    }

    static public String getDateFormatPattern(DateFormat value) {
        return DATE_FORMAT_PATTERN[value.ordinal()];
    }

    static public String getClockFormatPattern(ClockFormat value) {
        return CLOCK_FORMAT_PATTERN[value.ordinal()];
    }

    static public String getClockFormatPatternNoSuffix(ClockFormat value) {
        return CLOCK_FORMAT_PATTERN_NO_SUFFIX[value.ordinal()];
    }


    static public String getDateTimePattern(DateFormat date, ClockFormat clock) {
        return getDateFormatPattern(date) + " " + getClockFormatPattern(clock);
    }

    static public int computeAge(Calendar birthdate) {
        Calendar today = Calendar.getInstance();

        if (birthdate.after(today)) {
            throw new IllegalArgumentException("Can't be born in the future");
        }
        int currentYear = today.get(Calendar.YEAR);
        int birthYear = birthdate.get(Calendar.YEAR);
        int age = currentYear - birthYear;
        int currentMonth = today.get(Calendar.MONTH);
        int birthMonth = birthdate.get(Calendar.MONTH);
        if (birthMonth > currentMonth) {
            age--;
        } else if (currentMonth == birthMonth) {
            int currentDay = today.get(Calendar.DAY_OF_MONTH);
            int birthDay = birthdate.get(Calendar.DAY_OF_MONTH);
            if (birthDay > currentDay) {
                age--;
            }
        }

        return age;
    }
}
