package com.jht.chimera.lib.tv.settopbox;

import android.util.Log;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.androidcommonalgorithms.util.Strings;
import com.jht.chimera.lib.common.string.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class DirectTVCommHandler extends SerialPortCommHandler {
    private static final String TAG = "DirectTVCommHandler";
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
        Log.d(TAG, "1 Response " + StringUtil.byteArray2Hex(bytes));

        for(int i = 0; i < length; i++) {
            if((message[i] & 0xFF) == 0xF5) {
                length = 0;
                success = false;
                notification.setNotification();
            }
            else if((message[i] & 0xFF) == 0xF4) {
                length = 0;
                success = true;
                notification.setNotification();
            }
        }

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
        ArrayList<byte []> retVal = new ArrayList<>();
        retVal.add(Strings.hexStringToByteArray(button.command, " "));
        return retVal;
    }
}
