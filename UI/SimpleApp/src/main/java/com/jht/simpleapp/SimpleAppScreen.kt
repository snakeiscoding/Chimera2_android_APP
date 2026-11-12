package com.jht.simpleapp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.jht.resources.robotoFonts
import com.jht.theme.DynamicTheme
import com.jht.theme.DynamicThemeData
import com.jht.theme.darkDynamicTheme
import com.jht.theme.defaultDynamicTheme
import com.jht.ui.Device10inPreviews
import com.jht.ui.Device15inPreviews
import com.jht.ui.Device22inPreviews
import com.jht.ui.computeSize
import com.jht.widgets.FixedHeightAutoSizeText
import com.jht.widgets.PagerJumpingDotIndicator
import com.jht.widgets.helpers.StatInfo
import com.jht.widgets.helpers.StatInfoCategory
import com.jht.widgets.helpers.StatInfoCollection

const val testTagLargeCard = "LargeCard"
const val testTagSmallCard = "SmallCard" // This one is passed and modified for other tags
const val testTagGoalButtonMinus = "GoalButtonMinus"
const val testTagGoalButtonPlus = "GoalButtonPlus"
const val testTagLargeCardStat = "LargeCardStat" // This one is passed and modified for other tags
const val testTagFullscreenButton = "FullscreenBtn"
const val testTagLeft = "Left"
const val testTagRight = "Right"
const val testTagPage = "Page"

private val statTextStyle: TextStyle = TextStyle(
    fontFamily = robotoFonts,
    fontWeight = FontWeight.Bold,
    color = Color.White,
    fontSize = 16.sp,
    textAlign = TextAlign.Center
)

const val cornerStatFontSize = 36
const val cornerLabelFontSize = 14
const val largeCardStatFontSize = 100
const val largeCardLabelFontSize = 28
const val smallCardStatFontSize = 55
const val smallCardLabelFontSize = 16

private fun Color.alpha(value: Float): Color {
    return this.copy(alpha = value)
}

