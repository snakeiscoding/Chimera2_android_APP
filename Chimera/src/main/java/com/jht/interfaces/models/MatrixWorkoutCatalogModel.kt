package com.jht.interfaces.models

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.util.forEach
import androidx.fragment.app.FragmentActivity
import com.jht.androidcommonalgorithms.type.JHTCommonTypes
import com.jht.androidcommonalgorithms.type.JHTCommonTypes.MachineClassificationType
import com.jht.chimera.R
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.TiersKeys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.common.datamatrixkeys.VaKeys
import com.jht.chimera.lib.landmark.landmarkWorkoutItemList
import com.jht.chimera.lib.va.VAWorkoutList
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables
import com.jht.chimera.lib.workout.platform.ChimeraWorkoutManager
import com.jht.chimera.service.tiers.PremiumFeatureSet
import com.jht.chimera.ui.machinesettings.JHTTypes
import com.jht.chimera.ui.mainapp.GoScreenActivity
import com.jht.chimera.lib.workout.CustomSprint8File
import com.jht.chimera.lib.workout.Sprint8WorkoutData
import com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom.CustomWorkoutItem
import com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom.CustomWorkoutItemJSONParser
import com.jht.chimera.ui.mainapp.goscreen.WorkoutParameter
import com.jht.chimera.ui.mainapp.goscreen.WorkoutSetup.WorkoutStatInfo
import com.jht.chimera.ui.mainapp.goscreen.WorkoutSetup.getStatChangesForWorkout
import com.jht.chimera.ui.mainapp.goscreen.WorkoutSetup.getWorkoutInfo
import com.jht.customhiit.plugin.CustomHIITFeaturePlugin
import com.jht.data.BaseFeaturedWorkoutPlugin
import com.jht.data.CustomWorkout
import com.jht.data.CustomWorkouts
import com.jht.data.LandmarkWorkoutItem
import com.jht.data.WorkoutInput
import com.jht.data.WorkoutInputLevelInfo
import com.jht.data.WorkoutInputUi
import com.jht.data.WorkoutItem
import com.jht.data.sprint8.ISprint8WorkoutData
import com.jht.datamatrixlib.DataMatrix
import com.jht.enums.UnitMeasurement
import com.jht.enums.WorkoutId
import com.jht.interfaces.BoolValueListener
import com.jht.interfaces.CallbackManager
import com.jht.interfaces.DataVariableListener
import com.jht.interfaces.EventListener
import com.jht.spinbikeapps.targettraining.plugin.TargetTrainingPlugin
import com.jht.sprint8.plugin.Sprint8FeaturePlugin
import com.jht.switchStride.plugin.SwitchStrideFeaturePlugin
import com.jht.workoutcontrollerlib.model.WorkoutModel
import com.jht.workoutcontrollerlib.workoutcardio.WorkoutScriptVariables
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONException
import kotlin.coroutines.resume

private const val TAG = "MatrixWorkoutCatalogModel"

class MatrixWorkoutCatalogModel : WorkoutCatalogModel(){
    private val manager: CallbackManager = CallbackManager()
    private val workoutLaunch = MatrixWorkoutLauncherModel() // borrow for safety PIN checks until Sprint8/landmarks are better integrated
    override fun close() {
        manager.cancelAllListeners()
        workoutLaunch.close()
        clearFeaturePlugins() // have close clear plugins so host does not need to worry about it
    }

    private val chimeraWorkoutManager: ChimeraWorkoutManager = ChimeraWorkoutManager(ContextUtil.getApplicationContext())
    private val frameConfig = FrameConfigManager.getInstance().frameConfig
    private var workoutModel: WorkoutModel? = null

    override fun getSprint8WorkoutData(callback: (List<ISprint8WorkoutData>) -> Unit) {
        return chimeraWorkoutManager.retrieveModelInfo { it: WorkoutModel? ->
            it?.let { callback.invoke(chimeraWorkoutManager.convertSprint8ModelData(it)) }
        }
    }

