package com.jht.chimera.ui.mymedia.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.mediadevice.MediaItem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public class MediaItemListAdapter extends RecyclerView.Adapter<MediaItemListAdapter.ViewHolder> {
    private static final String TAG = "MediaItemListAdapter";

    private Context mContext;
    private List<MediaItem> mItems;
    private Queue<List<MediaItem>> mPendingUpdates;
    private Drawable mLeftIcon;
    private OnItemClickListener mOnItemClickListener;
    private int mSelectedIndex;
    private int mOldSelectedIndex = -1;
    private MediaItem mCurrentItem;


    private AsyncListDiffer<MediaItem> mDiffer;
    private RecyclerView mRecyclerView;

    private DiffUtil.ItemCallback<MediaItem> diffCallback
            = new DiffUtil.ItemCallback<MediaItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull MediaItem oldItem, @NonNull MediaItem newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull MediaItem oldItem, @NonNull MediaItem newItem) {
            return oldItem.equals(newItem);
        }
    };


    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public MediaItemListAdapter(Context context) {
        mContext = context;
        mItems = new ArrayList<>();
        mPendingUpdates = new ArrayDeque<>();
        mSelectedIndex = -1;
        mLeftIcon = AppCompatResources.getDrawable(context, R.drawable.svg_ic_browser_item_play);
        mDiffer = new AsyncListDiffer<>(this, diffCallback);
    }

    public void removeAt(int position) {
        mItems.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, mItems.size());
    }

    public void add(List<MediaItem> items) {
        int startPosition = mItems.size();
        boolean update = false;
        int count = 0;
        for(MediaItem item : items) {
            if(!mItems.contains(item)) {
                update = true;
                count++;
                mItems.add(item);
            }
        }

        if(update) {
            notifyItemRangeInserted(startPosition, count);
            Log.d(TAG, "add: total count: " + mItems.size());
        }

    }

    public void add(MediaItem item) {

        if (!mItems.contains(item)) {
            Log.d(TAG, "add: mContext: " + mContext);
            Log.e(TAG, "adding item: " + item.title + ", folder: " + Arrays.toString(item.currentFolderBytes));
            if(item.title == null || item.title.isEmpty() || bytesContainZero(item.title.getBytes())){
                item.title = mContext.getResources().getString(R.string.mymedia_title_substitution_uc);
            }
            mItems.add(item);
            notifyItemInserted(mItems.size() - 1);

        }
    }

    private static boolean bytesContainZero(byte[] bytes){
        for(int i = 0; i < bytes.length; i++){
            if(bytes[i] == 0){
                return true;
            }
        }

        return false;
    }

    public void addList(List<MediaItem> list) {
        mItems.addAll(list);
        notifyDataSetChanged();
    }

    public void update(List<MediaItem> items) {
        mPendingUpdates.add(items);
        if(mPendingUpdates.size() > 1) {
            return;
        }
        updateInternal(items);
    }

    private void updateInternal(final List<MediaItem> items) {
        final List<MediaItem> oldItems = new ArrayList<>(this.mItems);
        final Handler handler = new Handler();
        new Thread(() -> {
            final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCallBack(oldItems, items), false);
            handler.post(() -> applyDiffResult(items, diffResult));
        }).start();
    }

    private void applyDiffResult(List<MediaItem> items, DiffUtil.DiffResult diffResult) {
        mPendingUpdates.remove();
        mItems.clear();
        mItems.addAll(items);
        diffResult.dispatchUpdatesTo(this);

        if(mPendingUpdates.size() > 0) {
            updateInternal(mPendingUpdates.peek());
        }/* else {
            Log.d(TAG, "[KEVIN]applyDiffResult: add " + mItems.size() + " media item(s).");
        }*/
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        mOnItemClickListener = l;
    }

    public void setSelectedIndex(int selectedIndex) {
        mOldSelectedIndex = mSelectedIndex;
        mSelectedIndex = selectedIndex;
        notifyItemChanged(mOldSelectedIndex);
        notifyItemChanged(selectedIndex);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//        mContext = parent.getContext();
        View mediaItemView = LayoutInflater.from(mContext).inflate(R.layout.item_media, parent, false);

        ViewHolder vh = new ViewHolder(mediaItemView);
        mediaItemView.setOnClickListener(view -> {
            if(mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(view, (int) view.getTag());
            }
        });

        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        MediaItem item = mItems.get(position);
//        MediaItem item = mDiffer.getCurrentList().get(position);
        if(item.isTrack && (mCurrentItem != null && mCurrentItem.equals(item))) {
            mSelectedIndex = position;
        }

        holder.ivLeftIcon.setVisibility(View.GONE);
        holder.ivLeftIcon.setImageDrawable(null);
        if(mSelectedIndex == position) {
            holder.ivLeftIcon.setVisibility(View.VISIBLE);
            holder.ivLeftIcon.setImageDrawable(mLeftIcon);
        }

        holder.txtItemName.setText(item.title);


        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(mContext.getResources().getColor(R.color.browser_item_background_black));
        } else {
            holder.itemView.setBackgroundColor(mContext.getResources().getColor(R.color.browser_item_background_white));
        }

        holder.itemView.setTag(position);
    }

    public List<MediaItem> getMediaItems() {
        return mItems;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
//        return mDiffer.getCurrentList().size();
    }

    public List<MediaItem> getMediaItemList() {
        return mItems;
    }

    public MediaItem getMediaItem(int index) {
        return mItems.get(index);
//        return mDiffer.getCurrentList().get(index);
    }

    public void submitList(List<MediaItem> items) {
        List<MediaItem> newItems = new ArrayList<>();
        newItems.addAll(items);
        mDiffer.submitList(newItems);
    }

    public void setCurrentTrack(MediaItem item) {

        mCurrentItem = item;
//        for(int i = 0; i < mItems.size(); i++) {
//            MediaItem theItem = mItems.get(i);
//            Log.e("comparisonBug", "item.title:  " + item.title + ", theItem.title: " + theItem.title);
//            if(item.title.equals(theItem.title)){
//                setSelectedIndex(i);
//                if(mRecyclerView != null)
//                    mRecyclerView.scrollToPosition(i);
//                break;
//            }
//
//        }

        int index = mItems.indexOf(item);

//        int index = mDiffer.getCurrentList().indexOf(item);
        Log.d(TAG, "setCurrentTrack: index: " + index);
        setSelectedIndex(index);

        if(mRecyclerView != null)
            mRecyclerView.scrollToPosition(index);
    }

    public void updateMediaItemTitle(MediaItem item, String song){
        int index = mItems.indexOf(item);
        if(index >= 0 && index < mItems.size()) {
            MediaItem listItem = mItems.get(index);
            listItem.title = song;
            notifyItemChanged(index);
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    public void clear() {
        mItems.clear();
//        mDiffer.submitList(null);
        mSelectedIndex = -1;
        mOldSelectedIndex = -1;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivLeftIcon, ivRightIcon;
        TextView txtItemName;

        ViewHolder(View itemView) {
            super(itemView);
            ivLeftIcon = itemView.findViewById(R.id.iv_media_item_row_left_icon);
            txtItemName = itemView.findViewById(R.id.browse_item_name);
            ivRightIcon = itemView.findViewById(R.id.iv_media_item_row_right_icon);
        }
    }
}
