package com.jht.widgets

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.SystemModel
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.theme.darkDynamicTheme
import com.jht.theme.disabledBackground
import com.jht.theme.lightDynamicTheme
import com.jht.theme.themePadding
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.largerThan16
import com.jht.ui.smallerThan16
import com.jht.widgets.helpers.getInjectionEntryPoint

// Running this as a self contained widget as it basically just exposes some system adjustments
// TODO - the settings pane is still being designed
// Should popup be anchored to button? or should we provide two composables so parent can place popup/overlay wherever makes sense to it?

private const val tag = "SettingsButton"

@Composable
fun SettingsButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val showPopup = remember { mutableStateOf(false) }

    ThemeButton(
        onClick = { showPopup.value = true },
        enabled = enabled,
        contentPadding = PaddingValues(),
        modifier = Modifier
            .testTag(tag)
            .then(modifier)
    ) {
        Icon(
            modifier = Modifier
                .fillMaxSize(.5f)
            ,
            painter = painterResource(id = R.drawable.ic_new_settings_gray),
            contentDescription = null,
            tint = DynamicTheme.colors.text1,
        )
        // can we do an anchor? do we need to create a position provider? or could this be fullscreen with transparency? make separate and let parent handler?
        if (showPopup.value) SettingsPopup(showPopup)
    }
}

@Composable
fun SettingsPopup(
    showPopup: MutableState<Boolean>,
) {
    PopupFullscreen(
        onDismissRequest = { showPopup.value = false }
    ) {
        SettingsPopupInternal { showPopup.value = false }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SettingsPopupInternal(
    dismiss: ()->Unit,
) {
    val context = LocalContext.current
    val state = remember { SettingsButtonState(context) }
    DisposableEffect(Unit) {
        state.start()
        onDispose { state.stop() }
    }

    BoxWithConstraints(
        Modifier
            .semantics { testTagsAsResourceId = true }
            .background(disabledBackground)
            .fillMaxSize()
            .clickableNoEffect(onClick = dismiss)
    ) {
        val volumeWidth = maxWidth * 0.07f
        val buttonHeight = maxHeight * 0.1f
        Row(
            Modifier
                .background(DynamicTheme.colors.gray1)
                .fillMaxHeight()
                .align(Alignment.CenterEnd),
        ) {
            val columnModifier = Modifier
                .fillMaxHeight()
                .padding(themePadding)
            // menu column
            Column(
                columnModifier.width(IntrinsicSize.Min),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val menuButtonModifier = Modifier.fillMaxWidth().height(buttonHeight)
                MenuButton(R.string.device_pairing_tc, R.drawable.ic_new_settings_gray, menuButtonModifier.testTag("$tag:DevicePairing")) {
                    state.navigationModel.launchDevicePairing()
                    dismiss()
                }
            }
            // volume column
            Column(
                Modifier
                    .background(DynamicTheme.colors.gray2)
                    .then(columnModifier)
                    .width(volumeWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(themePadding),
            ) {
                val iconButtonModifier = Modifier.height(buttonHeight).aspectRatio(1f)
                // close
                MenuButton(0, R.drawable.ic_close, iconButtonModifier.testTag("$tag:Close"), dismiss)
                // volume
                VolumeSlider(state, Modifier.weight(1f))
                // mute
                MuteToggle(state, iconButtonModifier)
            }
        }
    }
}

@Composable
private fun MenuButton(
    labelId: Int,
    iconId: Int,
    modifier: Modifier,
    onClick: ()->Unit,
) {
    ThemeButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(themePadding),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (labelId != 0) Arrangement.Start else Arrangement.Center, // or just add horizontalArrangement parameter?
        ) {
            if (iconId != 0) Icon(
                painter = painterResource(id = iconId),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .aspectRatio(1f),
                tint = DynamicTheme.colors.text1,
            )
            if (iconId != 0 && labelId != 0) Spacer(Modifier.size(themePadding))
            if (labelId != 0) {
                Text(
                    stringResource(id = labelId),
                    Modifier.requiredWidth(IntrinsicSize.Max),
                    DynamicTheme.colors.text1,
                    //textAlign = TextAlign.Center,
                    style = menuStyle(),
                )
            }
        }
    }
}

// Should figure out how we want to do this properly? AutoSizeText kind of works but need a way to share least common size? Creative might standardize our sizing anyway?
@Composable
private fun menuStyle(): TextStyle =
    if (largerThan16()) DynamicTheme.typography.textStyle3
    else if (smallerThan16()) DynamicTheme.typography.textStyle1
    else DynamicTheme.typography.textStyle2


@Composable
private fun MuteToggle(
    state: SettingsButtonState,
    modifier: Modifier = Modifier,
) {
    val muteIcon = if (state.muted.value) R.drawable.ic_volume_mute_white else R.drawable.ic_volume_white
    MenuButton(labelId = 0, iconId = muteIcon, modifier = modifier.testTag("$tag:Mute")) {
        state.mute(!state.muted.value)
    }
}

@Composable
private fun VolumeSlider(
    state: SettingsButtonState,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Slider(
            value = state.volume.floatValue,
            onValueChange = { state.volume(it) }, // use onValueChange instead of onValueChangeFinished so user can hear the change?
            steps = 20,
            colors = SliderDefaults.colors(
                thumbColor = DynamicTheme.colors.gray9,
                activeTrackColor = DynamicTheme.colors.primary,
                inactiveTrackColor = DynamicTheme.colors.primary.copy(0.1f),
            ),
            modifier = Modifier
                .testTag("$tag:VolumeSlider")
                .graphicsLayer {
                    rotationZ = 270f
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = constraints.minHeight,
                            maxWidth = constraints.maxHeight,
                            minHeight = constraints.minWidth,
                            maxHeight = constraints.maxHeight,
                        )
                    )
                    layout(placeable.height, placeable.width) {
                        placeable.place(-placeable.width, 0)
                    }
                }
        )
    }
}


private class SettingsButtonState(context: Context) {
    val navigationModel: NavigationModel
    private val systemModel: SystemModel
    val muted = mutableStateOf(false)
    val volume = mutableFloatStateOf(0.5f)

    init {
        val injection = getInjectionEntryPoint(context)
        systemModel = injection.system()
        navigationModel = injection.navigation()
    }

    fun start() {
        systemModel.audioMute.listenAndTrigger { muted.value = it }
        systemModel.audioVolume.listenAndTrigger { volume.floatValue = it }
    }
    fun stop() {
        systemModel.close()
        navigationModel.close()
    }
    fun mute(mute: Boolean) { systemModel.audioMute.value = mute }
    fun volume(volume: Float) { systemModel.audioVolume.value = volume }
}


//region Previews
@Preview
@Composable
private fun PreviewSettingsButton() {
    SettingsButton()
}

@AllDevicePreviews
@Composable
private fun PreviewSettingsPopupDark() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Cyan)) {
        DynamicTheme(darkDynamicTheme) {
            SettingsPopupInternal {}
        }
    }
}

@AllDevicePreviews
@Composable
private fun PreviewSettingsPopupLight() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Cyan)) {
        DynamicTheme(lightDynamicTheme) {
            SettingsPopupInternal {}
        }
    }
}
//endregion Previews
