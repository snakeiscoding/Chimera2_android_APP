package com.jht.chimera.service.ui.panels.compose

import android.util.Log
import android.view.MotionEvent
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.jht.androidcommonalgorithms.type.JHTCommonTypes
import com.jht.androidcommonalgorithms.type.JHTCommonTypes.MachineClassificationType
import com.jht.chimera.R
import com.jht.chimera.lib.common.StatIncrementer
import com.jht.chimera.lib.common.StatIncrementer.IStatIncremented
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.config.frames.FrameConfig
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.common.formatting.BasicNumberFormat
import com.jht.chimera.lib.common.keypress.KeyPress
import com.jht.chimera.lib.common.keypress.KeyPress.KeyCode
import com.jht.chimera.lib.language.StringLookup
import com.jht.chimera.lib.widget.statarray.StatValueUpdater.IStatValueUpdate
import com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator
import com.jht.chimera.lib.workout.WorkoutIds
import com.jht.data.FrameControl
import com.jht.data.FrameControlCollection
import com.jht.data.FrameControlLocation
import com.jht.data.RangeInfo
import com.jht.datamatrixlib.DataMatrix
import com.jht.interfaces.CallbackManager
import com.jht.interfaces.DataValueListener
import java.util.Timer
import java.util.TimerTask
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

private const val TAG = "ControlGroupModel.kt"

/*
    Need to rough in something so UI can be tested.
    Old logic should probably be reviewed for potential consolidation and clean up?
 */

/** Common interface model implementation class for frame control
 * It is assumed to be used on the UI thread as part of view model setup.
 * The model consuming this is expected to handle [manager] lifecycle.
 * */
class FrameControlsValueListener(
    private val manager: CallbackManager,
) : DataValueListener<FrameControlCollection> {
    private var frameConfig: FrameConfig = FrameConfig()
    private var workoutId: Int = 0
    private var workoutStage: Int = 0
    private var controls: FrameControlCollection = FrameControlCollection()
    private var initialized = false
    private val callbacks = mutableSetOf<(FrameControlCollection) -> Unit>()
    init {
        initializeIfNeeded()
        // only need to attach this once per construction
        manager.cancelAllEventCallbacks.add {
            initialized = false
        }
    }
    private fun initializeIfNeeded() {
        if (!initialized) {
            callbacks.clear()
            // initializing before listeners should be fine since DM callbacks queue on UI handler anyway, assuming this class is also on UI thread
            frameConfig = FrameConfigManager.getInstance().frameConfig
            workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT)
            workoutStage = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT)
            controls = createFrameControlCollection(frameConfig, workoutId, workoutStage)
            manager.addStringListener(SettingKeys.General.DATA_FRAME_TYPE_STRING, {
                val currentFrameConfig = FrameConfigManager.getInstance().frameConfig
                updateControls(frameConfig = currentFrameConfig)
            }, false)
            manager.addIntListener(WorkoutKeys.DATA_WORKOUT_ID_INT, {
                updateControls(workoutId = it)
            }, false)
            manager.addIntListener(WorkoutKeys.DATA_WORKOUT_STAGE_INT, {
                updateControls(workoutStage = it)
            }, false)
            initialized = true
        }
    }
    private fun updateControls(
        frameConfig: FrameConfig = this.frameConfig,
        workoutId: Int = this.workoutId,
        workoutStage: Int = this.workoutStage,
    ) {
        var needUpdate = false
        if (frameConfig != this.frameConfig) {
            this.frameConfig = frameConfig
            needUpdate = true
        }
        if (workoutId != this.workoutId) {
            this.workoutId = workoutId
            needUpdate = true
        }
        if (workoutStage != this.workoutStage) {
            this.workoutStage = workoutStage
            needUpdate = true
        }
        if (needUpdate) {
            controls = createFrameControlCollection(frameConfig, workoutId, workoutStage)
            callbacks.forEach { it(controls) }
        }
    }
    override val value: FrameControlCollection get() = controls
    override fun listenAndTrigger(onChange: (FrameControlCollection) -> Unit) {
        initializeIfNeeded()
        callbacks.add(onChange)
        onChange(value)
    }
    override fun listen(onChange: (FrameControlCollection) -> Unit) {
        initializeIfNeeded()
        callbacks.add(onChange)
    }
}

fun createFrameControlCollection(
    frameConfig: FrameConfig,
    workoutId: Int,
    workoutStage: Int,
): FrameControlCollection {
    val controlList: List<FrameControl> = listOfNotNull(
        createFrameControlLeft(frameConfig, workoutId),
        createFrameControlRight(frameConfig, workoutId, workoutStage),
    )
    return FrameControlCollection(controlList)
}
fun createFrameControlLeft(
    frameConfig: FrameConfig,
    workoutId: Int,
): ControlGroupModel? {
    val location = FrameControlLocation.Left
    return if (WorkoutIds.isTargetHeartRate(workoutId)) {
        HearRateControl(frameConfig, location)
    } else if (workoutId == WorkoutIds.CONSTANT_WATTS) {
        TargetWattsControl(frameConfig, location)
    } else if (frameConfig.hasIncline()) {
        InclineControl(frameConfig, location)
    } else {
        null
    }
}
fun createFrameControlRight(
    frameConfig: FrameConfig,
    workoutId: Int,
    workoutStage: Int,
): ControlGroupModel? {
    val location = FrameControlLocation.Right
    return if (workoutId == WorkoutIds.CONSTANT_WATTS
        && workoutStage != WorkoutKeys.Constants.WORKOUT_STAGE_WARMUP && workoutStage != WorkoutKeys.Constants.WORKOUT_STAGE_COOLDOWN
    ) {
        null
    } else if (workoutId == WorkoutIds.COOPER_FITNESS_TEST) {
        TargetWattsControl(frameConfig, location)
    } else {
        when (frameConfig.machineClassType) {
            MachineClassificationType.Treadmill -> {
                SpeedControl(frameConfig, location)
            }
            else -> {
                ResistanceControl(frameConfig, location)
            }
        }
    }
}


