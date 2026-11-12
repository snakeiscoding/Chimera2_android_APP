package com.jht.test.manager

internal class TestCycleArgs(
    val zephyrAuth: String = "",
    val projectKey: String = "",
    val testCycleFolderId: Long = 0,
    val testCycleName: String = "") {

    constructor(args: Array<String>) : this(
        zephyrAuth = if(args.size > 1) args[1] else "",
        projectKey = if(args.size > 2) args[2] else "",
        testCycleFolderId = if(args.size > 3) args[3].toLong() else 0,
        testCycleName = if(args.size > 4) args[4] else ""

    ) {
        if (args.size != 5) {
            throw IllegalArgumentException()
        }
    }

    constructor(runTestArgs: RunTestsArgs) : this(
        runTestArgs.zephyrAuth,
        runTestArgs.projectKey,
        runTestArgs.testCycleFolderId,
        runTestArgs.testCycleName
    )
}