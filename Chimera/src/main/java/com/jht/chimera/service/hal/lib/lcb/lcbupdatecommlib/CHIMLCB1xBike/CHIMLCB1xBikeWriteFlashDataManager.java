package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMLCB1xBike;

public class CHIMLCB1xBikeWriteFlashDataManager {
    private static final String TAG = "CHIMLCB1xBikeWriteFl...";

    private CHIMLCB1xBikeinfoAddrLen mCHIMLCB1xBikeinfoAddrLen;

    public enum WriteFlashDataBlock {
        WRITE_FLASH_DATA_BLOCK_1(0),
        WRITE_FLASH_DATA_BLOCK_2(1),
        WRITE_FLASH_DATA_BLOCK_3(2),
        WRITE_FLASH_DATA_BLOCK_NUMBER(3);

        private int value;

        private WriteFlashDataBlock(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    private CHIMLCB1xBikeWriteFlashDataBlock mCHIMLCB1xBikeWriteFlashDataBlock[] = new CHIMLCB1xBikeWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_NUMBER.value()];
    private int mWriteFlashDataBlockNumber = WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_NUMBER.value();
    private int mWriteFlashDataBlockIndex = 0;
    private String mBinFilePathName;
    private int mWriteFlashDataLenBlock1 = 0;
    private int mWriteFlashDataLenBlock2 = 0;
    private int mWriteFlashDataLenBlock3 = 0;
    private float mWriteFlashDataTotalNum = 0;
    private float mWriteFlashDataProgressNum = 0;
    private float mWriteFlashDataPercent = 0;

    public CHIMLCB1xBikeWriteFlashDataManager() {
        //new CHIMLCB1xBikeWriteFlashDataBlock
        for (int i = 0; i < mWriteFlashDataBlockNumber; i++) {
            mCHIMLCB1xBikeWriteFlashDataBlock[i] = new CHIMLCB1xBikeWriteFlashDataBlock();
        }
    }