private fun createPresetValuesSpeed(frameConfig: FrameConfig, isMetric: Boolean): List<Float> {
    return if (isMetric) listOf(4f, 6f, 8f, 10f, 12f, 14f)
    else listOf(3f, 4f, 5f, 6f, 7f, 8f)
}
private fun createPresetValuesResistance(frameConfig: FrameConfig): List<Float> {
    return when (frameConfig.machineClassType) {
        MachineClassificationType.Ascent, MachineClassificationType.Elliptical -> listOf(1f, 4f, 8f, 12f, 16f, 20f)
        MachineClassificationType.Bike -> listOf(1f, 5f, 10f, 15f, 20f, 25f)
        else -> listOf(1f, 4f, 8f, 12f, 16f, 20f) // CXV Resistance? seems same as ascent
    }
}
private fun createPresetValuesIncline(frameConfig: FrameConfig): List<Float> {
    return when (frameConfig.machineClassType) {
        MachineClassificationType.Ascent -> listOf(0f, 20f, 40f, 60f, 80f, 100f)
        else -> listOf(0f, 2f, 4f, 6f, 8f, 10f) // Treadmill
    }
}

private fun createPresetValues(minValue: Float, maxValue: Float, stepValue: Float = 0f): List<Float> {
    val presetCount = 6 // do we make this a parameter?
    val presetStep = (maxValue - minValue) / (presetCount - 1)
    val list = mutableListOf<Float>()
    for (i in 0..(presetCount - 2)) {
        var preset = minValue + (i * presetStep)
        if (stepValue != 0f) preset = round(preset / stepValue) * stepValue
        list.add(preset)
    }
    list.add(maxValue) // manually add last as max to ensure last is exactly max and not over shoot due to math precision
    return list.toList()
}

/**
 * Class to contain values and callbacks for one ControlGroup
 */
open class ControlGroupModel(
    override val location: FrameControlLocation = FrameControlLocation.Left,
    override val label: MutableState<String> = mutableStateOf("RESISTANCE"),
    override val currentValue: MutableState<String> = mutableStateOf("0.0"),
    override val nextValueLabel: MutableState<String> = mutableStateOf(""),
    override val nextValue: MutableState<String> = mutableStateOf(""),
    override val unit: MutableState<String> = mutableStateOf("MPH"),
    override val previousVisible: MutableState<Boolean> = mutableStateOf(true),
    override val previousValue: MutableState<String> = mutableStateOf("--"),
    override val previousLabel: MutableState<String?> = mutableStateOf(null),
    override val range: MutableState<RangeInfo> = mutableStateOf(RangeInfo()),
    override val keypadUnit: MutableState<String> = mutableStateOf("MPH"),
    override val presetValues: MutableState<List<Float>> = mutableStateOf(listOf(1f, 2f, 3f, 4f, 5f, 6f)), // for quick keys
    override val iconId: MutableIntState = mutableIntStateOf(0),
) : FrameControl {
    val isLeft get() = (location == FrameControlLocation.Left)
    open fun tag(text: String) = "$location$text" // helper for building testTag
    override fun start() {}
    override fun stop() {}
    override fun getValueFloat(): Float = 0f
    override fun getUserChangesAllowed(): Boolean = true

    override var onUserInteraction = {} // callback for any user interactions

    override fun onPreviousValueClicked() { onUserInteraction() }
    override fun onKeypadCommit(value: Float) { onUserInteraction() }
    override fun onIncrementPress(up:Boolean) { onUserInteraction() }
    override fun onDecrementPress(up:Boolean) { onUserInteraction() }
    override fun stopIncrementing() {}

    // "OK" flashing
    private var mFlashTimer: Timer? = null
    private var mCounter = 8
    private var mLastValue: String? = null
    private fun showLastValue() { previousValue.value = mLastValue ?: "" }
    private fun showOk() { previousValue.value = StringLookup.get(R.string.ok_uc) }
    protected fun isFlashingOk(): Boolean { return mFlashTimer != null }
    protected fun startFlashingOk() {
        mLastValue = previousValue.value
        showOk()
        mCounter = 8
        val mFlashTask: TimerTask = object : TimerTask() {
            override fun run() {
                mCounter--
                if (mCounter == 0) stopFlashingOk()
                else if (mCounter % 2 == 0) showOk() else showLastValue()
            }
        }
        mFlashTimer?.cancel()
        mFlashTimer = Timer()
        mFlashTimer?.schedule(mFlashTask, 1000, 1000)
    }
    protected fun stopFlashingOk() {
        mFlashTimer?.cancel()
        showLastValue()
        mFlashTimer = null
    }

    // we can probably change StatIncrementer to take a pressed boolean and map MotionEvent on the old stuff?
    protected fun pressToEvent(up : Boolean): Int = if (up) MotionEvent.ACTION_UP else MotionEvent.ACTION_DOWN

    // last value save timer
    protected var mReadyToSave = true
    private var mSaveLastTimer: Timer? = null
    protected fun startSaveLastTimer() {
        mReadyToSave = false
        stopSaveTimer()
        mSaveLastTimer = Timer()
        mSaveLastTimer?.schedule(object : TimerTask() {
            override fun run() { mReadyToSave = true }
        }, 5000)
    }
    protected fun stopSaveTimer() {
        mSaveLastTimer?.cancel()
        mSaveLastTimer?.purge()
        mSaveLastTimer = null
    }
}

fun isWorkoutInactive(): Boolean {
    return DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_RUNNING
}


/**
 * INCLINE CONTROL
 */
class InclineControl(val frameConfig: FrameConfig, location: FrameControlLocation) : ControlGroupModel(location) {
    private var incrementer: StatIncrementer
    private var decrementer: StatIncrementer
    private var mLastValue: Float? = null
    private var stopped = false // set to true in stop to prevent touch events on this page (OC-214)

    private val readKey : String = HALKeys.DATA_TARGET_INCLINE_FLOAT.toString()
    private val writeKey : String = WorkoutKeys.ACTION_SET_INCLINE_WORKOUT_FLOAT.toString()
    private val mDataMatrix = DataMatrix()
    private val TOLERANCE = 0.0005f

