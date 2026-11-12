package com.jht.chimera.ui.mainapp.goscreen;

import static com.jht.chimera.ui.mainapp.ParkerGoScreenActivity.WorkoutLabelProvider;
import static com.jht.chimera.ui.mainapp.ParkerGoScreenActivity.WorkoutLabels;

import android.annotation.TargetApi;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.formatting.BasicNumberFormat;
import com.jht.chimera.lib.common.formatting.DistanceFormat;
import com.jht.chimera.lib.common.formatting.SpeedFormat;
import com.jht.chimera.lib.common.formatting.TimeFormat;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.datamatrixlib.DataMatrix;

import java.util.Collections;
import java.util.List;

/** RecyclerAdapter for the workout details next to the workout calendar */
@TargetApi(26)
public class CalendarWorkoutRecyclerAdapter extends RecyclerView.Adapter<CalendarWorkoutRecyclerAdapter.WorkoutItemHolder> {
    protected static final String TAG = "CalendarWorkoutRecyc...";
    protected List<CalendarWorkoutData> workouts = Collections.emptyList();
    protected WorkoutListEventListener eventHandler;

    public interface WorkoutListEventListener {
        void onRunWorkout(CalendarWorkoutData data);
    }
    private WorkoutLabelProvider workoutLabelProvider;

    public CalendarWorkoutRecyclerAdapter(WorkoutListEventListener events, WorkoutLabelProvider workoutLabelProvider) {
        super();
        eventHandler = events;
        this.workoutLabelProvider = workoutLabelProvider;
    }

    @Override
    public WorkoutItemHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.calendar_workout_item, viewGroup, false);
        WorkoutItemHolder mh = new WorkoutItemHolder(itemView);
        mh.buttonDoIt.setOnClickListener((View v) -> {
            if (eventHandler!=null) eventHandler.onRunWorkout(mh.data);
        });

        return mh;
    }

    @Override
    public void onBindViewHolder(WorkoutItemHolder myHolder, int i) {
        CalendarWorkoutData item = workouts.get(i);
        WorkoutLabels labels = (workoutLabelProvider == null ? null : workoutLabelProvider.getWorkoutLabels(item.startupParams));
        String label;
        if (labels == null) {
            label = StringLookup.get(R.string.unknown_uc);
        } else if (labels.appIcon != null && labels.appIcon.appName != null && !labels.appIcon.appName.isEmpty()) {
            label = labels.appIcon.appName;
        } else {
            label = labels.workoutName;
        }
        myHolder.setData(item, label);
    }

    @Override
    public int getItemCount() { return workouts.size(); }

    public void setWorkouts(List<CalendarWorkoutData> workouts) {
        this.workouts = workouts;
        this.notifyDataSetChanged();
    }

    public static class WorkoutItemHolder extends RecyclerView.ViewHolder {
        protected TextView time;
        // TODO - would stat recycler be cleaner? backed by stat objects with type enum containing drawables?
        protected TextView description;
        protected TextView statDuration;
        protected TextView statDistance;
        protected TextView statCalories;
        protected TextView statPace;
        protected TextView statSpeed;
        protected TextView statHeart;

        protected Button buttonDoIt;

        protected CalendarWorkoutData data;

        private WorkoutItemHolder(View v) {
            super(v);
            time = v.findViewById(R.id.time);
            description = itemView.findViewById(R.id.workout_description);

            statDuration = v.findViewById(R.id.stat_duration);
            statDistance = v.findViewById(R.id.stat_distance);
            statCalories = v.findViewById(R.id.stat_calories);

            statPace = v.findViewById(R.id.stat_pace);
            statSpeed = v.findViewById(R.id.stat_speed);
            statHeart = v.findViewById(R.id.stat_heart);

            buttonDoIt = v.findViewById(R.id.button_doit);
        }

        public void setData(CalendarWorkoutData itemData, String itemName) {
            boolean metric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
            data = itemData;

            description.setText(itemName);

            time.setText(TimeFormat.formatDate(itemData.date)+"\n"+TimeFormat.formatTimeOfDay(itemData.date));

            statDuration.setText(TimeFormat.formatSeconds(data.durationSeconds));
            statDistance.setText(DistanceFormat.formatWorkoutDistance(data.distanceKm, metric)); // does precision 2, but mock up is 1
            statCalories.setText(BasicNumberFormat.format(data.calories, 0));

            // workout summary calculates pace from speed rather than workout time and distance? so don't use data.paceSecondsPerKm ?
            if (data.speedKph > 0) {
                statPace.setVisibility(View.VISIBLE);
                statPace.setText(SpeedFormat.formatPace(data.speedKph, metric));
            } else {
                statPace.setVisibility(View.INVISIBLE);
            }

            statSpeed.setText(SpeedFormat.formatSpeed(data.speedKph, metric));

            if (data.heartRate > 0) {
                statHeart.setVisibility(View.VISIBLE);
                statHeart.setText(BasicNumberFormat.format(data.heartRate, 0));
            } else {
                statHeart.setVisibility(View.INVISIBLE);
            }
        }
    }
}
