package com.jht.workoutselection.chameleon.parts

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.data.BaseFeaturedWorkoutPlugin
import com.jht.data.WorkoutInputUi
import com.jht.data.WorkoutItem
import com.jht.enums.WorkoutId
import com.jht.mock.interfaces.models.MockWorkoutCatalogModel
import com.jht.resources.robotoFonts
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize
import com.jht.widgets.GlideAndPreviewImage
import com.jht.widgets.LockedOverlay
import com.jht.widgets.PagerJumpingDotIndicator
import com.jht.widgets.PanelOperationMode
import com.jht.widgets.PillboxGrid
import com.jht.widgets.PopupFullscreen
import com.jht.widgets.StartWorkoutButton
import com.jht.widgets.helpers.LoadingWorkoutAnimation
import com.jht.widgets.helpers.TabItem
import com.jht.workoutselection.R
import com.jht.workoutselection.TabFeaturedKey
import com.jht.workoutselection.WorkoutSelectionViewModel
import com.jht.workoutselection.common.parts.TabBase


// Pass any data needed through the create function to feed into composable?
@Composable
internal fun createTabFeatured(
    selectedWorkoutItem: WorkoutItem?,
    isLoadingWorkout: Boolean,
    workoutInputs:List<WorkoutInputUi>,
    featuredPagerWorkoutItems: List<WorkoutItem>,
    featuredWorkoutItems: List<WorkoutItem>,
    onSelectWorkoutItem: (WorkoutItem) -> Unit = {},
    onStartSelectedWorkout: () -> Unit =  {},
    onLoadFeaturedWorkouts: () -> Unit = {},
    activeFeaturePlugin: MutableState<BaseFeaturedWorkoutPlugin?>,
    ): TabItem {
    return TabItem(
        { selected, onSelect ->
            TabBase(R.drawable.ic_featured_workout_outline, R.drawable.ic_featured_workout, stringResource(R.string.featured_exercises_tc), selected, "tab$TabFeaturedKey", onSelect)
        },
        {


            LaunchedEffect(Unit) {
                // only runs on initial launch of this composable
                onLoadFeaturedWorkouts() // load featured items (they should be loaded this is a fail safe)
            }

            FeaturedTab(Modifier.fillMaxSize(),
                selectedWorkoutItem = selectedWorkoutItem,
                featuredWorkoutItems = featuredWorkoutItems,
                featuredPagerWorkoutItems = featuredPagerWorkoutItems,
                workoutInputs = workoutInputs,
                onSelectWorkoutItem = onSelectWorkoutItem,
                onStartSelectedWorkout = onStartSelectedWorkout,
                isLoadingWorkout = isLoadingWorkout,
                activeFeaturePlugin = activeFeaturePlugin,
            )
        },
        TabFeaturedKey
    )
}


@AllDevicePreviews
@Composable
private fun FeaturedTabPreview(){
    //FeaturedTab(Modifier.fillMaxSize())
    val mock = MockWorkoutCatalogModel()
    Row(Modifier.fillMaxSize()) {
        Box(modifier = Modifier
            .fillMaxSize()
            .weight(1f))
        FeaturedTab(
            modifier = Modifier
                .fillMaxSize()
                .weight(5f),
            workoutInputs = mock.getWorkoutInputsForPreview(),
            featuredWorkoutItems = mock.getFeaturedWorkouts,
            featuredPagerWorkoutItems = mock.getCarouselWorkouts,
            selectedWorkoutItem = mock.getFeaturedWorkouts[0],
            isLoadingWorkout = mock.isLoadingWorkout.value,
        )
    }

}


private const val GRID_SIDE_PADDING = 30
// In order to achieve the desired look of the grid being smaller in width to the carousel, we need
// to give padding to items in the grid on the side, but have that be different from the padding in
// between the items in the grid.  By giving individual padding of 30 to every grid item, there will
// be 30 padding on the sides and 60 padding in between each item.  Therefore the spacing of items
// in the grid must be negative so we end up with 60 - 45 = 15 padding
// (GRID_SIDE_PADDING * 2) - GRID_BETWEEN_ITEMS_PADDING = true padding in between items
private const val GRID_BETWEEN_ITEMS_PADDING = -45 // 60-45= **-> 15 <-**

