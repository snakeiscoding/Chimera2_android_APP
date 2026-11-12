package com.jht.chimera.ui.mymedia;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jht.chimera.lib.common.datamatrixkeys.UsbMediaDeviceKeys;
import com.jht.chimera.lib.common.mediadevice.MediaItem;
import com.jht.datamatrixlib.DataMatrix;

public abstract class MediaPlayerController extends Fragment {
    private static final String TAG = "MediaPlayerController";

    protected Handler mHandler;
    protected Uri mUri;
    MediaBrowser mMediaBrowser;
    MediaItem mSelectedItem;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
    }

    protected void selectTopLevel(boolean video) {
        DataMatrix.putBoolean(UsbMediaDeviceKeys.ACTION_SELECT_TOP_LEVEL_BOOL, video);
    }

    protected void select(MediaItem item) {
//        mSelectedItem = item;
        DataMatrix.putParcelable(UsbMediaDeviceKeys.ACTION_SELECT_PARCELABLE, item);
    }

    protected abstract boolean isVideoPlayer();
    protected abstract void updateItems();
}
