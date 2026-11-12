package com.jht.widgets.numpad

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.theme.DynamicTheme
import com.jht.ui.computeSize
import com.jht.widgets.R

/** A class containing style parameters for [com.jht.widgets.NumPadEntry]. */
@Immutable
open class NumPadStyle(
    /** Background of whole widget */
    val background: Color,
    /** Styles the background of the enter button */
    val enterBackground: Color,
    /** Styles the tint of the enter button icons */
    val enterForeground: Color,
    /** Styles the background of the enter button when disabled */
    val enterDisabledBackground: Color,
    /** Styles the tint of the enter button icons when disabled */
    val enterDisabledForeground: Color,
    /** Background of number buttons */
    val buttonBackground: Color,
    /** Styles the button text */
    val buttonTextStyle: TextStyle,
    /** Styles the background of the value area */
    val valueBackground: Color,
    /** Styles the value text */
    val valueTextStyle: TextStyle,
    /** Styles the percentage bar, if enabled */
    val valueBar: Color,
    /** Stroke of whole widget */
    val popUpStroke: Dp,
    /** Stroke of number button widget */
    val numberButtonStroke: Dp,
    /** Stroke of enter button widget */
    val enterButtonStroke: Dp,
    /** Background of delete button widget */
    val deleteButtonBackground: Color,
    /** Stroke of delete button widget */
    val deleteButtonStroke: Dp,
    /** Stroke of enter text panel widget */
    val valueTextPanelStroke: Dp,
    /** Background of enter text panel widget */
    val valueTextPanelBackground: Color,
    /** Delete icon of keypad */
    val deleteIcon: Int,
    /** Percentage bar width */
    val percentageBarWidth: Dp,
    /** Shape applied to number, confirm, and delete buttons */
    val buttonShape: Shape = RectangleShape,
    /** Border stroke applied to number, confirm, and delete buttons */
    val buttonBorderStroke: BorderStroke = BorderStroke(0.dp, Color.Transparent),
    /** Shape of the outer numpad container */
    val containerShape: Shape = RectangleShape,
    /** Padding inside the outer numpad container */
    val containerPadding: PaddingValues = PaddingValues(5.dp),
) {
    /** Returns border color based on input value & validity; currently always transparent. */
    open fun borderColor(
        userEnteredFloat: Float,
        ableToSubmitValue: Boolean,
        enterTextPanelBorderColorDefault: Color,
    ): Color = Color.Transparent

    /** Returns background color for the panel; currently always default style background. */
    open fun backgroundColor(
        userEnteredFloat: Float,
        ableToSubmitValue: Boolean,
    ): Color = valueTextPanelBackground

    /** Returns percentage bar color; currently always uses the provided default. */
    open fun percentageBarColor(
        ableToSubmitValue: Boolean,
        percentageBarColorDefault: Color,
    ): Color = percentageBarColorDefault
}

/** Singleton of [com.jht.widgets.NumPadEntry] defaults. Inspired by material ButtonDefaults. */
object NumPadDefaults {
    /** Copy construction of [NumPadStyle] using the default values as a source. */
    @Composable
    fun style(
        background: Color = DynamicTheme.colors.keypadBackground,
        enterBackground: Color = DynamicTheme.colors.primary,
        enterForeground: Color = DynamicTheme.colors.gray1,
        enterDisabledBackground: Color = DynamicTheme.colors.gray6,
        enterDisabledForeground: Color = DynamicTheme.colors.gray3,
        buttonBackground: Color = DynamicTheme.colors.keypadNumberBackground,
        buttonTextStyle: TextStyle = MaterialTheme.typography.labelMedium.copy(color = DynamicTheme.colors.keypadNumberForeground, fontSize = 40.sp.computeSize()),
        valueBackground: Color = DynamicTheme.colors.keypadValueBackground,
        valueTextStyle: TextStyle = MaterialTheme.typography.labelMedium.copy(color = DynamicTheme.colors.keypadValueForeground, fontSize = 50.sp.computeSize()),
        valueBar: Color = DynamicTheme.colors.primary,
        popUpStroke: Dp = 0.dp,
        numberButtonStroke: Dp = 0.dp,
        enterButtonStroke: Dp = 0.dp,
        deleteButtonBackground: Color = DynamicTheme.colors.keypadNumberBackground,
        deleteButtonStroke: Dp = 0.dp,
        valueTextPanelStroke: Dp = 0.dp,
        valueTextPanelBackground: Color = DynamicTheme.colors.keypadNumberBackground,
        deleteIcon: Int = R.drawable.ic_delete,
        percentageBarWidth: Dp = 10.dp,
        buttonShape: Shape = RectangleShape,
        buttonBorderStroke: BorderStroke = BorderStroke(0.dp, Color.Transparent),
        containerShape: Shape = RectangleShape,
        containerPadding: PaddingValues = PaddingValues(5.dp.computeSize()),
    ): NumPadStyle = NumPadStyle(
        background, enterBackground, enterForeground,
        enterDisabledBackground, enterDisabledForeground,
        buttonBackground, buttonTextStyle,
        valueBackground, valueTextStyle, valueBar,
        popUpStroke, numberButtonStroke, enterButtonStroke,
        deleteButtonBackground, deleteButtonStroke,
        valueTextPanelStroke, valueTextPanelBackground,
        deleteIcon, percentageBarWidth,
        buttonShape, buttonBorderStroke, containerShape, containerPadding,
    )
}
