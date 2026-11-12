package com.jht.chimera.lib.language;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.util.Log;

import com.jht.chimera.lib.common.context.ContextUtil;

public class IntegerLookup {
    private static final String TAG = "IntegerLookup";
    static Context sContext = ContextUtil.getApplicationContext();

    public static int get(int stringId) {
        return getInt(stringId, sContext);
    }

    public static int getInt(int stringId, Context context) {
        try {
            return context.getResources().getInteger(stringId);
        } catch (NotFoundException e) {
            Log.w(TAG, e);
        }
        return -1;
    }
}
