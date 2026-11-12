package com.jht.test.manager

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jht.test.consolefinder.ConsoleFinder
import com.jht.test.testfinder.InstrumentationTest
import com.jht.test.testfinder.TestFinder
import com.jht.test.util.SystemCommandHelper
import com.jht.test.util.catchAll
import com.jht.test.util.readFileToString
import com.jht.test.util.writeToFile
import com.jht.test.zephyr.TestCase
import com.jht.test.zephyr.TestCycle
import com.jht.test.zephyr.TestFolder
import com.jht.test.zephyr.Zephyr
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

object ConsoleTestManager {
    private const val consoleListFile = "consoles.txt"
    private const val testsFile = "tests.json"
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            if(args.isNotEmpty()) {
                when (args[0].lowercase(Locale.ENGLISH)){
                    "get_tests" -> getTests(GetTestArgs(args))
                    "run_tests" -> runTests(RunTestsArgs(args))
                    "run_tests_on_jenkins" -> runTestsOnJenkins(RunTestsOnJenkinsArgs(args))
                    "find_consoles" -> outputConsoles(ConsoleFinder(FindArgs(args).consoleType).findConsoles())
                    "run_test" -> runTest(RunTestArgs(args))
                    else -> {
                        printHelp()
                        exitProcess(-1)
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            printHelp()
            exitProcess(-1)
        }

    }

    private fun printHelp() {
        val help = """java -jar ConsoleTestManager.jar COMMAND ARGS

	Commands are case insensitive.
    GET_TESTS ZEPHYR_AUTH PROJECT_KEY TEST_CASE_FOLDER_ID FOLDER [FOLDER2 ...]
        This will search the java code in the folder(S) given and output a JSON file with all the instrumentation tests it finds.  It will also make sure the tests exist in Zephyr which will make running them easier.  This will have all the information needed to run the test and push the results.
        
        ZEPHYR_AUTH: The API access key for zephyr.
        PROJECT_KEY: The Jira project key that has zephyr.  For Chimera this is CHIM.
        TEST_CASE_FOLDER_ID: The folder id that contains the Zephyr test cases.  Note this is an integer.
        FOLDER: The folder(S) to search.

	FIND_CONSOLES [CONSOLE_TYPE]
		This outputs the IP address for all consoles that it finds on the network with the given CONSOLE_TYPE.  the addresses are written to consoles.txt which can be used in subsequent commands.
        
        CONSOLE_TYPE: The console type to use in the search.  Defaults to chimera_main_matrix_chimera.

	RUN_TEST ZEPHYR_AUTH PROJECT_KEY TEST_NAME TEST_RUNNER TEST_PACKAGE [TEST_CLASS [TEST_FUNCTION]]
		This will run an instrumentation on a console previously found in the update command and will post results to the test cycle that was created with the create_test_cycle command.  You must provide the test runner and test package to run.  Optionally you can provide the test class and the test function to target a smaller test set.  Each test function that runs will be posted to zephyr in the test cycle previously created.  Each function will map to a test case.  This will make sure the test case exists on zephyr and create one if needed.  Example as follows: 
			java -jar ConsoleTestManager.jar run_test ${"$"}ZEPHYR_AUTH CHIM 2257576 "EGym Tests" "com.jht.chimera.test/.runner.SplashAppTestRunner" com.jht.chimera.ui ErrorCodeFlashTest "

        ZEPHYR_AUTH: The API access key for zephyr.
        PROJECT_KEY: The Jira project key that has zephyr.  For Chimera this is CHIM.
        TEST_INDEX: The index to the test in the tests.json which was output in a previous GET_TEST call.

    RUN_TESTS ZEPHYR_AUTH PROJECT_KEY TEST_CYCLE_NAME 
        This will run all tests in tests.json using the consoles in consoles.txt.  It will do this in parallel based on the number of consoles available. It will create a new test cycle with the name input.  Make sure to update the consoles manually before running this.
    
        ZEPHYR_AUTH: The API access key for zephyr.
        PROJECT_KEY: The Jira project key that has zephyr.  For Chimera this is CHIM.
        TEST_CYCLE_NAME: The name of the test cycle.
        
    RUN_TESTS_JENKINS ZEPHYR_AUTH PROJECT_KEY TEST_CYCLE_NAME JENKINS_SERVER JENKINS_JOB JENKINS_AUTH
        This will run all tests in tests.json using the consoles in consoles.txt by spawning a Jenkins task for each test.  It will pass the necessary arguments so that it runs the test.
    
        ZEPHYR_AUTH: The API access key for zephyr.
        PROJECT_KEY: The Jira project key that has zephyr.  For Chimera this is CHIM.
        TEST_CYCLE_NAME: The name of the test cycle.
        JENKINS_SERVER: The url of the Jenkins server that contains the job.
        JENKINS_JOB: The job that will run the test.
        JENKINS_AUTH: Auth key that will allow this to call the Jenkins job.
        
"""
        println(help)
    }

    private fun runTest(args: RunTestArgs) {
        try {
            if(args.testIndex < 0) {
                println("Update console to latest software.")
                val console = Console(args.console, false)
                if(console.connect()) {
                    SystemCommandHelper().execute("sh updateToLatest.sh")
                    console.disconnect()
                }
                return
            }
            else if(args.testIndex >= 0 && args.testIndex < tests.size) {
                println("Run test")
                val testPassed = ConsoleTest(args, tests[args.testIndex]).runTest(args.testCycle)
                if(args.serverIP.isNotEmpty()) {
                    notifyTestFinished(args.serverIP, args.console)
                }
                if(testPassed) {
                    return
                }
            }
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }
        exitProcess(-1)
    }

    private fun notifyTestFinished(serverIp: String, console: String) {
        catchAll {
            Socket(serverIp, 5000).use { socket ->
                socket.getOutputStream().use { stream ->
                    stream.write("$console\n".toByteArray())
                }
            }
        }
    }

    private fun createTestCycle(args: TestCycleArgs): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        val zephyr = Zephyr(args.zephyrAuth, args.projectKey)
        val testCycle = TestCycle(TestFolder(args.testCycleFolderId),args.testCycleName + " " + simpleDateFormat.format(Date() ))
        zephyr.createTestCycle(testCycle, gitRevision)
        return testCycle.key
    }

