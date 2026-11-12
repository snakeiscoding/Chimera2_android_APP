package com.jht.progression.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ExtensionsTest {
    @Test
    fun `zeroIndexed shifts the range down by one and excludes the original upper bound`() {
        assertEquals(3..20, (4..21).zeroIndexed())
    }

    @Test
    fun `zeroIndexed handles a single element range`() {
        assertEquals(0..0, (1..1).zeroIndexed())
    }
}