@Composable
private fun FeaturedTab(
    modifier: Modifier,
    isLoadingWorkout:Boolean,
    featuredPagerWorkoutItems: List<WorkoutItem>,
    featuredWorkoutItems: List<WorkoutItem>,
    selectedWorkoutItem: WorkoutItem?,
    workoutInputs:List<WorkoutInputUi>,
    onSelectWorkoutItem: (WorkoutItem) -> Unit = {},
    onStartSelectedWorkout: () -> Unit =  {},
    activeFeaturePlugin: MutableState<BaseFeaturedWorkoutPlugin?> = mutableStateOf(null),
){
    val isPreview = LocalInspectionMode.current

    // https://stackoverflow.com/questions/70592694/laggy-lazy-column-android-compose
    val featuredWorkoutItemsREMEMBERED = remember(featuredWorkoutItems) { featuredWorkoutItems }
    val featuredWorkoutPagerItemsREMEMBERED = remember(featuredPagerWorkoutItems) { featuredPagerWorkoutItems }

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(GRID_BETWEEN_ITEMS_PADDING.dp.computeSize(), Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(15.dp.computeSize(), Alignment.Top),
    ) {
        header {
            CarouselPager(
                modifier = Modifier.fillMaxWidth(),
                featuredPagerWorkoutItems = featuredWorkoutPagerItemsREMEMBERED,
                onSetupClick = onSelectWorkoutItem,
            )
        }
        // secondary featured + va workout items
        if (isPreview) {
            // key makes it so that each item has a unique key.
            // mock is copy of single item over and over, so we do this or create individual mock items
            items(count = featuredWorkoutItemsREMEMBERED.size) { index ->
                FeaturedGridItem(
                    workoutItem = featuredWorkoutItemsREMEMBERED[index],
                    onClick = onSelectWorkoutItem,
                )
            }
        } else {
            items(count = featuredWorkoutItemsREMEMBERED.size,
                key = {
                    featuredWorkoutItemsREMEMBERED[it].workoutId.toString() + featuredWorkoutItemsREMEMBERED[it].optionalId
                }
            ) { index ->
                FeaturedGridItem(
                    workoutItem = featuredWorkoutItemsREMEMBERED[index],
                    onClick = onSelectWorkoutItem,
                )
            }
        }
    }

    if (featuredWorkoutPagerItemsREMEMBERED.isEmpty() && featuredWorkoutItemsREMEMBERED.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}

// https://stackoverflow.com/questions/68886848/how-can-i-add-a-stickyheader-to-lazyverticalgrid-like-lazycolumn-in-jetpack-comp
private fun LazyGridScope.header(
    content: @Composable LazyGridItemScope.() -> Unit
) {
    item(span = { GridItemSpan(this.maxLineSpan) }, content = content)
}

/** Attempt to create a matching unique background Uri list for given workout items */
private fun createUniqueImageList(originalList: List<WorkoutItem>): List<Uri> {
    // currently no items have overlapping image URI but an item can have multiple URI, so first-come-first-served should be fine for now
    val usedUri = mutableSetOf<Uri>()
    val newList = mutableListOf<Uri>()
    originalList.forEach { item ->
        var itemUri = item.background.find { !usedUri.contains(it) } ?: Uri.EMPTY
        if (itemUri != Uri.EMPTY) {
            usedUri.add(itemUri)
        } else if (item.background.isNotEmpty()) {
            itemUri = item.background.first() // if all we have is duplicate, just use the first anyway
        }
        newList.add(itemUri)
    }
    return newList
}

@Composable
private fun CarouselPager(
    modifier: Modifier,
    featuredPagerWorkoutItems: List<WorkoutItem>,
    onSetupClick: (workoutItem:WorkoutItem) -> Unit = {},
) {
    if (featuredPagerWorkoutItems.isEmpty()) return // what should this look like when empty? for now just skip it
    val orderedPagerWorkoutItems = remember(featuredPagerWorkoutItems) {
        WorkoutSelectionViewModel.orderPagerWorkouts(featuredPagerWorkoutItems)
    }
    val imageList = remember(orderedPagerWorkoutItems) {
        createUniqueImageList(orderedPagerWorkoutItems)
    }
    val pageCount = orderedPagerWorkoutItems.size
    val pagerState = rememberPagerState(
        initialPage = WorkoutSelectionViewModel.findCenteredIndex(pageCount),
        pageCount = {
            pageCount
        }
    )
    BoxWithConstraints(modifier) {
        // PAGER with content
        HorizontalPager(
            //modifier = modifier,
            state  = pagerState,
            pageSpacing = 30.dp.computeSize(),
            contentPadding = PaddingValues(
                start = (this.maxWidth.value*.15f).dp,
                end = (this.maxWidth.value*.15f).dp,
                bottom = 30.dp.computeSize(),
                top = 10.dp.computeSize()
            )
        ) { page ->
            val featuredItem = orderedPagerWorkoutItems[page]

            Box(modifier = Modifier.aspectRatio(2f)) {
                // BACKGROUND IMAGE
                LockedOverlay(
                    isLocked = orderedPagerWorkoutItems[page].locked,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    GlideAndPreviewImage(
                        uri = imageList[page],
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(.45f)
                        .background(Color.Black.copy(alpha = .6f))
                        .align(Alignment.CenterEnd),
                    contentAlignment = Alignment.TopCenter
                ){
                    Column(
                        modifier = Modifier
                            .padding(30.dp.computeSize())
                    ) {
                        val logoId = featuredItem.carouselLogoId
                        if (logoId != null) {
                            Image(
                                painter = painterResource(logoId),
                                contentDescription = stringResource(featuredItem.titleId),
                                alignment = Alignment.BottomStart,
                                contentScale = ContentScale.FillHeight,
                                modifier = Modifier
                                    .testTag("Carousel:Title:Logo")
                                    .height(36.dp)
                            )
                        } else {
                            Text(
                                text = stringResource(featuredItem.titleId),
                                color = DynamicTheme.colors.text1,
                                style = DynamicTheme.typography.textStyle3,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.testTag("Carousel:Title:Text")
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp.computeSize()))
                        Text(
                            text = stringResource(featuredItem.descriptionId),
                            color = DynamicTheme.colors.text1,
                            fontFamily = robotoFonts,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Start,
                        )
                        Spacer(modifier = Modifier.height(height = 32.dp.computeSize()))
                        ThemeButton(
                            onClick = { onSetupClick(featuredItem) },
                            border = BorderStroke(0.dp, DynamicTheme.colors.play),
                            colors = ButtonDefaults.buttonColors(containerColor = DynamicTheme.colors.play),
                            contentPadding = PaddingValues(horizontal = 25.dp, vertical = 10.dp),
                            modifier = Modifier
                                .width(IntrinsicSize.Min)
                                .testTag("Carousel:Start:Button")
                        ) {
                            Text(
                                text = stringResource(featuredItem.carouselButtonId ?: R.string.setup_button_tc),
                                color = DynamicTheme.colors.text1,
                                fontSize = 22.sp.computeSize(),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .requiredWidth(IntrinsicSize.Max)
                            )
                        }
                    }
                }
            }
        }
        // PAGER INDICATOR (dots on the bottom)
        PagerJumpingDotIndicator(
            state = pagerState,
            modifier = Modifier
                .align(Alignment.BottomCenter),
            activeColor = DynamicTheme.colors.primary,
        )
    }
}

@Composable
private fun FeaturedGridItem(
    workoutItem: WorkoutItem,
    onClick: (WorkoutItem)->Unit = {},
){
    val testTag = if (workoutItem.optionalId.isEmpty()) workoutItem.workoutId else "${workoutItem.workoutId}:${workoutItem.optionalId}"
    Column(
        Modifier
            .height(160.dp.computeSize())
            .padding(horizontal = GRID_SIDE_PADDING.dp.computeSize())
            .clickable {
                onClick(workoutItem)
            },
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LockedOverlay(
            isLocked = workoutItem.locked,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.54f),
            lockTestTag = "FeaturedWorkout:$testTag:Lock",
        ) {
            GlideAndPreviewImage(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("FeaturedWorkout:$testTag:Image"),
                uri = if(workoutItem.thumbnail != Uri.EMPTY) workoutItem.thumbnail else if (workoutItem.background.isNotEmpty()) workoutItem.background[0] else Uri.EMPTY,
                contentScale = ContentScale.FillBounds
            )
        }
        Text(
            modifier = Modifier
                .testTag("FeaturedWorkout:$testTag:Text")
                .padding(top = 8.dp.computeSize())
                .fillMaxWidth(),
            text = stringResource(workoutItem.titleId),
            color = DynamicTheme.colors.text1,
            style = DynamicTheme.typography.textStyle1,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
    }
}


@Composable
internal fun WorkoutSelectionModal(
    selectedWorkoutItem: WorkoutItem,
    title: String,
    description: String,
    workoutInputList:List<WorkoutInputUi>,
    activeFeaturePlugin: MutableState<BaseFeaturedWorkoutPlugin?>,
    isLoadingWorkout: Boolean,
    startSelectedWorkout: () -> Unit,
    onDismissRequest: () -> Unit,
){
    val imageUri = selectedWorkoutItem.background.firstOrNull() ?: Uri.EMPTY
    FeaturedModalBase(
        onDismissRequest = onDismissRequest,
        isLoadingWorkout = isLoadingWorkout,
        workoutInputList = workoutInputList,
        activeFeaturePlugin = activeFeaturePlugin.value,
        content = { modifier ->
            WorkoutSelectionModalContent(
                modifier = modifier,
                title = title,
                description = description,
                workoutInputList = workoutInputList,
                startSelectedWorkout = startSelectedWorkout,
                imageUri = imageUri,
            )
        },
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FeaturedModalBase(
    onDismissRequest: () -> Unit,
    isLoadingWorkout: Boolean,
    workoutInputList:List<WorkoutInputUi>,
    activeFeaturePlugin: BaseFeaturedWorkoutPlugin?,
    content: @Composable (modifier:Modifier) -> Unit,
){
    val usePlugin = activeFeaturePlugin != null
    PopupFullscreen(
        panelOperationMode = PanelOperationMode.ActivityContent,
        onDismissRequest = onDismissRequest,
    ) {
        // box around modal with tint
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = .7f))
                .clickableNoEffect(
                    onClick = onDismissRequest,
                ),
            contentAlignment = Alignment.Center
        ) {
            val screenWidth = this.maxWidth * 0.94f
            val screenHeight = this.maxHeight * 0.87f

            val modifier = Modifier
                .then(
                    if (!usePlugin) {
                        Modifier
                            .padding(
                                horizontal = 169.dp.computeSize(),
                                vertical = 54.dp.computeSize()
                            )
                            .fillMaxSize()
                    } else {
                        Modifier
                            .size(screenWidth, screenHeight)
                    }
                )
                .clip(RoundedCornerShape(30.dp))
                .background(
                    brush = DynamicTheme.colors.gradient1.vertical(),
                    shape = RoundedCornerShape(30.dp)
                )
                .clickableNoEffect(
                    onClick = {/* do nothing, so clicking inside the settings window doesn't dismiss it */ },
                )
                .semantics { testTagsAsResourceId = true }
            LoadingWorkoutAnimation(modifier, isLoadingWorkout) {
                if (usePlugin) {
                    activeFeaturePlugin.SetupScreen(
                        workoutInputs = workoutInputList,
                        onDismissRequest = onDismissRequest,
                    )
                } else {
                    content(it)
                }
            }
        }
    }
}


@Composable
private fun WorkoutSelectionModalContent(
    modifier: Modifier,
    title: String,
    imageUri: Uri,
    description: String,
    workoutInputList: List<WorkoutInputUi>,
    startSelectedWorkout: () -> Unit,
){

    Row(
        modifier = modifier
    ) {
        // image on left
        GlideAndPreviewImage(
            modifier = Modifier
                .fillMaxSize()
                .weight(2f),
            uri = imageUri,
            contentScale = ContentScale.FillHeight
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(3f)
                .padding(
                    horizontal = 50.dp.computeSize(),
                    vertical = 20.dp.computeSize()
                )
        ) {
            Text(
                text = title,
                color = DynamicTheme.colors.text1,
                style = DynamicTheme.typography.textStyle3,
                textAlign = TextAlign.Start,
                modifier = Modifier.testTag("FeaturedTab:SelectedWorkoutTitle:Text"),
            )
            Spacer(modifier = Modifier.size(height = 20.dp.computeSize(), width = 20.dp.computeSize()))
            Text(
                text = description,
                color = DynamicTheme.colors.text1,
                fontFamily = robotoFonts,
                fontSize = 16.sp,
                textAlign = TextAlign.Start,
            )
            Spacer(modifier = Modifier.size(height = 20.dp.computeSize(), width = 20.dp.computeSize()))

            PillboxGrid(
                modifier = Modifier
                    .aspectRatio(ratio = 1.7f)
                    .fillMaxWidth(),
                workoutInputList = workoutInputList,
            )

            Spacer(modifier = Modifier.size(height = 30.dp.computeSize(), width = 20.dp.computeSize()))

            StartWorkoutButton(
                onClick = {
                    startSelectedWorkout()
                },
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .height(60.dp.computeSize())
            )
        }
    }
}

@AllDevicePreviews
@Composable
private fun WorkoutSelectionModalPreview(){
    val mock = MockWorkoutCatalogModel()
    Box(modifier = Modifier
        .fillMaxSize()
    )

    // get modifier from FeaturedModalBase

    WorkoutSelectionModalContent(
        title = "Title",
        description = "description",
        imageUri = Uri.EMPTY,
        workoutInputList = mock.getWorkoutInputsForPreview(),
        startSelectedWorkout = {},
        modifier = Modifier
            .padding(
                horizontal = 169.dp.computeSize(),
                vertical = 54.dp.computeSize()
            )
            .clip(RoundedCornerShape(30.dp))
            .background(
                brush = DynamicTheme.colors.gradient1.vertical(),
                shape = RoundedCornerShape(30.dp)
            )
            .clickableNoEffect(
                onClick = { }, //do nothing, so clicking inside the settings window doesn't dismiss it
            )
            .fillMaxSize(),
    )

}
