package com.jht.test.manager

class RunTestsOnJenkinsArgs(
    args: Array<String>,
    val jenkinsUrl: String,
    val jenkinsJob: String,
    val jenkinsAuth: String,
    val jenkinsUser: String,
    val jenkinsPassword: String
) : RunTestsArgs(args) {

    constructor(args: Array<String>) : this(
        args = args.copyOfRange(0, 5),
        jenkinsUrl = if(args.size > 5) args[5] else "",
        jenkinsJob = if(args.size > 6) args[6] else "",
        jenkinsAuth = if(args.size > 7) args[7] else "",
        jenkinsUser = if(args.size > 8) args[8] else "",
        jenkinsPassword = if(args.size > 9) args[9] else ""

    ) {
        if (args.size != 10) {
            throw IllegalArgumentException()
        }
    }

}