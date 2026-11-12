package com.jht.chimera.ui.machinesettings.pages;

import static com.jht.chimera.R.id.eng_software_package_version;
import static com.jht.chimera.lib.common.context.ContextUtil.getString;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.TextView;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androidcommonalgorithms.util.SerialNumberUtil;
import com.jht.androidcommonalgorithms.util.ValueSerializer;
import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.HardwareIdUtil;
import com.jht.chimera.lib.common.ObjectUtil;
import com.jht.chimera.lib.common.VersionHelper;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.SerialNumberHelper;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.dapi.DapiUtil;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.ImportExportKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.tv.TVControllerBase;
import com.jht.chimera.lib.tv.TVControllerFactory;
import com.jht.chimera.lib.tv.switft.SwiftManager;
import com.jht.chimera.lib.tv.tuner.IChimeraTuner;
import com.jht.chimera.lib.tv.tuner.TunerFactory;
import com.jht.chimera.lib.widget.CommonRoutines;
import com.jht.chimera.lib.workoutapps.AppManager;
import com.jht.chimera.service.ConsoleServicesHelper;
import com.jht.chimera.ui.bluetooth.HeadphoneScanner;
import com.jht.chimera.ui.bluetooth.MediaDeviceScanner;
import com.jht.chimera.ui.machinesettings.EngineeringIdleTimerManager;
import com.jht.chimera.ui.machinesettings.pages.tv.EngineeringPageBase;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.peripheral.tuner.channel.IChannel;
import com.jht.translations.LanguageHelper;

import java.io.File;
import java.util.List;
import java.util.Locale;

import updatepackages.UpdatePackage;
import updatepackages.UpdatePackageList;
import updatepackages.UpdateXmlGenerator;


/**
 * Implements the general page gluing the page to the model layer.
 */
public class General extends EngineeringPageBase {

    private static final String TAG = "General";
    private final DataMatrix dataMatrix = new DataMatrix();
    private final FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
    private View view;
    private boolean showSimple = false;
    private Handler handler;

    public General() {
    }

