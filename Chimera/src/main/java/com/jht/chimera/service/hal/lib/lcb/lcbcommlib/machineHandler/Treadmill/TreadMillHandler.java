package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Treadmill;

import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.service.hal.lib.frictiontest.FrictionTestManagerKt;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IDeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ILcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.GeneralHandler;

public class TreadMillHandler {
    private static final String TAG = "TreadMillHandler";


    protected ILcbComm mLcb = null;
    protected IDeviceDataTransmitted mDeviceLcb = null;
    private ILcbComm mLcbComm;
    private TreadMillCallbackHandlerLCB mTreadMillLcbCommReceive = new TreadMillCallbackHandlerLCB(this);

    protected ITreadMillHandlerCallback mTreadMillHandlerCallback = null;

    private GeneralHandler mGeneralHandler;
    private LcbType machineLcbType = LcbType.LCB_UNKNOWN;

    //Handler Auto Heart Beat ===>
    private volatile boolean _isRunning = false;
    private static final Object _notifyHeartBeatSync = new Object();
    private static Thread _heartBeatThread = null;
    private static final int HEART_BEAT_TIMER_PERIOD = 400;
    private static volatile JHTTimer mStartHeartBeatTimer;

    //get Treadmill Ext status used
    private volatile boolean _isAutoReplyTreadmillExtStatus = false;
    private static final int TIMES_TREADMILL_EXT_STATUS_INTERVAL = 0; // 0 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesTreadmillExtStatus = 0; //HEART_BEAT_TIMER_PERIOD x times

    //get rpm used
    private volatile boolean _isAutoReplyRpm = false;
    private static final int TIMES_RPM_INTERVAL = 0; // 0 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesRpm = 0; //HEART_BEAT_TIMER_PERIOD x times

    //get cadence used
    private volatile boolean _isAutoReplyCadence = false;
    private static final int TIMES_CADENCE_INTERVAL = 3; // 3 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesCadence = 0; //HEART_BEAT_TIMER_PERIOD x times

    //get inclone used
    private volatile boolean _isAutoReplyIncline = false;
    private static final int TIMES_INCLINE_INTERVAL = 1; // 1 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesIncline = 0; //HEART_BEAT_TIMER_PERIOD x times

    //get InUsed used
    private volatile boolean _isAutoReplyInUsed = false;
    private static final int TIMES_INUSED_INTERVAL = 7; // 7 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesInUsed = 0; //HEART_BEAT_TIMER_PERIOD x times
    public int mMachineRpm = 0;

    //speed up InUsed cmd used
    private volatile boolean _isAutoReplySpeedUpInUsedCmd = false;
    private static final int TIMES_SPEEDUP_INUSED_CMD_INTERVAL = 3; // 3 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesSpeedUpInUsedCmd = 0; //HEART_BEAT_TIMER_PERIOD x times

    //get MotorInfo used
    private volatile boolean _isAutoReplyMotorInfo = false;
    private static final int TIMES_MOTOR_INFO_INTERVAL = 3; // 3 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesMotorInfo = 0; //HEART_BEAT_TIMER_PERIOD x times

    //set RPM no response 00A1 used
    public volatile boolean mTargetRpmCheckFlag = false;
    private static final int TIMES_TARGET_RPM_INTERVAL = 100; // 100 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesTargetRpm = 0; //HEART_BEAT_TIMER_PERIOD x times

	//TM Speed ramp and contiune last rpm
    private volatile boolean _isAutoSetRampUpAndLastRpmValueCmd = false;
    private static final int TIMES_SET_RAMP_UP_AND_LAST_RPM_INTERVAL = 1; // 2 HEART_BEAT_TIMER_PERIOD Interval 800ms
    private int cntIntervalTimesSetRampUpAndLastRpm = 0; //HEART_BEAT_TIMER_PERIOD x times
    private int mGUISetRpmValue = 0;
    private int mSetCurrentRpmValue = 0;
    private int mRpmRampUpStageValue = 250; //default
    private int mRpmRampDownStageValue = 250; //default

    //Hybrid TM get Ir Addr Ir Status
    private volatile boolean _isAutoReplyIrAddrIrStatus = false;
    private static final int TIMES_IR_ADDR_IR_STATUS_INTERVAL = 1; // 1 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesIrAddrIrStatus = 0; //HEART_BEAT_TIMER_PERIOD x times

//<=== Hanler Auto Heart Beat 

