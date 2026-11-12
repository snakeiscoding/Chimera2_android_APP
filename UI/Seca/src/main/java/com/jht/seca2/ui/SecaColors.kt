package com.jht.seca2.ui

import androidx.compose.ui.graphics.Color

/**
 * Contains colors used in SECA screens
 */
class SecaColors {
    companion object {
        //common colors
        val white = Color(0xFFFFFFFF)
        val black = Color(0xFF000000)
        val darkBlue = Color(0xFF030D2E)
        val secondaryBlue = Color(0xFFD3DDFF)
        val transparent = Color(0x00000000)

        //texts
        val titleText = Color(0xFF535353)
        val bodyText = Color(0xFF464646)
        val disabledText = Color(0x4D000000) //30% opacity black
        val pageIndicatorGray = Color(0x80000000)

        //background colors
        val cardSelectedRed = Color(0xFFF72F42)
        val unpressedButton = Color(0xFFF2F3F5)
        val unpressedButtonBorder = Color(0xFFD9DADC)

        //border colors
        val borderGray = Color(0xFFDADADA)

        val neutralGray0 = Color(0xFFfcfcfc)
        val neutralGray2 = Color(0xfff5f5f5)
        val neutralGray9 = Color(0xFFACABAB)
        val neutralGray14 = Color(0xFF2E2E2E)
        val neutralGray15 = Color(0xFF191919)
        val primaryRed = Color(0xFFf72f42)
        val muscleMassGreen = Color(0xFF00a650)
        val fatMassBlue = Color(0xFF5782f8)
        val fatFreeMassDarkBlue = Color(0xFF283353)
        val checkboxColor = Color(0xff030d2e)
        val declineButton = Color(0xFFFFFFFF)
        val borderColor = Color(0xFF000000)

        val primaryBlue = Color(0xFF030D2E)

        /* dropdown label background color */
        val neutralTertiary = Color(0xFFEDEEF0)
        val dropdownNotWhite = Color(0x05f72f42)
        val text1 = Color(0x05f72f42)
    }
}