package com.jht.chimera.ui.mymedia;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import com.jht.chimera.lib.widget.activity.RestringActivity;
import androidx.fragment.app.FragmentManager;

import com.jht.chimera.R;
import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UsbMediaDeviceKeys;
import com.jht.chimera.lib.common.permission.GrantPermission;
import com.jht.datamatrixlib.DataMatrix;

public class MyMediaActivity extends RestringActivity {
    private static final String TAG = "MyMediaActivity";
    private final DataMatrix mDataMatrix = new DataMatrix();
    private final FragmentManager mFragMgr = getSupportFragmentManager();
    private LinearLayout mLlMenu;
    private Button mBtnUsb;
    private Button mBtnBluetooth;
    private boolean mMenuHidden;
    private FrameLayout mFlRightSide;

    private boolean mIsUsbConnected;
    private String mDeviceName;
    private AudioSourceInput currentInput = AudioSourceInput.Default;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_media);

        GrantPermission.grant(this, getPackageName(), Manifest.permission.RECORD_AUDIO, getApplicationInfo().uid);

        mLlMenu = findViewById(R.id.ll_menu);
        mFlRightSide = findViewById(R.id.fl_right_side);
        mBtnUsb = findViewById(R.id.btn_usb);
        mBtnUsb.setOnClickListener(v -> {
            DataMatrix.putBoolean(HALKeys.DATA_IS_USING_BLE_BROWSING_BOOL, false);
            if(DataMatrix.getInt(HALKeys.DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM_INT) != AudioSourceInput.IOS.ordinal()) {
                if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_3)
                        || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)) {
                    currentInput = AudioSourceInput.USB;
                    DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.USB.ordinal());
                } else {
                    currentInput = AudioSourceInput.Foreground;
                    DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.Foreground.ordinal());
                }
            }
            else {
                currentInput = AudioSourceInput.IOS;
            }
            showMenu(false);
            loadLibraryPage();
        });

        /* Disable MyMedia BT function within App
        mBtnBluetooth = findViewById(R.id.btn_bluetooth);
        mBtnBluetooth.setOnClickListener(v -> {
            currentInput = AudioSourceInput.BT_Sink;
            if(DataMatrix.getBoolean(HALKeys.DATA_IS_BT_SINK_CONNECTED_BOOL)) {
                DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.BT_Sink.ordinal());
                DataMatrix.putBoolean(HALKeys.DATA_IS_USING_BLE_BROWSING_BOOL, true);

                Log.e(TAG, "counter: " + DataMatrix.getInt(HALKeys.DATA_FOLDER_PATH_COUNTER_INT));
                int counter = DataMatrix.getInt(HALKeys.DATA_FOLDER_PATH_COUNTER_INT);
                Bundle bundle = new Bundle();
                bundle.putInt("direction", 0);
                bundle.putString("uid", "0,0,0,0,0,0,0,0");
                for(int i = 0; i < counter; i++) {
                    DataMatrix.putParcelable(HALKeys.ACTION_CHANGE_PATH_COMMAND_PARCELABLE, bundle);
                }
                DataMatrix.putInt(HALKeys.DATA_FOLDER_PATH_COUNTER_INT, 0);

                showMenu(false);
                loadLibraryPage();

            }
            else{
                LaunchAppUtil.launchBluetoothPairing();
            }
        });
         */

        showDefault();
        findUsbDevice();

        initDataMatrix();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(currentInput != AudioSourceInput.Default && DataMatrix.getInt(HALKeys.DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM_INT) != AudioSourceInput.IOS.ordinal()) {
            DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, currentInput.ordinal());
        }
    }

    private void showDefault() {
        Log.e(TAG, "about to show default");
        enableButton(mBtnUsb, mIsUsbConnected);

        if (mIsUsbConnected) {
            return;
        }

        InstructionFragment mInstructionPage = InstructionFragment.newInstance();
        mFragMgr.beginTransaction()
                .replace(R.id.fl_right_side, mInstructionPage, "instruction") //NON-NLS
                .commitAllowingStateLoss();

        mFragMgr.executePendingTransactions();

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 3);
        mLlMenu.setLayoutParams(lp);

        lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 7);
        mFlRightSide.setLayoutParams(lp);

    }

    private void enableButton(final Button b, final boolean enable) {
        b.post(() -> {
            b.setAlpha(enable ? 1.0f : 0.5f);
            b.setEnabled(enable);
        });
    }

    private void findUsbDevice() {
        if (DataMatrix.getBoolean(UsbMediaDeviceKeys.DATA_IS_CONNECTED_BOOL)) {
            onConnect();
        }
    }

    private void initDataMatrix() {
        mDataMatrix.createStringAdapter(UsbMediaDeviceKeys.DATA_DEVICE_NAME_STRING, value -> mDeviceName = value);
        mDataMatrix.createBoolAdapter(UsbMediaDeviceKeys.DATA_IS_CONNECTED_BOOL, value -> {
            mIsUsbConnected = value;
            onConnect();
        });
        mDataMatrix.createStringAdapter(HALKeys.DATA_BT_SINK_CONNECTED_DEVICE_NAME_STRING, s -> {
            if (s.equals("")) {
                mBtnBluetooth.setText(getResources().getString(R.string.device_pairing_tc));
            } else {
                mBtnBluetooth.setText(s);
            }
        });

        mDataMatrix.createBoolAdapter(HALKeys.DATA_BT_SINK_CONNECTED_BOOL, value -> {
            if(!value) {
                runOnUiThread(() -> mBtnBluetooth.setText(getResources().getString(R.string.device_pairing_tc)));
                showDefault();
            }
        });
    }

    private void onConnect() {
        if (mIsUsbConnected) {
            enableButton(mBtnUsb, true);
            runOnUiThread(() -> mBtnUsb.setText(mDeviceName));
        } else {
            runOnUiThread(() -> mBtnUsb.setText(getResources().getString(R.string.med_app_usb_tc)));
            showDefault();
        }
    }

    private void loadLibraryPage() {
        UsbLibraryFragment mUsbLibraryPage = new UsbLibraryFragment();

        mUsbLibraryPage.setOnMenuClickListener(() -> {
            LinearLayout.LayoutParams lp;
            if(mMenuHidden) {
                lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 7);
            } else {
                lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            }
            mFlRightSide.setLayoutParams(lp);
            showMenu(mMenuHidden);
            mUsbLibraryPage.showAudioPlayerView(mMenuHidden);
        });

        mFragMgr.beginTransaction()
                .replace(R.id.fl_right_side, mUsbLibraryPage, "usbLibrary") //NON-NLS
                .commit();

        mFragMgr.executePendingTransactions();

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        mFlRightSide.setLayoutParams(lp);
    }

    private void showMenu(boolean show) {
        if(show) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 3);
            mLlMenu.setLayoutParams(lp);
        } else {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0);
            mLlMenu.setLayoutParams(lp);
        }

        mMenuHidden = !show;
    }
}
