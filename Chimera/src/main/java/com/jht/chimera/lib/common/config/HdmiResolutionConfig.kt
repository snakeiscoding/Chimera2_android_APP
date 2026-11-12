package com.jht.chimera.lib.common.config

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.TVHDMIOutputResolution

class HdmiResolutionConfig(
    val defaultResolution: TVHDMIOutputResolution,
    vararg additionalResolutions: TVHDMIOutputResolution
) {
    val supportedResolutions: List<TVHDMIOutputResolution> = listOf(defaultResolution) + additionalResolutions

    fun fromName(name: String?): TVHDMIOutputResolution {
        if (name == null) return defaultResolution
        return try {
            val resolution = TVHDMIOutputResolution.valueOf(name)
            if (supportedResolutions.contains(resolution)) resolution else defaultResolution
        } catch (e: IllegalArgumentException) {
            defaultResolution
        }
    }

    fun isValidResolution(name: String?): Boolean {
        return fromName(name).name == name
    }
}
