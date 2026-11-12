package com.jht.progression.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.constraintlayout.compose.ConstraintLayout
import com.jht.enums.ProgramType
import com.jht.progression.R
import com.jht.progression.data.Baseline15Min
import com.jht.progression.data.Program
import com.jht.progression.data.program.program5k.Program5k
import com.jht.progression.oxaniumStyle
import com.jht.progression.ui.widgets.MmDoubleArrowsRight
import com.jht.progression.ui.widgets.MmSetupFrame
import com.jht.progression.ui.widgets.MmThemedButton
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.scaleSize
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * This screen is for selecting between different workout programs
 */
@Composable
fun ProgramSelectScreen(
    programs: List<Program>,
    navigateToSplash: () -> Unit,
    onStartPressed: (programType: ProgramType) -> Unit
) {
    MmSetupFrame(
        topBarColor = Color.Transparent,
        backgroundId = R.drawable.mm_stars_bg,
        onBackPressed = navigateToSplash,
        backIcon = ImageVector.vectorResource(id = R.drawable.ic_home)
    ) {
        WelcomeScreenBody(
            programs = programs,
            onStartPressed = onStartPressed
        )
    }
}

@Composable
fun WelcomeScreenBody(
    programs: List<Program>,
    onStartPressed: (programType: ProgramType) -> Unit
) {
    val pagerState = rememberPagerState { programs.size }
    var currentProgramIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            currentProgramIndex = page
        }
    }

    if (programs.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("Program Select Screen"),
            horizontalAlignment = CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            ConstraintLayout(modifier = Modifier.fillMaxSize()) {
                val (title, image, button) = createRefs()
                Column(
                    horizontalAlignment = CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .constrainAs(title) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                ) {
                    Text(
                        text = stringResource(id = R.string.choose_a_training_program_tc).uppercase(),
                        fontSize = 36.sp.scaleSize(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = oxaniumStyle,
                        modifier = Modifier
                            .testTag("choose_a_training_program")
                            .padding(top = 30.dp.scaleSize(), bottom = 30.dp.scaleSize())
                    )

                    //This will be the distance value for the race, the current program is 5k.
                    val distanceValue = stringResource(id = programs[currentProgramIndex].nameId)
                    Text(
                        text = stringResource(
                            id = R.string.named_race_tc,
                            distanceValue
                        ).uppercase(),
                        color = Color.White,
                        fontSize = 48.sp.scaleSize(),
                        fontWeight = FontWeight.SemiBold,
                        style = oxaniumStyle,
                        modifier = Modifier
                            .testTag("program_race")
                    )
                    Text(
                        text = stringResource(id = R.string.eight_week_program_tc).uppercase(),
                        color = Color.White,
                        fontSize = 21.sp.scaleSize(),
                        fontWeight = FontWeight.Medium,
                        style = oxaniumStyle,
                        modifier = Modifier
                            .testTag("program_weeks"),
                        overflow = TextOverflow.Ellipsis
                    )

                }

                Box(
                    Modifier
                        .constrainAs(image) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                        .padding(top = 175.dp.scaleSize())
                ) {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 475.dp.scaleSize()),
                        verticalAlignment = Alignment.Top
                    ) { page ->
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    val pageOffset = ((pagerState.currentPage - page)
                                            + pagerState.currentPageOffsetFraction).absoluteValue
                                    alpha = lerp(
                                        start = 0.4f,
                                        stop = 1f,
                                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                    )
                                    scaleX = lerp(
                                        start = 0.5f,
                                        stop = 1f,
                                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                    )
                                    scaleY = lerp(
                                        start = 0.5f,
                                        stop = 1f,
                                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                    )
                                },
                            contentAlignment = Center
                        ) {
                            Text(
                                text = stringResource(id = programs[page].nameId),
                                modifier = Modifier.testTag("program_name"),
                                color = programs[page].color,
                                fontSize = 100.sp.scaleSize(),
                                fontWeight = FontWeight.Bold,
                                style = oxaniumStyle
                            )
                            Image(
                                painter = painterResource(
                                    id =
                                    if (page == currentProgramIndex) {
                                        R.drawable.hologram_on
                                    } else {
                                        R.drawable.hologram_off
                                    }
                                ),
                                contentDescription = "hologram",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .testTag("program_image")
                                    .fillMaxWidth()
                                    .padding(top = if (page == currentProgramIndex) 0.dp.scaleSize() else 180.dp.scaleSize())
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(.4f)
                            .align(Center)
                            .offset(y = 25.dp.scaleSize())
                    ) {
                        val coroutineScope = rememberCoroutineScope()

                        //Left Arrows
                        MmDoubleArrowsRight(
                            modifier = Modifier
                                .size(50.dp.scaleSize())
                                .rotate(180f)
                                .align(CenterStart)
                                .graphicsLayer {
                                    val pageOffset = ((pagerState.currentPage)
                                            + pagerState.currentPageOffsetFraction).absoluteValue
                                    alpha = lerp(
                                        start = 0f,
                                        stop = 1f,
                                        fraction = pageOffset.coerceIn(0f, 1f)
                                    )
                                }
                        ) {
                            coroutineScope.launch {
                                if (currentProgramIndex > 0) {
                                    pagerState.animateScrollToPage(--currentProgramIndex)
                                }
                            }
                        }

                        //Right Arrows
                        MmDoubleArrowsRight(
                            modifier = Modifier
                                .size(50.dp.scaleSize())
                                .align(CenterEnd)
                                .graphicsLayer {
                                    val pageOffset = ((programs.size - 1 - pagerState.currentPage)
                                            + pagerState.currentPageOffsetFraction).absoluteValue
                                    alpha = lerp(
                                        start = 0f,
                                        stop = 1f,
                                        fraction = pageOffset.coerceIn(0f, 1f)
                                    )
                                }
                        ) {
                            coroutineScope.launch {
                                if (currentProgramIndex < programs.size - 1) {
                                    pagerState.animateScrollToPage(++currentProgramIndex)
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .constrainAs(button) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                        .padding(top = 540.dp.scaleSize()),
                    contentAlignment = Center
                ) {
                    MmThemedButton(
                        text = stringResource(R.string.start_uc),
                        onClick = { onStartPressed(programs[currentProgramIndex].programType) },
                        color = MmColors.futuristicBlue,
                        modifier = Modifier.testTag("start_button")

                    )
                }
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun PreviewMmProgramSelectScreen() {
    ProgramSelectScreen(
        programs = listOf(
            Program(
                programType = ProgramType.Program5k,
                nameId = R.string.five_k_tc,
                workoutInterface = Program5k(),
                baselineInterface = Baseline15Min(),
                color = MmColors.futuristicBlue,
            ),
            Program(
                programType = ProgramType.Program10k,
                nameId = R.string.ten_k_tc,
                workoutInterface = Program5k(),
                baselineInterface = Baseline15Min(),
                color = Color(0xFFEA8115),
            )
        ),
        navigateToSplash = {},
        onStartPressed = {}
    )
}
