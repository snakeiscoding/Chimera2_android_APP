package com.jht.chimera.lib.common.config.frames;

import com.jht.chimera.R;
import com.jht.data.RangeInfo;
import com.jht.chimera.lib.common.conversionlib.DistanceConvertUtil;
import com.jht.chimera.lib.language.IntegerLookup;

public class RangeDefaults {

    public RangeInfo age = new RangeInfo();
    public RangeInfo heightCm = new RangeInfo();
    public RangeInfo heightIn = new RangeInfo();
    public RangeInfo constantWatts = new RangeInfo();
    public RangeInfo percentTargetHeartRate = new RangeInfo();
    public RangeInfo targetHeartRate = new RangeInfo();
    public RangeInfo workoutTime = new RangeInfo();
    public RangeInfo workoutCalories = new RangeInfo();
    public RangeInfo workoutDistanceKm = new RangeInfo();
    public RangeInfo workoutDistanceMi = new RangeInfo();
    public RangeInfo workoutFloors = new RangeInfo();

    public RangeDefaults() {

        age.setMin(age.setMinModel(14));
        age.setMax(age.setMaxModel(99));
        age.precision = 0;
        age.stepSize = 1;

        heightCm.setMin(heightCm.setMinModel(DistanceConvertUtil.convertInchesToCm(40)));
        heightCm.setMax(heightCm.setMaxModel(DistanceConvertUtil.convertInchesToCm(90)));
        heightCm.defaultValue = DistanceConvertUtil.convertInchesToCm(70);
        heightCm.precision = 0;
        heightCm.stepSize = 1;


        heightIn.setMin(heightIn.setMinModel(40));
        heightIn.setMax(heightIn.setMaxModel(90));
        heightIn.defaultValue = 70;
        heightIn.precision = 0;
        heightIn.stepSize = 1;

        constantWatts.setMin(constantWatts.setMinModel(15));
        constantWatts.setMax(constantWatts.setMaxModel(525));
        constantWatts.defaultValue = 15;
        constantWatts.precision = 0;
        constantWatts.stepSize = 5;

        percentTargetHeartRate.setMin(percentTargetHeartRate.setMinModel(50));
        percentTargetHeartRate.setMax(percentTargetHeartRate.setMaxModel(90));
        percentTargetHeartRate.defaultValue = 65;
        percentTargetHeartRate.precision = 0;
        percentTargetHeartRate.stepSize = 1;

        targetHeartRate.setMin(targetHeartRate.setMinModel(50));
        targetHeartRate.setMax(targetHeartRate.setMaxModel(202));
        targetHeartRate.defaultValue = 140;
        targetHeartRate.precision = 0;
        targetHeartRate.stepSize = 1;

        workoutTime.setMin(workoutTime.setMinModel(5));
        workoutTime.setMax(workoutTime.setMaxModel(IntegerLookup.get(R.integer.max_workout_time)));
        workoutTime.defaultValue = 30;
        workoutTime.precision = 0;
        workoutTime.stepSize = 1;

        workoutCalories.setMin(workoutCalories.setMinModel(50));
        workoutCalories.setMax(workoutCalories.setMaxModel(10000));
        workoutCalories.defaultValue = 300;
        workoutCalories.precision = 0;
        workoutCalories.stepSize = 1;

        workoutDistanceKm.setMin(workoutDistanceKm.setMinModel(1));
        workoutDistanceKm.setMax(workoutDistanceKm.setMaxModel(50));
        workoutDistanceKm.defaultValue = 1;
        workoutDistanceKm.precision = 1;
        workoutDistanceKm.stepSize = 0.1f;
        workoutDistanceMi.setMin(workoutDistanceMi.setMinModel(0.6f));
        workoutDistanceMi.setMax(workoutDistanceMi.setMaxModel(31.1f));
        workoutDistanceMi.defaultValue = 1;
        workoutDistanceMi.precision = workoutDistanceKm.precision;
        workoutDistanceMi.stepSize = 0.1f;

        workoutFloors.setMin(workoutFloors.setMinModel(5));
        workoutFloors.setMax(workoutFloors.setMaxModel(300));
        workoutFloors.defaultValue = 20;
        workoutFloors.precision = 0;
        workoutFloors.stepSize = 1;
    }

    public void copy(RangeDefaults other) {
        age.copy(other.age);
        heightCm.copy(other.heightCm);
        heightIn.copy(other.heightIn);
        constantWatts.copy(other.constantWatts);
        percentTargetHeartRate.copy(other.percentTargetHeartRate);
        targetHeartRate.copy(other.targetHeartRate);
        workoutTime.copy(other.workoutTime);
        workoutCalories.copy(other.workoutCalories);
        workoutDistanceKm.copy(other.workoutDistanceKm);
        workoutDistanceMi.copy(other.workoutDistanceMi);
        workoutFloors.copy(other.workoutFloors);
    }

}
