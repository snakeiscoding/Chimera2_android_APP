package com.jht.workoutselection.vortex.parts.mainscreen

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.data.AdHostController
import com.jht.data.FeaturedWorkoutAd
import com.jht.data.WorkoutBadge
import com.jht.data.WorkoutItem
import com.jht.mock.interfaces.models.MockWorkoutCatalogModel
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd
import com.jht.widgets.GlideAndPreviewImage
import com.jht.widgets.LockedOverlay
import com.jht.widgets.PageProgressIndicator
import com.jht.workoutselection.R
import com.jht.workoutselection.vortex.parts.ThemedBadgeView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

//Default time to auto cycle the carousel
private const val DEFAULT_AUTOSCROLL_TIME_MS = 10000

/** Represents basic item for display in [CarouselComponent] */
@Immutable
internal class CarouselItem(
    /** Contains any arbitrary object that caller would need */
    val tag: Any? = null,
    // basically the fields to render, largely taken from WorkoutItem
    val badge: WorkoutBadge = WorkoutBadge(),
    val locked: Boolean = false,
    val background: List<Uri> = listOf(),
    @param:StringRes
    val titleId: Int = 0,
    @param:StringRes
    val descriptionId: Int = 0,
    @param:DrawableRes
    val carouselLogoId: Int? = null,
    @param:StringRes
    val carouselButtonId: Int? = null,
    /** Optional foreground component to display next to info card */
    val foregroundComposable: (@Composable (Modifier, AdHostController)-> Unit)? = null
) {
    companion object {
        fun fromWorkoutItem(workoutItem: WorkoutItem): CarouselItem {
            return CarouselItem(
                tag = workoutItem,
                badge = workoutItem.badge,
                locked = workoutItem.locked,
                background = workoutItem.background,
                titleId = workoutItem.titleId,
                descriptionId = workoutItem.descriptionId,
                carouselLogoId = workoutItem.carouselLogoId,
                carouselButtonId = workoutItem.carouselButtonId,
            )
        }
        fun fromAd(ad: FeaturedWorkoutAd): CarouselItem {
            return CarouselItem(
                tag = ad,
                background = ad.backgroundUri,
                titleId = ad.titleId,
                descriptionId = ad.descriptionId,
                carouselButtonId = ad.carouselButtonId,
                foregroundComposable = { modifier, controller -> ad.Content(modifier, controller) },
            )
        }
    }
}

/** internal controller that handles paused page tracking */
@Stable
private class InternalAutoScrollController {
    // raw pager page that owns the pause; null = running
    var pausedByPage by mutableStateOf<Int?>(null)
        private set
    fun pause(page: Int)  { pausedByPage = page }
    fun resume(page: Int) { if (pausedByPage == page) pausedByPage = null }
}

/** Interface class to the content to control pausing */
@Stable
private class CarouselHostController(
    private val page: Int,
    private val controller: InternalAutoScrollController,
): AdHostController() {
    override fun pause()  = controller.pause(page)
    override fun resume() = controller.resume(page)
}

/**
 * Carousel that displays an image and title of a given workout.
 * It will autocycle or can be manually changed to the next available option
 */
@Composable
internal fun CarouselComponent(
    items: List<CarouselItem>,
    autoScrollIntervalMs: Int = DEFAULT_AUTOSCROLL_TIME_MS,
    onClick: (item: CarouselItem) -> Unit = { }
) {
    //Does not show if items are empty
    if(items.isEmpty()) return

    //Workaround for infinite scrolling by artificially inflating the page count X1000
    val pageCount = items.size * 1000
    //Initial page starts on the 1st index
    val initialPage = 0 //pageCount/2 + 1 // older UI would start center, for this flow just start at the start
    val pagerState = rememberPagerState (initialPage = initialPage, pageCount = {pageCount})

    //Vals for the progress and animation
    val coroutineScope = rememberCoroutineScope()
    val indicatorProgressAnim = remember { Animatable(0f) }

    val autoScrollController = remember { InternalAutoScrollController() }
    LaunchedEffect(pagerState.currentPage) {
        //If animations are turned off (duration scale = 0), we can return so the carousel does not infinitely cycle
        if (coroutineContext[MotionDurationScale]?.scaleFactor == 0f){
            return@LaunchedEffect
        }

        val page = pagerState.currentPage

        // Swipe clears any pause left behind by the page we just left.
        // A pause that legitimately belongs to THIS page survives (owner == page).
        autoScrollController.pausedByPage
            ?.takeIf { it != page }
            ?.let { autoScrollController.resume(it) }

        indicatorProgressAnim.snapTo(0f)
        snapshotFlow { autoScrollController.pausedByPage == page }
            .collectLatest { paused ->
                if (paused) return@collectLatest
                val remainingMs = ((1f - indicatorProgressAnim.value) * autoScrollIntervalMs).toInt().coerceAtLeast(0)
                indicatorProgressAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(remainingMs, easing = LinearEasing)
                )
                snapshotFlow { pagerState.isScrollInProgress }.first { !it }
                // Run the scroll in the durable scope so it survives the currentPage flip.
                coroutineScope.launch { pagerState.animateScrollToPage(page + 1) }
            }
    }

    //Main composables for the carousel
    Box (modifier = Modifier.aspectRatio(1920 / 586f)) {
        CarouselItemHolder(pagerState, autoScrollController, items, onClick)
        CarouselOverlayComponents(coroutineScope, pagerState, items, indicatorProgressAnim)
    }
}

/**
 * Primary content containing a horizontal pager, background image, and title card
 */
