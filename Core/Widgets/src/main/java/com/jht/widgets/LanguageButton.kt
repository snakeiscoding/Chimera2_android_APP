package com.jht.widgets

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jht.interfaces.models.SystemModel
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.widgets.helpers.getInjectionEntryPoint

@Composable
fun LanguageButton(
    modifier: Modifier = Modifier,
    onClick: ()->Unit = {},
    isLanguageOverlayShowing: MutableState<Boolean> = mutableStateOf(false)) {
    val context = LocalContext.current
    val state = remember { LanguageButtonState(context) }
    LaunchedEffect(Unit) { state.start() }
    DisposableEffect(Unit) { onDispose { state.stop() } }
    ThemeButton(
        modifier = Modifier
            .testTag("LanguageButton")
            .then(modifier),
        contentPadding = PaddingValues(),
        onClick = { if(!isLanguageOverlayShowing.value) onClick() else isLanguageOverlayShowing.value = false },
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors().copy(containerColor = DynamicTheme.colors.gray1),
        border = DynamicTheme.borders.medium.borderStroke(DynamicTheme.colors.gray2)

    ) {
        Icon(
            painterResource(id = if(!isLanguageOverlayShowing.value) R.drawable.ic_icon_globe else R.drawable.ic_close),
            null,
            Modifier.fillMaxSize(0.5f),
            DynamicTheme.colors.text1,
        )
    }
}
private class LanguageButtonState(context: Context) {
    private val systemModel: SystemModel
    val currentLanguageIndex = mutableStateOf(0)
    init {
        val injection = getInjectionEntryPoint(context)
        systemModel = injection.system()
    }
    fun getLanguages() = systemModel.languageList
    fun selectLanguage(index: Int) = systemModel.setLanguage(index)
    fun start() {
        systemModel.currentLanguageIndex.listenAndTrigger { currentLanguageIndex.value = it }
    }
    fun stop() {
        systemModel.close()
    }
}


@Preview
@Composable
private fun PreviewLanguageButton() {
    LanguageButton()
}

@Composable
fun LanguageOverlay(
    showOverlay: MutableState<Boolean>,
) {
    val context = LocalContext.current
    val state = remember { LanguageButtonState(context) }
    DisposableEffect(state) {
        state.start()
        onDispose {
            state.stop()
        }
    }
    if (showOverlay.value) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable {
                    showOverlay.value = false
                }
                .background(Color.Black.copy(alpha = 0.8f))) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(360 / 768f)
                    .background(color = DynamicTheme.colors.text2),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .aspectRatio(360 / 86f)
                        .clickable {
                            showOverlay.value = false
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_left_white),
                        contentDescription = null,
                        tint = DynamicTheme.colors.text1,
                        modifier = Modifier
                            .fillMaxHeight(25 / 86f)
                            .aspectRatio(1f)
                    )
                    Text(
                        text = stringResource(id = R.string.back_uc),
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        color = DynamicTheme.colors.text1,
                        textAlign = TextAlign.Start,
                        style = DynamicTheme.typography.textStyle1
                    )
                }
                Text(
                    text = stringResource(id = R.string.select_language_tc),
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = DynamicTheme.colors.text1,
                    textAlign = TextAlign.Start,
                    style = DynamicTheme.typography.textStyle1
                )

                LazyColumn(
                    modifier = Modifier
                        .padding(top = 15.dp)
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    itemsIndexed(state.getLanguages()) { index, language ->
                        LanguageBox(
                            language, state.currentLanguageIndex.value, index
                        ) { state.selectLanguage(index) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageBox(
    language: String,
    selectedLanguageIndex: Int,
    index: Int,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        //Top white divider line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(DynamicTheme.colors.gray2.copy(alpha = 0.1f))
        )
        Row(
            modifier = Modifier
                .aspectRatio(360 / 60f)
                .clip(shape = RoundedCornerShape(5.dp))
                .clickable { onClick() }
                .then(
                    if (index == selectedLanguageIndex)
                        Modifier.background(color = DynamicTheme.colors.text2)
                    else
                        Modifier.background(color = DynamicTheme.colors.background1)
                ),
            verticalAlignment = Alignment.CenterVertically
        )
        {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(5.dp)
                    .then(
                        if (index == selectedLanguageIndex)
                            Modifier.background(DynamicTheme.colors.primary)
                        else
                            Modifier.background(Color.Transparent)
                    )
            )
            Text(
                text = language,
                modifier = Modifier
                    .padding(horizontal = 15.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                color = DynamicTheme.colors.text1,
                textAlign = TextAlign.Start,
                style = DynamicTheme.typography.textStyle1
            )
        }
        //Bottom white divider line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DynamicTheme.colors.gray2.copy(alpha = 0.1f))
        )
    }
}