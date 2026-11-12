package com.jht.progression.ui.workout

import com.jht.progression.R

/**
 * Structure and map 225 tip strings into the 8-week workouts and the Assessment workout.
 * There are only the static tips during the assessment workout and the workouts in week 1.
 * Workouts form week 2 to Week 8 contain both the static tips and the random tips
 * Each week has 3 workout days and their indexes are 0, 2, and 4.
 * Each workout has a different amount of intervals with unlike time durations.
 * A tip string will map to a specific time during a workout and display at that time.
 * All tips will last showing up for a minute if it is not closed.
 * */
fun mapTipsForWorkout(
    workoutIndex: Int
): Map<Int, Int> {
    usedRandomTips.clear()
    val week = workoutIndex.floorDiv(7)
    val workout = workoutIndex % 7
    return when (week) {
        0 -> {
            mapTipsForWeek1(workout)
        }

        1 -> {
            mapTipsForWeek2(week, workout)
        }

        2 -> {
            mapTipsForWeek3(week, workout)
        }

        3 -> {
            mapTipsForWeek4(week, workout)
        }

        4 -> {
            mapTipsForWeek5(week, workout)
        }

        5 -> {
            mapTipsForWeek6(week, workout)
        }

        6 -> {
            mapTipsForWeek7(week, workout)
        }

        7 -> {
            mapTipsForWeek8(week, workout)
        }

        8 -> {
            mapOf() // Race day does not have any tips
        }

        else -> {
            mapTipsForAssessment()
        }
    }
}

private const val MIN_TO_MS = 60000
private const val TIME_SHOWS_FIRST_TIP_MS = 5000
private val usedRandomTips = mutableSetOf<Int>()

private fun mapTipsForAssessment(): Map<Int, Int> {
    return mapOf(
        TIME_SHOWS_FIRST_TIP_MS to R.string.mm_assessment_tip1_pg,
        MIN_TO_MS * 2 to R.string.mm_assessment_tip2_pg,
        MIN_TO_MS * 4 to R.string.mm_assessment_tip3_pg,
        MIN_TO_MS * 8 to R.string.mm_assessment_tip4_pg,
        MIN_TO_MS * 12 to R.string.mm_assessment_tip5_pg,
        MIN_TO_MS * 16 to R.string.mm_assessment_tip6_pg,
        MIN_TO_MS * 20 to R.string.mm_assessment_tip7_pg,
        MIN_TO_MS * 22 to R.string.mm_assessment_tip8_pg
    )
}

private fun mapTipsForWeek1(workout: Int): Map<Int, Int> {
    return when (workout) {
        0 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week1_tip1_pg,
                MIN_TO_MS * 2 to R.string.mm_week1_tip2_pg,
                MIN_TO_MS * 4 to R.string.mm_week1_tip3_pg,
                MIN_TO_MS * 6 to R.string.mm_week1_tip4_pg,
                MIN_TO_MS * 9 to R.string.mm_week1_tip5_pg,
                MIN_TO_MS * 14 to R.string.mm_week1_tip6_pg,
                MIN_TO_MS * 19 to R.string.mm_week1_tip7_pg,
                MIN_TO_MS * 28 to R.string.mm_week1_tip8_pg,
                MIN_TO_MS * 31 to R.string.mm_week1_tip9_pg
            )
        }

        2 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week1_tip10_pg,
                MIN_TO_MS * 2 to R.string.mm_week1_tip11_pg,
                MIN_TO_MS * 4 to R.string.mm_week1_tip12_pg,
                MIN_TO_MS * 7 to R.string.mm_week1_tip13_pg,
                MIN_TO_MS * 13 to R.string.mm_week1_tip14_pg,
                MIN_TO_MS * 19 to R.string.mm_week1_tip15_pg,
                MIN_TO_MS * 21 to R.string.mm_week1_tip16_pg,
                MIN_TO_MS * 23 to R.string.mm_week1_tip17_pg
            )
        }

        4 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week1_tip18_pg,
                MIN_TO_MS * 2 to R.string.mm_week1_tip19_pg,
                MIN_TO_MS * 4 to R.string.mm_week1_tip20_pg,
                MIN_TO_MS * 10 to R.string.mm_week1_tip21_pg,
                MIN_TO_MS * 16 to R.string.mm_week1_tip22_pg,
                MIN_TO_MS * 20 to R.string.mm_week1_tip23_pg,
                MIN_TO_MS * 22 to R.string.mm_week1_tip24_pg
            )
        }

        else -> {
            mapOf()
        }
    }
}

