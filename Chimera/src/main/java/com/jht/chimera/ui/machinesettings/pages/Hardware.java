package com.jht.chimera.ui.machinesettings.pages;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.SystemBrightness;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.conversionlib.DistanceConvertUtil;
import com.jht.chimera.lib.common.dapi.DapiLogin;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.rfid.PFBarcodeData;
import com.jht.chimera.lib.common.string.StringUtil;
import com.jht.chimera.service.hal.lib.hardware.DisplayAdjustment;
import com.jht.chimera.service.hal.lib.hardware.DisplayAdjustmentKt;
import com.jht.chimera.service.logger.BeltLubeManager;
import com.jht.chimera.ui.machinesettings.frictiontest.FrictionTestActivity;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.rfid.Legic.RFIDChip;
import com.jhtp.dapiservice.an.types.ButtonPressType;

import java.util.Calendar;
import java.util.Locale;


/**
 * Implements the hardware page gluing the page to the model layer.
 */
public class Hardware extends SettingPageBase {
    private static final String TAG = "Hardware";
    private DataMatrix dataMatrix = new DataMatrix();
    private FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
    private View view;

    private UserTextInput tagType;
    private UserTextInput uid;
    private UserTextInput dec;
    private boolean showSimple = false;
    private RFIDChip chip;
    private UserTextInput motorType;

    private Handler handler;
    private Handler mHandler = new Handler();

    public Hardware() {
    }

    public Hardware(boolean showSimple) {
        this.showSimple = showSimple;
    }


    @Override
    protected View onCreate(final Context context, ViewGroup container) {
        view = LayoutInflater.from(context).inflate(R.layout.hardware, null);
        container.addView(view);
        handler = new Handler();

        if (ProductConfig.hasAuxAudioInput()) {
            // parker has one headphone jack and one aux audio input
            TextView v = (TextView) view.findViewById(R.id.eng_hardware_headphone_insert_b_label);
            if (v != null) {
                v.setText(R.string.aux_input_tc);
            }
        }

        return view;
    }

    @Override
    public void create(ViewGroup container) {
        super.create(container);
    }

