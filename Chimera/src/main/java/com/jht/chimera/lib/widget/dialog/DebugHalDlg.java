package com.jht.chimera.lib.widget.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.jht.androidcommonalgorithms.network.AddressHelper;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("MissingPermission")
public class DebugHalDlg extends LinearLayout {
    @NonNls private static final String TAG = "DebugHalDlg";
    private static DataMatrix mDataMatrix = new DataMatrix();
    static Dialog sDialog;
    static FrameConfigManager sFrameConfigManager = FrameConfigManager.getInstance();

    Spinner mSpinnerFrame;
    //    SeekBar mSeekSpeed;
//    SeekBar mSeekIncline;
//    SeekBar mSeekResistance;
    SeekBar mSeekDemoRpm;
    SeekBar mSeekDemoHeartRate;
    SeekBar mSeekWorkoutSpeed;
    TextView mTextSeekBarSpeed;
    TextView mTextSeekBarIncline;
    TextView mTextSeekBarResistance;
    TextView mTextSeekBarDemoRpm;
    TextView mTextSeekBarDemoHeartRate;
    TextView mTextSeekBarWorkoutSpeed;
    TextView mTextData;
    TextView mTextLcbType;
    TextView mTextDemoMode;
    TextView mTextFrameType;
    TextView mTextMachineSetupStatus;

