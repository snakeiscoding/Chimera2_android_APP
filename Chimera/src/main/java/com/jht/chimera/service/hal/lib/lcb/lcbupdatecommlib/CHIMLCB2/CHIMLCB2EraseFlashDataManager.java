package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMLCB2;

public class CHIMLCB2EraseFlashDataManager {
//    private static final String TAG = CHIMLCB2EraseFlashDataManager.class.getName();

    private CHIMLCB2infoAddrLen mCHIMLCB2infoAddrLen;


    public enum EraseFlashDataBlock {
        ERASE_FLASH_DATA_BLOCK_A(0),
        ERASE_FLASH_DATA_BLOCK_B(1),
        ERASE_FLASH_DATA_BLOCK_C(2),
        ERASE_FLASH_DATA_BLOCK_NUMBER(3);

        private int value;

        private EraseFlashDataBlock(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    private CHIMLCB2EraseFlashDataBlock[] mCHIMLCB2EraseFlashDataBlock = new CHIMLCB2EraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_NUMBER.value()];
    private int mEraseFlashDataBlockNumber = EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_NUMBER.value();
    private int mEraseFlashDataBlockIndex = 0;
    private int mEraseFlashDataLenBlockA = 0;
    private int mEraseFlashDataLenBlockB = 0;
    private int mEraseFlashDataLenBlockC = 0;
    private float mEraseFlashDataTotalNum = 0;
    private float mEraseFlashDataProgressNum = 0;
    private float mEraseFlashDataPercent = 0;

    public CHIMLCB2EraseFlashDataManager() {
        //new CHIMLCB2EraseFlashDataBlock
        for (int i = 0; i < mEraseFlashDataBlockNumber; i++) {
            mCHIMLCB2EraseFlashDataBlock[i] = new CHIMLCB2EraseFlashDataBlock();
        }
    }

    /*******************************************************************************
    * Function Name  : initialEraseFlashData()
    * Description    : initial Erase Flash Data block
    * Input          : CHIMLCB2infoAddrLen phcLCBinfoAddrLen
    * Output         : None
    * Return         : None
    *******************************************************************************/
    public void initialEraseFlashData(CHIMLCB2infoAddrLen phcLCBinfoAddrLen) {
        mCHIMLCB2infoAddrLen = phcLCBinfoAddrLen;
        //CHIMLCB2 EraseFlashData block A
        mEraseFlashDataLenBlockA = mCHIMLCB2infoAddrLen.getFlashDataLength(CHIMLCB2infoAddrLen.FlashAction.FLASH_ACTION_ERASE, CHIMLCB2infoAddrLen.FlashBlock.FLASH_BLOCK_A);
        mCHIMLCB2EraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_A.value()].setEraseFlashDataBlock(
                mCHIMLCB2infoAddrLen.getFlashDataAddress(CHIMLCB2infoAddrLen.FlashAction.FLASH_ACTION_ERASE, CHIMLCB2infoAddrLen.FlashBlock.FLASH_BLOCK_A),
                mEraseFlashDataLenBlockA);

        //CHIMLCB2 EraseFlashData block B
        mEraseFlashDataLenBlockB = mCHIMLCB2infoAddrLen.getFlashDataLength(CHIMLCB2infoAddrLen.FlashAction.FLASH_ACTION_ERASE, CHIMLCB2infoAddrLen.FlashBlock.FLASH_BLOCK_B);
        mCHIMLCB2EraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_B.value()].setEraseFlashDataBlock(
                mCHIMLCB2infoAddrLen.getFlashDataAddress(CHIMLCB2infoAddrLen.FlashAction.FLASH_ACTION_ERASE, CHIMLCB2infoAddrLen.FlashBlock.FLASH_BLOCK_B),
                mEraseFlashDataLenBlockB);

