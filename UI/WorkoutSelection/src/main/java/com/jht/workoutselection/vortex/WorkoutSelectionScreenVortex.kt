package com.jht.workoutselection.vortex

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jht.data.BaseFeaturedWorkoutPlugin
import com.jht.data.LandmarkWorkoutItem
import com.jht.data.WorkoutInputLevelInfo
import com.jht.data.WorkoutInputUi
import com.jht.data.WorkoutItem
import com.jht.data.badgeLabelRes
import com.jht.data.previewImageModel
import com.jht.engage360.Engage360Handler
import com.jht.engage360.Engage360Widget
import com.jht.mock.interfaces.models.MockWorkoutCatalogModel
import com.jht.theme.DynamicTheme
import com.jht.theme.DynamicThemeData
import com.jht.theme.GradientButton
import com.jht.theme.darkDynamicTheme
import com.jht.theme.defaultDynamicTheme
import com.jht.theme.lightDynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd
import com.jht.widgets.DialogFullscreen
import com.jht.widgets.LanguageOverlay
import com.jht.widgets.MessageDialog
import com.jht.widgets.ProductDashboard
import com.jht.widgets.RFIDIndicator
import com.jht.widgets.RfidLoginWidget
import com.jht.widgets.RfidLoginWidgetHandler
import com.jht.workoutselection.CustomWorkoutHandler
import com.jht.workoutselection.R
import com.jht.workoutselection.WorkoutSelectionState
import com.jht.workoutselection.vortex.modals.LocationSelectionModal
import com.jht.workoutselection.vortex.parts.TabCustomWorkout
import com.jht.workoutselection.vortex.parts.TabItem
import com.jht.workoutselection.vortex.parts.WorkoutSelectionTopBar
import com.jht.workoutselection.vortex.parts.mainscreen.CarouselItem
import com.jht.workoutselection.vortex.parts.mainscreen.FeaturedTrainingSetup
import com.jht.workoutselection.vortex.parts.mainscreen.SingleWorkoutOnly
import com.jht.workoutselection.vortex.parts.mainscreen.WorkoutSelectionModal

