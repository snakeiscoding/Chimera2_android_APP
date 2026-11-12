package com.jht.chimera.lib.tv.settopbox;

import android.util.Log;

import androidx.annotation.NonNull;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.lib.common.string.StringUtil;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class STBComPortManager {
    private static final String TAG = "STBComPortManager";
    private final JHTNotification writeTransactionComplete = new JHTNotification(false);
    private int transactionCounter = 0;

    void startTransaction() {
        writeTransactionComplete.reset();
        synchronized (writeTransactionComplete) {
            transactionCounter = 0;
        }
    }

    boolean waitForTransactionComplete(int timeout) {
        return writeTransactionComplete.waitForNotification(timeout);
    }

    void forceTransactionComplete() {
        writeTransactionComplete.setNotification();
        if(writeThread != null) {
            writeThread.clearQueue();
        }
    }

    private class ReadThread implements Runnable {

        private volatile boolean running = true;

        ReadThread() {
            new Thread(this).start();
        }
        @Override
        public void run() {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while(running) {
                bytesRead = commHandler.readData(buffer);
                if(bytesRead > 0) {
                    byte[] bytes = new byte[bytesRead];
                    System.arraycopy(buffer, 0, bytes, 0, bytesRead);
                    Log.d(TAG, "1.0 Response " + StringUtil.byteArray2Hex(bytes));
                    commHandler.receivedData(buffer, bytesRead);
                }
            }
        }

        void stop() {
            running = false;
            commHandler.close();
        }
    }

    private class WriteThread implements Runnable {

        private class WritePacket {
            private final byte[] message;
            private final int size;
            WritePacket(byte[] message, int size) {
                this.message = message;
                this.size = size;
            }

            byte[] message() { return message; }
            int size() { return size; }
        }
        private volatile boolean running = true;
        private final BlockingQueue<WritePacket> writePacket = new LinkedBlockingQueue<>();

        WriteThread() {
            new Thread(this).start();
        }
        @Override
        public void run() {
            Log.d(TAG, "write thread start");
            while(running) {
                try {
                    WritePacket packet = writePacket.take();
                    if(packet == null || packet.message == null) {
                        return;
                    }

                    Log.d(TAG, "1 write thread sending message");
                    int NUM_TRIES = settings.useIR ? 1 : 2;
                    for(int i = 0; i < NUM_TRIES; i++) {
                        if (packet.size() > 0) {
                            commHandler.clearReceiveHandler();
                            commHandler.sendData(packet.message(), packet.size());
                        }
                        int response = commHandler.waitForResponse();
                        if(response == -200) {
                            break;
                        }
                        else if (response > 0) {
                            //noinspection BusyWait
                            Thread.sleep(response);
                        }
                        else {
                            //noinspection BusyWait
                            Thread.sleep(200);
                            break;
                        }
                    }
                    synchronized (writeTransactionComplete) {
                        transactionCounter--;
                        if (transactionCounter == 0) {
                            writeTransactionComplete.setNotification();
                        }
                    }

                } catch (InterruptedException ignored) {
                }
            }

            Log.d(TAG, "write thread end");
        }

        void writePacket(byte[] packet) {
            try {
                if(packet != null) {
                    writePacket.put(new WritePacket(packet, packet.length));
                }
            } catch (InterruptedException ignored) {
            }
        }

        void stop() {
            running = false;
            try {
                writePacket.put(new WritePacket(null, 0));
            } catch (InterruptedException ignored) {
            }
        }

        void clearQueue() {
            writePacket.clear();
        }
    }

    private final SetTopBoxSetting settings;
    private final ICommHandler commHandler;
    private ReadThread readThread = null;
    private WriteThread writeThread = null;

    STBComPortManager(@NonNull SetTopBoxSetting settings) {
        this.settings = settings;
        Log.d(TAG, "STB Manager " + settings.setTopBoxName);
        switch(settings.setTopBoxName) {
            case "DIRECTV H25 RS232": //NON-NLS
                commHandler = new DirectTVCommHandler();
                break;
            case "ENSEO 3300 RS232": //NON-NLS
                Log.d(TAG, "Get enseo comm handler.");
                commHandler = new EnseoCommHandler();
                break;
            case "LG 6500 RS232": //NON-NLS
                Log.d(TAG, "Get lg 6500 rs232 comm handler.");
                commHandler = new LG6500RS232CommHandler();
                break;
            default:
                commHandler = new IRCommHandler();
        }

    }

    public void open() {
        close();
        commHandler.open(settings);
        if(!settings.useIR) {
            readThread = new ReadThread();
        }
//        writeThread = new WriteThread();
        if(settings.useIR) {
            new JHTTimer(500, () -> writeThread = new WriteThread()).start();
        }
        else {
            writeThread = new WriteThread();
        }
    }

    void close() {
        if(readThread != null) {
            readThread.stop();
            readThread = null;
        }
        if(writeThread != null) {
            writeThread.stop();
            writeThread = null;
        }
    }

    void sendCommand(SetTopBoxSetting.RemoteButton button, SetTopBoxSetting settings) {
        if(writeThread == null) { return; }

        if(settings.useIR) {
            synchronized (writeTransactionComplete) {
                transactionCounter++;
            }
            writeThread.writePacket(button.getIRCommand(settings));
        }
        else {
            List<byte[]> packets = commHandler.getMessage(button);
            for(byte[] packet : packets) {
                writeThread.writePacket(packet);
            }
        }
    }



}
