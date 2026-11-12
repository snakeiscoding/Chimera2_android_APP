package com.jht.spinbikeapps.targettraining.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.GRAY_GRADIENT_2
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.GRAY_GRADIENT_3
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

@Composable
internal fun TargetTrainingSetupCard(
    title: Int,
    description: Int,
    image: Int,
    onClick: () -> Unit
) {
    val componentTag = "TTSetupCard"
    Card(
        modifier = Modifier
            .padding(horizontal = 8.dp.computeSize(1920f))
            .width(324.dp.computeSize(1920f))
            .wrapContentHeight().semantics {
                testTagsAsResourceId = true
            },
        shape = RoundedCornerShape(32.dp.computeSize(1920f)),
        border = BorderStroke(width = 4.dp.computeSize(1920f), color = GRAY_GRADIENT_3)
    ) {
        Column(
            modifier = Modifier
                .clickable { onClick() }
                .testTag("$componentTag:TargetSelect:Column")
                .wrapContentSize()
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            GRAY_GRADIENT_3,
                            GRAY_GRADIENT_2
                        )
                    )
                )
                .padding(
                    horizontal = 20.dp.computeSize(1920f),
                    vertical = 64.dp.computeSize(1920f)
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(image),
                contentDescription = null,
                modifier = Modifier.size(150.dp.computeSize(1920f))
                    .testTag("$componentTag:SetupImage:Image")
            )
            Text(
                stringResource(title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 32.sp.computeSize(1920f),
                ),
                color = DynamicTheme.colors.gray18,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp.computeSize(1920f))
            )
            Text(
                stringResource(description),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 18.sp.computeSize(1920f),
                ),
                color = DynamicTheme.colors.text1.copy(0.87f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp.computeSize(1920f))
            )
        }
    }
}

@AllDevicePreviews
@Composable
internal fun TargetTrainingSetupCardPreview(){
    TargetTrainingSetupCard(
        title = R.string.title_tc,
        description = R.string.description_tc,
        image = R.drawable.ic_logo_matrix
    ) { }
}