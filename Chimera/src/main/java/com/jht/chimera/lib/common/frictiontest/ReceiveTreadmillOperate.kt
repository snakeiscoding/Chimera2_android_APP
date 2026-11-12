package com.jht.chimera.lib.common.frictiontest

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * The data received from the 0x7e LCB command. Used for friction test.
 */
@Parcelize
data class ReceiveTreadmillOperate(
    val currentError: Int,
    val motorTorque: Int,
    val actualSpeed: Float,
    val actualIncline: Float,
    val motorVolts: Int,
    val motorCurrentMilliAmps: Int,
    val status: Int,
    val treadmillInUse: Boolean,
) : Parcelable