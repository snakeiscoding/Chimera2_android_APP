package com.jht.chimera.ui.mainapp.goscreen;

import static com.jht.chimera.lib.widget.CommonRoutines.getAssetUri;
import static com.jht.chimera.ui.mainapp.ParkerGoScreenActivity.WorkoutLabelProvider;
import static com.jht.chimera.ui.mainapp.ParkerGoScreenActivity.WorkoutLabels;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;
import com.jht.chimera.lib.workout.WorkoutStartupParams;

import java.util.List;

public class PreviousWorkoutRecyclerViewAdapter extends RecyclerView.Adapter<PreviousWorkoutRecyclerViewAdapter.WorkoutItemHolder> {

    public interface OnClickWorkoutListener {
        void onClick(WorkoutStartupParams workout);
    }

    List<? extends WorkoutStartupParams> workoutList;
    OnClickWorkoutListener onClickWorkoutListener;
    Context context;
    int imageWidth, imageHeight;
    String description;
    WorkoutLabelProvider workoutLabelProvider;

    public PreviousWorkoutRecyclerViewAdapter(@NonNull Context context, List<? extends WorkoutStartupParams> previousWorkoutList, String description, WorkoutLabelProvider workoutLabelProvider) {
        this.workoutList = previousWorkoutList;
        this.context = context;
        imageWidth = (int) context.getResources().getDimension(R.dimen.previous_workout_list_width);
        imageHeight = (int) context.getResources().getDimension(R.dimen.previous_workout_list_height);
        this.description = description;
        this.workoutLabelProvider = workoutLabelProvider;
    }

    @NonNull
    @Override
    public WorkoutItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.workout_item_holder, viewGroup, false);
        return new WorkoutItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutItemHolder holder, int position) {
        final WorkoutStartupParams selectedWorkout = workoutList.get(position);
        holder.image.setImageResource(R.color.white);
        holder.image.setContentDescription(description + position);
        holder.image.setOnClickListener(v -> {
            if (onClickWorkoutListener != null) onClickWorkoutListener.onClick(selectedWorkout);
        });

        Uri thumbnailDefault = getAssetUri("backgrounds/bg_manual.jpg"); // or just show nothing?
        WorkoutLabels labels = (this.workoutLabelProvider == null ? null : this.workoutLabelProvider.getWorkoutLabels(selectedWorkout));
        if (labels != null) {
            holder.name.setText(labels.workoutName);
            if (labels.appIcon != null) {
                holder.image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                holder.image.setImageDrawable(labels.appIcon.getAppIcon());
                if (labels.appIcon.appInfo.useThirdPartyInterface) {
                    if (labels.appIcon.appName != null && !labels.appIcon.appName.isEmpty()) holder.name.setText(labels.appIcon.appName); // if 3rd party, use the app name
                }
            } else {
                holder.image.setScaleType(ImageView.ScaleType.FIT_XY);
                BitmapAsyncLoadTask bitmapAsyncLoadTask = new BitmapAsyncLoadTask(holder.image, imageWidth, imageHeight);
                bitmapAsyncLoadTask.execute(labels.workoutBackground == null ? thumbnailDefault : labels.workoutBackground);
            }
        } else {    // Something went wrong, show unknown placeholder
            holder.name.setText(R.string.unknown_uc);
            holder.image.setScaleType(ImageView.ScaleType.FIT_XY);
            BitmapAsyncLoadTask bitmapAsyncLoadTask = new BitmapAsyncLoadTask(holder.image, imageWidth, imageHeight);
            bitmapAsyncLoadTask.execute(thumbnailDefault);
        }
    }

    @Override
    public int getItemCount() {
        return workoutList.size();
    }


    public class WorkoutItemHolder extends RecyclerView.ViewHolder {

        ImageView image;
        TextView name;

        public WorkoutItemHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            name = itemView.findViewById(R.id.text);
        }
    }

    public void setOnClickWorkoutListener(OnClickWorkoutListener onClickWorkoutListener) {
        this.onClickWorkoutListener = onClickWorkoutListener;
    }

}
