package com.jht.chimera.lib.tv.tuner.lg.lg6500;

import static com.jht.chimera.lib.tv.tuner.lg.lg6500.LG6500TunerBase.SLEEP_TIME;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;

import androidx.annotation.NonNull;

import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.serialport.serialcommunication.StringPacket;
import com.jht.serialport.serialcommunication.WriteManager;

import org.jetbrains.annotations.NonNls;


public class LG6500TunerSettings extends Dialog {
    @NonNls
    private final String TAG = "LG6500TunerSettings";
    private final String INVALID_IP = "0.0.0.0";

    private Context context;
    private LG6500Response settings;
    private WriteManager writeManager;

    private CheckBox dhcp;
    private UserTextInput ipAddress;
    private UserTextInput subnetMask;
    private UserTextInput gateway;
    private UserTextInput dns1;
    private UserTextInput dns2;
    private UserTextInput version;
    private UserTextInput macAddress;

    public LG6500TunerSettings(Context context, @NonNull LG6500Response settings, @NonNull WriteManager writeManager) {
        super(context);
        this.context = context;
        this.settings = settings;
        this.writeManager = writeManager;
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

        // delay for some time so STB can respond to messages
        new Handler().postDelayed(() -> {

            Log.d(TAG, "settings: " + settings.toString());
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
                    writeManager.sendMessage(new StringPacket("26:01\n")); // enable dhcp
                    SystemClock.sleep(SLEEP_TIME); // sleep so messages in LG web app are not concatenated
                } else {
                    writeManager.sendMessage(new StringPacket(String.format("21:%s\n", ipAddress.getText()))); // ip
                    SystemClock.sleep(SLEEP_TIME); // sleep so messages in LG web app are not concatenated
                    writeManager.sendMessage(new StringPacket(String.format("22:%s\n", subnetMask.getText()))); // subnet mask
                    SystemClock.sleep(SLEEP_TIME); // sleep so messages in LG web app are not concatenated
                    writeManager.sendMessage(new StringPacket(String.format("23:%s\n", gateway.getText()))); // gateway
                    SystemClock.sleep(SLEEP_TIME); // sleep so messages in LG web app are not concatenated
                    writeManager.sendMessage(new StringPacket(String.format("24:%s\n", dns1.getText()))); // dns1
                    SystemClock.sleep(SLEEP_TIME); // sleep so messages in LG web app are not concatenated
                    writeManager.sendMessage(new StringPacket(String.format("25:%s\n", dns2.getText()))); // dns2
                    SystemClock.sleep(SLEEP_TIME); // sleep so messages in LG web app are not concatenated
                    writeManager.sendMessage(new StringPacket("26:00\n")); // disable dhcp (use static ip)
                    SystemClock.sleep(SLEEP_TIME); // sleep so messages in LG web app are not concatenated
                }
            });

            findViewById(R.id.network_ip_settings_cancel).setOnClickListener(v -> hide());
            findViewById(R.id.reboot).setOnClickListener(v -> {
                // send reboot command
                writeManager.sendMessage(new StringPacket("00\n"));
                SystemClock.sleep(SLEEP_TIME); // sleep so messages in LG web app are not concatenated
                hide();
            });
        }, 2000);

    }

}
