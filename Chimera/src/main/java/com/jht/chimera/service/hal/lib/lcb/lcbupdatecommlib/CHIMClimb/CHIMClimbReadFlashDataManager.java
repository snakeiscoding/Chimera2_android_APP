package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMClimb;

public class CHIMClimbReadFlashDataManager {
//    private static final String TAG = CHIMClimbReadFlashDataManager.class.getName();

    private CHIMClimbinfoAddrLen mCHIMClimbinfoAddrLen;

    public enum ReadFlashDataBlock {
        READ_FLASH_DATA_BLOCK_1(0),
        READ_FLASH_DATA_BLOCK_2(1),
        READ_FLASH_DATA_BLOCK_3(2),
        READ_FLASH_DATA_BLOCK_NUMBER(3);

        private int value;

        private ReadFlashDataBlock(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    private CHIMClimbReadFlashDataBlock mCHIMClimbReadFlashDataBlock[] = new CHIMClimbReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_NUMBER.value()];
    private int mReadFlashDataBlockNumber = ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_NUMBER.value();
    private int mReadFlashDataBlockIndex = 0;
    private String mBinFilePathName;
    private int mReadFlashDataLenBlock1 = 0;
    private int mReadFlashDataLenBlock2 = 0;
    private int mReadFlashDataLenBlock3 = 0;
    private float mReadFlashDataTotalNum = 0;
    private float mReadFlashDataProgressNum = 0;
    private float mReadFlashDataPercent = 0;

    public CHIMClimbReadFlashDataManager() {
        //new CHIMClimbReadFlashDataBlock
        for (int i = 0; i < mReadFlashDataBlockNumber; i++) {
            mCHIMClimbReadFlashDataBlock[i] = new CHIMClimbReadFlashDataBlock();
        }
    }

