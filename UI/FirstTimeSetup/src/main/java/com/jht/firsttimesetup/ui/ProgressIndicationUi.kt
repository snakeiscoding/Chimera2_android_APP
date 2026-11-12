package com.jht.firsttimesetup.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.jht.firsttimesetup.R
import com.jht.ui.Device10inPreviews
import com.jht.ui.Device15inPreviews
import com.jht.ui.Device22inPreviews
import com.jht.ui.computeSize
import com.jht.widgets.BrandLogo
import com.jht.widgets.numpad.NumPadDisplay
import com.jht.widgets.NumPadEntryPopup

/**
 * Segment states
 */
internal enum class SegmentState {
    COMPLETED, // user has completed this part of FTSU
    SELECTED, // current segment
    UNSELECTED // segment of FTSU user has yet to complete
}

interface SegmentType {
    val nameResourceId : Int
}

/**
 * Enum for all the possible states we wish to support for FTSU
 *
 * @param nameResourceId is used for a translated string to display in relation to the State
 */
enum class SetupState(override val nameResourceId: Int) : SegmentType {
    LANGUAGE(R.string.language_tc),
    NETWORK(R.string.network_tc),
    FACILITY_ID(R.string.club_id_tc),
    SETTINGS(R.string.settings_tc),
    TV_SETUP(R.string.tv_setup_tc),

    //setting subsegments
    GENERAL_SETTINGS(R.string.general_settings_tc),
    TV_SETUP_TUNER(R.string.tv_setup_tc),
    TV_SETUP_IMPORT_CHANNELS(R.string.tv_setup_tc),
    TV_SETUP_EDIT_CHANNELS(R.string.tv_setup_tc),
    //end of subsegments

    MACHINE_CALIBRATION(R.string.machine_calibration_tc),
    SUMMARY(R.string.summary_tc)
}


/**
 * Segment represents a part of the FTSU process
 */
internal data class Segment(
    val type: SegmentType,
    var state: SegmentState = SegmentState.UNSELECTED,
    var subsegments: List<Segment> = emptyList(),
    var currentSubsegmentIndex: Int = 0,
    var parent: Segment? = null,
    val showInProgressBar: Boolean = true,
)

@Composable
internal fun FTSUProgressbar(
    modifier: Modifier = Modifier,
    segments: List<Segment>,
    currentSegmentIndex: Int = 0,
    isEngineeringMode: Boolean = false,
    onSegmentClicked: (Int) -> Unit = {  },
) {

    Row(
        modifier = modifier
            .wrapContentSize()
            .testTag("FTSU:ProgressBar:Image")
    ) {
        segments.forEachIndexed { index, segment ->

            FTSUProgressSegment(
                showLeftLine = segment != segments.first(),
                showRightLine = segment != segments.last(),
                text = stringResource(segment.type.nameResourceId),
                state = segment.state,
                modifier = Modifier
                    .weight(1f)
                    .testTag(if (segment.state == SegmentState.COMPLETED) "FTSU:ProgressSegment:Completed:${index}:Image}" else "FTSU:ProgressSegment:Uncompleted:${index}:Image"),
                subsegments = segment.subsegments,
                currentSubIndex = segment.currentSubsegmentIndex,
                showSubsegments = index == currentSegmentIndex,
                isEngineeringMode = isEngineeringMode,
                onSegmentClicked = { onSegmentClicked(index) },
            )

        }
    }
}

/**
 * Represents a segment of the FTSU setup process
 * @param [text] title of current segment
 * @param [state] represents whether the user is currently 1) at this step in FTSU 2) completed step 3) has not reached this step
 * @param [showLeftLine] left line is the bridge between each segment and its left neighbor only the first segment should not show left line
 * @param [showRightLine] right line is the bridge between each segment and its right neighbor only the last segment should not show right line
 * @param [subsegments] subsegments to keep track of inner state within a [Segment]
 */
