package com.jht.updated.utilities

import java.io.OutputStream

class BootEventParser: LogParser() {
    override fun parseLine(line: String, stream: OutputStream) {
        if (line.contains("beginning of")) {
            val event = line.substring(line.indexOf("beginning of"))
            val result = event + "\n"
            stream.write(result.toByteArray())
        }
    }
}