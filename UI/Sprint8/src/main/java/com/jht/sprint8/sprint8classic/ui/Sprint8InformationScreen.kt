package com.jht.sprint8.sprint8classic.ui
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.jht.sprint8.R
import com.jht.sprint8.Screen
import com.jht.sprint8.sprint8classic.components.Sprint8InformationComp
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BG_2
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

/**
 * Composable screen that displays information about the Sprint8 workout program.
 * This screen shows details about Sprint8 exercises.
 * @param navController Optional [NavController] used for navigation between screens.
 * @param previousRoute Optional name of the previous route to navigate back to.
 */
@Composable
internal fun Sprint8InfoScreen(
    navController: NavController? = null,
    previousRoute: String? = null
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val showCrossButton = previousRoute in listOf(
        Screen.Sprint8ExperienceScreen.route,
        Screen.Sprint8PreviewScreen.route,
        Screen.AssessmentLevelSelectionScreen.route,
        Screen.AssessmentInformationScreen.route
    )
    val showBackButton = previousRoute == Screen.Sprint8WorkoutSelectionScreen.route
    Box(
        modifier = Modifier
            .wrapContentSize()
            .background(
               Color.Transparent,
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        println("Tapped outside")
                    }
                )
            }
    ) {
        if (showCrossButton) {
            IconButton(
                onClick = {
                    navController?.popBackStack()
                }, modifier = Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(1f)
                    .padding(end = screenWidth * 0.14f, top = screenHeight * 0.101f)
                    .size(24.dp.computeSize())
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_round_close),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = screenWidth * 0.141f, vertical = screenHeight * 0.101f)
                .border(1.dp.computeSize(), Color.Gray.copy(alpha = 0.4f), shape = RoundedCornerShape(30.dp.computeSize()))
                .clip(RoundedCornerShape(30.dp.computeSize()))
                .background(
                    DynamicTheme.colors.onSecondary,
                    shape = RoundedCornerShape(12.dp.computeSize())
                )
        ) {
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.CenterEnd)
                    .padding(end = screenHeight * 0.088f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(top = screenHeight * 0.140f, bottom = screenHeight * 0.04f),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    HorizontalDivider(
                        modifier = Modifier
                            .height(2.dp.computeSize())
                            .width(screenWidth * 0.024f)
                            .padding(end = 2.dp.computeSize())
                            .align(Alignment.TopEnd),
                        color = DynamicTheme.colors.text1
                    )
                    VerticalDivider(
                        modifier = Modifier
                            .height(screenHeight * 0.65f)
                            .padding(bottom = 2.dp.computeSize() )
                            .width(2.dp.computeSize()),
                        color = DynamicTheme.colors.text1
                    )
                    HorizontalDivider(
                        modifier = Modifier
                            .height(2.dp.computeSize())
                            .width(screenWidth * 0.024f)
                            .padding(end = 2.dp.computeSize())
                            .align(Alignment.BottomCenter),
                        color = DynamicTheme.colors.text1
                    )
                }
                Column(
                    modifier = Modifier.padding(start = screenWidth * 0.012f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_repeat),
                        contentDescription = null,
                        modifier = Modifier
                    )
                    Text(
                        stringResource(R.string.repeat_8_x),
                        fontSize = 20.sp.computeSize(),
                        color = Color.White,
                        fontStyle = FontStyle.Normal,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
            if (showBackButton) {
                Row(
                    modifier = Modifier
                        .clickable { navController?.popBackStack() }
                        .wrapContentSize()
                        .padding(start = screenWidth * 0.018f, top = screenHeight * 0.018f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(painterResource(R.drawable.ic_back), contentDescription = null)
                    Text(
                        stringResource(R.string.back_tc),
                        fontSize = 22.sp.computeSize(),
                        color = DynamicTheme.colors.text1,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.SansSerif

                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = screenHeight * 0.1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.how_does_sprint8_work_tc),
                    fontSize = 32.sp.computeSize(),
                    color = DynamicTheme.colors.text1,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Normal,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = screenHeight * 0.050f)
                )
                Sprint8InformationComp(
                    icon = R.drawable.ic_run,
                    title = stringResource(R.string.sprint8_tc),
                    subTitle = stringResource(R.string.sprint_interval_info_pg),
                    borderColor = DynamicTheme.colors.background6,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )
                Spacer(modifier = Modifier.size(26.dp.computeSize()))
                Sprint8InformationComp(
                    icon = R.drawable.ic_sweat,
                    title = stringResource(com.jht.sprint8.R.string.sweat_score_tc),
                    subTitle = stringResource(R.string.sweat_score_info_pg),
                    borderColor = DynamicTheme.colors.text1,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    iconColor = SPRINT8_BG_2
                )
                Spacer(modifier = Modifier.size(26.dp.computeSize()))
                Sprint8InformationComp(
                    icon = R.drawable.ic_walk,
                    title = stringResource(com.jht.sprint8.R.string.active_recover_tc),
                    subTitle = stringResource(R.string.active_recovery_info_pg),
                    borderColor = DynamicTheme.colors.background5,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )

            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun Sprint8InfoScreenPreview(){
    Sprint8InfoScreen(
        navController = NavController(context = LocalContext.current)
    )
}