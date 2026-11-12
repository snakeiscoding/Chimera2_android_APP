package com.jht.mock.interfaces.models

import android.net.Uri
import android.util.SparseArray
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.jht.data.BaseFeaturedWorkoutPlugin
import com.jht.data.CustomWorkout
import com.jht.data.CustomWorkouts
import com.jht.data.LandmarkWorkoutItem
import com.jht.data.WorkoutBadge
import com.jht.data.WorkoutInput
import com.jht.data.WorkoutInputLevelInfo
import com.jht.data.WorkoutInputUi
import com.jht.data.WorkoutItem
import com.jht.data.sprint8.ISprint8WorkoutData
import com.jht.enums.ConsoleWorkoutVariables
import com.jht.enums.UnitMeasurement
import com.jht.interfaces.BoolValueListener
import com.jht.interfaces.DataVariableListener
import com.jht.interfaces.EventListener
import com.jht.interfaces.R
import com.jht.interfaces.models.WorkoutCatalogModel
import com.jht.mock.interfaces.MockBoolValue
import com.jht.mock.interfaces.MockEventListener

class MockWorkoutCatalogModel(): WorkoutCatalogModel() {
    var customWorkoutData = CustomWorkouts()

    override val workoutListReady: BoolValueListener = MockBoolValue(true)
    override val onVAWorkoutsChange: EventListener = MockEventListener()

    private val mockWorkoutItem = WorkoutItem(
        workoutId = 0,
        appId = "mock_app_id",
        titleId = R.string.title_tc,
        descriptionId = R.string.long_description,
        background = listOf(Uri.EMPTY),
        thumbnail = Uri.EMPTY,
        badge = WorkoutBadge(),
        optionalId = "mock_optional_id",
    )

    private val mockLandmarkWorkoutItem = LandmarkWorkoutItem(
        workoutId = 0,
        appId = "mock_app_id",
        titleId = R.string.title_tc,
        descriptionId = R.string.long_description,
        background = listOf(Uri.EMPTY),
        thumbnail = Uri.EMPTY,
        badge = WorkoutBadge(),
        optionalId = "mock_optional_id",
    )

    override val getWorkoutList: SparseArray<WorkoutItem> = SparseArray(10)

    val mapInputToFloat = mutableStateMapOf<String, MutableState<Float>>()
    val isLoadingWorkout = mutableStateOf(false)

    init {
        for (i in 0..9) {
            getWorkoutList.append(i,mockWorkoutItem)
        }
        mapInputToFloat[ConsoleWorkoutVariables.WEIGHT] = mutableStateOf(75f)
        mapInputToFloat[ConsoleWorkoutVariables.AGE] = mutableStateOf(25f)
        mapInputToFloat[ConsoleWorkoutVariables.HEIGHT] = mutableStateOf(1.5f)
        mapInputToFloat[ConsoleWorkoutVariables.GENDER] = mutableStateOf(0f)
        mapInputToFloat[ConsoleWorkoutVariables.LEVEL] = mutableStateOf(1f)
    }

    override val getVAWorkouts: List<WorkoutItem> = getMockWorkoutList(30)

    override val getLandmarkWorkouts: List<LandmarkWorkoutItem> = List(30, {mockLandmarkWorkoutItem})

    override val getFeaturedWorkouts: List<WorkoutItem> = getMockWorkoutList(30)

    override val getFeatureWorkoutPlugins: List<BaseFeaturedWorkoutPlugin> = emptyList()

    override val getCarouselWorkouts: List<WorkoutItem> = getMockWorkoutList(3)

    override val getTrainingCarouselWorkouts: List<WorkoutItem> = getMockWorkoutList(3)

    fun getMockWorkoutList(size: Int): List<WorkoutItem>{
       return List(size) { mockWorkoutItem }
    }

    override fun setFeaturedWorkouts() {
        TODO("Not yet implemented")
    }

    override fun getSprint8WorkoutData(callback: (List<ISprint8WorkoutData>) -> Unit) {
        return
    }

    override fun loadSprint8CustomWorkoutData(): List<ISprint8WorkoutData> = emptyList()

    override suspend fun getWorkoutInputs(workoutId: Int, optionalId: String): List<WorkoutInput> {
        // 3 is probably good for previews
        return listOf()
    }

