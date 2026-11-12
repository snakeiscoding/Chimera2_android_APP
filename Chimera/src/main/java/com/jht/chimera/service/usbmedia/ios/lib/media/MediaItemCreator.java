package com.jht.chimera.service.usbmedia.ios.lib.media;

import android.content.Context;
import android.util.Log;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.UsbMediaDeviceKeys;
import com.jht.chimera.lib.common.mediadevice.MediaItem;
import com.jht.chimera.service.usbmedia.ios.lib.iap2.ControlSession;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class MediaItemCreator implements Runnable {
    private static final String TAG = "MediaItemCreator";

    private Context mContext;
    private boolean mIsRunning;
    private BlockingQueue<Track> mTrackQueue;
    private BlockingQueue<MediaPlaylist> mPlaylistQueue;
    private Thread mThread;
    private Node mCurrentNode;
    private List<String> mPlaylistIds;
    private MediaItem mSelectedMediaItem;
    private List<String> mArtistIds;
    private List<String> mAlbumIds;
    private List<String> mGenreIds;
    private List<String> mComposerIds;
    private onReadyListener mListener;
    private List<Track> mTracks;
    private List<MediaPlaylist> mPlaylists;

    public interface onReadyListener {
        void onReady(MediaItem item);
    }

    public MediaItemCreator(Context context, BlockingQueue<Track> trackQueue, BlockingQueue<MediaPlaylist> playlistQueue) {
        mContext = context;
        mTrackQueue = trackQueue;
        mPlaylistQueue = playlistQueue;
        mPlaylistIds = new ArrayList<>();
        mArtistIds = new ArrayList<>();
        mAlbumIds = new ArrayList<>();
        mGenreIds = new ArrayList<>();
        mComposerIds = new ArrayList<>();
        mTracks = new ArrayList<>();
        mPlaylists = new ArrayList<>();

    }

    public void setSelectedMediaItem(MediaItem selectedMediaItem) {
        mSelectedMediaItem = selectedMediaItem;
    }

    public void setOnReadyListener(onReadyListener l) {
        mListener = l;
    }

    public void setCurrentNode(Node currentNode) {
        mCurrentNode = currentNode;
    }

    public void start() {
        if(mIsRunning) {
            return;
        }

        mIsRunning = true;
        mThread = new Thread(this);
        mThread.start();

    }

    public void stop() {
        mIsRunning = false;

        if(mThread != null) {
            mThread.interrupt();
        }
    }

    @Override
    public synchronized void run() {
        try {
            DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_SCANNING_BOOL, true);
            int count = 0;

            Node.Category currentCategory = mCurrentNode.getCategory();
            Node.Category parentCategory = null;
            if (mCurrentNode.getParent() != null) {
                parentCategory = mCurrentNode.getParent().getCategory();
            }
//            Log.d(TAG, "current category: " + currentCategory + ", parent category: " + parentCategory);

            if (currentCategory == Node.Category.TOP_LEVEL) {
                for (Node node : mCurrentNode) {
                    count++;
                    MediaItem item = createMediaItem(node.getId(), node.getName(), "", "", "", -1, false);
                    DataMatrix.putParcelable(UsbMediaDeviceKeys.EVENT_TRACK_FOUND_PARCELABLE, item);
                    Thread.sleep(50);
                }
                mIsRunning = false;
            } else if (currentCategory == Node.Category.LIBRARY) {
                for (Node node : mCurrentNode) {
                    count++;
                    MediaItem item = createMediaItem(node.getId(), node.getName(), "", "", "", -1, false);
                    DataMatrix.putParcelable(UsbMediaDeviceKeys.EVENT_TRACK_FOUND_PARCELABLE, item);
                    Thread.sleep(50);
                }
                mIsRunning = false;
            } else if (currentCategory == Node.Category.PLAYLIST || currentCategory == Node.Category.RADIO) {
                for (MediaPlaylist playlist : mPlaylists) {
                    if(playlist == null) {
                        continue;
                    }

                    if(playlist.getName().equals(mContext.getString(R.string.exit_tc))) {
                        mIsRunning = false;
                        break;
                    }

                    MediaItem item = null;
                    if(currentCategory == Node.Category.PLAYLIST) {
                        item = getPlaylist(playlist, false);
                    } else if(currentCategory == Node.Category.RADIO) {
                        item = getPlaylist(playlist, true);
                    }

                    if (item != null && mIsRunning) {
                        count++;
                        DataMatrix.putParcelable(UsbMediaDeviceKeys.EVENT_TRACK_FOUND_PARCELABLE, item);
                        mListener.onReady(item);
                        Thread.sleep(50);
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

                    MediaItem item = getAudioItem(track);
                    if (item != null && mIsRunning) {
                        count++;
                        DataMatrix.putParcelable(UsbMediaDeviceKeys.EVENT_TRACK_FOUND_PARCELABLE, item);
                        mListener.onReady(item);
                        Thread.sleep(50);
                    }
                }
            }

            while (mIsRunning) {
                if (currentCategory == Node.Category.PLAYLIST || currentCategory == Node.Category.RADIO) {
                        MediaPlaylist playlist = mPlaylistQueue.take();

                        mPlaylists.add(playlist);
                        if (playlist == null) {
                            continue;
                        }

                        if (playlist.getName().equals(mContext.getString(R.string.exit_tc))) {
                            mIsRunning = false;
                        } else {

                            MediaItem item = null;
                            if (currentCategory == Node.Category.PLAYLIST) {
                                item = getPlaylist(playlist, false);
                            } else if (currentCategory == Node.Category.RADIO) {
                                item = getPlaylist(playlist, true);
                            }

                            if (item != null && mIsRunning) {
                                count++;
                                DataMatrix.putParcelable(UsbMediaDeviceKeys.EVENT_TRACK_FOUND_PARCELABLE, item);
                                mListener.onReady(item);
                                Thread.sleep(50);
                            }
                        }
                } else {
                        Track track = mTrackQueue.take();
                        mTracks.add(track);
                        if (track.getTitle().equals(mContext.getString(R.string.exit_tc))) {
                            mIsRunning = false;
                        } else {
                            MediaItem item = getAudioItem(track);
                            if (item != null && mIsRunning) {
                                count++;
                                DataMatrix.putParcelable(UsbMediaDeviceKeys.EVENT_TRACK_FOUND_PARCELABLE, item);
                                mListener.onReady(item);
                                Thread.sleep(50);
                            }
                        }
                }
            }
            Log.d(TAG, "run: total media items: " + count);
        } catch (InterruptedException e) {
            MediaItem item = new MediaItem();
            DataMatrix.putParcelable(UsbMediaDeviceKeys.EVENT_TRACK_FOUND_PARCELABLE, item);
        } finally {
            DataMatrix.putBoolean(UsbMediaDeviceKeys.DATA_IS_SCANNING_BOOL, false);
            mPlaylistIds.clear();
            mArtistIds.clear();
            mAlbumIds.clear();
            mGenreIds.clear();
            mComposerIds.clear();
        }

    }

    private MediaItem getAudioItem(Track track) {
        MediaItem item = null;
        Node.Category currentCategory = mCurrentNode.getCategory();
        Node.Category parentCategory = null;
        if (mCurrentNode.getParent() != null) {
            parentCategory = mCurrentNode.getParent().getCategory();
        }

        switch(currentCategory) {
            case ALBUM:
                if(parentCategory == Node.Category.LIBRARY) {
                    item = getAlbum(track);
                } else if(parentCategory == Node.Category.ARTIST) {
                    item = getArtistAlbum(track);
                } else if(parentCategory == Node.Category.COMPOSER) {
                    item = getComposerAlbum(track);
                }
                break;
            case ARTIST:
                if(parentCategory == Node.Category.LIBRARY) {
                    item = getArtist(track);
                } else if(parentCategory == Node.Category.GENRE) {
                    item = getGenreArtist(track);
                }
                break;
            case AUDIO_TRACK:
                if(parentCategory == Node.Category.ALBUM) {
                    item = getAlbumSong(track);
                } else if(parentCategory == Node.Category.PLAYLIST) {
                    item = getPlaylistSong(track);
                } else if(parentCategory == Node.Category.PODCAST) {
                    item = getPodcastSong(track);
                } else if(parentCategory == Node.Category.COMPILATION) {
                    item = getCompilationSong(track);
                } else {
                    item = getSong(track);
                }
                break;
            case GENRE:
                item = getGenre(track);
                break;
            case COMPOSER:
                if(parentCategory == Node.Category.LIBRARY) {
                    item = getComposer(track);
                }
                break;
            case COMPILATION:
                item = getCompilationAlbum(track);
                break;
            case PLAYLIST:
                break;
            case PODCAST:
                if(parentCategory == Node.Category.TOP_LEVEL) {
                    item = getPodcast(track);
                } else if(parentCategory == Node.Category.PODCAST) {
                    item = getPodcastSong(track);
                }
                break;
        }

        return item;
    }

    private MediaItem getArtist(Track track) {
        MediaItem item = null;
        if(track.getType() == ControlSession.MediaType.MUSIC.ordinal() && track.getArtistTitle() != null && !mArtistIds.contains(track.getArtistId()) && !track.isCompilation()) {
            mArtistIds.add(track.getArtistId());
            item = createMediaItem(track.getArtistId(), track.getArtistTitle(), "", "", "", -1, false);
        }
        return item;
    }

    private MediaItem getGenreArtist(Track track) {
        MediaItem item = null;
        if(track.getType() != ControlSession.MediaType.MUSIC.ordinal()) {
            return item;
        }

        if(track.getGenreId().equals(mSelectedMediaItem.id) && !mArtistIds.contains(track.getArtistId())) {
            mArtistIds.add(track.getArtistId());
            item = createMediaItem(track.getArtistId(), track.getArtistTitle(), "", "", "", -1, false);
        }
        return item;
    }

    private MediaItem getArtistAlbum(Track track) {
        MediaItem item = null;
        if(track.getType() != ControlSession.MediaType.MUSIC.ordinal()) {
            return item;
        }

        if(track.getArtistId().equals(mSelectedMediaItem.id) && !mAlbumIds.contains(track.getAlbumId())) {
            mAlbumIds.add(track.getAlbumId());
            item = createMediaItem(track.getAlbumId(), track.getAlbumTitle(), "", "", "", -1, false);
        }

        return item;
    }

    private MediaItem getAlbum(Track track) {
        MediaItem item = null;
        if(track.getType() != ControlSession.MediaType.MUSIC.ordinal()) {
            return item;
        }

        if(!mAlbumIds.contains(track.getAlbumId())) {
            mAlbumIds.add(track.getAlbumId());
            item = createMediaItem(track.getAlbumId(), track.getAlbumTitle(), "", "", "", -1, false);
        }

        return item;
    }

    private MediaItem getSong(Track track) {
        MediaItem item = null;
        if(track.getType() != ControlSession.MediaType.MUSIC.ordinal()) {
            return item;
        }
        item = createMediaItem(track.getId(), track.getTitle(), track.getAlbumTitle(), track.getArtistTitle(), track.getGenreTitle(), track.getDuration(), true);

        return item;
    }

    private MediaItem getGenre(Track track) {
        MediaItem item = null;
        if(track.getType() != ControlSession.MediaType.MUSIC.ordinal()) {
            return item;
        }

        if(track.getGenreTitle() == null) {
            return item;
        }

        if(!mGenreIds.contains(track.getGenreId())) {
            mGenreIds.add(track.getGenreId());
            if(track.getGenreTitle() == null) {
                Log.d(TAG, "getGenre: genre id: " + track.getGenreId() + ", item title: " + track.getTitle());
            }
            item = createMediaItem(track.getGenreId(), track.getGenreTitle(), "", "", "", -1, false);
        }
        return item;
    }

    private MediaItem getComposer(Track track) {
        MediaItem item = null;
        if(track.getType() != ControlSession.MediaType.MUSIC.ordinal()) {
            return item;
        }

        if(track.getComposer() == null) {
            return item;
        }

        if(!mComposerIds.contains(track.getComposerId())) {
            mComposerIds.add(track.getComposerId());
            item = createMediaItem(track.getComposerId(), track.getComposer(), "", "", "", -1, false);
        }
        return item;
    }

    private MediaItem getComposerAlbum(Track track) {
        MediaItem item = null;
        if(track.getType() != ControlSession.MediaType.MUSIC.ordinal()) {
            return item;
        }

        if(track.getComposerId().equals(mSelectedMediaItem.id) && !mAlbumIds.contains(track.getAlbumId())) {
            mAlbumIds.add(track.getAlbumId());
            item = createMediaItem(track.getAlbumId(), track.getAlbumTitle(), "", "", "", -1, false);
        }
        return item;
    }

    private MediaItem getCompilationAlbum(Track track) {
        MediaItem item = null;
        if(track.getType() != ControlSession.MediaType.MUSIC.ordinal()) {
            return item;
        }

        if(track.isCompilation() && !mAlbumIds.contains(track.getAlbumId())) {
            mAlbumIds.add(track.getAlbumId());
            item = createMediaItem(track.getAlbumId(), track.getAlbumTitle(), "", "", "", -1, false);
        }
        return item;
    }

    private MediaItem getPlaylist(MediaPlaylist playlist, boolean isRadio) {
        MediaItem item = null;
        if(isRadio) {
            if(!playlist.isAppleMusicRadio()) {
                return item;
            }
        } else {
            if(playlist.isAppleMusicRadio()) {
                return item;
            }
        }

        if(!mPlaylistIds.contains(playlist.getId())) {
            mPlaylistIds.add(playlist.getId());
            item = createMediaItem(playlist.getId(), playlist.getName(), "", "", "", -1, false);
        }
        return item;
    }

    private MediaItem getPlaylistSong(Track track) {
        MediaItem item = null;
        for(MediaPlaylist _playlist : mPlaylists) {
            if(_playlist.getId().equals(mSelectedMediaItem.id)) {
                for(String itemId : _playlist.getMediaItemIds()) {
                    if(itemId.equals(track.getId())) {
                        item = createMediaItem(track.getId(), track.getTitle(), track.getAlbumTitle(), track.getArtistTitle(), track.getGenreTitle(), track.getDuration(), true);
                        break;
                    }
                }
                break;
            }
        }
        return item;
    }

    private MediaItem getAlbumSong(Track track) {
        MediaItem item = null;
        if(track.getType() != ControlSession.MediaType.MUSIC.ordinal()) {
            return item;
        }

        if(track.getAlbumId().equals(mSelectedMediaItem.id)) {
            item = createMediaItem(track.getId(), track.getTitle(), track.getAlbumTitle(), track.getArtistTitle(), track.getGenreTitle(), track.getDuration(), true);
        }
        return item;
    }

    private MediaItem getPodcast(Track track) {
        MediaItem item = null;
        if(track.getType() != ControlSession.MediaType.PODCAST.ordinal()) {
            return item;
        }

        if(!mAlbumIds.contains(track.getAlbumId())) {
            mAlbumIds.add(track.getAlbumId());
            item = createMediaItem(track.getAlbumId(), track.getAlbumTitle(), "", "", "", -1, false);
        }
        return item;
    }

    private MediaItem getPodcastSong(Track track) {
        MediaItem item = null;
        if(track.getType() != ControlSession.MediaType.PODCAST.ordinal()) {
            return item;
        }

        if(track.getAlbumId().equals(mSelectedMediaItem.id)) {
            item = createMediaItem(track.getId(), track.getTitle(), track.getAlbumTitle(), track.getArtistTitle(), track.getGenreTitle(), track.getDuration(), true);
        }
        return item;
    }

    private MediaItem getCompilationSong(Track track) {
        MediaItem item = null;
        if(track.getType() != ControlSession.MediaType.MUSIC.ordinal()) {
            return item;
        }

        if(track.isCompilation() && track.getAlbumId().equals(mSelectedMediaItem.id)) {
            item = createMediaItem(track.getId(), track.getTitle(), track.getAlbumTitle(), track.getArtistTitle(), track.getGenreTitle(), track.getDuration(), true);
        }

        return item;
    }

    private MediaItem createMediaItem(String id, String title, String album, String artist, String genre, int duration, boolean isTrack) {
        MediaItem item = new MediaItem();
        item.id = id;
        item.title = title;
        item.album = album;
        item.artist = artist;
        item.genre = genre;
        item.duration = duration;
        item.isTrack = isTrack;
        return item;
    }
}
