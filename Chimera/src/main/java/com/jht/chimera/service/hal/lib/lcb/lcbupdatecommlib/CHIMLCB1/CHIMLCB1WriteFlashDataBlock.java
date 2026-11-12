package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMLCB1;

import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.BinFileManager;

public class CHIMLCB1WriteFlashDataBlock {
//    private static final String TAG = CHIMLCB1WriteFlashDataBlock.class.getName();

    //bin file info
    private BinFileManager mBinFileManager = new BinFileManager();
    private int binFileReadAddrBase;
    private int binFileReadLenBase;
    private int binFileReadAddrCurr;
    private int binFileReadLenRemaining;
    private int binFileReadAddrRtn;
    private int binFileReadLenRtn;
    private int binFileReadCounter;
    private int binFileReadIndex; //index must < binFileReadCounter
    private boolean isFileReadCounterEnd = false;

    //flash data info
    private int flashDataWriteAddrBase;
    private int flashDataWriteAddrCurr;
    private int flashDataWriteAddrRtn;

    //write flash data info
    private byte[] rtnWriteFlashData;
    private int rtnWriteFlashDataLen;
    private int rtnWriteFlashDataAddr;

    private final int BIN_FILE_READ_LENGTH = 512; //once read 512 bytes

    public CHIMLCB1WriteFlashDataBlock() {
    }

    public boolean setBaseBlock(int binFileBlockAddr, int binFileBlockLength, int flashDataBlockAddr) {

        if (binFileBlockLength == 0) {
            isFileReadCounterEnd = true;
            return false;
        }

        // set bin file block  addr & len
        binFileReadAddrBase = binFileBlockAddr;
        binFileReadLenBase = binFileBlockLength;
        // set flash data  addr
        flashDataWriteAddrBase = flashDataBlockAddr;

        //init calculate bin file read block
        initCalcBlockData();
        return true;
    }

    private void initCalcBlockData() {
        //set read bin file count and index
        if ((binFileReadLenBase % BIN_FILE_READ_LENGTH) == 0)
            binFileReadCounter = (binFileReadLenBase / BIN_FILE_READ_LENGTH);
        else
            binFileReadCounter = (binFileReadLenBase / BIN_FILE_READ_LENGTH) + 1;

        binFileReadIndex = 0;

        //set read bin file addr and len
        binFileReadAddrCurr = binFileReadAddrBase;
        binFileReadLenRemaining = binFileReadLenBase;

        //set write flash data addr
        flashDataWriteAddrCurr = flashDataWriteAddrBase;

        //set return bin file addr
        binFileReadAddrRtn = binFileReadAddrCurr;
        //set return bin file len
        if (binFileReadLenRemaining < BIN_FILE_READ_LENGTH) {
            binFileReadLenRtn = binFileReadLenRemaining;
        } else {
            binFileReadLenRtn = BIN_FILE_READ_LENGTH;
            binFileReadLenRemaining = binFileReadLenRemaining - BIN_FILE_READ_LENGTH;
        }

        //set return flash data addr
        flashDataWriteAddrRtn = flashDataWriteAddrCurr;

        isFileReadCounterEnd = false;


    }

    public boolean isReadBinFileEnd() {
        return isFileReadCounterEnd;
    }

    public void procWriteFlashDataBinFileData(String binFilePathName) {
        int readBlockAddr = 0;
        int writeBlockAddr = 0;
        int readBlockLen = 0;

        //important first get BinFile addr and FlashData addr  then get len
        readBlockAddr = getReadBinFileBlockAddr();
        writeBlockAddr = getWriteFlashDataBlockAddr();
        readBlockLen = getReadBinFileBlockLen();
        byte[] RtnByte = new byte[readBlockLen];
        mBinFileManager.readBinFileDataByte(binFilePathName, readBlockAddr, RtnByte, readBlockLen);

        rtnWriteFlashData = RtnByte;
        rtnWriteFlashDataLen = readBlockLen;
        rtnWriteFlashDataAddr = writeBlockAddr;
    }

    public int getRtnWriteFlashDataAddr() {
        return rtnWriteFlashDataAddr;
    }

    public int getRtnWriteFlashDataLen() {
        return rtnWriteFlashDataLen;
    }

    public byte[] getRtnWriteFlashData() {
        return rtnWriteFlashData;
    }

    private int getReadBinFileBlockAddr() {
        int rtnAddr;

        if (isFileReadCounterEnd == true)
            return -1;

        rtnAddr = binFileReadAddrRtn;
        calcAddrReadBinFileBlock();
        return rtnAddr;
    }

    private void calcAddrReadBinFileBlock() {
        binFileReadAddrCurr = binFileReadAddrCurr + binFileReadLenRtn;
        binFileReadAddrRtn = binFileReadAddrCurr;
    }

    private int getWriteFlashDataBlockAddr() {
        int rtnAddr;

        if (isFileReadCounterEnd == true)
            return -1;

        rtnAddr = flashDataWriteAddrRtn;
        calcAddrWriteFlashDataBlock();
        return rtnAddr;
    }

    private void calcAddrWriteFlashDataBlock() {
        flashDataWriteAddrCurr = flashDataWriteAddrCurr + binFileReadLenRtn;
        flashDataWriteAddrRtn = flashDataWriteAddrCurr;
    }

    private int getReadBinFileBlockLen() {
        int rtnLen;

        if (isFileReadCounterEnd == true)
            return -1;

        rtnLen = binFileReadLenRtn;
        binFileReadIndex++;
        calcLenReadBinFileBlock();
        return rtnLen;
    }

    private void calcLenReadBinFileBlock() {
        if (binFileReadIndex < binFileReadCounter) {
            if (binFileReadLenRemaining < BIN_FILE_READ_LENGTH) {
                binFileReadLenRtn = binFileReadLenRemaining;
            } else {
                binFileReadLenRtn = BIN_FILE_READ_LENGTH;
                binFileReadLenRemaining = binFileReadLenRemaining - BIN_FILE_READ_LENGTH;
            }
            isFileReadCounterEnd = false;
        } else {
            isFileReadCounterEnd = true;
        }
    }

}
