package com.jht.chimera.lib.tv.settopbox;

import android.util.Log;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.chimera.lib.common.string.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class LG6500RS232CommHandler extends SerialPortCommHandler {
    private static final String TAG = "LG6500RS232CommHandler";
    private final byte[] message = new byte[4096];
    private int length = 0;
    private boolean success = true;
    private final JHTNotification notification = new JHTNotification(false);

    @Override
    public synchronized void receivedData(byte[] buffer, int bytesRead) {
        if (bytesRead >= 0) System.arraycopy(buffer, 0, message, length, bytesRead);
        length += bytesRead;

        byte[] bytes = new byte[length];
        System.arraycopy(message, 0, bytes, 0, length);
        Log.i(TAG, "1 Response " + StringUtil.byteArray2Hex(bytes) + ", length = " + length);

        if(((message[5] & 0xFF) == 0x4F) && ((message[6] & 0xFF) == 0x4B)) { // OK
            Log.i(TAG, "1 Response OK");
            success = true;
        }
        else if(((message[5] & 0xFF) == 0x4E) && ((message[6] & 0xFF) == 0x47)) { // NG
            Log.i(TAG, "1 Response NG");
            success = false;
        }
        length = 0;
        notification.setNotification();
    }

    @Override
    public int waitForResponse() {
        boolean result = notification.waitForNotification(500);
        if(result && success) {
            return -300;
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
        Log.i(TAG, "LG6500CommHandler.java getMessage: button = " + button.buttonName + ", button.command = " + button.command);
        ArrayList<byte []> retVal = new ArrayList<>();
        String[] commands = button.command.split("\\r");
        if(commands != null) {
            for(String command : commands) {
                retVal.add((command + "\r").getBytes());
            }
        }
        return retVal;
    }
}
