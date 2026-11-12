package com.jht.firsttimesetup

import com.jht.androidcommonalgorithms.type.JHTCommonTypes
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.datamatrixlib.DataMatrix
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Ktor client used for FTSU calls
 */
class FtsuClient @Inject constructor() : AutoCloseable {

    private val dataMatrix = DataMatrix()

    private var httpClient: HttpClient
    private lateinit var host: String
    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    init {
        dataMatrix.createIntAdapter(
            SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT,
            { environmentType ->
                val environment = JHTCommonTypes.DAPIEnvironment.entries[environmentType]
                host = when (environment) {
                    JHTCommonTypes.DAPIEnvironment.production -> "facility.jht.services"
                    JHTCommonTypes.DAPIEnvironment.staging -> "staging-facility.jhtdev.services"
                    JHTCommonTypes.DAPIEnvironment.qa -> "qa-facility.jhtdev.services"
                    else -> "dev-facility.jhtdev.services"
                }
            },
            true
        )
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

    fun getHost(): String {
        return host
    }

    override fun close() {
        dataMatrix.finish()
        httpClient.close()
    }

}