private fun mapTipsForWeek2(
    week: Int,
    workout: Int
): Map<Int, Int> {
    return when (workout) {
        0 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week2_tip1_pg,
                MIN_TO_MS * 2 to R.string.mm_week2_tip2_pg,
                MIN_TO_MS * 4 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 7 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 12 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 16 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 22 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 26 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 31 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 35 to R.string.mm_week2_tip10_pg,
                MIN_TO_MS * 38 to R.string.mm_week2_tip11_pg
            )
        }

        2 -> {
            mapOf(
                MIN_TO_MS * 1 to R.string.mm_week2_tip12_pg,
                MIN_TO_MS * 3 to R.string.mm_week2_tip13_pg,
                MIN_TO_MS * 6 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 9 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 13 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 16 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 19 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 23 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 25 to R.string.mm_week2_tip20_pg
            )
        }

        4 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week2_tip21_pg,
                MIN_TO_MS * 2 to R.string.mm_week2_tip22_pg,
                MIN_TO_MS * 4 to R.string.mm_week2_tip23_pg,
                MIN_TO_MS * 7 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 12 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 16 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 20 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 24 to R.string.mm_week2_tip28_pg,
                MIN_TO_MS * 26 to R.string.mm_week2_tip29_pg
            )
        }

        else -> {
            mapOf()
        }
    }
}

private fun mapTipsForWeek3(
    week: Int,
    workout: Int
): Map<Int, Int> {
    return when (workout) {
        0 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week3_tip1_pg,
                MIN_TO_MS * 2 to R.string.mm_week3_tip2_pg,
                MIN_TO_MS * 4 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 9 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 15 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 22 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 31 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 39 to R.string.mm_week3_tip8_pg,
                MIN_TO_MS * 42 to R.string.mm_week3_tip9_pg
            )
        }

        2 -> {
            mapOf(
                MIN_TO_MS * 1 to R.string.mm_week3_tip10_pg,
                MIN_TO_MS * 4 to R.string.mm_week3_tip11_pg,
                MIN_TO_MS * 8 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 15 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 21 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 25 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 27 to R.string.mm_week3_tip16_pg,
                MIN_TO_MS * 29 to R.string.mm_week3_tip17_pg
            )
        }

        4 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week3_tip18_pg,
                MIN_TO_MS * 2 to R.string.mm_week3_tip19_pg,
                MIN_TO_MS * 5 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 12 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 20 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 28 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 32 to R.string.mm_week3_tip24_pg,
                MIN_TO_MS * 35 to R.string.mm_week3_tip25_pg
            )
        }

        else -> {
            mapOf()
        }
    }
}

private fun mapTipsForWeek4(
    week: Int,
    workout: Int
): Map<Int, Int> {
    return when (workout) {
        0 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week4_tip1_pg,
                MIN_TO_MS * 2 to R.string.mm_week4_tip2_pg,
                MIN_TO_MS * 4 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 10 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 18 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 26 to R.string.mm_week4_tip6_pg,
                MIN_TO_MS * 31 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 36 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 40 to R.string.mm_week4_tip9_pg,
                MIN_TO_MS * 43 to R.string.mm_week4_tip10_pg
            )
        }

        2 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week4_tip11_pg,
                MIN_TO_MS * 2 to R.string.mm_week4_tip12_pg,
                MIN_TO_MS * 4 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 8 to R.string.mm_week4_tip14_pg,
                MIN_TO_MS * 13 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 18 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 24 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 29 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 32 to R.string.mm_week4_tip19_pg
            )
        }

        4 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week4_tip20_pg,
                MIN_TO_MS * 2 to R.string.mm_week4_tip21_pg,
                MIN_TO_MS * 5 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 12 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 18 to R.string.mm_week4_tip24_pg,
                MIN_TO_MS * 24 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 30 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 35 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 37 to R.string.mm_week4_tip28_pg
            )
        }

        else -> {
            mapOf()
        }
    }
}

