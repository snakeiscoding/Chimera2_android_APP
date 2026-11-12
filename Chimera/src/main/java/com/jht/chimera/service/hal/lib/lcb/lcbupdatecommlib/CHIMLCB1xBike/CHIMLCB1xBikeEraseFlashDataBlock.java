package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.CHIMLCB1xBike;

public class CHIMLCB1xBikeEraseFlashDataBlock {
    private static final String TAG = "CHIMLCB1xBikeEraseFl...";

    //flash data info
    private int flashDataEraseAddr;
    private int flashDataEraseLen;
    private boolean isFlashDataEraseCounterEnd = false;


    public CHIMLCB1xBikeEraseFlashDataBlock() {
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