@OptIn(ExperimentalComposeUiApi::class)
@Device15inPreviews
@Composable
fun PreviewSimpleAppScreenDark() {
    SimpleAppScreen(
        statInfoCollection = StatInfoCollection.dummyBikeStatInfoCollection(),
        theme = darkDynamicTheme
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Device15inPreviews
@Device10inPreviews
@Device22inPreviews
@Composable
fun PreviewSimpleAppScreenLight() {
    SimpleAppScreen(StatInfoCollection.dummyBikeStatInfoCollection())
}

@ExperimentalComposeUiApi
@Composable
fun SimpleAppScreen(
    statInfoCollection: StatInfoCollection,
    displayColor: Color = Color(0xFF0071FF),
    theme: DynamicThemeData = defaultDynamicTheme,
    allowGoalChange: Boolean = true,
    isFullscreen: Boolean = false,
    onIncrementGoalClick: () -> Unit = {},
    onDecrementGoalClick: () -> Unit = {},
    onSizeClick: () -> Unit = {},
    onSelectCard: (StatInfo) -> Unit = {},
    defaultSelectedColumn: Int = 2  //Current default is the middle column of Time Elapsed
) {

    if (statInfoCollection.isEmpty()) return

    val selectedCategory = remember { mutableIntStateOf(-1) }
    LaunchedEffect(Unit) {
        //The selected category can't be more than the number of categories
        selectedCategory.intValue = defaultSelectedColumn.coerceAtMost(statInfoCollection.categoryList.size -1)
    }

    DynamicTheme(theme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DynamicTheme.colors.background4)
                .semantics { testTagsAsResourceId = true }
        ) {

            TabShape(
                statInfoCollection,
                displayColor,
                allowGoalChange,
                selectedCategory,
                onIncrementGoalClick = onIncrementGoalClick,
                onDecrementGoalClick = onDecrementGoalClick,
                onSelectCard = onSelectCard
            )

            Icon(
                modifier = Modifier
                    .padding(end = 10.dp.computeSize(), top = 25.dp.computeSize())
                    .background(
                        color = DynamicTheme.colors.background2,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(12.dp.computeSize())
                    .size(16.dp.computeSize())
                    .testTag(testTagFullscreenButton)
                    .align(Alignment.TopEnd)
                    .clickable { onSizeClick() }
                    ,
                painter = painterResource(id = if (isFullscreen) R.drawable.ic_shrink_white else R.drawable.ic_expand_white),
                contentDescription = null,
                tint = DynamicTheme.colors.text1
            )
        }
    }
}

@Composable
private fun LargeCard(
    statCategory: StatInfoCategory,
    allowGoalChange: Boolean,
    onIncrementGoalClick: () -> Unit,
    onDecrementGoalClick: () -> Unit,
    onSelectCard: (StatInfo) -> Unit,
) {

    if (statCategory.statList.isEmpty()) return

    Box(
        modifier = Modifier
            .testTag(testTagLargeCard)
            .fillMaxSize()
    ) {

        statCategory.selected.value?.let {selectedStat->
            val iconId = remember(selectedStat) { selectedStat.iconId }
            Icon(
                painter = painterResource(id = iconId.value),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(200.dp.computeSize()),
                tint = DynamicTheme.colors.text1.alpha(.08f)
            )

            // large card stat
            StatTextWithButtons(
                modifier = Modifier.align(Alignment.Center),
                stat = selectedStat,
                allowGoalChange = allowGoalChange,
                onIncrementGoalClick = onIncrementGoalClick,
                onDecrementGoalClick = onDecrementGoalClick,
            )
        }

        // for test tags as resource ids for dqa
        val fourCorners = listOf("TopLeft", "TopRight", "BottomLeft", "BottomRight")
        var currentCorner = 0

        // assumes there are only a max of 5 stats in list
        val alignmentList = listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd)
        var alignmentCounter = 0
        statCategory.getUnselectedList().forEachIndexed { index, statInfo ->
            if (statInfo.active.value && alignmentCounter < alignmentList.size) {
                StatText(
                    modifier = Modifier
                        .testTag(testTagLargeCardStat + index)
                        .padding(30.dp)
                        .clickable { onSelectCard(statInfo) }
                        .align(alignmentList[alignmentCounter++]),
                    stat = statInfo,
                    widthPercent = .15f,
                    testTag = testTagLargeCardStat + fourCorners[currentCorner++]
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.SmallCardPager(
    modifier: Modifier = Modifier,
    statCategory: StatInfoCategory, backgroundColor: Color, selected: Boolean,
    onCardClick: (StatInfo) -> Unit, testTag: String
) {
    if (statCategory.statList.isEmpty()) return

    val activeStats = statCategory.getActiveList()
    val pagerState = rememberPagerState { activeStats.size }
    val iconId = remember { statCategory.statList[pagerState.currentPage].iconId }
    LaunchedEffect(selected) {
        pagerState.scrollToPage(activeStats.indexOf(statCategory.selected.value))
    }

    if (!selected) {
        Box(
            modifier = modifier
                .testTag(testTag)
                .weight(1f)
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(DynamicTheme.colors.background1)
        ) {

            HorizontalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = { onCardClick(activeStats[pagerState.currentPage]) },
                        onLongClick = {/* TODO test click vs long click */ },
                    ),
                state = pagerState,
            ) { page ->
                SmallCard(
                    modifier = Modifier.align(Alignment.Center),
                    stat = activeStats[page],
                    iconTint = backgroundColor,
                    testTag = testTag + testTagPage + page
                )
            }

            PagerJumpingDotIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                state = pagerState,
                activeColor = DynamicTheme.colors.text1,
                dotSize = 10.dp,
            )
        }
    } else {
        Box(
            modifier = modifier
                .testTag(testTag)
                .weight(1f)
                .fillMaxSize()
                .padding(5.dp)
                .clip(RoundedCornerShape(20.dp))
        ) {
            Icon(
                painter = painterResource(id = iconId.value),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(90.dp.computeSize()),
                tint = backgroundColor
            )
        }
    }
}

@Composable
private fun SmallCard(
    modifier: Modifier = Modifier,
    stat: StatInfo,
    statFontSize: Int = smallCardStatFontSize,
    labelFontSize: Int = smallCardLabelFontSize,
    iconTint: Color,
    testTag: String
) {

    val units = remember { stat.units }
    val text = remember { stat.text }
    val labelId = remember { stat.labelId }
    val iconId = remember { stat.iconId }
    val splitText = remember { stat.splitText }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .weight(1f),
            //.fillMaxHeight(),
            verticalAlignment = Alignment.Bottom,
        ){
            val iconModifier = Modifier
                .size(30.dp.computeSize())
                .padding(bottom = 10.dp, end = 5.dp)
            if (stat.type.isMyZone()) { // TODO - is there a better way to handle if resource is icon vs image?
                Image(painterResource(iconId.value), null, iconModifier)
            } else {
                Icon(
                    painter = painterResource(id = iconId.value),
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = iconTint
                )
            }
            Text( // label
                text = stringResource(labelId.value),
                color = DynamicTheme.colors.text1.alpha(.7f),
                fontSize = labelFontSize.sp.computeSize(),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 5.dp.computeSize(), start = 5.dp)
                    .testTag(testTag + "Label")
            )
        }
        if (splitText.value.isEmpty()) {
            // normal stats

            FixedHeightAutoSizeText( // data
                text = text.value,
                style = statTextStyle,
                fontSize = statFontSize.sp.computeSize(),
                color = DynamicTheme.colors.text1.alpha(.7f),
                modifier = Modifier.testTag(testTag + "Text")
            )
            Text( // units
                text = stringResource(id = units.value),
                color = DynamicTheme.colors.text1.alpha(.7f),
                fontSize = labelFontSize.sp.computeSize(),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    //.fillMaxHeight()
                    .padding(top = 10.dp)
                    .testTag(testTag + "Unit")
            )
        } else { // split stats
            // stats
            DualStatText(stat = stat, statFontSize = statFontSize, isSmallCard = true, isMainStat = false, testTag + "DualText")
            // units
            DualStatUnits(stat = stat, labelFontSize = labelFontSize, isSmallCard = true, isMainStat = false, testTag + "DualUnit")
        }
    }
}

