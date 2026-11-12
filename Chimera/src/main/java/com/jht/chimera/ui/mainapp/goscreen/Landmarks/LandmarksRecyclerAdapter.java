package com.jht.chimera.ui.mainapp.goscreen.Landmarks;

import android.util.Log;
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

public class LandmarksRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "LandmarksRecyclerAda...";
    static class LandmarksItemViewHolder extends RecyclerView.ViewHolder {
        TextView itemNameText;
        TextView itemNameGoal;
        ImageView itemBackground;
        BitmapAsyncLoadTask bitmapAsyncLoadTask;

        LandmarksItemViewHolder(View itemView) {
            super(itemView);
            itemNameText = itemView.findViewById(R.id.landmarks_item_title);
            itemNameGoal = itemView.findViewById(R.id.landmarks_item_goal);
            itemBackground = itemView.findViewById(R.id.landmarks_item_background);
        }
    }

    interface ILandmarksRecyclerAdapter {
        void onItemClicked(int position);
    }

    private List<GoScreenWorkoutDisplayItem> mItems;
    private ILandmarksRecyclerAdapter iLandmarksRecyclerAdapter;
    private int height;

    LandmarksRecyclerAdapter(List<GoScreenWorkoutDisplayItem> items, ILandmarksRecyclerAdapter iLandmarksRecyclerAdapter) {
        mItems = items;
        this.iLandmarksRecyclerAdapter = iLandmarksRecyclerAdapter;
    }

    public void setHeight(int height) {
        this.height = height;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View itemView = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.landmarks_recycler_item, viewGroup, false);
        return new LandmarksItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder customViewHolder, int position) {
        final GoScreenWorkoutDisplayItem item = mItems.get(position);
        switch (customViewHolder.getItemViewType()){
            case GoScreenWorkoutDisplayItem.WORKOUT_TYPE:
                Log.i(TAG, "onBindViewHolder: " + item.title + ", " + item.description + ", " + item.backgroundPath) ;
                ((LandmarksItemViewHolder)customViewHolder).itemNameText.setText(item.title);
                ((LandmarksItemViewHolder)customViewHolder).itemNameGoal.setText(item.description);
                ((LandmarksItemViewHolder)customViewHolder).bitmapAsyncLoadTask = new BitmapAsyncLoadTask(((LandmarksItemViewHolder) customViewHolder).itemBackground,
                        (int)(0.40 * height), (int)(0.46 * height));
                ((LandmarksItemViewHolder)customViewHolder).bitmapAsyncLoadTask.execute(item.backgroundPath);
//                ((LandmarksItemViewHolder)customViewHolder).bitmap = BitmapLoader.decodeSampledBitmap(context, item.backgroundPath,
//                        (int)(0.40 * height), (int)(0.46 * height));
//                ((LandmarksItemViewHolder) customViewHolder).itemBackground.setImageBitmap( ((LandmarksItemViewHolder)customViewHolder).bitmap);
//                BitmapLoader.loadSampledBitmap(context,((LandmarksItemViewHolder)customViewHolder).itemBackground, item.backgroundPath);

                customViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.i(TAG, "onClick: " + customViewHolder.getAdapterPosition());
                        iLandmarksRecyclerAdapter.onItemClicked(customViewHolder.getAdapterPosition());
                    }
                });
                break;
            default:
                break;
        }
    }

    @Override
    public int getItemCount() {
        return mItems == null ? 0 : mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).categoryType;
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
//        ((LandmarksItemViewHolder)holder).bitmap.recycle();
        ((LandmarksItemViewHolder)holder).bitmapAsyncLoadTask.cancel(true);
        ((LandmarksItemViewHolder)holder).itemBackground.setImageBitmap(null);
    }
}
