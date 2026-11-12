package com.jht.widgets.numpad

import com.jht.widgets.numpad.internal.ANIMATION_ITERATIONS
import com.jht.widgets.numpad.internal.ANIMATION_TIME
import com.jht.widgets.numpad.internal.AUTOCORRECT_TIMEOUT
import com.jht.widgets.numpad.internal.ENTRY_INITIAL
import com.jht.widgets.numpad.internal.ENTRY_MAX
import com.jht.widgets.numpad.internal.ENTRY_MIN
import com.jht.widgets.numpad.internal.ENTRY_ROUND
import com.jht.widgets.numpad.internal.MAX_USER_ENTRY_CHARACTER_LENGTH
import com.jht.widgets.numpad.internal.NUMPAD_TIMEOUT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base numpad input handler. Manages digit buffering, delete, autocorrect clamping,
 * and inactivity timeout.
 *
 * Use directly for screens that need a generic numpad. Extend for feature-specific
 * behavior (e.g. Sprint8NumPadHandler for decimal/incline formatting).
 *
 * Wire the composable callbacks to [onDigit], [onDelete], [onLongDelete]. Observe
 * [state] via `collectAsState()`. On confirm, read [currentFloat] and call [reset].
 */
open class NumPadHandler(
    private val scope: CoroutineScope,
    protected val config: NumPadConfig,
    initialValue: Float = 0f,
    private val onDismiss: () -> Unit = {},
) {
    private val _state = MutableStateFlow(
        NumPadState(
            displayValue = config.displayType.format(
                initialValue,
                config.decimalPlaces,
                config.displayType.needHours(config.max),
            ),
            isConfirmEnabled = initialValue in config.min..config.max,
            showPercentageBar = config.usePercentageBar,
            percentageFraction = if (config.usePercentageBar && config.max != config.min) {
                ((initialValue - config.min) / (config.max - config.min)).coerceIn(0f, 1f)
            } else 0f,
            unitLabel = config.unitLabel,
            valueFloat = initialValue,
        )
    )

    val state: StateFlow<NumPadState> = _state.asStateFlow()

    protected var digitBuffer: String = ENTRY_INITIAL
    private var initialFloat: Float = initialValue
    private var roundTarget: Float = config.min
    private var inactivityJob: Job? = null
    private var autocorrectJob: Job? = null

    init {
        resetInactivityTimer()
    }

    protected val useHours: Boolean get() = config.displayType.needHours(config.max)

    // -------------------------------------------------------------------------
    // Public event API
    // -------------------------------------------------------------------------

    fun onDigit(digit: String) {
        resetInactivityTimer()
        if (_state.value.isAnimating) return
        autocorrectJob?.cancel()
        val current = digitBuffer
        when {
            current.isNotEmpty() && !current.isDigitsOnly() -> digitBuffer = digit
            current.isNotEmpty() && current.isDigitsOnly() && bufferAsFloat() == 0f && digit == "0" -> return
            current.length < maxDigits() -> digitBuffer += digit
            else -> return
        }
        refreshState()
        scheduleAutocorrect()
    }

    fun onDelete() {
        resetInactivityTimer()
        if (_state.value.isAnimating) return
        autocorrectJob?.cancel()
        digitBuffer = if (digitBuffer.isDigitsOnly()) digitBuffer.dropLast(1) else ""
        refreshState()
    }

    fun onLongDelete() {
        resetInactivityTimer()
        if (_state.value.isAnimating) return
        autocorrectJob?.cancel()
        digitBuffer = ""
        refreshState()
    }

    /** Returns the current numeric value for submission on confirm. */
    fun currentFloat(): Float = bufferAsFloat()

    /** Resets the handler to the initial state after submission. */
    fun reset(newInitialValue: Float = initialFloat) {
        autocorrectJob?.cancel()
        inactivityJob?.cancel()
        initialFloat = newInitialValue
        digitBuffer = ENTRY_INITIAL
        refreshState()
    }

    // -------------------------------------------------------------------------
    // Open hooks override in subclasses for feature-specific behavior
    // -------------------------------------------------------------------------

    /**
     * Formats a raw digit buffer string into the display string shown in the numpad.
     * Only called with actual digit strings (empty or digits only). Sentinel values
     * are handled by the base class before this is called.
     *
     * Override to apply custom decimal placement or unit formatting.
     */
    protected open fun formatDisplay(digits: String): String {
        val value = decodeDigitBuffer(digits)
        return if (value < 0f) "" else config.displayType.format(value, config.decimalPlaces, useHours)
    }

    /** Maximum number of digits the buffer accepts. Override for feature-specific limits. */
    protected open fun maxDigits(): Int = when {
        useHours -> 6              // HH:MM:SS
        config.displayType.isTime() -> 4  // MM:SS
        else -> minOf(
            config.max.toInt().toString().length + config.decimalPlaces,
            MAX_USER_ENTRY_CHARACTER_LENGTH,
        )
    }

    /** Delay in ms before autocorrect fires after the last digit input. */
    protected open fun autocorrectDelayMs(): Long = AUTOCORRECT_TIMEOUT.toLong() * 1000L

    /** Delay in ms of inactivity before [onDismiss] is called. */
    protected open fun inactivityTimeoutMs(): Long = NUMPAD_TIMEOUT.toLong() * 1000L

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private fun bufferAsFloat(): Float = bufferToFloat(digitBuffer)

    private fun bufferToFloat(buffer: String): Float = when (buffer) {
        ENTRY_MIN -> config.min
        ENTRY_MAX -> config.max
        ENTRY_INITIAL -> initialFloat
        ENTRY_ROUND -> roundTarget
        else -> decodeDigitBuffer(buffer)
    }

    private fun decodeDigitBuffer(digits: String): Float {
        if (config.displayType.isTime()) {
            var hours = 0f
            var minutes = 0f
            var seconds = 0f
            when (digits.length) {
                1, 2 -> seconds = digits.toFloat()
                3 -> {
                    minutes = digits.dropLast(2).toFloat()
                    seconds = digits.drop(1).toFloat()
                }
                4 -> {
                    minutes = digits.dropLast(2).toFloat()
                    seconds = digits.drop(2).toFloat()
                }
                5 -> {
                    hours = digits.dropLast(4).toFloat()
                    minutes = digits.dropLast(2).drop(1).toFloat()
                    seconds = digits.drop(3).toFloat()
                }
                6 -> {
                    hours = digits.dropLast(4).toFloat()
                    minutes = digits.dropLast(2).drop(2).toFloat()
                    seconds = digits.drop(4).toFloat()
                }
            }
            val totalSeconds = (hours * 60 * 60) + (minutes * 60) + seconds
            return if (config.displayType == NumPadDisplay.TimeValueMinutes) totalSeconds / 60 else totalSeconds
        } else {
            var str = digits
            if (config.decimalPlaces > 0) {
                str = if (digits.length <= config.decimalPlaces) {
                    val padding = "0".repeat(config.decimalPlaces - digits.length)
                    "0.$padding$digits"
                } else {
                    digits.dropLast(config.decimalPlaces) + "." +
                        digits.drop(digits.length - config.decimalPlaces)
                }
            }
            if (str.isEmpty()) str = if (config.displayZeroForNoInput) "0" else "-1"
            return str.toFloat()
        }
    }

    private fun refreshState() {
        val value = bufferAsFloat()
        val outOfRange = value !in config.min..config.max
        val scaledValue = if (config.decimalPlaces == 0) value.toInt()
            else (value * 10 * config.decimalPlaces).toInt()
        val scaledResolution = if (config.decimalPlaces == 0) config.stepValue.toInt()
            else (config.stepValue * 10 * config.decimalPlaces).toInt()
        val acceptableStep = !outOfRange && (scaledResolution == 0 || scaledValue % scaledResolution == 0)
        val canSubmit = !(outOfRange || (config.forceStepValues && !acceptableStep))

        val displayStr = when {
            digitBuffer.isDigitsOnly() -> formatDisplay(digitBuffer)
            else -> {
                val v = bufferAsFloat()
                if (v < 0f) "" else config.displayType.format(v, config.decimalPlaces, useHours)
            }
        }

        _state.value = _state.value.copy(
            displayValue = displayStr,
            isConfirmEnabled = canSubmit,
            percentageFraction = if (config.usePercentageBar && config.max != config.min) {
                ((value - config.min) / (config.max - config.min)).coerceIn(0f, 1f)
            } else _state.value.percentageFraction,
            valueFloat = value,
        )
    }

    private fun scheduleAutocorrect() {
        autocorrectJob = scope.launch {
            delay(autocorrectDelayMs())
            if (digitBuffer == ENTRY_INITIAL || config.displayType == NumPadDisplay.Passcode) return@launch
            val value = bufferAsFloat()
            val outOfRange = value !in config.min..config.max
            val scaledValue = if (config.decimalPlaces == 0) value.toInt()
                else (value * 10 * config.decimalPlaces).toInt()
            val scaledResolution = if (config.decimalPlaces == 0) config.stepValue.toInt()
                else (config.stepValue * 10 * config.decimalPlaces).toInt()
            val acceptableStep = !outOfRange && (scaledResolution == 0 || scaledValue % scaledResolution == 0)
            when {
                outOfRange -> {
                    digitBuffer = if (value > config.max) ENTRY_MAX else ENTRY_MIN
                    triggerAnimation()
                }
                config.forceStepValues && !acceptableStep -> {
                    val diff = value % config.stepValue
                    roundTarget = if (diff >= config.stepValue * 0.5f) {
                        value + (config.stepValue - diff)
                    } else {
                        value - diff
                    }
                    digitBuffer = ENTRY_ROUND
                    triggerAnimation()
                }
            }
            refreshState()
        }
    }

    private fun triggerAnimation() {
        _state.value = _state.value.copy(isAnimating = true)
        scope.launch {
            delay((ANIMATION_TIME * ANIMATION_ITERATIONS).toLong())
            _state.value = _state.value.copy(isAnimating = false)
        }
    }

    private fun resetInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = scope.launch {
            delay(inactivityTimeoutMs())
            onDismiss()
        }
    }
}

private fun String.isDigitsOnly(): Boolean = all { it.isDigit() }