/**
 * Screens for selecting and starting a workout
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun WorkoutSelectionScreenVortex(
    //Theme and UI state
    theme: DynamicThemeData = defaultDynamicTheme,
    uiState: MutableState<WorkoutSelectionState> = mutableStateOf(WorkoutSelectionState.Featured),

    //Start Workouts
    onQuickStart: () -> Unit = {},
    onStartSelectedWorkout: () -> Unit = {},

    //Featured Lists
    featuredCarouselItems: List<CarouselItem> = listOf(),
    featuredWorkoutItems: List<WorkoutItem> = listOf(),

    //TrainingLists
    trainingCarouselItems: List<CarouselItem> = listOf(),
    trainingWorkoutItems: List<WorkoutItem> = listOf(),

    //Selected Workout Setup
    onSelectWorkoutItem: (WorkoutItem) -> Unit = {},
    selectedWorkoutItem: WorkoutItem? = null,
    workoutStartingInputs: List<WorkoutInputUi> = listOf(),
    workoutInputLevelInfoList: List<WorkoutInputLevelInfo> = listOf(),

    //Workout Setup Modals
    showWorkoutSetupModal: MutableState<Boolean> = mutableStateOf(false),
    showVirtualActiveModal: MutableState<Boolean> = mutableStateOf(false),
    showLandmarksModal: MutableState<Boolean> = mutableStateOf(false),
    activeFeaturePlugin: MutableState<BaseFeaturedWorkoutPlugin?> = mutableStateOf(null),
    showFeaturePluginModal: MutableState<Boolean> = mutableStateOf(false),

    //Information for special workouts
    virtualActiveWorkoutItems: List<WorkoutItem> = listOf(),
    landmarkWorkoutItems: List<LandmarkWorkoutItem> = listOf(),
    customWorkoutHandler: CustomWorkoutHandler = CustomWorkoutHandler(),
    onStartCustomWorkout: () -> Unit = {},

    //Engage360 and seca
    engage360Handler: Engage360Handler? = null,

    //Single Workout View
    singleWorkoutItemOnly: WorkoutItem? = null,

    //TopBar
    isHomeScreen: Boolean = false,
    isPTPUser: Boolean = false,
    isSecaUser: Boolean = false,
    onHome: () -> Unit = {},
    onUserButton: () -> Unit = {},
    onPtpShow: () -> Unit = {},

    //Language list shows when the welcome screen is disabled
    languageList: List<String> = listOf(),
    selectedLanguageIndex: Int = 0,
    onLanguageSelected: (Int) -> Unit = {},

    //RFID dialogs for login
    rfidHandler: RfidLoginWidgetHandler = RfidLoginWidgetHandler(),

    // Tiers can block certain workouts, so block and prompt if workout is locked
    showTiersUpgradePrompt: MutableState<Boolean> = mutableStateOf(false),

    //UI locking for starting workout
    blockUi: State<Boolean> = mutableStateOf(false),

    //Extra debugging
    showDebugDialog: () -> Unit = {},
    isRfidIconVisible: Boolean = false
) {
    val showProductDashboard = remember { mutableStateOf(false) }
    val showLanguageOverlay = remember(false) { mutableStateOf(false) }
    val isE360Open = uiState.value == WorkoutSelectionState.Engage360

    DynamicTheme(theme) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { testTagsAsResourceId = true }) {

                WorkoutSelectionTopBar(
                    onHome = onHome,
                    showE360Button = isPTPUser && !isE360Open, // only show if PTP user is active and we are not already showing PTPN
                    showPaceButton = isSecaUser && !isE360Open,
                    onPTP = { onPtpShow() },
                    onUserButton = onUserButton,
                    isHomeScreen = isHomeScreen,
                    onLanguage = { showLanguageOverlay.value = !showLanguageOverlay.value },
                    showProductDashboard = showProductDashboard,
                    onShowDebugDialog = showDebugDialog
                )

                MainUIScreens(
                    uiState = uiState,
                    featuredCarouselItems = featuredCarouselItems,
                    featuredWorkoutItems = featuredWorkoutItems,
                    onSelectWorkoutItem = onSelectWorkoutItem,
                    showVirtualActiveModal = showVirtualActiveModal,
                    onQuickStart = onQuickStart,
                    trainingCarouselItems = trainingCarouselItems,
                    trainingWorkoutItems = trainingWorkoutItems,
                    customWorkoutHandler = customWorkoutHandler,
                    onStartCustomWorkout = onStartCustomWorkout,
                    engage360Handler = engage360Handler,
                    singleWorkoutItemOnly = singleWorkoutItemOnly,
                )
            }
            RFIDIndicator(
                modifier = Modifier.padding(start = 44.dp.computeSizeHd())
                    .align(Alignment.CenterStart).zIndex(1f),
                isVisible = isRfidIconVisible
            )
        }

        activeFeaturePlugin.value?.let {
            ModalBase(showingModal = showFeaturePluginModal) {
                it.SetupScreen(
                    workoutInputs = workoutStartingInputs,
                    onDismissRequest = { showFeaturePluginModal.value = false },
                )
            }
        } ?: WorkoutModals(
            selectedWorkoutItem = selectedWorkoutItem,
            showWorkoutSetupModal = showWorkoutSetupModal,
            showVirtualActiveModal = showVirtualActiveModal,
            showLandmarksModal = showLandmarksModal,

            onSelectWorkoutItem = onSelectWorkoutItem,
            workoutStartingInputs = workoutStartingInputs,
            workoutInputLevelInfoList = workoutInputLevelInfoList,

            onStartSelectedWorkout = onStartSelectedWorkout,
            landmarkWorkoutItems = landmarkWorkoutItems,
            virtualActiveWorkoutItems = virtualActiveWorkoutItems,
        )

        ProductDashboard(showProductDashboard)
        RfidLoginWidget(rfidHandler)
        TiersUpgradePrompt(showTiersUpgradePrompt)
        LanguageOverlay(
            languageList = languageList,
            selectedLanguageIndex = selectedLanguageIndex,
            showLanguageOverlay = showLanguageOverlay,
            onLanguageSelected = onLanguageSelected
        )

        // UI block should be last
        if (blockUi.value) Box(
            Modifier
                .fillMaxSize()
                .clickableNoEffect {}) // set zIndex ?
    }
}

/**
 * Main screens for the workout setup that are driven by [uiState]
 * 1. Featured Setup
 * 2. Training Setup
 * 3. Custom Workouts
 * 4. Engage 360 (PTP)
 */
