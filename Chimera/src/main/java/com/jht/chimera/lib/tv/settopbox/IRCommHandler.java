package com.jht.chimera.lib.tv.settopbox;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IRCommHandler implements ICommHandler {
    private final JHTNotification notification = new JHTNotification(false);

    IRCommHandler() {
        DataMatrix dataMatrix = new DataMatrix();
        dataMatrix.createBoolAdapter(HALKeys.EVENT_IO_IR_RESULT_BOOL, value -> {
            if(value) {
                notification.setNotification();
            }
        });
    }

    @Override
    public synchronized void receivedData(byte[] buffer, int bytesRead) {
    }

    @Override
    public int waitForResponse() {
        return notification.waitForNotification(500) ? -200 : 200;
    }

    @Override
    public synchronized void clearReceiveHandler() {
        notification.reset();
    }

    public List<byte[]> getMessage(SetTopBoxSetting.RemoteButton button) {
        ArrayList<byte[]> messageList = new ArrayList<>();

        for(int i = 0; i < button.pulseSequences[SetTopBoxSetting.REMOTE_BUTTON_PULSE_SEQUENCE_TYPES.DATA.ordinal()].sequence.length; i++) {
            int number = button.pulseSequences[SetTopBoxSetting.REMOTE_BUTTON_PULSE_SEQUENCE_TYPES.DATA.ordinal()].sequence[i];
            if (i % 2 == 0) {
                number |= 0x8000;
            }

            ByteBuffer buffer = ByteBuffer.allocate(64);
            List<Byte> message = new ArrayList<>();
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            buffer.put((byte) 0x11);
            buffer.putChar((char) 2);
            buffer.putChar((char) number);
            int size = buffer.position();
            buffer.position(0);
            byte checksum = 0;
            message.add((byte) 0xF1);
            for (int j = 0; j < size; j++) {
                byte b = buffer.get();
                checksum ^= b;
                if ((b & 0xFF) == 0xF0) {
                    message.add((byte) 0xF0);
                    message.add((byte) 0x00);
                } else if ((b & 0xFF) == 0xF1) {
                    message.add((byte) 0xF0);
                    message.add((byte) 0x01);
                } else if ((b & 0xFF) == 0xF2) {
                    message.add((byte) 0xF0);
                    message.add((byte) 0x02);
                } else {
                    message.add(b);
                }
            }
            message.add(checksum);
            message.add((byte) 0xF2);
            byte[] retVal = new byte[message.size()];
            for (int j = 0; j < message.size(); j++) {
                retVal[j] = message.get(j);
            }
            messageList.add(retVal);
        }
        return messageList;

    }


    @Override
    public void open(SetTopBoxSetting setTopBoxSetting) {
        // Do nothing.  Communication is routed via IO.
    }

    @Override
    public void close() {
        // Do nothing.  Communication is routed via IO.
    }

    @Override
    public void sendData(byte[] buffer, int size) {
        DataMatrix.putBytes(HALKeys.ACTION_IO_SEND_IR_COMMAND_BYTES, Arrays.copyOf(buffer, size));
    }

    @Override
    public int readData(byte[] buffer) {
        return -1;
    }
}
