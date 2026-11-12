package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMAthena;

public class CHIMAthenainfoAddrLen {
    private static final String TAG = "CHIMAthenainfoAddrLen";

    private AthenaLCBinfo mAthenaLCBinfo = new AthenaLCBinfo();

    private static final int LCB_TYPE_Athena = 0x00;

    private static int _lcbType = LCB_TYPE_Athena;	//default value
    /************************************************************************************
    *    Flash Data Address, Length and info
    *************************************************************************************/
    // CHIMAthena base address
    private int phcLcbBase               = 0x08000000;	//default value

    // === Erase Block ===>
    // BLOCK_A address offset
    private int phcLcbBlockAoffset       = 0x0001FC00;	//default value
    // BLOCK_A address
    private int phcLcbBlockAaddr         = phcLcbBase + phcLcbBlockAoffset;	//default value

    // BLOCK_A length
    // Length  = 0x1FFFF - 0x1FC00 +1
    private int phcLcbBlockAlen          = 0x00000400;		//default value

    // BLOCK_B address offset
    private int phcLcbBlockBoffset       = 0x00006000;	//default value
    // BLOCK_B address
    private int phcLcbBlockBaddr         = phcLcbBase + phcLcbBlockBoffset;	//default value

    // BLOCK_B length
    // Length  = 0x1FBFF - 0x6000 +1
    private int phcLcbBlockBlen          = 0x00019C00;		//default value
    //<=== Erase Block ===

    // === Write Block ==>
    // BLOCK_1 address offset
    private int phcLcbBlock1writeOffset  = 0x00006000;	//default value
    // BLOCK_1 write address
    private int phcLcbBlock1writeAddr    = phcLcbBase + phcLcbBlock1writeOffset;

    // WriteLen = 0x1F7FF - 0x6000 +1
    private int phcLcbBlock1writeLen     = 0x00019800;	//default value

    // BLOCK_2 address offset
    private int phcLcbBlock2writeOffsect = 0x0001F800;	//default value
//    private int phcLcbBlock2writeOffsect = 0x00000310;	//default value
    // BLOCK_2 write address
    private int phcLcbBlock2writeAddr    = phcLcbBase + phcLcbBlock2writeOffsect;

    // WriteLen = 0x1FBFF - 0x1F800 +1
    private int phcLcbBlock2writeLen     = 0x000003F0;	//default value

    // BLOCK_3 address offset
    private int phcLcbBlock3writeOffset  = 0x0001FC00;	//default value
    // BLOCK_3 write address
    private int phcLcbBlock3writeAddr    = phcLcbBase + phcLcbBlock3writeOffset;

    // WriteLen = 0x1FEFF - 0x1FC00 +1
    private int phcLcbBlock3writeLen     = 0x000003B0;	//default value
    //<=== Write Block ===

    // === Read Block ===>
    // BLOCK_1 read address
    private int pchLcbBlock1readAddr     = phcLcbBlock1writeAddr;

    private int phcLcbBlock1readLen      = phcLcbBlock1writeLen;


    // BLOCK_2 read address
    private int phcLcbBlock2readAddr     = phcLcbBlock2writeAddr;

    private int phcLcbBlock2readLen      = phcLcbBlock2writeLen;

    // BLOCK_3 read address
    private int phcLcbBlock3readAddr     = phcLcbBlock3writeAddr;

    private int phcLcbBlock3readLen      = phcLcbBlock3writeLen;

    //<=== Read Block ===

    public enum FlashAction {
        FLASH_ACTION_ERASE,
        FLASH_ACTION_WRITE,
        FLASH_ACTION_READ
    }

    public enum FlashBlock {
        FLASH_BLOCK_A,
        FLASH_BLOCK_B,
        FLASH_BLOCK_1,
        FLASH_BLOCK_2,
        FLASH_BLOCK_3
    }

    /************************************************************************************
    *    Bin File Address , Length and info
    *************************************************************************************/
    // MCU Type address
    private int phcLcbMcuTypeAddr             = 0x0001FF4C;    //default value
    //Module Name address
    private int phcLcbModuleNameAddr          = 0x0001FF60;    //default value
    //Module No address
    private int phcLcbModuleNoAddr            = 0x0001FF74;    //default value

