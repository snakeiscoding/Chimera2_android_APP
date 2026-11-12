package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMAthena;

public class CHIMAthenaEraseFlashDataBlock {
    private static final String TAG = "CHIMAthenaEraseFlash...";

    //flash data info
    private int flashDataEraseAddr;
    private int flashDataEraseLen;
    private boolean isFlashDataEraseCounterEnd = false;


    public CHIMAthenaEraseFlashDataBlock() {
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