@Composable
private fun FTSUProgressSegment(
    modifier: Modifier = Modifier,
    text: String = "",
    fontSize: TextUnit = MaterialTheme.typography.bodyMedium.fontSize,
    state: SegmentState = SegmentState.SELECTED,
    showLeftLine: Boolean = false, //used to determine whether we show a line to the left of segment (first doesn't need )
    showRightLine: Boolean = false, // used to determine whether we show a line to the right of segment (last doesn't need)
    showSubsegments: Boolean = false, // we only want to show show subsegments when user is on a segment that contains subsegments
    subsegments: List<Segment>? = null, // some segments may have subsegments
    currentSubIndex: Int = 0,
    isEngineeringMode: Boolean = false,
    onSegmentClicked: () -> Unit = {  },
) {
    val segmentLineColor = colorResource(R.color.lineColor)
    val selectedColor = colorResource(R.color.selected_color)
    val unselectedColor = colorResource(R.color.unselected_color)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp)
            .clickable(
                enabled = isEngineeringMode,
                onClick = onSegmentClicked
            ),
    ) {

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxHeight(.33333f)
                .fillMaxWidth(),
        ) {


            Canvas(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(.5f)
            ) {
                drawLine(
                    color = if (showLeftLine) {
                        segmentLineColor
                    } else {
                        Color.Transparent
                    },
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx()
                )
            }

            Canvas(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(.5f)
            ) {
                drawLine(
                    color = if (showRightLine) segmentLineColor else Color.Transparent,
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx()
                )
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                verticalArrangement = Arrangement.Center
            ) {

                Box(
                    modifier = Modifier
                        .padding(0.dp)
                        .size(
                            if (state == SegmentState.UNSELECTED) {
                                //size of unselected segment is smaller indicating the state is completed or yet to be completed
                                15.dp
                            } else {
                                //size of current FTSU segment is bigger than completed/ unselected segments
                                25.dp
                            }
                        )
                        .drawBehind {
                            drawCircle(
                                color = if (state == SegmentState.SELECTED) {
                                    selectedColor
                                } else {
                                    unselectedColor
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                    content = {
                        if (state == SegmentState.COMPLETED) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Check for completed steps in segment",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                )

            }
        }

        Spacer(Modifier.height(16.dp.computeSize()))

        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .then(
                    if (isEngineeringMode)                //if in engineering mode then have border to show that the progressSegments are clickable
                        Modifier
                            .align(Alignment.Center)
                            .border(                                //else do nothing
                                3.dp,
                                color = ButtonDefaults.buttonColors().containerColor,
                                shape = RoundedCornerShape(5.dp)
                            )
                            .padding(5.dp)
                    else Modifier
                ),
            text = text,
            fontSize = if (state == SegmentState.SELECTED) MaterialTheme.typography.labelLarge.fontSize else fontSize,
            color = if (state == SegmentState.SELECTED) {
                selectedColor
            } else {
                unselectedColor
            },
            maxLines = 2,
            textAlign = TextAlign.Center
        )

        if (showSubsegments) {
            subsegments?.let {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {

                    subsegments.forEachIndexed { index, segment ->
                        Box(
                            modifier = Modifier
                                .testTag("FTSU:ProgressSubSegment:${segment.type}:Image")
                                .padding(0.dp)
                                .size(9.dp)
                                .drawBehind {
                                    drawCircle(
                                        color = if (index == currentSubIndex) {
                                            selectedColor
                                        } else {
                                            unselectedColor
                                        },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                            content = {}
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun SubsegmentPreview() {
    val subsegments = listOf(
        Segment(type = SetupState.GENERAL_SETTINGS, state = SegmentState.SELECTED),
        Segment(type = SetupState.TV_SETUP_TUNER, state = SegmentState.UNSELECTED),
    )
    FTSUProgressSegment(
        modifier = Modifier.height(500.dp),
        text = "Language",
        showLeftLine = true,
        showRightLine = true,
        subsegments = subsegments,
        showSubsegments = true
    )
}

@Preview
@Composable
private fun SelectedCircularIndicatorPreview() {
    FTSUProgressSegment(
        modifier = Modifier
            .height(500.dp)
            .width(200.dp),
        text = "Language",
        state = SegmentState.SELECTED
    )
}


@Preview
@Composable
private fun CompletedCircularIndicatorPreview() {
    FTSUProgressSegment(
        modifier = Modifier.height(500.dp),
        text = "Language",
        state = SegmentState.COMPLETED
    )
}

@Preview
@Composable
private fun UnselectedCircularIndicatorPreview() {
    FTSUProgressSegment(
        modifier = Modifier.height(500.dp),
        text = "Language",
        state = SegmentState.UNSELECTED
    )
}


@Preview
@Composable
private fun SelectedShowLeftCircularIndicatorPreview() {
    FTSUProgressSegment(
        modifier = Modifier.height(500.dp),
        text = "Language",
        showLeftLine = true
    )
}

@Preview
@Composable
private fun SelectedShowRightCircularIndicatorPreview() {
    FTSUProgressSegment(
        modifier = Modifier.height(500.dp),
        text = "Language",
        showRightLine = true
    )
}

@Preview
@Composable
private fun SelectedShowLeftAndRightCircularIndicatorPreview() {
    FTSUProgressSegment(
        modifier = Modifier.height(500.dp),
        text = "Language",
        showLeftLine = true,
        showRightLine = true,
    )
}

@Preview
@Composable
private fun FTSUProgressbarPreview() {

    val segments = listOf(
        Segment(
            type = SetupState.LANGUAGE,
            state = SegmentState.COMPLETED
        ),

        Segment(
            type = SetupState.NETWORK,
            state = SegmentState.SELECTED
        ),
        Segment(
            type = SetupState.FACILITY_ID,
            state = SegmentState.UNSELECTED
        ),
        Segment(
            type = SetupState.MACHINE_CALIBRATION,
            state = SegmentState.UNSELECTED
        )
    )

    FTSUProgressbar(
        segments = segments, modifier = Modifier
            .width(800.dp)
            .height(500.dp)
    )

}

@Composable
internal fun FirstTimeSetupTopAppBar(
    modifier: Modifier = Modifier,
    segments: List<Segment>,
    currentSegmentIndex: Int = 0,
    isEngineeringMode: Boolean = false,
    onMESCodeEnter: (code: Float) -> Boolean = { true },
    showProgressBar: Boolean = true,
    onSegmentClicked: (Int) -> Unit = {},
) {

    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var showMesKeypad by remember { mutableStateOf(false) }

        Spacer(Modifier.size(64.dp))

        BrandLogo(
            modifier = Modifier
                .fillMaxWidth(1 / 8f)
                .fillMaxHeight(.5f),
            onLongPress = { showMesKeypad = true }
        )

        Spacer(Modifier.size(32.dp))

        if (showMesKeypad) {
            NumPadEntryPopup(
                numPadValue = -1f,
                displayType = NumPadDisplay.Passcode,
                onCommit = { code ->
                    //If valid code is entered function returns true
                    //So on valid code enter the keypad will be hidden
                    showMesKeypad = onMESCodeEnter(code).not()
                },
                onDismiss = { showMesKeypad = false },
                displayNumberWithoutUserInput = false
            )
        }

        if (showProgressBar) {
            FTSUProgressbar(
                segments = segments,
                modifier = Modifier.fillMaxWidth(.8f),
                currentSegmentIndex = currentSegmentIndex,
                isEngineeringMode = isEngineeringMode,
                onSegmentClicked = onSegmentClicked,
            )
        }
    }
}

@Device10inPreviews
@Device15inPreviews
@Device22inPreviews
@Composable
private fun FirstTimeSetupTopAppBarPreview() {
    val segments = listOf(
        Segment(
            type = SetupState.LANGUAGE,
            state = SegmentState.SELECTED
        ),

        Segment(
            type = SetupState.NETWORK,
            state = SegmentState.UNSELECTED
        ),
        Segment(
            type = SetupState.FACILITY_ID,
            state = SegmentState.UNSELECTED
        ),
        Segment(
            type = SetupState.MACHINE_CALIBRATION,
            state = SegmentState.UNSELECTED
        )
    )


    FirstTimeSetupTopAppBar(
        segments = segments,
    )
}
