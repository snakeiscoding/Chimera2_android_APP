package com.jht.workoutdata.provider

import android.net.Uri

/**
 * The public contract for [WorkoutContentProvider].
 *
 * Callers should prefer the typed `WorkoutDataClient` over touching these
 * constants directly; they are exposed for cross-process callers and tests.
 *
 * Reads use the canonical `query()` / Cursor path: results stream through a
 * `CursorWindow` in shared memory, so even a full-history read is not bound by
 * the ~1 MB Binder transaction limit. Window and down-sample options are passed
 * as query parameters on the read [Uri].
 *
 * Writes use [android.content.ContentProvider.call] with a [Parcelable] point,
 * which is a single small transaction.
 */
object WorkoutDataContract {

    /** Must match the authority declared in the module's AndroidManifest. */
    const val AUTHORITY = "com.jht.workoutdata.provider"

    /**
     * Signature-level permission required to WRITE (addPoint / clear). Reads are
     * open to any app. Enforced in code inside [WorkoutContentProvider.call]
     * because the framework does not apply write-permission checks to call().
     * First-party writer APKs must `<uses-permission>` this.
     */
    const val PERMISSION_WRITE = "com.jht.workoutdata.permission.WRITE"

    /** Read endpoint, e.g. content://<authority>/points?window_sec=..&downsample_sec=.. */
    const val PATH_POINTS = "points"
    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_POINTS")

    /**
     * Future-profile endpoint. Change notifications for the planned (future) workout
     * profile fire on this URI, separately from [CONTENT_URI] (history) — observers can
     * tell what changed by which URI they registered on.
     */
    const val PATH_FUTURE = "future"
    val FUTURE_CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$PATH_FUTURE")

    /** Read query parameters (all optional), in seconds of elapsed workout time. */
    const val QUERY_PARAM_WINDOW_SEC = "window_sec"          // Int; absent/<=0 = all data
    const val QUERY_PARAM_DOWNSAMPLE_SEC = "downsample_sec"  // Int; absent/<=0 = no down-sampling
    // Absolute range on elapsed_time_sec (inclusive). When from/to are present they take
    // precedence over window/downsample. Used for incremental appends and gap backfill.
    const val QUERY_PARAM_FROM_SEC = "from_sec"             // Int; inclusive lower bound
    const val QUERY_PARAM_TO_SEC = "to_sec"                 // Int; inclusive upper bound

    /**
     * call() methods. addPoint/setFuture/clear require the write permission;
     * getFuture/getGeneration are open.
     */
    const val METHOD_ADD_POINT = "addPoint"
    const val METHOD_SET_FUTURE = "setFuture"
    const val METHOD_GET_FUTURE = "getFuture"
    const val METHOD_CLEAR = "clear" // call when a new workout starts; also clears the future
    /**
     * Returns the store's generation token ([EXTRA_GENERATION]): a value that changes on
     * every [METHOD_CLEAR] and on provider-process restart, and is otherwise stable.
     * Incremental readers compare it between reads to detect that the history they hold
     * belongs to an earlier workout — elapsed-second continuity alone cannot detect a
     * clear once a new workout's clock passes the old one.
     */
    const val METHOD_GET_GENERATION = "getGeneration"

    /** Bundle keys for [METHOD_ADD_POINT]. */
    const val EXTRA_DATA_POINT = "extra_data_point"   // WorkoutDataPoint (Parcelable)

    /** Bundle key for [METHOD_SET_FUTURE] input and [METHOD_GET_FUTURE] output. */
    const val EXTRA_FUTURE = "extra_future"           // WorkoutFutureData (Parcelable)

    /** Bundle key for [METHOD_GET_GENERATION] output. */
    const val EXTRA_GENERATION = "extra_generation"   // Long

    /** Bundle keys for call() results. */
    const val EXTRA_RESULT_COUNT = "extra_result_count" // Int
    const val EXTRA_ERROR = "extra_error"               // String; present only on failure

    /** Storage column names (shared by the Room entity, the Cursor, and clients). */
    const val COLUMN_ID = "id"
    const val COLUMN_ELAPSED_TIME_SEC = "elapsed_time_sec"
    const val COLUMN_WORKOUT_STAGE = "workout_stage"
    const val COLUMN_LAP_INDEX = "lap_index"
    const val COLUMN_SPLIT_INDEX = "split_index"
    const val COLUMN_DISTANCE_KM = "distance_km"
    const val COLUMN_SPEED_KPH = "speed_kph"
    const val COLUMN_INCLINE_PERCENT = "incline_percent"
    const val COLUMN_RESISTANCE = "resistance"
    const val COLUMN_RPM = "rpm"
    const val COLUMN_SPM = "spm"
    const val COLUMN_HEART_RATE = "heart_rate"
    const val COLUMN_WATTS = "watts"
    const val COLUMN_TARGET_HEART_RATE = "target_heart_rate"
    const val COLUMN_TARGET_WATTS = "target_watts"
    const val COLUMN_TOTAL_CALORIES = "total_calories"
    const val COLUMN_METS = "mets"
    const val COLUMN_TOTAL_FLOORS = "total_floors"
    const val COLUMN_TOTAL_STEPS = "total_steps"
    const val COLUMN_ELEVATION_METERS = "elevation_meters"
}
