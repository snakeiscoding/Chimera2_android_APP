package com.jht.chimera.ui.machinesettings.dialogs

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.border
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jht.androiduiwidgets.dialog.DialogBase
import com.jht.chimera.R
import com.jht.chimera.lib.common.string.StringUtil
import com.jht.chimera.lib.tv.tuner.lg.channel.LgBroadcastType
import com.jht.chimera.lib.tv.tuner.lg.channel.LgChannel
import com.jht.chimera.lib.tv.tuner.lg.channel.LgChannel.ChannelType
import com.jht.chimera.lib.tv.tuner.lg.channel.LgPolarization
import com.jht.chimera.lib.tv.tuner.lg.channel.LgRegion
import com.jht.theme.DynamicTheme
import com.jht.theme.darkDynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.widgets.BaseInput
import com.jht.widgets.DropdownInputItem
import com.jht.widgets.FieldInputDefaults
import com.jht.widgets.FieldInputOptions

/**
 * Dialog for editing an [LgChannel].
 *
 * Uses Jetpack Compose for its UI content while extending [DialogBase] for compatibility with
 * the existing dialog factory pattern. The composable content ([LgChannelDialogContent]) is
 * self-contained and can be reused in a pure Compose context without modification.
 *
 * To correctly use this dialog, ensure that [init] is called prior to showing the dialog.
 */
class LgChannelDialog @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : DialogBase(context) {

    override fun getLayoutResId() = R.layout.lg_channel_dialog

    /**
     * Initialize the dialog. This should be called prior to showing the dialog.
     *
     * @param channel The channel to edit (or a new default channel for add).
     * @param supportedChannelTypes The channel types supported by the tuner variant.
     * @param region The region of the tuner variant (determines RF broadcast display names).
     * @param existingChannels The existing channels (for future duplicate detection).
     * @param onConfirm Callback called when the user confirms with a valid channel.
     */
    fun init(
        channel: LgChannel,
        supportedChannelTypes: List<ChannelType>,
        region: LgRegion,
        existingChannels: List<LgChannel>,
        onConfirm: IOnConfirm
    ) {
        // Dismiss when tapping outside the dialog container
        setCloseOnOutsideTouch(true)

        val composeView = findViewById<ComposeView>(R.id.lg_channel_compose_content)
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        composeView.setContent {
            DynamicTheme(darkDynamicTheme) {  // hardcode dark theme to better match legacy UI
                LgChannelDialogContent(
                    initialChannel = channel,
                    supportedChannelTypes = supportedChannelTypes,
                    region = region,
                    onConfirm = { ch -> onConfirm.onConfirm(ch); hide() },
                    onCancel = { hide() }
                )
            }
        }
    }

    /**
     * Callback interface for channel edit confirmation.
     * Supports SAM-like lambda usage: `IOnConfirm { channel -> ... }`
     */
    interface IOnConfirm {
        fun onConfirm(channel: LgChannel?)

        companion object {
            operator fun invoke(action: (LgChannel?) -> Unit): IOnConfirm {
                return object : IOnConfirm {
                    override fun onConfirm(channel: LgChannel?) = action(channel)
                }
            }
        }
    }
}

//##################################################################################################
// Compose Content
//##################################################################################################

// Column weight ratio: 2 data fields + 1 dropdown per row
private const val FieldWeight = 1f
private const val DropdownWeight = 1.07f  // slightly wider to match original 320:300 ratio

/**
 * Self-contained composable form for editing an [LgChannel]. Renders fields dynamically based
 * on the selected [ChannelType] and validates all input before invoking [onConfirm].
 *
 * This composable has no dependency on [LgChannelDialog] or [DialogBase] and can be reused
 * directly in a pure Compose context (e.g. wrapped in a Compose `Dialog`).
 *
 * @param initialChannel The channel to populate fields from (or a default for new channels).
 * @param supportedChannelTypes The channel types the user can select from (provided by tuner variant).
 * @param region Determines which RF broadcast types and display names are available.
 * @param onConfirm Called with a fully validated [LgChannel] when the user confirms.
 * @param onCancel Called when the user cancels.
 */
