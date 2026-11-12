package com.jht.enums

// Using values from DAPI GenderIndentity

enum class Gender (val value: Int) {
    Male(0),
    Female(1),
    NonBinary(2),
    NotSpecified(3);

    companion object {
        val DEFAULT = Male
        fun from(id: Int) = values().find { it.value == id } ?: DEFAULT
    }
}
