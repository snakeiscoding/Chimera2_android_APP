package com.jht.welcome.chameleon

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.jht.clubcom.ui.ClubComWelcomeUi
import com.jht.data.LoginData
import com.jht.data.LoginType
import com.jht.data.RegisterData
import com.jht.data.UserFieldText
import com.jht.theme.DynamicTheme
import com.jht.theme.DynamicThemeData
import com.jht.theme.darkDynamicTheme
import com.jht.theme.defaultDynamicTheme
import com.jht.theme.lightDynamicTheme
import com.jht.ui.Device10inPreviews
import com.jht.ui.Device15inPreviews
import com.jht.ui.Device22inPreviews
import com.jht.ui.DrawableUtil
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize
import com.jht.ui.largerThan16
import com.jht.welcome.R
import com.jht.welcome.WelcomeViewModel
import com.jht.welcome.chameleon.overlays.LoginEntryOverlay
import com.jht.welcome.chameleon.overlays.LoginListOverlay
import com.jht.welcome.chameleon.overlays.LoginTab
import com.jht.welcome.chameleon.overlays.RegisterOverlay
import com.jht.welcome.chameleon.overlays.RegisterTab
import com.jht.welcome.chameleon.overlays.StatusOverlay
import com.jht.welcome.chameleon.overlays.VideoOverlay
import com.jht.welcome.chameleon.overlays.createPreviewLoginList
import com.jht.widgets.AnimatedTouchScreenIcon
import com.jht.widgets.Blur
import com.jht.widgets.DialogFullscreen
import com.jht.widgets.LanguageOverlay
import com.jht.widgets.PhoneLoginWidget
import com.jht.widgets.PhoneLoginWidgetHandler
import com.jht.widgets.ProductDashboard
import com.jht.widgets.RfidLoginWidget
import com.jht.widgets.RfidLoginWidgetHandler
import com.jht.widgets.helpers.AsyncUiState
import com.jht.widgets.helpers.TabList
import kotlin.math.roundToInt


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WelcomeScreen(
    theme: DynamicThemeData = defaultDynamicTheme,
    uiState: AsyncUiState = AsyncUiState.Unknown,
    networkAvailable: State<Boolean> = mutableStateOf(true),
    loginList: List<LoginData> = createPreviewLoginList(),
    onLogin: (LoginData) -> Unit = {},
    registerList: List<RegisterData> = listOf(),
    onRegister: (RegisterData) -> Unit = {},
    selectedTabKey: Any? = null,
    onSelectTabKey: (Any?) -> Unit = {},
    resetIdle: () -> Unit = {},
    blur: Boolean = true,
    screenTimedOut: Boolean = false,
    backgroundUri: MutableState<Uri> = mutableStateOf(DrawableUtil.getUri(LocalContext.current, R.drawable.new_welcome_background)), // passing whole state object to reduce recompose
    videoUriList: MutableState<List<Uri>> = mutableStateOf(listOf()),
    message: String = "",
    rfidHandler: RfidLoginWidgetHandler = RfidLoginWidgetHandler(),
    phoneHandler: PhoneLoginWidgetHandler = PhoneLoginWidgetHandler(),
    showDebugDialog: ()->Unit = {},
    onMediaError: (Exception)->Unit = {},
) {
    val showLanguageOverlay = remember(screenTimedOut) { mutableStateOf(false) }
    val showProductDashboard = remember(screenTimedOut) { mutableStateOf(false) }

    val loginsWithTabs by remember { derivedStateOf { loginList.filter { !it.isGuest() } } }
    val guestLogin by remember { derivedStateOf { loginList.find { it.isGuest() } } }
    val onLoginGuest = remember(guestLogin) {
        { login:LoginData? -> login?.let { if (uiState != AsyncUiState.Waiting) onLogin(it) } ?: Unit }
    }
    DynamicTheme(theme) {
        BoxWithConstraints(modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
        ) {
            val screenHeightDp = maxHeight
            val screenWidthDp = maxWidth

            val tabs = TabList(selectedTabKey, onSelectTabKey)
            for (login in loginsWithTabs) {
                tabs.add(
                    { selected, onSelect ->
                        LoginTab(data = login, selected = selected, onClick = onSelect)
                    },
                    {
                        LoginEntryOverlay(data = login, onLogin = onLogin, uiState)
                    },
                    key = WelcomeViewModel.makeLoginKey(login)
                )
            }
            for (register in registerList) {
                tabs.add(
                    { selected, onSelect ->
                        RegisterTab(selected, onSelect)
                    },
                    {
                        //Smaller screens will have registration full screen.
                        //TODO: Is there a better way for checking between 10", 16", and 22" screen sizes?
                        val registerModifier = Modifier.fillMaxSize()
                        if (largerThan16(screenWidthDp)) {
                            RegisterOverlay(register, registerModifier, theme, false, { onSelectTabKey(null) }, onRegister, uiState)
                        } else {
                            DialogFullscreen(onDismissRequest = { onSelectTabKey(null) }) {
                                RegisterOverlay(register, registerModifier, theme, true, { onSelectTabKey(null) }, onRegister, uiState)
                            }
                        }
                    },
                    key = WelcomeViewModel.makeRegisterKey(register)
                )
            }

            //Cache for blurring of left and right backgrounds on the welcome screen
            val leftBackgroundBlurCache = remember{HashMap<Uri, Bitmap>()}
            val rightBackgroundBlurCache = remember{HashMap<Uri, Bitmap>()}

            // Club com UI is under regular UI and replaces regular background when running
            ClubComWelcomeUi(Modifier.fillMaxSize(), screenSaverMode = screenTimedOut) {
                //Main background
                BackgroundSlideShow(backgroundUri = backgroundUri, onClick = resetIdle)
            }

            if (screenTimedOut && videoUriList.value.isNotEmpty()) VideoOverlay(uriList = videoUriList.value, modifier = Modifier.fillMaxSize(), onError = onMediaError)
            else Row {
                AnimatedVisibility(!screenTimedOut, enter = slideInHorizontally(initialOffsetX = {-300}),
                    exit = slideOutHorizontally(targetOffsetX = {-300})) {
                    // LEFT SIDE PANEL
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(360 / 768f),
                    ) {
                        LoginOverlayBackground(backgroundUri.value, blur, screenWidthDp, screenHeightDp, leftBackgroundBlurCache)
                        if (showLanguageOverlay.value) {
                            LanguageOverlay(
                                showLanguageOverlay,
                            )
                        } else {
                            LoginListOverlay(tabs, guestLogin, onLoginGuest, showLanguageOverlay, showProductDashboard, uiState, networkAvailable, showDebugDialog) { onSelectTabKey(null) }
                        }
                    }
                }
                if (!screenTimedOut && selectedTabKey != null) {
                    tabs.CurrentTabBody()
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                    AnimatedVisibility(!screenTimedOut, enter = slideInHorizontally(initialOffsetX = {300}),
                        exit = slideOutHorizontally(targetOffsetX = {300})) {
                        // RIGHT SIDE PANEL
                        StatusOverlay(
                        backgroundUri = backgroundUri.value,
                        blur = blur,
                        screenWidthDp = screenWidthDp,
                        screenHeightDp = screenHeightDp,
                        rightBackgroundBlurCache = rightBackgroundBlurCache,
                        )
                    }
                }
            }

            // THINGS TO SHOW/DO WHEN WELCOME PANELS ARE NOT SHOWN (screen timed out)
            if (screenTimedOut) {
                // TOUCH SCREEN PROMPT
                AnimatedTouchScreenIcon()
                // GYM MESSAGE STRING
                if (message.isNotEmpty()) Box(modifier = Modifier
                    .padding(top = 50.dp.computeSize())
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(.7f),
                ){
                    Text(
                        text = message,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = .3f))
                            .align(Alignment.Center)
                            .padding(5.dp.computeSize()),
                        fontSize = 30.sp.computeSize(),
                        textAlign = TextAlign.Center
                    )
                }
            }
            ProductDashboard(showProductDashboard)

            RfidLoginWidget(handler = rfidHandler)
            PhoneLoginWidget(handler = phoneHandler)
        } // BoxCon
    } // Theme
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun BackgroundSlideShow(
    backgroundUri: MutableState<Uri>,
    onClick: () -> Unit,
) {
    // for some reason having this up in screen causes it to recompose whenever background changes
    Crossfade(
        targetState = backgroundUri.value,
        animationSpec = tween(2000),
        label = "backgroundImageFade",
    ) {
        GlideImage(
            modifier = Modifier
                .fillMaxSize()
                .clickableNoEffect(onClick = onClick)
                .testTag("BackgroundImage"),
            model = it,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
        )
    }
}

