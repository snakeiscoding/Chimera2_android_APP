package com.jht.chimera.service.logger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.jht.chimera.lib.common.log.ServiceLog;

public class LoggerDataSource {
    private SQLiteDatabase database;
    private final LoggerDBOpener dbHelper;

    public LoggerDataSource(Context context) {
        dbHelper = new LoggerDBOpener(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void addServiceLog(ServiceLog log) {
        open();

        ContentValues values = new ContentValues();
        values.put("serviceType", log.serviceType);
        values.put("accumulatedDistanceKm", log.accumulatedDistanceKm);
        values.put("accumulatedTimeSec", log.accumulatedTimeSec);
        values.put("accumulatedFloor", log.accumulatedFloor);
        values.put("message", log.message);
        values.put("logTime", SQLiteHelper.dateToString(log.logTime));

        database.insert(LoggerDBOpener.SEREVICE_TABLE, null, values);

        close();

    }

    public ServiceLog[] getServiceLog() {
        ServiceLog[] logs = null;
        open();

        try(Cursor cursor = database.query(LoggerDBOpener.SEREVICE_TABLE, null, null, null, null, null, "logTime DESC")) {

            cursor.moveToFirst();

            if (cursor.getCount() > 0) {
                logs = new ServiceLog[cursor.getCount()];
                while (!cursor.isAfterLast()) {
                    logs[cursor.getPosition()] = new ServiceLog();
                    logs[cursor.getPosition()].serviceType = cursor.getInt(1);
                    logs[cursor.getPosition()].accumulatedDistanceKm = cursor.getDouble(2);
                    logs[cursor.getPosition()].accumulatedTimeSec = cursor.getInt(3);
                    logs[cursor.getPosition()].accumulatedFloor = cursor.getInt(4);
                    logs[cursor.getPosition()].message = cursor.getString(5);
                    logs[cursor.getPosition()].logTime = SQLiteHelper.stringToDate(cursor.getString(6));

                    cursor.moveToNext();
                }
            }
        }
        close();
        return logs;

    }

    public void deleteServiceLog(ServiceLog log) {
        open();
        String[] sqlArgs = new String[2];
        sqlArgs[0] = SQLiteHelper.dateToString(log.logTime);
        sqlArgs[1] = "" + log.serviceType;
        String sql = "DELETE FROM " + LoggerDBOpener.SEREVICE_TABLE + " WHERE logTime=? AND serviceType=?"; //NON-NLS
        database.execSQL(sql, sqlArgs);
        close();
    }

    public void clearServiceLog() {
        open();
        String sql = "DELETE FROM " + LoggerDBOpener.SEREVICE_TABLE; //NON-NLS
        database.execSQL(sql);
        close();
    }
}
