package com.jht.welcome.vortex.components

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jht.data.LoginData
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.largerThan16
import com.jht.welcome.R
import com.jht.welcome.chameleon.overlays.createPreviewLoginList
import com.jht.widgets.BlurBackgroundOverlay
import com.jht.widgets.FieldInput
import com.jht.widgets.FieldInputCollection
import com.jht.widgets.FieldInputData
import com.jht.widgets.helpers.AsyncUiState


@Composable
internal fun OtherLoginMethodsComponent(
    onBack: () -> Unit = {},
    onClose: () -> Unit = {},
    onLogin: (LoginData) -> Unit = {},
    uiState: AsyncUiState = AsyncUiState.Unknown,
    title: String,
    errorTitle: String,
    loginData: LoginData,
    isShowDefaultLoginState: Boolean
) {
    BlurBackgroundOverlay(
        modifier = Modifier.fillMaxWidth(if (largerThan16()) 0.38f else 0.5f)
            .height(IntrinsicSize.Min),
        showCloseButton = false,
        showBackButton = true,
        onCloseButtonClick = onClose,
        onBackButtonClick = onBack
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 45.dp.computeSize(),
                        horizontal = 45.dp.computeSize()
                    ),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = DynamicTheme.colors.text1,
                )
                Spacer(modifier = Modifier.size(32.dp))

                if (isShowDefaultLoginState)
                    LoginTypeComponent(
                        modifier = Modifier.fillMaxWidth(),
                        data = loginData,
                        onLogin = onLogin,
                        uiState = uiState,
                        errorTitle = errorTitle
                    )
            }
        }
    }
}

@Composable
private fun LoginTypeComponent(
    modifier: Modifier,
    data: LoginData,
    onLogin: (LoginData) -> Unit,
    uiState: AsyncUiState = AsyncUiState.Unknown,
    errorTitle: String
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
        val fieldModifier = Modifier
            .fillMaxWidth()

        fieldList.forEachIndexed { index, field ->
            if (index != 0) {
                Spacer(Modifier.size(20.dp.computeSize()))
            }
                FieldInput(field, fieldModifier, enabled)
        }
        Spacer(Modifier.size(24.dp.computeSize()))
        if (uiState == AsyncUiState.Failure)
            Column {
                Text(
                    errorTitle,
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
                    .testTag("LoginButton"),
                shape = RoundedCornerShape(20.dp.computeSize()),
                border = BorderStroke(0.dp, Color.Transparent),
                colors = ButtonDefaults.buttonColors().copy(
                    containerColor = DynamicTheme.colors.buttonPrimaryDefault,
                    disabledContainerColor = DynamicTheme.colors.buttonPrimaryDefault.copy(alpha = 0.3f)
                ),
                contentPadding = PaddingValues(vertical = 10.dp.computeSize())
            ) {
                Text(
                    text = stringResource(R.string.sign_in_tc),
                    style = MaterialTheme.typography.titleSmall,
                    color = DynamicTheme.colors.onTertiary
                )
            }

    }
}

@SuppressLint("UnusedBoxWithConstraintsScope") // WTaF
@AllDevicePreviews
@Composable
private fun PreviewOtherLogins() {
    val logins = createPreviewLoginList()
    val login = logins.first()
    DynamicTheme {
        BoxWithConstraints(
            Modifier.fillMaxSize().background(Color.Cyan),
            contentAlignment = Alignment.Center
        ) {
            OtherLoginMethodsComponent(
                title = "Title String",
                errorTitle = "Error String",
                loginData = login,
                isShowDefaultLoginState = true,
            )
        }
    }
}
