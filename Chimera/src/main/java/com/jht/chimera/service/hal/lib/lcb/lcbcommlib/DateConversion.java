package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateConversion {
    private static final String TAG = "DateConversion";
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
}
