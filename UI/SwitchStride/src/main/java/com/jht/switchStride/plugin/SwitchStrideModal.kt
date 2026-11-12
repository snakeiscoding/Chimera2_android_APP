package com.jht.switchStride.plugin

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.data.WorkoutInputUi
import com.jht.mock.interfaces.models.MockWorkoutCatalogModel
import com.jht.switchStride.R
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.widgets.GlideAndPreviewImage
import com.jht.widgets.PillboxGrid
import com.jht.widgets.StartWorkoutButton

// not sure why switch stride is using sprint8 constants, perhaps something that should be in theme
private val sprint8Background8 = Color(0xFF4c4c4c)
private val hbtButtonColor = Color(0xFFbb904a)

@Composable
internal fun SwitchStrideModal(
    modifier: Modifier = Modifier,
    title: String,
    imageUri: Uri,
    description: String,
    workoutInputList: List<WorkoutInputUi>,
    onClose: () -> Unit = {},
    startSelectedWorkout: () -> Unit,
) {
    val baseTag = "SwitchStrideSetupModal"
    Row(
        // We use the passed-in modifier directly, allowing the parent container to control the modal
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(2f)
        ) {
            GlideAndPreviewImage(
                modifier = Modifier
                    .fillMaxSize(),
                uri = imageUri,
                contentScale = ContentScale.FillHeight
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(3.2f)
                .padding(
                    start = 24.dp.computeSize(),
                    end = 24.dp.computeSize(),
                ),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.Start
        ) {
            Image(
                imageVector = Icons.Default.Close, // this was using R.drawable.ic_cross_x from sprint8
                contentDescription = "Close",
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.End)
                    .clickable {onClose()}
            )
            Text(
                text = title,
                color = DynamicTheme.colors.text1,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.testTag("$baseTag:SelectedWorkoutTitle:Text"),
            )
            Text(
                text = description,
                color = DynamicTheme.colors.text1,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.testTag("$baseTag:DescriptionTitle:Text")
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp.computeSize()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_switch_stride),
                    contentDescription = "Switch Stride Icon",
                    modifier = Modifier
                        .size(25.dp.computeSize())
                        .testTag("$baseTag:SwitchStrideIcon:Image")
                )
                Text(
                    text = stringResource(id = R.string.switch_stride_tc),
                    color = DynamicTheme.colors.text1,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.testTag("$baseTag:SwitchStrideLabel:Text")
                )
            }

            PillboxGrid(
                modifier = Modifier
                    .height(80.dp.computeSize()),
                workoutInputList = workoutInputList,
                rows = 1,
                columns = 2
            )

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp.computeSize(),
                color = sprint8Background8
            )
            Text(
                stringResource(id = R.string.switch_stride_pg),
                fontSize = 16.sp.computeSize(),
                fontFamily = FontFamily.SansSerif,
                fontStyle = FontStyle.Italic,
                color = DynamicTheme.colors.text1,
            )

            Column(
                modifier = Modifier.wrapContentSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp.computeSize())
            ) {
                Row(
                    modifier = Modifier.wrapContentSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        stringResource(R.string.hbt_start_speed_tc),
                        color = DynamicTheme.colors.text1,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        stringResource(R.string.hbt_start_speed_value),
                        color = DynamicTheme.colors.text1,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Row(
                    modifier = Modifier.wrapContentSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        stringResource(R.string.hbt_start_incline_tc),
                        color = DynamicTheme.colors.text1,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        stringResource(R.string.hbt_start_incline_value),
                        color = DynamicTheme.colors.text1,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                StartWorkoutButton(
                    onClick = {
                        startSelectedWorkout()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(IntrinsicSize.Min)
                        .height(48.dp.computeSize()),
                    buttonColor = hbtButtonColor,
                    buttonTitle = R.string.start_workout_tc,
                    btnTextColor = DynamicTheme.colors.background1,
                    btnShape = MaterialTheme.shapes.medium,
                    btnTextStyle = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun SwitchStrideModalPreview(){
    SwitchStrideModal(
        title = "Title",
        imageUri = Uri.EMPTY,
        description = "Description",
        workoutInputList = MockWorkoutCatalogModel().getWorkoutInputsForPreview()
    ) { }
}

