package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.EnClimbmill;

import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IDeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ILcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.GeneralHandler;

public class EnClimbHandler {
    private static final String TAG = "EnClimbHandler";


    protected ILcbComm mLcb;
    protected IDeviceDataTransmitted mDeviceLcb;
    private ILcbComm mLcbComm;
    private EnClimbCallbackHandlerLCB mEnClimbLcbCommReceive = new EnClimbCallbackHandlerLCB(this);

    protected IEnClimbHandlerCallback mEnClimbHandlerCallback;

    private GeneralHandler mGeneralHandler;
    private LcbType machineLcbType = LcbType.LCB_UNKNOWN;

    //Handler Auto Heart Beat ===>
    private volatile boolean _isRunning = false;
    private static final Object _notifyHeartBeatSync = new Object();
    private static Thread _heartBeatThread = null;
    private static final int HEART_BEAT_TIMER_PERIOD = 400;
    private static volatile JHTTimer mStartHeartBeatTimer;
//    private volatile boolean  _isAutoReplyEnClimbCmd                     = false;

    //get spm used
    private volatile boolean _isAutoReplySpm = false;
    private static final int TIMES_SPM_INTERVAL = 0; // 0 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesSpm = 0; //HEART_BEAT_TIMER_PERIOD x times

    //get BrakePressure used
    private volatile boolean _isAutoReplyBrakePressure = false;
    private static final int TIMES_BRAKE_PRESSURE_INTERVAL = 0; // 0 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesBrakePressure = 0; //HEART_BEAT_TIMER_PERIOD x times

    //get Watts used
    private volatile boolean _isAutoReplyWatts = false;
    private static final int TIMES_WATTS_INTERVAL = 0; // 0 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesWatts = 0; //HEART_BEAT_TIMER_PERIOD x times

    //get CalScaling used
    private volatile boolean _isAutoReplyCalScaling = false;
    private static final int TIMES_CAL_SCALING_INTERVAL = 0; // 0 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesCalScaling = 0; //HEART_BEAT_TIMER_PERIOD x times

    //get CZcapaIRfreq used
    private volatile boolean _isAutoReplyCZcapaIRfreq = false;
    private static final int TIMES_CZ_CAPA_IR_FREQ_INTERVAL = 1; // 1 HEART_BEAT_TIMER_PERIOD Interval
    private int cntIntervalTimesCZcapaIRfreq = 0; //HEART_BEAT_TIMER_PERIOD x times

//<=== Hanler Auto Heart Beat 

    private EnClimbHandler() {
    }

    private static class SingletonHolder {
        private static EnClimbHandler Instance = new EnClimbHandler();
    }

    public static EnClimbHandler getInstance() {
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
        mStartHeartBeatTimer.start("EnCmHbTimer");
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
        HeartBeatThread() { super("EnCmHbThread"); }
        @Override
        public void run() {
            super.run();
            Log.v(TAG, "EnClimbMill HeartBeatThread++++");

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
                Log.e(TAG, "Exception in EnClimbMill heartBeat thread! ", ex);
                _isRunning = false;
                //re-start HeartBeatThread
                Log.v(TAG, "Re-start EnClimbMill HeartBeatThread . ");
                timerStartAutoHeartBeatThread();
            }

            Log.v(TAG, "EnClimbMill HeartBeatThread----");
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
        autoReplyHeartBeatEnClimbStatus();

        if (_isAutoReplySpm)
            autoReplySpm();

        if (_isAutoReplyBrakePressure)
            autoReplyBrakePressure();

        if (_isAutoReplyWatts)
            autoReplyWatts();

        if (_isAutoReplyCalScaling)
            autoReplyCalScaling();

        if (_isAutoReplyCZcapaIRfreq)
            autoReplyCZcapaIRfreq();

    }

