package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMLCB2;

public class CHIMLCB2WriteFlashDataManager {
//    private static final String TAG = CHIMLCB2WriteFlashDataManager.class.getName();

    private CHIMLCB2infoAddrLen mCHIMLCB2infoAddrLen;

    public enum WriteFlashDataBlock {
        WRITE_FLASH_DATA_BLOCK_1(0),
        WRITE_FLASH_DATA_BLOCK_NUMBER(1);

        private int value;

        private WriteFlashDataBlock(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    private CHIMLCB2WriteFlashDataBlock mCHIMLCB2WriteFlashDataBlock[] = new CHIMLCB2WriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_NUMBER.value()];
    private int mWriteFlashDataBlockNumber = WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_NUMBER.value();
    private int mWriteFlashDataBlockIndex = 0;
    private String mBinFilePathName;
    private int mWriteFlashDataLenBlock1 = 0;
    private float mWriteFlashDataTotalNum = 0;
    private float mWriteFlashDataProgressNum = 0;
    private float mWriteFlashDataPercent = 0;

    public CHIMLCB2WriteFlashDataManager() {
        //new CHIMLCB2WriteFlashDataBlock
        for (int i = 0; i < mWriteFlashDataBlockNumber; i++) {
            mCHIMLCB2WriteFlashDataBlock[i] = new CHIMLCB2WriteFlashDataBlock();
        }
    }

    /*******************************************************************************
     * Function Name  : initialWriteFlashData()
     * Description    : initial Write Flash Data block
     * Input          : String binFilePathName , CHIMLCB2infoAddrLen phcLCBinfoAddrLen
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void initialWriteFlashData(String binFilePathName, CHIMLCB2infoAddrLen phcLCBinfoAddrLen) {
        mCHIMLCB2infoAddrLen = phcLCBinfoAddrLen;
        //CHIMLCB2 WriteFlashData block 1
        mWriteFlashDataLenBlock1 = mCHIMLCB2infoAddrLen.getBinFileLength(CHIMLCB2infoAddrLen.BinAction.BIN_ACTION_READ, CHIMLCB2infoAddrLen.BinBlock.BIN_BLOCK_1);
        mCHIMLCB2WriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value()].setBaseBlock(
                mCHIMLCB2infoAddrLen.getBinFileAddress(CHIMLCB2infoAddrLen.BinAction.BIN_ACTION_READ, CHIMLCB2infoAddrLen.BinBlock.BIN_BLOCK_1)
                , mWriteFlashDataLenBlock1
                , mCHIMLCB2infoAddrLen.getFlashDataAddress(CHIMLCB2infoAddrLen.FlashAction.FLASH_ACTION_WRITE, CHIMLCB2infoAddrLen.FlashBlock.FLASH_BLOCK_1));

        //init CHIMLCB2 WriteFlashData calc Percent
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
        if ((mCHIMLCB2WriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value()].isReadBinFileEnd())
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
        if (mCHIMLCB2WriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value()].isReadBinFileEnd() == false) {
            mWriteFlashDataBlockIndex = WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value();
        } else {
            return false;
        }

        //calc and process write flash data and bin file data
        mCHIMLCB2WriteFlashDataBlock[mWriteFlashDataBlockIndex].procWriteFlashDataBinFileData(mBinFilePathName);
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
        return mCHIMLCB2WriteFlashDataBlock[mWriteFlashDataBlockIndex].getRtnWriteFlashDataAddr();
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

        rtnWriteFashDataBlockLen = mCHIMLCB2WriteFlashDataBlock[mWriteFlashDataBlockIndex].getRtnWriteFlashDataLen();
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
        return mCHIMLCB2WriteFlashDataBlock[mWriteFlashDataBlockIndex].getRtnWriteFlashData();
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

        mWriteFlashDataTotalNum = 0;
        mWriteFlashDataProgressNum = 0;
        mWriteFlashDataPercent = 0;

        mWriteFlashDataTotalNum = writeFlashDataLenBlock1;
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
