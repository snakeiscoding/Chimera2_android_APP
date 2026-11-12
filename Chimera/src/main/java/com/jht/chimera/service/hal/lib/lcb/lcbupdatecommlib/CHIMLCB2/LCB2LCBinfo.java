package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMLCB2;

public class LCB2LCBinfo {
    private static final String TAG = "LCB2LCBinfo";
    /************************************************************************************
     *    Flash Data Address, Length and info
     *************************************************************************************/
    // LCB2LCB base address
    public final int LCB2LCB_BASE_ = 0x00000000;

    // === Erase Block ===>
    // BLOCK_A address offset
    public final int LCB2LCB_BLOCK_A_OFFSET_ = 0x00004000;

    // BLOCK_A length
    public final int LCB2LCB_BLOCK_A_LEN_ = 0x00008000;

    // BLOCK_B address offset
    public final int LCB2LCB_BLOCK_B_OFFSET_ = 0x00002400;

    // BLOCK_B length
    public final int LCB2LCB_BLOCK_B_LEN_ = 0x00000400;

    // BLOCK_C address offset
    public final int LCB2LCB_BLOCK_C_OFFSET_ = 0x00002800;

    // BLOCK_C length
    public final int LCB2LCB_BLOCK_C_LEN_ = 0x00000400;
    //<=== Erase Block ===

    // === Write Block  ==>
    // BLOCK_1 address offset (Program Code)
    public final int LCB2LCB_BLOCK_1_WRITE_OFFSET_ = 0x00004000 + 0x00000030;

    // WriteLen
    public final int LCB2LCB_BLOCK_1_WRITE_LEN_ = 0x00008000 - 0x00000030;

    //<=== Write Block ===


    /************************************************************************************
     *    Bin File Address , Length and info
     *************************************************************************************/
    //Bin Name address
    public final int LCB2LCB_BIN_NAME_ADDR_ = 0x00004030;

    public final char[] LCB2LCB_BinName = {'S', '7', '0', 'A', '5', 'X', 'L', 'C', 'B', 'S', '0', '0', '1', '.', 'B', 'I', 'N'};

    // === Bin File Read Block 1 ==>
    // === Bin File Block 1
    // LCB2LCB bin file base address
    public final int LCB2LCB_BIN_FILE_BASE_ = 0x00000000;
    // BLOCK_1 bin file address offset
    public final int LCB2LCB_BIN_FILE_BLOCK_1_READ_OFFSET_ = 0x00004000 + 0x00000030;

    public LCB2LCBinfo() {
    }
}