    /*******************************************************************************
     * Function Name  : initialWriteFlashData()
     * Description    : initial Write Flash Data block
     * Input          : String binFilePathName , CHIMLCB1xBikeinfoAddrLen phcLCBinfoAddrLen
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void initialWriteFlashData(String binFilePathName, CHIMLCB1xBikeinfoAddrLen phcLCBinfoAddrLen) {
        mCHIMLCB1xBikeinfoAddrLen = phcLCBinfoAddrLen;
        //CHIMLCB1xBike WriteFlashData block 1
        mWriteFlashDataLenBlock1 = mCHIMLCB1xBikeinfoAddrLen.getBinFileLength(CHIMLCB1xBikeinfoAddrLen.BinAction.BIN_ACTION_READ, CHIMLCB1xBikeinfoAddrLen.BinBlock.BIN_BLOCK_1);
        mCHIMLCB1xBikeWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value()].setBaseBlock(
                mCHIMLCB1xBikeinfoAddrLen.getBinFileAddress(CHIMLCB1xBikeinfoAddrLen.BinAction.BIN_ACTION_READ, CHIMLCB1xBikeinfoAddrLen.BinBlock.BIN_BLOCK_1)
                , mWriteFlashDataLenBlock1
                , mCHIMLCB1xBikeinfoAddrLen.getFlashDataAddress(CHIMLCB1xBikeinfoAddrLen.FlashAction.FLASH_ACTION_WRITE, CHIMLCB1xBikeinfoAddrLen.FlashBlock.FLASH_BLOCK_1));

        //CHIMLCB1xBike WriteFlashData block 2
        mWriteFlashDataLenBlock2 = mCHIMLCB1xBikeinfoAddrLen.getBinFileLength(CHIMLCB1xBikeinfoAddrLen.BinAction.BIN_ACTION_READ, CHIMLCB1xBikeinfoAddrLen.BinBlock.BIN_BLOCK_2);
        mCHIMLCB1xBikeWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_2.value()].setBaseBlock(
                mCHIMLCB1xBikeinfoAddrLen.getBinFileAddress(CHIMLCB1xBikeinfoAddrLen.BinAction.BIN_ACTION_READ, CHIMLCB1xBikeinfoAddrLen.BinBlock.BIN_BLOCK_2)
                , mWriteFlashDataLenBlock2
                , mCHIMLCB1xBikeinfoAddrLen.getFlashDataAddress(CHIMLCB1xBikeinfoAddrLen.FlashAction.FLASH_ACTION_WRITE, CHIMLCB1xBikeinfoAddrLen.FlashBlock.FLASH_BLOCK_2));

        //CHIMLCB1xBike WriteFlashData block 3
        mWriteFlashDataLenBlock3 = mCHIMLCB1xBikeinfoAddrLen.getBinFileLength(CHIMLCB1xBikeinfoAddrLen.BinAction.BIN_ACTION_READ, CHIMLCB1xBikeinfoAddrLen.BinBlock.BIN_BLOCK_3);
        mCHIMLCB1xBikeWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_3.value()].setBaseBlock(
                mCHIMLCB1xBikeinfoAddrLen.getBinFileAddress(CHIMLCB1xBikeinfoAddrLen.BinAction.BIN_ACTION_READ, CHIMLCB1xBikeinfoAddrLen.BinBlock.BIN_BLOCK_3)
                , mWriteFlashDataLenBlock3
                , mCHIMLCB1xBikeinfoAddrLen.getFlashDataAddress(CHIMLCB1xBikeinfoAddrLen.FlashAction.FLASH_ACTION_WRITE, CHIMLCB1xBikeinfoAddrLen.FlashBlock.FLASH_BLOCK_3));

        //init CHIMLCB1xBike WriteFlashData calc Percent
        initWriteFlashDataPercent();

        //set first write flash data block 1
        mWriteFlashDataBlockIndex = WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value();
        mBinFilePathName = binFilePathName;
    }

    /*******************************************************************************
     * Function Name  : isAllBlockFinish()
     * Description    : check all FlashData block write end
     * Input          : None
     * Output         : None
     * Return         : boolean all block flash data write finish
     *******************************************************************************/
    public boolean isAllBlockFinish() {
        if ((mCHIMLCB1xBikeWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value()].isReadBinFileEnd()) &&
                (mCHIMLCB1xBikeWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_2.value()].isReadBinFileEnd()) &&
                (mCHIMLCB1xBikeWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_3.value()].isReadBinFileEnd())) {
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
        if (mCHIMLCB1xBikeWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value()].isReadBinFileEnd() == false) {
            mWriteFlashDataBlockIndex = WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value();
        } else if (mCHIMLCB1xBikeWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_2.value()].isReadBinFileEnd() == false) {
            mWriteFlashDataBlockIndex = WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_2.value();
        } else if (mCHIMLCB1xBikeWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_3.value()].isReadBinFileEnd() == false) {
            mWriteFlashDataBlockIndex = WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_3.value();
        } else {
            return false;
        }

        //calc and process write flash data and bin file data
        mCHIMLCB1xBikeWriteFlashDataBlock[mWriteFlashDataBlockIndex].procWriteFlashDataBinFileData(mBinFilePathName);
        return true;
    }

    /*******************************************************************************
     * Function Name  : getBlockWriteFlashDataAddr()
     * Description    : get read FlashData block addr
     * Input          : None
     * Output         : None
     * Return         : int addr
     *******************************************************************************/
    public int getBlockWriteFlashDataAddr() {
        //get write FlashData block addr
        return mCHIMLCB1xBikeWriteFlashDataBlock[mWriteFlashDataBlockIndex].getRtnWriteFlashDataAddr();
    }

    /*******************************************************************************
     * Function Name  : getBlockWriteFlashDataLen()
     * Description    : get write FlashData block len
     * Input          : None
     * Output         : None
     * Return         : int len
     *******************************************************************************/
    public int getBlockWriteFlashDataLen() {
        int rtnWriteFashDataBlockLen = 0;

        rtnWriteFashDataBlockLen = mCHIMLCB1xBikeWriteFlashDataBlock[mWriteFlashDataBlockIndex].getRtnWriteFlashDataLen();
        //set WriteFlashDataPercentProgressNum , but not calc
        setWriteFlashDataPercentProgressNum(rtnWriteFashDataBlockLen);
        //get write FlashData block len
        return rtnWriteFashDataBlockLen;
    }

    /*******************************************************************************
     * Function Name  : getBlockWriteFlashData()
     * Description    : get write FlashData block data
     * Input          : None
     * Output         : None
     * Return         : byte[]
     *******************************************************************************/
    public byte[] getBlockWriteFlashData() {
        //get write FlashData block data
        return mCHIMLCB1xBikeWriteFlashDataBlock[mWriteFlashDataBlockIndex].getRtnWriteFlashData();
    }

    /*******************************************************************************
     * Function Name  : initWriteFlashDataPercent()
     * Description    : init write FlashData Percent
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void initWriteFlashDataPercent() {
        float writeFlashDataLenBlock1 = (float) mWriteFlashDataLenBlock1;
        float writeFlashDataLenBlock2 = (float) mWriteFlashDataLenBlock2;
        float writeFlashDataLenBlock3 = (float) mWriteFlashDataLenBlock3;

        mWriteFlashDataTotalNum = 0;
        mWriteFlashDataProgressNum = 0;
        mWriteFlashDataPercent = 0;

        mWriteFlashDataTotalNum = writeFlashDataLenBlock1 + writeFlashDataLenBlock2 + writeFlashDataLenBlock3;
    }

    /*******************************************************************************
     * Function Name  : setWriteFlashDataPercentProgressNum(int reduceNum)
     * Description    : set WriteFlashDataPercentProgressNum
     * Input          : int reduceNum
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void setWriteFlashDataPercentProgressNum(int reduceNum) {
        if (reduceNum < 0)
            return;

        float floatProgressNum = (float) reduceNum;
        float tempProgressNum = mWriteFlashDataProgressNum + floatProgressNum;

        if (tempProgressNum > mWriteFlashDataTotalNum)
            tempProgressNum = mWriteFlashDataTotalNum;

        mWriteFlashDataProgressNum = tempProgressNum;
    }

    /*******************************************************************************
     * Function Name  : calcWriteFlashDataPercent()
     * Description    : calc write FlashData Percent
     * Input          : None
     * Output         : None
     * Return         : float Percent
     *******************************************************************************/
    public float calcWriteFlashDataPercent() {
        float rtnPercent = 0;
        if ((mWriteFlashDataTotalNum <= 0) || (mWriteFlashDataProgressNum <= 0)) {
            rtnPercent = 0;
            return rtnPercent;
        }

        if (mWriteFlashDataProgressNum > mWriteFlashDataTotalNum) {
            mWriteFlashDataProgressNum = mWriteFlashDataTotalNum;
        }

        mWriteFlashDataPercent = mWriteFlashDataProgressNum / mWriteFlashDataTotalNum;
        rtnPercent = mWriteFlashDataPercent;

        return rtnPercent;
    }
}
