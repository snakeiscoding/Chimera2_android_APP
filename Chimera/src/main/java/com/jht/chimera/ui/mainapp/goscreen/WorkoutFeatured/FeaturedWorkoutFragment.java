package com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured;


import static com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured.FeaturedItem.DISPLAY_MAIN_FEATURE_TYPE;
import static com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured.FeaturedItem.WORKOUT_LANDMARKS;
import static com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured.FeaturedItem.WORKOUT_VA;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.chimera.service.ui.panels.DigitalSafetyPinDialog;
import com.jht.chimera.ui.mainapp.GoScreenActivity;
import com.jht.chimera.ui.mainapp.goscreen.FeaturePluginSetupFragment;
import com.jht.chimera.ui.mainapp.goscreen.GoScreenBaseFragment;
import com.jht.chimera.ui.mainapp.goscreen.GoScreenModule;
import com.jht.chimera.ui.mainapp.goscreen.GoScreenWorkoutDisplayItem;
import com.jht.chimera.ui.mainapp.goscreen.Landmarks.LandmarksSelectionFragment;
import com.jht.chimera.ui.mainapp.goscreen.VA.VASelectionFragment;
import com.jht.chimera.ui.mainapp.goscreen.VA.VASetupFragment;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutSetup.WorkoutSetupFragment;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by AlexN on 12/29/2018.
 */

public class FeaturedWorkoutFragment extends GoScreenBaseFragment {

    public final String TAG = "FeaturedWorkoutFragment";

    private static final String IS_CHANGE_WORKOUT_KEY = "is_change_workout";
    private RecyclerView featuredSubItemsRecyclerView;

    private FeaturedItemRecyclerViewAdapter featuredItemRecyclerViewAdapter;
    private final List<FeaturedItem> carouselFeaturedItems = new ArrayList<>();
    private final List<FeaturedItem> featuredItems = new ArrayList<>();

    private final int numColumns = 6;

    private final WorkoutSetupFragment workoutSetupFragment = new WorkoutSetupFragment();
    private boolean isChangeWorkout = false;
    private boolean hasCarouselItems = true;

    private DigitalSafetyPinDialog safetyPrompt;

    public FeaturedWorkoutFragment(boolean isChangeWorkout) {
        this.isChangeWorkout = isChangeWorkout;
    }

    public FeaturedWorkoutFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.featured_workout_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(savedInstanceState != null) {
            isChangeWorkout = savedInstanceState.getBoolean(IS_CHANGE_WORKOUT_KEY, isChangeWorkout);
        }