    override fun getValueFloat(): Float = DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT)
    override fun onIncrementPress(up:Boolean) {
        super.onIncrementPress(up)
        if (isWorkoutInactive() || stopped) return
        incrementer.onTouchEvent(pressToEvent(up))
    }
    override fun onDecrementPress(up:Boolean) {
        super.onDecrementPress(up)
        if (isWorkoutInactive() || stopped) return
        decrementer.onTouchEvent(pressToEvent(up))
    }
    override fun onPreviousValueClicked() {
        super.onPreviousValueClicked()
        if (mLastValue != null) {
            startSaveLastTimer()
            val currentLastInclineVal: Float = mLastValue as Float
            val incline = DataMatrix.getFloat(readKey)
            saveLastValue(incline)
            DataMatrix.putFloat(writeKey, currentLastInclineVal)
        }
    }
    override fun onKeypadCommit(value: Float) {
        super.onKeypadCommit(value)
        val incline = DataMatrix.getFloat(readKey)
        if (abs(value - incline) > TOLERANCE) {
            saveLastValue(incline)
            startSaveLastTimer()
            DataMatrix.putFloat(writeKey, value)
        }
    }


    init {
        incrementer = StatIncrementer(readKey, writeKey, true, frameConfig.incline, StatIncrementer.TYPE_FLOAT) {
            if (mReadyToSave) {
                val incline = DataMatrix.getFloat(readKey)
                saveLastValue(incline)
            }
            decrementer.stop()
            startSaveLastTimer()
        }
        incrementer.setMaxChanges(40)
        decrementer = StatIncrementer(readKey, writeKey, false, frameConfig.incline, StatIncrementer.TYPE_FLOAT) {
            if (mReadyToSave) {
                val incline = DataMatrix.getFloat(readKey)
                saveLastValue(incline)
            }
            incrementer.stop()
            startSaveLastTimer()
        }
        decrementer.setMaxChanges(40)
    }

    private fun saveLastValue(valueToSave: Float) {
        mLastValue = valueToSave
        when (frameConfig.machineClassType) {
            MachineClassificationType.Treadmill -> previousValue.value = BasicNumberFormat.format(valueToSave, 1)
            MachineClassificationType.Ascent -> previousValue.value = BasicNumberFormat.format(valueToSave, 0)
            else -> throw RuntimeException("Unexpected machine type error: " + frameConfig.machineClassType.name)
        }
    }

    override fun stopIncrementing() {
        incrementer.stop()
        decrementer.stop()
    }

    private val valueUpdate: IStatValueUpdate = object : IStatValueUpdate {
        override fun statValueChanged(value: String) {
            currentValue.value = value
        }
        override fun statTitleChanged(title: String) {
            label.value = StringLookup.get(R.string.incline_tc) //title
        }
        override fun statNextValueChanged(value: String) {
            nextValue.value = value
            nextValueLabel.value = StringLookup.get(R.string.next_incline_tc)
        }
    }

    override fun start() {
        setupStatUpdates()
        setupDataMatrixCallbacks()
        setupKeypad()
        stopped = false
    }

    override fun stop() {
        mDataMatrix.finish()
        stopped = true
        stopIncrementing()
        val stats = StatValueUpdaterCreator.getInstance()
        val inclineUpdater = stats.getValue(StatValueUpdaterCreator.INCLINE)
        inclineUpdater.removeCallback(valueUpdate)
    }

    private fun setupStatUpdates() {
        val stats = StatValueUpdaterCreator.getInstance()
        val inclineStatUpdater = stats.getValue(StatValueUpdaterCreator.INCLINE)
        currentValue.value = inclineStatUpdater.defaultValue
        unit.value = StringLookup.get(R.string.percent_lc)
        label.value = StringLookup.get(R.string.incline_tc) //inclineStatUpdater.defaultTitle
        inclineStatUpdater.addCallbackInterface(valueUpdate)
    }

    private fun setupDataMatrixCallbacks() {
        mDataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT,
            { value: Int? ->
                val keyCode = KeyPress.getKeyCode(value!!)
                val keyState = KeyPress.getKeyState(value)
                if (keyCode == KeyCode.KEY_INCLINE_DOWN) {
                    onDecrementPress(keyState == KeyPress.KeyState.KEY_UP)
                }
                if (keyCode == KeyCode.KEY_INCLINE_UP) {
                    onIncrementPress(keyState == KeyPress.KeyState.KEY_UP)
                }
            }, false
        )

        // When the state changes stop changing values
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT,
            { stopIncrementing() }, false
        )
        mDataMatrix.createEventAdapter(UiServiceKeys.EVENT_STOP_SIDE_BAR_INCREMENT,
            { stopIncrementing() }, false
        )
    }

    private fun setupKeypad() {
        range.value = frameConfig.incline
        keypadUnit.value = StringLookup.get(R.string.percent_lc)
        label.value = StringLookup.get(R.string.incline_tc)
        unit.value = StringLookup.get(R.string.percent_lc)
        //presetValues.value = createPresetValues(range.value.min, range.value.max, range.value.stepSize)
        presetValues.value = createPresetValuesIncline(frameConfig)
        iconId.intValue = R.drawable.ic_incline_white
    }
}


/**
 * SPEED CONTROL
 */
class SpeedControl(val frameConfig: FrameConfig, location: FrameControlLocation) : ControlGroupModel(location) {
    private var mCurrentSpeed = 0f
    private var mCurrentKPH = 0f
    private var mIsMetric = true
    private var mBlockPhysicalButtons = false

    private var mSpeedKphIncrement: StatIncrementer
    private var mSpeedMphIncrement: StatIncrementer
    private var mSpeedKphDecrement: StatIncrementer
    private var mSpeedMphDecrement: StatIncrementer
    private var mLastValue: Float? = null
    private var stopped = false // set to true in stop to prevent touch events on this page (OC-214)

    private val mDataMatrix = DataMatrix()

