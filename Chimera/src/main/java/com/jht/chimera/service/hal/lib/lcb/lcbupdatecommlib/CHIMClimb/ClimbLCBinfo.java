package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMClimb;

import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;

public class ClimbLCBinfo {
    private static final String TAG = "ClimbLCBinfo";
    /************************************************************************************
     *    Flash Data Address, Length and info
     *************************************************************************************/
    // ClimbLCB base address
    public final int ClimbLCB_BASE_ = 0x08000000;

    // === Erase Block ===>
    // BLOCK_A address offset
    public final int ClimbLCB_BLOCK_A_OFFSET_ = 0x0001FC00;

    // BLOCK_A length
    // Length  = 0x1FFFF - 0x1FC00 +1
    public final int ClimbLCB_BLOCK_A_LEN_ = 0x00000400;

    // BLOCK_B address offset
    public final int ClimbLCB_BLOCK_B_OFFSET_ = 0x00006000;

    // BLOCK_B length
    // Length  = 0x1FBFF - 0x6000 +1
    public final int ClimbLCB_BLOCK_B_LEN_ = 0x00019C00;
    //<=== Erase Block ===

    // === Write Block  ==>
    // BLOCK_1 address offset (Program Code)
    public final int ClimbLCB_BLOCK_1_WRITE_OFFSET_ = 0x00006000;

    // WriteLen = 0x1F7FF - 0x6000 +1
    public final int ClimbLCB_BLOCK_1_WRITE_LEN_ = 0x00019800;

    // BLOCK_2 address offset ( Memory Initial Table)
    public final int ClimbLCB_BLOCK_2_WRITE_OFFSET_ = 0x0001F800;

    // WriteLen = 0x1FBFF - 0x1F800 +1
    public final int ClimbLCB_BLOCK_2_WRITE_LEN_ = 0x00000400;

    // BLOCK_3 address offset ( Interrupt Vector Table)
    public final int ClimbLCB_BLOCK_3_WRITE_OFFSET_ = 0x0001FC00;

    // WriteLen = 0x1FFAF - 0x1FC00 +1
    public final int ClimbLCB_BLOCK_3_WRITE_LEN_ = 0x000003B0;
    //<=== Write Block ===


    /************************************************************************************
     *    Bin File Address , Length and info
     *************************************************************************************/
    // MCU Type address
    public final int ClimbLCB_MCU_TYPE_ADDR_ = 0x0001FF4C;
    //Module Name address
    public final int ClimbLCB_MODULE_NAME_ADDR_ = 0x0001FF60;
    //Module No address
    public final int ClimbLCB_MODULE_NO_ADDR_ = 0x0001FF74;

    public final char[] ClimbLCB_McuType = {'S', 'T', 'M', '3', '2', 'F', '1', '0', '2', 'R', 'B'};

    // === Bin File Read Block 1 ==>
    // === Bin File Block 1
    // ClimbLCB bin file base address
    public final int ClimbLCB_BIN_FILE_BASE_ = 0x00000000;
    // BLOCK_1 bin file address offset
    public final int ClimbLCB_BIN_FILE_BLOCK_1_READ_OFFSET_ = 0x00006000;

    // === Bin File Block 2
    // BLOCK_2 bin file address offset
    public final int ClimbLCB_BIN_FILE_BLOCK_2_READ_OFFSET_ = 0x00000310;

    // === Bin File Block 3
    // BLOCK_3 bin file address offset
    public final int ClimbLCB_BIN_FILE_BLOCK_3_READ_OFFSET_ = 0x0001FC00;

    public static int consoleLcbType = LcbType.LCB_Commercial_Climbmill.Value;

    // These values should be used for updating the 0x17 LCB. Switch this out with the two below if it's 0x19
    private final char[] ClimbLCB_ModuleName = {'E', 'N', ' ', 'C', 'l', 'i', 'm', 'b', 'M', 'i', 'l', 'l'};
    private final char[] ClimbLCB_ModuleNo = {'E', 'N', ' ', 'C', 'l', 'i', 'm', 'b', 'M', 'i', 'l', 'l'};

    // These values should be used for updating the 0x19 LCB. This is the only difference needed for updating
    private final char[] ClimbLCB_RetailModuleName = {'R', 'e', 't', 'a', 'i', 'l', ' ', 'C', 'l', 'i', 'm', 'b', 'M', 'i', 'l', 'l'};
    private final char[] ClimbLCB_RetailModuleNo = {'R', 'e', 't', 'a', 'i', 'l', ' ', 'C', 'l', 'i', 'm', 'b', 'M', 'i', 'l', 'l'};

    public char[] getClimbmillLcbModuleName() {
        // If this is retail climbmill LCB we need to send different module name
        if (consoleLcbType == LcbType.LCB_Retail_Climbmill.Value) {
            return ClimbLCB_RetailModuleName;
        } else {
            return ClimbLCB_ModuleName;
        }
    }

    public char[] getClimbmillModuleNo() {
        // If this is retail climbmill LCB we need to send different module no
        if (consoleLcbType == LcbType.LCB_Retail_Climbmill.Value) {
            return ClimbLCB_RetailModuleNo;
        } else {
            return ClimbLCB_ModuleNo;
        }
    }

    public ClimbLCBinfo() {
    }
}
