package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface;

import android.util.Log;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.TcvFxSpinBike.ITcvFxSpinBikeHandlerCallback;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.TcvFxSpinBike.TcvFxSpinBikeHandler;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.TcvFxSpinBike.TcvFxSpinBikeSensorData;

import java.util.Locale;

public class TcvFxSpinBikeInterface implements MachineInterfaceBase {

    private final String TAG = "TcvFxSpinBikeInterface";
    private ILCBCallback serviceCallback;
    private TcvFxSpinBikeHandler mTcvFxSpinBikeHandler;

    /* Sensor Data */
    private int crankRpm = 0;
    private int flywheelRpm;
    private int ecbPositionSensor;
    private int resistanceLevel;
    private int watts;

    public TcvFxSpinBikeInterface(final ILCBCallback callback) {
        mTcvFxSpinBikeHandler = TcvFxSpinBikeHandler.getInstance();
        this.serviceCallback = callback;
        mTcvFxSpinBikeHandler.setTcvFxSpinBikeHandlerCallback(mTcvFxSpinBikeHandlerCallback);
        mTcvFxSpinBikeHandler.initialTcvFxSpinBike();
    }

    private ITcvFxSpinBikeHandlerCallback mTcvFxSpinBikeHandlerCallback = new ITcvFxSpinBikeHandlerCallback() {
        @Override
        public void onTcvFxSpinBikeSensorData(TcvFxSpinBikeSensorData data) {
            onSensorDataHandler(data);
        }

        @Override
        public void onTcvFxSpinBikeRPM(int intTcvFxSpinBikeRPM) {
            serviceCallback.onRpm(intTcvFxSpinBikeRPM);
        }

        @Override
        public void onTcvFxSpinBikeWatts(int intTcvFxSpinBikeWatts) {

            // LCB delivers watts in 0.1W units; convert to W before surfacing.
            serviceCallback.onWatts(intTcvFxSpinBikeWatts / 10);
        }

        @Override
        public void onTcvFxSpinBikeResistanceLevel(int intTcvFxSpinBikeResistanceLevel) {
            serviceCallback.onResistanceLevel(intTcvFxSpinBikeResistanceLevel);
        }
    };

    @Override
    public void startReplyData(boolean start) {
        mTcvFxSpinBikeHandler.setAutoReplyTcvFxSpinBikeSensorData(start);
    }

    public void stopAutoHeartBeatThread() {
        mTcvFxSpinBikeHandler.stopAutoHeartBeatThread();
    }

    @Override
    public void unInitial() {
        mTcvFxSpinBikeHandler.unsetTcvFxSpinBikeHandlerCallback();
        mTcvFxSpinBikeHandler.unInitialTcvFxSpinBike();
        mTcvFxSpinBikeHandler = null;
    }

    private void onSensorDataHandler(TcvFxSpinBikeSensorData data) {
        crankRpm = data.crankRpm;
        flywheelRpm = data.flywheelRpm;
        ecbPositionSensor = data.ecbPositionSensor;
        resistanceLevel = data.resistanceLevel;
        watts = data.watts;

        String sensorDataString =
            String.format(Locale.ENGLISH, "%s: %d\n", ContextUtil.getString(R.string.crank_rpm_tc), crankRpm) +
            String.format(Locale.ENGLISH, "%s: %d\n", ContextUtil.getString(R.string.flywheel_rpm_tc), flywheelRpm) +
            String.format(Locale.ENGLISH, "ECB position sensor: %d\n", ecbPositionSensor) +
            String.format(Locale.ENGLISH, "%s: %d\n", ContextUtil.getString(R.string.resistance_tc), resistanceLevel) +
            String.format(Locale.ENGLISH, "%s: %d\n", ContextUtil.getString(R.string.watts_tc), watts);

        serviceCallback.onShowRawSensorDataString(sensorDataString);
    }
}
