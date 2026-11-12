package com.jht.chimera.ui.machinesettings.dialogs;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.ui.machinesettings.EngineeringIdleTimerManager;
import com.jht.datamatrixlib.DataMatrix;

public class SpinBikeCalibrationDialog extends DialogBase {
    private final String TAG = "SpinBikeCalibrationD...";
    private DataMatrix dataMatrix = new DataMatrix();
    private SpinBikeCalibrationDialog.IOnYesNo onYesNo;
    private WebView message;
    private TextView currentRPM;
    private TextView flywheelRPM;
    private TextView results;
    private Button startCalibration;
    private Button restoreFactory;

    public interface IOnYesNo {
        void onYesNo(boolean var1);
    }
    public SpinBikeCalibrationDialog(Context context) {
        this(context, null, 0);
    }

    public SpinBikeCalibrationDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpinBikeCalibrationDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        results = findViewById(R.id.results);
        startCalibration = findViewById (R.id.btnStartCalibration);
        restoreFactory = findViewById (R.id.restoreFactoryCalibration);
        currentRPM = findViewById(R.id.currentRPM);
        flywheelRPM = findViewById(R.id.flywheelRPM);
        DataMatrix.putInt(HALKeys.ACTION_SET_RESISTANCE_INT, 1);

        startCalibration = findViewById(R.id.btnStartCalibration);
        startCalibration.setOnClickListener(v ->
        {
            DataMatrix.sendEvent(HALKeys.ACTION_START_SPINDOWN_TEST);
            setResults(R.string.calculating_tc);
            startCalibration.setVisibility(View.INVISIBLE);
            restoreFactory.setVisibility(View.INVISIBLE);
        });

        findViewById(R.id.btnCancel).setOnClickListener(v -> {
            DataMatrix.sendEvent (HALKeys.ACTION_STOP_SPINDOWN_TEST);
            EngineeringIdleTimerManager.idleTimerManagement.monitorTimers();
            dataMatrix.finish();
            hide();
        });

        restoreFactory.setOnClickListener(v -> {
            DataMatrix.sendEvent(HALKeys.ACTION_CLEAR_TORQUE);
            DataMatrix.sendEvent(HALKeys.ACTION_RAPTOR_PARA_RESTORE);
        });

        dataMatrix.createIntAdapter(HALKeys.DATA_SPINDOWN_TEST_STATUS_INT, value -> {
            if (value == SpinDownStatus.Succeed.ordinal()) {
                setResults(R.string.success_lc);
        	dataMatrix.putBoolean(HALKeys.ACTION_GET_INFO_SPIN_DOWN_BOOL, false);
			//calibration finish save factor para to console
	        	dataMatrix.sendEvent(HALKeys.ACTION_RAPTOR_PARA_RESTORE);
            }
            else if (value == SpinDownStatus.Calculating.ordinal()) {
                setResults(R.string.calculating_tc);
            }
            else if (value == SpinDownStatus.DeviceError.ordinal()) {
                setResults(R.string.device_error_tc);
        	dataMatrix.putBoolean(HALKeys.ACTION_GET_INFO_SPIN_DOWN_BOOL, false);
            }
            else if (value == SpinDownStatus.OperationError.ordinal()) {
                setResults(R.string.device_error_tc);
        	dataMatrix.putBoolean(HALKeys.ACTION_GET_INFO_SPIN_DOWN_BOOL, false);
            }
            else if (value == SpinDownStatus.Fail.ordinal()) {
                setResults(R.string.failure_uc);
        	dataMatrix.putBoolean(HALKeys.ACTION_GET_INFO_SPIN_DOWN_BOOL, false);
            }
            else if (value == SpinDownStatus.Canceled.ordinal()) {
                setResults(R.string.test_cancelled_tc);
        	dataMatrix.putBoolean(HALKeys.ACTION_GET_INFO_SPIN_DOWN_BOOL, false);

            }
            else if (value == SpinDownStatus.None.ordinal()) {
                setResults(R.string.status_tc);
            }

            EngineeringIdleTimerManager.idleTimerManagement.monitorTimers();

            startCalibration.setVisibility(View.VISIBLE);
            restoreFactory.setVisibility(View.VISIBLE);
        });
    }

    private void setResults (int textID) {
        results.post(() -> {
            results.setText(textID);
        });
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.spindown_welcome_dialog;
    }

    public void init() {
        // Clear out any stagnant values
        DataMatrix.putInt(HALKeys.DATA_SPINDOWN_TEST_STATUS_INT, SpinDownStatus.None.ordinal());

         dataMatrix.createIntAdapter(HALKeys.DATA_RPM_INT, value ->
         {
             currentRPM.post(() -> currentRPM.setText(value.toString()));
             startCalibration.setEnabled(value > 105);

             if (value > 20) {
                 EngineeringIdleTimerManager.idleTimerManagement.stopMonitoring();
             }
             else {
                 EngineeringIdleTimerManager.idleTimerManagement.monitorTimers();
             }
         });

        dataMatrix.createIntAdapter(HALKeys.DATA_INFO_SPIN_DOWN_RPM_INT, value -> {
            flywheelRPM.post(() -> flywheelRPM.setText(value.toString()));
        });
    }

}
