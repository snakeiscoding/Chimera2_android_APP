package com.jht.chimera.lib.fleet

import android.util.Log
import software.amazon.awssdk.crt.mqtt.MqttClientConnection
import software.amazon.awssdk.crt.mqtt.MqttClientConnectionEvents
import software.amazon.awssdk.crt.mqtt.QualityOfService
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder
import software.amazon.awssdk.iot.iotidentity.IotIdentityClient
import software.amazon.awssdk.iot.iotidentity.model.CreateKeysAndCertificateRequest
import software.amazon.awssdk.iot.iotidentity.model.CreateKeysAndCertificateSubscriptionRequest
import software.amazon.awssdk.iot.iotidentity.model.RegisterThingRequest
import software.amazon.awssdk.iot.iotidentity.model.RegisterThingSubscriptionRequest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class IoTProvisioningService(
    private val endpoint: String,
    private val claimCertPem: String,
    private val claimKeyPem: String,
    private val templateName: String,
    private val timeoutSeconds: Long = 30
) {
    companion object {
        private const val TAG = "IoTProvisioningService"
    }

    fun provision(consoleUuid: String, cpuId: String): FleetProvisioningResult {
        var connection: MqttClientConnection? = null
        try {
            connection = buildConnection()
            connection.connect().get(timeoutSeconds, TimeUnit.SECONDS)

            val identityClient = IotIdentityClient(connection)

            val keysResult = createKeysAndCertificate(identityClient)

            val thingName = registerThing(
                identityClient,
                keysResult.certificateOwnershipToken,
                consoleUuid,
                cpuId
            )

            connection.disconnect().get(timeoutSeconds, TimeUnit.SECONDS)

            return FleetProvisioningResult.Success(
                ProvisioningIdentity(
                    thingName = thingName,
                    certificatePem = keysResult.certificatePem,
                    privateKey = keysResult.privateKey,
                    certificateId = keysResult.certificateId
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Provisioning failed", e)
            return FleetProvisioningResult.Error(e)
        } finally {
            try { connection?.disconnect() } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
        }
    }

    private fun buildConnection(): MqttClientConnection {
        return AwsIotMqttConnectionBuilder.newMtlsBuilder(claimCertPem, claimKeyPem)
            .withEndpoint(endpoint)
            .withClientId("provisioning-${System.nanoTime()}")
            .withCleanSession(true)
            .withConnectionEventCallbacks(object : MqttClientConnectionEvents {
                override fun onConnectionInterrupted(errorCode: Int) {
                    Log.w(TAG, "Connection interrupted: $errorCode")
                }
                override fun onConnectionResumed(sessionPresent: Boolean) {
                    Log.d(TAG, "Connection resumed, sessionPresent=$sessionPresent")
                }
            })
            .build()
    }

    private data class KeysAndCertResult(
        val certificatePem: String,
        val privateKey: String,
        val certificateId: String,
        val certificateOwnershipToken: String
    )

    private fun createKeysAndCertificate(client: IotIdentityClient): KeysAndCertResult {
        val future = CompletableFuture<KeysAndCertResult>()

        client.SubscribeToCreateKeysAndCertificateAccepted(
            CreateKeysAndCertificateSubscriptionRequest(),
            QualityOfService.AT_LEAST_ONCE,
            Consumer { response ->
                future.complete(
                    KeysAndCertResult(
                        certificatePem = response.certificatePem,
                        privateKey = response.privateKey,
                        certificateId = response.certificateId,
                        certificateOwnershipToken = response.certificateOwnershipToken
                    )
                )
            }
        ).get(timeoutSeconds, TimeUnit.SECONDS)

        client.SubscribeToCreateKeysAndCertificateRejected(
            CreateKeysAndCertificateSubscriptionRequest(),
            QualityOfService.AT_LEAST_ONCE,
            Consumer { error -> future.completeExceptionally(RuntimeException("CreateKeys rejected: ${error.errorMessage}")) }
        ).get(timeoutSeconds, TimeUnit.SECONDS)

        client.PublishCreateKeysAndCertificate(
            CreateKeysAndCertificateRequest(),
            QualityOfService.AT_LEAST_ONCE
        ).get(timeoutSeconds, TimeUnit.SECONDS)

        return future.get(timeoutSeconds, TimeUnit.SECONDS)
    }

    private fun registerThing(
        client: IotIdentityClient,
        ownershipToken: String,
        consoleUuid: String,
        cpuId: String
    ): String {
        val future = CompletableFuture<String>()

        val subscriptionRequest = RegisterThingSubscriptionRequest().apply {
            this.templateName = this@IoTProvisioningService.templateName
        }

        client.SubscribeToRegisterThingAccepted(
            subscriptionRequest,
            QualityOfService.AT_LEAST_ONCE,
            Consumer { response -> future.complete(response.thingName) }
        ).get(timeoutSeconds, TimeUnit.SECONDS)

        client.SubscribeToRegisterThingRejected(
            subscriptionRequest,
            QualityOfService.AT_LEAST_ONCE,
            Consumer { error -> future.completeExceptionally(RuntimeException("RegisterThing rejected: ${error.errorMessage}")) }
        ).get(timeoutSeconds, TimeUnit.SECONDS)

        val request = RegisterThingRequest().apply {
            this.templateName = this@IoTProvisioningService.templateName
            this.certificateOwnershipToken = ownershipToken
            this.parameters = hashMapOf(
                "SerialNumber" to "$consoleUuid:$cpuId"
            )
        }

        client.PublishRegisterThing(
            request,
            QualityOfService.AT_LEAST_ONCE
        ).get(timeoutSeconds, TimeUnit.SECONDS)

        return future.get(timeoutSeconds, TimeUnit.SECONDS)
    }
}
