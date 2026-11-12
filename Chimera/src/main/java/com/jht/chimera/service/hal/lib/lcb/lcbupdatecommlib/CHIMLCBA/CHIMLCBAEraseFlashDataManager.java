package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMLCBA;

public class CHIMLCBAEraseFlashDataManager {
    private static final String TAG = "CHIMLCBAEraseFlashDa...";

    private CHIMLCBAinfoAddrLen mCHIMLCBAinfoAddrLen;


    public enum EraseFlashDataBlock {
        ERASE_FLASH_DATA_BLOCK_A(0),
        ERASE_FLASH_DATA_BLOCK_B(1),
        ERASE_FLASH_DATA_BLOCK_NUMBER(2);

        private int value;

        private EraseFlashDataBlock(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    private CHIMLCBAEraseFlashDataBlock[] mCHIMLCBAEraseFlashDataBlock = new CHIMLCBAEraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_NUMBER.value()];
    private int mEraseFlashDataBlockNumber = EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_NUMBER.value();
    private int mEraseFlashDataBlockIndex = 0;
    private int mEraseFlashDataLenBlockA = 0;
    private int mEraseFlashDataLenBlockB = 0;
    private float mEraseFlashDataTotalNum = 0;
    private float mEraseFlashDataProgressNum = 0;
    private float mEraseFlashDataPercent = 0;

    public CHIMLCBAEraseFlashDataManager() {
        //new CHIMLCBAEraseFlashDataBlock
        for (int i = 0; i < mEraseFlashDataBlockNumber; i++) {
            mCHIMLCBAEraseFlashDataBlock[i] = new CHIMLCBAEraseFlashDataBlock();
        }
    }

    /*******************************************************************************
     * Function Name  : initialEraseFlashData()
     * Description    : initial Erase Flash Data block
     * Input          : CHIMLCBAinfoAddrLen phcLCBinfoAddrLen
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void initialEraseFlashData(CHIMLCBAinfoAddrLen phcLCBinfoAddrLen) {
        mCHIMLCBAinfoAddrLen = phcLCBinfoAddrLen;
        //CHIMLCBA EraseFlashData block A
        mEraseFlashDataLenBlockA = mCHIMLCBAinfoAddrLen.getFlashDataLength(CHIMLCBAinfoAddrLen.FlashAction.FLASH_ACTION_ERASE, CHIMLCBAinfoAddrLen.FlashBlock.FLASH_BLOCK_A);
        mCHIMLCBAEraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_A.value()].setEraseFlashDataBlock(
                mCHIMLCBAinfoAddrLen.getFlashDataAddress(CHIMLCBAinfoAddrLen.FlashAction.FLASH_ACTION_ERASE, CHIMLCBAinfoAddrLen.FlashBlock.FLASH_BLOCK_A),
                mEraseFlashDataLenBlockA);

        //CHIMLCBA EraseFlashData block B
        mEraseFlashDataLenBlockB = mCHIMLCBAinfoAddrLen.getFlashDataLength(CHIMLCBAinfoAddrLen.FlashAction.FLASH_ACTION_ERASE, CHIMLCBAinfoAddrLen.FlashBlock.FLASH_BLOCK_B);
        mCHIMLCBAEraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_B.value()].setEraseFlashDataBlock(
                mCHIMLCBAinfoAddrLen.getFlashDataAddress(CHIMLCBAinfoAddrLen.FlashAction.FLASH_ACTION_ERASE, CHIMLCBAinfoAddrLen.FlashBlock.FLASH_BLOCK_B),
                mEraseFlashDataLenBlockB);

        //init CHIMLCBA EraseFlashData calc Percent
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
        if ((mCHIMLCBAEraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_A.value()].isEraseFlashDataEnd()) &&
                (mCHIMLCBAEraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_B.value()].isEraseFlashDataEnd())) {
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
        if (mCHIMLCBAEraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_A.value()].isEraseFlashDataEnd() == false) {
            mEraseFlashDataBlockIndex = EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_A.value();
        } else if (mCHIMLCBAEraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_B.value()].isEraseFlashDataEnd() == false) {
            mEraseFlashDataBlockIndex = EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_B.value();
        } else {
            return false;
        }

        //calc and process erase flash data
        mCHIMLCBAEraseFlashDataBlock[mEraseFlashDataBlockIndex].procEraseFlashData();

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
        return mCHIMLCBAEraseFlashDataBlock[mEraseFlashDataBlockIndex].getEraseFashDataBlockAddr();
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

        rtnEraseFashDataBlockLen = mCHIMLCBAEraseFlashDataBlock[mEraseFlashDataBlockIndex].getEraseFashDataBlockLen();
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

        mEraseFlashDataTotalNum = 0;
        mEraseFlashDataProgressNum = 0;
        mEraseFlashDataPercent = 0;

        mEraseFlashDataTotalNum = eraseFlashDataLenBlockA + eraseFlashDataLenBlockB;
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
