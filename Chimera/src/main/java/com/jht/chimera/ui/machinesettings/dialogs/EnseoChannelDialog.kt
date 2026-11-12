/**
 * [Dev Notes]
 * EnseoRTPChannel extends EnseoUDPChannel.
 * When using conditionals (`is`/`when`), check for EnseoRTPChannel *before* EnseoUDPChannel
 * or it will be caught by the UDP branch.
 */

package com.jht.chimera.ui.machinesettings.dialogs

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import android.widget.CheckBox
import com.jht.androiduiwidgets.dialog.DialogBase
import com.jht.androiduiwidgets.dialog.DialogFactory
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog
import com.jht.androiduiwidgets.edittext.UserTextInput
import com.jht.androiduiwidgets.edittext.UserTextInput.InputType
import com.jht.androiduiwidgets.selector.OptionManager
import com.jht.androiduiwidgets.selector.OptionSelectorItem
import com.jht.chimera.R
import com.jht.chimera.lib.common.string.StringUtil
import com.jht.serialport.peripheral.tuner.enseo.EnseoChannel
import com.jht.serialport.peripheral.tuner.enseo.EnseoProtocol
import com.jht.serialport.peripheral.tuner.enseo.EnseoRF1Channel
import com.jht.serialport.peripheral.tuner.enseo.EnseoRF2Channel
import com.jht.serialport.peripheral.tuner.enseo.EnseoRF3Channel
import com.jht.serialport.peripheral.tuner.enseo.EnseoRFChannel
import com.jht.serialport.peripheral.tuner.enseo.EnseoRTPChannel
import com.jht.serialport.peripheral.tuner.enseo.EnseoUDPChannel
import com.jht.serialport.peripheral.tuner.enseo.RfBroadcastType

/**
 * Dialog for editing an [EnseoChannel].
 *
 * To correctly use this dialog, ensure that method [init] is called prior to showing the dialog.
 *
 * @param context The context for this dialog.
 * @param attrs The attributes for this dialog. Needed for view inflation.
 * @param defStyleAttr The default style attribute for this dialog. Needed for view inflation.
 */
class EnseoChannelDialog @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): DialogBase(context) {
    private var channel: EnseoChannel = EnseoUDPChannel("", "")
    private var onConfirm: IOnConfirm = IOnConfirm {}
    private var allowRfChannels: Boolean = false
    private var existingChannels: List<EnseoChannel> = emptyList()
    private var originalChannel: EnseoChannel? = null
    private val channelNameInput: UserTextInput = findViewById(R.id.channel_name)
    private val channelNumberInput: UserTextInput = findViewById(R.id.channel_number)
    private val channelProtocol: OptionSelectorItem = findViewById(R.id.channel_protocol)
    private val channelData1: UserTextInput = findViewById(R.id.channel_data1)
    private val channelData2: UserTextInput = findViewById(R.id.channel_data2)
    private val channelPath: UserTextInput = findViewById(R.id.channel_path)
    private val channelDeinterlace: CheckBox = findViewById(R.id.channel_deinterlace)
    private val channelRfBroadcast: OptionSelectorItem = findViewById(R.id.channel_rf_broadcast)
    private val confirmButton: Button = findViewById(R.id.dialog_confirm_btn)
    private val cancelButton: Button = findViewById(R.id.dialog_cancel_btn)

    /**
     * Provides the layout resource ID for this dialog.
     */
    override fun getLayoutResId() = R.layout.enseo_channel_dialog

    /**
     * Initialize the dialog. This should be called prior to showing the dialog.
     *
     * @param channel The channel to edit.
     * @param onConfirm Callback called when the user confirms the changes.
     */
    fun init(channel: EnseoChannel, allowRfChannels: Boolean, existingChannels: List<EnseoChannel>, onConfirm: IOnConfirm) {
        this.channel = channel
        this.onConfirm = onConfirm
        this.allowRfChannels = allowRfChannels
        this.existingChannels = existingChannels
        this.originalChannel = channel
        initChannelProtocol()
        initChannelData()
        initDialogHandlers()
        hideUnusedViews()
    }

    /**
     * Initializes the channel protocol option selector. This ensures the channel protocol selector
     * is configured to reflect the current channel protocol.
     */
    private fun initChannelProtocol() {
        val enseoProtocols = EnseoProtocol.asList(allowRfChannels)
        val selectedProtocol = EnseoProtocol.fromChannel(channel)
        val selectedProtocolIndex = enseoProtocols.indexOf(selectedProtocol).let {
            if (it != -1) it else 0
        }
        channelProtocol.setOptions(OptionManager(enseoProtocols.map { it.displayName }.toTypedArray()))
        channelProtocol.selectItem(selectedProtocolIndex)
    }

