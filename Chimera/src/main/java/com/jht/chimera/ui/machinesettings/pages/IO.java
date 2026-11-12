package com.jht.chimera.ui.machinesettings.pages;

import android.animation.Animator;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.tv.TVControllerBase;
import com.jht.chimera.lib.tv.TVControllerFactory;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;

import java.util.Locale;

public class IO extends SettingPageBase {
    private static final String TAG = "IO";
    private View view;
    private DataMatrix dataMatrix = new DataMatrix();
    private TVControllerBase cabTestController;

    private int commandIndex = 0;

    private JHTTimer cabTestTimeoutTimer;
    private Handler mHandler = new Handler();

    private Button power_monitor;
    private UserTextInput cabTestResult;
    private UserTextInput fan_level_text;
    private UserTextInput fan_rpm_text;
    private TextView console_power;
    private TextView extend_power;
    private TextView usb_power;
    private UserTextInput watchDog_reboot_counter;
    private CheckBox extend_power_switch;

    private PeriodicTimer powerMonitorTimer = new PeriodicTimer(0,500, () -> {
        powerMonitor();
    });

    // TODO Most of this was written for Chimera 3 and may not work or apply to all consoles.
    @Override
    protected View onCreate(Context context, ViewGroup container) {
        view = LayoutInflater.from(context).inflate(R.layout.io, null);
        container.addView(view);
        power_monitor = view.findViewById(R.id.eng_io_power_monitor);
        fan_level_text = view.findViewById(R.id.eng_io_fan_level);
        fan_rpm_text = view.findViewById(R.id.eng_io_fan_rpm);
        console_power = view.findViewById(R.id.eng_io_console_power);
        extend_power = view.findViewById(R.id.eng_io_extend_power);
        usb_power = view.findViewById(R.id.eng_io_usb_power);
        watchDog_reboot_counter = view.findViewById(R.id.eng_io_watchdog_reboot);
        fan_rpm_text.setText("0");
        extend_power_switch = view.findViewById(R.id.eng_io_extend_power_switch);

        return view;
    }

    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);

        linkCheckBox(view.findViewById(R.id.eng_io_extend_power_switch), SettingKeys.Experimental.DATA_IO_EXTERNAL_POWER_BOOL);

        if (ProductConfig.isProjectOne()) {
            extend_power_switch.setEnabled(false); // disable checkbox
        }

        linkUserIntInput(view.findViewById(R.id.eng_io_fan_level), HALKeys.DATA_IO_SET_FAN_SPEED_INT);

        view.findViewById(R.id.eng_clear_io_fan_speed).setOnClickListener(v -> {
            dataMatrix.putInt(HALKeys.DATA_IO_SET_FAN_SPEED_INT,0);
            fan_level_text.setText(Integer.toString(0));
        });

        dataMatrix.createIntAdapter(HALKeys.DATA_IO_FAN_RPM_INT,value -> {
           fan_rpm_text.setText(Integer.toString(value));
        });

        power_monitor.setOnClickListener(v -> {
            dataMatrix.sendEvent(HALKeys.ACTION_IO_POWER_MONITOR);
            new JHTTimer(100,() -> powerMonitor()).start();

        });

        dataMatrix.createIntAdapter(SettingKeys.Debug.DATA_WATCHDOG_REBOOT_COUNTER_INT, integer -> {
           watchDog_reboot_counter.setText(Integer.toString(integer));
        });


        // CAB Test
        findViewById(R.id.eng_hardware_cab_test_title).setVisibility(View.VISIBLE);
        cabTestResult = findViewById(R.id.eng_hardware_cab_test_title);
        cabTestController = TVControllerFactory.getTVController(getContext(), SettingKeys.TVSource.CAB);
        cabTestResult.setText("");
        // If no response after 3 seconds, show test failed message
        cabTestTimeoutTimer = new JHTTimer(3000, () -> mHandler.post(() -> ((UserTextInput) view.findViewById(R.id.eng_hardware_cab_test_title)).setText(getContext().getString(R.string.test_failed_tc))));

        view.findViewById(R.id.eng_hardware_cab_test).setOnClickListener(v -> {
            cabTestTimeoutTimer.start();
            commandIndex ++;
            if(commandIndex > 2)
                commandIndex = 0;
            if(commandIndex == 0)
                cabTestController.enter();
            else if(commandIndex == 1)
                cabTestController.channelUp();
            else if(commandIndex == 2)
                cabTestController.channelDown();
        });
    }

    // TODO: Translations ???
    public void powerMonitor(){
        StringBuilder consolePower = new StringBuilder();
        StringBuilder extendPower = new StringBuilder();
        StringBuilder usbPower = new StringBuilder();

        consolePower.append(String.format(Locale.ENGLISH,"Voltage: %.2f",(float)dataMatrix.getInt(HALKeys.DATA_CONSOLE_VOLTAGE_INT) / 100)).append(System.getProperty("line.separator"));
        consolePower.append(String.format(Locale.ENGLISH,"Current: %d",dataMatrix.getInt(HALKeys.DATA_CONSOLE_CURRENT_INT)));

        if(dataMatrix.getBoolean(HALKeys.DATA_EXTEND_ERROR_BOOL))
            extendPower.append("Extend Power Error"); //NON-NLS
        else {
            extendPower.append(String.format(Locale.ENGLISH, "Voltage: %.2f", (float) dataMatrix.getInt(HALKeys.DATA_EXTEND_VOLTAGE_INT) / 100)).append(System.getProperty("line.separator"));
            extendPower.append(String.format(Locale.ENGLISH, "Current: %d", dataMatrix.getInt(HALKeys.DATA_EXTEND_CURRENT_INT))).append(System.getProperty("line.separator"));
        }

        usbPower.append(String.format(Locale.ENGLISH,"Voltage: %.2f",(float)dataMatrix.getInt(HALKeys.DATA_USB_VOLTAGE_INT) / 100));

        mHandler.post(() -> {
            console_power.setText(consolePower.toString());
            extend_power.setText(extendPower.toString());
            usb_power.setText(usbPower.toString());
        });
    }

    @Override
    public void hide(Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        cabTestTimeoutTimer.stop();
        powerMonitorTimer.stop();
        cabTestController.close();
        dataMatrix.finish();
    }
}
