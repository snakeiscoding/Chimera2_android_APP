package com.jht.chimera.lib.common.frictiontest

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * The data sent to the LCB in the 0x7e command. Used for the friction test.
 */
@Parcelize
data class SendTreadmillOperateData(
    val targetSpeed: Float,
    val targetIncline: Float,
) : Parcelable