package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Bike;

import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IDeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ILcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.GeneralHandler;

public class BikeHandler {
    private static final String TAG = "BikeHandler";


    protected ILcbComm mLcb;
    protected IDeviceDataTransmitted mDeviceLcb;
    private ILcbComm mLcbComm;
    private BikeCallbackHandlerLCB mBikeLcbCommReceive = new BikeCallbackHandlerLCB(this);

    protected IBikeHandlerCallback mBikeHandlerCallback;

    private GeneralHandler mGeneralHandler;
    private LcbType machineLcbType = LcbType.LCB_UNKNOWN;

    //Handler Auto Heart Beat ===>
    private volatile boolean _isRunning = false;
    private static final Object _notifyHeartBeatSync = new Object();
    private static Thread _heartBeatThread = null;
    private static final int HEART_BEAT_TIMER_PERIOD = 400;
    private static volatile JHTTimer mStartHeartBeatTimer;

    //get rpm used
    private volatile boolean _isAutoReplyRpm = false;
    private static final int TIMES_RPM_INTERVAL = 0; // 0 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesRpm = 0; //HEART_BEAT_TIMER_PERIOD x times

    //set resistance rpm used
    private int mResendWatt = 0;
    private volatile boolean _isAutoResendWatt = false;
    private static final int TIMES_SEND_WATT_INTERVAL = 0; // 0 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesSendWatt = 0; //HEART_BEAT_TIMER_PERIOD x times
//<=== Hanler Auto Heart Beat 

    private BikeHandler() {
    }

    private static class SingletonHolder {
        private static BikeHandler Instance = new BikeHandler();
    }

