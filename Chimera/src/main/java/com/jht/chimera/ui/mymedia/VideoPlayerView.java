package com.jht.chimera.ui.mymedia;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UsbMediaDeviceKeys;
import com.jht.chimera.lib.common.mediadevice.MediaItem;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link VideoPlayerView#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VideoPlayerView extends Fragment implements View.OnClickListener, View.OnTouchListener, TextureView.SurfaceTextureListener {
    private static final String TAG = "VideoPlayerView";
    private static final int HIDE_FULL_SCREEN_MEDIA_CONTROLS_DELAY = 5000;
    private static final int UPDATE_INTERNAL_TIME = 1000;

    private Context mContext;
    private Timer mTimer;
    private MediaPlayer mMediaPlayer;

    private boolean mPaused;
    private String mUri;
    private MediaItem mCurrentItem;
    private UpdateHandler mUpdateHandler;
    private PeriodicTimer mUpdateTimer;
    private final ReentrantLock mLock = new ReentrantLock();
    private OnVideoControlListener mOnVideoControlListener;
    private OnBackListener mOnBackListener;
    private ScreenSize mScreenSize = ScreenSize.MEDIUM;
    private FrameLayout mFlFullScreenControl;
    private TextView mTvTitle, tvElapsedTime, tvRemainingTime;
    private ToggleButton mToggleButton;
    private SeekBar mSkBarProgress;
    private int mediumSurfaceWidth;
    private int mediumSurfaceHeight;
    private int fullSurfaceWidth;
    private int fullSurfaceHeight;
    private int surfaceWidth;
    private int surfaceHeight;
    private View mView;
    private TextureView mVideoView;
    private Surface mSurface;
    private boolean mIsShown;
    private FrameLayout mDolbyLogo;

    @NonNls private final static String DDP_LIB_EVENT = "com.nex.DDP_LIB_EVENT";

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable: width: " + width + ", height: " + height);
        mSurface = new Surface(surface);
        if (mediumSurfaceWidth == 0) {
            mediumSurfaceWidth = width;
            mediumSurfaceHeight = height;
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged: width: " + width + ", height: " + height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed: ");
        mSurface = null;
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public enum ScreenSize {
        MEDIUM, FULL_SCREEN
    }

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    public interface OnVideoControlListener {
        void onNext();
        void onPrevious();
    }

    public interface OnBackListener {
        void onBack();
    }

    public interface OnScreenSizeChangeListener {
        void onMediumScreen();
    }

    public static VideoPlayerView newInstance() {
        return new VideoPlayerView();
    }

    public void setOnVideoControlListener(OnVideoControlListener l) {
        mOnVideoControlListener = l;
    }

    public void setOnBackListener(OnBackListener l) {
        mOnBackListener = l;
    }

    public VideoPlayerView() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUpdateHandler = new UpdateHandler(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.video_player_view, container, false);
        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initView(view);
    }

    private void initView(View view) {
        view.findViewById(R.id.video_container).setOnTouchListener(this);
        mFlFullScreenControl = view.findViewById(R.id.fl_fullScreenControl);
        mVideoView = view.findViewById(R.id.tv_video);
        mVideoView.setSurfaceTextureListener(this);
        mVideoView.setRotation(0);
        Button mBtnPrevious = view.findViewById(R.id.btn_video_player_previous);
        Button mBtnNext = view.findViewById(R.id.btn_video_player_next);
        mSkBarProgress = view.findViewById(R.id.videoSeekBar);
        mSkBarProgress.setOnSeekBarChangeListener(new SeekBarChangeEvent());
        mBtnNext.setOnClickListener(this);
        mBtnPrevious.setOnClickListener(this);
        mToggleButton = view.findViewById(R.id.btn_video_player_play_pause);
        mToggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // pause the video
                pause();
            } else {
                // play or resume the video
                play(mCurrentItem, mUri);
            }
        });

        mTvTitle = view.findViewById(R.id.tv_title);
        tvElapsedTime = view.findViewById(R.id.video_elapsed_time);
        tvRemainingTime = view.findViewById(R.id.video_remaining_time);

        ImageButton mShowVideoList = view.findViewById(R.id.btn_show_video_list);
        mShowVideoList.setOnClickListener(v -> {
            pause();

            if (mScreenSize == ScreenSize.FULL_SCREEN) {
                mScreenSize = ScreenSize.MEDIUM;
                DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.REGULAR);
            }

            mOnBackListener.onBack();
        });

        ToggleButton ibExpand = view.findViewById(R.id.ib_expand);
        ibExpand.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mScreenSize == ScreenSize.MEDIUM) {
                Log.d(TAG, "onClick: Full Screen");
                DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.FULLSCREEN);
                mScreenSize = ScreenSize.FULL_SCREEN;

                DisplayMetrics displayMetrics = new DisplayMetrics();
                WindowManager manager = ((Activity) mContext).getWindowManager();
                manager.getDefaultDisplay().getMetrics(displayMetrics);
                fullSurfaceWidth = displayMetrics.widthPixels;
                fullSurfaceHeight = displayMetrics.heightPixels;
                surfaceWidth = fullSurfaceWidth;
                surfaceHeight = fullSurfaceHeight;
                //                changeVideoSize(surfaceWidth, surfaceHeight);
            } else {
                Log.d(TAG, "onClick: Medium Screen");
                mScreenSize = ScreenSize.MEDIUM;
                DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.REGULAR);
                surfaceWidth = mediumSurfaceWidth;
                surfaceHeight = mediumSurfaceHeight;
                //                changeVideoSize(surfaceWidth, surfaceHeight);
            }
            updateTextureViewSizeCenter();
        });

        mDolbyLogo = view.findViewById(R.id.dolby_logo);

    }

    BroadcastReceiver dolbyAudioReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: Dolby Audio");
            mDolbyLogo.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> mDolbyLogo.setVisibility(View.GONE), 5000);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (mCurrentItem != null && mIsShown) {
            play(mCurrentItem, mUri);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(DDP_LIB_EVENT);
        mContext.registerReceiver(dolbyAudioReceiver, filter);
    }

    @Override
    public void onPause() {
        // pause video when when fragment is paused
        pause();

        if (mScreenSize == ScreenSize.FULL_SCREEN) {
            mScreenSize = ScreenSize.MEDIUM;
            DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.REGULAR);
        }

        mContext.unregisterReceiver(dolbyAudioReceiver);
