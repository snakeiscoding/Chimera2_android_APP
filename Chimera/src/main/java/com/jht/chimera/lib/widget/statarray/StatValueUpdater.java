package com.jht.chimera.lib.widget.statarray;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.Nullable;

import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.widget.statarray.formatting.IStatFormattingLogic;
import com.jht.chimera.lib.widget.statarray.formatting.IStatTitles;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.List;

/**
 * Links a data matrix key to the default values for a stat item. This is initialized by
 * {@link StatValueUpdaterCreator} and used to generate {@link com.jht.chimera.lib.widget.statbar.StatItem} objects
 * for use in a stat bar. The callback interface {@link IStatValueUpdate} should be used to update
 * the values of the associated stat items.
 * <p>
 * This class is useful for displaying formatted strings for different stats. It provides a callback
 * that gives a formatted value for the stat along with the correct title.
 */
public class StatValueUpdater {
    private static final String TAG = "StatValueUpdater";

    public static final int STAT_TYPE_INT = 0;
    public static final int STAT_TYPE_FLOAT = 1;
    public static final int STAT_TYPE_DOUBLE = 2;
    public static final int STAT_TYPE_STRING = 3;

    private IStatTitles mTitles;
    private String mDataMatrixKey;
    private @Nullable String mDataMatrixNextValueKey = null;
    private int mType;
    private int mId;
    private IStatFormattingLogic mFormatting = null;
    private final DataMatrix mFactory = new DataMatrix();

    List<IStatValueUpdate> mCallbacks = new ArrayList<>();

    private BroadcastReceiver mLanguageChangeReceiver;

    private boolean mCallbacksActive = false;

    public StatValueUpdater(final Context context, int id, String dataMatrixKey, int type, final int titleId, final String value) {
        this(context, id, dataMatrixKey, type, new IStatTitles() {
            @Override
            public String getStatTitle() {
                return StringLookup.getString(titleId, context);
            }

            @Override
            public String getStatDefaultValue() {
                return value;
            }
        });
    }

    public StatValueUpdater(Context context, int id, String dataMatrixKey, int type, IStatTitles statDefaults) {
        mTitles = statDefaults;
        mId = id;
        mDataMatrixKey = dataMatrixKey;
        mType = type;
        setupLanguageBroadcast(context);
    }

    public StatValueUpdater(final Context context, int id, String dataMatrixKey, int type, final int titleId, final String value, IStatFormattingLogic formatting) {
        this(context, id, dataMatrixKey, type, new IStatTitles() {
            @Override
            public String getStatTitle() {
                return StringLookup.getString(titleId, context);
            }

            @Override
            public String getStatDefaultValue() {
                return value;
            }
        }, formatting);
    }

    public StatValueUpdater(Context context, int id, String dataMatrixKey, int type, IStatTitles statDefaults, IStatFormattingLogic formatting) {
        this(context, id, dataMatrixKey, type, statDefaults);
        mFormatting = formatting;
        if (mFormatting != null) {
            mFormatting.setStatUpdaterCallback(() -> forceCallback(mType));
        }
    }

    /** Adds a scripted "next value" for this stat. Needs to be called before callbacks are started to have effect. */
    public void setNextValueDataMatrixKey(String key) {
        mDataMatrixNextValueKey = key;
    }

    private void setupLanguageBroadcast(Context context) {
        mLanguageChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                forceCallback(mType);
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        context.registerReceiver(mLanguageChangeReceiver, filter);
    }

    private String formatValue(int value) {
        return mFormatting != null ? mFormatting.format(value) : String.valueOf(value);
    }
    private String formatValue(float value) {
        return mFormatting != null ? mFormatting.format(value) : String.valueOf(value);
    }
    private String formatValue(double value) {
        return mFormatting != null ? mFormatting.format(value) : String.valueOf(value);
    }
    private String formatValue(String value) {
        return mFormatting != null ? mFormatting.format(value) : value;
    }

    private void dispatchValue(String value) {
        for (IStatValueUpdate callback : mCallbacks) {
            callback.statValueChanged(value);
        }
    }
    private void dispatchNextValue(String value) {
        for (IStatValueUpdate callback : mCallbacks) {
            callback.statNextValueChanged(value);
        }
    }

