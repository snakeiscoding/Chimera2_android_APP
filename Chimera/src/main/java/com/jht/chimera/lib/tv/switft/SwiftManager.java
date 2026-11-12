package com.jht.chimera.lib.tv.switft;


import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.lib.tv.TVControllerBase;
import com.jht.chimera.lib.tv.switft.jhttvservice.atsc.ExportChannelListHelper;
import com.jht.chimera.lib.tv.switft.jhttvservice.atsc.ImportChannelListHelper;
import com.jht.chimera.lib.tv.switft.jhttvservice.atsc.UpgradeFirmwareHelper;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.SerialPort;
import com.jht.serialport.SerialPortConfiguration;

import org.jetbrains.annotations.NonNls;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SwiftManager extends SwiftInterface {
    @NonNls private static final String TAG = "SwiftManager";

    //    private final String DEVICE_PATH = "/dev/ttyS4"; // for K1 sample
    private static final String DEVICE_PATH = ProductConfig.portMappings.swiftPath(); // for K2 sample

    //    private static final int                  TRANSMIT_RETRIES = 3;   //follow the Swift TV  communication protocol
    private static final int TRANSMIT_RETRIES = 1;   //follow the Swift TV  communication protocol
    private static final int COMMUNICATION_RETRIES = 25;  //follow the Swift TV  communication protocol
    private static final int RECEIVE_WAIT_REPLY_MS = 250; //milliseconds, follow the Swift TV  communication protocol
    private static final int TX_RETRY_DELAY_TIME_MS = 50;  //milliseconds, follow the Swift TV  communication protocol

    private static final Object _notifyRxPacketSync = new Object();
    private static final Object _notifyTxPacketSync = new Object();

    private volatile boolean _isPortOpened = false;
    private volatile boolean _startParseMsg = false;
    private volatile boolean _isReceiverEnabled = false;
    private volatile int _totalRetryCounter = 0;

    private static InputStream _inStream = null;
    private static OutputStream _outStream = null;

    private static SerialPort _port = null;
    private static SwiftCommands _txMessage = null;
    private static SwiftCommands _rxMessage = null;
    private static Thread _rxThread = null;
    private static Thread _txThread = null;
    private static SwiftPacket _rxPacket = null;
    private static SwiftPacket _receivedPacket = new SwiftPacket();

    private static ConcurrentLinkedQueue<SwiftPacket> _rxPacketQueue = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<SwiftPacket> _txPacketQueue = new ConcurrentLinkedQueue<>();

    DataMatrix mDataMatrix = new DataMatrix();

    public void exportChannels(String filename, TVControllerBase.IOnProgress onProgress) {
        final ExportChannelListHelper helper = new ExportChannelListHelper(filename, onProgress);
        new Handler(Looper.getMainLooper()).postDelayed(helper::start, 1000);  // generations: 10000
    }

    public void importChannels(String filename, TVControllerBase.IOnProgress onProgress) {
        final ImportChannelListHelper helper = new ImportChannelListHelper(filename, onProgress);
        new Handler(Looper.getMainLooper()).postDelayed(helper::start, 1000);  // generations: 10000
    }

    public void updateFirmware(String filename, TVControllerBase.IOnProgress onProgress) {
        final UpgradeFirmwareHelper helper = new UpgradeFirmwareHelper(filename, onProgress);
        new Handler(Looper.getMainLooper()).postDelayed(helper::start, 1000);  // generations: 10000

    }

    public void reboot() {
        Log.w(TAG, "rebooting");
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftReboot);
        addTxPackets(packet);
    }

    private static class SingletonHolder {
        private static SwiftManager Instance = new SwiftManager();
    }

    public static SwiftManager getInstance() {
        return SingletonHolder.Instance;
    }

    /*
        private static SwiftManager SwiftAdapter = null;
        public static SwiftManager getInstance(){
            if(SwiftAdapter == null)
                SwiftAdapter = new SwiftManager();
            return SwiftAdapter;
        }
    */
    @Override
    public void registerSwiftOnReceiveListener(ISwiftCommResponse callback) {

        Log.w(TAG, "registerSwiftOnReceiveListener(), mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        for (int i = 0; i < mSwiftCallbacks.size(); i++) {
            if (mSwiftCallbacks.get(i) == callback)
                return;  //there is already existed in the list.
        }
        Log.w(TAG, "registerSwiftOnReceiveListener(), ADDDDDDDDDDDDDDDDDDDDD");

        mSwiftCallbacks.add(callback);

        Log.w(TAG, "registerSwiftOnReceiveListener(), AFTER ADD, mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

    }

    @Override
    public void unregisterSwiftOnReceiveListener(ISwiftCommResponse callback) {
        Log.w(TAG, "unregisterSwiftOnReceiveListener(), mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        for (int i = 0; i < mSwiftCallbacks.size(); i++) {
            if (mSwiftCallbacks.get(i) == callback)
                mSwiftCallbacks.remove(i);
        }
        Log.w(TAG, "unregisterSwiftOnReceiveListener(), AFTER REMOVED, mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

    }

    private SerialPortConfiguration serialPortSetting() {
        SerialPortConfiguration configuration = new SerialPortConfiguration();
        configuration.baudRate = SerialPortConfiguration.BaudRate.B9600;
        configuration.CREAD = true;
        configuration.CLOCAL = true;
        configuration.csize = SerialPortConfiguration.CSIZE.CS8;
        configuration.makeRaw = true;
        configuration.NOCTTY = true;
/*
        configuration.VTIME = 1;
        configuration.VMIN = 255;
*/
        return configuration;
    }

    @Override
    public boolean open(boolean updateFirmware) {
        if (ProductConfig.portMappings.swiftPath() == null) {
            Log.d(TAG, "tty for Swift is not defined");
            return false;
        }

        if (_isPortOpened) {
            Log.w(TAG, "The serial port had already opened!");
            return true;
        }

        SerialPortConfiguration configuration = new SerialPortConfiguration();
        if (updateFirmware)
            configuration.baudRate = SerialPortConfiguration.BaudRate.B115200;
        else
            configuration.baudRate = SerialPortConfiguration.BaudRate.B9600;
        configuration.CREAD = true;
        configuration.CLOCAL = true;
        configuration.csize = SerialPortConfiguration.CSIZE.CS8;
        configuration.makeRaw = true;
        configuration.NOCTTY = true;

        _port = new SerialPort(DEVICE_PATH, configuration);
        try {
            _port.open();
        } catch (Exception e) {
            Log.e(TAG, "Failed to open serial port", e);
            return false;
        }
        _inStream = _port.getInputStream();
        _outStream = _port.getOutputStream();
        if ((null == _inStream) || (null == _outStream)) {
            Log.w(TAG, "RETURN FALSE!!!!!!!!!!##################");

            return false;
        }
        _rxPacketQueue.clear();
        _txPacketQueue.clear();
        _rxThread = new SwiftRxThread();
        _txThread = new SwiftTxThread();
        _rxThread.start();
        _txThread.start();

        Log.w(TAG, "open: end");
        _isPortOpened = true;
        return _isPortOpened;
    }

    @Override
    public void close() {
        if (_isPortOpened) {
            _rxThread.interrupt();
            _txThread.interrupt();

            if (null != _port) {
                _port.close();
                _port = null;
                _inStream = null;
                _outStream = null;
            }

            // remove all packets from queue
            _rxPacketQueue.clear();
            _txPacketQueue.clear();
            _rxThread = null;
            _txThread = null;
            _isPortOpened = false;

            Log.w(TAG, "close: end");
        }
    }

    private synchronized void setReceiverEnable(boolean enabled) {
        _isReceiverEnabled = enabled;
    }

    private synchronized boolean getReceiverEnable() {
        return _isReceiverEnabled;
    }

    private synchronized void resetRx() {
        // reset rx packet
        _receivedPacket.reset();
        _startParseMsg = false;
    }

    public void unknownCommandHandler(byte[] payload) {
        if ((mSwiftCallbacks.size() > 0) && (payload.length > 0)) {
            Log.d(TAG, "Unknown command: " + SwiftCommands.fromInt(payload[0]));
            for (ISwiftCommResponse cb : mSwiftCallbacks) {
                cb.onSetSwiftNone(payload[0]);
            }
        }
        resetRx();
    }

    public void commLostErrorHandler() {
        resetRx();
        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onErrorCode((byte) 0xB1);
        }
    }

    private void processPacket(SwiftPacket packet) {

        Log.w(TAG, "SwiftManager.java processPacket(), packet.getMessage() = " + packet.getMessage());

        switch (packet.getMessage()) {
            case SetSwiftPowerOn:  //0x0001
                setSwiftPowerOnHandler(packet.getData());
                break;
            case SetSwiftMenu:  //0x0005
                setSwiftMenuHandler(packet.getData());
                break;
            case GetSwiftTunerType:
                Log.w(TAG, "SwiftManager.java processPacket(), packet.getRxIrCode() = 0x" + Integer.toHexString(packet.getRxIrCode() & 0xFF) + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
                getSwiftTunerTypeHandler(packet.getRxIrCode());
                break;
            case GetSwiftTunerTypeVersion:
                Log.w(TAG, "SwiftManager.java processPacket(), packet.getRxIrCode() = 0x" + Integer.toHexString(packet.getRxIrCode() & 0xFF) + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
                getSwiftTunerTypeVersionHandler(packet.getData());
                break;
            case GetSwiftComponentOutputMode:
                Log.w(TAG, "SwiftManager.java processPacket(), packet.getRxIrCode() = 0x" + Integer.toHexString(packet.getRxIrCode() & 0xFF) + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
                getSwiftComponentOutputHandler(packet.getData());
                break;
            case GetSwiftHDMIOutputMode:
                Log.w(TAG, "SwiftManager.java processPacket(), packet.getRxIrCode() = 0x" + Integer.toHexString(packet.getRxIrCode() & 0xFF) + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
                getSwiftHDMIOutputHandler(packet.getData());
                break;
            case GetSwiftStatus:
                Log.w(TAG, "SwiftManager.java processPacket(), packet.getRxIrCode() = 0x" + Integer.toHexString(packet.getRxIrCode() & 0xFF) + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
                getSwiftStatusHandler(packet.getRxIrCode());
                break;
            case GetSwiftManufacturer:
                Log.w(TAG, "SwiftManager.java processPacket(), packet.getRxIrCode() = 0x" + Integer.toHexString(packet.getRxIrCode() & 0xFF) + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
                getSwiftManufacturerHandler(packet.getData());
                break;
            case GetSwiftSoftwareVersion:
                Log.w(TAG, "SwiftManager.java processPacket(), packet.getRxIrCode() = 0x" + Integer.toHexString(packet.getRxIrCode() & 0xFF) + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
                getSwiftSoftwareVersionHandler(packet.getData());
                break;
            case GetSwiftMuteStatus:
                Log.w(TAG, "SwiftManager.java processPacket(), packet.getRxIrCode() = 0x" + Integer.toHexString(packet.getRxIrCode() & 0xFF) + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
                getSwiftMuteStatusHandler(packet.getData());
                break;
            case SetSwiftVolumeValue:
                Log.w(TAG, "SwiftManager.java processPacket(), packet.getRxIrCode() = 0x" + Integer.toHexString(packet.getRxIrCode() & 0xFF) + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
                setSwiftVolumeHandler(packet.getData());
                break;
            case GetSwiftVolumeValue:
                Log.w(TAG, "SwiftManager.java processPacket(), packet.getRxIrCode() = 0x" + Integer.toHexString(packet.getRxIrCode() & 0xFF) + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
                getSwiftVolumeHandler(packet.getData());
                break;
            case GetSwiftCurrentChannel:
                Log.w(TAG, "SwiftManager.java processPacket(), packet.getRxIrCode() = 0x" + Integer.toHexString(packet.getRxIrCode() & 0xFF) + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
                getSwiftChannelHandler(packet.getData());
                break;
            case SetSwiftCloneOutDataRequire:
                Log.w(TAG, "SwiftManager.java processPacket(), packet.getRxIrCode() = 0x" + Integer.toHexString(packet.getRxIrCode() & 0xFF) + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
                setSwiftCloneOutDataRequireHandler(packet.getData());
                break;
            case GetSwiftCloneDataLength:
                Log.w(TAG, "SwiftManager.java processPacket(), packet.getRxIrCode() = 0x" + Integer.toHexString(packet.getRxIrCode() & 0xFF) + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
                getSwiftCloneDataLengthHandler(packet.getData());
                break;
            case GetSwiftCloneOutData:
                Log.w(TAG, "SwiftManager.java processPacket(), packet.getRxIrCode() = 0x" + Integer.toHexString(packet.getRxIrCode() & 0xFF) + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
                getSwiftCloneOutDataHandler(packet.getData());
                break;
            case SetSwiftNotifyCloneIn:
                Log.w(TAG, "SwiftManager.java processPacket(), packet.getRxIrCode() = 0x" + Integer.toHexString(packet.getRxIrCode() & 0xFF) + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
                setSwiftNotifyCloneInHandler(packet.getData());
                break;
            case SetSwiftNotifyCloneDataLength:
                Log.w(TAG, "SwiftManager.java processPacket(), packet.getRxIrCode() = 0x" + Integer.toHexString(packet.getRxIrCode() & 0xFF) + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
                setSwiftNotifyCloneDataLengthHandler(packet.getData());
                break;
            case SetSwiftSendCloneInData:
                Log.w(TAG, "SwiftManager.java processPacket(), packet.getRxIrCode() = 0x" + Integer.toHexString(packet.getRxIrCode() & 0xFF) + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
                if ((packet.getRxIrCode() & 0xFF) == 0xd1)
                    setSwiftSendCloneInDataHandler(packet.getData());
                else if ((packet.getRxIrCode() & 0xFF) == 0xde)
                    getSwiftImportResultHandler(packet.getData());
                else if ((packet.getRxIrCode() & 0xFF) == 0xdf)
                    getSwiftUpgradeResultHandler(packet.getData());
                break;
            case GetSwiftImportResult:
                Log.w(TAG, "SwiftManager.java processPacket(), packet.getRxIrCode() = 0x" + Integer.toHexString(packet.getRxIrCode() & 0xFF) + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
                getSwiftImportResultHandler(packet.getData());
                break;
            case SetSwiftNotifySoftwareUpgrade:
                Log.w(TAG, "SwiftManager.java processPacket(), packet.getRxIrCode() = 0x" + Integer.toHexString(packet.getRxIrCode() & 0xFF) + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
                setSwiftNotifyUpgradeFirmwareHandler(packet.getData());
                break;
            case SetSwiftNone:            //0xFFFF
            default:
                unknownCommandHandler(packet.getData());
                break;
        }
    }

    public void receiveDataHandler(byte[] data, int length) {
        // append and parse received data
        SwiftPacket.ParseResult parseResult;
/*
        Log.w(TAG, "SwiftManager.java receiveDataHandler(), data.length = " + data.length + " , length = " + length);
        for(int i=0; i<length; i++)
        {
            Log.w(TAG, "SwiftManager.java receiveDataHandler(), index = " + i + " , data = 0x" + Integer.toHexString(data[i] & 0xFF));
        }
*/
        for (int i = 0; i < length; i++) {
            parseResult = _receivedPacket.parse(data[i], _txMessage);
//            Log.w(TAG, "SwiftManager.java receiveDataHandler(), parseResult = " + parseResult);

            if (parseResult == SwiftPacket.ParseResult.Success) {
                _rxPacketQueue.add(_receivedPacket.clone());
                synchronized (_notifyRxPacketSync) {
                    _notifyRxPacketSync.notify();
                }
            } else {
//              Log.w(TAG, "SwiftManager.java receiveDataHandler(), parseResult = " + parseResult);
            }
            if ((parseResult != SwiftPacket.ParseResult.Incomplete)) {
                resetRx();
            }
        }
    }

    private class SwiftRxThread extends Thread {
        private int bytes = 0;
        private byte[] buffer = new byte[200]; // 64: ok, 128: ok
        private byte[] buffer600E = new byte[256];
        private int bufer600EIndex = 0;
        private int totalParseLength = 0;

        @Override
        public void run() {
            super.run();

            Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@ SwiftManager.java SwiftRxThread() ++ &&&&&&&&&&&&&&&&&&&&&&&&");

            while (!isInterrupted()) {
                if (_rxPacketQueue.isEmpty()) {

//                    Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@ _rxPacketQueue.isEmpty()");

                    try {
                        bytes = _inStream.read(buffer);

                        totalParseLength = _txMessage.getrtnDataSize() + 4;

                        for (int i = 0; i < bytes; i++) {
                            Log.w(TAG, "####SwiftManager.java SwiftRxThread(), index = " + i + " , data = 0x" + Integer.toHexString(buffer[i] & 0xFF) + "");
                        }

                        Log.w(TAG, "####SwiftManager.java SwiftRxThread(), bytes = " + bytes + " , getReceiverEnable() = " + getReceiverEnable() + ", _txMessage = " + _txMessage.name() + ", returnSize = " + _txMessage.getrtnDataSize() + ", totalParseLength = " + totalParseLength);

                        // for 600E rx packet issue
                        byte data = 0;
                        if (ProductConfig.isLightCommercial()) {
                            for (int i = 0; i < bytes; i++) {
//                            if (bytes == 1) {
                                data = buffer[i];

                                // swift tuner packet start bit
                                if ((/*buffer[0]*/ data & 0xff) == 0xf5) {
                                    // reset
//                                    Log.w("cctest", "####SwiftManager.java SwiftRxThread(), buffer600E start bit found!");
                                    buffer600E = new byte[256];
                                    bufer600EIndex = 0;
                                }
                                buffer600E[bufer600EIndex] = data; //buffer[0];
//                                Log.w("cctest", "####SwiftManager.java SwiftRxThread(), buffer600E index = " + bufer600EIndex + " , data = 0x" + Integer.toHexString(buffer600E[bufer600EIndex] & 0xFF) + "");
                                bufer600EIndex++;

                                if (bufer600EIndex >= 5) {
//                                    Log.w("cctest", "####SwiftManager.java SwiftRxThread(), buffer600E index >= 5!");
//                                    Log.w("cctest", "####SwiftManager.java SwiftRxThread(), buffer600E data = " +
//                                            " 0x" + Integer.toHexString(buffer600E[0] & 0xFF) +
//                                            " 0x" + Integer.toHexString(buffer600E[1] & 0xFF) +
//                                            " 0x" + Integer.toHexString(buffer600E[2] & 0xFF) +
//                                            " 0x" + Integer.toHexString(buffer600E[3] & 0xFF) +
//                                            " 0x" + Integer.toHexString(buffer600E[4] & 0xFF));
//                                for (int i = 0; i < 5; i++) {
//                                    Log.w("cctest", "####SwiftManager.java SwiftRxThread(), buffer600E index = " + i + " , data = 0x" + Integer.toHexString(buffer600E[i] & 0xFF) + "");
//                                }

                                    // reset
//                                    buffer600E = new byte[5];
//                                    bufer600EIndex = 0;

                                }
//                            }
                            }
                        }
                        if (bytes < 0)
                            Log.e(TAG, "SwiftRxThread Read error!");
                        else {
                            if (getReceiverEnable()) {
//                                receiveDataHandler(buffer, bytes);
                                receiveDataHandler(buffer, bytes);
                            } else { // specially deal with de and df command (DE DF commands return import or update firmware result)
                                Log.w(TAG, "####SwiftManager.java SwiftRxThread(), specially deal with de and df command, _rxPacketQueue.size() = " + _rxPacketQueue.size());

                                if (checkDeDfCommand(buffer, bytes)) {
                                    receiveDataHandler(buffer, bytes);

                                    // waiting for parse result
                                    SystemClock.sleep(2000);

                                    Log.w(TAG, "####SwiftManager.java SwiftRxThread(), _rxPacketQueue.size() = " + _rxPacketQueue.size());

                                    if (_rxPacketQueue.size() > 0) {
                                        _rxPacket = _rxPacketQueue.remove();
                                        _rxMessage = _rxPacket.getMessage();

                                        Log.w(TAG, "####SwiftManager.java SwiftRxThread(), _rxMessage = " + _rxMessage);


                                        //it was the responding message from Swift that matched the request we just issued.
                                        processPacket(_rxPacket);
                                    }
                                }
                                // for 600E UART rx packet issue
                                if (ProductConfig.isLightCommercial()) {
                                    if (bufer600EIndex >= 5) {
                                        if (checkDeDfCommand(buffer600E, buffer600E.length)) {
                                            receiveDataHandler(buffer600E, buffer600E.length);

                                            // waiting for parse result
                                            SystemClock.sleep(2000);

                                            Log.w(TAG, "####SwiftManager.java SwiftRxThread(), _rxPacketQueue.size() = " + _rxPacketQueue.size());

                                            if (_rxPacketQueue.size() > 0) {
                                                _rxPacket = _rxPacketQueue.remove();
                                                _rxMessage = _rxPacket.getMessage();

                                                Log.w(TAG, "####SwiftManager.java SwiftRxThread(), _rxMessage = " + _rxMessage);


                                                //it was the responding message from Swift that matched the request we just issued.
                                                processPacket(_rxPacket);
                                            }
                                        }
                                    }

                                    if (bufer600EIndex >= totalParseLength) {
                                        // reset
                                        buffer600E = new byte[256];
                                        bufer600EIndex = 0;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "SwiftRxThread UART read I/O exception", e);
                        break;
                    }
                }
            }

            Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@ SwiftManager.java SwiftRxThread() -- &&&&&&&&&&&&&&&&&&&&&&&&");
        }
    }

    private void sendPacketThenWaitResponse(SwiftPacket packet) {
        int retries = 0;
        byte txPacket[] = packet.buildTxPacket();
        _txMessage = packet.getMessage();
        Log.d(TAG, "Swift TV txCmd = " + _txMessage);
/*
        for(int i = 0 ; i < txPacket.length ; i++)
        {
            Log.d(TAG, "SwiftManager.java sendPacketThenWaitResponse(), index = " + i + ", txPacket = " + Integer.toHexString(txPacket[i] & 0xFF));
        }

        Log.d(TAG, "SwiftManager.java sendPacketThenWaitResponse(), setReceiverEnable(true)!!!!!!!!!!!!");
*/
        setReceiverEnable(true);
        while (retries < TRANSMIT_RETRIES) {
            resetRx();
            _receivedPacket.setMessage(_txMessage);
            // write the packet out through UART
            try {
                _outStream.write(txPacket);
            } catch (Exception e) {
                Log.e(TAG, "UART write I/O exception:", e);
                return;
            }

            try {
                // wait for reply from Swift module
                synchronized (_notifyRxPacketSync) {
                    if (_txMessage == SwiftCommands.SetSwiftCloneOutDataRequire)
                        _notifyRxPacketSync.wait(6000);
                    else if (_txMessage == SwiftCommands.GetSwiftCloneOutData)
                        _notifyRxPacketSync.wait(10000);
                    else if (_txMessage == SwiftCommands.SetSwiftNotifyCloneIn)
                        _notifyRxPacketSync.wait(6000);
                    else if (_txMessage == SwiftCommands.SetSwiftSendCloneInData)
                        _notifyRxPacketSync.wait(10000);
                    else if (_txMessage == SwiftCommands.GetSwiftTunerTypeVersion)
                        _notifyRxPacketSync.wait(1000);
                    else if (_txMessage == SwiftCommands.GetSwiftManufacturer)
                        _notifyRxPacketSync.wait(1000);
                    else if (_txMessage == SwiftCommands.SetSwiftNotifySoftwareUpgrade)
                        _notifyRxPacketSync.wait(1000);
//                    else if (_txMessage == SwiftCommands.SetSwiftMuteOff)
//                        _notifyRxPacketSync.wait(1000);
/*
                        else if(_txMessage == SwiftCommands.GetSwiftImportResult)
                            _notifyRxPacketSync.wait(120000); // wait 2 minuts for tuner clone in channel list
*/
/*
                        else if(_txMessage == SwiftCommands.SetSwiftNotifySoftwareUpgrade)
                            _notifyRxPacketSync.wait(6000);
*/
                    else
                        _notifyRxPacketSync.wait(RECEIVE_WAIT_REPLY_MS);
                }
            } catch (Exception e) {
                Log.e(TAG, "Waiting for Swift command{" + _txMessage + "} response was interrupted", e);
            }

            if (_rxPacketQueue.isEmpty()) {
                Log.e(TAG, "Swift had no response to [" + _txMessage + "], retrying..." + retries);
                //timeout or no rxPacket retry delay 50 ms
                SystemClock.sleep(TX_RETRY_DELAY_TIME_MS);
            } else {  //there were something received
                _totalRetryCounter = 0;  //got the response messages, means that the UART works OK, so clear it.
                // dequeue a Rx packet from the queue head and process it
                _rxPacket = _rxPacketQueue.remove();
                _rxMessage = _rxPacket.getMessage();

                //check if _txCommand matches _rxCommand
                if (_txMessage.getValueID() == _rxMessage.getValueID()) {
                    //it was the responding message from Swift that matched the request we just issued.
                    processPacket(_rxPacket);
                    break;
                } else {
                    Log.d(TAG, "Rx[" + _rxMessage + "], Tx[" + _txMessage + "] command did not match, retry..." + retries);
                    if (_rxMessage.getValueID() == SwiftCommands.SetSwiftNone.getValueID()) {
                        unknownCommandHandler(_rxPacket.getData());
                    }
                }
            }
            retries++;
            _totalRetryCounter++;
        }

        if (retries >= TRANSMIT_RETRIES) {
            Log.e(TAG, "Tx[" + _txMessage + "] communication failed!");
            resetRx();
        }

        if (_totalRetryCounter >= COMMUNICATION_RETRIES) {
            Log.e(TAG, "Too many failures! Communication lost!");
            commLostErrorHandler();
        }

        Log.d(TAG, "SwiftManager.java sendPacketThenWaitResponse(), setReceiverEnable(false)??????????????");

        setReceiverEnable(false); // discard any unwanted received noise packets
    }

    private class SwiftTxThread extends Thread {

        @Override
        public void run() {
            super.run();

            Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@ SwiftManager.java SwiftTxThread() ++ &&&&&&&&&&&&&&&&&&&&&&&&");

            while (!isInterrupted()) {
                if (_txPacketQueue.isEmpty()) {
                    try {
                        synchronized (_notifyTxPacketSync) {
                            _notifyTxPacketSync.wait();  //just stuck here until the Tx queue is not empty
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    if (!_startParseMsg && !getReceiverEnable()) {  //do not issue another command while the RxThread is processing received packets.
                        // dequeue a Tx packet from the queue head
                        try {
                            sendPacketThenWaitResponse(_txPacketQueue.remove());
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@@@ SwiftManager.java SwiftTxThread() -- &&&&&&&&&&&&&&&&&&&&&&&&");
        }
    }

    private void addTxPackets(SwiftPacket packet) {
        _txPacketQueue.add(packet);
        synchronized (_notifyTxPacketSync) {
            _notifyTxPacketSync.notify();
        }
    }

    // DE DF commands return import or update firmware result
    private boolean checkDeDfCommand(byte[] data, int length) {
        boolean result = false;

        Log.d(TAG, "checkDeDfCommand: length = " + length);


        if (length < 3) return false;

        Log.d(TAG, "checkDeDfCommand: data[0] = " + data[0] + ", data[2] = " + data[2] + "data[0] = " + (data[0] & 0xff) + ", data[2] = " + (data[2] & 0xff));

        if ((data[0] & 0xff) == 0xf5 && (data[2] & 0xff) == 0xde)
            result = true;
        else if ((data[0] & 0xff) == 0xf5 && (data[2] & 0xff) == 0xdf)
            result = true;

        Log.d(TAG, "checkDeDfCommand: result = " + result);

        return result;
    }

    @Override
    public void setSwiftPowerOn() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftPowerOn);
        addTxPackets(packet);
    }

    private void setSwiftPowerOnHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onPowerOn();
            }
        }
    }

    @Override
    public void setSwiftMenu() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftMenu);
        addTxPackets(packet);
    }

    private void setSwiftMenuHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onMenu();
            }
        }
    }

    @Override
    public void setSwiftNumber1() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftNumber1);
        addTxPackets(packet);
    }

    private void setSwiftNumber1Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onNumber1();
            }
        }
    }

    @Override
    public void setSwiftNumber2() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftNumber2);
        addTxPackets(packet);
    }

    private void setSwiftNumber2Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onNumber2();
            }
        }
    }

    @Override
    public void setSwiftNumber3() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftNumber3);
        addTxPackets(packet);
    }

    private void setSwiftNumber3Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onNumber3();
            }
        }
    }

    @Override
    public void setSwiftNumber4() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftNumber4);
        addTxPackets(packet);
    }

    private void setSwiftNumber4Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onNumber4();
            }
        }
    }

    @Override
    public void setSwiftNumber5() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftNumber5);
        addTxPackets(packet);
    }

    private void setSwiftNumber5Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onNumber5();
            }
        }
    }

    @Override
    public void setSwiftNumber6() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftNumber6);
        addTxPackets(packet);
    }

    private void setSwiftNumber6Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onNumber6();
            }
        }
    }

    @Override
    public void setSwiftNumber7() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftNumber7);
        addTxPackets(packet);
    }

    private void setSwiftNumber7Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onNumber7();
            }
        }
    }

    @Override
    public void setSwiftNumber8() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftNumber8);
        addTxPackets(packet);
    }

    private void setSwiftNumber8Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onNumber8();
            }
        }
    }

    @Override
    public void setSwiftNumber9() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftNumber9);
        addTxPackets(packet);
    }

    private void setSwiftNumber9Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onNumber9();
            }
        }
    }

    @Override
    public void setSwiftNumber0() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftNumber0);
        addTxPackets(packet);
    }

    private void setSwiftNumber0Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onNumber0();
            }
        }
    }

    @Override
    public void setSwiftDash() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftDash);
        addTxPackets(packet);
    }

    private void setSwiftDashHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onDash();
            }
        }
    }

    @Override
    public void setSwiftEnter() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftEnter);
        addTxPackets(packet);
    }

    private void setSwiftEnterHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onEnter();
            }
        }
    }

    @Override
    public void setSwiftChannelUp() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftChannelUp);
        addTxPackets(packet);
    }

    private void setSwiftChannelUpHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onChannelUp();
            }
        }
    }

    @Override
    public void setSwiftChannelDown() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftChannelDown);
        addTxPackets(packet);
    }

    private void setSwiftChannelDownHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onChannelDown();
            }
        }
    }

    @Override
    public void setSwiftVolumeUp() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftAudioInc);
        addTxPackets(packet);
    }

    private void setSwiftVolumeUpHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onVolumeUp();
            }
        }
    }

    @Override
    public void setSwiftVolumeDown() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftAudioDec);
        addTxPackets(packet);
    }

    private void setSwiftVolumeDownHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onVolumeDown();
            }
        }
    }

    @Override
    public void setSwiftPowerOff() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftPowerOff);
        addTxPackets(packet);
    }

    private void setSwiftPowerOffHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onPowerOff();
            }
        }
    }

    @Override
    public void setSwiftMute() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftMute);
        addTxPackets(packet);
    }

    private void setSwiftMuteHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onMute();
            }
        }
    }

    @Override
    public void setSwiftInputSource() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftInputSource);
        addTxPackets(packet);
    }

    private void setSwiftInputSourceHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onInputSource();
            }
        }
    }

    @Override
    public void setSwiftExit() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftExit);
        addTxPackets(packet);
    }

    private void setSwiftExitHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onExit();
            }
        }
    }

    @Override
    public void setSwiftReturn() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftReturn);
        addTxPackets(packet);
    }

    private void setSwiftReturnHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onReturn();
            }
        }
    }

    @Override
    public void setSwiftClosedCaption() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftClosedCaption);
        addTxPackets(packet);
    }

    private void setSwiftClosedCaptionHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onClosedCaption();
            }
        }
    }

    @Override
    public void setSwiftRed() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftRed);
        addTxPackets(packet);
    }

    private void setSwiftRedHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onRed();
            }
        }
    }

    @Override
    public void setSwiftGreen() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftGreen);
        addTxPackets(packet);
    }

    private void setSwiftGreenHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onGreen();
            }
        }
    }

    @Override
    public void setSwiftYellow() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftYellow);
        addTxPackets(packet);
    }

    private void setSwiftYellowHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onYellow();
            }
        }
    }

    @Override
    public void setSwiftBlue() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftBlue);
        addTxPackets(packet);
    }

    private void setSwiftBlueHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onBlue();
            }
        }
    }

    @Override
    public void setSwiftAspectRatio() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftAspectRatio);
        addTxPackets(packet);
    }

    private void setSwiftAspectRatioHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onAspectRatio();
            }
        }
    }

    @Override
    public void setSwiftVChip() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftVChip);
        addTxPackets(packet);
    }

    private void setSwiftVChipHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onVChip();
            }
        }
    }

    @Override
    public void setSwiftEPG() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftEPG);
        addTxPackets(packet);
    }

    private void setSwiftEPGHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onEPG();
            }
        }
    }

    @Override
    public void setSwiftTeletext() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftTeletext);
        addTxPackets(packet);
    }

    private void setSwiftTeletextHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onTeletext();
            }
        }
    }

    @Override
    public void setSwiftCompositeIn() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftCompositeIn);
        addTxPackets(packet);
    }

    private void setSwiftCompositeInHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onCompositeIn();
            }
        }
    }

    @Override
    public void setSwiftSVideoIn() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftSVideoIn);
        addTxPackets(packet);
    }

    private void setSwiftSVideoInHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onSVideoIn();
            }
        }
    }

    @Override
    public void setSwiftComponentIn() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftComponentIn);
        addTxPackets(packet);
    }

    private void setSwiftComponentInHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onComponentIn();
            }
        }
    }

    @Override
    public void setSwiftVGAIn() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftVGAIn);
        addTxPackets(packet);
    }

    private void setSwiftVGAInHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onVGAIn();
            }
        }
    }

    @Override
    public void setSwiftTunerIn() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftTunerIn);
        addTxPackets(packet);
    }

    private void setSwiftTunerInHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onTunerIn();
            }
        }
    }

    @Override
    public void setSwiftHDMIIn() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftHDMIIn);
        addTxPackets(packet);
    }

    private void setSwiftHDMIInHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onHDMIIn();
            }
        }
    }

    @Override
    public void setSwiftFavorite() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftFavorite);
        addTxPackets(packet);
    }

    private void setSwiftFavoriteHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onFavorite();
            }
        }
    }

    @Override
    public void getSwiftTunerType() {
        Log.w(TAG, "getSwiftTunerType: ");
        SwiftPacket packet = new SwiftPacket(SwiftCommands.GetSwiftTunerType);
        addTxPackets(packet);
    }

    private void getSwiftTunerTypeHandler(byte irCode) {
        Log.w(TAG, "SwiftManager.java getSwiftTunerTypeHandler(), irCode = " + irCode + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        Log.w(TAG, "getSwiftTunerTypeHandler: tuner type = " + (irCode & 0xFF));
        DataMatrix.putInt(TVKeys.DATA_TUNER_TYPE_INT, (irCode & 0xFF));

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onGetTunerType(irCode);
        }
    }

    private void getSwiftTunerTypeHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java getSwiftTunerTypeHandler(), payload = " + payload + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onGetTunerType();
            }
        }
    }

    @Override
    public void getSwiftTunerTypeVersion() {
        Log.w(TAG, "getSwiftTunerTypeVersion: ");
        SwiftPacket packet = new SwiftPacket(SwiftCommands.GetSwiftTunerTypeVersion);
        addTxPackets(packet);
    }

    private void getSwiftTunerTypeVersionHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java getSwiftTunerTypeVersionHandler(), payload = " + payload + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        Log.w(TAG, "getSwiftTunerTypeVersionHandler: tuner type version = " + (payload[0] & 0xFF));
        mDataMatrix.putInt(TVKeys.DATA_TUNER_TYPE_INT, (payload[0] & 0xFF));
    }

    @Override
    public void setSwiftComponent1080i() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftComponentOutput1080i);
        addTxPackets(packet);
    }

    private void setSwiftComponent1080iHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onSetComponent1080i();
            }
        }
    }

    @Override
    public void setSwiftComponent480i() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftComponentOutput480i);
        addTxPackets(packet);
    }

    private void setSwiftComponent480iHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onSetComponent480i();
            }
        }
    }

    @Override
    public void setSwiftComponent576i() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftComponentOutput576i);
        addTxPackets(packet);
    }

    private void setSwiftComponent576iHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onSetComponent576i();
            }
        }
    }

    @Override
    public void setSwiftComponent480p() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftComponentOutput480p);
        addTxPackets(packet);
    }

    private void setSwiftComponent480pHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onSetComponent480p();
            }
        }
    }

    @Override
    public void setSwiftComponent576p() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftComponentOutput576p);
        addTxPackets(packet);
    }

    private void setSwiftComponent576pHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onSetComponent576p();
            }
        }
    }

    @Override
    public void setSwiftComponent720p() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftComponentOutput720p);
        addTxPackets(packet);
    }

    private void setSwiftComponent720pHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onSetComponent720p();
            }
        }
    }

    @Override
    public void getSwiftComponentOutput() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.GetSwiftComponentOutputMode);
        addTxPackets(packet);
    }

    private void getSwiftComponentOutputHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java getSwiftComponentOutputHandler(), payload = " + payload + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onGetComponentOutput(payload);
        }
    }

    @Override
    public void setSwiftHDMI720p() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftHDMIOutput720p);
        addTxPackets(packet);
    }

    private void setSwiftHDMI720pHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onSetHDMI720p();
            }
        }
    }

    @Override
    public void setSwiftHDMI1080p() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftHDMIOutput1080p);
        addTxPackets(packet);
    }

    private void setSwiftHDMI1080pHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onSetHDMI1080p();
            }
        }
    }

    @Override
    public void getSwiftHDMIOutput() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.GetSwiftHDMIOutputMode);
        addTxPackets(packet);
    }

    private void getSwiftHDMIOutputHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java getSwiftComponentOutputHandler(), payload = " + payload + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onGetHDMIOutput(payload);
        }
    }

    @Override
    public void getSwiftStatus() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.GetSwiftStatus);
        addTxPackets(packet);
    }

    private void getSwiftStatusHandler(byte irCode) {
        Log.w(TAG, "SwiftManager.java getSwiftStatusHandler(), irCode = " + irCode + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onGetStatus(irCode);
        }
    }

    @Override
    public void getSwiftManufacturer() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.GetSwiftManufacturer);
        addTxPackets(packet);
    }

    private void getSwiftManufacturerHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java getSwiftManufacturerHandler(), payload = " + payload + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        Log.w(TAG, "getSwiftManufacturerHandler: tuner facturer = " + (payload[0] & 0xFF));

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onGetManufacturer(payload);
        }
    }

    public void getSwiftSoftwareVersion() {
        Log.d(TAG, "cctest getSwiftVersion()");
        SwiftPacket packet = new SwiftPacket(SwiftCommands.GetSwiftSoftwareVersion);
        addTxPackets(packet);
    }

    private void getSwiftSoftwareVersionHandler(byte[] payload) {
        Log.d(TAG, "cctest gotSwiftVersion()");
        Log.w(TAG, "SwiftManager.java getSwiftSoftwareVersionHandler(), payload = " + Arrays.toString(payload) + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        int intVersion = payload[0] & 0xFF;
        int high = intVersion / 16;
        int low = intVersion % 16;
        DataMatrix.putString(TVKeys.DATA_TUNER_VERSION_STRING, String.format(Locale.ENGLISH, "%d.%02d", high, low));

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onGetSoftwareVersion(payload);
        }
    }

    @Override
    public void setSwiftMuteOn() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftMuteOn);
        addTxPackets(packet);
    }

    private void setSwiftMuteOnHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onMuteOn();
            }
        }
    }

    @Override
    public void setSwiftMuteOff() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftMuteOff);
        addTxPackets(packet);
    }

    private void setSwiftMuteOffHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onMuteOff();
            }
        }
    }

    public void getSwiftMuteStatus() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.GetSwiftMuteStatus);
        addTxPackets(packet);
    }

    private void getSwiftMuteStatusHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java getSwiftMuteStatusHandler(), payload = " + payload + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onGetMuteStatus(payload);
        }
    }

    public void setSwiftVolume(int intVolume) {
        Log.w(TAG, "################## SwiftManager.java setSwiftVolume(), intVolume = " + intVolume + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());
/*
        if(bytes.length == 0)
            Log.w(TAG, "!!!!!!!! SwiftManager.java setSwiftVolume(), bytes.length = " + bytes.length + ", RETURN!!");
*/

        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftVolumeValue);
