package com.jht.sprint8.utils

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Holds constant values for the colors used across different screens in the Sprint8.
 *
 * This object centralizes all UI color definitions to ensure consistent styling
 * across the app and to make it easy to update colors in one place.
 */

internal object Sprint8Colors {
    // Warmup
    val WARMUP_BACKGROUND_COLOR = Color(0xFFF0F0BD)
    val WARMUP_ACTIVE_BAR_COLOR = Color(0xFFA5A522)
    val WARMUP_PROGRESSION_BAR_COLOR = Color(0xFF48480F)
    val WARMUP_ICON_BORDER_COLOR = Color(0xFFCECE2A)
    val WARMUP_PAUSE_ICON_COLOR = Color(0xFF48480F)
    val WARMUP_HEART_RATE_RPM_COLOR = Color(0xFF7c7c19)
    val WARMUP_HEART_RATE_RPM_BORDER_COLORS = listOf(Color(0x4cd2cd48), Color(0xFF67670c))

    // Sprint
    val SPRINT_BACKGROUND_COLOR = Color(0xFFFDE0BB)
    val SPRINT_ACTIVE_BAR_COLOR = Color(0xFFBB751C)
    val SPRINT_PROGRESSION_BAR_COLOR = Color(0xFF57370D)
    val SPRINT_ICON_BORDER_COLOR = Color(0xFFF99C25)
    val SPRINT_PAUSE_ICON_COLOR = Color(0xFF57370D)
    val SPRINT_HEART_RATE_RPM_COLOR = Color(0xFFbb751c)
    val SPRINT_HEART_RATE_RPM_BORDER_COLORS = listOf(Color(0xFFffa129), Color(0xFF7a4400))


    // Recovery
    val RECOVERY_BACKGROUND_COLOR = Color(0xFFD1E9C5)
    val RECOVERY_ACTIVE_BAR_COLOR = Color(0xFF508B34)
    val RECOVERY_PROGRESSION_BAR_COLOR = Color(0xFF254118)
    val RECOVERY_ICON_BORDER_COLOR = Color(0xFF6AB945)
    val RECOVERY_PAUSE_ICON_COLOR = Color(0xFF254118)
    val RECOVERY_HEART_RATE_RPM_COLOR = Color(0xFF508b34)
    val RECOVERY_HEART_RATE_RPM_BORDER_COLORS = listOf(Color(0xFF62ce30), Color(0xFF377d17))


    // Cooldown
    val COOLDOWN_BACKGROUND_COLOR = Color(0xFFB0E0EB)
    val COOLDOWN_ACTIVE_BAR_COLOR = Color(0xFF01748F)
    val COOLDOWN_PROGRESSION_BAR_COLOR = Color(0xFF003643)
    val COOLDOWN_CLOCK_ICON_COLOR = Color(0xFF019ABE)
    val COOLDOWN_PAUSE_ICON_COLOR = Color(0xFF003643)
    val COOLDOWN_HEART_RATE_RPM_COLOR = Color(0xFF01748f)
    val COOLDOWN_HEART_RATE_RPM_BORDER_COLORS = listOf(Color(0xFF019abe), Color(0xFF004758))


    val TEXT_COLOR_1 = Color(0xFF2B2B2B)
    val SUMMARY_GRAPH_BAR_COLOR_1 = Color(0xFFffbc42)
    val SUMMARY_GRAPH_BAR_COLOR_2 = Color(0xFF11c3ff)
    val SUMMARY_SCORE_COLOR = Color(0xFFc5c5c5)
    val ICON_COLOR_1 = Color(0xFF525252)

//New Sprint8

