package com.jht.chimera.service.dapi;

import static com.jht.chimera.service.dapi.DAPIService.FIELD_NA_PLACEHOLDER;

import android.util.Log;

import com.jht.androidcommonalgorithms.network.AddressHelper;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.dapi.DapiUtil;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.workout.SpecialWorkouts;
import com.jht.chimera.service.dapi.handlers.am.cardioinfo.CardioInfoDAPIRequest;
import com.jht.chimera.service.dapi.handlers.am.consolefeatureset.ConsoleFeatureSetDataV1;
import com.jht.chimera.service.dapi.handlers.am.consolefeatureset.ConsoleFeatureSetDataV2;
import com.jht.chimera.service.dapi.handlers.am.consolefeatureset.ConsoleFeaturesetRequest;
import com.jht.chimera.service.dapi.handlers.am.getmachineid.GetMachineUUIDHandler;
import com.jht.chimera.service.dapi.handlers.am.getmachineid.GetMachineUUIDRequest;
import com.jht.chimera.service.dapi.handlers.am.getmachineid.MachineUUIDData;
import com.jht.chimera.service.dapi.handlers.am.getmachinestats.GetMachineStatsRequest;
import com.jht.chimera.service.dapi.handlers.am.getmachinestats.MachineStatsData;
import com.jht.chimera.service.dapi.testfixture.DAPIServiceTestFixture;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.runner.Default;
import com.jht.datamatrixlib.DataMatrix;
import com.jhtp.dapiservice.am.CardioInfo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

public class EquipmentSequenceTest {
    private static final String TAG ="EquipmentSequenceTest";
    private String testUUID = null;

    @Before
    public void init() {
        testUUID = null;
    }

    @Test @Default @Chimera
    public void testInitialSequenceNormalConsoleFeatureSetV2() {
        try(DAPIServiceTestFixture testFixture = new DAPIServiceTestFixture()) {
            startSystem(testFixture);
            setupMessageSequence(testFixture, getConsoleFeatureSetV2(), getMachineUUID());
            runTest(testFixture);
        }
    }

    @Test @Default @Chimera
    public void testInitialSequenceDifferentMachineUUID() {
        testUUID = "8aa69bdf-184f-465f-8660-c92af03efec7";
        try(DAPIServiceTestFixture testFixture = new DAPIServiceTestFixture()) {
            startSystem(testFixture);
            setupMessageSequence(testFixture, getConsoleFeatureSetV2(), testUUID);
            runTest(testFixture);
        }
    }

