package com.jht.enums

enum class WorkoutGoalType {
    GOAL_TYPE_NONE,
    GOAL_TYPE_TIME,
    GOAL_TYPE_DISTANCE,
    GOAL_TYPE_CALORIES,
    GOAL_TYPE_FLOORS;

    companion object {
        // TODO we need to eventually refactor code to use same enum type
        // Mapping const values to goal types in workout controller artifact
        // if the enum is not found, it will return GOAL_TYPE_TIME
        // Note: there are other goal types like elevation and steps but we don't seem to use them
        fun fromId(id: Int): WorkoutGoalType {
            return when (id) {
                0 -> GOAL_TYPE_NONE
                // GOAL_TYPE_TIME, GOAL_TYPE_TIME_STAGE
                1, 7 -> GOAL_TYPE_TIME
                // GOAL_TYPE_DISTANCE, GOAL_TYPE_DISTANCE_STAGE
                2, 8 -> GOAL_TYPE_DISTANCE
                // GOAL_TYPE_CALORIES, GOAL_TYPE_CALORIES_STAGE
                3, 9 -> GOAL_TYPE_CALORIES
                // GOAL_TYPE_FLOORS, GOAL_TYPE_FLOORS_STAGE
                4, 10 -> GOAL_TYPE_FLOORS
                else -> {GOAL_TYPE_TIME}
            }
        }
    }

    val isGoalNone: Boolean get() = this == GOAL_TYPE_NONE
    val isGoaTime: Boolean get() = this == GOAL_TYPE_TIME
    val isGoalDistance: Boolean get() = this == GOAL_TYPE_DISTANCE
    val isGoalCalories: Boolean get() = this == GOAL_TYPE_CALORIES
    val isGoalFloor: Boolean get() = this == GOAL_TYPE_FLOORS
}