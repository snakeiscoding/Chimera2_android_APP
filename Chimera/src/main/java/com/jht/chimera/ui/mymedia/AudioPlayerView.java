package com.jht.chimera.ui.mymedia;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.BTSinkKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UsbMediaDeviceKeys;
import com.jht.chimera.lib.common.enums.JHTTypes;
import com.jht.chimera.lib.common.mediadevice.MediaItem;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.gymkitmodule.comm.GsCommands;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 */

public class AudioPlayerView extends Fragment {
    private static final String TAG = "AudioPlayerView";

    private Context mContext;
    private JHTTypes.REPEAT_MODE mRepeatMode = JHTTypes.REPEAT_MODE.NONE;
    private boolean mShuffle;
    private PlaybackState mPlaybackState = PlaybackState.PAUSED;
    private Bitmap mOldBitmap;
    private boolean mLongPressed;

    private ImageView mAlbumCover;
    private ImageView mRepeatOne;
    private ToggleButton btnRepeatAudio;
    private ToggleButton btnShuffleAudio;
    private ToggleButton mPlayPauseButton;
    private TextView mItemTitle;
    private TextView mAlbum;
    private SeekBar mSkBarProgress;
    private Handler mHandler;
    private TextView mElapsedTime;
    private TextView mRemainingTime;
    private View mView;

    private boolean isSinkDevice = false;

