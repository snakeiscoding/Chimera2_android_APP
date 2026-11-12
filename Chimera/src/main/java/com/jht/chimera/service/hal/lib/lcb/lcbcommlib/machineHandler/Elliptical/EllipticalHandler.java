package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Elliptical;

import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IDeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ILcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.GeneralHandler;

public class EllipticalHandler {
    private static final String TAG = "EllipticalHandler";


    protected ILcbComm mLcb;
    protected IDeviceDataTransmitted mDeviceLcb;
    private ILcbComm mLcbComm;
    private EllipticalCallbackHandlerLCB mEllipticalLcbCommReceive = new EllipticalCallbackHandlerLCB(this);

    protected IEllipticalHandlerCallback mEllipticalHandlerCallback;

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
//<=== Hanler Auto Heart Beat 

    private EllipticalHandler() {
    }

    private static class SingletonHolder {
        private static EllipticalHandler Instance = new EllipticalHandler();
    }

    public static EllipticalHandler getInstance() {
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
        mStartHeartBeatTimer.start("EpHbTimer");
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
        HeartBeatThread() { super("EpHbThread"); }
        @Override
        public void run() {
            super.run();
            Log.v(TAG, "Elliptical HeartBeatThread++++");

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
                Log.e(TAG, "Exception in Elliptical heartBeat thread! ", ex);
                _isRunning = false;
                //re-start HeartBeatThread
                Log.v(TAG, "Re-start Elliptical HeartBeatThread . ");
                timerStartAutoHeartBeatThread();
            }

            Log.v(TAG, "Elliptical HeartBeatThread----");
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
    }

    /*******************************************************************************
     * Function Name  : autoReplyRpm()
     * Description    : process Heart Beat to AutoReply LCB Elliptical Rpm
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

//<===Hanler LCB Auto Heart Beat

    /*******************************************************************************
     * Function Name  : initialElliptical()
     * Description    : initial Elliptical  
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void initialElliptical() {

        mLcb = LcbComm.getInstance();
        mGeneralHandler = GeneralHandler.getInstance();
        mDeviceLcb = DeviceDataTransmitted.getInstance();
        mLcb.addReceiveListener(mEllipticalLcbCommReceive);
        mDeviceLcb.addReceiveListener(mEllipticalLcbCommReceive);
        timerStartAutoHeartBeatThread();

        _isAutoReplyRpm = false;

    }

    /*******************************************************************************
     * Function Name  : unInitialElliptical()
     * Description    : uninitial Elliptical  
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void unInitialElliptical() {

        stopAutoHeartBeatThread();
        mLcb.removeReceiveListener(mEllipticalLcbCommReceive);
        mLcb = null;
        mDeviceLcb = null;
    }

    /*******************************************************************************
     * Function Name  : setEllipticalHandlerCallback()
     * Description    : set EllipticalHandler Callback
     * Input          : IEllipticalHandlerCallback ellipticalHandlerCallback
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setEllipticalHandlerCallback(IEllipticalHandlerCallback ellipticalHandlerCallback) {
        if (ellipticalHandlerCallback != null)
            mEllipticalHandlerCallback = ellipticalHandlerCallback;
    }

    /*******************************************************************************
     * Function Name  : unsetEllipticalHandlerCallback()
     * Description    : unset EllipticalHandler Callback
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void unsetEllipticalHandlerCallback() {
        mEllipticalHandlerCallback = null;
    }

    /*******************************************************************************
     * Function Name  : setEllipticalResistance()
     * Description    : set Elliptical Resistance
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setEllipticalResistance(int intValue) {
        if (mLcb != null) {
            if (mGeneralHandler != null)
                machineLcbType = mGeneralHandler.getMachineLcbType();
            switch (machineLcbType) {
                case LCB_ECB_Board:
                    mLcb.setLcbECBposition(intValue);
                    //if (intValue == 0) mLcb.setLcbResetECB(); // is ECB reset needed?
                    break;
                default:
                    mLcb.setLcbPWMpercent(intValue);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyBikeRpm()
     * Description    : set Elliptical AutoReply Bike RPM
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyEllipticalRpm(boolean enable) {
        if (enable)
            _isAutoReplyRpm = true;
        else
            _isAutoReplyRpm = false;
    }

    /*******************************************************************************
     * Function Name  : setEllipticalGearRatio()
     * Description    : set Elliptical GearRatio
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setEllipticalGearRatio(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbRPMgearRatio(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setEllipticalResistanceType()
     * Description    : set Elliptical ResistanceType
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setEllipticalResistanceType(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbResistanceType(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setEllipticalLowLimitResistance()
     * Description    : set Elliptical LowLimitResistance
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setEllipticalLowLimitResistance(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbLimitRPMresistance(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setEllipticalLowLimitCharge()
     * Description    : set Elliptical LowLimitCharge
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setEllipticalLowLimitCharge(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbLimitRPMcharge(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setEllipticalInclineAction()
     * Description    : set Elliptical Turn Off Lcb
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setEllipticalTurnOffLcb(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbPowerOff(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : getEllipticalOneShotReplyRpm()
     * Description    : get Elliptical One Shot Reply Rpm
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getEllipticalOneShotReplyRpm() {
        if (mLcb != null) {
            mLcb.getOneShotLcbECBrpmHighPriority();
        }
    }

}
