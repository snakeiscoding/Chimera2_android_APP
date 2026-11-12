package com.jht.applibrary

import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.data.AppInfo
import com.jht.data.AppInfoCategory
import com.jht.theme.DynamicTheme
import com.jht.theme.DynamicThemeData
import com.jht.theme.darkDynamicTheme
import com.jht.theme.defaultDynamicTheme
import com.jht.theme.lightDynamicTheme
import com.jht.theme.ThemeButton
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize
import com.jht.widgets.MessageDialog
import com.jht.widgets.PopupFullscreen

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun AppLibraryScreen(
    appInfoList: List<AppInfo>,
    theme: DynamicThemeData = defaultDynamicTheme,
    ssoPrompt: MutableState<AppInfo?> = mutableStateOf(null),
    tunerConnected: Boolean = true,
    showTunerInitializingDialog: Boolean = false,
    onDismissTunerInitializingDialog: () -> Unit = {},
    onAppClick: (AppInfo) -> Unit = {},
    onForceLaunch: (AppInfo) -> Unit = {},
) {
    val verticalGradient = Brush.verticalGradient(
        listOf(Color(0xFF393939), Color(0xFF191919), Color(0xFF191919)),
        startY = -150.0f,
        endY = 500.0f
    )
    val upgradePrompt = remember(appInfoList) { mutableStateOf(false) }

    DynamicTheme(theme) {
        Box(
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .fillMaxSize()
                .background(verticalGradient),
        ) {
            AppLibraryView(
                appInfoPage = appInfoList,
                tunerConnected = tunerConnected,
            ) {
                if (it.locked) upgradePrompt.value = true
                else onAppClick(it)
            }
        }
        ssoPrompt.value?.let { app ->
            MessageDialog(
                title = "",
                message = stringResource(R.string.sync_pg).replace("app_name", app.title),
                modifier = Modifier
                    .fillMaxWidth(0.33f)
                    .aspectRatio(1.8f),
                okText = stringResource(R.string.yes_tc),
                cancelText = stringResource(R.string.no_tc),
            ) { confirm ->
                ssoPrompt.value = null
                if (confirm) onForceLaunch(app)
            }
        }
        if (showTunerInitializingDialog) {
            PopupFullscreen(
                onDismissRequest = onDismissTunerInitializingDialog,
                properties = PopupProperties(focusable = true, clippingEnabled = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickableNoEffect(onClick = onDismissTunerInitializingDialog),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .background(DynamicTheme.colors.gray1)
                            .clickableNoEffect { }
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.initializing_please_wait_pg),
                            color = DynamicTheme.colors.text1,
                        )
                        ThemeButton(
                            onClick = onDismissTunerInitializingDialog,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DynamicTheme.colors.secondary,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(
                                text = stringResource(android.R.string.ok),
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
        if (upgradePrompt.value) MessageDialog(
            title = stringResource(R.string.upgrade_unavailable_tc),
            message = stringResource(R.string.upgrade_unavailable_pg),
            modifier = Modifier
                .fillMaxWidth(0.33f)
                .aspectRatio(1.8f),
            onDismiss = { upgradePrompt.value = false },
        )
    }
}


@Composable
fun AppLibraryView(
    appInfoPage: List<AppInfo>,
    tunerConnected: Boolean = true,
    onAppClick: (AppInfo) -> Unit
) {
    // do an adjusted pad or use a max size?
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = screenHeight * 0.05f)
    ) {
        AppInfoRowComponent(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.33f),
            categoryTitle = stringResource(R.string.matrix_workout_tc),
            appInfoPage = appInfoPage,
            onAppClick = onAppClick,
            category = AppInfoCategory.Workout,
            screenWidth = screenWidth,
            tunerConnected = tunerConnected,
        )
        AppInfoRowComponent(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.33f),
            categoryTitle = stringResource(R.string.entertainment_tc),
            appInfoPage = appInfoPage,
            onAppClick = onAppClick,
            category = AppInfoCategory.Entertainment,
            screenWidth = screenWidth,
            tunerConnected = tunerConnected,
        )
        AppInfoRowComponent(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.33f),
            categoryTitle = stringResource(R.string.utility_pg),
            appInfoPage = appInfoPage,
            onAppClick = onAppClick,
            category = AppInfoCategory.System,
            screenWidth = screenWidth,
            tunerConnected = tunerConnected,
        )

    }
}