    override var getFeaturedWorkouts: MutableList<WorkoutItem> = arrayListOf()
    override val getFeatureWorkoutPlugins: MutableList<BaseFeaturedWorkoutPlugin> = arrayListOf()
    override var getCarouselWorkouts: MutableList<WorkoutItem> = arrayListOf()
    override var getTrainingCarouselWorkouts: MutableList<WorkoutItem> = updateLockStates(getTrainingCarouselWorkouts())


    override val getWorkoutList: SparseArray<WorkoutItem> get() = updateLockStates(removedBlockedWorkouts(chimeraWorkoutManager.mWorkoutMap.getLoadedWorkouts())) // should we lift the lock state up into workout map? does that create redundant operations in old ui?

    override val getVAWorkouts: List<WorkoutItem> get() = updateLockStates(chimeraWorkoutManager.getVAWorkouts())
    override val getLandmarkWorkouts: List<LandmarkWorkoutItem> get() = updateLockStates(landmarkWorkoutItemList)

    override val workoutListReady: BoolValueListener = object : BoolValueListener {
        private var pendingTiers: Boolean = false
        private var pendingUser: Boolean = false
        init {
            manager.addBoolListener(DAPIKeys.DATA_GETTING_USER_TIERS_BOOL, {
                pendingTiers = it
                listeners.forEach { it(value) }
           }, true)
            manager.addBoolListener(DAPIKeys.DATA_GETTING_USER_INFO_BOOL, {
                pendingUser = it
                listeners.forEach { it(value) }
            }, true)
            // any other things needed before we can use catalog lists?
        }
        override val value: Boolean get() {
            return !pendingTiers && !pendingUser
        }
        private val listeners = mutableListOf<(Boolean) -> Unit>()
        override fun listenAndTrigger(onChange: (Boolean) -> Unit) {
            listeners.add(onChange)
            onChange(value)
        }
        override fun listen(onChange: (Boolean) -> Unit) {
            listeners.add(onChange)
        }
    }

    // Triggers an event when the VA list is updated
    private class VaItemsEventListener(private val manager: CallbackManager) : EventListener {
        override fun listen(onEvent: () -> Unit) {
            manager.addParcelableListener(VaKeys.DATA_VA_ITEMS_PARCELABLE,
                VAWorkoutList::class.java,
                { onEvent() },
                false)
        }
    }
    override val onVAWorkoutsChange: EventListener = VaItemsEventListener(manager)

    override fun setFeaturedWorkouts(){
        getCarouselWorkouts.clear()
        getCarouselWorkouts.addAll(chimeraWorkoutManager.carouselWorkouts)
        getFeaturedWorkouts.clear()
        getFeaturedWorkouts.addAll(chimeraWorkoutManager.featuredWorkouts)
        updateLockStates(getCarouselWorkouts)
        updateLockStates(getFeaturedWorkouts)
        clearFeaturePlugins()
        ContextUtil.getApplicationContext()?.let { appContext->
            // if a plugin was already added, do not add additional instances (since WorkoutItems come from a map by ID, it does not matter which list it came from)
            (getCarouselWorkouts.asSequence() + getFeaturedWorkouts.asSequence())
                .distinctBy { it.workoutId }
                .forEach { populateFeaturePlugin(appContext, it) }
        }
    }
    private fun populateFeaturePlugin(appContext: Context, workoutItem: WorkoutItem) {
        when (workoutItem.workoutId) {
            WorkoutId.HIIT.id -> getFeatureWorkoutPlugins.add(CustomHIITFeaturePlugin(appContext, workoutItem))
            WorkoutId.SPRINT8.id -> getFeatureWorkoutPlugins.add(Sprint8FeaturePlugin(appContext, workoutItem))
            WorkoutId.HYBRID_TD_SWITCH_STRIDE.id-> getFeatureWorkoutPlugins.add(SwitchStrideFeaturePlugin(appContext, workoutItem))
            WorkoutId.TARGET_TRAINING.id -> getFeatureWorkoutPlugins.add(TargetTrainingPlugin(appContext, workoutItem))
        }
    }
    private fun clearFeaturePlugins() {
        getFeatureWorkoutPlugins.forEach { plugin -> plugin.onUnload() }
        getFeatureWorkoutPlugins.clear()
    }

