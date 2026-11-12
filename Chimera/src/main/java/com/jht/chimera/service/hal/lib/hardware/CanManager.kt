package com.jht.chimera.service.hal.lib.hardware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.jht.androidcommonalgorithms.type.JHTCommonTypes
import com.jht.chimera.lib.common.app.AndroidSetting
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.config.frames.FrameConfig
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.RUN_STATE_PAUSE
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.RUN_STATE_RUNNING
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.RUN_STATE_STOP
import com.jht.chimera.lib.common.keypress.KeyPress
import com.jht.chimera.service.hal.lib.hardware.CanManager.Companion.CUSTOM_CAN_FW
import com.jht.chimera.service.hal.lib.io.CmdHandler
import com.jht.datamatrixlib.DataMatrix
import com.jht.libbumpsensor.toBigEndianByteArray
import com.jht.libbumpsensor.toLittleEndianByteArray
import com.jht.mastboard.data.UiEventKeys
import com.jht.utility.unregisterReceiverSafe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

class CanManager {
    companion object {
        private const val TAG = "CanManager"
        private const val CAN_ID_CONSOLE = 0x050

        //Get Versions
        private const val GET_VERSION_COMMAND: Byte = 0x01
        private const val SEND_VERSION_COMMAND: Byte = 0x02
        private const val GET_VERSION_LEFT_COMMAND: Byte = 0x03
        private const val SEND_VERSION_LEFT_COMMAND: Byte = 0x04

        //Power State
        private const val SET_POWER_STATE_COMMAND: Byte = 0x05
        private const val SET_POWER_STATE_LEFT_COMMAND: Byte = 0x07

        //Universal codes for handgrip sides
        private const val GRIP_SIDE_RIGHT: Byte = 0x00
        private const val GRIP_SIDE_LEFT: Byte = 0x01

        //Button Event
        private const val BUTTON_EVENT_COMMAND: Byte = 0x10
        private const val BUTTON_EVENT_LEFT_COMMAND: Byte = 0x12
        private const val BUTTON_ID_UP: Byte = 0x00
        private const val BUTTON_ID_DOWN: Byte = 0x01
        private const val BUTTON_STATE_RELEASE: Byte = 0x00
        private const val BUTTON_STATE_PRESS: Byte = 0x01

        //Swipe Event
        private const val SWIPE_EVENT_COMMAND: Byte = 0x14
        private const val SWIPE_EVENT_LEFT_COMMAND: Byte = 0x16
        private const val SWIPE_DIRECTION_UP: Byte = 0x00
        //private const val SWIPE_DIRECTION_DOWN: Byte = 0x01

        //Heart Rate Event
        private const val HR_EVENT_COMMAND: Byte = 0x18

        //Set LCD Screen Index and Color
        private const val SET_LCD_SCREEN_COMMAND: Byte = 0x20
        private const val SET_LCD_SCREEN_LEFT_COMMAND: Byte = 0x22
        private const val SET_LCD_CONFIG_COMMAND: Byte = 0x24
        private const val SET_LCD_CONFIG_LEFT_COMMAND: Byte = 0x26
        private const val SET_LCD_VALUE_COMMAND: Byte = 0x28
        private const val SET_LCD_VALUE_LEFT_COMMAND: Byte = 0x2A
        private const val SET_LCD_COLOR_COMMAND: Byte = 0x2C
        private const val SET_LCD_COLOR_LEFT_COMMAND: Byte = 0x2E

        //Haptics
        private const val SET_HAPTIC_CONFIG_COMMAND: Byte = 0x30
        private const val SET_HAPTIC_CONFIG_LEFT_COMMAND: Byte = 0x32
        private const val TEST_HAPTICS_COMMAND: Byte = 0x34
        private const val TEST_HAPTICS_LEFT_COMMAND: Byte = 0x36

        //Crossbar GO/STOP commands
        private const val START_WORKOUT_COMMAND: Byte = 0x40
        private const val STOP_WORKOUT_COMMAND: Byte = 0x42

        //Larger data transfer commands
        private const val UPDATE_LCD_CUSTOM_IMAGE_COMMAND: Byte = 0xE0.toByte()
        private const val UPDATE_LCD_CUSTOM_IMAGE_ACK: Byte = 0xE1.toByte()
        private const val UPDATE_LCD_CUSTOM_IMAGE_LEFT_COMMAND: Byte = 0xE2.toByte()
        private const val UPDATE_LCD_CUSTOM_IMAGE_LEFT_ACK: Byte = 0xE3.toByte()
        private const val GET_LCD_CUSTOM_IMG_CRC: Byte = 0xE4.toByte()
        private const val SEND_LCD_CUSTOM_IMG_CRC: Byte = 0xE5.toByte()
        private const val GET_LEFT_LCD_CUSTOM_IMG_CRC: Byte = 0xE6.toByte()
        private const val SEND_LEFT_LCD_CUSTOM_IMG_CRC: Byte = 0xE7.toByte()
        private const val UPDATE_FIRMWARE_COMMAND: Byte = 0xF0.toByte()
        private const val UPDATE_FIRMWARE_ACK: Byte = 0xF1.toByte()
        private const val UPDATE_FIRMWARE_LEFT_COMMAND: Byte = 0xF2.toByte()
        private const val UPDATE_FIRMWARE_LEFT_ACK: Byte = 0xF3.toByte()

        //Firmware update intent strings. These need to be the same values as in UpdateService
        const val UPDATE_CAN_FW_SINGLE: String = "can_firmware_update_single"
        const val UPDATE_CAN_FW_LEFT: String = "can_firmware_update_left"
        const val UPDATE_CAN_FW_RIGHT: String = "can_firmware_update_right"

        const val ACTION_UPDATE_COMPLETE: String = "firmware_update_complete"
        const val ACTION_UPDATE_PROGRESS: String = "firmware_update_progress"
        const val EXTRA_PROGRESS: String = "progress"
        const val EXTRA_SUCCESS: String = "success"
        //adb shell am broadcast actions
        const val UPDATE_CAN_FW_ADB: String = "com.jht.can_firmware.update"
        const val ABORT_CAN_FW_UPDATE: String = "com.jht.can_firmware.update.abort"
        const val CUSTOM_CAN_FW: String = "com.jht.can_firmware.custom"
        const val CRC32_CUSTOM_CAN_FW: String = "com.jht.can_firmware.custom.crc32"
        const val ABORT_CAN_FW_CUSTOM: String = "com.jht.can_firmware.custom.abort"
        const val GET_CAN_FW_VERSION: String = "com.jht.can_firmware.version"
        const val FORCE_CAN_FW_VERSION: String = "com.jht.can_firmware.version.set"
        const val SET_CAN_FW_POWER: String = "com.jht.can_firmware.set_power"
        const val SET_CAN_FW_LCD_SCREEN: String = "com.jht.can_firmware.set_screen"
        const val SET_CAN_FW_LCD_VALUE: String = "com.jht.can_firmware.set_value"
        const val SET_CAN_FW_LCD_COLOR: String = "com.jht.can_firmware.set_color"
        const val CONFIG_CAN_FW_LCD: String = "com.jht.can_firmware.config_lcd"
        const val CONFIG_CAN_FW_HAPTICS: String = "com.jht.can_firmware.config_haptics"
        const val TEST_CAN_FW_HAPTICS: String = "com.jht.can_firmware.test_haptics"
        const val CONFIG_LOGGING: String = "com.jht.can_firmware.logging"

        const val MAX_UPDATE_CHUNK_SIZE: Int = 256

        //Firmware and Custom Image Command Sub-Opcodes
        const val START_UPDATE_SUB_OP: Byte = 0x01
        const val SEND_UPDATE_CHUNK_SUB_OP: Byte = 0x02
        const val COMMIT_UPDATE_SUB_OP: Byte = 0x03
        const val ABORT_UPDATE_SUB_OP: Byte = 0x04
        const val UPDATE_NO_ERROR_CODE: Byte = 0x00

        /**
         * This function intentionally returns "Right" if there is no specified screen side passed to adb
         * Screen Side is used to determine which command ID to send -in the case of setups with grips
         * that use 2 separate CAN nodes - as well as the actual screen side id
         * in most functions that send commands to grips.
         */
        fun getAdbScreenSide(sideString: String?): ScreenSide
        {
            return if (!sideString.isNullOrBlank() && sideString.uppercase() == "LEFT") ScreenSide.Left else ScreenSide.Right
        }

        //Instance for unit tests
        private val testInstance = CanManager()

        fun processCanRx(data: ByteArray) = testInstance.processCanRx(data)

        var firmwareVersion: String
            get() = testInstance.firmwareVersion
            set(value) { testInstance.firmwareVersion = value }

        var canRightHardwareInfo: CanConfigEnum
            get() = testInstance.canRightHardwareInfo
            set(value) { testInstance.canRightHardwareInfo = value }

        fun getSwipeFloat(initialValue: Float, delta: Float, isSwipeUp: Boolean): Float {
            return testInstance.getSwipeFloat(initialValue, delta, isSwipeUp)
        }
    }

