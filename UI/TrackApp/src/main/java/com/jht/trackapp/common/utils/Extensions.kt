package com.jht.trackapp.common.utils

import java.util.Locale

object Extensions {

    fun Int.formatTime(): String {
        val seconds = Math.round(this / 1000f)
        return String.format(Locale.ENGLISH, "%d:%02d", seconds / 60, seconds % 60)
    }

    fun List<Int>.getLapData(windowSize: Int = 2): List<Int> {
        if (this.size / windowSize == 0) return emptyList()
        val lastIndex = (this.size / windowSize) * windowSize
        val lapData = mutableListOf<Int>()
        // Take subList sum up to windowSize in each iteration and increments the iterator with windowSize
        for (i in 0 until lastIndex step (windowSize)) {
            lapData.add(this.subList(i, i + windowSize).sum())
        }
        return lapData
    }

}