//        int intVolume = Integer.parseInt(strVolume);

/*
        byte[] data = new byte[4];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putInt(intVolume);
*/
        //BigInteger bi = BigInteger.valueOf(intVolume);
        //byte[] data = bi.toByteArray();

        //byte[] data2 = ByteBuffer.allocate(4).putInt(intVolume).array();

        byte[] data = new byte[1];
        data[0] = (byte)intVolume;



        //Log.w(TAG, "################## SwiftManager.java setSwiftVolume(), intVolume = " + intVolume + ", data = " + data);


        packet.setData(data);

        addTxPackets(packet);
    }

    private void setSwiftVolumeHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java getSwiftMuteStatusHandler(), payload = " + payload + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onSetVolume(payload);
        }
    }

    public void getSwiftVolume() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.GetSwiftVolumeValue);
        addTxPackets(packet);
    }

    private void getSwiftVolumeHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java getSwiftVolumeHandler(), payload = " + payload + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onGetVolume(payload);
        }
    }

    public void getSwiftChannel() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.GetSwiftCurrentChannel);
        addTxPackets(packet);
    }

    private void getSwiftChannelHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java getSwiftChannelHandler(), payload = " + payload + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        Log.d(TAG, "getSwiftChannelHandler: payload[0] = " + (payload[0] & 0xFF));
        Log.d(TAG, "getSwiftChannelHandler: payload[1] = " + (payload[1] & 0xFF));
        Log.d(TAG, "getSwiftChannelHandler: payload[2] = " + (payload[2] & 0xFF));
        Log.d(TAG, "getSwiftChannelHandler: payload[3] = " + (payload[3] & 0xFF));

        int majorNumber = (payload[0] & 0xFF) * 256 + (payload[1] & 0xFF);
        int minorNumber = (payload[2] & 0xFF) * 256 + (payload[3] & 0xFF);

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onGetChannel(payload);
        }
    }

    public void setSwiftChannel(String strChannel) {
        Log.w(TAG, "################## SwiftManager.java setSwiftChannel(), strChannel = " + strChannel + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        String[] strChannelSplit;
        int major;
        int minor = 0;

        if(strChannel.contains("-")) {
            strChannelSplit = strChannel.split("-");
            major = Integer.parseInt(strChannelSplit[0]);
            minor = Integer.parseInt(strChannelSplit[1]);
        }
        else if(strChannel.contains(".")){
            strChannelSplit = strChannel.split("\\.");
            major = Integer.parseInt(strChannelSplit[0]);
            minor = Integer.parseInt(strChannelSplit[1]);
        }
        else {
            major = Integer.parseInt(strChannel);
        }


        byte[] data = {0, 0, 0, 0};
        data[0] = (byte) (major / 256);
        data[1] = (byte) (major % 256);
        data[2] = (byte) (minor / 256);
        data[3] = (byte) (minor % 256);

        for (int i = data.length - 1; i >= 0; i--)
            Log.w(TAG, "################## SwiftManager.java setSwiftChannel(), index = " + i + ", data[i] = " + data[i]);

        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftCurrentChannel);
        packet.setData(data);
        addTxPackets(packet);
    }

    private void setSwiftChannelHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java setSwiftChannelHandler(), payload = " + payload + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onSetChannel();
        }
    }

    public void setSwiftDefaultChannel(String strChannel) {
        Log.w(TAG, "################## SwiftManager.java setSwiftDefaultChannel(), strChannel = " + strChannel + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        String[] strChannelSplit = strChannel.split("\\.");
        String strMajor = "";
        String strMinor = "";
        int intMajor = 0;
        int intMinor = 0;

        strMajor = strChannelSplit[0];
        strMinor = strChannelSplit[1];

        intMajor = Integer.parseInt(strMajor);
        intMinor = Integer.parseInt(strMinor);

        byte[] data = {0, 0, 0, 0};
        data[0] = (byte) (intMajor / 256);
        data[1] = (byte) (intMajor % 256);
        data[2] = (byte) (intMinor / 256);
        data[3] = (byte) (intMinor % 256);

        for (int i = data.length - 1; i >= 0; i--)
            Log.w(TAG, "################## SwiftManager.java setSwiftDefaultChannel(), index = " + i + ", data[i] = " + data[i]);

        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftDefaultChannel);
        packet.setData(data);
        addTxPackets(packet);
    }

    private void setSwiftDefaultChannelHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java setSwiftDefaultChannelHandler(), payload = " + payload + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onSetDefaultChannel();
        }
    }

    public void setSwiftCloneOutDataRequire() {
        Log.w(TAG, "################## SwiftManager.java setSwiftCloneOutDataRequire()");

        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftCloneOutDataRequire);
        addTxPackets(packet);

        Log.w(TAG, "################## SwiftManager.java setSwiftCloneOutDataRequire() END");

    }

    private void setSwiftCloneOutDataRequireHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java setSwiftCloneOutDataRequireHandler(), payload = " + payload + ", payload[0] = " + payload[0] + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onCloneOutDataRequire(payload);
        }
    }

    public void getSwiftCloneDataLength() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.GetSwiftCloneDataLength);
        addTxPackets(packet);
    }

    private void getSwiftCloneDataLengthHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java getSwiftCloneDataLengthHandler(), payload = " + payload + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onGetCloneDataLength(payload);
        }
    }

    public void getSwiftCloneOutData(int index, int length) {
//        Log.w(TAG, "################## SwiftManager.java getSwiftCloneOutData(), index = " + index + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        byte[] data = {0, 0, 0, 0, 0, 0, 0, 0};
        byte[] arrayIndex = ByteBuffer.allocate(4).putInt(index).array();
        byte[] arrayLength = ByteBuffer.allocate(4).putInt(length).array();

        for (int i = 0; i < 8; ++i) {
            data[i] = i < 4 ? arrayIndex[i] : arrayLength[i - arrayIndex.length];
        }
/*
        for(int i=data.length-1; i>=0; i--)
            Log.w(TAG, "################## SwiftManager.java getSwiftCloneOutData(), index = " + i + ", data[i] = " + data[i]);
*/
        SwiftPacket packet = new SwiftPacket(SwiftCommands.GetSwiftCloneOutData);
        packet.setData(data);
        packet.setReturnDataSize(length + 4);
        addTxPackets(packet);
    }

    private void getSwiftCloneOutDataHandler(byte[] payload) {
//        Log.w(TAG, "SwiftManager.java getSwiftCloneOutDataHandler(), payload = " + payload + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onGetCloneOutData(payload);
        }
    }

    @Override
    public void setSwiftTest1() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftTest1);
        addTxPackets(packet);
    }

    private void setSwiftTest1Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onTest1();
            }
        }
    }

    @Override
    public void setSwiftTest2() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftTest2);
        addTxPackets(packet);
    }

    private void setSwiftTest2Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mSwiftCallbacks.size() > 0) {
                for (ISwiftCommResponse cb : mSwiftCallbacks)
                    cb.onTest2();
            }
        }
    }

    public void setSwiftTest3(String input) {
        Log.w(TAG, "################## SwiftManager.java setSwiftTest3(), input = " + input + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        byte[] data = input.getBytes();

        for (int i = data.length - 1; i >= 0; i--)
            Log.w(TAG, "################## SwiftManager.java setSwiftTest3(), index = " + i + ", data[i] = " + data[i] + ", 0x" + Integer.toHexString(data[i] & 0xFF));

        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftTest3);
        packet.setData(data);
        addTxPackets(packet);
    }

    private void setSwiftTest3Handler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java setSwiftTest3Handler(), payload = " + payload + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onTest3();
        }
    }

    public void setSwiftNotifyCloneIn() {
        Log.w(TAG, "################## SwiftManager.java setSwiftNotifyCloneIn()");

        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftNotifyCloneIn);
        addTxPackets(packet);

        Log.w(TAG, "################## SwiftManager.java setSwiftNotifyCloneIn() END");

    }

    private void setSwiftNotifyCloneInHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java setSwiftNotifyCloneInHandler(), payload = " + payload + ", payload[0] = " + payload[0] + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onNotifyCloneIn(payload);
        }
    }

    public void setSwiftNotifyCloneDataLength(int length) {
        Log.w(TAG, "################## SwiftManager.java setSwiftNotifyCloneDataLength(), length = " + length);

        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftNotifyCloneDataLength);
