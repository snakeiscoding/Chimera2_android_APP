package com.jht.chimera.lib.widget.activity;

import androidx.appcompat.app.AppCompatActivity;

import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;

public class AudioControllerActivity extends RestringActivity {

    @Override
    public void onResume() {
        super.onResume();
        if (ProductConfig.hardware != ProductConfig.ConsoleHardware.Parker_3)
            DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, audioSourceInput().ordinal());
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop audio.
        DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.Foreground.ordinal());
    }

    protected AudioSourceInput audioSourceInput() {
        return AudioSourceInput.Foreground;
    }

    protected void setVolume(int volume) {
        DataMatrix.putInt(HALKeys.DATA_SYSTEM_VOLUME_INT, volume);
    }

}
