package com.jht.interfaces.models

import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.dapi.DapiUser
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.MmKeys
import com.jht.datamatrixlib.DataMatrix
import com.jht.enums.ProgramType
import com.jht.interfaces.CallbackManager
import com.jht.interfaces.MatrixParcelableValueListener
import com.jht.interfaces.MatrixStringValueListener
import com.jht.interfaces.ParcelableValueListener
import com.jht.progression.interfaces.ProgressionModel
import com.jht.data.EventIndex
import com.jht.data.MmIntervalList
import com.jht.data.ProgressionUpdate
import com.jht.progression.userinfo.deleteMatrixMethodUserData
import java.io.File

const val PROGRESSION_DIR = "progression"

class MatrixProgressionModel: ProgressionModel() {

    private val manager: CallbackManager = CallbackManager()

    val uuid = MatrixStringValueListener(manager, DAPIKeys.DATA_USER_DAPI_UUID_STRING)

    // Only save locally if we are not a commercial console
    override val saveLocally: Boolean
        get() {
            return !ProductConfig.isCommercial()
        }

    override var progressionJsonString: String
        get() = DataMatrix.getString(DAPIKeys.DATA_PROGRESSION_PROGRAM_STRING, "")
        set(value) {
            // Reads come from this key and only a login refetch writes it on the JUMP path,
            // so update it here or the UI keeps showing data from before this save.
            DataMatrix.putString(DAPIKeys.DATA_PROGRESSION_PROGRAM_STRING, value)
            // Stamp with the current generation so a stale update can be dropped on arrival.
            val generation = DataMatrix.getInt(DAPIKeys.DATA_USER_SESSION_GENERATION_INT)
            DataMatrix.putParcelable(DAPIKeys.ACTION_UPDATE_PROGRESSION_DATA_PARCELABLE, ProgressionUpdate(value, generation))
        }

    override val offlineFileLocation: File?
        get() {
            return getProgressionFileLocation()
        }

    override val workoutIntervalList: ParcelableValueListener<MmIntervalList> by lazy {
        MatrixParcelableValueListener(
            manager,
            MmKeys.DATA_PROGRESSION_WORKOUT_INTERVALS_PARCELABLE,
            MmIntervalList(listOf()),
            MmIntervalList::class.java
        )
    }

    override val eventIndex: ParcelableValueListener<EventIndex> by lazy {
        //TODO: Come up with a better way of handling assessment event, the default is retrieved for the assessment
        MatrixParcelableValueListener(
            manager,
            MmKeys.DATA_MM_EVENT_INDEX_PARCELABLE,
            EventIndex(ProgramType.Program5k, -1),
            EventIndex::class.java
        )
    }

    override fun updateStartedWorkout(eventIndex: EventIndex?, intervals: MmIntervalList) {
        DataMatrix.putParcelable(
            MmKeys.DATA_MM_EVENT_INDEX_PARCELABLE,
            eventIndex
        )

        DataMatrix.putParcelable(
            MmKeys.DATA_PROGRESSION_WORKOUT_INTERVALS_PARCELABLE,
            intervals
        )
    }

    override fun close() {
        manager.cancelAllListeners()
    }
}

fun getProgressionFileLocation() : File? {
    val filesDir = ContextUtil.getApplicationContext()?.filesDir
    return if (filesDir != null) {
        File(ContextUtil.getApplicationContext()?.filesDir, PROGRESSION_DIR)
    } else {
        null
    }
}

fun resetGuestUser() {
    val fileDir = getProgressionFileLocation()
    if (fileDir != null) {
        deleteMatrixMethodUserData(DapiUser.GUEST_USER_UUID, fileDir)
    }
}

fun eraseLocalUserData(userId: String) {
    val fileDir = getProgressionFileLocation()
    if (fileDir != null) {
        deleteMatrixMethodUserData(userId, fileDir)
    }
}