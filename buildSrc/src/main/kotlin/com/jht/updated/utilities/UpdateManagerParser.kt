package com.jht.updated.utilities

import java.io.OutputStream

class UpdateManagerParser: LogParser() {
    private val tokens = arrayOf(
        "Starting unzipping process",
        "UpdateDownloadManager:",
        "errorOccurred",
        "done with update",
        "command Code",
        "checksum"
    )

    override fun parseLine(line: String, stream: OutputStream) {
        for (token in tokens) {
            if (line.contains(token)) {
                val event = line.substring(line.indexOf(token))
                val result = getTimeStamp(line) + " " + event + "\n"
                stream.write(result.toByteArray())
            }
        }
    }
}