package com.jht.widgets.helpers

import androidx.compose.runtime.mutableStateOf
import com.jht.enums.CaloriesSource
import com.jht.enums.FrameType
import com.jht.enums.HeartRateSource
import com.jht.enums.WorkoutGoalType
import com.jht.interfaces.models.DeviceModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.unit.UnitFormatter
import com.jht.unit.UnitFormatter.formatDecimal
import com.jht.unit.roundDown
import com.jht.widgets.R

import com.jht.widgets.helpers.StatType.AVERAGE_PACE
import com.jht.widgets.helpers.StatType.AVERAGE_SPM
import com.jht.widgets.helpers.StatType.AVERAGE_STRIDE_LENGTH
import com.jht.widgets.helpers.StatType.CALORIES
import com.jht.widgets.helpers.StatType.CALORIES_PER_HOUR
import com.jht.widgets.helpers.StatType.CLOCK
import com.jht.widgets.helpers.StatType.DISTANCE
import com.jht.widgets.helpers.StatType.DISTANCE_SECONDARY
import com.jht.widgets.helpers.StatType.ELEVATION
import com.jht.widgets.helpers.StatType.FLOORS
import com.jht.widgets.helpers.StatType.GOAL_REMAINING
import com.jht.widgets.helpers.StatType.HEART_RATE
import com.jht.widgets.helpers.StatType.INCLINE
import com.jht.widgets.helpers.StatType.METS
import com.jht.widgets.helpers.StatType.MYZONE_HR_PERCENT
import com.jht.widgets.helpers.StatType.MYZONE_MEPS
import com.jht.widgets.helpers.StatType.PACE
import com.jht.widgets.helpers.StatType.PEAK_HEART_RATE
import com.jht.widgets.helpers.StatType.PERCENT_HEART_RATE
import com.jht.widgets.helpers.StatType.RPM
import com.jht.widgets.helpers.StatType.SPEED
import com.jht.widgets.helpers.StatType.SPEED_SECONDARY
import com.jht.widgets.helpers.StatType.SPM
import com.jht.widgets.helpers.StatType.STEPS
import com.jht.widgets.helpers.StatType.STRIDE_LENGTH
import com.jht.widgets.helpers.StatType.TIME_ELAPSED
import com.jht.widgets.helpers.StatType.TOTAL_GOAL
import com.jht.widgets.helpers.StatType.WATTS
import java.math.RoundingMode

/*
    File contains helpers for the common 5 category stats listings.
    Provides the lists per modality and common formatting.
    Presently used by simple app and bottom bar stats to keep them consistent.
 */

/** All possible stat types supported at present */
enum class StatType {
    AVERAGE_PACE,
    AVERAGE_SPM,
    AVERAGE_STRIDE_LENGTH,
    CALORIES,
    CALORIES_PER_HOUR,
    CLOCK,
    DISTANCE,
    DISTANCE_SECONDARY,
    ELEVATION,
    FLOORS,
    GOAL_REMAINING,
    HEART_RATE,
    INCLINE,
    METS,
    PACE,
    PEAK_HEART_RATE,
    PERCENT_HEART_RATE,
    RPM,
    SPEED,
    SPEED_SECONDARY,
    SPM,
    STEPS,
    STRIDE_LENGTH,
    TIME_ELAPSED,
    TOTAL_GOAL,
    WATTS,
    MYZONE_HR_PERCENT,
    MYZONE_MEPS,
    ; // END VALUES
    /** Returns if current value has a secondary unit variation */
    fun hasSecondary() = (this == DISTANCE || this == SPEED)
    /** Returns if current value is a secondary unit variation */
    fun isSecondary() = (this == DISTANCE_SECONDARY || this == SPEED_SECONDARY)
    /** Returns if type is a MyZone type */
    fun isMyZone() = (this == MYZONE_HR_PERCENT || this == MYZONE_MEPS)
    /** Returns if type is a goal type */
    fun isGoal() = (this == GOAL_REMAINING || this == TOTAL_GOAL)
}


