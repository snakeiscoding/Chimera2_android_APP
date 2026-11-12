package com.jht.interfaces.models

import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.data.ProgressionUpdate
import com.jht.datamatrixlib.DataMatrix
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Reads of progression data come from DATA_PROGRESSION_PROGRAM_STRING, so a write has to land
 * there too. On the JUMP path nothing else writes that key, so without it the Journey screen
 * keeps showing pre-workout data until the next login refetches the program.
 */
class MatrixProgressionModelTest {

    private lateinit var strings: MutableMap<String, String>

    @Before
    fun setUp() {
        strings = mutableMapOf()
        mockkStatic(DataMatrix::class)

        every { DataMatrix.getInt(DAPIKeys.DATA_USER_SESSION_GENERATION_INT) } returns 7

        val getKey = slot<DAPIKeys>()
        val getFallback = slot<String>()
        every { DataMatrix.getString(capture(getKey), capture(getFallback)) } answers {
            strings[getKey.captured.name] ?: getFallback.captured
        }

        val putKey = slot<DAPIKeys>()
        val putValue = slot<String>()
        every { DataMatrix.putString(capture(putKey), capture(putValue)) } answers {
            strings[putKey.captured.name] = putValue.captured
        }
        every { DataMatrix.updateString(capture(putKey), capture(putValue)) } answers {
            strings[putKey.captured.name] = putValue.captured
            true
        }

        every { DataMatrix.putParcelable(any<DAPIKeys>(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `progressionJsonString returns the value that was just written`() {
        val model = MatrixProgressionModel()

        model.progressionJsonString = PROGRAM_JSON

        assertEquals(PROGRAM_JSON, model.progressionJsonString)
    }

    @Test
    fun `writing progression data still sends the update for persistence`() {
        val update = slot<ProgressionUpdate>()
        every {
            DataMatrix.putParcelable(DAPIKeys.ACTION_UPDATE_PROGRESSION_DATA_PARCELABLE, capture(update))
        } returns Unit

        MatrixProgressionModel().progressionJsonString = PROGRAM_JSON

        assertEquals(PROGRAM_JSON, update.captured.progressionJson)
        assertEquals(7, update.captured.generation)
    }

    @Test
    fun `a later write replaces the earlier one`() {
        val model = MatrixProgressionModel()

        model.progressionJsonString = PROGRAM_JSON
        model.progressionJsonString = UPDATED_PROGRAM_JSON

        assertEquals(UPDATED_PROGRAM_JSON, model.progressionJsonString)
    }

    private companion object {
        const val PROGRAM_JSON = """{"baselineKph":9.5,"programEvents":{"events":[]}}"""
        const val UPDATED_PROGRAM_JSON = """{"baselineKph":10.5,"programEvents":{"events":[]}}"""
    }
}