    override fun getValueFloat(): Float {
        val metric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)
        return if (metric) DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT) else SpeedConvertUtil.convertKphToMph(DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT))
    }
    override fun onIncrementPress(up:Boolean) {
        super.onIncrementPress(up)
        if (isWorkoutInactive() || stopped) return
        if (mIsMetric) {
            mSpeedKphIncrement.onTouchEvent(pressToEvent(up))
        } else {
            mSpeedMphIncrement.onTouchEvent(pressToEvent(up))
        }
    }
    override fun onDecrementPress(up:Boolean) {
        super.onDecrementPress(up)
        if (isWorkoutInactive() || stopped) return
        if (mIsMetric) {
            mSpeedKphDecrement.onTouchEvent(pressToEvent(up))
        } else {
            mSpeedMphDecrement.onTouchEvent(pressToEvent(up))
        }
    }
    override fun onPreviousValueClicked() {
        super.onPreviousValueClicked()
        if (mLastValue != null) {
            val currentLastValue: Float = mLastValue as Float
            val delta = if (mIsMetric) 1.65f else 1.05f
            if (isFlashingOk()) {
                stopFlashingOk()
                startSaveLastTimer()
                saveCurrentSpeed()
                DataMatrix.putFloat(
                    WorkoutKeys.ACTION_SET_SPEED_KPH_WORKOUT_FLOAT,
                    if (mIsMetric) currentLastValue else SpeedConvertUtil.convertMphToKph(
                        currentLastValue
                    )
                )
            } else if (currentLastValue > mCurrentSpeed + delta) {
                startFlashingOk()
            } else {
                startSaveLastTimer()
                saveCurrentSpeed()
                DataMatrix.putFloat(
                    WorkoutKeys.ACTION_SET_SPEED_KPH_WORKOUT_FLOAT,
                    if (mIsMetric) currentLastValue else SpeedConvertUtil.convertMphToKph(
                        currentLastValue
                    )
                )
            }
        }
    }

    override fun onKeypadCommit(value: Float) {
        super.onKeypadCommit(value)
        Log.e(TAG, "in float adapted, value: $value")
        val currentSpeed =
            if (mIsMetric) DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT) else SpeedConvertUtil.convertKphToMph(
                DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT)
            )
        val targetKPH = if (mIsMetric) value else SpeedConvertUtil.convertMphToKph(value)
        // If the value changed
        if (abs(value - currentSpeed) > 0.005) {
            saveLastValue(currentSpeed)
            Log.e(TAG, "in float adapted, value: $value, currentSpeed: $currentSpeed")
            DataMatrix.putFloat(WorkoutKeys.ACTION_SET_SPEED_KPH_WORKOUT_FLOAT, targetKPH)
        }
    }


    init {
        val statCallbackUp = IStatIncremented {
            if (mReadyToSave) saveCurrentSpeed()
            mSpeedKphDecrement.stop()
            mSpeedMphDecrement.stop()
            startSaveLastTimer()
        }

        val statCallbackDown = IStatIncremented {
            if (mReadyToSave) saveCurrentSpeed()
            mSpeedKphIncrement.stop()
            mSpeedMphIncrement.stop()
            startSaveLastTimer()
        }

        mSpeedKphIncrement = StatIncrementer(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT.toString(), WorkoutKeys.ACTION_SET_SPEED_KPH_WORKOUT_FLOAT.toString(), true, frameConfig.speedKph, StatIncrementer.TYPE_FLOAT, statCallbackUp)
        mSpeedKphIncrement.setMaxChanges(16)
        mSpeedKphDecrement = StatIncrementer(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT.toString(), WorkoutKeys.ACTION_SET_SPEED_KPH_WORKOUT_FLOAT.toString(), false, frameConfig.speedKph, StatIncrementer.TYPE_FLOAT, statCallbackDown)
        mSpeedKphDecrement.setMaxChanges(320) // Will stop decrementing if stuck after about 32kph

        mSpeedMphIncrement = StatIncrementer(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT.toString(), WorkoutKeys.ACTION_SET_SPEED_KPH_WORKOUT_FLOAT.toString(), true, frameConfig.speedMph, StatIncrementer.TYPE_FLOAT, statCallbackUp)
        mSpeedMphIncrement.setConversionFactor(SpeedConvertUtil.MILES_PER_HOUR_FACTOR)
        mSpeedMphIncrement.setMaxChanges(10)

        mSpeedMphDecrement = StatIncrementer(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT.toString(), WorkoutKeys.ACTION_SET_SPEED_KPH_WORKOUT_FLOAT.toString(), false, frameConfig.speedMph, StatIncrementer.TYPE_FLOAT, statCallbackDown)
        mSpeedMphDecrement.setConversionFactor(SpeedConvertUtil.MILES_PER_HOUR_FACTOR)
        mSpeedMphDecrement.setMaxChanges(200) // Will stop decrementing if stuck after about 20mph
    }

    private fun saveCurrentSpeed() {
        saveLastValue(mCurrentSpeed)
    }
    private fun saveLastValue(valueToSave: Float) {
        if (valueToSave == 0f) return // don't allow 0 speed
        mLastValue = valueToSave
        previousValue.value = BasicNumberFormat.format(valueToSave, 1)
    }

    override fun stopIncrementing() {
        mSpeedKphIncrement.stop()
        mSpeedKphDecrement.stop()
        mSpeedMphIncrement.stop()
        mSpeedMphDecrement.stop()
    }

    private val valueUpdate: IStatValueUpdate = object : IStatValueUpdate {
        override fun statValueChanged(value: String) {
            currentValue.value = value
        }
        override fun statTitleChanged(title: String) {
            label.value = StringLookup.get(R.string.speed_tc) //title
        }
        override fun statNextValueChanged(value: String) {
            nextValue.value = value
            nextValueLabel.value = StringLookup.get(R.string.next_speed_tc)
        }
    }

    override fun start() {
        setupStatUpdates()
        setupDataMatrixCallbacks()
        stopped = false
    }

    override fun stop() {
        mDataMatrix.finish()
        stopped = true
        stopIncrementing()
        val stats = StatValueUpdaterCreator.getInstance()
        val speedUpdater = stats.getValue(StatValueUpdaterCreator.TARGET_SPEED)
        speedUpdater.removeCallback(valueUpdate)
    }

    private fun setupStatUpdates() {
        val stats = StatValueUpdaterCreator.getInstance()
        val speedUpdater = stats.getValue(StatValueUpdaterCreator.TARGET_SPEED)
        currentValue.value =  speedUpdater.defaultValue
        unit.value = speedUpdater.defaultTitle
        label.value = StringLookup.get(R.string.speed_tc)
        speedUpdater.addCallbackInterface(valueUpdate)
    }

    private fun setupDataMatrixCallbacks() {
        mDataMatrix.createFloatAdapter(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT,
            { value: Float ->
                mCurrentKPH = value
                mCurrentSpeed = if (mIsMetric) value else SpeedConvertUtil.convertKphToMph(value)
            }, true
        )
        mDataMatrix.createBoolAdapter(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL,
            { isMetric: Boolean ->
                // only perform the unit conversion if the metric state has changed.
                // Previously, this block would execute even if the initial state perfectly matched the adapter's incoming value.
                if (mIsMetric != isMetric) {
                    mIsMetric = isMetric
                    mCurrentSpeed =
                        if (mIsMetric) SpeedConvertUtil.convertMphToKph(mCurrentSpeed)
                        else SpeedConvertUtil.convertKphToMph(mCurrentSpeed)
                        
                    if (mLastValue != null) {
                        saveLastValue(
                            if (mIsMetric) SpeedConvertUtil.convertMphToKph(mLastValue!!) 
                            else SpeedConvertUtil.convertKphToMph(mLastValue!!)
                        )
                    }
                }
                //setupKeypad must always run, even on initialization (when mIsMetric == isMetric),
                setupKeypad(isMetric)
            }, true
        )
        mDataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT,
            { value: Int? ->
                // If this is a custom HIIT workout do not handle key presses, the side panel is not shown
                if (!mBlockPhysicalButtons) {
                    val keyCode = KeyPress.getKeyCode(value!!)
                    val keyState = KeyPress.getKeyState(value)
                    if (keyCode == KeyCode.KEY_SPEED_DOWN) {
                        onDecrementPress(keyState == KeyPress.KeyState.KEY_UP)
                    }
                    if (keyCode == KeyCode.KEY_SPEED_UP) {
                        onIncrementPress(keyState == KeyPress.KeyState.KEY_UP)
                    }
                }
            }, false
        )
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, { value: Int ->
            if (value == WorkoutKeys.Constants.RUN_STATE_PAUSE) {
                if (mCurrentKPH <= 0.9 - 0.005) { // If we are at or very close to min speed
                    mLastValue = null
                    previousValue.value = "--"
                } else {
                    saveLastValue(mCurrentSpeed)
                }
            }
            stopIncrementing()
        }, false)
        mDataMatrix.createEventAdapter(UiServiceKeys.EVENT_STOP_SIDE_BAR_INCREMENT,
            { s: String? -> stopIncrementing() }, false
        )
    }

    private fun setupKeypad(metric: Boolean) {
        label.value = StringLookup.get(R.string.speed_tc)
        if (metric) {
            range.value = frameConfig.speedKph
            keypadUnit.value = StringLookup.get(R.string.kph_lc)
            unit.value = StringLookup.get(R.string.kph_uc)
        } else {
            range.value = frameConfig.speedMph
            keypadUnit.value = StringLookup.get(R.string.mph_lc)
            unit.value = StringLookup.get(R.string.mph_uc)
        }
        //presetValues.value = createPresetValues(range.value.min, range.value.max, range.value.stepSize)
        presetValues.value = createPresetValuesSpeed(frameConfig, metric)
        iconId.intValue = R.drawable.ic_speed_white
    }
}


