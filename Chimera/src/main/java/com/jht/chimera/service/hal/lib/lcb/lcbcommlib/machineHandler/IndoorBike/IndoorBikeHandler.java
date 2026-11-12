package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IndoorBike;

import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IDeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ILcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.GeneralHandler;

public class IndoorBikeHandler {
    private static final String TAG = "IndoorBikeHandler";

    protected ILcbComm mLcb;
    protected IDeviceDataTransmitted mDeviceLcb;
    private IndoorBikeCallbackHandlerLCB mIndoorBikeLcbCommReceive = new IndoorBikeCallbackHandlerLCB(this);

    protected IIndoorBikeHandlerCallback mIndoorBikeHandlerCallback;

    private GeneralHandler mGeneralHandler;
    private LcbType machineLcbType = LcbType.LCB_UNKNOWN;

//Handler Auto Heart Beat ===>
    private volatile boolean  _isRunning                              = false;
    private static final Object        _notifyHeartBeatSync = new Object();
    private static Thread              _heartBeatThread             = null;
    private static final int  HEART_BEAT_TIMER_PERIOD = 400;
    private static volatile JHTTimer mStartHeartBeatTimer;

    //get rpm used
    private volatile boolean  _isAutoReplyRpm                     = false;
    private static final int  TIMES_RPM_INTERVAL = 0; // 0 HEART_BEAT_TIMER_PERIOD Interval
    private int  cntIntervalTimesRpm                     = 0; //HEART_BEAT_TIMER_PERIOD x times 

    //get watts used (ICR70)
    private volatile boolean  _isAutoReplyWatts                     = false;
    private static final int  TIMES_WATTS_INTERVAL = 0; // 0 HEART_BEAT_TIMER_PERIOD Interval
    private int  cntIntervalTimesWatts                     = 0; //HEART_BEAT_TIMER_PERIOD x times

    //get sensor data used
    private volatile boolean  _isAutoReplySensorData                     = false;
    private static final int  TIMES_SENSORDATA_INTERVAL = 0; // 0 HEART_BEAT_TIMER_PERIOD Interval
    private int  cntIntervalTimesSensorData                     = 0; //HEART_BEAT_TIMER_PERIOD x times 

    //get info Spin Down data used
    private volatile boolean  _isAutoReplyInfoSpinDown                     = false;
    private static final int  TIMES_INFO_SPINDOWN_INTERVAL = 1; // 1 HEART_BEAT_TIMER_PERIOD Interval
    private int  cntIntervalTimesInfoSpinDown                     = 0; //HEART_BEAT_TIMER_PERIOD x times 
	
//<=== Hanler Auto Heart Beat 

    private IndoorBikeHandler() {
    }

    private static class SingletonHolder {
        private static IndoorBikeHandler Instance = new IndoorBikeHandler();
    }

    public static IndoorBikeHandler getInstance() {
        return SingletonHolder.Instance;
    }

//Hanler Auto Heart Beat ===>
	
    /*******************************************************************************
    * Function Name  : timerStartAutoHeartBeatThread()
    * Description    : used timer start auto HeartBeatThread
    * Input          : None
    * Output         : None
    * Return         : None
    *******************************************************************************/
    private void timerStartAutoHeartBeatThread() {
        mStartHeartBeatTimer = new JHTTimer(100, new JHTTimer.IOnTimer() {
            @Override
            public void onTimer() {
                startAutoHeartBeatThread();
                mStartHeartBeatTimer.stop();
            }
        });
        mStartHeartBeatTimer.start("InBikeHbTimer");
    }

    /*******************************************************************************
    * Function Name  : startAutoHeartBeatThread()
    * Description    : process start auto HeartBeatThread
    * Input          : None
    * Output         : None
    * Return         : None
    *******************************************************************************/
    private void startAutoHeartBeatThread() {
	stopAutoHeartBeatThread();
	_heartBeatThread = new HeartBeatThread();
	if (_heartBeatThread != null)
		_heartBeatThread.start();
    }

