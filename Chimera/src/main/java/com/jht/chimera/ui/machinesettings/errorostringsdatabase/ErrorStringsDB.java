package com.jht.chimera.ui.machinesettings.errorostringsdatabase;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.jht.chimera.lib.database.sqliteassethelper.SQLiteAssetHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to retrieve error code strings.
 */
public class ErrorStringsDB extends SQLiteAssetHelper {
    private static final int DATABASE_VERSION = 4;
    private final String TAG = "ErrorStringsDB.java";

    /**
     * Creates the helper for handling the error strings database.
     * @param context
     */
    public ErrorStringsDB(Context context) {
        super(context, ErrorStringDatabaseConstants.DATABASE_NAME_SERVICE, null, DATABASE_VERSION);
        Log.d(TAG, "DATABASE_VERSION: " + DATABASE_VERSION);
    }

    /**
     * Retrieves the error code descriptions from the database file.
     * @param decimal_code the decimal code for each error
     * @return ErrorCodeStrings object of the error code entries
     */
    public ErrorCodeStrings getServiceDescriptions(int decimal_code) {
        String queryString = "SELECT hex_code, decimal_code, cause, description, troubleshooting " + // NON-NLS
                "FROM ServiceStrings " + // NON-NLS
                "WHERE decimal_code =? "; // NON-NLS
        SQLiteDatabase db = getWritableDatabase();
        Log.d(TAG, ": databaseName: " + getDatabaseName());
        Log.d(TAG, ": DATABASE_VERSION: " + db.getVersion());
        Cursor cursor = db.rawQuery(queryString, new String[]{String.valueOf(decimal_code)});
        List<ErrorCodeStrings> codeEntries = new ArrayList<>();
        if (cursor != null && 0 < cursor.getCount()) {
            cursor.moveToFirst();
            do {
                String hexCode = cursor.getString(cursor.getColumnIndex(ErrorStringDatabaseConstants.FIELD_HEX_CODE));
                //int decimalCode = cursor.getInt(cursor.getColumnIndex(ErrorStringDatabaseConstants.FIELD_DECIMAL_CODE));
                String causeString = cursor.getString(cursor.getColumnIndex(ErrorStringDatabaseConstants.FIELD_CAUSE));
                String descriptionString = cursor.getString(cursor.getColumnIndex(ErrorStringDatabaseConstants.FIELD_DESCRIPTION));
                String troubleShootingString = cursor.getString(cursor.getColumnIndex(ErrorStringDatabaseConstants.FIELD_TROUBLESHOOTING));

                ErrorCodeStrings serviceCodeInfo = new ErrorCodeStrings(hexCode, causeString, descriptionString, troubleShootingString);
                serviceCodeInfo.decimal_code = decimal_code;
                codeEntries.add(serviceCodeInfo);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return (codeEntries.isEmpty() ? null : codeEntries.get(0));
    }
}