    private char[] LCB_McuType;
    private char[] LCB_ModuleName;
    private char[] LCB_ModuleNo;

    // === Bin File Read Block 1 ==>
    // === Bin File Block 1
    // CHIMAthena bin file base address
    private int phcLcbBinFileBase             = 0x00000000;    //default value
    // BLOCK_1 bin file address offset
    private int phcLcbBinFileBlock1readOffset = 0x00006000;    //default value

    // BLOCK_1 bin file read address
    private int phcLcbBinFileBlock1readAddr   = phcLcbBinFileBase + phcLcbBinFileBlock1readOffset;
    // BLOCK_1 bin file  ReadLen = 0x1F7FF - 0x6000 +1
    private int phcLcbBinFileBlock1readLen    = phcLcbBlock1writeLen; //0x00019800

    // === Bin File Block 2
    // BLOCK_2 bin file address offset
//    private int phcLcbBinFileBlock2readOffset = 0x0001F800;    //default value
    private int phcLcbBinFileBlock2readOffset = 0x00000310;    //default value
    // BLOCK_2 bin file read address
    private int phcLcbBinFileBlock2readAddr   = phcLcbBinFileBase + phcLcbBinFileBlock2readOffset;
    // BLOCK_2 bin file  ReadLen = 0x6FF - 0x310 +1
    private int phcLcbBinFileBlock2readLen    = phcLcbBlock2writeLen; // 0x000003F0;

    // === Bin File Block 3
    // BLOCK_3 bin file address offset
    private int phcLcbBinFileBlock3readOffset = 0x0001FC00;    //default value
    // BLOCK_3 bin file read address
    private int phcLcbBinFileBlock3readAddr   = phcLcbBinFileBase + phcLcbBinFileBlock3readOffset;
    // BLOCK_3 bin file  ReadLen = 0x1FFB0 - 0x1FC00 +1
    private int phcLcbBinFileBlock3readLen    = phcLcbBlock3writeLen; // 0x000003B1

    // <=== Bin File Read Block
    public enum BinAction {
        BIN_ACTION_READ
    }

    public enum BinBlock {
        BIN_BLOCK_1,
        BIN_BLOCK_2,
        BIN_BLOCK_3
    }

    public CHIMAthenainfoAddrLen(){
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
        }

        return rtnEraseAddress;
    }

    private int getWriteFlashDataAddress(FlashBlock flashBlock) {
        int rtnWriteAddress = 0x00000000;

        switch (flashBlock) {
            case FLASH_BLOCK_1:
                rtnWriteAddress = phcLcbBlock1writeAddr;
                break;
            case FLASH_BLOCK_2:
                rtnWriteAddress = phcLcbBlock2writeAddr;
                break;
            case FLASH_BLOCK_3:
                rtnWriteAddress = phcLcbBlock3writeAddr;
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
            case FLASH_BLOCK_2:
                rtnReadAddress = phcLcbBlock2readAddr;
                break;
            case FLASH_BLOCK_3:
                rtnReadAddress = phcLcbBlock3readAddr;
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
        }

        return rtnEraseLen;
    }

    private int getWriteFlashDataLen(FlashBlock flashBlock) {
        int rtnWriteLen = 0x00000000;

        switch (flashBlock) {
            case FLASH_BLOCK_1:
                rtnWriteLen = phcLcbBlock1writeLen;
                break;
            case FLASH_BLOCK_2:
                rtnWriteLen = phcLcbBlock2writeLen;
                break;
            case FLASH_BLOCK_3:
                rtnWriteLen = phcLcbBlock3writeLen;
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
            case FLASH_BLOCK_2:
                rtnReadLen = phcLcbBlock2readLen;
                break;
            case FLASH_BLOCK_3:
                rtnReadLen = phcLcbBlock3readLen;
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
            case BIN_BLOCK_2:
                rtnReadAddress = phcLcbBinFileBlock2readAddr;
                break;
            case BIN_BLOCK_3:
                rtnReadAddress = phcLcbBinFileBlock3readAddr;
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
            case BIN_BLOCK_2:
                rtnReadLen = phcLcbBinFileBlock2readLen;
                break;
            case BIN_BLOCK_3:
                rtnReadLen = phcLcbBinFileBlock3readLen;
                break;
        }

        return rtnReadLen;
    }

    public int getBinFileMcuTypeAddr() {
        return phcLcbMcuTypeAddr;
    }

    public char[] getMcuType() {
        char[] RtnChar = new char[20];

        for (int i = 0; i < LCB_McuType.length; i++) {
            RtnChar[i] = LCB_McuType[i];
        }

        return RtnChar;
    }

    public int getBinFileModuleNameAddr() {
        return phcLcbModuleNameAddr;
    }

    public char[] getModuleName() {
        char[] RtnChar = new char[20];

        for (int i = 0; i < LCB_ModuleName.length; i++) {
            RtnChar[i] = LCB_ModuleName[i];
        }

        return RtnChar;
    }

    public int getBinFileModuleNoAddr() {
        return phcLcbModuleNoAddr;
    }

    public char[] getModuleNo() {
        char[] RtnChar = new char[20];

        for (int i = 0; i < LCB_ModuleNo.length; i++) {
            RtnChar[i] = LCB_ModuleNo[i];
        }

        return RtnChar;
    }
