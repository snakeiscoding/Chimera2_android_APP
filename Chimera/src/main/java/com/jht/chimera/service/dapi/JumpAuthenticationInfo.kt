package com.jht.chimera.service.dapi

data class JumpAuthenticationInfo (
    val environmentId : Int,
    val machineId: String,
    val facilityId: Int
)

data class UserAuthenticationInfo (
    val machineType : String = "",
    val hasSeca: Boolean = false,
    val optionalSettings: List<String> = listOf(),
)