@Composable
fun LgChannelDialogContent(
    initialChannel: LgChannel,
    supportedChannelTypes: List<ChannelType>,
    region: LgRegion,
    onConfirm: (LgChannel) -> Unit,
    onCancel: () -> Unit
) {
    // Dropdown item lists (remembered so DropdownInputItem reference equality works)
    val channelTypeItems = remember(supportedChannelTypes) {
        supportedChannelTypes.sortedBy { it.displayName }.map { DropdownInputItem(it, it.displayName) }
    }
    val ipBroadcastItems = remember {
        LgBroadcastType.IpBroadcast.asList().map { DropdownInputItem(it, it.displayName()) }
    }
    val rfBroadcastItems = remember(region) {
        LgBroadcastType.RfBroadcast.asList(region).map { DropdownInputItem(it, it.displayName(region)) }
    }
    val polarizationItems = remember {
        LgPolarization.asList().map { DropdownInputItem(it, it.displayName()) }
    }

    // Channel type
    var currentChannelType by remember { mutableStateOf(channelTypeItems.findByValue(initialChannel.channelType).value as ChannelType) }
    val fieldConfig = getFieldConfig(currentChannelType)
    val isIpChannel = currentChannelType in listOf(ChannelType.IP_CLASS_1, ChannelType.IP_CLASS_2)

    // Common fields
    var number by remember { mutableStateOf(initialChannel.number()) }
    var name by remember { mutableStateOf(initialChannel.name()) }

    // Dropdown selections
    var selectedChannelType by remember { mutableStateOf(channelTypeItems.findByValue(initialChannel.channelType)) }
    var selectedIpBroadcast by remember { mutableStateOf(ipBroadcastItems.findByValue((initialChannel as? LgChannel.IpChannel)?.broadcastType)) }
    var selectedRfBroadcast by remember { mutableStateOf(rfBroadcastItems.findByValue((initialChannel as? LgChannel.RfChannel)?.broadcastType)) }
    var selectedPolarization by remember { mutableStateOf(polarizationItems.findByValue((initialChannel as? LgChannel.RfChannel.Class4)?.polarization)) }

    // Data fields — one per entry in fieldConfig.dataFields, reset when channel type changes
    var fieldValues by remember(currentChannelType) {
        mutableStateOf(getInitialFieldValues(initialChannel, currentChannelType, fieldConfig))
    }

    // Error state
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var errorFields by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Scaled layout values
    val fieldSpacing = 20.dp.computeSize()
    val rowSpacing = 20.dp.computeSize()
    val cornerShape = RoundedCornerShape(4.dp.computeSize())

    // Dialog-specific field style — matches EnseoChannelDialog's engineering look.
    // Swap to FieldInputDefaults.defaultStyle() (no overrides) when migrating to the new UI.
    val style = lgChannelFieldStyle(cornerShape)

    Column(
        modifier = Modifier
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // no ripple
            ) { /* consume clicks on dialog background to prevent DialogBase dismiss */ }
            .padding(horizontal = 20.dp.computeSize(), vertical = 24.dp.computeSize())
    ) {
        // Close button (top-right)
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier.align(Alignment.TopEnd).size(24.dp.computeSize())
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        Spacer(Modifier.height(8.dp.computeSize()))

        // Row 1: Number, Name, Channel Type
        DialogRow(fieldSpacing) {
            DialogField(value = number, onValueChange = { number = it }, label = "Number", keyboardType = KeyboardType.Number, inputFilter = digitsOnly, style = style, isError = "Number" in errorFields)
            DialogField(value = name, onValueChange = { name = it }, label = "Name", style = style, isError = "Name" in errorFields)
            DialogDropdown(label = "Channel Type", items = channelTypeItems, selected = selectedChannelType, style = style) { item ->
                selectedChannelType = item
                val newType = item.value as ChannelType
                if (newType != currentChannelType) {
                    currentChannelType = newType
                    selectedIpBroadcast = ipBroadcastItems.first()
                    selectedRfBroadcast = rfBroadcastItems.first()
                    selectedPolarization = polarizationItems.first()
                    errorMessage = null
                    errorFields = emptySet()
                }
            }
        }

        // Data field rows — split into rows of 2, with broadcast/polarization dropdowns in the third column
        val dataFields = fieldConfig.dataFields
        val row2Fields = dataFields.take(2)   // first 2 fields share a row with the broadcast dropdown
        val row3Fields = dataFields.drop(2)   // remaining fields share a row with the polarization dropdown

        Spacer(Modifier.height(rowSpacing))

        // Row 2: first 2 data fields + Broadcast Type
        DialogRow(fieldSpacing) {
            row2Fields.forEachIndexed { i, field ->
                val globalIndex = i
                DialogField(
                    value = fieldValues[globalIndex],
                    onValueChange = { fieldValues = fieldValues.toMutableList().also { list -> list[globalIndex] = it } },
                    label = field.label, keyboardType = field.keyboardType, inputFilter = field.inputFilter, style = style, isError = field.label in errorFields
                )
            }
            if (row2Fields.size < 2) DialogFieldSpacer()
            if (isIpChannel) {
                DialogDropdown(label = "Broadcast Type", items = ipBroadcastItems, selected = selectedIpBroadcast, style = style, isError = "Broadcast Type" in errorFields) { selectedIpBroadcast = it }
            } else {
                // Use restricted list when channel class constrains allowed broadcast types (e.g. RF Class 5/6)
                val effectiveRfItems = if (fieldConfig.allowedRfBroadcasts != null) {
                    rfBroadcastItems.filter { it.value in fieldConfig.allowedRfBroadcasts }
                } else rfBroadcastItems
                DialogDropdown(label = "Broadcast Type", items = effectiveRfItems, selected = selectedRfBroadcast, style = style, isError = "Broadcast Type" in errorFields) { selectedRfBroadcast = it }
            }
        }

        // Row 3: remaining data fields + Polarization (only if needed)
        if (row3Fields.isNotEmpty() || fieldConfig.showPolarization) {
            Spacer(Modifier.height(rowSpacing))
            DialogRow(fieldSpacing) {
                for (col in 0..1) {
                    val globalIndex = 2 + col
                    if (col < row3Fields.size) {
                        DialogField(
                            value = fieldValues[globalIndex],
                            onValueChange = { fieldValues = fieldValues.toMutableList().also { list -> list[globalIndex] = it } },
                            label = row3Fields[col].label, keyboardType = row3Fields[col].keyboardType, inputFilter = row3Fields[col].inputFilter, style = style, isError = row3Fields[col].label in errorFields
                        )
                    } else {
                        DialogFieldSpacer()
                    }
                }
                if (fieldConfig.showPolarization) {
                    DialogDropdown(label = "Polarization", items = polarizationItems, selected = selectedPolarization, style = style, isError = "Polarization" in errorFields) { selectedPolarization = it }
                } else {
                    DialogDropdownSpacer()
                }
            }
        }

        // Error message
        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(text = errorMessage!!, color = DynamicTheme.colors.stop)
        }

        Spacer(Modifier.height(rowSpacing))

        // Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(fieldSpacing)) {
            Spacer(Modifier.weight(1f))
            DialogConfirmButton("Confirm", cornerShape) {
                validateAndBuild(
                    channelType = currentChannelType,
                    name = name, number = number,
                    fieldValues = fieldValues, fieldConfig = fieldConfig,
                    isIpChannel = isIpChannel,
                    ipBroadcast = selectedIpBroadcast.value as LgBroadcastType.IpBroadcast,
                    rfBroadcast = selectedRfBroadcast.value as LgBroadcastType.RfBroadcast,
                    polarization = selectedPolarization.value as LgPolarization
                ).fold(
                    onSuccess = {
                        errorMessage = null
                        errorFields = emptySet()
                        onConfirm(it)
                    },
                    onFailure = {
                        errorMessage = it.message
                        errorFields = (it as? ValidationError)?.errorFields ?: emptySet()
                    }
                )
            }
            Spacer(Modifier.width(12.dp.computeSize()))
            DialogCancelButton("Cancel", cornerShape, onClick = onCancel)
            Spacer(Modifier.weight(1f))
        }
    }
}

