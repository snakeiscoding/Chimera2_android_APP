package com.jht.chimera.ui.mainapp.workoutsummary;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.formatting.BasicNumberFormat;
import com.jht.chimera.lib.common.formatting.DistanceFormat;
import com.jht.chimera.lib.common.formatting.FloorFormat;
import com.jht.chimera.lib.common.formatting.TimeFormat;
import com.jht.chimera.lib.common.stats.SummaryData;
import com.jht.chimera.lib.common.stats.WorkoutSplit;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.integrations.myzone.TimeInZones;
import com.jht.chimera.lib.workout.FitnessTestResultHelper;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WorkoutSummaryFullFragment extends Fragment {
    private static final String TAG = "WorkoutSummaryFull";
    private ListView mSummaryListView;

    private ConstraintLayout mFitnessTestSummaryLayout;
    private TextView mRatingTextView;
    private TextView mLastStageTextView;
    private ConstraintLayout mMyZoneSummaryLayout;

    private SummaryData mSummaryData;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.workout_summary_layout_full, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSummaryListView = view.findViewById(R.id.workout_summary_list_view);

        mFitnessTestSummaryLayout = view.findViewById(R.id.fitness_test_summary_layout);
        mRatingTextView = view.findViewById(R.id.fitness_test_result_rating);
        mLastStageTextView = view.findViewById(R.id.fitness_test_last_stage);
        mMyZoneSummaryLayout = view.findViewById(R.id.myzone_summary_layout);

    }

    @Override
    public void onResume() {
        super.onResume();

        mSummaryListView.setAdapter(null);
        SummaryRowAdapter rowAdapter = getRowAdapter();
        if (rowAdapter != null) {
            mSummaryListView.setAdapter(rowAdapter);
        }

        View view = getView();
        displaySpecialSummaryInfo();
        displayMyZoneSummaryInfo(view);
    }

    private SummaryRowAdapter getRowAdapter() {
        Context context = ContextUtil.getApplicationContext();
        List<SummaryRowItem> rowItems = new ArrayList<>();
        SummaryData summaryDataBytes = DataMatrix.getParcelable(WorkoutKeys.DATA_WORKOUT_SUMMARY_INFO_PARCELABLE, SummaryData.class);
        if (summaryDataBytes == null) return null;
        mSummaryData = summaryDataBytes;
        WorkoutSummaryValues summaryStatValues = new WorkoutSummaryValues(context);

        boolean showGymKit = false;
        if (mSummaryData.totalSummary.statsAccumulated.activeCalories > 0) showGymKit = true;
        WorkoutSummarySetup workoutSummarySetup = new WorkoutSummarySetup();
        List<WorkoutSummarySetup.WorkoutSummaryItem> summaryItems = workoutSummarySetup.getWorkoutSummary(showGymKit);

        for (WorkoutSummarySetup.WorkoutSummaryItem item : summaryItems) {
            SummaryRowItem rowItem = getSummaryRow(item, summaryStatValues);
            rowItems.add(rowItem);
        }

        // Show lines for the track app splits, not available on climbmill/stepper
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        if (config.machineClassType != JHTCommonTypes.MachineClassificationType.Climbmill &&
                config.machineClassType != JHTCommonTypes.MachineClassificationType.Stepper) {
            int splitNumber = 1;
            String distanceUnits = getResources().getString(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL) ? R.string.km_lc : R.string.mi_lc);
            for (WorkoutSplit split : mSummaryData.workoutSummary.statsSplits.workoutSplits) {
                SummaryRowItem splitNumberRow = new SummaryRowItem();
                splitNumberRow.setLabel(StringLookup.getString(R.string.summary_number_split_uc, context) + splitNumber);
                splitNumberRow.setWarmUp("-");
                splitNumberRow.setWorkout(String.valueOf(splitNumber));
                splitNumberRow.setCoolDown("-");
                splitNumberRow.setTotal(String.valueOf(splitNumber));

                SummaryRowItem splitTimeRow = new SummaryRowItem();
                splitTimeRow.setLabel(StringLookup.getString(R.string.summary_split_time_uc, context));
                splitTimeRow.setWarmUp("-");
                splitTimeRow.setWorkout(TimeFormat.formatTime(split.timeMs));
                splitTimeRow.setCoolDown("-");
                splitTimeRow.setTotal(TimeFormat.formatTime(split.timeMs));

                SummaryRowItem splitDistance = new SummaryRowItem();
                splitDistance.setLabel(StringLookup.getString(R.string.summary_split_distance_uc, context));
                splitDistance.setWarmUp("-");
                splitDistance.setWorkout(DistanceFormat.formatWorkoutDistance(split.distanceKm) + " " + distanceUnits);
                splitDistance.setCoolDown("-");
                splitDistance.setTotal(DistanceFormat.formatWorkoutDistance(split.distanceKm) + " " + distanceUnits);

                splitNumber++;

                rowItems.add(splitNumberRow);
                rowItems.add(splitTimeRow);
                rowItems.add(splitDistance);
            }

            int totalLaps = DataMatrix.getInt(WorkoutKeys.DATA_LAP_COUNT_INT);
            SummaryRowItem totalLapsRow = new SummaryRowItem();
            totalLapsRow.setLabel(StringLookup.getString(R.string.summary_total_laps_uc, context));
            totalLapsRow.setWarmUp("-");
            totalLapsRow.setWorkout(String.valueOf(totalLaps));
            totalLapsRow.setCoolDown("-");
            totalLapsRow.setTotal(String.valueOf(totalLaps));

            int bestTime = DataMatrix.getInt(WorkoutKeys.DATA_LAP_BEST_TIME_MS_INT);
            SummaryRowItem bestTimeRow = new SummaryRowItem();
            bestTimeRow.setLabel(StringLookup.getString(R.string.best_lap_uc, context));
            bestTimeRow.setWarmUp("-");
            bestTimeRow.setWorkout(TimeFormat.formatTime(bestTime));
            bestTimeRow.setCoolDown("-");
            bestTimeRow.setTotal(TimeFormat.formatTime(bestTime));

            rowItems.add(totalLapsRow);
            rowItems.add(bestTimeRow);
        }

        return new SummaryRowAdapter(context,
                R.layout.workout_summary_row_layout, rowItems, "#fcaf1a"); //NON-NLS
    }

    private SummaryRowItem getSummaryRow(WorkoutSummarySetup.WorkoutSummaryItem item, WorkoutSummaryValues summaryStatValues) {
        SummaryRowItem rowItem = new SummaryRowItem();
        WorkoutSummaryValues.SummaryValueInfo statInfo = summaryStatValues.getInfo(item.statId);

        rowItem.setLabel(statInfo.getName());
        String warmupStatValue;
        String workoutStatValue;
        String cooldownStatValue;
        String totalStatValue;

        boolean hasWarmup = mSummaryData.warmupSummary != null && mSummaryData.warmupSummary.statsAccumulated.secondsStateRun > 0;
        boolean hasWorkout = mSummaryData.workoutSummary != null && mSummaryData.workoutSummary.statsAccumulated.secondsStateRun > 0;
        boolean hasCooldown = mSummaryData.cooldownSummary != null && mSummaryData.cooldownSummary.statsAccumulated.secondsStateRun > 0;
        boolean hasTotal = mSummaryData.totalSummary != null && mSummaryData.totalSummary.statsAccumulated.secondsStateRun > 0;

        if (hasWarmup) warmupStatValue = statInfo.getStat(mSummaryData.warmupSummary);
        else warmupStatValue = "-";

        if (hasWorkout) workoutStatValue = statInfo.getStat(mSummaryData.workoutSummary);
        else workoutStatValue = "-";

        if (hasCooldown) cooldownStatValue = statInfo.getStat(mSummaryData.cooldownSummary);
        else cooldownStatValue = "-";

        if (hasTotal) totalStatValue = statInfo.getStat(mSummaryData.totalSummary);
        else totalStatValue = "-";


        int roundedPrecision = statInfo.getRoundedPrecision();
        // If -1 do not round this value
        if (roundedPrecision >= 0) {
            float warmupVal = hasWarmup ? Float.parseFloat(warmupStatValue) : 0;
            float workoutVal = hasWorkout ? Float.parseFloat(workoutStatValue) : 0;
            float cooldownVal;
            float totalVal = hasTotal ? Float.parseFloat(totalStatValue) : 0;

            if (hasCooldown) {
                cooldownVal = totalVal - workoutVal - warmupVal;
                cooldownStatValue = BasicNumberFormat.format(cooldownVal, roundedPrecision);
            } else if (hasWarmup) {
                warmupVal = totalVal - workoutVal;
                warmupStatValue = BasicNumberFormat.format(warmupVal, roundedPrecision);
            }
        }

        String unit = statInfo.getStatUnit();
        if (unit != null && !unit.isEmpty()) {
            if (hasWarmup) warmupStatValue = warmupStatValue + " " + unit;
            if (hasWorkout) workoutStatValue = workoutStatValue + " " + unit;
            if (hasCooldown) cooldownStatValue = cooldownStatValue + " " + unit;
            if (hasTotal) totalStatValue = totalStatValue + " " + unit;
        }

        rowItem.setWarmUp(warmupStatValue);
        rowItem.setWorkout(workoutStatValue);
        rowItem.setCoolDown(cooldownStatValue);
        rowItem.setTotal(totalStatValue);
        rowItem.setDivider(item.isDivider);
        return rowItem;
    }

    private void displaySpecialSummaryInfo() {
        FitnessTestResultHelper helper = new FitnessTestResultHelper(getContext(), mSummaryData);
        int workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);
        switch (workoutId) {
            case WorkoutIds.COOPER_FITNESS_TEST:
                mFitnessTestSummaryLayout.setVisibility(View.VISIBLE);
                String ratingText = getString(R.string.rating_tc) + ": " + helper.getCooperRating();
                mRatingTextView.setText(ratingText);
                String lastStageText = getString(R.string.last_stage_completed_tc) + ": " + helper.getCooperStagesCompleted();
                mLastStageTextView.setText(lastStageText);
                break;
            case WorkoutIds.GERKIN_FITNESS_TEST:
                mFitnessTestSummaryLayout.setVisibility(View.VISIBLE);
                mLastStageTextView.setVisibility(View.GONE);
                mRatingTextView.setText(helper.getGerkinRating());
                break;
            case WorkoutIds.MARINES_FITNESS_TEST:
            case WorkoutIds.ARMY_FITNESS_TEST:
                mFitnessTestSummaryLayout.setVisibility(View.VISIBLE);
                mLastStageTextView.setVisibility(View.GONE);
                mRatingTextView.setText(helper.getScoredFitnessTestResult(100));
                break;
            case WorkoutIds.PEB_FITNESS_TEST:
                mFitnessTestSummaryLayout.setVisibility(View.VISIBLE);
                mLastStageTextView.setVisibility(View.GONE);
                mRatingTextView.setText(helper.getScoredFitnessTestResult(95));
                break;
            case WorkoutIds.CPAT_FITNESS_TEST:
                mFitnessTestSummaryLayout.setVisibility(View.VISIBLE);
                mLastStageTextView.setVisibility(View.GONE);
                mRatingTextView.setText(helper.getCPATTestResult(100));
                break;
            case WorkoutIds.AIR_FORCE_FITNESS_TEST:
                mFitnessTestSummaryLayout.setVisibility(View.VISIBLE);
                mLastStageTextView.setVisibility(View.GONE);
                mRatingTextView.setText(helper.getAirForceTestResult());
                break;
            case WorkoutIds.NAVY_FITNESS_TEST:
                mFitnessTestSummaryLayout.setVisibility(View.VISIBLE);
                mLastStageTextView.setVisibility(View.GONE);
                mRatingTextView.setText(helper.getNavyTestResult());
                break;
            case WorkoutIds.WFI_FITNESS_TEST:
                mFitnessTestSummaryLayout.setVisibility(View.VISIBLE);
                String wfiStatusText = helper.getWfiStatus();
                mLastStageTextView.setText(wfiStatusText);
                mRatingTextView.setVisibility(View.GONE);
                break;
            case WorkoutIds.SUBMAX_FITNESS_TEST:
                mFitnessTestSummaryLayout.setVisibility(View.VISIBLE);
                mLastStageTextView.setVisibility(View.GONE);
                mRatingTextView.setText(helper.getSubmaxRating());
                break;
            case WorkoutIds.FTP_TEST_8:
            case WorkoutIds.FTP_TEST_20:
                mFitnessTestSummaryLayout.setVisibility(View.VISIBLE);
                mLastStageTextView.setVisibility(View.GONE);
                mRatingTextView.setText(helper.getFtpScore());
                break;
            case WorkoutIds.MAX_HR_TEST:
                mFitnessTestSummaryLayout.setVisibility(View.VISIBLE);
                mLastStageTextView.setVisibility(View.GONE);
                mRatingTextView.setText(helper.getMaxHRScore());
                break;
            case WorkoutIds.STEPPER_FITNESS_TEST:
                mFitnessTestSummaryLayout.setVisibility(View.VISIBLE);
                mLastStageTextView.setVisibility(View.GONE);
                mRatingTextView.setText(helper.getStepperRating());
                break;
        }
    }

    private void displayMyZoneSummaryInfo(View view) {
        TimeInZones timeInZones = DataMatrix.getParcelable(HALKeys.DATA_MYZONE_TIME_IN_ZONES_PARCELABLE, TimeInZones.class);
        if(DataMatrix.getBoolean(HALKeys.DATA_HAVE_MYZONE_TIME_IN_ZONES_BOOL) && timeInZones != null) {
            mMyZoneSummaryLayout.setVisibility(View.VISIBLE);
            //Calories
            TextView v = view.findViewById(R.id.myzone_calories_val);
            v.setText(String.format(Locale.ENGLISH, "%d", DataMatrix.getInt(HALKeys.DATA_MYZONE_CALORIES_INT, (int)mSummaryData.workoutSummary.statsAccumulated.calories)));
            //set Effort
            v = view.findViewById(R.id.myzone_effort_val);
            v.setText(String.format(Locale.ENGLISH, "%d%%", DataMatrix.getInt(HALKeys.DATA_MYZONE_EFFORT_INT)));
            //set MEPs
            v = view.findViewById(R.id.myzone_meps_val);
            v.setText(String.format(Locale.ENGLISH, "%d%%", DataMatrix.getInt(HALKeys.DATA_MYZONE_MEPS_INT)));
            //set average heartrate -- todo confirm that an average is not be derived from data retrieved myz
            v = view.findViewById(R.id.myzone_average_hr_val);
            v.setText(FloorFormat.format(mSummaryData.workoutSummary.statsAverages.averageHeartRate, 0));
            //set peak heartrate -- todo confirm
            v = view.findViewById(R.id.myzone_peak_hr_val);
            v.setText(String.valueOf(mSummaryData.workoutSummary.statsPeaks.peakHeartRate));
            //set duration
            v = view.findViewById(R.id.myzone_duration_val);
            v.setText(String.format(Locale.ENGLISH, "%s %s", String.valueOf(getMyZoneDuration(timeInZones)), getResources().getString(R.string.min_abbrev_lc)));
            setTimeAndZones(view, timeInZones);
        }
    }

    private int getMyZoneDuration(TimeInZones t) {
        int totalSeconds = 0;
        for(int x : t.timeInZone()) {
            totalSeconds += x;
        }
        return (int)Math.round(totalSeconds/60);
    }

    private int getMyZoneMets(TimeInZones t) {
        int retValue = 0;
        for(int x : t.mepsInZone()) {
            retValue += x;
        }
        return retValue;
    }

    private String getMepsAndTime(TimeInZones t, int level){
        int meps = t.mepsInZone()[level-1];
        int seconds = t.timeInZone()[level-1];
        StringBuilder sb = new StringBuilder();
        int q = seconds/3600;
        sb.append(String.format(Locale.ENGLISH, "%02d:%02d:%02d", seconds/3600, (seconds - q*3600) % 60, (seconds - (q*3600))/60 ));
        sb.append(" - ");
        sb.append(String.format(Locale.ENGLISH, "%d ", meps));
        sb.append(ContextUtil.getString(R.string.meps_tc));
        return sb.toString();
    }

    private void setTimeAndZones(View view, TimeInZones t) {
        TextView v = view.findViewById(R.id.mz_lvl1_data);
        v.setText(getMepsAndTime(t, 1));
        (v = view.findViewById(R.id.mz_lvl2_data)).setText(getMepsAndTime(t, 2));
        (v = view.findViewById(R.id.mz_lvl3_data)).setText(getMepsAndTime(t, 3));
        (v = view.findViewById(R.id.mz_lvl4_data)).setText(getMepsAndTime(t, 4));
        (v = view.findViewById(R.id.mz_lvl5_data)).setText(getMepsAndTime(t, 5));
        (v = view.findViewById(R.id.mz_lvl6_data)).setText(getMepsAndTime(t, 6));
    }

}
