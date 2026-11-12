package com.jht.updated

import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.Variant
import org.gradle.api.Project
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

//==================================================================================================
// Extensions for java.io.File
//==================================================================================================

// todo: document
fun File.get(vararg pathParts: String): File = File(this, pathParts.joinToString(File.separator))

// todo: document
fun File.get(pathParts: List<String>): File = File(this, pathParts.joinToString(File.separator))

/**
 * The size of this file or directory in Bytes. For a directory, its the total size of all children
 * in bytes.
 *
 * @param errorOnEmptyDir Whether to throw an error when dealing with an empty directory or not.
 */
fun File.numBytes(errorOnEmptyDir: Boolean): Long {
    if (isFile) {
        return length()
    }

    if (isDirectory) {
        var size = 0L
        val children = listFiles() ?: if (errorOnEmptyDir) error("$this is empty.") else emptyArray()
        children.forEach { size += it.numBytes(errorOnEmptyDir) }
        return size
    }

    error("$this does not exist.")
}

/**
 * The non-null list of children for a directory. This simplifies the logic for
 */
val File.children: List<File> get() {
    val result = mutableListOf<File>()
    listFiles()?.let { result.addAll(it) }
    return result
}

/**
 * Create a directory where this [File] is referencing if it doesn't already exist. If there are
 * missing parent directories along the way, they are created.
 */
fun File.createDirectory(): Boolean {
    var created = false
    if (!exists()) {
        if (!mkdirs()) {
            error("Failed to create directory $this")
        }
        created = true
    }
    if (!isDirectory) {
        error("$this is not a directory")
    }
    return created
}

/**
 * Remove the file or directory this [File] is referencing. If referencing a directory, the children
 * are deleted using a recursive top-down implementation.
 */
fun File.remove() {
    if (exists()) {
        if (isDirectory) {
            listFiles()?.forEach { it.remove() }
        }
        delete()
        if (exists()) {
            error("Failed to remove $this")
        }
    }
}

/**
 * Unzip this file into the given output directory.
 */
fun File.unzipFile(dest: File, password: String?) {
    val process: Process = if (password != null) {
        Runtime.getRuntime().exec(arrayOf("7z", "x", "-P$password", "-o${dest.absolutePath}", absolutePath))
    } else {
        Runtime.getRuntime().exec(arrayOf("7z", "x", "-o${dest.absolutePath}", absolutePath))
    }
    println(process.text)
}

/**
 * Zip the files in this directory into the given output directory.
 */
fun File.zipFiles(output: String, password: String?) {

    val process: Process = if (password != null) {
        Runtime.getRuntime().exec(arrayOf("7z", "-r", "a", "-P$password", "-mem=AES256", output, "*"), null, this)
    } else {
        Runtime.getRuntime().exec(arrayOf("7z", "-r", "a", "-mem=AES256", output, "*"), null, this)
    }
    println(process.text)
}

fun File.zipFiles(dest: File, password: String?) = zipFiles(
    output = dest.absolutePath,
    password = password
)

/**
 * Used to zip individual files not dirs
 */
fun File.zipFile(dest: File, password: String?) {
    val temp = File(dest.parentFile, "temp/mapping.txt")
    this.copyFile(temp)
    temp.parentFile.zipFiles(dest, password)
}

// fixme: this doesn't include cleanup.
fun File.copyDirectory(dest: File) {
    if (!isDirectory) {
        error("$this is not a directory")
    }
    dest.createDirectory()
    listFiles()?.forEach { child ->
        if (child.isDirectory) {
            child.copyDirectory(File(dest, child.name))
        } else if (child.isFile) {
            child.copyFile(File(dest, child.name))
        } else {
            error("$this is not a file or directory")
        }
    }
    if (!dest.exists()) {
        error("Failed to copy the directory.")
    }
    if (!dest.exists()) error("Failed to copy the directory.")
}

/**
 * TODO: Comment
 */
