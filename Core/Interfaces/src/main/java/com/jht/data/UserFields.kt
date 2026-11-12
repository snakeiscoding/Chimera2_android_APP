package com.jht.data

import android.util.Patterns
import com.jht.enums.Gender
import com.jht.enums.UnitSystem
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import kotlin.math.min
import kotlin.math.roundToInt

/** Describe validity of a UserField entry */
enum class UserFieldValidity { // should this be a class structure so you can pass range data or other additional info/values?
    VALID,
    INVALID,
    LENGTH_TOO_SHORT,
    LENGTH_TOO_LONG,
    VALUE_TOO_SMALL,
    VALUE_TOO_BIG,
    VALIDATING, // async validation in progress
    VALIDATED,  // async validation passed
    ; // END

    fun isValid() = (this == VALID || this == VALIDATING || this == VALIDATED)
}


/** Provides a means to find and identify fields by general function */
enum class UserFieldFunction {
    Unspecified,
    LoginId,
    Password,
    ConfirmPassword, // password confirmation is UI function, back end should not care? but is this convenient for other uses? what about specific names?
    EmailAddress,
    FirstName,
    LastName,
    Birthdate,
    Gender,
    UnitPreference,
    Weight,
    //Height,
}

private inline fun <reified E : Enum<E>> getEnum(name: String) : E? { return try { enumValueOf<E>(name) } catch (ignored:Exception) { null } }

private typealias UserFieldAsyncValidFunction = UserField.(UserFieldAsyncValidCallback) -> Unit
private typealias UserFieldAsyncValidCallback = (checkPerformed:Boolean, error:String) -> Unit

/** Describes an input field and its restrictions */
abstract class UserField(
    /** Specify a generic function for this field. Facilitates finding a field in a list. */
    open val function: UserFieldFunction = UserFieldFunction.Unspecified,
    /** Translated label string for use in forms */
    open val label: String = "",
    /** Translated hint string for use in forms */
    open val hint: String = "",
    //val password: Boolean = false, // just rely on assigned function or should it be separate and explicit ?
    /** Async validation to run for this field if applicable, leave null if none */
    protected open val asyncValidation: UserFieldAsyncValidFunction? = null,
) {
    /** Current value held in the field */
    open val value: Any? = null
    /** restores [value] to its default state */
    open fun clear() {}
    /** Returns if field is password style field (UI can choose to take actions like masking the presentation) */
    open fun isPassword() = function == UserFieldFunction.Password || function == UserFieldFunction.ConfirmPassword

    // ? is it better to have input blocking or input filtering or is there argument for both?
    /** returns if given string input is valid given the field's restrictions */
    open fun inputValid(inputString: String) = true
    /** returns a string that is filtered based on restrictions, if applicable */
    open fun filter(inputString: String) = inputString

    /** returns synchronous restrictions validity status as [UserFieldValidity] enum, if applicable */
    open fun getValidity() : UserFieldValidity = UserFieldValidity.VALID
    /** returns synchronous restrictions validity status as success boolean, if applicable */
    open fun isValid() = getValidity().isValid()

    /** indicates if field has async validation defined */
    open fun hasAsyncValidation(): Boolean = asyncValidation != null
    /** executes async validation if there is one, result is conveyed through callback */
    open fun runAsyncValidation(callback: UserFieldAsyncValidCallback) { asyncValidation?.invoke(this, callback) } // should we return if one was called?

    /** attempts to return value of field as a string */
    open fun getStringValue() : String? = value?.toString()
    /** attempts to set value of field from a string, falls back to default value (Check [inputValid] if you want to ignore invalid value) */
    open fun setStringValue(stringValue: String) {} // this should not auto filter, caller can use inputValid/filter, if value cannot be parsed use default?

    /** attempts to return value of field as a number, null if not possible */
    open fun getDoubleValue() : Double? = null
    /** attempts to set value of field from a number, falls back to default value */
    open fun setDoubleValue(doubleValue: Double) {}

    /** returns lower limit as string for constructing error messages */
    open fun getLowerLimit() = ""
    /** returns upper limit as string for constructing error messages */
    open fun getUpperLimit() = ""
}

