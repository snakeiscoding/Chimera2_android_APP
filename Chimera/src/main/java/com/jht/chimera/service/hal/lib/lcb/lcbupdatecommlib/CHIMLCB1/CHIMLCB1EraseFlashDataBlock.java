package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMLCB1;

public class CHIMLCB1EraseFlashDataBlock {
    private static final String TAG = "CHIMLCB1EraseFlashDa...";

    //flash data info
    private int flashDataEraseAddr;
    private int flashDataEraseLen;
    private boolean isFlashDataEraseCounterEnd = false;


    public CHIMLCB1EraseFlashDataBlock() {
    }

    public boolean isEraseFlashDataEnd() {
        return isFlashDataEraseCounterEnd;
    }

    public void procEraseFlashData() {
        isFlashDataEraseCounterEnd = true;
    }

    public void setEraseFlashDataBlock(int blockAddr, int blockLength) {
        flashDataEraseAddr = blockAddr;
        flashDataEraseLen = blockLength;
    }

    public int getEraseFashDataBlockAddr() {
        return flashDataEraseAddr;
    }

    public int getEraseFashDataBlockLen() {
        return flashDataEraseLen;
    }
}
