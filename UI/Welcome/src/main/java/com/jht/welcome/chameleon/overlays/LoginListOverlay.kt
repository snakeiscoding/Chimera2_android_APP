package com.jht.welcome.chameleon.overlays

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jht.concierge.ui.ConciergeButton
import com.jht.data.LoginData
import com.jht.data.LoginType
import com.jht.data.UserFieldText
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.ui.clickableNoEffect
import com.jht.ui.fillMaxHeightReset
import com.jht.welcome.R
import com.jht.widgets.BrandLogo
import com.jht.widgets.CenterMinRow
import com.jht.widgets.helpers.AsyncUiState
import com.jht.widgets.helpers.TabList
import java.util.Locale


@Composable
internal fun createPreviewLoginList() : List<LoginData> {
    // this is where it would be handy if LoginData was a data module that could be used in either implementation or mock situations
    return listOf(
        LoginData("Login", "Xid preview description", LoginType.Xid, null, listOf(UserFieldText())),
        LoginData("Guest", "", LoginType.Guest),
    )
}


@Composable
fun LoginListOverlay(
    tabList: TabList,
    guestLogin: LoginData?,
    onGuest: (LoginData) -> Unit,
    showLanguageOverlay: MutableState<Boolean>,
    showProductDash: MutableState<Boolean>,
    uiState: AsyncUiState = AsyncUiState.Unknown,
    networkAvailable: State<Boolean>,
    showDebugDialog: ()->Unit = {},
    onClick: () -> Unit,
) {
    val enabled = (uiState != AsyncUiState.Waiting)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickableNoEffect { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ConciergeButton(modifier = Modifier
            .padding(top = 20.dp, start = 20.dp) // need to figure a better alignment method (ie put padding in Column?)
            .height(70.dp)
            .align(Alignment.Start)
        )
        // THE LOGO
        BrandLogo(
            modifier = Modifier
                .padding(top = 25.dp, bottom = 10.dp)
                .fillMaxWidth(250 / 360f)
                .aspectRatio(3f),
            onLongPress = { showProductDash.value = true },
            onDebugFling = showDebugDialog,
        )
        // THE TABS
        tabList.TabColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(320 / 360f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        )

        // guest is shown separate of other logins due to it not needing an entry tab and behaving more like a button
        // if there are NO logins, we still need to be able to get to welcome screen, treat unknown user as implicit guest even if guest logins are not allowed
        val effectiveGuestLogin = if ((tabList.isEmpty() || !networkAvailable.value) && guestLogin == null) LoginData(stringResource(R.string.touch_to_continue_tc), "", LoginType.Guest) else guestLogin
        if (effectiveGuestLogin != null) {
            Column(Modifier.fillMaxWidth(320 / 360f)) {
                LoginTabButton(effectiveGuestLogin, showIcon = effectiveGuestLogin == guestLogin) { onGuest(effectiveGuestLogin) }
                //LoginTab(data = guestLogin, selected = false, onGuest)
            }
        }

        // THE LANGUAGES
        Row(
            modifier = Modifier
                .fillMaxWidth(320 / 360f)
                .aspectRatio(320 / 80f)
                .testTag("LanguageButton")
                .clickable(enabled) { showLanguageOverlay.value = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            CenterMinRow {
                Icon(
                    painter = painterResource(id = R.drawable.ic_icon_globe),
                    contentDescription = null,
                    tint = DynamicTheme.colors.text1,
                    modifier = Modifier
                        .fillMaxHeightReset()
                        .aspectRatio(1f)
                )
                Spacer(Modifier.size(5.dp))
                Text(
                    text = Locale.getDefault().displayLanguage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = DynamicTheme.colors.text1,
                    textAlign = TextAlign.Start,
                    style = DynamicTheme.typography.textStyle2
                )
            }
        }
    }
}


@Composable
fun LoginTab(
    data: LoginData,
    selected: Boolean,
    onClick: () -> Unit,
) {
    LoginTabBase(
        selected,
        data.type.name + "Button", // should we use something that is not tied to translations like the login type enum instead of label?
        onClick,
    )
    {
        when (data.type) {
            LoginType.Guest -> LoginTabGuest(data, selected)
            LoginType.Xid -> LoginTabXid(data, selected)
            LoginType.PhoneNumber -> LoginTabPhone(data, selected)
            LoginType.Barcode -> LoginTabQrCode(data, selected)
            LoginType.Wallet -> LoginTabWallet(data, selected)
            LoginType.QrCode -> LoginTabQrCode(data, selected)
            LoginType.Email -> LoginTabEmail(data, selected)
            else -> LoginTabIconText(data.label, R.drawable.ic_login, selected)
        }
    }
}

@Composable
fun RegisterTab(
    selected: Boolean,
    onClick: () -> Unit,
) {
    LoginTabBase(selected, "RegisterButton", onClick) {
        LoginTabIconText(stringResource(id = R.string.register_tc), R.drawable.ic_register, selected)
    }
}


@Composable
private fun LoginTabBase(
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .aspectRatio(3.5f) // stick to aspect or just give items a padding?
            .clip(MaterialTheme.shapes.medium)
            .testTag(testTag)
            .background(brush = DynamicTheme.colors.gradient1.vertical())
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    )
    {
        Box(
            Modifier
                .background(if (selected) DynamicTheme.colors.primary else Color.Transparent)
                .fillMaxHeight()
                .width(10.dp)
        )
        content.invoke()
    }
}


