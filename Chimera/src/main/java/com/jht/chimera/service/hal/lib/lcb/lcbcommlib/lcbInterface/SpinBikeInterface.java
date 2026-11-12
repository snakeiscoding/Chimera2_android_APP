package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface;

import android.util.Log;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IndoorBike.IIndoorBikeHandlerCallback;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IndoorBike.IndoorBikeHandler;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IndoorBike.IndoorBikeInfoSpinDown;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IndoorBike.IndoorBikeSensorData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

public class SpinBikeInterface implements MachineInterfaceBase {

    private final String TAG = "SpinBikeInterface";
    private ILCBCallback serviceCallback;
    private IndoorBikeHandler mIndoorbikeHandler;

    /* Sensor Data */
    private int crankRpm = 0;
    private int crankRpmDirection;
    private int flywheelRpm;
    private int watts;
    private int stepperMotorStatus;
    private int stepperMotorStepMode;
    private int stepperMotorPosition;
    private int stepperMotorRange;
    private int resistanceLevel;

    /* Stepper Motor Status */
    private boolean motorRunningStstus;
    private boolean calibrationTimeout;
    private boolean calibrationStrokeError;
    private boolean driverIcNoResponse;
    private boolean underVoltageLockout;
    private boolean thermalWarning;
    private boolean thermalShoutdown;
    private boolean overCurrent;
    private boolean stepLoss_A;
    private boolean stepLossB;

    /* Info Spin Down */
    private int spinDownStatus;
    private int spinDownFlywheelRotation;
    private float spinDownTorqueScalar;
    private float spinDownCorrectionFactor;
    private float spinDownInertia;	

    public SpinBikeInterface(final ILCBCallback callback) {
        mIndoorbikeHandler = IndoorBikeHandler.getInstance();
        this.serviceCallback = callback;
        mIndoorbikeHandler.setIndoorBikeHandlerCallback(mIndoorbikeHandlerCallback);
        mIndoorbikeHandler.initialIndoorBike();
    }

    private IIndoorBikeHandlerCallback mIndoorbikeHandlerCallback = new IIndoorBikeHandlerCallback() {
        @Override
        public void onIndoorBikeRPM(int intIndoorBikeRPM) {
            serviceCallback.onRpm(intIndoorBikeRPM);
        }

        @Override
        public void onIndoorBikeRawAdc(int adc) {
            serviceCallback.onRawADC(adc);
        }

        @Override
        public void onIndoorBikeOneShotRPM(int intIndoorBikeRPM) {
            Log.d(TAG, "onIndoorBikeOneShotRPM: "+intIndoorBikeRPM);
        }

        @Override
        public void onIndoorBikeWatts(int intIndoorBikeWatts) {
            onLcbWattsHandler(intIndoorBikeWatts);
        }

        @Override
        public void onIndoorBikeSensorData(IndoorBikeSensorData indoorBikeSensorData) {
            onSensorDataHandler(indoorBikeSensorData);
        }

        @Override
        public void onIndoorBikeOneShotSensorData(IndoorBikeSensorData indoorBikeSensorData) {
            onSensorDataHandler(indoorBikeSensorData);
        }

        @Override
        public void onIndoorBikeGetCalcParaOfDevice(byte[] bytesData) {
            onInfoRaptorParaHandler(bytesData);

        }

        @Override
        public void onIndoorBikeSetActionSpinDown(byte[] bytesDataReceived) {
//            //   HALKeys.DATA_SPINDOWN_TEST_STATUS_INT
//            // ATHENA To Be Completed
        }

        @Override
        public void onIndoorBikeSetErgModeSettings(byte[] bytesDataReceived) {

        }

        @Override
        public void onIndoorBikeInfoSpinDown(IndoorBikeInfoSpinDown indoorBikeInfoSpinDown) {
            onInfoSpinDownHandler(indoorBikeInfoSpinDown);
        }
		
    };

    @Override
    public void startReplyData(boolean start) {
        mIndoorbikeHandler.setAutoReplyIndoorBikeSensorData(start);
    }

    public void stopAutoHeartBeatThread() {
        mIndoorbikeHandler.stopAutoHeartBeatThread();
    }

