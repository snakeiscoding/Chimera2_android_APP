package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMLCB1;

public class CHIMLCB1infoAddrLen {
//    private static final String TAG = CHIMLCB1infoAddrLen.class.getName();

    private LCB1LCBinfo mLCB1LCBinfo = new LCB1LCBinfo();

    private static final int LCB_TYPE_LCB1 = 0x02;

    private static int _lcbType = LCB_TYPE_LCB1;    //default value
    /************************************************************************************
     *    Flash Data Address, Length and info
     *************************************************************************************/
    // CHIMLCB1 base address
    private int phcLcbBase = 0x00000000;    //default value

    // === Erase Block ===>
    // BLOCK_A address offset
    private int phcLcbBlockAoffset = 0x00004000;    //default value
    // BLOCK_A address
    private int phcLcbBlockAaddr = phcLcbBase + phcLcbBlockAoffset;    //default value

    // BLOCK_A length
    private int phcLcbBlockAlen = 0x00008000;        //default value

    // BLOCK_B address offset
    private int phcLcbBlockBoffset = 0x00002400;    //default value
    // BLOCK_B address
    private int phcLcbBlockBaddr = phcLcbBase + phcLcbBlockBoffset;    //default value

    // BLOCK_B length
    private int phcLcbBlockBlen = 0x00000400;        //default value

    // BLOCK_C address offset
    private int phcLcbBlockCoffset = 0x00002800;    //default value
    // BLOCK_C address
    private int phcLcbBlockCaddr = phcLcbBase + phcLcbBlockCoffset;    //default value

    // BLOCK_C length
    private int phcLcbBlockClen = 0x00000400;        //default value
    //<=== Erase Block ===

    // === Write Block ==>
    // BLOCK_1 address offset
    private int phcLcbBlock1writeOffset = 0x00004000 + 0x00000030;    //default value
    // BLOCK_1 write address
    private int phcLcbBlock1writeAddr = phcLcbBase + phcLcbBlock1writeOffset;

    private int phcLcbBlock1writeLen = 0x00008000 - 0x00000030;    //default value
    //<=== Write Block ===

    // === Read Block ===>
    // BLOCK_1 read address
    private int pchLcbBlock1readAddr = phcLcbBlock1writeAddr;

    private int phcLcbBlock1readLen = phcLcbBlock1writeLen;
    //<=== Read Block ===

    public enum FlashAction {
        FLASH_ACTION_ERASE,
        FLASH_ACTION_WRITE,
        FLASH_ACTION_READ
    }

    public enum FlashBlock {
        FLASH_BLOCK_A,
        FLASH_BLOCK_B,
        FLASH_BLOCK_C,
        FLASH_BLOCK_1
    }

    /************************************************************************************
     *    Bin File Address , Length and info
     *************************************************************************************/
    private int phcLcbBinNameAddr = 0x00004030;    //default value

    private char[] LCB_BinName;

    // === Bin File Read Block 1 ==>
    // === Bin File Block 1
    // CHIMLCB1 bin file base address
    private int phcLcbBinFileBase = 0x00000000;    //default value
    // BLOCK_1 bin file address offset
    private int phcLcbBinFileBlock1readOffset = 0x00004000 + 0x00000030;    //default value

    // BLOCK_1 bin file read address
    private int phcLcbBinFileBlock1readAddr = phcLcbBinFileBase + phcLcbBinFileBlock1readOffset;
    private int phcLcbBinFileBlock1readLen = phcLcbBlock1writeLen;

    // bin file CheckCode address offset
    private int phcLcbBinFileCheckCodereadOffset = 0x00004000 + 0x00000020;    //default value

    // <=== Bin File Read Block
    public enum BinAction {
        BIN_ACTION_READ
    }

    public enum BinBlock {
        BIN_BLOCK_1
    }

    public CHIMLCB1infoAddrLen() {
    }

//=================== Flash Data =========================>

    public int getFlashDataAddress(FlashAction flashAction, FlashBlock flashBlock) {
        int rtnAddress = 0x00000000;

        switch (flashAction) {
            case FLASH_ACTION_ERASE:
                rtnAddress = getEreaseFlashDataAddress(flashBlock);
                break;
            case FLASH_ACTION_WRITE:
                rtnAddress = getWriteFlashDataAddress(flashBlock);
                break;
            case FLASH_ACTION_READ:
                rtnAddress = getReadFlashDataAddress(flashBlock);
                break;
        }

        return rtnAddress;
    }

