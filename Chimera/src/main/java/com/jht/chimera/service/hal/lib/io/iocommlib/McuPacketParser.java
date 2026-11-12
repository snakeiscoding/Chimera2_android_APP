package com.jht.chimera.service.hal.lib.io.iocommlib;

import android.util.Log;

public class McuPacketParser {
    private static final String TAG = "McuPacketParser";

    public McuPacket packet = new McuPacket(); // do this as member or just have parser extend packet?

    private int commandCode = 0;
    private int packetStatus = 0;
    private byte[] payload = null;
    private int payloadLength = 0;
    private int payloadIndex = 0;

    private final CRC_8 crcCalculator = new CRC_8();

    private PacketFrame parseStage = PacketFrame.START_1;

    enum PacketFrame {
        START_1,
        START_2,
        COMMAND_1,
        COMMAND_2,
        RESULT_1,
        RESULT_2,
        LENGTH_1,
        LENGTH_2,
        PAYLOAD,
        CHECKSUM,
    }

    enum ParseResult {
        Success,
        NoReply,
        Incomplete,
        InvalidStartByte,
        InvalidCrc,
        Failure,
        InvalidLength
    }

    public McuPacketParser() {
        reset();
    }

    public void reset(){
//        Log.d(TAG,"reset packet");
        payload = null;
        parseStage = PacketFrame.START_1;
    }

    public ParseResult feedByte(byte aByte){
//        Log.d(TAG,"parse status: " + parseStatus);

        switch (parseStage) {
            case START_1:
                if (aByte == 0x55) {
                    crcCalculator.reset();
                    crcCalculator.feedByte(aByte);
                    parseStage = PacketFrame.START_2;
                } else {
                    return ParseResult.InvalidStartByte;
                }
                break;
            case START_2:
                if(aByte == (byte)0xAA) {
                    crcCalculator.feedByte(aByte);
                    parseStage = PacketFrame.COMMAND_1;
                } else {
                    return ParseResult.InvalidStartByte;
                }
                break;
            case COMMAND_1:
                crcCalculator.feedByte(aByte);
                commandCode = aByte & 0xff;
                parseStage = PacketFrame.COMMAND_2;
                break;
            case COMMAND_2:
                crcCalculator.feedByte(aByte);
                commandCode |= ((aByte & 0xFF) << 8);
                //Log.d(TAG, "command code: " + commandCode);
                parseStage = PacketFrame.RESULT_1;
                break;
            case RESULT_1:
                crcCalculator.feedByte(aByte);
                packetStatus = aByte & 0xff;
                parseStage = PacketFrame.RESULT_2;
                break;
            case RESULT_2:
                crcCalculator.feedByte(aByte);
                packetStatus |= ((aByte & 0xFF) << 8);
                parseStage = PacketFrame.LENGTH_1;
                break;
            case LENGTH_1:
                crcCalculator.feedByte(aByte);
                payloadLength = aByte & 0xff;
                parseStage = PacketFrame.LENGTH_2;
                break;
            case LENGTH_2:
                crcCalculator.feedByte(aByte);
                payloadLength |= ((aByte & 0xFF) << 8);
                //Log.d(TAG,"parse Length: " + payloadLength);
                if(payloadLength == 0) {
                    parseStage = PacketFrame.CHECKSUM;
                } else if (payloadLength > 600) {
                    Log.e(TAG, "data length exceeds 600 which is not expected, length=" + payloadLength);
                    return ParseResult.InvalidLength;
                } else {
                    payload = new byte[payloadLength];
                    payloadIndex = 0;
                    parseStage = PacketFrame.PAYLOAD;
                }
                break;
            case PAYLOAD:
                crcCalculator.feedByte(aByte);
                payload[payloadIndex] = aByte;
                if (++payloadIndex == payloadLength) {
                    parseStage = PacketFrame.CHECKSUM;
                }
                break;
            case CHECKSUM:
                /*String checkDataString = "";
                for(int i= 0; i <= rxIndex; i++)
                    checkDataString += String.format("%X",rxData[i]) + " ";
                Log.d(TAG,"checkSum data : " + checkDataString + " index: " + rxIndex);*/

                byte checkSum = crcCalculator.getCrc();
                if(aByte != checkSum) {
                    Log.e(TAG,"parse CheckSum: " + String.format("%X", checkSum) + " but read: " + String.format("%X", aByte));
                    return ParseResult.InvalidCrc;
                }
//                Log.d(TAG,"Command Type: " + commandType.toString());
                packet.commandCode = this.commandCode;
                packet.status = this.packetStatus;
                packet.data = this.payload;
                reset(); // reset for next?
                return ParseResult.Success;
        }

        return ParseResult.Incomplete;
    }

    public static boolean isInvalid(ParseResult result) {
        return (result == McuPacketParser.ParseResult.InvalidStartByte)
                || (result == McuPacketParser.ParseResult.InvalidCrc)
                || (result == McuPacketParser.ParseResult.InvalidLength);
    }

}