//        int intVolume = Integer.parseInt(strVolume);

/*
        byte[] data = new byte[4];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putInt(intVolume);
*/

        byte[] data2 = ByteBuffer.allocate(4).putInt(length).array();

        BigInteger bi = BigInteger.valueOf(length);
        byte[] data = bi.toByteArray();
        Log.w(TAG, "################## SwiftManager.java setSwiftNotifyCloneDataLength(), length = " + length + ", data = " + data + ", data.length = " + data.length);

        for (int i = 0; i < data.length; i++)
            Log.w(TAG, "SwiftMAnager.java setSwiftNotifyCloneDataLength(), index = " + i + ", data = " + Integer.toHexString(data[i] & 0xFF));


        for (int i = 0; i < data2.length; i++)
            Log.w(TAG, "SwiftMAnager.java setSwiftNotifyCloneDataLength(), index = " + i + ", data2 = " + Integer.toHexString(data2[i] & 0xFF));


        packet.setData(data2);
        addTxPackets(packet);
    }

    private void setSwiftNotifyCloneDataLengthHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java setSwiftNotifyCloneDataLengthHandler(), payload = " + payload + ", payload[0] = " + payload[0] + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onNotifyCloneDataLength(payload);
        }
    }

    public void setSwiftSendCloneInData(int startIndex, int dataSize, byte[] datas) {
        Log.w(TAG, "################## SwiftManager.java setSwiftSendCloneInData(), startIndex = " + startIndex + ", dataSize = " + dataSize);

//        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftSendCloneInData);
//        int intVolume = Integer.parseInt(strVolume);

/*
        byte[] data = new byte[4];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.putInt(intVolume);
*/

        byte[] data = {0, 0, 0, 0, 0, 0, 0, 0};
        byte[] arrayIndex = ByteBuffer.allocate(4).putInt(startIndex).array();
        byte[] arrayLength = ByteBuffer.allocate(4).putInt(dataSize).array();

        for (int i = 0; i < 8; ++i) {
            data[i] = i < 4 ? arrayIndex[i] : arrayLength[i - arrayIndex.length];
        }

        byte[] allData = new byte[data.length + dataSize];
        System.arraycopy(data, 0, allData, 0, data.length);
        System.arraycopy(datas, 0, allData, data.length, dataSize);

        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftSendCloneInData);
        packet.setData(allData);
        addTxPackets(packet);

        Log.w(TAG, "################## SwiftManager.java setSwiftSendCloneInData(), allData.length() = " + allData.length);

