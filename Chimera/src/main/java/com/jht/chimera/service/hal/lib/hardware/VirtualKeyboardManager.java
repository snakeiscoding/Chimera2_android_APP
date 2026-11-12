package com.jht.chimera.service.hal.lib.hardware;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.enums.JHTTypes;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage;
import com.jht.chimera.lib.widget.dialog.RemoveFocusDlg;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;

import java.util.Locale;

public class VirtualKeyboardManager {

    public final String TAG = "VirtualKeyboardManager";

    @NonNls private static final String ACTION_KEYBOARD_VISIBLE = "com.jht.action.KEYBOARD_VISIBLE";
    @NonNls private static final String ACTION_KEYBOARD_HIDDEN = "com.jht.action.KEYBOARD_HIDDEN";
    @NonNls private static final String ACTION_KEYBOARD_DISABLE_MESSAGE = "com.jht.action.KEYBOARD_DISABLE_MESSAGE";
    @NonNls private static final String ACTION_KEYBOARD_STATE_ENABLED = "com.jht.action.KEYBOARD_STATE_ENABLED";
    @NonNls private static final String ACTION_KEYBOARD_STATE_DISABLED = "com.jht.action.KEYBOARD_STATE_DISABLED";
    @NonNls private static final String ACTION_KEYBOARD_ENABLED = "com.jht.action.KEYBOARD_ENABLE";
    @NonNls private static final String ACTION_KEYBOARD_DISABLE = "com.jht.action.KEYBOARD_DISABLE";

    private boolean mIsKeyboardVisible = false;
    private boolean mIsKeyboardEnabled = false;
    private Context mContext;
    private FrameConfig mFrameConfig = FrameConfigManager.getInstance().getFrameConfig();
    private float mKphLimit;
    private boolean mCheckOnceDisabled = false;

    private Handler handler = new Handler();

    public static VirtualKeyboardManager getInstance() {
        return VirtualKeyboardManager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final VirtualKeyboardManager INSTANCE = new VirtualKeyboardManager();
    }