//<=================== Bin File =========================

    public void setLcbTypeUpdate(int lcbType) {
        _lcbType = lcbType;
        initialLcbTypeUpdateData();
    }

    private void initialLcbTypeUpdateData() {

        switch (_lcbType) {
            case LCB_TYPE_Athena:
                initAthenaData();
                break;
        }
        
    }

    private void initAthenaData(){

        /************************************************************************************
        *    Flash Data Address, Length and info
        *************************************************************************************/
        // CHIMAthena base address
        phcLcbBase               = mAthenaLCBinfo.AthenaLCB_BASE_;

        // === Erase Block ===>
        // BLOCK_A address offset
        phcLcbBlockAoffset       = mAthenaLCBinfo.AthenaLCB_BLOCK_A_OFFSET_;
        // BLOCK_A address
        phcLcbBlockAaddr         = phcLcbBase + phcLcbBlockAoffset;

        // BLOCK_A length
        // Length  = 0x1FFFF - 0x1FC00 +1
        phcLcbBlockAlen          = mAthenaLCBinfo.AthenaLCB_BLOCK_A_LEN_;

        // BLOCK_B address offset
        phcLcbBlockBoffset       = mAthenaLCBinfo.AthenaLCB_BLOCK_B_OFFSET_;
        // BLOCK_B address
        phcLcbBlockBaddr         = phcLcbBase + phcLcbBlockBoffset;

        // BLOCK_B length
        // Length  = 0x1FBFF - 0x6000 +1
        phcLcbBlockBlen          = mAthenaLCBinfo.AthenaLCB_BLOCK_B_LEN_;
        //<=== Erase Block ===

        // === Write Block ==>
        // BLOCK_1 address offset
        phcLcbBlock1writeOffset  = mAthenaLCBinfo.AthenaLCB_BLOCK_1_WRITE_OFFSET_;
        // BLOCK_1 write address
        phcLcbBlock1writeAddr    = phcLcbBase + phcLcbBlock1writeOffset;

        // WriteLen = 0x1F7FF - 0x6000 +1
        phcLcbBlock1writeLen     = mAthenaLCBinfo.AthenaLCB_BLOCK_1_WRITE_LEN_;

        // BLOCK_2 address offset
        phcLcbBlock2writeOffsect = mAthenaLCBinfo.AthenaLCB_BLOCK_2_WRITE_OFFSET_;
        // BLOCK_2 write address
        phcLcbBlock2writeAddr    = phcLcbBase + phcLcbBlock2writeOffsect;

        // WriteLen = 0x1FBEF - 0x1F800 +1
        phcLcbBlock2writeLen     = mAthenaLCBinfo.AthenaLCB_BLOCK_2_WRITE_LEN_;

        // BLOCK_3 address offset
        phcLcbBlock3writeOffset  = mAthenaLCBinfo.AthenaLCB_BLOCK_3_WRITE_OFFSET_;
        // BLOCK_3 write address
        phcLcbBlock3writeAddr    = phcLcbBase + phcLcbBlock3writeOffset;

        // WriteLen = 0x1FFAF - 0x1FC00 +1
        phcLcbBlock3writeLen     = mAthenaLCBinfo.AthenaLCB_BLOCK_3_WRITE_LEN_;
        //<=== Write Block ===

        // === Read Block ===>
        // BLOCK_1 read address
        pchLcbBlock1readAddr     = phcLcbBlock1writeAddr;

        phcLcbBlock1readLen      = phcLcbBlock1writeLen;

        // BLOCK_2 read address
        phcLcbBlock2readAddr     = phcLcbBlock2writeAddr;

        phcLcbBlock2readLen      = phcLcbBlock2writeLen;

        // BLOCK_3 read address
        phcLcbBlock3readAddr     = phcLcbBlock3writeAddr;

        phcLcbBlock3readLen      = phcLcbBlock3writeLen;

        /************************************************************************************
        *    Bin File Address , Length and info
        *************************************************************************************/
        // MCU Type address
        phcLcbMcuTypeAddr        = mAthenaLCBinfo.AthenaLCB_MCU_TYPE_ADDR_;
        //Module Name address
        phcLcbModuleNameAddr     = mAthenaLCBinfo.AthenaLCB_MODULE_NAME_ADDR_;
        //Module No address
        phcLcbModuleNoAddr       = mAthenaLCBinfo.AthenaLCB_MODULE_NO_ADDR_;

        LCB_McuType = new char[mAthenaLCBinfo.AthenaLCB_McuType.length];
        for (int i = 0; i < mAthenaLCBinfo.AthenaLCB_McuType.length; i++) {
            LCB_McuType[i] = mAthenaLCBinfo.AthenaLCB_McuType[i];
        }

        LCB_ModuleName = new char[mAthenaLCBinfo.AthenaLCB_ModuleName.length];
        for (int j = 0; j < mAthenaLCBinfo.AthenaLCB_ModuleName.length; j++) {
            LCB_ModuleName[j] = mAthenaLCBinfo.AthenaLCB_ModuleName[j];
        }

        LCB_ModuleNo = new char[mAthenaLCBinfo.AthenaLCB_ModuleNo.length];
        for (int k = 0; k < mAthenaLCBinfo.AthenaLCB_ModuleNo.length; k++) {
            LCB_ModuleNo[k] = mAthenaLCBinfo.AthenaLCB_ModuleNo[k];
        }
        // === Bin File Read Block 1 ==>
        // === Bin File Block 1
        // CHIMAthena bin file base address
        phcLcbBinFileBase               = mAthenaLCBinfo.AthenaLCB_BIN_FILE_BASE_;
        // BLOCK_1 bin file address offset
        phcLcbBinFileBlock1readOffset   = mAthenaLCBinfo.AthenaLCB_BIN_FILE_BLOCK_1_READ_OFFSET_;
        // BLOCK_1 bin file read address
        phcLcbBinFileBlock1readAddr     = phcLcbBinFileBase + phcLcbBinFileBlock1readOffset;
        // BLOCK_1 bin file  ReadLen = 0x1F7FF - 0x6000 +1
        phcLcbBinFileBlock1readLen      = phcLcbBlock1writeLen ; //0x00019800

        // === Bin File Block 2
        // BLOCK_2 bin file address offset
        phcLcbBinFileBlock2readOffset   = mAthenaLCBinfo.AthenaLCB_BIN_FILE_BLOCK_2_READ_OFFSET_;
        // BLOCK_2 bin file read address
        phcLcbBinFileBlock2readAddr     = phcLcbBinFileBase + phcLcbBinFileBlock2readOffset;
        // BLOCK_2 bin file  ReadLen = 0x6FF - 0x310 +1
        phcLcbBinFileBlock2readLen      = phcLcbBlock2writeLen ; // 0x000003F0;

        // === Bin File Block 3
        // BLOCK_3 bin file address offset
        phcLcbBinFileBlock3readOffset   = mAthenaLCBinfo.AthenaLCB_BIN_FILE_BLOCK_3_READ_OFFSET_;
        // BLOCK_3 bin file read address
        phcLcbBinFileBlock3readAddr     = phcLcbBinFileBase + phcLcbBinFileBlock3readOffset;
        // BLOCK_3 bin file  ReadLen = 0x1FFB0 - 0x1FC00 +1
        phcLcbBinFileBlock3readLen      = phcLcbBlock3writeLen ; // 0x000003B1
    }

}

