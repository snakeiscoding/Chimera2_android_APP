package com.jht.chimera.ui.machinesettings.pages;

import static com.jht.chimera.lib.common.context.ContextUtil.getApplicationContext;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.string.StringUtil;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.widget.CommonRoutines;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DataUtils;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IndoorBike.IndoorBikeCalibrationData;
import com.jht.chimera.ui.machinesettings.EngineeringIdleTimerManager;
import com.jht.chimera.ui.machinesettings.dialogs.CalibrateDialog;
import com.jht.chimera.ui.machinesettings.dialogs.ProjectDialogFactory;
import com.jht.chimera.ui.machinesettings.dialogs.SpinBikeCalibrationDialog;
import com.jht.chimera.ui.machinesettings.factory.IcrZeroSensorActivity;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.oauthlib.auth.BksCredentialProvider;

import org.jetbrains.annotations.NonNls;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Implements the calibration page gluing the page to the model layer.
 */
public class Calibration extends SettingPageBase {
    private final String TAG = "Calibration";
    private DataMatrix dataMatrix = new DataMatrix();
    private FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
    private DialogBase dialog;
    private Handler handler;
    private View view;
    private TimerTask _calibrationCountDownTask = null;
    private Timer _calibrationTimer = null;
    private int climbmillCalibrationSeconds = 0;
    private boolean startMagnetTuning = false;
    private boolean showSimple = false;
    private TableLayout calibrationDataTable;
    private boolean attemptingCalibrationSettingsUpdateFromCloud;
    private String frameSerialNumber;   // leave here instead of in method so it doesn't get garbage collected
    private TextView raptorNetworkRestoreMessage;
    public Handler mHandler = new Handler();
    private Button restoreFromUSBButton;
    private String mStringRaptorParaUSB;
    private final int EXPECTED_PARA_STRING_LENGTH = 168 + 2;  // #parameters 12 + 168 field strings

    public boolean wasCalibrationDone = false;
    private String getAdjustedFrameSn() {
        String rawSn = DataMatrix.getString (SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, "");
        return (rawSn != null && rawSn.length() > 1) ? rawSn.substring(1) : "";
    }

    public enum CalState {
        READY(0),
        CALIBRATING(1),
        SUCCESS(2),
        ERROR(3);

        private int value;

        CalState(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public Calibration() {
        handler = new Handler();
    }

    public Calibration(boolean showSimple) {
        this();
        this.showSimple = showSimple;
    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        view = LayoutInflater.from(context).inflate(R.layout.calibration, null);
        container.addView(view);
        DataMatrix.putInt(SettingKeys.Calibration.DATA_TREADMILL_MAGNET_ANGLE_INT, -1);
        return view;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        DataMatrix.putBoolean(HALKeys.ACTION_SHOW_CZ_IR_FREQ_BOOL, false);
    }

    private void toggleCalibration() {
        boolean start = !(DataMatrix.getInt(HALKeys.DATA_CALIBRATION_STATUS_INT) == CalState.CALIBRATING.getValue());
        DataMatrix.putBoolean(HALKeys.ACTION_CALIBRATE_BOOL, start);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);
        boolean isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
        climbmillCalibrationSeconds = DataMatrix.getInt(SettingKeys.Calibration.DATA_CLIMBMILL_CAL_TIME_SEC_INT);
        switch (config.machineClassType) {

            case Treadmill:
                findViewById(R.id.eng_calibration_treadmill).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_calibration_ascent).setVisibility(View.GONE);
                findViewById(R.id.eng_calibration_climbmill).setVisibility(View.GONE);
                findViewById(R.id.eng_spindown_calibration_layout).setVisibility(View.GONE);

                if (ProductConfig.isRetail()) {
                    // not all frame support treadmill calibration
                    if (!config.SUPPORT_RETAIL_TM_CALIBRATION) findViewById(R.id.eng_calibration_treadmill).setVisibility(View.GONE);

                    if (showSimple) {
                        // hide the manual offset for simple view
                        findViewById(R.id.eng_calibration_tm_min_incline_adc).setEnabled(false);
                        findViewById(R.id.eng_calibration_tm_max_incline_adc).setEnabled(false);
                    }
                    // the offset doesn't seem to be used anywhere so hide them
                    findViewById(R.id.eng_calibration_tm_min_incline_offset).setVisibility(View.GONE);
                    findViewById(R.id.eng_calibration_tm_max_incline_offset).setVisibility(View.GONE);
                }
                break;
            case Ascent:
                findViewById(R.id.eng_calibration_treadmill).setVisibility(View.GONE);
                findViewById(R.id.eng_calibration_ascent).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_calibration_climbmill).setVisibility(View.GONE);
                findViewById(R.id.eng_spindown_calibration_layout).setVisibility(View.GONE);