private fun mapTipsForWeek5(
    week: Int,
    workout: Int
): Map<Int, Int> {
    return when (workout) {
        0 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week5_tip1_pg,
                MIN_TO_MS * 2 to R.string.mm_week5_tip2_pg,
                MIN_TO_MS * 10 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 17 to R.string.mm_week5_tip4_pg,
                MIN_TO_MS * 23 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 31 to R.string.mm_week5_tip6_pg,
                MIN_TO_MS * 36 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 44 to R.string.mm_week5_tip8_pg,
                MIN_TO_MS * 47 to R.string.mm_week5_tip9_pg
            )
        }

        2 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week5_tip10_pg,
                MIN_TO_MS * 2 to R.string.mm_week5_tip11_pg,
                MIN_TO_MS * 4 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 8 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 13 to R.string.mm_week5_tip14_pg,
                MIN_TO_MS * 18 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 23 to R.string.mm_week5_tip16_pg,
                MIN_TO_MS * 28 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 31 to R.string.mm_week5_tip18_pg
            )
        }

        4 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week5_tip19_pg,
                MIN_TO_MS * 2 to R.string.mm_week5_tip20_pg,
                MIN_TO_MS * 4 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 9 to R.string.mm_week5_tip22_pg,
                MIN_TO_MS * 16 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 23 to R.string.mm_week5_tip24_pg,
                MIN_TO_MS * 29 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 36 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 40 to R.string.mm_week5_tip27_pg,
                MIN_TO_MS * 42 to R.string.mm_week5_tip28_pg
            )
        }

        else -> {
            mapOf()
        }
    }
}

private fun mapTipsForWeek6(
    week: Int,
    workout: Int
): Map<Int, Int> {
    return when (workout) {
        0 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week6_tip1_pg,
                MIN_TO_MS * 2 to R.string.mm_week6_tip2_pg,
                MIN_TO_MS * 5 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 11 to R.string.mm_week6_tip4_pg,
                MIN_TO_MS * 19 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 28 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 36 to R.string.mm_week6_tip7_pg,
                MIN_TO_MS * 43 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 49 to R.string.mm_week6_tip9_pg
            )
        }

        2 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week6_tip10_pg,
                MIN_TO_MS * 2 to R.string.mm_week6_tip11_pg,
                MIN_TO_MS * 4 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 9 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 15 to R.string.mm_week6_tip14_pg,
                MIN_TO_MS * 20 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 25 to R.string.mm_week6_tip16_pg,
                MIN_TO_MS * 29 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 33 to R.string.mm_week6_tip18_pg
            )
        }

        4 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week6_tip19_pg,
                MIN_TO_MS * 2 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 4 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 10 to R.string.mm_week6_tip22_pg,
                MIN_TO_MS * 17 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 25 to R.string.mm_week6_tip24_pg,
                MIN_TO_MS * 32 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 39 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 42 to R.string.mm_week6_tip27_pg
            )
        }

        else -> {
            mapOf()
        }
    }
}

private fun mapTipsForWeek7(
    week: Int,
    workout: Int
): Map<Int, Int> {
    return when (workout) {
        0 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week7_tip1_pg,
                MIN_TO_MS * 2 to R.string.mm_week7_tip2_pg,
                MIN_TO_MS * 4 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 9 to R.string.mm_week7_tip4_pg,
                MIN_TO_MS * 17 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 22 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 28 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 35 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 40 to R.string.mm_week7_tip9_pg,
                MIN_TO_MS * 48 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 50 to R.string.mm_week7_tip11_pg
            )
        }

        2 -> {
            mapOf(
                MIN_TO_MS * 1 to R.string.mm_week7_tip12_pg,
                MIN_TO_MS * 4 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 9 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 15 to R.string.mm_week7_tip15_pg,
                MIN_TO_MS * 20 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 25 to R.string.mm_week7_tip17_pg,
                MIN_TO_MS * 29 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 32 to R.string.mm_week7_tip19_pg
            )
        }

        4 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week7_tip20_pg,
                MIN_TO_MS * 3 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 8 to R.string.mm_week7_tip22_pg,
                MIN_TO_MS * 15 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 24 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 36 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 43 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 45 to R.string.mm_week7_tip27_pg
            )
        }

        else -> {
            mapOf()
        }
    }
}

