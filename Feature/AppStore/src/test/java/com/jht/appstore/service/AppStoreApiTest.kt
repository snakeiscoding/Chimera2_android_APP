package com.jht.appstore.service

import com.jht.androidcommonalgorithms.type.JHTCommonTypes
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.json.JSONArray
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException

/**
 * Tests for AppStore config download retry and exception handling.
 * Verifies that network failures are handled gracefully without crashing.
 */
class AppStoreApiTest {

    val consoleTypes: List<String> = listOf("chimera1", "chimera2", "projectone", "lowcostgui", "vortexlite", "vortex", "vortex4K", "unknown")
    val TEST_CLUB_ID = 1963

    @Test
    fun getAppStoreList_SocketTimeout_Thrown() = runTest {
            val mockAppStoreClient: AppStoreClient = mockk()
            every { mockAppStoreClient.getClient() } throws SocketTimeoutException("Test Socket Timeout")
            val testAPI = AppStoreApi(mockAppStoreClient)

            val result = testAPI.getAppStoreList(0, "testing", JHTCommonTypes.DAPIEnvironment.production)
            assertEquals(null, result)
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Chimera1_Production() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[0], JHTCommonTypes.DAPIEnvironment.production)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Chimera1_QA() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[0], JHTCommonTypes.DAPIEnvironment.qa)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Chimera1_Staging() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[0], JHTCommonTypes.DAPIEnvironment.staging)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Chimera1_Dev() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[0], JHTCommonTypes.DAPIEnvironment.dev)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Chimera2_Production() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[1], JHTCommonTypes.DAPIEnvironment.production)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Chimera2_QA() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[1], JHTCommonTypes.DAPIEnvironment.qa)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Chimera2_Staging() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[1], JHTCommonTypes.DAPIEnvironment.staging)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Chimera2_Dev() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[1], JHTCommonTypes.DAPIEnvironment.dev)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_ProjectOne_Production() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[2], JHTCommonTypes.DAPIEnvironment.production)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_ProjectOne_QA() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[2], JHTCommonTypes.DAPIEnvironment.qa)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_ProjectOne_Staging() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[2], JHTCommonTypes.DAPIEnvironment.staging)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_ProjectOne_Dev() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[2], JHTCommonTypes.DAPIEnvironment.dev)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_LowCostGUI_Production() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[3], JHTCommonTypes.DAPIEnvironment.production)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_LowCostGUI_QA() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[3], JHTCommonTypes.DAPIEnvironment.qa)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_LowCostGUI_Staging() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[3], JHTCommonTypes.DAPIEnvironment.staging)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_LowCostGUI_Dev() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[3], JHTCommonTypes.DAPIEnvironment.dev)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_VortexLite_Production() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[4], JHTCommonTypes.DAPIEnvironment.production)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_VortexLite_QA() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[4], JHTCommonTypes.DAPIEnvironment.qa)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_VortexLite_Staging() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[4], JHTCommonTypes.DAPIEnvironment.staging)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_VortexLite_Dev() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[4], JHTCommonTypes.DAPIEnvironment.dev)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Vortex_Production() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[5], JHTCommonTypes.DAPIEnvironment.production)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Vortex_QA() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[5], JHTCommonTypes.DAPIEnvironment.qa)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Vortex_Staging() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[5], JHTCommonTypes.DAPIEnvironment.staging)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Vortex_Dev() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[5], JHTCommonTypes.DAPIEnvironment.dev)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Vortex4K_Production() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[6], JHTCommonTypes.DAPIEnvironment.production)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Vortex4K_QA() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[6], JHTCommonTypes.DAPIEnvironment.qa)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Vortex4K_Staging() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[6], JHTCommonTypes.DAPIEnvironment.staging)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Vortex4K_Dev() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[6], JHTCommonTypes.DAPIEnvironment.dev)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Unknown_Production() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[7], JHTCommonTypes.DAPIEnvironment.production)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Unknown_QA() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[7], JHTCommonTypes.DAPIEnvironment.qa)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Unknown_Staging() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[7], JHTCommonTypes.DAPIEnvironment.staging)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }

    @Test
    fun getAppStoreList_Returns_Valid_Json_Unknown_Dev() = runTest {
        val testAPI = AppStoreApi(AppStoreClient())

        val resultString = testAPI.getAppStoreList(TEST_CLUB_ID, consoleTypes[7], JHTCommonTypes.DAPIEnvironment.dev)
        val json = Json {
            isLenient = true
        }
        if(resultString != null) {
            val resultJson = json.decodeFromString<JsonArray>(resultString)
            assertTrue(true) //result was not null and was successfully parsed into a JSONArray
        }
        else {
            assertTrue(false) //just make sure it fails if API call returned null
        }
    }
}
