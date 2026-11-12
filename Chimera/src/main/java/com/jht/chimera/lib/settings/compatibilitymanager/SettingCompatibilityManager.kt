package com.jht.chimera.lib.settings.compatibilitymanager

import com.jht.chimera.lib.settings.compatibilitymanager.model.CompatibilityPolicy


/**
 * Manages compatibility policies for console settings.
 *
 * This manager allows you to provide and evaluate policies that determine
 * what settings are compatible from one console to another.
 */
open class SettingCompatibilityManager(policies: Collection<CompatibilityPolicy<*>>) {
    private val compatibilityPolicies = mutableMapOf<String, MutableList<CompatibilityPolicy<*>>>()

    init {
        // Initialize the compatibility policies map with the provided policies.
        policies.forEach { compatibilityPolicies.getOrPut(it.key) { mutableListOf() }.add(it) }
    }

    /**
     * Determine if a setting is compatible based on the provided compatibility policies.
     *
     * @param key the unique key identifying the setting.
     * @param value the value of the setting.
     * @param sourceVersion the setting's version of the console exporting this setting.
     * @param targetVersion the setting's version of the console importing this setting.
     * @return `true` if the setting is compatible, `false` otherwise.
     */
    fun isSettingCompatible(
        key: String,
        value: Any?,
        sourceVersion: Int,
        targetVersion: Int
    ): Boolean = getApplicablePolicies(key, value, sourceVersion, targetVersion).let { policies ->
        policies.isEmpty() || policies.none { !it.compatible}
    }

    /*
     * Helper method to retrieve a list of applicable policies for a given setting.
     */
    private fun getApplicablePolicies(
        settingKey: String,
        value: Any?,
        fromVersion: Int,
        toVersion: Int
    ): List<CompatibilityPolicy<*>> = compatibilityPolicies[settingKey]?.filter {
        policy -> policy.appliesTo(value, fromVersion, toVersion)
    } ?: emptyList()
}