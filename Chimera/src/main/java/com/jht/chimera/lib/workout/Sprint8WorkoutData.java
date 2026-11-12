package com.jht.chimera.lib.workout;

import androidx.annotation.NonNull;

import com.jht.chimera.ui.mainapp.goscreen.WorkoutParameter;
import com.jht.data.sprint8.ISprint8WorkoutData;

import java.util.List;

/**
 * Sprint8 interval data for one workout parameter (speed, incline, resistance).
 *
 * Created by AlexN on 1/19/2019. Moved out of the legacy Sprint8 setup package, which is gone - the
 * workout manager and the workout catalog model still exchange Sprint8 data in this shape.
 */
public class Sprint8WorkoutData implements ISprint8WorkoutData {

    public WorkoutParameter workoutParameter;
    public List<List<Float>> intervalData;

    public Sprint8WorkoutData(WorkoutParameter workoutParameter, List<List<Float>> intervalData) {
        this.intervalData = intervalData;
        this.workoutParameter = workoutParameter;
    }

    @NonNull
    @Override
    public List<List<Float>> getIntervalData() {
        return intervalData;
    }
}
