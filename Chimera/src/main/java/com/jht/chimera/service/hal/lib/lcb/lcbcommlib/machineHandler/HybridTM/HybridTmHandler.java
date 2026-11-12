package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.HybridTM;

import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.service.hal.lib.frictiontest.FrictionTestManagerKt;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IDeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ILcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.GeneralHandler;

public class HybridTmHandler {
    private static final String TAG = "HybridTmHandler";


    protected ILcbComm mLcb;
    protected IDeviceDataTransmitted mDeviceLcb;
    private ILcbComm mLcbComm;
    private HybridTmCallbackHandlerLCB mHybridTmLcbCommReceive = new HybridTmCallbackHandlerLCB(this);

    protected IHybridTmHandlerCallback mHybridTmHandlerCallback;

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

    //get inclone used
    private volatile boolean _isAutoReplyIncline = false;
    private static final int TIMES_INCLINE_INTERVAL = 1; // 1 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesIncline = 0; //HEART_BEAT_TIMER_PERIOD x times

    //get InUsed used
    private volatile boolean _isAutoReplyInUsed = false;
    private static final int TIMES_INUSED_INTERVAL = 7; // 7 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesInUsed = 0; //HEART_BEAT_TIMER_PERIOD x times
    public int mMachineRpm = 0;

    //get MotorInfo used
    private volatile boolean _isAutoReplyMotorInfo = false;
    private static final int TIMES_MOTOR_INFO_INTERVAL = 2; // 2 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesMotorInfo = 0; //HEART_BEAT_TIMER_PERIOD x times

    //set RPM no response 00A1 used
    public volatile boolean mTargetRpmCheckFlag = false;
    private static final int TIMES_TARGET_RPM_INTERVAL = 100; // 100 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesTargetRpm = 0; //HEART_BEAT_TIMER_PERIOD x times
//<=== Hanler Auto Heart Beat 

    private HybridTmHandler() {
    }

    private static class SingletonHolder {
        private static HybridTmHandler Instance = new HybridTmHandler();
    }

