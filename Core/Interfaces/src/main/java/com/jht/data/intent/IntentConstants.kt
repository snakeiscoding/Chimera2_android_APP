@file:JvmName("IntentConstants")

package com.jht.data.intent

/*
    File contains constants used for Intent extras that may be passed to some UI modules.
 */


/** Boolean: if true indicates that the receiver is to operate as the home screen. Typical receiver is go-screen/workout-selection-screen. */
const val GoScreenIsHomeBool = "jht.goscreen.ishome"

/** Added to start intent when launching as part of workout summary */
const val WorkoutEndWorkoutIdInt = "jht.workoutEnd.workoutId"
