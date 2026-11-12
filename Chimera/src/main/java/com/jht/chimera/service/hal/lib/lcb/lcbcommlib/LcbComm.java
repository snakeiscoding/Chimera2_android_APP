package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.service.hal.LCBCommStat;
import com.jht.chimera.service.hal.lib.FloatingStatPanel;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.ISerialPort;
import com.jht.serialport.SerialPort;
import com.jht.serialport.SerialPortConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class LcbComm implements ILcbComm {
    private static final String TAG = "LcbComm";
    private LcbProcComm mLcbProcComm = LcbProcComm.getInstance();

    DataMatrix dataMatrix = new DataMatrix();

    private static final int RECEIVE_TIMEOUT_MS = 150;
    private static final int RETRY_DELAY_TIME_MS = 50;
    private static final int CMD_DELAY_TIME_MS = 30;
    private static final int LIMITED_TOTAL_RETRY_COUNTER = 25;
    private static final int LIMITED_ONE_CMD_COMM_RETRIES = 3;
    private static final int RX_BUFFER_SIZE = 256;
    private static final int BOOT_UP_LEAVE_ERP_RETRY_PERIOD = 1000;
    private static final int BOOT_UP_LEAVE_ERP_RETRIES = 3;
    private static final int WAKE_UP_LCB_RETRIES = 3;
    private static final int LEAVE_ERP_LCB_STATUS_RETRIES = 3;

    private int _retryTotalCounter = 0;
    private int _bootUpLeaveErpTimeCounter = 0;
    private int _wakeUpLcbCounter = 0;
    private int _leaveErpGetLcbStatusCounter = 0;

    private volatile boolean _isPortOpened = false;
    private volatile boolean _isRunning = false;
    private volatile boolean _bInclineMotorDownwardStatus = false;
    private volatile boolean _bInclineMotorUpwardStatus = false;
    private volatile boolean _bMainMotorStatus = false;
    private volatile boolean _bEncoderRPMStatus = false;
    private volatile boolean _bBatteryCapacityStatus = false;
    private volatile boolean _bCommandErrorStatus = false;
    private volatile boolean _bLcbErrorStatus = false;
    private volatile boolean _bInitialCalibrationStatus = false;
    private volatile boolean _bNotSupportCmd = false;
    private volatile boolean _isBootUpLeaveErp = false;
    private volatile boolean _bBlockedFlag = false;
    private volatile boolean _bCommThreadExceptFlag = false;

    private static final byte ENTER_ERP = (byte) 0xFF;
    private static final byte LEAVE_ERP = (byte) 0x00;
    private byte _lcbErpState = ENTER_ERP;

    private static final byte IR_ADDR_ENTER_ERP = (byte) 0xFF;
    private static final byte IR_ADDR_LEAVE_ERP = (byte) 0x00;
    private byte _irAddrErpState = IR_ADDR_ENTER_ERP;

    private static final Object _notifyTxPacketSync = new Object();
    private static final Object _notifyRxPacketSync = new Object();

    private final ArrayList<ILcbCommReceive> _receiveListenerList = new ArrayList<>();
    private final LcbParser _parser = new LcbParser();
    private final PacketQueue _txPacketQueue = new PacketQueue();
    private final PacketQueue _rxPacketQueue = new PacketQueue();

    private ISerialPort _port = null;
    private InputStream _inStream = null;
    private OutputStream _outStream = null;

    private LcbCommands _txCommand = LcbCommands.GetLCBStatus;
    private final byte IR_BOARD_ADDRESS_BYTE = (byte) 0x26;
    private final byte LCB_ADDRESS_BYTE = (byte) 0xFF;
    private byte _txLcbAddr = LCB_ADDRESS_BYTE;

    private static Thread _commThread = null;
    private static Thread _rxThread = null;

    private Timer _bootUpLeaveErpTimer = new Timer("LcbBootErpTimer");

    private AdvancedLogging mAdvancedLogging = AdvancedLogging.getInstance();
    private boolean mLoggingMsg = false;
    private boolean mLoggingDetail = false;
    private int mLoggingTxCount = 0;
    private int mLoggingRxCount = 0;

    private boolean isOneShotRpm = false;
    private boolean isOneShotSensorData = false;

    private static final int RESET_UART_PORT_CMD_TIMES = 500;
    private int mResetUartPortCmdCount = 0;
    private static Thread _resetUartPortThread = null;
    private static final Object _notifyUartPortFlashWaitSync = new Object();
    private static final Object _notifyStartUartPortFlashSync = new Object();
    private boolean mEnableUartPortFlash = false;

    private static Thread _erpStopUartPortThread = null;
    private static final Object _notifyErpStopUartPortSync = new Object();

    private LcbComm() {

        mLcbProcComm.setReceiveListenerList(_receiveListenerList);

        dataMatrix.createBoolAdapter(SettingKeys.Experimental.DATA_LOG_LCB_PACKETS_BOOL, value -> {
            if (_port != null) {
                _port.setTraceDebug(value);
            }
        });

        dataMatrix.createBoolAdapter(HALKeys.DATA_SHOW_LCB_ANALYTICS_BOOL, showFloatingPanel -> {
            if (showFloatingPanel) {
                mFloatingStatPanel.show();
                mFloatingStatPanel.setText(mStats.toString());
            } else {
                mFloatingStatPanel.hide();
            }
        });
    }

    private static class SingletonHolder {
        private static LcbComm Instance = new LcbComm();
    }

    public static LcbComm getInstance() {
        return SingletonHolder.Instance;
    }

    private LCBCommStat mStats = LCBCommStat.getInstance();

    public static ISerialPort createLcbPort(String deviceName, @NonNull SerialPortConfiguration.BaudRate baudrate) {
        if (deviceName == null) return null; // do NOT trust the annotation
        //set RS-485 configuration
        SerialPortConfiguration configuration = SerialPortConfiguration.createRawPort(baudrate);
        configuration.TRACE_DEBUG = DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_LCB_PACKETS_BOOL);
        configuration.enableRs485();
        // We use a dedicated read thread, so blocking for data or close should be fine.
        // Depending on the implementation of serial port, there may also be a select/poll timeout in addition to read.
        configuration.setReadBlocking();
        return new SerialPort(deviceName, configuration);
    }

    /*******************************************************************************
     * Function Name  : addReceiveListener()
     * Description    : add ILcbCommReceive Listener
     * Input          : ILcbCommReceive
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void addReceiveListener(ILcbCommReceive listener) {
        if (listener != null) {

            synchronized (_receiveListenerList) {
                _receiveListenerList.add(listener);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : removeReceiveListener()
     * Description    : remove ILcbCommReceive Listener
     * Input          : ILcbCommReceive
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void removeReceiveListener(ILcbCommReceive listener) {
        if (listener != null) {
            synchronized (_receiveListenerList) {
                _receiveListenerList.remove(listener);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : restart()
     * Description    : serial port restart
     * Input          : None
     * Output         : None
     * Return         : boolean success or not
     *******************************************************************************/
    @Override
    public boolean restart() {
        try {
            _port.close(); // should this be stop() ? what about _isPortOpened = false; ?
        } catch (IOException ignored) {
        }
        return start(_port);

    }

    /*******************************************************************************
     * Function Name  : start()
     * Description    : serial port start and start CommunicationThread & LcbRxThread
     * Input          : SerialPort device, String path, int baudRate
     * Output         : None
     * Return         : true : open port success , false : open port fail
     *******************************************************************************/
    @Override
    public boolean start(ISerialPort device) {

        if (_isPortOpened) {
            Log.w(TAG, "The LCB communication serial port had already opened!");
            return true;
        }

        if (_isRunning) {
            Log.w(TAG, "Communication thread already running");
            return true;
        }

        if (null != device) {
            _port = device;
            try {
                _port.open();
            } catch (Exception e) {
                //Log.e(TAG, "Failed to open serial port: " + path);
                Log.w(TAG, "failed to start", e);
                return false;
            }
            _outStream = _port.getOutputStream();
            _inStream = _port.getInputStream();
            if ((null == _inStream) || (null == _outStream)) {
                return false;
            }

            // remove all packets from queue
            _txPacketQueue.clear();
            _rxPacketQueue.clear();
            resetRxPacket();

            _commThread = new LcbCommunicationThread();
            _rxThread = new LcbRxThread();
            if (mEnableUartPortFlash) {
                _resetUartPortThread = new ResetUartPortThread();
            }
            _erpStopUartPortThread = new ErpStopUartPortThread();

            //first _commThread thread start
            _commThread.start();
            _rxThread.start();
            if (mEnableUartPortFlash) {
                _resetUartPortThread.start();
            }
            _erpStopUartPortThread.start();

            _isPortOpened = true;
        } else {
            Log.e(TAG, "serialPort instance is null!");
            return false;
        }

        Log.i(TAG, "LcbComm started.");
        return true;
    }

    /*******************************************************************************
     * Function Name  : stop()
     * Description    : serial port stop
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void stop() {
        if (_isPortOpened) {
            if (_isRunning) {
                _isRunning = false;

                if (mEnableUartPortFlash) {
                    _resetUartPortThread.interrupt();
                }
                _erpStopUartPortThread.interrupt();
                _rxThread.interrupt();
                _commThread.interrupt();

                // remove all packets from queue
                _txPacketQueue.clear();
                _rxPacketQueue.clear();
                resetRxPacket();

                // wake receive wait in thread
                synchronized (_notifyRxPacketSync) {
                    _notifyRxPacketSync.notifyAll();
                }
                // wake thread
                synchronized (_notifyTxPacketSync) {
                    _notifyTxPacketSync.notifyAll();
                }


                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                _commThread = null;
                _rxThread = null;
                if (mEnableUartPortFlash) {
                    _resetUartPortThread = null;
                }
                _erpStopUartPortThread = null;
            }

            try {
                _port.close();
            } catch (IOException ignored) {
            }
            _port = null;

            _isPortOpened = false;
        }
    }

    /*******************************************************************************
     * Function Name  : uartPortFlashStop
     * Description    : Reset uart port flash port stop
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void uartPortFlashStop() {
        if (_isPortOpened) {
            mResetUartPortCmdCount = 0;

            try {
                _port.close();
            } catch (IOException ignored) {
            }
            _isPortOpened = false;

            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

        }
    }

    /*******************************************************************************
     * Function Name  : uartPortFlashStart
     * Description    : Reset uart port flash port stop
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void uartPortFlashStart() {
        if (!_isPortOpened) {
            if (null != _port) {
                try {
                    _port.open();
                } catch (Exception e) {
                    //Log.e(TAG, "Failed to uart port flash open serial port IOException: " + path);
                    Log.w(TAG, "Failed to uart port flash open serial port", e);
                    // should this bail if open failed?
                }

                _isPortOpened = true;

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                // wake thread
                synchronized (_notifyUartPortFlashWaitSync) {
                    _notifyUartPortFlashWaitSync.notifyAll();
                }

            } else {
                Log.e(TAG, "uart port flash instance is null!");
            }
        }
    }

    /*******************************************************************************
     * Function Name  : erpStopUartPort()
     * Description    : erp Stop Uart Port
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void erpStopUartPort() {
        synchronized (_notifyErpStopUartPortSync) {
            _notifyErpStopUartPortSync.notify();
        }
    }

    /*******************************************************************************
     * Function Name  : resetRxPacket
     * Description    : reset the parser
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void resetRxPacket() {
        // reset rx packet
        _parser.reset();
    }

    /*******************************************************************************
     * Function Name  : receiveDataHandler()
     * Description    : receive UART port data byte array
     * Input          : byte array
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void receiveDataHandler(byte[] data, int length) {
        // append and parse received data

        _parser.parse(data, length, (parseResult, packet) -> {
            if (parseResult.isError()) {
                Log.e(TAG, "parseResult = " + parseResult);
                if (parseResult == LcbParser.ParseResult.InvalidCrc) {
                    mStats.sentMessageInvalidCRC();
                }
            } else if (parseResult == LcbParser.ParseResult.Success) {
                if (mLoggingDetail) {
                    mLoggingRxCount++;
                    Log.d(TAG, "##Details rxPacket Count = " + mLoggingRxCount);
                    Log.d(TAG, "##Details rxPacket = status: " + String.format("%02x", _parser.getRxLcbStatus())
                            + " cmd: " + String.format("%02x", _parser.getCommand().getByteValue())
                            + " data: " + DataUtils.byteArrayToHexString(_parser.getData()));
                }
                mStats.sentMessageSuccessful();
                _rxPacketQueue.add(packet);
                synchronized (_notifyRxPacketSync) {
                    _notifyRxPacketSync.notify();
                }
            }
        });
    }

    FloatingStatPanel mFloatingStatPanel = new FloatingStatPanel(ContextUtil.getApplicationContext());

    private class LcbRxThread extends Thread {
        LcbRxThread() { super("LcbRxThread"); }
        byte[] rxBuffer = new byte[RX_BUFFER_SIZE];
        int bytes = 0;

        @Override
        public void run() {
            super.run();

            Log.v(TAG, "LcbRxThread++");
            _isRunning = true;

            while (_isRunning && !isInterrupted()) {
                try {
                    bytes = _inStream.read(rxBuffer);
                    if (bytes >= 0) receiveDataHandler(rxBuffer, bytes);
                } catch (IOException e) {
                    _isRunning = false;
                    Log.e(TAG, "UART read I/O exception: ", e);
                } catch (Exception ex) {
                    _isRunning = false;
                    Log.e(TAG, "Exception in LcbRxThread ! ", ex);
                }
            }

            Log.v(TAG, "LcbRxThread--");
        }
    }

    /*******************************************************************************
     * Function Name  : processPacket
     * Description    : process receive complete Packet
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void processPacket(LcbPacket packet) {
        //check packet initial
        checkPacketInitial();
        //Status Byte Definition check
        byte byteLcbStatus = packet.getRxLcbStatus();
        checkStatusByteDefinition(packet);
        //check _bNotSupportCmd
        if (_bNotSupportCmd == true) {
            Log.v(TAG, "LCB have not support command or command wrong !!");
            return;
        }
        //Check Length of data bytes,max.255
        byte[] data = packet.getData();
        if (data.length > Constants.RECEIVE_DATA_LENGTH_LIMITED_MAX) {
            Log.v(TAG, "Limited length of data bytes , max.255 !!");
            return;
        }
        if (mEnableUartPortFlash) {
            mResetUartPortCmdCount++;
        }

        switch (packet.getCommand()) {
            case SetLCBInitialize:
                mLcbProcComm.procSetLcbInitialHandler(packet);
                break;
            case GetLCBStatus:
                mLcbProcComm.procGetLcbStatusHandler(packet);
                break;
            case GetLCBErrorCode:
                mLcbProcComm.procGetLcbErrorCodeHandler(packet);
                break;
            case GetLCBVersion:
                mLcbProcComm.procGetLcbVersionHandler(packet);
                break;
            case SetLCBCalibrate:
                mLcbProcComm.procSetLcbCalibrateHandler(packet);
                break;
            case SetLCBSkipCurrError:
                mLcbProcComm.procSetLcbSkipCurrErrorHandler(packet);
                break;
            case SetLCBUpdateProgram:
                mLcbProcComm.procSetLcbUpdateProgramHandler(packet);
                break;
            case SetLCBSpeExtCmd:
                mLcbProcComm.procSetLcbSpeExtCmdHandler(packet);
                break;
            case GetLCBDciVersion:
                mLcbProcComm.procGetLcbDciVersionHandler(packet);
                break;
            case GetLCBDciEnv:
                mLcbProcComm.procGetLcbDciEnvHandler(packet);
                break;
            case SetLCBCmdEUPsMode:
//                mLcbProcComm.procSetLcbCmdEUPsModeHandler(packet);
                procSetLcbCmdEUPsModeHandler(packet);
                break;
            case SetLCBCmdConsolePower:
                mLcbProcComm.procSetLcbCmdConsolePowerHandler(packet);
                break;
            case GetLCBProtocolVersion:
                mLcbProcComm.procGetLcbProtocolVersionHandler(packet);
                break;
            case SetLCBCmdSerialNumber:
                mLcbProcComm.procSetLcbCmdSerialNumberHandler(packet);
                break;
            case SetLCBTreadmillOperate:
                mLcbProcComm.procSetLcbTreadmillOperateHandler(packet);
                break;
            case GetLCBTreadmillCadence:
                mLcbProcComm.procGetLcbTreadmillCadenceHandler(packet);
                break;
            case SetLCBMotorSpeedRtnRPM:
                mLcbProcComm.procSetLcbMotorSpeedRtnRPMHandler(packet);
                break;
            case SetLCBMotorRPM:
                mLcbProcComm.procSetLcbMotorRPMHandler(packet);
                break;
            case SetLCBIncPWMStep:
                mLcbProcComm.procSetLcbIncPWMStepHandler(packet);
                break;
            case SetLCBDecPWMStep:
                mLcbProcComm.procSetLcbDecPWMStepHandler(packet);
                break;
            case SetLCBStopPWMStep:
                mLcbProcComm.procSetLcbStopPWMStepHandler(packet);
                break;
            case SetLCBInclineAction:
                mLcbProcComm.procSetLcbInclineActionHandler(packet);
                break;
            case SetLCBInclineLoc:
                mLcbProcComm.procSetLcbInclineLocHandler(packet);
                break;
            case SetLCBOperatingStatus:
                mLcbProcComm.procSetLcbOperatingStatusHandler(packet);
                break;
            case GetLCBRollerRPM:
                mLcbProcComm.procGetLcbRollerRPMHandler(packet);
                break;
            case GetLCBMotorRPM:
                if (isOneShotRpm) {
                    isOneShotRpm = false;
                    mLcbProcComm.procGetOneShotLcbMotorRpmHandler(packet);
                } else {
                    mLcbProcComm.procGetLcbMotorRPMHandler(packet);
                }
                break;
            case GetLCBInclineLoc:
                mLcbProcComm.procGetLcbInclineLocHandler(packet);
                break;
            case SetLCBTuneEndIncline:
                mLcbProcComm.procSetLcbTuneEndInclineHandler(packet);
                break;
            case SetLCBTuneEndIncline2:
                mLcbProcComm.procSetLcbTuneEndIncline2Handler(packet);
                break;
            case SetLCBInclineStroke:
                mLcbProcComm.procSetLcbInclineStrokeHandler(packet);
                break;
            case SetLCBCompensationVol:
                mLcbProcComm.procSetLcbCompensationVolHandler(packet);
                break;
            case SetLCBMotorSpecification:
                mLcbProcComm.procSetLcbMotorSpecificationHandler(packet);
                break;
            case GetLCBMotorType:
                mLcbProcComm.procGetLcbMotorTypeHandler(packet);
                break;
            case GetLCBDCIdataMaxLen:
                mLcbProcComm.procGetLcbDCIdataMaxLenHandler(packet);
                break;
            case SetLCBMotorPowerOff:
                mLcbProcComm.procSetLcbMotorPowerOffHandler(packet);
                break;
            case SetLCBDCIforceIncline:
                mLcbProcComm.procSetLcbDCIforceInclineHandler(packet);
                break;
            case GetLCBTreadmillInUsed:
                mLcbProcComm.procGetLcbTreadmillInUsedHandler(packet);
                break;
            case GetLCBMainMotorInfo:
                mLcbProcComm.procGetLcbMainMotorInfoHandler(packet);
                break;
            case GetLCBTreadmillExtStatus:
                mLcbProcComm.procGetLcbTreadmillExtStatusHandler(packet);
                break;
            case GetLCBDCIinclineRange:
                mLcbProcComm.procGetLcbDCIinclineRangeHandler(packet);
                break;
            case SetLCBDCIinclineRange:
                mLcbProcComm.procSetLcbDCIinclineRangeHandler(packet);
                break;
            case GetLCBDCIrpmSpeedRange:
                mLcbProcComm.procGetLcbDCIrpmSpeedRangeHandler(packet);
                break;
            case SetLCBDCIrpmSpeedRange:
                mLcbProcComm.procSetLcbDCIrpmSpeedRangeHandler(packet);
                break;
            case SetLCBUniversal:
                mLcbProcComm.procSetLcbUniversalHandler(packet);
                break;
            case SetLCBDCIManualCalibration:
                mLcbProcComm.procSetLcbDCIManualCalibrationHandler(packet);
                break;
            case SetLCBDeviceData:
                mLcbProcComm.procSetLcbDeviceDataHandler(packet);
                break;
            case SetLCBResetECB:
                mLcbProcComm.procSetLcbResetECBHandler(packet);
                break;
            case SetLCBECBposition:
                mLcbProcComm.procSetLcbECBpositionHandler(packet);
                break;
            case GetLCBECBrpm:
                if (isOneShotRpm) {
                    isOneShotRpm = false;
                    mLcbProcComm.procGetOneShotLcbECBrpmHandler(packet);
                } else {
                    mLcbProcComm.procGetLcbECBrpmHandler(packet);
                }
                break;
            case GetLCBECBadc:
                mLcbProcComm.procGetLcbECBadcHandler(packet);
                break;
            case SetLCBPWMpercent:
                mLcbProcComm.procSetLcbPWMpercentHandler(packet);
                break;
            case GetLCBBatteryVoltage:
                mLcbProcComm.procGetLcbBatteryVoltageHandler(packet);
                break;
            case SetLCBWatts:
                mLcbProcComm.procSetLcbWattsHandler(packet);
                break;
            case SetLCBRPMgearRatio:
                mLcbProcComm.procSetLcbRPMgearRatioHandler(packet);
                break;
            case SetLCBGeneratorPolePair:
                mLcbProcComm.procSetLcbGeneratorPolePairHandler(packet);
                break;
            case SetLCBLimitRPMcharge:
                mLcbProcComm.procSetLcbLimitRPMchargeHandler(packet);
                break;
            case SetLCBLimitRPMresistance:
                mLcbProcComm.procSetLcbLimitRPMresistanceHandler(packet);
                break;
            case SetLCBMachineType:
                mLcbProcComm.procSetLcbMachineTypeHandler(packet);
                break;
            case SetLCBPowerOff:
                mLcbProcComm.procSetLcbPowerOffHandler(packet);
                break;
            case SetLCBBatteryChargeLevel:
                mLcbProcComm.procSetLcbBatteryChargeLevelHandler(packet);
                break;
            case SetLCBMinResistanceInclineWorking:
                mLcbProcComm.procSetLcbMinResistanceInclineWorkingHandler(packet);
                break;
            case SetLCBInclinePercent:
                mLcbProcComm.procSetLcbInclinePercentHandler(packet);
                break;
            case GetLCBInclinePercent:
                mLcbProcComm.procGetLcbInclinePercentHandler(packet);
                break;
            case SetLCBECBaction:
                mLcbProcComm.procSetLcbECBactionHandler(packet);
                break;
            case GetLCBECBstatus:
                mLcbProcComm.procGetLcbECBstatusHandler(packet);
                break;
            case GetLCBECBcount:
                mLcbProcComm.procGetLcbECBcountHandler(packet);
                break;
            case SetLCBElectromagneticCurrent:
                mLcbProcComm.procSetLcbElectromagneticCurrentHandler(packet);
                break;
            case SetLCBResistanceType:
                mLcbProcComm.procSetLcbResistanceTypeHandler(packet);
                break;
            case SetLCBBeginBatteryCharge:
                mLcbProcComm.procSetLcbBeginBatteryChargeHandler(packet);
                break;
            case GetLCBBatteryCapacity:
                mLcbProcComm.procGetLcbBatteryCapacityHandler(packet);
                break;
            case SetLCBClimbmillStatus:
                mLcbProcComm.procSetLcbClimbmillStatusHandler(packet);
                break;
            case GetLCBClimbmillStatus:
                mLcbProcComm.procGetLcbClimbmillStatusHandler(packet);
                break;
            case SetLCBEStopParameter:
                mLcbProcComm.procSetLcbEStopParameterHandler(packet);
                break;
            case GetLCBBrakePressureValue:
                mLcbProcComm.procGetLcbBrakePressureValueHandler(packet);
                break;
            case GetLCBWatts:
                mLcbProcComm.procGetLcbWattsHandler(packet);
                break;
            case SetLCBClimbmillCal:
                mLcbProcComm.procSetLcbClimbmillCalHandler(packet);
                break;
            case SetLCBRecordCurrent:
                mLcbProcComm.procSetLcbRecordCurrentHandler(packet);
                break;
            case SetLCBPWM:
                mLcbProcComm.procSetLcbPWMHandler(packet);
                break;
            case SetLCBFinishCalibrating:
                mLcbProcComm.procSetLcbFinishCalibratingHandler(packet);
                break;
            case SetLCBSPM:
                mLcbProcComm.procSetLcbSPMHandler(packet);
                break;
            case GetLCBSPM:
                if (isOneShotRpm) {
                    isOneShotRpm = false;
                    mLcbProcComm.procGetOneShotLcbSpmHandler(packet);
                } else {
                    mLcbProcComm.procGetLcbSPMHandler(packet);
                }
                break;
            case SetLCBCalScaling:
                mLcbProcComm.procSetLcbCalScalingHandler(packet);
                break;
            case GetLCBCalScaling:
                mLcbProcComm.procGetLcbCalScalingHandler(packet);
                break;
            case SetLCBFrameCalScaling:
                mLcbProcComm.procSetLcbFrameCalScalingHandler(packet);
                break;
            case GetLCBFrameCalScaling:
                mLcbProcComm.procGetLcbFrameCalScalingHandler(packet);
                break;
            case GetLCBIndoorBikeSensorData:
				if (isOneShotSensorData){
					isOneShotSensorData = false;
                    mLcbProcComm.procGetOneShotLcbIndoorBikeSensorDataHandler(packet);
				} else {
                    mLcbProcComm.procGetLcbIndoorBikeSensorDataHandler(packet);
				}
                break;
            case SetLCBIndoorBikeStepperMotorPosition:
                mLcbProcComm.procSetLcbIndoorBikeStepperMotorPositionHandler(packet);
                break;
            case SetLCBIndoorBikeCalcParaOfDevice:
                mLcbProcComm.procSetLcbIndoorBikeCalcParaOfDeviceHandler(packet);
                break;
            case GetLCBIndoorBikeCalcParaOfDevice:
                mLcbProcComm.procGetLcbIndoorBikeCalcParaOfDeviceHandler(packet);
                break;
            case SetLCBIndoorBikeActionSpinDown:
                mLcbProcComm.procSetLcbIndoorBikeActionSpinDownHandler(packet);
                break;
            case GetLCBIndoorBikeInfoSpinDown:
                mLcbProcComm.procGetLcbIndoorBikeInfoSpinDownHandler(packet);
                break;
            case SetLCBIndoorBikeErgModeSettings:
                mLcbProcComm.procSetLcbIndoorBikeErgModeSettingsHandler(packet);
                break;
            case GetLCBIndoorBikeErgModeSettings:
                mLcbProcComm.procGetLcbIndoorBikeErgModeSettingsHandler(packet);
                break;
            case SetLCBIndoorBikeResistanceLevel:
                mLcbProcComm.procSetLcbIndoorBikeResistanceLevelHandler(packet);
                break;

            case SetLCBTreadmillEcbAction:
                mLcbProcComm.procSetLcbTreadmillEcbActionHandler(packet);
                break;
        }
    }

    /*******************************************************************************
     * Function Name  : processIrAddrPacket
     * Description    : process receive IrAddr 0x26 complete Packet
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void processIrAddrPacket(LcbPacket packet) {
        //check packet initial
        checkPacketInitial();
        //Status Byte Definition check
        byte byteLcbStatus = packet.getRxLcbStatus();
        checkStatusByteDefinition(packet);
        //check _bNotSupportCmd
        if (_bNotSupportCmd == true) {
            Log.v(TAG, "Ir Addr Packet have not support command or command wrong !!");
            return;
        }
        //Check Length of data bytes,max.255
        byte[] data = packet.getData();
        if (data.length > Constants.RECEIVE_DATA_LENGTH_LIMITED_MAX) {
            Log.v(TAG, "Ir Addr Packet Limited length of data bytes , max.255 !!");
            return;
        }
        if (mEnableUartPortFlash) {
            mResetUartPortCmdCount++;
        }

        switch (packet.getCommand()) {
            case GetLCBVersion:
                mLcbProcComm.procGetIrAddrVersionHandler(packet);
                break;
            case GetIrAddressIrStatus:
                mLcbProcComm.procGetIrAddrIrStatusHandler(packet);
                break;
            case SetLCBCmdEUPsMode:
                procSetIrAddrCmdEUPsModeHandler(packet);
                break;
        }
    }

    /*******************************************************************************
     * Function Name  : CommunicationThread
     * Description    : Communication Thread class
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private class LcbCommunicationThread extends Thread {
        LcbCommunicationThread() { super("LcbCommThread"); }
        @Override
        public void run() {
            super.run();
            Log.v(TAG, "LcbCommunicationThread+");

            _isRunning = true;
            _bCommThreadExceptFlag = true;

            try {
                while (_isRunning && !isInterrupted()) {
                    if (mEnableUartPortFlash) {
                        // wait if uart port cmd counter equal to reset times
                        if (mResetUartPortCmdCount == RESET_UART_PORT_CMD_TIMES) {
                            synchronized (_notifyStartUartPortFlashSync) {
                                _notifyStartUartPortFlashSync.notify();
                            }

                            synchronized (_notifyUartPortFlashWaitSync) {
                                _notifyUartPortFlashWaitSync.wait();
                            }
                        }
                    }
                    // wait if nothing to send
                    if (_txPacketQueue.size() == 0) {
                        synchronized (_notifyTxPacketSync) {
                            _notifyTxPacketSync.wait();
                        }
                    }
                    if (_txPacketQueue.size() > 0) {
                        // dequeue packet
                        LcbPacket packet = _txPacketQueue.remove();
                        int retries = 0;
                        while ((retries < LIMITED_ONE_CMD_COMM_RETRIES) && (packet != null)) {
                            byte[] txPacket = packet.buildPacket();
                            if (mLoggingDetail) {
                                mLoggingTxCount++;
                                Log.d(TAG, "##Details txPacket Count = " + mLoggingTxCount);
                                Log.d(TAG, "##Details txPacket = " + DataUtils.byteArrayToHexString(txPacket));
                            }
                            // reset rx packet
                            resetRxPacket();
                            // do any pulses required by the packet
                            processAnyPulses(packet);
                            // write packet
                            _outStream.write(txPacket);
                            // Tx packet write callback process (over speed)
                            processTxHandlerCallback(packet);
                            //set _txCommand
                            _txCommand = packet.getCommand();
                            _txLcbAddr = packet.getTxAddress();

                            //check receuve RxPacket timeout time
                            synchronized (_notifyRxPacketSync) {
                                if ((_txCommand.Value == LcbCommands.SetLCBCmdEUPsMode.Value)
                                        || (_txCommand.Value == LcbCommands.SetLCBCmdSerialNumber.Value))
                                    _notifyRxPacketSync.wait(300);
                                else
                                    _notifyRxPacketSync.wait(RECEIVE_TIMEOUT_MS);
                            }
                            if (!_isRunning) {
                                break;
                            }
                            // process packet if available
                            if (_rxPacketQueue.size() > 0) {
                                // delay before sending next command to give LCB time to prepare to receive next command
                                // do we have a spec for this? how did we pick this value?
                                try {
                                    Thread.sleep(CMD_DELAY_TIME_MS);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                                LcbPacket rxPacket = _rxPacketQueue.remove();
                                //set _rxCommand
                                LcbCommands _rxCommand = rxPacket.getCommand();
                                //check _txCommand and _rxCommand are same
                                //reset retry total counter limited
                                _retryTotalCounter = 0;
                                if (_txCommand.Value != _rxCommand.Value) {//_tx _rx Command not same Retry
//					Log.d(TAG, "Rx = " + _rxCommand + " receive rxPacket not same Tx = " + _txCommand + " commands.");
                                    processPacket(rxPacket);
                                    break;
                                } else {
                                    if(_txLcbAddr == IR_BOARD_ADDRESS_BYTE) {
                                        processIrAddrPacket(rxPacket);
                                    } else {
                                        processPacket(rxPacket);
                                    }
                                    break;
                                }
                            } else {
                                //Log.e(TAG, "Tx[" + _txCommand + "] No result, retrying..." + retries);
                                //timeout or no rxPacket retry delay 50 ms
                                try {
                                    Thread.sleep(RETRY_DELAY_TIME_MS);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                            //counter total tries
                            _retryTotalCounter++;
                            mStats.sentMessageRetry();//
                            retries++; //for one command
                        }
                        if (retries >= LIMITED_ONE_CMD_COMM_RETRIES) {
                            //Log.e(TAG, "Tx[" + _txCommand + "] retry 3 times, command failed!");
                            //Check Wake Up Lcb process
                            if (_txCommand.Value == LcbCommands.SetLCBCmdEUPsMode.Value) {
                                if (procLeaveErpBlockedLcbError())
                                    mLcbProcComm.procCommLcbErrorHandler();
                            } else if (_txCommand.Value == LcbCommands.GetLCBStatus.Value) {
                                if (procLeaveErpCheckLcbStatus())
                                    mLcbProcComm.procCommLcbErrorHandler();
                            } /*else if (_txCommand.Value == LcbCommands.EnterLCBUpdateMode.Value) {
				synchronized (_receiveListenerList) {
	                                for (ILcbCommReceive listener : _receiveListenerList) {
	                                    if (listener != null) {
	                                        listener.enterLcbUpdateModeHandler(true);
	                                    }
	                                }
				}
                            }*/
                        }

                        if (_retryTotalCounter >= LIMITED_TOTAL_RETRY_COUNTER) {
                            //Log.e(TAG, "Retry 25 times , Communication error!");
                            mLcbProcComm.procCommLcbErrorHandler();
                            mStats.sentMessageFail();//
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                _isRunning = false;
                _bCommThreadExceptFlag = false;
                Log.e(TAG, "Exception in communication thread! ", ex);
            }

            Log.v(TAG, "CommunicationThread-");
        }
    }

    private void processAnyPulses(LcbPacket packet) {
        if (packet.getSpecialOperation() == LcbPacket.SPECIAL_400MS_PULSE) {
            // do the wake pulse (400ms) - expect 20-40ms of OS overhead with sleep (in discussion spec seems to be interpreted as "at least" 400)
            try {
                _port.serialBreak(true);
                Thread.sleep(405); // if it must be at least 400ms, do we add a little for safety or is OS latency a given?
                _port.serialBreak(false);
                Thread.sleep(10); // the delay between pulse and start of packet is not defined, just give LCB receiver some amount of time?
            } catch (Exception e) {
                Log.e(TAG, "processAnyPulses SPECIAL_400MS_PULSE", e);
            }
        }
    }

    private void processTxHandlerCallback(LcbPacket packet) {
        LcbCommands txCommand = packet.getCommand();
        if(txCommand.Value == LcbCommands.SetLCBMotorRPM.Value) {
            mLcbProcComm.procSetTreadMillRpmValueHandler(packet);
        }else if(txCommand.Value == LcbCommands.SetLCBInclineLoc.Value) {
            mLcbProcComm.procSetTreadMillAdcValueHandler(packet);
        }
    }

    /*******************************************************************************
     * Function Name  : ResetUartPortThread
     * Description    : ResetUartPort Thread class
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private class ResetUartPortThread extends Thread {
        ResetUartPortThread() { super ("ResetUartThread"); }
        @Override
        public void run() {
            super.run();

            Log.v(TAG, "ResetUartPortThread+++");
            _isRunning = true;

            while (_isRunning && !isInterrupted()) {
                try {
                    synchronized (_notifyStartUartPortFlashSync) {
                        _notifyStartUartPortFlashSync.wait();
                    }
                    uartPortFlashStop();

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }

                    uartPortFlashStart();

                } catch (Exception ex) {
                    _isRunning = false;
                    //Log.e(TAG, "Reset UART UartPortThread: ", ex);
                    Log.w(TAG, ex);
                }
            }

            Log.v(TAG, "ResetUartPortThread---");
        }
    }

    /*******************************************************************************
     * Function Name  : UartPortErpStop
     * Description    : Reset UartPort Erp Stop
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void UartPortErpStop() {
        if (_isPortOpened) {

            try {
                _port.close();
            } catch (IOException ignored) {
            }
            _isPortOpened = false;

            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

        }
    }

    /*******************************************************************************
     * Function Name  : ErpStopUartPortThread
     * Description    : Erp Stop UartPort Thread class
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private class ErpStopUartPortThread extends Thread {
        ErpStopUartPortThread() { super("LcbErpStopThread"); }
        @Override
        public void run() {
            super.run();

            Log.v(TAG, "ErpStopUartPortThread++++");
            _isRunning = true;

            while (_isRunning && !isInterrupted()) {
                try {
                    synchronized (_notifyErpStopUartPortSync) {
                        _notifyErpStopUartPortSync.wait();
                    }
                    UartPortErpStop();

                } catch (Exception ex) {
                    _isRunning = false;
                    //Log.e(TAG, "Erp Stop UartPort Thread: ", ex);
                    Log.w(TAG, ex);
                }
            }

            Log.v(TAG, "ErpStopUartPortThread----");
        }
    }

    /*******************************************************************************
     * Function Name  : checkCommunicationThread()
     * Description    : get CommunicationThread _isRunning state
     * Input          : None
     * Output         : None
     * Return         : the state of _isRunning
     *******************************************************************************/
    @Override
    public boolean checkCommunicationThread() {
        return _isRunning;
    }

    /*******************************************************************************
     * Function Name  : checkCommThreadExcept()
     * Description    : get CommunicationThread _bCommThreadExceptFlag state
     * Input          : None
     * Output         : None
     * Return         : the state of _bCommThreadExceptFlag
     *******************************************************************************/
    @Override
    public boolean checkCommThreadExcept() {
        return _bCommThreadExceptFlag;
    }

    /*******************************************************************************
     * Function Name  : getTransmitQueueSize()
     * Description    : get the size of the transmit queue
     * Input          : None
     * Output         : None
     * Return         : the size of the transmit queue
     *******************************************************************************/
    @Override
    public int getTransmitQueueSize() {
        return _txPacketQueue.size();
    }

    /*******************************************************************************
     * Function Name  : checkPacketInitial()
     * Description    : check packet initial
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void checkPacketInitial() {
        _bNotSupportCmd = false;
    }


    /*******************************************************************************
     * Function Name  : checkStatusByteDefinition()
     * Description    : check LCB status byte every bit definition
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void checkStatusByteDefinition(LcbPacket packet) {

        byte byteLcbStatus = packet.getRxLcbStatus();


        //first check lcb status Lcb error status , only error bit 1 to process
//	if(!_bBlockedFlag){
        if (Constants.LCB_STATUS_BIT.FILTER_LCB_STATUS_B6_BIT_LCB_ERROR.getByteValue() == (byteLcbStatus & Constants.LCB_STATUS_BIT.FILTER_LCB_STATUS_B6_BIT_LCB_ERROR.getByteValue())) {    //error
            _bLcbErrorStatus = true;
            mLcbProcComm.procChangeLcbLcbErrorStatusDetectedHandler(true);
        } else {    //correct
            _bLcbErrorStatus = false;
            mLcbProcComm.procChangeLcbLcbErrorStatusDetectedHandler(false);
        }
//	}
        
/*	if(!_bBlockedFlag){
	        if (Constants.LCB_STATUS_BIT.FILTER_LCB_STATUS_B6_BIT_LCB_ERROR.getByteValue() == (byteLcbStatus & Constants.LCB_STATUS_BIT.FILTER_LCB_STATUS_B6_BIT_LCB_ERROR.getByteValue())) {    //error
	            if (!_bLcbErrorStatus) {
	                _bLcbErrorStatus = true;
	                mLcbProcComm.procChangeLcbLcbErrorStatusDetectedHandler(true);
	            }
	        }
	        else {    //correct
	            if (_bLcbErrorStatus) {
	                _bLcbErrorStatus = false;
	                mLcbProcComm.procChangeLcbLcbErrorStatusDetectedHandler(false);
	            }
	        }
	}
*/
        //check lcb status incline motor downward
        if (Constants.LCB_STATUS_BIT.FILTER_LCB_STATUS_B0_BIT_INCLINE_DOWN_ACTION.getByteValue() == (byteLcbStatus & Constants.LCB_STATUS_BIT.FILTER_LCB_STATUS_B0_BIT_INCLINE_DOWN_ACTION.getByteValue())) {    //action
            if (!_bInclineMotorDownwardStatus) {
                _bInclineMotorDownwardStatus = true;
                mLcbProcComm.procChangeLcbInclineMotorDownwardStatusHandler(true);
            }
        } else {    //stop
            if (_bInclineMotorDownwardStatus) {
                _bInclineMotorDownwardStatus = false;
                mLcbProcComm.procChangeLcbInclineMotorDownwardStatusHandler(false);
            }
        }

        //check lcb status incline motor upward
        if (Constants.LCB_STATUS_BIT.FILTER_LCB_STATUS_B1_BIT_INCLINE_UP_ACTION.getByteValue() == (byteLcbStatus & Constants.LCB_STATUS_BIT.FILTER_LCB_STATUS_B1_BIT_INCLINE_UP_ACTION.getByteValue())) {    //action
            if (!_bInclineMotorUpwardStatus) {
                _bInclineMotorUpwardStatus = true;
                mLcbProcComm.procChangeLcbInclineMotorUpwardStatusHandler(true);
            }
        } else {    //stop
            if (_bInclineMotorUpwardStatus) {
                _bInclineMotorUpwardStatus = false;
                mLcbProcComm.procChangeLcbInclineMotorUpwardStatusHandler(false);
            }
        }

        //check lcb status main motor status
        if (Constants.LCB_STATUS_BIT.FILTER_LCB_STATUS_B2_BIT_MAIN_MOTOR_ACTION.getByteValue() == (byteLcbStatus & Constants.LCB_STATUS_BIT.FILTER_LCB_STATUS_B2_BIT_MAIN_MOTOR_ACTION.getByteValue())) {    //action
            if (!_bMainMotorStatus) {
                _bMainMotorStatus = true;
                mLcbProcComm.procChangeLcbMainMotorStatusHandler(true);
            }
        } else {    //stop
            if (_bMainMotorStatus) {
                _bMainMotorStatus = false;
                mLcbProcComm.procChangeLcbMainMotorStatusHandler(false);
            }
        }

        //check lcb status encoder. RPM status
        //( Note : This bit will be to indicate the power source when the control board is an AC / DC (self-powered) system for bike series. 1 : AC 0: DC )
        if (Constants.LCB_STATUS_BIT.FILTER_LCB_STATUS_B3_BIT_POWER_STATUS_AC.getByteValue() == (byteLcbStatus & Constants.LCB_STATUS_BIT.FILTER_LCB_STATUS_B3_BIT_POWER_STATUS_AC.getByteValue())) {    //AC
            if (!_bEncoderRPMStatus) {
                _bEncoderRPMStatus = true;
                mLcbProcComm.procChangeLcbEncoderRPMStatusHandler(true);
            }
        } else {    //DC
            if (_bEncoderRPMStatus) {
                _bEncoderRPMStatus = false;
                mLcbProcComm.procChangeLcbEncoderRPMStatusHandler(false);
            }
        }

        //check lcb status battery capacity
        if (Constants.LCB_STATUS_BIT.FILTER_LCB_STATUS_B4_BIT_BATTERY_CAP_NOT_ENOUGH.getByteValue() == (byteLcbStatus & Constants.LCB_STATUS_BIT.FILTER_LCB_STATUS_B4_BIT_BATTERY_CAP_NOT_ENOUGH.getByteValue())) {    //not enough
            if (!_bBatteryCapacityStatus) {
                _bBatteryCapacityStatus = true;
                mLcbProcComm.procChangeLcbBatteryCapacityStatusHandler(true);
            }
        } else {    //enough
            if (_bBatteryCapacityStatus) {
                _bBatteryCapacityStatus = false;
                mLcbProcComm.procChangeLcbBatteryCapacityStatusHandler(false);
            }
        }

        //check lcb status command error
        if (Constants.LCB_STATUS_BIT.FILTER_LCB_STATUS_B5_BIT_COMMAND_ERROR.getByteValue() == (byteLcbStatus & Constants.LCB_STATUS_BIT.FILTER_LCB_STATUS_B5_BIT_COMMAND_ERROR.getByteValue())) {    //error
            if (!_bCommandErrorStatus) {
                _bCommandErrorStatus = true;
                mLcbProcComm.procChangeLcbCommandErrorStatusHandler(true);
                _bNotSupportCmd = true; //not support command or command wrong
            }
        } else {    //correct
            if (_bCommandErrorStatus) {
                _bCommandErrorStatus = false;
                mLcbProcComm.procChangeLcbCommandErrorStatusHandler(false);
            }
        }

        //check lcb status initial (calibration )status
        if (Constants.LCB_STATUS_BIT.FILTER_LCB_STATUS_B7_BIT_CALIBRATION_ACTION.getByteValue() == (byteLcbStatus & Constants.LCB_STATUS_BIT.FILTER_LCB_STATUS_B7_BIT_CALIBRATION_ACTION.getByteValue())) {    //action
            if (!_bInitialCalibrationStatus) {
                _bInitialCalibrationStatus = true;
                mLcbProcComm.procChangeLcbInitialCalibrationStatusHandler(true); //Status byte bit seven will set during initial operating (UCB do nothing)
            }
        } else {    //initial already
            if (_bInitialCalibrationStatus) {
                _bInitialCalibrationStatus = false;
                mLcbProcComm.procChangeLcbInitialCalibrationStatusHandler(false);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : sendPacket
     * Description    : send Packet
     * Input          : LcbPacket to queue, Bool indicating if only one should be queued
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void sendPacket(LcbPacket packet, QueueMode mode) {
        sendPriorityPacket(packet, LcbPacket.PRIORITY_NORMAL, mode);
    }

    private void sendPacket(LcbPacket packet) {
        sendPacket(packet, QueueMode.AlwaysAdd);
    }

    /*******************************************************************************
     *  QueueMode indicates packet adding behavior for sendPacket
     *  Terms: Same = packet is equal match, Similar = packet is same command but can be different data
     *******************************************************************************/
    public enum QueueMode {
        AlwaysAdd,      // just add packet to queue (old default behavior)
        AddIfSameNotQueued, // add only if equal packet is not already there
        ReplaceSimilar, // if command exists in queue, replace it with this one
    }

    /*******************************************************************************
     * Function Name  : sendPriorityPacket
     * Description    : send Priority Packet
     * Input          : LcbPacket , priority // lower means higher priority
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void sendPriorityPacket(LcbPacket packet, int priority, QueueMode mode) {
        packet.setPriority(priority);
        if (mLoggingMsg) {
            Log.v(TAG, String.format("##sendPriorityPacket command Tx = %x , priority = %d.", packet.getCommand().Value, packet.getPriority()));
        }

        // Some log indication if tx queue is exploding
        if (_txPacketQueue.size() > 500) {
            Log.w(TAG, "LCB _txPacketQueue exceeds 500 !! size=" + _txPacketQueue.size()); // could this get spammy? or at that point is it a matter of time before we die anyway?
            // should we go so far as to force the machine to stop and clear queue? do we block all but high priority packets?
            // do we do some kind of emergency cleaning/deduplication of queue?
        }

        mStats.sendingMessage(packet.getCommand());//

        switch (mode) {
            case AlwaysAdd:
                _txPacketQueue.add(packet);
                break;
            case AddIfSameNotQueued:
                _txPacketQueue.addIfSameNotQueued(packet);
                break;
            case ReplaceSimilar:
                _txPacketQueue.replaceCommand(packet);
                break;
        }
        synchronized (_notifyTxPacketSync) {
            _notifyTxPacketSync.notify();
        }

    }

    private void sendPriorityPacket(LcbPacket packet, int priority) {
        sendPriorityPacket(packet, priority, QueueMode.AlwaysAdd);
    }

    /*******************************************************************************
     * Function Name  : setLcbInitial()
     * Description    : Initialize the LCB. Command ID 0x70
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbInitial() {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBInitialize);
        sendPacket(packet);
    }

    @Override
    public void setLcbInitialHighestPriority() {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBInitialize);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGHEST);
    }

    /*******************************************************************************
     * Function Name  : getLcbStatus()
     * Description    : Get LCB status byte. Command ID 0x71
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbStatus(QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBStatus);
        sendPacket(packet, mode);
    }

    @Override
    public void getLcbStatusLowPriority(QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBStatus);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_LOW, mode);
    }

    /*******************************************************************************
     * Function Name  : getLcbErrorCode()
     * Description    : Get the LCB most recent error code. Command ID 0x72
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbErrorCode() {
        //Log.v(TAG, "Get LCB error code");
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBErrorCode);
        sendPacket(packet);
    }

    @Override
    public void getLcbErrorCodeHighPriority() {
        //Log.v(TAG, "HighPriority Get LCB error code");
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBErrorCode);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGH);
    }

    /*******************************************************************************
     * Function Name  : getLcbVersion()
     * Description    : Get the LCB firmware revision. Command ID 0x73
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbVersion() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBVersion);
        sendPacket(packet);
    }

    @Override
    public void getLcbVersionHighestPriority() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBVersion);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGHEST);
    }

    /*******************************************************************************
     * Function Name  : setLcbCalibrate()
     * Description    : Force the LCB to perform a calibration. Command ID 0x74
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbCalibrate() {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBCalibrate);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbUpdateProgram(byte[] bytesUpdateProgram )
     * Description    : Set UCB sent the JS to LCB. Then LCB shall response "Y" if the LCB supported the program update function
     * For LCBA : "JS" LCB MCU-B "JT" LCB MCU-B Loader "JA" LCB MCU-A "JB" LCB MCU-A Loader Command ID 0x75
     * Input          : byte[] bytesUpdateProgram 2 bytes
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbUpdateProgram(byte[] bytesUpdateProgram) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBUpdateProgram);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        for (byte byteProgram : bytesUpdateProgram) {
            byteBuffer.put(byteProgram);
        }

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbSkipCurrError()
     * Description    : UCB received the error code already the LCB to allow to skip current error. Command ID 0x76
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbSkipCurrError() {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBSkipCurrError);
        sendPacket(packet);
    }

    @Override
    public void setLcbSkipCurrErrorHighPriority() {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBSkipCurrError);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGH);
    }


    /*******************************************************************************
     * Function Name  : setLcbSpeExtCmd(byte byteCmd )
     * Description    : Special extend command. Command ID 0x77
     * Input          : byte byteCmd
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbSpeExtCmd(byte byteCmd) {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBSpeExtCmd);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put(byteCmd);
        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : getLcbDciVersion()
     * Description    : Get DCI Version( DCI LCB only). Command ID 0x78
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbDciVersion() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBDciVersion);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : getLcbDciEnv()
     * Description    :Get DCI Environment. Command ID 0x79
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbDciEnv() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBDciEnv);
        sendPacket(packet);
    }

//ERP enter/leave function , Chimera project no used ======>

    /*******************************************************************************
     * Function Name  : enterLcbErpMode()
     * Description    : Set LCB enter ERP mode. Message ID 0x07 and erpState 0xFF
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void enterLcbErpMode() {
        setLcbCmdEUPsMode(ENTER_ERP, false);
        _lcbErpState = ENTER_ERP;
    }

    /*******************************************************************************
     * Function Name  : leaveLcbErpMode()
     * Description    : Set LCB leave ERP mode. Message ID 0x07 and erpState 0x00
     * Input          : String lcb_device_path, int lcb_baud_rate
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void leaveLcbErpMode(String lcb_device_path, SerialPortConfiguration.BaudRate lcb_baud_rate) {
        //Wake up LCB
        start(createLcbPort(lcb_device_path, lcb_baud_rate));
        setLcbCmdEUPsMode(LEAVE_ERP, true);
        _lcbErpState = LEAVE_ERP;
        blockedLcbError();
    }

    /*******************************************************************************
     * Function Name  : setLcbCmdEUPsMode(byte byteCmd )
     * Description    : Set CmdEUPsMode command. Command ID 0x7A
     * Input          : byte byteCmd, boolean doPulse
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbCmdEUPsMode(byte byteCmd, boolean doPulse) {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBCmdEUPsMode);
        if (doPulse) packet.setSpecialOperation(LcbPacket.SPECIAL_400MS_PULSE);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put(byteCmd);
        packet.setData(data);
        if (byteCmd == LEAVE_ERP) sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGHEST); // cannot process any commands until woken!
        else sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : procSetLcbCmdEUPsModeHandler()
     * Description    : process set LCB  CmdEUPsMode command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void procSetLcbCmdEUPsModeHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_CMD_EUPS_MODE) {
            Log.v(TAG, "Data Set CmdEUPsMode received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Byte byteRtn = byteBuffer.get();
        {
            synchronized (_receiveListenerList) {
                int listSize = _receiveListenerList.size();
                for (int j = 0; j < listSize; j++) {
                    ILcbCommReceive listener = _receiveListenerList.get(j);

                    if (listener != null) {
                        listener.setLcbCmdEUPsModeHandler(byteRtn);
                    }
                }
            }
        }

        Byte byteLcbErpMode = byteRtn;

        if (_isBootUpLeaveErp) {//check boot up lcb leave erp
            stopBootUpLeaveErpTimer();
            if (byteLcbErpMode == (byte) 0x00) { //LCB not support ERP mode
                Log.v(TAG, "Boot up Data LCB ERP Mode not supported  !!");
                if (_lcbErpState == LEAVE_ERP) {
                    unblockedLcbError();
                    synchronized (_receiveListenerList) {
                        int listSize = _receiveListenerList.size();
                        for (int j = 0; j < listSize; j++) {
                            DataMatrix.sendEvent(HALKeys.ACTION_IO_LEAVE_ERP);
                            ILcbCommReceive listener = _receiveListenerList.get(j);

                            if (listener != null)
                                listener.bootUpLeaveLcbErpHandler(false);
                        }
                    }
                }
                mLcbProcComm.procCommLcbErrorHandler();
            } else if (byteLcbErpMode == (byte) 0x01) { //LCB enter/leave ERP mode success
                if (_lcbErpState == LEAVE_ERP) { // leave ERP return success
                    Log.v(TAG, "Boot up leave LCB ERP success.");
                    //When leave ERP mode success callback to bootUpLeaveLcbErpHandler(true)
                    unblockedLcbError();
                    synchronized (_receiveListenerList) {
                        int listSize = _receiveListenerList.size();
                        for (int j = 0; j < listSize; j++) {
                            ILcbCommReceive listener = _receiveListenerList.get(j);

                            if (listener != null)
                                listener.bootUpLeaveLcbErpHandler(true);
                        }
                    }
                }
            } else if (byteLcbErpMode == (byte) 0xA1) { //LCB leave ERP mode fail
                Log.v(TAG, "Boot up leave LCB ERP fail !!");
                if (_lcbErpState == LEAVE_ERP) { // leave ERP return fail
                    unblockedLcbError();
                    synchronized (_receiveListenerList) {
                        int listSize = _receiveListenerList.size();
                        for (int j = 0; j < listSize; j++) {
                            ILcbCommReceive listener = _receiveListenerList.get(j);

                            if (listener != null)
                                listener.bootUpLeaveLcbErpHandler(false);
                        }
                    }
                }
                mLcbProcComm.procCommLcbErrorHandler();
            }
            _isBootUpLeaveErp = false;
        } else { //console suspend then resume leave erp mode
            if (byteLcbErpMode == (byte) 0x00) { //LCB not support ERP mode
                Log.v(TAG, "Data LCB ERP Mode not supported  !!");

                if (_lcbErpState == ENTER_ERP) {
                    synchronized (_receiveListenerList) {
                        int listSize = _receiveListenerList.size();
                        for (int j = 0; j < listSize; j++) {
                            ILcbCommReceive listener = _receiveListenerList.get(j);

                            if (listener != null) {
                                listener.enterLcbErpModeHandler(false);
                            }
                        }
                    }
                } else if (_lcbErpState == LEAVE_ERP) {
                    unblockedLcbError();
                    synchronized (_receiveListenerList) {
                        int listSize = _receiveListenerList.size();
                        for (int j = 0; j < listSize; j++) {
                            ILcbCommReceive listener = _receiveListenerList.get(j);

                            if (listener != null)
                                listener.leaveLcbErpModeHandler(true, false);
                        }
                    }
                }
            } else if (byteLcbErpMode == (byte) 0x01) { //LCB enter/leave ERP mode success
                if (_lcbErpState == ENTER_ERP) { // enter ERP return success
                    if (!ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.UltraThin)) { // ERP wake mod for UT requires we keep the port open?
                        stop();
                        SystemClock.sleep(500);
                    }
                    synchronized (_receiveListenerList) {
                        int listSize = _receiveListenerList.size();
                        for (int j = 0; j < listSize; j++) {
                            ILcbCommReceive listener = _receiveListenerList.get(j);

                            if (listener != null) {
                                listener.enterLcbErpModeHandler(true);
                            }
                        }
                    }
                } else if (_lcbErpState == LEAVE_ERP) { // leave ERP return success
                    Log.v(TAG, "leave LCB ERP Mode then check lcb status.");
                    //When leave ERP mode success , must retry getLcbStatus(). If ok can callback to leaveLcbErpModeHandler(true,true)
                    getLcbStatus(QueueMode.AlwaysAdd);
                }
            } else if (byteLcbErpMode == (byte) 0xA1) { //LCB enter/leave ERP mode fail
                if (_lcbErpState == ENTER_ERP) { // enter ERP return fail
                    synchronized (_receiveListenerList) {
                        int listSize = _receiveListenerList.size();
                        for (int j = 0; j < listSize; j++) {
                            ILcbCommReceive listener = _receiveListenerList.get(j);

                            if (listener != null) {
                                listener.enterLcbErpModeHandler(false);
                            }
                        }
                    }
                } else if (_lcbErpState == LEAVE_ERP) { // leave ERP return fail
                    unblockedLcbError();
                    synchronized (_receiveListenerList) {
                        int listSize = _receiveListenerList.size();
                        for (int j = 0; j < listSize; j++) {
                            ILcbCommReceive listener = _receiveListenerList.get(j);

                            if (listener != null)
                                listener.leaveLcbErpModeHandler(true, false);
                        }
                    }
                }
            }
        }


    }

    /*******************************************************************************
     * Function Name  : blockedLcbError()
     * Description    : process when leaveLcbErpMode, wake up LCB ERP not trigger communication timeout
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void blockedLcbError() {
        _bBlockedFlag = true;

        _retryTotalCounter = 0;
    }

    /*******************************************************************************
     * Function Name  : unblockedLcbError()
     * Description    : process when wake up LCB ERP finish then unblocked communication timeout
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void unblockedLcbError() {
        _bBlockedFlag = false;

        _retryTotalCounter = 0;
    }

    /*******************************************************************************
     * Function Name  : procLeaveErpBlockedLcbError()
     * Description    : process blocked LCB error
     * Input          : None
     * Output         : None
     * Return         : boolean communication error
     *******************************************************************************/
    private boolean procLeaveErpBlockedLcbError() {
        boolean rtnCommErr = false;

        if ((_bBlockedFlag) && (_lcbErpState == LEAVE_ERP)) {
            _wakeUpLcbCounter++;
            if (_wakeUpLcbCounter < WAKE_UP_LCB_RETRIES) {
                synchronized (_receiveListenerList) {
                    int listSize = _receiveListenerList.size();
                    for (int j = 0; j < listSize; j++) {
                        ILcbCommReceive listener = _receiveListenerList.get(j);

                        if (listener != null)
                            listener.leaveLcbErpModeHandler(false, false);//callback lcb comm fail leaveLcbErpMode false
                    }
                }
            } else {
                _wakeUpLcbCounter = 0;
                unblockedLcbError();
                rtnCommErr = true; //that will callback timeout 0x04B0
                synchronized (_receiveListenerList) {
                    int listSize = _receiveListenerList.size();
                    for (int j = 0; j < listSize; j++) {
                        ILcbCommReceive listener = _receiveListenerList.get(j);

                        if (listener != null)
                            listener.leaveLcbErpModeHandler(false, false);//callback lcb comm fail leaveLcbErpMode false
                    }
                }
            }
        }

        return rtnCommErr;
    }

    /*******************************************************************************
     * Function Name  : procLeaveErpCheckLcbStatus()
     * Description    : process leave ERP then check LCB status
     * Input          : None
     * Output         : None
     * Return         : boolean communication error
     *******************************************************************************/
    private boolean procLeaveErpCheckLcbStatus() {
        boolean rtnCommErr = false;

        if ((_bBlockedFlag) && (_lcbErpState == LEAVE_ERP)) {
            _leaveErpGetLcbStatusCounter++;
            if (_leaveErpGetLcbStatusCounter < LEAVE_ERP_LCB_STATUS_RETRIES) {
                //retry getLcbStatus()
                getLcbStatus(QueueMode.AlwaysAdd);
            } else {
                unblockedLcbError();
                rtnCommErr = true; //that will callback timeout 0x04B0
                Log.v(TAG, "enter LCB ERP Mode then check lcb status fail , lcb communication error .");
                synchronized (_receiveListenerList) {
                    int listSize = _receiveListenerList.size();
                    for (int j = 0; j < listSize; j++) {
                        ILcbCommReceive listener = _receiveListenerList.get(j);

                        if (listener != null)
                            listener.leaveLcbErpModeHandler(false, true);//callback lcb comm fail leaveLcbErpMode false
                    }
                }
            }
        }

        return rtnCommErr;
    }

    /*******************************************************************************
     * Function Name  : procLeaveErpFinish()
     * Description    : process leave ERP then check LCB status OK callback
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void procLeaveErpFinish() {

        if ((_bBlockedFlag) && (_lcbErpState == LEAVE_ERP)) {
            unblockedLcbError();
            synchronized (_receiveListenerList) {
                int listSize = _receiveListenerList.size();
                for (int j = 0; j < listSize; j++) {
                    ILcbCommReceive listener = _receiveListenerList.get(j);

                    if (listener != null)
                        listener.leaveLcbErpModeHandler(true, true);//callback lcb comm success leaveLcbErpMode success
                }
            }
        }
    }

    /*******************************************************************************
     * Function Name  : checkBootUpLeaveLcbErp()
     * Description    : When boot up, check LCB leave ERP mode. Message ID 0x7A and erpState 0x00
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void checkBootUpLeaveLcbErp() {
        _isBootUpLeaveErp = true;

        try {
            if (_bootUpLeaveErpTimer == null) {
                _bootUpLeaveErpTimer = new Timer("LcbBootErpTimer");
            }
            _bootUpLeaveErpTimer.schedule(new BootUpLeaveErpTimerTask(), BOOT_UP_LEAVE_ERP_RETRY_PERIOD, BOOT_UP_LEAVE_ERP_RETRY_PERIOD);
        } catch (Exception ex) {
            // Sometimes the timer has been cancelled and throws an error
            Log.w(TAG, ex);
        }
        _lcbErpState = LEAVE_ERP;
        blockedLcbError();
    }

    private class BootUpLeaveErpTimerTask extends TimerTask {

        @Override
        public void run() {
            if (_bootUpLeaveErpTimeCounter < BOOT_UP_LEAVE_ERP_RETRIES) {
                if (_isBootUpLeaveErp) {
                    //Wake up LCB when retry LeaveErp once
                    setLcbCmdEUPsMode(LEAVE_ERP, _bootUpLeaveErpTimeCounter == 1);
                    _bootUpLeaveErpTimeCounter++;
                } else {
                    stopBootUpLeaveErpTimer();
                }
            } else {
                Log.e(TAG, "Boot Up Leave ERP Retry 3 times , Communication error!");
                stopBootUpLeaveErpTimer();
                unblockedLcbError();
                mLcbProcComm.procCommLcbErrorHandler();
            }
        }
    }

    private void stopBootUpLeaveErpTimer() {
        if (_bootUpLeaveErpTimer != null) {
            _bootUpLeaveErpTimer.cancel();
            _bootUpLeaveErpTimer.purge();
            _bootUpLeaveErpTimer = null;
        }

        _bootUpLeaveErpTimeCounter = 0;
    }

//<=====ERP enter/leave function , Chimera project no used

    /*******************************************************************************
     * Function Name  : setLcbCmdConsolePower(int watts )
     * Description    : Set the console power to LCB(unit 0.1Watt). Command ID 0x7B
     * Input          : int watts
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbCmdConsolePower(int watts) {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBCmdConsolePower);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) watts);
        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : getLcbProtocolVersion()
     * Description    : Get digital communication protocol version (0x0064 => v1.00). Command ID 0x7C
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbProtocolVersion() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBProtocolVersion);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbCmdSerialNumber()
     * Description    : Set frame serial number. Command ID 0x7D
     * Input          : serial (ex. FTM520C1801010000)
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbCmdSerialNumber(byte[] bytesCmdData) {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBCmdSerialNumber);
        byte[] data = new byte[bytesCmdData.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        for (byte b : bytesCmdData) {
            byteBuffer.put(b);
        }

        packet.setData(data);
        sendPacket(packet);

    }

    /*******************************************************************************
     * Function Name  : setLcbTreadmillOperate()
     * Description    : Set Treadmill Operate. Command ID 0x7E
     * Input          : cmd
     * 				Motor target Speed 2bytes 16 signed RPM
     * 				Incline Position Target 2bytes 16 signed counts
     * 				Reserved 1byte
     * 				Reserved 1byte
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbTreadmillOperate(int treadmillRpm, int inclineLoc) {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBTreadmillOperate);
        byte[] data = new byte[6];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) treadmillRpm);
        byteBuffer.putShort((short) inclineLoc);

        packet.setData(data);
        sendPacket(packet);

    }

    /*******************************************************************************
     * Function Name  : getLcbTreadmillCadence()
     * Description    : Get Cadence (SPM). Command ID 0x7F
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbTreadmillCadence(QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBTreadmillCadence);
        sendPacket(packet, mode);
    }

    /*******************************************************************************
     * Function Name  : setLcbMotorSpeedRtnRPM(int intSpeed )
     * Description    : Motor speed setting and response RPM. Command ID 0xF0
     * Input          : int intSpeed
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbMotorSpeedRtnRPM(int intSpeed) {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBMotorSpeedRtnRPM);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intSpeed);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbMotorRPM(int intRPM )
     * Description    : Set motor RPM. Set the motor RPM
     *	Bit 15 : 0 (Motor control signal in normal)
     *	Bit 15 : 1 (Motor control signal in reverse).Command ID 0xF0
     * Input          : int intRPM
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbMotorRPM(int intRPM) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBMotorRPM);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

//	short shortRPM = (short) intRPM;
        byteBuffer.putShort((short) intRPM);

        packet.setData(data);
        sendPacket(packet);
    }

    @Override
    public void setLcbMotorRPMhighestPriority(int intRPM) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBMotorRPM);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intRPM);

        packet.setData(data);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGHEST, QueueMode.ReplaceSimilar);
    }

    /*******************************************************************************
     * Function Name  : setLcbIncPWMStep(int intPWM )
     * Description    : Increment the PWM step by the specified value 50~500 (0.01M)
     * Input          : int intPWM
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbIncPWMStep(int intPWM) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBIncPWMStep);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intPWM);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbDecPWMStep(int intPWM )
     * Description    : Decrement the PWM step by the specified value 50~500 (0.01M)
     * Input          : int intPWM
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbDecPWMStep(int intPWM) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBDecPWMStep);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intPWM);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbStopPWMStep(int intPWM )
     * Description    : Set PWM step decrement value in Ramp down condition 50~500 (0.01M)
     * Input          : int intPWM
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbStopPWMStep(int intPWM) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBStopPWMStep);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intPWM);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbInclineAction(byte byteAction )
     * Description    : Set incline action
     * 0x00: to stop ; 0x01: to rise ; 0xFF: to drop
     * Input          : byte byteAction
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbInclineAction(byte byteAction) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBInclineAction);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put(byteAction);
        packet.setData(data);
        sendPacket(packet);

    }

    @Override
    public void setLcbInclineActionHighestPriority(byte byteAction) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBInclineAction);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put(byteAction);
        packet.setData(data);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGHEST);

    }

    /*******************************************************************************
     * Function Name  : setLcbInclineLoc(int intLoc )
     * Description    : Set the incline location based on ADC passed as
     * Bit 15 : 0 (Incline motor control signal in normal)
     * Bit 15 : 1 (Incline motor control signal in reverse) 0~32767 (0.01M)
     * Input          : int intLoc
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbInclineLoc(int intLoc) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBInclineLoc);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intLoc);

        packet.setData(data);
        sendPacket(packet);
    }

    @Override
    public void setLcbInclineLochighestPriority(int intLoc) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBInclineLoc);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intLoc);

        packet.setData(data);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGHEST, QueueMode.ReplaceSimilar);
    }

    /*******************************************************************************
     * Function Name  : setLcbOperatingStatus(byte byteStatus )
     * Description    : Set operating status
     * 1: operating 0: stop
     * Input          : byte byteStatus
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbOperatingStatus(byte byteStatus) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBOperatingStatus);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put(byteStatus);
        packet.setData(data);
        sendPacket(packet);

    }

    @Override
    public void setLcbOperatingStatusHighestPriority(byte byteStatus) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBOperatingStatus);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put(byteStatus);
        packet.setData(data);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGHEST);
    }

    /*******************************************************************************
     * Function Name  : getLcbRollerRPM()
     * Description    : Get the roller RPM. Command ID 0xF8
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbRollerRPM() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBRollerRPM);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : getLcbMotorRPM()
     * Description    :Get motor RPM. Bit 15 : 0 (Motor control signal in normal)Bit 15 : 1 (Motor control signal in reverse). Command ID 0xF9
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbMotorRPM(QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBMotorRPM);
        sendPacket(packet, mode);
    }

    @Override
    public void getOneShotLcbMotorRpmHighPriority() {
        isOneShotRpm = true;
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBMotorRPM);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGH);
    }

    /*******************************************************************************
     * Function Name  : getLcbInclineLoc()
     * Description    :Get incline location. Get the incline location ADC value 0~32767. Command ID 0xFA
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbInclineLoc(QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBInclineLoc);
        sendPacket(packet, mode);
    }

    /*******************************************************************************
     * Function Name  : setLcbTuneEndIncline(int intPercentage )
     * Description    : Tune endpoint incline 0~25.5% 0.1%
     * Input          : int intPercentage
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbTuneEndIncline(int intPercentage) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBTuneEndIncline);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intPercentage);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbTuneEndIncline2(int intPercentage )
     * Description    : Tune end point incline 0~25.5% 0.1%
     * Input          : int intPercentage
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbTuneEndIncline2(int intPercentage) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBTuneEndIncline2);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intPercentage);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbInclineStroke(int intPercentage )
     * Description    : Incline stroke setting 0~32768 %
     * Input          : int intPercentage
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbInclineStroke(int intPercentage) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBInclineStroke);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intPercentage);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbCompensationVol(int intVol )
     * Description    : To set a compensation voltage that use for Electromagnetic temperature compensation. Command ID 0xFE
     * Input          : int intVol
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbCompensationVol(int intVol) {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBCompensationVol);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intVol);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbMotorSpecification(int intWatts )
     * Description    : Motor specification set 1100:1.1k W / 1500:1.5k W / 1800:1.8k W
     * Input          : int intWatts
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbMotorSpecification(int intWatts) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBMotorSpecification);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intWatts);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : getLcbMotorType()
     * Description    : Get Motor Type. Command ID 0x91
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbMotorType() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBMotorType);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : getLcbDCIdataMaxLen()
     * Description    : Get DCI data Max Len. Command ID 0x92
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbDCIdataMaxLen() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBDCIdataMaxLen);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbMotorPowerOff(byte byteTime )
     * Description    : Set motor power off
     * Input          : byte byteTime
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbMotorPowerOff(byte byteTime) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBMotorPowerOff);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put(byteTime);
        packet.setData(data);
        sendPacket(packet);

    }

    /*******************************************************************************
     * Function Name  : setLcbDCIforceIncline(byte byteValue )
     * Description    : DCI FORCE INCLINE OPERATION
     * not under AUTO_CALIBRATE, it is supposed to be
     * used for incline calibration only, the effective time of
     * the command is 1000ms +/- 20ms; for normal incline
     * operation use SET_INCLINE_ACTION
     * Input          : byte byteValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbDCIforceIncline(byte byteValue) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBDCIforceIncline);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put(byteValue);
        packet.setData(data);
        sendPacket(packet);

    }

    /*******************************************************************************
     * Function Name  : getLcbTreadmillInUsed()
     * Description    : Get Treadmill In Used. Command ID 0x95
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbTreadmillInUsed(QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBTreadmillInUsed);
        sendPacket(packet, mode);
    }

    /*******************************************************************************
     * Function Name  : getLcbMainMotorInfo()
     * Description    :CmdGetMainMotorInformation. Command ID 0x96
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbMainMotorInfo(QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBMainMotorInfo);
        sendPacket(packet, mode);
    }

    /*******************************************************************************
     * Function Name  : getLcbTreadmillExtStatus()
     * Description    : Get the status of the LCB for Treadmill Ext. Command ID 0x97
     * (LCB Type 0xC6 0xC7 )
     * bit 0 : TM-Estop( 1: open, 0:close)
     * bit 1 : 
     * bit 2 : 
     * bit 3 : 
     * bit 4 : 
     * bit 5 : 
     * bit 6 : 
     * bit 7 : 
     * bit 8 : 
     * bit 9 : 
     * bit 10 : 
     * bit 11 : 
     * bit 12 : 
     * bit 13~15 : N/A
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbTreadmillExtStatus(QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBTreadmillExtStatus);
        sendPacket(packet, mode);
    }

    /*******************************************************************************
     * Function Name  :     setLcbTreadmillEcbAction(byte byteValue )
     * Description    : set Lcb Treadmill Ecb Action Command ID 0x98
     * Input          : byte byteValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbTreadmillEcbAction(byte byteValue) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBTreadmillEcbAction);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put(byteValue);
        packet.setData(data);
        sendPacket(packet);

    }

    @Override
    public void setLcbTreadmillEcbActionHighestPriority(byte byteValue) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBTreadmillEcbAction);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put(byteValue);
        packet.setData(data);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGHEST);

    }


    /*******************************************************************************
     * Function Name  : getLcbDCIinclineRange()
     * Description    : DCI Get Incline Range. Command ID 0xA0
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbDCIinclineRange() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBDCIinclineRange);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbDCIinclineRange(int intRange )
     * Description    : DCI Set Incline Range. Command ID 0xA1
     * Input          : int intRange
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbDCIinclineRange(int intRange) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBDCIinclineRange);
        byte[] data = new byte[4];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putInt((int) intRange);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : getLcbDCIrpmSpeedRange()
     * Description    : DCI Get Rpm Speed Range. Command ID 0xA6
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbDCIrpmSpeedRange() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBDCIrpmSpeedRange);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbDCIrpmSpeedRange(double doubleRange )
     * Description    : DCI Set Rpm Speed Range. Command ID 0xA7
     * Input          : double doubleRange
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbDCIrpmSpeedRange(double doubleRange) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBDCIrpmSpeedRange);
        byte[] data = new byte[8];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putDouble((double) doubleRange);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbUniversal(byte[] bytesData )
     * Description    : Special command for LCB.
     * Input          : int intVol
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbUniversal(byte[] bytesData, QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBUniversal);
        byte[] data = new byte[bytesData.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        for (byte b : bytesData) {
            byteBuffer.put(b);
        }

        packet.setData(data);
        sendPacket(packet, mode);
    }

    /*******************************************************************************
     * Function Name  : setLcbDCIManualCalibration()
     * Description    : DCI Set Manual Calibration. Command ID 0xAB
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbDCIManualCalibration() {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBDCIManualCalibration);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbDeviceData(byte[] bytesDataTransmitted  )
     * Description    : Special command for LCB of Bike. Data Bytes Transmitted. Command ID 0x50
     * Input          : byte[] bytesDataTransmitted
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbDeviceData(byte[] bytesDataTransmitted, QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBDeviceData);
        byte[] data = new byte[bytesDataTransmitted.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        for (byte b : bytesDataTransmitted) {
            byteBuffer.put(b);
        }

        packet.setData(data);
        sendPacket(packet, mode);
    }

    @Override
    public void setLcbDeviceDataHighestPriority(byte[] bytesDataTransmitted) {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBDeviceData);
        byte[] data = new byte[bytesDataTransmitted.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        for (byte b : bytesDataTransmitted) {
            byteBuffer.put(b);
        }

        packet.setData(data);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGHEST);
    }

    /*******************************************************************************
     * Function Name  : setLcbResetECB()
     * Description    : Reset ECB value. Command ID 0x61
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbResetECB() {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBResetECB);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbECBposition(int intPosition )
     * Description    : Set the ECB position
     * Input          : int intPosition
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbECBposition(int intPosition) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBECBposition);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intPosition);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : getLcbECBrpm()
     * Description    : Get ECB RPM. Command ID 0x63
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbECBrpm(QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBECBrpm);
        sendPacket(packet, mode);
    }

    @Override
    public void getOneShotLcbECBrpmHighPriority() {
        isOneShotRpm = true;
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBECBrpm);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGH);
    }

    /*******************************************************************************
     * Function Name  : getLcbECBadc()
     * Description    : Get ECB ADC. Command ID 0x64
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbECBadc() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBECBadc);
        sendPacket(packet);
    }


    /*******************************************************************************
     * Function Name  : setLcbPWMpercent(int intValue )
     * Description    : Set PWM percent
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbPWMpercent(int intValue) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBPWMpercent);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intValue);

        packet.setData(data);
        sendPacket(packet);
    }

    @Override
    public void setLcbPWMpercentHighestPriority(int intValue) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBPWMpercent);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intValue);

        packet.setData(data);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGHEST);
    }

    /*******************************************************************************
     * Function Name  : getLcbBatteryVoltage()
     * Description    : Get Get battery voltage. Command ID 0x66
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbBatteryVoltage() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBBatteryVoltage);
        sendPacket(packet);
    }

    @Override
    public void getLcbBatteryVoltageLowPriority(QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBBatteryVoltage);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_LOW, mode);
    }


    /*******************************************************************************
     * Function Name  : setLcbWatts(int intWatts )
     * Description    : Set the Watts
     * Input          : int intWatts
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbWatts(int intWatts, QueueMode mode) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBWatts);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intWatts);

        packet.setData(data);
        sendPacket(packet, mode);
    }

    @Override
    public void setLcbWattsHighestPriority(int intWatts) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBWatts);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intWatts);

        packet.setData(data);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGHEST);
    }

    /*******************************************************************************
     * Function Name  : setLcbRPMgearRatio(int intRatio )
     * Description    : Set Set RPM gear ratio
     * Input          : int intRatio
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbRPMgearRatio(int intRatio) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBRPMgearRatio);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intRatio);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbGeneratorPolePair(int intPair )
     * Description    : Set the generator pole pair
     * Input          : int intPair
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbGeneratorPolePair(int intPair) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBGeneratorPolePair);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byte bytePair = DataUtils.intToOneByte(intPair);

        byteBuffer.put(bytePair);
        packet.setData(data);
        sendPacket(packet);

    }

    /*******************************************************************************
     * Function Name  : setLcbLimitRPMcharge(int intRPM )
     * Description    : Set limit RPM for charge
     * Input          : int intRPM
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbLimitRPMcharge(int intRPM) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBLimitRPMcharge);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intRPM);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbLimitRPMresistance(int intRPM )
     * Description    : Set limit RPM for resistance
     * Input          : int intRPM
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbLimitRPMresistance(int intRPM) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBLimitRPMresistance);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intRPM);

        packet.setData(data);
        sendPacket(packet);
    }

    @Override
    public void setLcbLimitRPMresistanceHighestPriority(int intRPM) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBLimitRPMresistance);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intRPM);

        packet.setData(data);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGHEST);
    }

    /*******************************************************************************
     * Function Name  : setLcbMachineType(byte byteType )
     * Description    : Set machine type
     * Input          : byte byteType
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbMachineType(byte byteType) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBMachineType);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put(byteType);
        packet.setData(data);
        sendPacket(packet);

    }

    /*******************************************************************************
     * Function Name  : setLcbPowerOff(int intMinute )
     * Description    : Set power off
     * Input          : int intMinute
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbPowerOff(int intMinute) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBPowerOff);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byte byteMinute = DataUtils.intToOneByte(intMinute);

        byteBuffer.put(byteMinute);
        packet.setData(data);
        sendPacket(packet);

    }

    /*******************************************************************************
     * Function Name  : setLcbBatteryChargeLevel(int intPercentage )
     * Description    : Set the battery charge level
     * Input          : int intPercentage
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbBatteryChargeLevel(int intPercentage) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBBatteryChargeLevel);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byte bytePercentage = DataUtils.intToOneByte(intPercentage);

        byteBuffer.put(bytePercentage);
        packet.setData(data);
        sendPacket(packet);

    }

    /*******************************************************************************
     * Function Name  : setLcbMinResistanceInclineWorking(byte byteType )
     * Description    : Set min resistance when incline working
     * Input          : byte byteType
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbMinResistanceInclineWorking(byte byteType) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBMinResistanceInclineWorking);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put(byteType);
        packet.setData(data);
        sendPacket(packet);

    }

    /*******************************************************************************
     * Function Name  : setLcbInclinePercent(int intValue )
     * Description    : Set incline travel speed ( 0: 0% 32768: 100% )
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbInclinePercent(int intValue) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBInclinePercent);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intValue);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : getLcbInclinePercent()
     * Description    : Get incline travel speed ( 0: 0% 32768: 100% ). Command ID 0x81
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbInclinePercent(QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBInclinePercent);
        sendPacket(packet, mode);
    }

    /*******************************************************************************
     * Function Name  : setLcbECBaction(byte byteType )
     * Description    : Set ECB action Data Values 0x01: upward 0x80: downward 0x11: stop
     * Input          : byte byteType
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbECBaction(byte byteType) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBECBaction);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put(byteType);
        packet.setData(data);
        sendPacket(packet);

    }

    /*******************************************************************************
     * Function Name  : getLcbECBstatus()
     * Description    : Get ECB status Data Values (bit 0: ECB up , bit 1: ECB down , bit 2: ECB stop , bit 3: zero , bits 4-7: reserve. Command ID 0x83
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbECBstatus() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBECBstatus);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : getLcbECBcount()
     * Description    : Get ECB count 0~65535. Command ID 0x84
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbECBcount() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBECBcount);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbElectromagneticCurrent(int intmA )
     * Description    : Electromagnetic current (mA ) Command ID 0x85
     * Input          : int intmA
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbElectromagneticCurrent(int intmA) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBElectromagneticCurrent);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intmA);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbResistanceType(int intOhm )
     * Description    : Set Resistance type (0.1ohm ) Command ID 0x86
     * Input          : int intOhm
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbResistanceType(int intOhm) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBResistanceType);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intOhm);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbBeginBatteryCharge(int intmA )
     * Description    : Set the battery charge current of no resistance 0 ~ 1000 (mA) Command ID 0x87
     * Input          : int intmA
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbBeginBatteryCharge(int intmA) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBBeginBatteryCharge);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intmA);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : getLcbBatteryCapacity()
     * Description    : Get battery the percentage of capacity 0 ~ 100 %. Command ID 0x88
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbBatteryCapacity() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBBatteryCapacity);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbClimbmillStatus(byte byteValue )
     * Description    : Set the action (include E-STOP) of the LCB for climbmill. Command ID 0x8A
     * 0x01 : Stop controlling the speed (non-workout set)
     * 0x00 : Start to control the speed (workout set)
     * 0xA0 : the 1st control zone Disable and IR Disable
     * 0xA1 : the 1st control zone Enable and IR Enable
     * 0xA2 : the 1st control zone Disable and IR Enable
     * 0xA3 : the 1st control zone Enable and IR Disable
     * 0xA5 : the 1st control zone Unlock (use when enter E-STOP Mode)
     * 0xA6: the 2nd control zone disable
     * 0xA7: the 2nd control zone ensable
     * 0xB0 : Detect person Disable
     * 0xB1 : Detect person Enable
     * Input          : byte byteValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbClimbmillStatus(byte byteValue) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBClimbmillStatus);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put(byteValue);
        packet.setData(data);
        sendPacket(packet);

    }

    @Override
    public void setLcbClimbmillStatusHighestPriority(byte byteValue) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBClimbmillStatus);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put(byteValue);
        packet.setData(data);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGHEST);

    }

    /*******************************************************************************
     * Function Name  : getLcbClimbmillStatus()
     * Description    : Get the status of the LCB for climbmill. Command ID 0x8B
     * (LCB Type 0x17 before is return 1 byte)
     * bit 0 : the speed control ( 1: start, 0:stop)
     * bit 1 : The function of the 2nd control zone( 0: not use, 1: use)
     * bit 2 : IR Sensor use ( 0: not use, 1: use)
     * bit 3 : the status of the IR Sensor is response at once( 0 : No , 1 : Touch)
     * bit 4 : IR Sensor is once touched( 0 : No , 1 : Touch)
     * bit 5 : The function of the 1st control zone use( 0: not use, 1: use)
     * bit 6 : the status of the control zone is response atonce ( 0 : No , 1 : Touch)
     * bit 7 : control zone is once touched( 0 : No , 1 : Touch)
     * bit 8 : UCB Emergency stop status( 0 : No , 1 : Touch)
     * bit 9 : UCB Emergency stop is once touched( 0 : No , 1 : Touch)
     * bit 10 : Handrail Emergency stop status( 0 : No , 1 : Touch)
     * bit 11 : Handrail Emergency stop is once touched( 0 : No , 1 : Touch)
     * bit 12 : No person during using( 0 : No , 1 : Yes)
     * bit 13~15 : N/A
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbClimbmillStatus(QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBClimbmillStatus);
        sendPacket(packet, mode);
    }

    /*******************************************************************************
     * Function Name  : setLcbEStopParameter(byte byteValue )
     * Description    : Set the E-STOP parameter(4 - 100). Command ID 0x8C
     * Input          : byte byteValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbEStopParameter(byte byteValue) {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBEStopParameter);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put(byteValue);
        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : getLcbBrakePressureValue()
     * Description    : Get brake pressure value. Command ID 0x8D
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbBrakePressureValue(QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBBrakePressureValue);
        sendPacket(packet, mode);
    }

    /*******************************************************************************
     * Function Name  : getLcbWatts()
     * Description    : Get Watts. Command ID 0x8E
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbWatts(QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBWatts);
        sendPacket(packet, mode);
    }

    /*******************************************************************************
     * Function Name  : setLcbClimbmillCal(byte[] bytesClimbmillCal )
     * Description    : Set Calibrate climbmill. When LCB get these input information, LCB will auto start calibration. Command ID 0x8F
     * Input          : byte[] bytesClimbmillCal Weight:200-2000 SPM:200-1700 Time:1-3600
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbClimbmillCal(byte[] bytesClimbmillCal) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBClimbmillCal);
        byte[] data = new byte[6];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        for (byte byteValue : bytesClimbmillCal) {
            byteBuffer.put(byteValue);
        }

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbRecordCurrent(byte[] bytesRecordCurrent )
     * Description    : Set Before calibrating induction watt, first UCB sends acurrent value of one point from watt-talbe to LCB Command ID 0x87
     * Input          : byte[] bytesRecordCurrent Data0:point(0 or 1)Data1-2:current
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbRecordCurrent(byte[] bytesRecordCurrent) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBRecordCurrent);
        byte[] data = new byte[3];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        for (byte byteCurrents : bytesRecordCurrent) {
            byteBuffer.put(byteCurrents);
        }

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbPWM(int intPercentage )
     * Description    : Set PWM 0 - 1000(0.0% -100.0%) Command ID 0x48
     * Input          : int intPercentage
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbPWM(int intPercentage) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBPWM);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intPercentage);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbFinishCalibrating(byte byteValue )
     * Description    : Set finished calibrating one point. Command ID 0x49
     * 0:first point 1:second point
     * Input          : byte byteValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbFinishCalibrating(byte byteValue) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBFinishCalibrating);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put(byteValue);
        packet.setData(data);
        sendPacket(packet);

    }

    /*******************************************************************************
     * Function Name  : setLcbSPM(int intSPM )
     * Description    : Set SPM 0.1 SPM Command ID 0x4A
     * Input          : int intSPM
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbSPM(int intSPM) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBSPM);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intSPM);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : getLcbSPM()
     * Description    : Get SPM. Command ID 0x4B
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbSPM(QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBSPM);
        sendPacket(packet, mode);
    }

    @Override
    public void getOneShotLcbSpmHighPriority() {
        isOneShotRpm = true;
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBSPM);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGH);
    }

    /*******************************************************************************
     * Function Name  : setLcbCalScaling(int intCalScaling )
     * Description    : Set CalScaling 0.1% Command ID 0x4C
     * Input          : int intCalScaling
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbCalScaling(int intCalScaling) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBCalScaling);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intCalScaling);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : getLcbCalScaling()
     * Description    : Get CalScaling. Command ID 0x4D
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbCalScaling(QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBCalScaling);
        sendPacket(packet, mode);
    }

    /*******************************************************************************
     * Function Name  : setLcbFrameCalScaling(int intFrameCalScaling )
     * Description    : Set FrameCalScaling 0.1% Command ID 0x4E
     * Input          : int intFrameCalScaling
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbFrameCalScaling(int intFrameCalScaling) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBFrameCalScaling);
        byte[] data = new byte[2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putShort((short) intFrameCalScaling);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : getLcbFrameCalScaling()
     * Description    : Get FrameCalScaling. Command ID 0x4F
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbFrameCalScaling() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBFrameCalScaling);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : getLcbIndoorBikeSensorData()
     * Description    : Get IndoorBike SensorData. Command ID 0x51
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbIndoorBikeSensorData(QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBIndoorBikeSensorData);
        sendPacket(packet, mode);
    }

    @Override
    public void getOneShotLcbIndoorBikeSensorDataHighPriority() {
        isOneShotSensorData = true;
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBIndoorBikeSensorData);
        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGH);
    }

    /*******************************************************************************
     * Function Name  : setLcbIndoorBikeStepperMotorPosition()
     * Description    : Set stepper motor position. Command ID 0x52
     * Input          : cmd 
     * Byte 0: Step mode (unit)
     * 0x00: full step, 0x01: half step, 0x02: quarter step, 0x03: 1/8 step, 0x04: 1/16 step,
     * Default: 0x04 (1/16 step)
     * Byte 1 ~ Byte 3: stepper motor position
     * Range: 0 ~ 0xFFFFFF, 
     * unit: refer to step mode
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbIndoorBikeStepperMotorPosition(byte[] bytesCmdData) {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBIndoorBikeStepperMotorPosition);
        byte[] data = new byte[bytesCmdData.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        for (byte b : bytesCmdData) {
            byteBuffer.put(b);
        }

        packet.setData(data);
        sendPacket(packet);

    }

    /*******************************************************************************
     * Function Name  : setLcbIndoorBikeCalcParaOfDevice()
     * Description    : Set calculation parameters of device or algorithm. Command ID 0x52
     * Input          : cmd 
     * 1. Set calculation parameters of device or algorithm. Device is the thing that LCB can control or communicate.
     * 2. Calculation parameters are defined by used LCB.
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbIndoorBikeCalcParaOfDevice(byte[] bytesCmdData) {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBIndoorBikeCalcParaOfDevice);
        byte[] data = new byte[bytesCmdData.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        for (byte b : bytesCmdData) {
            byteBuffer.put(b);
        }

        packet.setData(data);
        sendPacket(packet);

    }

    /*******************************************************************************
     * Function Name  : getLcbIndoorBikeCalcParaOfDevice()
     * Description    : Get IndoorBike  calculation parameters of device or algorithm. Command ID 0x54
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbIndoorBikeCalcParaOfDevice() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBIndoorBikeCalcParaOfDevice);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : setLcbIndoorBikeActionSpinDown(byte[] bytesData )
     * Description    : Set the action of spin down . Command ID 0x55
     * 1. control action  for performing spin down
     * 2. Only for indoor cycle use
     * 3. Tare torque sensor(ID: 0x06).
     * 3.1 It is  excuted at 32 crank RPM during spin down.
     * 3.2 If UCB want to tare torque during spin down,
     *          UCB need to send ID 0x06 before spin down.
     * Input          : byte[] bytesDataTransmitted
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbIndoorBikeActionSpinDown(byte[] bytesData) {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBIndoorBikeActionSpinDown);
        byte[] data = new byte[bytesData.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        for (byte b : bytesData) {
            byteBuffer.put(b);
        }

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : getLcbIndoorBikeInfoSpinDown()
     * Description    : Get the info of spin down. Command ID 0x56
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbIndoorBikeInfoSpinDown(QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBIndoorBikeInfoSpinDown);
        sendPacket(packet, mode);
    }


    /*******************************************************************************
     * Function Name  : setLcbIndoorBikeErgModeSettings(byte[] bytesData )
     * Description    : Set Erg Mode Settings . Command ID 0x57
     * 1. Set erg mode (constant power mode) parameters.
     * Input          : byte[] bytesData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbIndoorBikeErgModeSettings(byte[] bytesData) {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBIndoorBikeErgModeSettings);
        byte[] data = new byte[bytesData.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        for (byte b : bytesData) {
            byteBuffer.put(b);
        }

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : getLcbIndoorBikeErgModeSettings()
     * Description    : Get Erg Mode Settings. Command ID 0x59
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getLcbIndoorBikeErgModeSettings() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBIndoorBikeErgModeSettings);
        sendPacket(packet);
    }


    /*******************************************************************************
     * Function Name  : setLcbIndoorBikeResistanceLevel(int intValue )
     * Description    : Set IndoorBike Resistance Level
     * Input          : int intValue
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setLcbIndoorBikeResistanceLevel(int intValue) {

        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBIndoorBikeResistanceLevel);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byte byteValue = DataUtils.intToOneByte(intValue);
        byteBuffer.put(byteValue);

        packet.setData(data);
        sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : procEStopMachineStop()
     * Description    : process E-Stopp error (0x02B2) Machine stop
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void procEStopMachineStop() {
        mLcbProcComm.procEStopErrorHandler();
    }

    /*******************************************************************************
     * Function Name  : procEStopMcbError()
     * Description    : process If UCB get estop signal, But MCB not have return 02B2 error code within 5 seconds. Send 024F error code.
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void procEStopMcbError() {
        mLcbProcComm.procEStopMcbErrorHandler();
    }

    /*******************************************************************************
     * Function Name  : procEStopUnlock()
     * Description    : process Safe key unlock response and initial lcb finish
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void procEStopUnlock() {
        mLcbProcComm.procEStopUnlockHandler();
    }

    /*******************************************************************************
     * Function Name  : procTmNoRpmError()
     * Description    : process If UCB not get RPM from LCB when already set RPM to LCB within 40 seconds.Send 00A1 error.
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void procTmNoRpmError() {
        mLcbProcComm.procTmNoRpmErrorHandler();
    }

    /*******************************************************************************
     * Function Name  : procSetAdvLoggingStatus()
     * Description    : process setAdvLoggingStatus
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void procSetAdvLoggingStatus() {
        byte setLoggingStatus = (byte) mAdvancedLogging.getAdvLoggingStatus();

        if (AdvancedLogging.LOGGING_STATUS_BIT.FILTER_LOGGING_STATUS_B0_BIT_MESSAGES.getByteValue() == (setLoggingStatus & AdvancedLogging.LOGGING_STATUS_BIT.FILTER_LOGGING_STATUS_B0_BIT_MESSAGES.getByteValue())) {
            mLoggingMsg = true;
        } else {
            mLoggingMsg = false;
        }

        if (AdvancedLogging.LOGGING_STATUS_BIT.FILTER_LOGGING_STATUS_B1_BIT_MESSAGE_DETAILS.getByteValue() == (setLoggingStatus & AdvancedLogging.LOGGING_STATUS_BIT.FILTER_LOGGING_STATUS_B1_BIT_MESSAGE_DETAILS.getByteValue())) {
            mLoggingDetail = true;
        } else {
            mLoggingDetail = false;
        }

    }

    /*******************************************************************************
     * Function Name  : getIrAddrVersion()
     * Description    : Get the IR board firmware revision. Addredd 0x26 Command ID 0x73
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getIrAddrVersion() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBVersion);
        packet.setTxAddress(LcbPacket.PACKET_IR_ADDRESS);

        sendPacket(packet);
    }

    @Override
    public void getIrAddrVersionHighestPriority() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetLCBVersion);
        packet.setTxAddress(LcbPacket.PACKET_IR_ADDRESS);

        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGHEST);
    }

    /*******************************************************************************
     * Function Name  : getIrAddrIrStatus(LcbComm.QueueMode mode)
     * Description    : Get the IR board IrStatus. Addredd 0x26 Command ID 0x99
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void getIrAddrIrStatus(LcbComm.QueueMode mode) {
        LcbPacket packet = new LcbPacket(LcbCommands.GetIrAddressIrStatus);
        packet.setTxAddress(LcbPacket.PACKET_IR_ADDRESS);

        sendPacket(packet);
    }

    @Override
    public void getIrAddrIrStatusHighestPriority() {
        LcbPacket packet = new LcbPacket(LcbCommands.GetIrAddressIrStatus);
        packet.setTxAddress(LcbPacket.PACKET_IR_ADDRESS);

        sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGHEST);
    }

    /*******************************************************************************
     * Function Name  : setIrAddrEnterErpMode()
     * Description    : set Ir board Addr Enter Erp Mode. Addredd 0x26 Command ID 0x7A
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setIrAddrEnterErpMode() {
        setIrAddrCmdEUPsMode(IR_ADDR_ENTER_ERP, false);
        _irAddrErpState = IR_ADDR_ENTER_ERP;
    }

    /*******************************************************************************
     * Function Name  : setIrAddrLeaveErpMode()
     * Description    : set Ir board Addr Leave Erp Mode. Addredd 0x26 Command ID 0x7A
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setIrAddrLeaveErpMode() {
        setIrAddrCmdEUPsMode(IR_ADDR_LEAVE_ERP, false);
        _irAddrErpState = IR_ADDR_LEAVE_ERP;
    }

    /*******************************************************************************
     * Function Name  : setIrAddrCmdEUPsMode(byte byteCmd )
     * Description    : Set CmdEUPsMode command. Command ID 0x7A
     * Input          : byte byteCmd, boolean doPulse
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void setIrAddrCmdEUPsMode(byte byteCmd, boolean doPulse) {
        LcbPacket packet = new LcbPacket(LcbCommands.SetLCBCmdEUPsMode);
        packet.setTxAddress(LcbPacket.PACKET_IR_ADDRESS);
        if (doPulse) packet.setSpecialOperation(LcbPacket.SPECIAL_400MS_PULSE);
        byte[] data = new byte[1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.put(byteCmd);
        packet.setData(data);
        if (byteCmd == IR_ADDR_LEAVE_ERP) sendPriorityPacket(packet, LcbPacket.PRIORITY_HIGHEST); // cannot process any commands until woken!
        else sendPacket(packet);
    }

    /*******************************************************************************
     * Function Name  : procSetIrAddrCmdEUPsModeHandler()
     * Description    : process set IrAddr  CmdEUPsMode command
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void procSetIrAddrCmdEUPsModeHandler(LcbPacket packet) {
        byte[] data = packet.getData();

        if (data.length != Constants.RECEIVE_DATA_SIZE_CMD_EUPS_MODE) {
            Log.v(TAG, "Data Set IrAddr CmdEUPsMode received not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        Byte byteRtn = byteBuffer.get();
        {
            synchronized (_receiveListenerList) {
                int listSize = _receiveListenerList.size();
                for (int j = 0; j < listSize; j++) {
                    ILcbCommReceive listener = _receiveListenerList.get(j);

                    if (listener != null) {
                        listener.setIrAddrCmdEUPsModeHandler(byteRtn);
                    }
                }
            }
        }

        Byte byteIrAddrErpMode = byteRtn;

        if (byteIrAddrErpMode == (byte) 0x00) { //IrAddr not support ERP mode
            Log.v(TAG, "Data IrAddr ERP Mode not supported  !!");
            synchronized (_receiveListenerList) {
                int listSize = _receiveListenerList.size();
                for (int j = 0; j < listSize; j++) {
                    ILcbCommReceive listener = _receiveListenerList.get(j);

                    if (listener != null) {

                        if (_irAddrErpState == IR_ADDR_ENTER_ERP) { // enter ERP not support return false
                            listener.enterIrAddrErpModeHandler(false);
                        }else{ // leave ERP not support return false
                            listener.leaveIrAddrErpModeHandler(false);
                        }
                    }
                }
            }
        } else if (byteIrAddrErpMode == (byte) 0x01) { //IrAddr enter/leave ERP mode success
            Log.v(TAG, "Data IrAddr ERP Mode ERP success.");
            synchronized (_receiveListenerList) {
                int listSize = _receiveListenerList.size();
                for (int j = 0; j < listSize; j++) {
                    ILcbCommReceive listener = _receiveListenerList.get(j);

                    if (listener != null) {
                        if (_irAddrErpState == IR_ADDR_ENTER_ERP) { // enter ERP return success
                            listener.enterIrAddrErpModeHandler(true);
                        }else { // leave ERP return success
                            listener.leaveIrAddrErpModeHandler(true);
                        }
                    }
                }
            }
        } else if (byteIrAddrErpMode == (byte) 0xA1) { //IrAddr enter/leave ERP mode wait fail
            Log.v(TAG, "Data IrAddr enter/leave ERP wait fail !!");
            synchronized (_receiveListenerList) {
                int listSize = _receiveListenerList.size();
                for (int j = 0; j < listSize; j++) {
                    ILcbCommReceive listener = _receiveListenerList.get(j);

                    if (listener != null) {
                        if (_irAddrErpState == IR_ADDR_ENTER_ERP) { // enter ERP return wait fail
                            listener.enterIrAddrErpModeHandler(false);
                        }else { // leave ERP return wait fail
                            listener.leaveIrAddrErpModeHandler(false);
                        }
                    }
                }
            }
        }
    }
}
