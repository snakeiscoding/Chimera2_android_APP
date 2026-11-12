package com.jht.chimera.ui.mainapp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.stats.WorkoutSummaryList;
import com.jht.chimera.lib.graph.GraphLegendWidget;
import com.jht.chimera.lib.graph.workoutgraph.WorkoutGraphThreaded;
import com.jht.chimera.lib.widget.athena.IntervalManager;
import com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator;
import com.jht.datamatrixlib.DataMatrix;

public class BikeIntervalsActivity extends BaseWorkoutProfileActivity {

    // private static final String TAG = BikeIntervalsActivity.class.getName();
    private final DataMatrix dataMatrix = new DataMatrix();
    private String  distanceAbbrev = "";
    //private final ArrayList<WorkoutSplit> splits = new ArrayList<>();
    //private WorkoutSplit currentSplit;
    //private float unitConvert = 1f;

    private int runningWorkoutTime = 0;
    private IntervalManager intervalManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.interval_view);

        View rootView = getWindow().getDecorView().getRootView();

        intervalManager = new IntervalManager(rootView, R.id.intervalsView);

        TextView graphExpandContractButton = findViewById (R.id.graphExpandContractButton);
        WorkoutGraphThreaded mWorkoutGraph = findViewById(R.id.workout_graph);
        GraphLegendWidget leftLegend = findViewById(R.id.graph_left_legend_widget);
        GraphLegendWidget topWorkoutGraphLegend = findViewById(R.id.graph_top_legend_widget);
        ConstraintLayout graphLayout = findViewById(R.id.graphLayout);

        setGraphAndLegend(graphLayout, mWorkoutGraph, leftLegend, topWorkoutGraphLegend);

        intervalManager.resetIntervals();

        graphExpandContractButton.setOnClickListener(v -> {
            if (mWorkoutGraph.getVisibility() == View.VISIBLE) {
                setGraphVisibility(View.GONE);
                graphExpandContractButton.setBackgroundResource(R.drawable.icon_chart_show);
            }
            else {
                setGraphVisibility(View.VISIBLE);
                graphExpandContractButton.setBackgroundResource(R.drawable.icon_chart_hide);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        dataMatrix.createBoolAdapter(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, isMetric -> {
            distanceAbbrev = isMetric ? getString(R.string.km_uc) : getString(R.string.mi_uc);
            WorkoutSummaryList summaryList = DataMatrix.getParcelable(WorkoutKeys.DATA_INTERVAL_LIST_PARCELABLE, WorkoutSummaryList.class);
            intervalManager.updateIntervals(summaryList, true);
        });

        dataMatrix.createFloatAdapter(WorkoutKeys.DATA_DISTANCE_KM_FLOAT, value ->
                intervalManager.updateDisplay(
                        runningWorkoutTime,
                        getDistance(),
                        distanceAbbrev));

        dataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, value ->
                runningWorkoutTime = value/1000);

        dataMatrix.createParcelableAdapter(WorkoutKeys.DATA_INTERVAL_LIST_PARCELABLE, WorkoutSummaryList.class, intervalList -> {
            intervalManager.updateIntervals(intervalList, false);
        });
    }

    private String getDistance() {
        return StatValueUpdaterCreator.getInstance().getValue(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL) ? StatValueUpdaterCreator.DISTANCE_METRIC : StatValueUpdaterCreator.DISTANCE_IMPERIAL).getCurrentValue();
    }

    @Override
    public void onPause ()
    {
        super.onPause();
        dataMatrix.finish();
    }
}
