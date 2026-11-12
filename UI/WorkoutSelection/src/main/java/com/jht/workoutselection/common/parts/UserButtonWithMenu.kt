package com.jht.workoutselection.common.parts

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.jht.data.LoginType
import com.jht.enums.WifiState
import com.jht.interfaces.models.DeviceModel
import com.jht.interfaces.models.LoginModel
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UserModel
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.theme.themeButtonBorder
import com.jht.theme.themeButtonColor
import com.jht.widgets.WebBrowserComposeOptions
import com.jht.widgets.helpers.getInjectionEntryPoint
import com.jht.widgets.launchWebBrowserCompose
import com.jht.workoutselection.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// For now this button does not actually interact with the screen, so maybe start off as self contained widget?
// If screen needs to take action based on this widget, maybe could break it out or provide parameter to handle that?

/** internal state tracking for [UserButtonWithMenu] */
private class UserButtonWithMenuState(val context: Context, val guestCaption: String) {
    val shown = mutableStateOf(true)
    val userName = mutableStateOf(guestCaption)
    val userInitials = mutableStateOf("")
    val userEditUrl = mutableStateOf("")
    val menuExpanded = mutableStateOf(false)
    val showDialogWalletQR = mutableStateOf(false)
    val qrCodeBitmap = mutableStateOf<Bitmap?>(null)
    val wifiState = mutableStateOf(WifiState.NO_CONNECTION)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val userModel: UserModel
    private val loginModel: LoginModel
    private val navigationModel: NavigationModel
    private val systemModel: SystemModel
    val loginsAvailable: Boolean
    val walletAvailable: Boolean
    val deviceModel: DeviceModel

    init {
        val injection = getInjectionEntryPoint(context)
        userModel = injection.user()
        loginModel = injection.login()
        navigationModel = injection.navigation()
        val loginOptions = injection.login().getLoginOptions()
        loginsAvailable = loginOptions.find { !it.isGuest() } != null
        walletAvailable = loginOptions.find { it.type == LoginType.Wallet } != null && userModel.walletPass.value.isNotEmpty()
        deviceModel = injection.devices()
        systemModel = injection.system()
    }

    fun start() {
        deviceModel.wifiState.listenAndTrigger { wifiState.value = it }
        userModel.name.listenAndTrigger {
            // name for guest is "Guest", so if you want something else we need to catch it
            if (userModel.isGuest) {
                userName.value = guestCaption
                userInitials.value = ""
            } else {
                userName.value = it
                userInitials.value = it.firstOrNull().toString()
            }
            // "We need to show the profile button when someone logs in with RFID and all login options are disabled"
            shown.value = !userModel.isGuest || loginsAvailable
        }
        userModel.editUrl.listenAndTrigger { userEditUrl.value = it }
    }
    fun stop() {
        scope.cancel()
        userModel.close()
        loginModel.close()
        navigationModel.close()
        deviceModel.close()
        systemModel.close()
    }
    val isGuest get() = userModel.isGuest
    fun onSignOut() {
        navigationModel.launchSplashScreen()
    }
    fun onEditProfile() {
        if (userEditUrl.value.isNotEmpty()) {
            // webpage has its own session timeout, but maybe dialog should respect system timeout?
            val options = WebBrowserComposeOptions(
                idleTimeoutMs = systemModel.welcomeScreenTimeoutSeconds * 1000,
            )
            launchWebBrowserCompose(
                context = context,
                url = userEditUrl.value,
                options = options,
            )
        }
        menuExpanded.value = false
    }
    fun onWalletSetup() {
        if (!userModel.isGuest) {
            // Run Wallet POC to show QR dialog once a user is logged in
            // Generate a pass that has xid and passcode saved
            if (userModel.walletPass.value.isNotEmpty() && userModel.name.value.isNotEmpty()) {
                generateQRCode(userModel.walletPass.value, userModel.name.value)
                showDialogWalletQR.value = true
            }
        }
        menuExpanded.value = false
    }
    private fun generateQRCode(passData: String, username: String) {
        scope.launch {
            qrCodeBitmap.value = loginModel.generateWalletQR(passData, username)
        }
    }
}

