package com.jht.chimera.lib.settings.compatibilitymanager

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.TVSource
import com.jht.chimera.lib.settings.compatibilitymanager.model.CompatibilityPolicy
import com.jht.chimera.lib.settings.compatibilitymanager.model.ValueConstraint
import com.jht.chimera.lib.settings.compatibilitymanager.model.VersionConstraint

/**
 * Manages compatibility policies specific to Site Survey.
 */
object SiteSurveyCompatibilityManager: SettingCompatibilityManager(
    policies = listOf(
        // Do not export KM6 TV source setting to consoles before settings version 2.
        // KM6 is only a valid TV source setting on consoles with settings version 2 and above.
        CompatibilityPolicy(
            key =  SettingKeys.TV.DATA_TV_SOURCE_STRING.toString(),
            value = ValueConstraint.of(TVSource.KM6.toString()),
            sourceVersion = VersionConstraint.any(),
            targetVersion = VersionConstraint.before(2),
            compatible = false
        ),
        // Do not export US9/KM7/BR3/JP1 TV source setting to consoles before settings version 3.
        // US9/KM7/BR3/JP1 is only a valid TV source setting on consoles with settings version 3 and above.
        CompatibilityPolicy(
            key =  SettingKeys.TV.DATA_TV_SOURCE_STRING.toString(),
            value = ValueConstraint.of(
                TVSource.US9.toString(),
                TVSource.KM7.toString(),
                TVSource.BR3.toString(),
                TVSource.JP1.toString()
            ),
            sourceVersion = VersionConstraint.any(),
            targetVersion = VersionConstraint.before(3),
            compatible = false
        ),
    )
)