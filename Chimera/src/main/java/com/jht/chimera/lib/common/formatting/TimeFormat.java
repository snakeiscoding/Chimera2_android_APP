package com.jht.chimera.lib.common.formatting;

import com.jht.chimera.lib.common.datamatrixkeys.ImportExportKeys;
import com.jht.datamatrixlib.DataMatrix;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Formats integer times that are passed into this class into correct time formats for the stat bar
 * <p>
 * Created by chris.foyer on 8/23/2017.
 */
public class TimeFormat
{
    /**
     * Gets the formatted time string for a certain number of milliseconds.
     *
     * @param seconds The number of milliseconds
     * @return The time string
     */
    public static String formatTimeSeconds(int seconds)
    {
        return String.format(Locale.ENGLISH, "%d:%02d", seconds / 60, seconds % 60);
    }


    /**
     * Gets the formatted time string for a certain number of milliseconds.
     *
     * @param milliseconds The number of milliseconds
     * @return The time string
     */
    public static String formatTime(int milliseconds)
    {
        return formatTime((float) milliseconds);
    }

    /**
     * Gets the time string for a certain number of milliseconds
     *
     * @param milliseconds The number of milliseconds
     * @return The time string
     */
    public static String formatTime(float milliseconds)
    {
        int seconds = Math.round(milliseconds / 1000f);
        return String.format(Locale.ENGLISH, "%d:%02d", seconds / 60, seconds % 60);
    }


    /**
     * Rounds half values down rather than up to provide the correct time remaining value.
     * @param milliseconds The milliseconds remaining.
     * @return The formatted time remaining
     */
    public static String formatTimeRemaining(int milliseconds)
    {
        int millisecondsRounded;
        int msRemainder = milliseconds % 1000;
        if (msRemainder <= 500) millisecondsRounded = milliseconds - msRemainder;
        else millisecondsRounded = milliseconds + 1000 - msRemainder;
        int seconds = millisecondsRounded / 1000;
        return String.format(Locale.ENGLISH, "%d:%02d", seconds / 60, seconds % 60);
    }

    /**
     * Rounds half values down rather than up to provide the correct time remaining value.
     * @param milliseconds The milliseconds remaining.
     * @return The formatted time remaining
     */
    public static String formatTimeRemaining(float milliseconds)
    {
        return formatTimeRemaining(Math.round(milliseconds));
    }

    public final static SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    public static String formatDate(Calendar c) {
        return dateFormat.format(c.getTime());
    }

    public final static SimpleDateFormat formatTime12h = new SimpleDateFormat("h:mm a", Locale.getDefault());
    public final static SimpleDateFormat formatTime24h = new SimpleDateFormat("HH:mm", Locale.getDefault());
    public static String formatTimeOfDay(Calendar c) {
        if (DataMatrix.getBoolean(ImportExportKeys.DATA_CURRENT_CLOCK_FORMAT_IS_24_HOURS_BOOL))
            return formatTime24h.format(c.getTime());
        return formatTime12h.format(c.getTime());
    }

    public static final SimpleDateFormat ISO8601Z = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    static TimeZone UTC_TZ = TimeZone.getTimeZone("UTC"); //NON-NLS
    static {
        ISO8601Z.setTimeZone(UTC_TZ);
    }
    public static final String formatDateTimeIso8601Z(Date date) {
        return ISO8601Z.format(date);
    }

    // TODO - is there a better java way?
    public static String formatSeconds(int secs) {
        // sec
        int s = secs % 60;
        // min
        secs /= 60;
        int m = secs % 60;
        // hr
        int h = secs / 60;
        return String.format("%02d",h) + ":" + String.format("%02d",m) + ":" + String.format("%02d",s);
    }

    public static String formatHoursMinutes(float secs) {
        int mins = Math.round(secs / 60);
        return String.format(Locale.ENGLISH, "%02d:%02d", mins / 60, mins % 60);
    }
}
