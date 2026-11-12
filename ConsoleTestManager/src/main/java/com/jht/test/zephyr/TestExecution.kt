package com.jht.test.zephyr

import com.jht.test.manager.TestCaseResults
import com.jht.test.testfinder.InstrumentationTest
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class TestExecution(
    private val cycle: String,
    private val test: InstrumentationTest,
    private val results: TestCaseResults? = null,
    private val errorMessage: String = "",
    private val executionTime: Long = 0
) : ZephyrObject {
    override fun addPostData(postData: JSONObject) {
        postData.put("testCaseKey", test.zephyrTestCase)
        postData.put("testCycleKey", cycle)
        results?.addZephyrPostData(postData)
        if(errorMessage.isNotEmpty()) {
            postData.put("statusName", "Fail")
            postData.put("executionTime", executionTime)
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH)
            postData.put("actualEndDate", format.format(Date()))
            postData.put("comment", errorMessage)
        }
    }

    override fun addQueryParams(params: HashMap<String?, String?>) {}
}