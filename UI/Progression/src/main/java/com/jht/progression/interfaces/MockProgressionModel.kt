package com.jht.progression.interfaces

import com.jht.data.EventIndex
import com.jht.data.MmIntervalList
import com.jht.enums.ProgramType
import com.jht.interfaces.DataValueListener
import com.jht.mock.interfaces.MockDataValue
import java.io.File

class MockProgressionModel() : ProgressionModel() {
    override val saveLocally: Boolean = false
    override var progressionJsonString: String = ""
    override val offlineFileLocation: File? = null
    override val workoutIntervalList: DataValueListener<MmIntervalList> = MockDataValue(MmIntervalList(ArrayList()))
    override val eventIndex: DataValueListener<EventIndex> = MockDataValue(EventIndex(ProgramType.Program5k, 0))

    override fun updateStartedWorkout(eventIndex: EventIndex?, intervals: MmIntervalList) {
        // do nothing
    }

    override fun close() {
        //Do nothing
    }
}