@Composable
private fun StatText(
    modifier: Modifier = Modifier,
    stat: StatInfo,
    statFontSize: Int = cornerStatFontSize,
    labelFontSize: Int = cornerLabelFontSize,
    widthPercent: Float,
    testTag: String
) {

    val text = remember(stat) { stat.text }
    val labelId = remember(stat) { stat.labelId }
    val units = remember(stat) { stat.units }
    val splitText = remember(stat) { stat.splitText }

    Column(
        modifier = modifier
            .fillMaxWidth(widthPercent)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text( // label
            text = stringResource(labelId.value),
            color = DynamicTheme.colors.text1.alpha(.7f),
            fontSize = labelFontSize.sp.computeSize(),
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(testTag + "Label")
        )
        if (splitText.value.isEmpty()) {
            FixedHeightAutoSizeText(
                text = text.value,
                style = statTextStyle,
                fontSize = statFontSize.sp.computeSize(),
                color = DynamicTheme.colors.text1.alpha(.7f),
                modifier = Modifier.testTag(testTag + "Text")
            )
            Text( // units
                text = stringResource(id = units.value),
                color = DynamicTheme.colors.text1.alpha(.7f),
                fontSize = labelFontSize.sp.computeSize(),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(testTag + "Unit")
            )
        } else {
            // actual stat
            DualStatText(stat = stat, statFontSize = statFontSize, isSmallCard = false, isMainStat = false, testTag + "DualText")
            // units
            DualStatUnits(stat = stat, labelFontSize = labelFontSize, false, isMainStat = false, testTag + "DualUnit")
        }

    }
}