private fun mapTipsForWeek8(
    week: Int,
    workout: Int
): Map<Int, Int> {
    return when (workout) {
        0 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week8_tip1_pg,
                MIN_TO_MS * 2 to R.string.mm_week8_tip2_pg,
                MIN_TO_MS * 4 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 10 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 17 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 25 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 34 to R.string.mm_week8_tip7_pg,
                MIN_TO_MS * 41 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 48 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 50 to R.string.mm_week8_tip10_pg
            )
        }

        2 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week8_tip11_pg,
                MIN_TO_MS * 2 to R.string.mm_week8_tip12_pg,
                MIN_TO_MS * 4 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 8 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 14 to R.string.mm_week8_tip15_pg,
                MIN_TO_MS * 20 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 27 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 32 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 35 to R.string.mm_week8_tip19_pg
            )
        }

        4 -> {
            mapOf(
                TIME_SHOWS_FIRST_TIP_MS to R.string.mm_week8_tip20_pg,
                MIN_TO_MS * 3 to R.string.mm_week8_tip21_pg,
                MIN_TO_MS * 6 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 12 to R.string.mm_week8_tip23_pg,
                MIN_TO_MS * 18 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 25 to R.string.mm_week8_tip25_pg,
                MIN_TO_MS * 30 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 37 to getRandomTipFromCurrentWeek(week),
                MIN_TO_MS * 43 to R.string.mm_week8_tip28_pg,
                MIN_TO_MS * 46 to R.string.mm_week8_tip29_pg
            )
        }

        else -> {
            mapOf()
        }
    }
}

/**
 * Create the random tip list for week 2 to week 8 and return a random tip from the current week
 * The assessment and week 1 are all static tips
 * */
