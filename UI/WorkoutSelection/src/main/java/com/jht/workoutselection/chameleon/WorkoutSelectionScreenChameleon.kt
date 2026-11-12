package com.jht.workoutselection.chameleon

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.jht.data.BaseFeaturedWorkoutPlugin
import com.jht.data.LandmarkWorkoutItem
import com.jht.data.WorkoutInputLevelInfo
import com.jht.data.WorkoutInputUi
import com.jht.data.WorkoutItem
import com.jht.data.badgeLabelRes
import com.jht.engage360.Engage360Handler
import com.jht.engage360.Engage360Widget
import com.jht.enums.WorkoutId
import com.jht.mock.interfaces.models.MockWorkoutCatalogModel
import com.jht.theme.DynamicTheme
import com.jht.theme.DynamicThemeData
import com.jht.theme.darkDynamicTheme
import com.jht.theme.defaultDynamicTheme
import com.jht.theme.lightDynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.widgets.LanguageOverlay
import com.jht.widgets.MessageDialog
import com.jht.widgets.ProductDashboard
import com.jht.widgets.RfidLoginWidget
import com.jht.widgets.RfidLoginWidgetHandler
import com.jht.widgets.helpers.TabList
import com.jht.workoutselection.CategoryWorkoutItems
import com.jht.workoutselection.CustomWorkoutHandler
import com.jht.workoutselection.R
import com.jht.workoutselection.TabFeaturedKey
import com.jht.workoutselection.TabTrainingKey
import com.jht.workoutselection.WorkoutSelectionState
import com.jht.workoutselection.chameleon.parts.WorkoutSelectionModal
import com.jht.workoutselection.chameleon.parts.WorkoutSelectionTopBar
import com.jht.workoutselection.chameleon.parts.createTabApps
import com.jht.workoutselection.chameleon.parts.createTabCustom
import com.jht.workoutselection.chameleon.parts.createTabFeatured
import com.jht.workoutselection.chameleon.parts.createTabTraining
import com.jht.workoutselection.vortex.modals.LocationSelectionModal

