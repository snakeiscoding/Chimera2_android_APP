package com.jht.chimera.lib.settings.compatibilitymanager.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionConstraintTest {
    @Test
    fun `Any version matches all versions`() {
        val anyVersion = VersionConstraint.any()
        assertTrue(anyVersion.appliesTo(-100))
        assertTrue(anyVersion.appliesTo(0))
        assertTrue(anyVersion.appliesTo(100))
        assertTrue(anyVersion.appliesTo(Int.MIN_VALUE))
        assertTrue(anyVersion.appliesTo(Int.MAX_VALUE))
    }

    @Test
    fun `Before version with non-inclusive matching`() {
        val beforeV10 = VersionConstraint.before(version = 10, inclusive = false)
        assertTrue(beforeV10.appliesTo(8))
        assertTrue(beforeV10.appliesTo(9))
        assertFalse(beforeV10.appliesTo(10))
        assertFalse(beforeV10.appliesTo(11))
    }

    @Test
    fun `Before version with inclusive matching`() {
        val beforeV11 = VersionConstraint.before(version = 10, inclusive = true)
        assertTrue(beforeV11.appliesTo(8))
        assertTrue(beforeV11.appliesTo(9))
        assertTrue(beforeV11.appliesTo(10))
        assertFalse(beforeV11.appliesTo(11))
    }

    @Test
    fun `After version with non-inclusive matching`() {
        val afterV10 = VersionConstraint.after(version = 10, inclusive = false)
        assertFalse(afterV10.appliesTo(9))
        assertFalse(afterV10.appliesTo(10))
        assertTrue(afterV10.appliesTo(11))
        assertTrue(afterV10.appliesTo(12))
    }

    @Test
    fun `After version with inclusive matching`() {
        val afterV9 = VersionConstraint.after(version = 10, inclusive = true)
        assertFalse(afterV9.appliesTo(9))
        assertTrue(afterV9.appliesTo(10))
        assertTrue(afterV9.appliesTo(11))
        assertTrue(afterV9.appliesTo(12))
    }

    @Test
    fun `Range version with both bounds`() {
        val rangeV10V20 = VersionConstraint.range(min = 10, max = 20)
        assertFalse(rangeV10V20.appliesTo(9))
        assertTrue(rangeV10V20.appliesTo(10))
        assertTrue(rangeV10V20.appliesTo(15))
        assertTrue(rangeV10V20.appliesTo(20))
        assertFalse(rangeV10V20.appliesTo(21))
    }

    @Test
    fun `Range version with only min bound`() {
        val range = VersionConstraint.range(min = 10)
        assertFalse(range.appliesTo(9))
        assertTrue(range.appliesTo(10))
        assertTrue(range.appliesTo(Int.MAX_VALUE))
    }

    @Test
    fun `Range version with only max bound`() {
        val range = VersionConstraint.range(max = 20)
        assertTrue(range.appliesTo(Int.MIN_VALUE))
        assertTrue(range.appliesTo(19))
        assertTrue(range.appliesTo(20))
        assertFalse(range.appliesTo(21))
    }

    @Test
    fun `Range version with no bounds`() {
        val range = VersionConstraint.range()
        assertTrue(range.appliesTo(Int.MIN_VALUE))
        assertTrue(range.appliesTo(0))
        assertTrue(range.appliesTo(Int.MAX_VALUE))
    }

    @Test
    fun `Versions matching with set of specific versions`() {
        val versions = VersionConstraint.of(1, 3, 5, 7)
        assertFalse(versions.appliesTo(0))
        assertTrue(versions.appliesTo(1))
        assertFalse(versions.appliesTo(2))
        assertTrue(versions.appliesTo(3))
        assertTrue(versions.appliesTo(5))
        assertTrue(versions.appliesTo(7))
        assertFalse(versions.appliesTo(8))
    }

    @Test
    fun `Version matching exact version`() {
        val version = VersionConstraint.of(10)
        assertFalse(version.appliesTo(9))
        assertTrue(version.appliesTo(10))
        assertFalse(version.appliesTo(11))
    }

    @Test
    fun `Version matching with boundary values`() {
        val version = VersionConstraint.of(Int.MAX_VALUE)
        assertFalse(version.appliesTo(Int.MAX_VALUE - 1))
        assertTrue(version.appliesTo(Int.MAX_VALUE))

        val minVersion = VersionConstraint.of(Int.MIN_VALUE)
        assertTrue(minVersion.appliesTo(Int.MIN_VALUE))
        assertFalse(minVersion.appliesTo(Int.MIN_VALUE + 1))
    }
}