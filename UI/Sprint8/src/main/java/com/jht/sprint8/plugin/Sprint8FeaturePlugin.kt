package com.jht.sprint8.plugin


import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.jht.data.BaseFeaturedWorkoutPlugin
import com.jht.data.WorkoutInputUi
import com.jht.data.WorkoutItem
import com.jht.enums.RelativeLocation
import com.jht.interfaces.models.DeviceModel
import com.jht.interfaces.models.WorkoutCatalogModel
import com.jht.interfaces.models.WorkoutLauncherModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.sprint8.R
import com.jht.sprint8.Screen
import com.jht.sprint8.Sprint8NavigationHost
import com.jht.theme.DynamicTheme
import com.jht.ui.computeSize
import com.jht.widgets.StartWorkoutButton
import com.jht.widgets.helpers.getInjectionEntryPoint

/** Featured workout plugin for Sprint8 workout app */
class Sprint8FeaturePlugin(
    appContext: Context,
    workoutItem: WorkoutItem,
) : BaseFeaturedWorkoutPlugin(appContext, workoutItem) {
    private val workoutModel: WorkoutModel
    private val workoutLauncherModel: WorkoutLauncherModel
    private val deviceModel: DeviceModel
    private val catalogModel: WorkoutCatalogModel // sprint8 custom launch logic is in catalog right now, might need to review that?

    init {
        val injection = getInjectionEntryPoint(appContext)
        workoutModel = injection.workout()
        workoutLauncherModel = injection.workoutLauncher()
        catalogModel = injection.workoutCatalog()
        deviceModel = injection.devices()
    }

    @Composable
    override fun SetupScreen(
        workoutInputs: List<WorkoutInputUi>,
        onDismissRequest: () -> Unit,
    ) {
        val navController = rememberNavController()
        val hasFrameControl by lazy {mutableStateOf(workoutModel.hasFrameControl())}

        Sprint8NavigationHost(
            navController = navController,
            modifier = Modifier,
            title = stringResource(workoutItem.titleId),
            imageUri = workoutItem.background.firstOrNull() ?: Uri.EMPTY,
            description = stringResource(workoutItem.descriptionId),
            workoutInputList = workoutInputs,
            onDismiss = onDismissRequest,
            startSelectedWorkout = { startWorkout(workoutInputs) },
            onCustomSprint8Start = ::startSprint8CustomWorkout,
            isRfidIconVisible = deviceModel.rfidReaderLocation == RelativeLocation.LEFT_MIDDLE,
            content = {
                StartWorkoutButton(
                    onClick = {
                        if (hasFrameControl.value) {
                            navController.navigate(Screen.Sprint8ExperienceScreen.route)
                        } else {
                            navController.navigate(Screen.Sprint8PreviewScreen.route)
                        }
                    },
                    modifier = Modifier
                        .width(IntrinsicSize.Min)
                        .height(60.dp.computeSize())
                        .testTag("Sprint8:SetupWorkout:Button"),
                    buttonColor = DynamicTheme.colors.primary,
                    buttonTitle = R.string.setup_workout_tc,
                    btnTextColor = DynamicTheme.colors.background1,
                    btnShape = RoundedCornerShape(18.dp.computeSize()),
                    btnTextStyle = MaterialTheme.typography.displayLarge
                )
            }
        )
    }

    override fun startWorkout(workoutInputs: List<WorkoutInputUi>) {
        // could maybe move sprint8 specific setup stuff here?
        workoutModel.workoutAppSetupBundle.value = appSetupBundle
        // do we callback to system or just start workout through interfacing?
        workoutLauncherModel.launchSelectedWorkoutWithSafetyCheck(workoutInputs)
    }

    internal fun startSprint8CustomWorkout(speedOrResistanceData: List<Float>, inclineData: List<Float>) {
        workoutLauncherModel.checkLaunchWorkoutPin { proceed ->
            if (proceed) catalogModel.startCustomSprint8Workout(speedOrResistanceData, inclineData)
        }
    }

    override fun onLoad(workoutInputs: List<WorkoutInputUi>) {
        // nothing to do
    }
    override fun onUnload() {
        catalogModel.close()
        deviceModel.close()
        workoutLauncherModel.close()
        workoutModel.close()
    }
}
