package com.jht.simpleapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import com.jht.enums.FrameType
import com.jht.mock.interfaces.models.MockDeviceModel
import com.jht.mock.interfaces.models.MockUIBaseModel
import com.jht.mock.interfaces.models.MockUIServiceModel
import com.jht.mock.interfaces.models.MockWorkoutModel
import com.jht.theme.darkDynamicTheme
import com.jht.ui.Device15inPreviews

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SimpleAppRoute(
    simpleAppViewModel: SimpleAppViewModel,
) {

    val allowGoalChange = remember { simpleAppViewModel.allowGoalChange }
    val mainColor = remember { simpleAppViewModel.mainColor }
    val isFullscreen = remember {simpleAppViewModel.isFullscreen}
    val theme = remember {simpleAppViewModel.theme}

    SimpleAppScreen(
        statInfoCollection = simpleAppViewModel.statInfoCollection.value,
        allowGoalChange = allowGoalChange.value,
        theme = theme.value,
        displayColor = mainColor.value,
        isFullscreen = isFullscreen.value,
        onIncrementGoalClick = simpleAppViewModel::incrementGoal,
        onDecrementGoalClick = simpleAppViewModel::decrementGoal,
        onSizeClick = simpleAppViewModel::onSizeClick,
        onSelectCard = simpleAppViewModel::onSelectCard,
    )

}



@Device15inPreviews
@Composable
fun PreviewMockViewModelTreadmill() {
    val mockUIBaseModel = MockUIBaseModel()
    val mockDeviceModel = MockDeviceModel()
    val mockWorkoutModel = MockWorkoutModel()
    val mockUIServiceModel = MockUIServiceModel()
    val simpleAppViewModel = SimpleAppViewModel(mockUIBaseModel, mockDeviceModel, mockWorkoutModel, mockUIServiceModel)

    // modify mock
    mockUIBaseModel.frameType = FrameType.Treadmill
    mockWorkoutModel.distanceKm.emit(1.99f)
    simpleAppViewModel.start()

    SimpleAppRoute(simpleAppViewModel)
}

@Device15inPreviews
@Composable
fun PreviewMockViewModelClimbmill() {
    val mockUIBaseModel = MockUIBaseModel()
    val mockDeviceModel = MockDeviceModel()
    val mockWorkoutModel = MockWorkoutModel()
    val mockUIServiceModel = MockUIServiceModel()
    val simpleAppViewModel = SimpleAppViewModel(mockUIBaseModel, mockDeviceModel, mockWorkoutModel, mockUIServiceModel)
    simpleAppViewModel.theme.value = darkDynamicTheme

    // modify mock
    mockUIBaseModel.frameType = FrameType.Climbmill
    mockWorkoutModel.allowUserGoalChanges.emit(false) // should hide goal buttons
    simpleAppViewModel.start()

    SimpleAppRoute(simpleAppViewModel)
}

@Device15inPreviews
@Composable
fun PreviewMockViewModelBike() {
    val mockUIBaseModel = MockUIBaseModel()
    val mockDeviceModel = MockDeviceModel()
    val mockWorkoutModel = MockWorkoutModel()
    val mockUIServiceModel = MockUIServiceModel()
    val simpleAppViewModel = SimpleAppViewModel(mockUIBaseModel, mockDeviceModel, mockWorkoutModel, mockUIServiceModel)

    // modify mock
    mockUIBaseModel.frameType = FrameType.Bike
    mockWorkoutModel.heartRate.emit(80)
    simpleAppViewModel.start()
    simpleAppViewModel.onSelectCard(simpleAppViewModel.statInfoCollection.value.categoryList[0].statList[0])

    SimpleAppRoute(simpleAppViewModel)

}

@Device15inPreviews
@Composable
fun PreviewMockViewModelGymKit() {
    val mockUIBaseModel = MockUIBaseModel()
    val mockDeviceModel = MockDeviceModel()
    val mockWorkoutModel = MockWorkoutModel()
    val mockUIServiceModel = MockUIServiceModel()
    val simpleAppViewModel = SimpleAppViewModel(mockUIBaseModel, mockDeviceModel, mockWorkoutModel, mockUIServiceModel)

    // modify mock
    mockUIBaseModel.frameType = FrameType.Bike
    mockWorkoutModel.gymkitCalories.emit(80f)
    simpleAppViewModel.start()

    SimpleAppRoute(simpleAppViewModel)

}