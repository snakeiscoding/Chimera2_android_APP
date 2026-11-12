package com.jht.welcome.vortex.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.data.LoginData
import com.jht.data.LoginType
import androidx.compose.runtime.LaunchedEffect
import com.jht.welcome.QrUiState
import com.lightspark.composeqr.QrCodeView
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.largerThan16
import com.jht.welcome.R
import com.jht.welcome.chameleon.overlays.createPreviewLoginList
import com.jht.welcome.vortex.getLoginTypeUiData
import com.jht.welcome.vortex.primaryLogin
import com.jht.widgets.BlurBackgroundOverlay
import com.jht.widgets.FieldInput
import com.jht.widgets.FieldInputCollection
import com.jht.widgets.FieldInputData
import com.jht.widgets.helpers.AsyncUiState

@Composable
internal fun LoginComponent(
    onGuest: (LoginData) -> Unit,
    loginList: List<LoginData>,
    onLogin: (LoginData) -> Unit,
    guestLogin: LoginData?,
    uiState: AsyncUiState = AsyncUiState.Unknown,
    networkAvailable: State<Boolean>,
    isShowSignUpText: Boolean = true,
    onRegister: () -> Unit = {},
    onSelectLogin: (LoginType) -> Unit = {},
    onClose : () -> Unit = {},
    qrUiState: QrUiState = QrUiState(),
) {
    val touchToContinue = LoginData(
        stringResource(R.string.touch_to_continue_tc),
        "",
        LoginType.Guest
    )
    val defaultLoginData = loginList.primaryLogin()
    val otherLoginList =
        loginList.filter {
            when (it.type) {
                LoginType.Guest -> false
                else -> (defaultLoginData?.type != it.type)
            }
        }
    val effectiveGuestLogin = if ((defaultLoginData == null || !networkAvailable.value) && guestLogin == null) touchToContinue else guestLogin
    val needSection1 = (defaultLoginData != null || isShowSignUpText) && networkAvailable.value
    val needSection2 = effectiveGuestLogin != null || otherLoginList.isNotEmpty()
    val hasBothSections = needSection1 && needSection2

    val loginCardSize = (if (largerThan16()) 0.55f else 0.6f) * (if (hasBothSections) 1f else 0.75f)
    BlurBackgroundOverlay(
        modifier = Modifier
            .fillMaxWidth(loginCardSize)
            .height(IntrinsicSize.Min),
        showCloseButton = false,
        onCloseButtonClick = onClose)
    {
        Column(
            modifier = Modifier
                .padding(horizontal = if (largerThan16()) 64.dp else 32.dp, vertical = 32.dp)
                .fillMaxWidth()
                .height(intrinsicSize = IntrinsicSize.Max),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                stringResource(R.string.login_tc) + if(isShowSignUpText && needSection1)"  |  ${stringResource(R.string.register_tc)}" else "",
                style = MaterialTheme.typography.headlineMedium,
                color = DynamicTheme.colors.text1
            )
            Spacer(modifier = Modifier.size(32.dp))
            // Body
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
            ) {
                val sectionModifier = if (hasBothSections) Modifier.weight(1f) else Modifier.fillMaxWidth(0.5f)
                if (needSection1) LoginOrSignUp(
                    modifier = sectionModifier
                        .align(Alignment.CenterVertically)
                        .weight(1f),
                    data = defaultLoginData ?: LoginData(),
                    onLogin = onLogin,
                    uiState = uiState,
                    onSelectLogin = onSelectLogin,
                    onRegister = onRegister,
                    isShowSignUpText = isShowSignUpText,
                    qrUiState = qrUiState,
                )
                if (hasBothSections) {
                    val spacerSize = if(largerThan16()) 32.dp else 16.dp
                    Spacer(modifier = Modifier.size(spacerSize))
                    Separator()
                    Spacer(modifier = Modifier.size(spacerSize))
                }
                if (needSection2) {
                    GuestAndOtherLogin(
                        modifier = sectionModifier
                            .align(Alignment.CenterVertically)
                            .weight(1f),
                        guestLogin = effectiveGuestLogin,
                        onGuestLogin = onGuest,
                        onSelectLogin = onSelectLogin,
                        otherLoginList = otherLoginList,
                        showOtherLogins = networkAvailable.value
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginOrSignUp(
    modifier: Modifier,
    data: LoginData, onLogin: (LoginData) -> Unit,
    uiState: AsyncUiState = AsyncUiState.Unknown,
    isShowSignUpText: Boolean = true,
    onSelectLogin: (LoginType) -> Unit = {},
    onRegister: () -> Unit = {},
    qrUiState: QrUiState = QrUiState(),
) {

    val enabled = uiState != AsyncUiState.Waiting
    val focusManager = LocalFocusManager.current
    val fieldList = remember(data) { FieldInputCollection() }
    val loginAction = {
        if (enabled) {
            // does UI validation make data side validation redundant?
            if (fieldList.checkValidity() && data.hasValidValues()) {
                focusManager.clearFocus()
                onLogin(data)
            }
        }
    }
    if (fieldList.isEmpty() && data.fields.isNotEmpty()) {
        for (i in data.fields.indices) {
            val field = data.fields[i]
            val doneCallback = if (i == data.fields.lastIndex) loginAction else null
            val newData = FieldInputData.build(field, "LoginField$i", doneCallback)
            fieldList.add(newData)
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        when (data.type) {
            LoginType.Wallet -> WalletLoginButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSelectLogin(LoginType.Wallet) }
            )
            LoginType.QrCode -> QrLogin(
                modifier = Modifier.fillMaxWidth(),
                qrUri = qrUiState.qrUri,
                userCode = qrUiState.userCode,
                verificationUri = qrUiState.verificationUri,
                showQr = qrUiState.qrShown,
                onShowQrClicked = qrUiState.onShowQrClicked,
                onQrDismissed = qrUiState.onQrDismissed,
            )
            else -> {
                val fieldModifier = Modifier.fillMaxWidth()
                fieldList.forEachIndexed { index, field ->
                    if (index != 0) Spacer(Modifier.size(20.dp.computeSize()))
                    FieldInput(field, fieldModifier, enabled)
                }
            }
        }
        Spacer(Modifier.size(24.dp.computeSize()))
        if (uiState == AsyncUiState.Failure)
            Column {
                val metaData = getLoginTypeUiData(data)
                Text(
                    stringResource(R.string.login_error_with_method_pg, metaData.name),
                    style = MaterialTheme.typography.bodySmall,
                    color = DynamicTheme.colors.stop
                )
                Spacer(Modifier.size(12.dp.computeSize()))
            }
        if (data.needsInput())
            ThemeButton(
                onClick = loginAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("WelcomeScreen:SignIn:Button"),
                shape = RoundedCornerShape(20.dp.computeSize()),
                border = BorderStroke(0.dp, Color.Transparent),
                colors = ButtonDefaults.buttonColors().copy(
                    containerColor = DynamicTheme.colors.buttonTertiaryDefault,
                    disabledContainerColor = DynamicTheme.colors.buttonTertiaryDefault.copy(alpha = 0.3f),
                ),
                contentPadding = PaddingValues(vertical = 10.dp.computeSize()),
                enabled = enabled,
            ) {
                Text(text = stringResource(R.string.sign_in_tc), style = MaterialTheme.typography.titleSmall, color = DynamicTheme.colors.gray1)
            }
        Spacer(Modifier.size(38.dp))
        if (isShowSignUpText)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dont_have_an_account_pg),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = stringResource(R.string.register_tc),
                    modifier = Modifier
                        .padding(start = 10.dp.computeSize())
                        .clickable(onClick = onRegister),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    textDecoration = TextDecoration.Underline,
                    maxLines = 1
                )
            }
    }

}

