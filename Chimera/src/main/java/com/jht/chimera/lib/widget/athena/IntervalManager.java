package com.jht.chimera.lib.widget.athena;

import static com.jht.chimera.lib.common.context.ContextUtil.getString;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.formatting.DistanceFormat;
import com.jht.chimera.lib.common.formatting.TimeFormat;
import com.jht.chimera.lib.common.stats.WorkoutSummary;
import com.jht.chimera.lib.common.stats.WorkoutSummaryList;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IntervalManager
{
    private RecyclerView recyclerView;
    private List<IntervalViewDataHolder> intervalDataList;
    private IntervalViewAdapter adapter;
    private int intervalCounter;
    private TextView currentTimeCountdownTV;
    private TextView currentDistanceTV;
    private TextView intervalNumberTextID;

    public IntervalManager (View parentView, int recyclerViewId)
    {
        recyclerView = parentView.findViewById(recyclerViewId);
        recyclerView.setHasFixedSize(true);
        recyclerView.setBackgroundResource(R.drawable.background_intervals_gradient);

        currentTimeCountdownTV = parentView.findViewById (R.id.timeCountdownTimeID);
        currentDistanceTV = parentView.findViewById (R.id.distanceNumericID);
        intervalNumberTextID = parentView.findViewById(R.id.intervalNumberTextID);

        intervalDataList = IntervalViewDataHolder.createIntervalDataList();

        adapter = new IntervalViewAdapter(intervalDataList);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(parentView.getContext()));
        intervalCounter = 1;
    }

    public void updateDisplay (float time, String value, String valueLabel)
    {
        int seconds = (int) time;
        currentDistanceTV.setText (createDistanceUnitString(value, valueLabel));
        currentTimeCountdownTV.setText (String.format (Locale.ENGLISH,"%02d:%02d:%02d", (int)(seconds/60)/60, ((int)(seconds/60)) % 60, (int)seconds%60));
        intervalNumberTextID.setText(intervalNumberTextID.getContext().getString(R.string.interval_tc).toUpperCase() + " " + intervalCounter);
    }

    public void updateIntervals(WorkoutSummaryList intervals, boolean forceUpdate) {
        // Add all intervals in cooldown
        int workoutStage = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT);

        if (!forceUpdate) { // check if we need to update the intervals
            if (workoutStage == WorkoutKeys.Constants.WORKOUT_STAGE_COOLDOWN) {
                if (intervalCounter == (intervals.workoutSummaries.size() + 1)) return;
            } else {
                // Do nothing if we don't have new intervals
                if (intervalCounter == intervals.workoutSummaries.size()) return;
            }
        }

        intervalDataList.clear();

        List<IntervalViewDataHolder> intervalDataListOrdered = new ArrayList<>();

        // Add all intervals when we're in cooldown
        int sizeToCheck;
        if (workoutStage == WorkoutKeys.Constants.WORKOUT_STAGE_COOLDOWN) {
            sizeToCheck = intervals.workoutSummaries.size();
        } else {
            sizeToCheck = intervals.workoutSummaries.size() - 1;
        }

        // We only want to show completed intervals, so ignore the last interval summary
        for (int i = 0; i < sizeToCheck; i++) {
            WorkoutSummary interval = intervals.workoutSummaries.get(i);

            int timeInInterval = Math.round(interval.statsAccumulated.secondsStateRun);
            String secondsString = TimeFormat.formatSeconds(timeInInterval);
            String distance = DistanceFormat.formatWorkoutDistance(interval.statsAccumulated.distanceKm);
            String distanceUnit = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL) ? getString(R.string.km_uc) : getString(R.string.mi_uc);

            SpannableStringBuilder distanceString = createDistanceUnitString(distance, distanceUnit);
            IntervalViewDataHolder intervalData = new IntervalViewDataHolder(i + 1, secondsString, distanceString, "");
            intervalDataListOrdered.add(intervalData);
        }

        // Add in reverse order
        for (int i = (intervalDataListOrdered.size() - 1); i >= 0; i--) {
            intervalDataList.add(intervalDataListOrdered.get(i));
        }

        intervalCounter = intervalDataList.size() + 1; // We want this to start at 1 for better numbering

        recyclerView.scrollToPosition(0); // index 0 position
        adapter.notifyDataSetChanged();
    }

    private SpannableStringBuilder createDistanceUnitString (String distance, String unit)
    {
        SpannableString ssDistance=  new SpannableString(distance);
        ssDistance.setSpan(new RelativeSizeSpan(1.0f), 0, ssDistance.length(), 0);
        SpannableString ssUnit=  new SpannableString(unit);
        ssUnit.setSpan(new RelativeSizeSpan(.70f), 0, ssUnit.length(), 0);
        ssUnit.setSpan(new ForegroundColorSpan(Color.parseColor("#a3a3a3")), 0, ssUnit.length(), 0);

        SpannableStringBuilder ssb = new SpannableStringBuilder();

        ssb.append (ssDistance);
        ssb.append (ssUnit);
        return ssb;
    }

    public int size ()
    {
        return intervalDataList.size();
    }

    public void resetIntervals()
    {
        // Need to reset interval counter to 1 for each time interval recycler view reloads
        intervalCounter = 1;
        intervalDataList.clear();
        DataMatrix.putInt(WorkoutKeys.DATA_STAGE_INTERVAL_NUM_INT, 0);
    }
}
