package com.jht.welcome.chameleon.overlays

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.jht.data.LoginData
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeFlatButton
import com.jht.ui.clickableNoEffect
import com.jht.welcome.R
import com.jht.widgets.FieldInput
import com.jht.widgets.FieldInputCollection
import com.jht.widgets.FieldInputData
import com.jht.widgets.helpers.AsyncUiState


@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun LoginEntryOverlay(
    data : LoginData,
    onLogin: (LoginData) -> Unit,
    uiState: AsyncUiState = AsyncUiState.Unknown,
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


    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .clickableNoEffect { focusManager.clearFocus() }
            .background(DynamicTheme.colors.text2.copy(alpha = 0.9f)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(60.dp),
        ) {
            Text(
                text = data.label,
                color = DynamicTheme.colors.text1,
                style = DynamicTheme.typography.textStyle3,
            )
            if (data.description.isNotEmpty()) Text(
                text = data.description,
                Modifier.padding(top = 10.dp).fillMaxWidth(0.6f),
                color = DynamicTheme.colors.text1,
                style = DynamicTheme.typography.textStyle1.copy(fontWeight = FontWeight.Normal),
            )
            Row {
                Column(Modifier.fillMaxWidth(0.5f)) {
                    val fieldModifier = Modifier.fillMaxWidth()
                    for (field in fieldList) {
                        Spacer(Modifier.size(20.dp))
                        FieldInput(field, fieldModifier, enabled, fieldInputStyle)
                    }
                    if (data.associateDeviceLogin != null) {
                        Row {
                            Text(stringResource(id = R.string.rfid_tag_scanned_tc), color = DynamicTheme.colors.text1)
                            // Should we show ID, or is indicating that "RFID tag was scanned" enough?
                            //Spacer(Modifier.size(10.dp))
                            //Text(data.associateDeviceLogin?.getIdString() ?: "", color = DynamicTheme.colors.text1)
                        }
                    }
                    Spacer(Modifier.size(30.dp))
                    if (data.needsInput()) ThemeFlatButton(
                        onClick = loginAction,
                        Modifier.align(Alignment.End).testTag("LoginButton"),
                        enabled = enabled,
                        isPrimary = true,
                    ) {
                            Text(
                                stringResource(R.string.login_tc),
                                Modifier.requiredWidth(IntrinsicSize.Max).padding(10.dp), // force max width text sizing?
                                color = DynamicTheme.colors.text2,
                                style = DynamicTheme.typography.textStyle1,
                            )
                    }
                }
                if (data.imageInfo != null) {
                    GlideImage(model = data.imageInfo, contentDescription = null, Modifier.padding(20.dp))
                }
            }
            if (uiState == AsyncUiState.Failure) Text(stringResource(id = R.string.login_failed_message_pg), color = DynamicTheme.colors.error)
        }
    }
}