@Composable
private fun MainUIScreens(
    uiState: MutableState<WorkoutSelectionState>,
    featuredCarouselItems: List<CarouselItem>,
    featuredWorkoutItems: List<WorkoutItem>,
    trainingCarouselItems: List<CarouselItem>,
    trainingWorkoutItems: List<WorkoutItem>,
    onSelectWorkoutItem: (WorkoutItem) -> Unit,
    showVirtualActiveModal: MutableState<Boolean>,
    onQuickStart: () -> Unit,
    customWorkoutHandler: CustomWorkoutHandler,
    onStartCustomWorkout: () -> Unit,
    engage360Handler: Engage360Handler?,
    singleWorkoutItemOnly: WorkoutItem?,
) {
    Box(modifier = Modifier.fillMaxSize()) {

        when (uiState.value) {
            WorkoutSelectionState.Featured -> {
                FeaturedTrainingMainScreen(
                    carouselItems = featuredCarouselItems,
                    featuredWorkoutItems = featuredWorkoutItems,
                    onSelectWorkoutItem = onSelectWorkoutItem,
                    uiState = uiState,
                    onQuickStart = onQuickStart,
                    onClickViewAll = { showVirtualActiveModal.value = true }
                )
            }

            WorkoutSelectionState.Training -> {
                FeaturedTrainingMainScreen(
                    carouselItems = trainingCarouselItems,
                    featuredWorkoutItems = trainingWorkoutItems,
                    onSelectWorkoutItem = onSelectWorkoutItem,
                    uiState = uiState,
                    onQuickStart = onQuickStart
                )
            }

            WorkoutSelectionState.Custom -> {
                TabCustomWorkout(
                    workoutInputs = customWorkoutHandler.workoutInputs,
                    customWorkouts = customWorkoutHandler.customWorkouts,
                    selected = customWorkoutHandler.selectedWorkout.value,
                    onSelect = customWorkoutHandler::selectWorkout,
                    onAddWorkout = customWorkoutHandler::addWorkout,
                    onDeleteWorkout = customWorkoutHandler::deleteWorkout,
                    onStartCustomWorkout = onStartCustomWorkout,
                )
                UiStateSelector(uiState)
            }

            WorkoutSelectionState.Engage360 -> {
                engage360Handler?.let {
                    Engage360Widget(
                        handler = engage360Handler,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }

            WorkoutSelectionState.SingleWorkout -> {
                singleWorkoutItemOnly?.let {
                    SingleWorkoutOnly(
                        currentItem = it,
                        onSelectWorkoutItem = onSelectWorkoutItem
                    )
                }
            }
        }
    }
}

/**
 * Carousel and grid selection for the featured and training setup
 */
@Composable
private fun BoxScope.FeaturedTrainingMainScreen(
    carouselItems: List<CarouselItem>,
    featuredWorkoutItems: List<WorkoutItem>,
    onSelectWorkoutItem: (WorkoutItem) -> Unit,
    uiState: MutableState<WorkoutSelectionState>,
    onQuickStart: () -> Unit,
    onClickViewAll: () -> Unit = {},
) {
    //Don't load the main screens until there are workouts to show
    if (carouselItems.isNotEmpty() || featuredWorkoutItems.isNotEmpty()) {
        FeaturedTrainingSetup(
            isFeatured = uiState.value == WorkoutSelectionState.Featured,
            carouselItems = carouselItems,
            workoutItems = featuredWorkoutItems,
            onSelectWorkoutItem = onSelectWorkoutItem,
            onClickViewAll = onClickViewAll
        )
        UiStateSelector(uiState)

        GoButton(onQuickStart)
    }
}

/**
 * Buttons for swapping between the featured/training/custom workouts
 */
@Composable
fun BoxScope.UiStateSelector(
    uiState: MutableState<WorkoutSelectionState>,
) {
    Row(
        modifier = Modifier
            .width(intrinsicSize = IntrinsicSize.Max)
            .fillMaxHeight(0.095f)
            .align(Alignment.TopCenter)
            .padding(top = 24.dp.computeSize())
            .background(
                Color(0xAA000000),
                shape = RoundedCornerShape(24.dp.computeSize())
            )
            .border(
                BorderStroke(1.dp, color = DynamicTheme.colors.gray4),
                shape = RoundedCornerShape(24.dp.computeSize())
            ),
    ) {
        val pages = listOf(
            Pair(WorkoutSelectionState.Featured, R.string.featured_tc),
            Pair(WorkoutSelectionState.Training, R.string.training_tc),
            Pair(WorkoutSelectionState.Custom, R.string.custom_no_ellipes_tc)
        )

        for (i in 0 until pages.size) {
            TabItem(
                modifier = Modifier.weight(1f),
                text = stringResource(pages[i].second),
                isSelected = uiState.value == pages[i].first,
                onClick = { uiState.value = pages[i].first }
            )
        }
    }
}

/**
 * Composable for Go Button and gradient bar at the bottom of the screen
 */
@Composable
private fun BoxScope.GoButton(
    onQuickStart: () -> Unit,
) {
    //Gradient box that fills the width of the screen
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(108 / 1000f)
            .align(Alignment.BottomCenter)
            .background(Color.Black.copy(alpha = 76 / 255f))
    ) {
        //Go Button
        val gradient = Brush.verticalGradient(listOf(Color(0xFF17FFA2), Color(0xFF04FD49)))
        GradientButton(
            onClick = onQuickStart,
            shape = RoundedCornerShape(18.dp.computeSize()),
            contentPadding = PaddingValues(10.dp.computeSize()),
            modifier = Modifier
                .fillMaxHeight(68 / 108f)
                .fillMaxWidth(0.13f)
                .align(Alignment.Center)
                .testTag("WorkoutSelection:GoButton"),
            gradient = gradient,
        ) {
            Icon(
                painterResource(id = R.drawable.ic_quick_start),
                contentDescription = "quick start",
                modifier = Modifier
                    .size(30.dp.computeSize())
                    .padding(vertical = 4.dp.computeSize()),
                tint = Color.Black,
            )
            Text(
                stringResource(R.string.go_tc),
                color = DynamicTheme.colors.gray1,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 4.dp.computeSize())
            )
        }
    }
}

