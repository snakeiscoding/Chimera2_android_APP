package com.jht.chimera.lib.widget.sound;

import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;

public class SystemVolume {
    private static final String TAG = "SystemVolume";
    public final static int MIN_VOLUME = 0;


    private SystemVolume() {
    }

    private static class SingletonHolder {
        private static final SystemVolume INSTANCE = new SystemVolume();
    }

    public static SystemVolume getInstance() {
        return SystemVolume.SingletonHolder.INSTANCE;
    }

    /**
     * Set HALKeys.DATA_SYSTEM_VOLUME_MUTE_BOOL to opposite value.
     */
    public void toggleMute() {
        DataMatrix.putBoolean(HALKeys.DATA_SYSTEM_VOLUME_MUTE_BOOL, !DataMatrix.getBoolean(HALKeys.DATA_SYSTEM_VOLUME_MUTE_BOOL));
    }

    /**
     * HALKeys.DATA_SYSTEM_VOLUME_MUTE_BOOL to true.
     */
    public void mute() {
        DataMatrix.putBoolean(HALKeys.DATA_SYSTEM_VOLUME_MUTE_BOOL, true);
    }

    /**
     * HALKeys.DATA_SYSTEM_VOLUME_MUTE_BOOL to false.
     */
    public void unmute() {
        DataMatrix.putBoolean(HALKeys.DATA_SYSTEM_VOLUME_MUTE_BOOL, false);
    }

    /**
     * @return DATA_SYSTEM_VOLUME_INT
     */
    public int getVolume() {
        return DataMatrix.getInt(HALKeys.DATA_SYSTEM_VOLUME_INT);
    }

    /**
     * Sets the DATA_SYSTEM_VOLUME_INT value to the specified volume.
     * @param volume new volume to save
     */
    public void setVolume(int volume) {
        int maxVolume = BuildConfig.MAX_VOLUME;
        unmute();
        if (volume >= MIN_VOLUME && volume <= maxVolume) {
            DataMatrix.putInt(HALKeys.DATA_SYSTEM_VOLUME_INT, volume);
        }
    }

    /** Get system volume as a range of [0,1] */
    public float getNormalizedVolume() {
        return normalizeVolume(DataMatrix.getInt(HALKeys.DATA_SYSTEM_VOLUME_INT));
    }
    /** Converts system volume to a range of [0,1] */
    public static float normalizeVolume(int systemVolume) {
        float normalVolume = ((float) systemVolume - MIN_VOLUME) / (BuildConfig.MAX_VOLUME - MIN_VOLUME);
        return Math.max(0f, Math.min(1f, normalVolume));
    }

    /** Set system volume as a range of [0,1] */
    public void setNormalizedVolume(float volume) {
        int systemVolume = denormalizeVolume(volume);
        unmute(); // system will unmute anyway, this just updates the flag
        DataMatrix.putInt(HALKeys.DATA_SYSTEM_VOLUME_INT, systemVolume);
    }
    /** Converts a range of [0,1] to system volume */
    public static int denormalizeVolume(float normalVolume) {
        float fixedNormalVolume = Math.max(0f, Math.min(1f, normalVolume));
        float systemVolume = fixedNormalVolume * (BuildConfig.MAX_VOLUME - MIN_VOLUME) + MIN_VOLUME;
        return Math.max(MIN_VOLUME, Math.min(BuildConfig.MAX_VOLUME, Math.round(systemVolume)));
    }

    /**
     * Increase volume by 1.
     */
    public void increaseVolume() {
        int maxVolume = BuildConfig.MAX_VOLUME;
        int volume = getVolume();
        if (volume < maxVolume) {
            setVolume(volume + 1);
        }
    }

    /**
     * Decrease volume by 1.
     */
    public void decreaseVolume() {
        int volume = getVolume();
        if (volume > MIN_VOLUME) {
            setVolume(volume - 1);
        }
    }
}