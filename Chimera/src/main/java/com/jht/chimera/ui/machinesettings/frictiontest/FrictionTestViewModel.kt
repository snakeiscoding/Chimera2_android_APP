package com.jht.chimera.ui.machinesettings.frictiontest

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.formatting.BasicNumberFormat
import com.jht.chimera.lib.common.frictiontest.ReceiveTreadmillOperate
import com.jht.datamatrixlib.DataMatrix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.absoluteValue

private const val TAG = "FrictionTest"

// Tolerance for the actual value for events requiring speed/incline to reach specific values
private const val SPEED_TOLERANCE = 0.1f
private const val INCLINE_TOLERANCE = 0.2f
class FrictionTestViewModel : ViewModel() {

    private val _frictionTestState = MutableStateFlow(
        FrictionTestState()
    )

    val frictionTestState: StateFlow<FrictionTestState> = _frictionTestState.asStateFlow()

    val dataMatrix = DataMatrix()

    private var valueEvents = mutableListOf<OnValueEvent>()

    private val raisingIncline = mutableListOf<Float>()
    private val raisingTorque = mutableListOf<Int>()

    private val loweringIncline = mutableListOf<Float>()
    private val loweringTorque = mutableListOf<Int>()

    private var dataStartTime = 0L
    private var dataEndTime = 0L
    private var dataReceivedNum = 0

    init {
        // Setup any datamatrix listeners here
        dataMatrix.createParcelableAdapter(
            HALKeys.ACTION_RECEIVED_TREADMILL_OPERATE_PARCELABLE,
            ReceiveTreadmillOperate::class.java
        ) { data ->
            _frictionTestState.update { current ->
                current.copy(
                    currentSpeedKph = formatSpeed(data.actualSpeed),
                    currentIncline = formatIncline(data.actualIncline)
                )
            }

            dataReceivedNum += 1

            when (_frictionTestState.value.testStage) {
                FrictionTestStage.Raising -> {
                    raisingIncline.add(data.actualIncline)
                    raisingTorque.add(data.motorTorque)
                }
                FrictionTestStage.Lowering -> {
                    loweringIncline.add(data.actualIncline)
                    loweringTorque.add(data.motorTorque)
                }
                else -> { /* Do nothing */}
            }

            updateWaitingEvents(data.actualSpeed, data.actualIncline)
        }

        setStageNotStarted()
    }

