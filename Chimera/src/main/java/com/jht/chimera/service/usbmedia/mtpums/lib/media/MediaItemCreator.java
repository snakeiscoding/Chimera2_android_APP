package com.jht.chimera.service.usbmedia.mtpums.lib.media;

import android.content.Context;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.UsbMediaDeviceKeys;
import com.jht.chimera.lib.common.mediadevice.MediaItem;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class MediaItemCreator implements Runnable {
    private static final String TAG = "MediaItemCreator";

    private Context mContext;
    private BlockingQueue<Track> mTrackQueue;
    private List<String> mArtists = new ArrayList<>();
    private List<String> mAlbums = new ArrayList<>();
    private List<String> mGenres = new ArrayList<>();
    private List<Track> mTracks = new ArrayList<>();
    private boolean mIsRunning;
    private Thread mThread;
    private boolean mIsVideo;
    private MediaItem mItem;
    private OnReadyListener mListener;
    private Node mCurrentNode;

    public interface OnReadyListener {
        void onReady(MediaItem item, Track track);
    }

    public MediaItemCreator(Context context, BlockingQueue<Track> q, boolean isVideo) {
        mContext = context;
        mTrackQueue = q;
        mIsVideo = isVideo;
    }

    public void setOnCreateMediaItemListener(OnReadyListener l) {
        mListener = l;
    }

    public void setCurrentNode(Node node) {
        mCurrentNode = node;
    }

    public boolean isVideo() {
        return mIsVideo;
    }

    public void setVideo(boolean video) {
        this.mIsVideo = video;
    }

    public void setItem(MediaItem item) {
        this.mItem = item;
    }

    public void start() {
        if(mIsRunning) {
            return;
        }

        mIsRunning = true;
        mThread = new Thread(this);
        mThread.start();
    }

    @Override
    public synchronized void run() {
        try {
            int audioCount = 0;
            int videoCount = 0;

            DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_SCANNING_BOOL, true);
            if(!mIsVideo) {
//                Log.d(TAG, "[MyMedia]run: current category: " + mCurrentNode.getCategory());
                if(mCurrentNode.getCategory() == Node.Category.TOP_LEVEL) {
                    for(Node node : mCurrentNode) {
                        MediaItem item = createMediaItem(node.getId(), node.getName(), "", "", "", -1, false, false);
                        DataMatrix.putParcelable(UsbMediaDeviceKeys.EVENT_TRACK_FOUND_PARCELABLE, item);
                        mListener.onReady(item, null);
                    }
                    mIsRunning = false;
                } else {
                    for (Track track : mTracks) {
                        if (!mIsRunning) {
                            break;
                        }

                        if (track.getTitle().equals(mContext.getString(R.string.exit_tc))) {
                            mIsRunning = false;
                            break;
                        }

                        if (track.getType() != Track.TrackType.AUDIO_TRACK) {
                            continue;
                        }

                        MediaItem item = getAudioItem(track);
                        if (item != null && mIsRunning) {
                            audioCount++;
                            DataMatrix.putParcelable(UsbMediaDeviceKeys.EVENT_TRACK_FOUND_PARCELABLE, item);
                            mListener.onReady(item, track);
                            Thread.sleep(50);
                        }
                    }
                }
            } else {
                for (Track track : mTracks) {
                    if (!mIsRunning) {
                        break;
                    }

                    if (track.getTitle().equals(mContext.getString(R.string.exit_tc))) {
                        mIsRunning = false;
                        break;
                    }

                    if (track.getType() != Track.TrackType.VIDEO_TRACK) {
                        continue;
                    }

                    MediaItem item = getVideoItem(track);
                    if (item != null && mIsRunning) {
                        videoCount++;
                        DataMatrix.putParcelable(UsbMediaDeviceKeys.EVENT_TRACK_FOUND_PARCELABLE, item);
                        mListener.onReady(item, track);
                        Thread.sleep(50);
                    }
                }
            }

            while(mIsRunning) {
                MediaItem item;
                Track track = mTrackQueue.take();
                mTracks.add(track);

                if(track.getTitle().equals(mContext.getString(R.string.exit_tc))) {
                    mIsRunning = false;
                    break;
                }

                if(!mIsVideo) {
                    if(track.getType() != Track.TrackType.AUDIO_TRACK) {
                        continue;
                    }
                    item = getAudioItem(track);
                } else {
                    if(track.getType() != Track.TrackType.VIDEO_TRACK) {
                        continue;
                    }
                    item = getVideoItem(track);
                }

                if(item != null && mIsRunning) {
                    if (!mIsVideo) {
                        audioCount++;
                    } else {
                        videoCount++;
                    }
                    DataMatrix.putParcelable(UsbMediaDeviceKeys.EVENT_TRACK_FOUND_PARCELABLE, item);
                    mListener.onReady(item, track);
                    Thread.sleep(50);
                }
            }
        } catch(InterruptedException e) {
            MediaItem item = new MediaItem();
            item.isVideo = mIsVideo;
            DataMatrix.putParcelable(UsbMediaDeviceKeys.EVENT_TRACK_FOUND_PARCELABLE, item);
        } finally {
            DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_SCANNING_BOOL, false);
            mArtists.clear();
            mAlbums.clear();
            mGenres.clear();
        }
    }

    private MediaItem getAudioItem(Track track) {
        MediaItem item = null;
        Node.Category current = mCurrentNode.getCategory();
        Node.Category parent = mCurrentNode.getParent().getCategory();
        switch(current) {
            case ARTIST:
                if(parent == Node.Category.TOP_LEVEL) {
                    item = getArtist(track);
                } else if(parent == Node.Category.GENRE) {
                    item = getGenreArtist(track);
                }
                break;
            case ALBUM:
                if(parent == Node.Category.TOP_LEVEL) {
                    item = getAlbum(track);
                } else if(parent == Node.Category.ARTIST) {
                    item = getArtistAlbum(track);
                }
                break;
            case GENRE:
                item = getGenre(track);
                break;
            case AUDIO_TRACK:
                if(parent == Node.Category.ALBUM) {
                    item = getAlbumSong(track);
                } else {
                    item = getSong(track);
                }
                break;
        }
        return item;
    }

    private MediaItem getArtist(Track track) {
        MediaItem item = null;
        if(!mArtists.contains(track.getArtistId())) {
            mArtists.add(track.getArtistId());
            item = createMediaItem(track.getArtistId(), track.getArtist(), "", "", "", -1, false, false);
        }
        return item;
    }

    private MediaItem getArtistAlbum(Track track) {
        MediaItem item = null;
        if(track.getArtistId().equals(mItem.id) && !mAlbums.contains(track.getAlbumId())) {
            mAlbums.add(track.getAlbumId());
            item = createMediaItem(track.getAlbumId(), track.getAlbum(), "", "", "", -1, false, false);
        }
        return item;
    }

    private MediaItem getAlbum(Track track) {
        MediaItem item = null;
        if(!mAlbums.contains(track.getAlbumId())) {
            mAlbums.add(track.getAlbumId());
            item = createMediaItem(track.getAlbumId(), track.getAlbum(), "", "", "", -1, false, false);
        }

        return item;
    }

    private MediaItem getAlbumSong(Track track) {
        MediaItem item = null;
        if(track.getAlbumId().equals(mItem.id)) {
            item = createMediaItem(track.getId(), track.getTitle(), track.getAlbum(), track.getArtist(), track.getGenre(), track.getDuration(), true, false);
        }
        return item;
    }

    private MediaItem getGenre(Track track) {
        MediaItem item = null;
        if(!mGenres.contains(track.getGenreId())) {
            mGenres.add(track.getGenreId());
            item = createMediaItem(track.getGenreId(), track.getGenre(), "", "", "", -1, false, false);
        }
        return item;
    }

    private MediaItem getGenreArtist(Track track) {
        MediaItem item = null;
        if(track.getGenreId().equals(mItem.id) && !mArtists.contains(track.getArtistId())) {
            mArtists.add(track.getArtistId());
            item = createMediaItem(track.getArtistId(), track.getArtist(), "", "", "", -1, false, false);
        }
        return item;
    }

    private MediaItem getSong(Track track) {
        MediaItem item = createMediaItem(track.getId(), track.getTitle(), track.getAlbum(), track.getArtist(), track.getGenre(), track.getDuration(), true, false);
        return item;
    }

    private MediaItem getVideoItem(Track track) {
        MediaItem item = new MediaItem();
        item.id = track.getId();
        item.title = track.getTitle();
        item.album = track.getAlbum();
        item.artist = track.getArtist();
        item.genre = track.getGenre();
        item.duration = track.getDuration();
        item.isTrack = true;
        item.isVideo = true;
        return item;
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    private MediaItem createMediaItem(String id, String title, String album, String artist, String genre, int duration, boolean isTrack, boolean isVideo) {
        MediaItem item = new MediaItem();
        item.id = id;
        item.title = title;
        item.album = album;
        item.artist = artist;
        item.genre = genre;
        item.duration = duration;
        item.isTrack = isTrack;
        item.isVideo = isVideo;
        return item;
    }

    public void stop() {
        mIsRunning = false;
        if(mThread != null) {
            mThread.interrupt();
        }
    }
}