//##################################################################################################
// Reusable Dialog Composables
//##################################################################################################

/** Standard row layout for dialog field groups. */
@Composable
private fun DialogRow(spacing: Dp, content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        modifier = Modifier.height(IntrinsicSize.Max),
        content = content
    )
}

/**
 * Text input field styled for this dialog. Uses [RowScope] weight for proportional sizing.
 * Maintains its own [TextFieldValue] internally to preserve cursor position across recompositions.
 * Applies [inputFilter] (if provided) on every keystroke to reject invalid characters.
 */
@Composable
private fun RowScope.DialogField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    style: FieldInputOptions,
    keyboardType: KeyboardType = KeyboardType.Text,
    inputFilter: ((String) -> String)? = null,
    isError: Boolean = false
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    // Sync if the external value changed (e.g. channel type switch reset fields)
    if (textFieldValue.text != value) {
        textFieldValue = TextFieldValue(value)
    }
    BaseInput(
        value = textFieldValue,
        onValueChange = { newValue ->
            val filtered = inputFilter?.invoke(newValue.text) ?: newValue.text
            textFieldValue = if (filtered != newValue.text) {
                TextFieldValue(filtered, selection = androidx.compose.ui.text.TextRange(filtered.length))
            } else {
                newValue
            }
            onValueChange(filtered)
        },
        label = label,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.weight(FieldWeight),
        fieldInputOptions = style
    )
}

