package com.jht.chimera.ui.machinesettings.pages;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.jht.androiduiwidgets.SimpleInputConfiguration;
import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.OptionManager;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.TVHDMIOutputResolution;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.widget.CommonRoutines;
import com.jht.chimera.lib.widget.dialog.MessageBoxDialog;
import com.jht.chimera.lib.workoutapps.AppIconManager;
import com.jht.chimera.lib.workoutapps.AppIds;
import com.jht.chimera.lib.workoutapps.StartupOverrideConfig;
import com.jht.chimera.ui.machinesettings.dialogs.ProjectDialogFactory;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.updatemanager.UpdateManagerClient;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Implements the eng_workout page gluing the page to the model layer.
 */
public class Experimental extends SettingPageBase {

    private View view;
    private SettingKeys.TVSource source;

    //region TextView UI elements
    private TextView buildInfo_text;
    private TextView crashesInCache_text;
    private TextView lcbType_text;
    //endregion

    //region CheckBox UI elements
    private CheckBox chameleonAutoHide_checkBox;
    private CheckBox enableStbStreaming_checkBox;
    private CheckBox enableAnalytics_checkBox;
    private CheckBox forceDowngradeUpdates_checkBox;
    private CheckBox enableDapiFallback_checkBox;
    private CheckBox crashReportDisableEndpoints_checkBox;
    private CheckBox crashReportDisableBlocklist_checkBox;
    private CheckBox crashReportDebugLogs_checkBox;
    private CheckBox crashMonitoring_checkBox;
    private CheckBox powerLimiting_checkBox;
    private CheckBox showStbCommandList_checkBox;
    private CheckBox testDelayedVa_checkBox;
    private CheckBox forceHideIfit_checkBox;
    private CheckBox showStbAdvanced_checkBox;
    private CheckBox enableYmca360_checkBox;
    private CheckBox enableMHRC_checkBox;
    private CheckBox dapiAnalyticsEnabled_checkBox;
    private CheckBox enableDisneyApp_checkBox;
    private CheckBox ftmsControlEnabled_checkBox;
    private CheckBox enableAmazonFreevee_checkBox;
    private CheckBox enableDailyForceReboot_checkBox;
    private CheckBox enableCustomPasscode_checkBox;
    private CheckBox sendLogDataToAltEndpoint_checkBox;
    private CheckBox sendIncidentDataAltEndpoint_checkBox;
    private CheckBox persistentLogs_checkBox;
    private CheckBox disableWifi_checkBox;
    private CheckBox enableHulu_checkBox;
    private CheckBox sendWorkoutDataAltEndpoint_checkBox;
    private CheckBox enableIscAtBoot_checkBox;
    private CheckBox nightlyUpdates_checkBox;
    //endregion

    //region Button UI elements
    private Button sendManualCrash_button;
    private Button clearCrashReportingCache_button;
    private Button updateCrashReportingCache_button;
    private Button downloadCachedLogs_button;
    private Button resetAnalytics_button;
    private Button exportAnalytics_button;
    private Button viewAnalytics_button;
    private Button applyPasscode_button;
    private Button clearUpdates_button;
    private Button removeIfitMarker_button;
    //endregion

    //region UserTextInput UI elements
    private UserTextInput wiredHeadphoneHdmiDelay_textInput;
    private UserTextInput wirelessHeadphoneHdmiDelay_textInput;
    private UserTextInput enterCustomPasscode_textInput;
    private UserTextInput confirmCustomPasscode_textInput;
    private UserTextInput crl_collectionTime_textInput;
    private UserTextInput crashReportingLogsAlternativeEndpoint_textInput;
    private UserTextInput crashReportingIncidentDataAlternativeEndpoint_textInput;
    private UserTextInput maxWorkoutTime_textInput;
    private UserTextInput hdmiErrorThreshold_textInput;
    private UserTextInput deinterlaceSkipFrame_textInput;
    private UserTextInput recentAppLimit_textInput;
    private UserTextInput distancePrecision_textInput;
    private UserTextInput bumpSensor_textInput;
    private UserTextInput workoutDataAlternativeEndpoint_textInput;
    private UserTextInput clubId_textInput;
    private UserTextInput customAppPackagePath_textInput;
    //endregion

    //region OptionSelectorItem UI elements
    private OptionSelectorItem iptvPlayerType_dropdown;
    private OptionSelectorItem uiList_chameleon_dropdown;
    private OptionSelectorItem themeList_dropdown;
    private OptionSelectorItem dapiEnv_dropdown;
    private OptionSelectorItem hdmiResolution_dropdown;
    private OptionSelectorItem weatherApp_dropdown;
    private OptionSelectorItem defaultSystemApp_dropdown;
    private OptionSelectorItem snapshotSelector_dropdown;
    //endregion

    public Experimental() {}

    @Override
    protected View onCreate(final Context context, ViewGroup container) {
        view = LayoutInflater.from(context).inflate(R.layout.experimental, null);
        container.addView(view);
        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);

