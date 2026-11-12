package com.jht.switchStride.plugin

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.jht.data.BaseFeaturedWorkoutPlugin
import com.jht.data.WorkoutInputUi
import com.jht.data.WorkoutItem
import com.jht.interfaces.models.WorkoutLauncherModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.ui.clickableNoEffect
import com.jht.widgets.helpers.getInjectionEntryPoint

/** Featured workout plugin for Switch Stride workout app */
@SuppressLint("MutableCollectionMutableState")
class SwitchStrideFeaturePlugin(
    appContext: Context,
    workoutItem: WorkoutItem,
) : BaseFeaturedWorkoutPlugin(appContext, workoutItem) {
    private val workoutModel: WorkoutModel
    private val workoutLauncherModel: WorkoutLauncherModel

    init {
        val injection = getInjectionEntryPoint(appContext)
        workoutModel = injection.workout()
        workoutLauncherModel = injection.workoutLauncher()
    }

    @Composable
    override fun SetupScreen(
        workoutInputs: List<WorkoutInputUi>,
        onDismissRequest: () -> Unit,
    ) {
        SwitchStrideModal(
            modifier = Modifier
                .fillMaxWidth(1210 / 1920f)
                .aspectRatio(1210 / 786f)
                .clip(RoundedCornerShape(30.dp))
                .background(Color.Black)
                .clickableNoEffect { /* do nothing: prevent clicks inside from dismissing the modal */ }
                .semantics { testTagsAsResourceId = true },
            title = stringResource(workoutItem.titleId),
            imageUri = workoutItem.background.firstOrNull() ?: Uri.EMPTY,
            description = stringResource(workoutItem.descriptionId),
            workoutInputList = workoutInputs,
            startSelectedWorkout = { startWorkout(workoutInputs) },
            onClose = onDismissRequest,
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
        workoutLauncherModel.close()
        workoutModel.close()
    }
}