    public static BikeHandler getInstance() {
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
        mStartHeartBeatTimer.start("BikeHbTimer");
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
    private void stopAutoHeartBeatThread() {
        if (_isRunning) {
            _isRunning = false;
            if(_heartBeatThread != null) {
                _heartBeatThread.interrupt();
            }

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
        HeartBeatThread() { super("BikeHbThread"); }
        @Override
        public void run() {
            super.run();
            Log.v(TAG, "Bike HeartBeatThread++++");

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
                Log.e(TAG, "Exception in Bike heartBeat thread! ", ex);
                _isRunning = false;
                //re-start HeartBeatThread
                Log.v(TAG, "Re-start Bike HeartBeatThread . ");
                timerStartAutoHeartBeatThread();
            }

            Log.v(TAG, "Bike HeartBeatThread----");
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
        if (_isAutoReplyRpm)
            autoReplyRpm();
        if (_isAutoResendWatt)
            autoResendWatt();
    }

    /*******************************************************************************
     * Function Name  : autoReplyRpm()
     * Description    : process Heart Beat to AutoReply LCB Ascent Rpm
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyRpm() {
        if (cntIntervalTimesRpm == TIMES_RPM_INTERVAL) {
            cntIntervalTimesRpm = 0;
            mLcb.getLcbECBrpm(LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesRpm++;
        }
    }

    /*******************************************************************************
     * Function Name  : autoResendWatt()
     * Description    : process Heart Beat to AutoResend LCB Bike watt
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoResendWatt() {
        if (cntIntervalTimesSendWatt == TIMES_SEND_WATT_INTERVAL) {
            cntIntervalTimesSendWatt = 0;
            mLcb.setLcbWatts(mResendWatt, LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesSendWatt++;
        }
    }

//<===Hanler LCB Auto Heart Beat

    /*******************************************************************************
     * Function Name  : initialBike()
     * Description    : initial Bike  
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void initialBike() {

        mLcb = LcbComm.getInstance();
        mGeneralHandler = GeneralHandler.getInstance();
        mDeviceLcb = DeviceDataTransmitted.getInstance();
        mLcb.addReceiveListener(mBikeLcbCommReceive);
        mDeviceLcb.addReceiveListener(mBikeLcbCommReceive);
        timerStartAutoHeartBeatThread();

        _isAutoReplyRpm = false;
        _isAutoResendWatt = false;
    }

    /*******************************************************************************
     * Function Name  : unInitialBike()
     * Description    : uninitial Bike  
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void unInitialBike() {

        stopAutoHeartBeatThread();
        mLcb.removeReceiveListener(mBikeLcbCommReceive);
        mLcb = null;
        mDeviceLcb = null;
    }

    /*******************************************************************************
     * Function Name  : setBikeHandlerCallback()
     * Description    : set BikeHandler Callback
     * Input          : IBikeHandlerCallback bikeHandlerCallback
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setBikeHandlerCallback(IBikeHandlerCallback bikeHandlerCallback) {
        if (bikeHandlerCallback != null)
            mBikeHandlerCallback = bikeHandlerCallback;
    }

    /*******************************************************************************
     * Function Name  : unsetBikeHandlerCallback()
     * Description    : unset BikeHandler Callback
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void unsetBikeHandlerCallback() {
        mBikeHandlerCallback = null;
    }

    /*******************************************************************************
     * Function Name  : setBikeWattOrResistance()
     * Description    : set Bike Resistance
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setBikeWattOrResistance(int intValue) {
        //Log.d("LCBTrace", "setBikeWattOrResistance " + intValue + " " + machineLcbType);
        if (mLcb != null) {
            if (mGeneralHandler != null)
                machineLcbType = mGeneralHandler.getMachineLcbType();


            switch (machineLcbType) {
                case LCB_LCB1_Current:
                case LCB_LCB3_Current: {
                    //Log.d("LCBTrace", "setLcbPWMpercent " + intValue + " " + machineLcbType);
                    mLcb.setLcbPWMpercent(intValue);
                }
                break;
                case LCB_Performance_Bike_DC36V:
                case LCB_LCBA:
                case LCB_1x_Bike_LCB: {
                    //Log.d("LCBTrace", "setLcbWatts " + intValue + " " + machineLcbType);
                    mLcb.setLcbWatts(intValue, LcbComm.QueueMode.ReplaceSimilar);
                    mResendWatt = intValue;
                }
                break;
                case LCB_ECB_Board:
                    mLcb.setLcbECBposition(intValue);
                    //if (intValue == 0) mLcb.setLcbResetECB(); // is ECB reset needed?
                break;
                default:
                    mLcb.setLcbPWMpercent(intValue);
                    return;
            }
        }
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyBikeRpm()
     * Description    : set Bike AutoReply Bike RPM
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyBikeRpm(boolean enable) {
        if (enable)
            _isAutoReplyRpm = true;
        else
            _isAutoReplyRpm = false;
    }

    /*******************************************************************************
     * Function Name  : setAutoResendBikeWatt()
     * Description    : set Bike Auto Resend Bike resistance watt
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoResendBikeWatt(boolean enable) {
        if (enable)
            _isAutoResendWatt = true;
        else
            _isAutoResendWatt = false;
    }

    /*******************************************************************************
     * Function Name  : setBikeGearRatio()
     * Description    : set Bike GearRatio
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setBikeGearRatio(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbRPMgearRatio(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setBikeResistanceType()
     * Description    : set Bike ResistanceType
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setBikeResistanceType(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbResistanceType(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setBikeLowLimitResistance()
     * Description    : set Bike LowLimitResistance
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setBikeLowLimitResistance(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbLimitRPMresistance(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setBikeLowLimitCharge()
     * Description    : set Bike LowLimitCharge
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setBikeLowLimitCharge(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbLimitRPMcharge(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setBikeInclineAction()
     * Description    : set Bike Turn Off Lcb
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setBikeTurnOffLcb(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbPowerOff(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : getBikeOneShotReplyRpm()
     * Description    : get Bike One Shot Reply Rpm
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getBikeOneShotReplyRpm() {
        if (mLcb != null) {
            mLcb.getOneShotLcbECBrpmHighPriority();
        }
    }

}