/** Dropdown selector styled for this dialog. Uses [RowScope] weight for proportional sizing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowScope.DialogDropdown(
    label: String,
    items: List<DropdownInputItem>,
    selected: DropdownInputItem,
    style: FieldInputOptions,
    isError: Boolean = false,
    onValueChange: (DropdownInputItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        modifier = Modifier.weight(DropdownWeight),
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        BaseInput(
            value = TextFieldValue(selected.displayName),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxSize().menuAnchor(),
            label = label,
            isError = isError,
            fieldInputOptions = style
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(DynamicTheme.colors.textFieldBackgroundDefault)
                .clip(DynamicTheme.corners.medium.makeCornerShape())
                .border(
                    BorderStroke(1.dp, DynamicTheme.colors.primary),
                    shape = DynamicTheme.corners.medium.makeCornerShape()
                ),
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .background(if (selected == item) DynamicTheme.colors.primary else DynamicTheme.colors.textFieldBackgroundDefault),
                    text = {
                        Text(
                            text = item.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected == item) DynamicTheme.colors.text2 else DynamicTheme.colors.text1
                        )
                    },
                    onClick = {
                        onValueChange(item)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(textColor = DynamicTheme.colors.text1)
                )
            }
        }
    }
}

/** Spacer that takes the same weight as a field column. */
@Composable
private fun RowScope.DialogFieldSpacer() {
    Spacer(Modifier.weight(FieldWeight))
}

/** Spacer that takes the same weight as a dropdown column. */
@Composable
private fun RowScope.DialogDropdownSpacer() {
    Spacer(Modifier.weight(DropdownWeight))
}

/** Primary (confirm) button styled for this dialog. */
@Composable
private fun DialogConfirmButton(text: String, shape: RoundedCornerShape, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = DynamicTheme.colors.primary,
            contentColor = Color.Black
        )
    ) {
        Text(text, color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp.computeSize(), vertical = 4.dp.computeSize()))
    }
}

/** Secondary (cancel) button styled for this dialog. */
@Composable
private fun DialogCancelButton(text: String, shape: RoundedCornerShape, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = DynamicTheme.colors.gray3,
            contentColor = DynamicTheme.colors.text1
        )
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 16.dp.computeSize(), vertical = 4.dp.computeSize()))
    }
}

