package com.jht.spinbikeapps.targettraining.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jht.spinbikeapps.R
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.widgets.HomeButton
import com.jht.widgets.fillScreenHeight

@Composable
internal fun TargetTrainingTopBar(
    modifier: Modifier = Modifier,
    navController: NavController? = null,
    onHome: () -> Unit,
) {
    val componentTag = "TTTopBannerComp"
    Box(
        Modifier
            .fillMaxWidth()
            .fillScreenHeight(0.1f)
            .background(DynamicTheme.colors.panelTopBackground.brush())
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {}
                )
            }
            .semantics {
                testTagsAsResourceId = true
            },
    ) {
        Row(
            Modifier
                .align(Alignment.CenterStart)
                .padding(30.dp, 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeButton(Modifier.aspectRatio(120 / 70f)) {
                onHome()
            }
        }
        Image(
            painter = painterResource(R.drawable.ic_logo_matrix),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.6f)
                .testTag("$componentTag:TopBannerImage:Image")
        )
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            thickness = 1.dp.computeSize(),
            color = Color.DarkGray
        )
    }
}

@AllDevicePreviews
@Composable
private fun TargetTrainingTopBarPreview(){
    TargetTrainingTopBar(){}
}