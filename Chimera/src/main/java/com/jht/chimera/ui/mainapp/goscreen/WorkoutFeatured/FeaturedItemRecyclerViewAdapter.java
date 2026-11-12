package com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.jht.chimera.R;
import com.jht.chimera.ui.mainapp.carousel.CarouselPagerAdapter;
import com.jht.chimera.ui.mainapp.goscreen.BitmapAsyncLoadTask;

import java.util.List;

/**
 * Created by AlexN on 11/3/2018.
 */

public class FeaturedItemRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static class FeaturedGridItemViewHolder extends RecyclerView.ViewHolder {
        View itemLayout;
        TextView itemNameText;
        ImageView itemBackground;
        BitmapAsyncLoadTask bitmapAsyncLoadTask;
        View workoutDisabledView;

        FeaturedGridItemViewHolder(View itemView) {
            super(itemView);
            itemLayout = itemView.findViewById(R.id.grid_item_layout);
            itemNameText = itemView.findViewById(R.id.grid_item_text);
            itemBackground = itemView.findViewById(R.id.grid_item_background);
            workoutDisabledView = itemView.findViewById(R.id.grid_item_background_disabled);
        }
    }

    static class FeaturedMainItemViewHolder extends RecyclerView.ViewHolder {

        ViewPager viewPager;

        FeaturedMainItemViewHolder(View itemView) {
            super(itemView);
            viewPager = itemView.findViewById(R.id.featured_view_pager);
        }
    }

    static class FeaturedSubTitleItemViewHolder extends RecyclerView.ViewHolder {
        TextView itemSubTitle;

        FeaturedSubTitleItemViewHolder(View itemView) {
            super(itemView);
            itemSubTitle = itemView.findViewById(R.id.featured_item_sub_title_text);
        }
    }

    public interface IGridRecyclerAdapter {
        void onItemClicked(int position);
    }

    public interface OnFeaturedWorkoutClickListener{
        void onItemClicked(FeaturedItem featuredItem);
    }

    private final List<FeaturedItem> mItems;
    private List<FeaturedItem> carouselFeaturedItems;
    private ImageView selectedBackground;
    private TextView selectedText;
    private final IGridRecyclerAdapter iGridRecyclerAdapter;
    private final OnFeaturedWorkoutClickListener onFeaturedWorkoutClickListener;
    private int width;
    private FragmentManager fragmentManager;
    private FeaturedMainItemViewHolder featuredMainItemViewHolder;
    private boolean isFeaturedMainPagerInitialized = false;

    public FeaturedItemRecyclerViewAdapter(List<FeaturedItem> items, IGridRecyclerAdapter iGridRecyclerAdapter, OnFeaturedWorkoutClickListener onFeaturedWorkoutClickListener,
                                           FragmentManager fragmentManager, List<FeaturedItem> carouselItems) {
        mItems = items;
        this.iGridRecyclerAdapter = iGridRecyclerAdapter;
        this.onFeaturedWorkoutClickListener = onFeaturedWorkoutClickListener;
        this.fragmentManager = fragmentManager;
        carouselFeaturedItems = carouselItems;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View itemView;
        switch (viewType) {
            case FeaturedItem.DISPLAY_GRID_TYPE:
                itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.featured_grid_item, viewGroup, false);
                return new FeaturedGridItemViewHolder(itemView);
            case FeaturedItem.DISPLAY_MAIN_FEATURE_TYPE:
                if (featuredMainItemViewHolder == null ) {
                    itemView = LayoutInflater.from(viewGroup.getContext())
                            .inflate(R.layout.featured_workout_carousel, viewGroup, false);
                    featuredMainItemViewHolder = new FeaturedMainItemViewHolder(itemView);
                }
                return featuredMainItemViewHolder;
            case FeaturedItem.DISPLAY_SUB_TITLE_TYPE:
                itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.featured_sub_title_item, viewGroup, false);
                return new FeaturedSubTitleItemViewHolder(itemView);
            default:
                itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.featured_sub_title_item, viewGroup, false);
                return new FeaturedGridItemViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        final FeaturedItem item = mItems.get(position);
        switch (holder.getItemViewType()) {
            case FeaturedItem.DISPLAY_GRID_TYPE:
                FeaturedGridItemViewHolder gridItemViewHolder = (FeaturedGridItemViewHolder) holder;
                gridItemViewHolder.itemNameText.setText(item.title);
                int thumbnailWidth = (int) (width * 8 / 50f);
                int thumbnailHeight = (int) (width * 0.1);
                gridItemViewHolder.bitmapAsyncLoadTask = new BitmapAsyncLoadTask(gridItemViewHolder.itemBackground,
                        thumbnailWidth, thumbnailHeight);
                if (item.thumbnailPath != null) {
                    gridItemViewHolder.bitmapAsyncLoadTask.execute(item.thumbnailPath);
                } else {
                    gridItemViewHolder.bitmapAsyncLoadTask.execute(item.getSafeBackgroundItem(0));
                }

                gridItemViewHolder.itemLayout.setOnClickListener(v -> {
                    if (selectedBackground != null) {
                        selectedText.setSelected(false);
                        selectedBackground.setSelected(false);
                    }

                    gridItemViewHolder.itemNameText.setSelected(true);
                    gridItemViewHolder.itemBackground.setSelected(true);

                    selectedText = gridItemViewHolder.itemNameText;
                    selectedBackground = gridItemViewHolder.itemBackground;
                    iGridRecyclerAdapter.onItemClicked((holder).getBindingAdapterPosition());
                });
                gridItemViewHolder.workoutDisabledView.setVisibility(item.enabled ? View.GONE : View.VISIBLE);
                break;
            case FeaturedItem.DISPLAY_MAIN_FEATURE_TYPE:

                if (isFeaturedMainPagerInitialized) return;
                isFeaturedMainPagerInitialized = true;

                FeaturedMainItemViewHolder mainItemHolder = (FeaturedMainItemViewHolder) holder;

                CarouselPagerAdapter adapter = new CarouselPagerAdapter(fragmentManager, carouselFeaturedItems);

                //Margin spacing for carousel items (left and right preview)
                int pageMargin = (int) ((width / 4) * 1.2);
                mainItemHolder.viewPager.setAdapter(adapter);
                mainItemHolder.viewPager.addOnPageChangeListener(adapter);
                mainItemHolder.viewPager.setPageMargin(-pageMargin);

                // default selection to middle
                mainItemHolder.viewPager.setCurrentItem(carouselFeaturedItems.size() * CarouselPagerAdapter.LOOP_COUNT / 2 + 1);

                // Set featured carousel on click
                adapter.setOnClickListener(index -> {
                    FeaturedItem selectedItem = carouselFeaturedItems.get(index);
                    if (onFeaturedWorkoutClickListener != null) onFeaturedWorkoutClickListener.onItemClicked(selectedItem);
                });

                break;
            case FeaturedItem.DISPLAY_SUB_TITLE_TYPE:
                FeaturedSubTitleItemViewHolder viewHolder = (FeaturedSubTitleItemViewHolder) holder;
                viewHolder.itemSubTitle.setText(item.title);
                break;
            default:
                break;

        }
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).displayType;
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        switch (holder.getItemViewType()) {
            case FeaturedItem.DISPLAY_GRID_TYPE:
                ((FeaturedGridItemViewHolder) holder).bitmapAsyncLoadTask.cancel(true);
                ((FeaturedGridItemViewHolder) holder).itemBackground.setImageBitmap(null);
                break;
        }
    }
}
