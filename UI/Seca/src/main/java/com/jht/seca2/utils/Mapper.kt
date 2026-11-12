package com.jht.seca2.utils

// Mapper from interfaces.models → engage360
fun com.jht.interfaces.models.QuickStatData.toEngage360(): com.jht.engage360.QuickStatDataE360 {
    return com.jht.engage360.QuickStatDataE360(
        iconId = this.iconId,
        labelId = this.labelId,
        value = this.value,
        unitId = this.unitId
    )
}

// Mapper from engage360 → interfaces.models
fun com.jht.engage360.QuickStatDataE360.toInterfacesModel(): com.jht.interfaces.models.QuickStatData {
    return com.jht.interfaces.models.QuickStatData(
        iconId = this.iconId,
        labelId = this.labelId,
        value = this.value,
        unitId = this.unitId
    )
}

// Mapper from interfaces.models → engage360
fun com.jht.interfaces.models.FullStatData.toEngage360(): com.jht.engage360.FullStatDataE360 {
    return com.jht.engage360.FullStatDataE360(
        labelId = this.labelId,
        unitId = this.unitId,
        warmupText = this.warmupText,
        workoutText = this.workoutText,
        cooldownText = this.cooldownText,
        totalText = this.totalText
    )
}

// Mapper from engage360 → interfaces.models
fun com.jht.engage360.FullStatDataE360.toInterfacesModel(): com.jht.interfaces.models.FullStatData {
    return com.jht.interfaces.models.FullStatData(
        labelId = this.labelId,
        unitId = this.unitId,
        warmupText = this.warmupText,
        workoutText = this.workoutText,
        cooldownText = this.cooldownText,
        totalText = this.totalText
    )
}

// Convert a list of interfaces.models.QuickStatData → engage360.QuickStatData
fun List<com.jht.interfaces.models.QuickStatData>.toEngage360QuickStatList(): List<com.jht.engage360.QuickStatDataE360> {
    return this.map { it.toEngage360() }
}

// Convert a list of engage360.QuickStatData → interfaces.models.QuickStatData
fun List<com.jht.engage360.QuickStatDataE360>.toInterfacesQuickStatList(): List<com.jht.interfaces.models.QuickStatData> {
    return this.map { it.toInterfacesModel() }
}

// Convert a list of interfaces.models.FullStatData → engage360.FullStatData
fun List<com.jht.interfaces.models.FullStatData>.toEngage360FullStatList(): List<com.jht.engage360.FullStatDataE360> {
    return this.map { it.toEngage360() }
}

// Convert a list of engage360.FullStatData → interfaces.models.FullStatData
fun List<com.jht.engage360.FullStatDataE360>.toInterfacesFullStatList(): List<com.jht.interfaces.models.FullStatData> {
    return this.map { it.toInterfacesModel() }
}