package com.jht.progression.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserInfo5KParams (
    var baselineKph: Float = 0f,
    var programEvents: ProgramEvents = ProgramEvents(listOf())
) : Parcelable