    public General(boolean showSimple) {
        this.showSimple = showSimple;
    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        handler = new Handler();
        view = LayoutInflater.from(context).inflate(R.layout.general, null);
        container.addView(view);
        return view;
    }


    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_gen_console_serial_number);

            if (ProductConfig.isRetail()) {
                // parker has a different serial number logic when the frame prefix determines frame type
                String consoleSerial = DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING);
                if (ObjectUtil.isNullOrEmpty(consoleSerial)) consoleSerial = ProductConfig.getConsoleType().getConsolePrefix();
                userTextInput.setInputProperties(userTextInput.getInputProperties().
                        inputValue(consoleSerial) // Now taking the unedited string
                        .minChars(0)
                        .maxChars(32));
                userTextInput.setOnTextChanged(text -> {
                    String newConsoleSerial = text;
                    SerialNumberHelper.Error consoleStatus = SerialNumberHelper.isConsoleSnValid(newConsoleSerial);
                    // if the serial number doesn't match or serial numbers are too short, show an error
                    if (consoleStatus.isError()) {
                        DialogFactory.message(userTextInput, getString(R.string.invalid_serial_number_pg), () -> {
                            // do nothing
                        }, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                        return;
                    }
                    DataMatrix.putString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, newConsoleSerial);
                });
            } else {
                userTextInput.setInputProperties(userTextInput.getInputProperties()
                        .inputValue(DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING))
                        .minChars(ProductConfig.getConsoleType().getConsolePrefix().length()));
                userTextInput.setOnTextChanged(s -> DataMatrix.putString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, SerialNumberUtil.fixSNToBeAlphanumeric(s)));
            }

            if (showSimple) userTextInput.setReadOnly(true);
            if (showSimple) userTextInput.setVisibility(View.GONE);
        }

        {

            UserTextInput userTextInput = view.findViewById(R.id.eng_gen_frame_serial_number);
            if (ProductConfig.isRetail()) {
                // parker has a different serial number logic when the frame prefix determines frame type
                String frameSerial = DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING);
                userTextInput.setInputProperties(userTextInput.getInputProperties().
                        inputValue(frameSerial) // Now taking the unedited string
                        .minChars(0)
                        .maxChars(32));
                userTextInput.setOnTextChanged(text -> {
                    FrameConfigEnum frameConfigEnum = SerialNumberHelper.getFrameFromSn(text);
                    Log.d(TAG, "validateSerialNumbers: " + frameConfigEnum);
                    SerialNumberHelper.Error frameStatus = SerialNumberHelper.isFrameSnValid(frameConfigEnum, text);
                    // if the serial number doesn't match or serial numbers are too short, show an error
                    if (frameStatus.isError()) {
                        DialogFactory.message(userTextInput, getString(R.string.invalid_serial_number_pg), () -> {
                            // do nothing
                        }, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                        return;
                    }
                    // update the frame
                    FrameConfigManager.getInstance().setFrame(frameConfigEnum);
                    DataMatrix.putString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, text);
                });
            } else {
                userTextInput.setInputProperties(userTextInput.getInputProperties()
                        .inputValue(DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING))
                        .minChars(FrameConfigManager.getInstance().getFramePrefix().length()));

                userTextInput.setOnTextChanged(s -> DataMatrix.putString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, SerialNumberUtil.fixSNToBeAlphanumeric(s)));
            }

            if (showSimple) userTextInput.setReadOnly(true);
            if (showSimple) userTextInput.setVisibility(View.GONE);
        }

        {

            UserTextInput bridgeSNField = view.findViewById(R.id.eng_gen_bridge_serial_number);
            if (BuildConfig.HAS_BRIDGE_SN) {
                if (bridgeSNField != null) {
                    bridgeSNField.setInputProperties(bridgeSNField.getInputProperties()
                            .inputValue(DataMatrix.getString(SettingKeys.AssetManagement.DATA_BRIDGE_SERIAL_STRING))
                            .minChars(1)
                            .maxChars(40)
                    );
                    bridgeSNField.setOnTextChanged(s -> DataMatrix.putString(SettingKeys.AssetManagement.DATA_BRIDGE_SERIAL_STRING, SerialNumberUtil.fixSNToBeAlphanumeric(s)));
                }
            } else {
                setVisible(bridgeSNField, View.GONE);
            }
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_gen_club_id);
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT));
            userTextInput.setOnTextChanged(s -> {
                if (ProductConfig.isLightCommercial()) {
                    DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL, CommonRoutines.parseInt(s) > 0);
                }
                DataMatrix.putInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT, CommonRoutines.parseInt(s));
            });
        }

        if (showSimple) view.findViewById(R.id.eng_reset_accumulators).setVisibility(View.GONE);

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_accumulated_distance);
            userTextInput.setInputProperties(userTextInput.getInputProperties()
                    .units(CommonRoutines.getUnit())
                    .max(getContext().getResources().getInteger(DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL) ? R.integer.max_accumulated_distance_km : R.integer.max_accumulated_distance_miles)));
            String text = "" + CommonRoutines.round(DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT) * CommonRoutines.getConvertFactorDistance(), 1);
            userTextInput.setText(text);
            userTextInput.setOnTextChanged(s -> {
                float value = CommonRoutines.parseFloat(s) / CommonRoutines.getConvertFactorDistance();
                DataMatrix.putFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT, value);
            });
            if (showSimple) userTextInput.setReadOnly(true);
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_accumulated_floors);
            userTextInput.setText(LanguageHelper.getDecimalFormat(userTextInput.getInputProperties().resolution()).format(DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT)));
            userTextInput.setOnTextChanged(s -> {
                float value = CommonRoutines.parseFloat(s);
                DataMatrix.putFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT, value);
            });
            if (showSimple) userTextInput.setReadOnly(true);
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_accumulated_time);
            int time = DataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT);
            userTextInput.setText(String.format(Locale.ENGLISH, "%d:%02d", time / 3600, time / 60 % 60));
            userTextInput.setOnTextChanged(s -> {
                String[] parts = s.split(":");
                if (parts.length == 2) {
                    DataMatrix.putInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT, CommonRoutines.parseInt(parts[0]) * 3600 + CommonRoutines.parseInt(parts[1]) * 60);
                }
            });
            if (showSimple) userTextInput.setReadOnly(true);
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_demo_mode);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, isChecked));
            if (showSimple) checkBox.setVisibility(View.GONE);
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_out_of_order);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.General.DATA_SHOW_OUT_OF_ORDER_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.General.DATA_SHOW_OUT_OF_ORDER_BOOL, isChecked));
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_show_setup_wizard);
            if (showSimple) checkBox.setVisibility(View.GONE);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.Hardware.DATA_SHOW_MACHINE_SETUP_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.Hardware.DATA_SHOW_MACHINE_SETUP_BOOL, isChecked));
        }


        view.findViewById(R.id.eng_reset_accumulators).setOnClickListener(v -> DialogFactory.confirm(v, getContext().getString(R.string.reset_accumulators_confirmation_pg), () -> {
            DataMatrix.putInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT, 0);
            DataMatrix.putInt(SettingKeys.Hardware.DATA_ACCUMULATED_TIME_AT_LAST_LUBE_SEC_INT, 0);
            DataMatrix.putFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT, 0);
            DataMatrix.putFloat(SettingKeys.Hardware.DATA_ACCUMULATED_DISTANCE_KM_AT_LAST_LUBE_FLOAT, 0);
            DataMatrix.putFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT, 0);

            // Note: we should probably create a function to update the text
            {
                UserTextInput userTextInput = view.findViewById(R.id.eng_accumulated_distance);
                userTextInput.setInputProperties(userTextInput.getInputProperties()
                        .units(CommonRoutines.getUnit()));
                String text = "" + CommonRoutines.round(DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT) * CommonRoutines.getConvertFactorDistance(), 1);
                userTextInput.setText(text);
            }

            {
                UserTextInput userTextInput = view.findViewById(R.id.eng_accumulated_floors);
                userTextInput.setText(LanguageHelper.getDecimalFormat(userTextInput.getInputProperties().resolution()).format(DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT)));
                String text = "" + CommonRoutines.round(DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT), 0);
                userTextInput.setText(text);
            }

            {
                UserTextInput userTextInput = view.findViewById(R.id.eng_accumulated_time);
                int time = DataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT);
                userTextInput.setText(String.format(Locale.ENGLISH, "%d:%02d", time / 3600, time / 60 % 60));
            }
        }, null));

        if(ProductConfig.isRetail()) {
            {
                CheckBox pinCheckBox = view.findViewById(R.id.eng_require_pin_checkbox);
                if (pinCheckBox != null) {
                    boolean pinEnabled = DataMatrix.getBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL);
                    pinCheckBox.setChecked(pinEnabled);
                    pinCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL, isChecked));
                }
            }

            {
                UserTextInput pinTextInput = view.findViewById(R.id.eng_gen_pin_number_input);
                if (pinTextInput != null) {
                    pinTextInput.setText("" + DataMatrix.getString(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_STRING, ""));
                    linkUserTextInput(pinTextInput, SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_STRING);
                }
            }
        }
        else {
            view.findViewById(R.id.eng_require_pin_checkbox).setVisibility(View.GONE);
            view.findViewById(R.id.eng_gen_pin_number_input).setVisibility(View.GONE);
        }

        {
            OptionSelectorItem screenTimeout = view.findViewById(R.id.eng_gen_screen_timeout);
            int timeout = DataMatrix.getInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT);

            String[] timeoutValues = getContext().getResources().getStringArray(R.array.screen_timeout);
            int timeoutIndex = timeoutValues.length - 1;
            if (timeout > 0) {
                for (int i = 0; i < timeoutValues.length - 1; i++) {
                    if (timeout <= Integer.parseInt(timeoutValues[i])) {
                        timeoutIndex = i;
                        break;
                    }
                }
            }
            screenTimeout.setOptions(R.array.screen_timeout, timeoutIndex);
            screenTimeout.setOnOptionSelected((pos, option) -> DataMatrix.putInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT, Integer.parseInt(timeoutValues[pos])));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_gen_reboot_inactive_time);
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.General.DATA_REBOOT_INACTIVE_TIME_HOUR_INT));
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.General.DATA_REBOOT_INACTIVE_TIME_HOUR_INT, CommonRoutines.parseInt(s)));
        }

        {
            // The serializer is needed here because when we change environment we call up to DAPI to get the
            // accumulated data.  If we don't wait for that operation to complete there is a change for the
            // data to get wiped if the user changes environments quickly.
            OptionSelectorItem dapiEnv = view.findViewById(R.id.eng_dapi_env);
            JHTNotification environmentChangeNotification = new JHTNotification();
            ValueSerializer<Integer> changeDapi = new ValueSerializer<>(DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT), 0, value -> {
                DataMatrix.putInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, value);
                environmentChangeNotification.waitForNotification(10000);
            });
            if (showSimple) dapiEnv.setVisibility(View.GONE);
            dapiEnv.setOptions(R.array.dapi_environments, DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT));
            dapiEnv.setOnOptionSelected((pos, option) -> changeDapi.nextValue(pos));
            dataMatrix.createBoolAdapter(DAPIKeys.DATA_LOCK_CARDIO_INFO_BOOL, lock -> {
                if(!lock) {
                    environmentChangeNotification.setNotification();
                }
            }, false);
        }


        // parker needs both restore defaults and restore factory defaults
        if (ProductConfig.isRetail()) {
            if (showSimple) {
                view.findViewById(R.id.restore_line_break).setVisibility(View.GONE);
                view.findViewById(R.id.eng_restore_defaults).setVisibility(View.GONE);
                view.findViewById(R.id.eng_restore_factory_defaults).setVisibility(View.GONE);
            }
        }

        view.findViewById(R.id.eng_restore_defaults).setOnClickListener(v -> DialogFactory.confirm(v, getContext().getResources().getString(R.string.restore_defaults_confirmation_pg), () -> {
            EngineeringIdleTimerManager.idleTimerManagement.stopMonitoring();
            DataMatrix.putInt(UiServiceKeys.ACTION_SHOW_OVERLAY_PANEL_MS_INT, 10000);
            performFactoryReset(getContext(), false);
        }, null));

        view.findViewById(R.id.eng_restore_factory_defaults).setOnClickListener(v -> DialogFactory.confirm(v, getContext().getResources().getString(R.string.restore_factory_defaults_confirmation_pg), () -> {
            EngineeringIdleTimerManager.idleTimerManagement.stopMonitoring();
            DataMatrix.putInt(UiServiceKeys.ACTION_SHOW_OVERLAY_PANEL_MS_INT, 10000);
            performFactoryReset(getContext(), true);
        }, null));

        getTunerVersion();
        refreshFirmwareVersions();

        StringBuilder softwareVersion = new StringBuilder();
        softwareVersion.append(String.format("Software %s", BuildConfig.SoftwareVersion));
        if (ProductConfig.isIfitBlocked()) { // this function includes flavor checks
            softwareVersion.append(" COCO");
        }
        softwareVersion.append(String.format("\nSystem Bundle %s", VersionHelper.getCurrentBundleVersion())); // top level bundle version
        if (!showSimple) {
            PackageInfo webViewInfo = WebView.getCurrentWebViewPackage();
            String webversion = "Unknown";
            if (webViewInfo != null) webversion = webViewInfo.versionName;
            softwareVersion.append(String.format("\nWebview version:  %s", webversion));
            softwareVersion.append(String.format("\nConsole UUID:  %s", DapiUtil.getMachineId()));
            String cpuId = HardwareIdUtil.getCpuSerialNumber();
            softwareVersion.append("\nCPU ID: ").append(cpuId == null ? "--" : cpuId);
            Log.d(TAG, "Webview Versions: " + webversion + " " + DapiUtil.getMachineId());
        }

        ((TextView) view.findViewById(eng_software_package_version)).setText(softwareVersion);

        if (showSimple){
            StringBuilder thirdPartyAppsVersion = new StringBuilder();
            try {
                @SuppressLint("SdCardPath") UpdatePackageList packageList = UpdateXmlGenerator.readUpdateFile(new File("/data/data/com.jht.updateservice/files/current_update.xml"));
                if (packageList != null){
                    for (UpdatePackage updatePackage : packageList.getUpdatePackages()){
                        if (updatePackage.getUpdateId().equals("fd9193ca-3ab3-43e9-829f-b41e83fc915f")) {
                            String version = updatePackage.getVersionString();
                            if (version.isEmpty()){
                                thirdPartyAppsVersion.append("0.0.0.0");
                            } else {
                                thirdPartyAppsVersion.append(version);
                            }
                            break;
                        }
                    }
                } else {
                    thirdPartyAppsVersion.append("0.0.0.0");
                }
            } catch (Exception e){
                thirdPartyAppsVersion.append("0.0.0.0");
            }
            ((TextView) view.findViewById(R.id.eng_software_app_versions)).setText(String.format("3rd Party Apps Bundle"+" %s", thirdPartyAppsVersion.toString()));
        } else {

            StringBuilder apkVersions = new StringBuilder();
            PackageManager pm = getContext().getPackageManager();
            List<String> packages = AppManager.getInstance().getApkPackages();
            for (String p : packages) {
                try {
                    PackageInfo info = pm.getPackageInfo(p, 0);
                    if (apkVersions.length() > 0) {
                        apkVersions.append("\n");
                    }
                    apkVersions.append(pm.getApplicationLabel(info.applicationInfo)).append(" - ").append(info.versionName).append(" [").append(info.versionCode).append("]");
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w(TAG, e);
                }
            }
            ((TextView) view.findViewById(R.id.eng_software_app_versions)).setText(apkVersions.toString());
        }

        if (config.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Stepper) ||
                config.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Climbmill)) {
            findViewById(R.id.eng_accumulated_distance).setVisibility(View.GONE);
            findViewById(R.id.eng_accumulated_floors).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.eng_accumulated_distance).setVisibility(View.VISIBLE);
            findViewById(R.id.eng_accumulated_floors).setVisibility(View.GONE);
        }


    }

    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        dataMatrix.finish();
    }


    public static void performFactoryReset(Context context, boolean isFullFactoryReset) {
        new Thread(() -> {
            ConsoleServicesHelper.stopServicesForReset();

            new HeadphoneScanner(ContextUtil.getApplicationContext(), true);
            new MediaDeviceScanner().clearDevices();

            if (isFullFactoryReset) SettingKeys.resetFactoryDefault();
            else SettingKeys.resetDefault();
            // Reset units, clock, and date format.
            DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL));
            DataMatrix.putInt(ImportExportKeys.DATA_CURRENT_DATE_FORMAT_INT, DataMatrix.getInt(SettingKeys.General.DATA_DATE_FORMAT_ENUM_INT));
            DataMatrix.putBoolean(ImportExportKeys.DATA_CURRENT_CLOCK_FORMAT_IS_24_HOURS_BOOL, DataMatrix.getBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL));
            rebootConsole();
        }).start();
    }
    
    public static void rebootConsole() {
        DataMatrix.putString(HALKeys.ACTION_CONSOLE_REBOOT_STRING, "MES");
    }

    private void getTunerVersion() {
        new Thread(() -> {
            try {
                // Reset the TV here.  We need to wait a little bit to make sure it is fully reset.
                SettingKeys.TVSource source = SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING));
                if (source.equals(SettingKeys.TVSource.US5) || source.equals(SettingKeys.TVSource.US6) || source.equals(SettingKeys.TVSource.KM4) || source.equals(SettingKeys.TVSource.JP0) || source.equals(SettingKeys.TVSource.BR2) || source.equals(SettingKeys.TVSource.US7)) {
                    if(DataMatrix.getString(TVKeys.DATA_TUNER_VERSION_STRING).equals("--")) {
                        TVControllerBase tvController = TVControllerFactory.getTVController(getContext(), source);
                        tvController.open();
                        // Delays help allow the swift info serial port send the commands and get responses.
                        Thread.sleep(300 /*3000*/);
                        Log.d(TAG, "show: call getSwiftSoftwareVersion");
                        SwiftManager.getInstance().getSwiftSoftwareVersion();
                        Thread.sleep(300);
                        tvController.destroy();

                        // need to refresh firmware versions strings
                        refreshFirmwareVersions();
                    }
                } else if (source.isLG()) {
                    if (DataMatrix.getString(TVKeys.DATA_TUNER_VERSION_STRING).equals("--")) {
                        IChimeraTuner<? extends IChannel> tuner = TunerFactory.INSTANCE.getTuner(getContext(), source);
                        if (tuner != null) {
                            tuner.getVersion();
                            SystemClock.sleep(1000);
                            tuner.destroy();
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void refreshFirmwareVersions() {
        VersionHelper.getFirmwareVersionsAsync("", "\n").thenAcceptAsync((firmwareVersions) -> {
            handler.post(() -> {
                ((TextView) view.findViewById(R.id.eng_software_firmware_version)).setText(firmwareVersions);
            });
        });
    }
}

