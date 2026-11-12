package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMLCB1;

import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.BinFileManager;

public class CHIMLCB1ReadFlashDataBlock {
//    private static final String TAG = CHIMLCB1ReadFlashDataBlock.class.getName();

    //flash data info
    private int flashDataReadAddrBase;
    private int flashDataReadLenBase;
    private int flashDataReadAddrCurr;
    private int flashDataReadLenRemaining;
    private int flashDataReadAddrRtn;
    private int flashDataReadLenRtn;
    private int flashDataReadCounter;
    private int flashDataReadIndex; //index must < flashDataReadCounter
    private boolean isFlashDataReadCounterEnd = false;

    //bin file info
    private BinFileManager mBinFileManager = new BinFileManager();
    private int binFileReadAddrBase;
    private int binFileReadAddrCurr;
    private int binFileReadAddrRtn;

    //read flash data info and bin file info
    private int rtnReadFlashDataAddr;
    private int rtnReadFlashDataLen;
    private byte[] rtnReadBinFile;

    private final int FLASH_DATA_READ_LENGTH = 512; //once read 512 bytes

    public CHIMLCB1ReadFlashDataBlock() {
    }

    public boolean setBaseBlock(int flashDataBlockAddr, int flashDataBlockLength, int binFileBlockAddr) {
        if (flashDataBlockLength == 0) {
            isFlashDataReadCounterEnd = true;
            return false;
        }

        // set flash data block  addr & len
        flashDataReadAddrBase = flashDataBlockAddr;
        flashDataReadLenBase = flashDataBlockLength;
        // set bin file  addr
        binFileReadAddrBase = binFileBlockAddr;

        //init calculate bin file read block
        initCalcBlockData();
        return true;
    }

    private void initCalcBlockData() {
        //set read flash data count and index
        if ((flashDataReadLenBase % FLASH_DATA_READ_LENGTH) == 0)
            flashDataReadCounter = (flashDataReadLenBase / FLASH_DATA_READ_LENGTH);
        else
            flashDataReadCounter = (flashDataReadLenBase / FLASH_DATA_READ_LENGTH) + 1;

        flashDataReadIndex = 0;

        //set read flash data addr and len
        flashDataReadAddrCurr = flashDataReadAddrBase;
        flashDataReadLenRemaining = flashDataReadLenBase;

        //set read bin file addr
        binFileReadAddrCurr = binFileReadAddrBase;

        //set return flash data addr
        flashDataReadAddrRtn = flashDataReadAddrCurr;
        //set return flash data len
        if (flashDataReadLenRemaining < FLASH_DATA_READ_LENGTH) {
            flashDataReadLenRtn = flashDataReadLenRemaining;
        } else {
            flashDataReadLenRtn = FLASH_DATA_READ_LENGTH;
            flashDataReadLenRemaining = flashDataReadLenRemaining - FLASH_DATA_READ_LENGTH;
        }

        //set return bin file addr
        binFileReadAddrRtn = binFileReadAddrCurr;

        isFlashDataReadCounterEnd = false;
    }

    public boolean isReadFlashDataEnd() {
        return isFlashDataReadCounterEnd;
    }

    public void procReadFlashDataBinFileData(String binFilePathName) {
        int readFlashDataBlockAddr = 0;
        int readBinFileBlockAddr = 0;
        int readFlashDataBlockLen = 0;
        int readBinFileBlockLen = 0;

        //important first get FlashData addr and BinFile addr  then get flash data len
        readFlashDataBlockAddr = getReadFlashDataBlockAddr();
        readBinFileBlockAddr = getReadBinFileBlockAddr();
        readFlashDataBlockLen = getReadFlashDataBlockLen();
        readBinFileBlockLen = readFlashDataBlockLen;

        byte[] RtnByte = new byte[readBinFileBlockLen];
        mBinFileManager.readBinFileDataByte(binFilePathName, readBinFileBlockAddr, RtnByte, readBinFileBlockLen);

        rtnReadFlashDataAddr = readFlashDataBlockAddr;
        rtnReadFlashDataLen = readFlashDataBlockLen;
        rtnReadBinFile = RtnByte;
    }

    public int getRtnReadFlashDataAddr() {
        return rtnReadFlashDataAddr;
    }

    public int getRtnReadFlashDataLen() {
        return rtnReadFlashDataLen;
    }

    public byte[] getRtnReadBinFile() {
        return rtnReadBinFile;
    }

    private int getReadFlashDataBlockAddr() {
        int rtnAddr;

        if (isFlashDataReadCounterEnd == true)
            return -1;

        rtnAddr = flashDataReadAddrRtn;
        calcAddrReadFlashDataBlock();

        return rtnAddr;
    }

    private void calcAddrReadFlashDataBlock() {
        flashDataReadAddrCurr = flashDataReadAddrCurr + flashDataReadLenRtn;
        flashDataReadAddrRtn = flashDataReadAddrCurr;
    }

    private int getReadBinFileBlockAddr() {
        int rtnAddr;

        if (isFlashDataReadCounterEnd == true)
            return -1;

        rtnAddr = binFileReadAddrRtn;
        calcAddrReadBinFileBlock();

        return rtnAddr;
    }

    private void calcAddrReadBinFileBlock() {
        binFileReadAddrCurr = binFileReadAddrCurr + flashDataReadLenRtn;
        binFileReadAddrRtn = binFileReadAddrCurr;
    }

    private int getReadFlashDataBlockLen() {
        int rtnLen;

        if (isFlashDataReadCounterEnd == true)
            return -1;

        rtnLen = flashDataReadLenRtn;
        flashDataReadIndex++;
        calcLenReadFlashDataBlock();

        return rtnLen;
    }

    private void calcLenReadFlashDataBlock() {
        if (flashDataReadIndex < flashDataReadCounter) {
            if (flashDataReadLenRemaining < FLASH_DATA_READ_LENGTH) {
                flashDataReadLenRtn = flashDataReadLenRemaining;
            } else {
                flashDataReadLenRtn = FLASH_DATA_READ_LENGTH;
                flashDataReadLenRemaining = flashDataReadLenRemaining - FLASH_DATA_READ_LENGTH;
            }
            isFlashDataReadCounterEnd = false;
        } else {
            isFlashDataReadCounterEnd = true;
        }
    }
}
