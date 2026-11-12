package com.jht.chimera.service.logger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.jetbrains.annotations.NonNls;

// fixme: I am just going to be dumping most of the data in this table.
public class LoggerDBOpener extends SQLiteOpenHelper {

    private static final int LOGGER_DB_VERSION = 3;
    @NonNls private static final String LOGGER_DB_NAME = "logger_db";

    @NonNls public static final String SEREVICE_TABLE = "service_tbl";

    @NonNls private static final String CREATE_SERVICE_TABLE =
            "create table " +
                    SEREVICE_TABLE + "(" +
                    "service_log_id integer primary key autoincrement, " +
                    "serviceType integer not null, " +
                    "accumulatedDistanceKm real not null, " +
                    "accumulatedTimeSec integer not null," +
                    "accumulatedFloor real not null, " +
                    "message text not null, " +
                    "logTime text not null " +
                    ");";

    public LoggerDBOpener(Context context) {
        super(context, LOGGER_DB_NAME, null, LOGGER_DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SERVICE_TABLE);
    }

    /**
     * We are only ever going to be upgrading the database and {@code newVersion} will always match
     * {@link #LOGGER_DB_VERSION} given our current implementation. For this reason, this
     * implementation is responsible for converting between the previous versions and the latest
     * version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For versions older than 3, all we need to do is remove tables if present.
        if (oldVersion < 3) {
            try {
                db.execSQL("DROP TABLE IF EXISTS error_time_tbl");
                db.execSQL("DROP TABLE IF EXISTS error_tbl");
                db.execSQL("DROP TABLE IF EXISTS logcat_tbl");
            } catch (Exception ignored) {
                // There is no reason for concern here. It would just be annoying.
            }
        }
    }
}