/** Dialog-specific field style matching EnseoChannelDialog's engineering look. */
@Composable
private fun lgChannelFieldStyle(shape: RoundedCornerShape) = FieldInputDefaults.defaultStyle(
    labelStyle = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.Bold),
    labelPaddingValues = PaddingValues(start = 2.dp.computeSize(), bottom = 4.dp.computeSize()),
    borderStroke = BorderStroke(2.dp, Color.Gray),
    shape = shape
)

//##################################################################################################
// Field Configuration
//##################################################################################################

// Input filters per IDCAP spec — applied on every keystroke to reject invalid characters.
private val digitsOnly: (String) -> String = { it.filter { c -> c.isDigit() } }
// Channel numbers may be hyphenated (e.g. "4-1" for major-minor RF types), so hyphens are allowed.
private val channelNumberChars: (String) -> String = { it.filter { c -> c.isDigit() || c == '-' } }
private val ipAddressChars: (String) -> String = { it.filter { c -> c.isDigit() || c == '.' } }

// Max values per IDCAP spec for range validation.
private const val MAX_UINT16 = 65535L       // 2^16 - 1 (port, logicalNumber, major, minor, programNumber, satelliteId)
private const val MAX_UINT32 = 4294967295L  // 2^32 - 1 (frequency)
private const val MAX_PLP_ID = 255L         // 0xFF

/** Defines a single data field: its label, keyboard type, input filter, and optional max value for range validation. */
private data class DataFieldDef(
    val label: String,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val inputFilter: ((String) -> String)? = null,
    val maxValue: Long? = null
)

/**
 * Describes which data fields and selectors are visible for a given [ChannelType].
 * [dataFields] is an ordered list — the composable renders them left-to-right across rows.
 * [allowedRfBroadcasts] if set, restricts the RF broadcast dropdown to only these values
 * (per IDCAP spec, e.g. RF Class 5 only allows TERRESTRIAL_2, RF Class 6 only allows ATSC3 types).
 */
private data class FieldConfig(
    val dataFields: List<DataFieldDef>,
    val showPolarization: Boolean = false,
    val allowedRfBroadcasts: List<LgBroadcastType.RfBroadcast>? = null
)

/** Returns the [FieldConfig] for [channelType], defining which fields are shown and their labels. */
private fun getFieldConfig(channelType: ChannelType): FieldConfig = when (channelType) {
    ChannelType.IP_CLASS_1 -> FieldConfig(listOf(DataFieldDef("Logical Number", KeyboardType.Number, digitsOnly, MAX_UINT16)))
    ChannelType.IP_CLASS_2 -> FieldConfig(listOf(DataFieldDef("IP Address", KeyboardType.Decimal, ipAddressChars), DataFieldDef("Port", KeyboardType.Number, digitsOnly, MAX_UINT16)))
    ChannelType.RF_CLASS_1 -> FieldConfig(listOf(DataFieldDef("Logical Number", KeyboardType.Number, digitsOnly, MAX_UINT16)))
    ChannelType.RF_CLASS_2 -> FieldConfig(listOf(DataFieldDef("Frequency", KeyboardType.Number, digitsOnly, MAX_UINT32), DataFieldDef("Program Number", KeyboardType.Number, digitsOnly, MAX_UINT16)), allowedRfBroadcasts = LgBroadcastType.RfBroadcast.TERRESTRIAL_CABLE_TYPES)
    ChannelType.RF_CLASS_3 -> FieldConfig(listOf(DataFieldDef("Major", KeyboardType.Number, digitsOnly, MAX_UINT16), DataFieldDef("Minor", KeyboardType.Number, digitsOnly, MAX_UINT16)))
    ChannelType.RF_CLASS_4 -> FieldConfig(listOf(DataFieldDef("Frequency", KeyboardType.Number, digitsOnly, MAX_UINT32), DataFieldDef("Satellite ID", KeyboardType.Number, digitsOnly, MAX_UINT16), DataFieldDef("Program Number", KeyboardType.Number, digitsOnly, MAX_UINT16)), showPolarization = true, allowedRfBroadcasts = LgBroadcastType.RfBroadcast.SATELLITE_TYPES)
    ChannelType.RF_CLASS_5 -> FieldConfig(listOf(DataFieldDef("Frequency", KeyboardType.Number, digitsOnly, MAX_UINT32), DataFieldDef("Program Number", KeyboardType.Number, digitsOnly, MAX_UINT16), DataFieldDef("PLP ID", KeyboardType.Number, digitsOnly, MAX_PLP_ID)), allowedRfBroadcasts = LgBroadcastType.RfBroadcast.TERRESTRIAL_2_TYPES)
    ChannelType.RF_CLASS_6 -> FieldConfig(listOf(DataFieldDef("Major", KeyboardType.Number, digitsOnly, MAX_UINT16), DataFieldDef("Minor", KeyboardType.Number, digitsOnly, MAX_UINT16), DataFieldDef("PLP ID", KeyboardType.Number, digitsOnly, MAX_PLP_ID)), allowedRfBroadcasts = LgBroadcastType.RfBroadcast.ATSC3_TYPES)
}

