package com.jht.welcome.vortex

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.jht.clubcom.ui.ClubComWelcomeUi
import com.jht.data.LoginData
import com.jht.data.LoginType
import com.jht.data.MarketingAd
import com.jht.data.RegisterData
import com.jht.theme.DynamicTheme
import com.jht.theme.DynamicThemeData
import com.jht.theme.darkDynamicTheme
import com.jht.theme.defaultDynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.DrawableUtil
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd
import com.jht.ui.largerThan16
import com.jht.welcome.QrUiState
import com.jht.welcome.R
import com.jht.welcome.WelcomeViewModel
import com.jht.welcome.chameleon.overlays.VideoOverlay
import com.jht.welcome.chameleon.overlays.createPreviewLoginList
import com.jht.welcome.vortex.components.LoginComponent
import com.jht.welcome.vortex.components.OtherLoginMethodsComponent
import com.jht.welcome.vortex.components.QrCodeLoginComponent
import com.jht.welcome.vortex.components.RegisterComponent
import com.jht.welcome.vortex.components.WalletLoginComponent
import com.jht.welcome.vortex.components.WelcomeScreenTopBar
import com.jht.widgets.AnimatedTouchScreenIcon
import com.jht.widgets.LanguageOverlay
import com.jht.widgets.PhoneLoginWidget
import com.jht.widgets.PhoneLoginWidgetHandler
import com.jht.widgets.ProductDashboard
import com.jht.widgets.RFIDIndicator
import com.jht.widgets.RfidLoginWidget
import com.jht.widgets.RfidLoginWidgetHandler
import com.jht.widgets.helpers.AsyncUiState