/**
 * RESISTANCE CONTROL (Climb mill and stepper are clumped in here too?)
 */
class ResistanceControl(val frameConfig: FrameConfig, location: FrameControlLocation) : ControlGroupModel(location) {
    private var incrementer: StatIncrementer
    private var decrementer: StatIncrementer
    private var mLastValue: Int? = null
    private var stopped = false // set to true in stop to prevent touch events on this page (OC-214)

    private val readKey : String = HALKeys.DATA_RESISTANCE_INT.toString()
    private val writeKey : String = WorkoutKeys.ACTION_SET_RESISTANCE_WORKOUT_INT.toString()
    private val mDataMatrix = DataMatrix()

    override fun getUserChangesAllowed(): Boolean {
        return FrameConfigManager.getInstance().frameConfig.machineType != JHTCommonTypes.MachineType.ManualResistanceSpinBike;
    }

    override fun getValueFloat(): Float = DataMatrix.getInt(readKey).toFloat()
    override fun onIncrementPress(up:Boolean) {
        super.onIncrementPress(up)
        if (isWorkoutInactive() || stopped) return
        incrementer.onTouchEvent(pressToEvent(up))
    }
    override fun onDecrementPress(up:Boolean) {
        super.onDecrementPress(up)
        if (isWorkoutInactive() || stopped) return
        decrementer.onTouchEvent(pressToEvent(up))
    }
    override fun onPreviousValueClicked() {
        super.onPreviousValueClicked()
        if (mLastValue != null) {
            if (frameConfig.machineClassType == MachineClassificationType.Climbmill) {
                val resistanceValue = DataMatrix.getInt(readKey)
                val currentLastResistanceVal: Int = mLastValue as Int
                if (isFlashingOk()) {
                    stopFlashingOk()
                    startSaveLastTimer()
                    saveLastValue(resistanceValue)
                    DataMatrix.putInt(writeKey, currentLastResistanceVal)
                } else if (mLastValue!! > resistanceValue + 3) {
                    startFlashingOk()
                } else {
                    startSaveLastTimer()
                    saveLastValue(resistanceValue)
                    DataMatrix.putInt(writeKey, currentLastResistanceVal)
                }
            } else {
                startSaveLastTimer()
                val resistanceValue = DataMatrix.getInt(readKey)
                val currentLastResistanceVal: Int = mLastValue as Int
                saveLastValue(resistanceValue)
                DataMatrix.putInt(writeKey, currentLastResistanceVal)
            }
        }
    }

