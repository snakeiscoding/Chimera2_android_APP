package com.jht.chimera.service.usbmedia.ios.lib.media;

import androidx.annotation.NonNull;

public class Track {

    private String mId;
    private String mTitle;
    private String mAlbumId;
    private String mAlbumTitle;
    private String mArtistId;
    private String mArtistTitle;
    private String mGenreId;
    private String mGenreTitle;
    private String mComposerId;
    private String mComposer;
    private boolean mIsCompilation;
    private int mDuration;
    private int mType;
    private boolean mIsPlaylist;

    public interface OnFoundListener {
        void onFound(Track track);
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        this.mId = id;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public String getAlbumId() {
        return mAlbumId;
    }

    public void setAlbumId(String albumId) {
        this.mAlbumId = albumId;
    }

    public String getAlbumTitle() {
        return mAlbumTitle;
    }

    public void setAlbumTitle(String albumTitle) {
        this.mAlbumTitle = albumTitle;
    }

    public String getArtistId() {
        return mArtistId;
    }

    public void setArtistId(String artistId) {
        this.mArtistId = artistId;
    }

    public String getArtistTitle() {
        return mArtistTitle;
    }

    public void setArtistTitle(String artistTitle) {
        this.mArtistTitle = artistTitle;
    }

    public String getGenreId() {
        return mGenreId;
    }

    public void setGenreId(String genreId) {
        this.mGenreId = genreId;
    }

    public String getGenreTitle() {
        return mGenreTitle;
    }

    public void setGenreTitle(String genreTitle) {
        this.mGenreTitle = genreTitle;
    }

    public String getComposerId() {
        return mComposerId;
    }

    public void setComposerId(String composerId) {
        this.mComposerId = composerId;
    }

    public String getComposer() {
        return mComposer;
    }

    public void setComposer(String composer) {
        this.mComposer = composer;
    }

    public boolean isCompilation() {
        return mIsCompilation;
    }

    public void setIsCompilation(boolean isCompilation) {
        this.mIsCompilation = isCompilation;
    }

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int duration) {
        this.mDuration = duration;
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        this.mType = type;
    }

    public boolean isPlaylist() {
        return mIsPlaylist;
    }

    public void setPlaylist(boolean isPlaylist) {
        this.mIsPlaylist = isPlaylist;
    }

    @NonNull
    @Override
    public String toString() {
        return mId + "_" + mTitle;
    }
}
