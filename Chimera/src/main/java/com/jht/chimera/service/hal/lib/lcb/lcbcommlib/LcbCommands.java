package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

import androidx.annotation.Nullable;

public enum LcbCommands {
    SetLCBInitialize(0x70),
    GetLCBStatus(0x71),
    GetLCBErrorCode(0x72),
    GetLCBVersion(0x73),
    SetLCBCalibrate(0x74),
    SetLCBUpdateProgram(0x75),
    SetLCBSkipCurrError(0x76),
    SetLCBSpeExtCmd(0x77),
    GetLCBDciVersion(0x78),
    GetLCBDciEnv(0x79),
    SetLCBCmdEUPsMode(0x7A),
    SetLCBCmdConsolePower(0x7B),
    GetLCBProtocolVersion(0x7C),
    SetLCBCmdSerialNumber(0x7D),
    SetLCBTreadmillOperate(0x7E),
    
    GetLCBTreadmillCadence(0x7F),
    SetLCBMotorSpeedRtnRPM(0xF0),
    SetLCBMotorRPM(0xF1),
    SetLCBIncPWMStep(0xF2),
    SetLCBDecPWMStep(0xF3),
    SetLCBStopPWMStep(0xF4),
    SetLCBInclineAction(0xF5),
    SetLCBInclineLoc(0xF6),
    SetLCBOperatingStatus(0xF7),
    GetLCBRollerRPM(0xF8),
    GetLCBMotorRPM(0xF9),
    GetLCBInclineLoc(0xFA),
    SetLCBTuneEndIncline(0xFB),
    SetLCBTuneEndIncline2(0xFC),
    SetLCBInclineStroke(0xFD),
    SetLCBCompensationVol(0xFE),

    SetLCBMotorSpecification(0x90),
    GetLCBMotorType(0x91),
    GetLCBDCIdataMaxLen(0x92),
    SetLCBMotorPowerOff(0x93),
    SetLCBDCIforceIncline(0x94),
    GetLCBTreadmillInUsed(0x95),
    GetLCBMainMotorInfo(0x96),
    GetLCBTreadmillExtStatus(0x97),
    SetLCBTreadmillEcbAction(0x98),

    GetLCBDCIinclineRange(0xA0),
    SetLCBDCIinclineRange(0xA1),
    GetLCBDCIrpmSpeedRange(0xA6),
    SetLCBDCIrpmSpeedRange(0xA7),
    SetLCBUniversal(0xAA),
    SetLCBDCIManualCalibration(0xAB),

    SetLCBDeviceData(0x50),
    GetLCBIndoorBikeSensorData(0x51),
    SetLCBIndoorBikeStepperMotorPosition(0x52),
    SetLCBIndoorBikeCalcParaOfDevice(0x53),
    GetLCBIndoorBikeCalcParaOfDevice(0x54),
    SetLCBIndoorBikeActionSpinDown(0x55),
    GetLCBIndoorBikeInfoSpinDown(0x56),
    SetLCBIndoorBikeErgModeSettings(0x57),
    SetLCBIndoorBikeResistanceLevel(0x58),
    GetLCBIndoorBikeErgModeSettings(0x59),

    SetLCBResetECB(0x61),
    SetLCBECBposition(0x62),
    GetLCBECBrpm(0x63),
    GetLCBECBadc(0x64),
    SetLCBPWMpercent(0x65),
    GetLCBBatteryVoltage(0x66),
    SetLCBWatts(0x67),
    SetLCBRPMgearRatio(0x68),
    SetLCBGeneratorPolePair(0x69),
    SetLCBLimitRPMcharge(0x6A),
    SetLCBLimitRPMresistance(0x6B),
    SetLCBMachineType(0x6C),
    SetLCBPowerOff(0x6D),
    SetLCBBatteryChargeLevel(0x6E),
    SetLCBMinResistanceInclineWorking(0x6F),

    SetLCBInclinePercent(0x80),
    GetLCBInclinePercent(0x81),
    SetLCBECBaction(0x82),
    GetLCBECBstatus(0x83),
    GetLCBECBcount(0x84),
    SetLCBElectromagneticCurrent(0x85),
    SetLCBResistanceType(0x86),
    SetLCBBeginBatteryCharge(0x87),
    GetLCBBatteryCapacity(0x88),
    SetLCBClimbmillStatus(0x8A),
    GetLCBClimbmillStatus(0x8B),
    SetLCBEStopParameter(0x8C),
    GetLCBBrakePressureValue(0x8D),
    GetLCBWatts(0x8E),
    SetLCBClimbmillCal(0x8F),

    SetLCBRecordCurrent(0x47),
    SetLCBPWM(0x48),
    SetLCBFinishCalibrating(0x49),
    SetLCBSPM(0x4A),
    GetLCBSPM(0x4B),
    SetLCBCalScaling(0x4C),
    GetLCBCalScaling(0x4D),
    SetLCBFrameCalScaling(0x4E),
    GetLCBFrameCalScaling(0x4F),
    GetIrAddressIrStatus(0x99); //Ir Addr 0x26 get IR status


    public int Value;

    LcbCommands(int value) {
        Value = value;
    }

    @Nullable
    public static LcbCommands fromInt(int i) {
        for (LcbCommands b : LcbCommands.values()) {
            if (b.Value == i) {
                return b;
            }
        }
        return null;
    }

    public byte getByteValue() {
        byte byteValue = (byte) Value;
        return byteValue;
    }

}
