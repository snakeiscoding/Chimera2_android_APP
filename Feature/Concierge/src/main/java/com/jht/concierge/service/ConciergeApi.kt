package com.jht.concierge.service

import android.content.Context
import com.jht.androidcommonalgorithms.type.JHTCommonTypes
import com.jht.concierge.interfaces.FacilityConfigResponse
import com.jht.concierge.interfaces.FacilityRequestType
import com.jht.oauthlib.auth.BksCredentialProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.json.JSONObject

private fun mapDapiEnv(env: String): JHTCommonTypes.DAPIEnvironment {
    return JHTCommonTypes.DAPIEnvironment.entries.find { it.name.equals(env, true) } ?: JHTCommonTypes.DAPIEnvironment.production
}

/**
 * Class used to make API requests for Concierge
 * @param dapiEnvironment the environment used to make requests
 */
class ConciergeApi(dapiEnvironment: String, context: Context) {
    private val httpClient: HttpClient
    private val baseUrl: String
    private val secret: String

    init {
        val environment = mapDapiEnv(dapiEnvironment)
        httpClient = HttpClient(Android.create()) {
            expectSuccess = true
            install(HttpTimeout)
            install(ContentNegotiation) {
                json(Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
        baseUrl = when (environment) {
            JHTCommonTypes.DAPIEnvironment.production -> "https://concierge.jht.services"
            JHTCommonTypes.DAPIEnvironment.staging -> "https://staging-concierge.jhtdev.services"
            JHTCommonTypes.DAPIEnvironment.qa -> "https://qa-concierge.jhtdev.services"
            else -> "https://dev-concierge.jhtdev.services"
        }
        val provider = BksCredentialProvider(context.applicationContext)
        val secretName = when (environment) {
            JHTCommonTypes.DAPIEnvironment.production -> "CONCIERGE_PRODUCTION_SECRET"
            JHTCommonTypes.DAPIEnvironment.staging -> "CONCIERGE_STAGING_SECRET"
            JHTCommonTypes.DAPIEnvironment.qa -> "CONCIERGE_QA_SECRET"
            else -> "CONCIERGE_DEV_SECRET"
        }
        secret = provider.getString(secretName)
    }

    /**
     * Requests a TOTP token from the server. TOTP tokens change every 15 seconds.
     */
    suspend fun authenticate(): String {
        val response: AuthResponse = httpClient.post("$baseUrl/auth/totp") {
            timeout { requestTimeoutMillis = 12000 }
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(JSONObject().put("mca_secret", secret).toString())
        }.body()
        return response.token
    }

    /**
     * Opens a new Concierge request
     * @param totp a valid TOTP token issued from the server
     * @param machineUID the DAPI UUID of the machine
     * @param facilityId the active Facility / Club ID
     * @param frameSerial the serial number of the frame
     * @param machineModality the modality of the machine (ex: Treadmill)
     * @param facilityRequestTypeId the id of the facility request the user has selected
     */
    suspend fun createConciergeRequest(
        totp: String,
        machineUID: String,
        facilityId: Int,
        frameSerial: String,
        machineModality: String,
        facilityRequestTypeId: String,
    ): ConciergeRequest {
        return httpClient.post("$baseUrl/console/request") {
            timeout { requestTimeoutMillis = 12000 }
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            headers { append("Authorization", totp) }
            setBody(
                JSONObject()
                    .put("machine_uuid", machineUID)
                    .put("facility_id", facilityId)
                    .put("machine_frame_serial_number", frameSerial)
                    .put("machine_modality", machineModality)
                    .put("request_type_id", facilityRequestTypeId.ifEmpty { null }) // null if no associated type per web documentation https://jhtnaee.atlassian.net/wiki/spaces/IA/pages/3798598448/Console-facing+Endpoints
                    .toString()
            )
        }.body()
    }

    /**
     * Get a Concierge request from the server
     * @param totp a valid TOTP token issued from the server
     * @param requestId the ID of the Concierge request
     */
    suspend fun getConciergeRequest(totp: String, requestId: String): ConciergeRequest {
        return httpClient.get("$baseUrl/console/request/$requestId") {
            timeout { requestTimeoutMillis = 12000 }
            accept(ContentType.Application.Json)
            headers { append("Authorization", totp) }
        }.body()
    }

    /**
     * Get a list of Facility requests from the server, these are the options that will display in
     * the concierge dialog for a user to select from.
     * @param totp a valid TOTP token issued from the server
     * @param facilityId the ID of the facility for the request
     */
    suspend fun getFacilityRequestType(totp: String, facilityId: String): List<FacilityRequestType> {
        return httpClient.get("$baseUrl/console/facility_request_type/$facilityId") {
            timeout { requestTimeoutMillis = 12000 }
            accept(ContentType.Application.Json)
            headers { append("Authorization", totp) }
        }.body()
    }

    suspend fun getFacilityConfigRequest(totp: String, facilityId: String): FacilityConfigResponse {
        return httpClient.get("$baseUrl/console/facility_config/$facilityId") {
            timeout { requestTimeoutMillis = 12000 }
            accept(ContentType.Application.Json)
            headers { append("Authorization", totp) }
        }.body()
    }

    /**
     * Closes a concierge request
     * @param totp a valid TOTP token issued from the server
     * @param requestId the ID of the concierge request you wish to close
     * @param machineUID the console's UUID of the concierge request you wish to close
     * @param facilityId the facility / club ID of the concierge request you wish to close
     */
    suspend fun closeConciergeRequest(
        totp: String,
        requestId: String,
        machineUID: String,
        facilityId: Int
    ): ConciergeRequest {
        // PATCH -> PUT, web team's architecture does not work with our java 1.8 PATCH workaround (X-HTTP-Method-Override)
        return httpClient.put("$baseUrl/console/request/$requestId") {
            timeout { requestTimeoutMillis = 12000 }
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            headers { append("Authorization", totp) }
            setBody(JSONObject()
                .put("machine_uuid", machineUID)
                .put("facility_id", facilityId)
                .toString())
        }.body()
    }
}
