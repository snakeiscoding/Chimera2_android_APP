package com.jht.sprint8.model

import com.jht.enums.MinMaxInfo
import com.jht.enums.MinMaxParam


/**
 * Represents configuration and values for custom statistical data.
 * @property primaryStat Pair containing the primary statistic parameter and its min/max info.
 * @property secondaryStat Optional pair containing the secondary statistic parameter and its min/max info.
 */
data class CustomStatInfo(
    val primaryStat: Pair<MinMaxParam, MinMaxInfo>,
    val secondaryStat: Pair<MinMaxParam, MinMaxInfo>?,
)