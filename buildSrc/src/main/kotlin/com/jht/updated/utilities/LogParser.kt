package com.jht.updated.utilities

import java.io.OutputStream

abstract class LogParser {
    abstract fun parseLine(line: String, stream: OutputStream)
    open fun reset() {}
    fun getTimeStamp(line: String): String = line.substring(0, 34)
}