    fun getWorkoutInputsForPreview(): List<WorkoutInputUi> {
        // 3 is probably good for previews
        return listOf(
            WorkoutInputUi(
                WorkoutInput(
                min = 50f,
                max = 500f,
                defaultValue = 150f,
                stepSize = 1f,
                precision = 0,
                label = "Weight",
                unitString = "Kg",
                unitMeasurement = UnitMeasurement.Kilogram,
                identifier = ConsoleWorkoutVariables.WEIGHT,
                ),
                currentValue = mutableFloatStateOf(150f)
            ),
            WorkoutInputUi(
                WorkoutInput(
                min = 1f,
                max = 20f,
                defaultValue = 1f,
                stepSize = 1f,
                precision = 0,
                label = "Level",
                unitString = "",
                identifier = ConsoleWorkoutVariables.LEVEL,
                ),
                currentValue = mutableFloatStateOf(1f)
            ),
            WorkoutInputUi(
                WorkoutInput(
                min = .8f,
                max = 3f,
                defaultValue = 1.5f,
                stepSize = .01f,
                precision = 2,
                label = "Height",
                unitString = "Cm",
                unitMeasurement = UnitMeasurement.Centimeter,
                identifier = ConsoleWorkoutVariables.HEIGHT,
                ),
                currentValue = mutableFloatStateOf(1.5f)
            ),
            WorkoutInputUi(
                WorkoutInput(
                min = 50f,
                max = 500f,
                defaultValue = 150f,
                stepSize = 1f,
                precision = 0,
                label = "Weight",
                unitString = "Kg",
                unitMeasurement = UnitMeasurement.Kilogram,
                identifier = ConsoleWorkoutVariables.WEIGHT,
                ),
                currentValue = mutableFloatStateOf(150f)
            ),
            WorkoutInputUi(
                WorkoutInput(
                min = 0f,
                max = 2f,
                defaultValue = 0f,
                stepSize = 1f,
                precision = 0,
                label = "Gender",
                unitString = "",
                identifier = ConsoleWorkoutVariables.GENDER,
                ),
                currentValue = mutableFloatStateOf(0f)
            ),
            WorkoutInputUi(
                WorkoutInput(
                min = 14f,
                max = 110f,
                defaultValue = 25f,
                stepSize = 1f,
                precision = 0,
                label = "Age",
                unitString = "Years",
                identifier = ConsoleWorkoutVariables.AGE,
                ),
                currentValue = mutableFloatStateOf(25f)
            ),
            WorkoutInputUi(
                WorkoutInput(
                min = 0.5f,
                max = 12f,
                defaultValue = 0.5f,
                stepSize = 1f,
                precision = 0,
                label = "Speed",
                unitString = "mph",
                identifier = ConsoleWorkoutVariables.SPEED_IN,
                ),
                currentValue = mutableFloatStateOf(0.5f)
            ),
            WorkoutInputUi(
                WorkoutInput(
                min = 0f,
                max = 15f,
                defaultValue = 0f,
                stepSize = 1f,
                precision = 0,
                label = "Incline",
                unitString = "%",
                identifier = ConsoleWorkoutVariables.INCLINE_IN,
                ),
                currentValue = mutableFloatStateOf(0f)
            )
        )
    }

    override fun getWorkoutInputLevelInfo(level: Int): List<WorkoutInputLevelInfo> {
        // will only ever have 1 or 2, 2 is good for preview
        return listOf(
            WorkoutInputLevelInfo(
                name = "Speed",
                max = "5.0 mph",
                average = "2.5 mph",
            ),
            WorkoutInputLevelInfo(
                name = "Incline",
                max = "5 %",
                average = "2.5 %",
            ),
        )
    }

    override suspend fun setWorkoutControllerModel() {
    }

    override val stepsPerFloor: Int =
        1

    override val stepsPerMeter: Float =
        7.2f

    override val customWorkouts = object : DataVariableListener<CustomWorkouts>{
        private val listeners = mutableListOf<(CustomWorkouts) -> Unit>()
        override fun listen(onChange: (CustomWorkouts) -> Unit) {
            listeners.add(onChange)
        }
        override var value: CustomWorkouts
            get() = customWorkoutData
            set(value) {
                customWorkoutData = value
                listeners.forEach { it.invoke(value) }
            }
        override fun listenAndTrigger(onChange: (CustomWorkouts) -> Unit) {
            listeners.add(onChange)
            onChange(customWorkoutData)
        }
    }
    override fun startCustomWorkout(customWorkout: CustomWorkout) {}
    override fun startCustomSprint8Workout(speedOrResistanceData: List<Float>, inclineData: List<Float>) {}


    override fun close() {
        TODO("Not yet implemented")
    }
}