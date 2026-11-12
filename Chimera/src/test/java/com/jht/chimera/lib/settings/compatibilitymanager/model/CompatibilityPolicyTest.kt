package com.jht.chimera.lib.settings.compatibilitymanager.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompatibilityPolicyTest {
    @Test
    fun `policy applies only when all constraints are met`() {
        val policy = CompatibilityPolicy(
            key = "DATA_TEST_SETTING_STRING",
            value = ValueConstraint.of("value1"),
            sourceVersion = VersionConstraint.of(1),
            targetVersion = VersionConstraint.of(2),
            compatible = true
        )

        // All conditions match
        assertTrue(policy.appliesTo("value1", 1, 2))

        // Value doesn't match
        assertFalse(policy.appliesTo("value2", 1, 2))

        // From version doesn't match
        assertFalse(policy.appliesTo("value1", 3, 2))

        // To version doesn't match
        assertFalse(policy.appliesTo("value1", 1, 3))

        // Nothing matches
        assertFalse(policy.appliesTo("value2", 3, 4))
    }
}