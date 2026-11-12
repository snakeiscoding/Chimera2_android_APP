package com.jht.chimera.ui.mymedia;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.UsbMediaDeviceKeys;
import com.jht.chimera.lib.common.mediadevice.MediaItem;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class VideoPlayerController extends MediaPlayerController {
    private static final String TAG = "VideoPlayerController";

    private View mView;
    private VideoPlayerView mVideoPlayer;
    private final DataMatrix mDataMatrix = new DataMatrix();
    private int mPlayingIndex;
    private final List<MediaItem> mVideos = new ArrayList<>();
    private VideoPlayerView.OnScreenSizeChangeListener mOnScreenSizeChangeListener;

    public static VideoPlayerController newInstance() {
        return new VideoPlayerController();
    }

    public VideoPlayerController() {
        // Required empty public constructor
    }

    private void initDataMatrix() {
        UsbMediaDeviceKeys.registerEventMediaItemFound(mDataMatrix, item -> {
            if (item != null && item.isVideo) {
                mVideos.add(item);
                mMediaBrowser.add(item);
//                updateItems();
            }
        });

        mDataMatrix.createStringAdapter(UsbMediaDeviceKeys.DATA_MEDIA_URI_STRING, uri -> {
            Log.d(TAG, "uri: " + uri);
            if (mSelectedItem != null && mSelectedItem.isVideo == isVideoPlayer()) {
                mHandler.post(() -> mVideoPlayer.play(mSelectedItem, uri));
            }
        });
    }

    @Override
    protected boolean isVideoPlayer() {
        return true;
    }

    @Override
    protected void updateItems() {
        mHandler.post(() -> mMediaBrowser.update(mVideos));
    }

    void setOnBackListener(VideoPlayerView.OnBackListener l) {
        mVideoPlayer.setOnBackListener(l);
    }

    void setOnScreenSizeChangeListener(VideoPlayerView.OnScreenSizeChangeListener l) {
        mOnScreenSizeChangeListener = l;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initDataMatrix();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.video_player_controller, container, false);
        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMediaBrowser = (MediaBrowser) getChildFragmentManager().findFragmentById(R.id.video_browser);
        if(mMediaBrowser != null) {
            mMediaBrowser.setOnBrowseListener(new MediaBrowser.OnBrowseListener() {
                @Override
                public void onItemClick(final MediaItem item, int position) {
                    Log.d(TAG, "Click item: " + item.toString());
                    mSelectedItem = item;
                    mPlayingIndex = position;
                    mVideoPlayer.resetHideFullScreenControlTimer();
                    mMediaBrowser.hide();
                    mVideoPlayer.show();
                    mOnScreenSizeChangeListener.onMediumScreen();
                    select(item);
                    getUri(item);
                }

                @Override
                public void onBrowseUp() {

                }
            });

            mVideoPlayer = (VideoPlayerView) getChildFragmentManager().findFragmentById(R.id.video_player_view);
            if(mVideoPlayer != null) {
                mVideoPlayer.setOnVideoControlListener(new VideoPlayerView.OnVideoControlListener() {
                    @Override
                    public void onNext() {
                        if (++mPlayingIndex >= mVideos.size()) {
                            mPlayingIndex = 0;
                        }
                        final MediaItem item = mVideos.get(mPlayingIndex);
                        Log.d(TAG, "Next item: " + item.toString());
                        mSelectedItem = item;
                        select(item);
                        getUri(item);
                    }

                    @Override
                    public void onPrevious() {
                        if (--mPlayingIndex < 0) {
                            mPlayingIndex = 0;
                        }
                        final MediaItem item = mVideos.get(mPlayingIndex);
                        Log.d(TAG, "Previous item: " + item.toString());
                        mSelectedItem = item;
                        select(item);
                        getUri(item);
                    }
                });
            }
        }

        if(mVideoPlayer != null) {
            mVideoPlayer.hide();
        }

        selectTopLevel(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mDataMatrix.finish();
    }

    void showDefault() {
        mVideoPlayer.hide();
        mMediaBrowser.show();
    }

    public void hide() {
        mView.setVisibility(View.GONE);
//        mSelectedItem = null;
    }

    public void show() {
        mView.setVisibility(View.VISIBLE);
    }

    private void getUri(MediaItem item) {
        if (item == null) {
            return;
        }

        DataMatrix.putParcelable(UsbMediaDeviceKeys.ACTION_GET_MEDIA_URI_PARCELABLE, item);
    }
}
