package com.jht.chimera.service.usbmedia.mtpums.lib.media;

public class Track {
    public enum TrackType {
        AUDIO_TRACK, VIDEO_TRACK
    }

    private String id;
    private String title;
    private String artistId;
    private String artist;
    private String albumId;
    private String album;
    private String genreId;
    private String genre;
    private int duration;
    private TrackType type;
    private String uri;

    public Track() {}

    public Track(String id, String title, String artist, String album, String genre, int duration, TrackType type) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
        this.duration = duration;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtistId() {
        return artistId;
    }

    public void setArtistId(String artistId) {
        this.artistId = artistId;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getGenreId() {
        return genreId;
    }

    public void setGenreId(String genreId) {
        this.genreId = genreId;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public TrackType getType() {
        return type;
    }

    public void setType(TrackType type) {
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }

        if(obj == null) {
            return false;
        }

        if(!(obj instanceof Track)) {
            return false;
        }

        Track other = (Track) obj;
        if(!id.equals(other.getId())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = result * 37 + id.hashCode();

        return result;
    }
}
