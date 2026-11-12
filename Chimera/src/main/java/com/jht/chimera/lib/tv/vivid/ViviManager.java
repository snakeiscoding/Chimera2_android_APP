package com.jht.chimera.lib.tv.vivid;


import android.os.SystemClock;
import android.util.Log;

import com.jht.serialport.SerialPort;

import org.jetbrains.annotations.NonNls;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;


public class ViviManager extends ViviInterface {
    @NonNls private final String TAG = "ViviManager";

    //    private static final int                  TRANSMIT_RETRIES = 3;   //follow the Vivi TV  communication protocol
    private static final int TRANSMIT_RETRIES = 1;   //follow the Vivi TV  communication protocol
    private static final int COMMUNICATION_RETRIES = 25;  //follow the Vivi TV  communication protocol
    private static final int RECEIVE_WAIT_REPLY_MS = 250; //milliseconds, follow the Vivi TV  communication protocol
    private static final int TX_RETRY_DELAY_TIME_MS = 50;  //milliseconds, follow the Vivi TV  communication protocol

    private static final Object _notifyRxPacketSync = new Object();
    private static final Object _notifyTxPacketSync = new Object();

    private volatile boolean _isPortOpened = false;
    private volatile boolean _startParseMsg = false;
    private volatile boolean _isReceiverEnabled = false;
    private volatile int _totalRetryCounter = 0;

    private static InputStream _inStream = null;
    private static OutputStream _outStream = null;

    private static SerialPort _port = null;
    private static ViviCommands _txMessage = null;
    private static ViviCommands _rxMessage = null;
    private static Thread _rxThread = null;
    private static Thread _txThread = null;
    private static ViviPacket _rxPacket = null;
    private static ViviPacket _receivedPacket = new ViviPacket();

    private static ConcurrentLinkedQueue<ViviPacket> _rxPacketQueue = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<ViviPacket> _txPacketQueue = new ConcurrentLinkedQueue<>();


    private static class SingletonHolder {
        private static ViviManager Instance = new ViviManager();
    }

    public static ViviManager getInstance() {
        return SingletonHolder.Instance;
    }

    @Override
    public void registerViviOnReceiveListener(IViviCommResponse callback) {
        for (int i = 0; i < mViviCallbacks.size(); i++) {
            if (mViviCallbacks.get(i) == callback)
                return;  //there is already existed in the list.
        }
        mViviCallbacks.add(callback);
    }

    @Override
    public void unregisterViviOnReceiveListener(IViviCommResponse callback) {
        for (int i = 0; i < mViviCallbacks.size(); i++) {
            if (mViviCallbacks.get(i) == callback)
                mViviCallbacks.remove(i);
        }
    }

    @Override
    public boolean start(SerialPort device/*, String path, int baudRate*/) {

        if (_isPortOpened) {
            Log.w(TAG, "The serial port had already opened!");
            return true;
        }

        if (null != device) {
            _port = device;
            try {
                _port.open(/*new File(path), baudRate, 0*/);
            } catch (Exception e) {
//                Log.e(TAG, "Failed to open serial port: " + path);
                Log.w(TAG, e);
                return false;
            }
            _inStream = _port.getInputStream();
            _outStream = _port.getOutputStream();
            if ((null == _inStream) || (null == _outStream)) {
                return false;
            }
            _rxPacketQueue.clear();
            _txPacketQueue.clear();
            _rxThread = new ViviRxThread();
            _txThread = new ViviTxThread();
            _rxThread.start();
            _txThread.start();
            _isPortOpened = true;
        } else {
            Log.e(TAG, "serialPort instance is null!");
            return false;
        }

        return _isPortOpened;
    }

    @Override
    public void stop() {
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
        if ((mViviCallbacks.size() > 0) && (payload.length > 0)) {
            Log.d(TAG, "Unknown command: " + ViviCommands.fromInt(payload[0]));
            for (IViviCommResponse cb : mViviCallbacks) {
                cb.onSetViviNone(payload[0]);
            }
        }
        resetRx();
    }

