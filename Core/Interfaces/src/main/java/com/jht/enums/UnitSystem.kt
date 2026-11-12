package com.jht.enums

enum class UnitSystem {
    Metric,
    Imperial,
    ;
    fun isMetric() = this == Metric

    companion object {
        val DEFAULT = Metric
        fun from(isMetric: Boolean) : UnitSystem = if (isMetric) Metric else Imperial
        //fun from(id: Int) = values().find { it.ordinal == id } ?: DEFAULT
    }
}
