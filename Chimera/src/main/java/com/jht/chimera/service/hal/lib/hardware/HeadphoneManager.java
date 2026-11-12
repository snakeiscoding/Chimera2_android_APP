package com.jht.chimera.service.hal.lib.hardware;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;

/**
 * See https://jhtnaee.atlassian.net/wiki/spaces/PAN/pages/4121362453/Parker+3.0+Audio
 */
public class HeadphoneManager {

    public final String TAG = "HeadphoneManager";

    public static HeadphoneManager getInstance() {
        return HeadphoneManager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final HeadphoneManager INSTANCE = new HeadphoneManager();
    }

    private boolean mIsHeadsetAConnected = false;
    private boolean mIsHeadsetBConnected = false;
    private boolean mFirstTime = true;

    private HeadphoneManager() {
        if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker)
                || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)
        ) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_HEADSET_PLUG);
            ContextUtil.getApplicationContext().registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.hasExtra("state")) {
                        if (intent.getIntExtra("state", 0) == 0) {
                            if (mIsHeadsetAConnected) {
                                DataMatrix.putBoolean(HALKeys.DATA_IS_HEADPHONE_CONNECTED_BOOL, false);
                            }
                            mIsHeadsetAConnected = false;
                        } else if (intent.getIntExtra("state", 0) == 1) {
                            if (!mIsHeadsetAConnected) {
                                if (!mFirstTime) {
                                    // increment insertion count
                                    int count = DataMatrix.getInt(SettingKeys.Hardware.DATA_HEADPHONE_B_COUNT_INT);
                                    count++;
                                    DataMatrix.putInt(SettingKeys.Hardware.DATA_HEADPHONE_B_COUNT_INT, count);
                                }
                                DataMatrix.putBoolean(HALKeys.DATA_IS_HEADPHONE_CONNECTED_BOOL, true);
                            }
                            mIsHeadsetAConnected = true;
                        }
                        mFirstTime = false;
                    }
                }
            }, filter);
        }

        if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera) || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_2)  || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_3)) {

            @NonNls final String HEADSET_A_PLUG_IN = "com.nex.action.HEADSET_A_PLUG_IN";
            @NonNls final String HEADSET_A_PLUG_OUT = "com.nex.action.HEADSET_A_PLUG_OUT";
            @NonNls final String HEADSET_B_PLUG_IN = "com.nex.action.HEADSET_B_PLUG_IN";
            @NonNls final String HEADSET_B_PLUG_OUT = "com.nex.action.HEADSET_B_PLUG_OUT";
            IntentFilter filter = new IntentFilter();
            filter.addAction(HEADSET_A_PLUG_IN);
            filter.addAction(HEADSET_A_PLUG_OUT);
            filter.addAction(HEADSET_B_PLUG_IN);
            filter.addAction(HEADSET_B_PLUG_OUT);

            ContextUtil.getApplicationContext().registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    boolean headsetConnected = mIsHeadsetAConnected || mIsHeadsetBConnected;
                    Log.e(TAG, "onReceive: " + intent.getAction());
                    if (intent.getAction().equals(HEADSET_A_PLUG_IN)) {
                        mIsHeadsetAConnected = true;
                    } else if (intent.getAction().equals(HEADSET_A_PLUG_OUT)) {
                        mIsHeadsetAConnected = false;
                        int count = DataMatrix.getInt(SettingKeys.Hardware.DATA_HEADPHONE_A_COUNT_INT);
                        count++;
                        DataMatrix.putInt(SettingKeys.Hardware.DATA_HEADPHONE_A_COUNT_INT, count);
                    } else if (intent.getAction().equals(HEADSET_B_PLUG_IN)) {
                        mIsHeadsetBConnected = true;
                    } else if (intent.getAction().equals(HEADSET_B_PLUG_OUT)) {
                        mIsHeadsetBConnected = false;
                        int count = DataMatrix.getInt(SettingKeys.Hardware.DATA_HEADPHONE_B_COUNT_INT);
                        count++;
                        DataMatrix.putInt(SettingKeys.Hardware.DATA_HEADPHONE_B_COUNT_INT, count);
                    }

                    if(headsetConnected != mIsHeadsetAConnected || mIsHeadsetBConnected) {
                        if (mIsHeadsetAConnected || mIsHeadsetBConnected) {
                            DataMatrix.putBoolean(HALKeys.DATA_IS_HEADPHONE_CONNECTED_BOOL, true);
                        } else {
                            DataMatrix.putBoolean(HALKeys.DATA_IS_HEADPHONE_CONNECTED_BOOL, false);
                        }
                    }
                }
            }, filter);
        }
    }

    public void start() {
        // do nothing
    }
}
