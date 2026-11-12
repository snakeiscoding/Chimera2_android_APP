package com.jht.seca2

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.resources.FontFamilyKeys
import com.jht.seca2.ui.SecaColors
import com.jht.theme.DynamicBorderSize
import com.jht.theme.DynamicBorderStyle
import com.jht.theme.DynamicColors
import com.jht.theme.DynamicCornerRect
import com.jht.theme.DynamicCornerSize
import com.jht.theme.DynamicImages
import com.jht.theme.DynamicPadding
import com.jht.theme.DynamicTextSize
import com.jht.theme.DynamicThemeData
import com.jht.ui.ConsoleSizeClass

const val secaDesignWidth = 1920f

val secaBasicImages = DynamicImages() // default image values are none

val secaCornerRadiusMap = mapOf(
    /*    Pair(ConsoleSizeClass.W1024, DynamicCornerSize(
            large = DynamicCornerRect(12.dp),
            medium = DynamicCornerRect(10.dp),
            small = DynamicCornerRect(10.dp),
        )),*/
    Pair(ConsoleSizeClass.W1366, DynamicCornerSize(
        large = DynamicCornerRect(15.dp),
        medium = DynamicCornerRect(14.dp),
        small = DynamicCornerRect(10.dp),
    )),
    Pair(ConsoleSizeClass.FHD, DynamicCornerSize(
        large = DynamicCornerRect(20.dp),
        medium = DynamicCornerRect(18.dp),
        small = DynamicCornerRect(12.dp),
    )),
    /*    Pair(ConsoleSizeClass.UHD, DynamicCornerSize(
            large = DynamicCornerRect(30.dp),
            medium = DynamicCornerRect(20.dp),
            small = DynamicCornerRect(16.dp),
        ))*/
)

val secaBorderMap = mapOf(
    /*    Pair(
            ConsoleSizeClass.W1024, DynamicBorderSize(
                large = DynamicBorderStyle(2.dp),
                medium = DynamicBorderStyle(1.dp),
                small = DynamicBorderStyle(1.dp),
                pillbox = DynamicBorderStyle(2.dp),
            )
        ),*/
    Pair(
        ConsoleSizeClass.FHD, DynamicBorderSize(
            large = DynamicBorderStyle(3.dp),
            medium = DynamicBorderStyle(2.dp),
            small = DynamicBorderStyle(1.dp),
            pillbox = DynamicBorderStyle(4.dp),
        )
    ),
    /*    Pair(
            ConsoleSizeClass.UHD, DynamicBorderSize(
                large = DynamicBorderStyle(4.dp),
                medium = DynamicBorderStyle(3.dp),
                small = DynamicBorderStyle(2.dp),
                pillbox = DynamicBorderStyle(5.dp),
            )
        ),*/
)

private val secaPaddingMap = mapOf(
    // SECA 1920 (default) in Zeplin
    Pair(ConsoleSizeClass.FHD, DynamicPadding(
        spacing0 = 0.dp,
        spacing1 = 4.dp,
        spacing2 = 8.dp,
        spacing3 = 10.dp,
        spacing4 = 12.dp,
        spacing5 = 16.dp,
        spacing6 = 20.dp,
        spacing7 = 24.dp,
        spacing8 = 32.dp,
        spacing9 = 40.dp,
        spacing10 = 48.dp,
        spacing11 = 64.dp,
        spacing12 = 80.dp,
        spacing13 = 160.dp,
    )),
    Pair(ConsoleSizeClass.W1366, DynamicPadding(
        spacing0 = 0.dp,
        spacing1 = 3.dp,
        spacing2 = 6.dp,
        spacing3 = 8.dp,
        spacing4 = 10.dp,
        spacing5 = 12.dp,
        spacing6 = 16.dp,
        spacing7 = 18.dp,
        spacing8 = 24.dp,
        spacing9 = 30.dp,
        spacing10 = 36.dp,
        spacing11 = 48.dp,
        spacing12 = 60.dp,
        spacing13 = 120.dp,
    )),
)

// for basic we have little to no rounded corners
val secaRadiusMap = mapOf(
    /*Pair(ConsoleSizeClass.W1024, DynamicCornerSize()),*/
    Pair(ConsoleSizeClass.W1366, DynamicCornerSize()),
    Pair(ConsoleSizeClass.FHD, DynamicCornerSize()),
    /*Pair(ConsoleSizeClass.UHD, DynamicCornerSize()),*/
)

val secaDynamicColors = DynamicColors(
    pillboxButtonForeground = Color.Black,
)

