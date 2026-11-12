package com.jht.interfaces.models


import com.jht.data.AppInfo
import com.jht.interfaces.BaseModel
import com.jht.interfaces.DataValueListener

/**
 * Provides a model for exposing and controlling app information and lists
 */
abstract class AppModel : BaseModel {
    /** Provides the list of apps available within a workout */
    abstract val appsAvailableInWorkout : DataValueListener<List<AppInfo>>
    abstract fun refreshAppList()
}
