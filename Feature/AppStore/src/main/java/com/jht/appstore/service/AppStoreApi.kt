package com.jht.appstore.service

import android.util.Log
import com.jht.androidcommonalgorithms.type.JHTCommonTypes
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Inject

private const val TAG = "AppStore"

/**
 * API client for App Store related calls
 * @param [client] -> http client
 */
class AppStoreApi(
    private val client: AppStoreClient
) {

    /**
     * Get the app store json for club ID, console type, and DAPI environment
     */
    suspend fun getAppStoreList(clubID: Int, consoleType: String, dapiEnvironment: JHTCommonTypes.DAPIEnvironment): String? {
        return try {
            client.use {
                return client.getClient().get {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = client.getHost(dapiEnvironment)
                        path("app-store/$clubID")
                        parameters.append("supportedConsoles", consoleType)
                        headers {
                            append(HttpHeaders.Accept, "application/json")
                        }
                    }
                }.body()
            }
        } catch (e: Exception) {
            Log.e(TAG, "get AppStore list exception ", e)
            null
        }
    }
}

/**
 * Ktor client used for App Store calls
 */
class AppStoreClient @Inject constructor() : AutoCloseable {
    private var httpClient: HttpClient
    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    init {
        httpClient = HttpClient(Android) {
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    fun getClient(): HttpClient {
        //must be reinitialized for every call as Ktor has an issue with reoccurring call
        //https://youtrack.jetbrains.com/issue/KTOR-2711
        httpClient = HttpClient(Android) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        return httpClient
    }

    fun getHost(dapiEnvironment: JHTCommonTypes.DAPIEnvironment): String {
        return when (dapiEnvironment) {
            JHTCommonTypes.DAPIEnvironment.production -> "config.jht.services"
            JHTCommonTypes.DAPIEnvironment.staging -> "staging-config.jhtdev.services"
            JHTCommonTypes.DAPIEnvironment.qa -> "qa-config.jhtdev.services"
            else -> "dev-config.jhtdev.services"
        }
    }

    override fun close() {
        httpClient.close()
    }

}
