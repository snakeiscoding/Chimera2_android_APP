package com.jht.chimera.service.hal.lib.hardware;

import android.annotation.SuppressLint;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import com.jht.chimera.lib.common.config.ProductConfig;

import org.jetbrains.annotations.NonNls;

/*
    This class manages a recording and playback thread for consoles that do not use an audio mixer.
    It is used primarily by AudioSelectManager.
    It relies on console hardware detection.
 */
public class AudioPlayback {
    @NonNls protected static final String TAG = "AudioPlayback";

    private Thread playThread;

    public void start() {
        //if (playThread != null) return;
        release();
        if (playThread == null) {
            playThread = new PlaybackThread();
            playThread.setName("AudioPlaybackThread");
            playThread.start();
        }
    }

    public void release() {
        if (playThread != null) {
            playThread.interrupt();
            try { playThread.join(250); }
            catch (Exception ignored) {}
            playThread = null;
        }
    }
}

@SuppressLint("MissingPermission")
class PlaybackThread extends Thread {
    AudioRecord audioRecord;
    AudioTrack audioTrack;

    @Override
    public void run() {
        int mInBufSize;
        byte[] mIn;
        synchronized (this) {
            // Setup
            AudioConfig audioConfig = AudioConfig.detect();
            mInBufSize = AudioRecord.getMinBufferSize(audioConfig.sampleRate, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            audioRecord = new AudioRecord(audioConfig.audioSource, audioConfig.sampleRate,
                    AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, mInBufSize);
            //Log.d(AudioPlayback.TAG, "mInBufSize= " + mInBufSize);
            mIn = new byte[mInBufSize];

            int outBufSize = AudioTrack.getMinBufferSize(audioConfig.sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            audioTrack = new AudioTrack(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                            .build(),
                    new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(audioConfig.sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build(),
                    outBufSize,
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE);

            try {
                // Start
                audioRecord.startRecording();
                audioTrack.play();
                // Loop
                while (!isInterrupted()) {
                    int bufferReadResult = audioRecord.read(mIn, 0, mInBufSize);
                    if (bufferReadResult == AudioRecord.ERROR_INVALID_OPERATION) {
                        Log.e(AudioPlayback.TAG, "The AudioRecord object was not properly initialized");
                        break;
                    } else if (bufferReadResult == AudioRecord.ERROR_BAD_VALUE) {
                        Log.e(AudioPlayback.TAG, "The parameters do not resolve to valid data and indexes.");
                        break;
                    } else if (bufferReadResult > mIn.length) { // is this even possible?
                        Log.e(AudioPlayback.TAG, "Read more bytes than is buffer length:" + bufferReadResult + ": " + mIn.length);
                        break;
                    } else if (bufferReadResult == 0) {
                        Log.e(AudioPlayback.TAG, "Read zero bytes");
                        break;
                    } else if (bufferReadResult > 0) {
                        audioTrack.write(mIn, 0, bufferReadResult);
                    }
                } // END WHILE
            } catch (Exception e) {
                Log.e(AudioPlayback.TAG, "Failed to start audio loop", e);
            }
            audioTrack.release();
            audioRecord.release();
        } // END SYNC
    } // END RUN
}

//TODO: Should we put something into ProductConfig for this? Do we need to cover sample rates as
// well?
enum AudioConfig {
    Parker(MediaRecorder.AudioSource.MIC, 44100),
    //TODO: Does Parker 3 need something here? I think it was something about setting the audio
    // level somewhere but I think this is fine. - James, 7/17

    //TODO: Supplied example uses 48000 for HDMI, but Parker is using 44100? Sample seems fine
    // with 44.1?
    UltraThin(MediaRecorder.AudioSource.CAMCORDER, 44100),
    Unknown(MediaRecorder.AudioSource.MIC, 44100),
    ; // END

    public final int audioSource;
    public final int sampleRate;
    AudioConfig(int source, int rate) {
        this.audioSource = source;
        this.sampleRate = rate;
    }

    public static AudioConfig detect() {
        switch (ProductConfig.hardware) {
            case Parker:
            case Parker_3:
                return Parker;
            case UltraThin:
                return UltraThin;
            default:
                return Unknown;
        }
    }
}
