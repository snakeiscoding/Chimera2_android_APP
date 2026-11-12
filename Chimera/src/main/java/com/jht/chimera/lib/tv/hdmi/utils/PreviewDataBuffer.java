package com.jht.chimera.lib.tv.hdmi.utils;

public class PreviewDataBuffer {
    private long timestamp;
    private long frameIndex;

    private byte[] nv21;

    public PreviewDataBuffer(byte[] nv21, long timestamp) {
        this.nv21 = nv21/*.clone()*/;
        this.timestamp = timestamp;
    }

    public void setFrameIndex(long index) {
        frameIndex = index;
    }

    public long getFrameIndex() {
        return frameIndex;
    }

    public byte[] getNV21() {
        return nv21;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "PreviewDataBuffer{" +
                "timestamp=" + timestamp +
                '}';
    }
}
