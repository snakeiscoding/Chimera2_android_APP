package com.jht.chimera.lib.tv.tuner.lg.lg750e;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;

import androidx.annotation.NonNull;

import android.os.Handler;
import android.os.Looper;
import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.dialog.SimpleYesNoMessageDialog;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ChimeraEnums;

import org.jetbrains.annotations.NonNls;


public class Lg750ETunerSettings extends Dialog {
    @NonNls
    private final static String TAG = "Lg750ETunerSettings";
    private final static String INVALID_IP = "0.0.0.0";

    private final Context context;
    private final Lg750ETunerBase tuner;

    private CheckBox dhcp;
    private UserTextInput ipAddress;
    private UserTextInput subnetMask;
    private UserTextInput gateway;
    private UserTextInput dns1;
    private UserTextInput dns2;
    private UserTextInput version;
    private UserTextInput macAddress;

    public Lg750ETunerSettings(Context context, @NonNull Lg750ETunerBase tuner) {
        super(context);
        this.context = context;
        this.tuner = tuner;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setType(ChimeraEnums.TYPE_SYSTEM_ALERT);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        setCanceledOnTouchOutside(false);
        setContentView(R.layout.lg_stb_settings);

        // update the dialog size since it doesn't appear to resize correctly
        WindowManager window = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = window.getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        int widthPixels = size.x;
        int heightPixels = size.y;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.width = (int) (widthPixels * .60);
        lp.height = (int) (heightPixels * .80);
        getWindow().setAttributes(lp);

        dhcp = findViewById(R.id.network_ip_settings_dialog_dhcp);
        ipAddress = findViewById(R.id.network_ip_settings_dialog_ip_address);
        subnetMask = findViewById(R.id.network_ip_settings_dialog_subnet_mask);
        gateway = findViewById(R.id.network_ip_settings_dialog_ip_gateway);
        dns1 = findViewById(R.id.network_ip_settings_dns1);
        dns2 = findViewById(R.id.network_ip_settings_dns2);
        version = findViewById(R.id.version);
        macAddress = findViewById(R.id.mac_address);

        dhcp.setChecked(true);
        ipAddress.setReadOnly(true);
        subnetMask.setReadOnly(true);
        dns1.setReadOnly(true);
        dns2.setReadOnly(true);
        gateway.setReadOnly(true);
        version.setReadOnly(true);
        macAddress.setReadOnly(true);
    }

    public void refresh(@NonNull Lg750EResponse settings) {
        Log.d(TAG, "settings: " + settings);
        version.setText(settings.version);
        macAddress.setText(settings.mac);

        if (settings.dhcp.equalsIgnoreCase("false") && settings.ip.equalsIgnoreCase(INVALID_IP)) {
            // don't do anything if no connection detected
        } else {
            if (settings.dhcp.equalsIgnoreCase("true")) {
                dhcp.setChecked(true);
                ipAddress.setReadOnly(true);
                subnetMask.setReadOnly(true);
                dns1.setReadOnly(true);
                dns2.setReadOnly(true);
                gateway.setReadOnly(true);
            } else {
                dhcp.setChecked(false);
                ipAddress.setReadOnly(false);
                subnetMask.setReadOnly(false);
                dns1.setReadOnly(false);
                dns2.setReadOnly(false);
                gateway.setReadOnly(false);
            }
            ipAddress.setText(settings.ip);
            subnetMask.setText(settings.subnet);
            dns1.setText(settings.dns1);
            dns2.setText(settings.dns2);
            gateway.setText(settings.gateway);
        }

        dhcp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ipAddress.setReadOnly(isChecked);
            subnetMask.setReadOnly(isChecked);
            dns1.setReadOnly(isChecked);
            dns2.setReadOnly(isChecked);
            gateway.setReadOnly(isChecked);
        });

        findViewById(R.id.network_ip_settings_save).setOnClickListener(v -> {
            hide();
            if (dhcp.isChecked()) {
                tuner.enableDhcp();
            } else {
                tuner.setStaticNetwork(ipAddress.getText(), subnetMask.getText(), gateway.getText(), dns1.getText(), dns2.getText());
            }
        });

        findViewById(R.id.network_ip_settings_cancel).setOnClickListener(v -> hide());
        findViewById(R.id.reboot).setOnClickListener(v -> {
            tuner.reboot();
            hide();
        });
        findViewById(R.id.factory_reset).setVisibility(View.VISIBLE);
        findViewById(R.id.factory_reset).setOnClickListener(v -> {
            DialogFactory.confirm(v, context.getString(R.string.factory_reset_stb_confirm_pg), () -> {
                v.setEnabled(false);
                tuner.factoryResetTunerWithResult(success -> {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (success) {
                            hide();
                        } else {
                            DialogFactory.message(v, context.getString(R.string.factory_reset_stb_failed_pg), this::hide, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                        }
                    });
                });
            }, null);
        });
    }

}