    /**
     * Initializes the channel data. This loads the channel data and populates it into their respective fields.
     */
    private fun initChannelData() {
        channelNumberInput.text = channel.number() ?: ""
        channelNameInput.text = channel.name() ?: ""
        when (channel) {
            is EnseoRTPChannel,
            is EnseoUDPChannel -> { initIPChannelData(channel as EnseoUDPChannel) }
            is EnseoRF1Channel -> { initRF1ChannelData(channel as EnseoRF1Channel) }
            is EnseoRF2Channel -> { initRF2ChannelData(channel as EnseoRF2Channel) }
            is EnseoRF3Channel -> { initRF3ChannelData(channel as EnseoRF3Channel) }
        }
    }

    /**
     * Initializes the dialog event handlers.
     */
    private fun initDialogHandlers() {
        channelProtocol.setOnOptionSelected { pos, _ -> onChannelProtocolChanged(pos) }
        confirmButton.setOnClickListener { onConfirmBtnClicked() }
        cancelButton.setOnClickListener { onCancelBtnClicked() }
    }

    /**
     * Hides views that are not utilized by this dialog, but shared with other dialogs (ex: IPTV dialog).
     */
    private fun hideUnusedViews() {
        channelPath.visibility = INVISIBLE  // Invisible to maintain view constraints
        channelDeinterlace.visibility = GONE
    }

    /**
     * Initializes the IP channel data fields.
     */
    private fun initIPChannelData(channel: EnseoUDPChannel) {
        // Populate IP address field
        channelData1.inputProperties = channelData1.inputProperties
            .inputType(InputType.IP_ADDRESS)
            .label(context.getString(R.string.address_tc))
        channelData1.text = channel.address() ?: ""
        channelData1.visibility = VISIBLE

        // Populate port field
        channelData2.inputProperties = channelData2.inputProperties
            .inputType(InputType.INTEGER)
            .label(context.getString(R.string.port_tc))
        channelData2.text = channel.port() ?: ""
        channelData2.visibility = VISIBLE

        // Hide RF broadcast selector
        channelRfBroadcast.visibility = INVISIBLE
    }

    /**
     * Initializes the RF class 1 channel data fields.
     */
    private fun initRF1ChannelData(channel: EnseoRF1Channel) {
        // Populate logical number field
        channelData1.inputProperties = channelData1.inputProperties
            .inputType(InputType.INTEGER)
            .label(context.getString(R.string.logical_number_tc))
        channelData1.text = channel.logicalNumber()
        channelData1.visibility = VISIBLE

        // Hide channel data input 2
        channelData2.visibility = INVISIBLE

        // Populate RF broadcast type field
        initRfBroadcastType(channel)
    }

    /**
     * Initializes the RF class 2 channel data fields.
     */
    private fun initRF2ChannelData(channel: EnseoRF2Channel) {
        // Populate frequency field
        channelData1.inputProperties = channelData1.inputProperties
            .inputType(InputType.INTEGER)
            .label(context.getString(R.string.frequency_tc))
        channelData1.text = channel.frequency()
        channelData1.visibility = VISIBLE

        // Populate program number field
        channelData2.inputProperties = channelData2.inputProperties
            .inputType(InputType.INTEGER)
            .label(context.getString(R.string.program_number_tc))
        channelData2.text = channel.programNumber()
        channelData2.visibility = VISIBLE

        // Populate RF broadcast type field
        initRfBroadcastType(channel)
    }

    /**
     * Initializes the RF class 3 channel data fields.
     */
    private fun initRF3ChannelData(channel: EnseoRF3Channel) {
        // Populate major field
        channelData1.inputProperties = channelData1.inputProperties
            .inputType(InputType.INTEGER)
            .label(context.getString(R.string.major_tc))
        channelData1.text = channel.major()
        channelData1.visibility = VISIBLE

        // Populate minor field
        channelData2.inputProperties = channelData2.inputProperties
            .inputType(InputType.INTEGER)
            .label(context.getString(R.string.minor_tc))
        channelData2.text = channel.minor()
        channelData2.visibility = VISIBLE

        // Populate RF broadcast type field
        initRfBroadcastType(channel)
    }

    /**
     * Initializes the RF broadcast type field. This populates the RF broadcast type selector and
     * selects the current channel's RF broadcast type.
     */
    private fun initRfBroadcastType(channel: EnseoRFChannel) {
        val rfBroadcastTypes = RfBroadcastType.asList()
        val selectedRfBroadcastIndex = rfBroadcastTypes.indexOf(channel.rfBroadcastType())
        channelRfBroadcast.setOptions(OptionManager(rfBroadcastTypes.map { it.name }.toTypedArray()))
        channelRfBroadcast.selectItem(selectedRfBroadcastIndex)
        channelRfBroadcast.visibility = VISIBLE
    }

    /**
     * Handler for channel protocol change events.
     */
    private fun onChannelProtocolChanged(pos: Int) {
        // When channel type changes, maintain channel number and name. Clear remaining channel data and re-initialize to update UI
        val enseoProtocols = EnseoProtocol.asList(allowRfChannels)
        val selectedProtocol = enseoProtocols.getOrNull(pos) ?: EnseoProtocol.UDP
        channel = selectedProtocol.createChannel(channelNumberInput.text, channelNameInput.text)
        initChannelData()
    }

