package com.jht.chimera.service.logger;

import android.os.SystemClock;
import android.util.Log;

import com.google.gson.Gson;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.analytic.AnalyticsUtil;
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys;
import com.jht.chimera.lib.common.stats.RunningAverageDuration;
import com.jht.datamatrixlib.DataMatrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Manages the recording and logging of performance metrics.
 */
public class PerformanceMetricsManager {
    private static final String TAG = "PerformanceMetricsManager";
    private static final String METRICS_DIRECTORY = AnalyticsUtil.STATS_FOLDER;
    private static final String METRICS_FILE = "performance_metrics.json";
    private DataMatrix mDataMatrix;
    private Boolean isRunning = false;
    private PerformanceMetrics metrics = new PerformanceMetrics();

    private PerformanceMetricsManager(){}

    private static class SingletonHolder {
        private static final PerformanceMetricsManager INSTANCE = new PerformanceMetricsManager();
    }

    /**
     * Provides access to the singleton PerformanceMetricsManager object.
     * @return the PerformanceMetricsManager singleton
     */
    public static PerformanceMetricsManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Starts recording and logging performance metrics.
     */
    public void start() {
        if (isRunning) return;
        isRunning = true;
        mDataMatrix = new DataMatrix();
        loadMetrics();
        initDataMatrixListeners();
        Log.d(TAG, "PerformanceMetricsManager started.");
    }

    /**
     * Stops recording and logging performance metrics.
     */
    public void stop() {
        isRunning = false;
        mDataMatrix.finish();
        Log.d(TAG, "PerformanceMetricsManager stopped.");
    }

    /**
     * Resets all recorded performance metrics data.
     */
    public void resetAllData() {
        metrics = new PerformanceMetrics();
        saveMetrics();
        Log.d(TAG, "Performance metrics reset.");
    }

    private void initDataMatrixListeners(){
        // Record Boot Time Performance Metric
        mDataMatrix.createEventAdapter(LoggerKeys.ACTION_RECORD_BOOT_TIME, value -> {
            long bootTimeDuration = SystemClock.elapsedRealtime();
            if (DataMatrix.getBoolean(LoggerKeys.DATA_BOOT_TIME_RECORDED_BOOL)) return;
            DataMatrix.putBoolean(LoggerKeys.DATA_BOOT_TIME_RECORDED_BOOL, true);
            metrics.avgBootTime.addDuration(bootTimeDuration);
            saveMetrics();
            Log.d(TAG, "Recorded boot time: (" + bootTimeDuration + " ms)");
        });
    }

    private void loadMetrics(){
        this.metrics = getMetrics();
    }

    private void saveMetrics() {
        try {
            // Make sure directory exists
            File metricsFile = new File(METRICS_DIRECTORY);
            if (!metricsFile.exists()){
                ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
                executeAsRoot.addCommand("mkdir -p " + METRICS_DIRECTORY);
                executeAsRoot.execute();
            }

            // Serialize performance metrics
            Gson gson = new Gson();
            String json = gson.toJson(metrics);

            // Write performance metrics to file
            metricsFile = new File(METRICS_DIRECTORY + METRICS_FILE);
            FileOutputStream fOut = new FileOutputStream(metricsFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(json);
            myOutWriter.close();
            fOut.flush();
            fOut.close();
            Log.d(TAG, "Saved performance metrics ==> " + json);
        } catch (Exception e) {
            Log.d(TAG, "saveMetrics failed -> " + e);
        }
    }

    /**
     * Returns the current performance metrics recorded thus far.
     * @return the current performance metrics recorded.
     */
    public static PerformanceMetrics getMetrics(){
        try {
            // Make sure directory and file exists
            File metricsFile = new File(METRICS_DIRECTORY + METRICS_FILE);
            if (!metricsFile.exists()) return new PerformanceMetrics();

            // Load performance metrics json
            StringBuilder json = new StringBuilder();
            FileInputStream fIn = new FileInputStream(metricsFile);
            InputStreamReader myInReader = new InputStreamReader(fIn);
            BufferedReader bufferedReader = new BufferedReader(myInReader);
            String line;
            while ((line = bufferedReader.readLine()) != null){
                json.append(line);
            }
            bufferedReader.close();
            myInReader.close();
            fIn.close();

            // Deserialize performance metrics json
            Gson gson = new Gson();
            PerformanceMetrics loadedMetrics = gson.fromJson(json.toString(), PerformanceMetrics.class);
            if (loadedMetrics == null) loadedMetrics = new PerformanceMetrics();
            Log.d(TAG, "Deserialized performance metrics ==> " + gson.toJson(loadedMetrics));

            return loadedMetrics;

        } catch (Exception e) {
            Log.d(TAG, "getMetrics failed -> " + e);
            return new PerformanceMetrics();
        }
    }

    /**
     * Stores all the currently tracked performance metrics.
     */
    public static class PerformanceMetrics {
        public RunningAverageDuration avgBootTime = new RunningAverageDuration(45);
    }
}