    fun registerLifecycleObserver(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                stopBelt()
            }
        })
    }

    private fun updateWaitingEvents(speedKph: Float, incline: Float) {
        synchronized(valueEvents) {
            val eventsToRemove = mutableListOf<Int>()
            valueEvents.forEachIndexed { index, valueEvent ->
                if ((valueEvent.speedKph - speedKph).absoluteValue <= SPEED_TOLERANCE &&
                    (valueEvent.incline - incline).absoluteValue <= INCLINE_TOLERANCE
                ) {
                    eventsToRemove.add(index)
                    valueEvent.callback()
                }
            }

            for (i in eventsToRemove.size - 1 downTo 0) {
                valueEvents.removeAt(i)
            }
        }
    }

    /**
     * Handles starting the test when we are able to
     */
    fun startButtonPressed() {
        if (_frictionTestState.value.testStage == FrictionTestStage.NotStarted ||
            _frictionTestState.value.testStage == FrictionTestStage.TestComplete) {
            _frictionTestState.update { current -> current.copy(testStage = FrictionTestStage.SpeedingUp) }

            Log.i(TAG, "Start friction test")
            // Start the test
            setStageSpeedingUp()
        }
    }

    private fun startSendingTreadmillData() {
        dataStartTime = System.currentTimeMillis()
        DataMatrix.sendEvent(HALKeys.ACTION_START_FRICTION_TEST_DATA_COLLECTION)
    }

    fun stopButtonPressed() {
        Log.i(TAG, "Stop pressed in friction test")
        setStageNotStarted()
    }

    private fun stopBelt() {
        updateTargetValues(0f, 0f)
        DataMatrix.sendEvent(HALKeys.ACTION_STOP_FRICTION_TEST_DATA_COLLECTION)

        _frictionTestState.update { current ->
            current.copy(
                currentSpeedKph = formatSpeed(0f),
                currentIncline = formatIncline(0f)
            )
        }

        DataMatrix.sendEvent(HALKeys.ACTION_STOP_MACHINE)
    }

    @Override
    override fun onCleared() {
        super.onCleared()

        Log.i(TAG, "Friction test closing")

        DataMatrix.sendEvent(HALKeys.ACTION_STOP_MACHINE)
    }

    private fun updateTargetValues(speedKph: Float, incline: Float) {
        DataMatrix.putFloat(HALKeys.ACTION_SET_FRICTION_TEST_TARGET_SPEED_KPH_FLOAT, speedKph)
        DataMatrix.putFloat(HALKeys.ACTION_SET_FRICTION_TEST_TARGET_INCLINE_FLOAT, incline)

        _frictionTestState.update { current ->
            current.copy(
                targetSpeedKph = formatSpeed(speedKph),
                targetIncline = formatIncline(incline)
            )
        }
    }

    private fun updateTestStage(testStage: FrictionTestStage) {
        _frictionTestState.update { current ->
            current.copy(
                testStage = testStage
            )
        }
    }

    private fun waitTime(waitTimeMs: Long, onWaitComplete: ()->Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(waitTimeMs)
            onWaitComplete()
        }
    }

    private fun setStageNotStarted() {
        stopBelt()
        updateTestStage(FrictionTestStage.NotStarted)

        synchronized(valueEvents) {
            valueEvents.clear()
        }

        updateInstructionText(
            "To test step on belt and press START TEST. Do not hold rails during test.\n\nThe test will increase speed to 5 kph and incline to 15%. Step off the belt after 15% incline."
        )
    }

    private fun updateInstructionText(instructionText: String) {
        Log.e("FrictionTest", "Text updated to: $instructionText")
        _frictionTestState.update { it.copy(
            instructionText = instructionText
        ) }
    }

    private fun setStageSpeedingUp() {
        clearStoredTestData()

        updateTestStage(FrictionTestStage.SpeedingUp)
        updateTargetValues(5f, 0f)
        startSendingTreadmillData()

        createValueEvent(5f, 0f) {
            setStageUserOnCountdown()
        }

        updateInstructionText(
            "Increasing speed to 5kph."
        )
    }

    private fun clearStoredTestData() {
        raisingIncline.clear()
        raisingTorque.clear()
        loweringIncline.clear()
        loweringTorque.clear()
    }

    private fun setStageUserOnCountdown() {
        updateTestStage(FrictionTestStage.UserOnCountdown)
        waitTime(5000) {
            setRaising()
        }

        updateInstructionText(
            "Start walking on the belt. \n\nIncline will begin to increase in 5 seconds."
        )
    }

    private fun setRaising() {
        updateTestStage(FrictionTestStage.Raising)
        updateTargetValues(5f, 15f)
        createValueEvent(5f, 15f) {
            setUserOffCountdown()
        }

        updateInstructionText(
            "Increasing incline to 15%."
        )
    }

    private fun setUserOffCountdown() {
        updateTestStage(FrictionTestStage.UserOffCountdown)
        waitTime(5000) {
            setLowering()
        }

        updateInstructionText(
            "Step off the belt."
        )
    }

    private fun setLowering() {
        updateTestStage(FrictionTestStage.Lowering)
        updateTargetValues(5f, 0f)
        createValueEvent(5f, 0f) {
            setTestComplete()
        }

        updateInstructionText(
            "Lowering incline to 0%."
        )
    }

    private fun setTestComplete() {
        val frictionValue = calculateFriction(raisingTorque, loweringTorque, raisingIncline, loweringIncline)
        updateTestStage(FrictionTestStage.TestComplete)
        stopBelt()

        updateInstructionText(
            "Friction has been calculated for this belt.\n\nFriction: $frictionValue"
        )

        dataEndTime = System.currentTimeMillis()
        val dataPerSecond = dataReceivedNum / ((dataEndTime - dataStartTime) / 1000.0)
        val dataPerSecondFormatted = String.format(Locale.ENGLISH, "%.2f", dataPerSecond)
        Log.e("FrictionTest", "The test sent $dataPerSecondFormatted samples per second")
        dataReceivedNum = 0
        dataStartTime = 0
    }

    private fun createValueEvent(speedKph: Float, incline: Float, callback: () -> Unit) {
        synchronized(valueEvents) {
            valueEvents.add(OnValueEvent(speedKph, incline, callback))
        }
    }

    private data class OnValueEvent(
        val speedKph: Float,
        val incline: Float,
        val callback: () -> Unit
    )


    // Don't follow our standard formatting since it breaks preview and uses datamatrix?
    private fun formatSpeed(speedKph: Float): String {
        return BasicNumberFormat.format(speedKph, 1)
    }

    private fun formatIncline(incline: Float): String {
        return BasicNumberFormat.format(incline, 1)
    }
}

// Data for display on the screen
data class FrictionTestState(
    val testStage: FrictionTestStage = FrictionTestStage.NotStarted,
    val targetSpeedKph: String = "0.0",
    val targetIncline: String = "0.0",
    val currentSpeedKph: String = "0.0",
    val currentIncline: String = "0.0",
    val instructionText: String = "",
    val showCountdown: Boolean = false,
    val countdownTime: String = "5"
)

// The different test stages for the friction test
enum class FrictionTestStage {
    NotStarted, // Test is not started, frame is idle
    SpeedingUp, // Test started, frame is moving to 5kph 0 incline
    UserOnCountdown, // Test is notifying user to get on belt
    Raising, // Incline is raising to max configured value
    UserOffCountdown, // User is notified to step off treadmill belt
    Lowering, // Incline lowers to 0, finishes data collection
    TestComplete // Show data, stop belt fully
}