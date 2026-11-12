package com.jht.widgets

import com.jht.data.UserFieldText
import com.jht.data.UserFieldValidity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldInputDataTests {

    @Test
    fun testSetNewValue_validInput() {
        val field = UserFieldText()
        val data = FieldInputData.build(field, "tag")
        val result = data.setNewValue("hello")

        assertEquals("hello", result)
    }

    @Test
    fun testSetNewValue_invalidInputReturnsNull() {
        val field = UserFieldText(maxLength = 3)
        val data = FieldInputData.build(field, "tag")
        val result = data.setNewValue("toolong")

        assertNull(result)
    }

    @Test
    fun testSetNewValue_updatesFieldValue() {
        val field = UserFieldText()
        val data = FieldInputData.build(field, "tag")
        val result = data.setNewValue("old")

        assertEquals("old", result)
        data.setNewValue("updated")
        assertEquals("updated", field.getStringValue())
    }

    @Test
    fun testCheckValidity() {
        val field = UserFieldText(minLength = 1)
        field.value = "abc"
        val data = FieldInputData.build(field, "tag")
        val result = data.checkValidity(includeAsync = false)

        assertEquals(UserFieldValidity.VALID, result)
        assertEquals(UserFieldValidity.VALID, data.validity.value)
    }

    @Test
    fun testCheckValidity_invalidTooShort() {
        val field = UserFieldText(minLength = 5)
        field.value = "ab"
        val data = FieldInputData.build(field, "tag")
        val result = data.checkValidity(includeAsync = false)

        assertEquals(UserFieldValidity.LENGTH_TOO_SHORT, result)
        assertEquals(UserFieldValidity.LENGTH_TOO_SHORT, data.validity.value)
    }

    @Test
    fun testCheckValidity_invalidTooLong() {
        val field = UserFieldText(minLength = 1, maxLength = 3)
        field.value = "toolong"
        val data = FieldInputData.build(field, "tag")
        val result = data.checkValidity(includeAsync = false)

        assertEquals(UserFieldValidity.LENGTH_TOO_LONG, result)
        assertEquals(UserFieldValidity.LENGTH_TOO_LONG, data.validity.value)
    }

    @Test
    fun testCheckValidity_invalidClearsCustomError() {
        val field = UserFieldText(minLength = 5)
        field.value = "ab"
        val data = FieldInputData.build(field, "tag")
        data.customError.value = "stale async error"

        data.checkValidity(includeAsync = false)
        assertEquals("", data.customError.value)
    }

    // CGC-23317: Async success sets validity to VALIDATED instead of a separate asyncState field.
    @Test
    fun testCheckValidity_asyncSuccess() {
        val field = UserFieldText(
            asyncValidation = { callback -> callback(true, "") }
        )
        field.value = "valid"
        val data = FieldInputData.build(field, "tag")

        data.checkValidity(includeAsync = true)
        assertEquals(UserFieldValidity.VALIDATED, data.validity.value)
    }

    // CGC-23317: Async failure sets validity to INVALID instead of a separate asyncState field.
    @Test
    fun testCheckValidity_asyncFailure() {
        val errorMessage = "already exists"
        val field = UserFieldText(
            asyncValidation = { callback -> callback(true, errorMessage) }
        )
        field.value = "valid"
        val data = FieldInputData.build(field, "tag")

        data.checkValidity(includeAsync = true)
        assertEquals(UserFieldValidity.INVALID, data.validity.value)
        assertEquals(errorMessage, data.customError.value)
    }

    // CGC-23317: Async not performed reverts validity to VALID.
    @Test
    fun testCheckValidity_asyncNotPerformed() {
        val field = UserFieldText(
            asyncValidation = { callback -> callback(false, "") }
        )
        field.value = "valid"
        val data = FieldInputData.build(field, "tag")

        data.checkValidity(includeAsync = true)
        assertEquals(UserFieldValidity.VALID, data.validity.value)
    }

    // CGC-23317: Sync failure after async success resets validity — no stale VALIDATED state.
    @Test
    fun testCheckValidity_syncFailureAfterAsyncSuccessClearsValidatedState() {
        val field = UserFieldText(
            minLength = 3,
            asyncValidation = { callback -> callback(true, "") }
        )
        field.value = "valid"
        val data = FieldInputData.build(field, "tag")

        data.checkValidity(includeAsync = true)
        assertEquals(UserFieldValidity.VALIDATED, data.validity.value)

        field.value = "ab"
        data.checkValidity(includeAsync = true)
        assertEquals(UserFieldValidity.LENGTH_TOO_SHORT, data.validity.value)
    }

    @Test
    fun testCheckValidity_skipAsyncWhenIncludeAsyncFalse() {
        var asyncCalled = false
        val field = UserFieldText(
            asyncValidation = { callback -> asyncCalled = true; callback(true, "") }
        )
        field.value = "valid"
        val data = FieldInputData.build(field, "tag")

        data.checkValidity(includeAsync = false)
        assertFalse(asyncCalled)
        assertEquals(UserFieldValidity.VALID, data.validity.value)
    }

    @Test
    fun testCheckValidity_noAsyncFieldSetsValid() {
        val field = UserFieldText()
        field.value = "valid"
        val data = FieldInputData.build(field, "tag")

        data.checkValidity(includeAsync = true)
        assertEquals(UserFieldValidity.VALID, data.validity.value)
    }

    @Test
    fun testCallDone_invokesCallback() {
        var called = false
        val field = UserFieldText()
        val data = FieldInputData.build(field, "tag", onDone = { called = true })

        data.callDone()
        assertTrue(called)
    }

    @Test
    fun testCallDone_nullCallbackDoesNothing() {
        val field = UserFieldText()
        val data = FieldInputData.build(field, "tag", onDone = null)

        data.callDone()
    }

    @Test
    fun testCollectionCheckValidity_allValid() {
        val collection = FieldInputCollection()
        val field1 = UserFieldText()
        field1.value = "abc"
        val field2 = UserFieldText()
        field2.value = "def"

        collection.add(FieldInputData.build(field1, "tag1"))
        collection.add(FieldInputData.build(field2, "tag2"))
        assertTrue(collection.checkValidity(includeAsync = false))
    }

    @Test
    fun testCollectionCheckValidity_oneInvalid() {
        val collection = FieldInputCollection()
        val field1 = UserFieldText()
        field1.value = "abc"
        val field2 = UserFieldText(minLength = 5)
        field2.value = "ab"

        collection.add(FieldInputData.build(field1, "tag1"))
        collection.add(FieldInputData.build(field2, "tag2"))
        assertFalse(collection.checkValidity(includeAsync = false))
    }

    @Test
    fun testCollectionGetLastValidity_reflectsCurrentState() {
        val collection = FieldInputCollection()
        val field = UserFieldText(minLength = 3)
        field.value = "ab"
        val data = FieldInputData.build(field, "tag")


        collection.add(data)
        data.checkValidity(includeAsync = false)
        assertFalse(collection.getLastValidity())
    }
}