                if (ProductConfig.isRetail()) {
                    // Parker ascent does not support frame calibration
                    // TODO probably need to add a SUPPORT_FRAME_CALIBRATION to frame config and update all frames to make this cleaner
                    findViewById(R.id.eng_calibration_ascent).setVisibility(View.GONE);
                }
                break;
            case Climbmill:
                findViewById(R.id.eng_calibration_treadmill).setVisibility(View.GONE);
                findViewById(R.id.eng_calibration_ascent).setVisibility(View.GONE);
                findViewById(R.id.eng_calibration_climbmill).setVisibility(View.VISIBLE);
//                findViewById(R.id.eng_calibration_cz_frequency).setVisibility(FrameConfigEnum.getFrameTypeFromString(DataMatrix.getString(SettingKeys.General.DATA_FRAME_TYPE_STRING)).equals(Climbmill_Performance) ? View.VISIBLE :View.GONE);
                findViewById(R.id.eng_calibration_cz_frequency).setVisibility(config.HAS_CM_CONTROL_ZONE ? View.VISIBLE :View.GONE);
                findViewById(R.id.eng_spindown_calibration_layout).setVisibility(View.GONE);
                break;
            case SpinBike:
                findViewById(R.id.eng_calibration_treadmill).setVisibility(View.GONE);
                findViewById(R.id.eng_calibration_ascent).setVisibility(View.GONE);
                findViewById(R.id.eng_calibration_climbmill).setVisibility(View.GONE);
                findViewById(R.id.eng_spindown_calibration_layout).setVisibility(View.VISIBLE);
                if (config.supportsLcbType(LcbType.LCB_JIS_ATHENA.Value)) {
                    findViewById(R.id.spindownCalibration).setVisibility(View.GONE); // ICR does not currently support spin down calibration
                    Button button = findViewById(R.id.buttonICSetTorqueCalibration);
                    button.setOnClickListener(v -> {
                        LaunchAppUtil.launchInternalScreen(IcrZeroSensorActivity.class.getCanonicalName());
                    });
                    button.setVisibility(View.VISIBLE);
                }
                break;
            default:
                findViewById(R.id.eng_calibration_treadmill).setVisibility(View.GONE);
                findViewById(R.id.eng_calibration_ascent).setVisibility(View.GONE);
                findViewById(R.id.eng_calibration_climbmill).setVisibility(View.GONE);
                findViewById(R.id.eng_spindown_calibration_layout).setVisibility(View.GONE);
                break;
        }

        dataMatrix.createIntAdapter(HALKeys.DATA_SAFETY_KEY_STATUS_INT, (safetyStatus) -> {
            if (safetyStatus != ChimeraEnums.SAFETY_KEY_NOT_PULLED) {
                boolean isCalibrationStarted = (DataMatrix.getInt(HALKeys.DATA_CALIBRATION_STATUS_INT) == CalState.CALIBRATING.getValue());
                if (isCalibrationStarted) {
                    DataMatrix.putBoolean(HALKeys.ACTION_CALIBRATE_BOOL, false);
                    dialog.hide();
                }
            }
        }, false);