/**
 * StatList singleton containing the list of stats
 * Currently the 5 columns of bottom bar and simple app match
 */
object StatList {
    private fun treadmillList(showWatts: Boolean, isTargetHeartRate: Boolean, showCadence: Boolean) = listOf(
        listOfNotNull(HEART_RATE, MYZONE_HR_PERCENT, PEAK_HEART_RATE, PERCENT_HEART_RATE.takeIf { isTargetHeartRate }, MYZONE_MEPS,
            SPM.takeIf { showCadence }, STEPS.takeIf { showCadence }),
        listOfNotNull(CALORIES, CALORIES_PER_HOUR, WATTS.takeIf { showWatts }, METS),
        listOfNotNull(TIME_ELAPSED, GOAL_REMAINING, TOTAL_GOAL, CLOCK),
        listOfNotNull(DISTANCE, DISTANCE_SECONDARY, STRIDE_LENGTH.takeIf { showCadence }, AVERAGE_STRIDE_LENGTH.takeIf { showCadence }),
        listOfNotNull(PACE, AVERAGE_PACE, ELEVATION, AVERAGE_SPM.takeIf { showCadence })
    )
    private fun rpmBasedList(showWatts: Boolean, isTargetHeartRate: Boolean) = listOf(
        listOfNotNull(HEART_RATE, MYZONE_HR_PERCENT, PEAK_HEART_RATE, PERCENT_HEART_RATE.takeIf { isTargetHeartRate }, MYZONE_MEPS),
        listOfNotNull(CALORIES, CALORIES_PER_HOUR, WATTS.takeIf { showWatts }, METS),
        listOfNotNull(TIME_ELAPSED, GOAL_REMAINING, TOTAL_GOAL, CLOCK),
        listOfNotNull(DISTANCE, DISTANCE_SECONDARY),
        listOfNotNull(RPM, SPEED, PACE, AVERAGE_PACE)
    )
    private fun stepsBasedList(showWatts: Boolean, isTargetHeartRate: Boolean) = listOf(
        listOfNotNull(HEART_RATE, MYZONE_HR_PERCENT, PEAK_HEART_RATE, PERCENT_HEART_RATE.takeIf { isTargetHeartRate }, MYZONE_MEPS),
        listOfNotNull(CALORIES, CALORIES_PER_HOUR, WATTS.takeIf { showWatts }, METS),
        listOfNotNull(TIME_ELAPSED, GOAL_REMAINING, TOTAL_GOAL, CLOCK),
        listOfNotNull(STEPS, FLOORS),
        listOfNotNull(SPM, AVERAGE_SPM)
    )

    /**
     * Gets the frame stat list.
     * @return The frame stat list
     */
    fun getList(frameType: FrameType, showWatts: Boolean, isTargetHeartRate: Boolean, showCadence: Boolean): List<List<StatType>> {
        return when (frameType) {
            FrameType.Treadmill ->
                treadmillList(showWatts, isTargetHeartRate, showCadence)
            FrameType.Elliptical, FrameType.Bike, FrameType.Ascent, FrameType.SpinBike ->
                rpmBasedList(showWatts, isTargetHeartRate)
            FrameType.Stepper, FrameType.Climbmill ->
                stepsBasedList(showWatts, isTargetHeartRate)
        }
    }
}


/**
 * Common formatting for stats
 */
