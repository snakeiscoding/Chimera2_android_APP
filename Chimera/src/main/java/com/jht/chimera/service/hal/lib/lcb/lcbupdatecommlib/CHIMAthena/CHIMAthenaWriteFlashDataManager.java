package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMAthena;

public class CHIMAthenaWriteFlashDataManager {
    private static final String TAG = "CHIMAthenaWriteFlash...";

    private CHIMAthenainfoAddrLen mCHIMAthenainfoAddrLen;

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

    private CHIMAthenaWriteFlashDataBlock mCHIMAthenaWriteFlashDataBlock[] = new CHIMAthenaWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_NUMBER.value()];
    private int mWriteFlashDataBlockNumber = WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_NUMBER.value();
    private int mWriteFlashDataBlockIndex = 0;
    private String mBinFilePathName;
    private int mWriteFlashDataLenBlock1 = 0;
    private int mWriteFlashDataLenBlock2 = 0;
    private int mWriteFlashDataLenBlock3 = 0;
    private float mWriteFlashDataTotalNum = 0;
    private float mWriteFlashDataProgressNum = 0;
    private float mWriteFlashDataPercent = 0;

    public CHIMAthenaWriteFlashDataManager() {
        //new CHIMAthenaWriteFlashDataBlock
        for (int i = 0; i < mWriteFlashDataBlockNumber; i++) {
            mCHIMAthenaWriteFlashDataBlock[i] = new CHIMAthenaWriteFlashDataBlock();
        }
    }

    /*******************************************************************************
    * Function Name  : initialWriteFlashData()
    * Description    : initial Write Flash Data block
    * Input          : String binFilePathName , CHIMAthenainfoAddrLen phcLCBinfoAddrLen
    * Output         : None
    * Return         : None
    *******************************************************************************/
    public void initialWriteFlashData(String binFilePathName, CHIMAthenainfoAddrLen phcLCBinfoAddrLen) {
        mCHIMAthenainfoAddrLen = phcLCBinfoAddrLen;
        //CHIMAthena WriteFlashData block 1
        mWriteFlashDataLenBlock1 = mCHIMAthenainfoAddrLen.getBinFileLength(CHIMAthenainfoAddrLen.BinAction.BIN_ACTION_READ, CHIMAthenainfoAddrLen.BinBlock.BIN_BLOCK_1);
        mCHIMAthenaWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value()].setBaseBlock(
                mCHIMAthenainfoAddrLen.getBinFileAddress(CHIMAthenainfoAddrLen.BinAction.BIN_ACTION_READ, CHIMAthenainfoAddrLen.BinBlock.BIN_BLOCK_1)
                , mWriteFlashDataLenBlock1
                , mCHIMAthenainfoAddrLen.getFlashDataAddress(CHIMAthenainfoAddrLen.FlashAction.FLASH_ACTION_WRITE, CHIMAthenainfoAddrLen.FlashBlock.FLASH_BLOCK_1));

        //CHIMAthena WriteFlashData block 2
        mWriteFlashDataLenBlock2 = mCHIMAthenainfoAddrLen.getBinFileLength(CHIMAthenainfoAddrLen.BinAction.BIN_ACTION_READ, CHIMAthenainfoAddrLen.BinBlock.BIN_BLOCK_2);
        mCHIMAthenaWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_2.value()].setBaseBlock(
                mCHIMAthenainfoAddrLen.getBinFileAddress(CHIMAthenainfoAddrLen.BinAction.BIN_ACTION_READ, CHIMAthenainfoAddrLen.BinBlock.BIN_BLOCK_2)
                , mWriteFlashDataLenBlock2
                , mCHIMAthenainfoAddrLen.getFlashDataAddress(CHIMAthenainfoAddrLen.FlashAction.FLASH_ACTION_WRITE, CHIMAthenainfoAddrLen.FlashBlock.FLASH_BLOCK_2));

        //CHIMAthena WriteFlashData block 3
        mWriteFlashDataLenBlock3 = mCHIMAthenainfoAddrLen.getBinFileLength(CHIMAthenainfoAddrLen.BinAction.BIN_ACTION_READ, CHIMAthenainfoAddrLen.BinBlock.BIN_BLOCK_3);
        mCHIMAthenaWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_3.value()].setBaseBlock(
                mCHIMAthenainfoAddrLen.getBinFileAddress(CHIMAthenainfoAddrLen.BinAction.BIN_ACTION_READ, CHIMAthenainfoAddrLen.BinBlock.BIN_BLOCK_3)
                , mWriteFlashDataLenBlock3
                , mCHIMAthenainfoAddrLen.getFlashDataAddress(CHIMAthenainfoAddrLen.FlashAction.FLASH_ACTION_WRITE, CHIMAthenainfoAddrLen.FlashBlock.FLASH_BLOCK_3));

        //init CHIMAthena WriteFlashData calc Percent
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
        if ((mCHIMAthenaWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value()].isReadBinFileEnd()) &&
            (mCHIMAthenaWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_2.value()].isReadBinFileEnd()) &&
            (mCHIMAthenaWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_3.value()].isReadBinFileEnd())) {
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
        if (mCHIMAthenaWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value()].isReadBinFileEnd() == false) {
            mWriteFlashDataBlockIndex = WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value();
        } else if (mCHIMAthenaWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_2.value()].isReadBinFileEnd() == false) {
            mWriteFlashDataBlockIndex = WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_2.value();
        } else if (mCHIMAthenaWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_3.value()].isReadBinFileEnd() == false) {
            mWriteFlashDataBlockIndex = WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_3.value();
        } else {
            return false;
        }

        //calc and process write flash data and bin file data
        mCHIMAthenaWriteFlashDataBlock[mWriteFlashDataBlockIndex].procWriteFlashDataBinFileData(mBinFilePathName);
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
        return mCHIMAthenaWriteFlashDataBlock[mWriteFlashDataBlockIndex].getRtnWriteFlashDataAddr();
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

        rtnWriteFashDataBlockLen = mCHIMAthenaWriteFlashDataBlock[mWriteFlashDataBlockIndex].getRtnWriteFlashDataLen();
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
        return mCHIMAthenaWriteFlashDataBlock[mWriteFlashDataBlockIndex].getRtnWriteFlashData();
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
