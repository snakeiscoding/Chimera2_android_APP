package com.jht.chimera.lib.common.performance;

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.lib.common.math.RunningAverageHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Records processor statistics for testing.
 */
public class SystemStats {
    private static final String TAG = "PerformanceStat";
    private JHTTimer timer = new JHTTimer();
    private List<PerformanceStat> mPerformanceStats = new ArrayList<>();
    private Context mContext;
    private boolean isRecording = false;

    public SystemStats(Context context) {
        mContext = context;
        timer.setOnTimer(() -> mPerformanceStats.add(getCurrentData()));
    }

    /**
     * Starts recording processor stats. Starting recording will clear previous recordings.
     */
    public void startRecording(int timeIntervalMs) {
        if (isRecording) return; // return if we are already recording
        timer.start(0, timeIntervalMs);
        isRecording = true;
    }

    /**
     * Stops recording. The data will still be available until starting the next recording.
     */
    public void stopRecording() {
        timer.stop();
        isRecording = false;
    }

    /**
     * Gets the currently recorded processor data.
     *
     * @return The processor data
     */
    public List<PerformanceStat> getRecordedData() {
        return mPerformanceStats;
    }

    /**
     * Finds the highest spikes of performance and memory usage in the current recorded data.
     *
     * @return a string for logging purposes
     */
    public String analyzeCurrentData() {
        RunningAverageHelper averageMemory = new RunningAverageHelper();
        RunningAverageHelper averageCpu = new RunningAverageHelper();
        int highestCpuUsage = 0;
        int highestMemUsage = 0;
        PerformanceStat lastStat = null;
        for (PerformanceStat stat : mPerformanceStats) {
            if (lastStat != null) {
                int cpuUsage = stat.getCPUUsage(lastStat);
                averageCpu.addValue(cpuUsage);
                if (cpuUsage > highestCpuUsage) {
                    highestCpuUsage = cpuUsage;
                }
            }

            averageMemory.addValue(stat.memoryUsagePercent);
            if (stat.memoryUsagePercent > highestMemUsage) {
                highestMemUsage = stat.memoryUsagePercent;
            }

            lastStat = stat;
        }

        return "Average memory usage: " + averageMemory.getValue() + // NON-NLS
                "%\nAverage CPU usage: " + averageCpu.getValue() + // NON-NLS
                "%\nPeak memory usage: " + highestMemUsage + // NON-NLS
                "%\nPeak CPU usage: " + highestCpuUsage + "%"; // NON-NLS
    }

    /**
     * Gets current data for processor and memory
     * @return The current processor statistics
     */
    public PerformanceStat getCurrentData() {
        BufferedReader reader;
        PerformanceStat processorStat = new PerformanceStat();
        try {
            reader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("cat /proc/stat").getInputStream()));
            while (true) {
                String cpuLine = reader.readLine();
                if (cpuLine.startsWith("cpu")) { //NON-NLS
                    String[] parts = cpuLine.split("\\s+"); //NON-NLS
                    if (parts.length >= 8) {
                        processorStat.user = Long.parseLong(parts[1]);
                        processorStat.nice = Long.parseLong(parts[2]);
                        processorStat.system = Long.parseLong(parts[3]);
                        processorStat.idle = Long.parseLong(parts[4]);
                        processorStat.iowait = Long.parseLong(parts[5]);
                        processorStat.irq = Long.parseLong(parts[6]);
                        processorStat.softirq = Long.parseLong(parts[7]);
                    }

                    processorStat.systemTime = System.currentTimeMillis();
                    processorStat.memoryUsagePercent = getMemoryData();
                    return processorStat;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "IO Exception while getting processor stats: " + e.getMessage());
            e.printStackTrace();
        }

        Log.e(TAG, "Did not read processor statistics correctly");
        return processorStat;
    }

    private int getMemoryData() {
        // Get the system memory usage.
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        // Get percent available memory
        return (int) ((mi.totalMem - mi.availMem) * 100 / mi.totalMem);
    }


    /**
     * This is used to read the CPU stats from /proc/stat.  We can take two objects and figure out
     * what the actual CPU usage is in percentage.
     */
    public class PerformanceStat {
        public long systemTime;
        public int memoryUsagePercent;

        // These are the CPU usage times read from /proc/stat.
        private long user;
        private long nice;
        private long system;
        private long idle;
        private long iowait;
        private long irq;
        private long softirq;

        /**
         * @return The total CPU time adding up all fields.
         */
        long getTotalTime() {
            return user + nice + system + idle + iowait + irq + softirq;
        }


        /**
         * @return The idle CPU time.
         */
        long getIdleTime() {
            return idle;
        }

        /**
         * @param previousStat The previous cpu usage stat reading.
         * @return The amount of time the processor spent doing work from the previous stat.
         */
        int getCPUUsage(PerformanceStat previousStat) {
            long time = getTotalTime() - previousStat.getTotalTime();
            long idle = getIdleTime() - previousStat.getIdleTime();

            if (time == 0 || idle == 0) {
                return 0;
            }
            return (int) ((time - idle) * 100 / time);
        }
    }
}