    private void updateERPSettingsState() {
        int erpMode = DataMatrix.getInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_MODE_ENUM_INT);
        ((UserTextInput) view.findViewById(R.id.eng_erp_timeout)).setReadOnly(erpMode != SettingKeys.ERPMode.Inactivity.ordinal());
        ((UserTextInput) view.findViewById(R.id.eng_erp_time)).setReadOnly(erpMode != SettingKeys.ERPMode.Absolute_Time.ordinal());
    }

    protected void updateLubeTime() {
        int timeSinceLube =
            DataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT) -
            DataMatrix.getInt(SettingKeys.Hardware.DATA_ACCUMULATED_TIME_AT_LAST_LUBE_SEC_INT);
        ((TextView)findViewById(R.id.accumulated_time_since_lube_value)).setText(String.format(
            Locale.ENGLISH, "%.1f %s",
            timeSinceLube/3600.0, ContextUtil.getString(R.string.hours_lc)));
    }
    protected void updateLubeDistance() {
        float distanceSinceLube =
            DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT) -
            DataMatrix.getFloat(SettingKeys.Hardware.DATA_ACCUMULATED_DISTANCE_KM_AT_LAST_LUBE_FLOAT);

        boolean isMetric = DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL);
        ((TextView)findViewById(R.id.accumulated_distance_since_lube_value)).setText(String.format(
            Locale.ENGLISH, "%.1f %s",
            isMetric ? distanceSinceLube : DistanceConvertUtil.convertKilometersToMiles(distanceSinceLube),
            ContextUtil.getString(isMetric ? R.string.km_lc : R.string.miles_lc)));
    }

    protected void resetLubeTime() {
        DataMatrix.putInt(SettingKeys.Hardware.DATA_ACCUMULATED_TIME_AT_LAST_LUBE_SEC_INT, DataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT));
    }
    protected void resetLubeDistance() {
        DataMatrix.putFloat(SettingKeys.Hardware.DATA_ACCUMULATED_DISTANCE_KM_AT_LAST_LUBE_FLOAT, DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT));
    }

    private void setupDisplayAdjustmentUi() {
        DisplayAdjustment displayAdjustment = DisplayAdjustment.create();
        if (displayAdjustment.getSupportsColorTemperature()) {
            int colorTemp = DisplayAdjustmentKt.clampColorTemperature(DataMatrix.getInt(SettingKeys.Hardware.DATA_DISPLAY_COLOR_TEMPERATURE_INT));
            SeekBar colorTempSeekBar = view.findViewById(R.id.eng_hardware_color_temp);
            TextView colorThumbText = findViewById(R.id.colorThumbText);
            colorTempSeekBar.setProgress(colorTemp);
            setThumbPosition(colorTempSeekBar, DataMatrix.getInt(SettingKeys.Hardware.DATA_DISPLAY_COLOR_TEMPERATURE_INT), colorThumbText);
            colorThumbText.setOnLongClickListener(v -> {
                Log.d(TAG, "Reset color temperature");
                ((SeekBar) view.findViewById(R.id.eng_hardware_color_temp)).setProgress(DisplayAdjustmentKt.COLOR_TEMPERATURE_DEFAULT);
                return false;
            });
            colorTempSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    DataMatrix.updateInt(SettingKeys.Hardware.DATA_DISPLAY_COLOR_TEMPERATURE_INT, progress);
                    TextView label = findViewById(R.id.colorThumbText);
                    setThumbPosition(seekBar, progress, label);
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    DataMatrix.updateInt(SettingKeys.Hardware.DATA_DISPLAY_COLOR_TEMPERATURE_INT, seekBar.getProgress());
                }
            });
        } else {
            findViewById(R.id.colorTemperature).setVisibility(View.GONE); // hide the whole UI container
        }
    }

    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);

        DataMatrix.putBoolean(HALKeys.DATA_ATHENA_MONITOR_SENSOR_DATA_BOOL, true);
        DataMatrix.putBoolean(HALKeys.DATA_SCAN_RFID_TAGS_BOOL, true);
        setupDisplayAdjustmentUi();

        int progress = DataMatrix.getInt(SettingKeys.Hardware.DATA_BACKLIGHT_PROGRESS_INT);
        if(progress != -1) {
            Log.e(TAG, "about to set progress");
            ((SeekBar) view.findViewById(R.id.eng_hardware_backlight)).setProgress(progress);
        } else {
            ((SeekBar) view.findViewById(R.id.eng_hardware_backlight)).setProgress(SystemBrightness.getUserBrightness(getContext().getContentResolver()));
        }

        TextView backlightThumbText = (TextView) findViewById(R.id.backlightThumbText);
        setThumbPosition((SeekBar) view.findViewById(R.id.eng_hardware_backlight),DataMatrix.getInt(SettingKeys.Hardware.DATA_BACKLIGHT_PROGRESS_INT),backlightThumbText);

        ((SeekBar) view.findViewById(R.id.eng_hardware_backlight)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                DataMatrix.putInt(SettingKeys.Hardware.DATA_BACKLIGHT_PROGRESS_INT, progress);
                TextView label = (TextView) findViewById(R.id.backlightThumbText);
                setThumbPosition(seekBar,progress,label);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                DataMatrix.putInt(SettingKeys.Hardware.DATA_BACKLIGHT_PROGRESS_INT, seekBar.getProgress());
            }
        });

        ((SeekBar) view.findViewById(R.id.eng_hardware_volume)).setProgress(DataMatrix.getInt(SettingKeys.Hardware.DATA_MASTER_VOLUME_V2_INT));
        TextView volumeThumbText = (TextView) findViewById(R.id.volumeThumbText);
        setThumbPosition((SeekBar) view.findViewById(R.id.eng_hardware_volume),DataMatrix.getInt(SettingKeys.Hardware.DATA_MASTER_VOLUME_V2_INT),volumeThumbText);
        ((SeekBar) view.findViewById(R.id.eng_hardware_volume)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TextView label = (TextView) findViewById(R.id.volumeThumbText);
                setThumbPosition(seekBar,progress,label);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                DataMatrix.putInt(SettingKeys.Hardware.DATA_MASTER_VOLUME_V2_INT, seekBar.getProgress());
            }
        });

        if(ProductConfig.platform.equals(ProductConfig.Platform.LCGUI)) {
            view.findViewById(R.id.eng_hardware_headphone_insert_a_label).setVisibility(View.GONE);
            view.findViewById(R.id.eng_hardware_headphone_insert_a).setVisibility(View.GONE);
            view.findViewById(R.id.eng_hardware_insert_a_reset).setVisibility(View.GONE);
            view.findViewById(R.id.eng_hardware_headphone_insert_b_label).setVisibility(View.GONE);
            view.findViewById(R.id.eng_hardware_headphone_insert_b).setVisibility(View.GONE);
            view.findViewById(R.id.eng_hardware_insert_b_reset).setVisibility(View.GONE);
        }

        // TODO swap A and B if they are swapped
        dataMatrix.createIntAdapter(SettingKeys.Hardware.DATA_HEADPHONE_B_COUNT_INT, integer -> ((TextView) view.findViewById(R.id.eng_hardware_headphone_insert_a)).setText("" + integer));
        dataMatrix.createIntAdapter(SettingKeys.Hardware.DATA_HEADPHONE_A_COUNT_INT, integer -> ((TextView) view.findViewById(R.id.eng_hardware_headphone_insert_b)).setText("" + integer));

        findViewById(R.id.eng_hardware_insert_a_reset).setOnClickListener(view -> DataMatrix.putInt(SettingKeys.Hardware.DATA_HEADPHONE_B_COUNT_INT, 0));
        findViewById(R.id.eng_hardware_insert_b_reset).setOnClickListener(view -> DataMatrix.putInt(SettingKeys.Hardware.DATA_HEADPHONE_A_COUNT_INT, 0));

        if (config.machineClassType == JHTCommonTypes.MachineClassificationType.SpinBike) {
            setVisible(findViewById(R.id.lcb_data_group), View.VISIBLE);
            TextView dataView = findViewById(R.id.lcb_data_values);
            if (dataView != null) {
                dataMatrix.createStringAdapter(HALKeys.DATA_ATHENA_SENSOR_DATA_STRING, (data) -> mHandler.post(() ->
                        dataView.setText(data)));
            }
        }
        else {
            setVisible(findViewById(R.id.lcb_data_group), View.GONE);
        }

        // USB attach count
        if (findViewById(R.id.eng_hardware_usb_attach_count) != null) {
            dataMatrix.createIntAdapter(SettingKeys.Hardware.DATA_USB_ATTACH_COUNT_INT, integer -> ((TextView) view.findViewById(R.id.eng_hardware_usb_attach_count)).setText("" + integer));
            findViewById(R.id.eng_hardware_usb_attach_reset).setOnClickListener(view -> DataMatrix.putInt(SettingKeys.Hardware.DATA_USB_ATTACH_COUNT_INT, 0));
        }

        // Teamviewer button
        /* CGC-20971
        if (showSimple) {
            if (view.findViewById(R.id.settings_panel_teamviewer_button) != null) {
                if (showSimple) {
                    findViewById(R.id.settings_panel_teamviewer_button).setVisibility(View.GONE);
                } else {
                    findViewById(R.id.settings_panel_teamviewer_button).setOnClickListener(view -> {
                        LaunchAppUtil.launchTeamviewer();
                    });
                }
            }
        }*/


        updateERPSettingsState();
        {
            OptionSelectorItem erpMode = view.findViewById(R.id.eng_hardware_erp_mode);
            erpMode.setOptions(R.array.erp_mode, DataMatrix.getInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_MODE_ENUM_INT));
            erpMode.setOnOptionSelected((pos, option) -> {
                DataMatrix.putInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_MODE_ENUM_INT, pos);
                updateERPSettingsState();
            });

            // show the ERP type option if console supports it
            if (ProductConfig.hasSleepMode()) {
                OptionSelectorItem erpTrigger = view.findViewById(R.id.eng_hardware_erp_type);
                erpTrigger.setLabel(StringUtil.toTitleCase(getContext().getString(R.string.options_uc)));
                erpTrigger.setOptions(R.array.erp_type, DataMatrix.getInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_TYPE_ENUM_INT));
                erpTrigger.setOnOptionSelected((pos, option) -> {
                    DataMatrix.putInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_TYPE_ENUM_INT, pos);
                });
            } else {
                setVisible(view.findViewById(R.id.eng_hardware_erp_type), View.GONE);
                setVisible(view.findViewById(R.id.eng_hardware_erp_type_space1), View.GONE);
                setVisible(view.findViewById(R.id.eng_hardware_erp_type_space2), View.GONE);
            }
        }

        if (ProductConfig.hasFan()) {
            OptionSelectorItem fanOptions = view.findViewById(R.id.eng_hardware_fan_mode);
            fanOptions.setOptions(R.array.fan_mode, DataMatrix.getInt(SettingKeys.Hardware.DATA_FAN_MODE_INT));
            fanOptions.setOnOptionSelected((pos, option) -> DataMatrix.putInt(SettingKeys.Hardware.DATA_FAN_MODE_INT, pos));
        } else {
            setVisible(view.findViewById(R.id.eng_hardware_fan_line), View.GONE);
            setVisible(view.findViewById(R.id.eng_hardware_fan_mode), View.GONE);
        }

        linkCheckBox(view.findViewById(R.id.eng_hardware_key_beeps), SettingKeys.Hardware.DATA_KEY_BEEP_BOOL);
        linkCheckBox(view.findViewById(R.id.eng_hardware_countdown_beeps), SettingKeys.Hardware.DATA_COUNTDOWN_BEEP_BOOL);
        linkCheckBox(view.findViewById(R.id.eng_hardware_ignore_incline_errors), SettingKeys.Hardware.DATA_IGNORE_INCLINE_ERRORS_BOOL);
        linkCheckBox(view.findViewById(R.id.eng_hardware_ignore_b_errors), SettingKeys.Hardware.DATA_IGNORE_B_LEVEL_ERRORS_BOOL);
        linkCheckBox(view.findViewById(R.id.eng_hardware_pfc_mode), SettingKeys.Hardware.DATA_PERFORMANCE_PLUS_TM_PFC_MODE_BOOL);
        linkUserIntInput(view.findViewById(R.id.eng_erp_timeout), SettingKeys.Hardware.DATA_ENERGY_SAVER_TIMEOUT_MIN_INT);
        linkCheckBox(view.findViewById(R.id.eng_hardware_gymkit), SettingKeys.Hardware.DATA_GYMKIT_ENABLE_BOOL);
        linkCheckBox(view.findViewById(R.id.eng_hardware_samsung_watch), SettingKeys.Hardware.DATA_SAMSUNG_WATCH_ENABLE_BOOL);
        linkCheckBox(view.findViewById(R.id.eng_hardware_mfi), SettingKeys.Hardware.DATA_MFI_ENABLE_BOOL);
        linkCheckBox(view.findViewById(R.id.eng_hardware_disable_audio), SettingKeys.Hardware.DATA_AUDIO_DISABLED_BOOL);

        setVisible(view.findViewById(R.id.eng_hardware_gymkit), (BuildConfig.HAS_BT_SINK && BuildConfig.HAS_GYMKIT ? View.VISIBLE : View.GONE));
        setVisible(view.findViewById(R.id.eng_hardware_samsung_watch), (BuildConfig.SAMSUNG_WATCH_COMPATIBLE ? View.VISIBLE : View.GONE));

        if (ProductConfig.isRetail()) {
            RadioGroup timeoutSelection = findViewById(R.id.timeout_selection);
            RadioButton hardwareEnergySaverButton = findViewById(R.id.hardware_energy_saver_button);
            RadioButton hardwareScreenSaverButton = findViewById(R.id.hardware_screen_saver_button);
            RadioButton hardwareDemoModeButton = findViewById(R.id.hardware_demo_mode_button);
            RadioButton hardwareNoTimeoutButton = findViewById(R.id.hardware_none_button);

            if (!showSimple) {
                setVisible(hardwareNoTimeoutButton, View.VISIBLE);
            }

            UserTextInput demoModeInput = findViewById(R.id.demo_mode_input);
            linkUserIntInput(demoModeInput, SettingKeys.Hardware.DATA_PROMOTIONAL_VIDEO_TIMEOUT_MIN_INT);
            UserTextInput energySaverInput = findViewById(R.id.energy_saver_input);
            linkUserIntInput(energySaverInput, SettingKeys.Hardware.DATA_ENERGY_SAVER_TIMEOUT_MIN_INT);

            hardwareEnergySaverButton.setOnClickListener((v) -> {
                DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.ERP_BTN_PRESS.name);
            });
            hardwareScreenSaverButton.setOnClickListener((v) -> {
                DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.SLEEP_MODE_BTN_PRESS.name);
            });
            hardwareDemoModeButton.setOnClickListener((v) -> {
                DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.DEMO_VIDEO_BTN_PRESS.name);
            });
            hardwareNoTimeoutButton.setOnClickListener((v) -> {
                DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.NO_POWER_SAVING_MODE_BTN_PRESS.name);
            });

            if (timeoutSelection != null){
                timeoutSelection.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                        if (checkedId == R.id.hardware_energy_saver_button || checkedId == R.id.hardware_screen_saver_button){
                            //Show energy saver input
                            setVisible(energySaverInput, View.VISIBLE);
                            setVisible(demoModeInput, View.INVISIBLE);
                            //Turn off Demo Mode
                            DataMatrix.putBoolean(SettingKeys.Hardware.DATA_ENABLE_PROMOTIONAL_VIDEO_BOOL, false);
                            //Set DATA_ENERGY_SAVER_MODE_ENUM_INT equal to Inactivity
                            DataMatrix.putInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_MODE_ENUM_INT, SettingKeys.ERPMode.Inactivity.ordinal());
                            if (checkedId == R.id.hardware_screen_saver_button) {
                                DataMatrix.putInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_TYPE_ENUM_INT, SettingKeys.ERPType.Screen.ordinal());
                            } else {
                                DataMatrix.putInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_TYPE_ENUM_INT, SettingKeys.ERPType.FullErp.ordinal());
                            }
                            //Update ERP Settings State
                            updateERPSettingsState();
                        }
                        else if (checkedId == R.id.hardware_demo_mode_button){
                            //Show energy saver input
                            setVisible(energySaverInput, View.INVISIBLE);
                            setVisible(demoModeInput, View.VISIBLE);
                            //Turn on Demo Mode
                            DataMatrix.putBoolean(SettingKeys.Hardware.DATA_ENABLE_PROMOTIONAL_VIDEO_BOOL, true);
                            //Turn off Energy Saver Mode
                            DataMatrix.putInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_MODE_ENUM_INT, SettingKeys.ERPMode.Disabled.ordinal());
                        }
                        else if  (checkedId == R.id.hardware_none_button){
                            setVisible(energySaverInput, View.INVISIBLE);
                            setVisible(demoModeInput, View.INVISIBLE);
                            //Turn off Demo Mode
                            DataMatrix.putBoolean(SettingKeys.Hardware.DATA_ENABLE_PROMOTIONAL_VIDEO_BOOL, false);
                            //Turn off Energy Saver Mode
                            DataMatrix.putInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_MODE_ENUM_INT, SettingKeys.ERPMode.Disabled.ordinal());
                            updateERPSettingsState();
                        }
                    }
                });
            }

            //Set the default clicked state
            if (hardwareNoTimeoutButton != null){
                try {
                    if (!DataMatrix.getBoolean(SettingKeys.Hardware.DATA_ENABLE_PROMOTIONAL_VIDEO_BOOL) &&
                            DataMatrix.getInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_MODE_ENUM_INT)
                                    == SettingKeys.ERPMode.Disabled.ordinal()){
                        hardwareNoTimeoutButton.setChecked(true);
                    }
                } catch (Exception e){ Log.e(TAG, "hardwareNoTimeoutButton set exception", e); }
            }

            if (hardwareDemoModeButton != null){
                if (DataMatrix.getBoolean(SettingKeys.Hardware.DATA_ENABLE_PROMOTIONAL_VIDEO_BOOL)){
                    hardwareDemoModeButton.setChecked(true);
                }
            }

            if (DataMatrix.getInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_MODE_ENUM_INT) == SettingKeys.ERPMode.Inactivity.ordinal()) {
                if (DataMatrix.getInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_TYPE_ENUM_INT) == SettingKeys.ERPType.Screen.ordinal()) {
                    if (hardwareScreenSaverButton != null) hardwareScreenSaverButton.setChecked(true);
                } else {
                    if (hardwareEnergySaverButton != null) hardwareEnergySaverButton.setChecked(true);
                }
            }

            //Show Parker Timeout Settings Area
            setVisible(findViewById(R.id.timeout_break), View.VISIBLE);
            setVisible(findViewById(R.id.timeout_group), View.VISIBLE);
            //Hide Views for Non Parker Timeout Settings Area
            setVisible(findViewById(R.id.energy_erp_demo_break), View.GONE);
            setVisible(findViewById(R.id.eng_hardware_erp_mode), View.GONE);
            setVisible(findViewById(R.id.eng_erp_time), View.GONE);
            setVisible(findViewById(R.id.eng_erp_timeout), View.GONE);
        }

        {
            Calendar cal = Calendar.getInstance();
            int timeMin = DataMatrix.getInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_TIME_MIN_INT);
            cal.set(0, 0, 0, timeMin / 60, timeMin % 60);
            UserTextInput userTextInput = view.findViewById(R.id.eng_erp_time);
            userTextInput.setText(cal);
            userTextInput.setOnTimePicked((view, hourOfDay, minute) -> DataMatrix.putInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_TIME_MIN_INT, hourOfDay * 60 + minute));
        }

        view.findViewById(R.id.eng_hardware_rfid_disassociate).setOnClickListener(v -> {
            if (chip != null) {
                DialogFactory.confirm(findViewById(R.id.hardware_main_container), getContext().getString(R.string.disassociate_rfid_confirmation_pg), () -> {
                    DataMatrix.putParcelable(DapiLogin.Keys.ACTION_DISASSOCIATE_RFID_PARCELABLE, chip);
                    tagType.setText("");
                    uid.setText("");
                    dec.setText("");
                    chip = null;
                }, null);
            }
        });

        if (ProductConfig.hasRFID()) {
            tagType = findViewById(R.id.eng_hardware_rfid_tag);
            uid = findViewById(R.id.eng_hardware_rfid_tag_value);
            dec = findViewById(R.id.eng_hardware_rfid_tag_value_dec);
            tagType.setText("");
            uid.setText("");
            dec.setText("");
        }

        dataMatrix.createParcelableAdapter(HALKeys.EVENT_RFID_TAG_DETECTED_PARCELABLE, RFIDChip.class, chip -> {
            this.chip = chip;
            String hex_string = StringUtil.byteArray2Hex(chip.uid());
            StringBuilder dec_string = new StringBuilder();
            uid.setText(hex_string);
            for (int i = 0; i < hex_string.length(); i++) { // sets the numbers two spaces so 1 shows up as 01
                dec_string.append(String.format(Locale.ENGLISH, "%02d",
                        Integer.parseInt(String.valueOf(hex_string.charAt(i)), 16)));
            }
            dec.setText(String.valueOf(dec_string));
            tagType.setText(chip.rfStandard().toString());

            handler.postDelayed(() -> DataMatrix.putBoolean(HALKeys.DATA_SCAN_RFID_TAGS_BOOL, true), 1000);

        }, false);

        dataMatrix.createParcelableAdapter(HALKeys.EVENT_PF_BARCODE_DETECTED_PARCELABLE, PFBarcodeData.class, barcodeData -> {
            this.chip = barcodeData.chip();
            String hex_string = StringUtil.byteArray2Hex(chip.uid());
            StringBuilder dec_string = new StringBuilder();
            uid.setText(hex_string);
            tagType.setText(chip.rfStandard().toString());
            for (int i = 0; i < hex_string.length(); i++) { // sets the numbers two spaces so 1 shows up as 01
                dec_string.append(String.format(Locale.ENGLISH, "%02d",
                        Integer.parseInt(String.valueOf(hex_string.charAt(i)), 16)));
            }
            dec.setText(String.valueOf(dec_string));
            handler.postDelayed(() -> DataMatrix.putBoolean(HALKeys.DATA_SCAN_RFID_TAGS_BOOL, true), 1000);
        }, false);

        motorType = findViewById(R.id.eng_hardware_motor_type_value);
        motorType.setText("");
        dataMatrix.createIntAdapter(HALKeys.DATA_GET_MOTOR_TYPE_INT, integer -> {
            motorType.setText(Integer.toString(integer));
        });
        DataMatrix.putInt(SettingKeys.Hardware.DATA_TREADMILL_MOTOR_TYPE_INT, 0);
        linkOption(view.findViewById(R.id.eng_hardware_motor_type), R.array.treadmill_motor_type, SettingKeys.Hardware.DATA_TREADMILL_MOTOR_TYPE_INT, (pos, option) -> getMotorType());

        linkCheckBox(view.findViewById(R.id.eng_hardware_enable_oc), SettingKeys.Hardware.DATA_ENABLE_OVER_CURRENT_PROTECTION_BOOL);
        linkCheckBox(view.findViewById(R.id.eng_ant_plus_enabled), SettingKeys.Hardware.DATA_ANT_PLUS_DEVICE_SEND_DATA_BOOL);
        linkUserIntInput(view.findViewById(R.id.eng_ant_plus_id), SettingKeys.Hardware.DATA_ANT_PLUS_DEVICE_ID_INT);
        linkCheckBox(view.findViewById(R.id.eng_ftms_enabled), SettingKeys.Hardware.DATA_FTMS_ADVERTISING_ENABLED_BOOL);
        linkUserTextInput(view.findViewById(R.id.eng_ftms_name), SettingKeys.Hardware.DATA_SALUTRON_BLE_NAME_STRING);
        UserTextInput ftmsName = view.findViewById(R.id.eng_ftms_name);
        ftmsName.setInputProperties(ftmsName.getInputProperties().hint(DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING)));

        OptionSelectorItem ant_plus_protocol = view.findViewById(R.id.eng_ant_plus_page);
        ant_plus_protocol.setOptions(R.array.ant_plus_protocol, DataMatrix.getInt(SettingKeys.Hardware.DATA_ANT_PLUS_PAGE_INT));
        ant_plus_protocol.setOnOptionSelected((pos, option) -> DataMatrix.putInt(SettingKeys.Hardware.DATA_ANT_PLUS_PAGE_INT, pos));

        // Belt lubrication stuff

        findViewById(R.id.belt_lube_layout).setVisibility(config.requiresBeltLubrication() ? View.VISIBLE : View.GONE);
        findViewById(R.id.chain_lube_layout).setVisibility(config.requiresChainLubrication() ? View.VISIBLE : View.GONE);
        if (config.requiresLubricationPerTime()) {
            dataMatrix.createIntAdapter(SettingKeys.Hardware.DATA_ACCUMULATED_TIME_AT_LAST_LUBE_SEC_INT, time -> updateLubeTime());
            dataMatrix.createIntAdapter(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT, time -> updateLubeTime());
        }
        if (config.requiresLubricationPerDistance()) {
            dataMatrix.createFloatAdapter(SettingKeys.Hardware.DATA_ACCUMULATED_DISTANCE_KM_AT_LAST_LUBE_FLOAT, dist -> updateLubeDistance());
            dataMatrix.createFloatAdapter(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT, dist -> updateLubeDistance());
        }

        if (showSimple && !BeltLubeManager.doesBeltNeedLube()) {
            findViewById(R.id.reset_lube_time_button).setVisibility(View.INVISIBLE);
            findViewById(R.id.reset_lube_distance_button).setVisibility(View.INVISIBLE);
        }
        findViewById(R.id.reset_lube_time_button).setOnClickListener(view -> resetLubeTime());
        findViewById(R.id.reset_lube_distance_button).setOnClickListener(view -> resetLubeDistance());

        // Motor stuff

        if (config.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
            if (!ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker)
                    && !ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)
            ) {
                //TODO CGC-20594
                // This needs some cleanup. - Samantha, 7/17
                // I would like this if logic cleaned up but unsure if isCommerical makes the most
                // sense. - James, 7/17
                findViewById(R.id.eng_hardware_enable_oc).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_hardware_pfc_mode).setVisibility(config.MODEL
                        == FrameConfig.MODEL_PERFORMANCE_PLUS ? View.VISIBLE : View.GONE);
            }
        } else {
            setMotorTypeVisible(View.GONE);
            findViewById(R.id.eng_hardware_enable_oc).setVisibility(View.GONE);
            findViewById(R.id.eng_hardware_pfc_mode).setVisibility(View.GONE);
        }

        if (config.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill || config.machineClassType == JHTCommonTypes.MachineClassificationType.Ascent) {
            findViewById(R.id.eng_hardware_ignore_incline_errors).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.eng_hardware_ignore_incline_errors).setVisibility(View.GONE);
        }

        // hide some settings for simple view
        if (showSimple) {
            findViewById(R.id.headphone_count_group).setVisibility(View.GONE);
            findViewById(R.id.headphone_count_break).setVisibility(View.GONE);
            setMotorTypeVisible(View.GONE);
        }

        // parker doesn't show error code except for climbmill which will only show safety ones
        if (ProductConfig.isRetail()) {
            view.findViewById(R.id.eng_hardware_ignore_incline_errors).setVisibility(View.GONE);
            view.findViewById(R.id.eng_hardware_ignore_b_errors).setVisibility(View.GONE);
        }

        dataMatrix.createBoolAdapter(SettingKeys.Hardware.DATA_AUDIO_DISABLED_BOOL, disableAudio -> {
            SeekBar volumeBar = findViewById(R.id.eng_hardware_volume);
            if (disableAudio) {
                volumeBar.setEnabled(false);
            } else {
                volumeBar.setEnabled(true);
            }
        }, true);

        if (ProductConfig.isCommercial()) // fix the crash on Parker where friction_test_layout is missing on Parker hardware.xml
        {
            // Handles launching friction test. Friction test is only available on treadmills.
            LinearLayout frictionTestLayout = view.findViewById(R.id.friction_test_layout);
            Button frictionTestButton = view.findViewById(R.id.friction_test_button);
            FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
            if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                frictionTestLayout.setVisibility(View.VISIBLE);
                // Launch directly since we don't need this to be available anywhere else in the system.
                frictionTestButton.setOnClickListener(view -> {
                    Intent frictionTestIntent = new Intent(getContext(), FrictionTestActivity.class);
                    getContext().startActivity(frictionTestIntent);
                });
            } else {
                frictionTestLayout.setVisibility(View.GONE);
            }
        }

        // ProjectOne (Onyx) only extras. The views these touch are declared gone in
        // main/res/layout/hardware.xml, so every other flavor pays nothing for them.
        if (ProductConfig.isProjectOne()) {
            ProjectOneHardwareExtras.apply(getContext(), view);
        }
    }

    private void setMotorTypeVisible(int visible) {
        setVisible(findViewById(R.id.eng_hardware_motor_type), visible);
        setVisible(findViewById(R.id.eng_hardware_motor_type_value), visible);
        setVisible(findViewById(R.id.eng_hardware_motor_type_line), visible);
    }

    private void setThumbPosition(SeekBar seekBar, int progress, TextView label){
        label.setText(progress+"");
    }

    private void getMotorType() {
        new Handler().postDelayed(() -> {
            DataMatrix.sendEvent(HALKeys.EVENT_GET_MOTOR_TYPE);
        }, 500);
    }

    @Override
    public void hide(Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        DataMatrix.putBoolean(HALKeys.DATA_SCAN_RFID_TAGS_BOOL, false);
        DataMatrix.putBoolean(HALKeys.DATA_ATHENA_MONITOR_SENSOR_DATA_BOOL, false);
        dataMatrix.finish();
    }

}
