package com.jht.chimera.lib.widget.dialog;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.chimera.R;
import com.jht.chimera.lib.widget.network.SSID;

/**
 * This class manages an editable field that the requires a keyboard or keypad.  We extend the
 * base Android EditText so that we can get labels and add units in the field.  The floating hint
 * is nice in the support library, but we cannot style the label as we would like.  This class
 * also takes care of validating the input using XML attributes for validation and can link to a field
 * in the model layer.
 *
 *
 */
public class NetworkPasswordEntry extends DialogBase {

    public interface IOnEntryComplete {
        void onCancel();
        void onEntry(PasswordEntryMode mode, SSID ssid, String ssidText, int security, String password);
    }

    public enum PasswordEntryMode {
        WEP,
        WPA,
        HIDDEN,

    }

    private SSID ssidInfo;
    private UserTextInput ssid;
    private OptionSelectorItem securityType;
    private UserTextInput password;
    private CheckBox showPassword;
    private IOnEntryComplete onEntryComplete;
    private PasswordEntryMode entryMode = PasswordEntryMode.WPA;

    /**
     * Initialize the option selector item.
     *
     * @param context   Current context for the view.
     */
    public NetworkPasswordEntry(Context context) {
        this(context, null, 0);
    }

    /**
     * Initialize the option selector item.
     *
     * @param context   Current context for the view.
     * @param attrs     Attributes that are set from the view's XML.
     */
    public NetworkPasswordEntry(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Initialize the option selector item.
     *
     * @param context   Current context for the view.
     * @param attrs     Attributes that are set from the view's XML.
     * @param defStyleAttr  Default style for the view.
     */
    public NetworkPasswordEntry(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);


        // Cache the text view.
        ssid = findViewById(R.id.network_dialog_ssid);
        securityType = findViewById(R.id.network_dialog_security);
        password = findViewById(R.id.network_dialog_password);
        showPassword = findViewById(R.id.network_dialog_show_password);
        showPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) {
                password.showPassword();
            } else {
                password.hidePassword();
            }
        });
        findViewById(R.id.network_dialog_connect).setOnClickListener(v -> {
            if(onEntryComplete != null) {
                onEntryComplete.onEntry(entryMode, ssidInfo, ssid.getText(), securityType.getSelectedPosition(), password.getText());
            }
            hide();

        });
        findViewById(R.id.network_dialog_cancel).setOnClickListener(v -> {
            if(onEntryComplete != null) {
                onEntryComplete.onCancel();
            }
            hide();
        });

        securityType.setOnOptionSelected((pos, option) -> {
            if(pos == 0) {
                password.setVisibility(View.GONE);
                showPassword.setVisibility(View.GONE);
            }
            else {
                password.setVisibility(View.VISIBLE);
                showPassword.setVisibility(View.VISIBLE);
                if(pos == 1) {
                    password.setInputProperties(password.getInputProperties().maxChars(13));
                } else {
                    password.setInputProperties(password.getInputProperties().maxChars(63));
                }

            }
        });

    }

    @Override
    protected int getLayoutResId() { return R.layout.network_password_entry; }

    public void init(SSID ssid, PasswordEntryMode entryMode, IOnEntryComplete onEntryComplete) {
        ssidInfo = ssid;
        this.onEntryComplete = onEntryComplete;
        this.password.setText("");
        this.showPassword.setChecked(false);
        this.password.hidePassword();
        this.securityType.selectItem(0);
        this.ssid.setText("");
        this.entryMode = entryMode;

        if(entryMode == PasswordEntryMode.HIDDEN) {
            this.ssid.setText("");
            this.ssid.setReadOnly(false);
            securityType.setVisibility(View.VISIBLE);
            password.setVisibility(View.GONE);
        } else {
            this.ssid.setText(ssidInfo.scanResult.SSID);
            this.ssid.setReadOnly(true);
            securityType.setVisibility(View.GONE);
            password.setVisibility(View.VISIBLE);
        }
        switch(entryMode) {
            case WEP:
                password.setInputProperties(password.getInputProperties().maxChars(13));
                break;
            case WPA:
                password.setInputProperties(password.getInputProperties().maxChars(63));
                break;
            case HIDDEN:
                break;
        }
    }


}
