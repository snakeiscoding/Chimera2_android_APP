package com.jht.chimera.lib.common.formatting;

import androidx.test.platform.app.InstrumentationRegistry;

import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.stats.WorkoutSummary;
import com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.Default;
import com.jht.chimera.ui.mainapp.workoutsummary.WorkoutSummaryValues;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.gattserver2.WatchState;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Locale;

public class StatFormatTest {

    private WorkoutSummary summary;
    private WorkoutSummaryValues summaryValues;
    private boolean displayIsMetric;
    private String currentFrame;

    /**
     * Grab the machine state for restoring at the end of the test and initialize the stat classes.
     */
    @Before
    public void setup() {

        displayIsMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
        currentFrame = DataMatrix.getString(SettingKeys.General.DATA_FRAME_TYPE_STRING);
        Assert.assertNotNull(InstrumentationRegistry.getInstrumentation().getTargetContext());
        summary = new WorkoutSummary();
        summaryValues = new WorkoutSummaryValues(InstrumentationRegistry.getInstrumentation().getTargetContext());
        connectSamsungWatch(false);

    }

    /**
     * Restore the machine state.
     */
    @After
    public void tearDown() {
        connectSamsungWatch(false);
        DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, displayIsMetric);
        if(currentFrame != null && !currentFrame.equals(DataMatrix.getString(SettingKeys.General.DATA_FRAME_TYPE_STRING))) {
            FrameConfigManager.getInstance().setFrame(FrameConfigEnum.getFrameTypeFromString(currentFrame));

        }
    }

    /**
     * Test out the pace.
     */
    @Test @Emulator @Default @Chimera
    public void testPace() {
        connectSamsungWatch(true);

        Assert.assertEquals("99:32", SpeedFormat.formatPace(0.97f, false));
        Assert.assertEquals("85:27", SpeedFormat.formatPace(1.13f, false));
        Assert.assertEquals("74:51", SpeedFormat.formatPace(1.29f, false));
        Assert.assertEquals("66:35", SpeedFormat.formatPace(1.45f, false));
        Assert.assertEquals("59:58", SpeedFormat.formatPace(1.61f, false));
        Assert.assertEquals("54:14", SpeedFormat.formatPace(1.78f, false));
        Assert.assertEquals("49:46", SpeedFormat.formatPace(1.94f, false));
        Assert.assertEquals("45:58", SpeedFormat.formatPace(2.10f, false));
        Assert.assertEquals("42:43", SpeedFormat.formatPace(2.26f, false));
        Assert.assertEquals("39:54", SpeedFormat.formatPace(2.42f, false));


        Assert.assertEquals("74:04", SpeedFormat.formatPace(0.81f, true));
        Assert.assertEquals("61:51", SpeedFormat.formatPace(0.97f, true));
        Assert.assertEquals("53:05", SpeedFormat.formatPace(1.13f, true));
        Assert.assertEquals("46:30", SpeedFormat.formatPace(1.29f, true));
        Assert.assertEquals("41:22", SpeedFormat.formatPace(1.45f, true));
        Assert.assertEquals("37:16", SpeedFormat.formatPace(1.61f, true));
        Assert.assertEquals("33:42", SpeedFormat.formatPace(1.78f, true));
        Assert.assertEquals("30:55", SpeedFormat.formatPace(1.94f, true));
        Assert.assertEquals("28:34", SpeedFormat.formatPace(2.10f, true));
        Assert.assertEquals("26:32", SpeedFormat.formatPace(2.26f, true));
        Assert.assertEquals("24:47", SpeedFormat.formatPace(2.42f, true));
        connectSamsungWatch(false);
        setFrame(FrameConfigEnum.Treadmill_Performance);
        updateDisplay(true);
        for(int i = 0; i < 100 * 10; i++) {
            float kph = 1.00f + 0.01f * i;
            int seconds = (int)(3600 / (Math.round(kph * 10.0) / 10.0));
            DataMatrix.putFloat(WorkoutKeys.DATA_WORKOUT_SPEED_KPH_FLOAT, kph);
            DataMatrix.putFloat(WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT, kph);
            summary.statsAverages.averageSpeedKph = kph;
            String pace = String.format(Locale.ENGLISH, "%d:%02d", seconds / 60, seconds % 60);
            Assert.assertEquals("Test round " + i + " " + kph, pace, StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.PACE).getCurrentValue());
            Assert.assertEquals("Test round " + i + " " + kph, pace, StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.AVERAGE_PACE).getCurrentValue());
            Assert.assertEquals("Test round " + i + " " + kph, pace, summaryValues.getInfo(WorkoutSummaryValues.AveragePace).getStat(summary));
        }
        updateDisplay(false);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        for(int i = 0; i < 100 * 10; i++) {
            float kph = (1.00f + 0.01f * i) * 1.609344f;
            float mph = kph / 1.609344f;
            int seconds = (int)(3600 / (Math.round(mph * 10.0) / 10.0));
            DataMatrix.putFloat(WorkoutKeys.DATA_WORKOUT_SPEED_KPH_FLOAT, kph);
            DataMatrix.putFloat(WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT, kph);
            summary.statsAverages.averageSpeedKph = kph;
            String pace = String.format(Locale.ENGLISH, "%d:%02d", seconds / 60, seconds % 60);
            Assert.assertEquals("Test round " + i + " " + kph + " " + mph, pace, StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.PACE).getCurrentValue());
            Assert.assertEquals("Test round " + i + " " + kph + " " + mph, pace, StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.AVERAGE_PACE).getCurrentValue());
            Assert.assertEquals("Test round " + i + " " + kph + " " + mph, pace, summaryValues.getInfo(WorkoutSummaryValues.AveragePace).getStat(summary));
        }
        connectSamsungWatch(true);
        updateDisplay(true);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }

        for(int i = 0; i < 1000 * 2; i++) {
            float kph = 1.00f + 0.001f * i;
            int seconds = (int)(3600 / (Math.round(kph * 100.0) / 100.0));
            DataMatrix.putFloat(WorkoutKeys.DATA_WORKOUT_SPEED_KPH_FLOAT, kph);
            DataMatrix.putFloat(WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT, kph);
            summary.statsAverages.averageSpeedKph = kph;
            String pace = String.format(Locale.ENGLISH, "%d:%02d", seconds / 60, seconds % 60);
            Assert.assertEquals("Test round " + i + " " + kph, pace, StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.PACE).getCurrentValue());
            Assert.assertEquals("Test round " + i + " " + kph, pace, StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.AVERAGE_PACE).getCurrentValue());
            Assert.assertEquals("Test round " + i + " " + kph, pace, summaryValues.getInfo(WorkoutSummaryValues.AveragePace).getStat(summary));
        }
        updateDisplay(false);

        for(int i = 0; i < 1000 * 2; i++) {
            float kph = (1.00f + 0.001f * i) * 1.609344f;

            float mphTarget = (float)(Math.round(kph / 1.609344f * 10.0) / 10.0);
            float kphAdj = (float)(Math.ceil(mphTarget * 1.609344f * 100.0) / 100.0);
            float mph = kphAdj / 1.609344f;

            int seconds = (int)(3600 / mph);
            DataMatrix.putFloat(WorkoutKeys.DATA_WORKOUT_SPEED_KPH_FLOAT, kph);
            DataMatrix.putFloat(WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT, kph);
            summary.statsAverages.averageSpeedKph = kph;
            String pace = String.format(Locale.ENGLISH, "%d:%02d", seconds / 60, seconds % 60);
            Assert.assertEquals("Test round " + i + " " + kph + " " + mph, pace, StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.PACE).getCurrentValue());
            Assert.assertEquals("Test round " + i + " " + kph + " " + mph, pace, StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.AVERAGE_PACE).getCurrentValue());
            Assert.assertEquals("Test round " + i + " " + kph + " " + mph, pace, summaryValues.getInfo(WorkoutSummaryValues.AveragePace).getStat(summary));
        }

    }

    @Test @Emulator @Default @Chimera
    public void testCalories() {

        // Note Active and Total calories would require espresso tests to verify UI data.
        DataMatrix.putFloat(WorkoutKeys.DATA_CALORIES_FLOAT, 13.6f);
        Assert.assertEquals("13", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.CALORIES).getCurrentValue());
        DataMatrix.putFloat(WorkoutKeys.DATA_CALORIES_FLOAT, 14.01f);
        Assert.assertEquals("14", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.CALORIES).getCurrentValue());
        DataMatrix.putFloat(WorkoutKeys.DATA_CALORIES_FLOAT, 14.99999f);
        Assert.assertEquals("14", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.CALORIES).getCurrentValue());

        summary.statsAccumulated.activeCalories = 3.6f;
        Assert.assertEquals("3", summaryValues.getInfo(WorkoutSummaryValues.ActiveCalories).getStat(summary));
        summary.statsAccumulated.activeCalories = 4.01f;
        Assert.assertEquals("4", summaryValues.getInfo(WorkoutSummaryValues.ActiveCalories).getStat(summary));
        summary.statsAccumulated.activeCalories = 4.99999f;
        Assert.assertEquals("4", summaryValues.getInfo(WorkoutSummaryValues.ActiveCalories).getStat(summary));

        summary.statsAccumulated.calories = 13.6f;
        Assert.assertEquals("13", summaryValues.getInfo(WorkoutSummaryValues.TotalCalories).getStat(summary));
        summary.statsAccumulated.calories = 14.01f;
        Assert.assertEquals("14", summaryValues.getInfo(WorkoutSummaryValues.TotalCalories).getStat(summary));
        summary.statsAccumulated.calories = 14.99999f;
        Assert.assertEquals("14", summaryValues.getInfo(WorkoutSummaryValues.TotalCalories).getStat(summary));

        summary.statsAccumulated.calories = 23.6f;
        Assert.assertEquals("23", summaryValues.getInfo(WorkoutSummaryValues.Calories).getStat(summary));
        summary.statsAccumulated.calories = 24.01f;
        Assert.assertEquals("24", summaryValues.getInfo(WorkoutSummaryValues.Calories).getStat(summary));
        summary.statsAccumulated.calories = 24.99999f;
        Assert.assertEquals("24", summaryValues.getInfo(WorkoutSummaryValues.Calories).getStat(summary));

    }

    @Test @Emulator @Default @Chimera
    public void testDistance() {
        updateDisplay(true);
        DataMatrix.putFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT, 3.123f);
        Assert.assertEquals("3.12", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.DISTANCE_METRIC).getCurrentValue());
        DataMatrix.putFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT, 3.126f);
        Assert.assertEquals("3.12", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.DISTANCE_METRIC).getCurrentValue());
        DataMatrix.putFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT, 3.1299f);
        Assert.assertEquals("3.12", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.DISTANCE_METRIC).getCurrentValue());

        updateDisplay(false);
        DecimalFormat format = new DecimalFormat("0.00");
        format.setRoundingMode(RoundingMode.FLOOR);
        for(int i = 0; i < 10000; i++) {
            float km = i / 10000f;
            DataMatrix.putFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT, km);

            float miles = (float)Math.floor(km * 1000) / 1000 / 1.609344f;
            //float miles = summary.statsAccumulated.distanceKm / 1.609344f;
            Assert.assertEquals("Test round " + i, format.format(miles), StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.DISTANCE_IMPERIAL).getCurrentValue());
        }

        updateDisplay(true);
        summary.statsAccumulated.distanceKm = 3.123f;
        Assert.assertEquals("3.12", summaryValues.getInfo(WorkoutSummaryValues.Distance).getStat(summary));
        summary.statsAccumulated.distanceKm = 3.126f;
        Assert.assertEquals("3.12", summaryValues.getInfo(WorkoutSummaryValues.Distance).getStat(summary));
        summary.statsAccumulated.distanceKm = 3.1299f;
        Assert.assertEquals("3.12", summaryValues.getInfo(WorkoutSummaryValues.Distance).getStat(summary));

        updateDisplay(false);
        format = new DecimalFormat("0.00");
        format.setRoundingMode(RoundingMode.FLOOR);
        for(int i = 0; i < 10000; i++) {
            summary.statsAccumulated.distanceKm = i / 10000f;
            float miles = (float)Math.floor(summary.statsAccumulated.distanceKm * 1000) / 1000 / 1.609344f;
            //float miles = summary.statsAccumulated.distanceKm / 1.609344f;
            Assert.assertEquals("Test round " + i, format.format(miles), summaryValues.getInfo(WorkoutSummaryValues.Distance).getStat(summary));
        }
    }

    @Test @Emulator @Default @Chimera
    public void testElevation() {

        updateDisplay(true);
        DataMatrix.putFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT, 3.123f);
        Assert.assertEquals("3", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.ELEVATION).getCurrentValue());
        DataMatrix.putFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT, 3.999f);
        Assert.assertEquals("3", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.ELEVATION).getCurrentValue());
        DataMatrix.putFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT, 4.00001f);
        Assert.assertEquals("4", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.ELEVATION).getCurrentValue());


        DecimalFormat format = new DecimalFormat("0");
        updateDisplay(false);
        format.setRoundingMode(RoundingMode.FLOOR);
        for(int i = 0; i < 100; i++) {
            float meters = i / 10000f + 3;
            DataMatrix.putFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT, meters);

            float feet = (float)Math.floor(meters * 10) / 10 / 0.3048f;
            //float miles = summary.statsAccumulated.distanceKm / 1.609344f;
            Assert.assertEquals("Test round " + i, format.format(feet), StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.ELEVATION).getCurrentValue());
        }

        updateDisplay(true);
        summary.statsAccumulated.elevationMeters = 3.123f;
        Assert.assertEquals("3", summaryValues.getInfo(WorkoutSummaryValues.Elevation).getStat(summary));
        summary.statsAccumulated.elevationMeters = 3.999f;
        Assert.assertEquals("3", summaryValues.getInfo(WorkoutSummaryValues.Elevation).getStat(summary));
        summary.statsAccumulated.elevationMeters = 4.00001f;
        Assert.assertEquals("4", summaryValues.getInfo(WorkoutSummaryValues.Elevation).getStat(summary));

        format = new DecimalFormat("0");
        updateDisplay(false);
        format.setRoundingMode(RoundingMode.FLOOR);
        for(int i = 0; i < 100; i++) {
            summary.statsAccumulated.elevationMeters = i / 10000f + 3;
            float feet = (float)Math.floor(summary.statsAccumulated.elevationMeters * 10) / 10 / 0.3048f;
            //float miles = summary.statsAccumulated.distanceKm / 1.609344f;
            Assert.assertEquals("Test round " + i, format.format(feet), summaryValues.getInfo(WorkoutSummaryValues.Elevation).getStat(summary) );
        }
    }


    @Test @Emulator @Default @Chimera
    public void testSpeed() {

        updateDisplay(true);
        DataMatrix.putFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, 3.123f);
        DataMatrix.putFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT, 3.123f);
        Assert.assertEquals("3.1", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.SPEED).getCurrentValue());
        DataMatrix.putFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, 3.182f);
        DataMatrix.putFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT, 3.182f);
        Assert.assertEquals("3.2", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.SPEED).getCurrentValue());
        DataMatrix.putFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, 3.223f);
        DataMatrix.putFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT, 3.223f);
        Assert.assertEquals("3.2", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.SPEED).getCurrentValue());

        connectSamsungWatch(true);
        DataMatrix.putFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, 3.123f);
        DataMatrix.putFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT, 3.123f);
        Assert.assertEquals("3.1", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.SPEED).getCurrentValue());
        DataMatrix.putFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, 3.182f);
        DataMatrix.putFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT, 3.182f);
        Assert.assertEquals("3.1", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.SPEED).getCurrentValue());
        DataMatrix.putFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, 3.223f);
        DataMatrix.putFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT, 3.223f);
        Assert.assertEquals("3.2", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.SPEED).getCurrentValue());


        connectSamsungWatch(false);
        updateDisplay(false);
        updateDisplay(false);
        DecimalFormat format = new DecimalFormat("0.0");
        format.setRoundingMode(RoundingMode.HALF_UP);
        float kph;
        for(int i = 0; i < 1000; i++) {
            kph = i / 1000f + 4;
            DataMatrix.putFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, kph);
            DataMatrix.putFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT, kph);
            float mph = (float)Math.floor(kph * 1000) / 1000 / 1.609344f;
            //float miles = summary.statsAccumulated.distanceKm / 1.609344f;
            Assert.assertEquals(format.format(mph), StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.SPEED).getCurrentValue());
        }

        connectSamsungWatch(true);
        format.setRoundingMode(RoundingMode.FLOOR);
        for(int i = 0; i < 1000; i++) {
            kph = i / 1000f + 4;
            DataMatrix.putFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, kph);
            DataMatrix.putFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT, kph);
            float mphTarget = (float)Math.round(kph / 1.609344f * 10) / 10.0f;
            float kphAdj = (float)Math.ceil(mphTarget * 1.609344f * 100) / 100.0f;
            Assert.assertEquals(format.format(kphAdj / 1.609344f), StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.SPEED).getCurrentValue());
        }
        DataMatrix.putInt(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, WatchState.Disconnected.ordinal());
    }

    @Test @Emulator @Default @Chimera
    public void testAverageSpeed() {
        updateDisplay(true);
        summary.statsAverages.averageSpeedKph = 3.123f;
        Assert.assertEquals("3.1", summaryValues.getInfo(WorkoutSummaryValues.AverageSpeed).getStat(summary));
        summary.statsAverages.averageSpeedKph = 3.182f;
        Assert.assertEquals("3.2", summaryValues.getInfo(WorkoutSummaryValues.AverageSpeed).getStat(summary));
        summary.statsAverages.averageSpeedKph = 3.20001f;
        Assert.assertEquals("3.2", summaryValues.getInfo(WorkoutSummaryValues.AverageSpeed).getStat(summary));

        connectSamsungWatch(true);
        summary.statsAverages.averageSpeedKph = 3.123f;
        Assert.assertEquals("3.1", summaryValues.getInfo(WorkoutSummaryValues.AverageSpeed).getStat(summary));
        summary.statsAverages.averageSpeedKph = 3.182f;
        Assert.assertEquals("3.1", summaryValues.getInfo(WorkoutSummaryValues.AverageSpeed).getStat(summary));
        summary.statsAverages.averageSpeedKph = 3.20001f;
        Assert.assertEquals("3.2", summaryValues.getInfo(WorkoutSummaryValues.AverageSpeed).getStat(summary));
        DataMatrix.putInt(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, WatchState.Disconnected.ordinal());

        updateDisplay(false);
        connectSamsungWatch(false);
        DecimalFormat format = new DecimalFormat("0.0");
        format.setRoundingMode(RoundingMode.HALF_UP);
        for(int i = 0; i < 1000; i++) {
            summary.statsAverages.averageSpeedKph = i / 1000f + 4;
            float mph = (float)Math.floor(summary.statsAverages.averageSpeedKph * 1000) / 1000 / 1.609344f;
            //float miles = summary.statsAccumulated.distanceKm / 1.609344f;
            Assert.assertEquals("Test round " + i, format.format(mph), summaryValues.getInfo(WorkoutSummaryValues.AverageSpeed).getStat(summary) );
        }

        connectSamsungWatch(true);
        format.setRoundingMode(RoundingMode.FLOOR);
        for(int i = 0; i < 1000; i++) {
            summary.statsAverages.averageSpeedKph = i / 1000f + 4;

            float mphTarget = (float)Math.round(summary.statsAverages.averageSpeedKph / 1.609344f * 10) / 10.0f;
            float kphAdj = (float)Math.ceil(mphTarget * 1.609344f * 100) / 100.0f;
            Assert.assertEquals("Test round " + i, format.format(kphAdj / 1.609344f), summaryValues.getInfo(WorkoutSummaryValues.AverageSpeed).getStat(summary) );
        }
        DataMatrix.putInt(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, WatchState.Disconnected.ordinal());
    }


    @Test @Emulator @Default @Chimera
    public void testWatts() {
        DataMatrix.putFloat(HALKeys.DATA_WATTS_FLOAT, 4.123f);
        Assert.assertEquals("4", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.WATTS).getCurrentValue());
        DataMatrix.putFloat(HALKeys.DATA_WATTS_FLOAT, 4.623f);
        Assert.assertEquals("4", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.WATTS).getCurrentValue());
        DataMatrix.putFloat(HALKeys.DATA_WATTS_FLOAT, 5.000001f);
        Assert.assertEquals("5", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.WATTS).getCurrentValue());

        summary.statsAverages.averageWatts = 3.123f;
        Assert.assertEquals("3", summaryValues.getInfo(WorkoutSummaryValues.AverageWatts).getStat(summary));
        summary.statsAverages.averageWatts = 3.982f;
        Assert.assertEquals("3", summaryValues.getInfo(WorkoutSummaryValues.AverageWatts).getStat(summary));
        summary.statsAverages.averageWatts = 4.00001f;
        Assert.assertEquals("4", summaryValues.getInfo(WorkoutSummaryValues.AverageWatts).getStat(summary));

        summary.statsPeaks.peakWatts = 3.123f;
        Assert.assertEquals("3", summaryValues.getInfo(WorkoutSummaryValues.PeakWatts).getStat(summary));
        summary.statsPeaks.peakWatts = 3.982f;
        Assert.assertEquals("3", summaryValues.getInfo(WorkoutSummaryValues.PeakWatts).getStat(summary));
        summary.statsPeaks.peakWatts = 4.00001f;
        Assert.assertEquals("4", summaryValues.getInfo(WorkoutSummaryValues.PeakWatts).getStat(summary));

    }

    @Test @Emulator @Default @Chimera
    public void testMets() {
        DataMatrix.putFloat(HALKeys.DATA_METS_FLOAT, 4.123f);
        Assert.assertEquals("4.12", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.METS).getCurrentValue());
        DataMatrix.putFloat(HALKeys.DATA_METS_FLOAT, 4.628f);
        Assert.assertEquals("4.62", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.METS).getCurrentValue());
        DataMatrix.putFloat(HALKeys.DATA_METS_FLOAT, 5.000001f);
        Assert.assertEquals("5.00", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.METS).getCurrentValue());

        summary.statsAverages.averageMets = 3.123f;
        Assert.assertEquals("3.12", summaryValues.getInfo(WorkoutSummaryValues.AverageMets).getStat(summary));
        summary.statsAverages.averageMets = 3.628f;
        Assert.assertEquals("3.62", summaryValues.getInfo(WorkoutSummaryValues.AverageMets).getStat(summary));
        summary.statsAverages.averageMets = 4.000001f;
        Assert.assertEquals("4.00", summaryValues.getInfo(WorkoutSummaryValues.AverageMets).getStat(summary));

        summary.statsPeaks.peakMets = 3.123f;
        Assert.assertEquals("3.12", summaryValues.getInfo(WorkoutSummaryValues.PeakMets).getStat(summary));
        summary.statsPeaks.peakMets = 3.628f;
        Assert.assertEquals("3.62", summaryValues.getInfo(WorkoutSummaryValues.PeakMets).getStat(summary));
        summary.statsPeaks.peakMets = 4.000001f;
        Assert.assertEquals("4.00", summaryValues.getInfo(WorkoutSummaryValues.PeakMets).getStat(summary));

    }


    @Test @Emulator @Default @Chimera
    public void testFloors() {
        DataMatrix.putFloat(WorkoutKeys.DATA_FLOORS_FLOAT, 4.123f);
        Assert.assertEquals("4", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.FLOORS).getCurrentValue());
        DataMatrix.putFloat(WorkoutKeys.DATA_FLOORS_FLOAT, 4.628f);
        Assert.assertEquals("4", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.FLOORS).getCurrentValue());
        DataMatrix.putFloat(WorkoutKeys.DATA_FLOORS_FLOAT, 5.000001f);
        Assert.assertEquals("5", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.FLOORS).getCurrentValue());

        summary.statsAccumulated.floors = 3.123f;
        Assert.assertEquals("3", summaryValues.getInfo(WorkoutSummaryValues.Floors).getStat(summary));
        summary.statsAccumulated.floors = 3.628f;
        Assert.assertEquals("3", summaryValues.getInfo(WorkoutSummaryValues.Floors).getStat(summary));
        summary.statsAccumulated.floors = 4.000001f;
        Assert.assertEquals("4", summaryValues.getInfo(WorkoutSummaryValues.Floors).getStat(summary));

    }

    @Test @Emulator @Default @Chimera
    public void testRPM_SPM() {
        summary.statsAverages.averageStepsPerMinute = 3.123f;
        Assert.assertEquals("3", summaryValues.getInfo(WorkoutSummaryValues.AverageSpm).getStat(summary));
        summary.statsAverages.averageStepsPerMinute = 3.982f;
        Assert.assertEquals("3", summaryValues.getInfo(WorkoutSummaryValues.AverageSpm).getStat(summary));
        summary.statsAverages.averageStepsPerMinute = 4.00001f;
        Assert.assertEquals("4", summaryValues.getInfo(WorkoutSummaryValues.AverageSpm).getStat(summary));

        summary.statsAverages.averageRpm = 3.123f;
        Assert.assertEquals("3", summaryValues.getInfo(WorkoutSummaryValues.AverageRpm).getStat(summary));
        summary.statsAverages.averageRpm = 3.982f;
        Assert.assertEquals("3", summaryValues.getInfo(WorkoutSummaryValues.AverageRpm).getStat(summary));
        summary.statsAverages.averageRpm = 4.00001f;
        Assert.assertEquals("4", summaryValues.getInfo(WorkoutSummaryValues.AverageRpm).getStat(summary));
    }

    @Test @Emulator @Default @Chimera
    public void testAverageHR() {
        summary.statsAverages.averageHeartRate = 95.123f;
        Assert.assertEquals("95 bpm", summaryValues.getInfo(WorkoutSummaryValues.AverageHeartRate).getStat(summary));
        summary.statsAverages.averageHeartRate = 95.982f;
        Assert.assertEquals("95 bpm", summaryValues.getInfo(WorkoutSummaryValues.AverageHeartRate).getStat(summary));
        summary.statsAverages.averageHeartRate = 96.00001f;
        Assert.assertEquals("96 bpm", summaryValues.getInfo(WorkoutSummaryValues.AverageHeartRate).getStat(summary));

        DataMatrix.putInt(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, WatchState.Connected.ordinal());
        summary.statsAverages.averageHeartRate = 95.123f;
        Assert.assertEquals("95 bpm", summaryValues.getInfo(WorkoutSummaryValues.AverageHeartRate).getStat(summary));
        summary.statsAverages.averageHeartRate = 95.982f;
        Assert.assertEquals("96 bpm", summaryValues.getInfo(WorkoutSummaryValues.AverageHeartRate).getStat(summary));
        summary.statsAverages.averageHeartRate = 96.00001f;
        Assert.assertEquals("96 bpm", summaryValues.getInfo(WorkoutSummaryValues.AverageHeartRate).getStat(summary));
        DataMatrix.putInt(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, WatchState.Disconnected.ordinal());
    }

    @Test @Emulator @Default @Chimera
    public void testIncline() {
        setFrame(FrameConfigEnum.Treadmill_Performance);

        DataMatrix.putFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT, 15.51236f);
        Assert.assertEquals("15.5", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.INCLINE).getCurrentValue());
        DataMatrix.putFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT, 10.054f);
        Assert.assertEquals("10.0", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.INCLINE).getCurrentValue());
        DataMatrix.putFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT, 9.0999f);
        Assert.assertEquals("9.0", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.INCLINE).getCurrentValue());

        summary.statsPeaks.peakIncline = 15.51236f;
        Assert.assertEquals("15.5", summaryValues.getInfo(WorkoutSummaryValues.PeakIncline).getStat(summary));
        summary.statsPeaks.peakIncline = 10.054f;
        Assert.assertEquals("10.0", summaryValues.getInfo(WorkoutSummaryValues.PeakIncline).getStat(summary));
        summary.statsPeaks.peakIncline = 9.0999f;
        Assert.assertEquals("9.0", summaryValues.getInfo(WorkoutSummaryValues.PeakIncline).getStat(summary));

        setFrame(FrameConfigEnum.Ascent_Performance);
        DataMatrix.putFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT, 15.51236f);
        Assert.assertEquals("15", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.INCLINE).getCurrentValue());
        DataMatrix.putFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT, 60.664f);
        Assert.assertEquals("60", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.INCLINE).getCurrentValue());
        DataMatrix.putFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT, 90.235f);
        Assert.assertEquals("90", StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.INCLINE).getCurrentValue());

        summary.statsPeaks.peakIncline = 15;
        Assert.assertEquals("15", summaryValues.getInfo(WorkoutSummaryValues.PeakIncline).getStat(summary));
        summary.statsPeaks.peakIncline = 60.664f;
        Assert.assertEquals("60", summaryValues.getInfo(WorkoutSummaryValues.PeakIncline).getStat(summary));
        summary.statsPeaks.peakIncline = 90.235f;
        Assert.assertEquals("90", summaryValues.getInfo(WorkoutSummaryValues.PeakIncline).getStat(summary));

    }

    private void connectSamsungWatch(boolean connect) {
        DataMatrix.putInt(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, connect ? WatchState.Connected.ordinal() : WatchState.Disconnected.ordinal());
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {
        }
    }

    private void updateDisplay(boolean metric) {
        DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, metric);
        try {
            Thread.sleep(300);
        } catch (InterruptedException ignored) {
        }
    }

    private void setFrame(FrameConfigEnum frame) {

        FrameConfigManager.getInstance().setFrame(frame);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
    }

}