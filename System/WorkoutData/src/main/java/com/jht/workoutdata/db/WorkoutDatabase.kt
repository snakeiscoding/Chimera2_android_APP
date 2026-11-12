package com.jht.workoutdata.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for workout history.
 *
 * Currently built **in-memory** ([inMemory]): all data lives in RAM and is
 * dropped when the owning process dies — matching the "in-memory now" decision
 * while still giving us SQL windowing/down-sampling and `CursorWindow` reads.
 *
 * To persist later, swap [inMemory] for a file-backed builder; nothing else in
 * the module changes:
 * ```
 * Room.databaseBuilder(context, WorkoutDatabase::class.java, "workout.db").build()
 * ```
 *
 * An in-memory database is destroyed when its last connection closes, so callers
 * must hold a single long-lived instance (the ContentProvider does this for the
 * life of its process).
 */
@Database(entities = [WorkoutDataPointEntity::class], version = 1, exportSchema = false)
abstract class WorkoutDatabase : RoomDatabase() {

    abstract fun workoutDataDao(): WorkoutDataDao

    companion object {
        fun inMemory(context: Context): WorkoutDatabase =
            Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                WorkoutDatabase::class.java,
            ).build()
    }
}
