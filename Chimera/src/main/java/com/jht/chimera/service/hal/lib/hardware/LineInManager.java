package com.jht.chimera.service.hal.lib.hardware;

import android.util.Log;

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

public class LineInManager {

    public final String TAG = "LineInManager";
    boolean mIsLineInConnected = false;
    private int mLineInDetGpio;

    public static LineInManager getInstance() {
        return LineInManager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final LineInManager INSTANCE = new LineInManager();
    }

    private LineInManager() {
        mLineInDetGpio = Driver.GpioWrapper.getGpioLineInDetect();  // var could be static final ?
    }

    public void start() {
        // hardware does not define a GPIO for this
        if (0 > mLineInDetGpio) {
            Log.w(TAG, "No GPIO is defined for Line In Detection.");
            return;
        }

        Driver.exportGpio(mLineInDetGpio, Driver.GpioDirection.in, Driver.GpioEdge.both);
        if (!Driver.isExportedGpio(mLineInDetGpio)) {
            Log.e(TAG, "GPIO failed to export as " + mLineInDetGpio);
            return;
        }

        updateLineInStatus(0);

        new Thread(() -> {
            // monitor the line in status
            while (true) {
                updateLineInStatus(-1);
                // have seen some cases where this can spam, so let's put a limiter in this loop
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ignored) {
                    //
                }
            }
        }, "LineInManager").start();
    }

    boolean updateLineInStatus(int timeoutMs) {
        int value = Driver.pollGpio(mLineInDetGpio, timeoutMs, 100); // PRKS-2808: 100 us delay for simple debounce

        if (value == 49) { // '1' in ascii code
            mIsLineInConnected = true;
            if (timeoutMs == -1) { // if the timeout is infinite
                // increment insertion count
                int count = DataMatrix.getInt(SettingKeys.Hardware.DATA_HEADPHONE_A_COUNT_INT);
                count++;
                DataMatrix.putInt(SettingKeys.Hardware.DATA_HEADPHONE_A_COUNT_INT, count);
            }
        } else if (value == 48) { // '0' in ascii code
            mIsLineInConnected = false;
        }
        Log.e(TAG, "updateLineInStatus: " + mIsLineInConnected);
        return value != -1;
    }

}