    enum class ScreenSide { Right, Left }

    /**
     * Most frames will use the portrait orientation.
     * Recumbent bike right grip will use LandscapeRight (rotated -90 degrees).
     * Recumbent bike left grip will use LandscapeLeft (rotated +90 degrees).
     */
    enum class LcdOrientation { Portrait, LandscapeRight, LandscapeLeft }

    /**
     * ScreenIndex values are sent as bytes to the LCD to determine which image to show
     * 0=MatrixLogo, 1=Custom/Black, 2=Speed, 3=Incline, 4=Resistance, 5=Pause,
     * 6=MatrixLogoLandscape, 7=CustomLandscape, 8=SpeedLandscape, 9=InclineLandscape, A=ResistanceLandscape, B=PauseLandscape
     * Currently the only landscape images in use are for Matrix Logo, Pause, and Resistance.
     * The other landscape values are reserved for possible future use.
     */
    enum class ScreenIndex {
       MatrixLogo, Custom, Speed, Incline, Resistance, Pause,
       MatrixLogoLandscape, CustomLandscape, SpeedLandscape,
       InclineLandscape, ResistanceLandscape, PauseLandscape
    }
    enum class PowerState { SLEEP, WAKE, LOW_POWER, UPDATING }

    enum class CanConfigEnum { SingleCanNode, DoubleCanNodes, UnknownHardware }

    enum class UpdateFileType {Firmware, CustomImage}

    private data class Panel(
        var screen: ScreenIndex = ScreenIndex.MatrixLogo,
        var precision: Int = 0
    )

    private val dataMatrix = DataMatrix()

    private val frameConfig: FrameConfig
        get() {
            return FrameConfigManager.getInstance().frameConfig
        }

    private val right:Panel = Panel()
    private val left:Panel = Panel()
    private var languageId:Int = 0
    private var isMetric: Boolean = false
    private var brightnessLcdPreference:Int = 100
    private var isRecumbent: Boolean = false
    private var runState:Int = 0
    private var io: CmdHandler? = null
    //Flags for LCD setups
    private var isIncline: Boolean = false //This bool uses the frame as source of truth
    private var isSpeed: Boolean = false
    private var isResistance: Boolean = false
    private var isLeftSidePauseStopButtons: Boolean = false

    //The amounts to change the right or left side workout values when a swipe occurs. These change based on frame type
    private var rightSwipeDelta: Float = 1.0f
    private var leftSwipeDelta: Float = 1.0f
    //Public Variables for external use
    var firmwareVersion: String = "0.0.0" //updated via command from handgrips
    var canRightHardwareInfo = CanConfigEnum.UnknownHardware
    var canLeftHardwareInfo = CanConfigEnum.UnknownHardware
    var canConfig = CanConfigEnum.UnknownHardware //updated when we check frame type
    private var heartRate: Int = 0
    private var isUpdatingFlag: Boolean = false
    private var isUploadingCustomImageFlag: Boolean = false
    private var updateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private data class AckResult(val side: ScreenSide, val subOpCode: Byte, val isSuccess: Boolean)
    private val ackChannel = Channel<AckResult>(Channel.UNLIMITED)

    var useLittleEndian: Boolean = true
    var showFirmwareProgressOnLcd: Boolean = false
    var showCanRxLogs:Boolean = false
    var showCanTxLogs:Boolean = false

    /**
     * When using the adb shell command [CUSTOM_CAN_FW], [showCustomImageAfterUpload] is set to true,
     * which will automatically call the function to display the new image on the correct LCD screen.
     * This is to simplify testing the commands as well as image compatibility.
     * After displaying the image this flag is set to false again, so the image can be updated
     * quietly if done through another processes such as first time setup (yet to be implemented)
     */
    var showCustomImageAfterUpload:Boolean = false
    var currentCrc32Value: Int = 0

    fun start(ioCommandHandler: CmdHandler) {
        io = ioCommandHandler

        stop() // ensure clean start
        Log.d(TAG, "--> start CanManager")

        //Setup UX logic differences based on which MachineType is detected
        configurePanelsByMachineType()
        requestVersionInfo(ScreenSide.Right)
        if(isDoubleCanNodes()){
            requestVersionInfo(ScreenSide.Left)
        }
        //Link state and speed/incline/resistance to LCD screen update calls
        createDataMatrixAdapters()
        //Register debug and firmware update adb commands
        registerAdbCalls()
        //Setup LCD Config variables based on current language and metric preferences and send them to both grips
        updateLcdConfig()
        //End of start()
    }

    /**
     * Called in [start] to set up what information the left and right LCD screens will display:
     * Treadmill: L: Incline R: Speed
     * Ascent:    L: Incline R: Resistance
     * Climb mill/Recumbent Bike/Upright Bike:
     *            L: MATRIX/Pause R: Resistance
     * Left and right swipe delta values are also dependent on MachineType
     */
    fun configurePanelsByMachineType() {

        when (frameConfig.machineType) {
            JHTCommonTypes.MachineType.Treadmill -> { //Incline and Speed
                isRecumbent = false
                isIncline = true
                isSpeed = true
                isResistance = false
                isLeftSidePauseStopButtons = false
                rightSwipeDelta = 1.0F //Speed changes by 1.0F on swipe
                leftSwipeDelta = 5.0F //Incline changes by 5% on swipe
                canConfig = CanConfigEnum.SingleCanNode
                left.apply { screen = ScreenIndex.Incline; precision = 1 }
                right.apply { screen = ScreenIndex.Speed; precision = 1 }
            }
            JHTCommonTypes.MachineType.Ascent -> { //Incline and Resistance
                isRecumbent = false
                isIncline = true
                isSpeed = false
                isResistance = true
                isLeftSidePauseStopButtons = true
                rightSwipeDelta = 5.0F //Resistance changes by 5 Levels on swipe
                leftSwipeDelta = 10.0F //Incline changes by 10% on swipe
                canConfig = CanConfigEnum.SingleCanNode
                left.apply { screen = ScreenIndex.Incline; precision = 0 }
                right.apply { screen = ScreenIndex.Resistance; precision = 0 }
            }
            JHTCommonTypes.MachineType.Climbmill -> {// Climb mill - Resistance Only
                isRecumbent = false
                isIncline = false
                isSpeed = false
                isResistance = true
                isLeftSidePauseStopButtons = true
                rightSwipeDelta = 3.0F //Resistance changes by 3 Levels on swipe
                leftSwipeDelta = 0.0F //Left swipe does nothing
                canConfig = CanConfigEnum.DoubleCanNodes
                left.apply { screen = ScreenIndex.MatrixLogo; precision = 0 }
                right.apply { screen = ScreenIndex.Resistance; precision = 0 }
            }
            JHTCommonTypes.MachineType.RecumbentBike -> {
                isRecumbent = true //Use Landscape images
                isIncline = false
                isSpeed = false
                isResistance = true
                isLeftSidePauseStopButtons = true
                rightSwipeDelta = 5.0F //Resistance changes by 5 Levels on swipe
                leftSwipeDelta = 0.0F //Left swipe does nothing
                canConfig = CanConfigEnum.DoubleCanNodes
                left.apply { screen = ScreenIndex.MatrixLogo; precision = 0 }
                right.apply { screen = ScreenIndex.Resistance; precision = 0 }
            }
            else -> { //Upright Bikes - Resistance Only
                isRecumbent = false
                isIncline = false
                isSpeed = false
                isResistance = true
                isLeftSidePauseStopButtons = true
                rightSwipeDelta = 5.0F //Resistance changes by 5 Levels on swipe
                leftSwipeDelta = 0.0F //Left swipe does nothing
                canConfig = CanConfigEnum.DoubleCanNodes
                left.apply { screen = ScreenIndex.MatrixLogo; precision = 0 }
                right.apply { screen = ScreenIndex.Resistance; precision = 0 }
            }
        }
    }

