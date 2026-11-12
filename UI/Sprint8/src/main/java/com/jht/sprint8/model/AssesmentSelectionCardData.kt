package com.jht.sprint8.model

import androidx.compose.ui.graphics.Color

/**
 * Represents the data for an assessment level selection card.
 * @property title The name of the assessment level (e.g., "Beginner", "Intermediate",
 * "Advanced", "Elite").
 * @property borderColor The color used for the border of the card to visually differentiate
 * levels or indicate selection.
 */

internal data class AssessmentSelectionCardData(
    val title: String,
    val borderColor: Color
)



