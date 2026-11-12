package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.E600ADboard;

import android.util.Log;

public class E600ADboardWriteFlashDataManager {
    private static final String TAG = "E600ADboardWriteFlas...";

    private E600ADboardinfoAddrLen mE600ADboardinfoAddrLen;

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

    private E600ADboardWriteFlashDataBlock mE600ADboardWriteFlashDataBlock[] = new E600ADboardWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_NUMBER.value()];
    private int mWriteFlashDataBlockNumber = WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_NUMBER.value();
    private int mWriteFlashDataBlockIndex = 0;
    private String mBinFilePathName;
    private int mWriteFlashDataLenBlock1 = 0;
    private int mWriteFlashDataLenBlock2 = 0;
    private int mWriteFlashDataLenBlock3 = 0;
    private float mWriteFlashDataTotalNum = 0;
    private float mWriteFlashDataProgressNum = 0;
    private float mWriteFlashDataPercent = 0;

    public E600ADboardWriteFlashDataManager() {
        //new E600ADboardWriteFlashDataBlock
        for (int i = 0; i < mWriteFlashDataBlockNumber; i++) {
            mE600ADboardWriteFlashDataBlock[i] = new E600ADboardWriteFlashDataBlock();
        }
    }

    /*******************************************************************************
    * Function Name  : initialWriteFlashData()
    * Description    : initial Write Flash Data block
    * Input          : String binFilePathName , E600ADboardinfoAddrLen phcLCBinfoAddrLen
    * Output         : None
    * Return         : None
    *******************************************************************************/
    public void initialWriteFlashData(String binFilePathName, E600ADboardinfoAddrLen phcLCBinfoAddrLen) {
         Log.d(TAG, String.format("####Frank E600ADboardWriteFlashDataManager initialWriteFlashData frank_1 "));
        mE600ADboardinfoAddrLen = phcLCBinfoAddrLen;
        //E600ADboard WriteFlashData block 1
        mWriteFlashDataLenBlock1 = mE600ADboardinfoAddrLen.getBinFileLength(E600ADboardinfoAddrLen.BinAction.BIN_ACTION_READ, E600ADboardinfoAddrLen.BinBlock.BIN_BLOCK_1);
        mE600ADboardWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value()].setBaseBlock(
                mE600ADboardinfoAddrLen.getBinFileAddress(E600ADboardinfoAddrLen.BinAction.BIN_ACTION_READ, E600ADboardinfoAddrLen.BinBlock.BIN_BLOCK_1)
                , mWriteFlashDataLenBlock1
                , mE600ADboardinfoAddrLen.getFlashDataAddress(E600ADboardinfoAddrLen.FlashAction.FLASH_ACTION_WRITE, E600ADboardinfoAddrLen.FlashBlock.FLASH_BLOCK_1));

        //E600ADboard WriteFlashData block 2
        mWriteFlashDataLenBlock2 = mE600ADboardinfoAddrLen.getBinFileLength(E600ADboardinfoAddrLen.BinAction.BIN_ACTION_READ, E600ADboardinfoAddrLen.BinBlock.BIN_BLOCK_2);
        mE600ADboardWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_2.value()].setBaseBlock(
                mE600ADboardinfoAddrLen.getBinFileAddress(E600ADboardinfoAddrLen.BinAction.BIN_ACTION_READ, E600ADboardinfoAddrLen.BinBlock.BIN_BLOCK_2)
                , mWriteFlashDataLenBlock2
                , mE600ADboardinfoAddrLen.getFlashDataAddress(E600ADboardinfoAddrLen.FlashAction.FLASH_ACTION_WRITE, E600ADboardinfoAddrLen.FlashBlock.FLASH_BLOCK_2));

        //E600ADboard WriteFlashData block 3
        mWriteFlashDataLenBlock3 = mE600ADboardinfoAddrLen.getBinFileLength(E600ADboardinfoAddrLen.BinAction.BIN_ACTION_READ, E600ADboardinfoAddrLen.BinBlock.BIN_BLOCK_3);
        mE600ADboardWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_3.value()].setBaseBlock(
                mE600ADboardinfoAddrLen.getBinFileAddress(E600ADboardinfoAddrLen.BinAction.BIN_ACTION_READ, E600ADboardinfoAddrLen.BinBlock.BIN_BLOCK_3)
                , mWriteFlashDataLenBlock3
                , mE600ADboardinfoAddrLen.getFlashDataAddress(E600ADboardinfoAddrLen.FlashAction.FLASH_ACTION_WRITE, E600ADboardinfoAddrLen.FlashBlock.FLASH_BLOCK_3));

        //init E600ADboard WriteFlashData calc Percent
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
        if ((mE600ADboardWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value()].isReadBinFileEnd()) &&
            (mE600ADboardWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_2.value()].isReadBinFileEnd()) &&
            (mE600ADboardWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_3.value()].isReadBinFileEnd())) {
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
        if (mE600ADboardWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value()].isReadBinFileEnd() == false) {
            mWriteFlashDataBlockIndex = WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_1.value();
        } else if (mE600ADboardWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_2.value()].isReadBinFileEnd() == false) {
            mWriteFlashDataBlockIndex = WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_2.value();
        } else if (mE600ADboardWriteFlashDataBlock[WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_3.value()].isReadBinFileEnd() == false) {
            mWriteFlashDataBlockIndex = WriteFlashDataBlock.WRITE_FLASH_DATA_BLOCK_3.value();
        } else {
            return false;
        }

        //calc and process write flash data and bin file data
        mE600ADboardWriteFlashDataBlock[mWriteFlashDataBlockIndex].procWriteFlashDataBinFileData(mBinFilePathName);
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
        return mE600ADboardWriteFlashDataBlock[mWriteFlashDataBlockIndex].getRtnWriteFlashDataAddr();
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

        rtnWriteFashDataBlockLen = mE600ADboardWriteFlashDataBlock[mWriteFlashDataBlockIndex].getRtnWriteFlashDataLen();
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
        return mE600ADboardWriteFlashDataBlock[mWriteFlashDataBlockIndex].getRtnWriteFlashData();
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
