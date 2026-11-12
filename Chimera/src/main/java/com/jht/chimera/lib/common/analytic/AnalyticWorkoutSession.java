package com.jht.chimera.lib.common.analytic;

import java.util.HashMap;
import java.util.Map;

public class AnalyticWorkoutSession extends AnalyticsMachineInfo {

    public long workoutId = 0;
    public long workoutStartTimeSec = 0; // should match dapi workout tracking

    public Map<String, Long> workoutActivityInfoTimeSecMap = new HashMap<>();
    public String loginType = "guest"; //NON-NLS
}
