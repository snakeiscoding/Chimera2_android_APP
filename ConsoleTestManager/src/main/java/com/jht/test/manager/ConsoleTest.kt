package com.jht.test.manager

import com.jht.test.testfinder.InstrumentationTest
import com.jht.test.util.SystemCommandHelper
import com.jht.test.util.readFileToString
import com.jht.test.zephyr.TestExecution
import com.jht.test.zephyr.Zephyr
import java.io.File

class ConsoleTest(private val args: RunTestArgs, private val test: InstrumentationTest) {
    private val zephyr: Zephyr = Zephyr(args.zephyrAuth, args.projectKey)
    private val testCommand: String = "class ${test.pkg}.${test.clazz}#${test.function}"
    private val testNameNoSpace by lazy { test.name.replace(" ", "_") }

    fun runTest(testCycleKey: String, parallel: Boolean = false): Boolean {
        val console = Console(args.console, parallel = parallel)
        val adbCommand = if(parallel) { "adb -s ${args.console}" } else { "adb" }
        return try {
            if(!parallel) {
                SystemCommandHelper().execute("$adbCommand root", 5000)
            }
            val resultsFileNameXML = "${testNameNoSpace}_Results.xml"
            val resultsFileNameJSON = "${testNameNoSpace}_Results.json"
            //SystemCommandHelper().execute("$adbCommand root")
            if(console.connect()) {
                // Note, do not care if the following fail.  Especially the command to show Android launcher as that fails
                // if we are already on the launcher.
                SystemCommandHelper().execute(
                    "$adbCommand shell setprop jht_test_suite_name \"${testNameNoSpace}\"",
                    5000
                )
                SystemCommandHelper().execute(
                    "$adbCommand shell 'if [[ \$(getprop ro.build.product) = \"leo_8mq\" ]] ; then am start -n com.android.launcher3/com.android.launcher3.uioverrides.QuickstepLauncher; else am start -n com.android.launcher3/com.android.launcher3.Launcher; fi'",
                    5000
                )

                SystemCommandHelper().execute("$adbCommand logcat -c", 5000)
                val startTime = System.currentTimeMillis()
                if (SystemCommandHelper().execute("$adbCommand shell am instrument -w -e $testCommand ${test.runner}", 15 * 60000).result == 0) {
                    SystemCommandHelper().execute("$adbCommand shell sync", 5000)
                    SystemCommandHelper().execute(
                        "$adbCommand pull /data/data/com.jht.chimera/files/$resultsFileNameXML ${
                            File(
                                resultsFileNameXML
                            )
                        }", 5000
                    )
                    SystemCommandHelper().execute(
                        "$adbCommand pull /data/data/com.jht.chimera/files/$resultsFileNameJSON ${
                            File(
                                resultsFileNameJSON
                            )
                        }", 5000
                    )
                    TestSuiteResults(File(resultsFileNameJSON).readFileToString()).sendResults(
                        zephyr,
                        testCycleKey,
                        test
                    )
                    true
                } else {
                    val executionTime = System.currentTimeMillis() - startTime
                    val result = SystemCommandHelper().execute(
                        "$adbCommand logcat -d > out_${args.console}.txt",
                        5000
                    )
                    val errorLog =
                        if (result.result == 0) File("out_${args.console}.txt").readText() else result.error
                    zephyr.executeTest(
                        TestExecution(
                            testCycleKey,
                            test,
                            null,
                            "General Test Error\n$errorLog",
                            executionTime
                        )
                    )
                    false
                }
            }
            else {
                zephyr.executeTest(
                    TestExecution(
                        testCycleKey,
                        test,
                        null,
                        "Unable to connect to console",
                        0
                    )
                )
                false
            }
        } catch (ex: Exception) {
            false
        } finally {
            console.disconnect()
        }
    }

}