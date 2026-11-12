package com.jht.seca2.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jht.seca.ui.screens.SecaSummaryDetailScreen
import com.jht.seca2.SecaViewModel
import com.jht.seca2.utils.toEngage360FullStatList
import com.jht.seca2.utils.toEngage360QuickStatList

/** Screens for navigation in seca summary */
enum class SecaScreen(val route: String) {
    ReviewRatingScreen("review_rating_screen"),
    SecaSummaryDetailScreen("seca_summary_detail_screen"),
}

/** Composable that handles navigation between different screens of the seca survey */
@Composable
internal fun SecaNav(
    navController: NavHostController,
    secaViewModel: SecaViewModel,
    onExit: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = SecaScreen.SecaSummaryDetailScreen.route
    ) {
        //Hiding the 5-Star rating UI screen is implemented from the ticket CGC-22339
        //If people really don't want this screen, please remove this and its related functions.
//        composable(route = SecaScreen.ReviewRatingScreen.route) {
//            ReviewRatingScreen(
//                onClickExit = { onExit() },
//                onClickViewSummary = { navController.navTo(SecaScreen.SecaSummaryDetailScreen) },
//                onRatingChanged = {},
//            )
//        }

        composable(route = SecaScreen.SecaSummaryDetailScreen.route) {
            val quickStats = remember { secaViewModel.getQuickStats().toEngage360QuickStatList() }
            val fullStats = remember { secaViewModel.getDetailedStats().toEngage360FullStatList() }
            val recommendedExercisesList = remember { secaViewModel.getRecommendedExercisesList() }
            val userData = remember { secaViewModel.getSecaUserData() }

            SecaSummaryDetailScreen(
                quickStats = quickStats,
                fullStats = fullStats,
                onHome = { onExit() },
                strengthStats = null,
                recommendedExercisesList = recommendedExercisesList.recommendedExerciseList,
                scan = userData?.scan,
                initialPage = 0,
            )
        }
    }
}

private fun NavController.navTo(screen: SecaScreen) {
    navigate(route = screen.route)
}