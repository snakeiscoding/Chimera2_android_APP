package com.jht.chimera.ui.machinesettings;

import static com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.ProjectOne.EVENT_SETUP_LCD_DEBUG_LAYOUT;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.ui.machinesettings.pages.tv.EngineeringPageBase;
import com.jht.datamatrixlib.DMCallback;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.libbumpsensor.BumpSensorController;

/**
 * Implements the general page gluing the page to the model layer.
 */
public class EngineeringDebug extends EngineeringPageBase {

    String TAG = "EngineeringDebug";
    private final DataMatrix dataMatrix = new DataMatrix();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private UserTextInput bumpSensorTriggerThreshold;
    private Button acceptTriggerThreshold;
    private Button btnErp;
    private TextView txtTriggerThresholdReadValue;
    private TextView bumpSensorVersion;
    private TextView lcdTouchFirmwareVersion;
    private View bumpSensorSettingControl;

    public EngineeringDebug() {
    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        View view = LayoutInflater.from(context).inflate(R.layout.engineering_debug, null);
        container.addView(view);
        return view;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);
        {

            //// Demo LCD
            // Show Debug LCDs
            findViewById(R.id.show_debug_button).setOnClickListener(v -> {
                DataMatrix.putBoolean(SettingKeys.ProjectOne.DATA_P1_ENABLE_DEBUG_VIEW_BOOL
                        , !DataMatrix.getBoolean(SettingKeys.ProjectOne.DATA_P1_ENABLE_DEBUG_VIEW_BOOL,false));
            });
            dataMatrix.createBoolAdapter(SettingKeys.ProjectOne.DATA_P1_ENABLE_DEBUG_VIEW_BOOL, new DMCallback<Boolean>() {
                @Override
                public void dataMatrixCallback(Boolean value) {
                    if (value) {
                        ((Button) findViewById(R.id.show_debug_button)).setText("Hide Debug LCDs");
                    } else {
                        ((Button) findViewById(R.id.show_debug_button)).setText("Show Debug LCDs");
                    }
                }
            },true);

            lcdTouchFirmwareVersion = findViewById(R.id.lcd_touch_firmware_version);
            String toggleVersion = Settings.Secure.getString(ContextUtil.getApplicationContext().getContentResolver(), "p1_lcd_toggle_version"); //NON-NLS
            lcdTouchFirmwareVersion.setText(String.format((getContext().getString(R.string.lcd_touch_version_tc) + " %1s"), toggleVersion));

            //// LCD Setup
            // Layout Setup
            ((Button) findViewById(R.id.debug_layout)).setOnClickListener(view -> {
                DataMatrix.sendEvent(EVENT_SETUP_LCD_DEBUG_LAYOUT);
            });

            //// Initialize the bump sensor section.
            if (BuildConfig.EXPOSE_BUMP_SENSOR_THRESHOLD_CHANGES) {
                initBumpSensor();
            }
        }
    }

    /**
     * Perform initialization of the bump sensor values. This only needs to be done if the values
     * are being exposed from the Build Config and their is a connected bump sensor.
     */
    private void initBumpSensor() {
        // bump sensor interaction cannot be on UI thread
        new Thread(() -> {
            if (BumpSensorController.isConnected()) {
                String text;
                try {
                    text = BumpSensorController.getVersion();
                } catch (Exception e) {
                    Log.e(TAG, "Caught exception", e);
                    text = "ERROR: Failed to retrieve value.";
                }
                final String finalText = text;
                handler.post(()-> updateBumpSensorUi(finalText));
            }
        }, "BumpMes").start();
    }
    /** perform UI updates on UI thread */
    @SuppressLint("SetTextI18n")
    private void updateBumpSensorUi(String bumpVersion) {
        bumpSensorSettingControl = findViewById(R.id.bump_sensor_setting_control);
        bumpSensorSettingControl.setVisibility(View.VISIBLE);

        // Initialize the bump sensor version information.
        bumpSensorVersion = findViewById(R.id.bump_sensor_version);
        bumpSensorVersion.setText(bumpVersion);

        // Initialize the bump sensor trigger threshold setting input.
        bumpSensorTriggerThreshold = findViewById(R.id.input_trigger_threshold);

        // Initialize the button allowing for trigger value changing.
        acceptTriggerThreshold = findViewById(R.id.accept_trigger_threshold);
        acceptTriggerThreshold.setOnClickListener(v -> {
            acceptTriggerThreshold.setEnabled(false); // include for debouncing.
            try {
                int threshold = Integer.parseInt(bumpSensorTriggerThreshold.getText());
                DataMatrix.putInt(SettingKeys.ProjectOne.DATA_BUMP_SENSOR_THRESHOLD_INT, threshold);
            } catch (Exception e) {
                Log.e(TAG, "Unable to set to the given value.", e);
            } finally {
                acceptTriggerThreshold.setEnabled(true);
            }
        });

        // Add button allowing for easily going to ERP.
        btnErp = findViewById(R.id.btn_erp);
        btnErp.setOnClickListener(v -> {
            DataMatrix.sendEvent(HALKeys.ACTION_ENTER_ERP);
        });

        // Add text view for showing the current value of the sensor.
        txtTriggerThresholdReadValue = findViewById(R.id.txt_trigger_threshold_read_value);

        dataMatrix.createIntAdapter(SettingKeys.ProjectOne.DATA_BUMP_SENSOR_THRESHOLD_INT, threshold -> {
            bumpSensorTriggerThreshold.setText(String.valueOf(threshold));
            txtTriggerThresholdReadValue.setText("Current Value: " + threshold + " mG");
        }, true);
    }

    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        dataMatrix.finish();
    }
}