fun File.copyFile(dest: File, bufferSize: Int = 8192) {
    if (!isFile) {
        error("$this is not a file.")
    }
    if (dest.isDirectory) {
        error("$dest is a directory which is unexpected.")
    }
    if (dest.isFile) {
        println("Skipping existing destination.")
        return
    }
//    // fixme: Do not include override logic. This just needs to rehost everything.
//    if (dest.exists() && !overwrite) error("Cannot copy $this to $dest because it already exists")
//    dest.remove() // this will ensure the destination is removed if existing and not a file.
    dest.parentFile.createDirectory()
    try {
        inputStream().use { inputStream ->
            dest.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream, bufferSize)
            }
        }
    } catch (t: Throwable) {
        try {
            dest.remove()
        } catch (_: Throwable) {}
        throw t
    }
    if (!dest.exists()) {
        error("Failed to copy $this to $dest")
    }
}

/**
 * Copy this file into the given dest.
 */
fun File.copyFileInto(dest: File, bufferSize: Int = 8192) {
    if (!isFile) {
        error("$this is not a file")
    }
    copyFile(File(dest, name), bufferSize)
}

/**
 * copy the contents of this directory into the given dest.
 */
fun File.copyDirContentsInto(dest: File, bufferSize: Int = 8192) {
    if (!isDirectory) {
        error("$this is not a directory")
    }
    listFiles()?.forEach { child ->
        if (child.isFile) {
            child.copyFile(File(dest, child.name), bufferSize)
        } else if (child.isDirectory) {
            child.copyDirectory(File(dest, child.name))
        } else {
            error("$child is not a file or directory")
        }
    }
}

/** Feeds [file] into this digest in chunks. Same bytes, same order, so the hash is unchanged. */
private fun MessageDigest.updateFrom(file: File) {
    val buffer = ByteArray(64 * 1024)
    file.inputStream().buffered().use { input ->
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            update(buffer, 0, read)
        }
    }
}

/**
 * The MD5 of this File. Chunked rather than loaded whole; staged bundle zips reach ~870MB and every
 * bundle is hashed once per manifest, four manifests over ~70 bundles per stage task.
 */
val File.md5: String get() {
    val md = MessageDigest.getInstance("MD5")
    if (isDirectory) {
        val folders = mutableListOf<File>()
        folders.add(this)
        while (folders.isNotEmpty()) {
            val folder = folders.removeAt(0)
            val files = folder.listFiles()?.sorted() ?: continue
            files.forEach { f ->
                if (f.isDirectory) {
                    folders.add(f)
                } else if (f.isFile) {
                    md.updateFrom(f)
                } else {
                    error("$f is neither file or directory.")
                }
            }
        }
    } else {
        md.updateFrom(this)
    }

    return md.digest().joinToString("") { String.format("%02x", it) }
}

/**
 * Returns the MD5 of a file
 * Intended for files larger than 1GB
 */
val File.largeFileMD5: String get() {
    val osName = System.getProperty("os.name")

    //Build services should NOT be using Windows, this is here for testing purposes
    //md5sum is more secure than CertUtil
    val process: Process = if (osName.contains("Windows"))
        Runtime.getRuntime().exec(arrayOf("CertUtil", "-hashfile", "$path", "MD5"))
    else if (osName.contains("Linux"))
        Runtime.getRuntime().exec(arrayOf("md5sum", "$path"))
    else
        throw error("Can't md5 check on $osName")

    val text = process.text
    if (osName.contains("Windows"))
        return text.split('\n')[1]
    else if (osName.contains("Linux"))
        return text.split(' ')[0]

    return text
}

//fun File.createFile(builder: StringBuilder) = createFile(builder.toString())

fun File.createFile(contents: String) {
    if (isDirectory) {
        error("$this is a directory.")
    }
    remove()
    parentFile.mkdirs()
    createNewFile()
    writeText(contents)
}

//==================================================================================================
// Extensions for InputStream
//==================================================================================================
/**
 * TODO: Comment
 */
fun InputStream.copyTo(outputStream: OutputStream, bufferSize: Int = 8192): Long {
    var bytesCopied = 0L
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0) {
        outputStream.write(buffer, 0, bytes)
        bytesCopied += bytes
        bytes = read(buffer)
    }
    return bytesCopied
}


//==================================================================================================
// Extensions for List
//==================================================================================================
/**
 * The size of the given list of files or directories in Bytes.
 *
 * @param errorOnEmptyDir Whether to throw an error if an empty directory is encountered.
 */
