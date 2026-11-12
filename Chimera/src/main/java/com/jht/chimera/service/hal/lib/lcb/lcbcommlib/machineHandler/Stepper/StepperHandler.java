package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Stepper;

import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IDeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ILcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.GeneralHandler;

public class StepperHandler {
    private static final String TAG = "StepperHandler";


    protected ILcbComm mLcb;
    protected IDeviceDataTransmitted mDeviceLcb;
    private ILcbComm mLcbComm;
    private StepperCallbackHandlerLCB mStepperLcbCommReceive = new StepperCallbackHandlerLCB(this);

    protected IStepperHandlerCallback mStepperHandlerCallback;

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

    private StepperHandler() {
    }

    private static class SingletonHolder {
        private static StepperHandler Instance = new StepperHandler();
    }

    public static StepperHandler getInstance() {
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
        mStartHeartBeatTimer.start("StepHbTimer");
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
        HeartBeatThread() { super("StepHbThread"); }
        @Override
        public void run() {
            super.run();
            Log.v(TAG, "Stepper HeartBeatThread++++");

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
                Log.e(TAG, "Exception in Stepper heartBeat thread! ", ex);
                _isRunning = false;
                //re-start HeartBeatThread
                Log.v(TAG, "Re-start Stepper HeartBeatThread . ");
                timerStartAutoHeartBeatThread();
            }

            Log.v(TAG, "Stepper HeartBeatThread----");
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
     * Description    : process Heart Beat to AutoReply LCB Stepper( Rpm
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
     * Function Name  : initialStepper()
     * Description    : initial Stepper  
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void initialStepper() {

        mLcb = LcbComm.getInstance();
        mGeneralHandler = GeneralHandler.getInstance();
        mDeviceLcb = DeviceDataTransmitted.getInstance();
        mLcb.addReceiveListener(mStepperLcbCommReceive);
        mDeviceLcb.addReceiveListener(mStepperLcbCommReceive);
        timerStartAutoHeartBeatThread();

        _isAutoReplyRpm = false;
    }

    /*******************************************************************************
     * Function Name  : unInitialStepper()
     * Description    : uninitial Stepper  
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void unInitialStepper() {

        stopAutoHeartBeatThread();
        mLcb.removeReceiveListener(mStepperLcbCommReceive);
        mLcb = null;
        mDeviceLcb = null;
    }

    /*******************************************************************************
     * Function Name  : setStepperHandlerCallback()
     * Description    : set StepperHandler Callback
     * Input          : IStepperHandlerCallback stepperHandlerCallback
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setStepperHandlerCallback(IStepperHandlerCallback stepperHandlerCallback) {
        if (stepperHandlerCallback != null)
            mStepperHandlerCallback = stepperHandlerCallback;
    }

    /*******************************************************************************
     * Function Name  : unsetStepperHandlerCallback()
     * Description    : unset StepperHandler Callback
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void unsetStepperHandlerCallback() {
        mStepperHandlerCallback = null;
    }

    /*******************************************************************************
     * Function Name  : setStepperResistance()
     * Description    : set Stepper Resistance
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setStepperResistance(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbLimitRPMresistance(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyStepperRpm()
     * Description    : set Stepper AutoReply Stepper RPM
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyStepperRpm(boolean enable) {
        if (enable)
            _isAutoReplyRpm = true;
        else
            _isAutoReplyRpm = false;
    }


    /*******************************************************************************
     * Function Name  : setStepperGearRatio()
     * Description    : set Stepper GearRatio
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setStepperGearRatio(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbRPMgearRatio(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setStepperResistanceType()
     * Description    : set Stepper ResistanceType
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setStepperResistanceType(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbResistanceType(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setStepperInclineAction()
     * Description    : set Stepper Turn Off Lcb
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setStepperTurnOffLcb(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbPowerOff(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : getStepperOneShotReplyRpm()
     * Description    : get Stepper One Shot Reply Rpm
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getStepperOneShotReplyRpm() {
        if (mLcb != null) {
            mLcb.getOneShotLcbECBrpmHighPriority();
        }
    }

}