    public static HybridTmHandler getInstance() {
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
        mStartHeartBeatTimer.start("HyTmHbTimer");
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
        HeartBeatThread() { super("HyTmHbThread"); }
        @Override
        public void run() {
            super.run();
            Log.v(TAG, "HybridTm HeartBeatThread++++");

            _isRunning = true;
            try {
                while (_isRunning && !isInterrupted()) {
                    synchronized (_notifyHeartBeatSync) {
                        _notifyHeartBeatSync.wait(HEART_BEAT_TIMER_PERIOD);
                        if (_isRunning) {
                            if (!FrictionTestManagerKt.isSendingData()) {
                                //auto reply cmd
                                autoReplyCmd();
                            }
                        }
                    }

                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                Log.e(TAG, "Exception in HybridTm heartBeat thread! ", ex);
                _isRunning = false;
                //re-start HeartBeatThread
                Log.v(TAG, "Re-start HybridTm HeartBeatThread . ");
                timerStartAutoHeartBeatThread();
            }

            Log.v(TAG, "HybridTm HeartBeatThread----");
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
        autoReplyHeartBeatStatus();

        if (_isAutoReplyRpm)
            autoReplyRpm();

        if (_isAutoReplyIncline)
            autoReplyIncline();

        if (_isAutoReplyInUsed)
            autoReplyInUsed();

        if (_isAutoReplyMotorInfo)
            autoReplyMotorInfo();

        //set RPM no response check
        if (mTargetRpmCheckFlag)
            targetRpmStatusCheck();

    }

    /*******************************************************************************
     * Function Name  : autoReplyHeartBeatStatus()
     * Description    : process Heart Beat to AutoReply LCB status
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyHeartBeatStatus() {
        mLcb.getLcbStatus(LcbComm.QueueMode.ReplaceSimilar);
    }

    /*******************************************************************************
     * Function Name  : autoReplyRpm()
     * Description    : process Heart Beat to AutoReply LCB HybridTM Rpm
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyRpm() {
        if (cntIntervalTimesRpm == TIMES_RPM_INTERVAL) {
            cntIntervalTimesRpm = 0;
            mLcb.getLcbMotorRPM(LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesRpm++;
        }
    }

    /*******************************************************************************
     * Function Name  : autoReplyIncline()
     * Description    : process Heart Beat to AutoReply LCB HybridTm Incline
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyIncline() {
        if (cntIntervalTimesIncline == TIMES_INCLINE_INTERVAL) {
            cntIntervalTimesIncline = 0;
            mLcb.getLcbInclineLoc(LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesIncline++;
        }
    }

    /*******************************************************************************
     * Function Name  : autoReplyInUsed()
     * Description    : process Heart Beat to AutoReply LCB TreadMill InUsed
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyInUsed() {
        if (mMachineRpm > 100) {
            if (cntIntervalTimesInUsed == TIMES_INUSED_INTERVAL) {
                cntIntervalTimesInUsed = 0;
                mLcb.getLcbTreadmillInUsed(LcbComm.QueueMode.ReplaceSimilar);
            } else {
                cntIntervalTimesInUsed++;
            }
        }
    }

    /*******************************************************************************
     * Function Name  : autoReplyMotorInfo()
     * Description    : process Heart Beat to AutoReply LCB TreadMill MotorInfo
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyMotorInfo() {
        if (cntIntervalTimesMotorInfo == TIMES_MOTOR_INFO_INTERVAL) {
            cntIntervalTimesMotorInfo = 0;
            mLcb.getLcbMainMotorInfo(LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesMotorInfo++;
        }
    }

    /*******************************************************************************
     * Function Name  : targetRpmStatusCheck()
     * Description    : process set RPM no response 00A1 used
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void targetRpmStatusCheck() {
        if (mTargetRpmCheckFlag) {
            if (mMachineRpm == 0) {
                if (cntIntervalTimesTargetRpm == TIMES_TARGET_RPM_INTERVAL) {
                    mTargetRpmCheckFlag = false;
                    cntIntervalTimesTargetRpm = 0;
                    mLcb.procTmNoRpmError();
                } else {
                    cntIntervalTimesTargetRpm++;
                }
            }
        } else {
            cntIntervalTimesTargetRpm = 0;
        }
    }

//<===Hanler LCB Auto Heart Beat

    /*******************************************************************************
     * Function Name  : initialHybridTm()
     * Description    : initial HybridTm 
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void initialHybridTm() {

        mLcb = LcbComm.getInstance();
        mGeneralHandler = GeneralHandler.getInstance();
        mDeviceLcb = DeviceDataTransmitted.getInstance();
        mLcb.addReceiveListener(mHybridTmLcbCommReceive);
        mDeviceLcb.addReceiveListener(mHybridTmLcbCommReceive);
        timerStartAutoHeartBeatThread();

        _isAutoReplyRpm = false;
        _isAutoReplyIncline = false;
        _isAutoReplyInUsed = false;
        _isAutoReplyMotorInfo = false;
    }

    /*******************************************************************************
     * Function Name  : unInitialHybridTm()
     * Description    : uninitial HybridTm 
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void unInitialHybridTm() {

        stopAutoHeartBeatThread();
        mLcb.removeReceiveListener(mHybridTmLcbCommReceive);
        mLcb = null;
        mDeviceLcb = null;
    }

    /*******************************************************************************
     * Function Name  : setHybridTmHandlerCallback()
     * Description    : set HybridTmHandler Callback
     * Input          : IHybridTmHandlerCallback hybridTmHandlerCallback
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setHybridTmHandlerCallback(IHybridTmHandlerCallback hybridTmHandlerCallback) {
        mHybridTmHandlerCallback = hybridTmHandlerCallback;
    }

    /*******************************************************************************
     * Function Name  : setHybridTmRPM()
     * Description    : set HybridTm Resistance
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setHybridTmRPM(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbMotorRPM(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setHybridTmIncline()
     * Description    : set HybridTm Incline
     * Input          : int intLoc
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setHybridTmIncline(int intLoc) {
        if (mLcb != null) {
            mLcb.setLcbInclineLoc(intLoc);
        }
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyHybridTmRpm()
     * Description    : set HybridTm AutoReply HybridTm RPM
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyHybridTmRpm(boolean enable) {
        if (enable)
            _isAutoReplyRpm = true;
        else
            _isAutoReplyRpm = false;
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyHybridTmincline()
     * Description    : set HybridTm AutoReply HybridTm Incline
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyHybridTmIncline(boolean enable) {
        if (enable)
            _isAutoReplyIncline = true;
        else
            _isAutoReplyIncline = false;
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyHybridTmInUsed()
     * Description    : set HybridTm AutoReply HybridTm InUsed
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyHybridTmInUsed(boolean enable) {
        if (enable)
            _isAutoReplyInUsed = true;
        else
            _isAutoReplyInUsed = false;
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyHybridTmMotorInfo()
     * Description    : set HybridTm AutoReply HybridTm MotorInfo
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyHybridTmMotorInfo(boolean enable) {
        if (enable)
            _isAutoReplyMotorInfo = true;
        else
            _isAutoReplyMotorInfo = false;
    }

    /*******************************************************************************
     * Function Name  : setHybridTmInclineAction()
     * Description    : set HybridTm Incline Action
     * Input          : byte byteValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setHybridTmInclineAction(byte byteValue) {
        if (mLcb != null) {
            mLcb.setLcbInclineAction(byteValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setHybridTmMotorType()
     * Description    : set HybridTm Motor Type
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setHybridTmMotorType(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbMotorSpecification(intValue);
        }
    }


}
