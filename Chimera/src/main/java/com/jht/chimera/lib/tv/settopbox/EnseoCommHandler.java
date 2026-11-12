package com.jht.chimera.lib.tv.settopbox;

import android.util.Log;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.chimera.lib.common.string.StringUtil;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class EnseoCommHandler extends SerialPortCommHandler {
    private static final String TAG = "EnseoCommHandler";
    private final byte[] message = new byte[4096];
    private int length = 0;
    private volatile boolean success = true;
    private volatile boolean wait = false;
    private final JHTNotification notification = new JHTNotification(false);

    @Override
    public void sendData(byte[] buffer, int size) {
        Log.d(TAG, "Send data");
        notification.reset();
        wait = false;
        success = false;
        super.sendData(buffer, size);
    }

    @Override
    public synchronized void receivedData(byte[] buffer, int bytesRead) {
        if(bytesRead >= 0) {
            System.arraycopy(buffer, 0, message, length, bytesRead);
            length += bytesRead;
        }

        try {
            byte[] bytes = new byte[length];
            System.arraycopy(message, 0, bytes, 0, length);
            @SuppressWarnings("CharsetObjectCanBeUsed") String response = new String(bytes, "UTF-8");
            Log.d(TAG, "1 Response " + StringUtil.byteArray2Hex(bytes));
            if(response.contains("\r\n")) {
                Log.d(TAG, "2 Response " + response);

                String[] lines = response.split("\r\n");
                for(String line : lines) {
                    line = line.replace("\r", "").replace("\n", "");
                    //noinspection HardCodedStringLiteral
                    if(line.equalsIgnoreCase("#OK")) {
                        wait = false;
                        success = true;
                    }
                    else if(line.equalsIgnoreCase("#WAIT")) {
                        wait = true;
                        success = false;
                    }
                }
                length = 0;
                notification.setNotification();
            }
        } catch (UnsupportedEncodingException ignored) {
        }

    }

    @Override
    public int waitForResponse() {
        boolean result = notification.waitForNotification(500);
        if(result) {
            if(wait) {
                return 1000;
            }
            if(success) {
                return -300;
            }
        }
        return 200;
    }

    @Override
    public synchronized void clearReceiveHandler() {
        notification.reset();
        length = 0;
    }

    @Override
    public List<byte[]> getMessage(SetTopBoxSetting.RemoteButton button) {

        ArrayList<byte []> retVal = new ArrayList<>();
        String[] commands = button.command.split("\\r\\n");
        if(commands != null) {
            for(String command : commands) {
                retVal.add((command + "\r\n").getBytes());
            }
        }
        return retVal;
    }

}
