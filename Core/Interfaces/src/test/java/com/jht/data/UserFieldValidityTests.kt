package com.jht.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserFieldValidityTests {

    @Test
    fun testIsValid_valid() {
        assertTrue(UserFieldValidity.VALID.isValid())
    }

    // CGC-23317: VALIDATING added to consolidate async validation into a single validity field.
    @Test
    fun testIsValid_validating() {
        assertTrue(UserFieldValidity.VALIDATING.isValid())
    }

    // CGC-23317: VALIDATED added to consolidate async validation into a single validity field.
    @Test
    fun testIsValid_validated() {
        assertTrue(UserFieldValidity.VALIDATED.isValid())
    }

    @Test
    fun testIsValid_invalid() {
        assertFalse(UserFieldValidity.INVALID.isValid())
    }

    @Test
    fun testIsValid_lengthTooShort() {
        assertFalse(UserFieldValidity.LENGTH_TOO_SHORT.isValid())
    }

    @Test
    fun testIsValid_lengthTooLong() {
        assertFalse(UserFieldValidity.LENGTH_TOO_LONG.isValid())
    }

    @Test
    fun testIsValid_valueTooSmall() {
        assertFalse(UserFieldValidity.VALUE_TOO_SMALL.isValid())
    }

    @Test
    fun testIsValid_valueTooBig() {
        assertFalse(UserFieldValidity.VALUE_TOO_BIG.isValid())
    }
}
