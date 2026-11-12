package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Climbmill;

import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IDeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ILcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.GeneralHandler;

public class ClimbHandler {
    private static final String TAG = "ClimbHandler";


    protected ILcbComm mLcb;
    protected IDeviceDataTransmitted mDeviceLcb;
    private ILcbComm mLcbComm;
    private ClimbCallbackHandlerLCB mClimbLcbCommReceive = new ClimbCallbackHandlerLCB(this);

    protected IClimbHandlerCallback mClimbHandlerCallback;

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

    //get CZcapaIRfreq used
    private volatile boolean _isAutoReplyCZcapaIRfreq = false;
    private static final int TIMES_CZ_CAPA_IR_FREQ_INTERVAL = 1; // 1 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesCZcapaIRfreq = 0; //HEART_BEAT_TIMER_PERIOD x times
//<=== Hanler Auto Heart Beat 

    private ClimbHandler() {
    }

    private static class SingletonHolder {
        private static ClimbHandler Instance = new ClimbHandler();
    }

    public static ClimbHandler getInstance() {
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
        mStartHeartBeatTimer.start("CmHbTimer");
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
        HeartBeatThread() { super("CmHbThread"); }
        @Override
        public void run() {
            super.run();
            Log.v(TAG, "ClimbMill HeartBeatThread++++");

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
                Log.e(TAG, "Exception in ClimbMill heartBeat thread! ", ex);
                _isRunning = false;
                //re-start HeartBeatThread
                Log.v(TAG, "Re-start ClimbMill HeartBeatThread . ");
                timerStartAutoHeartBeatThread();
            }

            Log.v(TAG, "ClimbMill HeartBeatThread----");
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
        autoReplyHeartBeatClimbStatus();

        if (_isAutoReplyRpm)
            autoReplyRpm();

        if (_isAutoReplyCZcapaIRfreq)
            autoReplyCZcapaIRfreq();
    }

    /*******************************************************************************
     * Function Name  : autoReplyHeartBeatClimbStatus()
     * Description    : process Heart Beat to AutoReply LCB Climb status
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyHeartBeatClimbStatus() {
        mLcb.getLcbClimbmillStatus(LcbComm.QueueMode.ReplaceSimilar);
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
     * Function Name  : autoReplyCZcapaIRfreq()
     * Description    : process Heart Beat to AutoReply LCB Climb CZcapaIRfreq
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyCZcapaIRfreq() {
        if (cntIntervalTimesCZcapaIRfreq == TIMES_CZ_CAPA_IR_FREQ_INTERVAL) {
            cntIntervalTimesCZcapaIRfreq = 0;
            mDeviceLcb.funDeviceGetEStopVale(LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesCZcapaIRfreq++;
        }
    }
//<===Hanler LCB Auto Heart Beat

    /*******************************************************************************
     * Function Name  : initialClimb()
     * Description    : initial Climb  
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void initialClimb() {

        mLcb = LcbComm.getInstance();
        mGeneralHandler = GeneralHandler.getInstance();
        mDeviceLcb = DeviceDataTransmitted.getInstance();
        mLcb.addReceiveListener(mClimbLcbCommReceive);
        mDeviceLcb.addReceiveListener(mClimbLcbCommReceive);
        timerStartAutoHeartBeatThread();

        _isAutoReplyRpm = false;
        _isAutoReplyCZcapaIRfreq = false;
    }

    /*******************************************************************************
     * Function Name  : unInitialClimb()
     * Description    : uninitial Climb  
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void unInitialClimb() {

        stopAutoHeartBeatThread();
        mLcb.removeReceiveListener(mClimbLcbCommReceive);
        mLcb = null;
        mDeviceLcb = null;
    }

    /*******************************************************************************
     * Function Name  : setClimbHandlerCallback()
     * Description    : set ClimbHandler Callback
     * Input          : IClimbHandlerCallback climbHandlerCallback
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setClimbHandlerCallback(IClimbHandlerCallback climbHandlerCallback) {
        if (climbHandlerCallback != null)
            mClimbHandlerCallback = climbHandlerCallback;
    }

    /*******************************************************************************
     * Function Name  : unsetClimbHandlerCallback()
     * Description    : unset ClimbHandler Callback
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void unsetClimbHandlerCallback() {
        mClimbHandlerCallback = null;
    }

    /*******************************************************************************
     * Function Name  : setClimbResistance()
     * Description    : set Climb Resistance
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setClimbResistance(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbLimitRPMresistance(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setClimbUnlock()
     * Description    : set Climb Unlock
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setClimbUnlock() {
        if (mLcb != null) {
            mLcb.setLcbClimbmillStatus((byte) 0x00);
        }
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyClimbRpm()
     * Description    : set Climb AutoReply Climb RPM
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyClimbRpm(boolean enable) {
        if (enable)
            _isAutoReplyRpm = true;
        else
            _isAutoReplyRpm = false;
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyClimbCZcapaIRfreq()
     * Description    : set Climb AutoReply Climb CZ-capacitance IR-frequency
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyClimbCZcapaIRfreq(boolean enable) {
        if (enable)
            _isAutoReplyCZcapaIRfreq = true;
        else
            _isAutoReplyCZcapaIRfreq = false;
    }

    /*******************************************************************************
     * Function Name  : setClimbGearRatio()
     * Description    : set ClimbMill GearRatio
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setClimbGearRatio(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbRPMgearRatio(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setClimbResistanceType()
     * Description    : set ClimbMill ResistanceType
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setClimbResistanceType(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbResistanceType(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setClimbGeneratorPole()
     * Description    : set ClimbMill GeneratorPole
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setClimbGeneratorPole(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbGeneratorPolePair(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setClimbEStopParameter()
     * Description    : set ClimbMill EStopParameter
     * Input          : byte byteValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setClimbEStopParameter(byte byteValue) {
        if (mLcb != null) {
            mLcb.setLcbEStopParameter(byteValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setClimbClimbmillCommand()
     * Description    : set ClimbMill ClimbmillCommand
     * Input          : byte byteValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setClimbClimbmillCommand(byte byteValue) {
        if (mLcb != null) {
            mLcb.setLcbClimbmillStatus(byteValue);
        }
    }

}
