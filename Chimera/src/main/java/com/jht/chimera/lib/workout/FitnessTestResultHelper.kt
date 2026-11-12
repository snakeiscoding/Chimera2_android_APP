package com.jht.chimera.lib.workout

import android.content.Context
import android.util.Log
import com.jht.chimera.R
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.common.formatting.FloorFormat
import com.jht.chimera.lib.common.stats.SummaryData
import com.jht.datamatrixlib.DataMatrix
import java.util.Locale
import kotlin.math.roundToInt

private const val TAG = "FitnessTestResultHelper"

class FitnessTestResultHelper(
    val context: Context,
    val summaryData: SummaryData
) {
    fun getCooperRating(): String? {
        val score: Int = getCooperStagesCompleted()
        return if (score < 3) {
            context.getString(R.string.cooper_rating_well_below_average_uc)
        } else if (score < 5) {
            context.getString(R.string.cooper_rating_below_average_uc)
        } else if (score < 7) {
            context.getString(R.string.average_uc)
        } else if (score < 9) {
            context.getString(R.string.cooper_rating_above_average_uc)
        } else {
            context.getString(R.string.cooper_rating_well_above_average_uc)
        }
    }

    fun getCooperStagesCompleted(): Int {
        val secondsInWorkout: Float = summaryData.workoutSummary.statsAccumulated.secondsStateRun
        // Determine how many 2 minute periods have passed in the workout
        return (secondsInWorkout / 120).toInt()
    }

    fun getGerkinRating(): String {
        val gerkinPercentMale = DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_MALE_FLOAT)
        val gerkinPercentFemale = DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_FEMALE_FLOAT)
        val gender = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_GENDER_INT)
        return when (gender) {
            ConsoleWorkoutVariables.GENDER_MALE_CONSTANT -> {
                context.getString(R.string.rating_tc) + ": " + getGerkinRatingByPercent(gerkinPercentMale)
            }
            ConsoleWorkoutVariables.GENDER_FEMALE_CONSTANT -> {
                context.getString(R.string.rating_tc) + ": " + getGerkinRatingByPercent(gerkinPercentFemale)
            }
            else -> {
                (context.getString(R.string.male_tc) + ": " + getGerkinRatingByPercent(gerkinPercentMale) + " | "
                    + context.getString(R.string.female_tc) + ": " + getGerkinRatingByPercent(gerkinPercentFemale))
            }
        }
    }
    private fun getGerkinRatingByPercent(percentile: Float): String {
        return if (percentile >= 95f) {
            context.getString(R.string.gerkin_rating_superior_uc)
        } else if (percentile >= 80f) {
            context.getString(R.string.excellent_uc)
        } else if (percentile >= 60f) {
            context.getString(R.string.good_uc)
        } else if (percentile >= 40f) {
            context.getString(R.string.fair_uc)
        } else if (percentile >= 20) {
            context.getString(R.string.gerkin_rating_poor_uc)
        } else {
            context.getString(R.string.gerkin_rating_very_poor_uc)
        }
    }

    fun getScoredFitnessTestResult(max: Int): String {
        val testScoreMale = DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_MALE_FLOAT).roundToInt()
        val testScoreFemale = DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_FEMALE_FLOAT).roundToInt()
        val gender = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_GENDER_INT)
        return when (gender) {
            ConsoleWorkoutVariables.GENDER_MALE_CONSTANT -> {
                context.getString(R.string.score_tc) + ": " + testScoreMale + "/" + max
            }
            ConsoleWorkoutVariables.GENDER_FEMALE_CONSTANT -> {
                context.getString(R.string.score_tc) + ": " + testScoreFemale + "/" + max
            }
            else -> {
                (context.getString(R.string.male_tc) + ": " + testScoreMale + "/" + max + " | "
                        + context.getString(R.string.female_tc) + ": " + testScoreFemale + "/" + max)
            }
        }
    }

    fun getCPATTestResult(max: Int): String {
        val score = DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_FLOAT).roundToInt()
        return context.getString(R.string.score_tc) + ": " + score + "/" + max
    }

    fun getAirForceTestResult(): String {
        val testScoreMale = DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_MALE_FLOAT)
        val testScoreFemale = DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_FEMALE_FLOAT)
        val gender = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_GENDER_INT)
        return when (gender) {
            ConsoleWorkoutVariables.GENDER_MALE_CONSTANT -> {
                context.getString(R.string.score_tc) + ": " + testScoreMale + "/" + 60.0
            }
            ConsoleWorkoutVariables.GENDER_FEMALE_CONSTANT -> {
                context.getString(R.string.score_tc) + ": " + testScoreFemale + "/" + 60.0
            }
            else -> {
                (context.getString(R.string.male_tc) + ": " + testScoreMale + "/" + 60.0 + " | "
                        + context.getString(R.string.female_tc) + ": " + testScoreFemale + "/" + 60.0)
            }
        }
    }

    fun getNavyTestResult(): String {
        val testScoreMale = DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_MALE_FLOAT).roundToInt()
        val testScoreFemale = DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_FEMALE_FLOAT).roundToInt()
        val gender = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_GENDER_INT)
        val ratingMale: String = getNavyScoreString(testScoreMale)
        val ratingFemale: String = getNavyScoreString(testScoreFemale)
        return when (gender) {
            ConsoleWorkoutVariables.GENDER_MALE_CONSTANT -> {
                context.getString(R.string.rating_tc) + ": " + ratingMale
            }
            ConsoleWorkoutVariables.GENDER_FEMALE_CONSTANT -> {
                context.getString(R.string.rating_tc) + ": " + ratingFemale
            }
            else -> {
                context.getString(R.string.male_tc) + ": " + ratingMale + " | " + context.getString(R.string.female_tc) + ": " + ratingFemale
            }
        }
    }
    private fun getNavyScoreString(score: Int): String {
        return when (score) {
            12 -> context.getString(R.string.navy_rating_outstanding_high_uc)
            11 -> context.getString(R.string.navy_rating_outstanding_medium_uc)
            10 -> context.getString(R.string.navy_rating_outstanding_low_uc)
            9 -> context.getString(R.string.navy_rating_excellent_high_uc)
            8 -> context.getString(R.string.navy_rating_excellent_medium_uc)
            7 -> context.getString(R.string.navy_rating_excellent_low_uc)
            6 -> context.getString(R.string.navy_rating_good_high_uc)
            5 -> context.getString(R.string.navy_rating_good_medium_uc)
            4 -> context.getString(R.string.navy_rating_good_low_uc)
            3 -> context.getString(R.string.navy_rating_satisfactory_high_uc)
            2 -> context.getString(R.string.navy_rating_satisfactory_medium_uc)
            1 -> context.getString(R.string.navy_rating_probationary_uc)
            else -> context.getString(R.string.navy_rating_probationary_uc)
        }
    }

    fun getWfiStatus(): String {
        val testScore = DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_FLOAT).roundToInt()
        return if (testScore == ConsoleWorkoutVariables.WFI_TEST_SUCCESSFUL) {
            val vo2 = DataMatrix.getFloat(WorkoutKeys.DATA_VO2_FLOAT)
            context.getString(R.string.max_vo2_tc) + ": " + FloorFormat.format(vo2, 1)
        } else if (testScore == ConsoleWorkoutVariables.WFI_TEST_FAILED_HR_TOO_LOW) {
            context.getString(R.string.hr_too_low_tc)
        } else if (testScore == ConsoleWorkoutVariables.WFI_TEST_STOPPED) {
            context.getString(R.string.test_failed_tc)
        } else if (testScore == ConsoleWorkoutVariables.WFI_TEST_NO_HR_INCREASE) {
            context.getString(R.string.hr_too_low_tc)
        } else if (testScore == ConsoleWorkoutVariables.WFI_NO_HR_FOUND) {
            context.getString(R.string.no_hr_detected_pg).uppercase(Locale.getDefault())
        } else {
            context.getString(R.string.test_failed_tc)
        }
    }

    fun getSubmaxRating(): String {
        val submaxEnd = DataMatrix.getInt(WorkoutKeys.DATA_SUBMAX_OUTPUT_INT)
        return if (submaxEnd == 0) {
            val ratingMale = DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_MALE_FLOAT).roundToInt()
            val ratingFemale = DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_FEMALE_FLOAT).roundToInt()
            val gender = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_GENDER_INT)
            when (gender) {
                ConsoleWorkoutVariables.GENDER_MALE_CONSTANT -> getSubmaxRating(ratingMale)
                ConsoleWorkoutVariables.GENDER_FEMALE_CONSTANT -> getSubmaxRating(ratingFemale)
                else -> {
                    context.getString(R.string.male_tc) + ": " + getSubmaxRating(ratingMale) + " | " +
                            context.getString(R.string.female_tc) + ": " + getSubmaxRating(ratingFemale)
                }
            }
        } else if (submaxEnd == 2) {
            context.getString(R.string.hr_no_increase_tc)
        } else {
            context.getString(R.string.hr_too_low_tc)
        }
    }
    private fun getSubmaxRating(rating: Int): String {
        return when (rating) {
            0 -> context.getString(R.string.submax_rating_low_uc)
            1 -> context.getString(R.string.fair_uc)
            2 -> context.getString(R.string.average_uc)
            3 -> context.getString(R.string.good_uc)
            4 -> context.getString(R.string.excellent_uc)
            else -> context.getString(R.string.submax_rating_low_uc)
        }
    }

    fun getFtpScore(): String {
        val scriptTestScore = DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_FLOAT)
        val roundedFtpScore = scriptTestScore.roundToInt()
        Log.d(TAG, "FTP $roundedFtpScore")
        if (roundedFtpScore > 5) {
            DataMatrix.putInt(DAPIKeys.ACTION_UPDATE_USER_FTP_INT, roundedFtpScore)
        }
        return context.getString(R.string.ftp_watts_tc) + ": " + roundedFtpScore
    }

    fun getMaxHRScore(): String {
        // Holding place for when this code
        val secondsInWorkout: Float = summaryData.workoutSummary.statsAccumulated.secondsStateRun
        var scoreMultiplier = 0f
        if (secondsInWorkout >= 5 * 60 + 59 && secondsInWorkout <= 6 * 60 + 1) scoreMultiplier = 1f

        // This calculation is done within the workout script
        val testScore =
            Math.round(DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_FLOAT) * scoreMultiplier)
        // Only publish score if it is real - this triggers a dapi save
        if (testScore > 40) {
            DataMatrix.putInt(DAPIKeys.ACTION_UPDATE_USER_MAXHR_INT, testScore)
        } else {
            return context.getString(R.string.hr_too_low_tc).uppercase(Locale.getDefault())
        }
        return context.getString(R.string.max_hr_result_uc).uppercase(Locale.getDefault()) + ": " + testScore
    }

    fun getStepperRating(): String {
        val testScoreMale = DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_MALE_FLOAT).roundToInt()
        val testScoreFemale = DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_FEMALE_FLOAT).roundToInt()
        val gender = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_GENDER_INT)
        return when (gender) {
            ConsoleWorkoutVariables.GENDER_MALE_CONSTANT -> getStepperRating(testScoreMale)
            ConsoleWorkoutVariables.GENDER_FEMALE_CONSTANT -> getStepperRating(testScoreFemale)
            else -> {
                context.getString(R.string.male_tc) + ": " + getStepperRating(testScoreMale) + " | " +
                        context.getString(R.string.female_tc) + ": " + getStepperRating(testScoreFemale)
            }
        }
    }
    private fun getStepperRating(rating: Int): String {
        return when (rating) {
            1 -> context.getString(R.string.gerkin_rating_poor_uc)
            2 -> context.getString(R.string.cooper_rating_below_average_uc)
            3 -> context.getString(R.string.average_uc)
            4 -> context.getString(R.string.cooper_rating_above_average_uc)
            5 -> context.getString(R.string.good_uc)
            6 -> context.getString(R.string.excellent_uc)
            0 -> context.getString(R.string.test_failed_tc)
            else -> context.getString(R.string.test_failed_tc)
        }
    }

}
