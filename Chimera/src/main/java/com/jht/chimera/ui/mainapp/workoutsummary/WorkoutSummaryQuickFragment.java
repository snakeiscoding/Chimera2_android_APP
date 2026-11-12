package com.jht.chimera.ui.mainapp.workoutsummary;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.PostWorkoutSurveyGate;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.formatting.BasicNumberFormat;
import com.jht.chimera.lib.common.formatting.DistanceFormat;
import com.jht.chimera.lib.common.formatting.FloorFormat;
import com.jht.chimera.lib.common.formatting.SpeedFormat;
import com.jht.chimera.lib.common.formatting.TimeFormat;
import com.jht.chimera.lib.common.stats.SummaryData;
import com.jht.chimera.lib.widget.StarRatingView;
import com.jht.chimera.lib.workout.FavoriteIcon;
import com.jht.chimera.lib.workout.WorkoutStartupParams;
import com.jht.datamatrixlib.DataMatrix;

public class WorkoutSummaryQuickFragment extends Fragment {
    private static final String TAG = "WorkoutSummaryQuick";

    // TODO: These questions should be set from a translated collection retrieved by a unique question ID
    private static final String QUESTION1 = null; //"How was your experience?";
    private static final String QUESTION2 = null;

    private ConstraintLayout surveySection;

    private StarRatingView mStarRatingView1;
    private StarRatingView mStarRatingView2;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.workout_summary_layout_quick, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // show the favorite icon for parker
        if (ProductConfig.hasUserProfiles()) {
            WorkoutStartupParams workout = getFavoriteParams();
            if (workout != null) FavoriteIcon.populateIcon(view, workout);
            /*List<PreviousWorkout> workouts = RecordedWorkoutDatabase.getInstance().getRecentWorkouts();
            if (workouts.size() > 0) {
                PreviousWorkout workout = workouts.get(0);
                FavoriteIcon.populateIcon(getWindow().getDecorView(), workout);
            }*/
        }

        TextView thanksTextView = view.findViewById(R.id.thanks_for_feedback);

        surveySection = view.findViewById(R.id.survey_group);
        Button surveyButton = view.findViewById(R.id.btn_take_survey);
        surveyButton.setOnClickListener((v) -> displayPostWorkoutSurvey());

        ImageView closeSurvey = view.findViewById(R.id.btn_close_workout_rating);
        closeSurvey.setOnClickListener(v -> surveySection.setVisibility(View.GONE));

        mStarRatingView1 = view.findViewById(R.id.star_rating1);
        // TODO: Properly set question id
        mStarRatingView1.setQuestion(0, QUESTION1);
        mStarRatingView1.setStarSelectedListener((selectedStarValue) -> {
            //Star rating selected for question 1
            Log.d(TAG, "Value selected for star rating 1: " + selectedStarValue);
            thanksTextView.setVisibility(View.VISIBLE);
        });