        if (BuildConfig.HAS_TV) {
            source = SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING, "US6")); //NON-NLS
        }

        setupTextViews();
        setupCheckboxesUiElements();
        initCheckboxes();
        setupButtons();
        initTextInputs();
        setupDropdownUiElements();

        setupPasscodeLockout();
        setupCrlCollectionTimeText();
        setupSendDataAlternateEndpoint();
        setupAlternateEndpoint();
        setupPersistentLogs();
        setupDisableWifi();
        setupAdbCheckbox();

        if (ProductConfig.isCommercial()) {
            commercialHuluCheckBox();
        }

        setupMaxWorkoutTime();
        setupHdmiErrorThreshold();
        setupDeinterlaceText();

        setupRecentAppLimit();
        setupDistancePrecision();
        setupIptvPlayer();
        setupBumpSensor();
        setupAlternateDataEndpoint();
        setupClubIdText();

        // this is quick and dirty test code for test purposes
        setupChameleonUI();

        setupDapiDropdown();
        setupHdmiResolution();
        setupWeatherApp();
        setupDefaultApps();
        setupLcbTypeTextField();
        setupSnapshotDropdown();
        setupClearUpdatesButton();
        setupIscAtBoot_checkBox();

        if (ProductConfig.isRetail()) {
            retailRemoveIfitMarker();
        }

        setupNightlyUpdatesUI();
    }

    private void setupDropdownUiElements() {
        iptvPlayerType_dropdown = view.findViewById(R.id.eng_iptv_player_type);
        uiList_chameleon_dropdown = view.findViewById(R.id.chameleon);
        themeList_dropdown = view.findViewById(R.id.eng_upload_theme);
        dapiEnv_dropdown = view.findViewById(R.id.eng_dapi_env);
        hdmiResolution_dropdown = view.findViewById(R.id.eng_hdmi_resolution);
        weatherApp_dropdown = view.findViewById(R.id.eng_weather_app_selection);
        defaultSystemApp_dropdown = view.findViewById(R.id.eng_default_system_app);
        snapshotSelector_dropdown = view.findViewById(R.id.workout_snapshot_selector);
    }

    private void setupPasscodeLockout() {
        enterCustomPasscode_textInput.setInputProperties(enterCustomPasscode_textInput.getInputProperties().maxChars(SettingsLockout.MAX_PASSCODE_LENGTH));
        confirmCustomPasscode_textInput.setInputProperties(confirmCustomPasscode_textInput.getInputProperties().maxChars(SettingsLockout.MAX_PASSCODE_LENGTH));
        setClick(applyPasscode_button, v -> updateSettingsLockoutState(enableCustomPasscode_checkBox, enterCustomPasscode_textInput, confirmCustomPasscode_textInput, view, true));
        enableCustomPasscode_checkBox.setOnClickListener(v -> {
            if (!enableCustomPasscode_checkBox.isChecked() && SettingsLockout.getInstance().getPasscodeState() == SettingsLockout.PasscodeState.ENABLED) { //we are disabling, so do a confirmation
                ProjectDialogFactory.confirm(view, StringLookup.getString(R.string.are_you_sure_passcode_pg, getContext()), () -> {
                    updateSettingsLockoutState(enableCustomPasscode_checkBox, enterCustomPasscode_textInput, confirmCustomPasscode_textInput, view, false); //on confirm, update the state
                }, () -> {
                    enableCustomPasscode_checkBox.setChecked(true); //on cancel, set checkbox back to what it was before
                });
            } else { //we are enabling
                updateSettingsLockoutState(enableCustomPasscode_checkBox, enterCustomPasscode_textInput, confirmCustomPasscode_textInput, view, false);
            }
        });
        enterCustomPasscode_textInput.setOnTextChanged(v -> {
            if (enterCustomPasscode_textInput.getText().isEmpty()) return;
            updateSettingsLockoutState(enableCustomPasscode_checkBox, enterCustomPasscode_textInput, confirmCustomPasscode_textInput, view, false);
        });
        confirmCustomPasscode_textInput.setOnTextChanged(v -> {
            if (confirmCustomPasscode_textInput.getText().isEmpty()) return;
            updateSettingsLockoutState(enableCustomPasscode_checkBox, enterCustomPasscode_textInput, confirmCustomPasscode_textInput, view, false);
        });

        SettingsLockout.PasscodeState passcodeState = SettingsLockout.getInstance().getPasscodeState();

        if (passcodeState == SettingsLockout.PasscodeState.PASSCODE_ENTERED || passcodeState == SettingsLockout.PasscodeState.CONFIRMATION_ENTERED) {
            //if we are either of these states in show(), then the user navigated away and back to the experimental tab in the middle of passcode entry, and should start over
            updateSettingsLockoutState(enableCustomPasscode_checkBox, enterCustomPasscode_textInput, confirmCustomPasscode_textInput, view, false);
        } else {
            updateSettingsLockoutVisuals(passcodeState, enableCustomPasscode_checkBox, enterCustomPasscode_textInput, confirmCustomPasscode_textInput);
        }
    }

    private void initTextInputs() {
        wiredHeadphoneHdmiDelay_textInput = view.findViewById(R.id.eng_wired_headphone_hdmi_delay);
        wirelessHeadphoneHdmiDelay_textInput = view.findViewById(R.id.eng_wireless_headphone_hdmi_delay);
        enterCustomPasscode_textInput = view.findViewById(R.id.enter_custom_passcode);
        confirmCustomPasscode_textInput = view.findViewById(R.id.confirm_custom_passcode);
        crashReportingLogsAlternativeEndpoint_textInput = view.findViewById(R.id.eng_send_crash_report_log_data_to_alternate_endpoint);
        crashReportingIncidentDataAlternativeEndpoint_textInput = view.findViewById(R.id.eng_send_crash_report_incident_data_to_alternate_endpoint);
        maxWorkoutTime_textInput = view.findViewById(R.id.eng_max_workout_time);
        hdmiErrorThreshold_textInput = view.findViewById(R.id.eng_hdmi_error_threshold);
        deinterlaceSkipFrame_textInput = view.findViewById(R.id.eng_deinterlace_skip_frame);
        recentAppLimit_textInput = view.findViewById(R.id.eng_recent_workout_app_limit);
        crl_collectionTime_textInput = view.findViewById(R.id.crash_reporting_collection_time);
        distancePrecision_textInput = view.findViewById(R.id.eng_distance_precision);
        bumpSensor_textInput = view.findViewById(R.id.bump_sensor_setting);
        workoutDataAlternativeEndpoint_textInput = view.findViewById(R.id.eng_workout_data_alternate_endpoint);
        clubId_textInput = view.findViewById(R.id.eng_gen_club_id);
        customAppPackagePath_textInput = view.findViewById(R.id.eng_custom_app_package_path);

        linkUserIntInput(wiredHeadphoneHdmiDelay_textInput, SettingKeys.Experimental.DATA_WIRED_HEADPHONE_HDMI_DELAY_INT);
        linkUserIntInput(wirelessHeadphoneHdmiDelay_textInput, SettingKeys.Experimental.DATA_WIRELESS_HEADPHONE_HDMI_DELAY_INT);
    }

    private void initCheckboxes() {
        initCheckbox(chameleonAutoHide_checkBox, SettingKeys.Experimental.DATA_ENABLE_AUTO_HIDE_PANELS_BOOL);
        initCheckbox(enableStbStreaming_checkBox, SettingKeys.Experimental.DATA_ENABLE_STB_STREAM_APPS_BOOL);
        initCheckbox(enableAnalytics_checkBox, SettingKeys.Experimental.DATA_ENABLE_ANALYTICS_BOOL);
        initCheckbox(forceDowngradeUpdates_checkBox, SettingKeys.Experimental.DATA_FORCE_DOWNGRADE_UPDATES_BOOL);
        initCheckbox(enableDapiFallback_checkBox, SettingKeys.Experimental.DATA_ENABLE_DAPI_FALLBACK_BOOL);
        initCheckbox(crashReportDisableEndpoints_checkBox, SettingKeys.Experimental.DATA_CRASH_REPORTING_DISABLE_ENDPOINT_BOOL);
        initCheckbox(crashReportDisableBlocklist_checkBox, SettingKeys.Experimental.DATA_CRASH_REPORTING_DISABLE_BLOCKLIST_BOOL);
        initCheckbox(crashReportDebugLogs_checkBox, SettingKeys.Experimental.DATA_CRASH_REPORTING_DEBUG_LOGS_BOOL);
        initCheckbox(crashMonitoring_checkBox, SettingKeys.Experimental.DATA_CRASH_MONITORING_BOOL);
        initCheckbox(powerLimiting_checkBox, SettingKeys.Experimental.DATA_POWER_LIMITING_BOOL);
        initCheckbox(showStbCommandList_checkBox, SettingKeys.Experimental.DATA_SHOW_STB_COMMAND_LIST_BOOL);
        initCheckbox(testDelayedVa_checkBox, SettingKeys.Experimental.DATA_DELAY_LOAD_VA_BOOL);
        initCheckbox(forceHideIfit_checkBox, SettingKeys.Experimental.DATA_FORCE_HIDE_IFIT_APP_BOOL);
        initCheckbox(showStbAdvanced_checkBox, SettingKeys.TV.DATA_SHOW_STB_ADVANCED_SETTINGS_BOOL);
        initCheckbox(enableYmca360_checkBox, SettingKeys.Experimental.DATA_ENABLE_YMCA_360_BOOL);
        initCheckbox(enableMHRC_checkBox, SettingKeys.Experimental.DATA_ENABLE_MILE_HIGH_RUN_CLUB_BOOL);
        initCheckbox(dapiAnalyticsEnabled_checkBox, SettingKeys.Experimental.DATA_ENABLE_DAPI_ANALYTICS_BOOL);
        initCheckbox(enableDisneyApp_checkBox, SettingKeys.Application.DATA_DISNEY_PLUS_ENABLED_BOOL);
        initCheckbox(ftmsControlEnabled_checkBox, SettingKeys.Experimental.DATA_FTMS_ENABLE_CONTROL_POINT_BOOL);
        initCheckbox(enableAmazonFreevee_checkBox, SettingKeys.Experimental.DATA_AMAZON_FREEVEE_ENABLE_BOOL);
        initCheckbox(enableDailyForceReboot_checkBox, SettingKeys.Experimental.DATA_DAILY_FORCE_REBOOT_ENABLE_BOOL);
        enableCustomPasscode_checkBox = view.findViewById(R.id.enable_settings_lockout);
        enableIscAtBoot_checkBox = findViewById(R.id.enable_isc_at_boot);
        nightlyUpdates_checkBox = findViewById(R.id.eng_nightly_updates);
    }

    private void setupButtons() {
        sendManualCrash_button = findViewById(R.id.sendManualCrash);
        clearCrashReportingCache_button = findViewById(R.id.clearCrashReportingCache);
        updateCrashReportingCache_button = findViewById(R.id.updateCrashReportingCache);
        downloadCachedLogs_button = findViewById(R.id.downloadCachedLogs);
        resetAnalytics_button = findViewById(R.id.reset_analytics);
        exportAnalytics_button = findViewById(R.id.export_analytics);
        viewAnalytics_button = findViewById(R.id.view_analytics);
        applyPasscode_button = findViewById(R.id.apply_passcode);
        clearUpdates_button = view.findViewById(R.id.clear_updates);
        removeIfitMarker_button = findViewById(R.id.eng_remove_iFit_marker);

        setClick(sendManualCrash_button, v -> DataMatrix.sendEvent(LoggerKeys.ACTION_SEND_MANUAL_CRASH));
        setClick(clearCrashReportingCache_button, v -> DataMatrix.sendEvent(LoggerKeys.ACTION_CLEAR_CRASH_REPORTING_CACHE));
        setClick(updateCrashReportingCache_button, v -> DataMatrix.sendEvent(LoggerKeys.ACTION_UPDATE_CRASH_REPORTING_CACHE));
        setClick(downloadCachedLogs_button, v -> DataMatrix.sendEvent(LoggerKeys.ACTION_DOWNLOAD_CACHED_LOGS));
        resetAnalytics_button.setOnClickListener(v -> DataMatrix.sendEvent(LoggerKeys.ACTION_RESET_ANALYTICS));
        exportAnalytics_button.setOnClickListener(v -> DataMatrix.sendEvent(LoggerKeys.ACTION_EXPORT_ANALYTICS));
        viewAnalytics_button.setOnClickListener(v -> DataMatrix.sendEvent(LoggerKeys.ACTION_VIEW_ANALYTICS));
    }

    private void setupCheckboxesUiElements() {
        chameleonAutoHide_checkBox = findViewById(R.id.chameleon_autohide);
        enableStbStreaming_checkBox = findViewById(R.id.enable_stb_streaming);
        enableAnalytics_checkBox = findViewById(R.id.enable_analytics);
        forceDowngradeUpdates_checkBox = findViewById(R.id.force_downgrade_updates_checkbox);
        enableDapiFallback_checkBox = findViewById(R.id.enable_dapi_fallback);
        crashReportDisableEndpoints_checkBox = findViewById(R.id.crash_report_disable_endpoints);
        crashReportDisableBlocklist_checkBox = findViewById(R.id.crash_report_disable_blocklist);
        crashReportDebugLogs_checkBox = findViewById(R.id.crash_report_debug_logs);
        crashMonitoring_checkBox = findViewById(R.id.crash_monitoring);
        powerLimiting_checkBox = findViewById(R.id.power_limiting);
        showStbCommandList_checkBox = findViewById(R.id.show_stb_command_list_checkbox);
        testDelayedVa_checkBox = findViewById(R.id.test_delayed_va);
        forceHideIfit_checkBox = findViewById(R.id.force_hide_ifit);
        showStbAdvanced_checkBox = findViewById(R.id.show_stb_advanced);
        enableYmca360_checkBox = findViewById(R.id.enable_ymca_360);
        enableMHRC_checkBox = findViewById(R.id.enable_mile_high_run_club);
        dapiAnalyticsEnabled_checkBox = findViewById(R.id.dapi_analytics_enabled);
        enableDisneyApp_checkBox = findViewById(R.id.enable_disney_app);
        ftmsControlEnabled_checkBox = findViewById(R.id.ftms_control_enabled);
        enableAmazonFreevee_checkBox = findViewById(R.id.enable_amazon_freevee);
        enableDailyForceReboot_checkBox = findViewById(R.id.enable_daily_force_reboot);
        sendLogDataToAltEndpoint_checkBox = view.findViewById(R.id.eng_send_crash_report_log_data_to_alternate_endpoint_enabled);
        sendIncidentDataAltEndpoint_checkBox = view.findViewById(R.id.eng_send_crash_report_incident_data_to_alternate_endpoint_enabled);
        persistentLogs_checkBox = findViewById(R.id.persistent_logs);
        disableWifi_checkBox = findViewById(R.id.disable_wifi);
        enableHulu_checkBox = findViewById(R.id.enable_hulu_app);
        sendWorkoutDataAltEndpoint_checkBox = view.findViewById(R.id.eng_send_workout_data_to_alternate_endpoint_enabled);
    }

    private void setupTextViews() {
        buildInfo_text = findViewById(R.id.build_info);
        crashesInCache_text = view.findViewById(R.id.crashes_in_cache);
        lcbType_text = view.findViewById(R.id.text_lcb_type);
        setText(buildInfo_text, "Build: " + BuildConfig.GIT_HASH + " " + BuildConfig.GIT_BRANCH);
        setText(crashesInCache_text, "Crashes Cached: " + DataMatrix.getInt(SettingKeys.Experimental.DATA_CRASH_REPORTING_CACHED_CRASHES_INT));
    }

    private void setupAdbCheckbox() {
        CheckBox adbCheckbox = findViewById(R.id.DATA_ADB_VIA_NETWORK_BOOL);
        adbCheckbox.setChecked(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ADB_VIA_NETWORK_BOOL));

        adbCheckbox.setOnCheckedChangeListener((v, isChecked) -> {
            nightlyCheckboxChanged(v, isChecked, adbCheckbox);
        });
    }

    private void setupCrlCollectionTimeText() {
        if (crl_collectionTime_textInput != null) {
            crl_collectionTime_textInput.setText(DataMatrix.getInt(
                    SettingKeys.Experimental.DATA_CRASH_REPORTING_COLLECTION_TIME_MINUTES_INT) + "");
            crl_collectionTime_textInput.setOnTextChanged(s ->
                    DataMatrix.putInt(SettingKeys.Experimental.DATA_CRASH_REPORTING_COLLECTION_TIME_MINUTES_INT,
                            CommonRoutines.parseInt(s))
            );
        }
    }

    private void setupSendDataAlternateEndpoint() {
        if (sendLogDataToAltEndpoint_checkBox != null && crashReportingLogsAlternativeEndpoint_textInput != null) {
            sendLogDataToAltEndpoint_checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_CRASH_REPORTING_LOG_URL_OVERRIDE_BOOL));
            sendLogDataToAltEndpoint_checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                DataMatrix.putBoolean(SettingKeys.Experimental.DATA_CRASH_REPORTING_LOG_URL_OVERRIDE_BOOL, isChecked);
                crashReportingLogsAlternativeEndpoint_textInput.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            });
            crashReportingLogsAlternativeEndpoint_textInput.setText(DataMatrix.getString(SettingKeys.Experimental.DATA_CRASH_REPORTING_LOG_URL_OVERRIDE_STRING));
            crashReportingLogsAlternativeEndpoint_textInput.setVisibility(sendLogDataToAltEndpoint_checkBox.isChecked() ? View.VISIBLE : View.GONE);
        }

        linkUserTextInput(crashReportingLogsAlternativeEndpoint_textInput, SettingKeys.Experimental.DATA_CRASH_REPORTING_LOG_URL_OVERRIDE_STRING);
    }

    private void setupAlternateEndpoint() {
        if (sendIncidentDataAltEndpoint_checkBox != null && crashReportingIncidentDataAlternativeEndpoint_textInput != null) {
            sendIncidentDataAltEndpoint_checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_CRASH_REPORTING_INCIDENT_URL_OVERRIDE_BOOL));
            sendIncidentDataAltEndpoint_checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                DataMatrix.putBoolean(SettingKeys.Experimental.DATA_CRASH_REPORTING_INCIDENT_URL_OVERRIDE_BOOL, isChecked);
                crashReportingIncidentDataAlternativeEndpoint_textInput.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            });
            crashReportingIncidentDataAlternativeEndpoint_textInput.setText(DataMatrix.getString(SettingKeys.Experimental.DATA_CRASH_REPORTING_INCIDENT_URL_OVERRIDE_STRING));
            crashReportingIncidentDataAlternativeEndpoint_textInput.setVisibility(sendIncidentDataAltEndpoint_checkBox.isChecked() ? View.VISIBLE : View.GONE);
        }
        linkUserTextInput(crashReportingIncidentDataAlternativeEndpoint_textInput, SettingKeys.Experimental.DATA_CRASH_REPORTING_INCIDENT_URL_OVERRIDE_STRING);
    }

    private void setupPersistentLogs() {
        persistentLogs_checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL));
        persistentLogs_checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                DataMatrix.putLong(SettingKeys.Debug.DATA_SETTING_PERSIST_LOGGING_START_TIME_LONG, new Date().getTime());
            }
            DataMatrix.putBoolean(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL, isChecked);
        });
    }

    private void setupDisableWifi() {
        if (disableWifi_checkBox != null) {
            disableWifi_checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_DISABLE_WIFI_BOOL));
            disableWifi_checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                DataMatrix.putBoolean(SettingKeys.Experimental.DATA_DISABLE_WIFI_BOOL, isChecked);
                Context applicationContext = ContextUtil.getApplicationContext();
                if (applicationContext != null) {
                    WifiManager wm = (WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE);
                    if (wm != null) {
                        //Disable Android wifi if setting is checked
                        Log.d("Experimental", !isChecked ? "ENABLING WIFI" : "DISABLING WIFI");
                        wm.setWifiEnabled(!isChecked);
                    }
                }
            });
        }
    }

    private static void nightlyCheckboxChanged(CompoundButton v, boolean isChecked, CheckBox adbCheckbox) {
        if (!isChecked && DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ADB_VIA_NETWORK_BOOL)) {
            DataMatrix.putBoolean(SettingKeys.Experimental.DATA_ADB_VIA_NETWORK_BOOL, false);
        } else if (isChecked && !DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ADB_VIA_NETWORK_BOOL)) {
            //Ignore the password requirement on debug builds
            if (BuildConfig.DEBUG) {
                DataMatrix.putBoolean(SettingKeys.Experimental.DATA_ADB_VIA_NETWORK_BOOL, true);
                adbCheckbox.setChecked(true);
            } else {
                //Require password input for release builds
                SimpleInputConfiguration input = new SimpleInputConfiguration();
                input.isPassword(true);
                adbCheckbox.setChecked(false);
                ProjectDialogFactory.keypadDialog(v, input, () -> {
                    if (input.value().equals("5482328378")) {
                        DataMatrix.putBoolean(SettingKeys.Experimental.DATA_ADB_VIA_NETWORK_BOOL, true);
                        adbCheckbox.setChecked(true);
                    }
                }, true);
            }
        }
    }

    private void commercialHuluCheckBox() {
        if (enableHulu_checkBox != null) {
            enableHulu_checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.Application.DATA_HULU_PLUS_ENABLED_BOOL));
            enableHulu_checkBox.setOnCheckedChangeListener((v, isChecked) -> DataMatrix.putBoolean(SettingKeys.Application.DATA_HULU_PLUS_ENABLED_BOOL, isChecked));
        }
    }

    /**
     * Advanced override for max workout time (for potentially warranty voiding durations)
     */
    private void setupMaxWorkoutTime() {
        if (maxWorkoutTime_textInput != null) {
            maxWorkoutTime_textInput.setOnTextChanged(s -> {
                int value = CommonRoutines.parseInt(s);
                DataMatrix.putInt(SettingKeys.Workout.DATA_MAX_WORKOUT_TIME_MIN_INT, value);
                if (value < DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_TIME_MIN_INT)) {
                    DataMatrix.putInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_TIME_MIN_INT, value);
                }
            });
            maxWorkoutTime_textInput.setInputProperties(maxWorkoutTime_textInput.getInputProperties().inputValue("" + DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_WORKOUT_TIME_MIN_INT)));
        }
    }

    private void setupHdmiErrorThreshold() {
        hdmiErrorThreshold_textInput.setText(DataMatrix.getInt(SettingKeys.Experimental.DATA_HDMI_ERROR_THRESHOLD_V2_INT) + "");
        hdmiErrorThreshold_textInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.Experimental.DATA_HDMI_ERROR_THRESHOLD_V2_INT, CommonRoutines.parseInt(s)));
    }

    private void setupDeinterlaceText() {
        deinterlaceSkipFrame_textInput.setText(DataMatrix.getInt(SettingKeys.Experimental.DATA_IPTV_DEINTERLACE_FRAME_SKIP_INT) + "");
        deinterlaceSkipFrame_textInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.Experimental.DATA_IPTV_DEINTERLACE_FRAME_SKIP_INT, CommonRoutines.parseInt(s)));
    }

    private void setupRecentAppLimit() {
        recentAppLimit_textInput.setText(DataMatrix.getInt(SettingKeys.Experimental.DATA_RECENT_WORKOUT_APP_LIMIT_2_INT) + "");
        recentAppLimit_textInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.Experimental.DATA_RECENT_WORKOUT_APP_LIMIT_2_INT, CommonRoutines.parseInt(s)));
    }

    private void setupDistancePrecision() {
        distancePrecision_textInput.setText(DataMatrix.getInt(SettingKeys.Experimental.DATA_DISTANCE_PRECISION_INT) + "");
        distancePrecision_textInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.Experimental.DATA_DISTANCE_PRECISION_INT, CommonRoutines.parseInt(s)));
    }

    private void setupIptvPlayer() {
        iptvPlayerType_dropdown.setOptions(R.array.iptv_player_type, DataMatrix.getInt(SettingKeys.Experimental.DATA_IPTV_PLAYER_TYPE_INT));
        iptvPlayerType_dropdown.setOnOptionSelected((pos, option) -> DataMatrix.putInt(SettingKeys.Experimental.DATA_IPTV_PLAYER_TYPE_INT, pos));
    }

    private void setupBumpSensor() {
        if (bumpSensor_textInput != null) {
            bumpSensor_textInput.setText("" + DataMatrix.getInt(SettingKeys.Experimental.DATA_BUMP_SENSOR_SETTING_INT));
            bumpSensor_textInput.setOnTextChanged(s -> {
                DataMatrix.putInt(SettingKeys.Experimental.DATA_BUMP_SENSOR_SETTING_INT, CommonRoutines.parseInt(s));
                UpdateManagerClient.cancelDownload(getContext());
            });
        }
    }

    private void setupAlternateDataEndpoint() {
        if (sendWorkoutDataAltEndpoint_checkBox != null && workoutDataAlternativeEndpoint_textInput != null) {
            sendWorkoutDataAltEndpoint_checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_SEND_WORKOUT_DATA_TO_ALTERNATE_ENDPOINT_BOOL));
            sendWorkoutDataAltEndpoint_checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                DataMatrix.putBoolean(SettingKeys.Experimental.DATA_ENABLE_SEND_WORKOUT_DATA_TO_ALTERNATE_ENDPOINT_BOOL, isChecked);
                workoutDataAlternativeEndpoint_textInput.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            });
            workoutDataAlternativeEndpoint_textInput.setText(DataMatrix.getString(SettingKeys.Experimental.DATA_WORKOUT_DATA_ALTERNATE_ENDPOINT_STRING));
            workoutDataAlternativeEndpoint_textInput.setVisibility(sendWorkoutDataAltEndpoint_checkBox.isChecked() ? View.VISIBLE : View.GONE);
        }

        linkUserTextInput(workoutDataAlternativeEndpoint_textInput, SettingKeys.Experimental.DATA_WORKOUT_DATA_ALTERNATE_ENDPOINT_STRING);
    }

    private void setupClubIdText() {
        if (clubId_textInput != null) {
            clubId_textInput.setText("" + DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT));
            clubId_textInput.setOnTextChanged(s -> {
                DataMatrix.putInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT, CommonRoutines.parseInt(s));
                UpdateManagerClient.cancelDownload(getContext());
            });
        }
    }

    private void setupChameleonUI() {
        if (uiList_chameleon_dropdown != null) {
            final HashMap<String, Integer> uiNames = getNames();
            final int selectedUiPosition = getStartPosition(uiNames, DataMatrix.getInt(SettingKeys.Experimental.DATA_UI_SYSTEM_ORDINAL_INT));
            OptionManager options = new OptionManager(uiNames.keySet().toArray(new String[0]), selectedUiPosition);
            uiList_chameleon_dropdown.setOptions(options);
            uiList_chameleon_dropdown.setOnOptionSelected((pos, option) -> {
                int position = uiNames.get(option);
                DataMatrix.putInt(SettingKeys.Experimental.DATA_UI_SYSTEM_ORDINAL_INT, position);
            });
        }
        if (themeList_dropdown != null) {
            final String[] themeNames = new String[]{"Light", "Onyx Dark", "Touch Basic", "Custom"};
            final Uri customFile = Uri.fromFile(new File(getContext().getFilesDir(), "theme/theme.json"));
            final String[] themeUris = new String[]{"preset:light", "preset:dark", "preset:basic", customFile.toString()};
            String themeUri = DataMatrix.getString(SettingKeys.General.DATA_DYNAMIC_THEME_URI_STRING);
            int index;
            for (index = 0; index < themeUris.length; ++index) {
                if (themeUris[index].equals(themeUri)) break;
            }
            if (index > themeNames.length - 1) index = themeNames.length - 1; // if not a preset, then its custom
            OptionManager themeOptionsManager = new OptionManager(themeNames, index);
            themeList_dropdown.setOptions(themeOptionsManager);
            themeList_dropdown.setOnOptionSelected((pos, option) -> {
                DataMatrix.putString(SettingKeys.General.DATA_DYNAMIC_THEME_URI_STRING, themeUris[pos]);
            });
        }
    }

    private void setupDapiDropdown() {
        if (dapiEnv_dropdown != null) {
            dapiEnv_dropdown.setOptions(R.array.dapi_environments, DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT));
            dapiEnv_dropdown.setOnOptionSelected((pos, option) -> {
                DataMatrix.putInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, pos);
                UpdateManagerClient.cancelDownload(getContext());
            });
        }
    }

    private void setupHdmiResolution() {
        if (hdmiResolution_dropdown != null) {
            List<TVHDMIOutputResolution> supportedResolutions = ProductConfig.getHDMIResolutionConfig().getSupportedResolutions();
            String[] displayNames = supportedResolutions.stream().map(r -> r.displayName).toArray(String[]::new);
            TVHDMIOutputResolution selectedResolution = TVHDMIOutputResolution.fromName(DataMatrix.getString(SettingKeys.TV.DATA_HDMI_OUTPUT_RESOLUTION_STRING));
            int selectedResolutionIndex = supportedResolutions.indexOf(selectedResolution);
            hdmiResolution_dropdown.setOptions(new OptionManager(displayNames, selectedResolutionIndex));
            hdmiResolution_dropdown.setOnOptionSelected((pos, option) ->
                    DataMatrix.putString(SettingKeys.TV.DATA_HDMI_OUTPUT_RESOLUTION_STRING, supportedResolutions.get(pos).name())
            );
        }
    }

    private void setupWeatherApp() {
        if (weatherApp_dropdown != null) {
            weatherApp_dropdown.setOptions(R.array.weather_applications, DataMatrix.getInt(SettingKeys.Application.DATA_WEATHER_APP_ENUM_INT));
            weatherApp_dropdown.setOnOptionSelected((pos, option) -> {
                DataMatrix.putInt(SettingKeys.Application.DATA_WEATHER_APP_ENUM_INT, pos);
                if (SettingKeys.WeatherApplication.values()[pos].equals(SettingKeys.WeatherApplication.MOJI_WEATHER_APP)) {
                    ExecuteAsRoot.run("pm enable com.moji.mjweather");
                } else {
                    ExecuteAsRoot.run("pm disable com.moji.mjweather");
                }
            });
        }
    }

        private void setupDefaultApps() {
            if (defaultSystemApp_dropdown != null && customAppPackagePath_textInput != null) {
                String currentUuid = DataMatrix.getString(SettingKeys.Experimental.DATA_OVERRIDE_MAIN_APP_ID_STRING, "");
                List<String> appIds = new ArrayList<>();
                for (StartupOverrideConfig.Entry entry : StartupOverrideConfig.DEFAULT_OVERRIDE_APPS) {
                    appIds.add(entry.uuid);// Build our dynamic list of start override options based on the StartupOverrideConfig entries.
                }
                if (!currentUuid.isEmpty() && !appIds.contains(currentUuid)) {// additional feature that can be removed, this dynamically adds any new Main apps that is not in our list to the selector UI.
                    appIds.add(currentUuid);
                }
                String[] displayNames = new String[appIds.size()];
                for (int i = 0; i < appIds.size(); i++) {
                    String appId = appIds.get(i);
                    String name = StartupOverrideConfig.getOverrideAppName(appId);// Build the display names using the single source of truth registry (StartupOverrideConfig).
                    if (name == null) {
                        name = AppIconManager.getInstance().findAppName(appId);//If it's a custom app not in the registry, we fall back to AppIconManager name lookup or raw UUID.
                    }
                    displayNames[i] = (name != null && !name.trim().isEmpty()) ? name : appId;
                }
                int selectedIndex = appIds.indexOf(currentUuid);// Find the index of the current active UUID within list. Default to index 0 if not found.
                if (selectedIndex == -1) {
                    selectedIndex = 0;
                }
                defaultSystemApp_dropdown.setOptions(new OptionManager(displayNames, selectedIndex));//Populate the OptionSelectorItem widget with the resolved display names and selected position.
                customAppPackagePath_textInput.setText(DataMatrix.getString(SettingKeys.Experimental.DATA_CUSTOM_APP_OVERRIDE_PACKAGE_NAME_STRING));
                customAppPackagePath_textInput.setOnTextChanged(s -> {
                    DataMatrix.putString(SettingKeys.Experimental.DATA_CUSTOM_APP_OVERRIDE_PACKAGE_NAME_STRING, customAppPackagePath_textInput.getText());
                    PackageManager pm = getContext().getPackageManager();
                    List<PackageInfo> packages = pm.getInstalledPackages(0);
                    boolean found = false;
                    for (PackageInfo packageInfo : packages) {
                        if (Objects.equals(s, packageInfo.packageName)) {
                            Log.d("Experimental", "Found package match: " + s);
                            found = true;
                            break;
                        }
                    }
                    if (!found && !s.isEmpty()) {
                        Log.d("Experimental", "Package not found: " + s);
                        Toast.makeText(getContext(), "Package not found: " + s, Toast.LENGTH_LONG).show();
                    }
                });
                defaultSystemApp_dropdown.setOnOptionSelected((pos, option) -> {
                    String overrideAppId = appIds.get(pos);//Persist the selected app UUID to storage whenever the option changes.
                    DataMatrix.putString(SettingKeys.Experimental.DATA_OVERRIDE_MAIN_APP_ID_STRING, overrideAppId);
                    DataMatrix.putString(SettingKeys.Experimental.DATA_CUSTOM_APP_OVERRIDE_PACKAGE_NAME_STRING, customAppPackagePath_textInput.getText());

                    if(Objects.equals(overrideAppId, AppIds.CUSTOM_OVERRIDE_APP_UUID)) {
                        customAppPackagePath_textInput.setVisibility(View.VISIBLE);
                    } else {
                        customAppPackagePath_textInput.setVisibility(View.INVISIBLE);
                    }
                });

                if(Objects.equals(currentUuid, AppIds.CUSTOM_OVERRIDE_APP_UUID)) {
                    customAppPackagePath_textInput.setVisibility(View.VISIBLE);
                } else {
                    customAppPackagePath_textInput.setVisibility(View.INVISIBLE);
                }
            }
        }

    private void setupLcbTypeTextField() {
        if (lcbType_text != null) {
            lcbType_text.setText(String.format("LCB: 0x%02x", DataMatrix.getInt(HALKeys.DATA_LCB_TYPE_INT)));
        }
    }

    private void setupSnapshotDropdown() {
        snapshotSelector_dropdown.setOptions(new OptionManager(getContext().getResources().getStringArray(R.array.workout_snapshot_settings), DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WORKOUT_SNAPSHOT_SETTING_INT)));
        snapshotSelector_dropdown.setOnOptionSelected((pos, option) -> DataMatrix.putInt(SettingKeys.AssetManagement.DATA_WORKOUT_SNAPSHOT_SETTING_INT, pos));
    }

    private void setupClearUpdatesButton() {
        if (clearUpdates_button != null) {
            clearUpdates_button.setOnClickListener(v -> DialogFactory.confirm(v, "Confirm clearing of update service", () -> {
                ExecuteAsRoot.run("pm clear com.jht.updateservice");
                General.rebootConsole();
            }, null));
        }
    }

    private void setupIscAtBoot_checkBox() {
        if (enableIscAtBoot_checkBox != null) {
            enableIscAtBoot_checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_ISC_AT_BOOT_BOOL));
            enableIscAtBoot_checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.Experimental.DATA_ENABLE_ISC_AT_BOOT_BOOL, isChecked));

            // enable isc at boot is only for km4 tuner
            if (!source.equals(SettingKeys.TVSource.KM4)) {
                enableIscAtBoot_checkBox.setVisibility(View.GONE);
            }
        }
    }

    private void retailRemoveIfitMarker() {
        if (removeIfitMarker_button != null && ProductConfig.isIfitBlockedOutsideNorthAmerica()) {
            removeIfitMarker_button.setVisibility(View.VISIBLE);
            removeIfitMarker_button.setOnClickListener(v -> {
                MessageBoxDialog dialog = new MessageBoxDialog(v.getContext());
                dialog.setTitle(getContext().getString(R.string.remove_ifit_marker_tc));
                dialog.setMessage(getContext().getString(R.string.remove_ifit_marker_pg));
                dialog.setButton0OnClickListener((v2) -> {
                    ExecuteAsRoot root = new ExecuteAsRoot();
                    root.addCommand("rm " + ProductConfig.getMarkerFilePathIfitBlockedOutsideNA());
                    root.addCommand("sync");
                    root.execute();
                    LaunchAppUtil.launchSplashScreen();
                });
                dialog.setButton1OnClickListener(v2 -> dialog.dismiss());
                dialog.setButtonsEnabled(true);
                dialog.show();
            });
        }
    }

    /**
     * Finds the key value pair where the value matches the currentOrdinal value and returns the
     * position in the map.
     *
     * @param uiNames        Map of name of UI versions to Ordinal values of SettingKeys.UiSystem
     * @param currentOrdinal number of selected Ordinal for the current UI System that the console is showing
     * @return returns the position of the key in the map with the value matching the currentOrdinal value.
     */
    private int getStartPosition(HashMap<String, Integer> uiNames, int currentOrdinal) {
        if (uiNames.containsValue(currentOrdinal)) {
            int count = 0;
            for (Object key : uiNames.keySet().toArray()) {
                try {
                    int item = uiNames.get((String) key);
                    if (item == currentOrdinal) {
                        return count;
                    }
                } catch (NullPointerException ignored) {
                }
                count++;
            }
        }
        return 0;
    }

    /**
     * Sets up the Check for Nightly Updates checkbox, and its check change listener. When unchecked, sets the DM key
     * to false. When checked, prompts for passkey entry. If passkey is 8675309, feature is enabled. See CGC-15340 for
     * more information.
     */
    private void setupNightlyUpdatesUI() {
        if (nightlyUpdates_checkBox != null) {
            nightlyUpdates_checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_NIGHTLY_UPDATES_BOOL, false));
            nightlyUpdates_checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isChecked) {
                    // if becomes unchecked, disable the feature
                    DataMatrix.putBoolean(SettingKeys.Experimental.DATA_ENABLE_NIGHTLY_UPDATES_BOOL, false);
                } else if (!DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_NIGHTLY_UPDATES_BOOL)) {
                    SimpleInputConfiguration input = new SimpleInputConfiguration();
                    input.isPassword(true);

                    /*
                     * If currently disabled, force checkbox to unchecked (false) until the valid passkey is entered.
                     * This addresses the problem of the checkbox not requiring the passkey to become checked.
                     */
                    nightlyUpdates_checkBox.setChecked(false);

                    // requires correct passkey in order to enable the feature
                    ProjectDialogFactory.keypadDialog(buttonView, input, () -> {
                        if (input.value().equals("8675309")) {
                            DataMatrix.putBoolean(SettingKeys.Experimental.DATA_ENABLE_NIGHTLY_UPDATES_BOOL, true);
                            nightlyUpdates_checkBox.setChecked(true);
                        }
                    }, true);
                }
            });
        }
    }


    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        AppIconManager.getInstance().reset();
    }

    /**
     * Calls into the settings lockout updateState method to track the current state of entering the passcode
     *
     * @param enableCustomPasscode  The checkbox for enabling the settings lockout in MES
     * @param enterCustomPasscode   The first text entry for the passcode in MES
     * @param confirmCustomPasscode The confirmation text entry for the passcode in MES
     * @param view                  The view to attach dialog messages for user feedback
     * @param fromApply             Whether or not the call is coming from the Apply Passcode button
     */
    public void updateSettingsLockoutState(CheckBox enableCustomPasscode, UserTextInput enterCustomPasscode,
                                           UserTextInput confirmCustomPasscode, View view, boolean fromApply) {
        String passcodeText = enterCustomPasscode.getText();
        String confirmationText = confirmCustomPasscode.getText();
        boolean isChecked = enableCustomPasscode.isChecked();
        SettingsLockout.PasscodeState currentState = SettingsLockout.getInstance().updateState(passcodeText, confirmationText, isChecked, view, fromApply);
        updateSettingsLockoutVisuals(currentState, enableCustomPasscode, enterCustomPasscode, confirmCustomPasscode);
    }

    /**
     * Updates the visibility of the passcode text entry fields based on the state of entering the passcode
     *
     * @param passcodeState The current state of passcode entry
     */
    public void updateSettingsLockoutVisuals(SettingsLockout.PasscodeState passcodeState, CheckBox enableCustomPasscode,
                                             UserTextInput enterCustomPasscode, UserTextInput confirmCustomPasscode) {
        Button applyCustomPasscode = view.findViewById(R.id.apply_passcode);
        switch (passcodeState) {
            case DISABLED:
                if (enableCustomPasscode.isChecked()) {
                    enterCustomPasscode.setVisibility(View.VISIBLE);
                } else {
                    enterCustomPasscode.setVisibility(View.GONE);
                    enterCustomPasscode.setText("");
                }
                confirmCustomPasscode.setVisibility(View.GONE);
                confirmCustomPasscode.setText("");
                applyCustomPasscode.setVisibility(View.GONE);
                break;
            case PASSCODE_ENTERED:
                enterCustomPasscode.setVisibility(View.VISIBLE);
                confirmCustomPasscode.setVisibility(View.VISIBLE);
                applyCustomPasscode.setVisibility(View.GONE);
                break;
            case CONFIRMATION_ENTERED:
                enterCustomPasscode.setVisibility(View.VISIBLE);
                confirmCustomPasscode.setVisibility(View.VISIBLE);
                applyCustomPasscode.setVisibility(View.VISIBLE);
                break;
            case ENABLED:
                enableCustomPasscode.setChecked(true);
                enterCustomPasscode.setVisibility(View.GONE);
                confirmCustomPasscode.setVisibility(View.GONE);
                applyCustomPasscode.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * Gets map of names of currently supported UI to show on the console.
     *
     * @return HashMap of ui version names as the key and ordinal of the UiSystem enum in SettingKeys as the value
     */
    private static HashMap<String, Integer> getNames() {
        SettingKeys.UiSystem[] array = SettingKeys.UiSystem.values();
        HashMap<String, Integer> retMap = new HashMap<>();

        for (SettingKeys.UiSystem uiSystem : array) {
            if (!Objects.equals(uiSystem.name(), SettingKeys.UiSystem.Legacy.name())) {
                retMap.put(uiSystem.name(), uiSystem.ordinal());
            }
        }

        return retMap;
    }
}