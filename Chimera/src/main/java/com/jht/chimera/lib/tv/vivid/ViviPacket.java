package com.jht.chimera.lib.tv.vivid;


import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class ViviPacket {
    private static final String TAG = "ViviPacket";
    private final byte InitialNumber = (byte) 0xF5;
    private final int PACKET_HEADER_SIZE = 1;
    private final int PACKET_CRC_SIZE = 1;
    private final int PACKET_MIN_SIZE = PACKET_HEADER_SIZE + PACKET_CRC_SIZE;
    private final int RX_BUFFER_SIZE = 256;

    private boolean _all_data_has_arrived = false;
    private byte[] _payload = new byte[0];
    private byte[] _rxData = new byte[0];
    private int _rxIndex = 0;
    private int _parsedDataLength = 0;
    private byte _rxStatus;
    private byte _rxIrCode;

    private volatile ViviCommands _message = ViviCommands.SetViviNone;
    private ParseState _parseState = ParseState.PARSE_BYTE_INITIALl_NUMBER;
    private Object _dataLock = new Object();
    private Object _msgLock = new Object();

    public enum ParseResult {
        Success,
        Incomplete,
        InvalidSyncByte,
        InvalidRtnCommand,
        InvalidCrc,
        BufferOverflow
    }

    private enum ParseState {
        PARSE_BYTE_INITIALl_NUMBER,
        PARSE_BYTE_STATUS,
        PARSE_BYTE_IR_CODE,
        PARSE_BYTE_DATA_PAYLOAD,
        PARSE_BYTE_CRC
    }

    public ViviPacket() {
    }

    public ViviPacket(ViviCommands msg) {
        setMessage(msg);
    }

    public synchronized ViviCommands getMessage() {
        synchronized (_msgLock) {
            return _message;
        }
    }

    public synchronized void setMessage(ViviCommands msg) {
        synchronized (_msgLock) {
            _message = msg;
        }
    }

    public synchronized byte getRxStatus() {
        synchronized (_msgLock) {
            return _rxStatus;
        }
    }

    public synchronized byte getRxIrCode() {
        synchronized (_msgLock) {
            return _rxIrCode;
        }
    }

    public synchronized byte[] getData() {
        synchronized (_dataLock) {
            return _payload;
        }
    }

    public synchronized void setData(byte data[]) {
        synchronized (_dataLock) {
            if (null == data) {
                _payload = new byte[0];
                return;
            }
            _payload = data;
        }
    }

    public synchronized byte[] buildTxPacket() {
        char[] charsCommand = _message.getCommand();

        byte msg[] = new byte[PACKET_MIN_SIZE + charsCommand.length + _payload.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(msg);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // sync bytes
        byteBuffer.put(InitialNumber);

        // message command
        for (int i = 0; i < charsCommand.length; i++) {
            Log.d(TAG, "ViviPacket.java buildTxPacket(), charsCommand = " + charsCommand[i]);

            byte byteChar = (byte) (0xFF & (int) charsCommand[i]);
            byteBuffer.put(byteChar);
        }

        // data payload
        for (byte b : _payload) {
            byteBuffer.put(b);
        }

        // checksum - one byte
        byteBuffer.put(ViviCrc.calCrc(msg, msg.length));

        return msg;
    }

    public synchronized ParseResult parse(byte data, final ViviCommands expectedMessage) {
        if (0 == _rxData.length) {
            _rxData = new byte[RX_BUFFER_SIZE];
            _rxIndex = 0;
        }

        Log.w(TAG, "ViviPacket.java parse(), _parseState = " + _parseState + ", _rxIndex = " + _rxIndex + ", data = " + data + ", (byte)0xF5 = " + (byte) 0xF5 + ", value = " + Integer.toHexString(data & 0xFF));


        _rxData[_rxIndex] = data;

        switch (_parseState) {
            case PARSE_BYTE_INITIALl_NUMBER:
                if ((_rxIndex == 0) && (InitialNumber == data))
                    _parseState = ParseState.PARSE_BYTE_STATUS;
                else
                    return ParseResult.InvalidSyncByte;
                break;

            case PARSE_BYTE_STATUS:
                if (_rxIndex == 1) {
                    byte Status = data;
                    _rxStatus = Status;
                    _parseState = ParseState.PARSE_BYTE_IR_CODE;
                }
                break;

            case PARSE_BYTE_IR_CODE:
                if (_rxIndex == 2) {
                    byte irCode = data;
                    _rxIrCode = irCode;
                    int dataSize = _message.getrtnDataSize();

                    Log.w(TAG, "ViviPacket.java parse(), IN PARSE_BYTE_IR_CODE, _rxIrCode = " + Integer.toHexString(data & 0xFF) + " , dataSize = " + dataSize);

                    if (dataSize == 0) {  //it's a message without data payload
                        _parseState = ParseState.PARSE_BYTE_CRC;
                        _all_data_has_arrived = true;
                    } else {
                        setData(new byte[dataSize]);
                        _parseState = ParseState.PARSE_BYTE_DATA_PAYLOAD;
                        _parsedDataLength = 0;
                    }
                }
                break;

            case PARSE_BYTE_DATA_PAYLOAD:
                if (_rxIndex >= 3) {
                    _payload[_parsedDataLength++] = data;
                    if (_message.getrtnDataSize() == _parsedDataLength) {  //all payload data were received
                        _parseState = ParseState.PARSE_BYTE_CRC;
                        _all_data_has_arrived = true;
                    }
                }
                break;

            case PARSE_BYTE_CRC:
                if (_all_data_has_arrived) {
                    byte parseCRC = _rxData[_rxIndex];
                    _parseState = ParseState.PARSE_BYTE_INITIALl_NUMBER;

                    byte calculatedCrc = ViviCrc.calCrc(_rxData, _rxIndex);
                    if (parseCRC != calculatedCrc) {
                        Log.e(TAG, String.format("CRC checksum didn't match! Parsed=%d, Calculated=%d", parseCRC, calculatedCrc));
                        return ParseResult.InvalidCrc;
                    } else {
                        if (expectedMessage.getValueID() == _message.getValueID()) {
                            return ParseResult.Success;
                        } else {
                            return ParseResult.InvalidRtnCommand;
                        }
                    }
                }
                break;
        }

        ++_rxIndex;
        if (((expectedMessage != null) && (_rxIndex > (expectedMessage.getrtnDataSize() + 4)))
                || (_rxIndex >= RX_BUFFER_SIZE)) {
            reset();
            return ParseResult.BufferOverflow;
        }

        return ParseResult.Incomplete;
    }

    public synchronized void reset() {
        Log.w(TAG, "ViviPacket.java reset() is called !!");

        _rxData = new byte[0];
        _payload = new byte[0];
        _rxIndex = 0;
        _rxStatus = 0;
        _rxIrCode = 0;
        _parseState = ParseState.PARSE_BYTE_INITIALl_NUMBER;
        _parsedDataLength = 0;
        _all_data_has_arrived = false;
    }

    public synchronized ViviPacket clone() {
        ViviPacket copy = new ViviPacket();
        byte[] data = getData();

        copy._message = ViviCommands.fromInt(getMessage().getValueID());
        copy._payload = Arrays.copyOf(data, data.length);
        copy._rxIrCode = getRxIrCode();

        return copy;
    }
}