class StatFormatter(
    private val frameType: FrameType,
    private val isMetric: Boolean,
    private val is24HourFormat: Boolean,
    private val precision: Int = 2,
) {

    fun formatPaceFromSpeed(kph: Float): String {
        return UnitFormatter.formatPace(kph, isMetric)
    }

    fun formatInt(value: Int): String {
        return UnitFormatter.formatInt(value)
    }

    fun formatCalories(calories: Float): String {
        return UnitFormatter.formatCalories(calories)
    }

    fun formatDistance(kilometers: Float): String {
        return UnitFormatter.formatDistance(kilometers, isMetric, precision)
    }

    fun formatDistanceSecondary(kilometers: Float, precision: Int = 2): String {
        return UnitFormatter.formatDistance(kilometers, !isMetric, precision)
    }

    fun formatElevation(elevationMeters: Float): String {
        return UnitFormatter.formatElevation(elevationMeters, isMetric)
    }

    //TODO: The code usually done in UnitFormatter is done here for now because UnitFormatter is in an artifact.
    fun formatStrideLength(strideLengthMeters: Float, precision: Int = 2): String {
        val strideLength = strideLengthMeters.roundDown(precision)
        //For now, stride length is always given in meters
//      if (!isMetric) {
//          strideLength = strideLength.meterUnit.toFeet
//      }
        return formatDecimal(precision, strideLength, RoundingMode.HALF_UP)
    }

    fun formatFloors(floors: Float): String {
        return UnitFormatter.formatFloors(floors)
    }

    fun formatFloorGoal(floors: Float): String {
        return UnitFormatter.formatFloorGoal(floors)
    }

    fun formatTimeMilliseconds(milliseconds: Int): String {
        return UnitFormatter.formatTimeMilliseconds(milliseconds)
    }

    fun formatTimeMilliseconds(milliseconds: Float): String {
        return UnitFormatter.formatTimeMilliseconds(milliseconds)
    }

    fun formatDistanceGoal(kilometers: Float): String {
        return UnitFormatter.formatDistanceGoal(kilometers, isMetric, precision)
    }

    fun formatHeartRate(heartRate: Int): String {
        return UnitFormatter.formatHeartRate(heartRate)
    }

    fun formatHeartRate(heartRate: Float): String {
        return UnitFormatter.formatHeartRate(heartRate)
    }

    fun formatIncline(incline: Float): String {
        return UnitFormatter.formatIncline(incline, frameType.isTreadmill)
    }

    fun formatMets(mets: Float): String {
        return UnitFormatter.formatMets(mets)
    }

    fun formatSpeed(kph: Float): String {
        return UnitFormatter.formatSpeed(kph, isMetric)
    }

    fun formatSpeedSecondary(kph: Float): String {
        return UnitFormatter.formatSpeed(kph, !isMetric)
    }

    fun formatWatts(watts: Float): String {
        return UnitFormatter.formatWatts(watts)
    }

    fun formatClock(): String {
        return UnitFormatter.formatClock(is24HourFormat)
    }
}


/**
 * Class for tracking UI values for specific stat
 */
