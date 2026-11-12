package com.jht.chimera.lib.ptp

import android.util.Log
import com.jht.chimera.lib.common.dapi.DapiUser
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.PTPKeys
import com.jht.datamatrixlib.DataMatrix

private const val TAG = "PtpWorkoutOwnership"

/** Older builds reset the loaded exercise id to all zeros, which means nothing is loaded. */
private const val NIL_UUID = "00000000-0000-0000-0000-000000000000"

/**
 * Decides whether a loaded PTP/PACE assignment may be submitted with the workout that just ended.
 *
 * PtpNative arms the assignment on preload, so browsing a list is enough, and the console it was
 * armed on may since have changed hands.
 */
object PtpWorkoutOwnership {

    /**
     * Reads the loaded assignment and decides whether it may be submitted, so callers do not have to
     * know which keys hold what.
     *
     * @return the owned assignment uuid, or null when there is none to submit
     */
    fun resolveLoadedAssignment(): String? {
        val loaded = DataMatrix.getParcelable(
            PTPKeys.DATA_PTP_LOADED_ASSIGNMENT_PARCELABLE,
            LoadedPtpAssignment::class.java,
        )
        val currentUser = DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING)
        val resolved = resolveProgramUuid(loaded, currentUser)
        if (loaded != null && loaded.isRealAssignment() && resolved != loaded.exerciseId) {
            Log.w(TAG, "Dropping PTP program ${loaded.exerciseId}: ${dropReason(loaded, currentUser)}")
        }
        return resolved
    }

    /** A missing owner and a mismatched owner are different failures, so name which one it was. */
    private fun dropReason(loaded: LoadedPtpAssignment, currentUser: String?): String = when {
        loaded.ownerUuid.isEmpty() -> "no owner was recorded when it loaded"
        loaded.ownerUuid == DapiUser.GUEST_USER_UUID -> "owner is the shared guest uuid"
        else -> "owner ${loaded.ownerUuid} is not current user $currentUser"
    }

    private fun LoadedPtpAssignment.isRealAssignment() =
        exerciseId.isNotEmpty() && exerciseId != NIL_UUID

    /**
     * @return the assignment uuid when its owner still holds the console, or null when there is no
     *   owned assignment to submit
     */
    @JvmStatic
    fun resolveProgramUuid(loaded: LoadedPtpAssignment?, currentUser: String?): String? =
        when {
            loaded == null || !loaded.isRealAssignment() -> null
            loaded.ownerUuid.isEmpty() -> null
            // Every guest shares one uuid, so it can never establish ownership.
            loaded.ownerUuid == DapiUser.GUEST_USER_UUID -> null
            loaded.ownerUuid != currentUser -> null
            else -> loaded.exerciseId
        }
}
