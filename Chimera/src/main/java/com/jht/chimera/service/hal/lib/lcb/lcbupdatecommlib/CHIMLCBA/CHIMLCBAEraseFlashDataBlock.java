package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMLCBA;

public class CHIMLCBAEraseFlashDataBlock {
    private static final String TAG = "CHIMLCBAEraseFlashDa...";

    //flash data info
    private int flashDataEraseAddr;
    private int flashDataEraseLen;
    private boolean isFlashDataEraseCounterEnd = false;


    public CHIMLCBAEraseFlashDataBlock() {
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
