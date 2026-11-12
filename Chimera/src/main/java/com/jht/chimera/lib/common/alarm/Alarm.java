package com.jht.chimera.lib.common.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jht.chimera.lib.common.datetime.DateTimeUtil;

import java.util.Calendar;

public class Alarm {
    public interface IOnAlarm {
        void onAlarm();
    }
    private static final String TAG = "Alarm";
    private final Context context;
    private final IOnAlarm onAlarm;
    private final AlarmManager manager;
    private final PendingIntent alarmIntent;

    private final BroadcastReceiver alarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Alarm received");
            onAlarm.onAlarm();
        }
    };

    public Alarm(@NonNull Context context, @NonNull String name, @NonNull Calendar alarmTime, @NonNull IOnAlarm onAlarm) {
        this.context = context;
        this.onAlarm = onAlarm;
        alarmIntent = PendingIntent.getBroadcast(context, 0, new Intent(name), PendingIntent.FLAG_IMMUTABLE);
        manager = (AlarmManager) (context.getSystemService(Context.ALARM_SERVICE));

        if(manager == null) {
            // Probably should throw an error, but not sure what to really do here.
            Log.e(TAG, "Unable to get the alarm manager service to set alarm");
            return;
        }
        context.registerReceiver(alarmReceiver, new IntentFilter(name));
        long delay = alarmTime.getTimeInMillis() - System.currentTimeMillis();
        Log.d(TAG, "setting alarm for " + delay + " " + DateTimeUtil.getDate(alarmTime) + " " + DateTimeUtil.getTime(alarmTime));
        manager.set(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), alarmIntent);
    }

    public void destroy() {
        manager.cancel(alarmIntent);
        try {
            context.unregisterReceiver(alarmReceiver);
        }
        catch(Exception ignored) {}
    }
}
