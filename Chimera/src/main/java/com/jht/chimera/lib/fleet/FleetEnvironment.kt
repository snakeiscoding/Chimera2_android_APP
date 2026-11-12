package com.jht.chimera.lib.fleet

enum class FleetEnvironment(
    val deployment: Int,
    val iotEndpoint: String,
    val templateName: String,
    val apiBaseUrl: String
) {
    DEV(
        deployment = 0,
        iotEndpoint = "dev-fleet-iot.jhtdev.services",
        templateName = "fleet-provisioning-dev",
        apiBaseUrl = "https://dev-fleet.jhtdev.services"
    ),
    QA(
        deployment = 1,
        iotEndpoint = "qa-fleet-iot.jhtdev.services",
        templateName = "fleet-provisioning-qa",
        apiBaseUrl = "https://qa-fleet.jhtdev.services"
    ),
    STAGING(
        deployment = 2,
        iotEndpoint = "staging-fleet-iot.jhtdev.services",
        templateName = "fleet-provisioning-staging",
        apiBaseUrl = "https://staging-fleet.jhtdev.services"
    ),
    PROD(
        deployment = 3,
        iotEndpoint = "fleet-iot.jht.services",
        templateName = "fleet-provisioning-prod",
        apiBaseUrl = "https://fleet.jht.services"
    );

    companion object {
        fun fromDeployment(deployment: Int): FleetEnvironment =
            entries.first { it.deployment == deployment }
    }
}
