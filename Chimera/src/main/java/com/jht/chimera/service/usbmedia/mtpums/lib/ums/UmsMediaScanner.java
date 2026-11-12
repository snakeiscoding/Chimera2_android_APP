package com.jht.chimera.service.usbmedia.mtpums.lib.ums;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;

import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.service.usbmedia.mtpums.IOnScanListener;
import com.jht.chimera.service.usbmedia.mtpums.lib.media.Track;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class UmsMediaScanner implements MediaScannerConnection.MediaScannerConnectionClient {
    private static final String TAG = "UmsMediaScanner";

    private Context mContext;
    private File mFile;
    private int mFileNum;
    private MediaScannerConnection mConn;
    private int mScanCount;
    private List<Integer> mMediaIds = new ArrayList<>();
    private List<File> mFiles = new ArrayList<>();
    private IOnScanListener mListener;
    private List<Integer> mAudioIds = new ArrayList<>();
    private List<Integer> mVideoIds = new ArrayList<>();
    private String[] excludeDirs = {"lost.dir", "system volume information"}; //NON-NLS NON-NLS
    private String usbPath = null; // latch onto first udisk path and ignore any alternative udisk paths

    public UmsMediaScanner(Context context, IOnScanListener listener) {
        mContext = context;
        mConn = new MediaScannerConnection(context, this);
        mListener = listener;
    }

    public void scanDir(String path) {
        mFile = new File(path);
        countFileInDir(mFile);
        mConn.connect();

    }

    public void stop() {
        mConn.disconnect();
    }

    private int countFileInDir(File file) {
        File[] files = file.listFiles();
        for (File f : files) {
            boolean ignore = false;
            for(String dir : excludeDirs) {
                if(f.getName().toLowerCase().equals(dir)) {
                    ignore = true;
                    break;
                }
            }

            if(ignore) {
                continue;
            }

            if (f.isDirectory()) {
                countFileInDir(f);
            } else {
                mFiles.add(f);
                mFileNum++;
            }
        }

        return mFileNum;
    }

    private void scan(File file, String type) {
        if(file.isFile()) {
            mConn.scanFile(file.getAbsolutePath(), type);
//            Log.d(TAG, file.getAbsolutePath() + " is scanning");
            return;
        }

        File[] files = file.listFiles();
        if(files == null) {
            return;
        }
        for(File f : files) {
            scan(f, type);
        }
    }

    private void scan(String type) {
        if(mScanCount == mFileNum) {
            mConn.disconnect();
            mListener.onAllCompletion();
            return;
        }

        if(mConn.isConnected()) {
            mConn.scanFile(mFiles.get(mScanCount).getAbsolutePath(), type);
        }

        mScanCount++;
    }

    @Override
    public void onMediaScannerConnected() {
        scan(null);
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        queryMedia(true);
        queryMedia(false);
        scan(null);
    }

    private void queryMedia(boolean isAudio) {
        Track track;
        String audioSelection = MediaStore.Audio.AudioColumns.IS_MUSIC + " != 0";
        String audioIdSelection = "";
        String videoIdSelection = "";
        for(int i : mAudioIds) {
            audioIdSelection = audioIdSelection + "'" + i + "',";
        }
        if(!audioIdSelection.equals("")) {
            audioIdSelection = audioIdSelection.substring(0, audioIdSelection.length() - 1);
            audioSelection = audioSelection + " AND " + BaseColumns._ID + " NOT IN (" + audioIdSelection + ")"; //NON-NLS
        }

        for(int j : mVideoIds) {
            videoIdSelection = videoIdSelection + "'" + j + "',";
        }
        if(!videoIdSelection.equals("")) {
            videoIdSelection = videoIdSelection.substring(0, videoIdSelection.length() - 1);
        }

        String[] genresProjection = {
                MediaStore.Audio.Genres.NAME,
                MediaStore.Audio.Genres._ID
        };

        String[] audioCols = {
                BaseColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATA, // uri
                MediaStore.Audio.AudioColumns.ARTIST,
                MediaStore.Audio.AudioColumns.ARTIST_ID,
                MediaStore.Audio.AudioColumns.ALBUM,
                MediaStore.Audio.AudioColumns.ALBUM_ID,
                MediaStore.MediaColumns.TITLE,
                MediaStore.Audio.AudioColumns.DURATION,
                MediaStore.MediaColumns.DATE_ADDED
        };

        String[] videoCols = {
                BaseColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATA, // uri
                MediaStore.Video.VideoColumns.ARTIST,
                MediaStore.Video.VideoColumns.ALBUM,
                MediaStore.MediaColumns.TITLE,
                MediaStore.Video.VideoColumns.DURATION,
                MediaStore.MediaColumns.DATE_ADDED
        };

        String[] cols = isAudio ? audioCols : videoCols;

        Uri uri;

        // The scanner is closer to Chimera than Parker 1.
        boolean audioConfigBoolean = ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_2)
                || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)
                || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_3);
        if (isAudio) {
            if (audioConfigBoolean) {
                uri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
            } else {
                uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }
        } else {
            if (audioConfigBoolean) {
                uri = MediaStore.Video.Media.INTERNAL_CONTENT_URI;
            } else {
                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            }
        }

        Cursor cur = mContext.getContentResolver().query(
//                isAudio ? MediaStore.Audio.Media.EXTERNAL_CONTENT_URI : MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                uri,
                cols,
                isAudio ? audioSelection :
                        (videoIdSelection.equals("") ? null : BaseColumns._ID + " NOT IN (" + videoIdSelection + ")"), //NON-NLS
                null,
                MediaStore.MediaColumns.DATE_ADDED
        );

        if(cur == null || cur.getCount() == 0) {
//            cur.close();
            return ;
        }

        cur.moveToFirst();
        do {
            String mediaUri = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA));
            if (mediaUri == null) {
                continue;
            }

            //udisk can be indexed as two potential paths and vary between consoles (some index /storage/udisk, /mnt/media_rw/udisk, or both)
            //https://stackoverflow.com/questions/27655703/what-is-the-difference-between-storage-udisk-sda4-and-mnt-media-rw-udisk-s#
            //Android14 doesn't give permission to /mnt/media_rw but /storage should work for Android 14 and older consoles
            if (usbPath == null) {
                final String uDisk = "/udisk/"; // check for slash to avoid potential to catch udisk in a file name
                int indexUDiskPath = mediaUri.indexOf(uDisk);
                if (indexUDiskPath < 0) continue;
                usbPath = mediaUri.substring(0, indexUDiskPath + uDisk.length());
            } else if (!mediaUri.contains(usbPath)) {
                continue;
            }

            int id = cur.getInt(cur.getColumnIndex(BaseColumns._ID));
            if(mMediaIds.contains(id)) {
                continue;
            }

            if(isAudio) {
                mMediaIds.add(id);
                mAudioIds.add(id);
                uri = MediaStore.Audio.Genres.getContentUriForAudioId("external", id); //NON-NLS
                String[] genre = getGenre(uri, genresProjection);

                track = new Track();
                track.setId(String.valueOf(id));
                track.setTitle(cur.getString(cur.getColumnIndex(MediaStore.MediaColumns.TITLE)));
                track.setArtistId(String.valueOf(cur.getInt(cur.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST_ID))));
                track.setArtist(cur.getString(cur.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST)));
                track.setAlbumId(String.valueOf(cur.getInt(cur.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID))));
                track.setAlbum(cur.getString(cur.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM)));
                track.setGenreId(genre[0]);
                track.setGenre(genre[1]);
                track.setDuration(cur.getInt(cur.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)));
                track.setType(Track.TrackType.AUDIO_TRACK);
                track.setUri(mediaUri);

                mListener.onOneCompletion(track, false);
            } else {
                mMediaIds.add(id);
                mVideoIds.add(id);

                track = new Track();
                track.setId(String.valueOf(id));
                track.setTitle(cur.getString(cur.getColumnIndex(MediaStore.MediaColumns.TITLE)));
                track.setArtist(cur.getString(cur.getColumnIndex(MediaStore.Video.VideoColumns.ARTIST)));
                track.setAlbum(cur.getString(cur.getColumnIndex(MediaStore.Video.VideoColumns.ALBUM)));
                track.setDuration(cur.getInt(cur.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)));
                track.setType(Track.TrackType.VIDEO_TRACK);
                track.setUri(mediaUri);

                mListener.onOneCompletion(track, true);
            }
        } while(cur.moveToNext());

        cur.close();

//        return ;
    }

    private String[] getGenre(Uri uri, String[] projection) {
        Cursor cur;
        String genreName;
        int genreId;
        String[] rtn = {"", ""};
        cur = mContext.getContentResolver().query(uri, projection, null, null, null);

        try {
            int genre_column_index = cur.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME);
            int genreIdColumnIndex = cur.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID);
            if (cur.moveToFirst()) {
                genreName = cur.getString(genre_column_index);
                genreId = cur.getInt(genreIdColumnIndex);
                rtn[0] = String.valueOf(genreId);
                rtn[1] = genreName;
            }
            return rtn;
        } catch(IllegalArgumentException e) {
            Log.d(TAG, "getGenre", e);
            return rtn;
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }
}
