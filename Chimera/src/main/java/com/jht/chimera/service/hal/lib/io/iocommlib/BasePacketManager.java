package com.jht.chimera.service.hal.lib.io.iocommlib;

import static com.jht.chimera.service.hal.lib.io.iocommlib.McuProtocol.McuStatus;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.chimera.lib.common.process.KillProcessHelper;
import com.jht.serialport.ISerialPort;

import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;

/*
    BasePacketManager contains plumbing common to handling IO MCU packets.
    It is extended by managers that use the McuPacket structure.
    Presently extended by classes for use with main IO and HDMI IR MCUs.
 */
public class BasePacketManager {
    @NonNls protected String logTag = "BasePacketManager";

    protected volatile boolean isPortOpened = false;
    protected int txRetryCounter = 0;

    protected InputStream inStream = null;
    protected OutputStream outStream = null;

    protected ISerialPort port = null;
    protected Thread rxThread = null;
    protected Thread txThread = null;
    protected volatile boolean isRunning = false;

    protected McuPacketParser.ParseResult result = McuPacketParser.ParseResult.Success;
    protected McuPacketParser parser = new McuPacketParser();
    protected McuPacket outPacket = new McuPacket();

    protected Context context;

    protected final JHTNotification notifyRxPacketSync = new JHTNotification();

    protected LinkedBlockingQueue<McuPacket> txCommandQueue = new LinkedBlockingQueue<>();
    protected LinkedBlockingQueue<McuPacket> txEventQueue = new LinkedBlockingQueue<>();
    protected LinkedBlockingQueue<byte[]> txPacketQueue = new LinkedBlockingQueue<>();

    private McuUpdateManager updateManager = null; // why automatically create instance if we rarely use it?
    public boolean isUpdating() {
        return (updateManager != null);
    }
    protected void startUpdate(BasePacketManager parent, Context context, Uri uri) {
        if (updateManager == null) {
            updateManager = new McuUpdateManager();
        } else {
            Log.w(logTag, "Update already in progress, possible retry");
        }
        updateManager.startUpdate(parent, context, uri);
    }
    protected void updateProcess(int commandCode, byte[] payload, McuPacket.CommandType commandType) {
        if (updateManager != null) updateManager.updateProcess(commandCode, payload, commandType);
    }
    protected void finishedUpdate(boolean success) {
        // called when update is finished
        updateManager = null;
    }

    protected BasePacketManager() {
        // ?
    }

    public void sendPacket(McuPacket packet) {
        txCommandQueue.add(packet);
    }

    public boolean start(ISerialPort device, Context context) {
        this.context = context;
        if (isPortOpened) {
            Log.w(logTag, "The serial port had already opened!");
            return true;
        }

        if (device != null) {
            port = device;
            try {
                port.open();
                isPortOpened = true;
            } catch (IOException e) {
                Log.e(logTag, "Failed to open serial port", e);
                return false;
            }
            inStream = port.getInputStream();
            outStream = port.getOutputStream();
            if (inStream == null || outStream == null)
                return false;

            txCommandQueue.clear();
            txPacketQueue.clear();
            txRetryCounter = 0;

            rxThread = new RxThread();
            txThread = new TxThread();
            rxThread.start();
            txThread.start();

        } else {
            Log.e(logTag, "serialPort instance is null!");
            return false;
        }


        return isPortOpened;
    }

    public void stop() {
        if (isPortOpened) {
            KillProcessHelper.stopThread(txThread);
            KillProcessHelper.stopThread(rxThread);

            if (port != null) {
                try {
                    port.close();
                } catch (IOException ignored) {}
                inStream = null;
                outStream = null;
            }

            txPacketQueue.clear();
            rxThread = null;
            txThread = null;
            isPortOpened = false;
        }
    }

    protected class RxThread extends Thread {
        public RxThread() {
            super("IoRx");
        }
        @Override
        public void run() {
            super.run();

            isRunning = true;
            Log.d(logTag, "rxThread start");
            while (!isInterrupted()) {

                try {
                    byte[] buffer = new byte[64];
                    int count = inStream.read(buffer);

                    if (count < 0) {
                        Log.e(logTag, "Read error!");
                    } else if (count > 0) {
                        receiveDataHandler(buffer, count);
                    }

                } catch (IOException e) {
                    isRunning = false;
                    Log.e(logTag, "UART read I/O exception: ", e);
                    break;
                } catch (Exception e) {
                    isRunning = false;
                    Log.e(logTag, "Exception in IO rxThread: ", e);
                    Log.v(logTag, "Restart rxThread");
                    restartRxThread();
                }
            }
        }
    }

    protected void restartRxThread() {
        if (isRunning) {
            isRunning = false;
            rxThread.interrupt();

            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            rxThread = null;
        }

        rxThread = new RxThread();
        rxThread.start();
    }

