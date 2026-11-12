package com.jht.chimera.lib.common.analytic;

import java.util.HashMap;
import java.util.Map;

public class AnalyticStats extends AnalyticsMachineInfo {

    public long totalWorkouts = 0;

    public Map<String, Long> workoutIdMap = new HashMap<>();

    public Map<String, Long> workoutAppClickCountMap = new HashMap<>();
    public Map<String, Long> workoutActivityInfoCountMap = new HashMap<>();
    public Map<String, Long> workoutActivityInfoTimeSecMap = new HashMap<>();
    public long headphoneConnectedCount = 0;
    public long btHeadphoneConnectedCount = 0;
    public long btSinkConnectedCount = 0;
    public long gymkitConnectedCount = 0;
    public long heartRateConnectedCount = 0;
    public long networkConnectedSecCount = 0;
    public Map<String, Long> loginTypeCountMap = new HashMap<>();
    public Map<String, Long> keyPressCountMap = new HashMap<>();

    public long treadmillSafetyKeyCount = 0;
    public long climbmillHandrailCount = 0;
    public long climbmillUCBCount = 0;
    public long climbmillControlZoneCount = 0;
    public long climbmillIRSensorCount = 0;
    public long usbAttachedCount = 0;
    public long userRegistrationCount = 0;
    public long fanClickCount = 0;

    public long ptpLoginCount = 0;
    public long ptpWorkoutCount = 0;
    public long ptpTimingLogin;
    public long ptpTimingUserInfo;
    public long ptpTimingUserConnections;
    public long ptpTimingShowUI;
    public long ptpTimingAPICookie;
    public long ptpTimingAPIUserInfo;
    public long ptpTimingAPIWorkouts;
    public long ptpTimingDisplay;
    public long ptpTimingComplete;
}
