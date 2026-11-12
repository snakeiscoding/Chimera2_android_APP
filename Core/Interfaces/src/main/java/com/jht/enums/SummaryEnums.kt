package com.jht.enums

enum class SummaryStat {
    TIME_ELAPSED,
    CALORIES,
    DISTANCE,
    STEPS,
    FLOORS,
    ELEVATION,
    AVERAGE_WATTS,
    AVERAGE_METS,
    AVERAGE_HEART_RATE,
    AVERAGE_RESISTANCE,
    AVERAGE_SPM,
    AVERAGE_RPM,
    AVERAGE_SPEED,
    AVERAGE_PACE, // this is just another form of AVERAGE_SPEED, basically seconds per distance
    PEAK_SPEED_KPH,
    PEAK_WATTS,
    PEAK_HEART_RATE,
    PEAK_INCLINE,
    PEAK_RESISTANCE,
    PEAK_METS,
    PEAK_RPM,
    PEAK_SPM,
    ACTIVE_CALORIES,
    AVERAGE_INCLINE,
    ; // END
    companion object {
        fun fromName(name: String): SummaryStat? = SummaryStat.entries.firstOrNull { it.name == name }
    }
}

enum class SummaryStage {
    WARMUP,
    WORKOUT,
    COOLDOWN,
    TOTAL,
    ; // END
    companion object {
        fun fromName(name: String): SummaryStage? = SummaryStage.entries.firstOrNull { it.name == name }
    }
}