private val IconModifier = Modifier
    .padding(start = 25.dp)
    .fillMaxWidth(32 / 320f)
    .aspectRatio(1f)
private val LabelModifier = Modifier
    .padding(horizontal = 20.dp)
    .fillMaxWidth()
    .wrapContentHeight()

@Composable
private fun LoginTabIconText(
    name: String,
    painter: Painter,
    selected: Boolean,
    tint: Color = DynamicTheme.colors.text1,
) {
    Icon(
        modifier = IconModifier,
        painter = painter,
        contentDescription = null,
        tint = tint,
    )
    Text(
        text = name,
        modifier = LabelModifier,
        color = DynamicTheme.colors.text1,
        textAlign = TextAlign.Start, // should we just do left for all languages so it is always next to icon? or should icon be right for RTL?
        style = DynamicTheme.typography.textStyle2
    )
}
@Composable
private fun LoginTabIconText(
    name: String,
    icon: Int,
    selected: Boolean,
    tint: Color = DynamicTheme.colors.text1,
) { LoginTabIconText(name, painterResource(id = icon), selected, tint) }
@Composable
private fun LoginTabIconText(
    name: String,
    imageVector: ImageVector,
    selected: Boolean,
    tint: Color = DynamicTheme.colors.text1,
) { LoginTabIconText(name, rememberVectorPainter(imageVector), selected, tint) }


@Composable
private fun LoginTabImageText(
    name: String,
    icon: Int,
    selected: Boolean,
) {
    Image(
        modifier = IconModifier,
        painter = painterResource(id = icon),
        contentDescription = null,
    )
    Text(
        text = name,
        modifier = LabelModifier,
        color = DynamicTheme.colors.text1,
        textAlign = TextAlign.Start,
        style = DynamicTheme.typography.textStyle2
    )
}

@Composable
private fun LoginTabXid(
    data: LoginData,
    selected: Boolean,
) {
    // comps have different icons based on selection
    val icon = if (selected) R.drawable.ic_login_xid_enabled else R.drawable.ic_login_xid_disabled
    val justLogin = stringResource(id = R.string.login_tc) // comp seems to rely on icon to indicate XID?
    LoginTabImageText(justLogin, icon, selected)
}

@Composable
private fun LoginTabPhone(
    data: LoginData,
    selected: Boolean,
) {
    val icon = R.drawable.ic_phone
    LoginTabIconText(data.label, icon, selected)
}

@Composable
private fun LoginTabEmail(
    data: LoginData,
    selected: Boolean,
) {
    val icon = if (selected) Icons.Filled.Email else Icons.Outlined.Email
    LoginTabIconText(data.label, icon, selected)
}

@Composable
private fun LoginTabWallet(
    data: LoginData,
    selected: Boolean,
) {
    val icon = R.drawable.ic_wallet // don't think we have two states for this icon yet
    LoginTabIconText(data.label, icon, selected)
}

@Composable
private fun LoginTabQrCode(
    data: LoginData,
    selected: Boolean,
) {
    val icon = R.drawable.ic_qr_code
    LoginTabIconText(data.label, icon, selected)
}

@Composable
private fun LoginTabGuest(
    data: LoginData,
    selected: Boolean,
) {
    // Guest uses generic profile icon with theme primary tint
    LoginTabIconText(data.label, R.drawable.ic_login, selected, DynamicTheme.colors.primary)
}

@Composable
private fun LoginTabButton(
    data: LoginData,
    showIcon: Boolean = true,
    onClick: () -> Unit,
) {
    ThemeButton(
        onClick,
        Modifier
            .testTag("GuestButton")
            .aspectRatio(320 / 100f),
    ) {
        if (showIcon) Icon(
            modifier = Modifier
                .fillMaxWidth(32 / 320f)
                .aspectRatio(1f),
            painter = painterResource(id = R.drawable.ic_login),
            contentDescription = null,
            tint = DynamicTheme.colors.primary,
        )
        Text(
            text = data.label,
            modifier = Modifier.padding(start = 20.dp),
            color = DynamicTheme.colors.text1,
            textAlign = TextAlign.Start,
            style = DynamicTheme.typography.textStyle2
        )
        //LoginTabGuest(data, false)
    }
}
