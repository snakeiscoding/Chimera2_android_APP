package com.jht.firsttimesetup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jht.firsttimesetup.ui.EditMachineInfoView
import com.jht.firsttimesetup.ui.FacilityProfileUi
import com.jht.firsttimesetup.ui.FacilityScreen
import com.jht.firsttimesetup.ui.FirstTimeSetupTopAppBar
import com.jht.firsttimesetup.ui.NetworkSelectScreen
import com.jht.firsttimesetup.ui.SettingsView
import com.jht.firsttimesetup.ui.SetupState
import com.jht.firsttimesetup.ui.SkipFtsuView
import com.jht.firsttimesetup.ui.SoftwareUpdateView
import com.jht.firsttimesetup.ui.SummaryEditType
import com.jht.firsttimesetup.ui.TvSetupView
import com.jht.firsttimesetup.viewmodels.FirstTimeSetupViewModel
import com.jht.firsttimesetup.viewmodels.SearchFacilityIdViewModel
import com.jht.firsttimesetup.viewmodels.TvSourceSelectionViewModel
import com.jht.ui.computeSize
import kotlinx.serialization.Serializable

@Serializable
object FirstTimeSetup

@Serializable
object FacilityDetail

internal sealed class Navigation(val route: String) {
    data object FirstTimeSetup : Navigation("ftsu")
    data object FacilityDetail : Navigation("facility_detail")
    data object FacilityProfile : Navigation("facility_profile_detail")

    data object Network : Navigation("network_detail")
    data object TVSetup : Navigation("tv_setup_detail")

    data object Machine : Navigation("machine_detail")
    data object GeneralSettings : Navigation("general_settings_detail")

    data object Update : Navigation("update_detail")
}