/** Login/Register "tab" keys for convenience and tracking of center panels (assumption is that default none is null) */
private enum class CenterPanel(val key: Any) {
    LoginXid(WelcomeViewModel.makeLoginKey(LoginType.Xid)),
    LoginEmail(WelcomeViewModel.makeLoginKey(LoginType.Email)),
    LoginBarcode(WelcomeViewModel.makeLoginKey(LoginType.Barcode)),
    LoginPhoneNumber(WelcomeViewModel.makeLoginKey(LoginType.PhoneNumber)),
    LoginCustom(WelcomeViewModel.makeLoginKey(LoginType.Custom)),
    LoginWallet(WelcomeViewModel.makeLoginKey(LoginType.Wallet)),
    LoginQrCode(WelcomeViewModel.makeLoginKey(LoginType.QrCode)),
    RegisterXid(WelcomeViewModel.makeRegisterKey(LoginType.Xid)),
    ; // END LIST
    companion object {
        fun forKey(key: Any?): CenterPanel? = entries.find { it.key == key }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun WelcomeScreen(
    theme: DynamicThemeData = defaultDynamicTheme,
    uiState: AsyncUiState = AsyncUiState.Unknown,
    networkAvailable: State<Boolean> = mutableStateOf(true),
    loginList: List<LoginData> = createPreviewLoginList(),
    onLogin: (LoginData) -> Unit = {},
    registerList: List<RegisterData> = listOf(),
    onRegister: (RegisterData) -> Unit = {},
    selectedTabKey: Any? = null,
    onSelectTabKey: (Any?) -> Unit = {},
    languageList: List<String> = listOf(),
    selectedLanguageIndex: Int = 0,
    onLanguageSelected: (Int) -> Unit = {},
    resetIdle: () -> Unit = {},
    blur: Boolean = true,
    screenTimedOut: Boolean = false,
    backgroundUri: MutableState<Uri> = mutableStateOf(
        DrawableUtil.getUri(
            LocalContext.current,
            R.drawable.new_welcome_background
        )
    ), // passing whole state object to reduce recompose
    videoUriList: MutableState<List<Uri>> = mutableStateOf(listOf()),
    adList: List<MarketingAd> = listOf(),
    dismissAd: (MarketingAd?)-> Unit = {},
    message: String = "",
    rfidHandler: RfidLoginWidgetHandler = RfidLoginWidgetHandler(),
    phoneHandler: PhoneLoginWidgetHandler = PhoneLoginWidgetHandler(),
    showDebugDialog: ()->Unit = {},
    onMediaError: (Exception)->Unit = {},
    qrUiState: QrUiState = QrUiState(),
    isRfidIconVisible: Boolean = false
) {
    val showLanguageOverlay = remember(screenTimedOut) { mutableStateOf(false) }
    val showProductDashboard = remember(screenTimedOut) { mutableStateOf(false) }
    val showSmartWatchLoginOverlay = remember(screenTimedOut) { mutableStateOf(false) }

    val guestLogin by remember { derivedStateOf { loginList.find { it.isGuest() } } }
    val onLoginGuest = remember(guestLogin) {
        { login: LoginData? ->
            login?.let { if (uiState != AsyncUiState.Waiting) onLogin(it) } ?: Unit
        }
    }

    DynamicTheme(theme) {
        Box(
            Modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true },
        ) {
            // TODO - is the top bar always overlaid when it is present?
            // Club com UI is under regular UI and replaces regular background when running
            ClubComWelcomeUi(Modifier.fillMaxSize(), screenSaverMode = screenTimedOut) {
                //Main background
                BackgroundSlideShow(backgroundUri = backgroundUri, onClick = resetIdle)
            }

            if (screenTimedOut && videoUriList.value.isNotEmpty())
                VideoOverlay(uriList = videoUriList.value, modifier = Modifier.fillMaxSize(), onError = onMediaError)
            else
                Column(
                    Modifier.fillMaxSize(),
                ) {
                    // trying to keep center panel centered but welcome ads needs more space on smaller screens
                    val topHalfWeight = 1f
                    val bottomHalfWeight = if (!largerThan16() && adList.isNotEmpty()) 0.5f else 1f
                    val centerPanel = remember(selectedTabKey) { CenterPanel.forKey(selectedTabKey) }
                    // TOP HALF
                    Column(
                        modifier = Modifier.weight(topHalfWeight).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        //verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        //TopBar
                        AnimatedVisibility(
                            !screenTimedOut,
                            enter = slideInVertically(initialOffsetY = { -300 }),
                            exit = slideOutVertically(targetOffsetY = { -300 })
                        ) {
                            Column {
                                WelcomeScreenTopBar(
                                    onLanguage = { showLanguageOverlay.value = true },
                                    showLanguageButton = languageList.isNotEmpty(),
                                    showProductDashboard = showProductDashboard,
                                    isLanguageOverlayShowing = showLanguageOverlay,
                                    showDebugDialog = showDebugDialog
                                ) {
                                    showSmartWatchLoginOverlay.value = it
                                }
                                HorizontalDivider(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(1.dp),
                                    color = DynamicTheme.colors.gray3
                                )
                            }
                        }
                        AnimatedVisibility(
                            visible = !screenTimedOut,
                            enter = fadeIn(animationSpec = tween(durationMillis = 400, delayMillis = 300)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 200)),
                            modifier = Modifier // take up space between bar and center box, limit width to similar to usual center box width
                                .weight(1f)
                                .fillMaxWidth(if (largerThan16()) 0.55f else 0.6f)
                        ) {
                            // how do we want to avoid stuff showing under stuff? another state? some kind of popup stack tracking?
                            // watches popups, register forms? perhaps only show if default center panel is showing
                            if (centerPanel == null && !showSmartWatchLoginOverlay.value) {
                                BoxWithConstraints(
                                    contentAlignment = Alignment.TopCenter,
                                    modifier = Modifier // fill animation area with padding for between sections
                                        // fill max size, or width with wrap? not sure wrap respects the limited height, but autoSize text seems fragile otherwise
                                        .fillMaxSize()
                                        //.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true)
                                        .padding(20.dp.computeSizeHd()),
                                ) {
                                    if (this.maxHeight > 0.dp) WelcomeAds(
                                        adList = adList,
                                        dismissAd = dismissAd,
                                        modifier = Modifier // let add take full width, leave height to it
                                            .fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }

                    // MIDDLE of the halves
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(
                            !screenTimedOut,
                            enter = slideInVertically(initialOffsetY = { -300 }),
                            exit = slideOutVertically(targetOffsetY = { -300 })
                        ) {
                            if (!showSmartWatchLoginOverlay.value) // check to avoid the layouts one on top of another looks weird?
                            // Background Rounded Card for Login/Register/Guest
                                Box(
                                    //modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (centerPanel) {
                                        CenterPanel.LoginXid -> {
                                            OtherLoginMethodsComponent(
                                                onBack = { onSelectTabKey(null) },
                                                onClose = { onSelectTabKey(null) },
                                                onLogin = onLogin,
                                                uiState = uiState,
                                                title = stringResource(R.string.btn_xid_login_tc),
                                                errorTitle = stringResource(R.string.login_error_with_method_pg,
                                                    stringResource(R.string.xid_tc),
                                                ),
                                                loginData = loginList.find { it.type == LoginType.Xid }
                                                    ?: LoginData(),
                                                isShowDefaultLoginState = loginList.filter { it.type == LoginType.Xid }.size == 1
                                            )
                                        }

                                        CenterPanel.LoginEmail -> {
                                            OtherLoginMethodsComponent(
                                                onBack = { onSelectTabKey(null) },
                                                onClose = { onSelectTabKey(null) },
                                                onLogin = onLogin,
                                                uiState = uiState,
                                                title = stringResource(R.string.email_login_tc),
                                                errorTitle = stringResource(R.string.login_error_with_method_pg,
                                                    stringResource(R.string.email_tc),
                                                ),
                                                loginData = loginList.find { it.type == LoginType.Email }
                                                    ?: LoginData(),
                                                isShowDefaultLoginState = loginList.filter { it.type == LoginType.Email }.size == 1
                                            )
                                        }

                                        CenterPanel.LoginBarcode -> {
                                            OtherLoginMethodsComponent(
                                                onBack = { onSelectTabKey(null) },
                                                onClose = { onSelectTabKey(null) },
                                                onLogin = onLogin,
                                                uiState = uiState,
                                                title = stringResource(R.string.barcode_login_tc),
                                                errorTitle = stringResource(R.string.login_error_with_method_pg,
                                                    stringResource(R.string.barcode_tc),
                                                ),
                                                loginData = loginList.find { it.type == LoginType.Barcode }
                                                    ?: LoginData(),
                                                isShowDefaultLoginState = loginList.filter { it.type == LoginType.Barcode }.size == 1
                                            )
                                        }

                                        CenterPanel.LoginPhoneNumber -> {
                                            OtherLoginMethodsComponent(
                                                onBack = { onSelectTabKey(null) },
                                                onClose = { onSelectTabKey(null) },
                                                onLogin = onLogin,
                                                uiState = uiState,
                                                title = stringResource(R.string.phone_login_tc),
                                                errorTitle = stringResource(R.string.login_error_with_method_pg,
                                                    stringResource(R.string.phone_tc),
                                                ),
                                                loginData = loginList.find { it.type == LoginType.PhoneNumber }
                                                    ?: LoginData(),
                                                isShowDefaultLoginState = loginList.filter { it.type == LoginType.PhoneNumber }.size == 1
                                            )
                                        }

                                        CenterPanel.LoginCustom -> {
                                            val customData = loginList.find { it.type == LoginType.Custom } ?: LoginData()
                                            OtherLoginMethodsComponent(
                                                onBack = { onSelectTabKey(null) },
                                                onClose = { onSelectTabKey(null) },
                                                onLogin = onLogin,
                                                uiState = uiState,
                                                title = customData.label,
                                                errorTitle = stringResource(R.string.login_error_with_method_pg,
                                                    stringResource(R.string.user_login_tc),
                                                ),
                                                loginData = customData,
                                                isShowDefaultLoginState = loginList.filter { it.type == LoginType.Custom }.size == 1
                                            )
                                        }

                                        CenterPanel.LoginWallet -> {
                                            WalletLoginComponent(
                                                Modifier,
                                                onBack = { onSelectTabKey(null) },
                                                onClose = { onSelectTabKey(null) },
                                                uiState)
                                        }

                                        CenterPanel.RegisterXid -> {
                                            for (registerData in registerList) {
                                                Popup(
                                                    alignment = Alignment.Center,
                                                    properties = PopupProperties(focusable = true),
                                                ) {
                                                    RegisterComponent(
                                                        modifier = Modifier,
                                                        rawData = registerData,
                                                        theme = theme,
                                                        onBack = { onSelectTabKey(null) },
                                                        onClose = { onSelectTabKey(null) },
                                                        onRegister = onRegister,
                                                        uiState = uiState
                                                    )
                                                }
                                            }
                                        }
                                        CenterPanel.LoginQrCode -> {
                                            QrCodeLoginComponent(
                                                onBackClicked = { onSelectTabKey(null) },
                                                qrUiState = qrUiState,
                                            )
                                        }

                                        else -> {
                                            val primaryLoginType = loginList.primaryLogin()?.type
                                            LoginComponent(
                                                onGuest = onLoginGuest,
                                                loginList = loginList,
                                                onLogin = onLogin,
                                                guestLogin = guestLogin,
                                                uiState = uiState,
                                                networkAvailable = networkAvailable,
                                                onRegister = { onSelectTabKey(CenterPanel.RegisterXid.key) },
                                                onSelectLogin = { onSelectTabKey(WelcomeViewModel.makeLoginKey(it)) },
                                                onClose = {},
                                                isShowSignUpText = registerList.isNotEmpty() && primaryLoginType != LoginType.QrCode,
                                                qrUiState = qrUiState,
                                            )
                                        }
                                    }
                                }
                        }
                    }

                    // BOTTOM HALF
                    Box(modifier = Modifier.weight(bottomHalfWeight).fillMaxWidth())
                }


            // THINGS TO SHOW/DO WHEN WELCOME PANELS ARE NOT SHOWN (screen timed out)
            if (screenTimedOut) {
                // TOUCH SCREEN PROMPT
                AnimatedTouchScreenIcon()
                // GYM MESSAGE STRING
                if (message.isNotEmpty()) Box(
                    modifier = Modifier
                        .padding(top = 50.dp.computeSize())
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(.7f),
                ) {
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
            LanguageOverlay(
                languageList,
                selectedLanguageIndex,
                showLanguageOverlay,
                onLanguageSelected
            )
            ProductDashboard(showProductDashboard)
            RfidLoginWidget(handler = rfidHandler)
            PhoneLoginWidget(handler = phoneHandler)
            RFIDIndicator(
                modifier = Modifier.padding(start = 44.dp.computeSizeHd())
                    .align(Alignment.CenterStart).zIndex(1f),
                isVisible = isRfidIconVisible
            )
        } // Top level box
    } // Theme
}


/** Presents marketing AD if any. If multiple ADs just stack them. */
@Composable
private fun WelcomeAds(
    adList: List<MarketingAd>,
    modifier: Modifier = Modifier,
    dismissAd: (MarketingAd?)-> Unit = {},
) {
    val cardColor = Color.Black.copy(alpha = 0.9f) // think this is background being applied by the blur background overlay?
    // do the cards need to be same size to stack? or is same width enough? or do we need a pager kind of thing?
    Box(modifier = modifier) {
        adList.forEach { item ->
            key(item.id) {
                Card(
                    modifier = Modifier.fillMaxWidth(), // let card render the full width
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                ) {
                    Row(
                        Modifier
                            .padding(20.dp.computeSizeHd()), // padding within the card
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        item.Content(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { dismissAd(item) },
                            modifier = Modifier.padding(10.dp),
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = DynamicTheme.colors.text1)
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun BackgroundSlideShow(
    backgroundUri: MutableState<Uri>,
    onClick: () -> Unit,
) {
    val localFocusManager = LocalFocusManager.current
    // for some reason having this up in screen causes it to recompose whenever background changes
    Crossfade(
        targetState = backgroundUri.value,
        animationSpec = tween(2000),
        label = "backgroundImageFade",
    ) {
        GlideImage(
            modifier = Modifier
                .fillMaxSize()
                .clickableNoEffect(onClick = { onClick(); localFocusManager.clearFocus() })
                .testTag("BackgroundImage"),
            model = it,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
        )
    }
}

@AllDevicePreviews
@Composable
private fun PreviewWelcomeScreenDark() {
    WelcomeScreen(
        theme = darkDynamicTheme,
        //Blur does not work in local Android Studio preview
        blur = !LocalInspectionMode.current,
    )
}