    /*******************************************************************************
    * Function Name  : stopAutoHeartBeatThread()
    * Description    : process  stop auto HeartBeatThread
    * Input          : None
    * Output         : None
    * Return         : None
    *******************************************************************************/
    public void stopAutoHeartBeatThread() {
	if (_isRunning) {
		_isRunning = false;
		_heartBeatThread.interrupt();

		// wake thread
		synchronized (_notifyHeartBeatSync) {
			_notifyHeartBeatSync.notifyAll();
		}

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
		
		_heartBeatThread = null;
	}
    }

    /*******************************************************************************
    * Function Name  : HeartBeatThread
    * Description    : HeartBeat Thread class
    * Input          : None
    * Output         : None
    * Return         : None
    *******************************************************************************/
    private class HeartBeatThread extends Thread {
        HeartBeatThread() { super("InBikeHbThread"); }
        @Override
        public void run() {
            super.run();
            Log.v(TAG, "IndoorBike HeartBeatThread++++");

            _isRunning = true;
            try {
                while (_isRunning && !isInterrupted()) {
                    synchronized (_notifyHeartBeatSync) {
                        _notifyHeartBeatSync.wait(HEART_BEAT_TIMER_PERIOD);
                        if (_isRunning) {
					        //auto reply cmd
					        autoReplyCmd();
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                Log.e(TAG, "Exception in IndoorBike heartBeat thread! ", ex);
                _isRunning = false;
		//re-start HeartBeatThread
                Log.v(TAG, "Re-start IndoorBike HeartBeatThread . ");
		timerStartAutoHeartBeatThread();
            }

            Log.v(TAG, "IndoorBike HeartBeatThread----");
        }
    }

    /*******************************************************************************
    * Function Name  : autoReplyCmd()
    * Description    : process Heart Beat to AutoReply cmd
    * Input          : None
    * Output         : None
    * Return         : None
    *******************************************************************************/
    private synchronized void autoReplyCmd() {
        if(_isAutoReplyRpm)
            autoReplyRpm();
        if (_isAutoReplyWatts)
            autoReplyWatts();

        if(_isAutoReplySensorData)
            autoReplySensorData();

        if(_isAutoReplyInfoSpinDown)
            autoReplyInfoSpinDown();
    }

    /*******************************************************************************
    * Function Name  : autoReplyRpm()
    * Description    : process Heart Beat to AutoReply LCB Ascent Rpm
    * Input          : None
    * Output         : None
    * Return         : None
    *******************************************************************************/
    private synchronized void autoReplyRpm() {
        if (cntIntervalTimesRpm ==TIMES_RPM_INTERVAL){
            cntIntervalTimesRpm = 0;
            mLcb.getLcbECBrpm(LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesRpm++;
        }
    }

    private synchronized void autoReplyWatts() {
        if (cntIntervalTimesWatts == TIMES_WATTS_INTERVAL) {
            cntIntervalTimesWatts = 0;
            mLcb.getLcbWatts(LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesWatts++;
        }
    }

    /*******************************************************************************
    * Function Name  : autoReplySensorData()
    * Description    : process Heart Beat to AutoReply LCB Sensor Data
    * Input          : None
    * Output         : None
    * Return         : None
    *******************************************************************************/
    private synchronized void autoReplySensorData() {
        if (cntIntervalTimesSensorData == TIMES_SENSORDATA_INTERVAL){
            cntIntervalTimesSensorData = 0;
            mLcb.getLcbIndoorBikeSensorData(LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesSensorData++;
        }
    }	

    /*******************************************************************************
    * Function Name  : autoReplyInfoSpinDown()
    * Description    : process Heart Beat to AutoReply LCB Info SpinDown
    * Input          : None
    * Output         : None
    * Return         : None
    *******************************************************************************/
    private synchronized void autoReplyInfoSpinDown() {
        if (cntIntervalTimesInfoSpinDown ==TIMES_INFO_SPINDOWN_INTERVAL){
            cntIntervalTimesInfoSpinDown = 0;
            mLcb.getLcbIndoorBikeInfoSpinDown(LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesInfoSpinDown++;
        }
    }	
	
	
//<===Hanler LCB Auto Heart Beat

    /*******************************************************************************
     * Function Name  : initialIndoorBike()
     * Description    : initial IndoorBike  
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void initialIndoorBike() {

        mLcb = LcbComm.getInstance();
	    mGeneralHandler = GeneralHandler.getInstance();
        mDeviceLcb = DeviceDataTransmitted.getInstance();
        mLcb.addReceiveListener(mIndoorBikeLcbCommReceive);
        mDeviceLcb.addReceiveListener(mIndoorBikeLcbCommReceive);
	    timerStartAutoHeartBeatThread();
		
        _isAutoReplyRpm = false;
        _isAutoReplyWatts = false;
        _isAutoReplySensorData = false;
        _isAutoReplyInfoSpinDown = false;

    }

    /*******************************************************************************
     * Function Name  : unInitialIndoorBike()
     * Description    : uninitial IndoorBike  
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void unInitialIndoorBike() {

        stopAutoHeartBeatThread();
        mLcb.removeReceiveListener(mIndoorBikeLcbCommReceive);
        mLcb = null;
        mDeviceLcb = null;
    }

    /*******************************************************************************
     * Function Name  : setIndoorBikeHandlerCallback()
     * Description    : set IndoorBikeHandler Callback
     * Input          : IIndoorBikeHandlerCallback IndoorBikeHandlerCallback
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setIndoorBikeHandlerCallback(IIndoorBikeHandlerCallback indoorBikeHandlerCallback) {
	if(indoorBikeHandlerCallback != null)
        mIndoorBikeHandlerCallback = indoorBikeHandlerCallback;
    }

    /*******************************************************************************
     * Function Name  : unsetIndoorBikeHandlerCallback()
     * Description    : unset IndoorBikeHandler Callback
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void unsetIndoorBikeHandlerCallback() {
        mIndoorBikeHandlerCallback = null;
    }


    /*******************************************************************************
     * Function Name  : setAutoReplyIndoorBikeRpm()
     * Description    : set IndoorBike AutoReply IndoorBike RPM
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyIndoorBikeRpm(boolean enable)
    {
    	if(enable)
		_isAutoReplyRpm = true;
	else
		_isAutoReplyRpm = false;
    }

    public void setAutoReplyGetWatts(boolean enable) {
        if (enable)
            _isAutoReplyWatts = true;
        else
            _isAutoReplyWatts = false;
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyIndoorBikeSensorData()
     * Description    : set IndoorBike AutoReply IndoorBike SensorData
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyIndoorBikeSensorData(boolean enable)
    {
    	if(enable)
		_isAutoReplySensorData = true;
	else
		_isAutoReplySensorData = false;
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyIndoorBikeInfoSpinDown()
     * Description    : set IndoorBike AutoReply Info Spin Down
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyIndoorBikeInfoSpinDown(boolean enable)
    {
    	if(enable)
		_isAutoReplyInfoSpinDown = true;
	else
		_isAutoReplyInfoSpinDown = false;
    }

    /*******************************************************************************
     * Function Name  : setIndoorBikeGearRatio()
     * Description    : set IndoorBike GearRatio
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setIndoorBikeGearRatio(int intValue)
    {
        if (mLcb != null){
	        mLcb.setLcbRPMgearRatio(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setIndoorBikeResistanceType()
     * Description    : set IndoorBike ResistanceType
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setIndoorBikeResistanceType(int intValue)
    {
        if (mLcb != null){
	        mLcb.setLcbResistanceType(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setIndoorBikeLowLimitResistance()
     * Description    : set IndoorBike LowLimitResistance
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setIndoorBikeLowLimitResistance(int intValue)
    {
        if (mLcb != null){
	        mLcb.setLcbLimitRPMresistance(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setIndoorBikeLowLimitCharge()
     * Description    : set IndoorBike LowLimitCharge
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setIndoorBikeLowLimitCharge(int intValue)
    {
        if (mLcb != null){
	        mLcb.setLcbLimitRPMcharge(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setIndoorBikeInclineAction()
     * Description    : set IndoorBike Turn Off Lcb
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setIndoorBikeTurnOffLcb(int intValue)
    {
        if (mLcb != null){
	        mLcb.setLcbPowerOff(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : getIndoorBikeOneShotReplyRpm()
     * Description    : get IndoorBike One Shot Reply Rpm
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getIndoorBikeOneShotReplyRpm()
    {
        if (mLcb != null){
	        mLcb.getOneShotLcbECBrpmHighPriority();
        }
    }

    /*******************************************************************************
     * Function Name  : getIndoorBikeOneShotReplySensorData()
     * Description    : get IndoorBike One Shot Reply SensorData
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getIndoorBikeOneShotReplySensorData()
    {
        if (mLcb != null){
	        mLcb.getOneShotLcbIndoorBikeSensorDataHighPriority();
        }
    }

    /*******************************************************************************
     * Function Name  : setLcbIndoorBikeStepperMotorPosition()
     * Description    : set IndoorBike Stepper Motor Position
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setLcbIndoorBikeStepperMotorPosition(byte[] bytesCmdData)
    {
        if (mLcb != null){
	        mLcb.setLcbIndoorBikeStepperMotorPosition(bytesCmdData);
        }
    }
    /*******************************************************************************
     * Function Name  : setLcbIndoorBikeCalcParaOfDevice()
     * Description    : set IndoorBike calculation parameters of device or algorithm
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setLcbIndoorBikeCalcParaOfDevice(byte[] bytesCmdData)
    {
        if (mLcb != null){
	        mLcb.setLcbIndoorBikeCalcParaOfDevice(bytesCmdData);
        }
    }

    /*******************************************************************************
     * Function Name  : getLcbIndoorBikeCalcParaOfDevice()
     * Description    : get IndoorBike One Shot Reply SensorData
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getLcbIndoorBikeCalcParaOfDevice()
    {
        if (mLcb != null){
	        mLcb.getLcbIndoorBikeCalcParaOfDevice();
        }
    }

    /*******************************************************************************
     * Function Name  : setLcbIndoorBikeActionSpinDown()
     * Description    : Set the action of spin down 
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setLcbIndoorBikeActionSpinDown(byte[] bytesData )
    {
        if (mLcb != null){
	        mLcb.setLcbIndoorBikeActionSpinDown(bytesData);
        }
    }

    /*******************************************************************************
     * Function Name  : getLcbIndoorBikeInfoSpinDown()
     * Description    : get the info of spin down
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getLcbIndoorBikeInfoSpinDown()
    {
        if (mLcb != null){
	        mLcb.getLcbIndoorBikeInfoSpinDown(LcbComm.QueueMode.AlwaysAdd);
        }
    }

    public void requestRawAdc(int index)
    {
        if (mLcb != null){
            if (isIcr()) mLcb.getLcbDCIinclineRange(); // ICR re-uses 0xA0 command for reading raw ADC
        }
    }

    /*******************************************************************************
     * Function Name  : setLcbIndoorBikeErgModeSettings()
     * Description    : Set Erg Mode Settings
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setLcbIndoorBikeErgModeSettings(byte[] bytesData )
    {
        if (mLcb != null){
	        mLcb.setLcbIndoorBikeErgModeSettings(bytesData);
        }
    }

    /*******************************************************************************
     * Function Name  : getLcbIndoorBikeErgModeSettings()
     * Description    : get Erg Mode Settings
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getLcbIndoorBikeErgModeSettings()
    {
        if (mLcb != null){
	        mLcb.getLcbIndoorBikeErgModeSettings();
        }
    }

    /*******************************************************************************
     * Function Name  : setLcbIndoorBikeResistanceLevel(int intValue )
     * Description    : set IndoorBike Resistance Level
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setLcbIndoorBikeResistanceLevel(int intValue )
    {
        if (mLcb != null){
            //Log.d (TAG, " set resistance=" + intValue);
            mLcb.setLcbIndoorBikeResistanceLevel(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setErgWatts(int intValue )
     * Description    : set Constant Watts Target Value (in watts)
     * Input          : target watts - type integers
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setLcbIndoorBikeErgWatts(int targetWatts) {
        if (mLcb != null) {
            Log.d (TAG, " target watts=" + targetWatts);
            mLcb.setLcbWatts(targetWatts, LcbComm.QueueMode.AlwaysAdd);
        }
    }

    public boolean isIcr() {
        if (mGeneralHandler != null)
            machineLcbType = mGeneralHandler.getMachineLcbType();
        return machineLcbType == LcbType.LCB_JIS_ATHENA;
    }
}
