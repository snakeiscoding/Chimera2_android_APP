package com.jht.data

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

/*
    File contains classes related to the frame control interfacing
 */


/** Names to identify a [FrameControl] within a list. Location is suggestion based on our classic interface. */
enum class FrameControlLocation {
    Left,
    Right,
} // could go Side0 and Side1? keeping Left/Right for now since test tags for automation currently use that

/**
 * Public contract for a single frame control group (speed, incline,
 * resistance, target HR, target watts, ...).
 */
interface FrameControl {
    /** System designation for this control group.
     * Currently using location based on our classic interface. Different UI may place differently. */
    val location: FrameControlLocation
    // --- Observable UI state ---
    /** Next scripted value label text (if applicable, otherwise empty) */
    val nextValueLabel: MutableState<String>
    /** Next scripted value formatted as string (if applicable, otherwise empty) */
    val nextValue: MutableState<String>
    /** Current value label text */
    val label: MutableState<String>
    /** Current value formatted as string */
    val currentValue: MutableState<String>
    /** Current value units text */
    val unit: MutableState<String>
    /** Previous value should be visible or not */
    val previousVisible: MutableState<Boolean>
    /** Previous value formatted as string */
    val previousValue: MutableState<String>
    /** Previous value label text. Null if none */
    val previousLabel: MutableState<String?>
    /** Value range information */
    val range: MutableState<RangeInfo>
    /** Unit for keypad */
    val keypadUnit: MutableState<String>
    /** Quick selection values */
    val presetValues: MutableState<List<Float>>
    /** Icon ID for stat under control (for example icon for speed, incline, etc) */
    val iconId: MutableIntState

    // --- Interaction callback ---
    /** Callback fired when controls have user interaction. Usually for resetting an idle timer. */
    var onUserInteraction: () -> Unit

    // --- Lifecycle ---
    /** start frame control handling */
    fun start()
    /** stop frame control handling */
    fun stop()

    // --- Queries ---
    /** Get the current value as numeric quantity */
    fun getValueFloat(): Float
    /** Get if user is allowed to make changes */
    fun getUserChangesAllowed(): Boolean

    // --- User interactions ---
    /** Call when last value button is clicked */
    fun onPreviousValueClicked()
    /** Call when commiting new value from keypad */
    fun onKeypadCommit(value: Float)
    /** Call when increment button pressed state changes */
    fun onIncrementPress(up: Boolean)
    /** Call when decrement button pressed state changes */
    fun onDecrementPress(up: Boolean)
    /** Stop any in progress incrementation */
    fun stopIncrementing()
}

/** Class for passing frame controls and related info across common interfacing from system to UI */
class FrameControlCollection(
    val controls: List<FrameControl> = listOf(),
) {
    fun start() = controls.forEach { it.start() }
    fun stop() = controls.forEach { it.stop() }
    fun stopIncrementing() = controls.forEach { it.stopIncrementing() }

    companion object {
        val EMPTY = FrameControlCollection()
    }
}

/** Mock implementation of [FrameControl] for previews */
open class MockFrameControl(
    override val location: FrameControlLocation = FrameControlLocation.Left,
    override val label: MutableState<String> = mutableStateOf("RESISTANCE"),
    override val currentValue: MutableState<String> = mutableStateOf("0.0"),
    override val nextValueLabel: MutableState<String> = mutableStateOf(""),
    override val nextValue: MutableState<String> = mutableStateOf(""),
    override val unit: MutableState<String> = mutableStateOf("MPH"),
    override val previousVisible: MutableState<Boolean> = mutableStateOf(true),
    override val previousValue: MutableState<String> = mutableStateOf("--"),
    override val previousLabel: MutableState<String?> = mutableStateOf(null),
    override val range: MutableState<RangeInfo> = mutableStateOf(RangeInfo()),
    override val keypadUnit: MutableState<String> = mutableStateOf("MPH"),
    override val presetValues: MutableState<List<Float>> = mutableStateOf(listOf(1f, 2f, 3f, 4f, 5f, 6f)), // for quick keys
    override val iconId: MutableIntState = mutableIntStateOf(0),
) : FrameControl {
    override var onUserInteraction: () -> Unit = {}
    override fun start() {}
    override fun stop() {}
    override fun stopIncrementing() {}
    var currentValueFloat = 0f
    override fun getValueFloat(): Float = currentValueFloat
    var allowChanges = true
    override fun getUserChangesAllowed(): Boolean = allowChanges
    override fun onPreviousValueClicked() {}
    override fun onKeypadCommit(value: Float) {}
    override fun onIncrementPress(up: Boolean) {}
    override fun onDecrementPress(up: Boolean) {}
}
