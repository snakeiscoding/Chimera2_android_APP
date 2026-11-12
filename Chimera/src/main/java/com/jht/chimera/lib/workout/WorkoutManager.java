package com.jht.chimera.lib.workout;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.VaKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.va.VAWorkoutList;
import com.jht.chimera.lib.workout.workoutlist.WorkoutEntry;
import com.jht.chimera.lib.workout.workoutlist.WorkoutScript;
import com.jht.chimera.lib.workout.workoutlist.WorkoutScripts;
import com.jht.chimera.lib.workout.workoutlist.workoutmap.WorkoutMap;
import com.jht.chimera.lib.workout.workoutparameters.MinMaxDictionary;
import com.jht.chimera.lib.workout.workoutparameters.WorkoutInputManager;
import com.jht.chimera.lib.workoutapps.AppIds;
import com.jht.chimera.service.workout.InitialCondition;
import com.jht.chimera.service.workout.WorkoutLauncher;
import com.jht.chimera.service.workout.WorkoutSettingOverrides;
import com.jht.data.WorkoutBadge;
import com.jht.data.WorkoutItem;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.enums.MinMaxParam;
import com.jht.ui.DrawableUtil;
import com.jht.virtualactive.service.datamodels.Workout;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * A single class for loading and starting workouts. Manages the behind the scenes logic and provides
 * a simple API for starting workouts.
 * <p>
 * To start a workout first load a workout by passing in the workout entry or the ID. Then call
 * {@link #startLoadedWorkout(List, boolean)} to begin the workout and switch to the correct app.
 */
public class WorkoutManager {
    private static final String TAG = "WorkoutManager";
    private final WorkoutMap mWorkoutMap;

    // An interface for returning when the workout has been loaded
    public interface IWorkoutManager {
        void onWorkoutLoaded(List<WorkoutInput> workoutInputs);
    }
	

    /**
     * Instantiates the workout manager with the currently loaded workouts
     *
     * @param workoutMap The workout map to use
     */
    public WorkoutManager(WorkoutMap workoutMap) {
        mWorkoutMap = workoutMap;
    }

    /**
     * Loads the quick start workout for the current frame.
     */
    public void loadQuickStartWorkout(IWorkoutManager callback) {
        loadQuickStartWorkout(null, callback);
    }

    /**
     * Loads the quick start workout for the current frame using the given App ID in place of normal launch App.
     */
    public void loadQuickStartWorkout(String entertainmentAppId, IWorkoutManager callback) {
        loadWorkout(WorkoutIds.MANUAL, entertainmentAppId, callback);
    }

    /**
     * Loads a workout with the ID that is passed in.
     *
     * @param id The ID of the workout to load
     */
    public void loadWorkoutWithId(int id, IWorkoutManager callback) {
        loadWorkout(id, null, callback);
    }

    /**
     * Loads a workout entry and returns the inputs for that workout.
     *
     * @param workoutId     The workout id to load
     * @param optionalAppID The optional app id to load
     * @param callback      The callback for when the workout is loaded
     */
    public synchronized void loadWorkout(int workoutId, String optionalAppID, IWorkoutManager callback) {
        WorkoutItem workout = mWorkoutMap.getWorkout(workoutId);
        if (workout != null) {
            // Load the script and create the workout entry
            WorkoutScripts workoutScripts = new WorkoutScripts();
            WorkoutScript script = workoutScripts.getWorkoutScript(workoutId);
            String appId = optionalAppID == null? workout.getAppId() : optionalAppID;
            if (script != null) {
                WorkoutEntry entry = new WorkoutEntry(workoutId, appId, script.scriptString);
                loadWorkout(entry, callback);
            } else {
                Log.e(TAG, "Cannot load null script for " + workoutId);
            }

        } else {
            Log.e(TAG, "Could not load workout with ID " + workoutId);
            if (callback != null) callback.onWorkoutLoaded(null);
        }
    }

    /**
     * Loads a workout with a known ID and script. This is used for custom workouts on Chimera.
     *
     * @param workoutId The workout id for the loaded workout
     * @param script The script to load
     */
    public synchronized void loadCustomWorkout(int workoutId, String appId, String script, IWorkoutManager callback) {
        if (script == null) {
            Log.e(TAG, "Cannot load null workout entry");
            if (callback != null) callback.onWorkoutLoaded(null);
            return;
        }

        if (appId == null) appId = AppIds.DEFAULT_APP_UUID;
        WorkoutEntry entry = new WorkoutEntry(workoutId, appId, script);
        loadWorkout(entry, callback);
    }
    public synchronized void loadCustomWorkout(int workoutId, String script, IWorkoutManager callback) {
        loadCustomWorkout(workoutId, null, script, callback);
    }

    private synchronized void loadWorkout(WorkoutEntry workoutEntry, IWorkoutManager callback) {
        // Put the parcelable for the workout controller to load, then wait for the callback.
        workoutEntry.callerId = UUID.randomUUID();
        createCallback(workoutEntry.callerId, callback);

        DataMatrix.putParcelable(WorkoutKeys.ACTION_LOAD_WORKOUT_ENTRY_PARCELABLE, workoutEntry);
    }

    private void createCallback(UUID callingId, IWorkoutManager callback) {
        if (callback == null) return;

        // Create a DM callback so we get the value when it comes from the workout controller
        DataMatrix dataMatrix = new DataMatrix();

        dataMatrix.createParcelableAdapter(WorkoutKeys.EVENT_LOADED_WORKOUT_INPUTS_PARCELABLE, StringInputList.class, inputsParcel -> {
            // Responses to other load requests (or a failed load, which echoes the previously loaded
            // entry's id) are not ours. Keep listening for our own instead of tearing the adapter
            // down, otherwise this caller's callback is silently dropped and never fires.
            if (!callingId.equals(inputsParcel.id)) return;

            List<String> inputs = inputsParcel.initialInputs;

            // We always want to set weight before the workout so if it's not here it should be added
            if (inputs != null && !inputs.contains(ConsoleWorkoutVariables.WEIGHT)) {
                inputs.add(0, ConsoleWorkoutVariables.WEIGHT);
            }
            callback.onWorkoutLoaded(getWorkoutInputs(inputs));
            dataMatrix.finish();
        }, false);
    }

    /**
     * Takes a list of string inputs and converts them to workout inputs that can be used for sliders
     * or pillbox value selectors. Will not send back parameters that should not be used in sliders.
     *
     * @param inputs The inputs for the workout
     * @return The list of workout values
     */
    private List<WorkoutInput> getWorkoutInputs(List<String> inputs) {
        if (inputs == null) {
            Log.e(TAG, "Inputs were null, should not happen!");
            return null;
        }

        List<WorkoutInput> workoutInputs = new ArrayList<>();
        for (String input : inputs) {
            MinMaxParam unit = WorkoutInputManager.getUnitType(input);
            if (unit != null) {
                MinMaxDictionary minMaxDictionary = MinMaxDictionary.getMinMaxDictionary(unit);
                WorkoutInput workoutInput = new WorkoutInput(
                        minMaxDictionary.min,
                        minMaxDictionary.max,
                        minMaxDictionary.defaultValue,
                        minMaxDictionary.stepSize,
                        minMaxDictionary.precision,
                        minMaxDictionary.captionText,
                        minMaxDictionary.unitString,
                        minMaxDictionary.measurement,
                        input);

                // If the unit is weight don't show the progress bar if used in pillbox
                if (input.equals(ConsoleWorkoutVariables.WEIGHT)) {
                    workoutInput.showProgress(false);
                }

                workoutInputs.add(workoutInput);
            }
        }

        return workoutInputs;
    }

    /**
     * Launches a loaded workout if it's possible to do so at this time. Uses the current value from
     * each {@link WorkoutInput} object passed in for the initial condition.
     *
     * @param initialConditions The initial conditions to pass to the workout controller
     * @param isSwitchWorkout   Whether we are switching to a workout or starting a new one
     * @return Whether the workout was able to be started
     */
    public boolean startLoadedWorkout(List<WorkoutInput> initialConditions, boolean isSwitchWorkout) {
        return startLoadedWorkout(initialConditions, isSwitchWorkout, new WorkoutSettingOverrides());
    }
    public boolean startLoadedWorkout(List<WorkoutInput> initialConditions, boolean isSwitchWorkout, WorkoutSettingOverrides settingOverrides) {
        return WorkoutLauncher.launchWorkout(convertToInitialCondition(initialConditions), isSwitchWorkout, settingOverrides);
    }

    private List<InitialCondition> convertToInitialCondition(List<WorkoutInput> workoutInputs) {
        List<InitialCondition> initialConditions = new ArrayList<>();
        if (workoutInputs != null) {
            for (WorkoutInput input : workoutInputs) {
                initialConditions.add(new InitialCondition(input.identifier(), (float) input.valueAsDouble()));
            }
        }
        return initialConditions;
    }

    /**
     * Gets a list of VA workouts. This can be used to display VA information.
     *
     * @return A list of the workouts that are available on the system
     */
    public List<WorkoutItem> getVaWorkouts(Context context) {
        List<WorkoutItem> vaWorkoutItems = new ArrayList<>();
        VAWorkoutList vaWorkoutList = DataMatrix.getParcelable(VaKeys.DATA_VA_ITEMS_PARCELABLE, VAWorkoutList.class);
        if (vaWorkoutList != null) {
            for (Workout vaWorkout : vaWorkoutList.vaWorkouts) {
                if(context != null) {
                    List<Uri> array = new ArrayList<>();
                    array.add(DrawableUtil.INSTANCE.getUri(context, getID(context, "still" + vaWorkout.getUniqueId().toLowerCase(), "drawable")));
                    WorkoutItem workoutItem = new WorkoutItem(
                            WorkoutIds.VIRTUAL_ACTIVE,
                            AppIds.VA_UUID,
                            getID(context, vaWorkout.getNameResourceString(), "string"),
                            getID(context, vaWorkout.getDescriptionResourceString(), "string"),
                            null,
                            array, // backgrounds array
                            array.get(0), // thumbnail (typically first background in list if no dedicated thumbnail)
                            new WorkoutBadge(R.drawable.ic_badge_va, 0),
                            vaWorkout.getUniqueId(), // optionalId
                            false,
                            null
                    );
                    vaWorkoutItems.add(workoutItem);
                }
            }
        }
        return vaWorkoutItems;
    }

    //Needs review if there is a more performant way to get the VA content
    private int getID(Context context, String name, String type){
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }


}