    private int getEreaseFlashDataAddress(FlashBlock flashBlock) {
        int rtnEraseAddress = 0x00000000;

        switch (flashBlock) {
            case FLASH_BLOCK_A:
                rtnEraseAddress = phcLcbBlockAaddr;
                break;
            case FLASH_BLOCK_B:
                rtnEraseAddress = phcLcbBlockBaddr;
                break;
            case FLASH_BLOCK_C:
                rtnEraseAddress = phcLcbBlockCaddr;
                break;
        }

        return rtnEraseAddress;
    }

    private int getWriteFlashDataAddress(FlashBlock flashBlock) {
        int rtnWriteAddress = 0x00000000;

        switch (flashBlock) {
            case FLASH_BLOCK_1:
                rtnWriteAddress = phcLcbBlock1writeAddr;
                break;
        }

        return rtnWriteAddress;
    }

    private int getReadFlashDataAddress(FlashBlock flashBlock) {
        int rtnReadAddress = 0x00000000;

        switch (flashBlock) {
            case FLASH_BLOCK_1:
                rtnReadAddress = pchLcbBlock1readAddr;
                break;
        }

        return rtnReadAddress;
    }

    public int getFlashDataLength(FlashAction flashAction, FlashBlock flashBlock) {
        int rtnLength = 0x00000000;
        switch (flashAction) {
            case FLASH_ACTION_ERASE:
                rtnLength = getEreaseFlashDataLen(flashBlock);
                break;
            case FLASH_ACTION_WRITE:
                rtnLength = getWriteFlashDataLen(flashBlock);
                break;
            case FLASH_ACTION_READ:
                rtnLength = getReadFlashDataLen(flashBlock);
                break;
        }

        return rtnLength;
    }

    private int getEreaseFlashDataLen(FlashBlock flashBlock) {
        int rtnEraseLen = 0x00000000;

        switch (flashBlock) {
            case FLASH_BLOCK_A:
                rtnEraseLen = phcLcbBlockAlen;
                break;
            case FLASH_BLOCK_B:
                rtnEraseLen = phcLcbBlockBlen;
                break;
            case FLASH_BLOCK_C:
                rtnEraseLen = phcLcbBlockClen;
                break;
        }

        return rtnEraseLen;
    }

    private int getWriteFlashDataLen(FlashBlock flashBlock) {
        int rtnWriteLen = 0x00000000;

        switch (flashBlock) {
            case FLASH_BLOCK_1:
                rtnWriteLen = phcLcbBlock1writeLen;
                break;
        }

        return rtnWriteLen;
    }

    private int getReadFlashDataLen(FlashBlock flashBlock) {
        int rtnReadLen = 0x00000000;

        switch (flashBlock) {
            case FLASH_BLOCK_1:
                rtnReadLen = phcLcbBlock1readLen;
                break;
        }

        return rtnReadLen;
    }
//<=================== Flash Data =========================

//=================== Bin File =========================>

    public int getBinFileAddress(BinAction binAction, BinBlock binBlock) {
        int rtnAddress = 0x00000000;

        switch (binAction) {
            case BIN_ACTION_READ:
                rtnAddress = getReadBinFileAddress(binBlock);
                break;
        }

        return rtnAddress;
    }


    private int getReadBinFileAddress(BinBlock binBlock) {
        int rtnReadAddress = 0x00000000;

        switch (binBlock) {
            case BIN_BLOCK_1:
                rtnReadAddress = phcLcbBinFileBlock1readAddr;
                break;
        }

        return rtnReadAddress;
    }

    public int getBinFileLength(BinAction binAction, BinBlock binBlock) {
        int rtnLength = 0x00000000;
        switch (binAction) {
            case BIN_ACTION_READ:
                rtnLength = getReadBinFileLen(binBlock);
                break;
        }

        return rtnLength;
    }

    private int getReadBinFileLen(BinBlock binBlock) {
        int rtnReadLen = 0x00000000;

        switch (binBlock) {
            case BIN_BLOCK_1:
                rtnReadLen = phcLcbBinFileBlock1readLen;
                break;
        }

        return rtnReadLen;
    }

