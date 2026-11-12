package com.jht.chimera.service.hal.lib.lcb;

import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ILcbCommReceive;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbPacket;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbCommands;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbProcComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IndoorBike.IndoorBikeInfoSpinDown;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IndoorBike.IndoorBikeSensorData;
import com.jht.chimera.service.hal.lib.frictiontest.TreadmillOperateRxData;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.Default;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 * Android unit tests for the 0x7F Get Cadence command.
 *
 * Tests verify that procGetLcbTreadmillCadenceHandler() correctly passes raw
 * byte[] data to the listener callback, focusing on:
 *   - Raw byte array is forwarded unchanged
 *   - Data length matches what was set in the packet
 *   - Byte content integrity
 */
public class LcbCadenceCommandTest {

    private static final int RECEIVE_DATA_SIZE_GET_CADENCE = 16;

    /**
     * Build a 16-byte cadence response payload using the defined field order:
     *   [0-1]   cadenceSpm           (uint16 BE)
     *   [2-3]   groundContactTimeLeft (uint16 BE)
     *   [4-5]   flightTimeLeft        (uint16 BE)
     *   [6-7]   groundContactTimeRight(uint16 BE)
     *   [8-9]   flightTimeRight       (uint16 BE)
     *   [10-11] leftBalance           (uint16 BE)
     *   [12-13] rightBalance          (uint16 BE)
     *   [14-15] accumulateSteps       (uint16 BE)
     */
    private LcbPacket buildCadencePacket(int cadenceSpm, int gctLeft, int flightLeft,
                                          int gctRight, int flightRight,
                                          int leftBalance, int rightBalance,
                                          int accumulateSteps) {
        byte[] data = new byte[RECEIVE_DATA_SIZE_GET_CADENCE];
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) cadenceSpm);
        buf.putShort((short) gctLeft);
        buf.putShort((short) flightLeft);
        buf.putShort((short) gctRight);
        buf.putShort((short) flightRight);
        buf.putShort((short) leftBalance);
        buf.putShort((short) rightBalance);
        buf.putShort((short) accumulateSteps);

        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBTreadmillCadence);
        packet.setData(data);
        return packet;
    }

    /**
     * Install a single-listener list into the LcbProcComm singleton,
     * run the handler, then restore an empty list.
     */
    private byte[] runHandler(LcbPacket packet) {
        final byte[][] result = {null};

        ILcbCommReceive listener = new NoOpLcbCommReceive() {
            @Override
            public void getLcbTreadmillCadenceHandler(byte[] bytesCadenceData) {
                result[0] = bytesCadenceData;
            }

            @Override
            public void getLcbIndoorBikeSensorDataHandler(byte[] data) {

            }

            @Override
            public void getOneShotLcbIndoorBikeSensorDataHandler(byte[] data) {

            }

            @Override
            public void getIrAddrVersionHandler(int intIrAddrVersion) {

            }

            @Override
            public void getIrAddrIrStatusHandler(byte byteStatus) {

            }

            @Override
            public void setIrAddrCmdEUPsModeHandler(byte byteRtn) {

            }

            @Override
            public void enterIrAddrErpModeHandler(boolean isEnterSuccess) {

            }

            @Override
            public void leaveIrAddrErpModeHandler(boolean isLeaveSuccess) {

            }
        };

        ArrayList<ILcbCommReceive> list = new ArrayList<>();
        list.add(listener);
        LcbProcComm proc = LcbProcComm.getInstance();
        proc.setReceiveListenerList(list);
        proc.procGetLcbTreadmillCadenceHandler(packet);
        proc.setReceiveListenerList(new ArrayList<>());

        return result[0];
    }

    /**
     * Test 1: Raw byte[] is forwarded with correct length.
     */
    @Test @Emulator @Default @Chimera
    public void testGetCadenceRawDataLength() {
        LcbPacket packet = buildCadencePacket(120, 0, 0, 0, 0, 0, 0, 0);
        byte[] data = runHandler(packet);
        Assert.assertNotNull("Callback should have been fired", data);
        Assert.assertEquals("Data length should be 16", RECEIVE_DATA_SIZE_GET_CADENCE, data.length);
    }

    /**
     * Test 2: cadenceSpm bytes [0-1] are passed correctly.
     */
    @Test @Emulator @Default @Chimera
    public void testGetCadenceSpmBytes() {
        LcbPacket packet = buildCadencePacket(120, 0, 0, 0, 0, 0, 0, 0);
        byte[] data = runHandler(packet);
        Assert.assertNotNull("Callback should have been fired", data);
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.BIG_ENDIAN);
        int cadenceSpm = buf.getShort() & 0xFFFF;
        Assert.assertEquals("cadenceSpm should be 120", 120, cadenceSpm);
    }

    /**
     * Test 3: accumulateSteps bytes [14-15] are passed correctly.
     */
    @Test @Emulator @Default @Chimera
    public void testGetCadenceAccumulateStepsBytes() {
        LcbPacket packet = buildCadencePacket(0, 0, 0, 0, 0, 0, 0, 500);
        byte[] data = runHandler(packet);
        Assert.assertNotNull("Callback should have been fired", data);
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.position(14);
        int accumulateSteps = buf.getShort() & 0xFFFF;
        Assert.assertEquals("accumulateSteps should be 500", 500, accumulateSteps);
    }

    /**
     * Test 4: All fields are passed correctly in the raw byte[] payload.
     */
    @Test @Emulator @Default @Chimera
    public void testGetCadenceAllFields() {
        LcbPacket packet = buildCadencePacket(
                90,   // cadenceSpm
                250,  // groundContactTimeLeft (ms)
                120,  // flightTimeLeft (ms)
                260,  // groundContactTimeRight (ms)
                110,  // flightTimeRight (ms)
                48,   // leftBalance (%)
                52,   // rightBalance (%)
                1234  // accumulateSteps
        );
        byte[] data = runHandler(packet);
        Assert.assertNotNull("Callback should have been fired", data);

        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.BIG_ENDIAN);
        Assert.assertEquals("cadenceSpm", 90, buf.getShort() & 0xFFFF);
        Assert.assertEquals("groundContactTimeLeft", 250, buf.getShort() & 0xFFFF);
        Assert.assertEquals("flightTimeLeft", 120, buf.getShort() & 0xFFFF);
        Assert.assertEquals("groundContactTimeRight", 260, buf.getShort() & 0xFFFF);
        Assert.assertEquals("flightTimeRight", 110, buf.getShort() & 0xFFFF);
        Assert.assertEquals("leftBalance", 48, buf.getShort() & 0xFFFF);
        Assert.assertEquals("rightBalance", 52, buf.getShort() & 0xFFFF);
        Assert.assertEquals("accumulateSteps", 1234, buf.getShort() & 0xFFFF);
    }

    /**
     * Test 5: Any data size is forwarded (no size filtering in LcbProcComm).
     */
    @Test @Emulator @Default @Chimera
    public void testGetCadenceAnyDataSizeForwarded() {
        byte[] smallData = new byte[8];
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBTreadmillCadence);
        packet.setData(smallData);

        byte[] data = runHandler(packet);
        Assert.assertNotNull("Callback should be fired for any data size", data);
        Assert.assertEquals("Data length should match input", 8, data.length);
    }

    // ---------------------------------------------------------------------------
    // No-op base implementation of ILcbCommReceive to keep test classes concise.
    // ---------------------------------------------------------------------------
    private static abstract class NoOpLcbCommReceive implements ILcbCommReceive {
        @Override public void bootUpLeaveLcbErpHandler(boolean b) {}
        @Override public void enterLcbErpModeHandler(boolean b) {}
        @Override public void leaveLcbErpModeHandler(boolean a, boolean b) {}
        @Override public void changeLcbInclineMotorDownwardStatusHandler(boolean b) {}
        @Override public void changeLcbInclineMotorUpwardStatusHandler(boolean b) {}
        @Override public void changeLcbMainMotorStatusHandler(boolean b) {}
        @Override public void changeLcbEncoderRPMStatusHandler(boolean b) {}
        @Override public void changeLcbBatteryCapacityStatusHandler(boolean b) {}
        @Override public void changeLcbCommandErrorStatusHandler(boolean b) {}
        @Override public void changeLcbLcbErrorStatusDetectedHandler(boolean b) {}
        @Override public void changeLcbInitialCalibrationStatusHandler(boolean b) {}
        @Override public void setLcbInitialHandler() {}
        @Override public void getLcbStatusHandler(byte b) {}
        @Override public void getLcbErrorCodeHandler(int i) {}
        @Override public void returnErrorCodeToUIHandler(int i) {}
        @Override public void getLcbVersionHandler(int i) {}
        @Override public void setLcbCalibrateHandler() {}
        @Override public void setLcbUpdateProgramHandler(byte b) {}
        @Override public void setLcbSkipCurrErrorHandler() {}
        @Override public void setLcbSpeExtCmdHandler(byte b) {}
        @Override public void getLcbDciVersionHandler(byte[] b) {}
        @Override public void getLcbDciEnvHandler(byte[] b) {}
        @Override public void setLcbCmdEUPsModeHandler(byte b) {}
        @Override public void setLcbCmdConsolePowerHandler() {}
        @Override public void getLcbProtocolVersionHandler(int i) {}
        @Override public void setLcbCmdSerialNumberHandler(byte[] b) {}
        @Override public void setLcbTreadmillOperateHandler(TreadmillOperateRxData d) {}
        @Override public void getLcbTreadmillCadenceHandler(byte[] b) {}
        @Override public void setLcbMotorSpeedRtnRPMHandler(int i) {}
        @Override public void setLcbMotorRPMHandler() {}
        @Override public void setLcbIncPWMStepHandler() {}
        @Override public void setLcbDecPWMStepHandler() {}
        @Override public void setLcbStopPWMStepHandler() {}
        @Override public void setLcbInclineActionHandler() {}
        @Override public void setLcbInclineLocHandler() {}
        @Override public void setLcbOperatingStatusHandler() {}
        @Override public void getLcbRollerRPMHandler(int i) {}
        @Override public void getLcbMotorRPMHandler(int i) {}
        @Override public void getOneShotLcbMotorRpmHandler(int i) {}
        @Override public void getLcbInclineLocHandler(int i) {}
        @Override public void setLcbTuneEndInclineHandler() {}
        @Override public void setLcbTuneEndIncline2Handler() {}
        @Override public void setLcbInclineStrokeHandler() {}
        @Override public void setLcbCompensationVolHandler(int i) {}
        @Override public void setLcbMotorSpecificationHandler() {}
        @Override public void getLcbMotorTypeHandler(byte b) {}
        @Override public void getLcbDCIdataMaxLenHandler(byte b) {}
        @Override public void setLcbMotorPowerOffHandler() {}
        @Override public void setLcbDCIforceInclineHandler() {}
        @Override public void getLcbTreadmillInUsedHandler(byte b) {}
        @Override public void getLcbMainMotorInfoHandler(byte[] b) {}
        @Override public void getLcbTreadmillExtStatusHandler(int i) {}
        @Override public void setLcbTreadmillEcbActionHandler(byte b) {}
        @Override public void getLcbDCIinclineRangeHandler(int i) {}
        @Override public void setLcbDCIinclineRangeHandler() {}
        @Override public void getLcbDCIrpmSpeedRangeHandler(int i) {}
        @Override public void setLcbDCIrpmSpeedRangeHandler() {}
        @Override public void setLcbUniversalHandler(int i) {}
        @Override public void setLcbDCIManualCalibrationHandler() {}
        @Override public void setLcbDeviceDataHandler(byte[] b) {}
        @Override public void setLcbResetECBHandler() {}
        @Override public void setLcbECBpositionHandler() {}
        @Override public void getLcbECBrpmHandler(int i) {}
        @Override public void getOneShotLcbECBrpmHandler(int i) {}
        @Override public void getLcbECBadcHandler(int i) {}
        @Override public void setLcbPWMpercentHandler() {}
        @Override public void getLcbBatteryVoltageHandler(int i) {}
        @Override public void setLcbWattsHandler() {}
        @Override public void setLcbRPMgearRatioHandler() {}
        @Override public void setLcbGeneratorPolePairHandler() {}
        @Override public void setLcbLimitRPMchargeHandler() {}
        @Override public void setLcbLimitRPMresistanceHandler() {}
        @Override public void setLcbMachineTypeHandler() {}
        @Override public void setLcbPowerOffHandler() {}
        @Override public void setLcbBatteryChargeLevelHandler() {}
        @Override public void setLcbMinResistanceInclineWorkingHandler() {}
        @Override public void setLcbInclinePercentHandler() {}
        @Override public void getLcbInclinePercentHandler(int i) {}
        @Override public void setLcbECBactionHandler() {}
        @Override public void getLcbECBstatusHandler(byte b) {}
        @Override public void getLcbECBcountHandler(int i) {}
        @Override public void setLcbElectromagneticCurrentHandler() {}
        @Override public void setLcbResistanceTypeHandler() {}
        @Override public void setLcbBeginBatteryChargeHandler() {}
        @Override public void getLcbBatteryCapacityHandler(int i) {}
        @Override public void setLcbClimbmillStatusHandler() {}
        @Override public void getLcbClimbmillStatusHandler(int i) {}
        @Override public void setLcbEStopParameterHandler() {}
        @Override public void getLcbBrakePressureValueHandler(int i) {}
        @Override public void getLcbWattsHandler(int i) {}
        @Override public void setLcbClimbmillCalHandler() {}
        @Override public void setLcbRecordCurrentHandler() {}
        @Override public void setLcbPWMHandler() {}
        @Override public void setLcbFinishCalibratingHandler() {}
        @Override public void setLcbSPMHandler() {}
        @Override public void getLcbSPMHandler(int i) {}
        @Override public void getOneShotLcbSpmHandler(int i) {}
        @Override public void setLcbCalScalingHandler() {}
        @Override public void getLcbCalScalingHandler(int i) {}
        @Override public void setLcbFrameCalScalingHandler() {}
        @Override public void getLcbFrameCalScalingHandler(int i) {}
        @Override public void setLcbIndoorBikeStepperMotorPositionHandler() {}
        @Override public void setLcbIndoorBikeCalcParaOfDeviceHandler() {}
        @Override public void getLcbIndoorBikeCalcParaOfDeviceHandler(byte[] b) {}
        @Override public void setLcbIndoorBikeActionSpinDownHandler(byte[] b) {}
        @Override public void getLcbIndoorBikeInfoSpinDownHandler(IndoorBikeInfoSpinDown d) {}
        @Override public void setLcbIndoorBikeErgModeSettingsHandler(byte[] b) {}
        @Override public void getLcbIndoorBikeErgModeSettingsHandler(byte[] b) {}
        @Override public void setLcbIndoorBikeResistanceLevelHandler() {}
        @Override public void setTreadMillAdcValueHandler(int i) {}
        @Override public void setTreadMillRpmValueHandler(int i) {}
    }
}