/*
        BigInteger bi = BigInteger.valueOf(length);
        byte[] data = bi.toByteArray();
        Log.w(TAG, "################## SwiftManager.java setSwiftSendCloneInData(), length = " + length + ", data = " + data);
        packet.setData(data);
        addTxPackets(packet);
*/
    }

    private void setSwiftSendCloneInDataHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java setSwiftSendCloneInDataHandler(), payload = " + payload + ", payload[0] = " + payload[0] + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onSendCloneInData(payload);
        }
    }

    public void getSwiftImportResult() {
        Log.w(TAG, "################## SwiftManager.java getSwiftImportResult()");
        SwiftPacket packet = new SwiftPacket(SwiftCommands.GetSwiftImportResult);
        addTxPackets(packet);
    }

    @Override
    public void setSwiftInitializeScanChannel() {
        Log.w(TAG, "################## [KM4] SwiftManager.java setSwiftInitializeScanChannel()");
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftInitializeScanChannel);
        addTxPackets(packet);
        Log.w(TAG, "################## [KM4] SwiftManager.java setSwiftInitializeScanChannel() END");
    }

    private void getSwiftImportResultHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java getSwiftImportResultHandler(), payload = " + payload + ", payload[0] = " + payload[0] + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onGetImportResult(payload);
        }
    }

    public void setSwiftNotifyUpgradeFirmware() {
        Log.w(TAG, "################## SwiftManager.java setSwiftNotifyUpgradeFirmware()");

        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftNotifySoftwareUpgrade);
        addTxPackets(packet);

        Log.w(TAG, "################## SwiftManager.java setSwiftNotifyUpgradeFirmware() END");

    }

    private void setSwiftNotifyUpgradeFirmwareHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java setSwiftNotifyUpgradeFirmwareHandler(), payload = " + payload + ", payload[0] = " + payload[0] + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onNotifyUpgradeFirmware(payload);
        }
    }

    private void getSwiftUpgradeResultHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java getSwiftUpgradeResultHandler(), payload = " + payload + ", payload[0] = " + payload[0] + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onGetUpgradeResult(payload);
        }
    }

    public void getSwiftUSBHandler() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.GetSwiftUSBHandler);
        addTxPackets(packet);
    }

    private void getSwiftUSBHandlerHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java getSwiftUSBHandlerHandler(), payload = " + payload + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onGetUSBHandler(payload);
        }
    }

    public void setSwiftAudioChannelSwitch() {
        SwiftPacket packet = new SwiftPacket(SwiftCommands.SetSwiftAudioChannelSwitch);
        addTxPackets(packet);
    }

    private void setSwiftAudioChannelSwitchHandler(byte[] payload) {
        Log.w(TAG, "SwiftManager.java setSwiftAudioChannelSwitchHandler(), payload = " + payload + ", mSwiftCallbacks.size() = " + mSwiftCallbacks.size());

        if (mSwiftCallbacks.size() > 0) {
            for (ISwiftCommResponse cb : mSwiftCallbacks)
                cb.onSetAudioChannelSwitch(payload);
        }
    }
}