    protected void receiveDataHandler(byte[] rxPacket, int length) {

        for (int index = 0; index < length; index++) {
            result = parser.feedByte(rxPacket[index]);

            if (McuPacketParser.isInvalid(result)) {
                Log.e(logTag, "error parse state: " + result);
                parser.reset();
                result = McuPacketParser.ParseResult.Incomplete;
            } else if (result == McuPacketParser.ParseResult.Success) {
                if (McuStatus.Success.equals(parser.packet.status)) {
                    try {
                        rxProcess(parser.packet.commandCode, parser.packet.data, parser.packet.getCommandType());
                    } catch (Exception e) {
                        Log.e(logTag, "Packet processing error", e);
                    }
                }
                parser.reset();
            }
        }

        if (McuStatus.UnknownCommand.equals(parser.packet.status)) {
            skipCurrentCommand();
            Log.e(logTag, "MCU does not support command: " + outPacket.getCommandString());
        }
        else if (McuStatus.InvalidParameter.equals(parser.packet.status)) {
            skipCurrentCommand();
            Log.e(logTag, "MCU does not support parameter: " + outPacket.getCommandString() + "> " + outPacket.getDataString());
        }
        else if (!McuStatus.Success.equals(parser.packet.status)) {
            result = McuPacketParser.ParseResult.Failure;
            Log.e(logTag,"packetStatus: " + parser.packet.getStatusString());
        }
//        Log.d(TAG,"receiveHandler parseState: " + result);
    }

    protected void skipCurrentCommand() {
        // since rx process isn't called, we need to reset for next command and wake TxThread
        finishTxCommand();
        result = McuPacketParser.ParseResult.Success;
        notifyRxPacketSync.setNotification();
    }

    protected void finishTxCommand() {
        txPacketQueue.poll();
        txRetryCounter = 0;
    }


    protected void rxProcess(int command, byte[] payload, McuPacket.CommandType commandType) {
        // defer to derived classes on this one? or is there some common thing we want to move here?
    }

    protected void rxError(int command) {
        // defer to derived classes
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    protected String stringFromPayload(byte[] payload) {
        String message = null;
        try {
            message = new String(payload, "ascii");
        } catch (Exception e) {
            Log.e(logTag, "cannot convert packet payload to string", e);
        }
        return message;
    }

    protected class TxThread extends Thread {
        public TxThread() {
            super("IoTx");
        }
        @Override
        public void run() {
            while (!this.isInterrupted()) {
                try {
                    if (!txCommandQueue.isEmpty() || !txEventQueue.isEmpty()) {
                        if(txPacketQueue.isEmpty()) {
//                            Log.d(TAG,"_txCommandQueue size: " + _txCommandQueue.size() + "  Parse result: " + result.toString());
                            if (result != null && (result == McuPacketParser.ParseResult.Success || result == McuPacketParser.ParseResult.NoReply)) {
                                parser.reset(); // receiving thread should be responsible for resetting parser, but the timeout handling is here, we should have another look at this sometime
                                if (!txEventQueue.isEmpty()){
                                    //Event reply need top priority
                                    outPacket = txEventQueue.take();
                                }
                                else {
                                    outPacket = txCommandQueue.take();
                                }
                                txPacketQueue.put(outPacket.getBytes());
                                txRetryCounter = 0;
                                result = null;
                            }
                        }
                    }
//                        Log.d(TAG, "TxThread txPacket Queue size: " + _txPacketQueue.size());
                    if (!txPacketQueue.isEmpty()) {
                        if (outPacket.isCommandNoReply()) {
                            result = McuPacketParser.ParseResult.NoReply;
                            txHandler(txPacketQueue.poll());
                        } else {
                            if (txRetryCounter < 3) {

                                // Clear stale notification before sending so the wait below only
                                // wakes on THIS packet's reply, not a leftover from a prior reply.
                                notifyRxPacketSync.reset();
                                txHandler(txPacketQueue.peek());

                                notifyRxPacketSync.waitForNotification(outPacket.getPacketTimeoutMs());
                                txRetryCounter++;
                                //Log.d(TAG, "txRetryCounter: " + _txRetryCounter);
                            } else {
                                Log.e(logTag, "MCU is no response or command error : " + outPacket.getCommandString() + " rxThread is alive : " + rxThread.isAlive());
                                rxError(outPacket.commandCode);
                                skipCurrentCommand();
                            }
                        }
                    }

                    sleep(50);


                } catch (InterruptedException e) {
                    Log.w(logTag, "TxThread", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    Log.w(logTag, "TxThread", e);
                }
            }
        }
    }

    protected void txHandler(byte[] command) {

        if (command != null) {
            try {

                String data = "";
                for (int i = 0; i < command.length; i++) {
                    data += String.format("%X", command[i]) + " ";
                }
                //Log.d(logTag, "txHandler write command: " + this.outPacket.getCommandString() + "  " + data);

                outStream.write(command);

            } catch (Exception e) {
                Log.w(logTag, "txHandler", e);
            }
        }
    }

}
