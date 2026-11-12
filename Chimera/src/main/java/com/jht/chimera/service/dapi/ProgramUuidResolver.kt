package com.jht.chimera.service.dapi

import android.util.Log
import com.jht.chimera.lib.ptp.PtpWorkoutOwnership
import java.util.UUID

private const val TAG = "ProgramUuidResolver"

/**
 * Picks the programUuid sent in the 701 cardio complete message, so every workout can be matched to
 * web data.
 */
object ProgramUuidResolver {

    /** Marks a program uuid the console generated; the backend and surveys expect this prefix. */
    const val MANUAL_UUID_PREFIX = "manual:"

    /**
     * Return the owned PTP assignment id, or a fresh [MANUAL_UUID_PREFIX] uuid on every call when there is none.
     *
     * @param newUuid source of the generated uuid; injectable for tests
     */
    @JvmStatic
    @JvmOverloads
    fun resolve(newUuid: () -> UUID = UUID::randomUUID): String =
        PtpWorkoutOwnership.resolveLoadedAssignment()
            ?: (MANUAL_UUID_PREFIX + newUuid()).also { Log.d(TAG, "Generated program uuid $it") }
}
