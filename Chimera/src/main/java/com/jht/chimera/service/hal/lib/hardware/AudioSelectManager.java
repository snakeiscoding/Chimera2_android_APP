package com.jht.chimera.service.hal.lib.hardware;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.drivers.audio.AudioController;
import com.jht.chimera.drivers.audio.AudioProperties;
import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.SystemPropertyUtil;
import com.jht.chimera.lib.common.app.AndroidSetting;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.widget.sound.SystemVolume;
import com.jht.datamatrixlib.DataMatrix;

import java.util.concurrent.atomic.AtomicInteger;

public class AudioSelectManager {
    public final String TAG = "AudioSelectManager";
    private final DataMatrix mDataMatrix = new DataMatrix();
    private AudioController audioController;
    private AudioPlayback audioPlayer; // audio redirection for parker / UT
    private int previousRunState = WorkoutKeys.Constants.RUN_STATE_STOP;

    public static AudioSelectManager getInstance() {
        return AudioSelectManager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final AudioSelectManager INSTANCE = new AudioSelectManager();
    }

    /**
     * Some consoles need to toggle HDMI input audio via some properties.
     * Parker1 doesn't need it, but UT does.
     * @param audioManager
     * @param on
     */
    private void setHDMI(AudioManager audioManager, boolean on) {
        if (on) {
            audioManager.setParameters("HDMIin_enable=true"); //NON-NLS
            SystemPropertyUtil.set("media.audio.hdmiin", "1");
        } else {
            SystemPropertyUtil.set("media.audio.hdmiin", "0");
            audioManager.setParameters("HDMIin_enable=false"); //NON-NLS
        }
    }
    public static int getMasterVolume() {
        int volume  = DataMatrix.getInt(SettingKeys.Hardware.DATA_MASTER_VOLUME_V2_INT);
        if (DataMatrix.getBoolean(SettingKeys.Hardware.DATA_AUDIO_DISABLED_BOOL)) volume = 0;
        return volume;
    }

    public void start() {
        Context context = ContextUtil.getApplicationContext();
        if(context == null) return;
        audioController = new AudioController(context,
                new AudioProperties(8192, 2),
                new AudioProperties(8192, 2),
                DataMatrix.getBoolean(TopBarKeys.DATA_BT_HEADPHONE_CONNECTED_BOOL),
                getMasterVolume());
        AudioManager audioManager;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);

        DataMatrix.putString(HALKeys.DATA_VERSION_AUDIO_STRING, audioController.getVersion());
        AndroidSetting.putString(ContextUtil.getApplicationContext().getContentResolver(), "chimera_audio_version", audioController.getVersion()); //NON-NLS
        Log.d(TAG, "Audio version: " + audioController.getVersion());

        // The audio source needs to be initialized to Default before the CSafe Audio will work
        if(audioController.selectAudioSource(AudioSourceInput.Default)) {
            AudioController.getAudioFocus(context);
            DataMatrix.putInt(HALKeys.DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM_INT, AudioSourceInput.Default.ordinal());
        }

        mDataMatrix.createIntAdapter(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, audioSourceOrdinal -> {
            setAudioSourceInput(audioSourceOrdinal, audioManager, context);
        });

        mDataMatrix.createBoolAdapter(TopBarKeys.DATA_BT_HEADPHONE_CONNECTED_BOOL, connected -> {
            int delay;
            switch (ProductConfig.hardware) {
                case Chimera:
                    delay = connected ? DataMatrix.getInt(SettingKeys.Experimental.DATA_WIRELESS_HEADPHONE_HDMI_DELAY_INT) : DataMatrix.getInt(SettingKeys.Experimental.DATA_WIRED_HEADPHONE_HDMI_DELAY_INT);
                    hdmiDelayByFile(delay);
                    audioController.onBTChanged(connected);
                    break;
                case Parker:
                case Parker_3:
                // TODO Check if Parker 3.0 needs different delay logic. Keeping logic inline with
                //  Parker for now.
                    hdmiDelayByDriver(connected ? 90000 : 0);
                    break;
                case UltraThin:
                    hdmiDelayByProp(connected ? 30 : 1);
                    break;
                case Chimera_2:
                    if (connected) {
                        hdmiDelayByOs(18, 105);
                    } else {
                        hdmiDelayByOs(0, 0);
                    }
                    audioController.onBTChanged(connected);
                    break;
            }
            // Need to add a delay.  Saw a log where this happened slightly before the BT connection
            // completed.  The delay is ok because there may be background music, but the user sees
            // the pairing app at this time.
            JHTTimer timer = new JHTTimer(1500);
            timer.setOnTimer(() -> {
                // This logic needs reviewing. Do we need to not run this again if the value never changes?
                // DATA_BT_HEADPHONE_CONNECTED_BOOL is pushed by the TopBar which gets recreated? Should it use update value instead of put
                audioController.refreshVolume();
                timer.stop(); // This will leak if stop is not called
            });
            timer.start("refreshVolume");
        });