val secaFontSizeMap = mapOf(
    /*    Pair(
            ConsoleSizeClass.W1024, DynamicTextSize(
                size6xl = 34.sp,
                size5xl = 30.sp,
                size4xl = 26.sp,
                size3xl = 24.sp,
                size2xl = 22.sp,
                size1xl = 20.sp,
                sizeLarge = 18.sp,
                sizeMedium = 17.sp,
                sizeSmall = 16.sp,
                size1xs = 15.sp,
                size2xs = 14.sp,
                size3xs = 14.sp,
            )),*/
    Pair(
        ConsoleSizeClass.W1366, DynamicTextSize(
            size6xl = 42.sp,
            size5xl = 38.sp,
            size4xl = 34.sp,
            size3xl = 30.sp,
            size2xl = 28.sp,
            size1xl = 26.sp,
            sizeLarge = 24.sp,
            sizeMedium = 22.sp,
            sizeSmall = 20.sp,
            size1xs = 18.sp,
            size2xs = 16.sp,
            size3xs = 14.sp,
        )),
    Pair(
        ConsoleSizeClass.FHD, DynamicTextSize(
            size6xl = 54.sp,
            size5xl = 50.sp,
            size4xl = 48.sp,
            size3xl = 42.sp,
            size2xl = 32.sp,
            size1xl = 28.sp,
            sizeLarge = 26.sp,
            sizeMedium = 24.sp,
            sizeSmall = 22.sp,
            size1xs = 20.sp,
            size2xs = 18.sp,
            size3xs = 16.sp,
        )),
    /*    Pair(
            ConsoleSizeClass.UHD, DynamicTextSize(
                size6xl = 70.sp,
                size5xl = 66.sp,
                size4xl = 58.sp,
                size3xl = 46.sp,
                size2xl = 42.sp,
                size1xl = 40.sp,
                sizeLarge = 36.sp,
                sizeMedium = 30.sp,
                sizeSmall = 28.sp,
                size1xs = 26.sp,
                size2xs = 22.sp,
                size3xs = 18.sp,
            ))*/
)

val secaLineHeightMap = mapOf(
    /*Pair(
        ConsoleSizeClass.W1024, DynamicTextSize(
            size6xl = 36.sp,
            size5xl = 32.sp,
            size4xl = 28.sp,
            size3xl = 32.sp,
            size2xl = 30.sp,
            size1xl = 28.sp,
            sizeLarge = 26.sp,
            sizeMedium = 25.sp,
            sizeSmall = 24.sp,
            size1xs = 23.sp,
            size2xs = 22.sp,
            size3xs = 22.sp,
        )),*/
    Pair(
        ConsoleSizeClass.W1366, DynamicTextSize(
            size6xl = 44.sp,
            size5xl = 40.sp,
            size4xl = 36.sp,
            size3xl = 38.sp,
            size2xl = 36.sp,
            size1xl = 34.sp,
            sizeLarge = 32.sp,
            sizeMedium = 30.sp,
            sizeSmall = 28.sp,
            size1xs = 26.sp,
            size2xs = 24.sp,
            size3xs = 22.sp,
        )),
    Pair(
        ConsoleSizeClass.FHD, DynamicTextSize(
            size6xl = 56.sp,
            size5xl = 52.sp,
            size4xl = 50.sp,
            size3xl = 50.sp,
            size2xl = 40.sp,
            size1xl = 36.sp,
            sizeLarge = 34.sp,
            sizeMedium = 32.sp,
            sizeSmall = 30.sp,
            size1xs = 28.sp,
            size2xs = 26.sp,
            size3xs = 24.sp,
        )),
    /*Pair(
        ConsoleSizeClass.UHD, DynamicTextSize(
            size6xl = 72.sp,
            size5xl = 68.sp,
            size4xl = 60.sp,
            size3xl = 54.sp,
            size2xl = 50.sp,
            size1xl = 48.sp,
            sizeLarge = 44.sp,
            sizeMedium = 38.sp,
            sizeSmall = 36.sp,
            size1xs = 34.sp,
            size2xs = 30.sp,
            size3xs = 26.sp,
        ))*/
)

/** Theme added for SECA integration */
val secaUiTheme = DynamicThemeData(
    // be careful with colors in here since it can be fussy with native widgets like the date picker dialog
    DynamicColors(
        primary = SecaColors.primaryRed,
        onPrimary = SecaColors.white,
        background1 = SecaColors.white,
        text1 = Color.Unspecified //to allow for color overrides to work correctly in widgets like date picker dialog
    ),
    secaCornerRadiusMap,
    secaBorderMap,
    secaPaddingMap,
    FontFamilyKeys.roboto,
    secaFontSizeMap,
    secaLineHeightMap,
    secaBasicImages
)

/** Theme added for SECA integration */
val paceUiTheme = DynamicThemeData(
    // be careful with colors in here since it can be fussy with native widgets like the date picker dialog
    DynamicColors(
        primary = SecaColors.primaryRed,
        onPrimary = SecaColors.black,
        background1 = SecaColors.black,
        text1 = Color.Unspecified //to allow for color overrides to work correctly in widgets like date picker dialog
    ),
    secaCornerRadiusMap,
    secaBorderMap,
    secaPaddingMap,
    FontFamilyKeys.roboto,
    secaFontSizeMap,
    secaLineHeightMap,
    secaBasicImages
)