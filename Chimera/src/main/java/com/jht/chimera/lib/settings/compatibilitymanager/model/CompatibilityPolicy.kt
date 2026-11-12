package com.jht.chimera.lib.settings.compatibilitymanager.model

/**
 * Represents a compatibility policy for a specific setting.
 *
 * @param T the data type of the setting value constraint.
 * @property key the unique key identifying the setting.
 * @property value a constraint that defines values for which this policy applies.
 * @property sourceVersion a constraint defining the source versions for which this policy applies.
 * @property targetVersion a constraint defining the target versions for which this policy applies.
 * @property compatible indicates whether the policy deems the setting as compatible.
 */
data class CompatibilityPolicy<T>(
    val key: String,
    val value: ValueConstraint<T>,
    val sourceVersion: VersionConstraint,
    val targetVersion: VersionConstraint,
    val compatible: Boolean
) {
    /**
     * Determines if this compatibility policy applies to the provided parameters.
     *
     * @param value the setting value to test against the policy.
     * @param fromVersion the source version to test against the policy.
     * @param toVersion the target version to test against the policy.
     * @return `true` if the policy applies; `false` otherwise.
     */
    fun appliesTo(value: Any?, fromVersion: Int, toVersion: Int): Boolean =
        this.value.appliesTo(value) &&
        this.sourceVersion.appliesTo(fromVersion) &&
        this.targetVersion.appliesTo(toVersion)
}