class StatInfo(
    private val labelIdDefault: Int = com.jht.translations.R.string.stats_tc,
    private val iconIdDefault: Int = R.drawable.ic_time_total_white,
    private val textDefault: String = "",
    private val unitsDefault: Int = com.jht.translations.R.string.units_tc,
    private val showGoalButtonsDefault: Boolean = false,
    private val splitTextDefault: String = "",
    private val splitUnitsDefault: Int = R.string.units_tc,
) {
    constructor(type: StatType) : this() {
        this.type = type
    }

    var type: StatType = StatType.CLOCK

    val active = mutableStateOf(true) // some stats are toggled active by live conditions like certain device being connected
    val labelId by lazy { mutableStateOf(labelIdDefault) }
    val text by lazy { mutableStateOf(textDefault) }
    val splitText by lazy { mutableStateOf(splitTextDefault) }
    val units by lazy { mutableStateOf(unitsDefault) }
    val splitUnits by lazy { mutableStateOf(splitUnitsDefault) }
    val iconId by lazy { mutableStateOf(iconIdDefault) }
    val showGoalButtons by lazy { mutableStateOf(showGoalButtonsDefault) }

    fun hasSplit() = splitText.value.isNotEmpty()
    override fun toString(): String {
        return type.toString()
    }

    companion object {
        fun getList(frameType: FrameType, showWatts: Boolean, isTargetHeartRate: Boolean, showCadence: Boolean)
            = StatList.getList(frameType, showWatts, isTargetHeartRate, showCadence).map { c-> c.map { r-> StatInfo(r) } }

        fun dummyBikeStatListOfList() = listOf(
            listOf(
                StatInfo(R.string.heart_rate_uc, R.drawable.ic_heart_white, "80", R.string.bpm_uc),
                StatInfo(R.string.peak_heart_rate_uc, R.drawable.ic_heart_white, "170", R.string.bpm_uc),
            ),
            listOf(
                StatInfo(R.string.calories_uc, R.drawable.ic_calories_white, "947", R.string.active_uc, false, "953", R.string.total_uc),
                StatInfo(R.string.calories_uc, R.drawable.ic_calories_white, "47", R.string.units_tc),
                StatInfo(R.string.calories_per_hour_uc, R.drawable.ic_calories_hour_white, "122", R.string.units_tc),
                StatInfo(R.string.watts_uc, R.drawable.ic_watts_white, "14", R.string.units_tc),
                StatInfo(R.string.mets_uc, R.drawable.ic_mets_white, "1.70", R.string.units_tc),
            ),
            listOf(
                StatInfo(R.string.time_elapsed_uc, R.drawable.ic_time_elapsed_white, "5:12", R.string.units_tc, false),
                StatInfo(R.string.time_remaining_uc, R.drawable.ic_time_remaining_white, "24:48", R.string.units_tc, true),
                StatInfo(R.string.time_goal_uc, R.drawable.ic_time_total_white, "22120:00", R.string.units_tc, true),
                StatInfo(R.string.clock_uc, R.drawable.ic_heart_white, "11:46", R.string.units_tc, false),
            ),
            listOf(
                StatInfo(R.string.miles_uc, R.drawable.ic_distance_white, "0.37", R.string.units_tc),
                StatInfo(R.string.km_uc, R.drawable.ic_distance_white, "0.61", R.string.units_tc),
            ),
            listOf(
                StatInfo(R.string.rpm_uc, R.drawable.ic_heart_white, "30", R.string.units_tc),
                StatInfo(R.string.mph_uc, R.drawable.ic_icon_speed_white, "3.7", R.string.units_tc),
                StatInfo(R.string.pace_uc, R.drawable.ic_pace_white, "16:12", R.string.units_tc),
                StatInfo(R.string.average_pace_uc, R.drawable.ic_pace_white, "20:00", R.string.units_tc),
            ),
        )

        fun registerListener(statListOfList: List<List<StatInfo>>, uiBaseModel: UIBaseModel, workoutModel: WorkoutModel) {
            for (statList in statListOfList) {
                for (stat in statList) {
                    registerStatListener(stat, uiBaseModel, workoutModel)
                }
            }
        }
    }
}

/** Represents one list of [StatInfo] and a selection state */
class StatInfoCategory(
    val statList: List<StatInfo>,
) {
    val selected = mutableStateOf<StatInfo?>(null)
    init { reset() }
    fun reset() { selected.value = if (statList.isNotEmpty()) statList[0] else null }
    fun getActiveSize(): Int {
        var sum = 0
        statList.forEach { if (it.active.value) sum++ }
        return sum
    }
    operator fun get(index: Int) = try { statList[index] } catch (_:Exception) { null }
    fun getUnselectedList() = statList.filter { it != selected.value }
    fun getActiveList() = statList.filter { it.active.value }
}

