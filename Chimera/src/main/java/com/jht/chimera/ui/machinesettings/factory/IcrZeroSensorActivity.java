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
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DataUtils;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IndoorBike.IndoorBikeCalibrationData;
import com.jht.datamatrixlib.DataMatrix;

@SuppressLint("SetTextI18n")
public class IcrZeroSensorActivity extends RestringActivity {
    private static final String TAG = "IcrZeroSensorActivity";

    private final DataMatrix dataMatrix = new DataMatrix();
    private TextView results;
    private View btnWrite;
    private final Handler handler = new Handler();

    private IndoorBikeCalibrationData.IcrCalibrationData calibration = new IndoorBikeCalibrationData.IcrCalibrationData();
    private byte[] lastWriteBytes;
    private boolean waitForConfirmation = false;
    private boolean waitForUpdate = false;

    // ADC points
    private TextView txAdc;
    private View btnAdc;
    private Integer readAdc = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_icr_zero_sensor);

        View backButton = findViewById(R.id.buttonClose);
        backButton.setOnClickListener(v -> finish());

        btnAdc = findViewById(R.id.button0);
        btnAdc.setOnClickListener(v -> getAdc());
        txAdc = findViewById(R.id.text_button0);

        btnWrite = findViewById(R.id.button_doit);
        btnWrite.setOnClickListener(v -> writeToLcb());

        results = findViewById(R.id.results);
    }

    private void setAdc(Integer adc) {
        readAdc = adc;
        if (adc == null) {
            txAdc.setText("--");
        } else {
            txAdc.setText(adc.toString());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        dataMatrix.createIntAdapter(HALKeys.ACTION_RECEIVE_RAW_ADC_INT, v -> {
            setAdc(v); // need to be posted to ui thread?
            results.setText("Read ADC " + v);
            toggleButtons(true);
        });
        dataMatrix.createStringAdapter(SettingKeys.Calibration.DATA_SETTING_RAPTOR_PARA_DATA_STRING, v -> {
            if (waitForUpdate) {
                waitForUpdate = false;
                updateCalibration(v);
            }
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
        btnAdc.setEnabled(isEnabled);
        btnWrite.setEnabled(isEnabled);
    }

    private void getAdc() {
        toggleButtons(false);
        txAdc.setText("- WAIT -");
        results.setText("Reading ADC...");
        DataMatrix.putInt(HALKeys.ACTION_REQUEST_RAW_ADC_INT, 0); // then wait for ACTION_RECEIVE_RAW_ADC_INT
    }

    private void writeToLcb() {
        if (readAdc == null) {
            results.setText("!! ERROR: Read Zero ADC from frame first !!");
            return;
        }
        results.setText("Updating LCB...");
        toggleButtons(false);
        //read old calibration
        waitForUpdate = true;
        DataMatrix.sendEvent(HALKeys.ACTION_RAPTOR_PARA_RESTORE);
        //toggleButtons(true);
    }

    private void updateCalibration(String oldCalibrationString) {
        byte[] oldCalibrationBytes = DataUtils.hexStringToByteArray(oldCalibrationString);
        IndoorBikeCalibrationData data = IndoorBikeCalibrationData.fromBytes(oldCalibrationBytes, 0);
        if (data instanceof IndoorBikeCalibrationData.IcrCalibrationData) {
            results.setText("Read old calibration (updating... )\n" + data.getReport());
            calibration = ((IndoorBikeCalibrationData.IcrCalibrationData) data);
            calibration.zeroADC.value = readAdc;
            lastWriteBytes = new byte[calibration.requiredByteSize()];
            calibration.toBytes(lastWriteBytes, 0);
            DataMatrix.putString(SettingKeys.Calibration.DATA_SETTING_RAPTOR_PARA_DATA_STRING, DataUtils.byteArrayToHexString(lastWriteBytes));
            DataMatrix.putString(SettingKeys.Application.DATA_VERSION_RAPTOR_PARAMETER_STRING, String.valueOf(lastWriteBytes[4]));
            DataMatrix.sendEvent(HALKeys.ACTION_RAPTOR_PARA_BACKUP); // we could make this pass an int, maybe 0 for the request and 1 for acknowledgement of completion?
            handler.postDelayed(()->{ // seems to work fine without delay, but should we keep delay as an extra precaution?
                waitForConfirmation = true;
                DataMatrix.sendEvent(HALKeys.ACTION_RAPTOR_PARA_RESTORE);
            }, 500); // then wait for DATA_SETTING_RAPTOR_PARA_DATA_STRING update
        } else {
            String report = (data == null ? "" : data.getReport());
            results.setText("!! LCB has bad calibration data !!\n" + report);
            toggleButtons(true);
        }
    }

}
