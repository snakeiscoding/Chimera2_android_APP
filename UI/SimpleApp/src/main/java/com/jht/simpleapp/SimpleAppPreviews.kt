package com.jht.simpleapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import com.jht.theme.darkDynamicTheme
import com.jht.theme.lightDynamicTheme
import com.jht.ui.Device15inPreviews
import com.jht.widgets.helpers.StatInfo
import com.jht.widgets.helpers.StatInfoCollection


@OptIn(ExperimentalComposeUiApi::class)
@Device15inPreviews
@Composable
fun PreviewSimpleAppScreenLightMode() {
    SimpleAppScreen(
        statInfoCollection =  StatInfoCollection.dummyBikeStatInfoCollection(),
//        colors = lightDynamicColors,
        displayColor = darkDynamicTheme.colors.primary,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Device15inPreviews
@Composable
fun PreviewSimpleAppScreenDarkMode() {
    SimpleAppScreen(
        statInfoCollection = StatInfoCollection.dummyBikeStatInfoCollection(),
        theme = darkDynamicTheme,
        displayColor = lightDynamicTheme.colors.primary,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Device15inPreviews
@Composable
fun PreviewSimpleAppScreenHeartRate1Light() {
    SimpleAppScreen(
        statInfoCollection = StatInfoCollection.dummyBikeStatInfoCollection(),
        theme = lightDynamicTheme,
        displayColor = lightDynamicTheme.colors.heart1,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Device15inPreviews
@Composable
fun PreviewSimpleAppScreenHeartRate1Dark() {
    SimpleAppScreen(
        statInfoCollection = StatInfoCollection.dummyBikeStatInfoCollection(),
        theme = darkDynamicTheme,
        displayColor = darkDynamicTheme.colors.heart1,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Device15inPreviews
@Composable
fun PreviewSimpleAppScreenHeartRate2Light() {
    SimpleAppScreen(
        statInfoCollection = StatInfoCollection.dummyBikeStatInfoCollection(),
        theme = lightDynamicTheme,
        displayColor = lightDynamicTheme.colors.heart2,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Device15inPreviews
@Composable
fun PreviewSimpleAppScreenHeartRate2Dark() {
    SimpleAppScreen(
        statInfoCollection = StatInfoCollection.dummyBikeStatInfoCollection(),
        theme = darkDynamicTheme,
        displayColor = darkDynamicTheme.colors.heart2,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Device15inPreviews
@Composable
fun PreviewSimpleAppScreenHeartRate3Light() {
    SimpleAppScreen(
        statInfoCollection = StatInfoCollection.dummyBikeStatInfoCollection(),
        theme = lightDynamicTheme,
        displayColor = lightDynamicTheme.colors.heart3,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Device15inPreviews
@Composable
fun PreviewSimpleAppScreenHeartRate3Dark() {
    SimpleAppScreen(
        statInfoCollection = StatInfoCollection.dummyBikeStatInfoCollection(),
        theme = darkDynamicTheme,
        displayColor = darkDynamicTheme.colors.heart3,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Device15inPreviews
@Composable
fun PreviewSimpleAppScreenHeartRate4Light() {
    SimpleAppScreen(
        statInfoCollection = StatInfoCollection.dummyBikeStatInfoCollection(),
        theme = lightDynamicTheme,
        displayColor = lightDynamicTheme.colors.heart4,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Device15inPreviews
@Composable
fun PreviewSimpleAppScreenHeartRate4Dark() {
    SimpleAppScreen(
        statInfoCollection = StatInfoCollection.dummyBikeStatInfoCollection(),
        theme = darkDynamicTheme,
        displayColor = darkDynamicTheme.colors.heart4,
    )
}


@OptIn(ExperimentalComposeUiApi::class)
@Device15inPreviews
@Composable
fun PreviewSimpleAppScreenHeartRate5Light() {
    SimpleAppScreen(
        statInfoCollection = StatInfoCollection.dummyBikeStatInfoCollection(),
        theme = lightDynamicTheme,
        displayColor = lightDynamicTheme.colors.heart5,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Device15inPreviews
@Composable
fun PreviewSimpleAppScreenHeartRate5Dark() {
    SimpleAppScreen(
        statInfoCollection = StatInfoCollection.dummyBikeStatInfoCollection(),
        theme = darkDynamicTheme,
        displayColor = darkDynamicTheme.colors.heart5,
    )
}


@OptIn(ExperimentalComposeUiApi::class)
@Device15inPreviews
@Composable
fun PreviewDummyStats() {
    // put dummy values to make sure we can populate fields with anything
    val dummyStatListOfList = listOf(
        listOf(
            StatInfo(R.string.guest_tc, android.R.drawable.ic_search_category_default, ":)", R.string.units_tc),
            StatInfo(R.string.abdominal_crunch_tc, R.drawable.ic_heart_white, "99", R.string.units_tc),
        ),
        listOf(
            StatInfo(R.string.samsung_help_pg, android.R.drawable.ic_dialog_info, "1", R.string.units_tc),
            StatInfo(R.string.no_club_id_found_tc, R.drawable.ic_calories_hour_white, "2", R.string.units_tc),
            StatInfo(R.string.wifi_tc, R.drawable.ic_watts_white, "3", R.string.units_tc),
            StatInfo(R.string.host_tc, R.drawable.ic_mets_white, "4", R.string.units_tc),
        ),
        listOf(
            StatInfo(R.string.japanese_tc, android.R.drawable.ic_dialog_map, "A", R.string.units_tc),
            StatInfo(R.string.simplified_chinese_tc, R.drawable.ic_time_remaining_white, "B", R.string.units_tc),
            StatInfo(R.string.korean_tc, R.drawable.ic_time_total_white, "C", R.string.units_tc),
            StatInfo(R.string.clock_uc, R.drawable.ic_heart_white, "D", R.string.units_tc),
            StatInfo(R.string.russian_tc, R.drawable.ic_heart_white, "E", R.string.units_tc),
        ),
//        listOf(
//            StatInfo("abc", R.string.leg_extension_tc, android.R.drawable.ic_lock_silent_mode),
//            StatInfo("def", R.string.apple_help_pg, R.drawable.ic_distance_white),
//        ),
        listOf(
            StatInfo(R.string.japanese_tc, android.R.drawable.ic_dialog_email, "111:0022132132423", R.string.units_tc),
            StatInfo(R.string.simplified_chinese_tc, R.drawable.ic_icon_speed_white, "222", R.string.units_tc),
            StatInfo(R.string.korean_tc, R.drawable.ic_pace_white, "333", R.string.units_tc),
            StatInfo(R.string.spanish_tc, R.drawable.ic_pace_white, "444", R.string.units_tc),
        ),
    )
    SimpleAppScreen(StatInfoCollection.fromListOfList(dummyStatListOfList), displayColor = Color(0xffff0000))
}

@OptIn(ExperimentalComposeUiApi::class)
@Device15inPreviews
@Composable
fun PreviewOneSmallCardEnabled() {
    // put dummy values to make sure we can populate fields with anything
    val dummyStatListOfList = listOf(
        listOf(
            StatInfo(R.string.japanese_tc, android.R.drawable.ic_dialog_email, "111", R.string.units_tc),
            StatInfo(R.string.simplified_chinese_tc, R.drawable.ic_icon_speed_white, "222", R.string.units_tc),
            StatInfo(R.string.korean_tc, R.drawable.ic_pace_white, "333", R.string.units_tc),
            StatInfo(R.string.spanish_tc, R.drawable.ic_pace_white, "444", R.string.units_tc),
        ),
    )
    // TODO Seems like we should enlarge font sizes when in the large card is shown
    SimpleAppScreen(StatInfoCollection.fromListOfList(dummyStatListOfList), displayColor = Color(0xff00ff00))
}
