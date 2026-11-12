package com.jht.chimera.lib.common.conversionlib;

/**
 * Created by chris.foyer on 8/31/2016.
 */
public class StringFormatting
{
    private static final int SECONDS_IN_MINUTE = 60;

    /**
     * Formats seconds to a string in the form mm:ss
     *
     * @param seconds Number of seconds
     * @return Formatted string
     */
    public static String formatSecondsToTimeString(int seconds)
    {
        int secondsRemaining = seconds;
        int minutes = secondsRemaining / SECONDS_IN_MINUTE;
        secondsRemaining = secondsRemaining - (minutes * SECONDS_IN_MINUTE);

        return minutes + ":" + getFormattedTimeString(secondsRemaining);
    }

    /**
     * Formats seconds to a string in the form mm:ss
     *
     * @param seconds Number of seconds
     * @return Formatted string
     */
    public static String formatSecondsToTimeString(float seconds)
    {
        int secondsRemaining = (int) seconds;
        int minutes = secondsRemaining / SECONDS_IN_MINUTE;
        secondsRemaining = secondsRemaining - (minutes * SECONDS_IN_MINUTE);

        return minutes + ":" + getFormattedTimeString(secondsRemaining);
    }

    /**
     * Formats milliseconds to a string in the form mm:ss
     *
     * @param ms Number of milliseconds
     * @return Formatted string
     */
    public static String formatMsToTimeString(long ms)
    {
        int secondsRemaining = (int) (ms / 1000L);
        int minutes = secondsRemaining / SECONDS_IN_MINUTE;
        secondsRemaining = secondsRemaining - (minutes * SECONDS_IN_MINUTE);

        return minutes + ":" + getFormattedTimeString(secondsRemaining);
    }

    public static String getFormattedClock(int hour, int minutes)
    {
        return hour + ":" + getFormattedTimeString(minutes);
    }

    private static String getFormattedTimeString(int minutes)
    {
        String minuteString;
        if (minutes < 10) minuteString = "0" + minutes;
        else minuteString = "" + minutes;
        return minuteString;
    }
}
