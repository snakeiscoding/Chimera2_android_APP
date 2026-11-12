package com.jht.mastboard.data

/**
 * The available LED style values.
 */
enum class LEDStyle(@JvmField val value: Int) {
    BASIC(0), // fixme: replace all references to this that actually want DEFAULT to DEFAULT
    NONE(4);

    companion object {
        /**
         * Return the LED style whose value matches the given [value].
         *
         * @param value The given value.
         * @return The matching LED style.
         * @throws NoSuchElementException There is no matching LED style.
         */
        @Throws(NoSuchElementException::class)
        @JvmStatic
        fun fromValue(value: Int): LEDStyle = values().firstOrNull {
            it.value == value
        } ?: throw NoSuchElementException("No LEDStyle with value: $value")

        /**
         * The default LED style. This should be used **everywhere** that is trying to use a default
         * setting to ensure we are universally referring to this.
         */
        @JvmField
        val DEFAULT = BASIC
    }
}