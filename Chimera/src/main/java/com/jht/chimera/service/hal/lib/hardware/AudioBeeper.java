package com.jht.chimera.service.hal.lib.hardware;

import android.annotation.SuppressLint;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

// Some consoles do not have a hardware beeper/buzzer. This class plays a short beep sound clip from our external assets.
// Sound is given high priority number, not sure if that makes sense?
public class AudioBeeper {

    private final int soundId;
    private final SoundPool soundPool;
    private static final float volume = 0.6f; // 0 to 1 float value

    @SuppressLint("SdCardPath")
    public AudioBeeper() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
            //.setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            //.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            // Think on some consoles interface sounds are disabled, so had to give media attributes
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
            .build();
        soundPool = new SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build();
        //soundId = soundPool.load(context, R.raw.beep, 1);
        soundId = soundPool.load("/sdcard/jht/sounds/beep.wav", 1);
    }

    public void playBeep() {
        soundPool.play(soundId, volume, volume, 1, 0, 1.0f);
    }

}