    private TreadMillHandler() {
    }

    private static class SingletonHolder {
        private static TreadMillHandler Instance = new TreadMillHandler();
    }

    public static TreadMillHandler getInstance() {
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
        mStartHeartBeatTimer.start("TmHbTimer");
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
        HeartBeatThread() { super("TmHbThread"); }
        @Override
        public void run() {
            super.run();
            Log.v(TAG, "TreadMill HeartBeatThread++++");

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
                Log.e(TAG, "Exception in TreadMill heartBeat thread! ", ex);
                _isRunning = false;
                //re-start HeartBeatThread
                Log.v(TAG, "Re-start TreadMill HeartBeatThread . ");
                timerStartAutoHeartBeatThread();
            }

            Log.v(TAG, "TreadMill HeartBeatThread----");
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

        if (_isAutoReplyTreadmillExtStatus)
	    	autoReplyTreadmillExtStatus();

        if (_isAutoReplyRpm)
            autoReplyRpm();

        if (_isAutoReplyCadence)
            autoReplyCadence();

        if (_isAutoReplyIncline)
            autoReplyIncline();

        if (_isAutoReplyInUsed)
            autoReplyInUsed();

        if (_isAutoReplySpeedUpInUsedCmd)
            autoReplySpeedUpInUsedCmd();

        if (_isAutoReplyMotorInfo)
            autoReplyMotorInfo();

        if (_isAutoSetRampUpAndLastRpmValueCmd) {
            autoReplySetRampUpAndLstRpmValueCmd();
        }


        //set RPM no response check
        if (mTargetRpmCheckFlag)
            targetRpmStatusCheck();

        if (_isAutoReplyIrAddrIrStatus)
            autoReplyIrAddrIrStatus();

    }

