import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jht.interfaces.BoolEmitter
import com.jht.interfaces.EventEmitter
import com.jht.mock.interfaces.models.MockDeviceModel
import com.jht.mock.interfaces.models.MockUIBaseModel
import com.jht.mock.interfaces.models.MockUIServiceModel
import com.jht.mock.interfaces.models.MockWorkoutModel
import com.jht.switchStride.R
import com.jht.switchStride.SwitchStrideViewModel
import com.jht.switchStride.components.ControlButton
import com.jht.switchStride.components.PagerCard
import com.jht.switchStride.components.statInfoToCardData
import com.jht.switchStride.ui.ReverseBeltScreen
import com.jht.switchStride.utils.bottomButtonBackground
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd

@Composable
internal fun SwitchStrideScreen(switchStrideViewModel: SwitchStrideViewModel) {

    val uiState by switchStrideViewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        RunScreen(
            isBeltInReverse = uiState.isBeltInReverse,
            switchStrideViewModel = switchStrideViewModel,
            pauseTimer = uiState.pauseTimer
        )
        if (uiState.showReverseBeltDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                ReverseBeltScreen(
                    isBeltInReverse = uiState.isBeltInReverse,
                    onCancel = switchStrideViewModel::onReverseDialogCancelled,
                    onReverse = switchStrideViewModel::onReverseConfirmed
                )
            }
        }
    }
}

@Composable
private fun RunScreen(
    isBeltInReverse: Boolean,
    switchStrideViewModel: SwitchStrideViewModel,
    pauseTimer: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painterResource(
                    id = if (isBeltInReverse) {
                        R.drawable.purple_background
                    } else {
                        R.drawable.blue_background
                    }
                ),
                contentScale = ContentScale.FillBounds
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // top cards area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction = 0.8f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    space = 8.dp.computeSizeHd(), alignment = Alignment.CenterVertically
                )
            ) {
                if (isBeltInReverse) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color.Black,
                                shape = RoundedCornerShape(size = 10.dp.computeSizeHd())
                            )
                            .padding(
                                horizontal = 40.dp.computeSizeHd(), vertical = 11.dp.computeSizeHd()
                            ), contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.belt_is_currently_in_reverse_tc),
                            fontSize = 20.sp.computeSize(1920f),
                            fontWeight = FontWeight.Normal,
                            color = Color.White
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Calories / Heart Rate pager
                    PagerCard(
                        modifier = Modifier,
                        cardDataList = listOf(
                            statInfoToCardData(
                                stat = switchStrideViewModel.caloriesStat,
                                icon = R.drawable.calories_switch_stride,
                                label = R.string.calories_tc
                            ),
                            statInfoToCardData(
                                stat = switchStrideViewModel.heartRateStat,
                                icon = R.drawable.ic_heart,
                                label = R.string.heart_rate_tc
                            )
                        ),
                        isInReverse = isBeltInReverse
                    )
                    // Single page Time Elapsed card (no pager dots because list size is 1)
                    PagerCard(
                        modifier = Modifier,
                        cardDataList = listOf(
                            statInfoToCardData(
                                stat = switchStrideViewModel.timeElapsedStat,
                                icon = R.drawable.ic_time_total_white,
                                label = R.string.time_elapsed_tc
                            )
                        ),
                        isInReverse = isBeltInReverse
                    )
                    // Distance / Pace pager
                    PagerCard(
                        modifier = Modifier,
                        cardDataList = listOf(
                            statInfoToCardData(
                                stat = switchStrideViewModel.distanceStat,
                                icon = R.drawable.ic_distance_white,
                                label = R.string.distance_tc
                            ),
                            statInfoToCardData(
                                stat = switchStrideViewModel.paceStat,
                                icon = R.drawable.ic_distance_white,
                                label = R.string.pace_tc
                            )
                        ),
                        isInReverse = isBeltInReverse
                    )
                }
            }

        }
        // bottom button area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bottomButtonBackground)
        ){
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp.computeSizeHd())
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(460.dp.computeSizeHd()))
                Spacer(Modifier.width(460.dp.computeSizeHd()))
                Box(
                    modifier = Modifier.width(460.dp.computeSizeHd()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pauseTimer,
                        modifier = Modifier.alpha(if (switchStrideViewModel.showPauseTimer) 1f else 0f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp.computeSize(1920f),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                switchStrideViewModel.controlButtonsTypes.forEach {
                    ControlButton(
                        buttonConfig = switchStrideViewModel.buttonConfig(it),
                    )
                }
            }
        }
    }
}


@Composable
@AllDevicePreviews
private fun SwitchStrideScreenPreview() {
    val mockUIBaseModel = MockUIBaseModel()
    val mockDeviceModel = MockDeviceModel()
    val mockWorkoutModel =
        MockWorkoutModel(distancePrecision = 1)
    val mockUIServiceModel = MockUIServiceModel()
    val viewModel = SwitchStrideViewModel(
        uiBaseModel = mockUIBaseModel,
        deviceModel = mockDeviceModel,
        workoutModel = mockWorkoutModel,
        uiServiceModel = mockUIServiceModel
    )

    viewModel.start()

    // Mock values
    mockWorkoutModel.calories.emit(60f)
    mockWorkoutModel.gymkitCalories.emit(55f)
    mockWorkoutModel.heartRate.emit(120)
    mockWorkoutModel.timeElapsedMs.emit(634000)
    mockWorkoutModel.distanceKm.emit(9.66f)
    mockWorkoutModel.speedKph.emit(9.0f)
    (mockWorkoutModel.isBeltInReverse as BoolEmitter).emit(false)
    (mockWorkoutModel.isWorkoutRunning as BoolEmitter).emit(true)
    (mockWorkoutModel.onStartWorkout as EventEmitter).emit()

    SwitchStrideScreen(
        switchStrideViewModel = viewModel
    )
}