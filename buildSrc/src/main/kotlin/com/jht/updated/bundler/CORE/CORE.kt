package com.jht.updated.bundler.CORE

import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import javax.net.ssl.HttpsURLConnection
import java.util.Base64

const val CORE_BASE_URL = "https://config.jht.services";
const val CORE_DEPLOYS_URL = "$CORE_BASE_URL/deploy/";
const val CORE_LOGIN_URL = "https://accounts.johnsonfit.com/oauth/token";
const val CORE_COLLECTION_URL = CORE_BASE_URL + "/collection/";
const val CORE_SOFTWARE_URL = CORE_BASE_URL + "/software/";

/**
 * Class used to handle calls to the CORE API
 * Handles managing Authentication, GET, PUT, and POST requests returning the string responses for each call
 */
class CORE(private val coreUser: String, private val corePassword: String) {
    private var authToken: String? = null;
    class COREException(message: String) : Exception(message) {};
    class LoginException(message: String) : Exception(message) {};


    /**
     * Deploys each software item provided and creates a software collection to deploy all packages with the provided COREAssociations
     */
    fun deploy(softwareConfigurationClass: String, version: String, associations: List<COREAssociation>, softwareItem: CORESoftwareItem) {
        try {
            login()
            if(CORESoftwareItem.upload(this, softwareItem)) {
                CORECollection(softwareConfigurationClass, version).deploy(
                    this,
                    softwareItem,
                    associations
                )
            }
            else {
                println("CORE Software Item failed to upload. Software Item may already Exist")
                throw Exception("CORE SoftwareItem failed to upload")
            }
        }
        catch (ex: Exception) {
            println(ex.message)
            throw ex //want this to be up to calling class if it fails or not
        }
    }

    /**
     * Wrapper for POST API call. Returns string response of call
     * @param api full string URL for POST call
     * @param postData string of data uploaded with POST call
     * @param zip if upload data is zipped or not
     */
    fun postCOREAPI(api: String, postData: String, zip: Boolean) : String {
        return callCOREAPI(api,"POST", postData, zip)
    }

    /**
     * Wrapper for GET API call. Returns string response of call\
     * @param api full string of URL GET call is to
     */
    fun getCOREAPI(api: String, isZipped: Boolean = true) : String {
        return callCOREAPI(api,"GET", "", isZipped)
    }

    /**
     * Base implementation of web call for different API calls
     */
    private fun callCOREAPI(api: String, method: String, postData: String, zip: Boolean, extraHeaders: Map<String, String>? = null) : String {
        val coreURL = URL(api)
        val urlConnection: HttpsURLConnection = coreURL.openConnection() as HttpsURLConnection
        urlConnection.requestMethod = method
        //Helpful debug print
        println("calling: $api, with method: $method, zip: $zip, postData: $postData")

        urlConnection.setRequestProperty("Accept", "application/json, text/plain, */*")
        urlConnection.setRequestProperty("Connection", "keep-alive")
        if (zip) {
            urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate, br")
            urlConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
        }
        if(postData.isNotEmpty()) {
            urlConnection.setRequestProperty("Content-Length", postData.length.toString())
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        if(this.loggedIn()) {
            urlConnection.setRequestProperty("Authorization", "Bearer $authToken")
        }
        else {
            println("CORE call without authToken, if not the log in request may cause errors")
        }

        //Add extraHeaders
        if(extraHeaders != null && !extraHeaders.isEmpty()) {
            extraHeaders.forEach { key, value ->
                urlConnection.setRequestProperty(key, value)
            }
        }
        if(postData.isNotEmpty()) {
            urlConnection.setDoOutput(true)
            try {
                urlConnection.outputStream.use { os ->
                    println("Writing out: $postData")
                    os.write(postData.toByteArray(Charsets.UTF_8))
                }
            }
            catch (ex: Exception) {
                println("Could not open an output stream")
                println(ex)
            }
            urlConnection.outputStream.close()
        }
        else {
            urlConnection.doOutput = false
        }

        val responseCode = urlConnection.responseCode
        if(responseCode != 200 && responseCode != 201) {
            println("Unexpected response: " + urlConnection.responseMessage)
            throw COREException("Unexpected response $responseCode")
        }
        var response: String = ""
        if (zip) {
            try {
                response = GZIPInputStream(urlConnection.inputStream).bufferedReader().use { it.readText() }
            }
            catch (ignore: Exception) {
                response = urlConnection.inputStream.bufferedReader().use { it.readText() }
            }
        }
        else {
            response = urlConnection.inputStream.bufferedReader().use { it.readText() }
        }
        return response
    }


    /**
     * Method to handle log in to CORE API
     * Stores the returned AuthToken provided by CORE
     */
    fun login() {
        var loginStr = coreUser + ':' + corePassword;
        var base64Login = baseEncode64(loginStr);
        var headers : MutableMap<String, String> = HashMap();
        headers["Content-Type"] = "application/x-www-form-urlencoded"
        headers["Authorization"] = "Basic $base64Login"
        val params =  "grant_type=client_credentials&scope=core%3Acollection%3Acreate+core%3Adeploy%3Acreate+core%3Asoftware%3Acreate+core%3Adeploy%3Aread+core%3Acollection%3Aread+core%3Asoftware%3Aread"


        try {
            val response = callCOREAPI(CORE_LOGIN_URL, "POST", params, false, extraHeaders = headers)
            val jsonResponse = JSONObject(response)
            if(jsonResponse.has("access_token")) {
                authToken = jsonResponse.getString("access_token")
            }
            else {
                println("valid sign in call did not have an access_token")
                throw COREException("missing access_token from login request")
            }
        }
        catch (ex: COREException) {
            throw LoginException("Login failed: " + ex.message)
        }
    }

    private fun baseEncode64(str: String) : String {
        return Base64.getEncoder().encodeToString(str.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Check to verify authToken has been generated already and a log in is not needed
     */
    fun loggedIn() : Boolean {
        return authToken != null && authToken!!.isNotEmpty()
    }

    /**
     * Gets Manifest for software version deployed on given DAPI env for the provided club (-1 == global)
     * Valid env strings are [dev,qa,staging,prod]
     */
    fun getReleasedSoftwareManifest(softwareType: String, clubID: Int = -1, env: String = "prod"): String? {
        login()
        val ret = JSONArray(getCOREAPI(CORE_DEPLOYS_URL))
        var deploy: JSONObject? = null
        for(i in 0 until ret.length()) {
            val item = ret.getJSONObject(i)
            if(item.get("environment_$env") == 1 && item.get("club_id") == clubID && item.get("software_configuration_class") == softwareType) {
                deploy = item
                break
            }
        }
        if (deploy != null) {
            return getManifest(deploy.getJSONArray("software").getJSONObject(0).getString("url"))
        }
        return null
    }

    private fun getManifest(url: String): String {
        println("calling $url")
        var ret = ""
        val coreURL = URL(url)
        val urlConnection: HttpsURLConnection = coreURL.openConnection() as HttpsURLConnection
        urlConnection.requestMethod = "GET"

        urlConnection.setRequestProperty("Accept", "application/json, text/plain, */*")
        urlConnection.setRequestProperty("Connection", "keep-alive")


        val responseCode = urlConnection.responseCode
        if(responseCode != 200 && responseCode != 201) {
            println("Unexpected response: " + urlConnection.responseMessage)
            throw COREException("Unexpected response $responseCode")
        }

        ret = urlConnection.inputStream.bufferedReader().use { it.readText() }
        ret.trim()

        println("Received XML: $ret")
        return ret
    }
}