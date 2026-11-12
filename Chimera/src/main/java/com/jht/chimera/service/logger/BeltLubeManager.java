package com.jht.chimera.service.logger;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.conversionlib.DistanceConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.keypress.KeyPress;
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage;
import com.jht.datamatrixlib.DataMatrix;

/**
 * The thing that makes sure to tell the user to lubricate the belt occasionally.
 */
public class BeltLubeManager {
    private final String TAG = "BeltLubeManager";
    private final static float TM_BELT_LIMIT_DISTANCE_KM = DistanceConvertUtil.convertMilesToKilometers(300);
    private final static float CM_BELT_LIMIT_HOURS = 1080;
    private final int HOLD_BUTTON_TIME_MS = 5000;

    private boolean mButton1Down = false;
    private boolean mButton2Down = false;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private DataMatrix mDataMatrix = new DataMatrix();
    private MessageCenterMessage mMessage = new MessageCenterMessage("", "", 30);

    private BeltLubeManager() {
        mMessage.setHideTime(999999);
        mMessage.setId(R.string.lube_belt_tc);
    }

    private static class SingletonHolder {
        private static final BeltLubeManager INSTANCE = new BeltLubeManager();
    }

    public static BeltLubeManager getInstance() {
        return BeltLubeManager.SingletonHolder.INSTANCE;
    }

    public void start() {

        mDataMatrix.createEventAdapter(LoggerKeys.ACTION_CHECK_BELT_LUBE, keyReturnedBack -> checkIfBeltNeedsLube());

        mDataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT, value -> {
            KeyPress.KeyCode keyCode = KeyPress.getKeyCode(value);
            KeyPress.KeyState keyState = KeyPress.getKeyState(value);
            FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
            if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                // if  user holds speed up and stop for 5 second, reset lube belt
                if (keyCode == KeyPress.KeyCode.KEY_SPEED_UP) {
                    if (keyState == KeyPress.KeyState.KEY_DOWN) {
                        mButton1Down = true;
                    } else {
                        mButton1Down = false;
                        mHandler.removeCallbacksAndMessages(null);
                    }
                }

                if (keyCode == KeyPress.KeyCode.KEY_STOP) {
                    if (keyState == KeyPress.KeyState.KEY_DOWN) {
                        mButton2Down = true;
                    } else {
                        mButton2Down = false;
                        mHandler.removeCallbacksAndMessages(null);
                    }
                }

                if (mButton1Down && mButton2Down) {
                    mHandler.postDelayed(() -> resetLubeBelt(), HOLD_BUTTON_TIME_MS);
                }

            } else if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill) {

                // if  user holds resistance up and stop for 5 second, reset lube belt
                if (keyCode == KeyPress.KeyCode.KEY_RESISTANCE_UP) {
                    if (keyState == KeyPress.KeyState.KEY_DOWN) {
                        mButton1Down = true;
                    } else {
                        mButton1Down = false;
                        mHandler.removeCallbacksAndMessages(null);
                    }
                }

                if (keyCode == KeyPress.KeyCode.KEY_STOP) {
                    if (keyState == KeyPress.KeyState.KEY_DOWN) {
                        mButton2Down = true;
                    } else {
                        mButton2Down = false;
                        mHandler.removeCallbacksAndMessages(null);
                    }
                }

                if (mButton1Down && mButton2Down) {
                    mHandler.postDelayed(() -> resetLubeBelt(), HOLD_BUTTON_TIME_MS);
                }
            }

        });
    }

    private void resetLubeBelt() {
        Log.d(TAG, "resetLubeBelt");

        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        if (frameConfig.requiresLubricationPerDistance()) {
            mDataMatrix.putFloat(SettingKeys.Hardware.DATA_ACCUMULATED_DISTANCE_KM_AT_LAST_LUBE_FLOAT, mDataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT));
        }
        if (frameConfig.requiresLubricationPerTime()) {
            mDataMatrix.putInt(SettingKeys.Hardware.DATA_ACCUMULATED_TIME_AT_LAST_LUBE_SEC_INT, mDataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT));
        }
        checkIfBeltNeedsLube();
    }

    private void checkIfBeltNeedsLube() {
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        if (frameConfig.requiresLubricationPerDistance()) {
            float accumulatedDistanceKm = mDataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT);
            float lastLubeDistanceKm = mDataMatrix.getFloat(SettingKeys.Hardware.DATA_ACCUMULATED_DISTANCE_KM_AT_LAST_LUBE_FLOAT);
            Log.d(TAG, "checkIfBeltNeedsLube distance km: " + (accumulatedDistanceKm - lastLubeDistanceKm));
            if (accumulatedDistanceKm - lastLubeDistanceKm >= TM_BELT_LIMIT_DISTANCE_KM) {
                showDialog();
            } else {
                hideDialog();
            }
        } else if (frameConfig.requiresLubricationPerTime()) {
            float accumulatedHours = (float) mDataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT) / 3600f;
            float lastLubeHours = (float) mDataMatrix.getInt(SettingKeys.Hardware.DATA_ACCUMULATED_TIME_AT_LAST_LUBE_SEC_INT) / 3600f;
            Log.d(TAG, "lastLubeHours hour: " + lastLubeHours);
            Log.d(TAG, "checkIfBeltNeedsLube hour: " + (accumulatedHours - lastLubeHours));
            if (accumulatedHours - lastLubeHours >= CM_BELT_LIMIT_HOURS) {
                showDialog();
            } else {
                hideDialog();
            }
        } else {
            hideDialog();
        }
    }

    public static boolean doesBeltNeedLube() {
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        if (frameConfig.requiresLubricationPerDistance()) {
            float accumulatedDistanceKm = DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT);
            float lastLubeDistanceKm = DataMatrix.getFloat(SettingKeys.Hardware.DATA_ACCUMULATED_DISTANCE_KM_AT_LAST_LUBE_FLOAT);
            if (accumulatedDistanceKm - lastLubeDistanceKm >= TM_BELT_LIMIT_DISTANCE_KM) {
                return true;
            }
        } else if (frameConfig.requiresLubricationPerTime()) {
            float accumulatedHours = (float) DataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT) / 3600f;
            float lastLubeHours = (float) DataMatrix.getInt(SettingKeys.Hardware.DATA_ACCUMULATED_TIME_AT_LAST_LUBE_SEC_INT) / 3600f;
            if (accumulatedHours - lastLubeHours >= CM_BELT_LIMIT_HOURS) {
                return true;
            }
        }
        return false;
    }

    private void showDialog() {
        String text = "";
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        if (frameConfig.requiresBeltLubrication()) {
            text = ContextUtil.getApplicationContext().getResources().getString(R.string.lube_belt_tc);
        } else if (frameConfig.requiresChainLubrication()) {
            text = ContextUtil.getApplicationContext().getResources().getString(R.string.lube_chain_tc);
        } else {
            // we don't need to show the mMessage but we should never get here
            return;
        }

        mMessage.setMessage(text);
        mMessage.setHidden(false);
        DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, mMessage);
    }

    private void hideDialog() {
        // hide the mMessage
        mMessage.setHidden(true);
        DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, mMessage);
    }
}
