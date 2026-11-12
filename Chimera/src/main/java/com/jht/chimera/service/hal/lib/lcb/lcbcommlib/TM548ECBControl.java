package com.jht.chimera.service.hal.lib.lcb;

public class TM548ECBControl {
    private static final String TAG = TM548ECBControl.class.getName();
    private static final byte DEFAULT_TM548_STATUS_BYTE = (byte) 0x03;//default initial Bit 1 & Bit 2 set 1
    public static TM548ECBStatus _TM548EcbStatus = new TM548ECBStatus((byte) DEFAULT_TM548_STATUS_BYTE);
    /*
    For Treadmill TM548 ECB status define
    Bit 0 : ECB motor(when E-stop) 0:one stage/ 1:two-stage H/W pin: MCB_I_3
    Bit 1 : MCB_I_2 : ECB resistance position 0:Down (resistance input) / 1:Up (resistance release) H/W pin: MCB_I_2
    Bit 2 : ECB Board 0: Off  / 1: On H/W pin: MCB_I_1
    Bit 3 ~ 7: Reserve
    */
    public enum TM548ECBState {
        TM548_ECB_B0_BIT_MOTOR(0x01),
        TM548_ECB_B1_BIT_RESISTANCE(0x02),
        TM548_ECB_B2_BIT_BOARD(0x04);

        private int Value;

        TM548ECBState(int value) {
            this.Value = value;
        }

        public static TM548ECBState fromInt(int i) {
            for (TM548ECBState b : TM548ECBState.values()) {
                if (b.Value == i) {
                    return b;
                }
            }
            return null;
        }

        public int getValue() {
            return Value;
        }

        public byte getByteValue() {
            byte byteValue = (byte) Value;
            return byteValue;
        }
    }

    public TM548ECBControl() {
		
    }
	
    public static byte setTM548EcbState(TM548ECBState ecbStateId) { 
        byte TM548EcbStatus = _TM548EcbStatus.getEcbStatus();
        byte rtnTM548ECBStateByte = (byte) (TM548EcbStatus);
        byte setEcbStateId = ecbStateId.getByteValue();
        TM548EcbStatus = (byte) (TM548EcbStatus | setEcbStateId);

	_TM548EcbStatus.onEcbStatus(TM548EcbStatus);

	 rtnTM548ECBStateByte = _TM548EcbStatus.getEcbStatus();
        return rtnTM548ECBStateByte;
    }

    public static byte getTM548EcbStatusByte() { 
        byte TM548EcbStatus = _TM548EcbStatus.getEcbStatus();
        byte rtnTM548ECBStateByte = TM548EcbStatus;

        return rtnTM548ECBStateByte;
    }

    public static byte setTM548EcbPowerOnLock() { 
        byte TM548EcbStatus = _TM548EcbStatus.getEcbStatus();
        byte rtnTM548ECBStateByte = (byte) (TM548EcbStatus);

        byte setEcbBoardStateId = TM548ECBState.TM548_ECB_B2_BIT_BOARD.getByteValue();
        TM548EcbStatus = (byte) (TM548EcbStatus | setEcbBoardStateId);

        byte setEcbResistanceStateId = (byte) ~((byte)(TM548ECBState.TM548_ECB_B1_BIT_RESISTANCE.getByteValue()));
        TM548EcbStatus = (byte) (TM548EcbStatus & setEcbResistanceStateId);

	_TM548EcbStatus.onEcbStatus(TM548EcbStatus);

	 rtnTM548ECBStateByte = _TM548EcbStatus.getEcbStatus();
        return rtnTM548ECBStateByte;
    }

    public static byte setTM548EcbPowerOff() { 
        byte TM548EcbStatus = _TM548EcbStatus.getEcbStatus();
        byte rtnTM548ECBStateByte = (byte) (TM548EcbStatus);

        byte setEcbBoardStateId = (byte) ~((byte)(TM548ECBState.TM548_ECB_B2_BIT_BOARD.getByteValue()));
        TM548EcbStatus = (byte) (TM548EcbStatus & setEcbBoardStateId);

        _TM548EcbStatus.onEcbStatus(TM548EcbStatus);

	    rtnTM548ECBStateByte = _TM548EcbStatus.getEcbStatus();
        return rtnTM548ECBStateByte;
    }