    @Override
    public void unInitial() {
        mIndoorbikeHandler.unsetIndoorBikeHandlerCallback();
        mIndoorbikeHandler.unInitialIndoorBike();
        mIndoorbikeHandler = null;
    }

    public void setResistanceSpinBike(int resistance) {
        //Log.d (TAG, " set resistance=" + resistance);
        mIndoorbikeHandler.setLcbIndoorBikeResistanceLevel(resistance);
    }

    public void setErgWattsSpinBike(int targetWatts) {
        //Log.d (TAG, " target watts=" + targetWatts);
        mIndoorbikeHandler.setLcbIndoorBikeErgWatts (targetWatts);
    }

    private void onLcbWattsHandler(int watts) {
        serviceCallback.onWatts(watts);
    }

    private void onSensorDataHandler(IndoorBikeSensorData indoorBikeSensorData) {

        crankRpm = indoorBikeSensorData.crankRpm;
        crankRpmDirection = indoorBikeSensorData.crankRpmDirection;
        flywheelRpm = indoorBikeSensorData.flywheelRpm;
        watts = indoorBikeSensorData.watts;
        stepperMotorStatus = indoorBikeSensorData.stepperMotorStatus;
        stepperMotorStepMode = indoorBikeSensorData.stepperMotorStepMode;
        stepperMotorPosition = indoorBikeSensorData.stepperMotorPosition;
        stepperMotorRange = indoorBikeSensorData.stepperMotorRange;
        resistanceLevel = indoorBikeSensorData.resistanceLevel;


        serviceCallback.onWatts(watts);
        serviceCallback.onRpm(crankRpm);

	    String sensorDataString =
            String.format(Locale.ENGLISH, "%s: %d\n", ContextUtil.getString(R.string.crank_rpm_tc), crankRpm) +
            String.format(Locale.ENGLISH, "%s: %d\n", ContextUtil.getString(R.string.crank_direction_tc), crankRpmDirection) +
            String.format(Locale.ENGLISH, "%s: %d\n", ContextUtil.getString(R.string.flywheel_rpm_tc), flywheelRpm) +
            String.format(Locale.ENGLISH, "%s: %d\n", ContextUtil.getString(R.string.watts_tc), watts) +
            String.format(Locale.ENGLISH, "%s: %d\n", ContextUtil.getString(R.string.stepper_motor_status_tc), stepperMotorStatus) +
            String.format(Locale.ENGLISH, "%s: %d\n", ContextUtil.getString(R.string.stepper_motor_mode_tc), stepperMotorStepMode) +
            String.format(Locale.ENGLISH, "%s: %d\n", ContextUtil.getString(R.string.stepper_motor_position_tc), stepperMotorPosition) +
            String.format(Locale.ENGLISH, "%s: %d\n", ContextUtil.getString(R.string.stepper_motor_range_tc), stepperMotorRange) +
            String.format(Locale.ENGLISH, "%s: %d\n", ContextUtil.getString(R.string.resistance_tc), resistanceLevel);

        serviceCallback.onShowRawSensorDataString(sensorDataString);

        onMotorStatusParser(stepperMotorStatus);
    }

    /*
        Stepper motor status
        Bit 0:  Motor running status  --> 0:Stop, 1:running
        Bit 1:  Calibration timeout
        Bit 2:  Calibration stroke error
        Bit 3:  Driver IC no response
        Bit 4:  Under voltage(VSthOff) lockout (6.6V - 7.8V)
        Bit 5:  Thermal warning (130 degree)
        Bit 6:  Thermal shutdown (160 degree)
        Bit 7:  Over-current (error)
        Bit 8:  STEP_LOSS_A
        Bit 9:  STEP_LOSS_B
        Bit 10 ~ Bit 15: reserved
    */

