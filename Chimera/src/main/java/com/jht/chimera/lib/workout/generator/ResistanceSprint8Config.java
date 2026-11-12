package com.jht.chimera.lib.workout.generator;

import static com.jht.chimera.lib.workout.generator.Sprint8Generator.RESISTANCE_TYPE;

import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.workoutcontrollerlib.model.DataModel;

import java.util.ArrayList;
import java.util.List;

public class ResistanceSprint8Config extends BaseSprint8Config
{
    public ResistanceSprint8Config()
    {
        type = RESISTANCE_TYPE;
    }

    public void setWarmup(float warmupResistance)
    {
        List<DataModel> warmupSet = new ArrayList<>(1);
        warmupSet.add(new DataModel(ConsoleWorkoutVariables.RESISTANCE, warmupResistance));
        setWarmupValues(warmupSet);
    }

    public void addInterval(float sprintResistance, float recoveryResistance)
    {
        List<DataModel> sprintSets = new ArrayList<>();
        sprintSets.add(new DataModel(ConsoleWorkoutVariables.RESISTANCE, sprintResistance));

        List<DataModel> recoverySets = new ArrayList<>();
        recoverySets.add(new DataModel(ConsoleWorkoutVariables.RESISTANCE, recoveryResistance));

        Sprint8Interval interval = new Sprint8Interval(sprintSets, recoverySets);
        addSprint8Interval(interval);
    }
}
