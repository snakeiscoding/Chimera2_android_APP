package com.jht.workoutdata.db

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data-access for workout points.
 *
 * The time axis is `elapsed_time_sec` (one point per workout second). Windowing
 * (`last N`) and down-sampling are expressed in SQL so the database does the
 * work and only the requested rows are materialised:
 *  - window:     `WHERE elapsed_time_sec >= :since`
 *  - downsample: bucket by `elapsed_time_sec / :bucketSec` and keep the latest
 *                row in each bucket (via a join on the per-bucket MAX — the join
 *                is duplicate-free only because `elapsed_time_sec` is UNIQUE).
 *
 * Each read has a `List` variant (small reads) and a `Cursor` variant (used by
 * the provider's `query()` so large results stream through a CursorWindow in
 * shared memory, bypassing the Binder size limit).
 */
@Dao
interface WorkoutDataDao {

    /**
     * Inserts one point. elapsed_time_sec has a UNIQUE index, so writing a second
     * that already exists REPLACES the previous row (last write wins) — one row
     * per workout second is a storage invariant.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(point: WorkoutDataPointEntity): Long

    @Query("DELETE FROM workout_points")
    fun clear()

    @Query("SELECT COUNT(*) FROM workout_points")
    fun count(): Int

    @Query("SELECT MAX(elapsed_time_sec) FROM workout_points")
    fun latestElapsedSec(): Int?

    /** Retention guard: keep only the [keep] most recent rows. */
    @Query(
        """
        DELETE FROM workout_points
        WHERE id NOT IN (
            SELECT id FROM workout_points ORDER BY elapsed_time_sec DESC LIMIT :keep
        )
        """
    )
    fun trimToMostRecent(keep: Int)

    // --- List reads ---

    @Query("SELECT * FROM workout_points WHERE elapsed_time_sec >= :since ORDER BY elapsed_time_sec ASC")
    fun pointsSince(since: Int): List<WorkoutDataPointEntity>

    @Query(
        """
        SELECT t.* FROM workout_points t
        JOIN (
            SELECT MAX(elapsed_time_sec) AS mx FROM workout_points
            WHERE elapsed_time_sec >= :since
            GROUP BY elapsed_time_sec / :bucketSec
        ) g ON t.elapsed_time_sec = g.mx
        ORDER BY t.elapsed_time_sec ASC
        """
    )
    fun pointsSinceDownsampled(since: Int, bucketSec: Int): List<WorkoutDataPointEntity>

    // --- Absolute range reads (incremental appends / gap backfill) ---

    @Query("SELECT * FROM workout_points WHERE elapsed_time_sec BETWEEN :from AND :to ORDER BY elapsed_time_sec ASC")
    fun pointsBetween(from: Int, to: Int): List<WorkoutDataPointEntity>

    @Query("SELECT * FROM workout_points WHERE elapsed_time_sec BETWEEN :from AND :to ORDER BY elapsed_time_sec ASC")
    fun pointsBetweenCursor(from: Int, to: Int): Cursor

    // --- Cursor reads (large; stream via CursorWindow / shared memory) ---

    @Query("SELECT * FROM workout_points WHERE elapsed_time_sec >= :since ORDER BY elapsed_time_sec ASC")
    fun pointsSinceCursor(since: Int): Cursor

    @Query(
        """
        SELECT t.* FROM workout_points t
        JOIN (
            SELECT MAX(elapsed_time_sec) AS mx FROM workout_points
            WHERE elapsed_time_sec >= :since
            GROUP BY elapsed_time_sec / :bucketSec
        ) g ON t.elapsed_time_sec = g.mx
        ORDER BY t.elapsed_time_sec ASC
        """
    )
    fun pointsSinceDownsampledCursor(since: Int, bucketSec: Int): Cursor
}