    /*******************************************************************************
     * Function Name  : initialReadFlashData()
     * Description    : initial Read Flash Data block
     * Input          : String binFilePathName ,  CHIMClimbinfoAddrLen phcLCBinfoAddrLen
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void initialReadFlashData(String binFilePathName, CHIMClimbinfoAddrLen phcLCBinfoAddrLen) {
        mCHIMClimbinfoAddrLen = phcLCBinfoAddrLen;
        //CHIMClimb ReadFlashData block 1
        mReadFlashDataLenBlock1 = mCHIMClimbinfoAddrLen.getFlashDataLength(CHIMClimbinfoAddrLen.FlashAction.FLASH_ACTION_READ, CHIMClimbinfoAddrLen.FlashBlock.FLASH_BLOCK_1);
        mCHIMClimbReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_1.value()].setBaseBlock(
                mCHIMClimbinfoAddrLen.getFlashDataAddress(CHIMClimbinfoAddrLen.FlashAction.FLASH_ACTION_READ, CHIMClimbinfoAddrLen.FlashBlock.FLASH_BLOCK_1)
                , mReadFlashDataLenBlock1
                , mCHIMClimbinfoAddrLen.getBinFileAddress(CHIMClimbinfoAddrLen.BinAction.BIN_ACTION_READ, CHIMClimbinfoAddrLen.BinBlock.BIN_BLOCK_1));

        //CHIMClimb ReadFlashData block 2
        mReadFlashDataLenBlock2 = mCHIMClimbinfoAddrLen.getFlashDataLength(CHIMClimbinfoAddrLen.FlashAction.FLASH_ACTION_READ, CHIMClimbinfoAddrLen.FlashBlock.FLASH_BLOCK_2);
        mCHIMClimbReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_2.value()].setBaseBlock(
                mCHIMClimbinfoAddrLen.getFlashDataAddress(CHIMClimbinfoAddrLen.FlashAction.FLASH_ACTION_READ, CHIMClimbinfoAddrLen.FlashBlock.FLASH_BLOCK_2)
                , mReadFlashDataLenBlock2
                , mCHIMClimbinfoAddrLen.getBinFileAddress(CHIMClimbinfoAddrLen.BinAction.BIN_ACTION_READ, CHIMClimbinfoAddrLen.BinBlock.BIN_BLOCK_2));

        //CHIMClimb ReadFlashData block 3
        mReadFlashDataLenBlock3 = mCHIMClimbinfoAddrLen.getFlashDataLength(CHIMClimbinfoAddrLen.FlashAction.FLASH_ACTION_READ, CHIMClimbinfoAddrLen.FlashBlock.FLASH_BLOCK_3);
        mCHIMClimbReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_3.value()].setBaseBlock(
                mCHIMClimbinfoAddrLen.getFlashDataAddress(CHIMClimbinfoAddrLen.FlashAction.FLASH_ACTION_READ, CHIMClimbinfoAddrLen.FlashBlock.FLASH_BLOCK_3)
                , mReadFlashDataLenBlock3
                , mCHIMClimbinfoAddrLen.getBinFileAddress(CHIMClimbinfoAddrLen.BinAction.BIN_ACTION_READ, CHIMClimbinfoAddrLen.BinBlock.BIN_BLOCK_3));

        //init CHIMClimb ReadFlashData calc Percent
        initReadFlashDataPercent();

        //set first read flash data block 1
        mReadFlashDataBlockIndex = ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_1.value();

        mBinFilePathName = binFilePathName;

    }

    /*******************************************************************************
     * Function Name  : isReadFlashDataBlockEnd()
     * Description    : check all FlashData block read end
     * Input          : None
     * Output         : None
     * Return         : boolean all block flash data read finish
     *******************************************************************************/
    public boolean isAllBlockFinish() {
        if ((mCHIMClimbReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_1.value()].isReadFlashDataEnd())
                && (mCHIMClimbReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_2.value()].isReadFlashDataEnd())
                && (mCHIMClimbReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_3.value()].isReadFlashDataEnd())
        ) {
            return true;
        }

        return false;
    }

    /*******************************************************************************
     * Function Name  : getNextData()
     * Description    : process flash data and bin file data to read
     * Input          : None
     * Output         : None
     * Return         : boolean have data to check
     *******************************************************************************/
    public boolean getNextBlockData() {
        //check which block is not end
        if (mCHIMClimbReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_1.value()].isReadFlashDataEnd() == false) {
            mReadFlashDataBlockIndex = ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_1.value();
        } else if (mCHIMClimbReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_2.value()].isReadFlashDataEnd() == false) {
            mReadFlashDataBlockIndex = ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_2.value();
        } else if (mCHIMClimbReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_3.value()].isReadFlashDataEnd() == false) {
            mReadFlashDataBlockIndex = ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_3.value();
        } else {
            return false;
        }

        //calc and process read flash data and bin file data
        mCHIMClimbReadFlashDataBlock[mReadFlashDataBlockIndex].procReadFlashDataBinFileData(mBinFilePathName);
        return true;
    }

    /*******************************************************************************
     * Function Name  : getBlockReadFlashDataAddr()
     * Description    : get read FlashData block addr
     * Input          : None
     * Output         : None
     * Return         : int addr
     *******************************************************************************/
    public int getBlockReadFlashDataAddr() {
        //get read FlashData block addr
        return mCHIMClimbReadFlashDataBlock[mReadFlashDataBlockIndex].getRtnReadFlashDataAddr();
    }

    /*******************************************************************************
     * Function Name  : getBlockReadFlashDataLen()
     * Description    : get read FlashData block len
     * Input          : None
     * Output         : None
     * Return         : int len
     *******************************************************************************/
    public int getBlockReadFlashDataLen() {
        int rtnReadFashDataBlockLen = 0;

        rtnReadFashDataBlockLen = mCHIMClimbReadFlashDataBlock[mReadFlashDataBlockIndex].getRtnReadFlashDataLen();
        //set ReadFlashDataPercentProgressNum , but not calc
        setReadFlashDataPercentProgressNum(rtnReadFashDataBlockLen);
        //get read FlashData block len
        return rtnReadFashDataBlockLen;
    }

    /*******************************************************************************
     * Function Name  : compReadFlashDataAndBinFile()
     * Description    : return read FlashData compare with BinFile
     * Input          : byte[] readData
     * Output         : None
     * Return         : boolean data are same or not
     *******************************************************************************/
    public boolean compReadFlashDataAndBinFile(byte[] readData) {
        byte[] binFileData = mCHIMClimbReadFlashDataBlock[mReadFlashDataBlockIndex].getRtnReadBinFile();

        for (int i = 0; i < readData.length; i++) {
            if (readData[i] != binFileData[i]) {
                // Read Flash Data bytes compare with Bin File data bytes not match
                return false;
            }
        }

        return true;
    }

    /*******************************************************************************
     * Function Name  : initReadFlashDataPercent()
     * Description    : init read FlashData Percent
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void initReadFlashDataPercent() {
        float readFlashDataLenBlock1 = (float) mReadFlashDataLenBlock1;
        float readFlashDataLenBlock2 = (float) mReadFlashDataLenBlock2;
        float readFlashDataLenBlock3 = (float) mReadFlashDataLenBlock3;

        mReadFlashDataTotalNum = 0;
        mReadFlashDataProgressNum = 0;
        mReadFlashDataPercent = 0;

        mReadFlashDataTotalNum = readFlashDataLenBlock1 + readFlashDataLenBlock2 + readFlashDataLenBlock3;
    }

    /*******************************************************************************
     * Function Name  : setReadFlashDataPercentProgressNum(int reduceNum)
     * Description    : set ReadFlashDataPercentProgressNum
     * Input          : int reduceNum
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void setReadFlashDataPercentProgressNum(int reduceNum) {
        if (reduceNum < 0)
            return;

        float floatProgressNum = (float) reduceNum;
        float tempProgressNum = mReadFlashDataProgressNum + floatProgressNum;

        if (tempProgressNum > mReadFlashDataTotalNum)
            tempProgressNum = mReadFlashDataTotalNum;

        mReadFlashDataProgressNum = tempProgressNum;
    }

    /*******************************************************************************
     * Function Name  : calcReadFlashDataPercent()
     * Description    : calc read FlashData Percent
     * Input          : None
     * Output         : None
     * Return         : float Percent
     *******************************************************************************/
    public float calcReadFlashDataPercent() {
        float rtnPercent = 0;
        if ((mReadFlashDataTotalNum <= 0) || (mReadFlashDataProgressNum <= 0)) {
            rtnPercent = 0;
            return rtnPercent;
        }

        if (mReadFlashDataProgressNum > mReadFlashDataTotalNum) {
            mReadFlashDataProgressNum = mReadFlashDataTotalNum;
        }

        mReadFlashDataPercent = mReadFlashDataProgressNum / mReadFlashDataTotalNum;
        rtnPercent = mReadFlashDataPercent;

        return rtnPercent;

    }
}
