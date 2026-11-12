package com.jht.test.manager

class RunTestArgs(
    val zephyrAuth: String,
    val projectKey: String,
    val testIndex: Int,
    val testCycle: String,
    val console: String,
    val serverIP: String) {

    constructor(args: Array<String>) : this(
        zephyrAuth = if(args.size > 1) args[1] else "",
        projectKey = if(args.size > 2) args[2] else "",
        testIndex = if(args.size > 3) args[3].toInt() else -1,
        testCycle = if(args.size > 4) args[4] else "",
        console = if(args.size > 5) args[5] else "",
        serverIP = if(args.size > 6) args[6] else ""

    ) {
        if (args.size != 7) {
            throw IllegalArgumentException()
        }
    }

    constructor(runTestsArgs: RunTestsArgs, testIndex: Int, testCycle: String, console: String) : this(
        runTestsArgs.zephyrAuth,
        runTestsArgs.projectKey,
        testIndex,
        testCycle,
        console,
        ""
    )

}