    override fun onKeypadCommit(value: Float) {
        super.onKeypadCommit(value)
        val resistanceToSet = value.roundToInt()
        val resistance = DataMatrix.getInt(readKey)
        if (resistanceToSet != resistance) {
            saveLastValue(resistance)
            startSaveLastTimer()
            DataMatrix.putInt(writeKey, resistanceToSet)
        }
    }


    init {
        incrementer = StatIncrementer(readKey, writeKey, true, frameConfig.resistance, StatIncrementer.TYPE_INT) {
            if (mReadyToSave) {
                val valueToSave = DataMatrix.getInt(readKey)
                saveLastValue(valueToSave)
            }
            decrementer.stop()
            startSaveLastTimer()
        }
        if (frameConfig.machineClassType == MachineClassificationType.Climbmill || frameConfig.machineClassType == MachineClassificationType.Stepper) {
            incrementer.setMaxChanges(3)
        } else {
            incrementer.setMaxChanges(30)
        }
        decrementer = StatIncrementer(readKey, writeKey, false, frameConfig.resistance, StatIncrementer.TYPE_INT) {
            if (mReadyToSave) {
                val valueToSave = DataMatrix.getInt(readKey)
                saveLastValue(valueToSave)
            }
            incrementer.stop()
            startSaveLastTimer()
        }
        decrementer.setMaxChanges(30)
    }

    private fun saveLastValue(valueToSave: Int) {
        if (valueToSave == 0) return // don't allow 0 resistance
        mLastValue = valueToSave
        previousValue.value = valueToSave.toString()
    }

    override fun stopIncrementing() {
        incrementer.stop()
        decrementer.stop()
    }

    private val valueUpdate: IStatValueUpdate = object : IStatValueUpdate {
        override fun statValueChanged(value: String) {
            currentValue.value = value
        }
        override fun statTitleChanged(title: String) {
            if (frameConfig.machineClassType != MachineClassificationType.Climbmill) {
                label.value = StringLookup.get(R.string.resistance_tc) //title
            }
        }
        override fun statNextValueChanged(value: String) {
            nextValue.value = value
            nextValueLabel.value = StringLookup.get(R.string.next_resistance_tc)
        }
    }

    override fun start() {
        setupStatUpdates()
        setupDataMatrixCallbacks()
        setupKeypad()
        stopped = false
    }

    override fun stop() {
        mDataMatrix.finish()
        stopped = true
        stopIncrementing()
        stopSaveTimer()
        val stats = StatValueUpdaterCreator.getInstance()
        val statValueUpdater = stats.getValue(StatValueUpdaterCreator.RESISTANCE)
        statValueUpdater.removeCallback(valueUpdate)
    }

    private fun setupStatUpdates() {
        val stats = StatValueUpdaterCreator.getInstance()
        val statUpdater = stats.getValue(StatValueUpdaterCreator.RESISTANCE)
        currentValue.value = statUpdater.currentValue
        if (frameConfig.machineClassType == MachineClassificationType.Climbmill) {
            unit.value = StringLookup.get(R.string.level_tc)
            label.value = ""
        } else {
            unit.value = ""
            label.value = StringLookup.get(R.string.resistance_tc) //statUpdater.defaultTitle
        }
        statUpdater.addCallbackInterface(valueUpdate)
    }

    private fun setupDataMatrixCallbacks() {
        mDataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT,
            { value: Int? ->
                val keyCode = KeyPress.getKeyCode(value!!)
                val keyState = KeyPress.getKeyState(value)
                if (keyCode == KeyCode.KEY_RESISTANCE_DOWN) {
                    onDecrementPress(keyState == KeyPress.KeyState.KEY_UP)
                }
                if (keyCode == KeyCode.KEY_RESISTANCE_UP) {
                    onIncrementPress(keyState == KeyPress.KeyState.KEY_UP)
                }
            }, false
        )

        // When the state changes stop changing values
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, {
            if (it == WorkoutKeys.Constants.RUN_STATE_PAUSE &&
                (frameConfig.machineClassType == MachineClassificationType.Climbmill ||
                        frameConfig.machineClassType == MachineClassificationType.Stepper)
            ) {
                val resistance = DataMatrix.getInt(readKey)
                if (resistance == 1) {
                    mLastValue = null
                    previousValue.value = "--"
                } else {
                    saveLastValue(resistance)
                }
            }
            stopIncrementing()
        }, false)
        mDataMatrix.createEventAdapter(UiServiceKeys.EVENT_STOP_SIDE_BAR_INCREMENT,
            { stopIncrementing() }, false
        )
    }

    private fun setupKeypad() {
        range.value = frameConfig.resistance
        if (frameConfig.machineClassType == MachineClassificationType.Climbmill) {
            keypadUnit.value = StringLookup.get(R.string.level_tc)
            label.value = ""
            unit.value = StringLookup.get(R.string.level_tc)
        } else {
            keypadUnit.value = StringLookup.get(R.string.resistance_tc)
            label.value = StringLookup.get(R.string.resistance_tc)
            unit.value = ""
        }
        //presetValues.value = createPresetValues(range.value.min, range.value.max, range.value.stepSize)
        presetValues.value = createPresetValuesResistance(frameConfig)
        iconId.intValue = 0 // not sure we have icon for resistance/level
    }
}


/**
 * TARGET HEART RATE CONTROL
 */
class HearRateControl(val frameConfig: FrameConfig, location: FrameControlLocation) : ControlGroupModel(location) {
    private val mDataMatrix = DataMatrix()
    private var incrementer: StatIncrementer
    private var decrementer: StatIncrementer
    private var incrementIncline: StatIncrementer
    private var decrementIncline: StatIncrementer
    private var stopped = false // set to true in stop to prevent touch events on this page (OC-214)