@Composable
private fun Separator(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()
    ) {
        VerticalDivider(
            color = Color.Gray,
            modifier = Modifier
                .weight(1f)
                .width(1.dp)
        )

        Text(
            text = stringResource(R.string.or_tc),
            color = Color.White,
            modifier = Modifier.padding(vertical = 16.dp.computeSize()),
            style = MaterialTheme.typography.labelLarge,
        )

        VerticalDivider(
            color = Color.Gray,
            modifier = Modifier
                .weight(1f)
                .width(1.dp)
        )
    }
}

@Composable
private fun GuestAndOtherLogin(
    modifier: Modifier,
    guestLogin: LoginData?,
    onGuestLogin: (LoginData) -> Unit,
    onSelectLogin: (LoginType) -> Unit = {},
    otherLoginList: List<LoginData>,
    showOtherLogins: Boolean = true,
    ) {
    Column(modifier = modifier
        .fillMaxWidth()
        .widthIn(max = 10.dp), verticalArrangement = Arrangement.SpaceBetween) {
        guestLogin?.let {
            GuestLogin(Modifier, it, onGuestLogin)
        }
        if (showOtherLogins)
            OtherLoginMethods(modifier = Modifier, otherLoginList = otherLoginList, onSelectLogin = onSelectLogin)
    }
}

