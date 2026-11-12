package com.jht.progression.userinfo

import android.util.Log
import com.jht.interfaces.models.UserModel
import com.jht.progression.interfaces.ProgressionModel
import com.jht.progression.data.EventLabel
import com.jht.progression.data.ProgramEvents
import com.jht.progression.data.WorkoutEvent
import com.jht.progression.data.getProgramOfType
import com.jht.progression.data.program.migrateProgramEvents
import com.jht.progression.data.program.programEventsNeedMigration
import com.jht.progression.util.zeroIndexed

private const val TAG = "UserInfoManager"

class UserInfoManager(
    private val userModel: UserModel,
    private val progressionModel: ProgressionModel,
) {
    var userInfo: UserInfo = UserInfo()
        private set

    /**
     * Loads saved user information for the workout program and updates the program data to the
     * most recent version.
     */
    fun loadAndUpdateUserInfo() {
        loadUserInfo()

        // migrate event data that doesn't exist (currently just adding race event)
        if (programEventsNeedMigration(userInfo.programEvents)) {
            Log.d(TAG, "Program info needs update, adding needed data")
            val updatedEvents = migrateProgramEvents(userInfo.programEvents)
            setProgramEvents(updatedEvents)
            writeUserInfoToFile()
        }
    }

    /**
     * Loads the user info for a specific program
     */
    fun loadUserInfo() {
        val offlineFileLocation = progressionModel.offlineFileLocation
        val localUserInfo = if (progressionModel.saveLocally && offlineFileLocation != null) {
            val programString = readUserProgramInfoFile(userModel.uuid.value, offlineFileLocation)
            if (programString != null) getUserInfoFromJsonString(programString) else null
        } else {
            null
        }

        val modelUserInfo = getUserInfoFromJsonString(progressionModel.progressionJsonString)

        // Decide to use local vs one from model
        userInfo = if (localUserInfo != null && localUserInfo.lastChangedTime >= modelUserInfo.lastChangedTime) {
            localUserInfo
        } else {
            modelUserInfo
        }
    }

    /**
     * Sets the baseline for a the user and writes that data to file.
     */
    fun setBaseline(baseline: Float) {
        userInfo.baselineKph = baseline
    }

    /**
     * Sets the user's initial baseline obtained through the program assessment
     */
    fun setAssessmentBaseline(baseline: Float) {
        userInfo.assessmentBaselineKph = baseline
    }

    /**
     * Sets the goal baseline and writes that data to file
     */
    fun setGoalBaseline(goalBaseline: Float?) {
        userInfo.goalBaselineKph = goalBaseline
        userInfo.goalIncrementKph = calculateGoalIncrement(goalBaseline)
    }

    fun setBackupGoalBaseline(goalBaseline: Float?) {
        userInfo.backupGoalBaseline = goalBaseline
    }

    fun setAutoAdjustGoalBaseline(enable: Boolean) {
        userInfo.autoAdjustGoalBaseline = enable
    }

    fun setGoalTimeInfoAcknowledged(acknowledged: Boolean) {
        userInfo.goalTimeInfoAcknowledged = acknowledged
    }

    /**
     * Get the increment for the goal after each workout
     */
    private fun calculateGoalIncrement(goalBaseline: Float?) : Float? {
        // increment is even amount between the current baseline and the end goal, split over the number of workouts remaining
        val remainingGoalIncrements = getNumGoalIncrementsRemaining()
        if (goalBaseline != null && goalBaseline > userInfo.baselineKph) {
            val speedDiff = goalBaseline - userInfo.baselineKph
            val speedIncrement = speedDiff / remainingGoalIncrements
            // Prevent returning infinity / NaN if recalculating goal increment with 0 workouts remaining.
            return if (speedIncrement.isFinite()) speedIncrement else null
        } else {
            return null
        }
    }

    /**
     * Get the number of goal increments remaining in the program
     */
    fun getNumGoalIncrementsRemaining() : Int {
        val currentTimeMs = System.currentTimeMillis()
        val program = getProgramOfType(userInfo.programType)
        val goalAdjustmentRange = program.workoutInterface.goalAdjustmentWorkoutRange.zeroIndexed()

        return userInfo.programEvents.events
            .filterIsInstance<WorkoutEvent>()
            .withIndex()
            .count { (index, workoutEvent) ->
                workoutEvent.label != EventLabel.RACE &&
                !workoutEvent.completed &&
                workoutEvent.endTime >= currentTimeMs &&
                index in goalAdjustmentRange
            }
    }

    /**
     * Returns the number of completed workouts for the user.
     *
     * @param includeMissedWorkouts If true, includes missed workouts (those with an end time before the current time) in the count.
     * @return The number of completed workouts, including missed ones if specified.
     */
    fun getNumOfCompletedWorkouts(includeMissedWorkouts: Boolean) : Int {
        val currentTimeMs = System.currentTimeMillis()
        return userInfo.programEvents.events
            .filterIsInstance<WorkoutEvent>()
            .count { it.label != EventLabel.RACE && (it.completed || (includeMissedWorkouts && it.endTime <= currentTimeMs)) }
    }

    /**
     * Sets program events and writes the updated user info to a file.
     */
    fun setProgramEvents(programEvents: ProgramEvents) {
        userInfo.programEvents = programEvents
    }

    /**
     * Overwrites the file used to store program info
     */
    fun resetProgram() {
        userInfo = UserInfo()
    }

    /**
     * Writes the user info object to a file. This will overwrite the previously stored file.
     */
    fun writeUserInfoToFile(updateLastSavedTime: Boolean = true) {
        // Update changed time since we are writing this now
        if (updateLastSavedTime) {
            userInfo.lastChangedTime = System.currentTimeMillis()
        }

        val programString = getUserProgramString(userInfo)
        val fileLocation = progressionModel.offlineFileLocation
        if (progressionModel.saveLocally && fileLocation != null) {
            writeUserProgramInfo(programString, userModel.uuid.value, fileLocation)
        }

        // Update the model so we can push this data to web if needed
        progressionModel.progressionJsonString = programString
    }

    /**
     * Deletes all Matrix Method data associated with a user. All programs will be removed.
     */
    fun deleteUserData() {
        val fileLocation = progressionModel.offlineFileLocation
        if (progressionModel.saveLocally && fileLocation != null) {
            deleteMatrixMethodUserData(userModel.uuid.value, fileLocation)
        }
    }

    fun isProgramStarted() : Boolean {
        return userInfo.baselineKph > 0
    }
}