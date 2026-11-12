package com.jht.sprint8.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.sprint8.utils.Sprint8Colors.ICON_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.TEXT_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.TEXT_FIELD_BACKGROUND_ERROR
import com.jht.sprint8.utils.Sprint8Colors.TEXT_FIELD_BACKGROUND_ERROR_STROKE
import com.jht.theme.DynamicTheme
import com.jht.ui.computeSize

import com.jht.widgets.numpad.NumPadStyle

class Sprint8NumPadStyle(
    background: Color,
    enterBackground: Color,
    enterForeground: Color,
    enterDisabledBackground: Color,
    enterDisabledForeground: Color,
    buttonBackground: Color,
    buttonTextStyle: TextStyle,
    valueBackground: Color,
    valueTextStyle: TextStyle,
    valueBar: Color,
    popUpStroke: Dp,
    numberButtonStroke: Dp,
    enterButtonStroke: Dp,
    deleteButtonBackground: Color,
    deleteButtonStroke: Dp,
    valueTextPanelStroke: Dp,
    valueTextPanelBackground: Color,
    deleteIcon: Int,
    percentageBarWidth: Dp,
    buttonShape: Shape = RoundedCornerShape(6.dp),
    containerShape: Shape = RoundedCornerShape(6.dp),
    containerPadding: PaddingValues = PaddingValues(5.dp),
) : NumPadStyle(
    background,
    enterBackground,
    enterForeground,
    enterDisabledBackground,
    enterDisabledForeground,
    buttonBackground,
    buttonTextStyle,
    valueBackground,
    valueTextStyle,
    valueBar,
    popUpStroke,
    numberButtonStroke,
    enterButtonStroke,
    deleteButtonBackground,
    deleteButtonStroke,
    valueTextPanelStroke,
    valueTextPanelBackground,
    deleteIcon,
    percentageBarWidth,
    buttonShape = buttonShape,
    containerShape = containerShape,
    containerPadding = containerPadding,
) {
    /** Singleton of [NumPadEntry] defaults. Inspired by material ButtonDefaults. */
    object Sprint8NumPadPopUpDefaults {
        /** Copy construction of [NumPadPopUpEntryStyle] using the default values as a source. */
        @Composable
        fun style(
            background: Color = DynamicTheme.colors.keypadNumberBackground,
            enterBackground: Color = DynamicTheme.colors.primary,
            enterForeground: Color = DynamicTheme.colors.gray1,
            enterDisabledBackground: Color = DynamicTheme.colors.gray6,
            enterDisabledForeground: Color = DynamicTheme.colors.gray3,
            buttonBackground: Color = TEXT_COLOR_1,
            buttonTextStyle: TextStyle = MaterialTheme.typography.labelMedium.copy(color = DynamicTheme.colors.keypadNumberForeground, fontSize = 40.sp.computeSize()),
            valueBackground: Color = DynamicTheme.colors.keypadValueBackground,
            valueTextStyle: TextStyle = MaterialTheme.typography.labelMedium.copy(color = DynamicTheme.colors.keypadValueForeground, fontSize = 42.sp),
            valueBar: Color = DynamicTheme.colors.primary,
            popUpStroke: Dp = 6.dp,
            numberButtonStroke: Dp = 6.dp,
            enterButtonStroke: Dp = 6.dp,
            deleteButtonBackground: Color =  TEXT_COLOR_1,
            deleteButtonStroke: Dp = 6.dp,
            valueTextPanelStroke: Dp = 6.dp,
            valueTextPanelBackground: Color =  TEXT_COLOR_1,
            deleteIcon: Int = R.drawable.backspace_new_bold,
            percentageBarWidth: Dp = 8.dp,
        ): NumPadStyle = Sprint8NumPadStyle(
            popUpStroke = popUpStroke,
            numberButtonStroke = numberButtonStroke,
            enterButtonStroke = enterButtonStroke,
            deleteButtonBackground = deleteButtonBackground,
            deleteButtonStroke = deleteButtonStroke,
            valueTextPanelStroke = valueTextPanelStroke,
            valueTextPanelBackground = valueTextPanelBackground,
            deleteIcon = deleteIcon,
            percentageBarWidth = percentageBarWidth,
            background = background,
            enterBackground = enterBackground,
            enterForeground = enterForeground,
            enterDisabledBackground = enterDisabledBackground,
            enterDisabledForeground = enterDisabledForeground,
            buttonBackground = buttonBackground,
            buttonTextStyle = buttonTextStyle,
            valueBackground = valueBackground,
            valueTextStyle = valueTextStyle,
            valueBar = valueBar,
            buttonShape = RoundedCornerShape(6.dp.computeSize()),
            containerShape = RoundedCornerShape(6.dp.computeSize()),
            containerPadding = PaddingValues(5.dp.computeSize()),
        )
    }

    override fun borderColor(
        userEnteredFloat: Float,
        ableToSubmitValue: Boolean,
        enterTextPanelBorderColorDefault: Color
    ): Color {
        return when {
            userEnteredFloat == 0f -> ICON_COLOR_1
            !ableToSubmitValue -> TEXT_FIELD_BACKGROUND_ERROR_STROKE
            else -> enterTextPanelBorderColorDefault
        }
    }

    override fun backgroundColor(
        userEnteredFloat: Float,
        ableToSubmitValue: Boolean,
    ): Color {
        return when {
            userEnteredFloat == 0f -> TEXT_COLOR_1
            !ableToSubmitValue -> TEXT_FIELD_BACKGROUND_ERROR
            else -> valueBackground
        }
    }

    override fun percentageBarColor(
        ableToSubmitValue: Boolean,
        percentageBarColorDefault: Color
    ): Color {
        return if (!ableToSubmitValue) {
            TEXT_FIELD_BACKGROUND_ERROR
        } else {
            percentageBarColorDefault
        }
    }

}