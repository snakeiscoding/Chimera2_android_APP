package com.jht.chimera.service.usbmedia.mtpums.lib.ums;

import android.Manifest;
import android.content.Context;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;

import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.enums.JHTTypes;
import com.jht.chimera.lib.common.permission.GrantPermission;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.service.usbmedia.mtpums.IOnScanListener;
import com.jht.chimera.service.usbmedia.mtpums.MtpUmsDevice;
import com.jht.chimera.service.usbmedia.mtpums.lib.media.Track;
import com.jht.datamatrixlib.DataMatrix;
import com.mpatric.mp3agic.Mp3File;

import java.util.ArrayList;
import java.util.List;

public class UmsMediaController extends MtpUmsDevice {
    private static final String TAG = "UmsMediaController";

    private IOnScanListener mListener;
    private List<String> mItemIds = new ArrayList<>();
    private ScanThread scanThread;

    public UmsMediaController(Context context, DataMatrix dataMatrix) {
        super(context, "USB", dataMatrix); //NON-NLS

        mDeviceType = JHTTypes.MediaDeviceType.UMS;

        GrantPermission.grant(context, context.getPackageName(), Manifest.permission.WRITE_EXTERNAL_STORAGE, context.getApplicationInfo().uid);
        GrantPermission.grant(context, context.getPackageName(), Manifest.permission.READ_EXTERNAL_STORAGE, context.getApplicationInfo().uid);

        mListener = new IOnScanListener() {
            @Override
            public void onOneCompletion(Track track, boolean isVideo) {
//                Log.d(TAG, "onOneCompletion: track: " + track.getId() + "-" + track.getTitle() + ", is video: " + isVideo);
                try {
                    mItemIds.add(track.getId());
                    if (!isVideo) {
                        mAudioQueue.put(track);
                    } else {
                        mVideoQueue.put(track);
                    }
                } catch (InterruptedException e) {
                    Log.w(TAG, e);
                }
            }

            @Override
            public void onAllCompletion() {
                try {
                    Track track = new Track();
                    track.setTitle("Exit"); //NON-NLS
                    mAudioQueue.put(track);
                    mVideoQueue.put(track);
                } catch (InterruptedException e) {
                    Log.w(TAG, e);
                }
            }
        };

        scan();

    }

    @Override
    public void scan() {
        scanThread = new ScanThread();
        scanThread.start();
    }

    @Override
    protected byte[] getAlbumArt(Track track) {
        byte[] albumArt = null;
        try {
            Mp3File mp3FileBuffer = new Mp3File(track.getUri(), false);
            if(mp3FileBuffer.hasId3v2Tag()) {

                albumArt = mp3FileBuffer.getId3v2Tag().getAlbumImage();

            }
        } catch(Exception e) {
            Log.w(TAG, e);
        }

        return albumArt;
    }

    class ScanThread implements Runnable {
        private UmsMediaScanner scanner = new UmsMediaScanner(mContext, mListener);

        public void start() {
            Thread t = new Thread(this);
            t.start();
        }

        @Override
        public void run() {
            scanner.scanDir(StorageUtils.getUSBPath().getAbsolutePath());
        }

        public void stop() {
            scanner.stop();
        }
    }

    @Override
    public void disconnect() {
        super.disconnect();

        if(scanThread != null) {
            scanThread.stop();
        }

        clearMediaDB();
    }

    private void clearMediaDB() {
        int num = mItemIds.size() / 200;
        if(mItemIds.size() % 200 > 0) {
            num++;
        }

        for(int i = 0; i < num; i++) {
            int len;
            String placeHolder;
            if(i+1 < num) {
                len = 200;
                placeHolder = constructPlaceholders(len);
            } else {
                len = mItemIds.size() - (i * 200);
                placeHolder = constructPlaceholders(len);
            }

            if(placeHolder == null) {
                return;
            }
            String select = BaseColumns._ID + " IN (" + placeHolder + ")"; //NON-NLS
            String[] selectionArg = new String[len];
            for(int j = 0; j < len; j++) {
                selectionArg[j] = mItemIds.get((i * 200) + j);
            }
            if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_2) || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_3) || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)) {
                mContext.getContentResolver().delete(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, select, selectionArg);
                mContext.getContentResolver().delete(MediaStore.Video.Media.INTERNAL_CONTENT_URI, select, selectionArg);
            } else {
                mContext.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, select, selectionArg);
                mContext.getContentResolver().delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, select, selectionArg);
            }
        }
    }

    private String constructPlaceholders(int len) {
        if (len < 1) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder(len * 2 - 1);
            sb.append("?");
            for (int i = 1; i < len; i++) {
                sb.append(",?");
            }
            return sb.toString();
        }
    }
}