//##################################################################################################
// Initial Data Extraction
//##################################################################################################

/**
 * Returns initial field values from [channel] matching [config]'s field count,
 * or a list of empty strings if the type changed (user switched channel type).
 */
private fun getInitialFieldValues(channel: LgChannel, currentType: ChannelType, config: FieldConfig): List<String> {
    val emptyValues = List(config.dataFields.size) { "" }
    if (channel.channelType != currentType) return emptyValues
    return when (channel) {
        is LgChannel.IpChannel.Class1 -> listOf(channel.logicalNumber)
        is LgChannel.IpChannel.Class2 -> listOf(channel.ip, channel.port)
        is LgChannel.RfChannel.Class1 -> listOf(channel.logicalNumber)
        is LgChannel.RfChannel.Class2 -> listOf(channel.frequency, channel.programNumber)
        is LgChannel.RfChannel.Class3 -> listOf(channel.major, channel.minor)
        is LgChannel.RfChannel.Class4 -> listOf(channel.frequency, channel.satelliteId, channel.programNumber)
        is LgChannel.RfChannel.Class5 -> listOf(channel.frequency, channel.programNumber, channel.plpId)
        is LgChannel.RfChannel.Class6 -> listOf(channel.major, channel.minor, channel.plpId)
    }
}

/** Finds the first item whose [DropdownInputItem.value] matches [target], or the first item. */
private fun List<DropdownInputItem>.findByValue(target: Any?): DropdownInputItem {
    return if (target != null) firstOrNull { it.value == target } ?: first() else first()
}

//##################################################################################################
// Validation & Channel Building
//##################################################################################################

/** Validation error that carries both the message and the labels of fields in error. */
private class ValidationError(message: String, val errorFields: Set<String>) : Exception(message)

/**
 * Validates all dialog fields and, if valid, constructs the appropriate [LgChannel] subtype.
 *
 * [fieldValues] is positional — values correspond 1:1 with [FieldConfig.dataFields].
 * Returns [Result.failure] with a [ValidationError] containing error field labels on failure.
 */
