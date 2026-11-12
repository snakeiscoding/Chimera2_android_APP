package com.jht.chimera.service.hal.lib.lcb;

public class TM548ECBStatus {
    private static final String TAG = TM548ECBStatus.class.getName();
    private static byte _ecbStatus = 0x00;
    public static boolean _ecbMotor;
    public static boolean _ecbResistance;
    public static boolean _ecbBoard;

    public TM548ECBStatus(byte ecbStatus) {
        _ecbStatus = ecbStatus;
        _ecbMotor = (_ecbStatus & 0x01) != 0;
        _ecbResistance = (_ecbStatus & 0x02) != 0;
        _ecbBoard = (_ecbStatus & 0x04) != 0;

		
    }

    @Override
    public String toString() {
        return "_ecbStatus:" + "\n" +
                "\tecbMotor: " + _ecbMotor + "\n" +
                "\tecbResistance: " + _ecbResistance + "\n" +
                "\tecbBoard: " + _ecbBoard ;
    }

    public void onEcbStatus(byte ecbStatus) {
        _ecbStatus = ecbStatus;
        _ecbMotor = (_ecbStatus & 0x01) != 0;
        _ecbResistance = (_ecbStatus & 0x02) != 0;
        _ecbBoard = (_ecbStatus & 0x04) != 0;

    }

    public byte getEcbStatus() {
        return _ecbStatus;
    }

    public boolean getEcbMotor() {
        return _ecbMotor;
    }

    public boolean getEcbResistance() {
        return _ecbResistance;
    }

    public boolean getEcbBoard() {
        return _ecbBoard;
    }

	
}