    public void commLostErrorHandler() {
        resetRx();
        if (mViviCallbacks.size() > 0) {
            for (IViviCommResponse cb : mViviCallbacks)
                cb.onErrorCode((byte) 0xB1);
        }
    }

    private void processPacket(ViviPacket packet) {

        Log.w(TAG, "ViviManager.java processPacket(), packet.getMessage() = " + packet.getMessage());

        switch (packet.getMessage()) {
            case SetViviPowerOn:  //0x0001
                setViviPowerOnHandler(packet.getData());
                break;
            case SetViviMenu:  //0x0005
                setViviMenuHandler(packet.getData());
                break;
            case GetViviTunerType:
                Log.w(TAG, "ViviManager.java processPacket(), packet.getRxIrCode() = " + packet.getRxIrCode() + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
//                getViviTunerTypeHandler(packet.getData() /*packet.getRxIrCode()*/);
                getViviTunerTypeHandler(packet.getRxIrCode());
                break;
            case GetViviComponentOutputMode:
                Log.w(TAG, "ViviManager.java processPacket(), packet.getRxIrCode() = " + packet.getRxIrCode() + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
//                getViviTunerTypeHandler(packet.getData() /*packet.getRxIrCode()*/);
                getViviComponentOutputHandler(packet.getRxIrCode());
                break;
            case GetViviHDMIOutputMode:
                Log.w(TAG, "ViviManager.java processPacket(), packet.getRxIrCode() = " + packet.getRxIrCode() + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
//                getViviTunerTypeHandler(packet.getData() /*packet.getRxIrCode()*/);
                getViviHDMIOutputHandler(packet.getRxIrCode());
                break;
            case GetViviStatus:
                Log.w(TAG, "ViviManager.java processPacket(), packet.getRxIrCode() = " + packet.getRxIrCode() + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
//                getViviTunerTypeHandler(packet.getData() /*packet.getRxIrCode()*/);
                getViviStatusHandler(packet.getRxIrCode());
                break;
            case GetViviManufacturer:
                Log.w(TAG, "ViviManager.java processPacket(), packet.getRxIrCode() = " + packet.getRxIrCode() + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
//                getViviTunerTypeHandler(packet.getData() /*packet.getRxIrCode()*/);
                getViviManufacturerHandler(packet.getRxIrCode());
                break;
            case GetViviSoftwareVersion:
                Log.w(TAG, "ViviManager.java processPacket(), packet.getRxIrCode() = " + packet.getRxIrCode() + ", packet.getData() = " + packet.getData() + ", length = " + packet.getData().length);
//                getViviTunerTypeHandler(packet.getData() /*packet.getRxIrCode()*/);
                getViviSoftwareVersionHandler(packet.getRxIrCode());
                break;
            case SetViviNone:            //0xFFFF
            default:
                unknownCommandHandler(packet.getData());
                break;
        }
    }

    public void receiveDataHandler(byte[] data, int length) {
        // append and parse received data
        ViviPacket.ParseResult parseResult;

        Log.w(TAG, "ViviManager.java receiveDataHandler(), data.length = " + data.length + " , length = " + length);
        for (int i = 0; i < length; i++) {
            Log.w(TAG, "ViviManager.java receiveDataHandler(), index = " + i + " , data = 0x" + Integer.toHexString(data[i] & 0xFF));
        }

        for (int i = 0; i < length; i++) {
            parseResult = _receivedPacket.parse(data[i], _txMessage);
            if (parseResult == ViviPacket.ParseResult.Success) {
                _rxPacketQueue.add(_receivedPacket.clone());
                synchronized (_notifyRxPacketSync) {
                    _notifyRxPacketSync.notify();
                }
            } else {
                Log.w(TAG, "ViviManager.java receiveDataHandler(), parseResult = " + parseResult);
            }
            if ((parseResult != ViviPacket.ParseResult.Incomplete)) {
                resetRx();
            }
        }
    }

    private class ViviRxThread extends Thread {
        private int bytes = 0;
        private byte[] buffer = new byte[64];

        @Override
        public void run() {
            super.run();

            while (!isInterrupted()) {
                if (_rxPacketQueue.isEmpty()) {
                    try {
                        bytes = _inStream.read(buffer);
                        if (bytes < 0)
                            Log.e(TAG, "Read error!");
                        else {
                            if (getReceiverEnable()) {
                                receiveDataHandler(buffer, bytes);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "UART read I/O exception", e);
                        break;
                    }
                }
            }
        }
    }

    private void sendPacketThenWaitResponse(ViviPacket packet) {
        int retries = 0;
        byte txPacket[] = packet.buildTxPacket();
        _txMessage = packet.getMessage();
        Log.d(TAG, "Vivi TV txCmd = " + _txMessage);

        for (int i = 0; i < txPacket.length; i++) {
            Log.d(TAG, "ViviManager.java sendPacketThenWaitResponse(), txPacket = " + txPacket[i]);
        }

        setReceiverEnable(true);
        while (retries < TRANSMIT_RETRIES) {
            resetRx();
            _receivedPacket.setMessage(_txMessage);
            // write the packet out through UART
            try {
                _outStream.write(txPacket);
            } catch (Exception e) {
                Log.e(TAG, "UART write I/O exception", e);
                return;
            }

            try {
                // wait for reply from Vivi module
                synchronized (_notifyRxPacketSync) {
                    _notifyRxPacketSync.wait(RECEIVE_WAIT_REPLY_MS);
                }
            } catch (Exception e) {
                Log.e(TAG, "Waiting for Vivi command{" + _txMessage + "} response was interrupted: ", e);
            }

            if (_rxPacketQueue.isEmpty()) {
                Log.e(TAG, "Vivi had no response to [" + _txMessage + "], retrying..." + retries);
                //timeout or no rxPacket retry delay 50 ms
                SystemClock.sleep(TX_RETRY_DELAY_TIME_MS);
            } else {  //there were something received
                _totalRetryCounter = 0;  //got the response messages, means that the UART works OK, so clear it.
                // dequeue a Rx packet from the queue head and process it
                _rxPacket = _rxPacketQueue.remove();
                _rxMessage = _rxPacket.getMessage();

                //check if _txCommand matches _rxCommand
                if (_txMessage.getValueID() == _rxMessage.getValueID()) {
                    //it was the responding message from Vivi that matched the request we just issued.
                    processPacket(_rxPacket);
                    break;
                } else {
                    Log.d(TAG, "Rx[" + _rxMessage + "], Tx[" + _txMessage + "] command did not match, retry..." + retries);
                    if (_rxMessage.getValueID() == ViviCommands.SetViviNone.getValueID()) {
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
        setReceiverEnable(false); // discard any unwanted received noise packets
    }

    private class ViviTxThread extends Thread {

        @Override
        public void run() {
            super.run();
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
                        sendPacketThenWaitResponse(_txPacketQueue.remove());
                    }
                }
            }
        }
    }

    private void addTxPackets(ViviPacket packet) {
        _txPacketQueue.add(packet);
        synchronized (_notifyTxPacketSync) {
            _notifyTxPacketSync.notify();
        }
    }

    @Override
    public void setViviPowerOn() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviPowerOn);
        addTxPackets(packet);
    }

    private void setViviPowerOnHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onPowerOn();
            }
        }
    }

    @Override
    public void setViviMenu() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviMenu);
        addTxPackets(packet);
    }