private fun getRandomTipFromCurrentWeek(week: Int): Int {
    val randomTipFromWeek2 = listOf(
        R.string.mm_week2_tip3_random_pg,
        R.string.mm_week2_tip4_random_pg,
        R.string.mm_week2_tip5_random_pg,
        R.string.mm_week2_tip6_random_pg,
        R.string.mm_week2_tip7_random_pg,
        R.string.mm_week2_tip8_random_pg,
        R.string.mm_week2_tip9_random_pg,
        R.string.mm_week2_tip14_random_pg,
        R.string.mm_week2_tip15_random_pg,
        R.string.mm_week2_tip16_random_pg,
        R.string.mm_week2_tip17_random_pg,
        R.string.mm_week2_tip18_random_pg,
        R.string.mm_week2_tip19_random_pg,
        R.string.mm_week2_tip24_random_pg,
        R.string.mm_week2_tip25_random_pg,
        R.string.mm_week2_tip26_random_pg,
        R.string.mm_week2_tip27_random_pg,
    )
    val randomTipsFromWeek3 = listOf(
        R.string.mm_week3_tip3_random_pg,
        R.string.mm_week3_tip4_random_pg,
        R.string.mm_week3_tip5_random_pg,
        R.string.mm_week3_tip6_random_pg,
        R.string.mm_week3_tip7_random_pg,
        R.string.mm_week3_tip12_random_pg,
        R.string.mm_week3_tip13_random_pg,
        R.string.mm_week3_tip14_random_pg,
        R.string.mm_week3_tip15_random_pg,
        R.string.mm_week3_tip20_random_pg,
        R.string.mm_week3_tip21_random_pg,
        R.string.mm_week3_tip22_random_pg,
        R.string.mm_week3_tip23_random_pg,
    )
    val randomTipsFromWeek4 = listOf(
        R.string.mm_week4_tip3_random_pg,
        R.string.mm_week4_tip4_random_pg,
        R.string.mm_week4_tip5_random_pg,
        R.string.mm_week4_tip7_random_pg,
        R.string.mm_week4_tip8_random_pg,
        R.string.mm_week4_tip13_random_pg,
        R.string.mm_week4_tip15_random_pg,
        R.string.mm_week4_tip16_random_pg,
        R.string.mm_week4_tip17_random_pg,
        R.string.mm_week4_tip18_random_pg,
        R.string.mm_week4_tip22_random_pg,
        R.string.mm_week4_tip23_random_pg,
        R.string.mm_week4_tip25_random_pg,
        R.string.mm_week4_tip26_random_pg,
        R.string.mm_week4_tip27_random_pg
    )
    val randomTipsFromWeek5 = listOf(
        R.string.mm_week5_tip3_random_pg,
        R.string.mm_week5_tip5_random_pg,
        R.string.mm_week5_tip7_random_pg,
        R.string.mm_week5_tip12_random_pg,
        R.string.mm_week5_tip13_random_pg,
        R.string.mm_week5_tip15_random_pg,
        R.string.mm_week5_tip17_random_pg,
        R.string.mm_week5_tip21_random_pg,
        R.string.mm_week5_tip23_random_pg,
        R.string.mm_week5_tip25_random_pg,
        R.string.mm_week5_tip26_random_pg
    )
    val randomTipsFromWeek6 = listOf(
        R.string.mm_week6_tip3_random_pg,
        R.string.mm_week6_tip5_random_pg,
        R.string.mm_week6_tip6_random_pg,
        R.string.mm_week6_tip8_random_pg,
        R.string.mm_week6_tip12_random_pg,
        R.string.mm_week6_tip13_random_pg,
        R.string.mm_week6_tip15_random_pg,
        R.string.mm_week6_tip17_random_pg,
        R.string.mm_week6_tip20_random_pg,
        R.string.mm_week6_tip21_random_pg,
        R.string.mm_week6_tip23_random_pg,
        R.string.mm_week6_tip25_random_pg,
        R.string.mm_week6_tip26_random_pg
    )
    val randomTipsFromWeek7 = listOf(
        R.string.mm_week7_tip3_random_pg,
        R.string.mm_week7_tip5_random_pg,
        R.string.mm_week7_tip6_random_pg,
        R.string.mm_week7_tip7_random_pg,
        R.string.mm_week7_tip8_random_pg,
        R.string.mm_week7_tip10_random_pg,
        R.string.mm_week7_tip13_random_pg,
        R.string.mm_week7_tip14_random_pg,
        R.string.mm_week7_tip16_random_pg,
        R.string.mm_week7_tip18_random_pg,
        R.string.mm_week7_tip21_random_pg,
        R.string.mm_week7_tip23_random_pg,
        R.string.mm_week7_tip24_random_pg,
        R.string.mm_week7_tip25_random_pg,
        R.string.mm_week7_tip26_random_pg
    )
    val randomTipsFromWeek8 = listOf(
        R.string.mm_week8_tip3_random_pg,
        R.string.mm_week8_tip4_random_pg,
        R.string.mm_week8_tip5_random_pg,
        R.string.mm_week8_tip6_random_pg,
        R.string.mm_week8_tip8_random_pg,
        R.string.mm_week8_tip9_random_pg,
        R.string.mm_week8_tip13_random_pg,
        R.string.mm_week8_tip14_random_pg,
        R.string.mm_week8_tip16_random_pg,
        R.string.mm_week8_tip17_random_pg,
        R.string.mm_week8_tip18_random_pg,
        R.string.mm_week8_tip22_random_pg,
        R.string.mm_week8_tip24_random_pg,
        R.string.mm_week8_tip26_random_pg,
        R.string.mm_week8_tip27_random_pg
    )

    val randomTipsFromCurrentWeek = when (week) {
        1 -> randomTipFromWeek2
        2 -> randomTipsFromWeek3
        3 -> randomTipsFromWeek4
        4 -> randomTipsFromWeek5
        5 -> randomTipsFromWeek6
        6 -> randomTipsFromWeek7
        7 -> randomTipsFromWeek8
        else -> {
            listOf()
        }
    }

    return if (randomTipsFromCurrentWeek.isNotEmpty()) {
        val unusedRandomTips = randomTipsFromCurrentWeek.filterNot { usedRandomTips.contains(it) }
        val randomTip = unusedRandomTips.random()
        usedRandomTips.add(randomTip)
        randomTip
    } else {
        0
    }
}
