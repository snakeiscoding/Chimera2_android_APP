package com.jht.trackapp.common.models

import com.jht.trackapp.R

enum class LapData(var title: String, val subTitleResource: Int, val res: Int) {
    LAP_TIME("0:00", R.string.lap_time_tc, R.drawable.ic_stopwatch),
    LAPS("0", R.string.laps_tc, R.drawable.ic_lap),
    BEST_LAP("0:00", R.string.best_lap_tc, R.drawable.ic_trophy);
}