@Composable
private fun StatTextWithButtons(
    modifier: Modifier = Modifier,
    stat: StatInfo,
    allowGoalChange: Boolean,
    statFontSize: Int = largeCardStatFontSize,
    labelFontSize: Int = largeCardLabelFontSize,
    widthPercent: Float = .5f,
    onIncrementGoalClick: () -> Unit,
    onDecrementGoalClick: () -> Unit,
) {

    val text = remember(stat) { stat.text }
    val labelId = remember(stat) { stat.labelId }
    val units = remember(stat) { stat.units }
    val splitText = remember(stat) { stat.splitText }
    val showGoalButtons = remember(stat) { stat.showGoalButtons }

    Column(
        modifier = modifier.fillMaxWidth(widthPercent),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = stringResource(id = labelId.value),
            color = DynamicTheme.colors.text1,
            fontSize = labelFontSize.sp.computeSize(),
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(testTagLargeCardStat + "MainLabel")
        )

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (showGoalButtons.value && allowGoalChange) {
                GoalButton(
                    modifier = Modifier
                        .width(100.dp.computeSize())
                        .height(100.dp.computeSize())
                        .clip(RoundedCornerShape(20.dp))
                        .testTag(testTagGoalButtonMinus),
                    onClick = { onDecrementGoalClick() },
                ) {
                    Icon(
                        modifier = Modifier.size(80.dp.computeSize()),
                        painter = painterResource(id = R.drawable.ic_minus_white_v2),
                        contentDescription = null,
                        tint = DynamicTheme.colors.text1
                    )
                }
            }
            if (splitText.value.isEmpty()) {
                FixedHeightAutoSizeText(
                    text = text.value,
                    style = statTextStyle,
                    fontSize = statFontSize.sp.computeSize(),
                    modifier = modifier
                        .fillMaxWidth(.70f)
                        .testTag(testTagLargeCardStat + "MainText"),
                    color = DynamicTheme.colors.text1,
                )
            } else {
                // actual stat
                DualStatText(stat = stat, statFontSize = statFontSize, isSmallCard = false, isMainStat = true, testTagLargeCardStat + "MainDualText")
            }

            if (showGoalButtons.value && allowGoalChange) {
                GoalButton(
                    modifier = Modifier
                        .width(100.dp.computeSize())
                        .height(100.dp.computeSize())
                        .clip(RoundedCornerShape(20.dp))
                        .testTag(testTagGoalButtonPlus),
                    onClick = { onIncrementGoalClick() },
                ) {
                    Icon(
                        modifier = Modifier.size(80.dp.computeSize()),
                        painter = painterResource(id = R.drawable.ic_plus_white_v2),
                        contentDescription = null,
                        tint = DynamicTheme.colors.text1
                    )
                }
            }
        }

        if (splitText.value.isEmpty()) {
            Text(
                text = stringResource(id = units.value),
                color = DynamicTheme.colors.text1,
                fontSize = labelFontSize.sp.computeSize(),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(testTagLargeCardStat + "MainUnit")
            )
        } else {
            // units
            DualStatUnits(stat = stat, labelFontSize = labelFontSize, false, isMainStat = true, testTagLargeCardStat + "MainDualUnit")
        }

    }
}

@Composable
private fun DualStatText(
    stat: StatInfo,
    statFontSize: Int,
    isSmallCard: Boolean,
    isMainStat: Boolean,
    testTag: String
){

    val text = remember(stat) { stat.text }
    val splitText = remember(stat) { stat.splitText }

    val splitTextSizeDivider = 1.333 // trying to make the split small card look alright (and fit 3 digits for both stats)

    val fontSize =
        if (isSmallCard) {
            statFontSize.sp.computeSize()/splitTextSizeDivider
        } else {
            statFontSize.sp.computeSize()
        }
    val color =
        if (isMainStat) {
            DynamicTheme.colors.text1
        } else {
            DynamicTheme.colors.text1.alpha(.7f)
        }

    Row (
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.value,
            style = statTextStyle,
            color = color,
            fontSize = fontSize,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .testTag(testTag + testTagLeft)
        )
        Text(
            text = "|",
            style = statTextStyle,
            color = DynamicTheme.colors.text1.alpha(.25f),
            fontSize = statFontSize.sp.computeSize(),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(.1f)
        )
        Text(
            text = splitText.value,
            style = statTextStyle,
            color = color,
            fontSize = fontSize,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .testTag(testTag + testTagRight)
        )
    }
}

@Composable
private fun ColumnScope.DualStatUnits(
    stat: StatInfo,
    labelFontSize: Int,
    isSmallCard: Boolean,
    isMainStat: Boolean,
    testTag: String
){

    val units = remember(stat) { stat.units }
    val splitUnits = remember(stat) { stat.splitUnits }

    val modifier =
    if (isSmallCard) {
        Modifier
            .weight(1f)
            //.fillMaxHeight()
            .padding(top = 10.dp)
    } else {
        Modifier
    }
    val color =
        if (isMainStat) {
            DynamicTheme.colors.text1
        } else {
            DynamicTheme.colors.text1.alpha(.7f)
        }

    Row (modifier){
        Text(
            text = stringResource(id = units.value),
            color = color,
            fontSize = labelFontSize.sp.computeSize(),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .testTag(testTag + testTagLeft)
        )
        Text(
            text = stringResource(id = splitUnits.value),
            color = color,
            fontSize = labelFontSize.sp.computeSize(),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .testTag(testTag + testTagRight)
        )
    }
}


