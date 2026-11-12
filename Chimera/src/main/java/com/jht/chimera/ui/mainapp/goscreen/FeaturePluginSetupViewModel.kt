package com.jht.chimera.ui.mainapp.goscreen

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.jht.customhiit.plugin.CustomHIITFeaturePlugin
import com.jht.data.BaseFeaturedWorkoutPlugin
import com.jht.data.WorkoutInputUi
import com.jht.data.WorkoutItem
import com.jht.enums.ConsoleWorkoutVariables
import com.jht.enums.WorkoutId
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.WorkoutCatalogModel
import com.jht.sprint8.plugin.Sprint8FeaturePlugin
import com.jht.theme.ThemedViewModel
import com.jht.theme.defaultDynamicTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

private const val TAG = "FeaturePluginSetup"

/** A workout load that has not answered in this long is never going to. */
private const val LOAD_TIMEOUT_MS = 5000L

/**
 * Provides a [BaseFeaturedWorkoutPlugin] and its workout inputs to the legacy java go screens.
 *
 * This is the small slice of WorkoutSelectionViewModel that a plugin setup screen actually needs:
 * a theme, the plugin instance, and the workout inputs that the plugin treats as the source of
 * truth. The plugin launches the workout itself through WorkoutLauncherModel, so there is no
 * "workout started" callback back into the legacy go screen interfaces.
 *
 * @see FeaturePluginSetupFragment for the view side.
 */
@HiltViewModel
class FeaturePluginSetupViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val uiBaseModel: UIBaseModel,
    private val catalogModel: WorkoutCatalogModel,
) : ThemedViewModel() {

    val theme = mutableStateOf(defaultDynamicTheme)

    /** Plugin being set up. Null until [onSetupShown] has finished loading it. */
    val plugin = mutableStateOf<BaseFeaturedWorkoutPlugin?>(null)

    /** Workout inputs owned by this host. The plugin reads and mutates these, then launches with them. */
    val workoutInputs = mutableStateListOf<WorkoutInputUi>()

    /** Set when the setup could not be loaded, so the host closes instead of showing an empty modal. */
    val loadFailed = mutableStateOf(false)

    // The legacy hosts are not a ThemedActivity, so they drive start/stop through the
    // onSetupShown/onSetupHidden pair below rather than having the activity do it.
    override fun start() {
        uiBaseModel.themeUri.listenAndTrigger { theme.value = loadTheme(it) }
    }

    override fun stop() {
        plugin.value?.onUnload()
        plugin.value = null
        workoutInputs.clear()
        loadFailed.value = false
        catalogModel.close()
        uiBaseModel.close()
    }

    /** Called when the setup modal becomes visible. Builds the plugin and loads its workout inputs. */
    fun onSetupShown(workoutId: Int) {
        if (plugin.value != null) return // already loaded for this showing
        start()
        val newPlugin = createPlugin(workoutId)
        if (newPlugin == null) {
            Log.e(TAG, "No feature plugin available for workout $workoutId, is it in the workout map?")
            loadFailed.value = true
            return
        }
        viewModelScope.launch {
            // Loading the inputs also loads the workout in the back end. That is what makes the
            // plugin's own launch path (WorkoutLauncherModel.launchSelectedWorkout -> the static
            // WorkoutLauncher) valid without the legacy go screen loading the workout for it.
            val inputs = withTimeoutOrNull(LOAD_TIMEOUT_MS) { catalogModel.getWorkoutInputs(workoutId) }
            if (inputs == null) {
                // The load response never came back, see WorkoutManager.createCallback
                Log.e(TAG, "Timed out loading workout inputs for $workoutId, closing setup")
                loadFailed.value = true
                return@launch
            }
            if (inputs.any { it.identifier == ConsoleWorkoutVariables.LEVEL }) {
                catalogModel.setWorkoutControllerModel()
            }
            workoutInputs.clear()
            inputs.forEach { input ->
                workoutInputs.add(WorkoutInputUi(input, mutableFloatStateOf(input.defaultValue)))
            }
            newPlugin.onLoad(workoutInputs)
            plugin.value = newPlugin
            Log.d(TAG, "Setup ready for workout $workoutId with ${workoutInputs.size} inputs")
        }
    }

    /**
     * Called when the setup modal goes away. Tears the plugin down so the next showing starts from
     * freshly loaded inputs, and releases the model listeners.
     */
    fun onSetupHidden() = stop()

    /**
     * Mirror of MatrixWorkoutCatalogModel.populateFeaturePlugin. The legacy go screens do not build
     * the featured/carousel lists that the new workout selection screen creates its plugins from, so
     * the plugin is built here from the loaded workout map instead.
     */
    private fun createPlugin(workoutId: Int): BaseFeaturedWorkoutPlugin? {
        val workoutItem: WorkoutItem = catalogModel.getWorkoutList.get(workoutId) ?: return null
        return when (workoutId) {
            WorkoutId.SPRINT8.id -> Sprint8FeaturePlugin(appContext, workoutItem)
            WorkoutId.HIIT.id -> CustomHIITFeaturePlugin(appContext, workoutItem)
            else -> null // add other plugins here as their legacy setups are retired
        }
    }
}
