package com.jht.chimera.service.dapi

import com.jht.enums.ProgramType
import com.jht.oauthlib.api.exerciser.MatrixMethodEvent
import com.jht.oauthlib.api.exerciser.MatrixMethodProgram
import com.jht.oauthlib.api.exerciser.MatrixMethodProgramEvents
import com.jht.progression.data.EventLabel
import com.jht.progression.data.NonWorkoutEvent
import com.jht.progression.data.ProgramEvents
import com.jht.progression.data.WorkoutEvent
import com.jht.progression.userinfo.UserInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests the translation between JUMP's Matrix Method data classes (oauthlib) and the console's [UserInfo]. */
class MatrixMethodProgramMapperTest {

    // Shape of a real GET /matrix_method/program response
    private val jumpProgram = MatrixMethodProgram(
        id = "2ea289d7-0aaf-4203-9b26-d3d866ec8242",
        programId = 0,
        updatedAt = "2026-08-04T21:34:34.483Z",
        baselineKph = 14.987178f,
        assessmentBaselineKph = 14.987178f,
        goalBaselineKph = 15.614795f,
        goalIncrementKph = 0.034867603f,
        backupGoalBaseline = 15.614795f,
        goalTimeAcknowledged = true,
        programEvents = MatrixMethodProgramEvents(
            listOf(
                MatrixMethodEvent(index = 0, completed = true, label = 0, startTime = 1785733200000, endTime = 1785733200000),
                MatrixMethodEvent(index = 1, completed = false, label = 3, startTime = 1785733200000, endTime = 1785733200000),
            )
        ),
    )

    @Test
    fun jumpProgramToUserInfo_mapsRenamedFields() {
        val userInfo = jumpProgramToUserInfo(jumpProgram, nowMs = 5000L)!!

        // programId -> programType by enum id
        assertEquals(ProgramType.Program5k, userInfo.programType)
        // goalTimeAcknowledged -> goalTimeInfoAcknowledged
        assertTrue(userInfo.goalTimeInfoAcknowledged)
        assertEquals(14.987178f, userInfo.baselineKph)
        assertEquals(15.614795f, userInfo.goalBaselineKph)
        assertEquals(0.034867603f, userInfo.goalIncrementKph)
        assertEquals(15.614795f, userInfo.backupGoalBaseline)
        assertEquals(2, userInfo.programEvents.events.size)
    }

    @Test
    fun jumpProgramToUserInfo_usesUpdatedAtForLastChangedTime() {
        val userInfo = jumpProgramToUserInfo(jumpProgram, nowMs = 5000L)!!
        assertEquals(1785879274483L, userInfo.lastChangedTime)
    }

    @Test
    fun jumpProgramToUserInfo_fallsBackToNowWhenNoUpdatedAt() {
        val userInfo = jumpProgramToUserInfo(jumpProgram.copy(updatedAt = null), nowMs = 5000L)!!
        assertEquals(5000L, userInfo.lastChangedTime)
    }

    @Test
    fun jumpProgramToUserInfo_fallsBackToNowWhenUpdatedAtUnparseable() {
        val userInfo = jumpProgramToUserInfo(jumpProgram.copy(updatedAt = "not a date"), nowMs = 5000L)!!
        assertEquals(5000L, userInfo.lastChangedTime)
    }

    @Test
    fun jumpProgramToUserInfo_returnsNullOnUnknownProgramId() {
        assertNull(jumpProgramToUserInfo(jumpProgram.copy(programId = 99), nowMs = 1L))
    }

    @Test
    fun isActiveJumpPlan_falseForEmptyShell() {
        assertFalse(isActiveJumpPlan(jumpProgram.copy(baselineKph = 0f, programEvents = MatrixMethodProgramEvents(emptyList()))))
    }

