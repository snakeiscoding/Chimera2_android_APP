package com.jht.chimera.ui.mainapp.goscreen;

import java.util.Calendar;
import java.util.List;

interface CalendarWorkoutDataSource {
    List<CalendarWorkoutData> getWorkoutsInDay(Calendar day);
    Calendar getEarliestWorkoutDate();
    Calendar getLatestWorkoutDate();
}
