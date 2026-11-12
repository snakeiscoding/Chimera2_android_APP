package com.jht.progression.userinfo

import android.os.Parcelable
import androidx.annotation.Keep
import com.jht.enums.ProgramType
import com.jht.progression.data.ProgramEvents
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class UserInfo (
    var baselineKph: Float = 0f,
    var assessmentBaselineKph: Float = 0f,
    var goalBaselineKph: Float? = null,
    var goalIncrementKph: Float? = null,
    var backupGoalBaseline: Float? = null,
    var autoAdjustGoalBaseline: Boolean = false,
    var programEvents: ProgramEvents = ProgramEvents(listOf()),
    var goalTimeInfoAcknowledged: Boolean = false,
    var lastChangedTime: Long = 0L,
    var programType: ProgramType = ProgramType.Program5k
) : Parcelable