        mStarRatingView2 = view.findViewById(R.id.star_rating2);
        // TODO: Properly set question id
        mStarRatingView2.setQuestion(0, QUESTION2);
        mStarRatingView2.setStarSelectedListener((selectedStarValue) -> {
            //Star rating selected for question 2
            Log.d(TAG, "Value selected for star rating 2: " + selectedStarValue);
            thanksTextView.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        populateStats();

        // commercial shows the survey before the summary (PostWorkoutSurveyActivity), so only retail offers it here
        if (PostWorkoutSurveyGate.showsInSummarySurvey(ProductConfig.isRetail(), DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_BOOL))) {
            surveySection.setVisibility(View.VISIBLE);
        } else {
            surveySection.setVisibility(View.GONE);
        }

        if(QUESTION1 != null && !QUESTION1.isEmpty()){
            mStarRatingView1.setVisibility(View.VISIBLE);
        } else {
            mStarRatingView1.setVisibility(View.GONE);
        }

        if(QUESTION2 != null && !QUESTION2.isEmpty()){
            mStarRatingView2.setVisibility(View.VISIBLE);
        } else {
            mStarRatingView2.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mStarRatingView1.sendAnalytics();
        mStarRatingView2.sendAnalytics();
    }

    private WorkoutStartupParams getFavoriteParams() {
        try {
            String appUuid = DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING);
            String customWorkoutUuid = DataMatrix.getString(WorkoutKeys.DATA_CUSTOM_WORKOUT_UUID_STRING);
            int workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);
            return (workoutId < 0 ? null : WorkoutStartupParams.forProgramTypeAndApp(workoutId, appUuid, customWorkoutUuid));
        } catch (Exception e) {
            Log.d(TAG, "getWorkoutStartupParams threw", e);
            return null;
        }
    }

    private void populateStats() {
        View v = getView();
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        boolean metric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);

        // Should top WorkoutSummaryActivity take care of this and pass it to fragments so it is only done once?
        // OR does each need to grab it as they are created and presented individually?
        SummaryData summaryData = DataMatrix.getParcelable(WorkoutKeys.DATA_WORKOUT_SUMMARY_INFO_PARCELABLE, SummaryData.class);
        if (summaryData == null) {
            Log.w(TAG, "DATA_WORKOUT_SUMMARY_INFO_PARCELABLE is null");
            return;
        }

        WorkoutQuickStat stat1 = v.findViewById(R.id.stat1);
        WorkoutQuickStat stat2 = v.findViewById(R.id.stat2);
        WorkoutQuickStat stat3 = v.findViewById(R.id.stat3);
        WorkoutQuickStat stat4 = v.findViewById(R.id.stat4);
        WorkoutQuickStat stat5 = v.findViewById(R.id.stat5);

        switch(config.machineClassType){
            case Treadmill:
                displayDuration(stat1, summaryData);
                displayTotalDistance(stat2,summaryData, metric);
                displayAvgSpeed(stat3, summaryData, metric);
                displayAvgIncline(stat4, summaryData);
                displayTotalCalories(stat5, summaryData);
                break;
            case Ascent:
                displayDuration(stat1, summaryData);
                displayTotalDistance(stat2,summaryData, metric);
                displayTotalCalories(stat3, summaryData);
                displayAvgIncline(stat4, summaryData);
                displayWatt(stat5, summaryData);
                break;
            case Bike:
            case SpinBike:
            case Elliptical:
            case RecumbentElliptical:
                displayDuration(stat1, summaryData);
                displayTotalDistance(stat2,summaryData, metric);
                displayTotalCalories(stat3, summaryData);
                if(!config.WATTS_INACCURATE)displayWatt(stat4, summaryData);
                break;
            case Stepper:
            case Climbmill:
                displayDuration(stat1, summaryData);
                displayTotalCalories(stat2, summaryData);
                displaySteps(stat3, summaryData);
                displayFloor(stat4, summaryData);
                break;
        }
    }

    private void displayDuration(WorkoutQuickStat stat, SummaryData summaryData){
        stat.setVisibility(View.VISIBLE);
        stat.setLabel(ContextUtil.getString(R.string.total_duration_tc), R.drawable.ic_time_total_white);
        stat.setValue(TimeFormat.formatTimeSeconds(Math.round(summaryData.totalSummary.statsAccumulated.secondsStateRun)));
        stat.setUnit(ContextUtil.getString(R.string.minutes_lc));
    }

    private void displayTotalDistance(WorkoutQuickStat stat, SummaryData summaryData, boolean metric){
        stat.setVisibility(View.VISIBLE);
        stat.setLabel(ContextUtil.getString(R.string.total_distance_tc), R.drawable.ic_distance_white);
        stat.setValue(DistanceFormat.formatWorkoutDistance(summaryData.totalSummary.statsAccumulated.distanceKm, metric));
        stat.setUnit(ContextUtil.getString(metric ? R.string.km_lc : R.string.miles_lc));
    }

    private void displayAvgSpeed(WorkoutQuickStat stat, SummaryData summaryData, boolean metric){
        stat.setVisibility(View.VISIBLE);
        stat.setLabel(ContextUtil.getString(R.string.avg_speed_tc), R.drawable.ic_speed_white);
        stat.setValue(SpeedFormat.formatSpeed(summaryData.totalSummary.statsAverages.averageSpeedKph, metric));
        stat.setUnit(ContextUtil.getString(metric ? R.string.kph_lc : R.string.mph_lc));
    }

    private void displayAvgIncline(WorkoutQuickStat stat, SummaryData summaryData){
        stat.setVisibility(View.VISIBLE);
        stat.setLabel(ContextUtil.getString(R.string.avg_incline_tc), R.drawable.ic_incline_white);
        stat.setValue(FloorFormat.format(summaryData.totalSummary.statsAverages.averageIncline, 1));
        stat.setUnit("%");
    }

    private void displayTotalCalories(WorkoutQuickStat stat, SummaryData summaryData){
        stat.setVisibility(View.VISIBLE);
        stat.setLabel(ContextUtil.getString(R.string.total_calories_tc), R.drawable.ic_calories_white);
        stat.setValue(FloorFormat.format(summaryData.totalSummary.statsAccumulated.calories, 0));
        stat.setUnit(ContextUtil.getString(R.string.kcal_lc));
    }

    private void displayWatt(WorkoutQuickStat stat, SummaryData summaryData){
        stat.setVisibility(View.VISIBLE);
        stat.setLabel(ContextUtil.getString(R.string.power_tc), R.drawable.ic_watts_white);
        stat.setValue(BasicNumberFormat.format(summaryData.totalSummary.statsAverages.averageWatts, 0));
        stat.setUnit(ContextUtil.getString(R.string.avg_watts_lc));
    }

    private void displaySteps(WorkoutQuickStat stat, SummaryData summaryData){
        stat.setVisibility(View.VISIBLE);
        stat.setLabel(ContextUtil.getString(R.string.steps_tc), R.drawable.ic_steps_total_white);
        stat.setValue(FloorFormat.format(summaryData.totalSummary.statsAccumulated.steps, 0));
        stat.setUnit("-");
    }

    private void displayFloor(WorkoutQuickStat stat, SummaryData summaryData){
        stat.setVisibility(View.VISIBLE);
        stat.setLabel(ContextUtil.getString(R.string.floors_tc), R.drawable.ic_floors_white);
        stat.setValue(FloorFormat.format(summaryData.totalSummary.statsAccumulated.floors, 0));
        stat.setUnit("-");
    }

    private void displayPostWorkoutSurvey() {
        // putting survey on the parent activity so it is on top of the paging fragments
        Activity activity = getActivity();
        if (activity instanceof WorkoutSummaryActivity) {
            ((WorkoutSummaryActivity) activity).displayPostWorkoutSurvey();
        }
    }
}
