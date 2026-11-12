package com.jht.chimera.lib.common.mediadevice;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public class MediaItem implements Parcelable {
    public String id;
    public String title;
    public String album;
    public String artist;
    public String genre;
    public int duration;
    public boolean isTrack;
    public boolean isVideo;
    public byte[] currentFolderBytes = new byte[8];

    public MediaItem(byte[] currentFolderBytes) {
        this.currentFolderBytes = currentFolderBytes;
    }

    public MediaItem() {

    }

    public MediaItem(Parcel in) {
        id = in.readString();
        title = in.readString();
        album = in.readString();
        artist = in.readString();
        genre = in.readString();
        duration = in.readInt();
        isTrack = in.readByte() == 1;
        isVideo = in.readByte() == 1;
        if(in.dataAvail() > 0)
            in.readByteArray(currentFolderBytes);
    }

    public static final Creator<MediaItem> CREATOR = new Creator<MediaItem>() {
        @Override
        public MediaItem createFromParcel(Parcel in) {
            return new MediaItem(in);
        }

        @Override
        public MediaItem[] newArray(int size) {
            return new MediaItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(album);
        dest.writeString(artist);
        dest.writeString(genre);
        dest.writeInt(duration);
        dest.writeByte((byte) (isTrack ? 1 : 0));
        dest.writeByte((byte) (isVideo ? 1 : 0));
        if(currentFolderBytes != null)
            dest.writeByteArray(currentFolderBytes);
    }

    @NonNull
    @Override
    public String toString() {
        return id + " - " + title + " / " + album;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(this == obj) {
            return true;
        }

        if(obj == null) {
            return false;
        }

        if(!(obj instanceof MediaItem)) {
            return false;
        }

        MediaItem other = (MediaItem) obj;

        return Objects.equals(this.title, other.title) && Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.title, this.id);
    }
}
