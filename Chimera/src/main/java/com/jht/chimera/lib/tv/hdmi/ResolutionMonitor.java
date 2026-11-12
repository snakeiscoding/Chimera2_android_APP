package com.jht.chimera.lib.tv.hdmi;

import android.os.Looper;
import android.os.Handler;
import android.util.Log;

import com.jht.chimera.service.hal.lib.hardware.Driver;

public class ResolutionMonitor {
    private static final String TAG = "HdmiResolutionMonitor";

    private final long pollIntervalMillis;
    private final Handler handler;
    private final Runnable monitorRunnable;

    private HdmiInputInfo lastKnownHdmiInfo;
    private boolean isMonitoring = false;
    private HdmiInputInfo currentHdmiInfo;

    private OnHdmiInfoChangedListener hdmiInfoChangedListener;
    public void setOnHdmiInfoChangedListener(OnHdmiInfoChangedListener listener) {
        this.hdmiInfoChangedListener = listener;
    }

    public ResolutionMonitor(long pollIntervalMillis) {
        this.pollIntervalMillis = pollIntervalMillis;
        this.handler = new Handler(Looper.getMainLooper());
        this.monitorRunnable = new Runnable() {
            @Override
            public void run() {
                fetchAndProcessHdmiInfo();
                handler.postDelayed(this, ResolutionMonitor.this.pollIntervalMillis);
            }
        };
    }

    public ResolutionMonitor() {
        this(2000L);
    }

    public void startMonitoring() {
        if (isMonitoring) {
            Log.d(TAG, "Monitoring is already active.");
            return;
        }
        Log.i(TAG, "Starting HDMI resolution monitoring. Interval: " + pollIntervalMillis + " ms");
        isMonitoring = true;
        handler.post(monitorRunnable);
    }

    public void stopMonitoring() {
        Log.i(TAG, "Stopping HDMI resolution monitoring.");
        isMonitoring = false;
        handler.removeCallbacks(monitorRunnable);
        lastKnownHdmiInfo = null;
        currentHdmiInfo = null;
        Driver.closeDeviceHDMI();
    }

    private void fetchAndProcessHdmiInfo() {
        HdmiInputInfo info = fetchCurrentHdmiInfoFromDriver();
        if (info != null) {
            if (lastKnownHdmiInfo == null ||
                    info.getWidth() != lastKnownHdmiInfo.getWidth() ||
                    info.getHeight() != lastKnownHdmiInfo.getHeight() ||
                    info.getFrameRate() != lastKnownHdmiInfo.getFrameRate() ||
                    info.isInterlaced() != lastKnownHdmiInfo.isInterlaced() ||
                    info.isConnected() != lastKnownHdmiInfo.isConnected()) {

                Log.i(TAG, "HDMI Info changed. New: " + info + ", Old: " + lastKnownHdmiInfo);

                if (info != lastKnownHdmiInfo) {
                    hdmiInfoChangedListener.onHdmiInfoChanged(info, lastKnownHdmiInfo);
                }
                lastKnownHdmiInfo = info;
                currentHdmiInfo = info;
                Log.d(TAG, "currentHdmiInfo: " + currentHdmiInfo);
            }
            else {
                Log.i(TAG, "resolution has not change:　" + currentHdmiInfo);
            }
        } else {
            if (lastKnownHdmiInfo == null || lastKnownHdmiInfo.isConnected()) {
                Log.w(TAG, "Failed to fetch HDMI info or HDMI disconnected.");
                lastKnownHdmiInfo = new HdmiInputInfo(0, 0, 0, 0, false, 0, false);
                currentHdmiInfo = lastKnownHdmiInfo;
            }
        }
    }

    private HdmiInputInfo fetchCurrentHdmiInfoFromDriver() {
        try {
            int fd = Driver.openDeviceHDMI();
            if (fd >= 0) {
                int[] formatNativeArray = Driver.getFormatNative();
                int frameRate = Driver.getFrameRate();
                boolean interlaced = Driver.isInterlaced() == 1;
                Driver.closeDeviceHDMI();

                if (formatNativeArray != null && formatNativeArray.length == 3) {
                    int width = formatNativeArray[0];
                    int height = formatNativeArray[1];
                    boolean connected = formatNativeArray[2] == 1;

                    return new HdmiInputInfo(width, height, frameRate, connected, interlaced);
                } else {
                    Log.w(TAG, "Monitor: Failed to get valid HDMI format array.");
                    return null;
                }
            } else {
                Log.e(TAG, "Monitor: Failed to open HDMI device. fd: " + fd);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Monitor: Exception fetching HDMI info: " + e.getMessage(), e);
            return null;
        }
    }

    public HdmiInputInfo getCurrentHdmiInfo() {
        return currentHdmiInfo;
    }
}
