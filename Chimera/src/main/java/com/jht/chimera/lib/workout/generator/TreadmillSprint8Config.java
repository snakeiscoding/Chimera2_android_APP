package com.jht.chimera.lib.workout.generator;

import static com.jht.chimera.lib.workout.generator.Sprint8Generator.TREADMILL_TYPE;

import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.workoutcontrollerlib.model.DataModel;

import java.util.ArrayList;
import java.util.List;

public class TreadmillSprint8Config extends BaseSprint8Config
{
    public TreadmillSprint8Config()
    {
        type = TREADMILL_TYPE;
    }

    public void setWarmup(float warmupSpeed, float warmupIncline)
    {
        List<DataModel> warmupSet = new ArrayList<>(2);
        warmupSet.add(new DataModel(ConsoleWorkoutVariables.SPEED, warmupSpeed));
        warmupSet.add(new DataModel(ConsoleWorkoutVariables.INCLINE, warmupIncline));
        setWarmupValues(warmupSet);
    }

    public void addInterval(float sprintSpeed, float sprintIncline, float recoverySpeed, float recoveryIncline)
    {
        List<DataModel> sprintSets = new ArrayList<>();
        sprintSets.add(new DataModel(ConsoleWorkoutVariables.SPEED, sprintSpeed));
        sprintSets.add(new DataModel(ConsoleWorkoutVariables.INCLINE, sprintIncline));

        List<DataModel> recoverySets = new ArrayList<>();
        recoverySets.add(new DataModel(ConsoleWorkoutVariables.SPEED, recoverySpeed));
        recoverySets.add(new DataModel(ConsoleWorkoutVariables.INCLINE, recoveryIncline));

        Sprint8Interval interval = new Sprint8Interval(sprintSets, recoverySets);
        addSprint8Interval(interval);
    }
}
