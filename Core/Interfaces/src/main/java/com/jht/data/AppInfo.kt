package com.jht.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val title: String,
    val uuid: String,
    val icon: Drawable, // might need to be Uri? but some icons have special rendering? just do Drawable for now?
    val locked: Boolean = false, // do we need a full enum for tiers or just need to know if an app is locked? or do we need a string resource to prompt as reason for the lock?
    val category: AppInfoCategory = AppInfoCategory.Entertainment,
    val tunerDependent: Boolean = false,
)

enum class AppInfoCategory {
    Workout,  // 1st party workout ui (has a workout selection associated) - simple, track, target, constant watts
    Entertainment,  // 3rd party non-workout - netflix, facebook (can be shown outside a workout, retail go screen)
    Integration,    // 3rd party integration - ifit, ymca
    FullIntegration, // 3rd party integration that owns the full user flow incl. post-workout (hero's journey, custom override) - system should not idle-timeout back to splash
    System; // internal systems apps - basically the "change" icons;

    companion object {
        fun fromString(value: String): AppInfoCategory? {
            return entries.find { it.name.equals(value, ignoreCase = true) }
        }
    }
}