    private val testVersion: String by lazy {
        File("rscu_version.txt").readFileToString().trimEnd().trim()
    }

    private val gitRevision: String by lazy {
        File("revision.txt").readFileToString().trimEnd().trim()
    }

    private val consoles: List<String> by lazy {
        listOf(*File(consoleListFile).readFileToString().split(" ".toRegex()).toTypedArray())
    }
    private val sType = object : TypeToken<List<InstrumentationTest>>() { }.type
    private val tests: List<InstrumentationTest> by lazy {
        Gson().fromJson(File(testsFile).reader(), sType)
    }

    private fun outputConsoles(consoles: List<String>) {
        val str = consoles.joinToString(" ")
        println("Found consoles $str")
        File(consoleListFile).writeToFile(str)
    }

    private fun runTests(args: RunTestsArgs) {
        val testCycle = createTestCycle(TestCycleArgs(args))
        var runningTests = true
        val consoleChannel = Channel<String>()
        val consolesUpdated: MutableList<String> = ArrayList()


        runBlocking(Dispatchers.IO) {
            consoles.forEach { consoleIP ->
                launch {
                    kotlin.runCatching {
                        val console = Console(consoleIP, true)
                        if (console.updateUpdateManager()) {
                            delay(45000)
                            if (console.update()) {
                                delay(5 * 60000)
                                while (runningTests && !console.updateComplete()) {
                                    delay(10000)
                                }
                                if (runningTests) {
                                    kotlin.runCatching {
                                        console.disconnect()
                                        consoleChannel.send(consoleIP)
                                        consolesUpdated.add(consoleIP)
                                    }
                                }
                            }
                        }
                    }
                    println("Finished updating console $consoleIP")
                }
            }
            runBlocking(Dispatchers.IO) {
                tests.forEachIndexed { index, test ->
                    if (test.name.isEmpty()) {
                        test.name = "${test.pkg}.${test.clazz}.${test.function}"
                    }
                    val console = consoleChannel.receive()
                    launch {
                        kotlin.runCatching {
                            withTimeout(20 * 60000) {
                                println("Running test ${index + 1}")
                                ConsoleTest(
                                    RunTestArgs(args, index, testCycle, console),
                                    test
                                ).runTest(
                                    testCycle,
                                    true
                                )
                                delay(1000)
                                if (runningTests) {
                                    consoleChannel.send(console)
                                }
                            }
                        }
                        println("Done Running test ${index + 1}")
                    }
                }
                runningTests = false
            }
            println("Done with tests, close the channel")
            consoleChannel.close()
            SystemCommandHelper().execute("adb disconnect")
            for(console in consolesUpdated) {
                val c = Console(console)
                if (c.connect()) {
                    SystemCommandHelper().execute("sh updateToLatest.sh")
                    c.disconnect()
                }
            }
        }
    }

    private fun runTestsOnJenkins(args: RunTestsOnJenkinsArgs) {
        val testCycle = createTestCycle(TestCycleArgs(args))
        JenkinsTestManager(args, consoles, testCycle, tests, testVersion).runTests()
    }

    private fun getTests(args: GetTestArgs) {
        val tests: MutableList<InstrumentationTest> = ArrayList()
        for(folder in args.srcFolders) {
            tests.addAll(TestFinder().findTests(File(folder)))
        }
        val zephyr = Zephyr(args.zephyrAuth, args.projectKey)
        val testFolder = TestFolder(args.testCaseFolderId)
        val testCases = zephyr.getTests(testFolder)
        for(test in tests) {
            for(testCase in testCases) {
                if(testCase.matches(test)) {
                    zephyr.updateTest(testCase, test)
                    break
                }
            }
            if(test.zephyrTestCase.isEmpty()) {
                zephyr.createTest(TestCase(folderId = args.testCaseFolderId, name = test.name, description = test.description, uuid = test.uuid), test)
            }
        }
        File(testsFile).writeText(Gson().toJson(tests))
    }

}