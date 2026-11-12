package com.jht.firsttimesetup.viewmodels

import com.jht.firsttimesetup.Channel
import com.jht.firsttimesetup.FirstTimeSetupModel
import com.jht.firsttimesetup.ui.FTSURemoteButtons
import com.jht.interfaces.models.VideoInput
import com.jht.interfaces.models.VideoInputModel
import com.jht.interfaces.models.VideoInputType
import com.jht.theme.ThemedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class FtsuRemoteViewModel @Inject constructor(
    private val ftsuModel: FirstTimeSetupModel,
    private val videoInputModel: VideoInputModel
) : ThemedViewModel() {
    private var _uiState = MutableStateFlow(EditChannelModel())

    val uiState = _uiState.asStateFlow()

    private val _videoInput = MutableStateFlow(VideoInput(VideoInputType.TV_VIEW))

    val videoOutput = _videoInput.asStateFlow()

    init {
        _videoInput.value = videoInputModel.applyVideoInput()
        updateChannelsList()
        _uiState.value = _uiState.value.copy(selectedChannel = _uiState.value.channels.firstOrNull())
    }

    private fun updateChannelsList() {
        val channels = ftsuModel.getTvChannelList()
        _uiState.value = _uiState.value.copy(
            channels = channels
        )
    }

    override fun stop() {
        videoInputModel.close()
    }

    override fun destroy() {
        ftsuModel.closeTuner()
        videoInputModel.close()
    }

    /**
     * Sets name for new channel in the UI
     * Note: this does not actually change the name of the channel
     */
    fun setChannelNamePreview(channelUuid: String, name: String) {
        val channels = _uiState.value.channels.map { channel ->
            if (channel.uuid == channelUuid) {
                channel.copy(name = name)
            } else {
                channel
            }
        }
        _uiState.value = _uiState.value.copy(channels = channels)
    }

    /**
     * Sets channel name when user submit channel name
     */
    fun setChannelName(channelUuid: String) {
        //trim here in case users input any line break of spaces
        val name = _uiState.value.channels.find { it.uuid == channelUuid }?.name?.trim()
        name?.let {
            ftsuModel.setChannelCustomName(name, channelUuid)
        }
        updateChannelsList()
    }

    /**
     * Previews channel name in ui
     */

    fun toggleHideChannel(channelUuid: String) {
        val isHidden = _uiState.value.channels.find { it.uuid == channelUuid }?.isHidden
        isHidden?.let { isHidden ->
            ftsuModel.toggleHideChannel(channelUuid, !isHidden)
        }
        updateChannelsList()
        onChannelClicked(channelUuid)
    }

    /**
     * Sets the default channel based on
     * @param [channelUuid]
     */
    fun setDefaultChannel(channelUuid: String) {
        ftsuModel.setDefaultChannel(channelUuid)
        updateChannelsList()
    }

    /**
     * Sets channel based on @param [channelUuid]
     */
    fun onChannelClicked(channelUuid: String) {
        val channel = _uiState.value.channels.find {
            it.uuid == channelUuid
        }
        _uiState.value = _uiState.value.copy(selectedChannel = channel)
        ftsuModel.onChannelClicked(channelUuid)
    }


    /**
     * Handles remote action buttons based on
     * @param [action]
     */
    fun handleRemoteClick(action: FTSURemoteButtons) {
        when (action) {
            FTSURemoteButtons.ok -> {
                ftsuModel.tvOkClick()
            }

            FTSURemoteButtons.down -> {
                ftsuModel.tvDownArrow()
            }

            FTSURemoteButtons.left -> {
                ftsuModel.tvLeftArrow()
            }

            FTSURemoteButtons.right -> {
                ftsuModel.tvRightArrow()
            }

            FTSURemoteButtons.up -> {
                ftsuModel.tvUpArrow()
            }

            FTSURemoteButtons.back -> {
                ftsuModel.tvBackButton()
            }

            FTSURemoteButtons.menu -> {
                ftsuModel.tvMenu()
            }

            FTSURemoteButtons.closedCaption -> {
                ftsuModel.tvClosedCaption()
            }

            FTSURemoteButtons.volUp -> {
                ftsuModel.tvVolumeUp()
            }

            FTSURemoteButtons.volDown -> {
                ftsuModel.tvVolumeDown()
            }

            FTSURemoteButtons.chanUp -> {
                ftsuModel.tvChannelUp()
            }

            FTSURemoteButtons.chanDown -> {
                ftsuModel.tvChannelDown()
            }

            FTSURemoteButtons.zero -> {
                ftsuModel.tvZeroClick()
            }

            FTSURemoteButtons.one -> {
                ftsuModel.tvOneClick()
            }

            FTSURemoteButtons.two -> {
                ftsuModel.tvTwoClick()
            }

            FTSURemoteButtons.three -> {
                ftsuModel.tvThreeClick()
            }

            FTSURemoteButtons.four -> {
                ftsuModel.tvFourClick()
            }

            FTSURemoteButtons.five -> {
                ftsuModel.tvFiveClick()
            }

            FTSURemoteButtons.six -> {
                ftsuModel.tvSixClick()
            }

            FTSURemoteButtons.seven -> {
                ftsuModel.tvSevenClick()
            }

            FTSURemoteButtons.eight -> {
                ftsuModel.tvEightClick()
            }

            FTSURemoteButtons.nine -> {
                ftsuModel.tvNineClick()
            }

            FTSURemoteButtons.enter -> {
                ftsuModel.tvCheckClick()
            }

            FTSURemoteButtons.dash -> {
                ftsuModel.tvDashCLick()
            }
        }
    }
}

/**
 * Ui model for tv editor view
 */
data class EditChannelModel(
    val channels: List<Channel> = emptyList(),
    val selectedChannel: Channel? = null
)