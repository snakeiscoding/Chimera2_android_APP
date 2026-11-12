package com.jht.chimera.ui.mainapp.goscreen;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.jht.androidcommonalgorithms.util.SystemDetection;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.ui.DigitalSafetyPinKeypad;
import com.jht.datamatrixlib.DataMatrix;

import java.io.File;

public class SecurityPinSetupScreen extends Dialog {

    public ImageView securityPinSetupBack;
    public Button setupNow;
    public Button accept;
    public Button maybeLater;
    public Button setup;
    public Activity activity;
    public WindowManager windowManager;
    private final EditText[] securityPinInput = new EditText[4];
    private DigitalSafetyPinKeypad keypad;

    private final View.OnClickListener numberButtonOnClickListener = v -> {
        Button button = (Button) v;
        String text = button.getText().toString();
        if (securityPinInput[0].getText().toString().equals("")) {
            securityPinInput[0].setText(text);
        } else if (securityPinInput[1].getText().toString().equals("")) {
            securityPinInput[1].setText(text);
        } else if (securityPinInput[2].getText().toString().equals("")) {
            securityPinInput[2].setText(text);
        } else if (securityPinInput[3].getText().toString().equals("")) {
            securityPinInput[3].setText(text);
        }
    };

    public SecurityPinSetupScreen(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setType(ChimeraEnums.TYPE_SYSTEM_ALERT);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        securityPinStartDialog();
        setCanceledOnTouchOutside(false);
    }

    private void securityPinStartDialog() {
        setContentView(R.layout.parker_security_pin_start_dialog);
        maybeLater = findViewById(R.id.bt_maybe_later);
        setup = findViewById(R.id.bt_setup);
        maybeLater.setOnClickListener(v -> securityPinAlertDialog());
        setup.setOnClickListener(v -> securityPinSetupDialog());
    }

    private void securityPinAlertDialog() {
        setContentView(R.layout.parker_security_pin_alert_dialog);
        setupNow = findViewById(R.id.bt_setup_now);
        accept = findViewById(R.id.bt_accept);
        ImageView alertIcon = findViewById(R.id.ic_security_pin_alert);
        alertIcon.setOnClickListener(v -> securityPinStartDialog());
        setupNow.setOnClickListener(v -> securityPinSetupDialog());
        accept.setOnClickListener(v -> {
            DataMatrix.putBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL, false);
            dismissDialog();
            blockPinSetupDialog();
        });
    }

    @SuppressLint("MissingInflatedId")
    private void securityPinSetupDialog() {
        setContentView(R.layout.parker_security_pin_setup_dialog);
        securityPinSetupBack = findViewById(R.id.ic_security_pin_back_up);
        securityPinSetupBack.setOnClickListener(v -> securityPinStartDialog());

        securityPinInput[0] = findViewById(R.id.pinInput1);
        securityPinInput[1] = findViewById(R.id.pinInput2);
        securityPinInput[2] = findViewById(R.id.pinInput3);
        securityPinInput[3] = findViewById(R.id.pinInput4);

        securityPinInput[0].setInputType(InputType.TYPE_NULL);
        securityPinInput[1].setInputType(InputType.TYPE_NULL);
        securityPinInput[2].setInputType(InputType.TYPE_NULL);
        securityPinInput[3].setInputType(InputType.TYPE_NULL);

        keypad = findViewById(R.id.pin_keypad);

        if (keypad != null) {
            if (keypad.button_confirm != null) {
                keypad.button_confirm.setOnClickListener(v -> {
                        if (getCode().length()==4) {
                            DataMatrix.putBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL, true);
                            setPin(getCode());
                            dismissDialog();
                            blockPinSetupDialog();
                        } else {
                            keypad.button_confirm = null;
                        }
                    }
                );
            }
        }

        if (keypad.button_0 != null) {
            keypad.button_0.setOnClickListener(numberButtonOnClickListener);
        }
        if (keypad.button_1 != null) {
            keypad.button_1.setOnClickListener(numberButtonOnClickListener);
        }
        if (keypad.button_2 != null) {
            keypad.button_2.setOnClickListener(numberButtonOnClickListener);
        }
        if (keypad.button_3 != null) {
            keypad.button_3.setOnClickListener(numberButtonOnClickListener);
        }
        if (keypad.button_4 != null) {
            keypad.button_4.setOnClickListener(numberButtonOnClickListener);
        }
        if (keypad.button_5 != null) {
            keypad.button_5.setOnClickListener(numberButtonOnClickListener);
        }
        if (keypad.button_6 != null) {
            keypad.button_6.setOnClickListener(numberButtonOnClickListener);
        }
        if (keypad.button_7 != null) {
            keypad.button_7.setOnClickListener(numberButtonOnClickListener);
        }
        if (keypad.button_8 != null) {
            keypad.button_8.setOnClickListener(numberButtonOnClickListener);
        }
        if (keypad.button_9 != null) {
            keypad.button_9.setOnClickListener(numberButtonOnClickListener);
        }

        if (keypad.button_back != null) {
            keypad.button_back.setOnClickListener(v -> {
                int inputLength = getCode().length();
                if (inputLength == 4) {
                    securityPinInput[3].setText("");
                } else if (inputLength == 3) {
                    securityPinInput[2].setText("");
                } else if (inputLength == 2) {
                    securityPinInput[1].setText("");
                } else if (inputLength == 1) {
                    securityPinInput[0].setText("");
                }
            });
        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            setPin(getCode());
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private String getEditTextString(EditText editText) {
        return editText.getText().toString().trim();
    }

    private String getCode() {
        StringBuilder code = new StringBuilder();
        for (EditText et : securityPinInput) {
            code.append(getEditTextString(et));
        }
        return code.toString();
    }

    private void setPin(String pin) {
        DataMatrix.putString(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_STRING, pin);
    }

    public void showSecurityPinSetupDialog() {
        if (this.isShowing()) return;
        this.show();
    }

    public void dismissDialog() {
        dismiss();
    }

    public static void blockPinSetupDialog() {
        if (ProductConfig.isParker()) {
            if (!shouldShowPinSetupDialog()) {
                ExecuteAsRoot root = new ExecuteAsRoot();
                if (SystemDetection.isEmulator()) root.addCommand("mkdir " + ProductConfig.getMarkerFilePath());
                root.addCommand("touch " + ProductConfig.getMarkerFilePath("pin_setup.txt"));
                root.addCommand("sync");
                root.execute();
            }
        }
    }

    public static boolean shouldShowPinSetupDialog() {
        if (ProductConfig.isParker()) {
            File pinMarker = new File(ProductConfig.getMarkerFilePath("pin_setup.txt"));
            return pinMarker.exists();
        } else {
            return false;
        }
    }
}
