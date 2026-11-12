package com.jht.chimera.ui.machinesettings.factory;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jht.chimera.lib.widget.activity.RestringActivity;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DataUtils;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IndoorBike.IndoorBikeCalibrationData;
import com.jht.datamatrixlib.DataMatrix;

import java.io.File;

// if there is a coder's heaven, this code may exception me to its inverse

@SuppressLint("SetTextI18n")
public class IcrCalibrationActivity extends RestringActivity {
    private static final String TAG = "IcrCalibration";

    private final DataMatrix dataMatrix = new DataMatrix();
    private IcrCalibrationSettings settings;
    private TextView results;
    private View btnWrite;
    private final Handler handler = new Handler();

    private final IndoorBikeCalibrationData.IcrCalibrationData calibration = new IndoorBikeCalibrationData.IcrCalibrationData();
    private byte[] lastWriteBytes;
    private boolean waitForConfirmation = false;

    // ADC points
    View[] btnAdc = new View[2];
    TextView[] txAdc = new TextView[2];
    double[] readAdc = new double[2];
    int indexAdc = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_icr_calibration);

        View backButton = findViewById(R.id.buttonClose);
        backButton.setOnClickListener(v -> finish());

        btnAdc[0] = findViewById(R.id.button0);
        btnAdc[0].setOnClickListener(v -> getAdc(0));
        btnAdc[1] = findViewById(R.id.button1);
        btnAdc[1].setOnClickListener(v -> getAdc(1));
        txAdc[0] = findViewById(R.id.text_button0);
        txAdc[1] = findViewById(R.id.text_button1);

        btnWrite = findViewById(R.id.button_doit);
        btnWrite.setOnClickListener(v -> writeToLcb());

        results = findViewById(R.id.results);
        loadSettings();

        TextView label0 = findViewById(R.id.label0);
        TextView label1 = findViewById(R.id.label1);
        label0.setText("Measure 0 kgf");
        label1.setText("Measure " + settings.nonZeroKGF + " kgf");

        // Load initial values if they have been set (dev may want to test with canned numbers from json)
        setAdc(0, settings.startAdc0);
        setAdc(1, settings.startAdc1);
    }

    private void setAdc(int index, Integer adc) {
        if (adc == null) {
            readAdc[index] = 0;
            txAdc[index].setText("--");
        } else {
            readAdc[index] = adc;
            txAdc[index].setText(adc.toString());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        dataMatrix.createIntAdapter(HALKeys.ACTION_RECEIVE_RAW_ADC_INT, v -> {
            if (indexAdc >= 0 && indexAdc < readAdc.length) {
                setAdc(indexAdc, v); // need to be posted to ui thread?
                indexAdc = -1;
                results.setText("Read ADC " + v);
                toggleButtons(true);
            }
        });
        dataMatrix.createStringAdapter(SettingKeys.Calibration.DATA_SETTING_RAPTOR_PARA_DATA_STRING, v -> {
            if (waitForConfirmation) {
                Log.d(TAG, "DATA_SETTING_RAPTOR_PARA_DATA_STRING = " + v);
                String lastWriteString = DataUtils.byteArrayToHexString(lastWriteBytes);
                boolean verified = (lastWriteString.equals(v));
                Log.d(TAG, "write confirmed " + verified);
                if (verified) {
                    results.setText("<< SUCCESS >>\n" + calibration.getReport());
                } else {
                    results.setText("!!! FAILED !!!\n" + calibration.getReport());
                }
                waitForConfirmation = false;
                toggleButtons(true);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        dataMatrix.finish();
    }

    private void toggleButtons(boolean isEnabled) {
        for (View v : btnAdc) v.setEnabled(isEnabled);
        btnWrite.setEnabled(isEnabled);
    }

    private void getAdc(int index) {
        toggleButtons(false);
        txAdc[index].setText("- WAIT -");
        results.setText("Reading ADC...");
        indexAdc = index;
        DataMatrix.putInt(HALKeys.ACTION_REQUEST_RAW_ADC_INT, 0); // then wait for ACTION_RECEIVE_RAW_ADC_INT
    }

    // some parameters are hardcoded, but in case they change, should provide a means to change them without resorting to user entry?
    private void loadSettings() {
        boolean createNew = true;
        String op = "SAVED TO ";
        File usb = StorageUtils.getUSBPath();
        File settingsPath = new File(usb, "icr_cal_settings.json");
        if (usb.exists()) {
            if (settingsPath.exists()) {
                try {
                    settings = IcrCalibrationSettings.load(settingsPath);
                    createNew = false;
                    op = "LOADED FROM ";
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load settings", e);
                }
            }
            if (createNew) {
                try {
                    if (settings == null) settings = new IcrCalibrationSettings();
                    settings.save(settingsPath);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to save settings", e);
                }
            }
            results.setText(op + settingsPath.getAbsolutePath() + "\n" + settings.getReport());
        } else {
            results.setText("!! CANNOT FIND USB DRIVE !!");
        }
    }

    private void writeToLcb() {
        if (readAdc[0] >= readAdc[1]) {
            results.setText("!! ERROR: First ADC reading must be less than the second reading !!");
            return;
        }
        toggleButtons(false);
        results.setText("Writing to LCB...");
        calibration.setTimeStamp();
        calibration.ST2.value = settings.ST2;
        calibration.ST1.value = settings.ST1;
        calibration.ST0.value = settings.ST0;
        calibration.beltRatio.value = settings.beltRatio;
        calibration.radiusFlywheelToResistance.value = settings.radiusMeters;
        calibration.stScale.value = settings.stScale;
        //calibration.adcScale.value = settings.adcScale;
        //calibration.minADC.value = settings.minADC;
        //calibration.maxADC.value = settings.maxADC;
        // compute from readAdc[]
        calibration.zeroADC.value = readAdc[0];
        calibration.MT2.value = 0;
        calibration.MT1.value = settings.nonZeroKGF / (readAdc[1] - readAdc[0]);
        calibration.MT0.value = 0;

        lastWriteBytes = new byte[calibration.requiredByteSize()];
        calibration.toBytes(lastWriteBytes, 0);
        DataMatrix.putString(SettingKeys.Calibration.DATA_SETTING_RAPTOR_PARA_DATA_STRING, DataUtils.byteArrayToHexString(lastWriteBytes));
        DataMatrix.putString(SettingKeys.Application.DATA_VERSION_RAPTOR_PARAMETER_STRING, String.valueOf(lastWriteBytes[4]));
        DataMatrix.sendEvent(HALKeys.ACTION_RAPTOR_PARA_BACKUP); // we could make this pass an int, maybe 0 for the request and 1 for acknowledgement of completion?
        // trigger a read back for confirmation ?
        handler.postDelayed(()->{ // seems to work fine without delay, but should we keep delay as an extra precaution?
            //DataMatrix.putString(SettingKeys.Calibration.DATA_SETTING_RAPTOR_PARA_DATA_STRING, "");
            //DataMatrix.putString(SettingKeys.Application.DATA_VERSION_RAPTOR_PARAMETER_STRING, "");
            waitForConfirmation = true;
            DataMatrix.sendEvent(HALKeys.ACTION_RAPTOR_PARA_RESTORE);
        }, 500); // then wait for DATA_SETTING_RAPTOR_PARA_DATA_STRING update

        //toggleButtons(true);
    }

}