// CGC-23243: matrix method is broken on chameleon, but not needed on it yet either, should work on vortex ui
private fun filterFeatures(featureItems: List<WorkoutItem>): List<WorkoutItem> = featureItems.filter { it.workoutId != WorkoutId.PROGRESSION_WORKOUT.id }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WorkoutSelectionScreenChameleon(
    isRetail: Boolean = true,
    theme: DynamicThemeData = defaultDynamicTheme,
    uiState: MutableState<WorkoutSelectionState> = mutableStateOf(WorkoutSelectionState.Featured),
    rfidHandler: RfidLoginWidgetHandler = RfidLoginWidgetHandler(),
    selectedTabKey: Any? = null,
    isLoadingWorkout: Boolean = false,
    showLanguageButton: Boolean = false,
    selectedWorkoutItem:WorkoutItem? = null,
    workoutInputs:List<WorkoutInputUi> = listOf(),
    workoutInputLevelInfoList: List<WorkoutInputLevelInfo> = listOf(),
    workoutCategories: List<CategoryWorkoutItems> = listOf(),
    virtualActiveWorkoutItems: List<WorkoutItem> = listOf(),
    landmarkWorkoutItems: List<LandmarkWorkoutItem> = listOf(),
    featuredWorkoutItems: List<WorkoutItem> = listOf(),
    featuredPagerWorkoutItems: List<WorkoutItem> = listOf(),
    customWorkoutHandler: CustomWorkoutHandler = CustomWorkoutHandler(),
    onStartCustomWorkout: () -> Unit = {},
    onLoadFeaturedWorkouts: () -> Unit = {},
    onSelectWorkoutItem: (WorkoutItem?) -> Unit = {},
    onStartSelectedWorkout: () -> Unit =  {},
    onSelectTabKey: (Any?) -> Unit = {},
    onUserButton: ()->Unit = {},
    onQuickStart: () -> Unit = {},
    onHome: () -> Unit = {},
    ptpHandler: Engage360Handler? = null,
    onPtpShow: (Boolean)->Unit = {},
    blockUi: State<Boolean> = mutableStateOf(false),
    isSecaUser: Boolean = false,
    isGuest: Boolean = false,
    isPTPUser: Boolean = false,
    showDebugDialog: ()->Unit = {},

    //Workout Setup Modals
    showWorkoutSetupModal: MutableState<Boolean> = mutableStateOf(false),
    showVirtualActiveModal: MutableState<Boolean> = mutableStateOf(false),
    showLandmarksModal: MutableState<Boolean> = mutableStateOf(false),
    activeFeaturePlugin: MutableState<BaseFeaturedWorkoutPlugin?> = mutableStateOf(null),
    showFeaturePluginModal: MutableState<Boolean> = mutableStateOf(false),
) {

    val showProductDashboard = remember { mutableStateOf(false) }
    val showLanguageOverlay = remember(false) { mutableStateOf(false) }

    // Tiers can block certain workouts, so block and prompt if workout is locked
    val showUpgradePrompt = remember { mutableStateOf(false) }
    val checkSelectedItem: (WorkoutItem?) -> Unit = remember {{
        if (it?.locked == true) showUpgradePrompt.value = true
        else onSelectWorkoutItem(it)
    }}
    val checkLaunchSelected: () -> Unit = remember(selectedWorkoutItem) {{
        if (selectedWorkoutItem?.locked == true) showUpgradePrompt.value = true
        else onStartSelectedWorkout()
    }}

    // Think the only difference in tabs known now is that retail will add an apps tab of some kind
    val tabs = TabList(selectedTabKey, onSelectTabKey).add(
        createTabFeatured(
            workoutInputs = workoutInputs,
            onSelectWorkoutItem = checkSelectedItem,
            onStartSelectedWorkout = checkLaunchSelected,
            isLoadingWorkout = isLoadingWorkout,
            featuredPagerWorkoutItems = filterFeatures(featuredPagerWorkoutItems),
            featuredWorkoutItems = filterFeatures(featuredWorkoutItems),
            onLoadFeaturedWorkouts = onLoadFeaturedWorkouts,
            selectedWorkoutItem = selectedWorkoutItem,
            activeFeaturePlugin = activeFeaturePlugin,
        ),
        createTabTraining(
            workoutCategories,
            selectedWorkoutItem,
            workoutInputs,
            workoutInputLevelInfoList,
            isLoadingWorkout,
            checkLaunchSelected,
            checkSelectedItem,
        ),
        createTabCustom(
            customWorkoutHandler,
            onStartCustomWorkout,
        ),
    )
    if (isRetail) tabs.add(createTabApps())

    DynamicTheme(theme) {
        Box(modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
        ) {
            Column {
                // TOP BAR
                WorkoutSelectionTopBar(
                    onUserButton = onUserButton,
                    onLanguage = { showLanguageOverlay.value = !showLanguageOverlay.value },
                    onHome = onHome,
                    onPTP = { onPtpShow(false) },
                    showPTP = isPTPUser && uiState.value != WorkoutSelectionState.Engage360, // only show if PTP user is active and we are not already showing PTPN
                    showLanguageButton = showLanguageButton,
                    showProductDashboard = showProductDashboard,
                    onShowDebugDialog = showDebugDialog,
                    isSecaUser = isSecaUser,
                    isGuest = isGuest
                )
                HorizontalDivider(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp),
                    color = DynamicTheme.colors.gray3
                )
                // BOTTOM AREA
                Box(
                    Modifier.weight(1f)
                ) {
                    Row {
                        // LEFT SIDE PANEL
                        Column(
                            modifier = Modifier
                                .background(DynamicTheme.colors.gradient1.vertical())
                                .fillMaxHeight()
                                .aspectRatio(0.3f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // THE TABS
                            tabs.TabColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            )

                            // put button in centered in box so we don't have to specify padding
                            if (selectedTabKey == TabFeaturedKey) Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                // THE GO BUTTON
                                IconButton(
                                    onClick = onQuickStart,
                                    modifier = Modifier
                                        .testTag("Go:Button")
                                        .fillMaxWidth(.7f)
                                        .aspectRatio(1f),
                                ) {
                                    Image(
                                        painterResource(id = R.drawable.ic_go_button_round),
                                        null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                        VerticalDivider(
                            Modifier
                                .fillMaxHeight()
                                .width(1.dp),
                            color = DynamicTheme.colors.gray3
                        )

                        // RIGHT SIDE
                        Column(
                            Modifier
                                .weight(1f)
                        ) {
                            // INSERT
                            Box(
                                Modifier
                                    .background(DynamicTheme.colors.gradient1.vertical())
                                    .fillMaxSize()
                            ) {
                                tabs.CurrentTabBody()
                            }
                        }
                    }
                    if (ptpHandler != null && uiState.value == WorkoutSelectionState.Engage360) {
                        // if we want an actual blur effect, we might need to record graphic layer of background composable and pass it to widget to draw with blur render effect
                        // https://stackoverflow.com/a/76948352 https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/layer/GraphicsLayer
                        // blur effect might require Android 12 presently, might have to use our blur helpers
                        Engage360Widget(
                            ptpHandler,
                            Modifier.matchParentSize(),
                        )
                    }
                }
                ProductDashboard(showProductDashboard)
                RfidLoginWidget(rfidHandler)
            }
            LanguageOverlay(showLanguageOverlay)

        } // BoxCon

        if (showLandmarksModal.value) {
            FeatureSetupModal(
                onDismissRequest = { showLandmarksModal.value = false },
            ) {
                LocationSelectionModal(
                    workoutItems = landmarkWorkoutItems,
                    onSelectWorkout = onSelectWorkoutItem,
                    onClose = { showLandmarksModal.value = false }
                )
            }
        }
        if (showVirtualActiveModal.value) {
            FeatureSetupModal(
                onDismissRequest = { showVirtualActiveModal.value = false },
            ) {
                LocationSelectionModal(
                    workoutItems = virtualActiveWorkoutItems,
                    onSelectWorkout = onSelectWorkoutItem,
                    onClose = { showVirtualActiveModal.value = false }
                )
            }
        }

        if (selectedWorkoutItem != null) {
            // could we rework WorkoutSelectionModal to be cleaner or use the vortex version of WorkoutSelectionModal?
            if (showFeaturePluginModal.value) {
                WorkoutSelectionModal(
                    selectedWorkoutItem = selectedWorkoutItem,
                    startSelectedWorkout = onStartSelectedWorkout,
                    isLoadingWorkout = isLoadingWorkout,
                    workoutInputList = workoutInputs,
                    activeFeaturePlugin = activeFeaturePlugin,
                    title = stringResource(selectedWorkoutItem.titleId),
                    description = stringResource(selectedWorkoutItem.descriptionId),
                    onDismissRequest = { showFeaturePluginModal.value = false },
                )
            } else if (selectedTabKey == TabFeaturedKey && showWorkoutSetupModal.value) { // note that training tab has setup UI inline
                WorkoutSelectionModal(
                    selectedWorkoutItem = selectedWorkoutItem,
                    startSelectedWorkout = onStartSelectedWorkout,
                    isLoadingWorkout = isLoadingWorkout,
                    workoutInputList = workoutInputs,
                    activeFeaturePlugin = activeFeaturePlugin,
                    title = stringResource(selectedWorkoutItem.titleId),
                    description = stringResource(selectedWorkoutItem.descriptionId),
                    onDismissRequest = { showWorkoutSetupModal.value = false },
                )
            }
        }

        if (showUpgradePrompt.value) MessageDialog(
            title = stringResource(R.string.upgrade_unavailable_tc),
            message = stringResource(R.string.upgrade_unavailable_pg),
            modifier = Modifier
                .fillMaxWidth(0.33f)
                .aspectRatio(1.8f),
            onDismiss = { showUpgradePrompt.value = false },
        )
        // UI block should be last
        if (blockUi.value) Box(Modifier
            .fillMaxSize()
            .clickableNoEffect {}) // set zIndex ?
    } // Theme
} // Screen


/** Modal hosting a feature setup dialog */
@Composable
private fun FeatureSetupModal(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
){
    // box around modal with tint
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .7f))
            .clickableNoEffect(
                onClick = onDismissRequest,
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}



//region Previews
@AllDevicePreviews
@Composable
private fun PreviewWorkoutSelectionScreenLight() {
    WorkoutSelectionScreenChameleon(
        theme = lightDynamicTheme,
        selectedTabKey = TabFeaturedKey,
    )
}

@AllDevicePreviews
@Composable
private fun PreviewWorkoutSelectionScreenPtp() {
    val ptpHandler = Engage360Handler(LocalContext.current)
    WorkoutSelectionScreenChameleon(
        theme = defaultDynamicTheme,
        selectedTabKey = TabFeaturedKey,
        ptpHandler = ptpHandler,
    )
}


@AllDevicePreviews
@Composable
private fun PreviewWorkoutSelectionScreenDark() {

    val mock = MockWorkoutCatalogModel()
    val categories = listOf(
        CategoryWorkoutItems(R.string.training_tc, mock.getVAWorkouts),
        CategoryWorkoutItems(R.string.goals_tc, mock.getVAWorkouts),
        CategoryWorkoutItems(R.string.marathon_tc, mock.getVAWorkouts),
        CategoryWorkoutItems(R.string.target_heart_rate_tc, mock.getVAWorkouts),
        CategoryWorkoutItems(R.string.fitness_test_tc, mock.getVAWorkouts),
    )
    // Tabs seem like they do not work, individual previews are working

    WorkoutSelectionScreenChameleon(
        theme = darkDynamicTheme,
        selectedTabKey = TabTrainingKey,
        selectedWorkoutItem = mock.getVAWorkouts[0],
        workoutCategories = categories,
        featuredPagerWorkoutItems = mock.getCarouselWorkouts,
        featuredWorkoutItems = mock.getFeaturedWorkouts,
        workoutInputs = mock.getWorkoutInputsForPreview(),
        workoutInputLevelInfoList = mock.getWorkoutInputLevelInfo(0)
    )
}

//endregion Previews