    /**
     * Handler for confirm button clicks.
     */
    private fun onConfirmBtnClicked() {
        validateChannelChange().fold(
            onSuccess = {
                saveChannelChanges()
                onConfirm.onConfirm(channel)
                hide()
            },
            onFailure = {
                showError(it.message ?: "Error: An unknown error occurred.")
            }
        )
    }

    /**
     * Handler for cancel button clicks. Closes the dialog.
     */
    private fun onCancelBtnClicked() { hide() }

    /**
     * Checks if the channel dialog state is capable of being saved as a valid [EnseoChannel].
     */
    private fun validateChannelChange(): Result<Unit> {
        val channelNameBlank = channelNameInput.text.isNullOrBlank()
        val channelNumberBlank = channelNumberInput.text.isNullOrBlank()
        val channelData1Blank = channelData1.text.isNullOrBlank()
        val channelData2Blank = if (channel is EnseoRF1Channel) false else channelData2.text.isNullOrBlank()

        if (channelNameBlank || channelNumberBlank || channelData1Blank || channelData2Blank) {
            return Result.failure(Exception("Error: Required field is empty."))
        }

        // EnseoRTPChannel is a sub-class of EnseoUDPChannel, hence this covers both channel types
        if (channel is EnseoUDPChannel && !StringUtil.isValidIPv4(channelData1.text)) {
            return Result.failure(Exception("Error: Invalid IPv4 address."))
        }

        // CGC-23503: Prevent duplicate channels, block if same name and number already exists.
        if (existingChannels.any { it.uuid() != originalChannel?.uuid() && it.name() == channelNameInput.text && it.number() == channelNumberInput.text }) {
            return Result.failure(Exception("Error: A channel with this name and number already exists."))
        }
        when (channel) {
            is EnseoRTPChannel,
            is EnseoUDPChannel -> {
                if (existingChannels.any {
                    if(it is EnseoUDPChannel) {
                        it.uuid() != originalChannel?.uuid() &&
                                it.address() == channelData1.text &&
                                it.port() == channelData2.text
                    } else false
                    }) {
                    return Result.failure(Exception(context.getString(R.string.channel_already_exists_pg)))
                }
            }
        }

        return Result.success(Unit)
    }

    /**
     * Saves the channel dialog state to the provided [EnseoChannel] instance.
     */
    private fun saveChannelChanges() {
        channel.name(channelNameInput.text)
        channel.number(channelNumberInput.text)
        when (channel) {
            is EnseoRTPChannel,
            is EnseoUDPChannel -> {
                (channel as EnseoUDPChannel).address(channelData1.text)
                (channel as EnseoUDPChannel).port(channelData2.text)
            }
            is EnseoRFChannel -> {
                val rfBroadcastTypes = RfBroadcastType.asList()
                val selectedRfBroadcast = rfBroadcastTypes.getOrNull(channelRfBroadcast.selectedPosition) ?: RfBroadcastType.UNKNOWN
                (channel as EnseoRFChannel).rfBroadcastType(selectedRfBroadcast)
                when (channel) {
                    is EnseoRF1Channel -> {
                        (channel as EnseoRF1Channel).logicalNumber(channelData1.text)
                    }
                    is EnseoRF2Channel -> {
                        (channel as EnseoRF2Channel).frequency(channelData1.text)
                        (channel as EnseoRF2Channel).programNumber(channelData2.text)
                    }
                    is EnseoRF3Channel -> {
                        (channel as EnseoRF3Channel).major(channelData1.text)
                        (channel as EnseoRF3Channel).minor(channelData2.text)
                    }
                }
            }
        }
    }

    /**
     * Helper method to show an error message.
     */
    private fun showError(error: String) {
        DialogFactory.message(this, error, null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK)
    }

    /**
     * Callback interface to handle confirmation of editing a [EnseoChannel] via the EnseoChannelDialog.
     */
    interface IOnConfirm {
        /**
         * Called when the user confirms changes to a [EnseoChannel] via the EnseoChannelDialog
         * This is only called once the channel is valid (i.e. no missing or invalid channel data)
         *
         * @param channel the [EnseoChannel] instance that was modified.
         */
        fun onConfirm(channel: EnseoChannel?)

        companion object {
            /**
             * Creates a [IOnConfirm] implementation from a lambda expression.
             * Enables concise and idiomatic Kotlin usage:
             * ```kotlin
             * val callback = IOnConfirm { channel -> }
             * ```
             * @param action a lambda providing the implementation of [onConfirm].
             * @return an instance of [IOnConfirm] implementing the provided [action].
             */
            operator fun invoke(action: (EnseoChannel?) -> Unit): IOnConfirm {
                return object : IOnConfirm {
                    override fun onConfirm(channel: EnseoChannel?) {
                        action(channel)
                    }
                }
            }
        }
    }
}