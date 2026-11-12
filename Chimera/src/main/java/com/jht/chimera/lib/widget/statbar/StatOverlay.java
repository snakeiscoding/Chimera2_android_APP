package com.jht.chimera.lib.widget.statbar;

import static com.jht.chimera.lib.common.datamatrixkeys.HALKeys.STATUS_GYM_KIT_CONNECTED;
import static com.jht.chimera.lib.common.datamatrixkeys.HALKeys.STATUS_GYM_KIT_OUT_OF_RANGE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.widget.statarray.StatManager;
import com.jht.datamatrixlib.DMCallback;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.List;

public class StatOverlay extends LinearLayout {
    private static final int WARMUP_COLOR = 0xffE1261C;
    private static final int COOLDOWN_COLOR = 0xff31719e;

    private StatManager mStatManager;
    private StatOverlayItem mGymKitStat;
    private final DataMatrix mDataMatrix = new DataMatrix();

    private StatOverlayColumn mColumn0;
    private StatOverlayColumn mColumn1;
    private StatOverlayColumn mColumn2;
    private StatOverlayColumn mColumn3;
    private StatOverlayColumn mColumn4;

    public StatOverlay(Context context) {
        super(context);
        initialize(context);
    }

    public StatOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    private void initialize(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.overlay_stat_bar, this);

        mColumn0 = findViewById(R.id.overlay_stat_column_0);
        mColumn1 = findViewById(R.id.overlay_stat_column_1);
        mColumn2 = findViewById(R.id.overlay_stat_column_2);
        mColumn3 = findViewById(R.id.overlay_stat_column_3);
        mColumn4 = findViewById(R.id.overlay_stat_column_4);
    }

    /**
     * Starts tracking of stats on the stat overlay
     */
    public void start() {
        start(null);
    }

    /**
     * Starts tracking of stats on the stat overlay
     *
     * @param statCallback The callback for stat events
     */
    public void start(StatManager.IStatManagerNotifications statCallback) {
        List<List<StatOverlayItem>> statColumnViews = new ArrayList<>();
        statColumnViews.add(mColumn0.getStatItemViews());
        statColumnViews.add(mColumn1.getStatItemViews());
        statColumnViews.add(mColumn2.getStatItemViews());
        statColumnViews.add(mColumn3.getStatItemViews());
        statColumnViews.add(mColumn4.getStatItemViews());

        mStatManager = new StatManager();

        List<List<IStatView>> statViews = getStatViews(statColumnViews);
        mStatManager.setupStatColumns(statViews, statCallback);

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_WORKOUT_STAGE_INT, value -> {
            switch (value) {
                case WorkoutKeys.Constants.WORKOUT_STAGE_COOLDOWN:
                    statColumnViews.get(2).get(0).changeTextColor(COOLDOWN_COLOR, COOLDOWN_COLOR);
                    break;
                case WorkoutKeys.Constants.WORKOUT_STAGE_WARMUP:
                    statColumnViews.get(2).get(0).changeTextColor(WARMUP_COLOR, WARMUP_COLOR);
                    break;
                case WorkoutKeys.Constants.WORKOUT_STAGE_WORKOUT:
                    statColumnViews.get(2).get(0).changeTextColor(0xFFFFFFFF, 0xFFFFFFFF);
                    break;
            }
        });

        mDataMatrix.createIntAdapter(UiServiceKeys.DATA_COL_2_STAT_SELECTED_ORDER_INT, mGymKitStateCallback);
        mDataMatrix.createIntAdapter(HALKeys.DATA_GYM_KIT_STATUS_INT, mGymKitStateCallback);

        mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_CALORIES_FLOAT, mGymKitStatsCallback);
        mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_ACTIVE_CALORIES_FLOAT, mGymKitStatsCallback);
        mDataMatrix.createEventAdapter(HALKeys.EVENT_MACHINE_SETUP_COMPLETE, event -> mStatManager.setupStatColumns(statViews, statCallback) );
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == GONE || visibility == INVISIBLE) closeStats();
    }

    /**
     * Stops callbacks to the stat overlay
     */
    public void finish(){
        if (mStatManager != null) mStatManager.stopStatColumns();
        mDataMatrix.finish();
    }

    private List<List<IStatView>> getStatViews(List<List<StatOverlayItem>> vaStatViews) {
        List<List<IStatView>> statViews = new ArrayList<>();
        for (List<StatOverlayItem> overlayStatColumn : vaStatViews) {
            List<IStatView> statColumn = new ArrayList<>(overlayStatColumn);
            statViews.add(statColumn);
        }

        return statViews;
    }

    /**
     * Closes open stat columns
     */
    public void closeStats() {
        if (mStatManager != null) mStatManager.closeStats();
    }

    /**
     * Updates the stat positions based on the values from the
     */
    public void updateStatPositions() {
        if (mStatManager != null) mStatManager.updateColumnIndexes();
        updateGymKitStatVisibility();
    }

    private void updateGymKitStatVisibility() {
        if (mGymKitStat != null) mGymKitStat.showNormalStat();
        mGymKitStat = (StatOverlayItem) mStatManager.getCaloriesStatView();
        if (mGymKitStat == null) return;
        int status = DataMatrix.getInt(HALKeys.DATA_GYM_KIT_STATUS_INT);
        if (status == STATUS_GYM_KIT_CONNECTED) {
            mGymKitStat.showSplitStat();
        } else if (status == STATUS_GYM_KIT_OUT_OF_RANGE) {
            mGymKitStat.showSplitStat();
        } else {
            mGymKitStat.showNormalStat();
        }
    }

    DMCallback<Integer> mGymKitStateCallback = value -> updateGymKitStatVisibility();

    DMCallback<Float> mGymKitStatsCallback = new DMCallback<Float>() {
        @SuppressLint("DefaultLocale")
        @Override
        public void dataMatrixCallback(Float value) {
            if (mGymKitStat == null) return;
            mGymKitStat.updateSplitValues(
                    String.format("%d", (int) DataMatrix.getFloat(WorkoutKeys.DATA_ACTIVE_CALORIES_FLOAT)),
                    String.format("%d", (int) DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT))
            );
        }
    };
}