private fun validateAndBuild(
    channelType: ChannelType,
    name: String,
    number: String,
    fieldValues: List<String>,
    fieldConfig: FieldConfig,
    isIpChannel: Boolean,
    ipBroadcast: LgBroadcastType.IpBroadcast,
    rfBroadcast: LgBroadcastType.RfBroadcast,
    polarization: LgPolarization
): Result<LgChannel> {
    // Collect ALL invalid fields across all validation checks
    val errors = mutableSetOf<String>()
    val messages = mutableListOf<String>()

    // Required fields.
    // Number is required for IP channels (no natural channel number in their tuning data).
    // For RF channels the number is always derivable from tuning fields, so it is optional.
    if (name.isBlank()) errors.add("Name")
    if (isIpChannel && number.isBlank()) errors.add("Number")
    fieldConfig.dataFields.zip(fieldValues).forEach { (def, value) ->
        if (value.isBlank()) errors.add(def.label)
    }
    if (errors.isNotEmpty()) messages.add("Required field is empty")

    // Zero-value validation per IDCAP spec
    if (errors.isEmpty()) { // only check zeros if fields aren't blank
        when (channelType) {
            ChannelType.IP_CLASS_1, ChannelType.RF_CLASS_1 -> if (fieldValues[0] == "0") errors.add(fieldConfig.dataFields[0].label)
            ChannelType.RF_CLASS_2, ChannelType.RF_CLASS_5 -> if (fieldValues[0] == "0") errors.add(fieldConfig.dataFields[0].label)
            ChannelType.RF_CLASS_3, ChannelType.RF_CLASS_6 -> if (fieldValues[0] == "0") errors.add(fieldConfig.dataFields[0].label)
            ChannelType.RF_CLASS_4 -> if (fieldValues[1] == "0") errors.add(fieldConfig.dataFields[1].label)
            ChannelType.IP_CLASS_2 -> if (fieldValues[0] == "0" && fieldValues[1] == "0") {
                errors.add(fieldConfig.dataFields[0].label); errors.add(fieldConfig.dataFields[1].label)
            }
        }
        if (errors.isNotEmpty()) messages.add("Value cannot be 0")
    }

    // Range validation per IDCAP spec
    if (errors.isEmpty()) { // only check range if no prior errors
        fieldConfig.dataFields.zip(fieldValues).forEach { (def, value) ->
            if (def.maxValue != null) {
                val numericValue = value.toLongOrNull()
                if (numericValue != null && numericValue > def.maxValue) {
                    errors.add(def.label)
                    messages.add("${def.label} exceeds max value ${def.maxValue}")
                }
            }
        }
    }

    // IP address validation
    if (channelType == ChannelType.IP_CLASS_2 && fieldValues[0].isNotBlank() && !StringUtil.isValidIPv4(fieldValues[0])) {
        errors.add(fieldConfig.dataFields[0].label)
        messages.add("Invalid IPv4 address")
    }

    // Broadcast type validation
    if (isIpChannel && ipBroadcast == LgBroadcastType.IpBroadcast.UNKNOWN) { errors.add("Broadcast Type"); messages.add("A valid broadcast type must be selected") }
    if (!isIpChannel && rfBroadcast == LgBroadcastType.RfBroadcast.UNKNOWN) { errors.add("Broadcast Type"); messages.add("A valid broadcast type must be selected") }

    // Polarization validation
    if (fieldConfig.showPolarization && polarization == LgPolarization.UNKNOWN) { errors.add("Polarization"); messages.add("A valid polarization must be selected") }

    // Return first error message with ALL error fields
    if (errors.isNotEmpty()) return Result.failure(ValidationError("Error: ${messages.first()}.", errors))

    // For RF channels, derive number from tuning data when the user left the field blank.
    // IP channels already validated number as required above, so they always have a value here.
    val effectiveNumber = if (number.isNotBlank()) number else when (channelType) {
        ChannelType.RF_CLASS_1 -> fieldValues[0]                              // logical number
        ChannelType.RF_CLASS_2, ChannelType.RF_CLASS_4, ChannelType.RF_CLASS_5 -> fieldValues[0] // frequency
        ChannelType.RF_CLASS_3 -> "${fieldValues[0]}-${fieldValues[1]}"       // major-minor
        ChannelType.RF_CLASS_6 -> "${fieldValues[0]}-${fieldValues[1]}"       // major-minor
        else -> number  // IP types: already validated non-blank
    }

    return Result.success(when (channelType) {
        ChannelType.IP_CLASS_1 -> LgChannel.IpChannel.Class1(broadcastType = ipBroadcast, logicalNumber = fieldValues[0]).also { it.name(name); it.number(effectiveNumber) }
        ChannelType.IP_CLASS_2 -> LgChannel.IpChannel.Class2(broadcastType = ipBroadcast, ip = fieldValues[0], port = fieldValues[1]).also { it.name(name); it.number(effectiveNumber) }
        ChannelType.RF_CLASS_1 -> LgChannel.RfChannel.Class1(broadcastType = rfBroadcast, logicalNumber = fieldValues[0]).also { it.name(name); it.number(effectiveNumber) }
        ChannelType.RF_CLASS_2 -> LgChannel.RfChannel.Class2(broadcastType = rfBroadcast, frequency = fieldValues[0], programNumber = fieldValues[1]).also { it.name(name); it.number(effectiveNumber) }
        ChannelType.RF_CLASS_3 -> LgChannel.RfChannel.Class3(broadcastType = rfBroadcast, major = fieldValues[0], minor = fieldValues[1]).also { it.name(name); it.number(effectiveNumber) }
        ChannelType.RF_CLASS_4 -> LgChannel.RfChannel.Class4(broadcastType = rfBroadcast, frequency = fieldValues[0], satelliteId = fieldValues[1], programNumber = fieldValues[2], polarization = polarization).also { it.name(name); it.number(effectiveNumber) }
        ChannelType.RF_CLASS_5 -> LgChannel.RfChannel.Class5(broadcastType = rfBroadcast, frequency = fieldValues[0], programNumber = fieldValues[1], plpId = fieldValues[2]).also { it.name(name); it.number(effectiveNumber) }
        ChannelType.RF_CLASS_6 -> LgChannel.RfChannel.Class6(broadcastType = rfBroadcast, major = fieldValues[1], minor = fieldValues[2], plpId = fieldValues[3]).also { it.name(name); it.number(effectiveNumber) }
    })
}