@Composable
private fun GuestLogin(
    modifier: Modifier,
    loginData: LoginData,
    onGuestLogin: (LoginData) -> Unit
) {
    Column(modifier.padding(top = 20.dp.computeSize())) {
        // this caption might not make sense with the "touch to continue" button
        if (loginData.label != stringResource(R.string.touch_to_continue_tc)) Text(
            text = stringResource(R.string.continue_as_tc),
            modifier = Modifier.padding(
                horizontal = 16.dp.computeSize(),
                vertical = 12.dp.computeSize()
            ),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(0.8f)
        )
        ThemeButton(
            onClick = { onGuestLogin(loginData) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("WelcomeScreen:Guest:Button"),
            shape = RoundedCornerShape(20.dp.computeSize()),
            border = BorderStroke(0.dp, Color.Transparent),
            colors = ButtonDefaults.buttonColors().copy(
                containerColor = DynamicTheme.colors.buttonPrimaryDefault,
                disabledContainerColor = DynamicTheme.colors.buttonPrimaryDefault.copy(alpha = 0.3f)
            ),
            contentPadding = PaddingValues(vertical = 10.dp.computeSize())
        ){
            Text(text = loginData.label, style = MaterialTheme.typography.titleSmall, color = DynamicTheme.colors.onTertiary )
        }
    }
}

@Composable
private fun OtherLoginMethods(
    modifier: Modifier,
    otherLoginList: List<LoginData>,
    onSelectLogin: (LoginType) -> Unit = {},
) {
    val itemsPerRow = 4
    val rows = otherLoginList.map { getLoginTypeUiData(it) }.chunked(itemsPerRow)

    Column(
        modifier
            .wrapContentHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        if (rows.isNotEmpty()) Text(
            text = "${stringResource(R.string.other_tc)} ${stringResource(R.string.login_types_tc)}",
            modifier = Modifier.padding(
                horizontal = 16.dp.computeSize(),
                vertical = 12.dp.computeSize()
            ),
            style = MaterialTheme.typography.labelSmall
        )

        rows.forEach { rowItems ->
            Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                rowItems.forEach { item ->
                    Column(modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectLogin(item.type) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val shape = DynamicTheme.corners.medium.makeCornerShape()
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(12.dp)
                                .background(DynamicTheme.colors.background1, shape)
                                .border(
                                    1.dp,
                                    Color.White,
                                    shape,
                                )
                        ) {
                            Icon(
                                painter = painterResource(item.src),
                                contentDescription = "",
                                tint = item.tint,
                                modifier = Modifier
                                    .size(20.dp.computeSize())
                                    .align(Alignment.Center)
                            )
                        }
                        Text(
                            modifier = Modifier.wrapContentSize(),
                            text = item.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (rowItems.size < itemsPerRow) {
                    repeat(itemsPerRow - rowItems.size) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    }
                }

            }
        }

    }

}



@Composable
internal fun WalletLoginComponent(
    modifier: Modifier,
    onBack: () -> Unit = {},
    onClose: () -> Unit = {},
    uiState: AsyncUiState,
) {
    BlurBackgroundOverlay(
        modifier = modifier
            .fillMaxWidth(0.45f)
            .fillMaxHeight(0.7f),
        showCloseButton = false,
        onCloseButtonClick = onClose,
        showBackButton = true,
        onBackButtonClick = onBack) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp.computeSize(), vertical = 12.dp.computeSize()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = stringResource(R.string.login_with_apple_or_google_wallet_tc),
                style = MaterialTheme.typography.headlineMedium,
                color = DynamicTheme.colors.text1
            )
            Spacer(Modifier.padding(top = 12.dp.computeSize()))
            Text(
                text = stringResource(R.string.login_with_apple_or_google_wallet_description_pg),
                style = MaterialTheme.typography.bodySmall,
                color = DynamicTheme.colors.text1,
            )

            Image(
                painter = painterResource(R.drawable.wallet_scan_rfid),
                contentDescription = "WalletScanRFID",
                modifier = Modifier.weight(1f),
                alignment = Alignment.Center
            )
            Spacer(Modifier.padding(top = 30.dp.computeSize()))
        }
    }
}