    override fun getValueFloat(): Float = DataMatrix.getFloat(WorkoutKeys.DATA_TARGET_HEART_RATE_PERCENT_FLOAT)
    override fun onIncrementPress(up:Boolean) {
        super.onIncrementPress(up)
        if (isWorkoutInactive() || stopped) return
        incrementer.onTouchEvent(pressToEvent(up))
    }
    override fun onDecrementPress(up:Boolean) {
        super.onDecrementPress(up)
        if (isWorkoutInactive() || stopped) return
        decrementer.onTouchEvent(pressToEvent(up))
    }
    override fun onPreviousValueClicked() {
        super.onPreviousValueClicked()
        // Last button for target heart rate just displays the incline
    }

    override fun onKeypadCommit(value: Float) {
        super.onKeypadCommit(value)
        DataMatrix.putFloat(WorkoutKeys.DATA_TARGET_HEART_RATE_PERCENT_FLOAT, value)
    }

    init {
        incrementer = StatIncrementer(
            WorkoutKeys.DATA_TARGET_HEART_RATE_PERCENT_FLOAT.toString(),
            WorkoutKeys.DATA_TARGET_HEART_RATE_PERCENT_FLOAT.toString(),
            true, frameConfig.percentTargetHeartRate, StatIncrementer.TYPE_FLOAT
        ) { decrementer.stop() }

        decrementer = StatIncrementer(
            WorkoutKeys.DATA_TARGET_HEART_RATE_PERCENT_FLOAT.toString(),
            WorkoutKeys.DATA_TARGET_HEART_RATE_PERCENT_FLOAT.toString(),
            false, frameConfig.percentTargetHeartRate, StatIncrementer.TYPE_FLOAT
        ) { incrementer.stop() }

        incrementIncline = StatIncrementer(
            HALKeys.DATA_TARGET_INCLINE_FLOAT.toString(),
            WorkoutKeys.ACTION_SET_INCLINE_WORKOUT_FLOAT.toString(),
            true, frameConfig.incline, StatIncrementer.TYPE_FLOAT
        ) { decrementIncline.stop() }

        decrementIncline = StatIncrementer(
            HALKeys.DATA_TARGET_INCLINE_FLOAT.toString(),
            WorkoutKeys.ACTION_SET_INCLINE_WORKOUT_FLOAT.toString(),
            false, frameConfig.incline, StatIncrementer.TYPE_FLOAT
        ) { incrementIncline.stop() }
    }

    override fun start() {
        setupStatUpdates()
        setupDataMatrixCallbacks()
        setupKeypad()
        stopped = false
    }

    private fun setupStatUpdates() {
        mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_TARGET_HEART_RATE_PERCENT_FLOAT) { value: Float? ->
            value?.let {
                currentValue.value = it.roundToInt().toString()
            }
        }
        unit.value = StringLookup.get(R.string.target_uc)
        label.value = StringLookup.get(R.string.percent_heart_rate_uc)
        if (frameConfig.hasIncline()) {
            val inclineUpdater = StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.INCLINE)
            previousLabel.value = StringLookup.get(R.string.incline_tc) //inclineUpdater.defaultTitle
            previousValue.value = inclineUpdater.defaultValue
            inclineUpdater.addCallbackInterface(mInclineUpdate)
        } else {
            previousVisible.value = false
        }
    }

    private val mInclineUpdate: IStatValueUpdate = object : IStatValueUpdate {
        override fun statValueChanged(value: String) {
            previousValue.value = value //setLastAlternateValue(value)
        }
        override fun statTitleChanged(title: String) {
            previousLabel.value = StringLookup.get(R.string.incline_tc) //title //setLastAlternateTitle(title)
        }
        override fun statNextValueChanged(value: String) {
            nextValue.value = value
            nextValueLabel.value = StringLookup.get(R.string.next_incline_tc)
        }
    }

    private fun setupDataMatrixCallbacks() {
        // When the state changes stop changing values
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT) { stopIncrementing() }
        mDataMatrix.createEventAdapter(UiServiceKeys.EVENT_STOP_SIDE_BAR_INCREMENT) { stopIncrementing() }
        // Handle frame buttons on certain frames to change incline during THR workouts
        when (frameConfig.machineClassType) {
            MachineClassificationType.Treadmill, MachineClassificationType.Ascent -> mDataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT) { value: Int? ->
                val keyCode = KeyPress.getKeyCode(value!!)
                val keyState = KeyPress.getKeyState(value)
                if (keyCode == KeyCode.KEY_INCLINE_DOWN) {
                    incrementDownFramePressed(if (keyState == KeyPress.KeyState.KEY_DOWN) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_UP)
                }
                if (keyCode == KeyCode.KEY_INCLINE_UP) {
                    incrementUpFramePressed(if (keyState == KeyPress.KeyState.KEY_DOWN) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_UP)
                }
            }
            else -> {}
        }
    }

    private fun incrementUpFramePressed(action: Int) {
        if (isWorkoutInactive() || stopped) return
        incrementIncline.onTouchEvent(action)
    }

    private fun incrementDownFramePressed(action: Int) {
        if (isWorkoutInactive() || stopped) return
        decrementIncline.onTouchEvent(action)
    }

    override fun stopIncrementing() {
        incrementer.stop()
        decrementer.stop()
        incrementIncline.stop()
        decrementIncline.stop()
    }

    private fun setupKeypad() {
        val labelString = StringLookup.get(R.string.target_uc) + " " + StringLookup.get(R.string.percent_heart_rate_uc)
        range.value = frameConfig.percentTargetHeartRate
        keypadUnit.value = labelString
        label.value = StringLookup.get(R.string.percent_heart_rate_uc)
        unit.value = StringLookup.get(R.string.target_uc)
        presetValues.value = createPresetValues(range.value.min, range.value.max, range.value.stepSize)
        iconId.intValue = R.drawable.ic_heart_white
    }

    override fun stop() {
        mDataMatrix.finish()
        stopped = true
        stopIncrementing()
        val inclineUpdater =StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.INCLINE)
        inclineUpdater.removeCallback(mInclineUpdate)
    }
}


