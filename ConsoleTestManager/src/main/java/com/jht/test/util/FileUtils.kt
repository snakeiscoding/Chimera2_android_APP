package com.jht.test.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

fun File.readFile(): ByteArray {
    if (exists()) {
        return catchAll(ByteArray(0) ) {
            val buffer = ByteArray(length().toInt())
            FileInputStream(this).use { fin ->
                fin.read(buffer)
                buffer
            }
        }
    }
    return ByteArray(0)
}

fun File.readFileToString(): String {
    return if (exists()) {
        String(readFile())
    } else ""
}

fun File.writeToFile(data: String): Boolean {
    return writeToFile(data.toByteArray())
}

fun File.writeToFile(data: ByteArray): Boolean {
    if (exists() && isDirectory) {
        return false
    }
    val parentFile = parentFile
    parentFile?.mkdirs()
    return catchAll(false) {
        FileOutputStream(this).use{
            it.write(data)
            true
        }
    }
}