@Composable
internal fun QrCodeLoginComponent(
    onBackClicked: () -> Unit = {},
    qrUiState: QrUiState = QrUiState(),
) {
    LaunchedEffect(Unit) {
        qrUiState.onShowQrClicked()
    }
    val dialogWidth = if (largerThan16()) 480.dp else 420.dp
    val dialogHeight = if (largerThan16()) 425.dp else 375.dp
    val titleBottomPadding = if (largerThan16()) 24.dp else 12.dp
    val horizontalPadding = if (largerThan16()) 64.dp else 48.dp
    val qrCodeTopPadding = if (largerThan16()) 24.dp else 18.dp
    val titleSmallFontSize = if (largerThan16()) 22.sp else 20.sp
    val titleSmallLineHeight = if (largerThan16()) 30.sp else 28.sp
    val headlineMediumFontSize = if (largerThan16()) 32.sp else 28.sp
    val headlineMediumLineHeight = if (largerThan16()) 40.sp else 36.sp
    val bodySmallLineHeight = if (largerThan16()) 24.sp else 22.sp
    val bodySmallFontSize =  16.sp
    val backgroundShape = DynamicTheme.corners.medium.makeCornerShape()

    Box(
        modifier = Modifier
            .size(dialogWidth.computeSize(), dialogHeight.computeSize())
            .background(color = Color.Black.copy(alpha = 0.9f), shape = backgroundShape)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            //Back icon button
            Row(
                modifier = Modifier
                    .padding(top = 24.dp.computeSize(), start = 24.dp.computeSize())
                    .fillMaxWidth()
                    .clickable { onBackClicked() },
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.caret_left),
                    contentDescription = "Back Icon",
                    tint = DynamicTheme.colors.text1,
                    modifier = Modifier
                        .size(32.dp.computeSize())
                        .testTag("welcomeScreen:QrCodeLogin:BackIcon:Button")
                )
                Spacer(Modifier.width(4.dp.computeSize()))
                Text(
                    text = stringResource(R.string.back_tc),
                    color = DynamicTheme.colors.text1,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = titleSmallFontSize.computeSize(),
                        lineHeight = titleSmallLineHeight.computeSize()
                    ),
                    modifier = Modifier
                        .testTag("welcomeScreen:QrCodeLogin:Back:Text"),
                )
            }
            Spacer(modifier = Modifier.height(24.dp.computeSize()))

            //Title
            Text(
                text = stringResource(R.string.qr_code_login_tc),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = headlineMediumFontSize.computeSize(),
                    lineHeight = headlineMediumLineHeight.computeSize()
                ),
                color = DynamicTheme.colors.text1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = horizontalPadding.computeSize())
                    .testTag("welcomeScreen:QrCodeLogin:Title:Text")
            )
            Spacer(modifier = Modifier.height(titleBottomPadding.computeSize()))

            //Subtext
            Text(
                text = stringResource(R.string.scan_qr_code_to_login_pg),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = bodySmallFontSize.computeSize(),
                    lineHeight = bodySmallLineHeight.computeSize()
                ),
                color = DynamicTheme.colors.text1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = horizontalPadding.computeSize())
                    .testTag("welcomeScreen:QrCodeLogin:Subtext:Text")
            )
            Spacer(modifier = Modifier.height(qrCodeTopPadding.computeSize()))

            Box(
                modifier = Modifier
                    .background(if (qrUiState.qrUri != null) Color.White else Color.Transparent)
                    .size(145.dp.computeSize())
                    .padding(8.dp)
                    .testTag("welcomeScreen:QrCodeLogin:QrCode:Image"),
                contentAlignment = Alignment.Center,
            ) {
                qrUiState.qrUri?.let { uri ->
                    QrCodeView(data = uri, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun PreviewQrCodeLoginComponent(){
    DynamicTheme{
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Cyan),
            contentAlignment = Alignment.Center
        ){
            QrCodeLoginComponent(onBackClicked = {})
        }
    }
}

@AllDevicePreviews
@Composable
private fun PreviewLoginComponent() {
    val loginList = moveLoginToDefault(createPreviewLoginList(), LoginType.Xid)
    val networkAvailable = remember { mutableStateOf(true) }
    DynamicTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Cyan),
            contentAlignment = Alignment.Center
        ) {
            LoginComponent(
                onGuest = {},
                networkAvailable = networkAvailable,
                loginList = loginList,
                onLogin = {},
                guestLogin = loginList.find { it.type == LoginType.Guest },
                onRegister = {},
                onSelectLogin = {},
                onClose = {},
                isShowSignUpText = true,
            )
        }
    }
}