        dataMatrix.createIntAdapter(HALKeys.DATA_CALIBRATION_STATUS_INT, status -> {

            if (status == CalState.SUCCESS.getValue()) {
                idleTimers(true);
                if(dialog != null && dialog.isShown()) {
                   dialog.hide();
                }
                dialog = ProjectDialogFactory.message(view, getContext().getResources().getString(R.string.calibration_success_tc), () -> idleTimers(true), SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                stopCountDown();

                // refresh some values
                {
                    UserTextInput userTextInput = view.findViewById(R.id.eng_calibration_tm_min_incline_adc);
                    userTextInput.setText("" + DataMatrix.getInt(SettingKeys.Calibration.DATA_TREADMILL_INLINE_ADC_MIN_INT));
                }
                {
                    UserTextInput userTextInput = view.findViewById(R.id.eng_calibration_tm_max_incline_adc);
                    userTextInput.setText("" + DataMatrix.getInt(SettingKeys.Calibration.DATA_TREADMILL_INLINE_ADC_MAX_INT));
                }
                {
                    UserTextInput userTextInput = view.findViewById(R.id.eng_calibration_tm_min_incline_offset);
                    userTextInput.setText("" + DataMatrix.getInt(SettingKeys.Calibration.DATA_TREADMILL_INCLINE_ADC_OFFSET_MIN_INT));
                }
                {
                    UserTextInput userTextInput = view.findViewById(R.id.eng_calibration_tm_max_incline_offset);
                    userTextInput.setText("" + DataMatrix.getInt(SettingKeys.Calibration.DATA_TREADMILL_INCLINE_ADC_OFFSET_MAX_INT));
                }
                wasCalibrationDone = true;
				//Calibration Complete Success then set to READY
	    	    DataMatrix.putInt(HALKeys.DATA_CALIBRATION_STATUS_INT, CalState.READY.getValue());
            } else if (status == CalState.ERROR.getValue()) {
                idleTimers(true);
                if(dialog != null) {
                    if (dialog.isShown())
                        dialog.hide();
                }
                dialog = ProjectDialogFactory.message(view, getContext().getResources().getString(R.string.calibration_failure_2_tc).replace("%1", "-1"), () -> idleTimers(true), SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                stopCountDown();
				//Calibration Complete Error then set to READY
	      		  DataMatrix.putInt(HALKeys.DATA_CALIBRATION_STATUS_INT, CalState.READY.getValue());
            }
        });

        {
            CheckBox checkBox = view.findViewById(R.id.eng_calibration_use_lcb_watts);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.Calibration.DATA_CLIMBMILL_USE_LCB_WATTS_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.Calibration.DATA_CLIMBMILL_USE_LCB_WATTS_BOOL, isChecked));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_calibration_en_cm_time);
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.Calibration.DATA_CLIMBMILL_CAL_TIME_SEC_INT));
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.Calibration.DATA_CLIMBMILL_CAL_TIME_SEC_INT, CommonRoutines.parseInt(s)));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_calibration_en_cm_spm);
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.Calibration.DATA_CLIMBMILL_CAL_SPM_INT));
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.Calibration.DATA_CLIMBMILL_CAL_SPM_INT, CommonRoutines.parseInt(s)));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_calibration_en_cm_factor);
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.Calibration.DATA_CLIMBMILL_CAL_FACTOR_INT));
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.Calibration.DATA_CLIMBMILL_CAL_FACTOR_INT, CommonRoutines.parseInt(s)));
			dataMatrix.createIntAdapter(HALKeys.DATA_CLIMBMILL_FRAME_SCALE_INT, value -> {
			DataMatrix.putInt(SettingKeys.Calibration.DATA_CLIMBMILL_CAL_FACTOR_INT, value);
           		userTextInput.setText("" + DataMatrix.getInt(SettingKeys.Calibration.DATA_CLIMBMILL_CAL_FACTOR_INT));
			});
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_calibration_en_cm_weight);
            String text;
            if (isMetric) {
                userTextInput.setInputProperties(userTextInput.getInputProperties()
                        .min(config.weightKg.getMin())
                        .max(config.weightKg.getMax())
                        .units(CommonRoutines.getUnit()));
                text = "" + CommonRoutines.round(DataMatrix.getFloat(SettingKeys.Calibration.DATA_CLIMBMILL_CAL_WEIGHT_KG_FLOAT), 0);
            } else {
                userTextInput.setInputProperties(userTextInput.getInputProperties()
                        .min(config.weightLbs.getMin())
                        .max(config.weightLbs.getMax())
                        .units(CommonRoutines.getUnit()));
                text = "" + CommonRoutines.round(WeightConvertUtil.convertKilogramsToPounds(DataMatrix.getFloat(SettingKeys.Calibration.DATA_CLIMBMILL_CAL_WEIGHT_KG_FLOAT)), 0);

            }
            userTextInput.setText(text);
            userTextInput.setOnTextChanged(s -> {
                float value = CommonRoutines.parseFloat(s) / CommonRoutines.getConvertFactorWeight();
                DataMatrix.putFloat(SettingKeys.Calibration.DATA_CLIMBMILL_CAL_WEIGHT_KG_FLOAT, value);
            });
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_calibration_cz_frequency);
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.Calibration.DATA_CLIMBMILL_CZ_FREQUENCY_INT));
	        dataMatrix.createIntAdapter(SettingKeys.Calibration.DATA_CLIMBMILL_CZ_FREQUENCY_INT, value -> userTextInput.setText("" + value));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_calibration_ir_frequency);
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.Calibration.DATA_CLIMBMILL_IR_FREQUENCY_INT));
	        dataMatrix.createIntAdapter(SettingKeys.Calibration.DATA_CLIMBMILL_IR_FREQUENCY_INT, value -> userTextInput.setText("" + value));
        }


        {

            dataMatrix.createIntAdapter(SettingKeys.Calibration.DATA_TREADMILL_MAGNET_ANGLE_INT, value -> {
                if (value > 0) {
                    float magnet_angle = (float) (value * 0.1);
                    String result = "\n" + magnet_angle;
                    if (startMagnetTuning) {
                        if (dialog != null && dialog.isShown()) {
                            dialog.hide();
                        }
                        if (value == 255)
                            dialog = ProjectDialogFactory.message(view, getContext().getResources().getString(R.string.tuning_magnet_fail_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.NONE);
                        else
                            dialog = ProjectDialogFactory.message(view, getContext().getResources().getString(R.string.tuning_magnet_complete_pg) + result, null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.NONE);
                    }
                    else
                        dialog = ProjectDialogFactory.message(view, getContext().getResources().getString(R.string.magnet_angle_tc) + result, null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                }
            });
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_calibration_tm_min_incline_adc);
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.Calibration.DATA_TREADMILL_INLINE_ADC_MIN_INT));
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.Calibration.DATA_TREADMILL_INLINE_ADC_MIN_INT, CommonRoutines.parseInt(s)));
        }
        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_calibration_tm_max_incline_adc);
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.Calibration.DATA_TREADMILL_INLINE_ADC_MAX_INT));
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.Calibration.DATA_TREADMILL_INLINE_ADC_MAX_INT, CommonRoutines.parseInt(s)));
        }
        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_calibration_tm_min_incline_offset);
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.Calibration.DATA_TREADMILL_INCLINE_ADC_OFFSET_MIN_INT));
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.Calibration.DATA_TREADMILL_INCLINE_ADC_OFFSET_MIN_INT, CommonRoutines.parseInt(s)));
        }
        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_calibration_tm_max_incline_offset);
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.Calibration.DATA_TREADMILL_INCLINE_ADC_OFFSET_MAX_INT));
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.Calibration.DATA_TREADMILL_INCLINE_ADC_OFFSET_MAX_INT, CommonRoutines.parseInt(s)));
        }

        view.findViewById(R.id.performance_plus_tm_calibration).setVisibility(View.GONE);
        view.findViewById(R.id.eng_calibration_treadmill_button).setOnClickListener(this::calibrate);
        view.findViewById(R.id.eng_magnet_angle_get_treadmill_button).setOnClickListener(v -> {
            DataMatrix.sendEvent(HALKeys.EVENT_GET_MAGNENT_ANGLE);
                startMagnetTuning = false;
        });
        view.findViewById(R.id.eng_magnet_angle_calibration_treadmill_button).setOnClickListener(v -> {
            DataMatrix.sendEvent(HALKeys.EVENT_AUTO_TUNE_MAGNENT_ANGLE);
            startMagnetTuning = true;
            idleTimers(false);
            dialog = ProjectDialogFactory.message(view, getContext().getResources().getString(R.string.tuning_magnet_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.NONE);
        });
        view.findViewById(R.id.eng_calibration_ascent_button).setOnClickListener(this::calibrate);
        view.findViewById(R.id.eng_calibration_en_cm_calibrate).setOnClickListener(this::calibrate);
        switch (config.machineClassType) {
            case Treadmill:
                if(config.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS) {
                    view.findViewById(R.id.performance_plus_tm_calibration).setVisibility(View.VISIBLE);
                }
                break;
            case Climbmill:
                // TODO???
                //Start update CZ IR Frg
                DataMatrix.putBoolean(HALKeys.ACTION_SHOW_CZ_IR_FREQ_BOOL, true);
                break;
            default:
                break;
        }

        /////// Start of Athena Bike Things
        view.findViewById(R.id.spindownCalibration).setOnClickListener(this::spinDownCalibrate);
        calibrationDataTable = view.findViewById(R.id.calibrationDataTable);

        dataMatrix.createStringAdapter(SettingKeys.Calibration.DATA_SETTING_RAPTOR_PARA_DATA_STRING, value -> createIndoorCycleCalibrationTable(value));
        dataMatrix.createIntAdapter(HALKeys.DATA_INFO_SPIN_DOWN_RPM_INT, value -> {
			//TO DO
            // Used in Spin down dialog
        });
        dataMatrix.createFloatAdapter(HALKeys.DATA_INFO_SPIN_DOWN_TORQUE_SCALAR_FLOAT, value -> {
			//TO DO
        });
        dataMatrix.createFloatAdapter(HALKeys.DATA_INFO_SPIN_DOWN_CORRECTION_FACTOR_FLOAT, value -> {
			//TO DO
        });
        dataMatrix.createFloatAdapter(HALKeys.DATA_INFO_SPIN_DOWN_INERTIA_FLOAT, value -> {
			//TO DO
        });

        findViewById(R.id.restoreFromNetwork).setOnClickListener(v -> restoreCalibrationSettingsFromNetwork());
        restoreFromUSBButton = findViewById(R.id.restoreFromUSB);
        restoreFromUSBButton.setEnabled(false);
        restoreFromUSBButton.setOnClickListener(v -> {
            if (mStringRaptorParaUSB == null || mStringRaptorParaUSB.isEmpty()) return;
            if (validateParameterCalibrationString(mStringRaptorParaUSB))
            {
                DataMatrix.putString(SettingKeys.Calibration.DATA_SETTING_RAPTOR_PARA_DATA_STRING, mStringRaptorParaUSB);
                DataMatrix.sendEvent(HALKeys.ACTION_RAPTOR_PARA_BACKUP);
                raptorNetworkRestoreMessage.post(() -> raptorNetworkRestoreMessage.setText(StringLookup.get(R.string.success_lc).toUpperCase()));
            } else {
                raptorNetworkRestoreMessage.post(() -> raptorNetworkRestoreMessage.setText(StringLookup.get(R.string.did_not_find_calibration_data_tc).toUpperCase()));
            }
        });

        findViewById(R.id.pushToLCB).setOnClickListener(v -> saveCalibrationSettingsToLCB());
        findViewById(R.id.retrieveFromLCB).setOnClickListener(v -> retrieveCalibrationSettingsFromLCB());

        raptorNetworkRestoreMessage =  findViewById(R.id.raptorNetworkRestoreMessage);

        // Check in case we went into this routine with a USB stick already stuck in the console
        refreshRaptorParaBackupUSB(StorageUtils.getUdisk2Path()); //NON-NLS
        if (mStringRaptorParaUSB == null || mStringRaptorParaUSB.contentEquals("")) {
            refreshRaptorParaBackupUSB(StorageUtils.getUdiskPath()); //NON-NLS
        }

        // Back door to aid in verification testing.  Just clears the table and internal string
        findViewById (R.id.currentCalibrationSettingsLabel).setOnLongClickListener(v ->
        {
            DataMatrix.putString(SettingKeys.Calibration.DATA_SETTING_RAPTOR_PARA_DATA_STRING, "");
            calibrationDataTable.removeAllViews();
            return false;
        });

        if (config.machineClassType == JHTCommonTypes.MachineClassificationType.SpinBike) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            getContext().registerReceiver(mUsbMountedReceiver, filter);
        }

        /////// End of Athena Bike Things
    }

    private void saveCalibrationSettingsToLCB() {
        DataMatrix.sendEvent(HALKeys.ACTION_RAPTOR_PARA_BACKUP);
    }

    private void retrieveCalibrationSettingsFromLCB() {
        DataMatrix.sendEvent(HALKeys.ACTION_RAPTOR_PARA_RESTORE);
    }

    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        dataMatrix.finish();
    }

    private SpinBikeCalibrationDialog spinDownWelcomeDialog;
    private void spinDownCalibrate (View v) {
        spinDownWelcomeDialog = ProjectDialogFactory.spindownWelcomeDialog (v);
    }

    private void calibrate(View v) {

        //Incline moving, don't start calibration
        if (DataMatrix.getBoolean(HALKeys.DATA_INCLINE_MOVING_BOOL)) {
            if(dialog != null) {
                if (dialog.isShown())
                    dialog.hide();
            }
            dialog = ProjectDialogFactory.message(v, getContext().getResources().getString(R.string.calibration_aborted_pg), () -> idleTimers(true), SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
        } else {
            toggleCalibration();
            idleTimers(false);
            climbmillCalibrationSeconds = DataMatrix.getInt(SettingKeys.Calibration.DATA_CLIMBMILL_CAL_TIME_SEC_INT);

            String title = "";
            String message = "";
            switch (config.machineClassType) {
                case Treadmill:
                    title = getContext().getString(R.string.calibrating_treadmill_tc);
                    message = "";
                    break;
                case Ascent:
                    title = getContext().getString(R.string.calibrating_ascent_tc);
                    message = "";
                    break;
                case Climbmill:
                    title = getContext().getString(R.string.calibrating_en_climbmill_tc);
                    message = getContext().getString(R.string.calibrating_ascent_pg);
                    startCountDown();
                    break;
                default:
                    break;
            }
            if(dialog != null && dialog.isShown()) {
                dialog.hide();
            }
            dialog = ProjectDialogFactory.calibrateDialog(v, title, message, true, stopCalibration, CalibrateDialog.CALIBRATE_BUTTON_STATE.NONE);
        }

    }

    private CalibrateDialog.IOnCalibrateDialog stopCalibration = this::toggleCalibration;

    private void startCountDown() {
        if (_calibrationTimer != null) {
            _calibrationTimer.cancel();
        }

        _calibrationCountDownTask = new TimerTask() {
            @Override
            public void run() {
                    handler.post(() -> {
                        climbmillCalibrationSeconds --;
                        if(climbmillCalibrationSeconds < 0){
                            stopCountDown();
            climbmillCalibrationSeconds = 0;
                        }else{
                            if (dialog != null && dialog instanceof CalibrateDialog) {
                                String message = getContext().getResources().getString(R.string.calibrating_en_climbmill_pg).replace("%1", "" + climbmillCalibrationSeconds);
//                              String message = getContext().getString(R.string.calibrating_en_climbmill_pg);
                                ((CalibrateDialog) dialog).setMessage(message);
                            }
                        }

                    });
            }
        };

        _calibrationTimer = new Timer();
        _calibrationTimer.schedule(_calibrationCountDownTask, 10000, 1000);
    }

    private void stopCountDown() {
        if (_calibrationTimer != null) {
            _calibrationTimer.cancel();
            _calibrationTimer = null;
            _calibrationCountDownTask = null;
        }
        switch (config.machineClassType) {
            case Climbmill:
	            DataMatrix.sendEvent(HALKeys.EVENT_GET_CLIMB_FRAME_CAL_SCALING);
                break;
        }
        
    }

    private String checkAndFixIfNeededParameterString(String calibHexString) {
        Log.d(TAG, "Calibration HEX string check(" + calibHexString.length() + ")=" + calibHexString);
        if (calibHexString.startsWith ("555555")) {
            // 15 is the number of parameters in the string - that needs to be preappended to some strings.
            // parameter calibration strings that come in from the network or a USB restore do not include
            // this 2 digit number.    Strings coming from LCB, will contain the # of parameters bytes.
            return ("15" + calibHexString);
        }
        return calibHexString;
    }

    private boolean validateParameterCalibrationString (String calString)
    {
        if (calString.length() != EXPECTED_PARA_STRING_LENGTH) {
            Log.e (TAG, "Calibration string incorrect length (" + calString.length() + ") " + calString);
            return false;
        }

        if (!calString.startsWith("15555555")) {
            Log.e (TAG, "Calibration string incorrect format (" + calString.length() + ") " + calString);
            return false;
        }
        return true;
    }

    private void createIndoorCycleCalibrationTable(String calibHexString) {
        calibrationDataTable.removeAllViews();

        // check and report if the string is messed up
        if (!validateParameterCalibrationString(calibHexString)) {
            // Next line is in place to help desk/demo mode testing.
            calibHexString = checkAndFixIfNeededParameterString(calibHexString);
        }
        Log.d(TAG, "Calibration display HEX string(" + calibHexString.length() + ")=" + calibHexString);

        IndoorBikeCalibrationData data = IndoorBikeCalibrationData.fromBytes(DataUtils.hexStringToByteArray(calibHexString), 0);
        if (data != null) {
            addIndoorCycleCalibrationTableRow(data.getHeader());
            for (IndoorBikeCalibrationData.BikeParameter p : data.getParameters()) {
                if (!p.isReserved()) addIndoorCycleCalibrationTableRow(p);
            }
        } else {
            final String errantCalibString = calibHexString;
            raptorNetworkRestoreMessage.post(() -> raptorNetworkRestoreMessage.setText(errantCalibString));
        }
    }
    private void addIndoorCycleCalibrationTableRow(IndoorBikeCalibrationData.BikeParameter p) {
        TableRow inflatedRow = (TableRow) View.inflate(calibrationDataTable.getContext(), R.layout.cycle_calibration_data_table_row, null);
        TextView labelView = inflatedRow.findViewById(R.id.labelView);
        labelView.setText(p.name);
        TextView dataView = inflatedRow.findViewById(R.id.dataView);
        dataView.setText(p.getValue());
        calibrationDataTable.addView(inflatedRow);
    }

    private void restoreCalibrationSettingsFromNetwork() {
        final Thread thread = new Thread(() -> {
            try {
                attemptingCalibrationSettingsUpdateFromCloud = true;

                frameSerialNumber = getAdjustedFrameSn();

                raptorNetworkRestoreMessage.post(() -> raptorNetworkRestoreMessage.setText(StringLookup.get(R.string.attempting_to_download_calibration_data_for_frame_tc) + " " + frameSerialNumber));

                String address = String.format (Locale.ENGLISH,"https://dapi.jfit.co/prod/machine/calibration?key=%s", frameSerialNumber);
                URL url = new URL(address);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("cache-control", "no-cache");
                urlConnection.setRequestProperty("content-type", "application/json");
                BksCredentialProvider provider = new BksCredentialProvider(getContext().getApplicationContext());
                String apiKey = provider.getString("CALIBRATION_API_KEY");
                urlConnection.setRequestProperty("x-api-key", apiKey);
                try {
                    BufferedReader  in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        //Log.d(TAG, "Hey Data->" + inputLine);

                        if (inputLine.contains("5555")) {
                            Log.d(TAG, "Calibration from network string->" + inputLine);

                            raptorNetworkRestoreMessage.postDelayed(() -> raptorNetworkRestoreMessage.setText(getContext().getString(R.string.retreived_calibration_data_tc)), 1000);
                            processCalibrationString(inputLine);
                            break;
                        }
                    }
                    in.close();
                } finally {
                    urlConnection.disconnect();
                }

                Thread.sleep(5000);
            } catch (Exception e) {
                Log.e (TAG, "", e);
            }
        });
        thread.start();

        mHandler.postDelayed(() -> {
            if (attemptingCalibrationSettingsUpdateFromCloud) {
                attemptingCalibrationSettingsUpdateFromCloud = false;

                raptorNetworkRestoreMessage.post(() -> raptorNetworkRestoreMessage.setText(getContext().getString(R.string.failed_to_download_calibration_pg)));
            }
            else
            {
                // Tracking down a crash from CGC-23841
                raptorNetworkRestoreMessage.post(() -> raptorNetworkRestoreMessage.setText(StringUtil.capitalize(getContext().getResources().getConfiguration().getLocales().get(0), getContext().getString(R.string.success_lc))));
            }
        }, 15 * 1000);
    }

    private String getCalibFilePath ()
    {
        String frameSerialNumber = getAdjustedFrameSn();
        return getApplicationContext().getFilesDir().getAbsolutePath() + "/" + frameSerialNumber + ".cal"; //NON-NLS
    }

    private void deleteCalibrationDownloadFile()
    {
        String filePath = getCalibFilePath();

        try {
            File fp = new File(filePath);
            if (fp.exists()) {
                Log.d(TAG, "Deleting " + fp.getAbsolutePath());
                fp.delete();
            }
        } catch (Exception ex) { // eat it
        }
    }

    @SuppressLint("SetTextI18n")
    private void refreshRaptorParaBackupUSB(@NonNls String folderNameToScan) {

        File folderToScan = new File(folderNameToScan);

        Log.d (TAG, "scanning directory " + folderNameToScan);

        String frameSerialNumber = getAdjustedFrameSn();
        String fileToMatch = frameSerialNumber + "_calibration_restore"; //NON-NLS
        Log.d (TAG, "Calibration USB restore, looking for " + fileToMatch);

        File[] listOfFiles = folderToScan.listFiles();

        if (listOfFiles != null && listOfFiles.length > 0) {
            for (File file : listOfFiles) {
                Log.d (TAG, "found file " + file.getName());

                if (file.getName().contains(fileToMatch)) {
                    try {
                        Log.d (TAG, "found file name match " + file.getName());
                        BufferedReader mBufferedReader = new BufferedReader(new FileReader(file));
                        String line = mBufferedReader.readLine();
                        mBufferedReader.close();

                        if (line.startsWith("5555")) {
                            mStringRaptorParaUSB = checkAndFixIfNeededParameterString(line);
                            if (validateParameterCalibrationString(mStringRaptorParaUSB)) {
                                postedRestoreButtonEnable(true);
                                raptorNetworkRestoreMessage.post(() -> raptorNetworkRestoreMessage.setText(StringLookup.get(R.string.found_calibration_data_tc) + file.getName()));
                            }
                            break;
                        } else {
                            mStringRaptorParaUSB = "";
                            postedRestoreButtonEnable(false);
                            raptorNetworkRestoreMessage.post(() -> raptorNetworkRestoreMessage.setText(StringLookup.get(R.string.did_not_find_calibration_data_tc) + file.getName()));
                        }
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        Log.w(TAG, e);
                    }
                }
            }
        }
    }

    private void postedRestoreButtonEnable (boolean enableFlag) {
        restoreFromUSBButton.post(() -> restoreFromUSBButton.setEnabled(enableFlag));
    }

    // Convert the json object and pull out the calibration data of interest
    private void processCalibrationString(String dataLine) {
        if (dataLine.contains("5555")) {
            JsonObject jobj = new Gson().fromJson(dataLine, JsonObject.class);

            mStringRaptorParaUSB = jobj.get("hex_string").getAsString(); //NON-NLS
            mStringRaptorParaUSB = checkAndFixIfNeededParameterString(mStringRaptorParaUSB);
            Log.d(TAG, "Processing Calibration Data ");
            raptorNetworkRestoreMessage.postDelayed(() -> raptorNetworkRestoreMessage.setText(R.string.processing_calibration_data_tc), 1000);
            DataMatrix.putString(SettingKeys.Calibration.DATA_SETTING_RAPTOR_PARA_DATA_STRING, mStringRaptorParaUSB);
            DataMatrix.sendEvent(HALKeys.ACTION_RAPTOR_PARA_BACKUP);
            attemptingCalibrationSettingsUpdateFromCloud = false;
            raptorNetworkRestoreMessage.postDelayed(() -> raptorNetworkRestoreMessage.setText(R.string.success_lc), 1000);
        } else {
            Log.d(TAG, "Did NOT find Calibration Data " + dataLine);
            mStringRaptorParaUSB = "";
            raptorNetworkRestoreMessage.postDelayed(() -> raptorNetworkRestoreMessage.setText(R.string.did_not_find_calibration_data_tc), 3000);
        }
    }

    private BroadcastReceiver mUsbMountedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Log.d (TAG, "USB receiver action");

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {

                postedRestoreButtonEnable(false);
                mStringRaptorParaUSB =  "";

                Log.d (TAG, "Media insertion detected - thread scanning");
                Thread lookForCalFileThread = new Thread(() -> {
                    int tries = 0;
                    while ((mStringRaptorParaUSB == null || mStringRaptorParaUSB.contentEquals("")) && tries < 10) {
                        try {
                            Log.d (TAG, "scanning try " + tries);
                            refreshRaptorParaBackupUSB(StorageUtils.getUdisk2Path());
                            if (mStringRaptorParaUSB == null || mStringRaptorParaUSB.contentEquals("")) {
                                refreshRaptorParaBackupUSB(StorageUtils.getUdiskPath());
                            }
                            Thread.sleep(1000);
                        } catch (Exception ignored) {
                        }

                        tries++;
                    }

                });
                lookForCalFileThread.start();
            }
        }
    };

    private void idleTimers(boolean enabled) {
        if (EngineeringIdleTimerManager.idleTimerManagement == null) return;
        if (enabled) {
            EngineeringIdleTimerManager.idleTimerManagement.monitorTimers();
        } else {
            EngineeringIdleTimerManager.idleTimerManagement.stopMonitoring();
        }
    }
}
