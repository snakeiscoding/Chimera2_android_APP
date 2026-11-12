package com.jht.chimera.ui.machinesettings.pages;

import android.animation.Animator;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;

import androidx.recyclerview.widget.RecyclerView;

import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.analytic.AnalyticStats;
import com.jht.chimera.lib.common.analytic.AnalyticsUtil;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys;
import com.jht.chimera.lib.workoutapps.AppIcon;
import com.jht.chimera.lib.workoutapps.AppIconManager;
import com.jht.chimera.service.logger.PerformanceMetricsManager;
import com.jht.chimera.service.logger.PerformanceMetricsManager.PerformanceMetrics;
import com.jht.chimera.ui.machinesettings.adapters.analytic.AnalyticItem;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("HardCodedStringLiteral")
public class AnalyticsPage extends SettingPageBase {

    DataMatrix dataMatrix = new DataMatrix();
    private SelectorAdapterBase adapter;
    private RecyclerView table;
    private SelectorItemBase base;
    private Handler handler = new Handler();


    public AnalyticsPage() {
    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        View view = LayoutInflater.from(context).inflate(R.layout.analytics, null);
        container.addView(view);

        table = view.findViewById(R.id.analytics_table);
        SelectorDefaultAnimations.setDefaultAnimations(context, table);
        base = new SelectorItemBase(new ArrayList<>());
        adapter = new SelectorAdapterBase(getContext(), base, null);
        table.setAdapter(adapter);

        view.findViewById(R.id.reset_button).setOnClickListener(v -> {
            DataMatrix.sendEvent(LoggerKeys.ACTION_RESET_ANALYTICS);
            view.postDelayed(this::updateAdapter, 1000);
        });

        Button resetPTPButton = view.findViewById(R.id.reset_ptp_button);
        resetPTPButton.post(() -> resetPTPButton.setText((resetPTPButton.getText().toString() + " PTP")));
        resetPTPButton.setOnClickListener(v -> {
            DataMatrix.sendEvent(LoggerKeys.ACTION_RESET_PTP_ANALYTICS);
            view.postDelayed(this::updateAdapter, 1000);
        });

        RadioButton lcb_button = view.findViewById(R.id.lcb_communication_analytics_button);

        if (DataMatrix.getBoolean(HALKeys.DATA_SHOW_LCB_ANALYTICS_BOOL)) {
            lcb_button.setChecked(true);
            lcb_button.setSelected(true);
        } else {
            lcb_button.setChecked(false);
            lcb_button.setSelected(false);
        }

        lcb_button.setOnClickListener(v -> {
            if (!lcb_button.isSelected()) {
                lcb_button.setChecked(true);
                lcb_button.setSelected(true);
                DataMatrix.putBoolean(HALKeys.DATA_SHOW_LCB_ANALYTICS_BOOL, true);
                updateAdapterTask();

            } else {
                lcb_button.setChecked(false);
                lcb_button.setSelected(false);
                DataMatrix.putBoolean(HALKeys.DATA_SHOW_LCB_ANALYTICS_BOOL, false);
                updateAdapterTask();
            }
        });

        return view;
    }


    private void updateAdapter() {
        new Thread(this::updateAdapterTask).start();
    }

