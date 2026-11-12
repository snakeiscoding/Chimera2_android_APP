package com.jht.chimera.ui.mainapp.goscreen.VA;

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

public class VASelectionRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    static class VAItemViewHolder extends RecyclerView.ViewHolder {
        View itemLayout;
        TextView itemNameText;
        ImageView itemBackground;
        BitmapAsyncLoadTask bitmapAsyncLoadTask;

        VAItemViewHolder(View itemView) {
            super(itemView);
            itemLayout = itemView.findViewById(R.id.va_item_layout);
            itemNameText = itemView.findViewById(R.id.va_item_text);
            itemBackground = itemView.findViewById(R.id.va_item_backround);
        }
    }

    static class VATitleViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;

        VATitleViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.va_item_description);
        }
    }

    interface IVARecyclerAdapter {
        void onItemClicked(int position);
    }

    private List<GoScreenWorkoutDisplayItem> mItems;
    private IVARecyclerAdapter iVARecyclerAdapter;
    private int width;

    VASelectionRecyclerAdapter(List<GoScreenWorkoutDisplayItem> items, IVARecyclerAdapter iVARecyclerAdapter) {
        mItems = items;
        this.iVARecyclerAdapter = iVARecyclerAdapter;
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
            case GoScreenWorkoutDisplayItem.WORKOUT_TYPE:
                itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.va_recycler_item, viewGroup, false);
                viewHolder = new VAItemViewHolder(itemView);
                break;
            case GoScreenWorkoutDisplayItem.CATEGORY_TITLE_TYPE:
                itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.va_recycler_title_item, viewGroup, false);
                viewHolder = new VATitleViewHolder(itemView);
                break;
            default:
                itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.va_recycler_item, viewGroup, false);
                viewHolder = new VAItemViewHolder(itemView);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder customViewHolder, int position) {
        final GoScreenWorkoutDisplayItem item = mItems.get(position);
        switch (customViewHolder.getItemViewType()){
            case GoScreenWorkoutDisplayItem.WORKOUT_TYPE:
                ((VAItemViewHolder)customViewHolder).itemNameText.setText(item.title);
                ((VAItemViewHolder)customViewHolder).bitmapAsyncLoadTask = new BitmapAsyncLoadTask(((VAItemViewHolder) customViewHolder).itemBackground,
                        (int)(0.28 * width), (int)(0.16 * width));
                ((VAItemViewHolder)customViewHolder).bitmapAsyncLoadTask.execute(item.backgroundPath);
                customViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        iVARecyclerAdapter.onItemClicked(customViewHolder.getAdapterPosition());
                    }
                });
                break;
            case GoScreenWorkoutDisplayItem.CATEGORY_TITLE_TYPE:
                ((VATitleViewHolder)customViewHolder).titleText.setText(item.title);
                break;
            default:
                break;
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).categoryType;
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        switch (holder.getItemViewType()){
            case GoScreenWorkoutDisplayItem.WORKOUT_TYPE:
                ((VAItemViewHolder)holder).bitmapAsyncLoadTask.cancel(true);
                ((VAItemViewHolder)holder).itemBackground.setImageBitmap(null);
        }

    }
}
