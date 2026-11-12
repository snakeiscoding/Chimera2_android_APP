package com.jht.interfaces

import com.jht.data.UserFieldDate
import com.jht.data.UserFieldDigits
import com.jht.data.UserFieldDouble
import com.jht.data.UserFieldEmail
import com.jht.data.UserFieldInteger
import com.jht.data.UserFieldNumber
import com.jht.data.UserFieldText
import com.jht.data.UserFieldValidity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

// Since the email validation pattern comes from android, this ends up as instrumentation rather than unit test

class UserFieldTests {

    @Test
    fun testTextField() {
        val unboundField = UserFieldText()
        testTextField(unboundField)
        testTextField(UserFieldText(minLength = 0, maxLength = 10))
        testTextField(UserFieldText(minLength = 1, maxLength = 10))
        testTextField(UserFieldText(minLength = 1, maxLength = 0))
        // value
        unboundField.value = "TEST"
        assertTrue("Set value", unboundField.getStringValue() == "TEST")
        unboundField.clear()
        assertTrue("Clear value", unboundField.value.isEmpty())
    }

    @Test
    fun testDigitField() {
        val unboundField = UserFieldDigits()
        testTextField(unboundField)
        testTextField(UserFieldDigits(minLength = 0, maxLength = 10))
        testTextField(UserFieldDigits(minLength = 1, maxLength = 10))
        testTextField(UserFieldDigits(minLength = 1, maxLength = 0))

        unboundField.value = "123"
        assertTrue("Valid value", unboundField.isValid())
        unboundField.value = "ABC"
        assertFalse("Invalid value", unboundField.isValid())
    }

    @Test
    fun testEmailField() {
        val emailGoodSample = "bob@z.com"
        val emailBadSamples = arrayOf("bob", "bob@", "bob@z")
        val unboundField = UserFieldEmail()
        testTextField(unboundField)
        testTextField(UserFieldEmail(minLength = 0, maxLength = 10))
        testTextField(UserFieldEmail(minLength = 1, maxLength = 10))
        testTextField(UserFieldEmail(minLength = 1, maxLength = 0))

        unboundField.value = emailGoodSample
        assertTrue("Valid email", unboundField.isValid())
        emailBadSamples.forEach {
            unboundField.value = it
            assertFalse("Invalid email $it", unboundField.isValid())
        }
    }

    @Test
    fun testIntegerField() {
        val stops = listOf(-10, 0, 10)
        val unboundField = UserFieldInteger()
        testNumberField(unboundField, 0, 0)
        stops.forEach { min->
            stops.forEach { max->
                if (min <= max) testNumberField(UserFieldInteger(minValue = min, maxValue = max), min, max)
            }
        }
        // value
        unboundField.value = 24
        assertTrue("Set value", unboundField.getDoubleValue() == 24.0)
        unboundField.clear()
        assertTrue("Clear value", unboundField.value == null)
    }

    @Test
    fun testDoubleField() {
        val stops = listOf(-10, 0, 10)
        val unboundField = UserFieldDouble()
        testNumberField(unboundField, 0, 0)
        stops.forEach { min->
            stops.forEach { max->
                if (min <= max) testNumberField(UserFieldDouble(minValue = min.toDouble(), maxValue = max.toDouble()), min, max)
            }
        }
        // value
        unboundField.value = 24.0
        assertTrue("Set value", unboundField.getDoubleValue() == 24.0)
        unboundField.clear()
        assertTrue("Clear value", unboundField.value == null)
    }

    @Test
    fun testDateField() {
        val date0 = LocalDate.of(1950, 6, 1)
        val date1 = LocalDate.of(1950, 12, 25)
        val date2 = LocalDate.of(2023, 6, 1)
        val date3 = LocalDate.of(2023, 12, 25)
        val date4 = LocalDate.of(2025, 1, 1)
        val unboundField = UserFieldDate()
        unboundField.value = date0
        assertTrue("Unbound date", unboundField.isValid())

        val boundField = UserFieldDate(minDate = date1, maxDate = date3)
        boundField.value = date0
        assertEquals("Before min", UserFieldValidity.VALUE_TOO_SMALL, boundField.getValidity())
        boundField.value = date1
        assertTrue("At min", boundField.isValid())
        boundField.value = date4
        assertEquals("After max", UserFieldValidity.VALUE_TOO_BIG, boundField.getValidity())
        boundField.value = date3
        assertTrue("At max", boundField.isValid())
        boundField.value = date2
        assertTrue("Date in range", boundField.isValid())
    }

    @Test
    fun testAsyncFieldValidate() {
        var testCheckPerformed = true
        var testErrorMessage = ""
        val field = UserFieldText(
            asyncValidation = {callback ->
                callback(testCheckPerformed, testErrorMessage)
            }
        )
        assertTrue("Field has async validation", field.hasAsyncValidation())
        val fieldNone = UserFieldText()
        assertFalse("Field has no async validation", fieldNone.hasAsyncValidation())

        field.runAsyncValidation { checkPerformed, error ->
            assertTrue("Async success", checkPerformed && error.isEmpty())
        }

        testCheckPerformed = true
        testErrorMessage = "FAILED"
        field.runAsyncValidation { checkPerformed, error ->
            assertTrue("Async failure", checkPerformed && error.isNotEmpty())
        }

        testCheckPerformed = false
        testErrorMessage = ""
        field.runAsyncValidation { checkPerformed, error ->
            assertFalse("Async not performed", checkPerformed)
        }
    }

}