//##################################################################################################
// Previews
//##################################################################################################

private val AllChannelTypes = ChannelType.entries

@AllDevicePreviews
@Composable
private fun PreviewIpClass1() {
    DynamicTheme(darkDynamicTheme) {
        LgChannelDialogContent(
            initialChannel = LgChannel.IpChannel.Class1(
                logicalNumber = "101",
                broadcastType = LgBroadcastType.IpBroadcast.UDP
            ).also { it.name("IPTV Logical"); it.number("1") },
            supportedChannelTypes = AllChannelTypes, region = LgRegion.NORTH_AMERICA,
            onConfirm = {}, onCancel = {}
        )
    }
}

@AllDevicePreviews
@Composable
private fun PreviewIpClass2() {
    DynamicTheme(darkDynamicTheme) {
        LgChannelDialogContent(
            initialChannel = LgChannel.IpChannel.Class2(
                ip = "239.0.0.1", port = "5004",
                broadcastType = LgBroadcastType.IpBroadcast.UDP
            ).also { it.name("ESPN"); it.number("2") },
            supportedChannelTypes = AllChannelTypes, region = LgRegion.NORTH_AMERICA,
            onConfirm = {}, onCancel = {}
        )
    }
}

@AllDevicePreviews
@Composable
private fun PreviewRfClass4() {
    DynamicTheme(darkDynamicTheme) {
        LgChannelDialogContent(
            initialChannel = LgChannel.RfChannel.Class4(
                frequency = "12000", programNumber = "101",
                satelliteId = "SAT-1", polarization = LgPolarization.HORIZONTAL,
                broadcastType = LgBroadcastType.RfBroadcast.SATELLITE
            ).also { it.name("Satellite Ch"); it.number("6") },
            supportedChannelTypes = AllChannelTypes, region = LgRegion.EUROPE,
            onConfirm = {}, onCancel = {}
        )
    }
}

@AllDevicePreviews
@Composable
private fun PreviewNewChannel() {
    DynamicTheme(darkDynamicTheme) {
        LgChannelDialogContent(
            initialChannel = LgChannel.IpChannel.Class2(),
            supportedChannelTypes = AllChannelTypes, region = LgRegion.NORTH_AMERICA,
            onConfirm = {}, onCancel = {}
        )
    }
}