//        mShowVideoList.callOnClick();

        // reset the current item since the surface view has been created
        // TODO: if we want to resume the old video, we would need to seek to current location
//        mCurrentItem = null;

        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mUpdateHandler != null) {
            mUpdateHandler.removeMessages(0);
        }

        stopUpdateProgress();

        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
        }

    }

    class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//            if(fromUser) {
//                resetHideFullScreenControlTimer();
//                mProgress = progress;
//            }

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int progress = seekBar.getProgress();
            mMediaPlayer.seekTo(progress);
        }
    }

    public void play(MediaItem item, String uri) {
        mTvTitle.setText(item.title);
        if (mPaused && item.equals(mCurrentItem)) {
            DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_MEDIA_PAUSE);
            mMediaPlayer.start();
            mPaused = false;
            return;
        }

        mToggleButton.setChecked(false);
        mPaused = false;
        mUri = uri;
        mCurrentItem = item;
        if (uri != null) {
            mSkBarProgress.setProgress(0);
            play(uri);
        }
    }


    private void play(final String uri) {
        Log.d(TAG, "play: " + uri);
        DataMatrix.sendEvent(UsbMediaDeviceKeys.ACTION_MEDIA_PAUSE);
        try {
            if (mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer();
            }
            mMediaPlayer.reset();
            mMediaPlayer.setVolume(0.26f, 0.26f);
            mMediaPlayer.setDataSource(uri);
            mMediaPlayer.setSurface(mSurface);
            mMediaPlayer.prepareAsync();
            mMediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build());
            mMediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "onPrepared - video width: " + mMediaPlayer.getVideoWidth() + ", video height: " + mMediaPlayer.getVideoHeight());
                if (mScreenSize == ScreenSize.MEDIUM) {
                    surfaceWidth = mediumSurfaceWidth;
                    surfaceHeight = mediumSurfaceHeight;
                } else {
                    surfaceWidth = fullSurfaceWidth;
                    surfaceHeight = fullSurfaceHeight;
                }
                updateTextureViewSizeCenter();