        featuredSubItemsRecyclerView = view.findViewById(R.id.featured_items_recycler_view);
        featuredSubItemsRecyclerView.setHasFixedSize(true);
        FeaturedContentItemDecoration decoration = new FeaturedContentItemDecoration(numColumns, 20, hasCarouselItems);
        featuredSubItemsRecyclerView.addItemDecoration(decoration);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), numColumns);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (featuredItemRecyclerViewAdapter.getItemViewType(position)) {
                    case FeaturedItem.DISPLAY_SUB_TITLE_TYPE:
                    case FeaturedItem.DISPLAY_MAIN_FEATURE_TYPE:
                        return numColumns;
                    case FeaturedItem.DISPLAY_GRID_TYPE:
                    default:
                        return 1;
                }
            }
        });
        removeEmptyItems();
        findItemRowStart();
        check3rdPartyFeaturedItems();
        updateFeaturedItemsState();

        featuredSubItemsRecyclerView.setLayoutManager(gridLayoutManager);
        featuredItemRecyclerViewAdapter = new FeaturedItemRecyclerViewAdapter(featuredItems,
                position -> {
                    FeaturedItem selectedItem = featuredItems.get(position);
                    onWorkoutSelected(selectedItem);
                },
                this::onWorkoutSelected,
                getChildFragmentManager(), carouselFeaturedItems);

        //Need getWidth as non-zero value after drawing, so observe layout binding
        final ViewTreeObserver observer = featuredSubItemsRecyclerView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                featuredSubItemsRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                featuredItemRecyclerViewAdapter.setWidth(featuredSubItemsRecyclerView.getWidth());
                featuredSubItemsRecyclerView.setAdapter(featuredItemRecyclerViewAdapter);
            }
        });

        if (getActivity() instanceof GoScreenActivity) {
            DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.GO_SCREEN.name());
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // onPause does not call onViewRecycled. The thumbnails use are loaded asynchronously so this is
        // a hack to trigger the onViewRecycled. Note that this will not save the current state of the recycler
        // view so going back into the fragment will refresh the recycler view. Refactor this logic if we
        // find a better way to cancel the thumbnails async tasks.

        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), numColumns);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (featuredItemRecyclerViewAdapter.getItemViewType(position)) {
                    case FeaturedItem.DISPLAY_SUB_TITLE_TYPE:
                    case FeaturedItem.DISPLAY_MAIN_FEATURE_TYPE:
                        return numColumns;
                    case FeaturedItem.DISPLAY_GRID_TYPE:
                    default:
                        return 1;
                }
            }
        });
        featuredSubItemsRecyclerView.setLayoutManager(gridLayoutManager);
    }

    /** Swaps a setup screen onto the activity content view */
    private void showSetupFragment(androidx.fragment.app.Fragment setupFragment) {
        FragmentActivity activity = getActivity();
        if (activity == null) return;
        try {
            activity.getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                    .replace(android.R.id.content, setupFragment).commit();
        } catch (IllegalStateException e) {
            //reset go screen when this occurs
            LaunchAppUtil.launchGoScreen();
            Log.e(TAG, "Failed to launch Featured Workouts", e);
        }
    }

    private void onWorkoutSelected(FeaturedItem selectedItem) {
        if (workoutEnabledStates != null) {
            String workoutId = "" + selectedItem.workoutId;
            Integer workoutState = workoutEnabledStates.get(workoutId);
            if (workoutState != null && workoutState == 1 && goScreenInterface != null) {
                goScreenInterface.onDisabledWorkoutSelected(workoutId);
                return;
            }
        }
        if (selectedItem.setupApp != null) {
            // LEAVING A COMMENT SO IT IS EASY TO FIND NEXT TIME
            // THIS IS WHERE THIRD PARTY APPS LIKE IFIT/YMCA/FUTURE APPS LAUNCH FROM CURRENTLY
            checkIfSafetyPinNeeded(()-> selectedItem.setupApp.launchSetupApp(featuredSubItemsRecyclerView));
        } else if (selectedItem.optionalFragment instanceof FeaturePluginSetupFragment) {
            // Compose plugin setups draw their own modal and size themselves against the window, so
            // they take the content view directly instead of WorkoutSetupFragment's inset card.
            // They are also single use, a removed instance does not re-add cleanly.
            FeaturePluginSetupFragment pluginSetup =
                    ((FeaturePluginSetupFragment) selectedItem.optionalFragment).freshCopy();
            selectedItem.optionalFragment = pluginSetup;
            showSetupFragment(pluginSetup);
        } else {
            if (selectedItem.optionalFragment instanceof FeaturedSetupFragment) {
                ((FeaturedSetupFragment) selectedItem.optionalFragment).loadFeaturedWorkoutInfo(selectedItem);
            } else if (selectedItem.optionalFragment instanceof VASetupFragment) {
                ((VASetupFragment) selectedItem.optionalFragment).loadVAWorkoutInfo(selectedItem.workoutItem);
            }
            workoutSetupFragment.loadNewSetupFragment(selectedItem.optionalFragment);
            showSetupFragment(workoutSetupFragment);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setFeaturedItems(List<FeaturedItem> items) {
        featuredItems.clear();
        featuredItems.addAll(items);
        if(!carouselFeaturedItems.isEmpty()){
            featuredItems.add(0, carouselFeaturedItems.get(0));
        }
        findItemRowStart();
        if (featuredItemRecyclerViewAdapter != null) {
            check3rdPartyFeaturedItems();
            featuredItemRecyclerViewAdapter.notifyDataSetChanged();
        }

        workoutEnabledStates = null;
    }

    private void findItemRowStart() {
        //Determine the grid elements at the start of rows
        int count = 0;
        for (int i = 0; i < featuredItems.size(); i++) {
            featuredItems.get(i).startOfRow = false;
            switch (featuredItems.get(i).displayType) {
                case FeaturedItem.DISPLAY_GRID_TYPE:
                    //Offset by 1 due to the main item at index 0
                    if ((count) % 6 == 0 && i != 1) {
                        featuredItems.get(i).startOfRow = true;
                    }
                    count++;
                    break;
                case FeaturedItem.DISPLAY_SUB_TITLE_TYPE:
                    count = 0;
            }
        }
    }

    private void removeEmptyItems() {
        Iterator<FeaturedItem> featuredItemIterator = featuredItems.iterator();
        while (featuredItemIterator.hasNext()) {
            FeaturedItem featuredItem = featuredItemIterator.next();
            switch (featuredItem.workoutType) {
                case WORKOUT_VA:
                    if (!((VASelectionFragment) featuredItem.optionalFragment).hasVaItems()) {
                        featuredItemIterator.remove();
                    }
                    break;
                case WORKOUT_LANDMARKS:
                    if (!((LandmarksSelectionFragment) featuredItem.optionalFragment).hasLandmarksItems()) {
                        featuredItemIterator.remove();
                    }
                    break;
            }
        }
    }

    /**
     * Converts VA into individual workouts for featured content
     */
    private void check3rdPartyFeaturedItems() {
        int vaIndex = getVaIndex();
        // Check that VA exists
        if (vaIndex != -1) {
            // Get the VA content
            FeaturedItem vaItem = featuredItems.get(vaIndex);
            VASelectionFragment vaSelectionFragment = ((VASelectionFragment) vaItem.optionalFragment);
            vaSelectionFragment.getVaSetupFragment().hideBackButton();

            // Remove VA folder, we just want to display the expanded contents
            featuredItems.remove(vaIndex);

            List<GoScreenWorkoutDisplayItem> vaWorkouts = vaSelectionFragment.getVaItems();

            for (int i = 0; i < vaWorkouts.size(); i++) {
                GoScreenWorkoutDisplayItem vaWorkout = vaWorkouts.get(i);
                // Check if this is actually a workout, then add to the featured items
                if (vaWorkout.categoryType == GoScreenWorkoutDisplayItem.WORKOUT_TYPE) {
                    FeaturedItem vaWorkoutItem = new FeaturedItem(WorkoutIds.VIRTUAL_ACTIVE, vaWorkout.title, vaWorkout.subTitle, "",
                            vaWorkout.backgroundPath, null, null, null, vaWorkout.identifier, vaWorkout.description);
                    vaWorkoutItem.optionalFragment = vaSelectionFragment.getVaSetupFragment();
                    vaWorkoutItem.workoutItem = vaWorkout;
                    featuredItems.add(vaWorkoutItem);
                }
            }
        }

        findItemRowStart();
    }

    /**
     * Gets the index of VA in the list of featured content
     *
     * @return The index for VA
     */
    private int getVaIndex() {
        //start at index 1 instead of 0, because 0 is always the carousel, we want the featured content folder index
        for (int i = 1; i < featuredItems.size(); i++) {
            FeaturedItem item = featuredItems.get(i);
            if (item.workoutType == WORKOUT_VA) {
                return i;
            }
        }

        return -1;
    }

    public void setMainFeaturedItems(List<FeaturedItem> mainFeaturedItems) {
        if (!mainFeaturedItems.isEmpty()) {
            carouselFeaturedItems.clear();
            carouselFeaturedItems.addAll(mainFeaturedItems);
        }
        for(int i = 0; i< carouselFeaturedItems.size(); i++){
            carouselFeaturedItems.get(i).displayType = DISPLAY_MAIN_FEATURE_TYPE;
        }

    }

    private Map<String, Integer> workoutEnabledStates = null;
    private GoScreenModule.IGoScreenInterface goScreenInterface = null;

    public void setWorkoutEnabledStates(Map<String, Integer> workoutEnabledStates, GoScreenModule.IGoScreenInterface goScreenInterface) {
        this.workoutEnabledStates = workoutEnabledStates;
        this.goScreenInterface = goScreenInterface;
        updateFeaturedItemsState();
    }

    private void updateFeaturedItemsState() {
        if (workoutEnabledStates != null) {
            for (FeaturedItem item : featuredItems) {
                Integer enabledState = workoutEnabledStates.get(item.workoutId + "");
                item.enabled = enabledState == null || enabledState == 0;
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle instanceState) {
        instanceState.putBoolean(IS_CHANGE_WORKOUT_KEY, isChangeWorkout);
    }

    // safety pin stuff!
    private void checkIfSafetyPinNeeded(DigitalSafetyPinDialog.Action action) {
        if (DataMatrix.getBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL)) {
            if (safetyPrompt == null) safetyPrompt = new DigitalSafetyPinDialog(requireActivity().getApplicationContext());
            safetyPrompt.checkIfNeeded(action);
        } else {
            action.doAction();
        }
    }

    public void setHasCarouselItems(boolean hasCarouselItems){
        this.hasCarouselItems = hasCarouselItems;
    }
}