/**
 * Setup dialogs for the selected workout
 * Programs like VA, Landmarks, and Sprint8 have their own setup
 */
@Composable
private fun WorkoutModals(
    selectedWorkoutItem: WorkoutItem?,

    //Modal booleans
    showWorkoutSetupModal: MutableState<Boolean>,
    showVirtualActiveModal: MutableState<Boolean>,
    showLandmarksModal: MutableState<Boolean>,

    //Landmarks and VA workouts
    virtualActiveWorkoutItems: List<WorkoutItem>,
    landmarkWorkoutItems: List<LandmarkWorkoutItem>,

    onSelectWorkoutItem: (WorkoutItem) -> Unit,
    workoutStartingInputs: List<WorkoutInputUi>,
    workoutInputLevelInfoList: List<WorkoutInputLevelInfo>,
    onStartSelectedWorkout: () -> Unit,
) {
    //VA Modal
    ModalBase(
        showingModal = showVirtualActiveModal,
        showCloseButton = false
    ) {
        LocationSelectionModal(
            workoutItems = virtualActiveWorkoutItems,
            onSelectWorkout = onSelectWorkoutItem,
            onClose = { showVirtualActiveModal.value = false }
        )
    }

    //Landmarks Modal
    ModalBase(
        showingModal = showLandmarksModal,
        showCloseButton = false
    ) {
        LocationSelectionModal(
            workoutItems = landmarkWorkoutItems,
            onSelectWorkout = onSelectWorkoutItem,
            onClose = { showLandmarksModal.value = false }
        )
    }

    //Default Workout Modal for launching a single workout
    //This should show above the Location Selection Modal for VA and Landmarks
    if (showWorkoutSetupModal.value && selectedWorkoutItem != null) {
        WorkoutSelectionModal(
            previewImage = selectedWorkoutItem.previewImageModel,
            title = stringResource(selectedWorkoutItem.titleId),
            description = stringResource(selectedWorkoutItem.descriptionId),
            workoutStartingInputs = workoutStartingInputs,
            workoutInputLevelInfoList = workoutInputLevelInfoList,
            startSelectedWorkout = onStartSelectedWorkout,
            showingModal = showWorkoutSetupModal,
            workoutBadge = selectedWorkoutItem.badge,
            badgeLabel = stringResource(selectedWorkoutItem.badgeLabelRes)
        )
    }
}