fun List<File>.numBytes(errorOnEmptyDir: Boolean): Long {
    var size = 0L
    forEach { size += it.numBytes(errorOnEmptyDir) }
    return size
}

/**
 * Copy all given files and directories into the correct location.
 */
fun List<File>.copyInto(dest: File, bufferSize: Int = 8192) {
    forEach { file ->
        if (file.isFile) {
            file.copyFileInto(dest, bufferSize)
        } else if (file.isDirectory) {
            file.copyDirContentsInto(dest, bufferSize)
        } else {
            error("$file is not a file or directory")
        }
    }
}

//==================================================================================================
// Extensions for Process
//==================================================================================================
/**
 * Closes all the streams associated with this process (ignoring any IOExceptions).
 */
fun Process.closeStreams() {
    try { errorStream.close() } catch (_: Throwable) {}
    try { inputStream.close() } catch (_: Throwable) {}
    try { outputStream.close() } catch (_: Throwable) {}
}

val Process.text: String get() {
    val result = inputStream.bufferedReader().use { it.readText() }
    closeStreams()
    return result
}


//==================================================================================================
// Extensions for Project
//==================================================================================================
/** Locate the keystore directory for this project. */
val Project.keystore: File get() {
    var path: File? = projectDir
    while (path != null) {
        val keystore = File(path, "Keystore")
        if (keystore.isDirectory) {
            return keystore
        }
        path = path.parentFile
    }
    error("Unable to locate the keystore directory")
}

//==================================================================================================
// Extensions for String
//==================================================================================================
/**
 * Convert this String to a URL.
 */
fun String.toURL(): URL = URL(this)

//==================================================================================================
// Extensions for URL
//==================================================================================================
/**
 * fixme: Need to update the documentation for this method.
 */
fun URL.exists(): Boolean {
    var httpURLConnection: HttpURLConnection? = null

    try {
        httpURLConnection = openConnection() as HttpURLConnection
        httpURLConnection.requestMethod = "HEAD"
        val responseCode = httpURLConnection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return true
        }
        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
            error("$this has moved permanently to ${httpURLConnection.getHeaderField("Location")}. Please change to the new URL.")
        }
        return false
    } finally {
        httpURLConnection?.disconnect()
    }
}

/**
 * This will download the file if it exists. If it doesn't then it won't download the file.
 *
 * fixme: need to finish documenting.
 *
 * @return `true` if the file was downloaded, else `false`.
 */
fun URL.download(dest: File): Boolean {
    if (!exists()) {
        return false
    }
    if (dest.isDirectory) {
        error("Invalid destination. $dest is a directory.")
    }
    if (dest.isFile) {
        println("Skipping, this already exists.")
        return false
    }
//    dest.remove()
    dest.parentFile.createDirectory()
    // fixme: likely need to check exists first. Don't care though right now.
    openStream().buffered().use { istream ->
        dest.outputStream().use { ostream ->
            istream.copyTo(ostream)
        }
    }
    return true
}

//==================================================================================================
// Extensions for com.android.build.api.variant.Variant
//==================================================================================================
fun Variant.putBooleanBuildConfig(name: String, value: Boolean?, comment: String? = null) {
    buildConfigFields?.put(name, BuildConfigField("Boolean",
        if (value == null) {
            "null"
        } else if (value) {
            "true"
        } else {
            "false"
        }, comment)
    )
}

fun Variant.putIntBuildConfig(name: String, value: Int?, comment: String? = null) {
    buildConfigFields?.put(name, BuildConfigField("Integer",
        if (value == null) {
            "null"
        } else {
            "$value"
        }, comment)
    )
}

fun Variant.putStringBuildConfig(name: String, value: String?, comment: String? = null) {
    buildConfigFields?.put(name, BuildConfigField("String",
        if (value == null) {
            "null"
        } else {
            "\"$value\""
        }, comment)
    )
}

fun Variant.putNonNullBooleanBuildConfig(name: String, value: Boolean, comment: String? = null) {
    buildConfigFields?.put(name, BuildConfigField("boolean",
        if (value) {
            "true"
        } else {
            "false"
        }, comment)
    )
}