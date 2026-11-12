package com.jht.interfaces.models

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import com.jht.androidcommonalgorithms.type.JHTCommonTypes
import com.jht.chimera.BuildConfig
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.workout.platform.ChimeraWorkoutManager
import com.jht.chimera.lib.common.dapi.DapiLogin
import com.jht.chimera.lib.common.dapi.DapiUtil
import com.jht.chimera.lib.common.dapi.JumpSession
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.PTPKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.common.datamatrixkeys.VaKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.ptp.HeartRateZones
import com.jht.chimera.lib.ptp.LoadedPtpAssignment
import com.jht.chimera.lib.va.VAWorkoutList
import com.jht.chimera.service.workout.enums.WorkoutRunState
import com.jht.datamatrixlib.DataMatrix
import com.jht.engage360.Engage360SystemModel
import com.jht.engage360.Sprint8AllLevelsResult
import com.jht.engage360.SummaryStage
import com.jht.engage360.SummaryStatE360
import com.jht.engage360.data.ExerciserEnvironment
import com.jht.engage360.data.ExerciserEquipment
import com.jht.engage360.data.ExerciserStrengthVariant
import com.jht.engage360.data.RecommendedExercise
import com.jht.engage360.data.RecommendedExercises
import com.jht.enums.SummaryStat
import com.jht.seca.data.BenchmarkState
import com.jht.seca.data.BodyScanState
import com.jht.seca.data.Scan
import com.jht.seca.data.SecaSurveyState
import com.jht.seca.data.SecaUserData
import com.jhtp.dapiservice.DapiEnvironment
import java.util.Locale


private const val TAG = "MatrixEngage360SystemModel"

class MatrixEngage360SystemModel : Engage360SystemModel() {

    private val dataMatrix = DataMatrix()
    private val summaryModel = MatrixWorkoutSummaryModel() // borrow from the core/interfaces model implementations
    private var chimeraWorkoutManager: ChimeraWorkoutManager? = null
    private fun getChimeraWorkoutManager(): ChimeraWorkoutManager {
        return chimeraWorkoutManager ?: ChimeraWorkoutManager(ContextUtil.getApplicationContext()).also { chimeraWorkoutManager = it }
    }

