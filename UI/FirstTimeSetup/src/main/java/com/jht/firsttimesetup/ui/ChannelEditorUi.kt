package com.jht.firsttimesetup.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jht.firsttimesetup.Channel
import com.jht.firsttimesetup.R
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButton
import com.jht.firsttimesetup.ui.commoncomponents.FTSUButtonType
import com.jht.firsttimesetup.ui.commoncomponents.FTSUTextField
import com.jht.firsttimesetup.ui.commoncomponents.ProgressLoader
import com.jht.firsttimesetup.ui.commoncomponents.ShimmerBrush
import com.jht.firsttimesetup.viewmodels.FtsuRemoteViewModel
import com.jht.interfaces.models.VideoInput
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.widgets.VideoInputBox


@Composable
fun CustomizeChannelView(
    vm: FtsuRemoteViewModel = hiltViewModel(),
    onNextClick: () -> Unit = {},
    onIptvClicked: () -> Unit = {},
    isEdit: Boolean = false,
    onReturnToSummary: () -> Unit = {},
    isEngineeringMode: Boolean,
    showIptvSettingsButton: Boolean,
    showIpTvSettingsDialog: Boolean,
    onHideIpTvSettingsDialog: () -> Unit,
) {

    val uiState = vm.uiState.collectAsStateWithLifecycle()

    val video = vm.videoOutput.collectAsStateWithLifecycle()

    if (showIpTvSettingsDialog) {
        Dialog(
            onDismissRequest = onHideIpTvSettingsDialog,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            IptvSettingsView(
                modifier = Modifier
                    .fillMaxWidth(.5f)
                    .fillMaxHeight(.85f),
                onExitClicked = onHideIpTvSettingsDialog,
                onSaveClicked = {
                    //todo
                },
                onRebootClicked = {
                    //todo
                })
        }
    }


    ChannelEditorView(
        onNextClick = onNextClick,
        onIptvClicked = onIptvClicked,
        channels = uiState.value.channels,
        onSetDefaultChannel = { vm.setDefaultChannel(it) },
        onHideClicked = { vm.toggleHideChannel(it) },
        onChannelItemClicked = { vm.onChannelClicked(it) },
        onSetChannelName = { index -> vm.setChannelName(index) },
        onPreviewChannelName = { name, index -> vm.setChannelNamePreview(index, name) },
        videoInput = video.value,
        isEdit = isEdit,
        onReturnToSummary = onReturnToSummary,
        onTvRemoteAction = { action -> vm.handleRemoteClick(action) },
        isEngineeringMode = isEngineeringMode,
        selectedChannel =  uiState.value.selectedChannel,
        showIptvSettingsButton = showIptvSettingsButton
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChannelEditorView(
    onNextClick: () -> Unit = {},
    onIptvClicked: () -> Unit = {},
    channels: List<Channel> = emptyList(),
    onSetDefaultChannel: (channelUuid: String) -> Unit = {},
    onHideClicked: (channelUuid: String) -> Unit = {},
    onChannelItemClicked: (channelUuid: String) -> Unit = {},
    onSetChannelName: (channelUuid: String) -> Unit = { _ -> },
    onPreviewChannelName: (name: String, channelUuid: String) -> Unit = { _, _ -> },
    videoInput: VideoInput? = null,
    isEdit: Boolean,
    onReturnToSummary: () -> Unit = {},
    onTvRemoteAction: (action: FTSURemoteButtons) -> Unit = {},
    isEngineeringMode: Boolean = false,
    onReturnToTVSource: () -> Unit = {},
    showIptvSettingsButton: Boolean,
    selectedChannel: Channel? = null,
) {

    val localFocusManager = LocalFocusManager.current

    Column(
        Modifier
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    localFocusManager.clearFocus()
                })
            }, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //title
        Text(
            stringResource(R.string.customize_channels_tc),
            style = MaterialTheme.typography.headlineLarge,
            color = colorResource(R.color.light_text),
            modifier = Modifier.testTag("FTSU:TvEdit:Title:Text")
        )

        //sub title
        Text(
            stringResource(R.string.customize_tip_pg),
            style = MaterialTheme.typography.bodyLarge,
            color = colorResource(R.color.light_text),
            modifier = Modifier.testTag("FTSU:TvEdit:Subtitle:Text")
        )

        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 64.dp, vertical = 24.dp.computeSize()),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            //channel list
            if (channels.isEmpty()) {
                ChannelListSkeletonLoader(
                    Modifier.weight(3 / 12f)
                )
            } else {
                ChannelList(
                    modifier = Modifier.weight(3 / 12f),
                    channels,
                    selectedChannel?.uuid,
                    onHideClicked = { uuid ->
                        onHideClicked(uuid)
                    },
                    onItemClick = { uuid ->
                        onChannelItemClicked(uuid)
                    }
                )
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp)
                    .weight(6 / 12f),
            ) {
                //edit channel
                val editChannel = channels.find { it.uuid == selectedChannel?.uuid }
                editChannel?.let {
                    EditChannelComponent(
                        modifier = Modifier,
                        channel = it,
                        onFavoriteToggle = { isChecked ->
                            selectedChannel?.uuid?.let {
                                onSetDefaultChannel(if (isChecked) selectedChannel.uuid else "")
                            }
                        },
                        onChangeChannelName = {
                            selectedChannel?.uuid?.let {
                                onSetChannelName(selectedChannel.uuid)
                            }
                        },
                        onPreviewChannelName = { name ->
                            selectedChannel?.uuid?.let{
                                onPreviewChannelName(
                                    name, selectedChannel.uuid
                                )
                            }
                        }
                    )
                }

                Spacer(Modifier.size(20.dp.computeSize()))

                //tv viewer
                // Largest 16:9 that fits both the column width and the height left below the editor.
                Card(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .aspectRatio(16f / 9f, matchHeightConstraintsFirst = true),
                    colors = CardDefaults.cardColors().copy(colorResource(R.color.grey_5)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (selectedChannel?.isHidden?.not() ?: true){
                            videoInput?.let {
                                VideoInputBox(
                                    Modifier
                                        .align(Alignment.Center)
                                        .fillMaxSize()
                                        .border(
                                            8.dp,
                                            colorResource(R.color.grey_5),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clip(shape = RoundedCornerShape(34.dp)),
                                    videoInput = videoInput
                                )
                            } ?: run {
                                ProgressLoader(
                                    centerText = stringResource(R.string.searching_tc),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .fillMaxHeight(.8f)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,

                            ) {
                                Text(
                                    stringResource(R.string.channel_is_hidden_tc),
                                    textAlign = TextAlign.Center,
                                    color = colorResource(R.color.light_text),
                                    style = TextStyle.Default.copy(fontSize = 20.sp)
                                )

                                Icon(
                                    modifier = Modifier.size(64.dp),
                                    painter = painterResource(R.drawable.hide),
                                    contentDescription = "Hidden Channel Help Icon",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            //remote
            FTSUTVController(
                modifier = Modifier.weight(2 / 12f),
                handleRemoteClickAction = onTvRemoteAction
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            //text button
            if (showIptvSettingsButton) {
                FTSUButton(
                    buttonType = FTSUButtonType.Tertiary,
                    label = stringResource(R.string.iptv_settings_tc),
                    imageStart = {
                        Icon(
                            painter = painterResource(R.drawable.setting_gear),
                            "setting gear",
                            tint = colorResource(R.color.light_text)
                        )
                    },
                    onClick = { onIptvClicked() }
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }

            //next button
            FTSUButton(
                modifier = Modifier.height(60.dp),
                label = if (isEdit) stringResource(R.string.save_tc) else stringResource(R.string.next_tc),
                imageEnd = {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        "",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp.computeSize())
                    )
                },
                onClick = if (isEdit) {
                    onReturnToSummary
                } else {
                    onNextClick
                }
            )
        }
    }
}

@AllDevicePreviews
@Composable
private fun PreviewChannelEditor() {
    ChannelEditorView(videoInput = VideoInput(), isEdit = false, showIptvSettingsButton = true)
}

@Composable
private fun EditChannelComponent(
    modifier: Modifier,
    channel: Channel?,
    onFavoriteToggle: (Boolean) -> Unit,
    onChangeChannelName: () -> Unit,
    onPreviewChannelName: (name: String) -> Unit
) {

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
    ) {
        FTSUTextField(
            modifier = Modifier
                .fillMaxWidth(.33f)
                .height(80.dp),
            label = stringResource(R.string.custom_channel_name_tc),
            text = channel?.name ?: "",
            testTag = "FTSU:TvEdit:ChangeName",
            onTextChange = {
                onPreviewChannelName(it)
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions {
                onChangeChannelName()
                focusManager.clearFocus()
                keyboardController?.hide()
            }

        )

        Spacer(Modifier.size(10.dp.computeSize()))

        Column(Modifier.align(Alignment.CenterVertically)) {
            val checkboxColors = CheckboxDefaults.colors().copy(
                checkedBoxColor = colorResource(R.color.blue_1),
                checkedBorderColor = colorResource(R.color.blue_1),
                checkedCheckmarkColor = colorResource(R.color.grey_0)
            )

            if (channel?.isHidden == false) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Checkbox(
                        channel.isDefault,
                        onCheckedChange = onFavoriteToggle,
                        colors = checkboxColors,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(Modifier.size(4.dp.computeSize()))

                    Icon(
                        painter = painterResource(R.drawable.star),
                        "Favorite Star",
                        tint = colorResource(R.color.blue_1),
                        modifier = Modifier
                            .size(20.dp)
                            .padding(0.dp)
                    )

                    Spacer(Modifier.size(4.dp.computeSize()))

                    Text(
                        stringResource(R.string.set_default_channel_tc),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorResource(R.color.light_text),
                        modifier = Modifier.padding(0.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ChannelSkeletonLoader() {
    ChannelListSkeletonLoader()
}


/**
 * Empty box for channel list preview
 */
@Composable
private fun ChannelListEmptyView(
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .verticalScroll(state = rememberScrollState())
    ) {
        //displays 10 empty channel boxes
        (0 until 10).forEach { x ->
            Card(
                colors = CardDefaults.cardColors(colorResource(R.color.language_card_color)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .testTag("")
                    .fillMaxWidth()
                    .height(50.dp.computeSize())
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                ) { }
            }
        }
    }
}

/**
 * Skeleton Loader for tv channel
 */
@Composable
private fun ChannelListSkeletonLoader(
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .verticalScroll(state = rememberScrollState())
    ) {
        for (x in 0 until 10) {
            Card(
                colors = CardDefaults.cardColors(colorResource(R.color.language_card_color)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .testTag("")
                    .fillMaxWidth()
                    .height(50.dp.computeSize())
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = ShimmerBrush())

                ) {

                }
            }
        }
    }
}


@Composable
fun ChannelList(
    modifier: Modifier = Modifier,
    channels: List<Channel>,
    selectedChannelUuid: String?,
    onItemClick: (channelUuid: String) -> Unit,
    onHideClicked: (channelUuid: String) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .heightIn(max = 800.dp)
    ) {
        itemsIndexed(channels) { i, channel ->
            ChannelListItem(
                name = channel.name,
                isSelected = channel.uuid == selectedChannelUuid,
                isDefault = channel.isDefault,
                isHidden = channel.isHidden,
                onHideClicked = { onHideClicked(channel.uuid) },
                onClick = { onItemClick(channel.uuid) },
                itemIndex = i
            )
        }
    }
}

@Preview
@Composable
private fun ChannelListPrev() {
    val c1 = Channel("MTV", "1", true, true)
    val c2 = Channel("Animal Planet", "2", false, false)
    val c3 = Channel("HGTV", "3", false, false)
    val c4 = Channel("Discovery Channel", "4", false, false)

    val list: List<Channel> = listOf(c1, c2, c3, c4)

    ChannelList(
        channels = list,
        selectedChannelUuid = "",
        modifier = Modifier,
        onHideClicked = {},
        onItemClick = {}
    )
}

@Composable
private fun ChannelListItem(
    name: String,
    isSelected: Boolean,
    isDefault: Boolean,
    isHidden: Boolean,
    onHideClicked: () -> Unit = {},
    onClick: () -> Unit = {},
    itemIndex: Int
) {
    val cardColor = if (isSelected) colorResource(R.color.blue_1).copy(alpha = .2f)
    else colorResource(R.color.language_card_color)
    val borderColor = if (isSelected) colorResource(R.color.grey_4)
    else colorResource(R.color.language_card_color)

    Card(
        colors = CardDefaults.cardColors(cardColor),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(2.dp, borderColor),
        onClick = onClick,
        modifier = Modifier.testTag("FTSU:TvEdit:ChannelListItem:$itemIndex:Button")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Favorite star
                if (isDefault) {
                    Icon(
                        painter = painterResource(R.drawable.star),
                        "Favorite Star",
                        tint = colorResource(R.color.blue_1),
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("FTSU:TvEdit:ChanelListItem:$itemIndex:DefaultIcon:Image")
                    )
                }

                Spacer(Modifier.size(6.dp.computeSize()))

                // Channel name
                Text(
                    name,
                    color = colorResource(R.color.light_text),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.testTag("FTSU:TvEdit:ChanelListItem:$itemIndex:ChannelName:Text")
                )
            }

            // Hide button
            IconButton(
                onClick = onHideClicked,
                modifier = Modifier
                    .height(50.dp)
                    .width(60.dp)
                    .border(
                        1.dp,
                        shape = RoundedCornerShape(4.dp.computeSize()),
                        color = colorResource(R.color.grey_1)
                    )
                    .testTag("FTSU:TvEdit:ChanelListItem:$itemIndex:HiddenIcon:Button"),
            ) {
                Icon(
                    painter = painterResource(if (isHidden) R.drawable.hide else R.drawable.show),
                    "Hidden channel icon",
                    modifier = Modifier
                        .padding(4.dp)
                        .testTag("FTSU:TvEdit:ChanelListItem:$itemIndex:HiddenIcon:Image"),
                    tint = colorResource(R.color.light_text)
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewChanListItem() {
    ChannelListItem("MTV", true, true, false, onHideClicked = {}, itemIndex = 0)
}

@Preview
@Composable
private fun PreviewChanListHiddenChannelItem() {
    ChannelListItem("MTV", true, true, true, onHideClicked = {}, itemIndex = 0)
}


@Composable
fun FTSUTVController(
    modifier: Modifier = Modifier, selectedTab: Int = 0,
    handleRemoteClickAction: (action: FTSURemoteButtons) -> Unit = {}
) {

    Column(
        modifier
            .width(IntrinsicSize.Min)
            .fillMaxHeight()
    ) {
        var curTab by remember { mutableIntStateOf(selectedTab) }

        Card(
            modifier = modifier.fillMaxHeight(),
            colors = CardDefaults.cardColors().copy(containerColor = colorResource(R.color.grey_7)),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            when (curTab) {
                0 -> FTSURemote(
                    Modifier
                        .fillMaxWidth()
                        .scaleDownToFitHeight(),
                    onClick = handleRemoteClickAction
                )

                1 -> {
                    Spacer(
                        Modifier
                            .size(5.dp)
                            .weight(1f)
                    )

                    FTSUKeypad(
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        onKeypadClick = handleRemoteClickAction
                    )

                    Spacer(
                        Modifier
                            .size(5.dp)
                            .weight(1f)
                    )
                }
            }
        }

        FtsuTabRow(curTab, onRemoteClick = {
            curTab = 0
        }, onKeypadClick = {
            curTab = 1
        })
    }
}

/**
 * Measures the content at its natural height and, if that is taller than the space available,
 * scales it down uniformly (from the top centre) to fit. For fixed-design controls such as the remote,
 * whose d-pad and button sizes add up to ~500dp, where shrinking beats clipping or scrolling.
 * The scale is applied as a layer, so touch targets scale with the drawing.
 */
private fun Modifier.scaleDownToFitHeight(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(
        constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
    )
    val maxHeight = constraints.maxHeight
    if (maxHeight == Constraints.Infinity || placeable.height <= maxHeight) {
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    } else {
        val scale = maxHeight.toFloat() / placeable.height
        layout(placeable.width, maxHeight) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
        }
    }
}

@Preview
@Composable
private fun PreviewFtsuControllerRemote() {
    FTSUTVController()
}

@Preview
@Composable
private fun PreviewFtsuControllerKeypad() {
    FTSUTVController(selectedTab = 1)
}

enum class FTSURemoteButtons {
    ok, down, left, right, up, back, menu, closedCaption, volUp, volDown, chanUp, chanDown, zero, one, two, three, four, five, six, seven, eight, nine, enter, dash,
}

@Composable
fun FTSURemote(
    modifier: Modifier = Modifier,
    onClick: (FTSURemoteButtons) -> Unit = {},
) {
    Card(
        colors = CardDefaults.cardColors().copy(containerColor = colorResource(R.color.grey_7)),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = modifier,

        ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()
        ) {
            val circleColor = colorResource(R.color.grey_5)

            //remote
            Spacer(Modifier.size(40.dp))

            //dpad
            Box(
                modifier = Modifier
                    .size(195.dp)
                    .clip(CircleShape)
                    .drawBehind {
                        drawCircle(
                            color = circleColor
                        )
                    },
            ) {
                val buttonColor = colorResource(R.color.grey_7)
                Button(
                    onClick = { onClick(FTSURemoteButtons.ok) },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(84.dp)
                        .height(84.dp)
                        .testTag("FTSU:TvEdit:Remote:OK:Button"),
                    colors = ButtonColors(
                        buttonColor,
                        contentColor = Color.Unspecified,
                        disabledContainerColor = Color.Unspecified,
                        disabledContentColor = Color.Unspecified
                    )
                ) {
                    Text(
                        stringResource(R.string.ok_uc),
                        color = colorResource(R.color.light_text),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                IconButton(
                    onClick = { onClick(FTSURemoteButtons.up) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(48.dp)
                        .testTag("FTSU:TvEdit:Remote:UpArrow:Button")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.caret_up),
                        "up arrow dpad",
                        tint = colorResource(R.color.light_text),
                    )
                }

                IconButton(
                    onClick = { onClick(FTSURemoteButtons.left) },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(48.dp)
                        .testTag("FTSU:TvEdit:Remote:LeftArrow:Button")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.caret_left),
                        "left arrow dpad",
                        tint = colorResource(R.color.light_text),
                    )
                }

                IconButton(
                    onClick = { onClick(FTSURemoteButtons.right) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(48.dp)
                        .testTag("FTSU:TvEdit:Remote:RightArrow:Button")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.caret_right_new_bold),
                        "right arrow dpad",
                        tint = colorResource(R.color.light_text),
                    )
                }

                IconButton(
                    onClick = { onClick(FTSURemoteButtons.down) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(48.dp)
                        .testTag("FTSU:TvEdit:Remote:DownArrow:Button")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.caret_down),
                        "down arrow dpad",
                        tint = colorResource(R.color.light_text),
                    )
                }
            }


            // vol, button cluster & ch rockers
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .height(IntrinsicSize.Max)
                    .padding(start = 16.dp, top = 32.dp, end = 16.dp, bottom = 30.dp)
                    .fillMaxWidth()
            ) {
                //vol rocker
                RemoteRocker(
                    label = stringResource(R.string.remote_vol_tc),
                    containerColor = circleColor,
                    upIcon = painterResource(R.drawable.plus),
                    upDescription = "Volume Up",
                    upTestTag = "FTSU:TvEdit:Remote:VolumeUp:Button",
                    onUp = { onClick(FTSURemoteButtons.volUp) },
                    downIcon = painterResource(R.drawable.minus),
                    downDescription = "volume down",
                    downTestTag = "FTSU:TvEdit:Remote:VolumeDown:Button",
                    onDown = { onClick(FTSURemoteButtons.volDown) },
                )

                //icon button cluster
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        colors = IconButtonColors(
                            containerColor = circleColor,
                            contentColor = Color.Unspecified,
                            disabledContainerColor = Color.Unspecified,
                            disabledContentColor = Color.Unspecified
                        ),
                        onClick = { onClick(FTSURemoteButtons.menu) },
                        modifier = Modifier
                            .size(60.dp)
                            .testTag("FTSU:TvEdit:Remote:TVList:Button")
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.tv_guide),
                            "TV List",
                            tint = colorResource(R.color.light_text),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        { onClick(FTSURemoteButtons.closedCaption) },
                        colors = IconButtonColors(
                            containerColor = circleColor,
                            contentColor = Color.Unspecified,
                            disabledContainerColor = Color.Unspecified,
                            disabledContentColor = Color.Unspecified
                        ),
                        modifier = Modifier
                            .size(60.dp)
                            .testTag("FTSU:TvEdit:Remote:ClosedCaption:Button")
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.closed_caption),
                            "closed caption",
                            tint = colorResource(R.color.light_text),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = { onClick(FTSURemoteButtons.back) },
                        colors = IconButtonColors(
                            containerColor = circleColor,
                            contentColor = Color.Unspecified,
                            disabledContainerColor = Color.Unspecified,
                            disabledContentColor = Color.Unspecified
                        ),
                        modifier = Modifier
                            .size(60.dp)
                            .testTag("FTSU:TvEdit:Remote:Return:Button")
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            "back arrow",
                            tint = colorResource(R.color.light_text),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                //ch rocker
                RemoteRocker(
                    label = stringResource(R.string.remote_ch_tc),
                    containerColor = circleColor,
                    upIcon = painterResource(R.drawable.caret_up),
                    upDescription = "Channel Up",
                    upTestTag = "FTSU:TvEdit:Remote:ChannelUp:Button",
                    onUp = { onClick(FTSURemoteButtons.chanUp) },
                    downIcon = painterResource(R.drawable.caret_down),
                    downDescription = "channel down",
                    downTestTag = "FTSU:TvEdit:Remote:ChannelDown:Button",
                    onDown = { onClick(FTSURemoteButtons.chanDown) },
                )
            }
        }
    }
}

/**
 * Vol/Ch rocker pill. The top half of the pill is the up button and the bottom half the down button,
 * like a physical rocker, so each touch target is half the pill (~50x100dp) rather than a 32dp icon.
 * The label sits on top and is not itself clickable.
 */
@Composable
private fun RemoteRocker(
    label: String,
    containerColor: Color,
    upIcon: Painter,
    upDescription: String,
    upTestTag: String,
    onUp: () -> Unit,
    downIcon: Painter,
    downDescription: String,
    downTestTag: String,
    onDown: () -> Unit,
) {
    Card(
        colors = CardColors(
            containerColor,
            contentColor = Color.Unspecified,
            disabledContainerColor = Color.Unspecified,
            disabledContentColor = Color.Unspecified,
        ),
        shape = RoundedCornerShape(50.dp),
    ) {
        Box(
            modifier = Modifier
                .width(50.dp)
                .fillMaxHeight()
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = onUp)
                        .testTag(upTestTag)
                ) {
                    Icon(
                        painter = upIcon,
                        contentDescription = upDescription,
                        tint = colorResource(R.color.light_text),
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .size(32.dp)
                    )
                }

                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = onDown)
                        .testTag(downTestTag)
                ) {
                    Icon(
                        painter = downIcon,
                        contentDescription = downDescription,
                        tint = colorResource(R.color.light_text),
                        modifier = Modifier
                            .padding(bottom = 10.dp)
                            .size(32.dp)
                    )
                }
            }

            Text(
                label,
                color = colorResource(R.color.light_text),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun FTSUKeypad(
    modifier: Modifier = Modifier,
    onKeypadClick: (action: FTSURemoteButtons) -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors().copy(containerColor = colorResource(R.color.grey_7)),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            Modifier
                .padding(horizontal = 20.dp, vertical = 23.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column {
                Row {
                    KeypadButton(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("FTSU:TvEdit:Keypad:1:Button"),
                        label = "1",
                        onClick = { onKeypadClick(FTSURemoteButtons.one) })

                    KeypadButton(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("FTSU:TvEdit:Keypad:2:Button"),
                        label = "2",
                        onClick = { onKeypadClick(FTSURemoteButtons.two) })

                    KeypadButton(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("FTSU:TvEdit:Keypad:3:Button"),
                        label = "3",
                        onClick = { onKeypadClick(FTSURemoteButtons.three) })
                }

                Row {
                    KeypadButton(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("FTSU:TvEdit:Keypad:4:Button"),
                        label = "4",
                        onClick = { onKeypadClick(FTSURemoteButtons.four) })

                    KeypadButton(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("FTSU:TvEdit:Keypad:5:Button"),
                        label = "5",
                        onClick = { onKeypadClick(FTSURemoteButtons.five) })

                    KeypadButton(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("FTSU:TvEdit:Keypad:6:Button"),
                        label = "6",
                        onClick = { onKeypadClick(FTSURemoteButtons.six) })
                }

                Row {
                    KeypadButton(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("FTSU:TvEdit:Keypad:7:Button"),
                        label = "7",
                        onClick = { onKeypadClick(FTSURemoteButtons.seven) })

                    KeypadButton(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("FTSU:TvEdit:Keypad:8:Button"),
                        label = "8",
                        onClick = { onKeypadClick(FTSURemoteButtons.eight) })

                    KeypadButton(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("FTSU:TvEdit:Keypad:9:Button"),
                        label = "9",
                        onClick = { onKeypadClick(FTSURemoteButtons.nine) })
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KeypadButton(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("FTSU:TvEdit:Keypad:-:Button"),
                        label = "-",
                        onClick = { onKeypadClick(FTSURemoteButtons.dash) })

                    KeypadButton(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("FTSU:TvEdit:Keypad:0:Button"),
                        label = "0",
                        onClick = { onKeypadClick(FTSURemoteButtons.zero) })

                    KeypadButton(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("FTSU:TvEdit:Keypad:0:Button"),
                        onClick = { onKeypadClick(FTSURemoteButtons.enter) },
                        iconLabel = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f)
                            ) {
                                IconButton(
                                    onClick = { onKeypadClick(FTSURemoteButtons.enter) },
                                    colors = IconButtonColors(
                                        containerColor = colorResource(R.color.grey_5),
                                        contentColor = Color.Unspecified,
                                        disabledContainerColor = Color.Unspecified,
                                        disabledContentColor = Color.Unspecified
                                    ),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .aspectRatio(1f)
                                        .testTag("FTSU:TvEdit:Keypad:Check:Button")
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.check_bold),
                                        "Check Button",
                                        tint = colorResource(R.color.light_text),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        })
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    modifier: Modifier = Modifier,
    label: String? = null,
    onClick: () -> Unit = {},
    iconLabel: @Composable () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors().copy(containerColor = colorResource(R.color.grey_5)),
        modifier = modifier
            .padding(7.dp.computeSize())
            .width(60.dp.computeSize())
            .height(60.dp.computeSize())
            .aspectRatio(1f),
        shape = RoundedCornerShape(5.dp.computeSize()),
        onClick = { onClick() }) {
        Box(Modifier.fillMaxSize()) {
            label?.let {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorResource(R.color.light_text),
                    modifier = Modifier.align(Alignment.Center)
                )
            } ?: run {
                iconLabel()
            }
        }
    }
}


