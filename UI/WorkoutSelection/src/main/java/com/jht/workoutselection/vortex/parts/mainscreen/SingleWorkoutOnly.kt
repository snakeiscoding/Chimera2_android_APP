package com.jht.workoutselection.vortex.parts.mainscreen

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.data.WorkoutItem
import com.jht.resources.robotoFonts
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.ui.computeSize
import com.jht.widgets.FocusCropImage
import com.jht.workoutselection.R

@Composable
fun SingleWorkoutOnly(
    currentItem: WorkoutItem,
    onSelectWorkoutItem: (workoutItem: WorkoutItem) -> Unit = {},
) {

    Box(modifier = Modifier.fillMaxSize()) {
        FocusCropImage(
            modifier = Modifier
                .fillMaxSize(),
            model = currentItem.background.firstOrNull() ?: Uri.EMPTY,
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.93f)
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(592.dp.computeSize(1920f))
                    .height(432.dp.computeSize(1920f))
                    .clip(RoundedCornerShape(18.dp.computeSize()))
                    .background(Color.Black.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 40.dp.computeSize(1920f),
                        vertical = 28.dp.computeSize(1920f)
                    )
                ) {
                    Text(
                        text = stringResource(currentItem.titleId),
                        fontSize = 42.sp.computeSize(1920f),
                        fontFamily = robotoFonts,
                        color = DynamicTheme.colors.gray15
                    )
                    Spacer(modifier = Modifier.padding(12.dp.computeSize(1920f)))

                    Text(
                        text = stringResource(currentItem.descriptionId),
                        fontSize = 20.sp.computeSize(1920f),
                        fontFamily = robotoFonts,
                        color = DynamicTheme.colors.gray15
                    )
                    Spacer(modifier = Modifier.padding(12.dp.computeSize(1920f)))

                    ThemeButton(
                        onClick = { onSelectWorkoutItem(currentItem) },
                        modifier = Modifier
                            .wrapContentWidth()
                            .testTag("WorkoutSelect:Setup:Button"),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(0.dp, Color.Transparent),
                        colors = ButtonDefaults.buttonColors().copy(
                            containerColor = DynamicTheme.colors.buttonPrimaryDefault
                        ),
                        contentPadding = PaddingValues(
                            horizontal = 20.dp.computeSize(),
                            vertical = 6.dp.computeSize()
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.setup_workout_tc),
                            style = DynamicTheme.typography.textStyle1.copy(
                                fontWeight = FontWeight.Bold,
                                color = DynamicTheme.colors.onPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}