    @Test
    fun isActiveJumpPlan_trueWithEventsOrBaseline() {
        assertTrue(isActiveJumpPlan(jumpProgram))
        assertTrue(isActiveJumpPlan(jumpProgram.copy(baselineKph = 8.5f, programEvents = MatrixMethodProgramEvents(emptyList()))))
    }

    @Test
    fun eventsUpdate_clampsOversizedTimestamps() {
        val userInfo = UserInfo(programEvents = ProgramEvents(listOf(
            NonWorkoutEvent(index = 0, startTime = 0, endTime = Long.MAX_VALUE, completed = false, label = EventLabel.REST)
        )))
        val update = userInfoToJumpEventsUpdate(userInfo, "prog-1")!!
        assertEquals(9007199254740991L, update.eventsToUpdate["0"]?.endTime)
    }

    @Test
    fun eventsUpdate_keysByEventIndex() {
        val userInfo = UserInfo(programEvents = ProgramEvents(listOf(
            NonWorkoutEvent(index = 0, startTime = 0, endTime = 0, completed = false, label = EventLabel.REST),
            NonWorkoutEvent(index = 2, startTime = 0, endTime = 0, completed = false, label = EventLabel.REST),
        )))
        val update = userInfoToJumpEventsUpdate(userInfo, "prog-1")!!
        assertEquals("prog-1", update.id)
        assertTrue(update.eventsToUpdate.keys.containsAll(setOf("0", "2")))
    }

    @Test
    fun eventsUpdate_returnsNullWhenNoEvents() {
        assertNull(userInfoToJumpEventsUpdate(UserInfo(programEvents = ProgramEvents(emptyList())), "prog-1"))
    }

    @Test
    fun eventsUpdate_includesOnlyChangedEvents() {
        val previous = UserInfo(programEvents = ProgramEvents(listOf(
            NonWorkoutEvent(index = 0, startTime = 0, endTime = 0, completed = false, label = EventLabel.REST),
            NonWorkoutEvent(index = 1, startTime = 0, endTime = 0, completed = false, label = EventLabel.REST),
        )))
        val current = UserInfo(programEvents = ProgramEvents(listOf(
            NonWorkoutEvent(index = 0, startTime = 0, endTime = 0, completed = false, label = EventLabel.REST),
            NonWorkoutEvent(index = 1, startTime = 0, endTime = 0, completed = true, label = EventLabel.REST),
        )))

        val update = userInfoToJumpEventsUpdate(current, "prog-1", previous)!!

        assertEquals(1, update.eventsToUpdate.size)
        assertTrue(update.eventsToUpdate["1"]!!.completed)
    }

    @Test
    fun eventsUpdate_returnsNullWhenNothingChanged() {
        val same = UserInfo(programEvents = ProgramEvents(listOf(
            NonWorkoutEvent(index = 0, startTime = 0, endTime = 0, completed = true, label = EventLabel.REST)
        )))
        assertNull(userInfoToJumpEventsUpdate(same, "prog-1", same))
    }

    @Test
    fun eventsUpdate_sendsNullForEventsDroppedFromPrevious() {
        val previous = UserInfo(programEvents = ProgramEvents(listOf(
            NonWorkoutEvent(index = 0, startTime = 0, endTime = 0, completed = false, label = EventLabel.REST),
            NonWorkoutEvent(index = 1, startTime = 0, endTime = 0, completed = false, label = EventLabel.REST),
        )))
        val current = UserInfo(programEvents = ProgramEvents(listOf(
            NonWorkoutEvent(index = 0, startTime = 0, endTime = 0, completed = false, label = EventLabel.REST),
        )))
        val update = userInfoToJumpEventsUpdate(current, "prog-1", previous)!!
        assertTrue(update.eventsToUpdate.containsKey("1"))
        assertNull(update.eventsToUpdate["1"])
    }

