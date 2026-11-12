package com.jht.chimera.lib.settings.compatibilitymanager.model

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Represents a constraint for some value.
 *
 * Use [appliesTo] to check whether a provided value satisfies the constraint.
 *
 * Example 1: ValueConstraint.of("Hello") will only apply to the value "Hello"
 * Example 2: ValueConstraint.of<String?>("Hello") will apply to the value "Hello" or null
 * Example 3: ValueConstraint.of<String>("Hello", "World") will apply to the value "Hello" or "World", but not null
 * Example 4: ValueConstraint.any<Int?>() will apply to any value of type Int including null
 * Example 5: ValueConstraint.any<String>() will apply to any value of type String but not null
 */
sealed class ValueConstraint<T> {
    data class AnyOf<T>(val type: KType) : ValueConstraint<T>()
    private data class ValuesOf<T>(val values: Set<T>) : ValueConstraint<T>()
    private data class ValueOf<T>(val value: T) : ValueConstraint<T>()

    companion object {
        /**
         * Creates a value constraint that accepts any value of type T.
         */
        inline fun <reified T> any(): ValueConstraint<T> = AnyOf(typeOf<T>())

        /**
         * Creates a value constraint that accepts only the values specified.
         *
         * @param values the values for which the constraint is met.
         */
        fun <T> of(vararg values: T): ValueConstraint<T> = ValuesOf(values.toSet())

        /**
         * Creates a value constraint that accepts only the values from the given collection.
         *
         * @param values the collection containing the values for which the constraint is met.
         */
        fun <T> of(values: Collection<T>): ValueConstraint<T> = ValuesOf(values.toSet())

        /**
         * Creates a value constraint that accepts only the given value.
         *
         * @param value the value for which the constraint is met.
         */
        fun <T> of(value: T): ValueConstraint<T> = ValueOf(value)
    }

    /**
     * Checks if the provided value satisfies the constraint.
     *
     * @param value the value to test.
     * @return true if the value meets the constraint; false otherwise.
     */
    fun appliesTo(value: Any?): Boolean = when (this) {
        is AnyOf -> {
            if (value == null) {
                // Return true only if the expected type is nullable.
                type.isMarkedNullable
            } else {
                // Retrieve the classifier from the type, which should be a KClass.
                val classifier = type.classifier as? KClass<*>
                classifier?.isInstance(value) ?: false
            }
        }
        is ValuesOf -> value in values
        is ValueOf ->  this.value == value
    }
}