@AllDevicePreviews
@Composable
private fun PreviewLoginComponentWalletDefault() {
    val loginList = moveLoginToDefault(createPreviewLoginList(), LoginType.Wallet)
    val networkAvailable = remember { mutableStateOf(true) }
    DynamicTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Cyan),
            contentAlignment = Alignment.Center
        ) {
            LoginComponent(
                onGuest = {},
                networkAvailable = networkAvailable,
                loginList = loginList,
                onLogin = {},
                guestLogin = loginList.find { it.type == LoginType.Guest },
                onRegister = {},
                onSelectLogin = {},
                onClose = {},
                isShowSignUpText = true,
            )
        }
    }
}

@AllDevicePreviews
@Composable
private fun PreviewLoginComponentQrCodeDefault() {
    val loginList = moveLoginToDefault(
        createPreviewLoginList() + LoginData("QrCode", "QrCode preview description", LoginType.QrCode),
        LoginType.QrCode
    )
    val networkAvailable = remember { mutableStateOf(true) }
    val qrUiState = rememberPreviewQrUiState()
    DynamicTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Cyan),
            contentAlignment = Alignment.Center
        ) {
            LoginComponent(
                onGuest = {},
                networkAvailable = networkAvailable,
                loginList = loginList,
                onLogin = {},
                guestLogin = loginList.find { it.type == LoginType.Guest },
                onRegister = {},
                onSelectLogin = {},
                onClose = {},
                isShowSignUpText = false,
                qrUiState = qrUiState,
            )
        }
    }
}

@Composable
private fun rememberPreviewQrUiState(): QrUiState {
    var shown by remember { mutableStateOf(false) }
    return QrUiState(
        qrUri = if (shown) "https://example.com/preview-qr?user_code=X7Y9-Z2" else null,
        userCode = if (shown) "X7Y9-Z2" else null,
        verificationUri = if (shown) "https://example.com/preview-qr" else null,
        qrShown = shown,
        onShowQrClicked = { shown = true },
    )
}

@AllDevicePreviews
@Composable
private fun PreviewLoginComponentQrCodeDefaultWithWallet() {
    val loginList = moveLoginToDefault(
        createPreviewLoginList()
            + LoginData("QrCode", "QrCode preview description", LoginType.QrCode)
            + LoginData("Wallet", "Wallet preview description", LoginType.Wallet),
        LoginType.QrCode
    )
    val networkAvailable = remember { mutableStateOf(true) }
    val qrUiState = rememberPreviewQrUiState()
    DynamicTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Cyan),
            contentAlignment = Alignment.Center
        ) {
            LoginComponent(
                onGuest = {},
                networkAvailable = networkAvailable,
                loginList = loginList,
                onLogin = {},
                guestLogin = loginList.find { it.type == LoginType.Guest },
                onRegister = {},
                onSelectLogin = {},
                onClose = {},
                isShowSignUpText = false,
                qrUiState = qrUiState,
            )
        }
    }
}

@AllDevicePreviews
@Composable
private fun PreviewLoginComponentWalletPrimary() {
    val loginList = moveLoginToDefault(
        createPreviewLoginList() + LoginData("Wallet", "Wallet preview description", LoginType.Wallet),
        LoginType.Wallet
    )
    val networkAvailable = remember { mutableStateOf(true) }
    DynamicTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Cyan),
            contentAlignment = Alignment.Center
        ) {
            LoginComponent(
                onGuest = {},
                networkAvailable = networkAvailable,
                loginList = loginList,
                onLogin = {},
                guestLogin = loginList.find { it.type == LoginType.Guest },
                onRegister = {},
                onSelectLogin = {},
                onClose = {},
                isShowSignUpText = true,
            )
        }
    }
}
@Composable
private fun moveLoginToDefault(loginList: List<LoginData>, loginType: LoginType): List<LoginData> {
    val editLoginList = loginList.toMutableList()
    editLoginList.find { it.type == loginType }?.let { item ->
        editLoginList.remove(item)
        editLoginList.add(0, item)
    }
    return editLoginList
}
