package com.jht.test.manager

import com.jht.test.testfinder.InstrumentationTest
import com.jht.test.zephyr.TestExecution
import com.jht.test.zephyr.Zephyr
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Creates a test case object for the test report.
 */
class TestCaseResults internal constructor(testCase: JSONObject, private val name: String) {
    private val className: String = testCase.optString("classname", "")
    private val executionTime: Long = testCase.optLong("time", 0)
    private val failureObj: JSONObject? = testCase.optJSONObject("failure")
    private val failureMessage: String get() = failureObj?.optString("message", "") ?: ""
    private val exception: String get() = failureObj?.optString("exception", "") ?: ""
    private val trace: String get() = failureObj?.optString("trace", "") ?: ""
    private val failure get() = failureObj != null
    private val testName: String
        get() = "$className.$name"

    fun addZephyrPostData(postData: JSONObject) {
        postData.put("statusName", if (failure) "Fail" else "Pass")
        postData.put("executionTime", executionTime)
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH)
        postData.put("actualEndDate", format.format(Date()))
        if (failure) {
            postData.put("comment", """
     $failureMessage
     
     $exception
     
     $trace
     """.trimIndent()
            )
        }
    }

    fun sendResults(zephyr: Zephyr, testCycle: String, test: InstrumentationTest ) {
        zephyr.executeTest(TestExecution(testCycle, test, this))
    }

}