    private void onMotorStatusParser(int st) {

        byte status[] = {(byte) (st & 0xFF), (byte) ((st >> 8) & 0xFF)};

        motorRunningStstus = (status[0] & 0x01) != 0;
        calibrationTimeout = (status[0] & 0x02) != 0;
        calibrationStrokeError = (status[0] & 0x04) != 0;
        driverIcNoResponse = (status[0] & 0x08) != 0;
        underVoltageLockout = (status[0] & 0x10) != 0;
        thermalWarning = (status[0] & 0x20) != 0;
        thermalShoutdown = (status[0] & 0x40) != 0;
        overCurrent = (status[0] & 0x80) != 0;
        stepLoss_A = (status[1] & 0x01) != 0;
        stepLossB = (status[1] & 0x02) != 0;

        /*
        Log.d(TAG, " onMotorStatusParser: "+ motorRunningStstus+ "\n" +
                " calibrationTimeout : "+calibrationTimeout + "\n" +
                " calibrationStrokeError : "+calibrationStrokeError + "\n" +
                " driverIcNoResponse : "+driverIcNoResponse + "\n" +
                " underVoltageLockout : "+underVoltageLockout + "\n" +
                " thermalWarning : "+thermalWarning + "\n" +
                " thermalShoutdown: "+thermalShoutdown + "\n" +
                " overCurrent: "+overCurrent + "\n" +
                " stepLoss_A: "+stepLoss_A + "\n" +
                " stepLossB: "+stepLossB ); */
    }


    public static int byteArrayToInt(byte[] b) {
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a) {
        return new byte[] {
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

    //set Auto reply IndoorBike Info SpinDown cmd
    public void setAutoReplySpinBikeInfoSpinDown(boolean enable) {
        mIndoorbikeHandler.setAutoReplyIndoorBikeInfoSpinDown(enable);
    }

    private void onInfoSpinDownHandler(IndoorBikeInfoSpinDown indoorBikeInfoSpinDown) {

        spinDownStatus = indoorBikeInfoSpinDown.status;
        spinDownFlywheelRotation = indoorBikeInfoSpinDown.flywheelRotation;
        spinDownTorqueScalar = indoorBikeInfoSpinDown.torqueScalar;
        spinDownCorrectionFactor = indoorBikeInfoSpinDown.correctionFactor;
        spinDownInertia = indoorBikeInfoSpinDown.inertia;	


        serviceCallback.onSpinBikeInfoSpinDown(spinDownStatus , spinDownFlywheelRotation , spinDownTorqueScalar, spinDownCorrectionFactor, spinDownInertia);

    }

    /*  Set the action of spin down .
        Data 0x01: start spin down. 
        Data 0x02: stop spin down
        Data 0x03: clear system torque scalar from spin down        
        Data 0x04: set correction  factor
        Data 0x05: set inertia
    */

    public void setSpinBikeSpinDownOn(boolean on) {

        byte[] data = new byte[1];
        data[0] = (byte) ((on) ? (0x01) : (0x02));
        mIndoorbikeHandler.setLcbIndoorBikeActionSpinDown(data);
    }

    public void setSpinBikeClearTorque() {

        byte[] data = new byte[1];
        data[0] = (byte) 0x03;
        mIndoorbikeHandler.setLcbIndoorBikeActionSpinDown(data);
    }

    public void requestRawAdc(int index) {
        mIndoorbikeHandler.requestRawAdc(index);
    }

    public void setSpinBikeCorrectionFactor(float factor) {
        byte[] payload = new byte[5];//cmd + data(4 bytes float)
        ByteBuffer byteBuffer = ByteBuffer.wrap(payload);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.put((byte) 0x04);
        byteBuffer.putFloat(factor);
        mIndoorbikeHandler.setLcbIndoorBikeActionSpinDown(payload);
    }

    public void setSpinBikeInertia(float inertia) {

        byte[] payload = new byte[5];//cmd + data(4 bytes float)
        ByteBuffer byteBuffer = ByteBuffer.wrap(payload);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.put((byte) 0x05);
        byteBuffer.putFloat(inertia);
        mIndoorbikeHandler.setLcbIndoorBikeActionSpinDown(payload);
    }

    public void getSpinBikeRaptorPara() {
        mIndoorbikeHandler.getLcbIndoorBikeCalcParaOfDevice();
    }

    public void setSpinBikeRaptorPara(byte[] bytesData) {
        mIndoorbikeHandler.setLcbIndoorBikeCalcParaOfDevice(bytesData);
    }

    private void onInfoRaptorParaHandler(byte[] bytesData) {
        serviceCallback.onSpinBikeRaptorPara(bytesData);

    }
	
}
