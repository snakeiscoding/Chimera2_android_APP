package com.jht.chimera.service.hal.lib.lcb

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * The data received from the 0x7f LCB command. SPM and Accumulate Steps.
 */
//20260316 ESDV-1069 Add Vortex Chimera PS(lcb 0xC9) / PP(lcb 0xF2) TM new Cadence cmd 0x7F , SPM and Accumulate Steps value.>
@Parcelize
data class ReceiveTreadmillCadence(
    val cadenceSpm: Int,
    val accumulateSteps: Int,
) : Parcelable
//<20260316 ESDV-1069 Add Vortex Chimera PS(lcb 0xC9) / PP(lcb 0xF2) TM new Cadence cmd 0x7F , SPM and Accumulate Steps value.
