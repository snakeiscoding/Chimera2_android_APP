package com.jht.test.manager

class GetTestArgs(args: Array<String>) {
    val zephyrAuth: String
    val projectKey: String
    val testCaseFolderId: Long
    val srcFolders: MutableList<String> = ArrayList()

    init {
        if (args.size > 4) {
            zephyrAuth = args[1]
            projectKey = args[2]
            testCaseFolderId = args[3].toLong()
            for(i in 4 until args.size) {
                srcFolders.add(args[i])
            }
        } else {
            throw IllegalArgumentException()
        }
    }
}