/** Represents one list of [StatInfoCategory] and a selection state */
class StatInfoCollection(
    val categoryList: List<StatInfoCategory>,
) {
    // Is there a more generic way to handle things like MyZone stats?
    val myZoneActive = mutableStateOf(false) // track the live availability of MyZone stats
    val myZoneHrPercent = mutableStateOf(0) // some UI formatting may depend on this
    val caloriesSource = mutableStateOf(CaloriesSource.Standard)
    fun isEmpty(): Boolean {
        for (category in categoryList) {
            if (category.statList.isNotEmpty()) return false
        }
        return true
    }
    init { reset() }
    fun reset() {
        for (category in categoryList) category.reset()
    }
    operator fun get(index: Int) = try { categoryList[index] } catch (_:Exception) { null }
    operator fun get(category: Int, stat: Int) = get(category)?.get(stat)
    fun getCategoryActiveSize(index: Int): Int = get(index)?.getActiveSize() ?: 0
    fun registerListener(uiBaseModel: UIBaseModel, deviceModel: DeviceModel, workoutModel: WorkoutModel) {
        for (category in categoryList) {
            for (info in category.statList) {
                registerStatListener(info, uiBaseModel, workoutModel)
            }
        }
        deviceModel.heartRateSource.listenAndTrigger { showMyZone(it == HeartRateSource.MyZone) }
        workoutModel.goalType.listenAndTrigger { showGoals(it != WorkoutGoalType.GOAL_TYPE_NONE) }
        workoutModel.myZoneHeartRatePercent.listenAndTrigger { myZoneHrPercent.value = it }
        workoutModel.caloriesSource.listenAndTrigger {
            caloriesSource.value = it
        }
    }
    fun findStatInfo(statType: StatType): StatInfo? {
        for (category in categoryList) {
            val info = category.statList.find { it.type == statType }
            if (info != null) return info
        }
        return null
    }
    fun findStatCategory(statType: StatType): StatInfoCategory? {
        for (category in categoryList) {
            val info = category.statList.find { it.type == statType }
            if (info != null) return category
        }
        return null
    }
    private fun showMyZone(show: Boolean) {
        myZoneActive.value = show
        categoryList.forEach {category->
            if (category.selected.value?.type?.isMyZone() == true) category.reset()
            category.statList.forEach {stat->
                if (stat.type.isMyZone()) stat.active.value = show
            }
        }
    }
    private fun showGoals(show: Boolean) {
        categoryList.forEach {category->
            if (category.selected.value?.type?.isGoal() == true) category.reset()
            category.statList.forEach {stat->
                if (stat.type.isGoal()) stat.active.value = show
            }
        }
    }
    companion object {
        fun fromListOfList(statListOfList: List<List<StatInfo>>): StatInfoCollection {
            val list = mutableListOf<StatInfoCategory>()
            for (statList in statListOfList) {
                list.add(StatInfoCategory(statList))
            }
            return StatInfoCollection(list)
        }
        fun getList(frameType: FrameType, showWatts: Boolean, isTargetHeartRate: Boolean, showCadence: Boolean) =
            fromListOfList(StatInfo.getList(frameType, showWatts, isTargetHeartRate, showCadence))
        fun dummyBikeStatInfoCollection() = fromListOfList(StatInfo.dummyBikeStatListOfList())
    }
}

fun averageCadence(accumulatedSteps: Int, timeSeconds: Int): Float {
    if (timeSeconds <= 0) return 0f
    return (accumulatedSteps.toFloat() / timeSeconds.toFloat()) * 60f
}

