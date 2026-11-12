package com.jht.spinbikeapps.targettraining.plugin

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.jht.data.BaseFeaturedWorkoutPlugin
import com.jht.data.WorkoutInputUi
import com.jht.data.WorkoutItem
import com.jht.enums.RelativeLocation
import com.jht.interfaces.models.DeviceModel
import com.jht.interfaces.models.WorkoutLauncherModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.resources.robotoFonts
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.ui.TargetTrainingNavHost
import com.jht.spinbikeapps.targettraining.ui.TargetTrainingScreen
import com.jht.theme.DynamicTheme
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd
import com.jht.widgets.StartWorkoutButton
import com.jht.widgets.helpers.getInjectionEntryPoint

/** Featured workout plugin for Target Training workout app */
class TargetTrainingPlugin(
    appContext: Context,
    workoutItem: WorkoutItem,
) : BaseFeaturedWorkoutPlugin(appContext, workoutItem) {
    private val workoutModel: WorkoutModel
    private val deviceModel: DeviceModel
    private val workoutLauncherModel: WorkoutLauncherModel

    init {
        val injection = getInjectionEntryPoint(appContext)
        workoutModel = injection.workout()
        workoutLauncherModel = injection.workoutLauncher()
        deviceModel = injection.devices()
    }

    @Composable
    override fun SetupScreen(
        workoutInputs: List<WorkoutInputUi>,
        onDismissRequest: () -> Unit,
    ) {
        val navController = rememberNavController()
        TargetTrainingNavHost(
            navController = navController,
            modifier = Modifier,
            title = stringResource(workoutItem.titleId),
            imageUri = workoutItem.background.firstOrNull() ?: Uri.EMPTY,
            description = stringResource(workoutItem.descriptionId),
            workoutInputList = workoutInputs,
            startSelectedWorkout = { startWorkout(workoutInputs) },
            isRfidIconVisible = deviceModel.rfidReaderLocation == RelativeLocation.LEFT_MIDDLE,
            content = {
                StartWorkoutButton(
                    onClick = {
                        navController.navigate(TargetTrainingScreen.TargetSetupScreen.route)
                    },
                    modifier = Modifier
                        .height(48.dp.computeSize(1920f))
                        .width(218.dp.computeSize(1920f)),
                    buttonColor = Color(0xFFFFCB27),
                    buttonTitle = R.string.start_workout_tc,
                    btnTextColor = DynamicTheme.colors.background1,
                    btnShape = MaterialTheme.shapes.medium.copy(CornerSize(18.dp.computeSizeHd())),
                    btnTextStyle = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 22.sp.computeSize(1920f),
                        fontFamily = robotoFonts
                    )
                )
            }
        )
    }

    override fun startWorkout(workoutInputs: List<WorkoutInputUi>) {
        workoutModel.workoutAppSetupBundle.value = appSetupBundle
        workoutLauncherModel.launchSelectedWorkoutWithSafetyCheck(workoutInputs)
    }

    override fun onLoad(workoutInputs: List<WorkoutInputUi>) {
        // nothing to do
    }
    override fun onUnload() {
        deviceModel.close()
        workoutLauncherModel.close()
        workoutModel.close()
    }
}