private const val smallCardHeightRatio = .33f
private const val largeCardHeightRatio = 1 - smallCardHeightRatio
private const val smallCardWidthAdjustment = .005f


private fun getTabShapeVertices(smallCardPosition: Int): FloatArray {
    return when(smallCardPosition) {
        1 -> {
            floatArrayOf(
                0f, 0f, // start big card top left
                1f, 0f, // big card top right
                1f, largeCardHeightRatio, // big card bottom right
                (smallCardPosition/5f) - smallCardWidthAdjustment, largeCardHeightRatio, // small card top right
                (smallCardPosition/5f) - smallCardWidthAdjustment, 1f, // small card bottom right
                0f, 1f, // small card bottom left
                //(smallCardPosition-1)/5f, smallCardHeightRatio, // not needed
                0f, largeCardHeightRatio, // big card bottom left
            )
        }
        5 -> {
            floatArrayOf(
                0f, 0f, // start big card top left
                1f, 0f, // big card top right
                1f, largeCardHeightRatio, // big card bottom right
                //smallCardPosition/5f, smallCardHeightRatio, // not needed
                1f, 1f, // small card bottom right
                ((smallCardPosition-1)/5f) + smallCardWidthAdjustment, 1f, // small card bottom left
                ((smallCardPosition-1)/5f) + smallCardWidthAdjustment, largeCardHeightRatio, // small card top left
                0f, largeCardHeightRatio, // big card bottom left
            )
        }
        else -> {
            floatArrayOf(
                0f, 0f, // start big card top left
                1f, 0f, // big card top right
                1f, largeCardHeightRatio, // big card bottom right
                (smallCardPosition/5f) - smallCardWidthAdjustment, largeCardHeightRatio, // small card top right
                (smallCardPosition/5f) - smallCardWidthAdjustment, 1f, // small card bottom right
                ((smallCardPosition-1)/5f) + smallCardWidthAdjustment, 1f, // small card bottom left
                ((smallCardPosition-1)/5f) + smallCardWidthAdjustment, largeCardHeightRatio, // small card top left
                0f, largeCardHeightRatio, // big card bottom left
            )
        }
    }
}


@Composable
private fun TabShape(
    statInfoCollection: StatInfoCollection,
    mainColor: Color,
    allowGoalChange: Boolean,
    selectedCategoryIndex: MutableIntState,
    onIncrementGoalClick: () -> Unit,
    onDecrementGoalClick: () -> Unit,
    onSelectCard: (StatInfo) -> Unit,
) {
    // background is the funky tab shape that creative wants
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 25.dp.computeSize(), horizontal = 70.dp.computeSize())
            .drawWithCache {

                val funkyShapePath = RoundedPolygon(
                    vertices = getTabShapeVertices(selectedCategoryIndex.intValue + 1),
                    rounding = CornerRounding(.015f)
                )
                    .toPath()
                    .asComposePath()

                val funkyShapeBrush = Brush.verticalGradient(
                    colors = listOf(mainColor.alpha(.0f), mainColor.alpha(.2f)),
                    startY = 0f,
                    endY = 1f
                )

                onDrawBehind {
                    scale(scaleX = size.width, scaleY = size.height, pivot = Offset.Zero) {
                        drawPath(funkyShapePath, brush = funkyShapeBrush)
                        drawPath(funkyShapePath, color = mainColor, style = Stroke(.002f))

                    }
                }
            }
    ) {
        val selectCategory = statInfoCollection[selectedCategoryIndex.intValue]
        // Large Card
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(largeCardHeightRatio)
                .padding(20.dp)
        ) {
            if (selectCategory != null) LargeCard(
                selectCategory,
                allowGoalChange,
                onIncrementGoalClick = onIncrementGoalClick,
                onDecrementGoalClick = onDecrementGoalClick,
                onSelectCard = { stat ->
                    selectCategory.selected.value = stat
                    onSelectCard(stat)
                }
            )
        }
        // Small Card row
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(smallCardHeightRatio)
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp.computeSize())
        ) {
            statInfoCollection.categoryList.forEachIndexed { index, value ->
                SmallCardPager(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                    ,
                    value, mainColor, value == selectCategory,
                    onCardClick = {stat ->
                        value.selected.value = stat // select stat inside of category
                        onSelectCard(stat)
                        selectedCategoryIndex.intValue = index // updates selected small card and tab shape
                    }, testTagSmallCard + index)
            }
        }
    }
}