/** Wires a [StatInfo] class to needed interfaces */
private fun registerStatListener(
    stat: StatInfo,
    uiBaseModel: UIBaseModel,
    workoutStats: WorkoutModel,
) {

    val statFormatter = StatFormatter(
        frameType = uiBaseModel.frameType,
        isMetric = workoutStats.isMetric.value,
        is24HourFormat = workoutStats.is24HourFormat,
        precision = workoutStats.distancePrecision,
    )
    val isMetric = workoutStats.isMetric.value
    /** TODO - creative wants most of these strings to be title case, we only have them in upper case.
     * this is a module so we only consume the translations artifact, we need to get these into
     * our regular translations updates in order for them to be truly translated.
     * I am going to add these new strings to a local strings file in this module for now
     */
    with(stat) {
        when (stat.type) {
            AVERAGE_PACE -> {
                iconId.value = R.drawable.ic_pace_white
                labelId.value = R.string.average_pace_tc
                units.value = R.string.minutes_tc
                workoutStats.averageSpeedKph.listenAndTrigger {
                    // pace is already in preferred unit
                    text.value = statFormatter.formatPaceFromSpeed(it)
                }
            }
            CALORIES -> {
                iconId.value = R.drawable.ic_calories_white
                // workout service will put MyZone calories in for regular calories, so a separate handler is not needed
                workoutStats.calories.listenAndTrigger {
                    text.value = statFormatter.formatCalories(it)
                    labelId.value = R.string.calories_tc
                    units.value = R.string.total_tc
                }
                workoutStats.gymkitCalories.listenAndTrigger {
                    if (it < 0) {
                        splitText.value = ""
                    } else {
                        splitText.value = statFormatter.formatCalories(it)
                        // update gymkit label
                        units.value = R.string.total_tc
                        splitUnits.value = R.string.active_tc
                    }
                }
            }
            CALORIES_PER_HOUR -> {
                iconId.value = R.drawable.ic_calories_hour_white
                labelId.value = R.string.calories_per_hour_tc
                units.value = R.string.empty
                workoutStats.caloriesPerHour.listenAndTrigger {
                    text.value = statFormatter.formatInt(it)
                }
            }
            CLOCK -> {
                iconId.value = R.drawable.ic_time_total_white
                labelId.value = R.string.clock_tc
                units.value = R.string.empty
                workoutStats.clockUpdateEvent.listen {
                    text.value = statFormatter.formatClock()
                }
            }
            DISTANCE -> {
                iconId.value = R.drawable.ic_distance_white
                labelId.value = R.string.distance_tc
                if (isMetric) {
                    units.value = R.string.kilometers_tc
                } else{
                    units.value = R.string.miles_tc
                }
                workoutStats.distanceKm.listenAndTrigger {
                    text.value = statFormatter.formatDistance(it)
                }
            }
            DISTANCE_SECONDARY -> {
                iconId.value = R.drawable.ic_distance_white
                labelId.value = R.string.distance_tc
                if (!isMetric) { // special logic for secondary distance (we show both miles and km) can't be the same as one above
                    units.value = R.string.kilometers_tc
                } else{
                    units.value = R.string.miles_tc
                }
                workoutStats.distanceKm.listenAndTrigger {
                    text.value = statFormatter.formatDistanceSecondary(it)
                }

            }
            ELEVATION -> {
                iconId.value = R.drawable.ic_elevation_white
                labelId.value = R.string.elevation_tc
                if (isMetric) {
                    units.value = R.string.meters_tc
                } else{
                    units.value = R.string.feet_tc
                }
                workoutStats.elevationMeter.listenAndTrigger {
                    text.value = statFormatter.formatElevation(it)
                }
            }
            FLOORS -> {
                iconId.value = R.drawable.ic_floors_white
                labelId.value = R.string.floors_tc
                units.value = R.string.total_floors_tc
                workoutStats.floors.listenAndTrigger {
                    text.value = statFormatter.formatFloors(it)
                }
            }
            GOAL_REMAINING -> {
                showGoalButtons.value = true
                workoutStats.goalRemaining.listenAndTrigger {
                    val goalType = workoutStats.goalType.value
                    if (goalType.isGoaTime) {
                        text.value = statFormatter.formatTimeMilliseconds(it)
                        iconId.value = R.drawable.ic_time_total_white
                        labelId.value = R.string.time_remaining_tc
                        units.value = R.string.minutes_tc
                    } else if (goalType.isGoalCalories) {
                        text.value = statFormatter.formatCalories(it)
                        iconId.value = R.drawable.ic_calories_white
                        labelId.value = R.string.calories_remaining_tc
                        units.value = R.string.empty
                    } else if (goalType.isGoalDistance) {
                        text.value = statFormatter.formatDistanceGoal(it)
                        iconId.value = R.drawable.ic_distance_white
                        labelId.value = R.string.distance_remaining_tc
                        if (isMetric) {
                            units.value = R.string.kilometers_tc
                        } else{
                            units.value = R.string.miles_tc
                        }
                    } else if (goalType.isGoalFloor) {
                        text.value = statFormatter.formatFloorGoal(it)
                        iconId.value = R.drawable.ic_floors_white
                        labelId.value = R.string.floors_remaining_tc
                        units.value = R.string.floors_tc
                    } else {
                        text.value = statFormatter.formatTimeMilliseconds(it)
                        iconId.value = R.drawable.ic_time_total_white
                        labelId.value = R.string.time_remaining_tc
                        units.value = R.string.minutes_tc
                    }
                }
            }
            HEART_RATE -> {
                iconId.value = R.drawable.ic_heart_white
                labelId.value = R.string.heart_rate_tc
                units.value = R.string.bpm_uc
                workoutStats.heartRate.listenAndTrigger {
                    text.value = statFormatter.formatHeartRate(it)
                }
            }
            INCLINE -> {
                iconId.value = R.drawable.ic_incline_white
                labelId.value = R.string.incline_tc
                units.value = R.string.percent_lc
                workoutStats.elevationMeter.listenAndTrigger {
                    text.value = statFormatter.formatIncline(it)
                }
            }
            METS -> {
                iconId.value = R.drawable.ic_mets_white
                labelId.value = R.string.mets_tc
                units.value = R.string.empty
                workoutStats.mets.listenAndTrigger {
                    text.value = statFormatter.formatMets(it)
                }
            }
            PACE -> {
                iconId.value = R.drawable.ic_pace_white
                labelId.value = R.string.pace_tc
                units.value = R.string.minutes_tc
                workoutStats.speedKph.listenAndTrigger {
                    text.value = statFormatter.formatPaceFromSpeed(it)
                }
            }
            PEAK_HEART_RATE -> {
                iconId.value = R.drawable.ic_heart_white
                labelId.value = R.string.peak_heart_rate_tc
                units.value = R.string.bpm_uc
                workoutStats.peakHeartRate.listenAndTrigger {
                    text.value = statFormatter.formatHeartRate(it)
                }
            }
            PERCENT_HEART_RATE -> {
                iconId.value = R.drawable.ic_heart_white
                // TODO does percent heart rate have a label and a unit, or is it just one of the two?
                labelId.value = R.string.percent_heart_rate_tc // "% Heart Rate"
                units.value = R.string.empty
                workoutStats.targetHeartRatePercent.listenAndTrigger {
                    text.value = statFormatter.formatHeartRate(it)
                }
            }
            RPM -> {
                iconId.value = R.drawable.ic_rpm_white
                labelId.value = R.string.empty
                units.value = R.string.rpm_uc
                workoutStats.rpm.listenAndTrigger {
                    text.value = statFormatter.formatInt(it)
                }
            }
            SPEED -> {
                iconId.value = R.drawable.ic_speed_white
                labelId.value = R.string.speed_tc
                if (isMetric) {
                    units.value = R.string.kph_uc
                } else {
                    units.value = R.string.mph_uc
                }
                workoutStats.speedKph.listenAndTrigger {
                    text.value = statFormatter.formatSpeed(it)
                }
            }
            SPEED_SECONDARY -> {
                // secondary reverses primary unit
                iconId.value = R.drawable.ic_speed_white
                labelId.value = R.string.speed_tc
                if (!isMetric) {
                    units.value = R.string.kph_uc
                } else {
                    units.value = R.string.mph_uc
                }
                workoutStats.speedKph.listenAndTrigger {
                    text.value = statFormatter.formatSpeedSecondary(it)
                }
            }
            STEPS -> {
                iconId.value = if (uiBaseModel.frameType.isClimbmill) R.drawable.ic_steps_total_white else R.drawable.ic_cadence_steps_white
                labelId.value = R.string.empty
                units.value = R.string.steps_tc // steps are a unit, so it occupies the text field below the stat (make it a labelID to put it above the stat)
                workoutStats.steps.listenAndTrigger {
                    text.value = statFormatter.formatInt(it)
                }
            }
            SPM -> {
                iconId.value = if (uiBaseModel.frameType.isClimbmill) R.drawable.ic_spm_white else R.drawable.ic_cadence_white
                labelId.value = R.string.empty
                units.value = R.string.spm_uc // SPM is a unit, so it occupies the text field below the stat (make it a labelID to put it above the stat)
                workoutStats.spm.listenAndTrigger {
                    text.value = statFormatter.formatInt(it)
                }
            }
            AVERAGE_SPM -> {
                iconId.value = R.drawable.ic_spm_white
                labelId.value = R.string.average_spm_tc
                units.value = R.string.spm_uc  // SPM is a unit, so it occupies the text field below the stat (make it a labelID to put it above the stat)
                workoutStats.averageSpm.listenAndTrigger {
                    val accumulatedSteps = workoutStats.steps.value
                    val timeSeconds = workoutStats.timeElapsedMs.value / 1000
                    val averageSpm = averageCadence(accumulatedSteps, timeSeconds).toInt()
                    text.value = statFormatter.formatInt(averageSpm)
                }
            }
            STRIDE_LENGTH -> {
                iconId.value = R.drawable.ic_pace_white
                labelId.value = R.string.stride_length_tc
                units.value = R.string.meters_tc
                workoutStats.strideLengthMeters.listenAndTrigger {
                    text.value = statFormatter.formatStrideLength(it, 2)
                }
            }
            AVERAGE_STRIDE_LENGTH -> {
                iconId.value = R.drawable.ic_pace_white
                labelId.value = R.string.average_stride_length_tc
                units.value = R.string.meters_tc
                workoutStats.averageStrideLengthMeters.listenAndTrigger {
                    text.value = statFormatter.formatStrideLength(it, 2)
                }
            }
            TIME_ELAPSED -> {
                showGoalButtons.value = false
                iconId.value = R.drawable.ic_time_elapsed_white
                labelId.value = R.string.time_elapsed_tc
                units.value = R.string.minutes_tc
                workoutStats.timeElapsedMs.listenAndTrigger {
                    text.value = statFormatter.formatTimeMilliseconds(it)
                }
            }
            TOTAL_GOAL -> {
                showGoalButtons.value = true
                workoutStats.goalTotal.listenAndTrigger {
                    val goalType = workoutStats.goalType.value
                    if (goalType.isGoaTime) {
                        text.value = statFormatter.formatTimeMilliseconds(it)
                        iconId.value = R.drawable.ic_time_total_white
                        labelId.value = R.string.total_time_tc
                        units.value = R.string.minutes_tc
                    } else if (goalType.isGoalCalories) {
                        text.value = statFormatter.formatCalories(it)
                        iconId.value = R.drawable.ic_calories_white
                        labelId.value = R.string.total_calories_tc
                        units.value = R.string.empty
                    } else if (goalType.isGoalDistance) {
                        text.value = statFormatter.formatDistanceGoal(it)
                        iconId.value = R.drawable.ic_distance_white
                        labelId.value = R.string.total_distance_tc
                        if (isMetric) {
                            units.value = R.string.kilometers_tc
                        } else {
                            units.value = R.string.miles_tc
                        }
                    } else if (goalType.isGoalFloor) {
                        text.value = statFormatter.formatFloors(it)
                        iconId.value = R.drawable.ic_floors_white
                        labelId.value = R.string.total_floors_tc
                        units.value = R.string.floors_tc
                    } else { // same as time goal
                        text.value = statFormatter.formatTimeMilliseconds(it)
                        iconId.value = R.drawable.ic_time_total_white
                        labelId.value = R.string.total_time_tc
                        units.value = R.string.minutes_tc
                    }
                }
            }
            WATTS -> {
                iconId.value = R.drawable.ic_watts_white
                labelId.value = R.string.watts_tc
                units.value = R.string.empty
                workoutStats.watts.listenAndTrigger {
                    text.value = statFormatter.formatWatts(it)
                }
            }
            MYZONE_HR_PERCENT -> {
                active.value = false
                iconId.value = R.drawable.logo_myzone
                labelId.value = R.string.percent_hr_uc
                units.value = R.string.empty
                workoutStats.myZoneHeartRatePercent.listenAndTrigger {
                    text.value = statFormatter.formatInt(it)
                }
            }
            MYZONE_MEPS -> {
                active.value = false
                iconId.value = R.drawable.logo_myzone
                labelId.value = R.string.meps_tc
                units.value = R.string.empty
                workoutStats.myZoneMeps.listenAndTrigger {
                    text.value = statFormatter.formatInt(it)
                }
            }


        }
    }
}
