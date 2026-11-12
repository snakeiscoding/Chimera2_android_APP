package com.jht.progression.ui.setup

enum class SetupScreens(val route: String) {
    // Assessment Screens
    AssessmentScreen("assessment_screen"),
    ManualTimeEntry("manual_time_entry"),

    // Returning User Screens
    WorkoutJourneyScreen("workout_journey_screen"),
    WorkoutPreviewScreen("workout_preview_screen"),
    NonWorkoutPreviewScreen("non_workout_preview_screen"),

    WorkoutSummaryScreen("workout_summary_screen"),
    ProgramSummaryScreen("program_summary_screen"),
    ProgramCompleteScreen("program_complete_screen"),
    GoalSettingsScreen("goal_settings_screen")
}