    public int getBinFileBinNameAddr() {
        return phcLcbBinNameAddr;
    }

    public char[] getBinName() {
        char[] RtnChar = new char[14];

        for (int i = 0; i < LCB_BinName.length; i++) {
            RtnChar[i] = LCB_BinName[i];
        }

        return RtnChar;
    }

    public int getBinFileCheckCode() {
        return phcLcbBinFileCheckCodereadOffset;
    }

//<=================== Bin File =========================

    public void setLcbTypeUpdate(int lcbType) {
        _lcbType = lcbType;
        initialLcbTypeUpdateData();
    }

    private void initialLcbTypeUpdateData() {

        switch (_lcbType) {
            case LCB_TYPE_LCB1:
                initLcb1Data();
                break;
        }

    }

    private void initLcb1Data() {

        /************************************************************************************
         *    Flash Data Address, Length and info
         *************************************************************************************/
        // CHIMLCB1 base address
        phcLcbBase = mLCB1LCBinfo.LCB1LCB_BASE_;

        // === Erase Block ===>
        // BLOCK_A address offset
        phcLcbBlockAoffset = mLCB1LCBinfo.LCB1LCB_BLOCK_A_OFFSET_;
        // BLOCK_A address
        phcLcbBlockAaddr = phcLcbBase + phcLcbBlockAoffset;

        // BLOCK_A length
        phcLcbBlockAlen = mLCB1LCBinfo.LCB1LCB_BLOCK_A_LEN_;

        // BLOCK_B address offset
        phcLcbBlockBoffset = mLCB1LCBinfo.LCB1LCB_BLOCK_B_OFFSET_;
        // BLOCK_B address
        phcLcbBlockBaddr = phcLcbBase + phcLcbBlockBoffset;

        // BLOCK_B length
        phcLcbBlockBlen = mLCB1LCBinfo.LCB1LCB_BLOCK_B_LEN_;

        // BLOCK_C address offset
        phcLcbBlockCoffset = mLCB1LCBinfo.LCB1LCB_BLOCK_C_OFFSET_;
        // BLOCK_C address
        phcLcbBlockCaddr = phcLcbBase + phcLcbBlockCoffset;

        // BLOCK_C length
        phcLcbBlockClen = mLCB1LCBinfo.LCB1LCB_BLOCK_C_LEN_;
        //<=== Erase Block ===

        // === Write Block ==>
        // BLOCK_1 address offset
        phcLcbBlock1writeOffset = mLCB1LCBinfo.LCB1LCB_BLOCK_1_WRITE_OFFSET_;
        // BLOCK_1 write address
        phcLcbBlock1writeAddr = phcLcbBase + phcLcbBlock1writeOffset;

        phcLcbBlock1writeLen = mLCB1LCBinfo.LCB1LCB_BLOCK_1_WRITE_LEN_;

        //<=== Write Block ===

        // === Read Block ===>
        // BLOCK_1 read address
        pchLcbBlock1readAddr = phcLcbBlock1writeAddr;

        phcLcbBlock1readLen = phcLcbBlock1writeLen;

        /************************************************************************************
         *    Bin File Address , Length and info
         *************************************************************************************/
        //Bin Name address
        phcLcbBinNameAddr = mLCB1LCBinfo.LCB1LCB_BIN_NAME_ADDR_;

        LCB_BinName = new char[mLCB1LCBinfo.LCB1LCB_BinName.length];
        for (int j = 0; j < mLCB1LCBinfo.LCB1LCB_BinName.length; j++) {
            LCB_BinName[j] = mLCB1LCBinfo.LCB1LCB_BinName[j];
        }
        // === Bin File Read Block 1 ==>
        // === Bin File Block 1
        // CHIMLCB1 bin file base address
        phcLcbBinFileBase = mLCB1LCBinfo.LCB1LCB_BIN_FILE_BASE_;
        // BLOCK_1 bin file address offset
        phcLcbBinFileBlock1readOffset = mLCB1LCBinfo.LCB1LCB_BIN_FILE_BLOCK_1_READ_OFFSET_;
        // BLOCK_1 bin file read address
        phcLcbBinFileBlock1readAddr = phcLcbBinFileBase + phcLcbBinFileBlock1readOffset;
        phcLcbBinFileBlock1readLen = phcLcbBlock1writeLen;

    }

}