/** Describes a text input field and its restrictions */
open class UserFieldText(
    override val function: UserFieldFunction = UserFieldFunction.Unspecified,
    override val label: String = "",
    override val hint: String = "",
    open val minLength : Int = 0,
    open val maxLength : Int = 0,
    override val asyncValidation: UserFieldAsyncValidFunction? = null,
) : UserField() {
    override var value : String = ""
    override fun inputValid(inputString: String): Boolean = (maxLength == 0 || inputString.length <= maxLength)
    override fun filter(inputString: String): String {
        return if (maxLength > 0) inputString.substring(0, min(maxLength, inputString.length)) else inputString
    }
    override fun setStringValue(stringValue: String) { value = stringValue }
    override fun getStringValue() = value
    override fun getDoubleValue(): Double? = value.toDoubleOrNull()
    override fun setDoubleValue(doubleValue: Double) { value = doubleValue.toString() }
    override fun clear() { value = "" }
    override fun getValidity() : UserFieldValidity {
        if (minLength > 0 && value.length < minLength) return UserFieldValidity.LENGTH_TOO_SHORT
        if (maxLength > 0 && value.length > maxLength) return UserFieldValidity.LENGTH_TOO_LONG
        return UserFieldValidity.VALID
    }
    override fun getLowerLimit() = if (minLength > 0) minLength.toString() else ""
    override fun getUpperLimit() = if (maxLength > 0) maxLength.toString() else ""
}


/** Describes a text input field that takes only a certain number of digits */
open class UserFieldDigits(
    override val function: UserFieldFunction = UserFieldFunction.Unspecified,
    override val label: String = "",
    override val hint: String = "",
    override val minLength : Int = 0,
    override val maxLength : Int = 0,
    override val asyncValidation: UserFieldAsyncValidFunction? = null,
) : UserFieldText() {
    private fun charValid(it: Char) = it.isDigit()
    override fun inputValid(inputString: String): Boolean = super.inputValid(inputString) && inputString.all { charValid(it) }
    override fun filter(inputString: String) = super.filter(inputString.filter { charValid(it) })
    override fun getStringValue() = value
    override fun clear() { value = "" }
    override fun getValidity() : UserFieldValidity {
        if (minLength > 0 && value.length < minLength) return UserFieldValidity.LENGTH_TOO_SHORT
        if (maxLength > 0 && value.length > maxLength) return UserFieldValidity.LENGTH_TOO_LONG
        return if (inputValid(value)) UserFieldValidity.VALID else UserFieldValidity.INVALID
    }
}


/** Describes an email input field and its restrictions */
open class UserFieldEmail(
    override val function: UserFieldFunction = UserFieldFunction.EmailAddress,
    override val label: String = "",
    override val hint: String = "",
    override val minLength : Int = 0,
    override val maxLength : Int = 0,
    override val asyncValidation: UserFieldAsyncValidFunction? = null,
) : UserFieldText() {
    override fun getValidity() : UserFieldValidity {
        if (minLength > 0 && value.length < minLength) return UserFieldValidity.LENGTH_TOO_SHORT
        if (maxLength > 0 && value.length > maxLength) return UserFieldValidity.LENGTH_TOO_LONG
        return if (Patterns.EMAIL_ADDRESS.matcher(value).matches()) UserFieldValidity.VALID else UserFieldValidity.INVALID
    }
}


/** Base for a UserField that handles numeric values */
abstract class UserFieldNumber<T>(
    open val minValue: T? = null,
    open val maxValue: T? = null,
    override val asyncValidation: UserFieldAsyncValidFunction? = null,
) : UserField() where T : Number, T : Comparable<T> {
    override val value: T? = null // could declare val base and override as var? base could only be get and not set?
    abstract fun allowNegative() : Boolean // Kotlin does not like comparing Number to zero, so defer to specific implementation
    override fun getStringValue() = value?.toString()
    override fun getDoubleValue(): Double? = value?.toDouble()
    override fun getLowerLimit() = minValue?.toString() ?: ""
    override fun getUpperLimit() = maxValue?.toString() ?: ""
    override fun getValidity(): UserFieldValidity {
        if (value == null) return UserFieldValidity.INVALID
        if (minValue != null && value!! < minValue!!) return UserFieldValidity.VALUE_TOO_SMALL
        if (maxValue != null && value!! > maxValue!!) return UserFieldValidity.VALUE_TOO_BIG
        return UserFieldValidity.VALID
    }
}

/** Private class to provide character checking for number entry */
private class NumberPredicate(
    val allowNegative: Boolean
) {
    // is it better to cache these values than to grab them for every char we check?
    private val pointChar: Char
    private val groupChar: Char
    private val negativeChar: Char
    init {
        val symbols = DecimalFormatSymbols.getInstance()
        pointChar = symbols.decimalSeparator    // do we just allow both "." and "," since most just use one of those or better to ask locale?
        groupChar = symbols.groupingSeparator
        negativeChar = symbols.minusSign
    }
    // technically minus is only valid as the first character? point is only valid once? does parse handle group or should we just block it?
    fun check(it: Char): Boolean = it.isDigit() || it == pointChar || it == groupChar || (it == negativeChar && allowNegative)
}


