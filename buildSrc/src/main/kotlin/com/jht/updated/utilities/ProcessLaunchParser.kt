package com.jht.updated.utilities

import java.io.OutputStream
import java.util.regex.Pattern

class ProcessLaunchParser: LogParser() {
    override fun parseLine(line: String, stream: OutputStream) {
        if (line.contains("START u0")) {
            var process = line.find(Pattern.compile("cmp=.*}"))!!.toString()
            process = process.substring(4, process.length - 1)
            val uid = line.substring(line.lastIndexOf(" "))
            val result = getTimeStamp(line) + " " + uid + " started " + process + "\n"
            stream.write(result.toByteArray())
        }
    }
}

/**
 * Fixme: This should get moved into the extensions too?
 */
fun String.find(pattern: Pattern): String? {
    val matcher = pattern.matcher(toString())
    return if (matcher.find()) matcher.group(0) else null
}