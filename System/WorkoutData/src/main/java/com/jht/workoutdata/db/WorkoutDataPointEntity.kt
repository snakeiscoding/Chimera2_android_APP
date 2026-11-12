package com.jht.workoutdata.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jht.workoutdata.model.WorkoutDataPoint
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_DISTANCE_KM
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_ELAPSED_TIME_SEC
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_ELEVATION_METERS
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_HEART_RATE
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_ID
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_INCLINE_PERCENT
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_LAP_INDEX
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_METS
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_RESISTANCE
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_RPM
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_SPEED_KPH
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_SPLIT_INDEX
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_SPM
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_TARGET_HEART_RATE
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_TARGET_WATTS
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_TOTAL_CALORIES
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_TOTAL_FLOORS
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_TOTAL_STEPS
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_WATTS
import com.jht.workoutdata.provider.WorkoutDataContract.COLUMN_WORKOUT_STAGE

/**
 * Room storage row for one workout data point.
 *
 * Kept separate from the [WorkoutDataPoint] transport/domain model so the DB
 * schema can evolve independently of the cross-process contract. Column names
 * come from [com.jht.workoutdata.provider.WorkoutDataContract] so the Cursor a
 * reader receives uses the exact names the contract documents.
 *
 * A UNIQUE index on [elapsedTimeSec] enforces one row per workout second — the
 * elapsed second is the logical key. Combined with the DAO's REPLACE conflict
 * strategy, writing the same second again overrides the previous row (last write
 * wins) instead of accumulating duplicates from poll-timer jitter. The index also
 * keeps the time-window filter (`WHERE elapsed_time_sec >= ?`) and ordering cheap.
 */
@Entity(
    tableName = WorkoutDataPointEntity.TABLE,
    indices = [Index(value = [COLUMN_ELAPSED_TIME_SEC], unique = true)],
)
data class WorkoutDataPointEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = COLUMN_ID) val id: Long = 0,
    @ColumnInfo(name = COLUMN_ELAPSED_TIME_SEC) val elapsedTimeSec: Int,
    @ColumnInfo(name = COLUMN_WORKOUT_STAGE) val workoutStage: Int,
    @ColumnInfo(name = COLUMN_LAP_INDEX) val lapIndex: Int,
    @ColumnInfo(name = COLUMN_SPLIT_INDEX) val splitIndex: Int,
    @ColumnInfo(name = COLUMN_DISTANCE_KM) val distanceKm: Float,
    @ColumnInfo(name = COLUMN_SPEED_KPH) val speedKph: Float,
    @ColumnInfo(name = COLUMN_INCLINE_PERCENT) val inclinePercent: Float,
    @ColumnInfo(name = COLUMN_RESISTANCE) val resistance: Int,
    @ColumnInfo(name = COLUMN_RPM) val rpm: Int,
    @ColumnInfo(name = COLUMN_SPM) val spm: Int,
    @ColumnInfo(name = COLUMN_HEART_RATE) val heartRate: Int,
    @ColumnInfo(name = COLUMN_WATTS) val watts: Int,
    @ColumnInfo(name = COLUMN_TARGET_HEART_RATE) val targetHeartRate: Int,
    @ColumnInfo(name = COLUMN_TARGET_WATTS) val targetWatts: Int,
    @ColumnInfo(name = COLUMN_TOTAL_CALORIES) val totalCalories: Float,
    @ColumnInfo(name = COLUMN_METS) val mets: Float,
    @ColumnInfo(name = COLUMN_TOTAL_FLOORS) val totalFloors: Float,
    @ColumnInfo(name = COLUMN_TOTAL_STEPS) val totalSteps: Int,
    @ColumnInfo(name = COLUMN_ELEVATION_METERS) val elevationMeters: Float,
) {
    fun toDomain(): WorkoutDataPoint = WorkoutDataPoint(
        elapsedTimeSec = elapsedTimeSec,
        workoutStage = workoutStage,
        lapIndex = lapIndex,
        splitIndex = splitIndex,
        distanceKm = distanceKm,
        speedKph = speedKph,
        inclinePercent = inclinePercent,
        resistance = resistance,
        rpm = rpm,
        spm = spm,
        heartRate = heartRate,
        watts = watts,
        targetHeartRate = targetHeartRate,
        targetWatts = targetWatts,
        totalCalories = totalCalories,
        mets = mets,
        totalFloors = totalFloors,
        totalSteps = totalSteps,
        elevationMeters = elevationMeters,
    )

    companion object {
        const val TABLE = "workout_points"

        fun fromDomain(p: WorkoutDataPoint): WorkoutDataPointEntity = WorkoutDataPointEntity(
            elapsedTimeSec = p.elapsedTimeSec,
            workoutStage = p.workoutStage,
            lapIndex = p.lapIndex,
            splitIndex = p.splitIndex,
            distanceKm = p.distanceKm,
            speedKph = p.speedKph,
            inclinePercent = p.inclinePercent,
            resistance = p.resistance,
            rpm = p.rpm,
            spm = p.spm,
            heartRate = p.heartRate,
            watts = p.watts,
            targetHeartRate = p.targetHeartRate,
            targetWatts = p.targetWatts,
            totalCalories = p.totalCalories,
            mets = p.mets,
            totalFloors = p.totalFloors,
            totalSteps = p.totalSteps,
            elevationMeters = p.elevationMeters,
        )
    }
}
