package com.jht.chimera.lib.fleet

data class ProvisioningIdentity(
    val thingName: String,
    val certificatePem: String,
    val privateKey: String,
    val certificateId: String
)

sealed class FleetProvisioningResult {
    data class Success(val identity: ProvisioningIdentity) : FleetProvisioningResult()
    data class Error(val cause: Throwable) : FleetProvisioningResult()
}
