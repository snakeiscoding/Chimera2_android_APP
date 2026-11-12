package com.jht.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TrackInfo(
    var trackData: List<Int> = emptyList()
) : Parcelable