@Preview
@Composable
private fun PreviewKeypad() {
    FTSUKeypad(
        onKeypadClick = { },
    )
}


@Composable
private fun FtsuTabRow(
    selectedTab: Int = 0,
    onRemoteClick: () -> Unit = {},
    onKeypadClick: () -> Unit = {},
) {
    Row(Modifier.height(IntrinsicSize.Min)) {
        FtsuTab(
            modifier = Modifier
                .weight(1f)
                .testTag("FTSU:TvEdit:RemoteTab:Button"),
            icon = painterResource(R.drawable.remote),
            label = stringResource(R.string.remote_tc),
            isSelected = selectedTab == 0,
            onClick = onRemoteClick,
            shape = RoundedCornerShape(bottomStart = 20.dp)
        )

        FtsuTab(
            modifier = Modifier
                .weight(1f)
                .testTag("FTSU:TvEdit:KeypadTab:Button"),
            icon = painterResource(R.drawable.keypad),
            label = stringResource(R.string.keypad_tc),
            isSelected = selectedTab == 1,
            onClick = onKeypadClick,
            shape = RoundedCornerShape(bottomEnd = 20.dp)
        )
    }
}

@Composable
private fun FtsuTab(
    modifier: Modifier = Modifier,
    icon: Painter,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit = {},
    shape: RoundedCornerShape,
) {
    val cardColor = if (isSelected) colorResource(R.color.grey_5)
    else colorResource(R.color.grey_6)

    Card(
        modifier = modifier.width(IntrinsicSize.Max), colors = CardColors(
            containerColor = cardColor,
            contentColor = Color.Unspecified,
            disabledContainerColor = Color.Unspecified,
            disabledContentColor = Color.Unspecified
        ), shape = shape, onClick = onClick
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //line
            HorizontalDivider(
                thickness = 2.dp, color = if (isSelected) colorResource(R.color.blue_1)
                else colorResource(R.color.grey_5)
            )

            Spacer(Modifier.size(8.dp))

            //icon
            val unSelectedColor = Color.White.copy(.6f)
            Icon(
                painter = icon, "", tint = if (isSelected) colorResource(R.color.blue_2)
                else unSelectedColor, modifier = Modifier.size(32.dp)

            )

            Spacer(Modifier.size(2.dp))
            //text
            Text(
                label,
                modifier = Modifier.padding(bottom = 11.dp),
                color = if (isSelected) colorResource(R.color.blue_1)
                else unSelectedColor
            )
        }
    }
}

@Preview
@Composable
private fun RemotePreview() {
    FTSURemote()
}

@Preview
@Composable
private fun FtsuTabBarPre() {
    FtsuTabRow()
}

@Preview
@Composable
private fun EmptyChannelListPreivew() {
    ChannelListEmptyView(modifier = Modifier.fillMaxSize(.8F))
}