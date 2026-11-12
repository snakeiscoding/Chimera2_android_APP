package com.jht.chimera.lib.common.dapi

import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.datamatrixlib.DataMatrix
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class JumpSessionTest {

    @Before
    fun setUp() {
        mockkStatic(DataMatrix::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `current returns the stored session`() {
        val stored = JumpSession(userId = "user", accessToken = "access", idToken = "id")
        every { DataMatrix.getParcelable(DAPIKeys.DATA_JUMP_SESSION_PARCELABLE, JumpSession::class.java) } returns stored

        assertEquals(stored, JumpSession.current())
    }

    @Test
    fun `current returns an empty session when nothing is stored`() {
        every { DataMatrix.getParcelable(DAPIKeys.DATA_JUMP_SESSION_PARCELABLE, JumpSession::class.java) } returns null

        assertEquals(JumpSession(), JumpSession.current())
    }
}