    public DebugHalDlg(Context context) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.dialog_debug_hal, this, true);

        // setup lcb type
        mTextLcbType = (TextView) findViewById(R.id.text_lcb_type);
        mDataMatrix.createIntAdapter(HALKeys.DATA_LCB_TYPE_INT, value -> mTextLcbType.setText(String.format("LCB: 0x%02x", value)));

        // setup frame spinner
        mSpinnerFrame = (Spinner) findViewById(R.id.spinner_frame);
        FrameConfigEnum[] frameList = FrameConfigEnum.getSupportedFrames();
        List<String> frameStringList = new ArrayList<String>();
        for (FrameConfigEnum frame : frameList) {
            frameStringList.add(frame.toString());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, frameStringList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerFrame.setAdapter(adapter);
        String settingFrameType = mDataMatrix.getString(SettingKeys.General.DATA_FRAME_TYPE_STRING);
        mSpinnerFrame.setSelection(adapter.getPosition(settingFrameType));

        // update frame functionality
        findViewById(R.id.button_set_frame).setOnClickListener(view -> {
            FrameConfigManager.getInstance().setFrame(FrameConfigEnum.getFrameTypeFromString(mSpinnerFrame.getSelectedItem().toString()));

        });
/*
        // speed seek bar
        mTextSeekBarSpeed = (TextView) findViewById(R.id.text_seekbar_speed);
        mSeekSpeed = (SeekBar) findViewById(R.id.seekbar_speed);
        mSeekSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int i, boolean b) {
                mTextSeekBarSpeed.post(new Runnable() {
                    @Override
                    public void run() {
                        mTextSeekBarSpeed.setText("" + i);
                        mDataMatrix.putFloat(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, i);
                    }
                });
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
*/

/*
        // incline seek bar
        mTextSeekBarIncline = (TextView) findViewById(R.id.text_seekbar_incline);
        mSeekIncline = (SeekBar) findViewById(R.id.seekbar_incline);
        mSeekIncline.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int i, boolean b) {
                mTextSeekBarIncline.post(new Runnable() {
                    @Override
                    public void run() {
                        mTextSeekBarIncline.setText("" + i);
                        mDataMatrix.putFloat(HALKeys.ACTION_SET_INCLINE_FLOAT, i);
                    }
                });
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
*/

/*
        // resistance seek bar
        mTextSeekBarResistance = (TextView) findViewById(R.id.text_seekbar_resistance);
        mSeekResistance = (SeekBar) findViewById(R.id.seekbar_resistance);
        mSeekResistance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int i, boolean b) {
                mTextSeekBarResistance.post(new Runnable() {
                    @Override
                    public void run() {
                        mTextSeekBarResistance.setText("" + (i + 1)); // resistance starts at 1
                        mDataMatrix.putInt(HALKeys.ACTION_SET_RESISTANCE_INT, (i + 1));
                    }
                });
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
*/
        // demo rpm seek bar
        mTextSeekBarDemoRpm = (TextView) findViewById(R.id.text_seekbar_rpm);
        mSeekDemoRpm = (SeekBar) findViewById(R.id.seekbar_rpm);
        int rpm = mDataMatrix.getInt(HALKeys.DATA_DEMO_RPM_INT);
        mSeekDemoRpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int i, boolean b) {
                mTextSeekBarDemoRpm.post(() -> {
                    mTextSeekBarDemoRpm.setText("" + i);
                    mDataMatrix.putInt(HALKeys.DATA_DEMO_RPM_INT, i);
                });
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mSeekDemoRpm.setProgress(rpm);

        // demo heart rate seek bar
        mTextSeekBarDemoHeartRate = (TextView) findViewById(R.id.text_seekbar_hr);
        mSeekDemoHeartRate = (SeekBar) findViewById(R.id.seekbar_hr);
        mSeekDemoHeartRate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int i, boolean b) {
                mTextSeekBarDemoHeartRate.post(() -> {
                    mTextSeekBarDemoHeartRate.setText("" + i);
                    mDataMatrix.putInt(HALKeys.DATA_DEMO_HEART_RATE_INT, i);
                });
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mTextSeekBarWorkoutSpeed = findViewById(R.id.text_seekbar_workout_speed);
        mSeekWorkoutSpeed = findViewById(R.id.seekbar_workout_speed);
        mSeekWorkoutSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                mTextSeekBarWorkoutSpeed.post(() -> {
                    mTextSeekBarWorkoutSpeed.setText(String.valueOf(progress));
                    DataMatrix.putFloat(WorkoutKeys.ACTION_SET_WORKOUT_SPEED_MULTIPLIER_FLOAT, progress);
                });
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // text data
        mTextData = (TextView) findViewById(R.id.text_data);
        mDataMatrix.createFloatAdapter(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT, value -> {
            @NonNls StringBuilder sb = new StringBuilder();
            sb.append("Target KPH: " + mDataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT));
            sb.append("\nActual KPH: " + mDataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT));
            sb.append("\nTarget Incline: " + mDataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT));
            sb.append("\nActual Incline: " + mDataMatrix.getFloat(HALKeys.DATA_ACTUAL_INCLINE_FLOAT));
            sb.append("\nResistance: " + mDataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT));
            sb.append("\nRPM: " + mDataMatrix.getInt(HALKeys.DATA_RPM_INT));
            sb.append("\nSPM: " + mDataMatrix.getInt(HALKeys.DATA_SPM_INT));
            mTextData.setText(sb.toString());
        });

        // demo mode
        mTextDemoMode = findViewById(R.id.text_demo_mode);
        mDataMatrix.createBoolAdapter(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, value -> mTextDemoMode.setText(value ? "ON" : "OFF")); //NON-NLS
        findViewById(R.id.button_demo_mode_on).setOnClickListener(view -> mDataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, true));
        findViewById(R.id.button_demo_mode_off).setOnClickListener(view -> mDataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, false));

        // adb wifi
        findViewById(R.id.button_adb_wifi).setOnClickListener(v -> {
            mDataMatrix.sendEvent(LoggerKeys.ACTION_ENABLE_ADB_WIFI);
            String strIp = AddressHelper.getIPAddress("wlan0"); //NON-NLS
            if (strIp.isEmpty()) strIp = AddressHelper.getIPAddress("eth0"); //NON-NLS
            ((TextView) findViewById(R.id.text_ip)).setText(strIp);
        });

        // frame info
        mTextFrameType = findViewById(R.id.text_frame_type);
        mTextMachineSetupStatus = findViewById(R.id.text_machine_setup_status);
        mDataMatrix.createStringAdapter(SettingKeys.General.DATA_FRAME_TYPE_STRING, value -> {
            mTextFrameType.setText("Frame Type: " + value); //NON-NLS
            updateFrame();
        });
        updateFrame();
    }

    public void dismiss() {
        mDataMatrix.finish();
    }

    public static void showDlg() {
        if (sDialog != null && sDialog.isShowing()) return;

        try {
            Context context = ContextUtil.getApplicationContext();
            sDialog = new Dialog(context);
            final DebugHalDlg debugDlg = new DebugHalDlg(context);
            sDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            sDialog.setContentView(debugDlg);
            sDialog.setCanceledOnTouchOutside(true);
            sDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.argb(100, 255, 255, 255)));
            sDialog.setOnDismissListener(dialog -> debugDlg.dismiss());
            sDialog.getWindow().setType(ChimeraEnums.TYPE_SYSTEM_ALERT);
            sDialog.show();

        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    void updateFrame() {
        findViewById(R.id.layout_incline).setVisibility(GONE); // no longer needed with ui panels
        findViewById(R.id.layout_resistance).setVisibility(GONE); // no longer needed with ui panels
        findViewById(R.id.layout_speed).setVisibility(GONE); // no longer needed with ui panels
        findViewById(R.id.layout_rpm).setVisibility(VISIBLE);
        switch (sFrameConfigManager.getFrameConfig().machineClassType) {
            case Treadmill:
                findViewById(R.id.layout_resistance).setVisibility(GONE);
                findViewById(R.id.layout_rpm).setVisibility(GONE);
                break;
            case Ascent:
                findViewById(R.id.layout_speed).setVisibility(GONE);
                break;
            case Elliptical:
            case RecumbentElliptical:
            case Bike:
            case SpinBike:
                findViewById(R.id.layout_incline).setVisibility(GONE);
                findViewById(R.id.layout_speed).setVisibility(GONE);
                break;
            case Climbmill:
                findViewById(R.id.layout_incline).setVisibility(GONE);
                findViewById(R.id.layout_speed).setVisibility(GONE);
                findViewById(R.id.layout_rpm).setVisibility(GONE);
                break;
        }
    }
}
