package com.jht.chimera.ui.machinesettings.pages;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.androidcommonalgorithms.util.SerialNumberUtil;
import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.OptionManager;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.lib.widget.CommonRoutines;
import com.jht.chimera.ui.machinesettings.AssetManagementLogins;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

/**
 * Contains the asset management page in MES.
 */
public class AssetManagement extends SettingPageBase {
    private static final String TAG = "AssetManagement";

    /**
     * Required file name to be able to import serial number from text file on a USB.
     */
    private static final String BRIDGE_SERIAL_NUMBER_FILE_NAME = "BSerialNumber.txt";
    private DataMatrix dataMatrix = new DataMatrix();
    private FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
    private SimpleMessageDialog importDialog;
    private Handler handler;
    private View view;

    public AssetManagement() {
        handler = new Handler();

    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        view = LayoutInflater.from(context).inflate(R.layout.asset_management, null);
        container.addView(view);
        return view;
    }


    @SuppressLint("SdCardPath")
    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();

        if (ProductConfig.isLightCommercial() && config.machineClassType != JHTCommonTypes.MachineClassificationType.Treadmill) {
            view.findViewById(R.id.eng_welcome_icon_position).setVisibility(View.GONE);
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_gen_console_serial_number);
            userTextInput.setInputProperties(userTextInput.getInputProperties()
                    .inputValue(DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING))
                    .minChars(ProductConfig.getConsoleType().getConsolePrefix().length()));
            userTextInput.setOnTextChanged(s -> DataMatrix.putString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, SerialNumberUtil.fixSNToBeAlphanumeric(s)));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_gen_frame_serial_number);
            userTextInput.setInputProperties(userTextInput.getInputProperties()
                    .inputValue(DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING))
                    .minChars(ProductConfig.getConsoleType().getConsolePrefix().length()));
            userTextInput.setOnTextChanged(s -> DataMatrix.putString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, SerialNumberUtil.fixSNToBeAlphanumeric(s)));
        }

        {
            UserTextInput bridgeSN = view.findViewById(R.id.eng_am_bridge_serial_number);
            if (BuildConfig.HAS_BRIDGE_SN) {
                if (bridgeSN != null) {
                    bridgeSN.setInputProperties(
                            bridgeSN.getInputProperties()
                                    .inputValue(DataMatrix.getString(SettingKeys.AssetManagement.DATA_BRIDGE_SERIAL_STRING))
                                    .minChars(1)
                                    .maxChars(40)
                    );
                    bridgeSN.setOnTextChanged(s -> DataMatrix.putString(SettingKeys.AssetManagement.DATA_BRIDGE_SERIAL_STRING, s));
                }
            } else {
                setVisible(bridgeSN, View.GONE);
            }
        }

        {
            TextView userTextInput = view.findViewById(R.id.machine_uuid);
            String uuid = DataMatrix.getString(DAPIKeys.DATA_DAPI_MACHINE_UUID_STRING);
            userTextInput.setText(DataMatrix.getString(DAPIKeys.DATA_DAPI_MACHINE_UUID_STRING));
            handler.post(new Runnable() {
                @Override
                public void run() {
                    userTextInput.setVisibility(uuid != null ? View.VISIBLE: View.GONE);
                }
            });
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_gen_club_id);
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT));
            userTextInput.setOnTextChanged(s -> {
                if (ProductConfig.isLightCommercial()) {
                    if (CommonRoutines.parseInt(s) > 0) {
                        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL, true);
                    } else {
                        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL, false);
                    }
                }
                DataMatrix.putInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT, CommonRoutines.parseInt(s));
            });
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_enable_am);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL, isChecked);
                checkLoginBoxes(view);
            });
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_enable_wt);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL, isChecked);
                checkLoginBoxes(view);
            });
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_allow_guest);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ALLOW_GUEST_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ALLOW_GUEST_BOOL, isChecked));
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_go_button_enabled);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_PHYSICAL_GO_BTN_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_PHYSICAL_GO_BTN_BOOL, isChecked));
        }


        {
            CheckBox checkBox = view.findViewById(R.id.eng_show_welcome_screen);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_SHOW_WELCOME_SCREEN_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_SHOW_WELCOME_SCREEN_BOOL, isChecked));
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_login_xid_enabled);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_LOGIN_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_LOGIN_BOOL, isChecked));
        }
        {
            CheckBox checkBox = view.findViewById(R.id.eng_register_xid_enabled);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_REGISTRATION_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XID_REGISTRATION_BOOL, isChecked));
        }
        {
            CheckBox checkBox = view.findViewById(R.id.eng_login_barcode_enabled);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_BARCODE_LOGIN_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_BARCODE_LOGIN_BOOL, isChecked));
        }
        {
            CheckBox checkBox = view.findViewById(R.id.eng_login_phone_number_enabled);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_PHONE_NUMBER_LOGIN_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_PHONE_NUMBER_LOGIN_BOOL, isChecked));
        }
        {
            CheckBox checkBox = view.findViewById(R.id.eng_login_email_enabled);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_EMAIL_LOGIN_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_EMAIL_LOGIN_BOOL, isChecked));
        }
        {
            CheckBox enableWalletLoginCheckBox = view.findViewById(R.id.eng_login_wallet_enabled);
            enableWalletLoginCheckBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_WALLET_LOGIN_BOOL));
            enableWalletLoginCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_WALLET_LOGIN_BOOL, isChecked));
        }
        {
            CheckBox enableQrLoginCheckBox = view.findViewById(R.id.eng_login_qr_enabled);
            enableQrLoginCheckBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_QR_LOGIN_BOOL));
            enableQrLoginCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_QR_LOGIN_BOOL, isChecked));
        }
        {
            linkUserTextInput(view.findViewById(R.id.eng_jump_device_redirect_app),
                    SettingKeys.AssetManagement.DATA_JUMP_DEVICE_REDIRECT_APP_STRING);
        }
        {
            CheckBox checkBox = view.findViewById(R.id.eng_login_fitlinxx_enabled);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_FITLINXX_LOGIN_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_FITLINXX_LOGIN_BOOL, isChecked));
        }
        {
            CheckBox checkBox = view.findViewById(R.id.eng_login_rfid_enabled);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_RFID_LOGIN_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_RFID_LOGIN_BOOL, isChecked));
        }

        {
            CheckBox checkBox = view.findViewById(R.id.concierge_enabled);
            if (checkBox != null) {
                checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_CONCIERGE_BOOL, false));
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_CONCIERGE_BOOL, isChecked);
                });
            }
        }

        {
            // App Store
            CheckBox checkBox = view.findViewById(R.id.appstore_enabled);
            if (checkBox != null) {
                if (ProductConfig.hasAppStore()) {
                    checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_APPSTORE_BOOL));
                    checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_APPSTORE_BOOL, isChecked);
                    });
                } else {
                    checkBox.setVisibility(View.GONE);
                }
            }
        }

        {
            ViewGroup layout = view.findViewById(R.id.xarefit_data_fields);
            boolean bIsXareFitEnabled = DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XAREFIT_LOGIN_BOOL);
            CheckBox checkBox = view.findViewById(R.id.eng_xarefit_enabled);
            checkBox.setChecked(bIsXareFitEnabled);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_XAREFIT_LOGIN_BOOL, isChecked);
                updateXareFitSettingsLayout(layout, isChecked);
            });
            updateXareFitSettingsLayout(layout, bIsXareFitEnabled);

            linkUserTextInput(view.findViewById(R.id.xarefit_site_id), SettingKeys.AssetManagement.DATA_XAREFIT_SITE_ID_STRING);
            linkUserTextInput(view.findViewById(R.id.xarefit_server_url), SettingKeys.AssetManagement.DATA_XAREFIT_SERVER_URL_STRING);
            linkUserTextInput(view.findViewById(R.id.xarefit_server_port), SettingKeys.AssetManagement.DATA_XAREFIT_SERVER_PORT_STRING);
        }


        { // safety pin feature (so little babies like me don't accidentally start a workout and jettison themselves off the treadmill)
            CheckBox checkBox = view.findViewById(R.id.eng_safety_pin_enabled);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL)); // false by default since it checks if it is retail and we are
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL, isChecked));

            UserTextInput safetyPinText = view.findViewById(R.id.eng_safety_pin);
            safetyPinText.setText(DataMatrix.getString(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_STRING)); // populate safety pin
            safetyPinText.setOnTextChanged(s-> {
                if (s.length() == 4) { // accept new pin
                    DataMatrix.putString(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_STRING, s);
                } else { // entered pin is not 4, so the text box lights up red
                    safetyPinText.setIsValid(false, "Enter 4 digits for pin"); // can't actually see the message, but text field lights up red
                }
            });
        }

        {
            OptionSelectorItem optionSelectorItem = view.findViewById(R.id.eng_primary_login_method);
            if (optionSelectorItem != null) {
                AssetManagementLogins assetManagementLogins = new AssetManagementLogins(getContext());
                String[] labels = assetManagementLogins.getLabels();
                int currentIndex = assetManagementLogins.getIndexFromType(DataMatrix.getString(SettingKeys.AssetManagement.DATA_PRIMARY_LOGIN_METHOD_STRING), 0);
                optionSelectorItem.setOptions(new OptionManager(labels, currentIndex));
                optionSelectorItem.setOnOptionSelected((pos, option) -> {
                    String dapiKeyword = assetManagementLogins.getTypeFromIndex(pos).getValue();
                    DataMatrix.putString(SettingKeys.AssetManagement.DATA_PRIMARY_LOGIN_METHOD_STRING, dapiKeyword);
                });
            }
        }

        {

            CheckBox checkBox = view.findViewById(R.id.eng_login_custom_enabled);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_CUSTOM_LOGIN_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_CUSTOM_LOGIN_BOOL, isChecked));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_custom_login_name);
            userTextInput.setText(DataMatrix.getString(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_NAME_STRING));
            userTextInput.setOnTextChanged(s -> DataMatrix.putString(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_NAME_STRING, s));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_custom_login_label);
            userTextInput.setText(DataMatrix.getString(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_LABEL_STRING));
            userTextInput.setOnTextChanged(s -> DataMatrix.putString(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_LABEL_STRING, s));
        }

        {
            OptionSelectorItem optionSelectorItem = view.findViewById(R.id.eng_custom_login_input_type);
            optionSelectorItem.setOptions(new OptionManager(getContext().getResources().getStringArray(R.array.custom_login_input_type), DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_INPUT_TYPE_INT)));
            optionSelectorItem.setOnOptionSelected((pos, option) -> DataMatrix.putInt(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_INPUT_TYPE_INT, pos));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_custom_password_label);
            userTextInput.setText(DataMatrix.getString(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_PASSWORD_LABEL_STRING));
            userTextInput.setOnTextChanged(s -> DataMatrix.putString(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_PASSWORD_LABEL_STRING, s));
        }

        {
            OptionSelectorItem optionSelectorItem = view.findViewById(R.id.eng_custom_password_input_type);
            optionSelectorItem.setOptions(new OptionManager(getContext().getResources().getStringArray(R.array.custom_login_input_type), DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_PASSWORD_INPUT_TYPE_INT)));
            optionSelectorItem.setOnOptionSelected((pos, option) -> DataMatrix.putInt(SettingKeys.AssetManagement.DATA_CUSTOM_LOGIN_PASSWORD_INPUT_TYPE_INT, pos));
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_show_custom_logo);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_LOGO_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_LOGO_BOOL, isChecked));
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_show_custom_welcome_background);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_WELCOME_SCREEN_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_WELCOME_SCREEN_BOOL, isChecked));
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_show_custom_color_background);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_BACKGROUND_COLOR_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_BACKGROUND_COLOR_BOOL, isChecked));
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_show_custom_workout_background);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_BACKGROUND_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_BACKGROUND_BOOL, isChecked));
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_club_com);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_CLUB_COM_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_ENABLE_CLUB_COM_BOOL, isChecked);
                updateClubComSettings();
            });
            updateClubComSettings();
        }

        {
            CheckBox switchStrideOnlyCheckBox  = view.findViewById(R.id.show_switch_stride_only);
            switchStrideOnlyCheckBox .setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_SHOW_SWITCH_STRIDE_MODE_ONLY_BOOL));
            switchStrideOnlyCheckBox .setOnCheckedChangeListener((buttonView, isChecked) -> {
                DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_SHOW_SWITCH_STRIDE_MODE_ONLY_BOOL, isChecked);
            });
            if (!frameConfig.TM_SUPPORTS_REVERSE_BELT) {
                switchStrideOnlyCheckBox .setVisibility(View.GONE);
            }
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_welcome_screen_fade);
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_AUTO_HIDE_TIMEOUT_SEC_INT));
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_AUTO_HIDE_TIMEOUT_SEC_INT, CommonRoutines.parseInt(s)));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_welcome_screen_slide_show);
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_SLIDESHOW_SEC_INT));
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_SLIDESHOW_SEC_INT, CommonRoutines.parseInt(s)));
        }

        {
            OptionSelectorItem optionSelectorItem = view.findViewById(R.id.eng_welcome_icon_position);
            optionSelectorItem.setOptions(new OptionManager(getContext().getResources().getStringArray(R.array.welcome_icon_positions), DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_ICON_POSITION_ENUM_INT)));
            optionSelectorItem.setOnOptionSelected((pos, option) -> DataMatrix.putInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_ICON_POSITION_ENUM_INT, pos));
        }

        {
            OptionSelectorItem optionSelectorItem = view.findViewById(R.id.eng_welcome_screen_time_until_dim);
            optionSelectorItem.setOptions(new OptionManager(getContext().getResources().getStringArray(R.array.screen_brightness_timeout), DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_BRIGHTNESS_TIMEOUT_ENUM_INT)));
            optionSelectorItem.setOnOptionSelected((pos, option) -> DataMatrix.putInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_BRIGHTNESS_TIMEOUT_ENUM_INT, pos));
        }

        {
            UserTextInput inputCustomAppName = view.findViewById(R.id.eng_custom_app_name);
            inputCustomAppName.setText(DataMatrix.getString(SettingKeys.AssetManagement.DATA_CUSTOM_WEB_APP_NAME_STRING));
            inputCustomAppName.setOnTextChanged(s -> DataMatrix.putString(SettingKeys.AssetManagement.DATA_CUSTOM_WEB_APP_NAME_STRING, s));
        }

        {
            UserTextInput inputCustomAppURL = view.findViewById(R.id.eng_custom_app_url);
            inputCustomAppURL.setText(DataMatrix.getString(SettingKeys.AssetManagement.DATA_CUSTOM_WEB_APP_URL_STRING));
            inputCustomAppURL.setOnTextChanged(s -> DataMatrix.putString(SettingKeys.AssetManagement.DATA_CUSTOM_WEB_APP_URL_STRING, s));
        }

        {

            int progress = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_BRIGHTNESS_PERCENT_AFTER_TIMEOUT_INT);
            ((SeekBar) view.findViewById(R.id.asset_management_backlight)).setProgress(progress);
            TextView backlightThumbText = (TextView) findViewById(R.id.am_thumb_backlight_text);
            backlightThumbText.setText(DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_BRIGHTNESS_PERCENT_AFTER_TIMEOUT_INT) + "");
            ((SeekBar) view.findViewById(R.id.asset_management_backlight)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                    DataMatrix.putInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_BRIGHTNESS_PERCENT_AFTER_TIMEOUT_INT, progress);
                    TextView label = (TextView) findViewById(R.id.am_thumb_backlight_text);
                    label.setText(progress + "");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    DataMatrix.putInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_BRIGHTNESS_PERCENT_AFTER_TIMEOUT_INT, seekBar.getProgress());
                }
            });
        }

        view.findViewById(R.id.eng_upload_theme).setOnClickListener(v -> {
            importDialog = DialogFactory.message(v, getContext().getResources().getString(R.string.upload_custom_theme_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.NONE);
            new Thread(() -> {
                File file = new File(StorageUtils.getUSBPath(), "generations_theme.zip");
                if (file.exists()) {

                    ZipFile zipFile;
                    try {
                        zipFile = new ZipFile(file);
                        @SuppressLint("SdCardPath") File destination = new File("/sdcard/Download/chimera_theme");
                        FileUtils.delete(destination);
                        //noinspection ResultOfMethodCallIgnored
                        destination.mkdirs();
                        //noinspection HardCodedStringLiteral
                        zipFile.extractAll("/sdcard/Download/chimera_theme");

                        ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
                        executeAsRoot.addCommand("rm -rf /data/data/com.jht.chimera/files/settings/theme/*");
                        executeAsRoot.addCommand("mkdir -p /data/data/com.jht.chimera/files/settings/theme");
                        executeAsRoot.addCommand("cp -rf " + destination.getAbsolutePath() + "/* /data/data/com.jht.chimera/files/settings/theme");
                        executeAsRoot.execute();

                    } catch (ZipException e) {
                        Log.e(TAG, "ZipException in upload theme through usb", e);
                    }

                    handler.post(() -> importDialog.init(getContext().getResources().getString(R.string.file_upload_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK));

                } else if (new File(StorageUtils.getUSBPath(), "generations_theme").exists()) {
                    Log.e(TAG, "generations theme exists");
                    File src = new File(StorageUtils.getUSBPath(), "generations_theme");
                    ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
                    executeAsRoot.addCommand("rm -rf /data/data/com.jht.chimera/files/settings/theme/*");
                    executeAsRoot.addCommand("mkdir -p /data/data/com.jht.chimera/files/settings/theme");
                    executeAsRoot.addCommand("cp -rf " + src.getAbsolutePath() + "/* /data/data/com.jht.chimera/files/settings/theme");
                    executeAsRoot.addCommand("chmod -R 777 " + getContext().getFilesDir() + "/settings");
                    executeAsRoot.execute();
                    ExecuteAsRoot.fixFilesInPackage("com.jht.chimera");
                    handler.post(() -> importDialog.init(getContext().getResources().getString(R.string.file_upload_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK));

                } else {
                    handler.post(() -> importDialog.init(getContext().getResources().getString(R.string.theme_upload_failed_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK));
                }
            }).start();

        });

        //This is to setup and upload a bitmap for a custom web app icon
        view.findViewById(R.id.eng_upload_custom_app_icon).setOnClickListener(v -> {
            importDialog = DialogFactory.message(v, getContext().getResources().getString(R.string.upload_custom_app_icon_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.NONE);
            new Thread(() -> {
                File path = new File(StorageUtils.getUSBPath(), "custom_app");
                if(path.exists()) {
                    File[] imageFiles = path.listFiles((dir, name) -> {
                        String[] extensions = new String[]{"jpg", "png", "gif", "jpeg", "webp"};
                        for (String extension : extensions) {
                            if (name.endsWith(extension)) {
                                return true;
                            }
                        }
                        return false;
                    });
                    if (imageFiles != null && imageFiles.length > 0) {
                        try {
                            Bitmap customBitmap = setupTheBitmap(imageFiles[0]);

                            File customAppPath = new File(getContext().getFilesDir(), "/settings/apps/bitmaps");
                            //noinspection ResultOfMethodCallIgnored
                            customAppPath.mkdirs();
                            try (FileOutputStream storeBitmap = new FileOutputStream(new File(customAppPath, "custom_app.png"))) {
                                customBitmap.compress(Bitmap.CompressFormat.PNG, 100, storeBitmap);
                                handler.post(() -> importDialog.init(getContext().getResources().getString(R.string.file_upload_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK));
                            } catch (IOException ignored) {
                            }
                            return;
                        }
                        catch(Exception ignored) {
                        }
                    }
                }
                handler.post(() -> importDialog.init(getContext().getResources().getString(R.string.custom_app_icon_upload_failed_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK));

            }).start();
        });

        view.findViewById(R.id.eng_upload_serial_numbers).setOnClickListener(v -> {
            String consoleSerialNumberImport = SerialNumberUtil.readSerialNumberFromFile(new File(StorageUtils.getUSBPath(), "CSerialNumber.txt")); //NON-NLS
            consoleSerialNumberImport = consoleSerialNumberImport.toUpperCase();
            if (!consoleSerialNumberImport.equals("")) {
                ((UserTextInput) view.findViewById(R.id.eng_gen_console_serial_number)).setText(consoleSerialNumberImport);
                DataMatrix.putString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, consoleSerialNumberImport);
            }

            String frameSerialNumberImport = SerialNumberUtil.readSerialNumberFromFile(new File(StorageUtils.getUSBPath(), "FSerialNumber.txt")); //NON-NLS
            frameSerialNumberImport = frameSerialNumberImport.toUpperCase();
            if (!frameSerialNumberImport.equals("")) {
                ((UserTextInput) view.findViewById(R.id.eng_gen_frame_serial_number)).setText(frameSerialNumberImport);
                DataMatrix.putString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, frameSerialNumberImport);
            }

            UserTextInput bridgeSNInput = view.findViewById(R.id.eng_am_bridge_serial_number);
            if (BuildConfig.HAS_BRIDGE_SN) {
                String bridgeSerialNumberImport = SerialNumberUtil.readSerialNumberFromFile(new File(StorageUtils.getUSBPath(), BRIDGE_SERIAL_NUMBER_FILE_NAME));
                bridgeSerialNumberImport = bridgeSerialNumberImport.toUpperCase();
                if (!bridgeSerialNumberImport.isEmpty()) {
                    if (bridgeSNInput != null) {
                        bridgeSNInput.setText(bridgeSerialNumberImport);
                        DataMatrix.putString(SettingKeys.AssetManagement.DATA_BRIDGE_SERIAL_STRING, bridgeSerialNumberImport);
                    }
                }
            } else {
                setVisible(bridgeSNInput, View.GONE);
            }
        });

        checkLoginBoxes(view);

        {
            CheckBox checkbox = findViewById(R.id.enable_egym);
            checkbox.setChecked(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_EGYM_ENABLED_BOOL));
            checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                DataMatrix.putBoolean(SettingKeys.Experimental.DATA_EGYM_ENABLED_BOOL, isChecked);
            });
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_custom_login_rfid_only_enabled);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_CUSTOM_RFID_ONLY_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_CUSTOM_RFID_ONLY_BOOL, isChecked));
        }

        {
            OptionSelectorItem timezone = view.findViewById(R.id.egym_environment);
            int optionId = 0;
            String[] options = getContext().getResources().getStringArray(R.array.egym_environments);
            String currentOption = DataMatrix.getString(SettingKeys.Experimental.DATA_EGYM_ENVIRONMENT_2_STRING);
            for(int i = 0; i < options.length; i++) {
                if(options[i].equalsIgnoreCase(currentOption)) {
                    optionId = i;
                    break;
                }
            }
            timezone.setOptions(R.array.egym_environments, optionId);
            timezone.setOnOptionSelected((pos, option) -> {
                DataMatrix.putString(SettingKeys.Experimental.DATA_EGYM_ENVIRONMENT_2_STRING, option);
                updateEGymOptions();
            });
        }

        {
            UserTextInput textInput = view.findViewById(R.id.egym_update_time);
            int time = DataMatrix.getInt(SettingKeys.Experimental.DATA_EGYM_REFRESH_HOUR_INT);
            String timeString = time + ":00";
            Calendar updateTime = Calendar.getInstance();
            updateTime.set(Calendar.HOUR_OF_DAY, time);
            updateTime.set(Calendar.MINUTE, 0);
            updateTime.set(Calendar.SECOND, 0);
            textInput.setText(updateTime);
            textInput.setText(timeString);
            textInput.setOnTimePicked((view, hourOfDay, minute) -> {
                DataMatrix.putInt(SettingKeys.Experimental.DATA_EGYM_REFRESH_HOUR_INT, hourOfDay);
                String timeString2 = hourOfDay + ":00";
                textInput.setText(timeString2);
            });
        }

        updateEGymOptions();

    }

    //The helper method is to setup the image that will upload for custom the web app
    private  Bitmap setupTheBitmap(File firstImage) {
        // Get the dimensions of the image.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(firstImage.getPath(), options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;

        // Find the appropriate scale for generating the bitmap.
        options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (imageWidth > 512 || imageHeight > 512){
            options.inSampleSize++;
            imageWidth /= 2;
            imageHeight /= 2;
        }
        return BitmapFactory.decodeFile(firstImage.getPath(), options);
    }

    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        dataMatrix.finish();
    }

    private void checkLoginBoxes(View view) {
        boolean enabled = DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL) || DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL);

        view.findViewById(R.id.eng_login_xid_enabled).setEnabled(enabled);
        view.findViewById(R.id.eng_login_rfid_enabled).setEnabled(enabled);
        view.findViewById(R.id.eng_login_barcode_enabled).setEnabled(enabled);
        view.findViewById(R.id.eng_login_phone_number_enabled).setEnabled(enabled);
        view.findViewById(R.id.eng_login_email_enabled).setEnabled(enabled);
        view.findViewById(R.id.eng_login_fitlinxx_enabled).setEnabled(enabled);
        view.findViewById(R.id.eng_login_qr_enabled).setEnabled(enabled);
        view.findViewById(R.id.eng_login_custom_enabled).setEnabled(enabled);
        view.findViewById(R.id.eng_custom_login_name).setEnabled(enabled);
        view.findViewById(R.id.eng_custom_login_label).setEnabled(enabled);
        ((OptionSelectorItem) view.findViewById(R.id.eng_custom_login_input_type)).setReadOnly(!enabled);
        view.findViewById(R.id.eng_custom_password_label).setEnabled(enabled);
        ((OptionSelectorItem) view.findViewById(R.id.eng_custom_password_input_type)).setReadOnly(!enabled);

    }

    private void updateClubComSettings() {
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_CLUB_COM_BOOL)) {
            findViewById(R.id.eng_show_custom_welcome_background).setVisibility(View.GONE);
            findViewById(R.id.eng_show_custom_color_background).setVisibility(View.GONE);
            findViewById(R.id.eng_welcome_screen_slide_show).setVisibility(View.GONE);
            findViewById(R.id.eng_welcome_screen_fade).setVisibility(View.GONE);
        } else {
            findViewById(R.id.eng_show_custom_welcome_background).setVisibility(View.VISIBLE);
            findViewById(R.id.eng_show_custom_color_background).setVisibility(View.VISIBLE);
            findViewById(R.id.eng_welcome_screen_slide_show).setVisibility(View.VISIBLE);
            findViewById(R.id.eng_welcome_screen_fade).setVisibility(View.VISIBLE);
        }
    }

    private void updateEGymOptions() {
        UserTextInput userTextInput = view.findViewById(R.id.egym_website);
        SettingKeys.Experimental websiteKey;
        String currentEnvironment = DataMatrix.getString(SettingKeys.Experimental.DATA_EGYM_ENVIRONMENT_2_STRING);
        if(currentEnvironment.equalsIgnoreCase("development")) {
            websiteKey = SettingKeys.Experimental.DATA_EGYM_DEV_WEBSITE_2_STRING;
        } else if(currentEnvironment.equalsIgnoreCase("production")) {
            websiteKey = SettingKeys.Experimental.DATA_EGYM_PROD_WEBSITE_2_STRING;
        } else {
            websiteKey = SettingKeys.Experimental.DATA_EGYM_TESTING_WEBSITE_2_STRING;
        }

        if (userTextInput != null) {
            userTextInput.setText("" + DataMatrix.getString(websiteKey));
            userTextInput.setOnTextChanged(s -> {
                DataMatrix.putString(websiteKey, s);
            });
            view.findViewById(R.id.reset_egym_website).setOnClickListener(view -> {
                DataMatrix.putString(websiteKey, websiteKey.DataDefault.valueAsString());
                userTextInput.setText(websiteKey.DataDefault.valueAsString());
            });
        }
    }

    private void updateXareFitSettingsLayout(ViewGroup layout, boolean isChecked) {
        if (isChecked == true) {
            layout.setVisibility(View.VISIBLE);
        } else {
            layout.setVisibility(View.GONE);
        }
    }


}
