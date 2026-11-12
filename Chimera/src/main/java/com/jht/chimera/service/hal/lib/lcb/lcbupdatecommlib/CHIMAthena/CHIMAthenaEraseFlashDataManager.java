package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMAthena;

public class CHIMAthenaEraseFlashDataManager {
    private static final String TAG = "CHIMAthenaEraseFlash...";

    private CHIMAthenainfoAddrLen mCHIMAthenainfoAddrLen;


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

    private CHIMAthenaEraseFlashDataBlock[] mCHIMAthenaEraseFlashDataBlock = new CHIMAthenaEraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_NUMBER.value()];
    private int mEraseFlashDataBlockNumber = EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_NUMBER.value();
    private int mEraseFlashDataBlockIndex = 0;
    private int mEraseFlashDataLenBlockA = 0;
    private int mEraseFlashDataLenBlockB = 0;
    private float mEraseFlashDataTotalNum = 0;
    private float mEraseFlashDataProgressNum = 0;
    private float mEraseFlashDataPercent = 0;

    public CHIMAthenaEraseFlashDataManager() {
        //new CHIMAthenaEraseFlashDataBlock
        for (int i = 0; i < mEraseFlashDataBlockNumber; i++) {
            mCHIMAthenaEraseFlashDataBlock[i] = new CHIMAthenaEraseFlashDataBlock();
        }
    }

    /*******************************************************************************
    * Function Name  : initialEraseFlashData()
    * Description    : initial Erase Flash Data block
    * Input          : CHIMAthenainfoAddrLen phcLCBinfoAddrLen
    * Output         : None
    * Return         : None
    *******************************************************************************/
    public void initialEraseFlashData(CHIMAthenainfoAddrLen phcLCBinfoAddrLen) {
        mCHIMAthenainfoAddrLen = phcLCBinfoAddrLen;
        //CHIMAthena EraseFlashData block A
        mEraseFlashDataLenBlockA = mCHIMAthenainfoAddrLen.getFlashDataLength(CHIMAthenainfoAddrLen.FlashAction.FLASH_ACTION_ERASE, CHIMAthenainfoAddrLen.FlashBlock.FLASH_BLOCK_A);
        mCHIMAthenaEraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_A.value()].setEraseFlashDataBlock(
                mCHIMAthenainfoAddrLen.getFlashDataAddress(CHIMAthenainfoAddrLen.FlashAction.FLASH_ACTION_ERASE, CHIMAthenainfoAddrLen.FlashBlock.FLASH_BLOCK_A),
                mEraseFlashDataLenBlockA);

        //CHIMAthena EraseFlashData block B
        mEraseFlashDataLenBlockB = mCHIMAthenainfoAddrLen.getFlashDataLength(CHIMAthenainfoAddrLen.FlashAction.FLASH_ACTION_ERASE, CHIMAthenainfoAddrLen.FlashBlock.FLASH_BLOCK_B);
        mCHIMAthenaEraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_B.value()].setEraseFlashDataBlock(
                mCHIMAthenainfoAddrLen.getFlashDataAddress(CHIMAthenainfoAddrLen.FlashAction.FLASH_ACTION_ERASE, CHIMAthenainfoAddrLen.FlashBlock.FLASH_BLOCK_B),
                mEraseFlashDataLenBlockB);

        //init CHIMAthena EraseFlashData calc Percent
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
        if ((mCHIMAthenaEraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_A.value()].isEraseFlashDataEnd()) &&
            (mCHIMAthenaEraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_B.value()].isEraseFlashDataEnd())) {
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
        if (mCHIMAthenaEraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_A.value()].isEraseFlashDataEnd() == false) {
            mEraseFlashDataBlockIndex = EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_A.value();
        } else if (mCHIMAthenaEraseFlashDataBlock[EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_B.value()].isEraseFlashDataEnd() == false) {
            mEraseFlashDataBlockIndex = EraseFlashDataBlock.ERASE_FLASH_DATA_BLOCK_B.value();
        } else {
            return false;
        }

        //calc and process erase flash data
        mCHIMAthenaEraseFlashDataBlock[mEraseFlashDataBlockIndex].procEraseFlashData();

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
        return mCHIMAthenaEraseFlashDataBlock[mEraseFlashDataBlockIndex].getEraseFashDataBlockAddr();
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

        rtnEraseFashDataBlockLen = mCHIMAthenaEraseFlashDataBlock[mEraseFlashDataBlockIndex].getEraseFashDataBlockLen();
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