    private void updateAdapterTask() {
        AnalyticStats stats = AnalyticsUtil.loadStats();
        PerformanceMetrics performanceMetrics = PerformanceMetricsManager.getMetrics();

        base.removeAllChildren();
        base.addItem(new AnalyticItem("Total Workout", String.valueOf(stats.totalWorkouts)));
        base.addItem(new AnalyticItem("Earphone Insertions", String.valueOf(stats.headphoneConnectedCount)));
        base.addItem(new AnalyticItem("USB Insertions", String.valueOf(stats.usbAttachedCount)));

        switch (FrameConfigManager.getInstance().getFrameConfig().machineClassType) {
            case Treadmill:
                base.addItem(new AnalyticItem("Emergency Stop Toggle", String.valueOf(stats.treadmillSafetyKeyCount)));
                break;
            case Climbmill:
                base.addItem(new AnalyticItem("Emergency Stop Toggle", String.valueOf(stats.climbmillUCBCount)));
                base.addItem(new AnalyticItem("Handlebar E-Stop Toggle", String.valueOf(stats.climbmillHandrailCount)));
                base.addItem(new AnalyticItem("Control Zone Toggle", String.valueOf(stats.climbmillControlZoneCount)));
                base.addItem(new AnalyticItem("IR Sensor Toggle", String.valueOf(stats.climbmillIRSensorCount)));
                break;
        }

        if (ProductConfig.hasFan()) {
            base.addItem(new AnalyticItem("User Fan", String.valueOf(stats.fanClickCount)));
        }

        // add key press count
        for (Map.Entry<String, Long> entry : stats.keyPressCountMap.entrySet()) {
            String key = entry.getKey();
            Long value = entry.getValue();
            base.addItem(new AnalyticItem(key, String.valueOf(value)));
        }

        // add login types count
        for (Map.Entry<String, Long> entry : stats.loginTypeCountMap.entrySet()) {
            String key = entry.getKey();
            Long value = entry.getValue();
            base.addItem(new AnalyticItem(AnalyticsUtil.getDisplayLoginType(key), String.valueOf(value)));
        }

        base.addItem(new AnalyticItem("BT Headphone Pairs", String.valueOf(stats.btHeadphoneConnectedCount)));
        base.addItem(new AnalyticItem("BT Heart Monitor Pairs", String.valueOf(stats.heartRateConnectedCount)));
        if (BuildConfig.HAS_BT_SINK) {
            base.addItem(new AnalyticItem("BT Media Devices Pairs", String.valueOf(stats.btSinkConnectedCount)));
        }

        // add workout id count
        for (Map.Entry<String, Long> entry : stats.workoutIdMap.entrySet()) {
            String key = entry.getKey();
            Long value = entry.getValue();
            base.addItem(new AnalyticItem("Workout - " + key, String.valueOf(value)));
        }

        // add in app usage
        Map<String, AppIcon> appList = AppIconManager.getInstance().getAllApps();
        for (Map.Entry<String, AppIcon> entry : appList.entrySet()) {
            AppIcon appIcon = entry.getValue();
            Long clickCount = stats.workoutAppClickCountMap.get(appIcon.iconId);
            Long timeSec = stats.workoutActivityInfoTimeSecMap.get(appIcon.iconId);
            if (clickCount != null && timeSec != null) {
                String value = String.format(Locale.ENGLISH,
                        "%d (%s)", clickCount, AnalyticsUtil.formatTime(timeSec));
                base.addItem(new AnalyticItem("App - " + appIcon.getAppName(), value));
            }
        }


        //PTP Timings
        if (stats.ptpLoginCount > 0) {
            base.addItem(new AnalyticItem("PTP Login Count", String.valueOf(stats.ptpLoginCount)));
            base.addItem(new AnalyticItem("PTP Average Workout Count", String.valueOf(stats.ptpWorkoutCount * 1f / stats.ptpLoginCount)));
            base.addItem(new AnalyticItem("PTP Average Login Time", String.valueOf(stats.ptpTimingLogin / 1000f / stats.ptpLoginCount)));
            base.addItem(new AnalyticItem("PTP Average User Info Time", String.valueOf(stats.ptpTimingUserInfo / 1000f / stats.ptpLoginCount)));
            base.addItem(new AnalyticItem("PTP Average User Connections Time", String.valueOf(stats.ptpTimingUserConnections / 1000f / stats.ptpLoginCount)));
            base.addItem(new AnalyticItem("PTP Average Show UI", String.valueOf(stats.ptpTimingShowUI / 1000f / stats.ptpLoginCount)));
            base.addItem(new AnalyticItem("PTP Average API Cookie", String.valueOf(stats.ptpTimingAPICookie / 1000f / stats.ptpLoginCount)));
            base.addItem(new AnalyticItem("PTP Average API Workouts", String.valueOf(stats.ptpTimingAPIWorkouts / 1000f / stats.ptpLoginCount)));
            base.addItem(new AnalyticItem("PTP Average Display", String.valueOf(stats.ptpTimingDisplay / 1000f / stats.ptpLoginCount)));
            base.addItem(new AnalyticItem("PTP Average Total Time", String.valueOf(stats.ptpTimingComplete / 1000f / stats.ptpLoginCount)));
        }

        // Performance Metrics
        if (performanceMetrics.avgBootTime.getAverageDuration() != null){
            base.addItem(new AnalyticItem("Average Boot Time", performanceMetrics.avgBootTime.toString()));
        }

        handler.post(() -> adapter.notifyDataSetChanged());
    }


    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);
        updateAdapter();
        //dataMatrix.createByteArrayAdapter(LoggerKeys.DATA_ERROR_LOG_LIST_PARCELABLE, bytes -> updateAdapter());
    }

    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        dataMatrix.finish();
    }


}