        //CHIMLCB2 EraseFlashData block C
        mEraseFlashDataLenBlockC = mCHIMLCB2infoAddrLen.getFlashDataLength(CHIMLCB2infoAddrLen.FlashAction.FLASH_ACTION_ERASE, CHIMLCB2infoAddrLen.FlashBlock.FLASH_BLOCK_C);
        mCHIMLCB2EraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_C.value()].setEraseFlashDataBlock(
                mCHIMLCB2infoAddrLen.getFlashDataAddress(CHIMLCB2infoAddrLen.FlashAction.FLASH_ACTION_ERASE, CHIMLCB2infoAddrLen.FlashBlock.FLASH_BLOCK_C),
                mEraseFlashDataLenBlockC);

        //init CHIMLCB2 EraseFlashData calc Percent
        initEraseFlashDataPercent();

        //set first erase flash data block A
        mEraseFlashDataBlockIndex = EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_A.value();
    }

    /*******************************************************************************
    * Function Name  : isAllBlockFinish()
    * Description    : check all FlashData block read end
    * Input          : None
    * Output         : None
    * Return         : boolean all block flash data read finish
    *******************************************************************************/
    public boolean isAllBlockFinish() {
        if ((mCHIMLCB2EraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_A.value()].isEraseFlashDataEnd()) &&
	    (mCHIMLCB2EraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_B.value()].isEraseFlashDataEnd()) &&
            (mCHIMLCB2EraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_B.value()].isEraseFlashDataEnd())) {
            return true;
        }

        return false;
    }

    /*******************************************************************************
    * Function Name  : getNextData()
    * Description    : process flash data to erase
    * Input          : None
    * Output         : None
    * Return         : boolean have data to check
    *******************************************************************************/
    public boolean getNextBlockData() {
        //check which block is not end
        if (mCHIMLCB2EraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_A.value()].isEraseFlashDataEnd() == false) {
            mEraseFlashDataBlockIndex = EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_A.value();
        } else if (mCHIMLCB2EraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_B.value()].isEraseFlashDataEnd() == false) {
            mEraseFlashDataBlockIndex = EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_B.value();
        } else if (mCHIMLCB2EraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_C.value()].isEraseFlashDataEnd() == false) {
            mEraseFlashDataBlockIndex = EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_C.value();
        } else {
            return false;
        }

        //calc and process erase flash data
        mCHIMLCB2EraseFlashDataBlock[mEraseFlashDataBlockIndex].procEraseFlashData();

        return true;
    }

    /*******************************************************************************
    * Function Name  : getBlockEraseFlashDataAddr()
    * Description    : get erase FlashData block addr
    * Input          : None
    * Output         : None
    * Return         : int addr
    *******************************************************************************/
    public int getBlockEraseFlashDataAddr() {
        //get erase FlashData block addr
        return mCHIMLCB2EraseFlashDataBlock[mEraseFlashDataBlockIndex].getEraseFashDataBlockAddr();
    }

    /*******************************************************************************
    * Function Name  : getBlockEraseFlashDataLen()
    * Description    : get erase FlashData block len
    * Input          : None
    * Output         : None
    * Return         : int len
    *******************************************************************************/
    public int getBlockEraseFlashDataLen() {
        int rtnEraseFashDataBlockLen = 0;

        rtnEraseFashDataBlockLen = mCHIMLCB2EraseFlashDataBlock[mEraseFlashDataBlockIndex].getEraseFashDataBlockLen();
        //set EraseFlashDataPercentProgressNum , but not calc
        setEraseFlashDataPercentProgressNum(rtnEraseFashDataBlockLen);
        //get erase FlashData block len
        return rtnEraseFashDataBlockLen;
    }

    /*******************************************************************************
    * Function Name  : initEraseFlashDataPercent()
    * Description    : init erase FlashData Percent
    * Input          : None
    * Output         : None
    * Return         : None
    *******************************************************************************/
    private void initEraseFlashDataPercent() {
        float eraseFlashDataLenBlockA = (float) mEraseFlashDataLenBlockA;
        float eraseFlashDataLenBlockB = (float) mEraseFlashDataLenBlockB;
        float eraseFlashDataLenBlockC = (float) mEraseFlashDataLenBlockC;

        mEraseFlashDataTotalNum = 0;
        mEraseFlashDataProgressNum = 0;
        mEraseFlashDataPercent = 0;

        mEraseFlashDataTotalNum = eraseFlashDataLenBlockA + eraseFlashDataLenBlockB + eraseFlashDataLenBlockC;
    }

    /*******************************************************************************
    * Function Name  : setEraseFlashDataPercentProgressNum(int reduceNum)
    * Description    : set EraseFlashDataPercentProgressNum
    * Input          : int reduceNum
    * Output         : None
    * Return         : None
    *******************************************************************************/
    private void setEraseFlashDataPercentProgressNum(int reduceNum) {
        if (reduceNum < 0)
            return;

        float floatProgressNum = (float) reduceNum;
        float tempProgressNum = mEraseFlashDataProgressNum + floatProgressNum;

        if (tempProgressNum > mEraseFlashDataTotalNum)
            tempProgressNum = mEraseFlashDataTotalNum;

        mEraseFlashDataProgressNum = tempProgressNum;
    }

    /*******************************************************************************
    * Function Name  : calcEraseFlashDataPercent()
    * Description    : calc erase FlashData Percent
    * Input          : None
    * Output         : None
    * Return         : float Percent
    *******************************************************************************/
    public float calcEraseFlashDataPercent() {
        float rtnPercent = 0;
        if ((mEraseFlashDataTotalNum <= 0) || (mEraseFlashDataProgressNum <= 0)) {
            rtnPercent = 0;
            return rtnPercent;
        }

        if (mEraseFlashDataProgressNum > mEraseFlashDataTotalNum) {
            mEraseFlashDataProgressNum = mEraseFlashDataTotalNum;
        }

        mEraseFlashDataPercent = mEraseFlashDataProgressNum / mEraseFlashDataTotalNum;
        rtnPercent = mEraseFlashDataPercent;

        return rtnPercent;
    }
}
