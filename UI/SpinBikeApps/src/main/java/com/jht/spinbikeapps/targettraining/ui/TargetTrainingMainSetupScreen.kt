package com.jht.spinbikeapps.targettraining.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.common.TargetTrainingSetupCard
import com.jht.spinbikeapps.targettraining.model.TargetTrainingSetupData
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

@Composable
internal fun TargetTrainingMainSetupScreen(
    navController: NavController? = null,
    targetTrainingSetupData: List<TargetTrainingSetupData>,
) {
    val componentTag = "TTMainSetupScreen"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures{}
            }.semantics {
                testTagsAsResourceId = true
            }
            .background(DynamicTheme.colors.gray1),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.select_your_primary_target_tc),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 32.sp.computeSize(1920f),
                fontWeight = FontWeight.Medium
            ),
            color = DynamicTheme.colors.gray18,
            modifier = Modifier.padding(top = 120.dp.computeSize(1920f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 200.dp.computeSize(1920f))
                .testTag("$componentTag:SetupCard:Row"),
            horizontalArrangement = Arrangement.Center,
        ) {
            targetTrainingSetupData.forEach {
                TargetTrainingSetupCard(
                    it.title, it.description, it.image,
                    onClick = {
                        navController?.navigate(it.route)
                    })
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun TargetTrainingMainSetupScreenPreview(){
    TargetTrainingMainSetupScreen(navController  = null,
    targetTrainingSetupData = listOf(
    TargetTrainingSetupData(
        R.string.rpm_uc,
        R.string.rpm_description_pg,
        R.drawable.ic_rpm_meter,
        TargetTrainingScreen.RpmScreen.route
    ),
    TargetTrainingSetupData(
        R.string.watts_tc,
        R.string.watts_description_pg,
        R.drawable.ic_lightning,
        TargetTrainingScreen.WattsScreen.route

    ),
    TargetTrainingSetupData(
        R.string.heart_rate_tc,
        R.string.heart_rate_description_pg,
        R.drawable.ic_heart,
        TargetTrainingScreen.HeartRateScreen.route
    ),
    TargetTrainingSetupData(
        R.string.calories_tc,
        R.string.calories_description_pg,
        R.drawable.ic_fire_tt,
        TargetTrainingScreen.CaloriesScreen.route
    ),
    TargetTrainingSetupData(
        R.string.distance_tc,
        R.string.distance_description_pg,
        R.drawable.ic_distance_green,
        TargetTrainingScreen.DistanceScreen.route
    )
    ))
}