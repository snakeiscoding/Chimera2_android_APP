package com.jht.chimera.lib.tv.hdmi;

import androidx.annotation.NonNull;

public class HdmiInputInfo {
    private int width;
    private int height;
    private int frameRate;
    private int sampleRate;
    private boolean connected;
    private int audioChannels;
    private final boolean interlaced;

    public HdmiInputInfo(int width, int height, int sampleRate) {
        this.width = width;
        this.height = height;
        this.sampleRate = sampleRate;
        this.interlaced = false;
    }

    public HdmiInputInfo(int width, int height, int sampleRate,  int frameRate, boolean connected, int audioChannels, boolean interlaced) {
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
        this.sampleRate = sampleRate;
        this.connected = connected;
        this.audioChannels = audioChannels;
        this.interlaced = interlaced;
    }
    public HdmiInputInfo(int width, int height, int frameRate, boolean connected, boolean interlaced) {
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
        this.sampleRate = 0;
        this.connected = connected;
        this.audioChannels = 0;
        this.interlaced = interlaced;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public int getSampleRate(){
        return sampleRate;
    }
    public boolean isConnected() {
        return connected;
    }
    public int getAudioChannels(){
        return audioChannels;
    }
    public boolean isInterlaced() {
        return interlaced;
    }

    public void setWidth(int width){
        this.width = width;
    }

    public void setHeight(int height){
        this.height = height;
    }
    public void setFrameRate(int frameRate){
        this.frameRate = frameRate;
    }
    public void setSampleRate(int sampleRate){
        this.sampleRate = sampleRate;
    }
    public void setConnected(boolean connected){
        this.connected = connected;
    }
    public boolean getConnected() { return this.connected; }

    @NonNull
    @Override
    public String toString() {
        return width + "x" + height + (interlaced ? "i" : "p") + frameRate + "Hz";
    }

}
