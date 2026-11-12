package com.jht.test.manager

import com.jht.test.testfinder.InstrumentationTest
import com.jht.test.zephyr.Zephyr
import org.json.JSONException
import org.json.JSONObject

/**
 * The test suite for the test report.  Used to generate and manage the overall test report.
 */
class TestSuiteResults internal constructor(json: String) {
    private val testCaseResults: MutableList<TestCaseResults> = ArrayList() // Map to all the tests.
    fun sendResults(zephyr: Zephyr, testCycle: String, test: InstrumentationTest) {
        for (result in testCaseResults) {
            result.sendResults(zephyr, testCycle, test)
        }
    }

    /**
     * Create the test suite
     */
    init {
        try {
            if(json.isNotEmpty()) {
                val results = JSONObject(json)
                val caseArray = results.optJSONArray("testCases")
                if (caseArray != null) {
                    for (i in 0 until caseArray.length()) {
                        testCaseResults.add(
                            TestCaseResults(
                                caseArray.optJSONObject(i),
                                results.optString("name", "")
                            )
                        )
                    }
                }
            }
        } catch(ignored: JSONException) {
        }
    }
}