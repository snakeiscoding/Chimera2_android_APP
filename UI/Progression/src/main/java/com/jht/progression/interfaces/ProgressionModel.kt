package com.jht.progression.interfaces

import com.jht.data.EventIndex
import com.jht.data.MmIntervalList
import com.jht.interfaces.BaseModel
import com.jht.interfaces.DataValueListener
import java.io.File

abstract class ProgressionModel : BaseModel {
    /** Whether we should save to a local file */
    abstract val saveLocally: Boolean

    /** The string that represents the current program */
    abstract var progressionJsonString: String

    /** The location for saving files on the console when not using DAPI */
    abstract val offlineFileLocation: File?

    /** A list of intervals in the current progression workout */
    abstract val workoutIntervalList: DataValueListener<MmIntervalList>

    /** The currently running event for progression workouts */
    abstract val eventIndex: DataValueListener<EventIndex>

    /**
     * Updates workout data for a running workout. This will be used so that the app can get
     * information about the running workout even if the activity was killed and re-entered.
     */
    abstract fun updateStartedWorkout(eventIndex: EventIndex?, intervals: MmIntervalList)
}