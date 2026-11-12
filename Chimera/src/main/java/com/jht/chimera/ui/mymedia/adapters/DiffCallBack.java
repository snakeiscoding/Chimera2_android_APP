package com.jht.chimera.ui.mymedia.adapters;

import androidx.recyclerview.widget.DiffUtil;

import com.jht.chimera.lib.common.mediadevice.MediaItem;

import java.util.List;

public class DiffCallBack extends DiffUtil.Callback {
    private List<MediaItem> mOldItems, mNewItems;

    public DiffCallBack(List<MediaItem> oldItems, List<MediaItem> newItems) {
        mOldItems = oldItems;
        mNewItems = newItems;
    }
    @Override
    public int getOldListSize() {
        return mOldItems != null ? mOldItems.size() : 0;
    }

    @Override
    public int getNewListSize() {
        return mNewItems != null ? mNewItems.size() : 0;
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        boolean result = mOldItems.get(oldItemPosition).id.equals(mNewItems.get(newItemPosition).id);
        return result;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        MediaItem oldItem = mOldItems.get(oldItemPosition);
        MediaItem newItem = mNewItems.get(newItemPosition);
        return oldItem.equals(newItem);
    }

//    @Nullable
//    @Override
//    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
//        MediaItem oldItem = mOldItems.get(oldItemPosition);
//        MediaItem newItem = mNewItems.get(newItemPosition);
//        Bundle payload = new Bundle();
//
//        return null;
//    }
}
