package com.jht.sprint8.assessment.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Provides a [DataStore] instance for storing assessment-related preferences.
 * This extension property initializes a Preferences DataStore scoped to the
 * application context and is used to persist assessment visibility state
 * and the recommended assessment level.
 */
val Context.assessmentDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "assessment_prefs"
)

/**
 * Manages persistence of assessment-related preferences using Jetpack DataStore.
 * This class is responsible for saving, clearing, and observing assessment
 * visibility and recommended assessment level values. It acts as a single
 * source of truth for assessment-related preferences and exposes them as
 * reactive [Flow] streams.
 * @constructor Creates an instance of [AssessmentDataStoreManager].
 * @param context Application context used to access the [DataStore] instance.
 */
class AssessmentDataStoreManager @Inject constructor(
    @ApplicationContext context: Context
) {
    /** Internal DataStore instance for assessment preferences. */
    private val dataStore = context.assessmentDataStore
    /** Preference key used to store whether the assessment is visible. */
    private val ASSESSMENT_VISIBLE_KEY = booleanPreferencesKey("is_assessment_visible")

    /** Preference key used to store the recommended assessment level. */
    private val ASSESSMENT_RECOMMENDED_LEVEL_KEY = intPreferencesKey("assessment_recommended_level")

    /**
     * Saves the assessment visibility state.
     * @param isVisible
     * `true` if the assessment should be shown to the user,
     * `false` otherwise.
     */
    suspend fun saveAssessmentVisibility(isVisible: Boolean) {
        dataStore.edit { preferences ->
            preferences[ASSESSMENT_VISIBLE_KEY] = isVisible
        }
    }
    /**
     * Clears all assessment-related preferences.
     * This removes both the assessment visibility flag and
     * the recommended assessment level from the DataStore.
     * Useful when resetting assessment state.
     */
    suspend fun clearAssessmentVisibility() {
        dataStore.edit { preferences ->
            preferences.remove(ASSESSMENT_VISIBLE_KEY)
            preferences.remove(ASSESSMENT_RECOMMENDED_LEVEL_KEY)
        }
    }
    /**
     * Saves the recommended assessment level.
     * @param assessmentLevel
     * Integer value representing the recommended assessment level.
     */
    suspend fun saveRecommendedLevel(assessmentLevel: Int) {
        dataStore.edit { preferences ->
            preferences[ASSESSMENT_RECOMMENDED_LEVEL_KEY] = assessmentLevel
        }
    }
    /**
     * Emits the current assessment visibility state.
     * @return
     * A [Flow] that emits `true` if the assessment is visible,
     * or `false` if not set or cleared.
     */
    val isAssessmentVisible: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[ASSESSMENT_VISIBLE_KEY] ?: false
        }
    /**
     * Emits the recommended assessment level.
     * @return
     * A [Flow] that emits the recommended assessment level value.
     * Defaults to `0` if no level has been stored.
     */
    val assessmentRecommendedLevel: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[ASSESSMENT_RECOMMENDED_LEVEL_KEY] ?: 0
        }
}