    private long mCurrentClickTime;
    private LongClickThread mLongClickThread;
    private final TouchListener mTouchL = new TouchListener();

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    enum PlaybackState {
        PLAYING, PAUSED
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DataMatrix.getBoolean(HALKeys.DATA_IS_USING_BLE_BROWSING_BOOL)) {
            isSinkDevice = true;
        }
        mHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.audio_player_view, container, false);
        return mView;
    }

    private class LongClickThread implements Runnable {
        View view;
        public LongClickThread(View v) {
            mLongPressed = false;
            view = v;
        }

        @Override
        public void run() {
            mLongPressed = true;
            onLongClick(view);
        }
    }

    private class TouchListener implements View.OnTouchListener {

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mCurrentClickTime = Calendar.getInstance().getTimeInMillis();
                    mLongClickThread = new LongClickThread(v);
                    mHandler.postDelayed(mLongClickThread, ViewConfiguration.getLongPressTimeout());
                    break;
                case MotionEvent.ACTION_UP:
                    if (Calendar.getInstance().getTimeInMillis() - mCurrentClickTime <= ViewConfiguration.getLongPressTimeout()) {
                        mHandler.removeCallbacks(mLongClickThread);
                    }
                    onTouchUp(v);
                    break;
            }
            return true;
        }
    }

    @SuppressLint("NonConstantResourceId")
    private void onTouchUp(View v) {
        int viewId = v.getId();
        switch (viewId) {
            case R.id.next:
                if (!mLongPressed) {
                    // click
                    next();
                } else {
                    if (isSinkDevice) {
                        DataMatrix.putInt(HALKeys.ACTION_SEND_BT_SINK_COMMAND_INT, GsCommands.AvrcpFastForwardUp.Value);
                    } else {
                        DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_TOUCH_UP);
                    }
                }
                break;
            case R.id.previous:
                if (!mLongPressed) {
                    // click
                    previous();
                } else {
                    if (isSinkDevice) {
                        DataMatrix.putInt(HALKeys.ACTION_SEND_BT_SINK_COMMAND_INT, GsCommands.AvrcpRewindUp.Value);
                    } else {
                        DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_TOUCH_UP);
                    }
                }
                break;
            case R.id.btnRepeatAudio:
                if (isSinkDevice) {
                    setRepeatMode();
                } else {
                    if (!mLongPressed) {
                        // click
                        setRepeatMode();
                    } else {
                        DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_TOUCH_UP);
                    }
                }
                break;
            case R.id.btnShuffleAudio:
                if (isSinkDevice) {
                    setShuffleMode();
                } else {
                    if(!mLongPressed) {
                        setShuffleMode();
                    } else {
                        DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_TOUCH_UP);
                    }
                }
                break;
            case R.id.btn_audio_player_play_pause:
                if (!mLongPressed) {
                    Log.d(TAG, "playback state: " + mPlaybackState.name());
                    if(mPlaybackState == PlaybackState.PLAYING) {
                        pause();
                    } else if (mPlaybackState == PlaybackState.PAUSED) {
                        play();
                    }
                } else {
                    if (!isSinkDevice) {
                        DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_TOUCH_UP);
                    }
                }
                break;
        }
    }

    @SuppressLint("NonConstantResourceId")
    private void onLongClick(View v) {
        int viewId = v.getId();
        switch (viewId) {
            case R.id.btnRepeatAudio:
                if (!isSinkDevice) {
                    DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_REPEAT_DOWN);
                }
                break;
            case R.id.btnShuffleAudio:
                if (!isSinkDevice) {
                    DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_SHUFFLE_DOWN);
                }
                break;
            case R.id.next:
                if (isSinkDevice) {
                    DataMatrix.putBoolean(BTSinkKeys.DATA_BT_SINK_CAN_START_SONG_POSITION_TIMER_BOOL, false);
                }
                fastForward();
                break;
            case R.id.previous:
                if (isSinkDevice) {
                    DataMatrix.putBoolean(BTSinkKeys.DATA_BT_SINK_CAN_START_SONG_POSITION_TIMER_BOOL, false);
                }
                rewind();
                break;
            case R.id.btn_audio_player_play_pause:
                if (!isSinkDevice) {
                    DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_PLAY_PAUSE_DOWN);
                }
                break;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAlbumCover = view.findViewById(R.id.album_cover);
        ImageButton mPreviousButton = view.findViewById(R.id.previous);
        ImageButton mNextButton = view.findViewById(R.id.next);
        btnRepeatAudio = view.findViewById(R.id.btnRepeatAudio);
        mRepeatOne = view.findViewById(R.id.iconRepeatOne);
        btnShuffleAudio = view.findViewById(R.id.btnShuffleAudio);
        mPlayPauseButton = view.findViewById(R.id.btn_audio_player_play_pause);
        mItemTitle = view.findViewById(R.id.item_name);
        mAlbum = view.findViewById(R.id.album);
        mSkBarProgress = view.findViewById(R.id.seekbar);
        if(!isSinkDevice) {
            mSkBarProgress.setOnSeekBarChangeListener(new SeekBarChangeEvent());
        } else {
            mSkBarProgress.setEnabled(false);
        }
        mElapsedTime = view.findViewById(R.id.elapsed_time);
        mRemainingTime = view.findViewById(R.id.remaining_time);

        showShuffleButton(DataMatrix.getBoolean(UsbMediaDeviceKeys.DATA_IS_SHUFFLE_BOOL));
        showRepeatButton(JHTTypes.REPEAT_MODE.values()[DataMatrix.getInt(UsbMediaDeviceKeys.DATA_REPEAT_MODE_ENUM_INT)]);

        mPlayPauseButton.setOnTouchListener(mTouchL);
        mNextButton.setOnTouchListener(mTouchL);
        mPreviousButton.setOnTouchListener(mTouchL);
        btnRepeatAudio.setOnTouchListener(mTouchL);
        btnShuffleAudio.setOnTouchListener(mTouchL);

    }

    void updateAlbumCover(Bitmap bm) {
        if(bm != null) {
            if(mOldBitmap != null) {
                final Bitmap bitmapToRecycle = mOldBitmap;
                mHandler.postDelayed(bitmapToRecycle::recycle, 500);

            }
            mAlbumCover.setImageDrawable(new BitmapDrawable(mContext.getResources(), bm));
        } else {
            if(mOldBitmap != null) {
                final Bitmap bitmapToRecycle = mOldBitmap;
                mHandler.postDelayed(bitmapToRecycle::recycle, 500);

            }
            mAlbumCover.setImageResource(R.drawable.background_audio_no_album);
        }

        mOldBitmap = bm;
    }

    void updateCurrentTrack(MediaItem current) {
        mSkBarProgress.setMax(current.duration);;
        mItemTitle.setText(current.title);
        mAlbum.setText(current.album);
    }

    void updateArtistName(String name) {
        mAlbum.setText(name);
    }

    void updateSongName(String name) {
        mItemTitle.setText(name);
    }

    void setMaxBarProgress(int max) {
        mSkBarProgress.setMax(max);
    }

    void updatePosition(int pos) {
        mSkBarProgress.setProgress(Math.min(pos, mSkBarProgress.getMax()));
        int positionInMSec = (pos / 1000) * 1000;
        mElapsedTime.setText(getTimeString(positionInMSec));
        mRemainingTime.setText(getRemainingTimeString(mSkBarProgress.getMax() - positionInMSec));
    }


    void updatePlayingStatus(boolean isPlaying) {

        Log.e(TAG, "notifyPlaybackStatusChanged. isPlaying: " + isPlaying);

        if(isPlaying) {
            showPauseButton();
        } else {
            showPlayButton();
        }
    }

    class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener {
        int mProgress;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser) {
                mProgress = progress;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int progress = seekBar.getProgress();
            seek(progress);
        }
    }

    synchronized private void setRepeatMode() {
        JHTTypes.REPEAT_MODE mode = null;
        switch (mRepeatMode) {
            case NONE:
                mode = JHTTypes.REPEAT_MODE.ALL;
                break;
            case ALL:
                mode = JHTTypes.REPEAT_MODE.ONE;
                break;
            case ONE:
                mode = JHTTypes.REPEAT_MODE.NONE;
                break;
        }
        showRepeatButton(mode);
        if(isSinkDevice){
            Bundle bundle = new Bundle();

            switch (mode) {
                case NONE:
                    bundle.putInt("id", 2);
                    bundle.putInt("value", 1);
                    break;
                case ALL:
                    bundle.putInt("id", 2);
                    bundle.putInt("value", 3);
                    break;
                case ONE:
                    bundle.putInt("id", 2);
                    bundle.putInt("value", 2);
                    break;
            }

            DataMatrix.putParcelable(HALKeys.ACTION_SET_CURRENT_PLAYER_APP_SETTING_VALUE_PARCELABLE, bundle);
        }
        else {
            DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_MEDIA_REPEAT);
        }
    }

    synchronized void showRepeatButton(JHTTypes.REPEAT_MODE repeatMode) {
        if(mRepeatMode == repeatMode) {}
        Log.d(TAG,"notifyCurrentPlayerAppSettingsChanged. repeatMode: " + repeatMode.name());

        switch(repeatMode) {
            case NONE:
            case ALL:
                mRepeatOne.setVisibility(View.GONE);
                break;
            case ONE:
                mRepeatOne.setVisibility(View.VISIBLE);
                break;
        }

        btnRepeatAudio.setChecked(repeatMode == JHTTypes.REPEAT_MODE.ALL || repeatMode == JHTTypes.REPEAT_MODE.ONE);
        mRepeatMode = repeatMode;
    }

    private void setShuffleMode() {
        showShuffleButton(!mShuffle);
        if(isSinkDevice){
            Bundle bundle = new Bundle();
            if(mShuffle) {
               bundle.putInt("id", 3);
               bundle.putInt("value", 2);

            } else {
                bundle.putInt("id", 3);
                bundle.putInt("value", 1);
            }
            DataMatrix.putParcelable(HALKeys.ACTION_SET_CURRENT_PLAYER_APP_SETTING_VALUE_PARCELABLE, bundle);
        }
        else {
            DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_MEDIA_SHUFFLE);
        }
    }

    void showShuffleButton(boolean shuffle) {
        if(mShuffle == shuffle) {
         //   return;
        }

        Log.e(TAG,"notifyCurrentPlayerAppSettingsChanged. shuffleMode: " + shuffle);

        mShuffle = shuffle;
        btnShuffleAudio.setChecked(shuffle);
    }

    private void showPauseButton() {

        Log.e(TAG, "showPauseButton");
        mPlaybackState = PlaybackState.PLAYING;
        mPlayPauseButton.setChecked(false);
    }

    private void showPlayButton() {

        Log.e(TAG, "showPlayButton");
        mPlaybackState = PlaybackState.PAUSED;
        mPlayPauseButton.setChecked(true);
    }

    public void play() {

        Log.e(TAG, "play()");

        if(mPlaybackState == PlaybackState.PLAYING) {
            Log.e(TAG, "play(). playbackstate is PLAYING");
           // return;
        }

        if(isSinkDevice){
            Log.e(TAG, "play(). isSinkDevice");
            DataMatrix.putInt(HALKeys.ACTION_SEND_BT_SINK_COMMAND_INT, GsCommands.AvrcpPlay.Value);
        } else {
            if(mPlaybackState == PlaybackState.PLAYING) {
                return;
            }

            DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_MEDIA_PLAY);
        }
    }

    public void pause() {

        Log.e(TAG, "pause()");
        if(mPlaybackState == PlaybackState.PAUSED) {
            Log.e(TAG, "pause(). playbackstate is PAUSED");
            //return;
        }

        if(isSinkDevice){
            Log.e(TAG, "pause(). isSinkDevice");
            DataMatrix.putInt(HALKeys.ACTION_SEND_BT_SINK_COMMAND_INT, GsCommands.AvrcpPause.Value);
        } else {
            if(mPlaybackState == PlaybackState.PAUSED) {
                 return;
            }

            DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_MEDIA_PAUSE);
        }
    }

    private void next() {
        if(isSinkDevice) {
            DataMatrix.putInt(HALKeys.ACTION_SEND_BT_SINK_COMMAND_INT, GsCommands.AvrcpNext.Value);
        } else {
            DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_MEDIA_NEXT);
        }
    }

    private void previous() {
        if(isSinkDevice){
            DataMatrix.putInt(HALKeys.ACTION_SEND_BT_SINK_COMMAND_INT, GsCommands.AvrcpPrevious.Value);
        }
        else {
            DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_MEDIA_PREVIOUS);
        }
    }

    private void fastForward() {
        if(isSinkDevice){
            DataMatrix.putInt(HALKeys.ACTION_SEND_BT_SINK_COMMAND_INT, GsCommands.AvrcpFastForward.Value);
        }
        else {
            DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_MEDIA_FASTFORWARD);
        }
    }

    private void rewind() {
        if(isSinkDevice){
            DataMatrix.putInt(HALKeys.ACTION_SEND_BT_SINK_COMMAND_INT, GsCommands.AvrcpRewind.Value);
        }
        else {
            DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_MEDIA_REWIND);
        }
    }

    private void seek(int pos) {
        DataMatrix.putInt(UsbMediaDeviceKeys.ACTION_SEEK_INT, pos);
    }

    private String getTimeString(int time) {
        time /= 1000;
        //noinspection HardCodedStringLiteral,HardCodedStringLiteral
        return String.format(Locale.getDefault(), "%02d", time / 60) + ":"
                + String.format(Locale.getDefault(), "%02d", time % 60);
    }

    private String getRemainingTimeString(int time) {
        if(time < 0) {
            return "-:-";
        }

        return "-" + getTimeString(time) + " ";
    }

    public void hide() {
        mView.setVisibility(View.GONE);
    }

    public void show() {
        Log.e(TAG, "will show");
        mView.setVisibility(View.VISIBLE);
    }

}
