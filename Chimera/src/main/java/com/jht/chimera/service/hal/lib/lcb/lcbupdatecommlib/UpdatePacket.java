package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib;

import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DataUtils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Comparator;

public class UpdatePacket {
    private static final String TAG = "UpdatePacket";
    public int priority = 0;

    private final int PACKET_HEADER_SIZE = 2;    //Start code(1 byte) + End Code (1 byte)
    private final int PACKET_COMMAND_SIZE = 1; //command(1 byte)
    private final int PACKET_MIN_SIZE = PACKET_HEADER_SIZE + PACKET_COMMAND_SIZE;
    private final int RX_BUFFER_SIZE = 1024;

    private byte[] _rxData = new byte[0];
    private int _rxIndex = 0;
    private byte[] _data = new byte[0];
    private UpdateCommands _command = UpdateCommands.CmdNone;
    private UpdateCommands _rtnCommand = UpdateCommands.CmdNone;

    private int _parseFieldIndex = 0;
    private int _parseDataLength = 0;
    private byte[] _rxFinalData = new byte[0];

    private ParseUpdateState _parseUpdateState = ParseUpdateState.LCBUPDATE_START;
    private ParseUpdateResult _parseUpdateResult = ParseUpdateResult.Incomplete;

    private final byte _SFC_ = (byte) 0xD0;
    private final byte _Start_ = (byte) 0xD1;
    private final byte _End_ = (byte) 0xD2;
    private final byte _REV_ = (byte) 0xD3;

    /*
        @Override
        public int compareTo(UpdatePacket updatePacket) {
            if(_command != updatePacket._command) {
                return -1;
            }

            if(_data.length != updatePacket._data.length) {
                return -1;
            }
            else {
                for(int i=0; i<_data.length; i++) {
                    if(_data[i] != updatePacket._data[i]) {
                        return -1;
                    }
                }
            }

            return 0;
        }
    */
    public enum ParseUpdateResult {
        Success,
        Incomplete,
        InvalidStartCode,
        InvalidRtnCommand,
        InvalidStuffByte,
        BufferOverflow
    }

    private enum ParseUpdateState {
        LCBUPDATE_START,
        LCBUPDATE_COMMAND,
        LCBUPDATE_DATA,
        LCBUPDATE_FRAME_BYTE_STUFF,
        LCBUPDATE_STOP
    }


    public UpdatePacket() {
    }

    public UpdatePacket(UpdateCommands command, UpdateCommands rtnCommand) {
        _command = command;
        _rtnCommand = rtnCommand;
    }

    public synchronized UpdateCommands getCommand() {
        return _command;
    }

    public synchronized UpdateCommands getRtnCommand() {
        return _rtnCommand;
    }

    public synchronized void setCommand(UpdateCommands command) {
        _command = command;
    }

    public synchronized byte[] getData() {
        return _rxFinalData;
    }

    public void setData(byte[] data) {
        if (data == null) {
            _data = new byte[0];
            return;
        }
        _data = data;
    }

    public synchronized ParseUpdateResult getParseUpdateResult() {
        return _parseUpdateResult;
    }