private fun testNumberField(fieldNumber: UserFieldNumber<*>, minValue: Int, maxValue: Int) {

    if (fieldNumber.minValue != null) {
        fieldNumber.setDoubleValue(minValue - 1.0)
        assertEquals("Test < min $minValue", UserFieldValidity.VALUE_TOO_SMALL, fieldNumber.getValidity())
        fieldNumber.setDoubleValue(minValue + 1.0)
        assertNotEquals("Test > min $minValue", UserFieldValidity.VALUE_TOO_SMALL, fieldNumber.getValidity())
        fieldNumber.setDoubleValue(minValue.toDouble())
        assertNotEquals("Test == min $minValue", UserFieldValidity.VALUE_TOO_SMALL, fieldNumber.getValidity())

        // negative value entry
        if (minValue >= 0) {
            assertFalse("Negative not allowed", fieldNumber.allowNegative())
            assertFalse("Negative not allowed $minValue $maxValue", fieldNumber.inputValid("-1"))
        } else if (maxValue >= -1) {
            assertTrue("Negative is allowed", fieldNumber.allowNegative())
            assertTrue("Negative is allowed $minValue $maxValue", fieldNumber.inputValid("-1"))
        }
    } else {
        fieldNumber.setDoubleValue(Int.MIN_VALUE.toDouble())
        assertNotEquals("Test low value", UserFieldValidity.VALUE_TOO_SMALL, fieldNumber.getValidity())
        // negative value entry
        assertTrue("Negative is allowed", fieldNumber.allowNegative())
        assertTrue("Negative is allowed", fieldNumber.inputValid("-1"))
    }

    if (fieldNumber.maxValue != null) {
        fieldNumber.setDoubleValue(maxValue + 1.0)
        assertEquals("Test > max $maxValue", UserFieldValidity.VALUE_TOO_BIG, fieldNumber.getValidity())
        fieldNumber.setDoubleValue(maxValue - 1.0)
        assertNotEquals("Test < max $maxValue", UserFieldValidity.VALUE_TOO_BIG, fieldNumber.getValidity())
        fieldNumber.setDoubleValue(maxValue.toDouble())
        assertNotEquals("Test == max $maxValue", UserFieldValidity.VALUE_TOO_BIG, fieldNumber.getValidity())
    } else {
        fieldNumber.setDoubleValue(Int.MAX_VALUE.toDouble())
        assertNotEquals("Test large value", UserFieldValidity.VALUE_TOO_BIG, fieldNumber.getValidity())
    }

}


private fun testTextField(fieldText: UserFieldText) {
    val fixedSize = (fieldText.minLength == fieldText.maxLength) && fieldText.minLength > 0

    if (fieldText.minLength > 0) {
        fieldText.value = ""
        assertEquals("Test < min", UserFieldValidity.LENGTH_TOO_SHORT, fieldText.getValidity())
        if (!fixedSize) {
            fieldText.value = createDigitString(fieldText.minLength+1)
            assertNotEquals("Test > min", UserFieldValidity.LENGTH_TOO_SHORT, fieldText.getValidity())
        }
        fieldText.value = createDigitString(fieldText.minLength)
        assertNotEquals("Test == min", UserFieldValidity.LENGTH_TOO_SHORT, fieldText.getValidity())
    } else {
        fieldText.value = ""
        assertNotEquals("Test zero size", UserFieldValidity.LENGTH_TOO_SHORT, fieldText.getValidity())
    }

    if (fieldText.maxLength > 0) {
        fieldText.value = createDigitString(fieldText.maxLength+1)
        assertEquals("Test > max", UserFieldValidity.LENGTH_TOO_LONG, fieldText.getValidity())
        if (!fixedSize) {
            fieldText.value = createDigitString(fieldText.maxLength-1)
            assertNotEquals("Test < max", UserFieldValidity.LENGTH_TOO_LONG, fieldText.getValidity())
        }
        fieldText.value = createDigitString(fieldText.maxLength)
        assertNotEquals("Test == max", UserFieldValidity.LENGTH_TOO_LONG, fieldText.getValidity())
    } else {
        fieldText.value = createDigitString(255)
        assertNotEquals("Test large size", UserFieldValidity.LENGTH_TOO_LONG, fieldText.getValidity())
    }

    // Value parse/clear
    fieldText.setDoubleValue(12.5)
    assertTrue("set/get double", fieldText.getDoubleValue() == 12.5)
    fieldText.clear()
    assertTrue("Value clear", fieldText.value.isEmpty())
}


private fun createDigitString(length: Int): String {
    var temp = ""
    for (i in 0 until length) {
        temp += (i % 10)
    }
    return temp
}