    private fun getTrainingCarouselWorkouts(): MutableList<WorkoutItem> {
        //Static training carousel list for all modalities
        val trainingCarouselItems: MutableList<WorkoutItem> = mutableListOf()
        chimeraWorkoutManager.mWorkoutMap.getWorkout(WorkoutId.FAT_BURN.id)?.let { trainingCarouselItems.add(it) }
        chimeraWorkoutManager.mWorkoutMap.getWorkout(WorkoutId.ROLLING_HILLS.id)?.let { trainingCarouselItems.add(it) }
        chimeraWorkoutManager.mWorkoutMap.getWorkout(WorkoutId.CALORIES_GOAL.id)?.let { trainingCarouselItems.add(it) }
        return trainingCarouselItems
    }

    private fun getWorkoutLockList(): Map<String, Boolean> {
        val map = mutableMapOf<String, Boolean>()
        val dmKey = if (DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL)) TiersKeys.DATA_GUEST_FEATURE_SET_PARCELABLE else TiersKeys.DATA_FEATURE_SET_PARCELABLE
        val set = DataMatrix.getParcelable(dmKey, PremiumFeatureSet::class.java)
        if (set != null) {
            for (feature in set.features) {
                //logcatFeatureSet(feature)
                if (feature.featureType == JHTTypes.FEATURE_TYPE.FEATURE_TYPE_WORKOUT) {
                    map[feature.id()] = feature.featureState == JHTTypes.FEATURE_STATE.FEATURE_DISABLED
                }
            }
        }
        return map
    }
    private fun <T : Iterable<WorkoutItem>> updateLockStates(items: T): T {
        if (TiersKeys.usingTiers()) {
            val map = getWorkoutLockList()
            items.forEach { it.locked = map[it.workoutId.toString()] ?: false }
        }
        return items
    }
    private fun updateLockStates(items: SparseArray<WorkoutItem>): SparseArray<WorkoutItem> {
        if (TiersKeys.usingTiers()) {
            val map = getWorkoutLockList()
            items.forEach { _, item-> item.locked = map[item.workoutId.toString()] ?: false }
        }
        return items
    }

    private fun removedBlockedWorkouts(items: SparseArray<WorkoutItem>): SparseArray<WorkoutItem> {
        // do we just filter out excluded items or should createWorkoutMap have an optional parameter for excluded IDs?
        if (!DataMatrix.getBoolean(SettingKeys.Workout.DATA_SHOW_MARATHON_MODE_BOOL)) {
            items.remove(WorkoutId.MARATHON.id)
        }
        frameConfig.blockedWorkoutIds?.forEach(action = items::remove)
        return items
    }

    override val stepsPerFloor: Int =
        frameConfig.STEPS_PER_FLOOR

    override val stepsPerMeter: Float =
        frameConfig.STEPS_PER_METER



    // good example of converting a callback to suspend coroutine
    override suspend fun getWorkoutInputs(workoutId: Int, optionalId: String): List<WorkoutInput> = suspendCancellableCoroutine {
        workoutModel = null // cached model belongs to the previous workout, it does not get to answer for this one
        // TODO - optionalId should probably just be passed as part of the workout launch then we can drop this DM key which seems only used to pick up in workout launch?
        if (workoutId == WorkoutId.VIRTUAL_ACTIVE.id) DataMatrix.putString(VaKeys.DATA_SELECTED_WORKOUT_UNIQUEID_STRING, optionalId)
        chimeraWorkoutManager.mWorkoutManager.loadWorkoutWithId(workoutId) { workoutInputs ->
            val newWorkoutInputList = arrayListOf<WorkoutInput>()
            if (workoutInputs != null) {
                for (input in workoutInputs) {

                    // Omit start resistance pillbox in case of Manual Resistance Spin Bike as user can't change on-screen
                    if (input.identifier() == ConsoleWorkoutVariables.START_RESISTANCE && frameConfig.machineType == JHTCommonTypes.MachineType.ManualResistanceSpinBike) {
                        continue
                    }

                    newWorkoutInputList.add(WorkoutInput(
                        // time goal is in minutes, why it isn't in seconds as a base and then do calculations from there I have no idea
                        min = input.min().toFloat(),
                        max = input.max().toFloat(),
                        defaultValue = input.defaultValue(),
                        stepSize = input.resolution().toFloat(),
                        precision = input.precision().toInt(),
                        label = input.label(),
                        unitString = input.units(),
                        unitMeasurement = input.measurement(),
                        identifier = input.identifier(),
                    ))
                }
            }
            if (it.isActive) {
                it.resume(newWorkoutInputList)
            }
        }
    }

    /** True only if the workout actually defines per-level programs (not just an "input LEVEL" declaration). [getStatChangesForWorkout] only collects workout stage */
    private fun WorkoutModel.hasDefinedLevels(): Boolean = stages.any { it.name != "warmup" && it.name != "cooldown" && it.levels.isNotEmpty() }

    // do we ever have 2 stats at the same time?
    // brazenly stolen from WorkoutSelectionFragment.java
    override fun getWorkoutInputLevelInfo(level: Int): List<WorkoutInputLevelInfo> {
        if (level !in 0..19) {
            return listOf()
        }
        // No per-level programs defined (e.g. VA passes LEVEL through but has no `level N` blocks) → no level info to show.
        val model = workoutModel?.takeIf { it.hasDefinedLevels() } ?: return listOf()
        var statList: List<String> = listOf()
        when (frameConfig.machineClassType) {
            MachineClassificationType.Treadmill -> { statList = listOf(WorkoutScriptVariables.SPEED, WorkoutScriptVariables.INCLINE)}
            MachineClassificationType.Elliptical,
            MachineClassificationType.Bike,
            MachineClassificationType.Climbmill,
            MachineClassificationType.Stepper,
            MachineClassificationType.SpinBike -> { statList = listOf(WorkoutScriptVariables.RESISTANCE)}
            MachineClassificationType.Ascent -> { statList = listOf(WorkoutScriptVariables.RESISTANCE, WorkoutScriptVariables.INCLINE)}
            else -> {}
        }
        val workoutInfoList = arrayListOf<WorkoutStatInfo>()
        for (stat in statList) {
            val stageChanges: List<Float> = getStatChangesForWorkout(level, stat, model)
            val statInfo = getWorkoutInfo(stat, stageChanges)
            if (statInfo != null) workoutInfoList.add(statInfo)
        }
        val workoutInputLevelInfo = arrayListOf<WorkoutInputLevelInfo>()
        for (item in workoutInfoList) {
            workoutInputLevelInfo.add(
                WorkoutInputLevelInfo(
                    name = item.name,
                    average = item.average,
                    max = item.max
                )
            )
        }
        return workoutInputLevelInfo
    }

    override suspend fun setWorkoutControllerModel(): Unit = suspendCancellableCoroutine {
        chimeraWorkoutManager.retrieveModelInfo { workoutModel ->
            this.workoutModel = workoutModel
            if (it.isActive) {
                it.resume(Unit) // nothing to return from this suspend coroutine function, but if we did it would go here
            }
        }
    }

    // go screen listens for DAPIKeys.DATA_USER_CUSTOM_WORKOUT_JSON_STRING, but also DAPIKeys.DATA_EXTENDED_INFO_RECEIVED_BOOL as well?
    override val customWorkouts = object : DataVariableListener<CustomWorkouts> {
        //var lastJson: String = "" // buffer last value so we only trigger on changes?
        override var value: CustomWorkouts
            get() = loadCustomWorkouts()
            set(value) {
                saveCustomWorkouts(value)
            }
        override fun listen(onChange: (CustomWorkouts) -> Unit) {
            manager.addStringListener(DAPIKeys.DATA_USER_CUSTOM_WORKOUT_JSON_STRING, {
                onChange(loadCustomWorkouts())
            }, false)
        }
        override fun listenAndTrigger(onChange: (CustomWorkouts) -> Unit) {
            manager.addStringListener(DAPIKeys.DATA_USER_CUSTOM_WORKOUT_JSON_STRING, {
                onChange(loadCustomWorkouts())
            }, true)
        }
    }
    private fun loadCustomWorkouts(): CustomWorkouts {
        chimeraWorkoutManager.clearInputCache() // clear cache in case display metric setting changes
        val workoutParameters = chimeraWorkoutManager.customWorkoutParameters.map {
            // time has special conditions in CustomWorkoutFragment.java (due to the switch up from whole minutes to H:M:S)
            if (it.identifier() == ConsoleWorkoutVariables.TIME_GOAL_START) {
                // some translation of "HH:MM:SS" is too big, use "minutes" like the other time entry or leave empty? should probably "fix" translation to be more compact?
                //val timeLabel = ContextUtil.getString(R.string.two_digit_hours_uc) + ":" + ContextUtil.getString(R.string.two_digit_minutes_uc) + ":" + ContextUtil.getString(R.string.two_digit_seconds_uc), //NON-NLS,
                val timeLabel = ContextUtil.getString(R.string.minutes_lc)
                WorkoutInputUi(
                    WorkoutInput(
                    if (frameConfig.WATTS_INACCURATE) 1f else 10.0f / 60, // PM wants 10 sec minimum, but for ECB resistance, minimum is 1 minute to avoid a lot of ECB value changing
                    it.max().toFloat(),
                    it.defaultValue(),
                    (5.0f / 60), // change step to 5 sec per PM
                    it.precision(),
                    it.originalLabel,
                    timeLabel,
                    it.measurement(),
                    it.identifier(),
                   ),
                    currentValue = mutableFloatStateOf(it.defaultValue()),
                )
            } else {
                // why are some of these doubles and not others?
                WorkoutInputUi(
                    WorkoutInput(
                    it.min().toFloat(),
                    it.max().toFloat(),
                    it.defaultValue(),
                    it.resolution().toFloat(),
                    it.precision(),
                    it.originalLabel,
                    it.units(),
                    it.measurement(),
                    it.identifier(), // this is actually the workout variable?
                    ),
                    currentValue = mutableFloatStateOf(it.defaultValue()),
                )
            }
        }
        val customWorkoutsJson = DataMatrix.getString(DAPIKeys.DATA_USER_CUSTOM_WORKOUT_JSON_STRING)
        val customWorkoutItems: List<CustomWorkoutItem> = try {
            CustomWorkoutItemJSONParser.fromJson(customWorkoutsJson, DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL))
        } catch (e: JSONException) {
            Log.e(TAG, "Failed to load custom workouts from JSON: ", e)
            listOf()
        }
        val customWorkouts = customWorkoutItems.map {
            CustomWorkout(
                it.uuid,
                it.title,
                it.intervalData,
                it.hasCooldown,
            )
        }.toMutableList()
        return CustomWorkouts(workoutParameters, customWorkouts)
    }
    private fun saveCustomWorkouts(customWorkouts: CustomWorkouts) {
        val customWorkoutItems = customWorkouts.workouts.map {
            it.toCustomWorkoutItem()
        }
        val json: String = try {
            CustomWorkoutItemJSONParser.toJsonObject(customWorkoutItems, DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)).toString()
        } catch (e: JSONException) {
            Log.e(GoScreenActivity.TAG, "Failed to save custom workout: ", e)
            return
        }
        DataMatrix.putString(DAPIKeys.ACTION_UPDATE_CUSTOM_WORKOUT_JSON_STRING, json)
    }

    override fun startCustomWorkout(customWorkout: CustomWorkout) {
        chimeraWorkoutManager.startCustomWorkout(customWorkout.toCustomWorkoutItem())
    }

    override fun startCustomSprint8Workout(speedOrResistanceData: List<Float>, inclineData: List<Float>) {
        val oldSprint8Data = mutableListOf<Sprint8WorkoutData>()
        val parameterIdentifier = if(frameConfig.machineType == JHTCommonTypes.MachineType.Treadmill){
            ConsoleWorkoutVariables.START_SPEED
        }
        else{
            ConsoleWorkoutVariables.START_RESISTANCE
        }
        oldSprint8Data.add(Sprint8WorkoutData(
            WorkoutParameter(0f,0f,0f,0f, 0,"", "", UnitMeasurement.KilometerPerHour,
                parameterIdentifier),
            listOf(speedOrResistanceData)
        ))
        if(inclineData.isNotEmpty()){
            oldSprint8Data.add(Sprint8WorkoutData(
                WorkoutParameter(0f,0f,0f,0f, 0,"", "", UnitMeasurement.KilometerPerHour,
                    ConsoleWorkoutVariables.START_INCLINE),
                listOf(inclineData)
            ))
        }
        saveSprint8CustomWorkoutData(oldSprint8Data)
        chimeraWorkoutManager.startSprint8Custom(oldSprint8Data)
    }

    override fun loadSprint8CustomWorkoutData(): List<ISprint8WorkoutData> {
        if (!ProductConfig.hasUserProfiles()) return emptyList()
        // Read on the way into the Sprint8 screens, the workout screen included, so a profile we
        // cannot make sense of has to degrade to "nothing saved" rather than take the app down.
        try {
            val file = CustomSprint8File.load() ?: return emptyList()
            val saved = file.toSprint8WorkoutData()
            if (saved.isEmpty()) return emptyList()
            // Profiles we saved hold metric, hand them back in the units the UI works in. Profiles
            // from the legacy setup were never normalized, so they are used as they were stored.
            val kphToMphFactor = UnitMeasurement.Kilometer.ratio / UnitMeasurement.Mile.ratio
            return if (file.savedInMetric) saved.map { it.scaleSpeed(kphToMphFactor, roundToTenths = true) } else saved
        } catch (e: Exception) {
            Log.w(TAG, "Could not read the saved custom Sprint8 workout", e)
            return emptyList()
        }
    }
    /**
     * Keeps the user's custom Sprint8 for next time. Stored in metric so that switching the console
     * between miles and kilometres does not rescale a saved workout.
     */
    private fun saveSprint8CustomWorkoutData(customData: List<Sprint8WorkoutData>) {
        if (!ProductConfig.hasUserProfiles()) return
        try {
            val mphToKphFactor = UnitMeasurement.Mile.ratio / UnitMeasurement.Kilometer.ratio
            CustomSprint8File.save(CustomSprint8File.from(customData.map { it.scaleSpeed(mphToKphFactor) }, savedInMetric = true))
        } catch (e: Exception) {
            Log.w(TAG, "Could not save the custom Sprint8 workout", e)
        }
    }
    /** Scales the speed parameter only, incline and resistance are unitless to us here */
    private fun Sprint8WorkoutData.scaleSpeed(factor: Double, roundToTenths: Boolean = false): Sprint8WorkoutData {
        if (DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)) return this
        if (workoutParameter?.identifier() != ConsoleWorkoutVariables.START_SPEED) return this
        val scaled = intervalData.map { level ->
            level.map { value ->
                val converted = value * factor
                if (roundToTenths) (kotlin.math.round(converted * 10) / 10).toFloat() else converted.toFloat()
            }
        }
        return Sprint8WorkoutData(workoutParameter, scaled)
    }
}

tailrec fun Context.findActivity(): FragmentActivity? {
    return when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun CustomWorkout.toCustomWorkoutItem() = CustomWorkoutItem(
    this.title,
    this.uuid,
    this.intervalData,
    this.hasCoolDown,
)
