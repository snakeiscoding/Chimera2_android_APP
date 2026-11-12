package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMClimb;

public class CHIMClimbWriteFlashDataManager {
//    private static final String TAG = CHIMClimbWriteFlashDataManager.class.getName();

    private CHIMClimbinfoAddrLen mCHIMClimbinfoAddrLen;

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

    private CHIMClimbWriteFlashDataBlock mCHIMClimbWriteFlashDataBlock[] = new CHIMClimbWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_NUMBER.value()];
    private int mWriteFlashDataBlockNumber = WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_NUMBER.value();
    private int mWriteFlashDataBlockIndex = 0;
    private String mBinFilePathName;
    private int mWriteFlashDataLenBlock1 = 0;
    private int mWriteFlashDataLenBlock2 = 0;
    private int mWriteFlashDataLenBlock3 = 0;
    private float mWriteFlashDataTotalNum = 0;
    private float mWriteFlashDataProgressNum = 0;
    private float mWriteFlashDataPercent = 0;

    public CHIMClimbWriteFlashDataManager() {
        //new CHIMClimbWriteFlashDataBlock
        for (int i = 0; i < mWriteFlashDataBlockNumber; i++) {
            mCHIMClimbWriteFlashDataBlock[i] = new CHIMClimbWriteFlashDataBlock();
        }
    }

    /*******************************************************************************
     * Function Name  : initialWriteFlashData()
     * Description    : initial Write Flash Data block
     * Input          : String binFilePathName , CHIMClimbinfoAddrLen phcLCBinfoAddrLen
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void initialWriteFlashData(String binFilePathName, CHIMClimbinfoAddrLen phcLCBinfoAddrLen) {
        mCHIMClimbinfoAddrLen = phcLCBinfoAddrLen;
        //CHIMClimb WriteFlashData block 1
        mWriteFlashDataLenBlock1 = mCHIMClimbinfoAddrLen.getBinFileLength(CHIMClimbinfoAddrLen.BinAction.BIN_ACTION_READ, CHIMClimbinfoAddrLen.BinBlock.BIN_BLOCK_1);
        mCHIMClimbWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value()].setBaseBlock(
                mCHIMClimbinfoAddrLen.getBinFileAddress(CHIMClimbinfoAddrLen.BinAction.BIN_ACTION_READ, CHIMClimbinfoAddrLen.BinBlock.BIN_BLOCK_1)
                , mWriteFlashDataLenBlock1
                , mCHIMClimbinfoAddrLen.getFlashDataAddress(CHIMClimbinfoAddrLen.FlashAction.FLASH_ACTION_WRITE, CHIMClimbinfoAddrLen.FlashBlock.FLASH_BLOCK_1));

        //CHIMClimb WriteFlashData block 2
        mWriteFlashDataLenBlock2 = mCHIMClimbinfoAddrLen.getBinFileLength(CHIMClimbinfoAddrLen.BinAction.BIN_ACTION_READ, CHIMClimbinfoAddrLen.BinBlock.BIN_BLOCK_2);
        mCHIMClimbWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_2.value()].setBaseBlock(
                mCHIMClimbinfoAddrLen.getBinFileAddress(CHIMClimbinfoAddrLen.BinAction.BIN_ACTION_READ, CHIMClimbinfoAddrLen.BinBlock.BIN_BLOCK_2)
                , mWriteFlashDataLenBlock2
                , mCHIMClimbinfoAddrLen.getFlashDataAddress(CHIMClimbinfoAddrLen.FlashAction.FLASH_ACTION_WRITE, CHIMClimbinfoAddrLen.FlashBlock.FLASH_BLOCK_2));

        //CHIMClimb WriteFlashData block 3
        mWriteFlashDataLenBlock3 = mCHIMClimbinfoAddrLen.getBinFileLength(CHIMClimbinfoAddrLen.BinAction.BIN_ACTION_READ, CHIMClimbinfoAddrLen.BinBlock.BIN_BLOCK_3);
        mCHIMClimbWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_3.value()].setBaseBlock(
                mCHIMClimbinfoAddrLen.getBinFileAddress(CHIMClimbinfoAddrLen.BinAction.BIN_ACTION_READ, CHIMClimbinfoAddrLen.BinBlock.BIN_BLOCK_3)
                , mWriteFlashDataLenBlock3
                , mCHIMClimbinfoAddrLen.getFlashDataAddress(CHIMClimbinfoAddrLen.FlashAction.FLASH_ACTION_WRITE, CHIMClimbinfoAddrLen.FlashBlock.FLASH_BLOCK_3));

        //init CHIMClimb WriteFlashData calc Percent
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
        if ((mCHIMClimbWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value()].isReadBinFileEnd()) &&
                (mCHIMClimbWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_2.value()].isReadBinFileEnd()) &&
                (mCHIMClimbWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_3.value()].isReadBinFileEnd())) {
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
        if (mCHIMClimbWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value()].isReadBinFileEnd() == false) {
            mWriteFlashDataBlockIndex = WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value();
        } else if (mCHIMClimbWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_2.value()].isReadBinFileEnd() == false) {
            mWriteFlashDataBlockIndex = WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_2.value();
        } else if (mCHIMClimbWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_3.value()].isReadBinFileEnd() == false) {
            mWriteFlashDataBlockIndex = WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_3.value();
        } else {
            return false;
        }

        //calc and process write flash data and bin file data
        mCHIMClimbWriteFlashDataBlock[mWriteFlashDataBlockIndex].procWriteFlashDataBinFileData(mBinFilePathName);
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
        return mCHIMClimbWriteFlashDataBlock[mWriteFlashDataBlockIndex].getRtnWriteFlashDataAddr();
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

        rtnWriteFashDataBlockLen = mCHIMClimbWriteFlashDataBlock[mWriteFlashDataBlockIndex].getRtnWriteFlashDataLen();
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
        return mCHIMClimbWriteFlashDataBlock[mWriteFlashDataBlockIndex].getRtnWriteFlashData();
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
