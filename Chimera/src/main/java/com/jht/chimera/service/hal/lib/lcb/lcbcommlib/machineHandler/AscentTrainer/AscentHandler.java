package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.AscentTrainer;

import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IDeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ILcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.GeneralHandler;

public class AscentHandler {
    private static final String TAG = "AscentHandler";


    protected ILcbComm mLcb;
    protected IDeviceDataTransmitted mDeviceLcb;
    private ILcbComm mLcbComm;
    private AscentCallbackHandlerLCB mAscentLcbCommReceive = new AscentCallbackHandlerLCB(this);

    protected IAscentHandlerCallback mAscentHandlerCallback;

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
//<=== Hanler Auto Heart Beat 

    private AscentHandler() {
    }

    private static class SingletonHolder {
        private static AscentHandler Instance = new AscentHandler();
    }

    public static AscentHandler getInstance() {
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
        mStartHeartBeatTimer.start("AscentHbTimer");

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
        HeartBeatThread() { super("AscentHbThread"); }
        @Override
        public void run() {
            super.run();
            Log.v(TAG, "Ascent HeartBeatThread++++");

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
                Log.e(TAG, "Exception in Ascent heartBeat thread! ", ex);
                _isRunning = false;
                //re-start HeartBeatThread
                Log.v(TAG, "Re-start Ascent HeartBeatThread . ");
                timerStartAutoHeartBeatThread();
            }

            Log.v(TAG, "Ascent HeartBeatThread----");
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

        if (_isAutoReplyIncline)
            autoReplyIncline();
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
     * Function Name  : autoReplyIncline()
     * Description    : process Heart Beat to AutoReply LCB Ascent Incline
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyIncline() {
        if (cntIntervalTimesIncline == TIMES_INCLINE_INTERVAL) {
            cntIntervalTimesIncline = 0;
            mLcb.getLcbInclinePercent(LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesIncline++;
        }
    }

//<===Hanler LCB Auto Heart Beat

    /*******************************************************************************
     * Function Name  : initialAscent()
     * Description    : initial Ascent 
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void initialAscent() {

        mLcb = LcbComm.getInstance();
        mGeneralHandler = GeneralHandler.getInstance();
        mDeviceLcb = DeviceDataTransmitted.getInstance();
        mLcb.addReceiveListener(mAscentLcbCommReceive);
        mDeviceLcb.addReceiveListener(mAscentLcbCommReceive);
        timerStartAutoHeartBeatThread();

        _isAutoReplyRpm = false;
        _isAutoReplyIncline = false;
    }

    /*******************************************************************************
     * Function Name  : unInitialAscent()
     * Description    : uninitial Ascent 
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void unInitialAscent() {

        stopAutoHeartBeatThread();
        mLcb.removeReceiveListener(mAscentLcbCommReceive);
        mLcb = null;
        mDeviceLcb = null;
    }

    /*******************************************************************************
     * Function Name  : setAscentHandlerCallback()
     * Description    : set AscentHandler Callback
     * Input          : IAscentHandlerCallback ascentHandlerCallback
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAscentHandlerCallback(IAscentHandlerCallback ascentHandlerCallback) {
        if (ascentHandlerCallback != null)
            mAscentHandlerCallback = ascentHandlerCallback;
    }

    /*******************************************************************************
     * Function Name  : unsetAscentHandlerCallback()
     * Description    : unset AscentHandler Callback
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void unsetAscentHandlerCallback() {
        mAscentHandlerCallback = null;
    }

    /*******************************************************************************
     * Function Name  : setAscentResistance()
     * Description    : set Ascent Resistance
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAscentResistance(int intValue) {
        if (mLcb != null) {
            if (mGeneralHandler != null)
                machineLcbType = mGeneralHandler.getMachineLcbType();
            switch (machineLcbType) {
                case LCB_ClimbMill_or_A30:
                    mLcb.setLcbECBposition(intValue);
                    //if (intValue == 0) mLcb.setLcbResetECB(); // is ECB reset needed?
                    break;
                default:
                    mLcb.setLcbPWMpercent(intValue);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : setAscentIncline()
     * Description    : set Ascent Incline
     * Input          : int intLoc
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAscentIncline(int intLoc) {
        if (mLcb != null) {
            if (mGeneralHandler != null)
                machineLcbType = mGeneralHandler.getMachineLcbType();
            switch (machineLcbType) {
                case LCB_Electromagnetic_brake_Incline_LCB: // A50
                case LCB_ClimbMill_or_A30: // A30
                    mLcb.setLcbInclineLoc(intLoc);
                    break;
                default:
                    mLcb.setLcbInclinePercent(intLoc);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyAscentRpm()
     * Description    : set Ascent AutoReply Ascent RPM
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyAscentRpm(boolean enable) {
        if (enable)
            _isAutoReplyRpm = true;
        else
            _isAutoReplyRpm = false;
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyAscentIncline()
     * Description    : set Ascent AutoReply Ascent Incline
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyAscentIncline(boolean enable) {
        if (enable)
            _isAutoReplyIncline = true;
        else
            _isAutoReplyIncline = false;
    }

    /*******************************************************************************
     * Function Name  : setAscentGearRatio()
     * Description    : set Ascent GearRatio
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAscentGearRatio(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbRPMgearRatio(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setAscentResistanceType()
     * Description    : set Ascent ResistanceType
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAscentResistanceType(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbResistanceType(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setAscentGeneratorPole()
     * Description    : set Ascent GeneratorPole
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAscentGeneratorPole(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbGeneratorPolePair(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setAscentLowLimitResistance()
     * Description    : set Ascent LowLimitResistance
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAscentLowLimitResistance(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbLimitRPMresistance(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setAscentLowLimitCharge()
     * Description    : set Ascent LowLimitCharge
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAscentLowLimitCharge(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbLimitRPMcharge(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setAscentTuneEndIncline()
     * Description    : set Ascent TuneEndIncline
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAscentTuneEndIncline(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbTuneEndIncline(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setAscentConsolePower()
     * Description    : set Ascent ConsolePower
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAscentConsolePower(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbCmdConsolePower(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setAscentInclineAction()
     * Description    : set Ascent Incline Action
     * Input          : byte byteValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAscentInclineAction(byte byteValue) {
        if (mLcb != null) {
            mLcb.setLcbInclineAction(byteValue);
        }
    }

    /*******************************************************************************
     * Function Name  : autoAscentCalibrateIncline()
     * Description    : auto Ascent Calibrate Incline
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void autoAscentCalibrateIncline() {
        if (mLcb != null) {
            mLcb.setLcbCalibrate();
        }
    }

    /*******************************************************************************
     * Function Name  : setAscentTurnOffLcb()
     * Description    : set Ascent Turn Off Lcb
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAscentTurnOffLcb(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbPowerOff(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : stopAscentCalibrate()
     * Description    : stop Ascent Calibrate
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void stopAscentCalibrate() {
        if (mLcb != null) {
            mLcb.setLcbInclineActionHighestPriority((byte) 0x00);
        }
    }

    /*******************************************************************************
     * Function Name  : stopAscentInclineAction()
     * Description    : stop Ascent Incline Action
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void stopAscentInclineAction() {
        if (mLcb != null) {
            mLcb.setLcbInclineAction((byte) 0x00);
        }
    }

    /*******************************************************************************
     * Function Name  : getAscentOneShotReplyRpm()
     * Description    : get Ascent One Shot Reply Rpm
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getAscentOneShotReplyRpm() {
        if (mLcb != null) {
            mLcb.getOneShotLcbECBrpmHighPriority();
        }
    }

}
