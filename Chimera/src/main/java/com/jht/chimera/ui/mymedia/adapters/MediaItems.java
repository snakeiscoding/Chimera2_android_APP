package com.jht.chimera.ui.mymedia.adapters;

import android.os.Parcel;
import android.os.Parcelable;

import com.jht.chimera.lib.common.mediadevice.MediaItem;

import java.util.ArrayList;
import java.util.List;

public class MediaItems implements Parcelable {

    private List<MediaItem> mediaItems = new ArrayList<>();

    public MediaItems() {

    }

    @SuppressWarnings("unchecked")
    public MediaItems(Parcel in) {
        mediaItems = in.readArrayList(MediaItem.class.getClassLoader());
    }

    public static final Creator<MediaItems> CREATOR = new Creator<MediaItems>() {
        @Override
        public MediaItems createFromParcel(Parcel in) {
            return new MediaItems(in);
        }

        @Override
        public MediaItems[] newArray(int size) {
            return new MediaItems[size];
        }
    };

    public void addMediaItem(MediaItem item) {
        if(!mediaItems.contains(item))
            mediaItems.add(item);

    }

    public List<MediaItem> getMediaItems() {
        return mediaItems;
    }

    public void clear() {
        mediaItems.clear();
    }



    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(mediaItems);
    }

}
