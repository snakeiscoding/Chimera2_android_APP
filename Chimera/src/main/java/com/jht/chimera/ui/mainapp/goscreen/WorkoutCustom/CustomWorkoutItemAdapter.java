package com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;
import com.jht.chimera.ui.mainapp.goscreen.GoScreenWorkoutDisplayItem;

import java.util.List;

/**
 * Created by AlexN on 11/3/2018.
 */
public class CustomWorkoutItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int MAX_WORKOUTS = 100;

    static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        View itemBackground;
        AppCompatTextView itemNameTextView;
        Button deleteItemButton;
        AppCompatImageView deleteIcon;

        WorkoutViewHolder(View itemView) {
            super(itemView);
            itemBackground = itemView.findViewById(R.id.background_view);
            itemNameTextView = itemView.findViewById(R.id.custom_workout_item_text);
            deleteItemButton = itemView.findViewById(R.id.delete_button);
            deleteIcon = itemView.findViewById(R.id.delete_item_icon);
        }
    }

    static class NewExerciseViewHolder extends RecyclerView.ViewHolder {
        TextView itemNameText;
        View itemBackground;

        NewExerciseViewHolder(View itemView) {
            super(itemView);
            itemNameText = itemView.findViewById(R.id.custom_workout_item_text);
            itemBackground = itemView.findViewById(R.id.custom_workout_item_background);
        }
    }

    static class TitleViewHolder extends RecyclerView.ViewHolder {
        AppCompatTextView titleText;

        TitleViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.list_item_title_text);
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageView loadingImage;

        LoadingViewHolder(View itemView) {
            super(itemView);
            loadingImage = itemView.findViewById(R.id.loading_image);
        }
    }

    interface ICustomWorkoutAdapter {
        void loadWorkoutAtPosition(int position);
        void addWorkout();
        void removeWorkout(CustomWorkoutItem item);
    }

    private List<CustomWorkoutItem> mItems;
    private int mSelectedPosition;
    private boolean mIsGuest;
    private ICustomWorkoutAdapter mFragmentCallback;

    CustomWorkoutItemAdapter(List<CustomWorkoutItem> items, ICustomWorkoutAdapter fragmentCallback, boolean isGuest) {
        mItems = items;
        mSelectedPosition = -1; //getFirstItem();
        this.mFragmentCallback = fragmentCallback;
        mIsGuest = isGuest;
        //mFragmentCallback.loadWorkoutAtPosition(mSelectedPosition); // should wait until all views are loaded before triggering selection
    }

    void setGuest(boolean isGuest) {
        if (isGuest != mIsGuest) {
            mIsGuest = isGuest;
            notifyDataSetChanged();
        }
    }

    int getSelectedPosition() {
        return mSelectedPosition;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View itemView;
        RecyclerView.ViewHolder viewHolder;
        switch (viewType) {
            case CustomWorkoutItem.WORKOUT_TYPE:
                itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.custom_workout_item, viewGroup, false);
                viewHolder = new WorkoutViewHolder(itemView);
                break;
            case CustomWorkoutItem.CATEGORY_TITLE_TYPE:
                itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.workout_recycler_title_item, viewGroup, false);
                viewHolder = new TitleViewHolder(itemView);
                break;
            case CustomWorkoutItem.CATEGORY_SUBTITLE_TITLE:
                itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.custom_workout_dash_item, viewGroup, false);
                viewHolder = new NewExerciseViewHolder(itemView);
                break;
            case CustomWorkoutItem.LOADING_TYPE:
                itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.custom_loading_item, viewGroup, false);
                viewHolder = new LoadingViewHolder(itemView);
                break;
            default:
                itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.custom_workout_item, viewGroup, false);
                viewHolder = new WorkoutViewHolder(itemView);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder customViewHolder, int position) {
        final CustomWorkoutItem item = mItems.get(position);
        switch (customViewHolder.getItemViewType()) {
            case CustomWorkoutItem.WORKOUT_TYPE:
                WorkoutViewHolder viewHolder = (WorkoutViewHolder) customViewHolder;
                bindWorkout(viewHolder, position);
                break;
            case CustomWorkoutItem.CATEGORY_TITLE_TYPE:
                ((TitleViewHolder) customViewHolder).titleText.setText(item.title);
                ((TitleViewHolder) customViewHolder).titleText.setMaxLines(item.title.contains(" ") || item.title.contains("-")? 2 : 1);
                break;
            case CustomWorkoutItem.CATEGORY_SUBTITLE_TITLE:
                ((NewExerciseViewHolder) customViewHolder).itemBackground.setOnClickListener(v -> {
                    int numWorkouts = getNumberOfWorkouts();
                    if (numWorkouts < MAX_WORKOUTS) {
                        mFragmentCallback.addWorkout();
                        mSelectedPosition = position;
                        mFragmentCallback.loadWorkoutAtPosition(mSelectedPosition);
                        notifyDataSetChanged();
                    }
                });
                break;
            case CustomWorkoutItem.LOADING_TYPE:
                LoadingViewHolder loadingViewHolder = (LoadingViewHolder) customViewHolder;
                RotateAnimation rotateAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotateAnimation.setDuration(1000);
                rotateAnimation.setRepeatMode(Animation.RESTART);
                rotateAnimation.setRepeatCount(Animation.INFINITE);
                rotateAnimation.setInterpolator(new LinearInterpolator());
                loadingViewHolder.loadingImage.startAnimation(rotateAnimation);
                break;
            default:
                break;
        }
    }

    private void bindWorkout(WorkoutViewHolder viewHolder, int position) {
        boolean isSelected = position == mSelectedPosition;
        viewHolder.itemNameTextView.setText(mItems.get(position).title);
        viewHolder.itemBackground.setSelected(isSelected);
        viewHolder.itemNameTextView.setSelected(isSelected);
        boolean showDelete = isSelected && getNumberOfWorkouts() > 1 && !mIsGuest;
        viewHolder.deleteItemButton.setVisibility(showDelete ? View.VISIBLE : View.INVISIBLE);
        viewHolder.deleteIcon.setVisibility(showDelete ? View.VISIBLE : View.INVISIBLE);

        viewHolder.itemBackground.setOnClickListener(v -> selectItem(position));
        viewHolder.deleteItemButton.setOnClickListener(v -> deleteItem(position));
    }

    public void selectItem(int position) {
        if (mSelectedPosition != position) {
            notifyItemChanged(mSelectedPosition);
            mSelectedPosition = position;
            notifyItemChanged(mSelectedPosition);
            mFragmentCallback.loadWorkoutAtPosition(position);
        }
    }
    public void selectFirstItem() {
        int position = getFirstItem();
        selectItem(position);
    }

    private void deleteItem(int position) {
        CustomWorkoutItem item = mItems.remove(position);
        mSelectedPosition = getFirstItem();
        notifyDataSetChanged();

        // Load the first workout
        mFragmentCallback.loadWorkoutAtPosition(mSelectedPosition);
        mFragmentCallback.removeWorkout(item);
    }

    private int getFirstItem() {
        for (int i = 0; i < mItems.size(); i++) {
            CustomWorkoutItem data = mItems.get(i);
            if (data.categoryType == GoScreenWorkoutDisplayItem.WORKOUT_TYPE) return i;
        }
        // This will be the default value if there's no workout items, after loading it will be on the correct space
        return 1;
    }

    private int getNumberOfWorkouts() {
        int count = 0;
        for (CustomWorkoutItem item : mItems) {
            if (item.categoryType == GoScreenWorkoutDisplayItem.WORKOUT_TYPE) {
                count++;
            }
        }
        return count;
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
}