//                changeVideoSize(surfaceWidth, surfaceHeight);
                mMediaPlayer.start();
                mSkBarProgress.setMax(mMediaPlayer.getDuration());
                updateProgress();
            });

            mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.d(TAG, "OnError - Error code: " + what + " Extra code: " + extra);
                return false;
            });

            mMediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "onCompletion: " + uri + " is completed.");
                mOnVideoControlListener.onNext();
            });
        } catch (IOException e) {
            Log.w(TAG, e);
        }

    }

    private void updateProgress() {
        if (mUpdateTimer != null) {
            return;
        }

        mUpdateTimer = new PeriodicTimer(1000, UPDATE_INTERNAL_TIME, () -> {
            if (mMediaPlayer != null && !mSkBarProgress.isPressed()) {
                mUpdateHandler.sendEmptyMessage(0);
            }
        });
        mUpdateTimer.start();
    }

    private void stopUpdateProgress() {
        if (mUpdateTimer != null) {
            mUpdateTimer.stop();
            mUpdateTimer = null;
        }
    }

    private void pause() {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
        }
        mPaused = true;
    }

    private void next() {
        try {
            mLock.lock();
            mPaused = false;
            stopUpdateProgress();
            mOnVideoControlListener.onNext();
        } finally {
            mLock.unlock();
        }
    }

    private void previous() {
        try {
            mLock.lock();
            mPaused = false;
            stopUpdateProgress();
            mOnVideoControlListener.onPrevious();
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.btn_video_player_next) {
            next();
        } else if (i == R.id.btn_video_player_previous) {
            previous();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (v.getId() == R.id.video_container) {
                if (mFlFullScreenControl.getVisibility() == View.VISIBLE) {
                    mFlFullScreenControl.setVisibility(View.GONE);
                } else {
                    mFlFullScreenControl.setVisibility(View.VISIBLE);
                    resetHideFullScreenControlTimer();
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            resetHideFullScreenControlTimer();
            v.performClick();
        }

        return true;
    }

    void resetHideFullScreenControlTimer() {
        if (mTimer != null) {
            mTimer.cancel();
        }

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                ((Activity) mContext).runOnUiThread(() -> mFlFullScreenControl.setVisibility(View.GONE));
            }
        }, HIDE_FULL_SCREEN_MEDIA_CONTROLS_DELAY);
    }

    private String getTimeString(int time) {
        time /= 1000;
        return String.format(Locale.getDefault(), "%02d", time / 60) + ":"
                + String.format(Locale.getDefault(), "%02d", time % 60);
    }

    private String getRemainingTimeString(int time) {
        if (time < 0) {
            return "-:-";
        }

        return "-" + getTimeString(time) + " ";
    }

    private static class UpdateHandler extends Handler {
        private final WeakReference<VideoPlayerView> mVideoPlayerView;

        UpdateHandler(VideoPlayerView videoPlayerView) {
            mVideoPlayerView = new WeakReference<>(videoPlayerView);
        }

        @Override
        public void handleMessage(@NotNull Message msg) {
            VideoPlayerView videoPlayerView = mVideoPlayerView.get();

            if (videoPlayerView != null) {
                int progress = videoPlayerView.mMediaPlayer.getCurrentPosition();
                videoPlayerView.mSkBarProgress.setProgress(progress);
                int positionInMSec = (progress / 1000) * 1000;
                videoPlayerView.tvElapsedTime.setText(videoPlayerView.getTimeString(positionInMSec));
                videoPlayerView.tvRemainingTime.setText(videoPlayerView.getRemainingTimeString(videoPlayerView.mSkBarProgress.getMax() - positionInMSec));
            }
        }
    }



    public void hide() {
        mView.setVisibility(View.GONE);
        mIsShown = false;
    }

    public void show() {
        mView.setVisibility(View.VISIBLE);
        mIsShown = true;
    }



    private void updateTextureViewSizeCenter(){
        int videoWidth = mMediaPlayer.getVideoWidth();
        int videoHeight = mMediaPlayer.getVideoHeight();

        float sx = (float) surfaceWidth / (float) videoWidth;
        float sy = (float) surfaceHeight / (float) videoHeight;

        Matrix matrix = new Matrix();
        matrix.preTranslate(((float)surfaceWidth - videoWidth) / 2, ((float)surfaceHeight - videoHeight) / 2);
        matrix.preScale(videoWidth / (float) surfaceWidth, videoHeight / (float) surfaceHeight);
        if (sx >= sy){
            matrix.postScale(sy, sy, (float)surfaceWidth / 2, (float)surfaceHeight / 2);
        }else{
            matrix.postScale(sx, sx, (float)surfaceWidth / 2, (float)surfaceHeight / 2);
        }

        mVideoView.setTransform(matrix);
        mVideoView.postInvalidate();
    }
}