    /*******************************************************************************
     * Function Name  : autoReplyHeartBeatEnClimbStatus()
     * Description    : process Heart Beat to AutoReply LCB EnClimb status
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyHeartBeatEnClimbStatus() {
        mLcb.getLcbClimbmillStatus(LcbComm.QueueMode.ReplaceSimilar);
    }

    /*******************************************************************************
     * Function Name  : autoReplySpm()
     * Description    : process Heart Beat to AutoReply LCB EnClimb Spm
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplySpm() {
        if (cntIntervalTimesSpm == TIMES_SPM_INTERVAL) {
            cntIntervalTimesSpm = 0;
            mLcb.getLcbSPM(LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesSpm++;
        }
    }

    /*******************************************************************************
     * Function Name  : autoReplyBrakePressure()
     * Description    : process Heart Beat to AutoReply LCB EnClimb BrakePressure
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyBrakePressure() {
        if (cntIntervalTimesBrakePressure == TIMES_BRAKE_PRESSURE_INTERVAL) {
            cntIntervalTimesBrakePressure = 0;
            mLcb.getLcbBrakePressureValue(LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesBrakePressure++;
        }
    }

    /*******************************************************************************
     * Function Name  : autoReplyWatts()
     * Description    : process Heart Beat to AutoReply LCB EnClimb Watts
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyWatts() {
        if (cntIntervalTimesWatts == TIMES_WATTS_INTERVAL) {
            cntIntervalTimesWatts = 0;
            mLcb.getLcbWatts(LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesWatts++;
        }
    }

    /*******************************************************************************
     * Function Name  : autoReplyCalScaling()
     * Description    : process Heart Beat to AutoReply LCB EnClimb CalScaling
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private synchronized void autoReplyCalScaling() {
        if (cntIntervalTimesWatts == TIMES_CAL_SCALING_INTERVAL) {
            cntIntervalTimesCalScaling = 0;
            mLcb.getLcbCalScaling(LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesCalScaling++;
        }
    }

    /*******************************************************************************
     * Function Name  : autoReplyCZcapaIRfreq()
     * Description    : process Heart Beat to AutoReply LCB EnClimb CZcapaIRfreq
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
     * Function Name  : initialEnClimb()
     * Description    : initial EnClimb
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void initialEnClimb() {

        mLcb = LcbComm.getInstance();
        mGeneralHandler = GeneralHandler.getInstance();
        mDeviceLcb = DeviceDataTransmitted.getInstance();
        mLcb.addReceiveListener(mEnClimbLcbCommReceive);
        mDeviceLcb.addReceiveListener(mEnClimbLcbCommReceive);
        timerStartAutoHeartBeatThread();

        _isAutoReplySpm = false;
        _isAutoReplyBrakePressure = false;
        _isAutoReplyWatts = false;
        _isAutoReplyCalScaling = false;
        _isAutoReplyCZcapaIRfreq = false;
    }

    /*******************************************************************************
     * Function Name  : unInitialEnClimb()
     * Description    : uninitial EnClimb
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void unInitialEnClimb() {

        stopAutoHeartBeatThread();
        mLcb.removeReceiveListener(mEnClimbLcbCommReceive);
        mLcb = null;
        mDeviceLcb = null;
    }

    /*******************************************************************************
     * Function Name  : setEnClimbHandlerCallback()
     * Description    : set EnClimbHandler Callback
     * Input          : IEnClimbHandlerCallback enClimbHandlerCallback
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setEnClimbHandlerCallback(IEnClimbHandlerCallback enClimbHandlerCallback) {
        if (enClimbHandlerCallback != null)
            mEnClimbHandlerCallback = enClimbHandlerCallback;
    }

    /*******************************************************************************
     * Function Name  : unsetEnClimbHandlerCallback()
     * Description    : unset EnClimbHandler Callback
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void unsetEnClimbHandlerCallback() {
        mEnClimbHandlerCallback = null;
    }

    /*******************************************************************************
     * Function Name  : setEnClimbSPM()
     * Description    : set EnClimb SPM
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setEnClimbSPM(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbSPM(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setEnClimbResistance()
     * Description    : set EnClimb Resistance
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setEnClimbResistance(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbLimitRPMresistance(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setEnClimbUnlock()
     * Description    : set EnClimb Unlock
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setEnClimbUnlock() {
        if (mLcb != null) {
            mLcb.setLcbClimbmillStatus((byte) 0x00);
        }
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyEnClimbSpm()
     * Description    : set EnClimb AutoReply EnClimb SPM
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyEnClimbSpm(boolean enable) {
        if (enable)
            _isAutoReplySpm = true;
        else
            _isAutoReplySpm = false;
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyEnClimbBrakePressure()
     * Description    : set EnClimb AutoReply EnClimb BrakePressure
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyEnClimbBrakePressure(boolean enable) {
        if (enable)
            _isAutoReplyBrakePressure = true;
        else
            _isAutoReplyBrakePressure = false;
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyEnClimbWatts()
     * Description    : set EnClimb AutoReply EnClimb Watts
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyEnClimbWatts(boolean enable) {
        if (enable)
            _isAutoReplyWatts = true;
        else
            _isAutoReplyWatts = false;
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyEnClimbCalScaling()
     * Description    : set EnClimb AutoReply EnClimb CalScaling
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyEnClimbCalScaling(boolean enable) {
        if (enable)
            _isAutoReplyCalScaling = true;
        else
            _isAutoReplyCalScaling = false;
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyClimbCZcapaIRfreq()
     * Description    : set EnClimb AutoReply EnClimb CZ-capacitance IR-frequency
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyEnClimbCZcapaIRfreq(boolean enable) {
        if (enable)
            _isAutoReplyCZcapaIRfreq = true;
        else
            _isAutoReplyCZcapaIRfreq = false;
    }

    /*******************************************************************************
     * Function Name  : setEnClimbGearRatio()
     * Description    : set EnClimbMill GearRatio
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setEnClimbGearRatio(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbRPMgearRatio(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setEnClimbResistanceType()
     * Description    : set EnClimbMill ResistanceType
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setEnClimbResistanceType(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbResistanceType(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setEnClimbGeneratorPole()
     * Description    : set EnClimbMill GeneratorPole
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setEnClimbGeneratorPole(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbGeneratorPolePair(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setEnClimbEStopParameter()
     * Description    : set EnClimbMill EStopParameter
     * Input          : byte byteValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setEnClimbEStopParameter(byte byteValue) {
        if (mLcb != null) {
            mLcb.setLcbEStopParameter(byteValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setEnClimbClimbmillCommand()
     * Description    : set EnClimbMill ClimbmillCommand
     * Input          : byte byteValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setEnClimbClimbmillCommand(byte byteValue) {
        if (mLcb != null) {
            mLcb.setLcbClimbmillStatus(byteValue);
        }
    }

    /*******************************************************************************
     * Function Name  : setEnClimbClimbmillCal()
     * Description    : set EnClimbMill ClimbmillCal
     * Input          : byte[] bytes
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setEnClimbClimbmillCal(byte[] bytes) {
        if (mLcb != null) {
            mLcb.setLcbClimbmillCal(bytes);
        }
    }

    /*******************************************************************************
     * Function Name  : getEnClimbClimbmillCalScaling()
     * Description    : get EnClimbClimbmill Cal Scaling
     * Input          : None
     * Output         : int intValue
     * Return         : None
     *******************************************************************************/
    public void getEnClimbClimbmillCalScaling() {
        if (mLcb != null) {
            mLcb.getLcbCalScaling(LcbComm.QueueMode.AlwaysAdd);
        }
    }

    /*******************************************************************************
     * Function Name  : setEnClimbClimbmillCalScaling()
     * Description    : set EnClimbClimbmill Cal Scaling
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setEnClimbClimbmillCalScaling(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbCalScaling(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : getEnClimbClimbmillFrameCalScaling()
     * Description    : get EnClimbClimbmill FrameCal Scaling
     * Input          : None
     * Output         : int intValue
     * Return         : None
     *******************************************************************************/
    public void getEnClimbClimbmillFrameCalScaling() {
        if (mLcb != null) {
            mLcb.getLcbFrameCalScaling();
        }
    }

    /*******************************************************************************
     * Function Name  : setEnClimbClimbmillFrameCalScaling()
     * Description    : set EnClimbClimbmill FrameCal Scaling
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setEnClimbClimbmillFrameCalScaling(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbFrameCalScaling(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : getEnClimbOneShotReplySpm()
     * Description    : get EnClimb One Shot Reply Spm
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getEnClimbOneShotReplySpm() {
        if (mLcb != null) {
            mLcb.getOneShotLcbSpmHighPriority();
        }
    }

}