    fun stop() {
        dataMatrix.finish()
        val context = ContextUtil.getApplicationContext()
        context?.unregisterReceiverSafe(commandReceiver)
    }

    private fun ByteArray.toHexString(format: HexFormat = HexFormat.Default): String = toHexString(0, size, format)
    private fun panelOf(side: ScreenSide): Panel = if (side == ScreenSide.Right) right else left

    /**
     * Packs [payload] into a message that is sent to the IO
     */
    private fun sendCanMessage(payload: ByteArray) {
        // Dynamically allocate based on payload size + 2 bytes for ID
        val buffer = ByteBuffer.allocate(2 + payload.size).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            putShort(CAN_ID_CONSOLE.toShort())
            put(payload)
        }.array()

        io?.sendCanTx(buffer)

        if (showCanTxLogs && !(isUpdatingFlag || isUploadingCustomImageFlag)) {
            Log.d(TAG, "--> sendCanMessage canId=0x${CAN_ID_CONSOLE.toString(16)} payload=[${buffer.toHexString()}]")
        }
    }

    /**
     * Asks [side] grip to respond with a CAN reply containing version info
     */
    private fun requestVersionInfo(side: ScreenSide) {
        val payload: ByteArray = byteArrayOf(
            when (side) {
                ScreenSide.Right -> GET_VERSION_COMMAND
                ScreenSide.Left -> GET_VERSION_LEFT_COMMAND
            }
        )
        sendCanMessage(payload)
    }

    /**
     * Tells the [side] grip to change its LCD screen to display 1 of 6 images
     * [index] 0x00 = MatrixLogo, 0x01 = Custom/Black, 0x02 = Speed,
     *         0x03 = Incline, 0x04 = Resistance, 0x05 = Pause
     */
    private fun setLcdScreenIndex(side: ScreenSide, index: ScreenIndex) {
        val optionalParameter: Byte = 0x00
        val payload = byteArrayOf(
            SET_LCD_SCREEN_COMMAND,
            side.ordinal.toByte(),
            index.ordinal.toByte(),
            optionalParameter
        )
        if (isDoubleCanNodes() && side == ScreenSide.Left) {
            payload[0] = SET_LCD_SCREEN_LEFT_COMMAND
        }
        Log.d(TAG, "Change LCD $side Screen to $index ")
        sendCanMessage(payload)
    }

    /**
     * Asks the grips to change their screen to display a solid color using
     * [red], [green], and [blue] values
     * This function is callable via ADB Debug command e.g.
     * "com.jht.can_firmware.set_color --es color 0000ff" (for "blue")
     * Note that adb does not allow a "#" in extra data so "--es color #0000ff" causes an error
     */
    private fun setLcdColor(side: ScreenSide, red: Int, green: Int, blue: Int) {

        val payload = byteArrayOf(
            SET_LCD_COLOR_COMMAND,
            side.ordinal.toByte(),
            red.toByte(),
            green.toByte(),
            blue.toByte()
        )
        if (isDoubleCanNodes() && side == ScreenSide.Left) {
            payload[0] = SET_LCD_COLOR_LEFT_COMMAND
        }
        Log.d(TAG, "Change LCD $side Color to #${red.toString(16)},${green.toString(16)},${blue.toString(16)} ")
        sendCanMessage(payload)
    }

    /**
     * Asks the grips to change their power mode.
     * [state] 0x00 = Sleep, 0x01 = Wake, 0x02 = Low Power, 0x03 = Updating
     * This function is callable via ADB Debug command
     * "com.jht.can_firmware.set_power"
     */
    private fun setPowerState(screenSide: ScreenSide, state: PowerState) {

        val payload = byteArrayOf(
            SET_POWER_STATE_COMMAND,
            screenSide.ordinal.toByte(),
            state.ordinal.toByte(),
        )

        if (isDoubleCanNodes() && screenSide == ScreenSide.Left) {
            payload[0] = SET_POWER_STATE_LEFT_COMMAND
        }

        Log.d(TAG, "Set Power State $state side: $screenSide")
        sendCanMessage(payload)
    }

    fun sendGripsToSleep() {
        setPowerState(ScreenSide.Right, PowerState.SLEEP)
        if (isDoubleCanNodes()) {
            setPowerState(ScreenSide.Left, PowerState.SLEEP)
        }
    }

    /**
     * Constructs a payload to send a set lcd [value] message to the [side] handgrip
     * If [precision] is left to default, uses the setting saved in panel.precision
     * Note that both old and new versions of toggle firmware use Little Endian.
     */
    private fun setLcdValue(side: ScreenSide, value: Float, precision: Int = -1, overridePause: Boolean = false) {
        if (!(isWorkoutRunning() || overridePause)) {
            Log.d(TAG, "setLcdValue called when workout is paused")
            return
        }

        val command = if (isDoubleCanNodes() && side == ScreenSide.Left) SET_LCD_VALUE_LEFT_COMMAND else SET_LCD_VALUE_COMMAND
        val precisionByte = (if (precision < 0) panelOf(side).precision else precision).toByte()

        val payload = ByteBuffer.allocate(7).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put(command)
            put(side.ordinal.toByte())
            put(precisionByte)
            putFloat(value)
        }.array()

        sendCanMessage(payload)
    }

    /**
     * Looks at the user settings for selected language and Metric preference and sends them,
     * along with the brightness preference, to both sides of the handgrips.
     */
    private fun updateLcdConfig() {
        Log.d(TAG, "Update LCD Config")
        languageId = DataMatrix.getInt(SettingKeys.General.DATA_SELECTED_LANGUAGE_ENUM_INT)
        isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)
        val metricCode: Int = if (isMetric) 1 else 0

        // Check if the current machine is something other than a Recumbent Bike
        val isDefaultOrientation: Boolean = frameConfig.machineType != JHTCommonTypes.MachineType.RecumbentBike

        // Select orientations based on configuration
        val (leftOrientation, rightOrientation) = if (isDefaultOrientation) {
            Pair(LcdOrientation.Portrait, LcdOrientation.Portrait)
        } else {
            Pair(LcdOrientation.LandscapeLeft, LcdOrientation.LandscapeRight)
        }

        // Apply configuration to both screens
        setLcdConfig(ScreenSide.Left, metricCode, languageId, brightnessLcdPreference, leftOrientation.ordinal)
        setLcdConfig(ScreenSide.Right, metricCode, languageId, brightnessLcdPreference, rightOrientation.ordinal)
    }

    /**
     * Sends a message to ask the grips to change their configuration
     * [isMetric] uses the same ordinals as i18n\Unit.java
     * [languageId] uses the same ordinals as i18n\LanguageProperty.java
     * [brightness] should be a value between 0 and 100
     * [orientation] 0 = Portrait, 1 = LandscapeRight, 2 = LandscapeLeft
     */
    private fun setLcdConfig(side: ScreenSide, isMetric:Int, languageId: Int, brightness:Int, orientation: Int = 0) {
        val payload = byteArrayOf(
            SET_LCD_CONFIG_COMMAND,
            isMetric.toByte(),
            languageId.toByte(),
            brightness.toByte(),
            orientation.toByte()
        )
        if (isDoubleCanNodes() && side == ScreenSide.Left) {
            payload[0] = SET_LCD_CONFIG_LEFT_COMMAND
        }
        Log.d(TAG, "Set LCD Config $side, $isMetric, $languageId, $brightness, $orientation")
        sendCanMessage(payload)
    }

    /**
     *  Request the grips change their haptics settings
     *  [strength]: 0x00 = Off, 0x01 = Low, 0x02 = Medium, 0x03 = High
     *  [logic]: 0x00 = Off, 0x01 = Buzz Once, 0x02+ = Unused
     *  This function is _currently_ only callable via ADB Debug command
     *  "com.jht.can_firmware.config_haptics"
     */
    private fun setHapticsConfig(side: ScreenSide, strength:Int, logic: Int) {
        val payload = byteArrayOf(
            SET_HAPTIC_CONFIG_COMMAND,
            strength.toByte(),
            logic.toByte()
        )
        if (isDoubleCanNodes() && side == ScreenSide.Left) {
            payload[0] = SET_HAPTIC_CONFIG_LEFT_COMMAND
        }
        sendCanMessage(payload)
    }

    /**
     * Creates a message to request that grips run a haptics test.
     * This function is currently only callable via ADB Debug command
     * "com.jht.can_firmware.test_haptics"
     */
    private fun testHaptics(side: ScreenSide) {
        val payload = byteArrayOf(
            TEST_HAPTICS_COMMAND,
        )
        if (isDoubleCanNodes() && side == ScreenSide.Left) {
            payload[0] = TEST_HAPTICS_LEFT_COMMAND
        }
        sendCanMessage(payload)
    }

    /**
     * A helper function used to turn [key] and [state] information
     * parsed from CAN grips into actual key events
     */
    private fun sendKeyEvent(key: KeyPress.KeyCode, state: KeyPress.KeyState) {
        val bitmapKey = KeyPress.createBitmapKey(key, state)
        DataMatrix.putInt(HALKeys.EVENT_KEYPRESS_BITMAP_INT, bitmapKey)
    }

    /**
     * The big function that parses the bytes from a CAN RX payload into instructions
     */
    fun processCanRx(data: ByteArray) {
        if (data.size < 3) {
            Log.e(TAG, "Invalid CAN RX frame: too short")
            return
        }
        //Log.d(TAG, "RX: ${data.toHexString()}")

        // Extract little-endian CAN ID
        val canId = (data[0].toInt() and 0xFF) or
                ((data[1].toInt() and 0xFF) shl 8)
        val payload = data.copyOfRange(2, data.size)

        var messageID: Byte = 0x00
        var gripSide: Byte = 0x00
        var inputDirection: Byte = 0x00
        var inputState: Byte = 0x00

        if (payload.isNotEmpty()) {
            messageID = payload[0]
        }
        if (payload.size > 1) {
            gripSide = payload[1]
        }
        if (payload.size > 2) inputDirection = payload[2]
        if (payload.size > 3) inputState = payload[3]

        var keyState = KeyPress.KeyState.KEY_DOWN

        if (isUpdatingFlag || isUploadingCustomImageFlag) { //Only process update related messages while updating
            when (messageID) {
                UPDATE_FIRMWARE_ACK, UPDATE_FIRMWARE_LEFT_ACK,
                UPDATE_LCD_CUSTOM_IMAGE_ACK, UPDATE_LCD_CUSTOM_IMAGE_LEFT_ACK -> {
                    val side = if (messageID == UPDATE_FIRMWARE_ACK || messageID == UPDATE_LCD_CUSTOM_IMAGE_ACK) ScreenSide.Right else ScreenSide.Left
                    val subOp = if (payload.size > 1) payload[1] else 0.toByte()
                    val success = if (payload.size > 2) payload[2] == UPDATE_NO_ERROR_CODE else false
                    scope.launch {
                        ackChannel.send(AckResult(side, subOp, success))
                    }
                    return
                }
            }
            return
        }

        if(showCanRxLogs && payload[0] != HR_EVENT_COMMAND) {
            Log.d(TAG, "--> processCanRx: canId=0x${canId.toString(16)} payload=[${payload.toHexString()}]")
        }

        if (inputState == BUTTON_STATE_PRESS) {
            keyState = KeyPress.KeyState.KEY_DOWN
        } else if (inputState == BUTTON_STATE_RELEASE) {
            keyState = KeyPress.KeyState.KEY_UP
        }

        //Treat left buttons like Pause/Stop for Double CAN
        if (isDoubleCanNodes() && messageID == BUTTON_EVENT_LEFT_COMMAND) {
            when (inputDirection) {
                BUTTON_ID_UP -> {
                    sendKeyEvent(KeyPress.KeyCode.KEY_PAUSE, KeyPress.KeyState.KEY_DOWN)
                    Log.d(TAG, "PAUSE BUTTON (ALT) PRESSED")
                }
                BUTTON_ID_DOWN -> {
                    sendKeyEvent(KeyPress.KeyCode.KEY_STOP, KeyPress.KeyState.KEY_DOWN)
                    Log.d(TAG, "STOP BUTTON (ALT) PRESSED")
                }
            }
            return
        }

        //Main message parsing block
        when (messageID) {
            STOP_WORKOUT_COMMAND -> {
                sendKeyEvent(KeyPress.KeyCode.KEY_STOP, KeyPress.KeyState.KEY_DOWN)
                Log.d(TAG, "STOP BUTTON PRESSED")
            }

            START_WORKOUT_COMMAND -> {
                Log.d(TAG, "PAUSE/GO BUTTON PRESSED")
                when {
                    isIncline -> sendKeyEvent(KeyPress.KeyCode.KEY_GO, KeyPress.KeyState.KEY_DOWN)
                    // Climb Mill uses pause button instead of play button
                    isLeftSidePauseStopButtons -> sendKeyEvent(KeyPress.KeyCode.KEY_PAUSE, KeyPress.KeyState.KEY_DOWN)
                }
            }

            BUTTON_EVENT_COMMAND, BUTTON_EVENT_LEFT_COMMAND -> {
                when (gripSide){
                    GRIP_SIDE_LEFT -> {
                        sendKeyEvent(handleLeftGripButton(inputDirection), keyState)
                    }
                    GRIP_SIDE_RIGHT -> {
                        sendKeyEvent(handleRightGripButton(inputDirection), keyState)
                    }
                }
            }
            SWIPE_EVENT_COMMAND, SWIPE_EVENT_LEFT_COMMAND -> onSwipe(inputDirection, gripSide)
            HR_EVENT_COMMAND -> {
                if (payload.size > 1) {
                    heartRate = payload[1].toInt() and 0xFF
                    DataMatrix.putInt(HALKeys.DATA_CAN_HEART_RATE_INT, heartRate)
                }
            }

            SEND_VERSION_COMMAND -> parseVersionInformation(ScreenSide.Right, payload)
            SEND_VERSION_LEFT_COMMAND -> parseVersionInformation(ScreenSide.Left, payload)
            SEND_LCD_CUSTOM_IMG_CRC, SEND_LEFT_LCD_CUSTOM_IMG_CRC -> parseCustomImageCrc(payload)
        }
    }

    private fun handleLeftGripButton(direction: Byte): KeyPress.KeyCode {
        return when (direction) {
            BUTTON_ID_UP -> KeyPress.KeyCode.KEY_INCLINE_UP
            BUTTON_ID_DOWN -> KeyPress.KeyCode.KEY_INCLINE_DOWN
            else -> KeyPress.KeyCode.KEY_UNKNOWN.also { Log.e(TAG, "Unexpected direction key byte: $direction") }
        }
    }

    private fun handleRightGripButton(direction: Byte): KeyPress.KeyCode {
        return when (direction) {
            BUTTON_ID_UP -> if (isSpeed) KeyPress.KeyCode.KEY_SPEED_UP else KeyPress.KeyCode.KEY_RESISTANCE_UP
            BUTTON_ID_DOWN -> if (isSpeed) KeyPress.KeyCode.KEY_SPEED_DOWN else KeyPress.KeyCode.KEY_RESISTANCE_DOWN
            else -> KeyPress.KeyCode.KEY_UNKNOWN.also { Log.e(TAG, "Unexpected direction key byte: $direction") }
        }
    }

    /**
     * Parses swipe information into UI actions based on the [gripSide], [inputDirection]
     * and frame configuration. The swipe action increases or decreases associated values
     * by a larger amount than tapping a button. That amount is different for each frame type.
     */
    private fun onSwipe(inputDirection: Byte, gripSide: Byte) {
        val isSwipeUp = inputDirection == SWIPE_DIRECTION_UP
        Log.d(TAG, "Swipe ${if (isSwipeUp) "up" else "down"}")
        if (isWorkoutRunning()){
            when (gripSide) {
                GRIP_SIDE_RIGHT -> handleRightSwipe(isSwipeUp)
                GRIP_SIDE_LEFT -> handleLeftSwipe(isSwipeUp)
                else -> {Log.e(TAG, "Unexpected grip side byte: $gripSide")}
            }
        }
    }

    private fun handleRightSwipe(isSwipeUp: Boolean) {
        when {
            isSpeed -> {
                val currentSpeedKph = DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT)
                val swipeSpeedKph = if (isMetric) {
                    getSwipeFloat(currentSpeedKph, rightSwipeDelta, isSwipeUp)
                } else {
                    val speedMph = getSwipeFloat(
                        SpeedConvertUtil.convertKphToMph(currentSpeedKph),
                        rightSwipeDelta,
                        isSwipeUp
                    )
                    SpeedConvertUtil.convertMphToKph(speedMph)
                }
                DataMatrix.putFloat(WorkoutKeys.ACTION_SET_SPEED_KPH_WORKOUT_FLOAT, swipeSpeedKph)
            }
            isResistance -> {
                val currentResistance = DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT)
                val swipeResistance = getSwipeInt(currentResistance, rightSwipeDelta.toInt(), isSwipeUp)
                DataMatrix.putInt(WorkoutKeys.ACTION_SET_RESISTANCE_WORKOUT_INT, swipeResistance)
            }
        }
    }

    private fun handleLeftSwipe(isSwipeUp: Boolean) {
        if (isIncline) {
            val incline = DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT)
            val swipeIncline = getSwipeFloat(incline, leftSwipeDelta, isSwipeUp)
            DataMatrix.putFloat(WorkoutKeys.ACTION_SET_INCLINE_WORKOUT_FLOAT, swipeIncline)
        }
    }

    /**
     * Get the new value that is changed by a "swipe" gesture
     */
    fun getSwipeFloat(initialValue: Float, delta: Float, isSwipeUp: Boolean):Float {
        return if (isSwipeUp) {
            initialValue + delta
        } else {
            initialValue - delta
        }
    }

    /**
     * Get the new value that is changed by a "swipe" gesture
     */
    fun getSwipeInt(initialValue:Int, delta:Int, isSwipeUp: Boolean):Int {
        return if (isSwipeUp) {
            initialValue + delta
        } else {
            initialValue - delta
        }
    }
    /*
        byte[1] major
        byte[2] minor
        byte[3] patch
        byte[4] hardware_type
            0x00 = Smart Plastics (single CAN node)
            0x01 = Smart Plastics (two CAN nodes)
            0x02 = Dumb Plastics (two CAN nodes)
    */

    /**
     * Checks a version information response from handgrips and logs results.
     * At some point this information could also go somewhere useful like show up in
     * the project dashboard. This also populates [canConfig] which is used
     * as a conditional elsewhere in  CanManager, but canConfig is also
     * populated by frame information, which could be a better source of truth.
     */
    private fun parseVersionInformation(side: ScreenSide, payload: ByteArray) {

        if (payload.size < 5) {
            Log.e(
                TAG,
                "parseVersionInformation ByteArray too short. Expected size: 5, got size: " + payload.size.toString()
            )
            firmwareVersion = "unknown"
            return
        }

        val majorVersion: UInt = payload[1].toUByte().toUInt()
        val minorVersion: UInt = payload[2].toUByte().toUInt()
        val patchVersion: UInt = payload[3].toUByte().toUInt()
        val hardwareType: Int = payload[4].toUByte().toInt()
        var tmpLeftVersion: String

        if (side == ScreenSide.Right) {
            firmwareVersion = "$majorVersion.$minorVersion.$patchVersion"
            Log.i(TAG, "Firmware Version: $firmwareVersion")
            Log.i(TAG, "Hardware Type: $hardwareType")

            canRightHardwareInfo = decodeHardwareType(hardwareType)
            //This is a temporary compatibility fix for older firmware that will likely still be floating around during testing of frames.
            if(majorVersion<= 0u && minorVersion<= 1u) {
                Log.w(TAG, "Older firmware detected. Switching to Big Endian mode for back compatibility.")
                useLittleEndian = false
            }

            updateVersionInSettings(firmwareVersion, canRightHardwareInfo, ScreenSide.Right)
        } else {
            tmpLeftVersion = "$majorVersion.$minorVersion.$patchVersion"
            canLeftHardwareInfo = decodeHardwareType(hardwareType)

            if (firmwareVersion == tmpLeftVersion) {
                Log.d(TAG, "Firmware L/R Match: $tmpLeftVersion")
            } else {
                //There are some situations where one side might not have the same version number on purpose, so this is just a warning.
                Log.w(TAG, "Firmware Mismatch! Right Ver: $firmwareVersion, Left Ver: $tmpLeftVersion")
            }

            updateVersionInSettings(tmpLeftVersion, canLeftHardwareInfo, ScreenSide.Left)
        }
    }

    /**
     * The original TG0 firmware used a different byte value to indicate hardware type,
     * the JIS toggles send 2 for double can nodes instead of 1, and send 3 instead of 0
     * for the single node hardware. This might be something to revise with JIS now that we are
     * no longer using the TG0 toggles at all, but it's quicker just to decode it this way.
     */
    private fun decodeHardwareType(hardwareType: Int): CanConfigEnum {
        return when (hardwareType) {
            0 -> CanConfigEnum.SingleCanNode
            1 -> CanConfigEnum.DoubleCanNodes
            2 -> CanConfigEnum.DoubleCanNodes
            3 -> CanConfigEnum.SingleCanNode
            else -> CanConfigEnum.UnknownHardware
        }
    }

    private fun isDoubleCanNodes(): Boolean {
        return  canConfig == CanConfigEnum.DoubleCanNodes
    }

    private fun isWorkoutRunning(): Boolean {
        return DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == RUN_STATE_RUNNING
    }

    /**
     * Create adapters to link run state, and the speed/incline/resistance values to
     * the functions that will send CAN messages to update the grips LCD screens
     * with state and value information
     */
    private fun createDataMatrixAdapters() {

        //Recumbent bike uses rotated versions of logo, pause, and resistance images
        val matrixLogoIndex = if(isRecumbent) ScreenIndex.MatrixLogoLandscape else ScreenIndex.MatrixLogo
        val pauseIndex = if(isRecumbent) ScreenIndex.PauseLandscape else ScreenIndex.Pause
        val resistanceIndex = if(isRecumbent) ScreenIndex.ResistanceLandscape else ScreenIndex.Resistance

        // Update the screen based on run state
        dataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, { value: Int? ->
            value?.let { runState = it }
            when (value) {
                RUN_STATE_STOP -> {
                    Log.d(TAG, "RUN_STATE_STOP → show Matrix logos")
                    setLcdScreenIndex(ScreenSide.Right, matrixLogoIndex)
                    setLcdScreenIndex(ScreenSide.Left, matrixLogoIndex)
                }

                RUN_STATE_RUNNING -> {
                    Log.d(TAG, "RUN_STATE_RUNNING → show workout panels")
                    //Left Screen
                    if (isIncline) {
                        setLcdScreenIndex(ScreenSide.Left, ScreenIndex.Incline)
                        setLcdValue(ScreenSide.Left, DataMatrix.getFloat(HALKeys.DATA_ACTUAL_INCLINE_FLOAT), -1)
                    } else {
                        setLcdScreenIndex(ScreenSide.Left, matrixLogoIndex)
                    }

                    //Right Screen
                    if (isSpeed) {
                        setLcdScreenIndex(ScreenSide.Right, ScreenIndex.Speed)
                        val speedKph = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT)
                        setLcdValue(ScreenSide.Right, if (isMetric) speedKph else SpeedConvertUtil.convertKphToMph(speedKph), -1)
                    } else if (isResistance) {
                        setLcdScreenIndex(ScreenSide.Right, resistanceIndex)
                        setLcdValue(ScreenSide.Right, DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT).toFloat(), -1)
                    } else {
                        setLcdScreenIndex(ScreenSide.Right, matrixLogoIndex)
                    }
                }

                RUN_STATE_PAUSE -> {
                    Log.d(TAG, "RUN_STATE_PAUSE")
                    setLcdScreenIndex(ScreenSide.Right, pauseIndex)
                    setLcdScreenIndex(ScreenSide.Left, pauseIndex)
                }
            }
        }, true)

        // Update the speed which is always on right side
        dataMatrix.createFloatAdapter(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, { value: Float? ->
            if (runState == RUN_STATE_STOP) return@createFloatAdapter
            value?.let {
                val speed = if (isMetric) it else SpeedConvertUtil.convertKphToMph(it)
                setLcdValue(ScreenSide.Right, speed)
            }
        }, false)

        // Update the incline which is always on left side
        dataMatrix.createFloatAdapter(HALKeys.DATA_TARGET_INCLINE_FLOAT, { value: Float? ->
            if (runState == RUN_STATE_STOP) return@createFloatAdapter
            value?.let {
                setLcdValue(ScreenSide.Left, value)
            }
        }, false)

        // Update the resistance which is always on right side
        dataMatrix.createIntAdapter(HALKeys.DATA_RESISTANCE_INT, { value: Int? ->
            if (runState == RUN_STATE_STOP) return@createIntAdapter
            value?.let {
                setLcdValue(ScreenSide.Right, value.toFloat())
            }
        }, false)

        // Update the metric value
        dataMatrix.createBoolAdapter(
            UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL //FIND THE LOCAL VALUE THAT CHANGES AND BOOL ADAPT THERE INSTEAD
        ) { value: Boolean? ->
            isMetric = value == true
            updateLcdConfig()
        }

        dataMatrix.createIntAdapter(SettingKeys.General.DATA_SELECTED_LANGUAGE_ENUM_INT, { updateLcdConfig() }, false)
    }

    /**
     * Register ADB functions used in debugging/testing CAN peripherals
     */
    private fun registerAdbCalls() {
        val context = ContextUtil.getApplicationContext()
        val intentFilters = arrayOf(
            UPDATE_CAN_FW_SINGLE, UPDATE_CAN_FW_RIGHT, UPDATE_CAN_FW_LEFT,
            UPDATE_CAN_FW_ADB, ABORT_CAN_FW_UPDATE, CUSTOM_CAN_FW,
            CRC32_CUSTOM_CAN_FW, ABORT_CAN_FW_CUSTOM, GET_CAN_FW_VERSION, FORCE_CAN_FW_VERSION,
            SET_CAN_FW_POWER, SET_CAN_FW_LCD_SCREEN, SET_CAN_FW_LCD_VALUE, SET_CAN_FW_LCD_COLOR,
            CONFIG_CAN_FW_LCD, CONFIG_CAN_FW_HAPTICS, TEST_CAN_FW_HAPTICS, CONFIG_LOGGING
        )
        if(context != null) {
            for (intentString in intentFilters) {
                ContextCompat.registerReceiver(
                    context,
                    commandReceiver,
                    IntentFilter(intentString),
                    ContextCompat.RECEIVER_EXPORTED
                )
            }
        }
    }

    /**
     * Receives and parses the adb shell am broadcast for commands, mostly used for testing and forcing firmware updates.
     * Also received messages from the UpdateService for updates via USB and AWS
     */
    private val commandReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            Log.d(TAG, "onReceive: " + intent.action)

            when(intent.action) {
                UPDATE_CAN_FW_SINGLE -> {

                    if(isDoubleCanNodes()) {
                        Log.w(TAG, "Ignoring request to update single CAN version.")
                        return
                    }

                    val path = intent.getStringExtra("path")
                    if(path!=null) {
                        updateFirmware(UpdateFileType.Firmware, ScreenSide.Right, path)
                    } else {
                        Log.e(TAG, "Missing filepath string.")
                    }
                }
                UPDATE_CAN_FW_RIGHT -> {

                    if(!isDoubleCanNodes()) {
                        Log.w(TAG, "Ignoring request to update right CAN version.")
                        return
                    }

                    val path = intent.getStringExtra("path")
                    if(path!=null) {
                        updateFirmware(UpdateFileType.Firmware, ScreenSide.Right, path)
                    } else {
                        Log.e(TAG, "Missing filepath string.")
                    }
                }
                UPDATE_CAN_FW_LEFT -> {

                    if(!isDoubleCanNodes()) {
                        Log.w(TAG, "Ignoring request to update left CAN version.")
                        return
                    }

                    val path = intent.getStringExtra("path")
                    if(path!=null) {
                        updateFirmware(UpdateFileType.Firmware, ScreenSide.Left, path)
                    } else {
                        Log.e(TAG, "Missing filepath string.")
                    }
                }

                UPDATE_CAN_FW_ADB -> {
                    val side: String? = intent.getStringExtra("side")
                    val screenSide: ScreenSide = getAdbScreenSide(side)
                    val path = intent.getStringExtra("path")
                    if(path!=null) {
                        updateFirmware(UpdateFileType.Firmware, screenSide, path)
                    } else {
                        Log.e(TAG, "Missing filepath string. Use --es path to define a filepath")
                    }
                }
                ABORT_CAN_FW_UPDATE -> {
                    abortFirmwareUpdate(UpdateFileType.Firmware)
                }
                CUSTOM_CAN_FW-> {
                    val side: String? = intent.getStringExtra("side")
                    val screenSide: ScreenSide = getAdbScreenSide(side)
                    val path = intent.getStringExtra("path")
                    if(path != null) {
                        updateFirmware(UpdateFileType.CustomImage, screenSide, path)
                    } else {
                        Log.e(TAG, "Missing filepath string. Use --es path to define a filepath")
                    }
                }
                ABORT_CAN_FW_CUSTOM-> {
                    abortFirmwareUpdate(UpdateFileType.CustomImage)
                }
                CRC32_CUSTOM_CAN_FW-> {
                    val side: String? = intent.getStringExtra("side")
                    val screenSide: ScreenSide = getAdbScreenSide(side)
                    val path: String? = intent.getStringExtra("path")
                    val i: Int = intent.getIntExtra("index", 1)

                    checkCustomImageCrc32(screenSide, path, i)
                }
                GET_CAN_FW_VERSION -> {
                    val side: String? = intent.getStringExtra("side")
                    val screenSide: ScreenSide = getAdbScreenSide(side)
                    requestVersionInfo(screenSide)
                }
                FORCE_CAN_FW_VERSION -> {
                    val side: String? = intent.getStringExtra("side")
                    val screenSide: ScreenSide = getAdbScreenSide(side)
                    val forcedVersion: String? = intent.getStringExtra("version")
                    val versionRegex = Regex("""^\d+\.\d+\.\d+$""")

                    val isValid = forcedVersion != null && versionRegex.matches(forcedVersion)
                    if(isValid) {
                        updateVersionInSettings(forcedVersion, canConfig, screenSide)
                    } else {
                        Log.e(TAG, "Use --es version to define a version. Must be in format: \"0.0.0\"")
                    }
                }
                SET_CAN_FW_POWER -> {
                    val side:String? = intent.getStringExtra("side")
                    val screenSide: ScreenSide = getAdbScreenSide(side)
                    val stateString:String? = intent.getStringExtra("state")?.uppercase()
                    var powerState: PowerState = PowerState.WAKE
                    when(stateString){
                        "SLEEP"-> powerState = PowerState.SLEEP
                        "WAKE"-> powerState = PowerState.WAKE
                        "LOW_POWER"-> powerState = PowerState.LOW_POWER
                        "UPDATING"-> powerState = PowerState.UPDATING
                    }
                    setPowerState(screenSide, powerState)
                }
                SET_CAN_FW_LCD_SCREEN -> {
                    val side: String? = intent.getStringExtra("side")
                    val screenSide: ScreenSide = getAdbScreenSide(side)
                    val i: Int = intent.getIntExtra("index", 0)
                    var screenIndex: ScreenIndex
                    if (-1 < i && i < ScreenIndex.entries.size) {
                        screenIndex = ScreenIndex.entries[i]
                    } else {
                        Log.e(TAG, "Index $i is out of bounds.")
                        return
                    }
                    setLcdScreenIndex(screenSide, screenIndex)
                }
                SET_CAN_FW_LCD_VALUE -> {
                    val side: String? = intent.getStringExtra("side")
                    val screenSide: ScreenSide = getAdbScreenSide(side)
                    val precision: Int = intent.getIntExtra("precision", 0)
                    val value = intent.getFloatExtra("value", 0f)
                    setLcdValue(screenSide, value, precision, true)
                }
                SET_CAN_FW_LCD_COLOR -> {
                    val side: String? = intent.getStringExtra("side")
                    val screenSide: ScreenSide = getAdbScreenSide(side)
                    val colorString = "#"+intent.getStringExtra("color")
                    Log.d(TAG, "Set color $colorString")
                    colorString.let {
                        try {
                            val colorInt = it.toColorInt()
                            val red = Color.red(colorInt)
                            val green = Color.green(colorInt)
                            val blue = Color.blue(colorInt)
                            setLcdColor(screenSide, red, green, blue)
                        } catch (_: Exception) {
                            Log.e("ColorReceiver", "Invalid color format: $it")
                        }
                    }
                }
                CONFIG_CAN_FW_LCD -> {
                    Log.d(TAG, "Config LCD")
                    val side: String? = intent.getStringExtra("side")
                    val screenSide: ScreenSide = getAdbScreenSide(side)
                    val isMetric: Int = intent.getIntExtra("metric", 0)
                    val languageId:Int = intent.getIntExtra("language", 0)
                    val brightness:Int = 100.coerceAtMost(intent.getIntExtra("brightness", 100))
                    setLcdConfig(screenSide, isMetric, languageId, brightness)
                }
                CONFIG_CAN_FW_HAPTICS -> {
                    Log.d(TAG, "Config Haptics")
                    val side: String? = intent.getStringExtra("side")
                    val screenSide: ScreenSide = getAdbScreenSide(side)
                    val strength: Int = intent.getIntExtra("strength", 2)
                    val logic:Int = intent.getIntExtra("logic", 1)
                    setHapticsConfig(screenSide, strength, logic)
                }
                TEST_CAN_FW_HAPTICS -> {
                    Log.d(TAG, "Test Haptics")
                    val side: String? = intent.getStringExtra("side")
                    val screenSide: ScreenSide = getAdbScreenSide(side)
                    testHaptics(screenSide)
                }
                CONFIG_LOGGING -> {
                    showCanRxLogs = intent.getBooleanExtra("rx", showCanRxLogs)
                    showCanTxLogs = intent.getBooleanExtra("tx", showCanTxLogs)
                    showFirmwareProgressOnLcd = intent.getBooleanExtra("upload_progress", showFirmwareProgressOnLcd)
                    showCustomImageAfterUpload = intent.getBooleanExtra("show_custom", showCustomImageAfterUpload)
                    Log.d(TAG, "Configure Logging Options rx: $showCanRxLogs, tx: $showCanTxLogs, upload_progress: $showFirmwareProgressOnLcd, show_custom: $showCustomImageAfterUpload")
                }
                else -> {
                    Log.e(TAG, "Not a valid action: ${intent.action}")
                }
            }
        }
    }

    /**
     * Called via ADB command to kick off a firmware update on the [screenSide] grip. E.g.
     * "adb shell am broadcast -a com.jht.can_firmware.update --es path sdcard/update/update_filename.bin"
     * or "adb shell am broadcast -a com.jht.can_firmware.custom --es path sdcard/update/image_filename.bin"
     * [updateFileType] determines if this is a firmware file or custom image file.
     */
    fun updateFirmware(updateFileType: UpdateFileType, screenSide: ScreenSide, path:String)
    {
        DataMatrix.putString(
            UiEventKeys.Key.DATA_UI_EVENT_STRING,
            UiEventKeys.EVENT.UPDATE.name
        )

        val file = File(path)
        if (file.exists()) {
            updateJob?.cancel()
            updateJob = scope.launch {
                performUpdate(updateFileType, screenSide, file)
            }
        } else {
            Log.e(TAG, "FILE NOT FOUND: $path")
        }
    }

    private suspend fun performUpdate(updateFileType: UpdateFileType, side: ScreenSide, file: File) {
        val startTime = System.currentTimeMillis()
        previousPercent = 0
        try {
            if (updateFileType == UpdateFileType.Firmware) isUpdatingFlag = true
            else isUploadingCustomImageFlag = true

            val bytes = file.readBytes()
            val totalSize = bytes.size
            currentCrc32Value = calculateCrc(bytes)

            Log.d(TAG, "Starting $updateFileType update for $side. Size: $totalSize, CRC: ${currentCrc32Value.toString(16)}")

            //Start Update
            sendStartUpdateCommand(updateFileType, side)
            waitForAck(side, START_UPDATE_SUB_OP)

            if (showFirmwareProgressOnLcd) {
                setLcdScreenIndex(side, ScreenIndex.Custom)
                setLcdScreenIndex(ScreenSide.Right, ScreenIndex.Resistance)
                setLcdValue(ScreenSide.Right, 0.0f, 0, true)
            }

            //Send data chunks until they are all sent
            var offset = 0
            while (offset < totalSize) {
                val length = minOf(MAX_UPDATE_CHUNK_SIZE, totalSize - offset)
                val chunk = bytes.copyOfRange(offset, offset + length)

                sendChunkWithRetry(updateFileType, side, offset, chunk)

                offset += length
                showFirmwareUpdateProgress(offset, totalSize)
            }

            //Finish update
            finishFirmwareUpdate(updateFileType, side)
            waitForAck(side, COMMIT_UPDATE_SUB_OP)

            val elapsedTimeMs = System.currentTimeMillis() - startTime
            val minutes = (elapsedTimeMs / 1000) / 60
            val seconds = (elapsedTimeMs / 1000) % 60
            Log.i(TAG, "$updateFileType update for $side completed successfully in ${minutes}m ${seconds}s")

            broadcastUpdateStatus(ACTION_UPDATE_COMPLETE, EXTRA_SUCCESS, true)

            //If the flag is set, show the custom image immediately
            if (showCustomImageAfterUpload && updateFileType == UpdateFileType.CustomImage) {
                if (canConfig == CanConfigEnum.SingleCanNode) {
                    setLcdScreenIndex(ScreenSide.Left, ScreenIndex.Custom)
                    setLcdScreenIndex(ScreenSide.Right, ScreenIndex.Custom)
                } else {
                    setLcdScreenIndex(side, ScreenIndex.Custom)
                }
            }

        } catch (e: Exception) {
            if (e is CancellationException) {
                Log.i(TAG, "$updateFileType update for $side was cancelled")
            } else {
                Log.e(TAG, "$updateFileType update for $side failed: ${e.message}")
            }
            sendAbortUpdateCommand(updateFileType, side)

            broadcastUpdateStatus(ACTION_UPDATE_COMPLETE, EXTRA_SUCCESS, false)
        } finally {
            isUpdatingFlag = false
            isUploadingCustomImageFlag = false
            // Clear channel for next time
            while (ackChannel.tryReceive().isSuccess) {
                // Do nothing
            }
        }
    }

    private suspend fun sendChunkWithRetry(
        type: UpdateFileType,
        side: ScreenSide,
        offset: Int,
        chunk: ByteArray,
        maxRetries: Int = 5
    ) {
        var retries = 0
        while (true) {
            val commandByte = getUpdateCommandByte(type, side)
            val subOpByte = SEND_UPDATE_CHUNK_SUB_OP
            val offsetBytes = if (useLittleEndian) offset.toLittleEndianByteArray(4) else offset.toBigEndianByteArray(4)
            val dataLengthBytes = if (useLittleEndian) chunk.size.toLittleEndianByteArray(4) else chunk.size.toBigEndianByteArray(4)
            val payload = byteArrayOf(commandByte, subOpByte) + offsetBytes + dataLengthBytes + chunk

            sendCanMessage(payload)

            try {
                waitForAck(side, SEND_UPDATE_CHUNK_SUB_OP)
                return
            } catch (_: Exception) {
                retries++
                if (retries >= maxRetries) throw Exception("Failed to send chunk at offset $offset after $maxRetries retries")
                Log.w(TAG, "Retry chunk at offset $offset (retry $retries)")
                delay(100.milliseconds) // Small delay before retry
            }
        }
    }

    private suspend fun waitForAck(side: ScreenSide, expectedSubOp: Byte, timeout: Long = 10000): AckResult {
        return withTimeout(timeout.milliseconds) {
            for (ack in ackChannel) {
                if (ack.side == side && ack.subOpCode == expectedSubOp) {
                    if (!ack.isSuccess) throw Exception("Device returned error for subOp $expectedSubOp")
                    return@withTimeout ack
                }
            }
            throw Exception("Never received ACK for subOp $expectedSubOp")
        }
    }

    private fun sendStartUpdateCommand(updateFileType: UpdateFileType, side: ScreenSide) {
        val payload: ByteArray = byteArrayOf(
            getUpdateCommandByte(updateFileType, side),
            START_UPDATE_SUB_OP,
            if(useLittleEndian) side.ordinal.toByte()
            else if(canConfig == CanConfigEnum.DoubleCanNodes) ScreenSide.Right.ordinal.toByte()
            else side.ordinal.toByte()
        )
        sendCanMessage(payload)
    }

    private fun finishFirmwareUpdate(updateFileType: UpdateFileType, side: ScreenSide) {
        val payload: ByteArray = byteArrayOf(
            getUpdateCommandByte(updateFileType, side),
            COMMIT_UPDATE_SUB_OP
        )
        val crcBytes: ByteArray = if (useLittleEndian) currentCrc32Value.toLittleEndianByteArray(4) else currentCrc32Value.toBigEndianByteArray(4)
        sendCanMessage(payload + crcBytes)
    }

    private fun sendAbortUpdateCommand(updateFileType: UpdateFileType, side: ScreenSide) {
        val payload: ByteArray = byteArrayOf(
            getUpdateCommandByte(updateFileType, side),
            ABORT_UPDATE_SUB_OP
        )
        sendCanMessage(payload)
    }

    private fun abortFirmwareUpdate(updateFileType: UpdateFileType) {
        updateJob?.cancel()
        updateJob = null
        scope.launch {
            sendAbortUpdateCommand(updateFileType, ScreenSide.Right)
            if (isDoubleCanNodes()) {
                sendAbortUpdateCommand(updateFileType, ScreenSide.Left)
            }
        }
    }

    /**
     * Sends the CRC32 of the image to a grip to ask if it matches the CRC32 of its custom image
     */
    private fun checkCustomImageCrc32(screenSide: ScreenSide, path:String?, index:Int) {

        if (path.isNullOrEmpty()) {
            Log.e(TAG, "Path string is null")
            return
        }

        val imageFile = File(path)
        if (imageFile.exists()) {
            val bytes = imageFile.readBytes()
            currentCrc32Value = calculateCrc(bytes)
            Log.d(TAG, "Image CRC: ${currentCrc32Value.toString(16)}")
        } else {
            Log.e(TAG, "FILE NOT FOUND!")
        }

        val payload = byteArrayOf(
            GET_LCD_CUSTOM_IMG_CRC,
            screenSide.ordinal.toByte(),
            index.toByte()
        )
        if (isDoubleCanNodes() && screenSide == ScreenSide.Left) {
            payload[0] = GET_LEFT_LCD_CUSTOM_IMG_CRC
        }
        sendCanMessage(payload)
    }

    /**
     * Called when the console receives a CAN response to a request for the CRC32 value of the
     * custom image loaded on the grip. This function currently only checks the payload and
     * logs if it matches the one that the console thinks is correct. This functionality will
     * need to be expanded when custom image logic is integrated into console settings
     */
    private fun parseCustomImageCrc(payload: ByteArray) {
        if (payload.size < 5){
            Log.e(TAG, "Payload is too short")
            return
        }

        val buffer = ByteBuffer.wrap(payload, 1, 4)
        buffer.order(if (useLittleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)

        val receivedCrcValue:Int = buffer.int
        Log.d(TAG, "Received CRC Value: ${receivedCrcValue.toString(16)}")

        if(currentCrc32Value == receivedCrcValue) {
            Log.d(TAG, "CRC Values Match! ${receivedCrcValue.toString(16)}")
        } else {
            Log.d(TAG, "CRC Values do not match ${currentCrc32Value.toString(16)} != ${receivedCrcValue.toString(16)}")
        }
    }

    private fun getUpdateCommandByte(updateFileType: UpdateFileType, side: ScreenSide): Byte {
        val returnVal = when(updateFileType) {
            UpdateFileType.Firmware ->
                if (canConfig == CanConfigEnum.DoubleCanNodes) {
                    when (side) {
                        ScreenSide.Right -> UPDATE_FIRMWARE_COMMAND
                        ScreenSide.Left -> UPDATE_FIRMWARE_LEFT_COMMAND
                    }
                } else {
                    UPDATE_FIRMWARE_COMMAND
                }
            UpdateFileType.CustomImage ->
                if (canConfig == CanConfigEnum.DoubleCanNodes) {
                    when (side) {
                        ScreenSide.Right -> UPDATE_LCD_CUSTOM_IMAGE_COMMAND
                        ScreenSide.Left -> UPDATE_LCD_CUSTOM_IMAGE_LEFT_COMMAND
                    }
                } else {
                    UPDATE_LCD_CUSTOM_IMAGE_COMMAND
                }
        }
        return returnVal
    }




    private fun showFirmwareUpdateProgress(offset: Int, total: Int) {
        val percentComplete = min(100f, (100 * offset).toFloat() / total.toFloat())
        val currentPercent = percentComplete.toInt()

        if (previousPercent < currentPercent) {
            previousPercent = currentPercent
            Log.d(TAG, "FIRMWARE UPDATE: $currentPercent%")

            broadcastUpdateStatus(ACTION_UPDATE_PROGRESS, EXTRA_PROGRESS, currentPercent)

            if (showFirmwareProgressOnLcd) {
                setLcdValue(ScreenSide.Right, percentComplete, 0, true)
            }
        }
    }

    private var previousPercent:Int = 0

    fun calculateCrc(input: ByteArray): Int {
        val crc32 = CRC32()
        crc32.update(input)
        val checksum: Long = crc32.value
        Log.d(TAG, "Long to Int CRC HEX: ${checksum.toInt().toString(16)}")
        return checksum.toInt()
    }

    /**
     * Takes the firmware version and module type from the get firmware version request made to the modules,
     * and stores it in the Android Settings
     */
    private fun updateVersionInSettings(versionString: String, hardwareConfig: CanConfigEnum, side: ScreenSide) {
        val moduleString: String = when(hardwareConfig){
            CanConfigEnum.SingleCanNode -> {"single"}
            CanConfigEnum.DoubleCanNodes -> {
                when(side){
                    ScreenSide.Right -> {"right"}
                    ScreenSide.Left -> {"left"}
                }
            }
            CanConfigEnum.UnknownHardware -> {
                Log.e(TAG, "Unknown CAN Type. Unable to update version in Android settings")
                return
            }
        }

        val versionKey = "can_" + moduleString + "_version"

        Log.d(TAG, "Updating $versionKey in settings to $versionString")

        val context = ContextUtil.getApplicationContext()
        if(context != null) {
           // Store the version in Android Settings
           AndroidSetting.putString(
               context.contentResolver,
               versionKey,
               versionString
           )
        }
    }

    private fun broadcastUpdateStatus(action: String, extraKey: String? = null, extraValue: Any? = null) {
        scope.launch {
            val intent = Intent(action)
            extraKey?.let { key ->
                when (extraValue) {
                    is Boolean -> intent.putExtra(key, extraValue)
                    is Int -> intent.putExtra(key, extraValue)
                }
            }
            ContextUtil.getApplicationContext()?.sendBroadcast(intent)
        }
    }

}