    private void setViviMenuHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onMenu();
            }
        }
    }

    @Override
    public void setViviNumber1() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviNumber1);
        addTxPackets(packet);
    }

    private void setViviNumber1Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onNumber1();
            }
        }
    }

    @Override
    public void setViviNumber2() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviNumber2);
        addTxPackets(packet);
    }

    private void setViviNumber2Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onNumber2();
            }
        }
    }

    @Override
    public void setViviNumber3() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviNumber3);
        addTxPackets(packet);
    }

    private void setViviNumber3Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onNumber3();
            }
        }
    }

    @Override
    public void setViviNumber4() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviNumber4);
        addTxPackets(packet);
    }

    private void setViviNumber4Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onNumber4();
            }
        }
    }

    @Override
    public void setViviNumber5() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviNumber5);
        addTxPackets(packet);
    }

    private void setViviNumber5Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onNumber5();
            }
        }
    }

    @Override
    public void setViviNumber6() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviNumber6);
        addTxPackets(packet);
    }

    private void setViviNumber6Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onNumber6();
            }
        }
    }

    @Override
    public void setViviNumber7() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviNumber7);
        addTxPackets(packet);
    }

    private void setViviNumber7Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onNumber7();
            }
        }
    }

    @Override
    public void setViviNumber8() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviNumber8);
        addTxPackets(packet);
    }

    private void setViviNumber8Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onNumber8();
            }
        }
    }

    @Override
    public void setViviNumber9() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviNumber9);
        addTxPackets(packet);
    }

    private void setViviNumber9Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onNumber9();
            }
        }
    }

    @Override
    public void setViviNumber0() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviNumber0);
        addTxPackets(packet);
    }

    private void setViviNumber0Handler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onNumber0();
            }
        }
    }

    @Override
    public void setViviDash() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviDash);
        addTxPackets(packet);
    }

    private void setViviDashHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onDash();
            }
        }
    }

    @Override
    public void setViviEnter() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviEnter);
        addTxPackets(packet);
    }

    private void setViviEnterHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onEnter();
            }
        }
    }

    @Override
    public void setViviChannelUp() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviChannelUp);
        addTxPackets(packet);
    }

    private void setViviChannelUpHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onChannelUp();
            }
        }
    }

    @Override
    public void setViviChannelDown() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviChannelDown);
        addTxPackets(packet);
    }

    private void setViviChannelDownHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onChannelDown();
            }
        }
    }

    @Override
    public void setViviVolumeUp() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviAudioInc);
        addTxPackets(packet);
    }

    private void setViviVolumeUpHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onVolumeUp();
            }
        }
    }

    @Override
    public void setViviVolumeDown() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviAudioDec);
        addTxPackets(packet);
    }

    private void setViviVolumeDownHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onVolumeDown();
            }
        }
    }

    @Override
    public void setViviPowerOff() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviPowerOff);
        addTxPackets(packet);
    }

    private void setViviPowerOffHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onPowerOff();
            }
        }
    }

    @Override
    public void setViviMute() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviMute);
        addTxPackets(packet);
    }

    private void setViviMuteHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onMute();
            }
        }
    }

    @Override
    public void setViviInputSource() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviInputSource);
        addTxPackets(packet);
    }

    private void setViviInputSourceHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onInputSource();
            }
        }
    }

    @Override
    public void setViviExit() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviExit);
        addTxPackets(packet);
    }

    private void setViviExitHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onExit();
            }
        }
    }

    @Override
    public void setViviReturn() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviReturn);
        addTxPackets(packet);
    }

    private void setViviReturnHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onReturn();
            }
        }
    }

    @Override
    public void setViviClosedCaption() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviClosedCaption);
        addTxPackets(packet);
    }

    private void setViviClosedCaptionHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onClosedCaption();
            }
        }
    }

    @Override
    public void setViviRed() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviRed);
        addTxPackets(packet);
    }

    private void setViviRedHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onRed();
            }
        }
    }

    @Override
    public void setViviGreen() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviGreen);
        addTxPackets(packet);
    }

    private void setViviGreenHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onGreen();
            }
        }
    }

    @Override
    public void setViviYellow() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviYellow);
        addTxPackets(packet);
    }

    private void setViviYellowHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onYellow();
            }
        }
    }

    @Override
    public void setViviBlue() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviBlue);
        addTxPackets(packet);
    }

    private void setViviBlueHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onBlue();
            }
        }
    }

    @Override
    public void setViviAspectRatio() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviAspectRatio);
        addTxPackets(packet);
    }

    private void setViviAspectRatioHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onAspectRatio();
            }
        }
    }

    @Override
    public void setViviVChip() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviVChip);
        addTxPackets(packet);
    }

    private void setViviVChipHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onVChip();
            }
        }
    }

    @Override
    public void setViviEPG() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviEPG);
        addTxPackets(packet);
    }

    private void setViviEPGHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onEPG();
            }
        }
    }

    @Override
    public void setViviTeletext() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviTeletext);
        addTxPackets(packet);
    }

    private void setViviTeletextHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onTeletext();
            }
        }
    }

    @Override
    public void setViviCompositeIn() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviCompositeIn);
        addTxPackets(packet);
    }

    private void setViviCompositeInHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onCompositeIn();
            }
        }
    }

    @Override
    public void setViviSVideoIn() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviSVideoIn);
        addTxPackets(packet);
    }

    private void setViviSVideoInHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onSVideoIn();
            }
        }
    }

    @Override
    public void setViviComponentIn() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviComponentIn);
        addTxPackets(packet);
    }

    private void setViviComponentInHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onComponentIn();
            }
        }
    }

    @Override
    public void setViviVGAIn() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviVGAIn);
        addTxPackets(packet);
    }

    private void setViviVGAInHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onVGAIn();
            }
        }
    }

    @Override
    public void setViviTunerIn() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviTunerIn);
        addTxPackets(packet);
    }

    private void setViviTunerInHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onTunerIn();
            }
        }
    }

    @Override
    public void setViviHDMIIn() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviHDMIIn);
        addTxPackets(packet);
    }

    private void setViviHDMIInHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onHDMIIn();
            }
        }
    }

    @Override
    public void setViviFavorite() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviFavorite);
        addTxPackets(packet);
    }

    private void setViviFavoriteHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onFavorite();
            }
        }
    }

    @Override
    public void getViviTunerType() {
        ViviPacket packet = new ViviPacket(ViviCommands.GetViviTunerType);
        addTxPackets(packet);
    }

    private void getViviTunerTypeHandler(byte irCode) {
        Log.w(TAG, "ViviManager.java getViviTunerTypeHandler(), irCode = " + irCode + ", mViviCallbacks.size() = " + mViviCallbacks.size());

        if (mViviCallbacks.size() > 0) {
            for (IViviCommResponse cb : mViviCallbacks)
                cb.onGetTunerType(irCode);
        }
    }

    private void getViviTunerTypeHandler(byte[] payload) {
        Log.w(TAG, "ViviManager.java getViviTunerTypeHandler(), payload = " + payload + ", mViviCallbacks.size() = " + mViviCallbacks.size());

        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onGetTunerType();
            }
        }
    }

    @Override
    public void setViviComponent1080i() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviComponentOutput1080i);
        addTxPackets(packet);
    }

    private void setViviComponent1080iHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onSetComponent1080i();
            }
        }
    }

    @Override
    public void setViviComponent480i() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviComponentOutput480i);
        addTxPackets(packet);
    }

    private void setViviComponent480iHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onSetComponent480i();
            }
        }
    }

    @Override
    public void setViviComponent576i() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviComponentOutput576i);
        addTxPackets(packet);
    }

    private void setViviComponent576iHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onSetComponent576i();
            }
        }
    }

    @Override
    public void setViviComponent480p() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviComponentOutput480p);
        addTxPackets(packet);
    }

    private void setViviComponent480pHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onSetComponent480p();
            }
        }
    }

    @Override
    public void setViviComponent576p() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviComponentOutput576p);
        addTxPackets(packet);
    }

    private void setViviComponent576pHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onSetComponent576p();
            }
        }
    }

    @Override
    public void setViviComponent720p() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviComponentOutput720p);
        addTxPackets(packet);
    }

    private void setViviComponent720pHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onSetComponent720p();
            }
        }
    }

    @Override
    public void getViviComponentOutput() {
        ViviPacket packet = new ViviPacket(ViviCommands.GetViviComponentOutputMode);
        addTxPackets(packet);
    }

    private void getViviComponentOutputHandler(byte irCode) {
        Log.w(TAG, "ViviManager.java getViviComponentOutputHandler(), irCode = " + irCode + ", mViviCallbacks.size() = " + mViviCallbacks.size());

        if (mViviCallbacks.size() > 0) {
            for (IViviCommResponse cb : mViviCallbacks)
                cb.onGetComponentOutput(irCode);
        }
    }

    @Override
    public void setViviHDMI720p() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviHDMIOutput720p);
        addTxPackets(packet);
    }

    private void setViviHDMI720pHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onSetHDMI720p();
            }
        }
    }

    @Override
    public void setViviHDMI1080p() {
        ViviPacket packet = new ViviPacket(ViviCommands.SetViviHDMIOutput1080p);
        addTxPackets(packet);
    }

    private void setViviHDMI1080pHandler(byte[] payload) {
        if (0 == payload.length) {
            if (mViviCallbacks.size() > 0) {
                for (IViviCommResponse cb : mViviCallbacks)
                    cb.onSetHDMI1080p();
            }
        }
    }

    @Override
    public void getViviHDMIOutput() {
        ViviPacket packet = new ViviPacket(ViviCommands.GetViviHDMIOutputMode);
        addTxPackets(packet);
    }

    private void getViviHDMIOutputHandler(byte irCode) {
        Log.w(TAG, "ViviManager.java getViviComponentOutputHandler(), irCode = " + irCode + ", mViviCallbacks.size() = " + mViviCallbacks.size());

        if (mViviCallbacks.size() > 0) {
            for (IViviCommResponse cb : mViviCallbacks)
                cb.onGetHDMIOutput(irCode);
        }
    }

    @Override
    public void getViviStatus() {
        ViviPacket packet = new ViviPacket(ViviCommands.GetViviStatus);
        addTxPackets(packet);
    }

    private void getViviStatusHandler(byte irCode) {
        Log.w(TAG, "ViviManager.java getViviStatusHandler(), irCode = " + irCode + ", mViviCallbacks.size() = " + mViviCallbacks.size());

        if (mViviCallbacks.size() > 0) {
            for (IViviCommResponse cb : mViviCallbacks)
                cb.onGetStatus(irCode);
        }
    }

    @Override
    public void getViviManufacturer() {
        ViviPacket packet = new ViviPacket(ViviCommands.GetViviManufacturer);
        addTxPackets(packet);
    }

    private void getViviManufacturerHandler(byte irCode) {
        Log.w(TAG, "ViviManager.java getViviManufacturerHandler(), irCode = " + irCode + ", mViviCallbacks.size() = " + mViviCallbacks.size());

        if (mViviCallbacks.size() > 0) {
            for (IViviCommResponse cb : mViviCallbacks)
                cb.onGetManufacturer(irCode);
        }
    }

    public void getViviSoftwareVersion() {
        ViviPacket packet = new ViviPacket(ViviCommands.GetViviSoftwareVersion);
        addTxPackets(packet);
    }

    private void getViviSoftwareVersionHandler(byte irCode) {
        Log.w(TAG, "ViviManager.java getViviSoftwareVersionHandler(), irCode = " + irCode + ", mViviCallbacks.size() = " + mViviCallbacks.size());

        if (mViviCallbacks.size() > 0) {
            for (IViviCommResponse cb : mViviCallbacks)
                cb.onGetSoftwareVersion(irCode);
        }
    }
}
