package com.jht.widgets

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.interfaces.models.UIBaseModel
import com.jht.theme.DynamicTheme
import com.jht.widgets.helpers.getInjectionEntryPoint

/** Show demo mode icon and/or debug indications. (Based on old TopBarDebugIndicator) */
@Composable
fun DebugIndicator(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state = remember { DebugIndicatorState(context) }
    LaunchedEffect(Unit) { state.start() }
    DisposableEffect(Unit) { onDispose { state.stop() } }

    if (state.demoMode.value || state.debugText.isNotEmpty()) { // row should not take up space if empty
        Row(
            modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.demoMode.value) {
                Icon(
                    painterResource(id = R.drawable.ic_demo_mode),
                    null,
                    tint = DynamicTheme.colors.text1,
                )
            }
            if (state.debugText.isNotEmpty()) {
                Text(
                    state.debugText,
                    Modifier.wrapContentHeight(),
                    color = DynamicTheme.colors.error,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                )
            }
        }
    }
}
private class DebugIndicatorState(context: Context) {
    val demoMode = mutableStateOf(false)
    val debugText: String
    val uiBaseModel: UIBaseModel

    init {
        val injection = getInjectionEntryPoint(context)
        uiBaseModel = injection.uiBase()
        val debugBuild = uiBaseModel.isDebugBuild
        val persistent = Settings.Secure.getInt(context.contentResolver, "jht_disable_persistent", 0) == 0;

        val sb = StringBuilder()
        if (debugBuild) {
            sb.append("DEBUG BUILD") //NON-NLS
            sb.append(if (persistent) "\nPERS" else "\nNO PERS") //NON-NLS
        } else {
            if (!persistent) sb.append("\nNO PERS"); //NON-NLS
        }
        debugText = sb.toString()
    }

    fun start() {
        uiBaseModel.isDemoMode.listenAndTrigger { demoMode.value = it }
    }
    fun stop() {
        uiBaseModel.close()
    }
}