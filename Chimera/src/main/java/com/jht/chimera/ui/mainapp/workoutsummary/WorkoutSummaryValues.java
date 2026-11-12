package com.jht.chimera.ui.mainapp.workoutsummary;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.formatting.BasicNumberFormat;
import com.jht.chimera.lib.common.formatting.DistanceFormat;
import com.jht.chimera.lib.common.formatting.FloorFormat;
import com.jht.chimera.lib.common.formatting.SpeedFormat;
import com.jht.chimera.lib.common.formatting.TimeFormat;
import com.jht.chimera.lib.common.stats.WorkoutSummary;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.datamatrixlib.DataMatrix;

import java.util.List;
import java.util.Locale;

/**
 * Formats and provides names for the stats in the workout summary.
 * <p>
 * Created by chris.foyer on 1/8/2018.
 */
@SuppressWarnings("WeakerAccess")
public class WorkoutSummaryValues {
    private static final String TAG = "WorkoutSummaryValues";

    private static final String NO_STAT_STRING = "-";
    public static final int TimeElapsed = 0;
    public static final int TotalCalories = 1;
    public static final int Calories = 2;
    public static final int Distance = 3;
    public static final int AveragePace = 4;
    public static final int Steps = 5;
    public static final int Floors = 6;
    public static final int Elevation = 7;
    public static final int AverageWatts = 8;
    public static final int AverageMets = 9;
    public static final int AverageHeartRate = 10;
    public static final int AverageResistance = 11;
    public static final int AverageSpm = 12;
    public static final int AverageRpm = 13;
    public static final int AverageSpeed = 14;
    public static final int PeakSpeedKph = 15;
    public static final int PeakWatts = 16;
    public static final int PeakHeartRate = 17;
    public static final int PeakIncline = 18;
    public static final int PeakResistance = 19;
    public static final int PeakMets = 20;
    public static final int PeakRpm = 21;
    public static final int PeakSpm = 22;
    public static final int S8AverageSweatScore = 23;
    public static final int S8PeakSweatScore = 24;
    public static final int S8TotalSweatScore = 25;
    public static final int VO2 = 26;
    public static final int VO2Percentile = 27;
    public static final int MaxVO2 = 28;
    public static final int ActiveCalories = 29;
    public static final int AverageIncline = 30;

    private SparseArray<SummaryValueInfo> mStatGetters = new SparseArray<>();

    private Context mContext;

    public WorkoutSummaryValues(Context context) {
        mContext = context;
        setupGetters();
    }

    /**
     * Gets the value in the stored workout summary.
     *
     * @param statId The stat to get the value for
     * @return The formatted string value to display for that stat
     */
    public SummaryValueInfo getInfo(int statId) {
        return mStatGetters.get(statId);
    }

    public abstract class SummaryValueInfo {
        public abstract String getName();
        public abstract String getStat(WorkoutSummary summary);
        public abstract int getRoundedPrecision(); // Gets the precision used for rounding, -1 means the value is not rounded

        /**
         * Gets the unit to add after the stat value
         * @return The unit to add after the stat value
         */
        public String getStatUnit() {
            return null;
        }
    }

    private void setupGetters() {
        mStatGetters.append(TimeElapsed, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(TimeElapsed), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                if (summary.statsAccumulated.secondsStateRun == 0) return NO_STAT_STRING;
                else return TimeFormat.formatTimeSeconds((int)summary.statsAccumulated.secondsStateRun);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });

        mStatGetters.append(ActiveCalories, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(ActiveCalories), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                return FloorFormat.format(summary.statsAccumulated.activeCalories, 0);
            }

