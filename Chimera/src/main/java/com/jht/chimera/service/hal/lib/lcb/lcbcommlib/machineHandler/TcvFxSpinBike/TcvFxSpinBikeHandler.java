package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.TcvFxSpinBike;

import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IDeviceDataTransmitted;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ILcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbComm;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.GeneralHandler;

public class TcvFxSpinBikeHandler implements ITcvFxSpinBikeHandler {
    private static final String TAG = "TcvFxSpinBikeHandler";

    protected ILcbComm mLcb;
    protected IDeviceDataTransmitted mDeviceLcb;
    private TcvFxSpinBikeCallbackHandlerLCB mTcvFxSpinBikeLcbCommReceive = new TcvFxSpinBikeCallbackHandlerLCB(this);

    protected ITcvFxSpinBikeHandlerCallback mTcvFxSpinBikeCallback;
    private GeneralHandler mGeneralHandler;
    private LcbType machineLcbType = LcbType.LCB_UNKNOWN;

//Handler Auto Heart Beat ===>
    private volatile boolean  _isRunning                              = false;
    private static final Object        _notifyHeartBeatSync = new Object();
    private static Thread              _heartBeatThread             = null;
    private static final int  HEART_BEAT_TIMER_PERIOD = 400;
    private static volatile JHTTimer mStartHeartBeatTimer;

    //get sensor data used
    private volatile boolean  _isAutoReplySensorData                     = false;
    private static final int  TIMES_SENSORDATA_INTERVAL = 0; // 0 HEART_BEAT_TIMER_PERIOD Interval
    private int  cntIntervalTimesSensorData                     = 0; //HEART_BEAT_TIMER_PERIOD x times
//<=== Handler Auto Heart Beat

    private TcvFxSpinBikeHandler() {
    }

    private static class SingletonHolder {
        private static TcvFxSpinBikeHandler Instance = new TcvFxSpinBikeHandler();
    }

    public static TcvFxSpinBikeHandler getInstance() {
        return SingletonHolder.Instance;
    }

//Handler Auto Heart Beat ===>

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
        mStartHeartBeatTimer.start("TcvFxHbTimer");
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
    * Description    : process stop auto HeartBeatThread
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
        HeartBeatThread() { super("TcvFxHbThread"); }
        @Override
        public void run() {
            super.run();
            Log.v(TAG, "TcvFxSpinBike HeartBeatThread++++");

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
                Log.e(TAG, "Exception in TcvFxSpinBike heartBeat thread! ", ex);
                _isRunning = false;
                //re-start HeartBeatThread
                Log.v(TAG, "Re-start TcvFxSpinBike HeartBeatThread . ");
                timerStartAutoHeartBeatThread();
            }

            Log.v(TAG, "TcvFxSpinBike HeartBeatThread----");
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
        if (_isAutoReplySensorData)
            autoReplySensorData();
    }

    /*******************************************************************************
    * Function Name  : autoReplySensorData()
    * Description    : process Heart Beat to AutoReply LCB Sensor Data
    * Input          : None
    * Output         : None
    * Return         : None
    *******************************************************************************/
    private synchronized void autoReplySensorData() {
        if (cntIntervalTimesSensorData == TIMES_SENSORDATA_INTERVAL) {
            cntIntervalTimesSensorData = 0;
            mLcb.getLcbIndoorBikeSensorData(LcbComm.QueueMode.ReplaceSimilar);
        } else {
            cntIntervalTimesSensorData++;
        }
    }
//<=== Handler LCB Auto Heart Beat

    /*******************************************************************************
     * Function Name  : initialTcvFxSpinBike()
     * Description    : initial TcvFxSpinBike
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void initialTcvFxSpinBike() {

        mLcb = LcbComm.getInstance();
        mGeneralHandler = GeneralHandler.getInstance();
        mDeviceLcb = DeviceDataTransmitted.getInstance();
        mLcb.addReceiveListener(mTcvFxSpinBikeLcbCommReceive);
        mDeviceLcb.addReceiveListener(mTcvFxSpinBikeLcbCommReceive);
        timerStartAutoHeartBeatThread();

        _isAutoReplySensorData = false;
    }

    /*******************************************************************************
     * Function Name  : unInitialTcvFxSpinBike()
     * Description    : uninitial TcvFxSpinBike
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void unInitialTcvFxSpinBike() {

        stopAutoHeartBeatThread();
        mLcb.removeReceiveListener(mTcvFxSpinBikeLcbCommReceive);
        mLcb = null;
        mDeviceLcb = null;
    }

    /*******************************************************************************
     * Function Name  : setTcvFxSpinBikeHandlerCallback()
     * Description    : set TcvFxSpinBikeHandler Callback
     * Input          : ITcvFxSpinBikeHandlerCallback tcvFxSpinBikeHandlerCallback
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setTcvFxSpinBikeHandlerCallback(ITcvFxSpinBikeHandlerCallback tcvFxSpinBikeHandlerCallback) {
        if (tcvFxSpinBikeHandlerCallback != null)
            mTcvFxSpinBikeCallback = tcvFxSpinBikeHandlerCallback;
    }

    /*******************************************************************************
     * Function Name  : unsetTcvFxSpinBikeHandlerCallback()
     * Description    : unset TcvFxSpinBikeHandler Callback
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void unsetTcvFxSpinBikeHandlerCallback() {
        mTcvFxSpinBikeCallback = null;
    }

    /*******************************************************************************
     * Function Name  : setAutoReplyTcvFxSpinBikeSensorData()
     * Description    : set TcvFxSpinBike AutoReply SensorData
     * Input          : boolean enable
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setAutoReplyTcvFxSpinBikeSensorData(boolean enable) {
        _isAutoReplySensorData = enable;
    }

    /*******************************************************************************
     * Function Name  : getTcvFxSpinBikeOneShotReplySensorData()
     * Description    : get TcvFxSpinBike One Shot Reply SensorData
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getTcvFxSpinBikeOneShotReplySensorData() {
        if (mLcb != null) {
            mLcb.getOneShotLcbIndoorBikeSensorDataHighPriority();
        }
    }

    ITcvFxSpinBikeHandlerCallback getCallback() {
        return mTcvFxSpinBikeCallback;
    }
}