    /*******************************************************************************
     * Function Name  : autoReplyHeartBeatStatus()
     * Description    : process Heart Beat to AutoReply LCB status
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyHeartBeatStatus() {
        if (mLcb != null) {
            if (mGeneralHandler != null)
                machineLcbType = mGeneralHandler.getMachineLcbType();
            switch (machineLcbType) {
                case LCB_Toptek: // LCB 0xC1 does not support this command
                    //Log.d(TAG, "Skipping get LCB status for LCB 0xC1");
                    break;
                default:
                    mLcb.getLcbStatus(LcbComm.QueueMode.ReplaceSimilar);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : autoReplyTreadmillExtStatus()
     * Description    : process Heart Beat to AutoReply LCB Treadmill Ext status
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyTreadmillExtStatus() {
        if (cntIntervalTimesTreadmillExtStatus == TIMES_TREADMILL_EXT_STATUS_INTERVAL) {
            cntIntervalTimesTreadmillExtStatus = 0;
	    if (mLcb != null)
            	mLcb.getLcbTreadmillExtStatus(LcbComm.QueueMode.ReplaceSimilar);
		
        } else {
            cntIntervalTimesTreadmillExtStatus++;
        }
    }

    /*******************************************************************************
     * Function Name  : autoReplyRpm()
     * Description    : process Heart Beat to AutoReply LCB TM Rpm
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
     * Function Name  : autoReplyCadence()
     * Description    : Auto reply Cadence (SPM). Command ID 0x7F
     *                  Only supported by LCB 0xC9 (PS PMSM 2.2kW) and 0xF2 (PP PMSM 2.2kW).
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyCadence() {
        if (cntIntervalTimesCadence == TIMES_CADENCE_INTERVAL) {
            cntIntervalTimesCadence = 0;
            mLcb.getLcbTreadmillCadence(LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesCadence++;
        }
    }

    /*******************************************************************************
     * Function Name  : autoReplyIncline()
     * Description    : process Heart Beat to AutoReply LCB TreadMill Incline
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
        //if (mMachineRpm > 100) {  //remove P+ 0.5Mile rpm <100 not trigger
            if (cntIntervalTimesInUsed == TIMES_INUSED_INTERVAL) {
                cntIntervalTimesInUsed = 0;
                mLcb.getLcbTreadmillInUsed(LcbComm.QueueMode.ReplaceSimilar);
            } else {
                cntIntervalTimesInUsed++;
            }
        //}
    }

    /*******************************************************************************
     * Function Name  : autoReplySpeedUpInUsedCmd()
     * Description    : process Heart Beat to AutoReply LCB TreadMill SpeedUp InUsed Cmd
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplySpeedUpInUsedCmd() {
        if (cntIntervalTimesSpeedUpInUsedCmd == TIMES_SPEEDUP_INUSED_CMD_INTERVAL) {
            cntIntervalTimesSpeedUpInUsedCmd = 0;
            mLcb.getLcbTreadmillInUsed(LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesSpeedUpInUsedCmd++;
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
     * Function Name  : autoReplyIrAddrIrStatus()
     * Description    : process Heart Beat to AutoReply Ir board Addr 0x26 Hybris TM Ir Status
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyIrAddrIrStatus() {
        if (cntIntervalTimesIrAddrIrStatus == TIMES_IR_ADDR_IR_STATUS_INTERVAL) {
            cntIntervalTimesIrAddrIrStatus = 0;
            mLcb.getIrAddrIrStatus(LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesIrAddrIrStatus++;
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

    /*******************************************************************************
     * Function Name  : autoReplySetRampUpAndLstRpmValueCmd()
     * Description    : process Heart Beat to AutoReply LCB TreadMill Set Ramp Up And Lst Rpm Value Cmd
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplySetRampUpAndLstRpmValueCmd() {
        if (cntIntervalTimesSetRampUpAndLastRpm == TIMES_SET_RAMP_UP_AND_LAST_RPM_INTERVAL) {
            cntIntervalTimesSetRampUpAndLastRpm = 0;
            checkSetAutoReplyTmRpmValueCmd();
        } else {
            cntIntervalTimesSetRampUpAndLastRpm++;
        }
    }

//<===Hanler LCB Auto Heart Beat

    /*******************************************************************************
     * Function Name  : initialTreadMill()
     * Description    : initial TreadMill  
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void initialTreadMill() {

        mLcb = LcbComm.getInstance();
        mGeneralHandler = GeneralHandler.getInstance();
        mDeviceLcb = DeviceDataTransmitted.getInstance();
        mLcb.addReceiveListener(mTreadMillLcbCommReceive);
        mDeviceLcb.addReceiveListener(mTreadMillLcbCommReceive);
        timerStartAutoHeartBeatThread();

        mGeneralHandler.lastErrorCode = 0;
        _isAutoReplyTreadmillExtStatus = false;
        _isAutoReplyRpm = false;
        _isAutoReplyCadence = false;
        _isAutoReplyIncline = false;
        _isAutoReplyInUsed = false;
        _isAutoReplySpeedUpInUsedCmd = false;
        _isAutoReplyMotorInfo = false;
        _isAutoSetRampUpAndLastRpmValueCmd = false;
    }

    /*******************************************************************************
     * Function Name  : unInitialTreadMill()
     * Description    : uninitial TreadMill  
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void unInitialTreadMill() {

        stopAutoHeartBeatThread();
        mLcb.removeReceiveListener(mTreadMillLcbCommReceive);
        mDeviceLcb.removeReceiveListener(mTreadMillLcbCommReceive);
        mLcb = null;
        mDeviceLcb = null;
    }

    /*******************************************************************************
     * Function Name  : setTreadMillHandlerCallback()
     * Description    : set TreadMillHandler Callback
     * Input          : ITreadMillHandlerCallback treadMillHandlerCallback
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setTreadMillHandlerCallback(ITreadMillHandlerCallback treadMillHandlerCallback) {
        if (treadMillHandlerCallback != null)
            mTreadMillHandlerCallback = treadMillHandlerCallback;
    }

    /*******************************************************************************
     * Function Name  : unsetTreadMillHandlerCallback()
     * Description    : unset TreadMillHandler Callback
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void unsetTreadMillHandlerCallback() {
        mTreadMillHandlerCallback = null;
    }

    /*******************************************************************************
     * Function Name  : setTreadMillRPM()
     * Description    : set TreadMill Resistance
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setTreadMillRPM(int intValue) { setTreadMillRPM(intValue, false); }
    public void setTreadMillRPM(int intValue, boolean highPriority) {

        if (intValue == 0) {
            mTargetRpmCheckFlag = false;
        } else {
            mTargetRpmCheckFlag = true;
            cntIntervalTimesTargetRpm = 0;
        }

        //Check and Start set last rpm value function
        if(_isAutoSetRampUpAndLastRpmValueCmd){
            mGUISetRpmValue = intValue;
            if( mGUISetRpmValue == 0) {
                if (mLcb != null) {
                    mLcb.setLcbMotorRPMhighestPriority(mGUISetRpmValue);
                }
            }
        }else {

            if (mLcb != null) {
                if (highPriority) mLcb.setLcbMotorRPMhighestPriority(intValue);
                else mLcb.setLcbMotorRPM(intValue);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : setTreadMillIncline()
     * Description    : set TreadMill Incline
     * Input          : int intLoc
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setTreadMillIncline(int intLoc) { setTreadMillIncline(intLoc, false); }
    public void setTreadMillIncline(int intLoc, boolean highPriority) {
        if (mLcb != null) {
            if (highPriority) mLcb.setLcbInclineLochighestPriority(intLoc);
            else mLcb.setLcbInclineLoc(intLoc);
        }
    }
	
    /*******************************************************************************
     * Function Name  : setTreadMillIncline()
     * Description    : set TreadMill Incline
     * Input          : int intLoc
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void sendTreadmillOperate(int treadmillRpm, int inclineLoc) {
        if (mLcb != null) {
            mLcb.setLcbTreadmillOperate(treadmillRpm, inclineLoc);
        }
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyTreadmillExtStatus()
     * Description    : set TreadMill AutoReply Treadmill Ext Status
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyTreadmillExtStatus(boolean enable) {
        if (enable)
            _isAutoReplyTreadmillExtStatus = true;
        else
            _isAutoReplyTreadmillExtStatus = false;
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyTmRpm()
     * Description    : set TreadMill AutoReply TreadMill RPM
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyTmRpm(boolean enable) {
        if (enable)
            _isAutoReplyRpm = true;
        else
            _isAutoReplyRpm = false;
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyTmCadence()
     * Description    : set Auto Reply Cadence enable/disable
     * Input          : enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyTmCadence(boolean enable) {
        _isAutoReplyCadence = enable;
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyTmIncline()
     * Description    : set TreadMill AutoReply TreadMill Incline
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyTmIncline(boolean enable) {
        if (enable)
            _isAutoReplyIncline = true;
        else
            _isAutoReplyIncline = false;
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyTmInUsed()
     * Description    : set TreadMill AutoReply TreadMill InUsed
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyTmInUsed(boolean enable) {
        if (enable)
            _isAutoReplyInUsed = true;
        else
            _isAutoReplyInUsed = false;
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyTmSpeedUpInUsed()
     * Description    : set TreadMill AutoReply TreadMill SpeedUp InUsed Cmd
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyTmSpeedUpInUsed(boolean enable) {
        if (enable)
            _isAutoReplySpeedUpInUsedCmd = true;
        else
            _isAutoReplySpeedUpInUsedCmd = false;
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyTmMotorInfo()
     * Description    : set TreadMill AutoReply TreadMill MotorInfo
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyTmMotorInfo(boolean enable) {
        if (enable)
            _isAutoReplyMotorInfo = true;
        else
            _isAutoReplyMotorInfo = false;
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyIrAddrIrStatus(boolean enable)
     * Description    : set Hybrid AutoReply Ir Addr 0x26 Hybris TM Ir Status
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyIrAddrIrStatus(boolean enable) {
        if (enable)
            _isAutoReplyIrAddrIrStatus = true;
        else
            _isAutoReplyIrAddrIrStatus = false;
    }

    /*******************************************************************************
     * Function Name  : setTreadMillInclineAction()
     * Description    : set TreadMill Incline Action
     * Input          : byte byteValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setTreadMillInclineAction(byte byteValue) {
        if (mLcb != null) {
            if (mGeneralHandler != null)
                machineLcbType = mGeneralHandler.getMachineLcbType();
            switch (machineLcbType) {
                case LCB_E0: // TODO - ST90 incline will not work if we send stop action
                    Log.d(TAG, "Skipping incline action for LCB 0xE0");
                    break;
                default:
                    mLcb.setLcbInclineAction(byteValue);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : setTreadMillMotorType()
     * Description    : set TreadMill Motor Type
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setTreadMillMotorType(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbMotorSpecification(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : getTreadMillOneShotReplyRpm()
     * Description    : get TreadMill One Shot Reply Rpm
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getTreadMillOneShotReplyRpm() {
        if (mLcb != null) {
            mLcb.getOneShotLcbMotorRpmHighPriority();
        }
    }

    /*******************************************************************************
     * Function Name  : setTreadMillEcbAction(byte byteValue)
     * Description    : set TreadMill Ecb Action
     * Input          : byte byteValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setTreadMillEcbAction(byte byteValue) {
        if (mLcb != null) {
            mLcb.setLcbTreadmillEcbActionHighestPriority(byteValue);
        }
    }

    /*******************************************************************************
     * Function Name  : getTreadMillOneShotReplyInclineAdc()
     * Description    : get TreadMill OneShot Reply Incline Adc
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getTreadMillOneShotReplyInclineAdc() {
        if (mLcb != null) {
            mLcb.getLcbInclineLoc(LcbComm.QueueMode.ReplaceSimilar);
        }
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyTmRampUpAndLastRpmValue()
     * Description    : set TreadMill Auto Reply TreadMill Set Ramp Up And Last Rpm Value Cmd
     * Input          : boolean enable,int rampUpStage,int rampDownStage
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyTmRampUpAndLastRpmValue(boolean enable,int rampUpStage,int rampDownStage) {
        if (enable)
            _isAutoSetRampUpAndLastRpmValueCmd = true;
        else
            _isAutoSetRampUpAndLastRpmValueCmd = false;

        mRpmRampUpStageValue = rampUpStage;
        mRpmRampDownStageValue = rampDownStage;
    }

    /*******************************************************************************
     * Function Name  : checkSetAutoReplyTmRpmValueCmd()
     * Description    : check and set Auto Reply Tm Rpm Value Cmd
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void checkSetAutoReplyTmRpmValueCmd() {
        if(mGUISetRpmValue == 0){ //Rpm = 0
            mSetCurrentRpmValue = 0;
            if(mLcb != null) mLcb.setLcbMotorRPMhighestPriority(mSetCurrentRpmValue);
            return;
        }

        if (mSetCurrentRpmValue == mGUISetRpmValue) {
            if(mLcb != null) mLcb.setLcbMotorRPMhighestPriority(mSetCurrentRpmValue);
            return;
        }

        int checkCurrentRpmValue = mSetCurrentRpmValue;
        int current = Math.max(0, mGUISetRpmValue);
        if (current > checkCurrentRpmValue){ // Speed Up
            current = Math.min(checkCurrentRpmValue + mRpmRampUpStageValue, current);
        } else { // Speed Down
            current = Math.max(checkCurrentRpmValue - mRpmRampDownStageValue, current);
        }

        if (current != checkCurrentRpmValue) {
            if(mLcb != null) mLcb.setLcbMotorRPMhighestPriority(current);
            mSetCurrentRpmValue = current;
        }

    }

    /*******************************************************************************
     * Function Name  : eStopResetGuiCurrentRpm()
     * Description    : eStop trigger Reset Gui and Current Rpm value 0
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void eStopResetGuiCurrentRpm(){
        mGUISetRpmValue = 0;
        mSetCurrentRpmValue = 0;
    }

    /*******************************************************************************
     * Function Name  : getHybridTMIrAddrIrStatus()
     * Description    : get HybridTM Ir Addr Ir Status
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getHybridTMIrAddrIrStatus() {

        if (mLcb != null) {
            mLcb.getIrAddrIrStatusHighestPriority();
        }
    }

    /*******************************************************************************
     * Function Name  : getHybridTMIrAddrVersion()
     * Description    : get HybridTM IR Addr0x26 Version
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getHybridTMIrAddrVersion() {
        if (mLcb != null) {
            mLcb.getIrAddrVersion();
        }
    }

    /*******************************************************************************
     * Function Name  : setHybridTMIrAddrEnterErpMode()
     * Description    : set HybridTM Ir Addr 0x26 EnterErpMode
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setHybridTMIrAddrEnterErpMode() {
        if (mLcb != null) {
            mLcb.setIrAddrEnterErpMode();
        }
    }

    /*******************************************************************************
     * Function Name  : setHybridTMIrAddrLeaveErpMode()
     * Description    : set HybridTM Ir Addr 0x26 LeaveErpMode
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setHybridTMIrAddrLeaveErpMode() {
        if (mLcb != null) {
            mLcb.setIrAddrLeaveErpMode();
        }
    }

}