    override val userWeightKg: Float
        get() = DataMatrix.getFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT)
    override val userAge: Float
        get() = DataMatrix.getInt(DAPIKeys.DATA_USER_AGE_INT).toFloat()
    override val maxWorkoutTimeMinutes: Int
        get() = DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_WORKOUT_TIME_MIN_INT)
    override val isDebugBuild: Boolean
        get() = BuildConfig.DEBUG
    override val facilityId: String
        get() = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT).toString()
    override val machineId: String
        get() = DapiUtil.getMachineId()
    override val showEngage360Launch: Boolean
        get() = checkShowEngage360()
    override val exerciserEquipment: ExerciserEquipment
        get() = when (FrameConfigManager.getInstance().frameConfig.machineClassType) {
            JHTCommonTypes.MachineClassificationType.Treadmill -> ExerciserEquipment.Treadmill
            JHTCommonTypes.MachineClassificationType.Bike, JHTCommonTypes.MachineClassificationType.SpinBike -> ExerciserEquipment.Bike
            JHTCommonTypes.MachineClassificationType.Elliptical -> ExerciserEquipment.Elliptical
            JHTCommonTypes.MachineClassificationType.Ascent -> ExerciserEquipment.Ascent
            JHTCommonTypes.MachineClassificationType.Climbmill -> ExerciserEquipment.Climbmill
            JHTCommonTypes.MachineClassificationType.Stepper -> ExerciserEquipment.Stepper
            JHTCommonTypes.MachineClassificationType.Strength -> ExerciserEquipment.Strength
            else -> ExerciserEquipment.Treadmill
        }
    override val exerciserStrengthVariant: ExerciserStrengthVariant?
        get() = null

    override fun setUserChangedCallback(userChanged: (String?) -> Unit) {
        // Technically if new user is not PTP, they might as well be no user. Granted a non-PTP user would get empty return from PTP cookie retrieval.
        // NOTE: DATA_USER_DAPI_UUID_STRING updates before DATA_IS_PTP_USER_BOOL (if it goes the other way, we could have a problem)
        // Maybe DATA_IS_PTP_USER_BOOL should be a string either containing user ID if PTP or empty string
        dataMatrix.createBoolAdapter(DAPIKeys.DATA_GETTING_USER_INFO_BOOL, {
            if(!it) {
                clearLoadedExerciseIfStale()
                userChanged(
                    if (DataMatrix.getBoolean(SettingKeys.General.DATA_IS_PTP_USER_BOOL) || DataMatrix.getBoolean(
                            DAPIKeys.DATA_HAS_SECA_FEATURE_BOOL
                        )
                    ) DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING) else null
                ) // assuming a guest cannot have PTP true, but cookie retrieval would fail anyway
            }
        }, false)

        dataMatrix.createEventAdapter(DapiLogin.Keys.ACTION_LOGOUT, {
            clearLoadedExercise()
            userChanged(null)
        }, false)
        /*dataMatrix.createStringAdapter(DAPIKeys.DATA_USER_DAPI_UUID_STRING, {
            userChanged(if (it == DapiUser.GUEST_USER_UUID) null else it) // treat guest the same as unknown user
        }, false)*/
    }

    override fun setWorkoutEndCallback(workoutEnd: () -> Unit) {
        dataMatrix.createIntAdapter(
            WorkoutKeys.DATA_RUN_STATE_INT,
            { runState: Int ->
                if (runState == WorkoutRunState.STOP.ordinal) {
                    workoutEnd()
                }
            }, false
        )
    }

    override fun onExerciseLoaded(exerciseId: String, ownerUuid: String) {
        DataMatrix.putParcelable(
            PTPKeys.DATA_PTP_LOADED_ASSIGNMENT_PARCELABLE,
            LoadedPtpAssignment(exerciseId, ownerUuid),
        )
    }

    /**
     * Used on the login edge, which fires more than once per login and can land after the incoming
     * user has legitimately preloaded - clearing unconditionally there would lose their assignment.
     */
    private fun clearLoadedExerciseIfStale() {
        val owner = DataMatrix.getParcelable(
            PTPKeys.DATA_PTP_LOADED_ASSIGNMENT_PARCELABLE,
            LoadedPtpAssignment::class.java,
        )?.ownerUuid ?: return
        if (owner == DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING)) return
        clearLoadedExercise()
    }

    private fun clearLoadedExercise() {
        DataMatrix.putParcelable(PTPKeys.DATA_PTP_LOADED_ASSIGNMENT_PARCELABLE, null)
    }

    override fun setApiEnvironmentCallback(onEnvironment: (ExerciserEnvironment) -> Unit) {
        dataMatrix.createIntAdapter(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, {
            when (it) {
                DapiEnvironment.Production.ordinal -> onEnvironment(ExerciserEnvironment.Production)
                DapiEnvironment.Staging.ordinal -> onEnvironment(ExerciserEnvironment.Staging)
                DapiEnvironment.QA.ordinal -> onEnvironment(ExerciserEnvironment.QA)
                DapiEnvironment.Dev.ordinal -> onEnvironment(ExerciserEnvironment.Dev)
            }
        }, true)
    }

    override fun getWorkoutStat(stat: SummaryStatE360, stage: SummaryStage): Float {
        // for now these enums line up, but will that always be the case?
        val coreStat = SummaryStat.fromName(stat.name) ?: return 0f
        val coreStage = com.jht.enums.SummaryStage.fromName(stage.name) ?: return 0f
        return summaryModel.getWorkoutStat(coreStat, coreStage)
    }

    override fun onUserIsActive(isActive: Boolean) {
        // nothing for now
    }

    override fun onRecommendedExercises(exercises: RecommendedExercises) {
        DataMatrix.putParcelable(PTPKeys.DATA_RECOMMENDED_EXERCISES_PARCELABLE, exercises)
    }

    override fun onBiaScan(biaScanInfo: Scan) {
        return
//        TODO("Not yet implemented")
    }

    override fun getWorkoutIntervalStat(stat: SummaryStatE360): List<Float> {
        // for now these enums line up, but will that always be the case?
        val coreStat = SummaryStat.fromName(stat.name) ?: return listOf()
        return summaryModel.getWorkoutIntervalStat(coreStat)
    }

    override fun getVirtualActiveLocations(): List<String> {
        val vaWorkoutList = DataMatrix.getParcelable(VaKeys.DATA_VA_ITEMS_PARCELABLE, VAWorkoutList::class.java)
        val vaWorkouts = vaWorkoutList?.vaWorkouts
        var ptpVAList : List<String> = listOf()
        val context = ContextUtil.getApplicationContext() ?: return listOf()
        val vaLookup = VirtualActiveStringLookup(context)
        vaWorkouts?.forEach {
            ptpVAList = ptpVAList + vaLookup.getStringByResourceName(it.nameResourceString)
        }
        return ptpVAList
    }

    override fun get8RepMaxLaunch() {
//        TODO("Not yet implemented")
    }

    override fun getBenchmarkState(): BenchmarkState {
        return BenchmarkState.NONE
    }

    override fun getVirtualActiveIdFromLocation(location: String): String {
        val vaWorkoutList = DataMatrix.getParcelable(VaKeys.DATA_VA_ITEMS_PARCELABLE, VAWorkoutList::class.java)
        val context = ContextUtil.getApplicationContext()
        if (vaWorkoutList?.vaWorkouts?.isNotEmpty() != true || context == null) return ""
        val vaLookup = VirtualActiveStringLookup(context)
        val workout = vaWorkoutList.vaWorkouts.find { vaLookup.getStringByResourceName(it.nameResourceString) == location } ?: vaWorkoutList.vaWorkouts.first()
        return workout.uniqueId
    }

    override fun getSprint8AllLevelsData(callback: (Sprint8AllLevelsResult) -> Unit) {
        val manager = getChimeraWorkoutManager()
        manager.retrieveSprint8WorkoutData { data ->
            if (data.isNullOrEmpty()) {
                callback(Sprint8AllLevelsResult(emptyList()))
                return@retrieveSprint8WorkoutData
            }
            callback(
                Sprint8AllLevelsResult(
                    primaryData = data.firstOrNull()?.intervalData ?: emptyList(),
                    inclineData = data.getOrNull(1)?.intervalData,
                )
            )
        }
    }

    override fun setHeartRateZones(
        regenerativeMin: Int,
        regenerativeMax: Int,
        basicExtensiveMax: Int,
        basicIntensiveMax: Int,
        developmentMax: Int,
        peakMax: Int
    ) {
        val heartRateZones = HeartRateZones().also {
            it.regenerativeMin = regenerativeMin
            it.regenerativeMax = regenerativeMax
            it.basicExtensiveMax = basicExtensiveMax
            it.basicIntensiveMax = basicIntensiveMax
            it.developmentMax = developmentMax
            it.peakMax = peakMax
        }
        DataMatrix.putParcelable(WorkoutKeys.DATA_CS_THR_ZONES_PARCELABLE, heartRateZones)
    }

    override fun close() {
        dataMatrix.finish()
        summaryModel.close()
        chimeraWorkoutManager = null
    }

    /** seca overrides */
    override val isSecaUser: Boolean
        get() = DataMatrix.getBoolean(DAPIKeys.DATA_HAS_SECA_FEATURE_BOOL)
    override val secaUserData: SecaUserData?
        get() = DataMatrix.getParcelable(DAPIKeys.DATA_SECA_USER_INFO_PARCELABLE, SecaUserData::class.java)
    override var recommendedExercisesList: List<RecommendedExercise> = listOf()
        get() = DataMatrix.getParcelable(PTPKeys.DATA_RECOMMENDED_EXERCISES_PARCELABLE, RecommendedExercises::class.java)?.recommendedExerciseList ?: listOf()
    override val jumpUserId: String
        get() = JumpSession.current().userId
    override val jumpToken: String
        get() = JumpSession.current().accessToken
    override val useMetric: Boolean
        get() = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)

    fun checkShowEngage360() : Boolean{
        var show = true
        //If using seca, we auto launch into it unless the survey has been declined.
        if(isSecaUser) {
            val bodyScanState = secaUserData?.scanState
            val surveyState = secaUserData?.surveyState
            show = surveyState != SecaSurveyState.DECLINED
        }
        return show
    }
}


/** Helper to look up Virtual Active title strings */
private class VirtualActiveStringLookup(val context: Context) {
    val resources = getLocalizedResources(context, Locale.ENGLISH) // PTP titles are in English, I think? really should add IDs to the end point

    @SuppressLint("DiscouragedApi")
    fun getStringByResourceName(name: String): String {
        return try {
            val id = resources.getIdentifier(name, "string", context.packageName)
            resources.getString(id)
        } catch (e:Exception) {
            Log.e(TAG, "Could not find VA resource for $name", e)
            ""
        }
    }
    private fun getLocalizedResources(context: Context, desiredLocale: Locale): Resources {
        val conf = Configuration(context.resources.configuration)
        conf.setLocale(desiredLocale)
        val localizedContext: Context = context.createConfigurationContext(conf)
        return localizedContext.resources
    }
}