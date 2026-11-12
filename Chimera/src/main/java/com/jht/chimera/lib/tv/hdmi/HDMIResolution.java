package com.jht.chimera.lib.tv.hdmi;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Arrays;

/**
 * Used to manage HDMI resolution as it is read from the hardware.  This provides a means to parse
 * the output from the driver and compare two resolutions.
 */
public class HDMIResolution {
    private HdmiInputInfo hdmiInputInfo;

    public HdmiInputInfo getHdmiInputInfo() {
        return hdmiInputInfo;
    }

    public HDMIResolution(HdmiInputInfo hdmiInputInfo) {
        this.hdmiInputInfo = hdmiInputInfo;
    }

    /**
     * Read the resolution.  The expected string looks like the following.
     *    width:1280
     *    height:720
     *    samplerate:4800
     *
     * @param resolution    The string output from the driver.
     */
    public HDMIResolution(String resolution) {

        // Split the string.
        String[] parts = resolution.split("\n");
        int width = -1;
        int height = -1;
        int sampleRate = -1;

        // Get the parts of the output.
        for (String s : parts) {
            if (s.startsWith("width:")) {
                width = Integer.parseInt(s.substring("width:".length())); //NON-NLS
            } else if (s.startsWith("height:")) {
                height = Integer.parseInt(s.substring("height:".length())); //NON-NLS
            } else if (s.startsWith("samplerate:")) {
                sampleRate = Integer.parseInt(s.substring("samplerate:".length())); //NON-NLS
            }
        }
        hdmiInputInfo = new HdmiInputInfo(width, height, sampleRate);
        hdmiInputInfo.setConnected(hdmiInputInfo.getWidth() > 0 && hdmiInputInfo.getHeight() > 0);
    }
    /**
     * Create a known resolution.
     * @param width         The video width.
     * @param height        The video height.
     * @param sampleRate    The audio sample rate.
     * @param connected     True if HDMI is connected.
     */
    public HDMIResolution(int width, int height, int sampleRate, boolean connected) {
        hdmiInputInfo = new HdmiInputInfo(width, height, sampleRate);
        hdmiInputInfo.setConnected(connected);
    }
    /**
     * Returns true if the resolution given is different.  This ignores the sample rate.
     *
     * @param resolution    The resolution to compare to.
     * @return              True if the resolution input is different from this object.
     */
    boolean isDifferent(HDMIResolution resolution) {
        return isDifferent(resolution, false);
    }
    /**
     *
     * Returns true if the resolution given is different.
     *
     * @param resolution        The resolution to compare to.
     * @param checkSampleRate   True if we should check the sample rate.
     * @return                  True if the resolution input is different from this object.
     *
     */
    public boolean isDifferent(HDMIResolution resolution, boolean checkSampleRate) {
        if(checkSampleRate) {
            return hdmiInputInfo.getWidth() != resolution.getHdmiInputInfo().getWidth() || hdmiInputInfo.getHeight() != resolution.getHdmiInputInfo().getHeight()  || hdmiInputInfo.getSampleRate() != resolution.getHdmiInputInfo().getSampleRate() || hdmiInputInfo.isConnected() != resolution.getHdmiInputInfo().isConnected();
        }
        return hdmiInputInfo.getWidth() != resolution.getHdmiInputInfo().getWidth() || hdmiInputInfo.getHeight() != resolution.getHdmiInputInfo().getHeight() || hdmiInputInfo.isConnected() != resolution.getHdmiInputInfo().isConnected();


    }
    @NonNull
    @Override
    public String toString() {
        return hdmiInputInfo.getWidth() + "x" + hdmiInputInfo.getHeight() + (hdmiInputInfo.isInterlaced() ? "i" : "p") + hdmiInputInfo.getFrameRate() + "_" + hdmiInputInfo.getSampleRate() + "Hz_" + hdmiInputInfo.getAudioChannels() + "ch";
    }
    public static final HDMIResolution UNKNOWN = new HDMIResolution(new HdmiInputInfo(0, 0, 0, 0, false, 0, false));

    public static HDMIResolution fromFormat(String format) {
        if (format == null || format.trim().isEmpty()) {
            return HDMIResolution.UNKNOWN;
        }

        try {
            // Example: 1920x1080P60_48000_2 or 1920x1080P60
            String[] parts = format.split("_");

            String videoPart = parts[0];

            // Split on the first occurrence of P/I/p/i
            String[] resolutionAndRate = videoPart.split("(?i)[pi]", 2);
            Log.d("hdmiresolution", "resolutionAndRate: "+ Arrays.toString(resolutionAndRate));
            if (resolutionAndRate.length < 2) return HDMIResolution.UNKNOWN;

            String[] dimensions = resolutionAndRate[0].split("x");

            Log.d("hdmiresolution", "dimensions: "+ Arrays.toString(dimensions));
            if (dimensions.length < 2) return HDMIResolution.UNKNOWN;

            int width = parseIntSafe(dimensions[0]);
            int height = parseIntSafe(dimensions[1]);
            int frameRate = parseIntSafe(resolutionAndRate[1]);

            boolean interlaced = videoPart.contains("I") || videoPart.contains("i");

            int sampleRate = 48000; // Default
            int audioChannels = 2;  // Default

            if (parts.length > 1) {
                sampleRate = parseIntSafe(parts[1], sampleRate);
            }
            if (parts.length > 2) {
                audioChannels = parseIntSafe(parts[2], audioChannels);
            }
            boolean connected = width > 0 && height > 0;

            return new HDMIResolution(new HdmiInputInfo(width, height, sampleRate, frameRate, connected, audioChannels, interlaced));
        } catch (Exception e) {
            return HDMIResolution.UNKNOWN;
        }
    }

    // Utility function for safe integer parsing
    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int parseIntSafe(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
