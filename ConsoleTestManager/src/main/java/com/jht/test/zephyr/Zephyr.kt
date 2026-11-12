package com.jht.test.zephyr

import com.jht.test.testfinder.InstrumentationTest
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * This class works with the Zephyr API.
 */
class Zephyr
/**
 * Initialize zephyr getting the authorization.
 */(private val authorization: String, private val projectKey: String) {
    fun executeTest(testExecution: TestExecution) {
        createZephyrObject(path = "testexecutions", postData = createPostData(testExecution) )
    }

    fun createTestCycle(testCycle: TestCycle, gitCommit: String) {
        testCycle.key = createZephyrObject(path = "testcycles", postData = createPostData(testCycle) )?.
            optString("key", "") ?: ""

        if(gitCommit.isNotEmpty()) {
            val postData = JSONObject()
            postData.put("description", "Commit")
            postData.put(
                "url",
                "https://bitbucket.org/johnsonhealthtech/chimera2/commits/$gitCommit"
            )
            executeZephyrCommand(
                path = "testcycles/${testCycle.key}/links/weblinks",
                postData = postData,
                httpCommand = "POST"
            )
        }

    }

    fun createTest(testCase: TestCase, test:InstrumentationTest) {
        test.zephyrTestCase = createZephyrObject(path = "testcases", postData = createPostData(testCase) )?.
            optString("key", "") ?: ""
        syncTestRequirements(testCase, test)
    }

    fun updateTest(testCase: TestCase, test: InstrumentationTest) {
        testCase.update(test)
        updateZephyrObject(path = "testcases/${test.zephyrTestCase}", postData = createPostData(testCase) )
        syncTestRequirements(testCase, test)
    }

    private fun syncTestRequirements(testCase: TestCase, test: InstrumentationTest) {
        if(test.requirement.isNotEmpty()) {
            if(!testCase.hasRequirement(test.requirement)) {
                val postData = JSONObject()
                postData.put("description", "Requirement")
                postData.put("url", test.requirement)
                executeZephyrCommand(
                    path = "testcases/${test.zephyrTestCase}/links/weblinks",
                    postData = postData,
                    httpCommand = "POST"
                )
            }
        }
    }


    fun getTests(folder: TestFolder): List<TestCase> {
        val queryParams = createQueryKeys(folder)
        var startAt = 0
        val tests: MutableList<TestCase> = ArrayList()
        while (true) {
            queryParams["startAt"] = "" + startAt
            val result = executeZephyrCommand(path = "testcases", queryParams = queryParams)
                ?: return tests
            println("Got tests results")
            val testCases = result.optJSONArray("values") ?: return tests
            for (i in 0 until testCases.length()) {
                val testCase = testCases.optJSONObject(i) ?: return tests
                val links = testCase.optJSONObject("links")
                val webLinks = links.optJSONArray("webLinks")
                val webLinkList = mutableListOf<TestWebLink>()
                for(j in 0 until webLinks.length()) {
                    val link = webLinks.optJSONObject(j)
                    webLinkList.add(TestWebLink(link.optInt("id", 0), link.optString("url", ""), link.optString("description", "")))
                }
                tests.add(TestCase(testCase))
                startAt++
            }
            if (result.optBoolean("isLast", false) || startAt >= result.optInt("total", 0)) {
                return tests
            }
        }
    }

    private fun createZephyrObject (
        path: String,
        queryParams: HashMap<String?, String?>? = null,
        postData: JSONObject? = null,
        httpCommand: String = "POST",
        expectedResponseCode: Int = HttpURLConnection.HTTP_CREATED
    ): JSONObject? {
        return executeZephyrCommand(path, queryParams, postData, httpCommand, expectedResponseCode)
    }

    private fun updateZephyrObject (
        path: String,
        queryParams: HashMap<String?, String?>? = null,
        postData: JSONObject? = null,
        httpCommand: String = "PUT",
        expectedResponseCode: Int = HttpURLConnection.HTTP_OK
    ) {
        executeZephyrCommand(path, queryParams, postData, httpCommand, expectedResponseCode)
    }

    private fun executeZephyrCommand(
        path: String,
        queryParams: HashMap<String?, String?>? = null,
        postData: JSONObject? = null,
        httpCommand: String = "GET",
        expectedResponseCode: Int = HttpURLConnection.HTTP_OK
    ): JSONObject? {
        val connection: HttpURLConnection
        try {
            val url = StringBuilder()
            url.append("https://api.zephyrscale.smartbear.com/v2/").append(path)
            if (queryParams != null) {
                url.append("?")
                var firstValue = true
                for ((key, value) in queryParams) {
                    if (!firstValue) {
                        url.append("&")
                    }
                    url.append(key).append("=").append(value)
                    firstValue = false
                }
            }
            connection = URL(url.toString()).openConnection() as HttpURLConnection
            connection.readTimeout = 100000
            connection.connectTimeout = 15000
            connection.doInput = true
            connection.doOutput = postData != null
            connection.addRequestProperty("Authorization", "Bearer $authorization")
            if (postData != null) {
                connection.addRequestProperty("Content-Length", "${postData.toString().toByteArray().size}" )
                connection.addRequestProperty("Content-Type", "application/json")
            }
            connection.requestMethod = httpCommand
            connection.connect()
            println("$httpCommand $url")
            println(authorization)
            if (postData != null) {

                println("Post Data: $postData")
                connection.outputStream.write(
                    postData.toString().toByteArray(StandardCharsets.UTF_8)
                )
            }
            val response = connection.responseCode
            println("$response vs $expectedResponseCode")

            val responseStr = if(response >= 400) connection.errorStream.bufferedReader().readText() else connection.inputStream.bufferedReader().readText()
            println(responseStr)
            if (response == expectedResponseCode) {
                return try {
                    JSONObject(responseStr)
                } catch(ex: java.lang.Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun createPostData(zephyrObject: ZephyrObject): JSONObject {
        val postData = JSONObject()
        postData.put("projectKey", projectKey)
        zephyrObject.addPostData(postData)
        return postData
    }

    private fun createQueryKeys(zephyrObject: ZephyrObject): HashMap<String?, String?> {
        val query = HashMap<String?, String?>()
        query["projectKey"] = projectKey
        zephyrObject.addQueryParams(query)
        return query
    }
}