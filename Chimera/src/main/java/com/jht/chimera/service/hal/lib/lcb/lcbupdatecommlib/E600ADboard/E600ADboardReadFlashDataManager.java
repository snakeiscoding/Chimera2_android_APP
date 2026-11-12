package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.E600ADboard;

import android.util.Log;

public class E600ADboardReadFlashDataManager {
    private static final String TAG = "E600ADboardReadFlash...";

    private E600ADboardinfoAddrLen mE600ADboardinfoAddrLen;

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

    private E600ADboardReadFlashDataBlock mE600ADboardReadFlashDataBlock[] = new E600ADboardReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_NUMBER.value()];
    private int mReadFlashDataBlockNumber = ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_NUMBER.value();
    private int mReadFlashDataBlockIndex = 0;
    private String mBinFilePathName;
    private int mReadFlashDataLenBlock1 = 0;
    private int mReadFlashDataLenBlock2 = 0;
    private int mReadFlashDataLenBlock3 = 0;
    private float mReadFlashDataTotalNum = 0;
    private float mReadFlashDataProgressNum = 0;
    private float mReadFlashDataPercent = 0;

    public E600ADboardReadFlashDataManager(){
        //new E600ADboardReadFlashDataBlock
        for(int i=0; i<mReadFlashDataBlockNumber; i++){
            mE600ADboardReadFlashDataBlock[i] = new E600ADboardReadFlashDataBlock();
        }
    }

    /*******************************************************************************
    * Function Name  : initialReadFlashData()
    * Description    : initial Read Flash Data block
    * Input          : String binFilePathName ,  E600ADboardinfoAddrLen phcLCBinfoAddrLen
    * Output         : None
    * Return         : None
    *******************************************************************************/
    public void initialReadFlashData(String binFilePathName , E600ADboardinfoAddrLen phcLCBinfoAddrLen){
         Log.d(TAG, String.format("####Frank E600ADboardReadFlashDataManager initialReadFlashData frank_1 "));
        mE600ADboardinfoAddrLen = phcLCBinfoAddrLen;
        //E600ADboard ReadFlashData block 1
        mReadFlashDataLenBlock1 = mE600ADboardinfoAddrLen.getFlashDataLength(E600ADboardinfoAddrLen.FlashAction.FLASH_ACTION_READ , E600ADboardinfoAddrLen.FlashBlock.FLASH_BLOCK_1);
        mE600ADboardReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_1.value()].setBaseBlock(
                mE600ADboardinfoAddrLen.getFlashDataAddress(E600ADboardinfoAddrLen.FlashAction.FLASH_ACTION_READ , E600ADboardinfoAddrLen.FlashBlock.FLASH_BLOCK_1)
                ,mReadFlashDataLenBlock1
                ,mE600ADboardinfoAddrLen.getBinFileAddress(E600ADboardinfoAddrLen.BinAction.BIN_ACTION_READ , E600ADboardinfoAddrLen.BinBlock.BIN_BLOCK_1));

        //E600ADboard ReadFlashData block 2
        mReadFlashDataLenBlock2 = mE600ADboardinfoAddrLen.getFlashDataLength(E600ADboardinfoAddrLen.FlashAction.FLASH_ACTION_READ , E600ADboardinfoAddrLen.FlashBlock.FLASH_BLOCK_2);
        mE600ADboardReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_2.value()].setBaseBlock(
                mE600ADboardinfoAddrLen.getFlashDataAddress(E600ADboardinfoAddrLen.FlashAction.FLASH_ACTION_READ , E600ADboardinfoAddrLen.FlashBlock.FLASH_BLOCK_2)
                ,mReadFlashDataLenBlock2
                ,mE600ADboardinfoAddrLen.getBinFileAddress(E600ADboardinfoAddrLen.BinAction.BIN_ACTION_READ , E600ADboardinfoAddrLen.BinBlock.BIN_BLOCK_2));

        //E600ADboard ReadFlashData block 3
        mReadFlashDataLenBlock3 = mE600ADboardinfoAddrLen.getFlashDataLength(E600ADboardinfoAddrLen.FlashAction.FLASH_ACTION_READ , E600ADboardinfoAddrLen.FlashBlock.FLASH_BLOCK_3);
        mE600ADboardReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_3.value()].setBaseBlock(
                mE600ADboardinfoAddrLen.getFlashDataAddress(E600ADboardinfoAddrLen.FlashAction.FLASH_ACTION_READ , E600ADboardinfoAddrLen.FlashBlock.FLASH_BLOCK_3)
                ,mReadFlashDataLenBlock3
                ,mE600ADboardinfoAddrLen.getBinFileAddress(E600ADboardinfoAddrLen.BinAction.BIN_ACTION_READ , E600ADboardinfoAddrLen.BinBlock.BIN_BLOCK_3));

        //init E600ADboard ReadFlashData calc Percent
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
    public boolean isAllBlockFinish(){
        if( (mE600ADboardReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_1.value()].isReadFlashDataEnd())
            && (mE600ADboardReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_2.value()].isReadFlashDataEnd())
            && (mE600ADboardReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_3.value()].isReadFlashDataEnd())
            ){
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
    public boolean getNextBlockData(){
        //check which block is not end
        if(mE600ADboardReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_1.value()].isReadFlashDataEnd() == false){
            mReadFlashDataBlockIndex = ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_1.value();
        }else if(mE600ADboardReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_2.value()].isReadFlashDataEnd() == false){
            mReadFlashDataBlockIndex = ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_2.value();
        }else if(mE600ADboardReadFlashDataBlock[ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_3.value()].isReadFlashDataEnd() == false){
            mReadFlashDataBlockIndex = ReadFlashDataBlock.READ_FLASH_DATA_BLOCK_3.value();
        }else{
            return false;
        }

        //calc and process read flash data and bin file data
        mE600ADboardReadFlashDataBlock[mReadFlashDataBlockIndex].procReadFlashDataBinFileData(mBinFilePathName);
        return true;
    }

    /*******************************************************************************
    * Function Name  : getBlockReadFlashDataAddr()
    * Description    : get read FlashData block addr
    * Input          : None
    * Output         : None
    * Return         : int addr
    *******************************************************************************/
    public int  getBlockReadFlashDataAddr(){
        //get read FlashData block addr
        return mE600ADboardReadFlashDataBlock[mReadFlashDataBlockIndex].getRtnReadFlashDataAddr();
    }

    /*******************************************************************************
    * Function Name  : getBlockReadFlashDataLen()
    * Description    : get read FlashData block len
    * Input          : None
    * Output         : None
    * Return         : int len
    *******************************************************************************/
    public int  getBlockReadFlashDataLen(){
        int rtnReadFashDataBlockLen = 0;

        rtnReadFashDataBlockLen = mE600ADboardReadFlashDataBlock[mReadFlashDataBlockIndex].getRtnReadFlashDataLen();
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
    public boolean  compReadFlashDataAndBinFile(byte[] readData){
        byte[] binFileData = mE600ADboardReadFlashDataBlock[mReadFlashDataBlockIndex].getRtnReadBinFile();

        for(int i=0 ; i < readData.length ; i++){
            if(readData[i] != binFileData[i]){
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
    private void  initReadFlashDataPercent(){
        float readFlashDataLenBlock1 = (float)mReadFlashDataLenBlock1;
        float readFlashDataLenBlock2 = (float)mReadFlashDataLenBlock2;
        float readFlashDataLenBlock3 = (float)mReadFlashDataLenBlock3;

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
    private void  setReadFlashDataPercentProgressNum(int reduceNum){
        if(reduceNum < 0)
            return;

        float floatProgressNum = (float) reduceNum;
        float tempProgressNum = mReadFlashDataProgressNum + floatProgressNum;

        if(tempProgressNum > mReadFlashDataTotalNum)
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
    public float  calcReadFlashDataPercent(){
        float rtnPercent = 0;
        if((mReadFlashDataTotalNum <= 0) ||(mReadFlashDataProgressNum <= 0)){
            rtnPercent = 0;
            return rtnPercent;
        }

        if (mReadFlashDataProgressNum > mReadFlashDataTotalNum ){
            mReadFlashDataProgressNum = mReadFlashDataTotalNum;
        }

        mReadFlashDataPercent = mReadFlashDataProgressNum / mReadFlashDataTotalNum;
        rtnPercent = mReadFlashDataPercent;

        return rtnPercent;

    }
}