/** top level button hosting a user menu and wallet setup dialog */
@Composable
fun UserButtonWithMenu(
    modifier: Modifier = Modifier,
    onGuestClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val guestCaption = stringResource(id = R.string.login_tc)
    val state = remember { UserButtonWithMenuState(context, guestCaption) }
    LaunchedEffect(Unit) { state.start() }
    DisposableEffect(Unit) { onDispose { state.stop() } }
    val hasWifi = state.wifiState.value != WifiState.NO_CONNECTION

    if (state.shown.value && hasWifi) {
        Box { // Box to anchor menu to button
            ThemeButton(
                onClick = {
                    if (state.isGuest) onGuestClick()
                    else state.menuExpanded.value = true
                },
                modifier = Modifier.testTag("User:Button").then(modifier),
                contentPadding = PaddingValues(20.dp, 10.dp),
            ) {
                if (state.isGuest) UserIcon()
                else if (state.userInitials.value == "null") UserIcon() // this is here to prevent the race condition showing the string "null" instead of the user initials in the circle
                else UserInitialCircle( text = state.userInitials.value, modifier = Modifier.aspectRatio(1f).width(40.dp) )

                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    state.userName.value,
                    Modifier.testTag("Username:Text"),
                    color = DynamicTheme.colors.text1,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.size(10.dp))
                if (!state.isGuest) Icon(
                    painter = if (state.menuExpanded.value) painterResource(android.R.drawable.arrow_down_float) else painterResource(
                        android.R.drawable.arrow_up_float
                    ),
                    contentDescription = "arrow icon",
                    tint = DynamicTheme.colors.text1,
                )
            }
            // https://stackoverflow.com/a/66782261 workaround to force shape onto material3 DropdownMenu is to override extraSmall shape
            MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = MaterialTheme.shapes.medium)) {
                DropdownMenu(
                    expanded = state.menuExpanded.value,
                    onDismissRequest = { state.menuExpanded.value = false },
                    offset = DpOffset(0.dp, 10.dp),
                    modifier = Modifier
                        .background(themeButtonColor)
                        .border(themeButtonBorder, MaterialTheme.shapes.medium)
                        .clip(MaterialTheme.shapes.medium)
                        .padding(10.dp)
                ) {
                    UserButtonMenu(
                        state,
                        Modifier
                            .widthIn(min = 300.dp)
                            .align(Alignment.CenterHorizontally),
                    )
                }
            }
        }
    }
    if (state.showDialogWalletQR.value) {
        DialogWalletQR(state.qrCodeBitmap.value) {
            state.showDialogWalletQR.value = false
        }
    }
}

/** the menu that expands from [UserButtonWithMenu] */
@Composable
private fun UserButtonMenu(
    state: UserButtonWithMenuState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val iconModifier = Modifier.width(40.dp)
        UserItem(
            userName = state.userName.value,
            userInitials = state.userInitials.value,
            modifier = iconModifier,
        )
        SeparatorItem()
        if (state.userEditUrl.value.isNotEmpty()) {
            GenericItem(
                iconResource = R.drawable.ic_user_circle_new_bold,
                text = stringResource(R.string.edit_profile_tc),
                modifier = iconModifier.testTag("User:EditProfile"),
            ) { state.onEditProfile() }
        }
        if (state.walletAvailable) {
            GenericItem(
                iconResource = R.drawable.ic_phone_scan_rfid,
                text = stringResource(R.string.setup_wallet_tc),
                modifier = iconModifier.testTag("User:SetupWallet"),
            ) { state.onWalletSetup() }
        }
        SeparatorItem()
        GenericItem(
            iconResource = R.drawable.ic_sign_out,
            text = stringResource(R.string.logout_tc),
            modifier = iconModifier.testTag("User:Logout"),
        ) { state.onSignOut() }
    }
}

@Composable
private fun SeparatorItem() {
    Box(
        Modifier.height(1.dp).fillMaxWidth(0.9f).background(DynamicTheme.colors.gray7)
    )
}

@Composable
private fun GenericItem(
    text: String,
    iconResource: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    DropdownMenuItem(
        leadingIcon = {
            Icon(
                painterResource(iconResource),
                null,
                modifier = modifier,
                tint = DynamicTheme.colors.gray12,
            )
        },
        text = {
            Text(
                text,
                Modifier.padding(10.dp),
                color = DynamicTheme.colors.text1,
                style = MaterialTheme.typography.labelMedium,
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun UserItem(
    userName: String,
    userInitials: String,
    modifier: Modifier = Modifier,
    userNote: String = "",
    onClick: () -> Unit = {},
) {
    DropdownMenuItem(
        leadingIcon = {
            //UserIcon(modifier) // old user icon
            UserInitialCircle(text = userInitials, modifier = modifier.aspectRatio(1f)) // new initials user icon
        },
        text = {
            Column {
                Text(
                    userName,
                    Modifier.padding(10.dp),
                    color = DynamicTheme.colors.text1,
                    style = MaterialTheme.typography.labelMedium,
                )
                // Comp has subtext here
                //Text(userNote, color = DynamicTheme.colors.gray12, style = MaterialTheme.typography.bodySmall)
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun UserIcon(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(40.dp)
            .background(DynamicTheme.colors.gray13, CircleShape)
    ) {
        Icon(
            modifier = Modifier
                .align(Alignment.Center)
                .size(20.dp),
            painter = painterResource(id = R.drawable.ic_login),
            contentDescription = null,
            tint = Color.Black //DynamicTheme.colors.text1,
        )
    }
}

@Composable
private fun UserInitialCircle(
    text: String,
    modifier: Modifier = Modifier,
    circleColor: Color = DynamicTheme.colors.primary,
    circlePadding: PaddingValues = PaddingValues(0.dp),
    textStyle: TextStyle = MaterialTheme.typography.titleSmall.copy(color = DynamicTheme.colors.text2),
) {
    Box(
        modifier = modifier.background(circleColor, CircleShape)
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(circlePadding),
            text = text,
            style = textStyle,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}


@Preview
@Composable
private fun PreviewUserButton() {
    UserButtonWithMenu()
}
@Preview
@Composable
private fun PreviewUserButtonMenu() {
    val state = UserButtonWithMenuState(LocalContext.current, "Guest")
    state.userName.value = "Test"
    state.userInitials.value = "T"
    UserButtonMenu(state)
}
