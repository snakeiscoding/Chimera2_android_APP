package com.jht.test.jenkins

import com.jht.test.manager.RunTestsOnJenkinsArgs
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*


class Jenkins (
    private val args: RunTestsOnJenkinsArgs
) {

    fun executeTask(parameters: HashMap<String, String>): Boolean {
        val connection: HttpURLConnection
        try {
            val fullUrl = StringBuilder()
            fullUrl.append(args.jenkinsUrl).append(args.jenkinsJob).append("/buildWithParameters?TOKEN=").append(args.jenkinsAuth)
            for ((key, value) in parameters) {
                fullUrl.append("&").append(key).append("=").append(URLEncoder.encode(value, "utf-8"))
            }
            connection = URL(fullUrl.toString()).openConnection() as HttpURLConnection
            connection.readTimeout = 100000
            connection.connectTimeout = 15000
            connection.doInput = true
            connection.doOutput = false
            connection.requestMethod = "POST"
            val auth: String = args.jenkinsUser + ":" + args.jenkinsPassword
            connection.addRequestProperty("Authorization", "Basic ${Base64.getEncoder().encodeToString(auth.toByteArray())}")

            connection.connect()
            println(fullUrl)

            val response = connection.responseCode
            println("Response code: $response")
            val responseStr = if(response >= 400) connection.errorStream.bufferedReader().readText() else connection.inputStream.bufferedReader().readText()
            println(responseStr)

            return response == HttpURLConnection.HTTP_OK || response == HttpURLConnection.HTTP_CREATED
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}