    private void setupDataMatrixCallback(int type) {
        if (!mCallbacksActive) {
            mCallbacksActive = true;
            boolean hasNext = mDataMatrixNextValueKey != null;
            switch (type) {
                case STAT_TYPE_INT:
                    mFactory.createIntAdapter(mDataMatrixKey, value -> dispatchValue(formatValue(value)), false);
                    if (hasNext) {
                        mFactory.createIntAdapter(mDataMatrixNextValueKey, value -> dispatchNextValue(formatValue(value)), false);
                    }
                    break;
                case STAT_TYPE_FLOAT:
                    mFactory.createFloatAdapter(mDataMatrixKey, value -> dispatchValue(formatValue(value)), false);
                    if (hasNext) {
                        mFactory.createFloatAdapter(mDataMatrixNextValueKey, value -> dispatchNextValue(formatValue(value)), false);
                    }
                    break;
                case STAT_TYPE_DOUBLE:
                    mFactory.createDoubleAdapter(mDataMatrixKey, value -> dispatchValue(formatValue(value)), false);
                    if (hasNext) {
                        mFactory.createDoubleAdapter(mDataMatrixNextValueKey, value -> dispatchNextValue(formatValue(value)), false);
                    }
                    break;
                case STAT_TYPE_STRING:
                    mFactory.createStringAdapter(mDataMatrixKey, value -> dispatchValue(formatValue(value)), false);
                    if (hasNext) {
                        mFactory.createStringAdapter(mDataMatrixNextValueKey, value -> dispatchNextValue(formatValue(value)), false);
                    }
                    break;
                default:
                    Log.e(TAG, "Incorrect type! Check that the type is set correctly!");
                    break;
            }
        }
        forceCallback(type);
    }

    private String readFormatted(int type, String key) {
        switch (type) {
            case STAT_TYPE_INT: return formatValue(DataMatrix.getInt(key, 0));
            case STAT_TYPE_FLOAT: return formatValue(DataMatrix.getFloat(key, 0));
            case STAT_TYPE_DOUBLE: return formatValue(DataMatrix.getDouble(key, 0));
            case STAT_TYPE_STRING: return formatValue(DataMatrix.getString(key, ""));
            default:
                Log.e(TAG, "Incorrect type! Check that the type is set correctly!");
                return "";
        }
    }

    private void forceCallback(int type) {
        String newTitle = mTitles.getStatTitle();
        String newValue = readFormatted(type, mDataMatrixKey);
        String newNextValue = mDataMatrixNextValueKey != null ? readFormatted(type, mDataMatrixNextValueKey) : null;

        for (IStatValueUpdate callback : mCallbacks) {
            callback.statTitleChanged(newTitle);
            callback.statValueChanged(newValue);
            if (newNextValue != null) callback.statNextValueChanged(newNextValue);
        }
    }

    private void forceDefault() {
        for (IStatValueUpdate callback : mCallbacks) {
            callback.statTitleChanged(getDefaultTitle());
            callback.statValueChanged(getDefaultValue());
        }
    }

    /**
     * Sends updated stat values to the associated views
     */
    public interface IStatValueUpdate {
        void statValueChanged(String value);

        void statTitleChanged(String title);

        void statNextValueChanged(String value);
    }

    /**
     * Adds a callback that will be sent with the formatted string value as the data matrix
     * key associated with this stat is updated.
     *
     * @param callbackInterface the callback to receive formatted values for
     */
    public void addCallbackInterface(IStatValueUpdate callbackInterface) {
        if (!mCallbacks.contains(callbackInterface)) {
            mCallbacks.add(callbackInterface);
            startCallbacks();
        }
    }

    /**
     * Removes a callback from the list of interfaces that are sent to
     *
     * @param callbackInterface The interface to remove
     */
    public void removeCallback(IStatValueUpdate callbackInterface) {
        mCallbacks.remove(callbackInterface);
        if (mCallbacks.size() == 0) stopCallbacks();
    }

    /**
     * Connects the DataMatrix and begins sending value updates to connected interfaces
     */
    protected void startCallbacks() {
        setupDataMatrixCallback(mType);
    }

    /**
     * Stops all DataMatrix callbacks and sending the values to the associated interfaces.
     * This should be called before the object is destroyed to prevent leaks
     */
    protected void stopCallbacks() {
        mFactory.finish();
        mCallbacksActive = false;
    }

    /**
     * Gets the default title for this stat value
     *
     * @return The default title
     */
    public String getDefaultTitle() {
        return mTitles.getStatTitle();
    }

    /**
     * Gets the default value for this stat item
     *
     * @return The default value
     */
    public String getDefaultValue() {
        return mTitles.getStatDefaultValue();
    }

    /**
     * Gets the ID of the currently represented stat
     * @return The ID of the currently represented stat
     */
    public int getId() {
        return mId;
    }

    /**
     * Gets the current formatted value of the represented stat
     * @return The current value of the stat
     */
    public String getCurrentValue() {
        return readFormatted(mType, mDataMatrixKey);
    }

    /**
     * Forces an update of the both the title (if pulled from an interface) and the value (if sent
     * through a formatter). This is used to update the value if something outside of changing the
     * value changed the result of formatting so that all associated text views can be updated.
     */
    public interface IStatValueUpdaterCallback {
        void forceStatUpdate();
    }

    public String getDataMatrixKey() {
        return mDataMatrixKey;
    }
}