    @Test @Default @Chimera
    public void testInitialSequenceGetStats() {
        Random random = new Random();

        try(DAPIServiceTestFixture testFixture = new DAPIServiceTestFixture()) {
            MachineStatsData stats = new MachineStatsData(random.nextFloat() * random.nextInt(6000), random.nextInt(10000));
            DataMatrix.putBoolean(SettingKeys.Experimental.DATA_LOG_DAPI_PACKETS_BOOL, true);
            startSystem(testFixture);
            setupMessageSequence(testFixture, getConsoleFeatureSetV2(), getMachineUUID(), stats);
            runTest(testFixture, true);
            if (FrameConfigManager.getInstance().getFrameConfig().machineClassType.equals(JHTCommonTypes.MachineClassificationType.Stepper) ||
                    FrameConfigManager.getInstance().getFrameConfig().machineClassType.equals(JHTCommonTypes.MachineClassificationType.Climbmill)) {
                Assert.assertEquals(stats.distance(), DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT), 0.001);
            } else {
                Assert.assertEquals(stats.distance(), DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT), 0.001);
            }
            Assert.assertEquals(stats.time(), DataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT));
        }
    }

    private void startSystem(DAPIServiceTestFixture testFixture) {
        testFixture.startTest();
        testFixture.switchToMockServer();
    }


    private void runTest(DAPIServiceTestFixture testFixture) {
        runTest(testFixture, false);
    }

    private void runTest(DAPIServiceTestFixture testFixture, boolean getStats) {
        DataMatrix.putBoolean(DAPIKeys.ACTION_DO_FIRST_TIME_CONNECTED_TASKS_BOOL, getStats);
        testFixture.waitForTestComplete(5000);
        testFixture.runFinalCheck();
    }

    private void setupMessageSequence(DAPIServiceTestFixture testFixture, ConsoleFeatureSetDataV1 consoleFeatureSet, String expectedMachineId) {
        setupMessageSequence(testFixture, consoleFeatureSet, expectedMachineId, null);
    }

    private void setupMessageSequence(DAPIServiceTestFixture testFixture, ConsoleFeatureSetDataV1 consoleFeatureSet, String expectedMachineId, MachineStatsData stats) {
        testFixture.setMachineUUIDResponse(new GetMachineUUIDHandler.MachineUUIDResponse(expectedMachineId));
        testFixture.addExepectedRequest(new GetMachineUUIDRequest(1, new MachineUUIDData(DapiUtil.computeMachineId())));
        if(stats != null) {
            testFixture.setMachineStats(stats);
            testFixture.addExepectedRequest(new GetMachineStatsRequest(1, getMachineUUID()));
            testFixture.addExepectedRequest(new CardioInfoDAPIRequest(1, getCardioInfo(stats)));
        }
        else {
            testFixture.addExepectedRequest(new CardioInfoDAPIRequest(1, getCardioInfo(null)));
        }
        testFixture.addExepectedRequest(new ConsoleFeaturesetRequest(2, consoleFeatureSet));
    }

    private ConsoleFeatureSetDataV1 getConsoleFeatureSetV2() {
        return new ConsoleFeatureSetDataV2(
                getMachineUUID(),
                DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL),
                DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL),
                SpecialWorkouts.isIfitEnabled()
        );
    }
    private CardioInfo getCardioInfo(MachineStatsData stats) {

        int totalTime = stats == null ? DataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT) : stats.time();
        float totalDistance;

        if(stats != null) {
            totalDistance = stats.distance();
        }
        else {
            if (FrameConfigManager.getInstance().getFrameConfig().machineClassType.equals(JHTCommonTypes.MachineClassificationType.Stepper) ||
                    FrameConfigManager.getInstance().getFrameConfig().machineClassType.equals(JHTCommonTypes.MachineClassificationType.Climbmill)) {
                totalDistance = DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT);
            } else {
                totalDistance = DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT);
            }
        }

        CardioInfo cardioInfo = new CardioInfo();
        cardioInfo.machineId = getMachineUUID();
        cardioInfo.brand = DapiUtil.getBrandId();
        cardioInfo.machineType = FrameConfigManager.getInstance().getFrameConfig().machineClassType.getValue();
        cardioInfo.modelType = FrameConfigManager.getInstance().getModelId();
        cardioInfo.consoleType = ProductConfig.getConsoleType().id;
        cardioInfo.csafeManufacture = FIELD_NA_PLACEHOLDER; // n/a for retail
        cardioInfo.csafeId = FIELD_NA_PLACEHOLDER; // n/a for retail
        cardioInfo.csafeModelId = FIELD_NA_PLACEHOLDER; // n/a for retail
        cardioInfo.consoleSerialNumber = DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING);
        cardioInfo.frameSerialNumber = DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING);
        cardioInfo.clubId = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT);
        cardioInfo.softwareVersion = "GUI:" + DataMatrix.getString(HALKeys.DATA_VERSION_CHIMERA_STRING); //NON-NLS
        cardioInfo.osVersion = DataMatrix.getString(HALKeys.DATA_VERSION_OS_STRING);
        cardioInfo.maxUsers = 1;
        cardioInfo.ipAddress = AddressHelper.getIPAddress("wlan0"); //NON-NLS
        cardioInfo.totalTime = totalTime;
        cardioInfo.averageTimePerWorkout = FIELD_NA_PLACEHOLDER; // = secondsPerWorkout;
        cardioInfo.totalDistance = totalDistance;
        cardioInfo.averageDistancePerWorkout = FIELD_NA_PLACEHOLDER; // = distancePerWorkout;
        cardioInfo.averagePace = FIELD_NA_PLACEHOLDER;
        cardioInfo.peakResistance = FIELD_NA_PLACEHOLDER;
        cardioInfo.averageResistance = FIELD_NA_PLACEHOLDER;
        cardioInfo.peakWatts = FIELD_NA_PLACEHOLDER;
        cardioInfo.averageWatts = FIELD_NA_PLACEHOLDER;
        cardioInfo.batteryPercentage = FIELD_NA_PLACEHOLDER; // n/a
        cardioInfo.peakKph = FIELD_NA_PLACEHOLDER;
        cardioInfo.averageKph = FIELD_NA_PLACEHOLDER;
        cardioInfo.headphoneInsertions = DataMatrix.getInt(SettingKeys.Hardware.DATA_HEADPHONE_A_COUNT_INT);
        cardioInfo.headphoneRemovals = FIELD_NA_PLACEHOLDER;
        cardioInfo.machineSubtype = "cardio"; //NON-NLS

        // Cardio specific (not needed?)
        cardioInfo.driveType = FIELD_NA_PLACEHOLDER;
        cardioInfo.minKph = (int) FrameConfigManager.getInstance().getFrameConfig().speedKph.getMin();
        cardioInfo.maxKph = (int) FrameConfigManager.getInstance().getFrameConfig().speedKph.getMax();
        cardioInfo.minResistance = (int) FrameConfigManager.getInstance().getFrameConfig().resistance.getMin();
        cardioInfo.maxResistance = (int) FrameConfigManager.getInstance().getFrameConfig().resistance.getMax();
        cardioInfo.minIncline = (int) FrameConfigManager.getInstance().getFrameConfig().incline.getMin();
        cardioInfo.maxIncline = (int) FrameConfigManager.getInstance().getFrameConfig().incline.getMax();
        cardioInfo.lcbVendor = DataMatrix.getInt(SettingKeys.General.DATA_LCB_TYPE_INT);
        cardioInfo.lcbVersion = DataMatrix.getString(HALKeys.DATA_VERSION_LCB_STRING) + ".0.0";
        return cardioInfo;
    }

    private String getMachineUUID() {
        String uuid = testUUID == null ? DapiUtil.getMachineId() : testUUID;
        Log.d(TAG, "getMachineUUID int test " + uuid);
        return uuid;
    }
}