@Composable
private fun CarouselItemHolder(
    pagerState: PagerState,
    controller: InternalAutoScrollController,
    items: List<CarouselItem>,
    onClick: (CarouselItem) -> Unit
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("WorkoutSelection:CarouselPager")
    ) { page ->
        //Need to modulo based on the item size due to artificially increasing the page count
        val item = items[page % items.size]
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            LockedOverlay(
                isLocked = item.locked,
                modifier = Modifier.fillMaxSize(),
            ) {
                GlideAndPreviewImage(
                    uri = item.background.firstOrNull() ?: Uri.EMPTY,
                    modifier = Modifier
                        .background(Color.Black) // in absence of an image should we use black background? or should the passed item specify?
                        .matchParentSize()
                        .align(Alignment.TopCenter),
                    contentScale = ContentScale.Crop
                )
            }
            CarouselTitleCard(
                currentItem = item,
                page = page,
                controller = controller,
            ) {
                onClick(it)
            }
        }
    }
}

/**
 * Workout information of title, description, and setup button
 */
@Composable
private fun CarouselTitleCard(
    currentItem: CarouselItem,
    page: Int,
    controller: InternalAutoScrollController,
    onClick: (item: CarouselItem) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(.8f)
    ) {
        ThemedBadgeView(
            badge = currentItem.badge,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = 60.dp.computeSizeHd(),
                    top = 28.dp.computeSizeHd()
                )
                .size(48.dp.computeSizeHd())
        )

        currentItem.foregroundComposable?.let { content ->
            val pauseScope = remember(page, controller) { CarouselHostController(page, controller) }
            content(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.5f)
                    .fillMaxHeight(),
                pauseScope,
            )
        }

        // if background is dark then black card background will not be seen
        val titleCardBackground = if (currentItem.background.isNotEmpty()) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.12f)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxWidth(0.33f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(18.dp.computeSize()))
                .background(titleCardBackground)
                .padding(
                    start = 40.dp.computeSizeHd(),
                    end = 40.dp.computeSizeHd(),
                    top = 48.dp.computeSizeHd(),
                    bottom = 48.dp.computeSizeHd(),
                )
        ) {
            val logoId = currentItem.carouselLogoId
            if (logoId != null) {
                Image(
                    painter = painterResource(logoId),
                    contentDescription = stringResource(currentItem.titleId),
                    contentScale = ContentScale.FillHeight,
                    alignment = Alignment.BottomStart,
                    modifier = Modifier
                        // Sized for pace logo-will likely need changed for any others
                        .height(36.dp)
                )
            } else {
                Text(
                    text = stringResource(currentItem.titleId),
                    style = MaterialTheme.typography.headlineLarge,
                    color = DynamicTheme.colors.gray15
                )
            }
            Spacer(modifier = Modifier.padding(8.dp.computeSize()))

            Text(
                text = stringResource(currentItem.descriptionId),
                style = MaterialTheme.typography.bodyLarge,
                color = DynamicTheme.colors.gray15,
                maxLines = 5,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = MaterialTheme.typography.bodySmall.fontSize, // Minimum font size to shrink to
                    maxFontSize = MaterialTheme.typography.bodyLarge.fontSize, // Maximum font size
                    stepSize = 2.sp // How much to step down by
                ),
            )
            Spacer(modifier = Modifier.padding(12.dp.computeSize()))

            ThemeButton(
                onClick = { onClick(currentItem) },
                modifier = Modifier
                    .wrapContentWidth()
                    .testTag("WorkoutSelect:Carousel:Setup:Button"),
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
                    text = stringResource(currentItem.carouselButtonId ?: R.string.setup_workout_tc),
                    style = DynamicTheme.typography.textStyle1.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

/**
 * Overlay UI of next/previous arrows and page indicator
 */
@Composable
private fun BoxScope.CarouselOverlayComponents(
    coroutineScope: CoroutineScope,
    pagerState: PagerState,
    items: List<CarouselItem>,
    indicatorProgressAnim: Animatable<Float, AnimationVector1D>
) {
    // --- Arrow Buttons ---
    Icon(
        painter = painterResource(R.drawable.caret_left),
        tint = Color.White,
        contentDescription = "left angle symbol",
        modifier = Modifier
            .padding(start = 60.dp.computeSizeHd())
            .align(Alignment.CenterStart)
            .clickable {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)

                }
            }
    )
    Icon(
        painter = painterResource(R.drawable.caret_right),
        tint = Color.White,
        contentDescription = "right angle symbol",
        modifier = Modifier
            .padding(end = 60.dp.computeSizeHd())
            .align(Alignment.CenterEnd)
            .clickable {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
    )

    // --- Carousel Indicators ---
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxHeight(0.23f)
    ) {
        PageProgressIndicator(
            currentIndex = pagerState.currentPage % items.size,
            itemCount = items.size,
            selectedIndicatorWidth = 36.dp.computeSize(),
            indicatorWidth = 12.dp.computeSize(),
            indicatorHeight = 12.dp.computeSize(),
            paddingValues = PaddingValues(6.dp),
            shape = RoundedCornerShape(50.dp),
            shadow = 10.dp,
            indicatorProgress = indicatorProgressAnim.value
        )
    }
}

@AllDevicePreviews
@Composable
private fun PreviewCarouselComponent() {
    val mock = MockWorkoutCatalogModel()
    CarouselComponent(
        items = mock.getCarouselWorkouts.subList(0,3).map { CarouselItem.fromWorkoutItem(it) }
    )
}