    @Test
    fun eventsUpdate_sendsAllEventsWhenNoPreviousState() {
        val current = UserInfo(programEvents = ProgramEvents(listOf(
            NonWorkoutEvent(index = 0, startTime = 0, endTime = 0, completed = false, label = EventLabel.REST),
            NonWorkoutEvent(index = 1, startTime = 0, endTime = 0, completed = false, label = EventLabel.REST),
        )))
        val update = userInfoToJumpEventsUpdate(current, "prog-1", previous = null)!!
        assertEquals(2, update.eventsToUpdate.size)
    }

    @Test
    fun createProgram_coercesNullNumericFieldsToZero() {
        // JUMP 400s with "<field> must be a number" if these are sent as null
        val userInfo = UserInfo(goalBaselineKph = null, goalIncrementKph = null, backupGoalBaseline = null)

        val program = userInfoToJumpProgram(userInfo, "prog-1")
        assertEquals(0f, program.goalBaselineKph)
        assertEquals(0f, program.goalIncrementKph)
        assertEquals(0f, program.backupGoalBaseline)

        val metadata = userInfoToJumpMetadata(userInfo, "prog-1")
        assertEquals(0f, metadata.goalBaselineKph)
        assertEquals(0f, metadata.goalIncrementKph)
        assertEquals(0f, metadata.backupGoalBaseline)
    }

    @Test
    fun jumpProgramToUserInfo_treatsZeroAsNullForGoalFields() {
        // A null sent up as 0 must come back as null, or != null checks break
        val userInfo = UserInfo(goalBaselineKph = null, goalIncrementKph = null, backupGoalBaseline = null)
        val sentToJump = userInfoToJumpProgram(userInfo, "prog-1")

        val roundTripped = jumpProgramToUserInfo(sentToJump, nowMs = 1L)!!

        assertNull(roundTripped.goalBaselineKph)
        assertNull(roundTripped.goalIncrementKph)
        assertNull(roundTripped.backupGoalBaseline)
    }

    @Test
    fun metadata_excludesProgramEventsAndUserId() {
        val userInfo = jumpProgramToUserInfo(jumpProgram, nowMs = 1L)!!
        val metadata = userInfoToJumpMetadata(userInfo, "prog-1")

        assertEquals("prog-1", metadata.id)
        assertTrue(metadata.goalTimeAcknowledged)
        // Required by the live endpoint even though the published schema omits it
        assertEquals(0, metadata.programId)
    }

    @Test
    fun roundTrip_preservesEveryUserInfoFieldButLastChangedTime() {
        val userInfo = UserInfo(
            baselineKph = 8.5f,
            assessmentBaselineKph = 8.0f,
            goalBaselineKph = 10.0f,
            goalIncrementKph = 0.25f,
            backupGoalBaseline = 9.5f,
            autoAdjustGoalBaseline = true,
            goalTimeInfoAcknowledged = true,
            programType = ProgramType.Program10k,
            lastChangedTime = 1234L,
            programEvents = ProgramEvents(listOf(
                WorkoutEvent(index = 0, startTime = 10, endTime = 20, completed = true, label = EventLabel.WORKOUT_INTERVAL, baselineKph = 8.5f),
                NonWorkoutEvent(index = 1, startTime = 20, endTime = 30, completed = false, label = EventLabel.REST),
            )),
        )

        val stored = userInfoToJumpProgram(userInfo, "prog-1")
        val back = jumpProgramToUserInfo(stored, nowMs = 999L)!!

        assertEquals(userInfo.copy(lastChangedTime = back.lastChangedTime), back)
    }

    @Test
    fun roundTrip_preservesMetadataAndProgramType() {
        val userInfo = jumpProgramToUserInfo(jumpProgram, nowMs = 1L)!!
        val recreated = userInfoToJumpProgram(userInfo, "prog-1")

        assertEquals(0, recreated.programId)
        assertEquals(14.987178f, recreated.baselineKph)
        assertTrue(recreated.goalTimeAcknowledged)
        assertEquals(2, recreated.programEvents.events.size)
    }
}
