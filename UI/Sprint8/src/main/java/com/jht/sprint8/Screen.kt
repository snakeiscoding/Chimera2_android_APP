package com.jht.sprint8

/**
 * Represents the different navigation screens in the Sprint8 workout flow.
 * Each object corresponds to a unique route used by the navigation
 * component to navigate between screens.
 * @property route
 * The unique string identifier for the navigation route.
 */
sealed class Screen(val route: String) {
    data object Sprint8WorkoutSelectionScreen: Screen("sprint8_workout_selection_screen")
    data object Sprint8InformationScreen: Screen("sprint8_information_screen")
    data object Sprint8ExperienceScreen: Screen("sprint8_experience_screen")
    data object AssessmentLevelSelectionScreen: Screen("assessment_level_selection_screen")
    data object Sprint8PreviewScreen: Screen("sprint8_preview_screen")
    data object Sprint8NavGraph: Screen("sprint8_graph")
    data object AssessmentInformationScreen: Screen("assessment_info_screen")
}

/**
 * List of screen routes where the top bar banner should be displayed.
 */
val topBarBannerScreens = listOf(
    Screen.AssessmentLevelSelectionScreen.route,
    Screen.Sprint8PreviewScreen.route,
    Screen.Sprint8ExperienceScreen.route,
    Screen.Sprint8InformationScreen.route,
    Screen.AssessmentInformationScreen.route

)
