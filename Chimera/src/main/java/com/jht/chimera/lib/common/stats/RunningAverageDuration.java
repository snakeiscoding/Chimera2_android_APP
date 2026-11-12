package com.jht.chimera.lib.common.stats;

import androidx.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Queue;

public class RunningAverageDuration {
    private final Queue<Long> durations = new ArrayDeque<>();
    private final int windowSize;
    private Long sum = 0L;

    /**
     * Constructs a new running average duration object.
     * @param windowSize the max number of durations used to calculate the average.
     */
    public RunningAverageDuration(int windowSize){
        this.windowSize = windowSize;
    }

    /**
     * Updates the running average with a newly inputted duration.
     * @param durationMillis the duration (in milliseconds) to add. Minimum duration is 0 milliseconds.
     */
    public void addDuration(long durationMillis){
        // Ignore negative durations
        if (durationMillis < 0) { return; }

        // Prevent unlikely overflow resulting from huge durations or large window size
        // Only runs if adding durationMillis will cause overflow.
        while (sum > (Long.MAX_VALUE - durationMillis)){
            sum -= durations.remove();
        }

        sum += durationMillis;
        durations.add(durationMillis);

        if (durations.size() > windowSize){
            sum -= durations.remove();
        }
    }

    /**
     * Resets the current average duration.
     */
    public void reset(){
        this.durations.clear();
        this.sum = 0L;
    }

    /**
     * Gets the current average duration.
     * @return the current average duration (in milliseconds)
     */
    public Long getAverageDuration(){
        if (durations.size() == 0) return null;
        return sum / durations.size();
    }

    /**
     * Returns a string representation of the current average duration
     * @return the current average duration "HH:mm:ss.SSS"
     */
    @NonNull
    @Override
    public String toString(){
        final long MILLIS_PER_SECOND = 1000;
        final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
        final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;
        
        Long milliseconds = getAverageDuration();
        if (milliseconds == null) return "";

        long hours = milliseconds / MILLIS_PER_HOUR;
        milliseconds -= hours * MILLIS_PER_HOUR;

        long minutes = milliseconds / MILLIS_PER_MINUTE;
        milliseconds -= minutes * MILLIS_PER_MINUTE;

        long seconds = milliseconds / MILLIS_PER_SECOND;
        milliseconds -= seconds * MILLIS_PER_SECOND;

        return String.format(Locale.ENGLISH,"%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
    }
}