/** Describes a integer input field and its restrictions. If you need fractional numbers see UserFieldDouble. */
class UserFieldInteger(
    override val function: UserFieldFunction = UserFieldFunction.Unspecified,
    override val label: String = "",
    override val hint: String = "",
    override val minValue: Int? = null,
    override val maxValue: Int? = null,
    override val asyncValidation: UserFieldAsyncValidFunction? = null,
) : UserFieldNumber<Int>() {
    override var value: Int? = null
    override fun allowNegative(): Boolean = (minValue == null || minValue < 0)
    private fun charValid(it: Char) = it.isDigit() || (allowNegative() && it == '-')
    override fun inputValid(inputString: String): Boolean = inputString.all { charValid(it) }
    override fun filter(inputString: String) = inputString.filter { charValid(it) }
    override fun setStringValue(stringValue: String) { value = stringValue.toIntOrNull() }
    override fun setDoubleValue(doubleValue: Double) { value = doubleValue.roundToInt() }
    override fun clear() { value = null }
}

/** Describes a double input field and its restrictions */
class UserFieldDouble(
    override val function: UserFieldFunction = UserFieldFunction.Unspecified,
    override val label: String = "",
    override val hint: String = "",
    override val minValue: Double? = null,
    override val maxValue: Double? = null,
    override val asyncValidation: UserFieldAsyncValidFunction? = null,
) : UserFieldNumber<Double>() {
    override var value: Double? = null
    override fun allowNegative(): Boolean = (minValue == null || minValue < 0)
    private val charChecker = NumberPredicate(allowNegative())
    override fun inputValid(inputString: String): Boolean = inputString.all { charChecker.check(it) }
    override fun filter(inputString: String) = inputString.filter { charChecker.check(it) }
    override fun setStringValue(stringValue: String) { value = stringValue.toDoubleOrNull() }
    override fun setDoubleValue(doubleValue: Double) { value = doubleValue }
    override fun clear() { value = null }
}

/** Describes a measurement unit system input field and its restrictions */
open class UserFieldUnits(
    override val function: UserFieldFunction = UserFieldFunction.UnitPreference,
    override val label: String = "",
    override val hint: String = "",
    val defaultValue : UnitSystem = UnitSystem.DEFAULT,
    override val asyncValidation: UserFieldAsyncValidFunction? = null,
) : UserField() {
    override var value: UnitSystem = defaultValue
    override fun setStringValue(stringValue: String) { value = getEnum<UnitSystem>(stringValue) ?: defaultValue }
    override fun getStringValue() = value.name
    override fun clear() { value = defaultValue }
}

/** Describes a gender input field and its restrictions */
open class UserFieldGender(
    override val function: UserFieldFunction = UserFieldFunction.Gender,
    override val label: String = "",
    override val hint: String = "",
    val defaultValue : Gender = Gender.DEFAULT,
    override val asyncValidation: UserFieldAsyncValidFunction? = null,
) : UserField() {
    override var value: Gender = defaultValue
    override fun setStringValue(stringValue: String) { value = getEnum<Gender>(stringValue) ?: defaultValue }
    override fun getStringValue() = value.name
    override fun clear() { value = defaultValue }
}

/** Describes a date input field and its restrictions */
open class UserFieldDate(
    override val function: UserFieldFunction = UserFieldFunction.Birthdate,
    override val label: String = "",
    override val hint: String = "",
    val minDate: LocalDate? = null,
    val maxDate: LocalDate? = null,
    override val asyncValidation: UserFieldAsyncValidFunction? = null,
) : UserField() {
    override var value: LocalDate? = null
    override fun clear() { value = null }
    override fun getValidity() : UserFieldValidity {
        if (value == null) return UserFieldValidity.INVALID
        if (minDate != null && value!! < minDate) return UserFieldValidity.VALUE_TOO_SMALL
        if (maxDate != null && value!! > maxDate) return UserFieldValidity.VALUE_TOO_BIG
        return UserFieldValidity.VALID
    }
    override fun getLowerLimit() = minDate?.toString() ?: ""
    override fun getUpperLimit() = maxDate?.toString() ?: ""
}


/** Manages a list of UserFields */
open class UserFieldList(
    open val fields: List<UserField> = listOf(),
) {
    open fun clearValues() { fields.forEach { it.clear() } }
    open fun hasValidValues() : Boolean {
        for (field in fields) {
            if (!field.isValid()) return false
        }
        return true
    }
    fun find(fieldFunction: UserFieldFunction) : UserField? = fields.find { it.function == fieldFunction }
    inline fun <reified T : UserField> findCast(fieldFunction: UserFieldFunction) : T? = fields.find { it.function == fieldFunction && it is T } as? T
}