    private VirtualKeyboardManager() {

        mContext = ContextUtil.getApplicationContext();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_KEYBOARD_DISABLE_MESSAGE);
        intentFilter.addAction(ACTION_KEYBOARD_VISIBLE);
        intentFilter.addAction(ACTION_KEYBOARD_HIDDEN);
        intentFilter.addAction(ACTION_KEYBOARD_STATE_DISABLED);
        intentFilter.addAction(ACTION_KEYBOARD_STATE_ENABLED);
        BroadcastReceiver mKeyboardReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals(ACTION_KEYBOARD_DISABLE_MESSAGE)) {
                        Log.d(TAG, "ACTION_KEYBOARD_DISABLE_MESSAGE ");
                        showKeyboardMessage();
                    }
                    if (action.equals(ACTION_KEYBOARD_HIDDEN)) {
                        Log.d(TAG, "ACTION_KEYBOARD_HIDDEN ");
                        mIsKeyboardVisible = false;
                    }
                    if (action.equals(ACTION_KEYBOARD_VISIBLE)) {
                        Log.d(TAG, "ACTION_KEYBOARD_VISIBLE ");
                        mIsKeyboardVisible = true;
                    }
                    if (action.equals(ACTION_KEYBOARD_STATE_DISABLED)) {
                        Log.d(TAG, "ACTION_KEYBOARD_STATE_DISABLED ");
                        mIsKeyboardEnabled = false;
                        if (mCheckOnceDisabled) {
                            FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
                            switch (config.machineClassType) {
                                case Treadmill:
                                    updateKeyboardEnabledSpeed(DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT));
                                    break;
                                case Ascent:
                                case Elliptical:
                                case RecumbentElliptical:
                                case Bike:
                                case SpinBike:
                                case Climbmill:
                                case Stepper:
                                    updateKeyboardEnabledResistance(DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT));
                                    break;
                            }

                            mCheckOnceDisabled = false;
                        }
                    }
                    if (action.equals(ACTION_KEYBOARD_STATE_ENABLED)) {
                        Log.d(TAG, "ACTION_KEYBOARD_STATE_ENABLED ");
                        mIsKeyboardEnabled = true;
                    }
                }
            }
        };
        mContext.registerReceiver(mKeyboardReceiver, intentFilter);
        enableVirtualKeyboard();

        DataMatrix mDataMatrix = new DataMatrix();
        mDataMatrix.createFloatAdapter(SettingKeys.Workout.DATA_KEYBOARD_DISABLE_THRESHOLD_SPEED_KPH_FLOAT, value -> mKphLimit = value);

        mDataMatrix.createFloatAdapter(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, this::updateKeyboardEnabledSpeed);
        mDataMatrix.createIntAdapter(HALKeys.ACTION_SET_RESISTANCE_INT, this::updateKeyboardEnabledResistance);

        // Disables the keypad and immediately resets whether the keypad is enabled. Must be handled this way to hide keypad on third party apps
        mDataMatrix.createEventAdapter(UiServiceKeys.EVENT_HIDE_KEYBOARD, s -> {
            if(mIsKeyboardVisible) {
                RemoveFocusDlg.showDlg();
                /*
                Context context = ContextUtil.getApplicationContext();
                WindowManager window = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                Display display = window.getDefaultDisplay();
                Point outSize = new Point();
                display.getRealSize(outSize);
                int x = outSize.x / 2;
                int y = outSize.y - 5;
                ExecuteAsRoot.run("input touchscreen tap " + x + " " + y);
                */
            }
            //mCheckOnceDisabled = true;
            //disableVirtualKeyboard(false);
        });
    }

    private void updateKeyboardEnabledSpeed(float speed) {
        // We delay because the first message may be right before the workout starts.
        if (DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == WorkoutKeys.Constants.RUN_STATE_STOP) {
            handler.postDelayed(() -> updateKeyboardEnabledSpeedImpl(speed), 1000);
        }
        else {
            updateKeyboardEnabledSpeedImpl(speed);
        }

    }

    private void updateKeyboardEnabledSpeedImpl(float speed) {
        Boolean enabled = isKeyboardEnabledSpeed(speed);
        if (enabled != null) {
            if (enabled) {
                enableVirtualKeyboard();
            } else {
                disableVirtualKeyboard();
            }
        }
    }

    private void updateKeyboardEnabledResistance(int resistance) {
        // We delay because the first message may be right before the workout starts.
        if (DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == WorkoutKeys.Constants.RUN_STATE_STOP) {
            handler.postDelayed(() -> updateKeyboardEnabledResistanceImpl(resistance), 1000);
        }
        else {
            updateKeyboardEnabledResistanceImpl(resistance);
        }
    }

    private void updateKeyboardEnabledResistanceImpl(int resistance) {
        Boolean enable = isKeyboardEnabledResistance(resistance);
        if (enable != null) {
            if (enable) {
                enableVirtualKeyboard();
            } else {
                disableVirtualKeyboard();
            }
        }
    }

    // convert the speed in kph to the display units if necessary then round to the nearest tenth
    private float roundToTenthOfSpeedInDisplayUnit(float speedKph) {
        float speed = speedKph;
        boolean isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
        if (!isMetric) {
            speed = SpeedConvertUtil.convertKphToMph(speedKph);
        }
        return Math.round(speed * 10f) / 10f;
    }

    private Boolean isKeyboardEnabledSpeed(float speed) {
        Boolean enableKeyboard;
        if (DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == WorkoutKeys.Constants.RUN_STATE_STOP) {
            enableKeyboard = true;
        } else if (mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
            enableKeyboard = !(roundToTenthOfSpeedInDisplayUnit(speed) > roundToTenthOfSpeedInDisplayUnit(mKphLimit));
        } else {
            enableKeyboard = true;
        }

        return enableKeyboard;
    }

    private Boolean isKeyboardEnabledResistance(int resistance) {
        Boolean enableKeyboard;
        if (mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
            // Set to null since we don't want the keyboard state to be affected at all on treadmills
            enableKeyboard = null;
        } else if (DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == WorkoutKeys.Constants.RUN_STATE_STOP) {
            enableKeyboard = true;
        } else if (mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill) {
            enableKeyboard = resistance <= 1;
        } else {
            enableKeyboard = true;
        }

        return enableKeyboard;
    }



    public void start() {
        // do nothing
    }

    private void enableVirtualKeyboard() {
        if (!mIsKeyboardEnabled)
            mContext.sendBroadcast(new Intent(ACTION_KEYBOARD_ENABLED));
    }

    private void disableVirtualKeyboard() {
        if (mIsKeyboardEnabled) mContext.sendBroadcast(new Intent(ACTION_KEYBOARD_DISABLE));
        if (mIsKeyboardVisible) showKeyboardMessage();
    }

    private void showKeyboardMessage() {
        String text;
        if (mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
            if (DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)) {
                String tmp = StringLookup.getString(R.string.keyboard_disabled_kph_tm_pg, mContext).replace("%1", "%.1f");
                text = String.format(Locale.ENGLISH, tmp, mKphLimit);
            } else {
                float mphLimit = SpeedConvertUtil.convertKphToMph(mKphLimit);
                String tmp = StringLookup.getString(R.string.keyboard_disabled_mph_tm_pg, mContext).replace("%1", "%.1f");
                text = String.format(Locale.ENGLISH, tmp, mphLimit);
            }
            MessageCenterMessage msg = new MessageCenterMessage("", text, 30);
            msg.setId(JHTTypes.MessageIds.Keyboard_Disabled.getId());
            DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, msg);
        } else if (mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill) {
            text = StringLookup.getString(R.string.keyboard_disabled_climbmill_pg, mContext);
            MessageCenterMessage msg = new MessageCenterMessage("", text, 30);
            msg.setId(JHTTypes.MessageIds.Keyboard_Disabled.getId());
            DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, msg);
        }

    }
}