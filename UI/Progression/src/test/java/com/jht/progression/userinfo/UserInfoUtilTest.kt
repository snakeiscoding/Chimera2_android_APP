package com.jht.progression.userinfo

import android.util.Log
import com.jht.progression.data.EventLabel
import com.jht.progression.data.NonWorkoutEvent
import com.jht.progression.data.ProgramEvents
import com.jht.progression.data.WorkoutEvent
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class UserInfoUtilTest {
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("mm_user_info_test").toFile()
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        tempDir.deleteRecursively()
    }

    private fun buildUserInfo(): UserInfo {
        return UserInfo(
            baselineKph = 8.5f,
            assessmentBaselineKph = 7.5f,
            goalBaselineKph = 9f,
            goalIncrementKph = 0.5f,
            programEvents = ProgramEvents(
                listOf(
                    WorkoutEvent(
                        index = 0,
                        startTime = 1000L,
                        endTime = 2000L,
                        completed = true,
                        label = EventLabel.WORKOUT_TEMPO,
                        workoutDistanceKm = 5f,
                        workoutTimeMs = 1800000,
                        totalDistanceKm = 5f,
                        totalTimeSec = 1800,
                        totalCalories = 300f,
                        totalAverageSpeedKph = 10f,
                        totalPeakSpeedKph = 15f,
                        totalAverageHR = 140,
                        totalElevationMeters = 20f,
                        baselineKph = 8.5f,
                        goalTimeExisted = true
                    ),
                    NonWorkoutEvent(index = 1, startTime = 2000L, endTime = 3000L, completed = false, label = EventLabel.REST)
                )
            ),
            lastChangedTime = 123456789L
        )
    }

    @Test
    fun `writing and reading program info round trips through json`() {
        val userInfo = buildUserInfo()
        val userId = "user-1"

        writeUserProgramInfo(getUserProgramString(userInfo), userId, tempDir)
        val readJson = readUserProgramInfoFile(userId, tempDir)

        assertEquals(userInfo, getUserInfoFromJsonString(readJson!!))
    }

    @Test
    fun `readUserProgramInfoFile returns null when no file has been written`() {
        assertNull(readUserProgramInfoFile("missing-user", tempDir))
    }

    @Test
    fun `deleteMatrixMethodUserData removes previously written program info`() {
        val userId = "user-2"
        writeUserProgramInfo(getUserProgramString(buildUserInfo()), userId, tempDir)
        assertTrue(readUserProgramInfoFile(userId, tempDir) != null)

        deleteMatrixMethodUserData(userId, tempDir)

        assertNull(readUserProgramInfoFile(userId, tempDir))
    }

    @Test
    fun `getUserInfoFromJsonString falls back to defaults on malformed json`() {
        assertEquals(UserInfo(), getUserInfoFromJsonString("{not valid json"))
    }
}