    public synchronized byte[] buildTxPacket() {
        byte[] resultData = new byte[PACKET_MIN_SIZE + _data.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(resultData);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // start byte
        byteBuffer.put((byte) _Start_);

        // command byte
        byteBuffer.put((byte) _command.Value);

        // data
        for (byte b : _data) {
            byteBuffer.put(b);
        }

        // end byte
        byteBuffer.put((byte) _End_);

        //resultData 	Stuff Packet
        return StuffPacket(resultData);
    }

    /*
     * Stuff Packet
     */
    private byte[] StuffPacket(byte[] bytes) {
        ByteArrayOutputStream packetData = new ByteArrayOutputStream();

        for (int i = 0; i < bytes.length; i++) {
            if (i == 0 || i == bytes.length - 1) // don't stuff the first and last byte
            {
                packetData.write((byte) bytes[i]);
            } else if (bytes[i] == (byte) 0xD0) {
                packetData.write((byte) 0xD0);
                packetData.write((byte) 0x00);
            } else if (bytes[i] == (byte) 0xD1) {
                packetData.write((byte) 0xD0);
                packetData.write((byte) 0x01);
            } else if (bytes[i] == (byte) 0xD2) {
                packetData.write((byte) 0xD0);
                packetData.write((byte) 0x02);
            } else if (bytes[i] == (byte) 0xD3) {
                packetData.write((byte) 0xD0);
                packetData.write((byte) 0x03);
            } else {
                packetData.write((byte) bytes[i]);
            }
        }

        return packetData.toByteArray();
    }

    public synchronized ParseUpdateResult parseUpdate(byte data) {
        // allocate rx buffer when needed
        if (_rxData.length == 0) {
            _rxData = new byte[RX_BUFFER_SIZE];
            _rxIndex = 0;
        }

        if (_rxData.length > RX_BUFFER_SIZE)
            return ParseUpdateResult.BufferOverflow;

        _rxData[_rxIndex] = data;

        if (data == _End_) {
            _parseUpdateState = ParseUpdateState.LCBUPDATE_STOP;
        }

        switch (_parseUpdateState) {
            case LCBUPDATE_START:
                if (data == _Start_) {
                    _data = new byte[RX_BUFFER_SIZE];
                    _parseUpdateState = ParseUpdateState.LCBUPDATE_COMMAND;
                } else {
                    // Missing start byte
                    return ParseUpdateResult.InvalidStartCode;
                }
                break;
            case LCBUPDATE_COMMAND:
                byte CommandID = data;
                int intCommandID = DataUtils.unsignedByteToInt(CommandID);
                _command = UpdateCommands.fromInt(intCommandID);
                if (_command == null) {
                    return ParseUpdateResult.InvalidRtnCommand;
                }
                _parseUpdateState = ParseUpdateState.LCBUPDATE_DATA;
                _parseFieldIndex = 0;
                _parseDataLength = 0;
                break;
            case LCBUPDATE_DATA:
                if ((data == _REV_) || (data == _Start_)) {
                    return ParseUpdateResult.InvalidStuffByte;
                }

                if (data == _SFC_) {
                    _parseUpdateState = ParseUpdateState.LCBUPDATE_FRAME_BYTE_STUFF;
                } else {
                    _data[_parseFieldIndex] = data;
                    _parseDataLength++;
                    _parseFieldIndex++;
                }
                break;
            case LCBUPDATE_FRAME_BYTE_STUFF:
                _data[_parseFieldIndex] = (byte) (_SFC_ + data);
                _parseDataLength++;
                _parseFieldIndex++;
                _parseUpdateState = ParseUpdateState.LCBUPDATE_DATA;
                break;
            case LCBUPDATE_STOP:
                _rxFinalData = new byte[_parseDataLength];
                for (int i = 0; i < _parseDataLength; i++) {
                    _rxFinalData[i] = _data[i];
                }
                _parseUpdateState = ParseUpdateState.LCBUPDATE_START;
                return ParseUpdateResult.Success;
        }

        _rxIndex++;

        if (_rxIndex >= RX_BUFFER_SIZE) {
            _parseUpdateState = ParseUpdateState.LCBUPDATE_START;
            _rxIndex = 0;
        }

        return ParseUpdateResult.Incomplete;
    }

    public synchronized void reset() {
        _rxData = new byte[0];
        _rxIndex = 0;
        _data = new byte[0];
        _command = UpdateCommands.CmdNone;
        _parseUpdateResult = ParseUpdateResult.Incomplete;
        _parseUpdateState = ParseUpdateState.LCBUPDATE_START;
        _parseFieldIndex = 0;
        _parseDataLength = 0;
        _rxFinalData = new byte[0];
    }

    public synchronized UpdatePacket clone() {
        UpdatePacket result = new UpdatePacket();

        result._command = _command;
        result._rtnCommand = _rtnCommand;
        result._data = new byte[_data.length];
        for (int i = 0; i < _data.length; i++) {
            result._data[i] = _data[i];
        }
        result._rxFinalData = new byte[_rxFinalData.length];
        for (int i = 0; i < _rxFinalData.length; i++) {
            result._rxFinalData[i] = _rxFinalData[i];
        }

        return result;
    }

    // priority queue needs something to compare even though update packets are not using priority
    public static class UpdatePacketSort implements Comparator<UpdatePacket> {
        @Override
        public int compare(UpdatePacket packet1, UpdatePacket packet2) {
            return Integer.compare(packet1.priority, packet2.priority);
        }
    }
}