@Composable
fun AppInfoRowComponent(
    modifier: Modifier,
    categoryTitle: String,
    appInfoPage: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    category: AppInfoCategory,
    screenWidth: Dp,
    tunerConnected: Boolean = true,
) {
    val iconPad =  screenWidth * 0.008f
    val aspectRatio = 0.75f
    val startPadding = screenWidth * 0.056f
    val listItemPadding = screenWidth * 0.048f
    val topPadding =  screenWidth * 0.016f
    val imageCornerRadius = 32.dp //screenWidth * 0.024f
    Box(
        modifier = modifier
    ) {
        Text(
            categoryTitle,
            fontSize = 16.sp.computeSize(),
            color = DynamicTheme.colors.gray15,
            style = DynamicTheme.typography.textStyle1,
            modifier = Modifier
                .padding(start = startPadding)
                .wrapContentSize()
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .testTag("AppLibrary:AppRow:${category.name}"),
            contentPadding = PaddingValues(horizontal = listItemPadding)
        ) {
            item {
                appInfoPage.forEach {
                    if (it.category == category) {
                        AppInfoRowItem(
                            Modifier
                                .aspectRatio(aspectRatio)
                                .padding(iconPad),
                            it,
                            imageCornerRadius = imageCornerRadius,
                            tunerConnected = tunerConnected,
                            onAppClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppInfoRowItem(
    modifier: Modifier,
    gridItem: AppInfo,
    imageCornerRadius : Dp,
    tunerConnected: Boolean = true,
    onAppClick: (AppInfo) -> Unit,
) {
    val isTvNotReady = gridItem.tunerDependent && !tunerConnected
    AppButton(
        title = gridItem.title,
        icon = gridItem.icon,
        modifier = modifier.alpha(if (isTvNotReady) 0.4f else 1f),
        testID = gridItem.uuid,
        locked = gridItem.locked,
        imageCornerRadius = imageCornerRadius
    ) {
        onAppClick(gridItem)
    }
}


//region PREVIEWS
@Composable
private fun dummyDataWorkout() = List(15) {
    val icon = AppCompatResources.getDrawable(LocalContext.current, R.drawable.ic_app_button_x)
    AppInfo(
        title = "Twitter",
        uuid = "",
        icon = icon!!,
        category = AppInfoCategory.Workout
    )
}
@Composable
private fun dummyDataEntertainment() = List(15) {
    val icon = AppCompatResources.getDrawable(LocalContext.current, R.drawable.ic_app_button_x)
    AppInfo(
        title = "Twitter",
        uuid = "",
        icon = icon!!,
        category = AppInfoCategory.Entertainment
    )
}

@Composable
private fun dummyDataUtility() = List(15) {
    val icon = AppCompatResources.getDrawable(LocalContext.current, R.drawable.ic_app_button_x)
    AppInfo(
        title = "Twitter",
        uuid = "",
        icon = icon!!,
        category = AppInfoCategory.System
    )
}


@AllDevicePreviews
@Composable
private fun PreviewAppLibraryScreenDark() {
    AppLibraryScreen(
        listOf(
            dummyDataWorkout(),
            dummyDataEntertainment(),
            dummyDataUtility()).flatten(),
        theme = darkDynamicTheme,
    )
}

@AllDevicePreviews
@Composable
private fun PreviewAppLibraryScreenLight() {
    AppLibraryScreen(
        listOf(
            dummyDataWorkout(),
            dummyDataEntertainment(),
            dummyDataUtility()).flatten(),
        theme = lightDynamicTheme,
    )
}


//endregion PREVIEWS
