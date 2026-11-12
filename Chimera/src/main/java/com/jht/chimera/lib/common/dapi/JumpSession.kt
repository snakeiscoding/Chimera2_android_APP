package com.jht.chimera.lib.common.dapi

import android.os.Parcelable
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.datamatrixlib.DataMatrix
import kotlinx.parcelize.Parcelize

/**
 * The signed-in JUMP user and their tokens, stored as one DataMatrix value so readers never see a
 * new access token paired with a stale id token.
 *
 * Every field is an empty string when nobody is logged in.
 *
 * @property idToken the OpenID id token (JWT), not the user id
 */
@Parcelize
data class JumpSession(
    val userId: String = "",
    val accessToken: String = "",
    val idToken: String = "",
) : Parcelable {
    companion object {
        /** Read the stored session, or an empty one when nothing has been written yet */
        @JvmStatic
        fun current(): JumpSession =
            DataMatrix.getParcelable(DAPIKeys.DATA_JUMP_SESSION_PARCELABLE, JumpSession::class.java) ?: JumpSession()
    }
}
