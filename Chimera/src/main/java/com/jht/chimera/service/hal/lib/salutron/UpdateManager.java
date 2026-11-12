package com.jht.chimera.service.hal.lib.salutron;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import com.jht.serialport.SerialPort;

import org.jetbrains.annotations.NonNls;
import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class UpdateManager {
    @NonNls private static final String TAG = "UpdateManager";
    private static UpdateManager sInstance;
    private Context mContext;
    private boolean mIsPortOpened;
    private SerialPort mPort;
    private InputStream mInStream;
    private OutputStream mOutStream;
//    private ManifestFile mManifest;
    private BlockingQueue<DFUPacket> mDFUPacketQueue = new LinkedBlockingQueue<>(1);
    private BlockingQueue<DFUReturnPacket> mDFUReturnQueue = new LinkedBlockingQueue<>(1);
    private Thread mRxThread;
    private Thread mTxThread;

    enum PacketType {
        START(3),
        INIT(1),
        DATA(4),
        STOP(5);

        private int value;
        PacketType(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public static UpdateManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new UpdateManager(context);
        }

        return sInstance;
    }

    private UpdateManager(Context context) {
        mContext = context;
    }

    public void startUpdate(SerialPort device, Uri uri) {
        Log.d(TAG, "startUpdate: uri: " + uri.getPath());
//        mContext = context;
        start(device);
        try {
//            broadcastUpdateStatus(100, 0f);
            ManifestFile mManifest = new ManifestFile(new File(uri.getPath()));
            performSlipSequence(mManifest);
        } catch (IOException e) {
            Log.w(TAG, e);
        } catch (JSONException e) {
            Log.w(TAG, e);
        }

    }

    private boolean start(SerialPort device) {
        if (mIsPortOpened) {
            Log.w(TAG, "The serial port had already opened!");
            return true;
        }

        if (device != null) {
            mPort = device;
            try {
                mPort.open();
            } catch (IOException e) {
                Log.w(TAG, e);
                return false;
            }
            mInStream = mPort.getInputStream();
            mOutStream = mPort.getOutputStream();
            if (mInStream == null || mOutStream == null) {
                return false;
            }

            mDFUPacketQueue.clear();
            mDFUReturnQueue.clear();

            mRxThread = new RxThread();
            mTxThread = new TxThread();
            mRxThread.start();
            mTxThread.start();

        } else {
            Log.e(TAG, "serialPort instance is null!");
            return false;
        }
        return mIsPortOpened = true;
    }

    private void performSlipSequence(ManifestFile manifest) {
        byte[] image;
        try {
            image = getImage(manifest);
            DFUPacket startPacket = DFUProtocol.buildStartPacket(ImageType.APPLICATION, (long) image.length);
            Log.d(TAG, "update HRM8700 firmware: send START packet: " + byteArray2Hex(startPacket.toByteArray()));
            sendPacket(startPacket, PacketType.START);
//
            DFUPacket initPacket = DFUProtocol.buildInitPacket(manifest.getInt("application", "init_packet_data", "device_type"), manifest.getInt("application", "init_packet_data", "device_revision"), manifest.getLong("application", "init_packet_data", "application_version"), manifest.getIntArray("application", "init_packet_data", "softdevice_req"), manifest.getLong("application", "init_packet_data", "firmware_crc16")); //NON-NLS
            Log.d(TAG, "update HRM8700 firmware: send INIT packet: " + byteArray2Hex(initPacket.toByteArray()));
            sendPacket(initPacket, PacketType.INIT);
//
            int seq = 3;
            for(int i = 0; i < image.length; ++seq) {
                DFUPacket dataPacket = DFUProtocol.buildDataPacket(image, i, seq);
                if (dataPacket != null) {
//                    Log.d(TAG, "send DATA packet#" + (seq-2) + ": " + byteArray2Hex(dataPacket.toByteArray()));
                    sendPacket(dataPacket, PacketType.DATA);

                }

                i += 512;
                float progress = (i * 100f / image.length);
                broadcastUpdateStatus(101, progress);
                Log.d(TAG, "update HRM8700 firmware: send DATA packet#" + (seq-2) + " progress: " + (i * 100 / image.length) + "%");
            }

            DFUPacket stopPacket = DFUProtocol.buildStopPacket(seq);
            Log.d(TAG, "update HRM8700 firmware: send STOP packet: " + byteArray2Hex(stopPacket.toByteArray()));
            sendPacket(stopPacket, PacketType.STOP);
//            broadcastUpdateStatus(102, 0);

        } catch (IOException e) {
            Log.w(TAG, e);
        }

    }

    private String byteArray2Hex(byte[] data) {
        if(data == null) {
            return "";
        }
        StringBuffer formatedString = new StringBuffer();
        for(byte b: data) {
            formatedString.append(String.format("%#04x", b) + " ");
        }
        return formatedString.toString();
    }

    private byte[] getImage(ManifestFile manifest) throws IOException {
        //noinspection HardCodedStringLiteral
        String binFilename = manifest.getString("application", "bin_file");
        File[] files = manifest.getDirectory().listFiles((file, s) -> s.equals(binFilename));
        if (files.length == 0) {
            throw new FileNotFoundException(binFilename);
        }

        byte[] image = Files.readAllBytes(Paths.get(files[0].toURI()));

        return image;
    }

    private void sendPacket(DFUPacket packet, PacketType type) {
        int seq = packet.getSeqNumber();
        DFUReturnPacket rxPacket = writeAndRead(packet);
        Log.d(TAG, "send ACKn: " + (packet.getAckNumber() & 7) + ", return ACKn: " + rxPacket.getAckNumber());
        if (rxPacket.getAckNumber() != (packet.getAckNumber() & 7)) {
            sendPacket(packet, type);
        } else {
            if (type == PacketType.START) {
                Log.i(TAG, "update HRM8700 firmware: send START packet successfully");
                SystemClock.sleep(2000);
            } else if (type == PacketType.INIT) {
                Log.i(TAG, "update HRM8700 firmware: send INIT packet successfully");
            } else if (type == PacketType.STOP) {
                Log.i(TAG, "update HRM8700 firmware: send STOP packet successfully. Update completed.");
                close();
                broadcastUpdateStatus(102, 0);
            }
        }
    }

    private void close() {
        if (mIsPortOpened) {
            mRxThread.interrupt();
            mTxThread.interrupt();

            if (mPort != null) {
                mPort.close();
                mPort = null;
                mInStream = null;
                mOutStream = null;
            }

            mRxThread = null;
            mTxThread = null;
            mIsPortOpened = false;
        }
    }

    private DFUReturnPacket writeAndRead(DFUPacket packet) {

        write(packet);
        DFUReturnPacket returnPacket = null;
        try {
            returnPacket = mDFUReturnQueue.take();
        } catch (InterruptedException e) {
            Log.w(TAG, e);
        }

        return returnPacket;
    }

    private void write(DFUPacket packet) {
        try {
            mDFUPacketQueue.put(packet);
        } catch (InterruptedException e) {
            Log.w(TAG, e);
        }
    }

    private class RxThread extends Thread {

        @Override
        public void run() {
            super.run();

            while (!isInterrupted()) {
                try {
                    boolean start = false;
                    boolean end = false;
                    ArrayList<Byte> buff = new ArrayList<>();
                    byte byteData = (byte) mInStream.read();
                    if (byteData == (byte) 0xC0) {
                        start = true;
                        buff.add((byte) 0xC0);
                        do {
                            byteData = (byte) mInStream.read();
                            buff.add(byteData);
                            if (byteData == (byte) 0xC0) {
                                end = true;
                            }
                        } while (!end);
                        byte[] data = new byte[buff.size()];
                        for (int i = 0; i < buff.size(); i++) {
                            data[i] = buff.get(i);
                        }

                        DFUReturnPacket returnPacket = new DFUReturnPacket(data);
                        mDFUReturnQueue.put(returnPacket);
                        buff.clear();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "UART read I/O exception: ", e);
                    break;
                }
            }
        }
    }

    private class TxThread extends Thread {

        @Override
        public void run() {
            while (!this.isInterrupted()) {
                try {
                    DFUPacket packet = mDFUPacketQueue.take();
                    byte[] buffer = packet.toByteArray();
                    byte[] crc = DFUPacket.crc16(buffer);
                    buffer = Arrays.copyOf(buffer, buffer.length + 2);
                    buffer[buffer.length - 2] = crc[0];
                    buffer[buffer.length - 1] = crc[1];
                    byte[] data = DFUProtocol.encode(buffer);
                    mOutStream.write(data);
                } catch (InterruptedException e) {
                    Log.w(TAG, e);
                } catch (IOException e) {
                    Log.w(TAG, e);
                }
            }

        }
    }

    public void broadcastUpdateStatus(int statusCommand, float percent) {
        Intent intent = new Intent("com.jht.update8700.result");
        intent.putExtra("status", statusCommand);
        intent.putExtra("percent", percent);
        mContext.sendBroadcast(intent);
    }
}
