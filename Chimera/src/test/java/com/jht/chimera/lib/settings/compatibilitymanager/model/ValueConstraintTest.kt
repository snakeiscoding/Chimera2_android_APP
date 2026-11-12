package com.jht.chimera.lib.settings.compatibilitymanager.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValueConstraintTest {
    @Test
    fun `test AnyOf applies to values correctly`() {
        // Should apply to any value of any type
        val anyValue: ValueConstraint<Any> = ValueConstraint.any()
        assertTrue(anyValue.appliesTo(5))
        assertTrue(anyValue.appliesTo("test-value"))
        assertTrue(anyValue.appliesTo(10.5f))
        assertTrue(anyValue.appliesTo(0.5))

        // Should only apply to String values
        val anyString: ValueConstraint<String> = ValueConstraint.any()
        assertTrue(anyString.appliesTo("test-value"))
        assertTrue(anyString.appliesTo("test-value2"))
        assertFalse(anyString.appliesTo(10))
        assertFalse(anyString.appliesTo('C'))

        // Should only apply to Float values
        val anyFloat: ValueConstraint<Float> = ValueConstraint.any()
        assertTrue(anyFloat.appliesTo(10.5f))
        assertTrue(anyFloat.appliesTo(0.5f))
        assertFalse(anyFloat.appliesTo(10.5))
        assertFalse(anyFloat.appliesTo(10))

        // Should only apply to Double values
        val anyDouble: ValueConstraint<Double> = ValueConstraint.any()
        assertTrue(anyDouble.appliesTo(10.5))
        assertTrue(anyDouble.appliesTo(0.5))
        assertFalse(anyDouble.appliesTo(10.5f))
        assertFalse(anyDouble.appliesTo(10))
    }

    @Test
    fun `test AnyOf applies to nullability correctly`() {
        // Should apply to any value that is not null
        val anyValue: ValueConstraint<Any> = ValueConstraint.any()
        assertFalse(anyValue.appliesTo(null))

        // Should apply to any value including null
        val anyValueNullable: ValueConstraint<Any?> = ValueConstraint.any()
        assertTrue(anyValueNullable.appliesTo(null))

        // Should apply to any string that is not null
        val anyString: ValueConstraint<String> = ValueConstraint.any()
        assertFalse(anyString.appliesTo(null))

        // Should apply to any string including null
        val anyStringNullable: ValueConstraint<String?> = ValueConstraint.any()
        assertTrue(anyStringNullable.appliesTo(null))

        // Should apply to any integer that is not null
        val anyInt: ValueConstraint<Int> = ValueConstraint.any()
        assertFalse(anyInt.appliesTo(null))

        // Should apply to any integer including null
        val anyIntNullable: ValueConstraint<Int?> = ValueConstraint.any()
        assertTrue(anyIntNullable.appliesTo(null))
    }

    @Test
    fun `test ValuesOf applies to values correctly`(){
        // Should apply only to values 5, "test-value", and 0.5f
        val specificValues: ValueConstraint<Any> = ValueConstraint.of(5, "test-value", 0.5)
        assertTrue(specificValues.appliesTo(5))
        assertTrue(specificValues.appliesTo("test-value"))
        assertTrue(specificValues.appliesTo(0.5))
        assertFalse(specificValues.appliesTo(5.0))
        assertFalse(specificValues.appliesTo("test-value2"))
        assertFalse(specificValues.appliesTo(0.5f))

        // Should only apply to String values of "test-value" and "test-value2"
        val specificStrings = ValueConstraint.of("test-value", "test-value2")
        assertTrue(specificStrings.appliesTo("test-value"))
        assertTrue(specificStrings.appliesTo("test-value2"))
        assertFalse(specificStrings.appliesTo("test-value3"))
        assertFalse(specificStrings.appliesTo(5))

        // Should only apply to Double values of 10.5 and 0.5
        val specificDoubles = ValueConstraint.of(10.5, 0.5)
        assertTrue(specificDoubles.appliesTo(10.5))
        assertTrue(specificDoubles.appliesTo(0.5))
        assertFalse(specificDoubles.appliesTo(10.5f))
        assertFalse(specificDoubles.appliesTo("test-value"))
    }

    @Test
    fun `test ValuesOf applies to nullability correctly`() {
        // Should only apply to values 5 and "test-value". Null does not apply
        val specificValues: ValueConstraint<Any> = ValueConstraint.of(listOf(5, "test-value"))
        assertTrue(specificValues.appliesTo(5))
        assertTrue(specificValues.appliesTo("test-value"))
        assertFalse(specificValues.appliesTo(null))
        assertFalse(specificValues.appliesTo(6))

        // Should only apply to values 5 and "test-value" since null is not included in valid values
        val specificValuesNullable: ValueConstraint<Any?> = ValueConstraint.of(5, "test-value")
        assertTrue(specificValues.appliesTo(5))
        assertTrue(specificValues.appliesTo("test-value"))
        assertFalse(specificValuesNullable.appliesTo(null))
        assertFalse(specificValues.appliesTo(6))

        // Should only apply to values "test-value" and null
        val specificStringsNullable = ValueConstraint.of("test-value", null)
        assertTrue(specificStringsNullable.appliesTo("test-value"))
        assertTrue(specificStringsNullable.appliesTo(null))
        assertFalse(specificStringsNullable.appliesTo("test-value2"))
    }

    @Test
    fun `test ValueOf applies to value correctly`(){
        // Should only apply to a value of "test-value"
        val exactAny: ValueConstraint<Any> = ValueConstraint.of("test-value")
        assertTrue(exactAny.appliesTo("test-value"))
        assertFalse(exactAny.appliesTo("test-value2"))
        assertFalse(exactAny.appliesTo(5))

        // Should only apply to a value of "test-value"
        val exactDouble = ValueConstraint.of(0.5)
        assertTrue(exactDouble.appliesTo(0.5))
        assertFalse(exactDouble.appliesTo(0.5f))
        assertFalse(exactDouble.appliesTo("test-value"))

        // Should only apply to a value of true
        val exactBoolean = ValueConstraint.of(true)
        assertTrue(exactBoolean.appliesTo(true))
        assertFalse(exactBoolean.appliesTo(false))
        assertFalse(exactBoolean.appliesTo(1))
    }

    @Test
    fun `test ValueOf applies to nullability correctly`() {
        // Should only apply to a value of "test-value"
        val exactAny: ValueConstraint<Any> = ValueConstraint.of("test-value")
        assertTrue(exactAny.appliesTo("test-value"))
        assertFalse(exactAny.appliesTo(null))

        // Should only apply to a value of 5
        val exactInt: ValueConstraint<Int?> = ValueConstraint.of(5)
        assertTrue(exactInt.appliesTo(5))
        assertFalse(exactInt.appliesTo(null))

        // Should only apply to a value of null
        val exactBoolean: ValueConstraint<Boolean?> = ValueConstraint.of(null)
        assertTrue(exactBoolean.appliesTo(null))
        assertFalse(exactBoolean.appliesTo(true))
    }
}