    public static byte setTM548EcbEstopResistance() {
        byte TM548EcbStatus = _TM548EcbStatus.getEcbStatus();
        byte rtnTM548ECBStateByte = (byte) (TM548EcbStatus);

        byte setEcbResistanceStateId = (byte) ~((byte)(TM548ECBState.TM548_ECB_B1_BIT_RESISTANCE.getByteValue()));
        TM548EcbStatus = (byte) (TM548EcbStatus & setEcbResistanceStateId);

        _TM548EcbStatus.onEcbStatus(TM548EcbStatus);
        rtnTM548ECBStateByte = _TM548EcbStatus.getEcbStatus();
        return rtnTM548ECBStateByte;
    }

    public static byte setTM548EcbEstopMotorOffBrake() { 
        byte TM548EcbStatus = _TM548EcbStatus.getEcbStatus();
        byte rtnTM548ECBStateByte = (byte) (TM548EcbStatus);

        byte setEcbMotorStateId = (byte) ~((byte)(TM548ECBState.TM548_ECB_B0_BIT_MOTOR.getByteValue()));
        TM548EcbStatus = (byte) (TM548EcbStatus & setEcbMotorStateId);

	    _TM548EcbStatus.onEcbStatus(TM548EcbStatus);
	    rtnTM548ECBStateByte = _TM548EcbStatus.getEcbStatus();
        return rtnTM548ECBStateByte;
    }

    public static byte setTM548EcbEstopMotor2StageBrake() { 
        byte TM548EcbStatus = _TM548EcbStatus.getEcbStatus();
        byte rtnTM548ECBStateByte = (byte) (TM548EcbStatus);

        byte setEcbMotorStateId = TM548ECBState.TM548_ECB_B0_BIT_MOTOR.getByteValue();
        TM548EcbStatus = (byte) (TM548EcbStatus | setEcbMotorStateId);

	    _TM548EcbStatus.onEcbStatus(TM548EcbStatus);
    	 rtnTM548ECBStateByte = _TM548EcbStatus.getEcbStatus();
        return rtnTM548ECBStateByte;
    }

    public static byte setTM548EcbEstopRelease() {
        byte TM548EcbStatus = (byte) DEFAULT_TM548_STATUS_BYTE;
        byte rtnTM548ECBStateByte = (byte) (TM548EcbStatus);

        byte setEcbBoardStateId = TM548ECBState.TM548_ECB_B2_BIT_BOARD.getByteValue();
        TM548EcbStatus = (byte) (TM548EcbStatus | setEcbBoardStateId);

        byte setEcbResistanceStateId = (byte) ~((byte)(TM548ECBState.TM548_ECB_B1_BIT_RESISTANCE.getByteValue()));
        TM548EcbStatus = (byte) (TM548EcbStatus & setEcbResistanceStateId);

	    _TM548EcbStatus.onEcbStatus(TM548EcbStatus);
	    rtnTM548ECBStateByte = _TM548EcbStatus.getEcbStatus();
        return rtnTM548ECBStateByte;
    }

    public static byte setTM548EcbWorkoutRelease() { 
        byte TM548EcbStatus = _TM548EcbStatus.getEcbStatus();
        byte rtnTM548ECBStateByte = (byte) (TM548EcbStatus);

        byte setEcbResistanceStateId = TM548ECBState.TM548_ECB_B1_BIT_RESISTANCE.getByteValue();
        TM548EcbStatus = (byte) (TM548EcbStatus | setEcbResistanceStateId);

	_TM548EcbStatus.onEcbStatus(TM548EcbStatus);

	 rtnTM548ECBStateByte = _TM548EcbStatus.getEcbStatus();
        return rtnTM548ECBStateByte;
    }
	
    public static byte setTM548EcbWorkoutBrake() { 
        byte TM548EcbStatus = _TM548EcbStatus.getEcbStatus();
        byte rtnTM548ECBStateByte = (byte) (TM548EcbStatus);

        byte setEcbResistanceStateId = (byte) ~((byte)(TM548ECBState.TM548_ECB_B1_BIT_RESISTANCE.getByteValue()));
        TM548EcbStatus = (byte) (TM548EcbStatus & setEcbResistanceStateId);

	_TM548EcbStatus.onEcbStatus(TM548EcbStatus);

	 rtnTM548ECBStateByte = _TM548EcbStatus.getEcbStatus();
        return rtnTM548ECBStateByte;
    }
	

}
