package com.jht.chimera.service.hal.lib.io.iocommlib;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class McuPacketParserTest {
    // rev bytes for version packets
    private static final byte revMinor = 0xA;
    private static final byte revMajor = 0x5;
    private static final byte revCrc = (byte)0x96; // crc for version response containing the above

    private McuPacket createVersionResponse() {
        return new McuPacket(McuProtocol.McuCommands.VERSION, revMinor, revMajor);
    }
    private McuPacket createVersionCommand() {
        return new McuPacket(McuProtocol.McuCommands.VERSION);
    }

    @Test public void testPacketCrc() {
        McuPacket packet = createVersionResponse();
        byte[] packetBytes = packet.getBytes();
        assertEquals(packet.getDataLength() + 9, packetBytes.length); // packet should be 9 bytes plus data
        assertEquals(revCrc, packetBytes[packetBytes.length-1]);
    }

    @Test public void testPacketWithPayload() {
        McuPacketParser parser = new McuPacketParser();
        McuPacket packet = createVersionResponse();
        byte[] packetBytes = packet.getBytes();
        McuPacketParser.ParseResult result = McuPacketParser.ParseResult.Failure;
        for (byte b : packetBytes) {
            result = parser.feedByte(b);
            if (McuPacketParser.isInvalid(result)) break;
        }

        assertEquals(McuPacketParser.ParseResult.Success, result);

        assertEquals(McuProtocol.McuCommands.VERSION.get_command(), parser.packet.commandCode);
        assertEquals(McuProtocol.McuStatus.Success.getValue(), parser.packet.status);
        assertEquals(2, parser.packet.getDataLength());
        assertEquals(revMinor, parser.packet.data[0]);
        assertEquals(revMajor, parser.packet.data[1]);

        assertEquals(packet, parser.packet);
    }

    @Test public void testPacketWithoutPayload() {
        McuPacketParser parser = new McuPacketParser();
        McuPacket packet = createVersionCommand();
        byte[] packetBytes = packet.getBytes();
        McuPacketParser.ParseResult result = McuPacketParser.ParseResult.Failure;
        for (byte b : packetBytes) {
            result = parser.feedByte(b);
            if (McuPacketParser.isInvalid(result)) break;
        }

        assertEquals(McuPacketParser.ParseResult.Success, result);

        assertEquals(McuProtocol.McuCommands.VERSION.get_command(), parser.packet.commandCode);
        assertEquals(McuProtocol.McuStatus.Success.getValue(), parser.packet.status);
        assertEquals(0, parser.packet.getDataLength());

        assertEquals(packet, parser.packet);
    }

    @Test public void testPacketStatus() {
        final McuProtocol.McuStatus testStatus = McuProtocol.McuStatus.UnknownCommand;
        final int testStatusValue = 1;
        assertEquals(testStatusValue, testStatus.getValue()); // check enum value is correct

        McuPacketParser parser = new McuPacketParser();
        McuPacket packet = createVersionResponse();
        packet.status = testStatus.getValue();
        byte[] packetBytes = packet.getBytes();
        McuPacketParser.ParseResult result = McuPacketParser.ParseResult.Failure;
        for (byte b : packetBytes) {
            result = parser.feedByte(b);
            if (McuPacketParser.isInvalid(result)) break;
        }

        assertEquals(McuPacketParser.ParseResult.Success, result);

        assertEquals(McuProtocol.McuCommands.VERSION.get_command(), parser.packet.commandCode);
        assertEquals(testStatus.getValue(), parser.packet.status);
        assertEquals(2, parser.packet.getDataLength());
        assertEquals(revMinor, parser.packet.data[0]);
        assertEquals(revMajor, parser.packet.data[1]);

        assertEquals(packet, parser.packet);
    }

    @Test public void testBadHeader() {
        McuPacketParser parser = new McuPacketParser();
        McuPacket packet = createVersionResponse();
        byte[] packetBytes = packet.getBytes();
        packetBytes[1] = 0x00; // mess up 2nd start byte
        McuPacketParser.ParseResult result = McuPacketParser.ParseResult.Failure;
        for (byte b : packetBytes) {
            result = parser.feedByte(b);
            if (McuPacketParser.isInvalid(result)) break;
        }

        assertEquals(McuPacketParser.ParseResult.InvalidStartByte, result);
    }

    @Test public void testBadChecksum() {
        McuPacketParser parser = new McuPacketParser();
        McuPacket packet = createVersionResponse();
        byte[] packetBytes = packet.getBytes();
        packetBytes[packetBytes.length-1] = 0x00; // blow away CRC byte
        McuPacketParser.ParseResult result = McuPacketParser.ParseResult.Failure;
        for (byte b : packetBytes) {
            result = parser.feedByte(b);
            if (McuPacketParser.isInvalid(result)) break;
        }

        assertEquals(McuPacketParser.ParseResult.InvalidCrc, result);
    }

    @Test public void testBadLength() {
        McuPacketParser parser = new McuPacketParser();
        McuPacket packet = createVersionResponse();
        byte[] packetBytes = packet.getBytes();
        packetBytes[McuPacketParser.PacketFrame.LENGTH_1.ordinal()] = (byte)0xFF; // make it freaking huge
        packetBytes[McuPacketParser.PacketFrame.LENGTH_2.ordinal()] = (byte)0xFF;
        McuPacketParser.ParseResult result = McuPacketParser.ParseResult.Failure;
        for (byte b : packetBytes) {
            result = parser.feedByte(b);
            if (McuPacketParser.isInvalid(result)) break;
        }

        assertEquals(McuPacketParser.ParseResult.InvalidLength, result);
    }

}
