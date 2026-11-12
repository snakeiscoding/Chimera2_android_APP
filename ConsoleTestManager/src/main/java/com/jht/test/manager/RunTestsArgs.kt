package com.jht.test.manager

open class RunTestsArgs(args: Array<String>) {
    val zephyrAuth: String
    val projectKey: String
    val testCycleName: String
    val testCycleFolderId: Long


    init {
        if (args.size == 5) {
            zephyrAuth = args[1]
            projectKey = args[2]
            testCycleName = args[3]
            testCycleFolderId = args[4].toLong()
        } else {
            throw IllegalArgumentException()
        }
    }
}