    val SPRINT8_NEW_WARM_UP_COLOR = Color(0xFFeaff38)
    val SPRINT8_NEW_SPRINT_COLOR = Color(0xFFffb224)
    val SPRINT8_NEW_ACTIVE_RECOVERY_COLOR = Color(0xFFa0ff61)
    val SPRINT8_COOLDOWN_COLOR = Color(0xFF48d1ff)
    val SPRINT8_NEW_LIGHT_BLUE = Color(0xFFcfefff)
    val SPRINT8_NEW_LIGHT_YELLOW = Color(0xFFecff4a)
    val SPRINT8_NEW_GET_READY_COLORS = listOf(
        Color(0x00203c45),
        Color(0xFF0f3948),
        Color(0x00203c45)
    )
    val SPRINT8_NEW_DARK_GREY = Color(0xFF191309)
    val SPRINT8_NEW_GREY = Color(0xFF191919)
    val SPRINT8_NEW_WHITE = Color(0xFFffffff)
    val ASSESSMENT_NEW_DARK_GREY = Color(0xFFefefef)
    val ASSESSMENT_WARMUP_COLOR = Color(0xFFe8ff2a)
    val SPRINT8_NEW_RED = Color(0xFFef5a39)
    val SPRINT8_DISABLED_BTN_COLOR = Color(0xFFababac)
    val SPRINT8_BTN_CONTENT_COLOR = Color(0xFFd1d1d1)
    val SPRINT8_BTN_CONTENT_COLOR_2 = Color(0xFFefefef)
    val SPRINT8_DISABLED_BTN_CONTENT_COLOR = Color(0xFF252525)
    val CUSTOM_WORKOUT_LIST_ITEM_COLOR = Color(0xFF1f1f1f)
    val CUSTOM_WORKOUT_LIST_BORDER_COLOR = Color(0xFF4c4c4c)
    val CUSTOM_WORKOUT_SAVE_BTN_COLOR = Color(0xFF393939)
    val CUSTOM_WORKOUT_TEXT_FIELD_BORDER_COLOR = Color(0xFFffcb27)
    val CUSTOM_WORKOUT_START_WORKOUT_BTN_COLOR = Color(0xFFe5e53e)
    val ASSESSMENT_DIALOG_BUTTON_BG_COLOR_1 = Color(0xff37270f)
    val ASSESSMENT_DIALOG_BUTTON_BG_COLOR_2 = Color(0xff09181f)
    val ASSESSMENT_WHITE_COLOR = Color(0xffffffff)
    val ASSESSMENT_LIGHT_WHITE_COLOR = Color(0xfff5f5f5)
    val ASSESSMENT_INTERMEDIATE_COLOR = Color(0xff2dcaef)
    val ASSESSMENT_ELITE_COLOR = Color(0xfff5a640)
    val TOGGLE_BUTTON_BACKGROUND_COLOR = Color(0xff121212)


// Gradients

    val BORDER_GRADIENT = Brush.verticalGradient(listOf(Color(0xb3ffffff), Color(0xffdbd6d6)))

// Border

    val BORDER_GRAY = Color(0xFF666666)
    val BORDER_GRAY_2 = Color(0xFF7f7f7f)

    val WARMUP_BAR_COLOR_1 = Color(0xFFbaba28)
    val WARMUP_BAR_COLOR_2 = Color(0xFF565616)
    val SPRINT_BAR_COLOR_1 = Color(0xFFfe8e00)
    val SPRINT_BAR_COLOR_2 = Color(0xFF985500)
    val COOLDOWN_BAR_COLOR_2 = Color(0xFF004758)

    val SPRINT8_BG_1 = Color(0xFF171718)
    val SPRINT8_BG_2 = Color(0xFFfef5e9)
    val SPRINT8_BG_10 = Color(0xFF335921)
    val SPRINT8_BG_12 = Color(0xFFe24d2c)
    val KEYPAD_SPRINTING_CONFIRM_BTN_COLOR = Color(0xFFffa600)
    val KEYPAD_ACTIVE_RECOVERY_CONFIRM_BTN_COLOR = Color(0xFF8cf048)
    val TEXT_FIELD_BACKGROUND_ERROR = Color(0x33ff432e)
    val TEXT_FIELD_BACKGROUND_ERROR_STROKE = Color(0xFFff432e)

    val MANUAL_SPRINT8_BACKGROUND_COLOR = Color(0xFF171718)
    val MANUAL_SPRINT8_PRESSED_COLOR = Color(0xFF4C4C4C)
    val MANUAL_SPRINT8_DEFAULT = Color(0xFF2B2B2B)
    val MANUAL_SPRINT_SELECTED_BORDER = Color(0xFFF9B332)
    val MANUAL_SPRINT8_SELECTED_START_GRADIENT = Color(0x19F9B332)
    val MANUAL_SPRINT8_SELECTED_END_GRADIENT = Color(0x00F9B332)
    val MANUAL_SPRINT8_TEXT_GRADIENT = Color(0xFF999797)
}
