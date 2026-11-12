package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.JhtADboard;

import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IDeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ILcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.GeneralHandler;

public class JhtADboardHandler {
    private static final String TAG = "JhtADboardHandler";


    protected ILcbComm mLcb;
    protected IDeviceDataTransmitted mDeviceLcb;
    private ILcbComm mLcbComm;
    private JhtADboardCallbackHandlerLCB mJhtADboardLcbCommReceive = new JhtADboardCallbackHandlerLCB(this);

    protected IJhtADboardHandlerCallback mJhtADboardHandlerCallback;

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

    private JhtADboardHandler() {
    }

    private static class SingletonHolder {
        private static JhtADboardHandler Instance = new JhtADboardHandler();
    }

    public static JhtADboardHandler getInstance() {
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
        mStartHeartBeatTimer.start("JhtAdHbTimer");
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
        HeartBeatThread() { super("JhtAdHbThread"); }
        @Override
        public void run() {
            super.run();
            Log.v(TAG, "JhtADboard HeartBeatThread++++");

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
                Log.e(TAG, "Exception in JhtADboard heartBeat thread! ", ex);
                _isRunning = false;
                //re-start HeartBeatThread
                Log.v(TAG, "Re-start JhtADboard HeartBeatThread . ");
                timerStartAutoHeartBeatThread();
            }

            Log.v(TAG, "JhtADboard HeartBeatThread----");
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

//<===Hanler LCB Auto Heart Beat

    /*******************************************************************************
     * Function Name  : initialJhtADboard()
     * Description    : initial JhtADboard  
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void initialJhtADboard() {

        mLcb = LcbComm.getInstance();
        mGeneralHandler = GeneralHandler.getInstance();
        mDeviceLcb = DeviceDataTransmitted.getInstance();
        mLcb.addReceiveListener(mJhtADboardLcbCommReceive);
        mDeviceLcb.addReceiveListener(mJhtADboardLcbCommReceive);
        timerStartAutoHeartBeatThread();

        _isAutoReplyRpm = false;
    }

    /*******************************************************************************
     * Function Name  : unInitialJhtADboard()
     * Description    : uninitial JhtADboard  
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void unInitialJhtADboard() {

        stopAutoHeartBeatThread();
        mLcb.removeReceiveListener(mJhtADboardLcbCommReceive);
        mLcb = null;
        mDeviceLcb = null;
    }

    /*******************************************************************************
     * Function Name  : setJhtADboardHandlerCallback()
     * Description    : set JhtADboardHandler Callback
     * Input          : IJhtADboardHandlerCallback jhtADboardHandlerCallback
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setJhtADboardHandlerCallback(IJhtADboardHandlerCallback jhtADboardHandlerCallback) {
        if (jhtADboardHandlerCallback != null)
            mJhtADboardHandlerCallback = jhtADboardHandlerCallback;
    }

    /*******************************************************************************
     * Function Name  : unsetJhtADboardHandlerCallback()
     * Description    : unset JhtADboardHandler Callback
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void unsetJhtADboardHandlerCallback() {
        mJhtADboardHandlerCallback = null;
    }

    /*******************************************************************************
     * Function Name  : setJhtADboardWattOrResistance()
     * Description    : set JhtADboard Resistance
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setJhtADboardWattOrResistance(int intValue) {
        //Log.d("LCBTrace", "setJhtADboardWattOrResistance " + intValue + " " + machineLcbType);
        if (mLcb != null) {
            if (mGeneralHandler != null)
                machineLcbType = mGeneralHandler.getMachineLcbType();


            switch (machineLcbType) {
                case LCB_JHT_AD_Board: {
                    //Log.d("LCBTrace", "setLcbPWMpercent " + intValue + " " + machineLcbType);
                    mLcb.setLcbPWMpercent(intValue);
                }
                break;
                default:
                    return;
            }
        }
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyJhtADboardRpm()
     * Description    : set JhtADboard AutoReply JhtADboard RPM
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setAutoReplyJhtADboardRpm(boolean enable) {
        if (enable)
            _isAutoReplyRpm = true;
        else
            _isAutoReplyRpm = false;
    }


    /*******************************************************************************
     * Function Name  : setJhtADboardGearRatio()
     * Description    : set JhtADboard GearRatio
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void setJhtADboardGearRatio(int intValue) {
        if (mLcb != null) {
            mLcb.setLcbRPMgearRatio(intValue);
        }
    }

    /*******************************************************************************
     * Function Name  : getJhtADboardOneShotReplyRpm()
     * Description    : get JhtADboard One Shot Reply Rpm
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void getJhtADboardOneShotReplyRpm() {
        if (mLcb != null) {
            mLcb.getOneShotLcbECBrpmHighPriority();
        }
    }

}
