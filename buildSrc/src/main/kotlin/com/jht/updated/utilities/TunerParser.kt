package com.jht.updated.utilities

import java.io.OutputStream
import javax.xml.bind.DatatypeConverter

class TunerParser: LogParser() {
    override fun parseLine(line: String, stream: OutputStream) {
        if (line.contains("Write serial port") && line .contains("ttymxc3")) {
            val dataBytes = line.substring(line.lastIndexOf(" ") + 1)
            if (dataBytes.length >= 6) {
                val result = getTimeStamp(line) + " Sent " + String(DatatypeConverter.parseHexBinary(dataBytes.substring(0, 6))) + " to tuner\n"
                stream.write(result.toByteArray())
            }

        }
        else if (line.contains("Read serial port") && line .contains("ttymxc3")) {
            val dataBytes = line.substring(line.lastIndexOf(" ") + 1)
            if (dataBytes.toLowerCase().startsWith("f5") && dataBytes.length >= 6) {
                val result = getTimeStamp(line) + " Received ir code 0x" + dataBytes.substring(4, 6).toUpperCase() + " from tuner: " + dataBytes + "\n"
                stream.write(result.toByteArray())
            }
            else {
                val result = getTimeStamp(line) + " Received garbage from tuner: " + dataBytes + "\n"
                stream.write(result.toByteArray())
            }
        }
        else if (line.contains("Parsing Swift packet")) {
            val command = line.substring(line.lastIndexOf(" ") + 1)
            val result = getTimeStamp(line) + " Parsing tuner packet " + command + "\n"
            stream.write(result.toByteArray())
        } else if (line.contains("Missing response ")) {
            val command = line.substring(line.lastIndexOf(" ") + 1)
            val result = getTimeStamp(line) + " Missing tuner packet " + command + "\n"
            stream.write(result.toByteArray())
        } else if (line.contains("resetTunerProperties")) {
            val result = getTimeStamp(line) + " resetTunerProperties\n"
            stream.write(result.toByteArray())
        }
    }
}