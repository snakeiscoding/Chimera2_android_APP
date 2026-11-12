package com.jht.chimera.ui.mainapp.goscreen;

import androidx.annotation.NonNull;

import com.jht.chimera.lib.workout.RecordedWorkout;
import com.jht.chimera.lib.workout.WorkoutStartupParams;

import java.util.Calendar;
import java.util.Random;

public class CalendarWorkoutData implements Comparable<CalendarWorkoutData> {
    public Calendar date;
    public int durationSeconds;
    public float distanceKm;
    public float calories;

    public int paceSecondsPerKm; // -1 to mean N/A
    public float speedKph;
    public float heartRate;

    public RecordedWorkout recordedWorkout;
    public WorkoutStartupParams startupParams;

    public CalendarWorkoutData(Calendar date) {
        this.date = date;
    }

    public static CalendarWorkoutData getRandom(long minTs, long maxTs) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(rand(minTs, maxTs));
        CalendarWorkoutData workout = new CalendarWorkoutData(date);
        // Ehhhh
        workout.durationSeconds = rand(15, 60) * 60;
        workout.distanceKm = (rand(50, 200) / 10F);
        workout.calories = (rand(3000, 4000) / 10F);
        workout.paceSecondsPerKm = rand(10, 20) * 60;
        workout.speedKph = (rand(30, 50) / 10F);
        workout.heartRate = rand(100, 200);
        return workout;
    }

    private static long rand(long min, long max) {
        Random r = new Random();
        return ((r.nextLong() & 0x7FFFFFFF) % (max-min)) + min;
    }


    private static int rand(int min, int max) {
        Random r = new Random();
        return r.nextInt(max - min) + min;
    }

    @NonNull
    @Override
    public String toString() {
        return "durationSeconds=" + durationSeconds + " distanceKm=" + distanceKm + " calories=" + calories;
    }

    @Override
    public int compareTo(CalendarWorkoutData o) {
        return date.compareTo(o.date);
    }
}
