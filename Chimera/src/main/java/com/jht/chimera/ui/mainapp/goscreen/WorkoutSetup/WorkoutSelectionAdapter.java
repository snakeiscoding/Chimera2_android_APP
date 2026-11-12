package com.jht.chimera.ui.mainapp.goscreen.WorkoutSetup;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;
import com.jht.chimera.ui.mainapp.goscreen.BitmapAsyncLoadTask;
import com.jht.chimera.ui.mainapp.goscreen.GoScreenWorkoutDisplayItem;

import java.util.List;

/**
 * Created by AlexN on 11/3/2018.
 */

public class WorkoutSelectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private String TAG = "WorkoutSelectionAdapter.java";

    static class WorkoutItemViewHolder extends RecyclerView.ViewHolder {
        View itemLayout;
        TextView itemNameText;
        ImageView itemBackground;
        ImageView itemOverlay;
        View itemDisabledBackground;
        BitmapAsyncLoadTask bitmapAsyncLoadTask;

        WorkoutItemViewHolder(View itemView) {
            super(itemView);
            itemLayout = itemView.findViewById(R.id.list_item_layout);
            itemNameText = itemView.findViewById(R.id.list_item_text);
            itemBackground = itemView.findViewById(R.id.list_item_background);
            itemOverlay = itemView.findViewById(R.id.list_item_overlay);
            itemDisabledBackground = itemView.findViewById(R.id.list_item_background_disabled);
        }
    }

    static class WorkoutTitleViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;

        WorkoutTitleViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.list_item_title_text);
        }
    }

    static class WorkoutSubTitleViewHolder extends RecyclerView.ViewHolder {
        TextView subTitleText;

        WorkoutSubTitleViewHolder(View itemView) {
            super(itemView);
            subTitleText = itemView.findViewById(R.id.list_item_sub_title_text);
        }
    }

    public interface IGridRecyclerAdapter {
        void onItemClicked(int position);
    }

    private final List<GoScreenWorkoutDisplayItem> mItems;
    private int mSelectedPosition = 2;
    private final IGridRecyclerAdapter iGridRecyclerAdapter;
    private int width;

    public WorkoutSelectionAdapter(List<GoScreenWorkoutDisplayItem> items, IGridRecyclerAdapter iGridRecyclerAdapter) {
        mItems = items;
        this.iGridRecyclerAdapter = iGridRecyclerAdapter;
        iGridRecyclerAdapter.onItemClicked(mSelectedPosition);
    }

    public WorkoutSelectionAdapter(List<GoScreenWorkoutDisplayItem> items, int selectedPosition, IGridRecyclerAdapter iGridRecyclerAdapter) {
        mSelectedPosition = selectedPosition;
        mItems = items;
        this.iGridRecyclerAdapter = iGridRecyclerAdapter;
        iGridRecyclerAdapter.onItemClicked(mSelectedPosition);
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View itemView;
        RecyclerView.ViewHolder viewHolder;
        switch (viewType) {
            case GoScreenWorkoutDisplayItem.CATEGORY_TITLE_TYPE:
                itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.workout_recycler_title_item, viewGroup, false);
                viewHolder = new WorkoutTitleViewHolder(itemView);
                break;
            case GoScreenWorkoutDisplayItem.CATEGORY_SUBTITLE_TITLE:
                itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.workout_recycler_sub_title_item, viewGroup, false);
                viewHolder = new WorkoutSubTitleViewHolder(itemView);
                break;
            case GoScreenWorkoutDisplayItem.WORKOUT_TYPE:
            default:
                itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.workout_recycler_item, viewGroup, false);
                viewHolder = new WorkoutItemViewHolder(itemView);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder customViewHolder, int position) {
        final GoScreenWorkoutDisplayItem item = mItems.get(position);
        switch (customViewHolder.getItemViewType()) {
            case GoScreenWorkoutDisplayItem.WORKOUT_TYPE:
                WorkoutItemViewHolder viewHolder = (WorkoutItemViewHolder) customViewHolder;
                bindWorkoutItem(viewHolder, position);
                break;
            case GoScreenWorkoutDisplayItem.CATEGORY_TITLE_TYPE:
                ((WorkoutTitleViewHolder) customViewHolder).titleText.setText(item.title);
                break;
            case GoScreenWorkoutDisplayItem.CATEGORY_SUBTITLE_TITLE:
                ((WorkoutSubTitleViewHolder) customViewHolder).subTitleText.setText(item.title);
                break;
            default:
                break;
        }
    }

    private void bindWorkoutItem(WorkoutItemViewHolder viewHolder, int position) {
        GoScreenWorkoutDisplayItem item = mItems.get(position);
        if (item.enabledState == GoScreenWorkoutDisplayItem.WorkoutStateTypes.DISABLED.ordinal()) {
            viewHolder.itemOverlay.setEnabled(false);
        } else if (item.enabledState == GoScreenWorkoutDisplayItem.WorkoutStateTypes.ENABLED.ordinal()) {
            viewHolder.itemOverlay.setEnabled(true);
        }

        viewHolder.itemNameText.setText(item.title);
        viewHolder.itemBackground.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        viewHolder.itemView.setContentDescription(item.title);
        viewHolder.bitmapAsyncLoadTask = new BitmapAsyncLoadTask(viewHolder.itemBackground, (int) (0.755 * width), (int) (0.49 * width));
        viewHolder.bitmapAsyncLoadTask.execute(item.thumbnailPath);
        viewHolder.itemView.setOnClickListener(v -> selectItem(position));

        viewHolder.itemNameText.setSelected(mSelectedPosition == position);
        viewHolder.itemOverlay.setSelected(mSelectedPosition == position);
        viewHolder.itemDisabledBackground.setVisibility(item.enabledState == 1 ? View.VISIBLE : View.GONE);
    }


    public void selectItem(int position) {
        if (mSelectedPosition != position) {
            notifyItemChanged(mSelectedPosition);
            mSelectedPosition = position;
            notifyItemChanged(mSelectedPosition);
            iGridRecyclerAdapter.onItemClicked(position);
        }
    }

    @Override
    public int getItemCount() {
        if (mItems != null) {
            return mItems.size();
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).categoryType;
    }


    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.getItemViewType() == GoScreenWorkoutDisplayItem.WORKOUT_TYPE) {
            ((WorkoutItemViewHolder) holder).bitmapAsyncLoadTask.cancel(true);
            ((WorkoutItemViewHolder) holder).itemBackground.setImageBitmap(null);
        }
    }
}