@Composable
private fun LoginOverlayBackground(backgroundUri: Uri, blur: Boolean, screenWidthDp : Dp, screenHeightDp : Dp, leftBackgroundBlurCache: HashMap<Uri, Bitmap>) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        if (blur) {
            with (LocalDensity.current) {
                val screenWidth = screenWidthDp.toPx().roundToInt()
                val screenHeight = screenHeightDp.toPx().roundToInt()
                val thisWidth = maxWidth.toPx().roundToInt()
                val thisHeight = maxHeight.toPx().roundToInt()
                Blur(
                    backgroundUri = backgroundUri,
                    rect = Rect(0, 0, thisWidth, thisHeight),
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    backgroundBlurCache = leftBackgroundBlurCache
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = DynamicTheme.colors.glass1.vertical()
                ),
        )
    }
}




//region Previews
@Device10inPreviews
@Device15inPreviews
@Device22inPreviews
@Composable
private fun PreviewWelcomeScreenLight() {
    WelcomeScreen(
        theme = lightDynamicTheme,
        //Blur does not work in local Android Studio preview
        blur = !LocalInspectionMode.current,
    )
}

@Device10inPreviews
@Device15inPreviews
@Device22inPreviews
@Composable
private fun PreviewWelcomeScreenDark() {
    WelcomeScreen(
        theme = darkDynamicTheme,
        //Blur does not work in local Android Studio preview
        blur = !LocalInspectionMode.current,
    )
}

@Device15inPreviews
@Composable
private fun PreviewWelcomeScreenTimeoutDark() {
    WelcomeScreen(
        theme = darkDynamicTheme,
        //Blur does not work in local Android Studio preview
        blur = !LocalInspectionMode.current,
        message = "This is a message from your local neighborhood gym",
        screenTimedOut = true,
    )
}

@Device15inPreviews
@Composable
private fun PreviewWelcomeScreenNoLogins() {
    WelcomeScreen(
        loginList = listOf(),
        theme = darkDynamicTheme,
        //Blur does not work in local Android Studio preview
        blur = !LocalInspectionMode.current,
    )
}

@Device15inPreviews
@Composable
private fun PreviewLogin() {
    val login = LoginData(label = "Login Label", description = "Description for login. This is extra text to make sure it reaches beyond the end of the login fields shown below. It gives us an idea of how that looks.",
    fields = listOf(UserFieldText(label = "Field Label"), UserFieldText(label = "Field Label")), type = LoginType.Xid)
    WelcomeScreen(
        theme = darkDynamicTheme,
        //Blur does not work in local Android Studio preview
        blur = !LocalInspectionMode.current,
        loginList = listOf(login),
        selectedTabKey = WelcomeViewModel.makeLoginKey(login),
    )
}
//endregion Previews


