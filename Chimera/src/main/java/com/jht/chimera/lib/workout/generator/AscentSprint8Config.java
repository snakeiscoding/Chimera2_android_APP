package com.jht.chimera.lib.workout.generator;

import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.INCLINE;
import static com.jht.chimera.lib.workout.ConsoleWorkoutVariables.RESISTANCE;
import static com.jht.chimera.lib.workout.generator.Sprint8Generator.ASCENT_TYPE;

import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.workoutcontrollerlib.model.DataModel;

import java.util.ArrayList;
import java.util.List;

public class AscentSprint8Config extends BaseSprint8Config
{
    public AscentSprint8Config()
    {
        type = ASCENT_TYPE;
    }

    public void setWarmup(float warmupResistance, float warmupIncline)
    {
        List<DataModel> warmupSet = new ArrayList<>(2);
        warmupSet.add(new DataModel(RESISTANCE, warmupResistance));
        warmupSet.add(new DataModel(ConsoleWorkoutVariables.INCLINE, warmupIncline));
        setWarmupValues(warmupSet);
    }

    public void addInterval(float sprintResistance, float sprintIncline, float recoveryResistance, float recoveryIncline)
    {
        List<DataModel> sprintSets = new ArrayList<>();
        sprintSets.add(new DataModel(RESISTANCE, sprintResistance));
        sprintSets.add(new DataModel(INCLINE, sprintIncline));

        List<DataModel> recoverySets = new ArrayList<>();
        recoverySets.add(new DataModel(RESISTANCE, recoveryResistance));
        recoverySets.add(new DataModel(INCLINE, recoveryIncline));

        Sprint8Interval interval = new Sprint8Interval(sprintSets, recoverySets);
        addSprint8Interval(interval);
    }
}
