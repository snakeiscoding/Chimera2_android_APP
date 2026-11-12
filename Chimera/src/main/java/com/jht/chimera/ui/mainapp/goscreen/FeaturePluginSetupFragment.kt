package com.jht.chimera.ui.mainapp.goscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.jht.datamatrixlib.DataMatrix
import com.jht.enums.WorkoutId
import com.jht.mastboard.data.UiEventKeys
import com.jht.theme.DynamicTheme
import com.jht.ui.clickableNoEffect
import dagger.hilt.android.AndroidEntryPoint

/** Scrim behind the setup card, matches ModalDialogBase in the new workout selection screen. */
private const val SCRIM_ALPHA = 0.7f

/**
 * Hosts a compose feature workout plugin setup screen inside the legacy java go screens.
 *
 * Takes the whole activity content view rather than sitting inside WorkoutSetupFragment's inset
 * card: plugin setup screens draw their own centered modal (Sprint8 uses fillMaxSize(0.67f)) and
 * size their contents against the window width via computeSize(), so nesting them inside another
 * card renders them small and cramped. This fragment therefore provides what ModalDialogBase
 * provides in the new UI - a dim scrim that dismisses on an outside touch - plus the feature setup
 * UI events and the close-on-pause behaviour the legacy modal host used to contribute.
 *
 * The plugin starts the workout through its own launch path, so nothing is reported back to
 * IGoScreenInterface - the modal simply closes when the go screen pauses to launch the workout.
 *
 * Instances are single use: create a new one for every showing with [forSprint8], [forHiit] or [freshCopy].
 * The host activity must be annotated @AndroidEntryPoint, since the plugin's setup UI resolves its
 * view model with hiltViewModel().
 */
@AndroidEntryPoint
class FeaturePluginSetupFragment : Fragment() {

    private val viewModel: FeaturePluginSetupViewModel by viewModels()

    val workoutId: Int
        get() = arguments?.getInt(ARG_WORKOUT_ID) ?: WorkoutId.SPRINT8.id

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            DynamicTheme(viewModel.theme.value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                        .clickableNoEffect(onClick = { dismiss() }),
                    contentAlignment = Alignment.Center,
                ) {
                    val activePlugin = viewModel.plugin.value
                    activePlugin?.SetupScreen(
                        workoutInputs = viewModel.workoutInputs,
                        onDismissRequest = { dismiss() },
                    )
                }
                // Close rather than sit on an empty modal if the setup could not be loaded
                if (viewModel.loadFailed.value) {
                    LaunchedEffect(Unit) { dismiss() }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.onSetupShown(workoutId)
    }

    override fun onStart() {
        super.onStart()
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.OPEN_FEATURE_WORKOUT_SETUP.name)
    }

    override fun onPause() {
        super.onPause()
        // The go screen pauses to launch the workout, the setup should not be waiting underneath it
        dismiss()
    }

    override fun onStop() {
        super.onStop()
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.CLOSE_FEATURE_WORKOUT_SETUP.name)
    }

    override fun onDestroyView() {
        viewModel.onSetupHidden()
        super.onDestroyView()
    }

    /** A new instance for the same workout, for hosts that hold on to a setup fragment reference. */
    fun freshCopy(): FeaturePluginSetupFragment = newInstance(workoutId)

    private fun dismiss() {
        if (!isAdded) return
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(com.jht.chimera.R.animator.fade_in, com.jht.chimera.R.animator.fade_out)
            .remove(this)
            .commit()
    }

    companion object {
        private const val ARG_WORKOUT_ID = "workoutId"

        @JvmStatic
        fun forSprint8(): FeaturePluginSetupFragment = newInstance(WorkoutId.SPRINT8.id)
        @JvmStatic
        fun forHiit(): FeaturePluginSetupFragment = newInstance(WorkoutId.HIIT.id)

        @JvmStatic
        fun newInstance(workoutId: Int): FeaturePluginSetupFragment =
            FeaturePluginSetupFragment().apply {
                arguments = Bundle().apply { putInt(ARG_WORKOUT_ID, workoutId) }
            }
    }
}