/**
 * Simple container for having a click effect to hide the modal
 */
// migrated ModalBase to accept a `useDialog` parameter, This enables Compose-based modals to be wrapped inside DialogFullscreen,
// resolving event/gesture freezing bugs. Fragment-based modals (Target Training) can set `useDialog = false` to run in-tree,
// once everything is in Compose useDialog = false can be deprecated.
@Composable
private fun ModalBase(
    showingModal: MutableState<Boolean>,
    showCloseButton: Boolean = true,
    useDialog: Boolean = true,
    content: @Composable () -> Unit
){
    if(showingModal.value) {
        if (useDialog) {
            // Running inside its own layer.
            DialogFullscreen(
                onDismissRequest = { showingModal.value = false },
            ) {
                ModalDialogBase(
                    showCloseButton = showCloseButton,
                    onDismiss = { showingModal.value = false },
                    content = content,
                )
            }
        } else {
            // Keep in-tree to prevent fragment transaction container failures.
            ModalDialogBase(
                showCloseButton = showCloseButton,
                onDismiss = { showingModal.value = false },
                content = content,
            )
        }
    }
}
@Composable
private fun ModalDialogBase(
    showCloseButton: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    // box around modal with tint
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .7f))
            .clickableNoEffect(
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
        if (showCloseButton) {
            Image(
                painter = painterResource(R.drawable.round_icon_button_close_enabled),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = 114.dp.computeSize() - 5.dp.computeSize(),
                        end = 199.dp.computeSize() - 5.dp.computeSize()
                    )
                    .clickable(onClick = onDismiss)
            )
        }
    }
}


/**
 * Prompt for showing the workout is unavailable due to Tiers
 */
@Composable
private fun TiersUpgradePrompt(showUpgradePrompt: MutableState<Boolean>) {
    if (showUpgradePrompt.value) MessageDialog(
        title = stringResource(R.string.upgrade_unavailable_tc),
        message = stringResource(R.string.upgrade_unavailable_pg),
        modifier = Modifier
            .fillMaxWidth(0.33f)
            .aspectRatio(1.8f),
        onDismiss = { showUpgradePrompt.value = false },
        usePopupFullscreen = false
    )
}


//region Previews
@AllDevicePreviews
@Composable
private fun PreviewWorkoutSelectionScreenLight() {
    val mock = MockWorkoutCatalogModel()
    WorkoutSelectionScreenVortex(
        theme = lightDynamicTheme,
        uiState = remember { mutableStateOf(WorkoutSelectionState.Featured) },
        featuredCarouselItems = mock.getCarouselWorkouts.map { CarouselItem.fromWorkoutItem(it) },
        featuredWorkoutItems = mock.getFeaturedWorkouts,
    )
}

@AllDevicePreviews
@Composable
private fun PreviewWorkoutSelectionScreenDark() {

    val mock = MockWorkoutCatalogModel()
    WorkoutSelectionScreenVortex(
        theme = darkDynamicTheme,
        uiState = remember { mutableStateOf(WorkoutSelectionState.Training) },
        trainingWorkoutItems = mock.getVAWorkouts,
    )
}

@AllDevicePreviews
@Composable
private fun PreviewWorkoutSelectionScreenPtp() {
    val ptpHandler = Engage360Handler(LocalContext.current)
    WorkoutSelectionScreenVortex(
        uiState = remember { mutableStateOf(WorkoutSelectionState.Engage360) },
        engage360Handler = ptpHandler,
    )
}

//endregion Previews

