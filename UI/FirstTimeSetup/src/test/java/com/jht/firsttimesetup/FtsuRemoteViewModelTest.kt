package com.jht.firsttimesetup

import com.jht.firsttimesetup.ui.FTSURemoteButtons
import com.jht.firsttimesetup.viewmodels.FtsuRemoteViewModel
import com.jht.interfaces.models.VideoInput
import com.jht.interfaces.models.VideoInputModel
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class FtsuRemoteViewModelTest {

    private lateinit var vm : FtsuRemoteViewModel

    @MockK
    private lateinit var ftsuModel: FirstTimeSetupModel

    @MockK
    private lateinit var videoInput: VideoInputModel


    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        every { videoInput.applyVideoInput() } returns VideoInput()
        vm = FtsuRemoteViewModel(
            ftsuModel,
            videoInput
        )
    }

    @Test
    fun `Given ok button action we expect appropriate method in model`() {
        vm.handleRemoteClick(FTSURemoteButtons.ok)
        verify(exactly = 1) { ftsuModel.tvOkClick() }
    }

    @Test
    fun `Given down button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.down)
        verify(exactly = 1) { ftsuModel.tvDownArrow() }
    }

    @Test
    fun `Given left button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.left)
        verify(exactly = 1) { ftsuModel.tvLeftArrow() }
    }

    @Test
    fun `Given right button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.right)
        verify(exactly = 1) { ftsuModel.tvRightArrow() }
    }

    @Test
    fun `Given up button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.up)
        verify(exactly = 1) { ftsuModel.tvUpArrow() }
    }

    @Test
    fun `Given back button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.back)
        verify(exactly = 1) { ftsuModel.tvBackButton() }
    }

    @Test
    fun `Given menu button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.menu)
        verify(exactly = 1) { ftsuModel.tvMenu() }
    }

    @Test
    fun `Given closed caption button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.closedCaption)
        verify(exactly = 1) { ftsuModel.tvClosedCaption() }
    }

    @Test
    fun `Given volume up button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.volUp)
        verify(exactly = 1) { ftsuModel.tvVolumeUp() }
    }

    @Test
    fun `Given volume down button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.volDown)
        verify(exactly = 1) { ftsuModel.tvVolumeDown() }
    }

    @Test
    fun `Given tv chanel up button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.chanUp)
        verify(exactly = 1) { ftsuModel.tvChannelUp() }
    }

    @Test
    fun `Given tv chanel down button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.chanDown)
        verify(exactly = 1) { ftsuModel.tvChannelDown() }
    }

    @Test
    fun `Given zero button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.zero)
        verify(exactly = 1) { ftsuModel.tvZeroClick() }
    }

    @Test
    fun `Given one button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.one)
        verify(exactly = 1) { ftsuModel.tvOneClick() }
    }

    @Test
    fun `Given two button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.two)
        verify(exactly = 1) { ftsuModel.tvTwoClick() }
    }

    @Test
    fun `Given three button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.three)
        verify(exactly = 1) { ftsuModel.tvThreeClick() }
    }

    @Test
    fun `Given four button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.four)
        verify(exactly = 1) { ftsuModel.tvFourClick() }
    }

    @Test
    fun `Given five button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.five)
        verify(exactly = 1) { ftsuModel.tvFiveClick() }
    }

    @Test
    fun `Given six button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.six)
        verify(exactly = 1) { ftsuModel.tvSixClick() }
    }

    @Test
    fun `Given seven button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.seven)
        verify(exactly = 1) { ftsuModel.tvSevenClick() }
    }

    @Test
    fun `Given eight button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.eight)
        verify(exactly = 1) { ftsuModel.tvEightClick() }
    }

    @Test
    fun `Given nine button action we expect appropriate method in model `() {
        vm.handleRemoteClick(FTSURemoteButtons.nine)
        verify(exactly = 1) { ftsuModel.tvNineClick() }
    }

    @Test
    fun `Given a default channel set we expect the channel to be set in FTSU model`() {
        val channelUuid = "314"
        vm.setDefaultChannel(channelUuid)
        verify(exactly = 1) { ftsuModel.setDefaultChannel(channelUuid)  }
    }

    @Test
    fun `Given a custom channel name set we expect the channel to be set in FTSU model`() {
        val channelUuid = "31415925" // since it's the only channel
        val channelName = "Pokemon All day Channel"
        every { ftsuModel.getTvChannelList() } returns listOf(Channel(channelName, "24", uuid = channelUuid))
        //vm must be re-initialized because channel list is updated in init
        vm = FtsuRemoteViewModel(
            ftsuModel,
            videoInput
        )
        vm.setChannelNamePreview(channelUuid,channelName)
        vm.setChannelName(channelUuid)
        verify(exactly = 1) { ftsuModel.setChannelCustomName(channelName,channelUuid)  }
    }

    @Test
    fun `Given an unhidden channel we expect hiding channel to change its state to hidden`() {
        val channelUuid = "2781562135413"
        val channelToToggle = Channel("MelonHeadTv (All day melon head special)", "42", isHidden = false, uuid = channelUuid)
        every { ftsuModel.getTvChannelList() } returns listOf(channelToToggle)
        //vm must be re-initialized because channel list is updated in init
        vm = FtsuRemoteViewModel(
            ftsuModel,
            videoInput
        )
        vm.toggleHideChannel(channelUuid)
        verify(exactly = 1) { ftsuModel.toggleHideChannel(channelUuid, true) }
    }
}