/**
 * View that holds navigation related controllers and routes
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FirstTimeSetupNavigationView(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    vm: FirstTimeSetupViewModel = hiltViewModel(),
    searchVm: SearchFacilityIdViewModel = hiltViewModel(),
    tvSourceSelectionViewModel: TvSourceSelectionViewModel = hiltViewModel(),
    onScannerEnabledChanged: (enabled: Boolean) -> Unit = {},
) {

    /**
     * Keeps track of user's choice to update after facility profile import
     */
    var shouldInstallUpdate by remember { mutableStateOf(false) }

    val searchVmState = searchVm.uiState.collectAsStateWithLifecycle()

    // A new search result must replace a facility imported from an earlier search.
    LaunchedEffect(searchVmState.value.facility) {
        vm.onSearchedFacilityChanged(searchVmState.value.facility)
    }

    var showSkipFtsuDialog by remember { mutableStateOf(false) }

    if (showSkipFtsuDialog) {
        Dialog(
            onDismissRequest = { showSkipFtsuDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            SkipFtsuView(
                modifier = Modifier
                    .fillMaxWidth(.55f),
                onCancelClicked = { showSkipFtsuDialog = false },
                onConfirmClicked = { vm.onSkipFtsu() }
            )
        }
    }

    NavHost(
        navController,
        startDestination = Navigation.FirstTimeSetup.route,
        modifier = modifier.semantics { testTagsAsResourceId = true }
    ) {
        composable(route = Navigation.FirstTimeSetup.route) { backStackEntry ->
            FirstTimeSetupView(
                onNavigateToFacilityDetail = {
                    navController.navigate(route = Navigation.FacilityDetail.route + "/false")
                },
                viewModel = vm,
                facilitySearchViewModel = searchVm,
                onEditNavigationClicked = { type -> handleEditClicked(type, navController) },
                shouldInstallUpdate = shouldInstallUpdate,
                onSkipFtsu = { showSkipFtsuDialog = true },
                navigateToFacilityProfile = {
                    navController.navigate(route = Navigation.FacilityDetail.route + "/false")
                },
                navigateToMachineInfo  = {
                    navController.navigate(route = Navigation.Machine.route + "/false")
                },
                onScannerEnabledChanged = onScannerEnabledChanged,
            )
        }
        composable(
            route = Navigation.FacilityDetail.route + "/{isEdit}",
            arguments = listOf(navArgument("isEdit") { type = NavType.BoolType })
        ) { backstackEntry ->
            val isEdit = backstackEntry.arguments?.getBoolean("isEdit") ?: false
            FacilityProfileUi(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxSize(),
                onBackClicked = {
                    navController.previousBackStackEntry?.let {
                        navController.popBackStack()
                    }
                },
                onImportFacility = {
                    if (isEdit) {
                        navController.previousBackStackEntry?.let {
                            navController.popBackStack(route = Navigation.FirstTimeSetup.route, false)
                        }
                    } else {
                        navController.previousBackStackEntry?.let {
                            navController.navigate(Navigation.Update.route)
                        }
                    }
                    val searchFacilityProfile = searchVm.uiState.value.facility
                    searchFacilityProfile?.let { //only set facility if searched facility is not null
                        vm.facilityProfile = searchVm.uiState.value.facility
                    }
                },
                onManuallyConfigureClicked = {
                    if (!isEdit) {
                        navController.popBackStack()
                        vm.moveToNextSegment()
                    }
                },
                facility = vm.facilityProfile ?: searchVmState.value.facility,
                isEdit = isEdit,
                onCancelClicked = {
                    searchVm.restorePreviousState()
                    searchVm.setFacilityId()
                    navController.popBackStack(route = Navigation.FirstTimeSetup.route, false)
                }

            )
        }

        composable(route = Navigation.Network.route) {
            EditScreen {
                NetworkSelectScreen(
                    isEdit = true,
                    onSaveClicked = {
                        navController.previousBackStackEntry?.let {
                            navController.popBackStack()
                        }
                    },
                    onCancelClicked = {
                        navController.previousBackStackEntry?.let {
                            navController.popBackStack()
                        }
                    },
                    onSkipFtsu = { showSkipFtsuDialog = true }
                )
            }
        }

        composable(route = Navigation.FacilityProfile.route) {
            EditScreen {
                FacilityScreen(
                    onNavigateToDetail = { navController.navigate(route = Navigation.FacilityDetail.route + "/true") },
                    vm = searchVm,
                    isEdit = true,
                    onSaveClicked = {
                        navController.previousBackStackEntry?.let {
                            navController.popBackStack()
                        }
                    },
                    onCancelClicked = {
                        navController.previousBackStackEntry?.let {
                            navController.popBackStack()
                        }
                    }
                )
            }
        }

        composable(route = Navigation.GeneralSettings.route) {
            EditScreen {
                SettingsView(
                    onTvSetupClicked = { vm.incrementSubsegment() },
                    currentSubsegment = null,
                    showLedColorOptions = true, //todo CGC-20448 this should be toggled based console type
                    onNextClicked = { },
                    isEdit = true,
                    onNavigateBack = {
                        navController.previousBackStackEntry?.let {
                            navController.popBackStack()
                        }
                    },
                    isEngineeringMode = vm.isEngineeringMode(),
                    onSubSubsegmentClicked = { vm.incrementSubsegment() },
                    isTvSetupNext = vm.isTunerSupported(),
                    isCalibrateNext =  vm.isFrameCalibratable(),
                )
            }
        }

        composable(
            route = Navigation.Machine.route + "/{isEdit}",
            arguments = listOf(navArgument("isEdit") { type = NavType.BoolType })
        ) { backstackEntry ->
            val isEdit = backstackEntry.arguments?.getBoolean("isEdit") ?: false
            EditScreen(onMESCodeEnter = vm::onMesCodeEnter) {
                EditMachineInfoView(
                    ftsuViewModel = vm,
                    onDiscard = {
                        vm.completeFrameSetup()
                        if (isEdit) {
                            navController.previousBackStackEntry?.let {
                                navController.popBackStack()
                            }
                        } else {
                            navController.previousBackStackEntry?.let {
                                navController.popBackStack(route = Navigation.FirstTimeSetup.route, inclusive = false)
                            }
                        }
                    },
                    isEdit = isEdit,
                )
            }
        }

        composable(route = Navigation.Update.route) {
            SoftwareUpdateView(
                navigateToMachineCalibration = { shouldUpdate ->
                    if (vm.isFrameCalibratable()) {
                        vm.navigateToSegment(type = SetupState.MACHINE_CALIBRATION)
                    } else {
                        vm.navigateToSegment(type = SetupState.SUMMARY)
                    }
                    navController.popBackStack(route = Navigation.FirstTimeSetup.route, inclusive = false)
                    shouldInstallUpdate = shouldUpdate
                },
            )
        }
        composable(route = Navigation.TVSetup.route) {
            EditScreen {
                TvSetupView(
                    onReturnToSummary = {
                        navController.previousBackStackEntry?.let {
                            navController.popBackStack()
                        }
                    },
                    isEdit = true,
                    isCalibrateNext = false, //This is for the edit flow, so the machine calibration will never be next in the flow from this edit version of the screen.
                    isEngineeringMode = vm.isEngineeringMode(),
                    tvSourceViewModel = tvSourceSelectionViewModel,
                    onExitEdit = {
                        navController.previousBackStackEntry?.let {
                            navController.popBackStack(route = Navigation.FirstTimeSetup.route, false)
                        }

                    },
                )
            }
        }

    }

}

/**
 * Wrapper around views to add matrix logo for detail flow
 */
@Composable
fun EditScreen(
    onMESCodeEnter: (code: Float) -> Boolean = { true },
    content: @Composable () -> Unit,
) {

    Column(
        modifier = Modifier
            .padding(32.dp.computeSize())
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(.12f)
        ) {
            FirstTimeSetupTopAppBar(
                modifier = Modifier.fillMaxHeight(),
                segments = emptyList(),
                isEngineeringMode = false,
                onMESCodeEnter = onMESCodeEnter,
                showProgressBar = false
            )
        }

        Spacer(Modifier.size(40.dp.computeSize()))

        content()
    }

}

/**
 * Handles routing based on @param [type]
 * @param [navController] -> Used to navigate
 */
private fun handleEditClicked(type: SummaryEditType, navController: NavController) {
    when (type) {
        SummaryEditType.FacilityProfile -> {
            navController.navigate(route = Navigation.FacilityProfile.route)
        }

        SummaryEditType.Machine -> {
            navController.navigate(route = Navigation.Machine.route + "/true")
        }

        SummaryEditType.Network -> {
            navController.navigate(route = Navigation.Network.route)
        }

        SummaryEditType.GeneralSettings -> {
            navController.navigate(route = Navigation.GeneralSettings.route)
        }

        SummaryEditType.TVSetup -> {
            navController.navigate(route = Navigation.TVSetup.route)
        }
    }
}