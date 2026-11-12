package com.jht.sprint8.model

import androidx.compose.runtime.Immutable

/**
 * Configuration data for the "Get Ready" display before an assessment or workout.
 * @property title The text to display in the "Get Ready", "Go Go Go", "Set" message. Defaults to an empty string.
 * @property isVisible Determines whether the "Get Ready" message is currently visible on the screen.
 * Defaults to `false`.
 * @property isBlinking Indicates whether the "Get Ready" message should blink to draw attention.
 * Defaults to `false`.
 */
@Immutable
data class GetReadyConfig(
    val title: String = "",
    val isVisible: Boolean = false,
    val isBlinking: Boolean = false
)
