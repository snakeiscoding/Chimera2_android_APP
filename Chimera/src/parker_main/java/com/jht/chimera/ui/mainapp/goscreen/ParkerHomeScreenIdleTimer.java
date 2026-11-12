package com.jht.chimera.ui.mainapp.goscreen;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.timer.IdleTimerManagement;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.interfaces.IStartStop;

import java.util.Calendar;

public class ParkerHomeScreenIdleTimer implements IStartStop {
    private final String TAG = "ParkerHomeScreenIdle...";

    private static final String ERP_INTENT = "com.jht.erp"; //NON-NLS
    private final IdleTimerManagement mIdleTimerManagement = new IdleTimerManagement();
    private final Context mContext;
    private boolean mIsBroadcastReceiverRegistered;
    private PendingIntent mErpAlarmPendingIntent = null;

    public ParkerHomeScreenIdleTimer(Context context) {
        mContext = context;
    }

    @Override
    public void start() {
        mIsBroadcastReceiverRegistered = false;

        mIdleTimerManagement.clearTimers();

        if (DataMatrix.getBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL)) {
            // 2 minutes?
            mIdleTimerManagement.addTimer(new JHTTimer(2 * 1000 * 60, () -> {
                Log.i(TAG, "==> digital safety PIN timeout elapsed");
                DataMatrix.putBoolean(HALKeys.DATA_DIGITAL_SAFETY_PIN_PROMPT_NEEDED_BOOL, true);
            }));
        }

        if (DataMatrix.getBoolean(SettingKeys.Hardware.DATA_ENABLE_PROMOTIONAL_VIDEO_BOOL)) {
            mIdleTimerManagement.addTimer(new JHTTimer(DataMatrix.getInt(SettingKeys.Hardware.DATA_PROMOTIONAL_VIDEO_TIMEOUT_MIN_INT) * 1000 * 60, () -> {
                Log.i(TAG, "==> demo video timeout elapsed");
                LaunchAppUtil.launchInternalScreen(DemoVideoActivity.class.getCanonicalName(), null);
            }));
        }

        if (DataMatrix.getInt(SettingKeys.General.DATA_REBOOT_INACTIVE_TIME_HOUR_INT) > 0) {
            mIdleTimerManagement.addTimer(new JHTTimer(DataMatrix.getInt(SettingKeys.General.DATA_REBOOT_INACTIVE_TIME_HOUR_INT) * 1000 * 60 * 60, () -> {
                Log.i(TAG, "==> reboot timeout elapsed");
                DataMatrix.putString(HALKeys.ACTION_CONSOLE_REBOOT_STRING, "Inactive Timeout");
            }));
        }

        if (DataMatrix.getInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_MODE_ENUM_INT) == SettingKeys.ERPMode.Inactivity.ordinal()) {
            if (DataMatrix.getInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_TIMEOUT_MIN_INT) > 0) {
                mIdleTimerManagement.addTimer(new JHTTimer(DataMatrix.getInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_TIMEOUT_MIN_INT) * 1000 * 60, () -> {
                    Log.i(TAG, "==> erp timeout elapsed");
                    DataMatrix.sendEvent(HALKeys.ACTION_ENTER_ERP);
                }));
            }
        } else if (DataMatrix.getInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_MODE_ENUM_INT) == SettingKeys.ERPMode.Absolute_Time.ordinal()) {
            mContext.registerReceiver(mReceiver, new IntentFilter(ERP_INTENT));
            mIsBroadcastReceiverRegistered = true;
            setErpAlarm();
        }


        mIdleTimerManagement.monitorTimers();
    }

    @Override
    public void stop() {
        mIdleTimerManagement.stopMonitoring();

        if (mIsBroadcastReceiverRegistered) {
            try {
                mContext.unregisterReceiver(mReceiver);
            } catch (Exception ignored) {
            }
        }
        if (mErpAlarmPendingIntent != null) {
            AlarmManager manager = (AlarmManager) (mContext.getSystemService(Context.ALARM_SERVICE));
            manager.cancel(mErpAlarmPendingIntent);
        }
    }

    private long getNextAutoUpdateCheckTime(long currentTimeMillis, int triggerTimeMin) {
        Calendar currentCalendarTime = Calendar.getInstance();
        currentCalendarTime.setTimeInMillis(currentTimeMillis);
        Calendar updateTime = Calendar.getInstance();
        updateTime.setTimeInMillis(currentTimeMillis);
        updateTime.set(Calendar.HOUR_OF_DAY, triggerTimeMin / 60);
        updateTime.set(Calendar.MINUTE, triggerTimeMin % 60);
        updateTime.set(Calendar.SECOND, 0);
        updateTime.set(Calendar.MILLISECOND, 0);
        if (updateTime.before(currentCalendarTime)) {
            updateTime.add(Calendar.DAY_OF_YEAR, 1);
        }
        //Log.i(TAG, "Next auto update check is " + getAutoUpdateCheckHour(mAutoUpdateHour) + ":" + randomNum);
        return updateTime.getTimeInMillis();
    }

    private void setErpAlarm() {
        int triggerTimeMin = DataMatrix.getInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_TIME_MIN_INT);
        if (triggerTimeMin < 0) return;

        AlarmManager manager = (AlarmManager) (mContext.getSystemService(Context.ALARM_SERVICE));
        PendingIntent checkIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ERP_INTENT), PendingIntent.FLAG_IMMUTABLE);
        manager.cancel(checkIntent);

        long currentTime = System.currentTimeMillis();
        long nextCheckTime = getNextAutoUpdateCheckTime(currentTime, triggerTimeMin);
        long delayCheck = nextCheckTime - currentTime;

        Log.d(TAG, "Sending erp check in " + delayCheck + " ms");
        if (mErpAlarmPendingIntent == null) {
            mErpAlarmPendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ERP_INTENT), PendingIntent.FLAG_IMMUTABLE);
        }
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delayCheck, checkIntent);

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "==> onReceive " + intent);
            DataMatrix.sendEvent(HALKeys.ACTION_ENTER_ERP);
        }
    };
}
