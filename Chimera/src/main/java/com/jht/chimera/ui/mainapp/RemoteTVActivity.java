
package com.jht.chimera.ui.mainapp;

import android.Manifest;
import android.os.Bundle;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.jht.chimera.lib.widget.activity.RestringActivity;
import com.jht.androiduiwidgets.SimpleInputConfiguration;
import com.jht.androiduiwidgets.pillbox.PillBox;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.lib.common.permission.GrantPermission;
import com.jht.chimera.lib.widget.sound.SystemVolume;
import com.jht.datamatrixlib.DataMatrix;

public class RemoteTVActivity extends RestringActivity {

    DataMatrix mDataMatrix = new DataMatrix();
    ToggleButton mPlayPauseButton;
    private boolean mHasAudioFocus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remote_tv);

        GrantPermission.grant(this, getPackageName(), Manifest.permission.RECORD_AUDIO, getApplicationInfo().uid);

        final SimpleInputConfiguration volumeConfiguration = new SimpleInputConfiguration();
        volumeConfiguration.min(0);
        volumeConfiguration.max(BuildConfig.MAX_VOLUME);
        volumeConfiguration.value(SystemVolume.getInstance().getVolume());
        volumeConfiguration.label(getResources().getText(R.string.volume_tc).toString());
        ((PillBox) findViewById(R.id.remote_tv_volume_pill)).initParameterSettings(volumeConfiguration);
        ((PillBox) findViewById(R.id.remote_tv_volume_pill)).setiValueSelectorInterface((v, s) -> {
            if ((int) v != SystemVolume.getInstance().getVolume()) {
                SystemVolume.getInstance().setVolume((int) v);
            }
        });


        SimpleInputConfiguration channelConfiguration = new SimpleInputConfiguration();
        if (DataMatrix.getInt(TVKeys.DATA_REMOTE_TV_NUM_CHANNELS_INT) > 0) {
            channelConfiguration.min(1);
            channelConfiguration.max(DataMatrix.getInt(TVKeys.DATA_REMOTE_TV_NUM_CHANNELS_INT));
            channelConfiguration.value(DataMatrix.getInt(SettingKeys.TV.DATA_TV_REMOTE_TV_DEFAULT_CHANNEL_INT));
        } else {
            channelConfiguration.min(0);
            channelConfiguration.max(0);
            channelConfiguration.value(0);
        }

        channelConfiguration.label(getResources().getText(R.string.channel_tc).toString());
        ((PillBox) findViewById(R.id.remote_tv_channel_pill)).initParameterSettings(channelConfiguration);
        ((PillBox) findViewById(R.id.remote_tv_channel_pill)).setiValueSelectorInterface((v, s) -> DataMatrix.putInt(TVKeys.DATA_REMOTE_TV_CURRENT_CHANNEL_INT, (int) v));

        mPlayPauseButton = findViewById(R.id.btn_audio_player_play_pause);
        mPlayPauseButton.setOnClickListener(v -> {
            if (DataMatrix.getInt(HALKeys.DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM_INT) == AudioSourceInput.RemoteTV.ordinal()) {
                DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, DataMatrix.getBoolean(HALKeys.DATA_BT_SINK_CONNECTED_BOOL) ? AudioSourceInput.BT_Sink.ordinal() : AudioSourceInput.Foreground.ordinal());
            }
            else {
                DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.RemoteTV.ordinal());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDataMatrix.createIntAdapter(HALKeys.DATA_SYSTEM_VOLUME_INT, integer -> ((PillBox) findViewById(R.id.remote_tv_volume_pill)).setValue(integer));
        mDataMatrix.createIntAdapter(HALKeys.DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM_INT, audioSelection -> mPlayPauseButton.setChecked(audioSelection != AudioSourceInput.RemoteTV.ordinal()));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDataMatrix.finish();
    }

}



