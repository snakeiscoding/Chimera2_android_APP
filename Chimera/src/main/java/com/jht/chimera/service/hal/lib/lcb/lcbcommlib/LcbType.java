package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

public enum LcbType {
    LCB_AC_LCB(0x01),    //(JIS)
    LCB_LCB1_PWM(0x02),    //(JHT)
    LCB_LCB3_PWM(0x03),    //(JHT)
    LCB_DC_LCB(0x04),    //(JIS)
    LCB_LCB_CD(0x05),    //(JHT LCB1 cost down version)
    LCB_TM_LCB(0x06),    //(Panatta)
    LCB_LCB1_Current(0x07),    //(JHT)
    LCB_LCB3_Current(0x08),    //(JHT)
    LCB_LCB1(0x09),    //(Panatta)
    LCB_LCB2(0x0A),    //(JHT), A5x Redesign
    LCB_ClimbMill_or_A30(0x0B),    //(JHT)/ EP (JIS)
    LCB_BIKE(0x0C),    //(JIS Electromagnet)
    LCB_DC_LCB_200_275(0x0D),    //(JIS 2.0~2.75HP DC LCB)
    LCB_DC_LCB_300_325(0x0E),    //(JIS 3.0~3.25HP DC LCB)
    LCB_AC_LCB_200_275(0x0F),    //(JIS 2.0~2.75HP AC LCB)
    LCB_AC_LCB_300_325(0x10),    //(JIS 3.0~3.25HP AC LCB)
    LCB_AD_Board(0x11),    //(JIS Analog conversion digital board)
    LCB_ECB_Board(0x12),    //(JIS)
    LCB_LCBA(0x13),
    LCB_1x_Bike_LCB(0x14),    //(Watt control)
    LCB_Electromagnetic_brake_Incline_LCB(0x15),    //(JIS)
    LCB_Smart_Strength_Load_Cell(0x16),
    LCB_Commercial_Climbmill(0x17),
    LCB_R3xm_LCB(0x18),
    LCB_Retail_Climbmill(0x19),
    LCB_ATHENA_LCB(0x1B),
    LCB_JHT_AD_Board(0x1C),    //(JHT Analog conversion digital board)
    LCB_JIS_ATHENA(0x1D),   //JIS ICR70
    LCB_Performance_Ascent_DC36V(0x1F),
    LCB_Performance_Climbmill_DC36V(0x20),
    LCB_Performance_Bike_DC36V(0x21), // Performance Bike LCB (DC36V) LCB_Performance_Bike_DC36V
    LCB_TCV_FX(0x22),
    LCB_JHT_VAVE_PS_ES_Board(0xA1),    //(JHT VAVE A1 For T-PS/T-ES)
    LCB_JHT_VAVE_T600_Board(0xA2),    //(JHT VAVE A2 For T600/T600E)
    LCB_JHT_VAVE_LS_Board(0xA3),    //(JHT VAVE A3 For T-LS)

    LCB_Global_15_20HP_110V(0xB0),
    LCB_Global_15_20HP_220V(0xB1),
    LCB_Global_20_25HP_110V(0xB2),
    LCB_Global_20_25HP_220V(0xB3),
    LCB_ECB_JIS(0xB4),
    LCB_ECB_INDUCTOR_BRAKE(0xB5),
    LCB_Global_ECB_Incline(0xB6),
    LCB_Global_Induction(0xB7),
    LCB_Global_Induction_Incline(0xB8),
    LCB_Global_Full_Function(0xB9),

    LCB_DCI_daughter_board(0xC0),
    LCB_Toptek(0xC1),
    LCB_DELTA(0xC2),
    LCB_DELTA_AC_11KW(0xC3),
    LCB_DELTA_Hybrid(0xC4),
    LCB_DELTA_T1(0xC5),
    LCB_DELTA_spare_1(0xC6),
    LCB_DELTA_spare_2(0xC7),
    LCB_DELTA_spare_3(0xC8),
    LCB_PS_PMSM_22kw(0xC9), //(PS PMSM 2.2kW MCB (Performance Tread))
    LCB_LS_MD_MCB(0xCA),

    LCB_Global_200_325HP_110V(0xD0),    //(Global 2.0~3.25HP/110V)
    LCB_Global_200_325HP_220V(0xD1),    //(Global 2.0~3.25HP/220V)

    LCB_E0(0xE0),   // TODO - what do we call this?
    LCB_E1(0xE1),   // TODO - what do we call this?

    LCB_JHT_VAVE_PP_Board(0xF1),    //(JHT VAVE F1 For T-PP)
    LCB_JHT_PMSM_22kw(0xF2),    //(PP PMSM 2.2kW MCB)
    LCB_UNKNOWN(0x00);

    public int Value;

    LcbType(int value) {
        Value = value;
    }

    public static LcbType fromInt(int i) {
        for (LcbType b : LcbType.values()) {
            if (b.Value == i) {
                return b;
            }
        }
        return LCB_UNKNOWN;
    }

    public int getValue() {
        return Value;
    }

    public byte getByteValue() {
        byte byteValue = (byte) Value;
        return byteValue;
    }

}
