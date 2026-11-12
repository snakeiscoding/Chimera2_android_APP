package com.jht.chimera.lib.common.timer;

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
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.ImportExportKeys;
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.i18n.LanguageProperty;
import com.jht.chimera.lib.common.i18n.LocaleUtil;
import com.jht.chimera.lib.common.testing.TestingUtils;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.interfaces.IStartStop;

import java.util.Calendar;

public class HomeScreenIdleTimer implements IStartStop {
    private static final String TAG = "HomeScreenIdleTimer";

    private static final String ERP_INTENT = "com.jht.erp"; //NON-NLS
    private final IdleTimerManagement mIdleTimerManagement = new IdleTimerManagement();
    private final Context mContext;
    private final boolean mIsHomeScreen;
    private boolean mIsBroadcastReceiverRegistered;
    private PendingIntent mErpAlarmPendingIntent = null;

    public HomeScreenIdleTimer(Context context, boolean isHomeScreen) {
        mContext = context;
        mIsHomeScreen = isHomeScreen;
    }

    @Override
    public void start() {
        if (TestingUtils.isAutomationEnabled()) {
            Log.d(TestingUtils.TAG, "Aborting HomeScreenIdleTimer start because automation is enabled.");
            return;
        }

        Log.d(TestingUtils.TAG, "Allowing the HomeScreenIdleTimer");
        DataMatrix.putBoolean(MainAppKeys.DATA_IS_SAFE_TO_UPDATE_BOOL, false);
        mIsBroadcastReceiverRegistered = false;
        Log.i(TAG, "==> " + mContext.getClass().getSimpleName() + " | mIsHomeScreen: " + mIsHomeScreen);

            if (mIsHomeScreen) {
                mIdleTimerManagement.clearTimers();

                int languageResetTimer = DataMatrix.getInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT);
                if (languageResetTimer <= 0) languageResetTimer = 120;
                mIdleTimerManagement.addTimer(new JHTTimer(languageResetTimer * 1000, () -> {
                    Log.i(TAG, "==> language timeout elapsed");
                    resetLanguage();
                }));


                mIdleTimerManagement.addTimer(new JHTTimer(30000, () -> {
                    Log.i(TAG, "==> safe to update timeout elapsed");
                    DataMatrix.putBoolean(MainAppKeys.DATA_IS_SAFE_TO_UPDATE_BOOL, true);
                }));
                if (DataMatrix.getInt(SettingKeys.General.DATA_REBOOT_INACTIVE_TIME_HOUR_INT) > 0) {
                    mIdleTimerManagement.addTimer(new JHTTimer(DataMatrix.getInt(SettingKeys.General.DATA_REBOOT_INACTIVE_TIME_HOUR_INT) * 1000 * 60 * 60, () -> {
                        DataMatrix.putString(HALKeys.ACTION_CONSOLE_REBOOT_STRING, "Inactive Timeout");
                    }));
                }

                if (DataMatrix.getInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_MODE_ENUM_INT) == SettingKeys.ERPMode.Inactivity.ordinal()) {
                    if (DataMatrix.getInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_TIMEOUT_MIN_INT) > 0) {
                        mIdleTimerManagement.addTimer(new JHTTimer(DataMatrix.getInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_TIMEOUT_MIN_INT) * 1000 * 60, () -> {
                            Log.d(TAG, "==> erp timeout elapsed");
                            DataMatrix.sendEvent(HALKeys.ACTION_ENTER_ERP);
                        }));
                    }
                } else if (DataMatrix.getInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_MODE_ENUM_INT) == SettingKeys.ERPMode.Absolute_Time.ordinal()) {
                    mContext.registerReceiver(mReceiver, new IntentFilter(ERP_INTENT));
                    mIsBroadcastReceiverRegistered = true;
                    setErpAlarm();
                }

                if (!DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL)) {
                    int screenTimeout = DataMatrix.getInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT);

                    // make the screen timeout for PTP always 3 minutes
                    if (DataMatrix.getBoolean(SettingKeys.General.DATA_IS_PTP_USER_BOOL)) {
                        screenTimeout = 3 * 60;
                    }

                    if (screenTimeout > 0) {
                        mIdleTimerManagement.addTimer(new JHTTimer(screenTimeout * 1000, () -> {
                            Log.i(TAG, "==> screen timeout elapsed");
                            DataMatrix.sendEvent(HALKeys.ACTION_DISCONNECT_SMART_WATCHES);
                            stop();
                            LaunchAppUtil.launchSplashScreen();
                        }));
                    }
                }

            } else {
                mIdleTimerManagement.clearTimers();
                int screenTimeout = DataMatrix.getInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT);

                // make the screen timeout for PTP always 3 minutes
                if (!DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL) && DataMatrix.getBoolean(SettingKeys.General.DATA_IS_PTP_USER_BOOL)) {
                    screenTimeout = 3 * 60;
                }

                if (screenTimeout > 0) {
                    mIdleTimerManagement.addTimer(new JHTTimer(screenTimeout * 1000, () -> {
                        Log.i(TAG, "==> screen timeout elapsed");
                        DataMatrix.sendEvent(HALKeys.ACTION_DISCONNECT_SMART_WATCHES);
                        stop();
                        LaunchAppUtil.launchSplashScreen();
                    }));
                }
            }
        mIdleTimerManagement.monitorTimers();
    }

    @Override
    public void stop() {
        if (TestingUtils.isAutomationEnabled()) {
            Log.d(TestingUtils.TAG, "Aborting HomeScreenIdleTimer stop() because in automation mode");
            return;
        }

        mIdleTimerManagement.stopMonitoring();
        DataMatrix.putBoolean(MainAppKeys.DATA_IS_SAFE_TO_UPDATE_BOOL, false);

        if (mIsBroadcastReceiverRegistered) {
            try {
                mContext.unregisterReceiver(mReceiver);
            }
            catch(Exception ignored) {}
        }
        if (mErpAlarmPendingIntent != null) {
            AlarmManager manager = (AlarmManager) (mContext.getSystemService(Context.ALARM_SERVICE));
            manager.cancel(mErpAlarmPendingIntent);
        }
    }

    public void resetLanguage(){
        LanguageProperty selectedLanguage = LanguageProperty.getLanguagePropertyFromIndex(DataMatrix.getInt(SettingKeys.General.DATA_SELECTED_LANGUAGE_ENUM_INT));
        LanguageProperty defaultLanguage = LanguageProperty.getLanguagePropertyFromIndex(DataMatrix.getInt(SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT));
        if (selectedLanguage != defaultLanguage) {
            LocaleUtil.changeLanguage(mContext, defaultLanguage, false);
        }

        // Reset units, clock, and date format.
        DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL));
        DataMatrix.putInt(ImportExportKeys.DATA_CURRENT_DATE_FORMAT_INT, DataMatrix.getInt(SettingKeys.General.DATA_DATE_FORMAT_ENUM_INT));
        DataMatrix.putBoolean(ImportExportKeys.DATA_CURRENT_CLOCK_FORMAT_IS_24_HOURS_BOOL, DataMatrix.getBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL));
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
            if (TestingUtils.isAutomationEnabled()) {
                Log.i(TAG, "Ignoring the request to transition to ERP because in Testing mode");
                return;
            }

            Log.d(TAG, "==> onReceive " + intent);
            boolean autoUpdatesEnabled = DataMatrix.getBoolean(SettingKeys.Update.DATA_AUTO_UPDATE_BOOL);
            boolean autoUpdateReady = DataMatrix.getBoolean(DAPIKeys.DATA_IS_AUTO_UPDATE_READY_BOOL);
            if (autoUpdatesEnabled && autoUpdateReady) {
                Log.d(TAG, "Erp time elapsed, starting auto update");
                DataMatrix.putBoolean(MainAppKeys.DATA_IS_SAFE_TO_UPDATE_BOOL, true);
            }
            else {
                Log.i(TAG, "==> erp time elapsed");
                DataMatrix.sendEvent(HALKeys.ACTION_ENTER_ERP);
            }
        }
    };
}