            @Override
            public int getRoundedPrecision() {
                return 0;
            }
        });
        mStatGetters.append(TotalCalories, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(TotalCalories), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                return FloorFormat.format(summary.statsAccumulated.calories, 0);
            }

            @Override
            public int getRoundedPrecision() {
                return 0;
            }
        });
        mStatGetters.append(Calories, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(Calories), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                return FloorFormat.format(summary.statsAccumulated.calories, 0);
            }

            @Override
            public int getRoundedPrecision() {
                return 0;
            }
        });
        mStatGetters.append(Distance, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(Distance), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                return DistanceFormat.formatWorkoutDistance(summary.statsAccumulated.distanceKm);
            }

            @Override
            public int getRoundedPrecision() {
                return DataMatrix.getInt(SettingKeys.Experimental.DATA_DISTANCE_PRECISION_INT);
            }

            @Override
            public String getStatUnit() {
                return mContext.getResources().getString(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL) ? R.string.km_lc : R.string.mi_lc);
            }
        });
        mStatGetters.append(Elevation, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(Elevation), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                return DistanceFormat.formatElevation(summary.statsAccumulated.elevationMeters);
            }

            @Override
            public int getRoundedPrecision() {
                return 0;
            }

            @Override
            public String getStatUnit() {
                return mContext.getResources().getString(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL) ? R.string.m_lc : R.string.ft_lc);
            }
        });
        mStatGetters.append(AveragePace, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(AveragePace), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                return SpeedFormat.formatPace(summary.statsAverages.averageSpeedKph);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(Steps, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(Steps), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                return FloorFormat.format(summary.statsAccumulated.steps, 0);
            }

            @Override
            public int getRoundedPrecision() {
                return 0;
            }
        });
        mStatGetters.append(Floors, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(Floors), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                return FloorFormat.format(summary.statsAccumulated.floors, 0);
            }

            @Override
            public int getRoundedPrecision() {
                return 0;
            }
        });
        mStatGetters.append(AverageWatts, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(AverageWatts), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                return FloorFormat.format(summary.statsAverages.averageWatts, 0);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(AverageMets, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(AverageMets), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                return FloorFormat.format(summary.statsAverages.averageMets, 2);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(AverageHeartRate, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(AverageHeartRate), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                if (summary.statsAverages.averageHeartRate == 0) return "--";
                float avgHR = summary.statsAverages.averageHeartRate;
                return FloorFormat.formatAvgHr(summary.statsAverages.averageHeartRate, 0) + " " + mContext.getResources().getString(R.string.bpm_lc);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(AverageResistance, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(AverageResistance), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                return FloorFormat.format(summary.statsAverages.averageResistance, 0);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(AverageSpm, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(AverageSpm), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                return FloorFormat.format(summary.statsAverages.averageStepsPerMinute, 0);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(AverageRpm, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(AverageRpm), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                return FloorFormat.format(summary.statsAverages.averageRpm, 0);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(AverageSpeed, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(AverageSpeed), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                return SpeedFormat.formatSpeedToUserSetting(summary.statsAverages.averageSpeedKph);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }

            @Override
            public String getStatUnit() {
                return mContext.getResources().getString(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL) ? R.string.kph_lc : R.string.mph_lc);
            }
        });
        mStatGetters.append(PeakSpeedKph, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(PeakSpeedKph), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                String units = mContext.getResources().getString(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL) ? R.string.kph_lc : R.string.mph_lc);
                return String.format(Locale.ENGLISH, "%s %s", SpeedFormat.formatSpeedToUserSetting(summary.statsPeaks.peakSpeedKph), units);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(PeakWatts, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(PeakWatts), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                return FloorFormat.format(summary.statsPeaks.peakWatts, 0);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(PeakHeartRate, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(PeakHeartRate), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                if (summary.statsPeaks.peakHeartRate == 0) return "--";
                return summary.statsPeaks.peakHeartRate + " " + mContext.getResources().getString(R.string.bpm_lc);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(PeakIncline, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(PeakIncline), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
                switch (frameConfig.machineClassType) {
                    case Ascent:
                        return FloorFormat.format(summary.statsPeaks.peakIncline, 0);
                    case Treadmill:
                    default:
                        return FloorFormat.format(summary.statsPeaks.peakIncline, 1);
                }
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(PeakResistance, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(PeakResistance), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                return String.valueOf(summary.statsPeaks.peakResistance);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(PeakMets, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(PeakMets), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                return FloorFormat.format(summary.statsPeaks.peakMets, 2);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(PeakRpm, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(PeakRpm), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                return String.valueOf(summary.statsPeaks.peakRpm);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(PeakSpm, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(PeakSpm), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                return String.valueOf(summary.statsPeaks.peakStepsPerMinute);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(S8AverageSweatScore, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(S8AverageSweatScore), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                List<Float> sweatScores = summary.statsSprint8.sweatScores;
                int totalValue = 0;
                for (Float sweatScore : sweatScores) {
                    totalValue += sweatScore.intValue();
                }

                float sweatScoreAverage = (float)totalValue / (float) sweatScores.size();
                if (sweatScores.size() == 0) sweatScoreAverage = 0;
                return BasicNumberFormat.format(sweatScoreAverage, 0);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(S8PeakSweatScore, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(S8PeakSweatScore), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                List<Float> sweatScores = summary.statsSprint8.sweatScores;
                int topScore = 0;
                for (Float sweatScore : sweatScores) {
                    if (topScore < sweatScore.intValue()) topScore = sweatScore.intValue();
                }

                return String.valueOf(topScore);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(S8TotalSweatScore, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(S8PeakSweatScore), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                List<Float> sweatScores = summary.statsSprint8.sweatScores;
                int totalScore = 0;
                for (Float sweatScore : sweatScores) {
                    int sweatScoreInt = sweatScore.intValue();
                    totalScore += sweatScoreInt;
                }

                return String.valueOf(totalScore);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(VO2, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(VO2), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                float vo2 = DataMatrix.getFloat(WorkoutKeys.DATA_VO2_FLOAT);
                return FloorFormat.format(vo2, 2);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(VO2Percentile, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(VO2Percentile), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                float score = DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_FLOAT);
                return FloorFormat.format(score, 0);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(MaxVO2, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(MaxVO2), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                float score = DataMatrix.getFloat(WorkoutKeys.DATA_VO2_FLOAT);
                return FloorFormat.format(score, 0);
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
        mStatGetters.append(AverageIncline, new SummaryValueInfo() {
            @Override
            public String getName() {
                return StringLookup.getString(getMeasurementResourceId(AverageIncline), mContext);
            }

            @Override
            public String getStat(WorkoutSummary summary) {
                FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
                switch (frameConfig.machineClassType) {
                    case Ascent:
                        return FloorFormat.format(summary.statsAverages.averageIncline, 0);
                    case Treadmill:
                    default:
                        return FloorFormat.format(summary.statsAverages.averageIncline, 1);
                }
            }

            @Override
            public int getRoundedPrecision() {
                return -1;
            }
        });
    }

    public static int getMeasurementResourceId(int statId) {
        int resourceId = 0;

        if (statId == AverageHeartRate) resourceId = R.string.average_heart_rate_uc;
        else if (statId == AverageMets) resourceId = R.string.average_mets_uc;
        else if (statId == AveragePace) resourceId = R.string.average_pace_uc;
        else if (statId == AverageRpm) resourceId = R.string.average_rpm_uc;
        else if (statId == AverageSpeed) resourceId = R.string.average_speed_uc;
        else if (statId == AverageSpm) resourceId = R.string.average_spm_uc;
        else if (statId == AverageWatts) resourceId = R.string.average_watts_uc;
        else if (statId == AverageResistance) resourceId = R.string.average_resistance_uc;
        else if (statId == Calories) resourceId = R.string.calories_uc;
        else if (statId == Distance) resourceId = R.string.distance_uc;
        else if (statId == Elevation) resourceId = R.string.elevation_uc;
        else if (statId == TimeElapsed) resourceId = R.string.time_elapsed_uc;
        else if (statId == TotalCalories) resourceId = R.string.total_calories_uc;
        else if (statId == Steps) resourceId = R.string.total_steps_uc;
        else if (statId == PeakSpeedKph) resourceId = R.string.peak_speed_uc;
        else if (statId == PeakWatts) resourceId = R.string.peak_watts_uc;
        else if (statId == PeakHeartRate) resourceId = R.string.peak_heart_rate_uc;
        else if (statId == PeakIncline) resourceId = R.string.peak_incline_uc;
        else if (statId == PeakResistance) resourceId = R.string.peak_resistance_uc;
        else if (statId == PeakMets) resourceId = R.string.peak_mets_uc;
        else if (statId == PeakRpm) resourceId = R.string.peak_rpm_uc;
        else if (statId == PeakSpm) resourceId = R.string.peak_spm_uc;
        else if (statId == S8AverageSweatScore) resourceId = R.string.s8_average_sweat_score_uc;
        else if (statId == S8PeakSweatScore) resourceId = R.string.s8_peak_sweat_score_uc;
        else if (statId == S8TotalSweatScore) resourceId = R.string.total_uc;
        else if (statId == VO2) resourceId = R.string.vo2_tc;
        else if (statId == VO2Percentile) resourceId = R.string.vo2_percentile_tc;
        else if (statId == MaxVO2) resourceId = R.string.max_vo2_tc;
        else if (statId == ActiveCalories) resourceId = R.string.active_calories_uc;
        else if (statId == Floors) resourceId = R.string.floors_uc;
        else if (statId == AverageIncline) resourceId = R.string.average_incline_uc;
        else Log.e(TAG, "Unknown type of stat!");

        return resourceId;
    }
}
