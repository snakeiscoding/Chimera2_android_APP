package com.jht.mock.interfaces.models

import com.jht.data.AppInfo
import com.jht.interfaces.DataValueListener
import com.jht.interfaces.models.AppModel
import com.jht.mock.interfaces.MockDataValue

class MockAppModel : AppModel() {
    val appList = mutableListOf<AppInfo>()

    override val appsAvailableInWorkout: DataValueListener<List<AppInfo>> = MockDataValue(appList)
    override fun refreshAppList() {
        println("Not yet implemented")
    }

    override fun close() { }
}
