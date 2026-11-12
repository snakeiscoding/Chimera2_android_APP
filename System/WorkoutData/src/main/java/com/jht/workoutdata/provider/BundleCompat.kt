package com.jht.workoutdata.provider

import android.os.Build
import android.os.Bundle
import android.os.Parcelable

/**
 * Backwards-compatible [Bundle.getParcelable] that uses the type-safe API on
 * Android 13+ (Tiramisu) and the deprecated one below it.
 */
@Suppress("DEPRECATION")
internal fun <T : Parcelable> Bundle.getParcelableCompat(key: String, clazz: Class<T>): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, clazz)
    } else {
        @Suppress("UNCHECKED_CAST")
        getParcelable(key) as? T
    }
