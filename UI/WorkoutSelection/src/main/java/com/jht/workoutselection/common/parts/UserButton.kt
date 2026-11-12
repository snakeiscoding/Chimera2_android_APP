package com.jht.workoutselection.common.parts

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jht.workoutselection.R
import com.jht.interfaces.models.UserModel
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.widgets.helpers.getInjectionEntryPoint

// For now this button does not actually interact with the screen, so maybe start off as self contained widget?
// If screen needs to take action based on this widget, maybe could break it out or provide parameter to handle that?

@Composable
fun UserButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: ()->Unit = {},
) {
    val context = LocalContext.current
    val guestCaption = stringResource(id = R.string.login_tc)
    val state = remember { UserButtonState(context, guestCaption) }
    LaunchedEffect(Unit) { state.start() }
    DisposableEffect(Unit) { onDispose { state.stop() } }

    if (state.shown.value) {
        ThemeButton(
            onClick = onClick, // should we check state.loginsAvailable before allowing click, or pass as bool, or leave entirely up to parent?
            modifier = Modifier.testTag("User:Button").then(modifier),
            enabled = enabled,
            contentPadding = PaddingValues(20.dp, 10.dp)
        ) {
            // Is this meant to be theme able or fixed?
            Box(
                Modifier
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
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                state.caption.value,
                Modifier.testTag("Username:Text"),
                color = DynamicTheme.colors.text1,
                style = DynamicTheme.typography.textStyle1
            )
        }
    }
}

private class UserButtonState(context: Context, val guestCaption: String) {
    val shown = mutableStateOf(true)
    val caption = mutableStateOf(guestCaption)
    val userModel: UserModel
    val loginsAvailable: Boolean

    init {
        val injection = getInjectionEntryPoint(context)
        userModel = injection.user()
        loginsAvailable = injection.login().getLoginOptions().find { !it.isGuest() } != null
    }

    fun start() {
        userModel.name.listenAndTrigger {
            // name for guest is "Guest", so if you want something else we need to catch it
            if (userModel.isGuest) guestCaption else caption.value = it
            // "We need to show the profile button when someone logs in with RFID and all login options are disabled"
            shown.value = !userModel.isGuest || loginsAvailable
        }
    }
    fun stop() {
        userModel.close()
    }
}


@Preview
@Composable
private fun PreviewUserButton() {
    UserButton()
}
