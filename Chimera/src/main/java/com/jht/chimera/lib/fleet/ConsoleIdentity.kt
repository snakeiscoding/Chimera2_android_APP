package com.jht.chimera.lib.fleet

import com.jht.chimera.lib.common.getCpuSerialNumber
import com.jht.chimera.lib.common.dapi.DapiUtil

data class ConsoleIdentity(
    val consoleUuid: String,
    val cpuId: String
) {
    companion object {
        fun fromDevice(): ConsoleIdentity {
            val consoleUuid = DapiUtil.getMachineId() ?: ""
            val cpuId = getCpuSerialNumber() ?: ""
            return ConsoleIdentity(consoleUuid = consoleUuid, cpuId = cpuId)
        }
    }
}