/**
 * TARGET WATTS CONTROL
 */
class TargetWattsControl(val frameConfig: FrameConfig, location: FrameControlLocation) : ControlGroupModel(location) {
    private val mDataMatrix = DataMatrix()
    private var incrementer: StatIncrementer
    private var decrementer: StatIncrementer
    private var stopped = false // set to true in stop to prevent touch events on this page (OC-214)

    override fun getValueFloat(): Float = DataMatrix.getInt(WorkoutKeys.DATA_TARGET_WATTS_INT).toFloat()
    override fun getUserChangesAllowed(): Boolean {
        return DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT) != WorkoutIds.COOPER_FITNESS_TEST
    }
    override fun onIncrementPress(up:Boolean) {
        super.onIncrementPress(up)
        if (isWorkoutInactive() || stopped) return
        incrementer.onTouchEvent(pressToEvent(up))
    }
    override fun onDecrementPress(up:Boolean) {
        super.onDecrementPress(up)
        if (isWorkoutInactive() || stopped) return
        decrementer.onTouchEvent(pressToEvent(up))
    }
    override fun onPreviousValueClicked() {
        super.onPreviousValueClicked()
        // Last button for constant watts just displays the incline
    }

    override fun onKeypadCommit(value: Float) {
        super.onKeypadCommit(value)
        DataMatrix.putInt(WorkoutKeys.DATA_TARGET_WATTS_INT, value.roundToInt())
    }

    override fun start() {
        setupStatUpdates()
        setupDataMatrixCallbacks()
        setupKeypad()
        stopped = false
    }

    private fun setupStatUpdates() {
        currentValue.value = ""
        label.value = StringLookup.get(R.string.target_uc)
        unit.value = StringLookup.get(R.string.watts_uc)
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_TARGET_WATTS_INT,
            { value: Int ->
                val targetWatts = value.toString()
                currentValue.value = targetWatts
            }, true
        )
        if (frameConfig.hasIncline()) {
            val inclineUpdater = StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.INCLINE)
            previousLabel.value = inclineUpdater.defaultTitle
            previousValue.value = inclineUpdater.defaultValue
            inclineUpdater.addCallbackInterface(mInclineUpdate)
        } else {
            previousVisible.value = false
        }
    }

    private val mInclineUpdate: IStatValueUpdate = object : IStatValueUpdate {
        override fun statValueChanged(value: String) {
            previousValue.value = value //setLastAlternateValue(value)
        }
        override fun statTitleChanged(title: String) {
            previousLabel.value = title //setLastAlternateTitle(title)
        }
        override fun statNextValueChanged(value: String) {
            nextValue.value = value
        }
    }

    init {
        incrementer = StatIncrementer(
            WorkoutKeys.DATA_TARGET_WATTS_INT.toString(),
            WorkoutKeys.DATA_TARGET_WATTS_INT.toString(),
            true, frameConfig.constantWatts, StatIncrementer.TYPE_INT
        ) { decrementer.stop() }
        incrementer.setMaxChanges(105)
        decrementer = StatIncrementer(
            WorkoutKeys.DATA_TARGET_WATTS_INT.toString(),
            WorkoutKeys.DATA_TARGET_WATTS_INT.toString(),
            false, frameConfig.constantWatts, StatIncrementer.TYPE_INT
        ) { incrementer.stop() }
        decrementer.setMaxChanges(105)
    }

    private fun setupDataMatrixCallbacks() {
        // When the state changes stop changing values
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT) { stopIncrementing() }
        mDataMatrix.createEventAdapter(UiServiceKeys.EVENT_STOP_SIDE_BAR_INCREMENT) { stopIncrementing() }
        mDataMatrix.createIntAdapter(HALKeys.EVENT_KEYPRESS_BITMAP_INT) { value: Int? ->
            val workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT)
            // Do not accept user changes for cooper test
            if (workoutId == WorkoutIds.COOPER_FITNESS_TEST) return@createIntAdapter
            val keyCode = KeyPress.getKeyCode(value!!)
            val keyState = KeyPress.getKeyState(value)
            if (keyState == KeyPress.KeyState.KEY_DOWN) {
                when (keyCode) {
                    KeyCode.KEY_RESISTANCE_DOWN -> incrementResistance(false)
                    KeyCode.KEY_RESISTANCE_UP -> incrementResistance(true)
                    KeyCode.KEY_INCLINE_DOWN -> incrementIncline(false)
                    KeyCode.KEY_INCLINE_UP -> incrementIncline(true)
                    else -> {}
                }
            }
        }
    }

    private fun incrementIncline(up: Boolean) {
        val currentIncline = DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT)
        val stepSize = frameConfig.incline.stepSize
        val newIncline = if (up) currentIncline + stepSize else currentIncline - stepSize
        DataMatrix.putFloat(WorkoutKeys.ACTION_SET_INCLINE_WORKOUT_FLOAT, newIncline)
    }

    private fun incrementResistance(up: Boolean) {
        val currentResistance = DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT)
        val stepSize = frameConfig.resistance.stepSize
        val newResistance = Math.round(if (up) currentResistance + stepSize else currentResistance - stepSize)
        DataMatrix.putInt(WorkoutKeys.ACTION_SET_RESISTANCE_WORKOUT_INT, newResistance)
    }

    override fun stopIncrementing() {
        incrementer.stop()
        decrementer.stop()
    }

    private fun setupKeypad() {
        range.value = frameConfig.constantWatts
        keypadUnit.value = StringLookup.get(R.string.watts_uc)
        label.value = StringLookup.get(R.string.target_uc)
        unit.value = StringLookup.get(R.string.watts_uc)
        presetValues.value = createPresetValues(range.value.min, range.value.max, range.value.stepSize)
        iconId.intValue = R.drawable.ic_watts_white
    }

    override fun stop() {
        mDataMatrix.finish()
        stopped = true
        stopIncrementing()
        val inclineUpdater = StatValueUpdaterCreator.getInstance().getValue(StatValueUpdaterCreator.INCLINE)
        inclineUpdater.removeCallback(mInclineUpdate)
    }
}
