package com.jht.chimera.service.usbmedia.mtpums;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UsbMediaDeviceKeys;
import com.jht.chimera.lib.common.enums.JHTTypes;
import com.jht.chimera.lib.common.mediadevice.MediaItem;
import com.jht.chimera.service.hal.lib.hardware.AudioSelectManager;
import com.jht.chimera.service.usbmedia.UsbMediaDevice;
import com.jht.chimera.service.usbmedia.mtpums.lib.media.MediaItemCreator;
import com.jht.chimera.service.usbmedia.mtpums.lib.media.MediaTree;
import com.jht.chimera.service.usbmedia.mtpums.lib.media.Node;
import com.jht.chimera.service.usbmedia.mtpums.lib.media.Track;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class MtpUmsDevice extends UsbMediaDevice {
    @NonNls private static final String TAG = "MtpUmsDevice";
    protected static final String MTP_LOCAL_ADDR = "http://localhost:8050/"; //NON-NLS
    private static final int SKIP_PERIOD_MSEC = 5000;

    private MediaPlayer mMediaPlayer = new MediaPlayer();
    protected boolean mIsPaused;
    protected MediaItem mPlayingItem;
    private int mPlayingIndex;
    private List<MediaItem> mItems = new ArrayList<>();
    protected BlockingQueue<Track> mAudioQueue = new LinkedBlockingQueue<>();
    protected BlockingQueue<Track> mVideoQueue = new LinkedBlockingQueue<>();
    private Node mCurrentNode;
    private MediaTree mMediaTree = new MediaTree();
    private List<Track> mAudios = new ArrayList<>();
    private List<Track> mVideos = new ArrayList<>();
    private boolean mIsRewinding;
    private JHTTypes.REPEAT_MODE mRepeatMode = JHTTypes.REPEAT_MODE.NONE;
    private int mStreamType = AudioManager.STREAM_MUSIC;
    private PeriodicTimer mPlayingStatusTimer;
    private PeriodicTimer mFFTimer;
    private PeriodicTimer mRewindTimer;
    private boolean mIsShuffle;
    private boolean mIsFFing;

    public MtpUmsDevice(Context context, String deviceName, DataMatrix dataMatrix) {
        super(context);
        mDeviceName = deviceName;
        updateDeviceInfo();

        mAudioCreator = new MediaItemCreator(context, mAudioQueue, false);
        mAudioCreator.setOnCreateMediaItemListener((item, track) -> {
            if (mCurrentNode.getCategory() != Node.Category.TOP_LEVEL) {
                Node child = new Node(mCurrentNode.getCategory(), item.id, item.title);
                mCurrentNode.add(child);
            }
            mItems.add(item);
            if (track != null) {
                mAudios.add(track);
            }

        });

        mVideoCreator = new MediaItemCreator(context, mVideoQueue, true);
        mVideoCreator.setOnCreateMediaItemListener((item, track) -> mVideos.add(track));

        AudioSelectManager.monitorAudioLoss(AudioSourceInput.UserAudio, dataMatrix, () -> pause(true));
    }

    private void updateDeviceInfo() {
        DataMatrix.putString(UsbMediaDeviceKeys.DATA_DEVICE_NAME_STRING, mDeviceName);
        DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_HAS_VIDEO_BOOL, true);
        DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_CONNECTED_BOOL, true);
    }

    @Override
    public void selectTopLevel(boolean video) {
        super.selectTopLevel(video);
        if (video) {
            if (mVideoCreator != null) {
                mVideoCreator.stop();
                mVideoCreator.start();
            }
        } else {
            mCurrentNode = mMediaTree.resetToTop();
            mAudioCreator.setCurrentNode(mCurrentNode);
            if (mAudioCreator != null) {
                mAudioCreator.stop();
                mAudioCreator.start();
            }
        }
    }

    @Override
    public synchronized boolean select(MediaItem item) {
        if (item.isTrack) {
            mIsPaused = false;
            mPlayingItem = item;
            int index = 0;
            if (!item.isVideo) {
                for (Track track : mAudios) {
                    if (item.id.equals(track.getId())) {
                        mPlayingIndex = index;
                        break;
                    }
                    index++;
                }
            } else {
                for (Track track : mVideos) {
                    if (item.id.equals(track.getId())) {
                        mPlayingIndex = index;
                        break;
                    }
                    index++;
                }
            }

            return false;
        }

        if (mAudioCreator != null) {
            mAudioCreator.stop();
        }

        mAudios.clear();
        mItems.clear();

        if (mCurrentNode != null && mAudioCreator != null) {
            for (Node node : mCurrentNode) {
                if (node.getId().equals(item.id)) {
                    mCurrentNode = mMediaTree.next(node);
                    mCurrentNode.getParent().setId(item.id);
                    mCurrentNode.getParent().setName(item.title);
                    break;
                }
            }

            mAudioCreator.setItem(item);
            mAudioCreator.setCurrentNode(mCurrentNode);
            mAudioCreator.setVideo(false);
            mAudioCreator.start();
        }

        return true;
    }

    @Override
    public void back() {
        if (mAudioCreator != null) {
            mAudioCreator.stop();
        }
        mAudios.clear();
        mItems.clear();

        mCurrentNode = mMediaTree.previous();
        if (mCurrentNode == null) {
            return;
        }

        if (mCurrentNode.getCategory() != Node.Category.TOP_LEVEL) {
            MediaItem item = new MediaItem();
            item.id = mCurrentNode.getParent().getId();
            item.title = mCurrentNode.getParent().getName();
            mAudioCreator.setItem(item);
        }
        mAudioCreator.setCurrentNode(mCurrentNode);
        mAudioCreator.setVideo(false);
        mAudioCreator.start();

    }

    @Override
    public void play(boolean auto) {
        if (mIsRewinding) {
            if (mRewindTimer != null) {
                mRewindTimer.stop();
                mRewindTimer = null;
            }
            mIsRewinding = false;
            mIsPaused = false;
            return;
        }

        if (mIsFFing) {
            if (mFFTimer != null) {
                mFFTimer.stop();
                mFFTimer = null;
            }
            mIsFFing = false;
            mIsPaused = false;
            return;
        }

        play2();
    }

    @Override
    public void pause(boolean auto) {
        mIsPaused = true;
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
        }
    }

    @Override
    public void next() {
        mIsPaused = false;
        MediaItem nextItem = getNextItem(true);
        if (nextItem.equals(mPlayingItem)) {
            mIsPaused =true;
            DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_PLAYING_BOOL, !mIsPaused);
            return;
        }
        mPlayingItem = nextItem;
        play2();
    }

    @Override
    public void previous() {
        mIsPaused = false;
        MediaItem previousItem = getNextItem(false);
        if (previousItem.equals(mPlayingItem)) {
            return;
        }
        mPlayingItem = previousItem;
        play2();

    }

    @Override
    public void fastForward() {
        mIsPaused = true;
        mIsFFing = true;
        if (mFFTimer != null) {
            mFFTimer.stop();
            mFFTimer = null;
        }

        mFFTimer = new PeriodicTimer(1000, 500, () -> {
            int currentPos = mMediaPlayer.getCurrentPosition();
            int newPos = currentPos + SKIP_PERIOD_MSEC;
            mMediaPlayer.seekTo(newPos);
        });
        mFFTimer.start();

        DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_PLAYING_BOOL, !mIsPaused);
    }

    @Override
    public void rewind() {
        mIsPaused = true;
        mIsRewinding = true;
        if (mRewindTimer != null) {
            mRewindTimer.stop();
            mRewindTimer = null;
        }

        mRewindTimer = new PeriodicTimer(1000, 500, () -> {
            int currentPos = mMediaPlayer.getCurrentPosition();
            int newPos = currentPos - SKIP_PERIOD_MSEC;
            if (newPos < 0) {
                newPos = 0;
            }
            mMediaPlayer.seekTo(newPos);
        });
        mRewindTimer.start();

        DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_PLAYING_BOOL, !mIsPaused);
    }

    @Override
    public void seek(int pos) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(pos);
        }
    }

    @Override
    public void repeat() {
        switch (mRepeatMode) {
            case NONE:
                mRepeatMode = JHTTypes.REPEAT_MODE.ALL;
                break;
            case ALL:
                mRepeatMode = JHTTypes.REPEAT_MODE.ONE;
                break;
            case ONE:
                mRepeatMode = JHTTypes.REPEAT_MODE.NONE;
                break;
        }
    }

    @Override
    public void repeatDown() {}

    @Override
    public void touchUp() {}

    @Override
    public void shuffle() {
        mIsShuffle = !mIsShuffle;
    }

    @Override
    public void shuffleDown() {}

    @Override
    public void playPauseButtonDown() {}

    @Override
    public String getUri(MediaItem item) {
        String uri = null;
        if (!item.isVideo) {
            for (Track track : mAudios) {
                if (track.getId().equals(item.id)) {
                    uri = track.getUri();
                    break;
                }
            }
        } else {
            for (Track track : mVideos) {
                if (track.getId().equals(item.id)) {
                    uri = track.getUri();
                    break;
                }
            }
        }

        return uri;
    }

    @Override
    public void disconnect() {
        super.disconnect();

        stopPlayingStatusTimer();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (mAudioCreator != null) {
            mAudioCreator.stop();
        }

        if (mVideoCreator != null) {
            mVideoCreator.stop();
        }
    }

    synchronized protected void play2() {
        //MTP uri: "http://localhost:8050/" + playMediaFileName
        if (mPlayingItem != null) {
            DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.UserAudio.ordinal());
            String mediaUri = null;
            if (mIsPaused) {
                mIsPaused = false;
                mMediaPlayer.start();
            } else {
                stopPlayingStatusTimer();
                if (mDeviceType == JHTTypes.MediaDeviceType.MTP) {
                    if (mAudios.size() == 0 || mPlayingIndex >= mAudios.size()) {
                        Log.d(TAG, "play2: mAudios size is 0 or index >= mAudios size");
                        return;
                    }
                    mediaUri = MTP_LOCAL_ADDR + mAudios.get(mPlayingIndex).getUri();
                } else if (mDeviceType == JHTTypes.MediaDeviceType.UMS) {
                    if (mAudios.size() == 0 || mPlayingIndex >= mAudios.size()) {
                        Log.d(TAG, "play2: mAudios size is 0 or index >= mAudios size");
                        return;
                    }
                    mediaUri = mAudios.get(mPlayingIndex).getUri();
                }
                Log.d(TAG, "play2: Play " + mediaUri);

                if (mediaUri != null && mediaUri.length() > 0) {
                    if (mAudios.size() == 0 || mPlayingIndex >= mAudios.size()) {
                        Log.d(TAG, "play2: mAudios size is 0 or index >= mAudios size");
                        return;
                    }
                    byte[] albumArt = getAlbumArt(mAudios.get(mPlayingIndex));
                    DataMatrix.putBytes(UsbMediaDeviceKeys.DATA_ALBUM_ART_BYTES, albumArt);
                    try {
                        mMediaPlayer.reset();
                        mMediaPlayer.setVolume(0.26f, 0.26f);
                        mMediaPlayer.setDataSource(mediaUri);
                        mMediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setLegacyStreamType(mStreamType)
                                .build());

                        /*mMediaPlayer.prepareAsync();
                        mMediaPlayer.setOnPreparedListener(mp -> {
                            Log.d(TAG, "play2: the media source is ready for playback");
                            mMediaPlayer.start();
                            startPlayingStatusTimer();
                        });*/
                        mMediaPlayer.prepare();
                        Log.d(TAG, "play2: the media source is ready for playback");
                        mMediaPlayer.start();
                        startPlayingStatusTimer();
                        mMediaPlayer.setOnCompletionListener(mp -> {
                            switch (mRepeatMode) {
                                case NONE:
                                case ALL:
                                    next();
                                    break;
                                case ONE:
                                    play2();
                                    break;
                            }
                        });

                        mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                            Log.d(TAG, "play2: onError: " + what + " " + extra);
                            return false;
                        });

                    } catch (IOException e) {
                        Log.d(TAG, "play2: exception: ", e);
                    }
                }
            }
        }
        Log.d(TAG, "play2: end");
    }

    private void startPlayingStatusTimer() {
        mPlayingItem.duration = mMediaPlayer.getDuration();
        DataMatrix.putParcelable(UsbMediaDeviceKeys.DATA_CURRENT_MEDIA_ITEM_PARCELABLE, mPlayingItem);
        mPlayingStatusTimer = new PeriodicTimer(100, 500, () -> {
            int pos = mMediaPlayer.getCurrentPosition();
            DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_PLAYING_BOOL, !mIsPaused);
            DataMatrix.putInt(UsbMediaDeviceKeys.DATA_PLAYBACK_POSITION_INT, pos);
        });
        mPlayingStatusTimer.start();
    }

    private void stopPlayingStatusTimer() {
        if (mPlayingStatusTimer != null) {
            mPlayingStatusTimer.stop();
            mPlayingStatusTimer = null;
        }
    }

    private int getRandomIndex() {
        int index = 0;
        if (mAudios != null && mAudios.size() > 1) {
            Random randomGen = new Random();
            int tries = 0;
            do {
                index = randomGen.nextInt(mAudios.size());
            } while (index == mPlayingIndex && tries++ < 5);
        }
        return index;
    }

    protected MediaItem getNextItem(boolean next) {
        if (mIsShuffle) {
            mPlayingIndex = getRandomIndex();
        } else {
            if (next) {
                mPlayingIndex++;
            } else {
                mPlayingIndex--;
            }
        }

        if (next) {

            if (mPlayingIndex >= mAudios.size()) {
                if (mRepeatMode == JHTTypes.REPEAT_MODE.ALL||mRepeatMode==JHTTypes.REPEAT_MODE.ONE) {
                    mPlayingIndex = 0;
                } else if (mRepeatMode == JHTTypes.REPEAT_MODE.NONE) {
                    mPlayingIndex--;
                }
            }
        } else {
            if (mPlayingIndex < 0) {
                mPlayingIndex = 0;
            }
        }

        return mItems.get(mPlayingIndex);
    }


}