        mDataMatrix.createIntAdapter(HALKeys.DATA_IOS_AUDIO_SAMPLE_RATE_INT, rate -> audioController.setSampleRate(AudioSourceInput.IOS, rate));

        mDataMatrix.createIntAdapter(HALKeys.DATA_SYSTEM_VOLUME_INT, integer -> {
            boolean disableAudio = DataMatrix.getBoolean(SettingKeys.Hardware.DATA_AUDIO_DISABLED_BOOL);
            if (disableAudio) {
                return;
            }
            audioController.setVolume(integer);
        });
        mDataMatrix.createBoolAdapter(HALKeys.DATA_SYSTEM_VOLUME_MUTE_BOOL, muteVolume -> {
            Log.v(TAG, "in mute adapter: " + muteVolume);
            audioController.mute(muteVolume);
        });
        mDataMatrix.createFloatAdapter(HALKeys.DATA_SYSTEM_VOLUME_LIMIT_RATIO_FLOAT, limitRatio -> {
            Log.v(TAG, "in limit adapter: " + limitRatio);
            audioController.refreshVolume();
        });

        mDataMatrix.createBoolAdapter(HALKeys.DATA_IS_BT_SINK_CONNECTED_BOOL, aBoolean -> updateAudioMixing());

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, runState -> {
            if (previousRunState == WorkoutKeys.Constants.RUN_STATE_STOP && runState == WorkoutKeys.Constants.RUN_STATE_RUNNING) {
                if (!ProductConfig.isRetail()) {

                    Log.v(TAG, "run state RUNNING. volume: " + getMasterVolume());
                    // reset the volume to default at the start of a workout
                    SystemVolume.getInstance().setVolume(getMasterVolume());
                }
            } else if (runState == WorkoutKeys.Constants.RUN_STATE_STOP) {
                Log.v(TAG, "run state STOP");
                if (ProductConfig.isCommercial()) {
                    // stop the music
                    SystemVolume.getInstance().setVolume(0);
                }
                AudioController.getAudioFocus(context);
            }
            previousRunState = runState;
        }, false);
        mDataMatrix.createEventAdapter(HALKeys.ACTION_SPLASH_RESET, value -> setAudioSourceInput(AudioSourceInput.Default.ordinal(), audioManager, context));
    }

    private void setAudioSourceInput(int audioSourceOrdinal, AudioManager audioManager, Context context) {
        try {
            AudioSourceInput audioSourceInput = AudioSourceInput.values()[audioSourceOrdinal];
            Log.d(TAG, "ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT: " + audioSourceInput);
            if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker) || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)
                    || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.UltraThin)) {
                if (audioPlayer == null) audioPlayer = new AudioPlayback();
                switch (audioSourceInput) {
                    case HDMI:
                        setHDMI(audioManager, true);
                        Driver.GpioWrapper.audioSwitchHDMI();
                        audioPlayer.start();
                        break;
                    case RemoteTV:
                        setHDMI(audioManager, false);
                        Driver.GpioWrapper.audioSwitchCSAFE();
                        audioPlayer.start();
                        break;
                    case BT_Sink:
                        setHDMI(audioManager, false);
                        Driver.GpioWrapper.audioSwitchBtSink();
                        audioPlayer.start();
                        break;
                    case LineIn:
                        Driver.GpioWrapper.audioSwitchEXTLINE();
                        audioPlayer.start();
                        break;
                    case USB:
                        Driver.GpioWrapper.audioSwitchUSB();
                        break;
                    case ForegroundLite:
                        if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)) {
                            Driver.GpioWrapper.audioSwitchVA();
                            break;
                        }
                    case Foreground:
                        if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)) {
                            Driver.GpioWrapper.audioSwitchWiFi();
                        }
                        else {
                            setHDMI(audioManager, false);
                            Driver.GpioWrapper.audioSwitchNone();
                            if (ProductConfig.hasAuxAudioInput()) {
                                audioPlayer.start(); // TODO only start if AUX IN is plugged in
                            } else {
                                audioPlayer.release();
                            }
                        }
                        break;
                    default:
                        if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)) {
                            Driver.GpioWrapper.audioSwitchUSB();
                        }
                        else {
                            setHDMI(audioManager, false);
                            Driver.GpioWrapper.audioSwitchNone();
                            if (ProductConfig.hasAuxAudioInput()) {
                                audioPlayer.start(); // TODO only start if AUX IN is plugged in
                            } else {
                                audioPlayer.release();
                            }
                        }
                        break;
                }
                AudioController.getAudioFocus(context);
                DataMatrix.putInt(HALKeys.DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM_INT, audioSourceOrdinal);
            } else if (ProductConfig.hardware.isVortexBase()) {
                // TODO VORTEX Do we need to do any audio control???
                // Adding logic to start playing audio from MIC for RemoteTV. Any other logic needed? Use a switch statement if there's more logic
                if (audioPlayer == null) audioPlayer = new AudioPlayback();
                if (audioSourceInput == AudioSourceInput.RemoteTV) {
                    audioPlayer.start();
                } else {
                    audioPlayer.release();
                }
                AudioController.getAudioFocus(context);
                DataMatrix.putInt(HALKeys.DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM_INT, audioSourceOrdinal);
            } else {
                if (audioController.selectAudioSource(audioSourceInput)) {
                    AudioController.getAudioFocus(context);
                    DataMatrix.putInt(HALKeys.DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM_INT, audioSourceOrdinal);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    /**
     * @param delay
     */
    private void hdmiDelayByFile(int delay) {
        ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
        executeAsRoot.addCommand("echo " + delay + " > /mnt/jhtdata1/HdmiTimeSet");
        executeAsRoot.addCommand("chmod 666 /mnt/jhtdata1/HdmiTimeSet");
        executeAsRoot.execute();
    }

    /**
     * @param delay
     */
    private void hdmiDelayByDriver(int delay) {
        int ret = HdmiIn.getInstance().setVideoDelay(delay); // basically writes to /dev/delay_video
        Log.d(TAG, "onReceive: set video delay " + delay + ", ret " + ret);
    }

    /**
     * @param frames
     */
    private void hdmiDelayByProp(int frames) {
        SystemPropertyUtil.set("camera.cachedframe.size", "" + frames);
    }

    private void hdmiDelayByOs(int hdmiSet1, int hdmiSet2) {
        ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
        executeAsRoot.addCommand("echo " + hdmiSet1 + "> /mnt/jhtdata2/HdmiTimeSet");
        executeAsRoot.addCommand("chmod 666 /mnt/jhtdata2/HdmiTimeSet");
        executeAsRoot.addCommand("echo " + hdmiSet2 + "> /mnt/jhtdata2/HdmiTimeSet2");
        executeAsRoot.addCommand("chmod 666 /mnt/jhtdata2/HdmiTimeSet2");
        executeAsRoot.execute();
    }
    
    public void stop() {
        mDataMatrix.finish();
    }

    /**
     * Logic will mix the BT sink if the BT sink connection changes.
     */
    private void updateAudioMixing() {
        try {
            boolean isBTSinkConnected = DataMatrix.getBoolean(HALKeys.DATA_IS_BT_SINK_CONNECTED_BOOL);
            // we are not sure if other flavor needs this so adding this for parker for now
            if (ProductConfig.isParker()) {
                // Fix Bluetooth and aux in switch exception when use one device
                if (!isBTSinkConnected) {
                    audioController.selectAudioSource(AudioSourceInput.Default);
                }
            } else if(ProductConfig.product.equals(ProductConfig.Product.Chimera)) {
                if (!isBTSinkConnected && DataMatrix.getInt(HALKeys.DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM_INT) == AudioSourceInput.BT_Sink.ordinal()) {
                    DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.Foreground.ordinal());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public interface IOnAudioLoss {
        void onAudioLoss();
    }

    /**
     * @param source
     * @param dm
     * @param onAudioLoss
     */
    public static void monitorAudioLoss(@NonNull AudioSourceInput source, @NonNull DataMatrix dm, @NonNull IOnAudioLoss onAudioLoss) {
        AtomicInteger previousSource = new AtomicInteger(-1);
        dm.createIntAdapter(HALKeys.DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM_INT, newSource -> {
            if(previousSource.get() != -1  && previousSource.get() == source.ordinal() && newSource != previousSource.get()) {
                onAudioLoss.onAudioLoss();
            }
            previousSource.set(newSource);
        }, false);
    }
}
