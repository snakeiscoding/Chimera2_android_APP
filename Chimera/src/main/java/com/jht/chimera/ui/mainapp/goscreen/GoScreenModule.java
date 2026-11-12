package com.jht.chimera.ui.mainapp.goscreen;

import static com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured.FeaturedItem.WORKOUT_CUSTOM_HIIT;
import static com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured.FeaturedItem.WORKOUT_IFIT;
import static com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured.FeaturedItem.WORKOUT_LANDMARKS;
import static com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured.FeaturedItem.WORKOUT_SPRINT8;
import static com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured.FeaturedItem.WORKOUT_TARGET_TRAINING;
import static com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured.FeaturedItem.WORKOUT_VA;

import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;
import com.jht.chimera.ui.mainapp.goscreen.Landmarks.LandmarksSelectionFragment;
import com.jht.chimera.ui.mainapp.goscreen.Landmarks.LandmarksSetupFragment;
import com.jht.chimera.ui.mainapp.goscreen.VA.VASelectionFragment;
import com.jht.chimera.ui.mainapp.goscreen.VA.VASetupFragment;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom.CustomWorkoutFragment;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom.CustomWorkoutItem;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured.FeaturedItem;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured.FeaturedSetupFragment;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured.FeaturedWorkoutFragment;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutSetup.WorkoutSelectionFragment;
import com.jht.workoutcontrollerlib.model.WorkoutModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by AlexN on 1/8/2019.
 */

public class GoScreenModule {
    private static final String TAG = "GoScreenModule";
    private final CategoryRecyclerViewAdapter categoryRecyclerViewAdapter;
    private final List<CategoryItem> categoryItems;

    private final FeaturedWorkoutFragment featuredWorkoutFragment;

    private final WorkoutSelectionFragment workoutSelectionFragment;
    private final VASelectionFragment vaSelectionFragment;
    private final VASetupFragment vaSetupFragment;
    private final LandmarksSelectionFragment landmarksSelectionFragment;
    private final CustomWorkoutFragment customWorkoutFragment;
    private final FeaturedSetupFragment featuredSetupFragment;
    private boolean isGuest;

    private GoScreenBaseFragment currentFragment;

    private final ImageView quickstartButton;
    private final TextView quickstartText;

    private final IGoScreenInterface iGoScreenInterface;
    private int currentPosition = -1;
    private long pressTime = 0;

    public interface IGoScreenInterface {
        void onWorkoutSelected(String workoutIdentifier);
        void onWorkoutStart(String workoutIdentifier, List<WorkoutParameter> parameters);
        void onQuickStart();
        void saveCustomWorkouts(List<CustomWorkoutItem> customWorkoutXmls);
        void onCustomWorkoutStart(CustomWorkoutItem customWorkout);
        void onDisabledWorkoutSelected(String workoutID);
        boolean isChangeWorkout();
        void getWorkoutInfo();
    }

    public GoScreenModule(final FragmentActivity activity, IGoScreenInterface goScreenInterface) {

        iGoScreenInterface = goScreenInterface;
        RecyclerView categoryRecycler = activity.findViewById(R.id.category_recycler);
        featuredWorkoutFragment = new FeaturedWorkoutFragment(goScreenInterface.isChangeWorkout());
        customWorkoutFragment = new CustomWorkoutFragment();
        customWorkoutFragment.setCustomWorkoutInterface(new CustomWorkoutFragment.ICustomWorkoutInterface() {
            @Override
            public void saveWorkouts(List<CustomWorkoutItem> customWorkoutXmls) {
                iGoScreenInterface.saveCustomWorkouts(customWorkoutXmls);
            }

            @Override
            public void onStartCustomWorkout(CustomWorkoutItem customWorkout) {
                iGoScreenInterface.onCustomWorkoutStart(customWorkout);
            }

            // chimera doesn't use back button currently
            @Override
            public void onBackButtonShow(boolean isShown) {
            }
            @Override
            public void onBackButtonClicked() {
            }
            @Override
            public void onUnableToSaveShow(boolean isShown) {
            }
        });

        workoutSelectionFragment = new WorkoutSelectionFragment();
        workoutSelectionFragment.setWorkoutSelectionInterface(new WorkoutSelectionFragment.IWorkoutSelectionInterface() {
            @Override
            public void workoutSelected(String workoutIdentifier) {
                currentFragment = workoutSelectionFragment;
                iGoScreenInterface.onWorkoutSelected(workoutIdentifier);
            }

            @Override
            public void onStartWorkout(String workoutIdentifier, List<WorkoutParameter> parameters) {
                iGoScreenInterface.onWorkoutStart(workoutIdentifier, parameters);
            }

            @Override
            public void onDisabledWorkoutSelected(String workoutID) {
                iGoScreenInterface.onDisabledWorkoutSelected(workoutID);
            }

            @Override
            public void getWorkoutInfo() {
                iGoScreenInterface.getWorkoutInfo();
            }
        });

        vaSetupFragment = new VASetupFragment();
        vaSetupFragment.setIVASetupFragment(new VASetupFragment.IVASetupFragment() {
            @Override
            public void onVAWorkoutStart(String workoutIdentifier, List<WorkoutParameter> parameters) {
                currentFragment = vaSetupFragment;
                iGoScreenInterface.onWorkoutStart(workoutIdentifier, parameters);
            }

            @Override
            public void onVAGetParameters(String workoutIdentifier) {
                currentFragment = vaSetupFragment;
                iGoScreenInterface.onWorkoutSelected(workoutIdentifier);
            }
        });

        vaSelectionFragment = new VASelectionFragment();
        vaSelectionFragment.setVaSetupFragment(vaSetupFragment);
        vaSelectionFragment.setIVASelectionInterface(vaIdentifier -> {
            currentFragment = vaSelectionFragment;
            iGoScreenInterface.onWorkoutSelected(vaIdentifier);
        });

        LandmarksSetupFragment landmarksSetupFragment = new LandmarksSetupFragment();
        landmarksSetupFragment.setILandmarksSetupFragment(iGoScreenInterface::onWorkoutStart);
        landmarksSelectionFragment = new LandmarksSelectionFragment();
        landmarksSelectionFragment.setLandmarksSetupFragment(landmarksSetupFragment);
        landmarksSelectionFragment.setILandmarksSelectionInterface(landmarksIdentifier -> {
            currentFragment = landmarksSelectionFragment;
            iGoScreenInterface.onWorkoutSelected(landmarksIdentifier);
        });

        featuredSetupFragment = new FeaturedSetupFragment();
        featuredSetupFragment.setiFeaturedSetupFragment(new FeaturedSetupFragment.IFeaturedSetupFragment() {
            @Override
            public void onFeaturedWorkoutStart(String workoutIdentifier, List<WorkoutParameter> parameters) {
                iGoScreenInterface.onWorkoutStart(workoutIdentifier, parameters);
            }

            @Override
            public void onFeaturedWorkoutGetParameters(String workoutIdentifier) {
                currentFragment = featuredSetupFragment;
                iGoScreenInterface.onWorkoutSelected(workoutIdentifier);
            }
        });

        categoryItems = new ArrayList<>();
        //First Item loaded as default
        categoryItems.add(new CategoryItem(activity.getString(R.string.featured_exercises_uc), R.drawable.ic_star, featuredWorkoutFragment));
        categoryItems.add(new CategoryItem(activity.getString(R.string.training_exercises_uc), R.drawable.ic_running_man, workoutSelectionFragment));
        categoryItems.add(new CategoryItem(activity.getString(R.string.custom_workouts_uc), R.drawable.ic_custom_workout, customWorkoutFragment));

        categoryRecycler.setHasFixedSize(true);
        categoryRecycler.setLayoutManager(new LinearLayoutManager(activity));
        categoryRecyclerViewAdapter = new CategoryRecyclerViewAdapter(activity, categoryItems, position -> {
            if (currentPosition != position) {
                currentPosition = position;
                activity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                        .replace(R.id.fragment_container, categoryItems.get(position).fragment,
                                categoryItems.get(position).buttonTitle).commit();
            }
        });
        categoryRecycler.setAdapter(categoryRecyclerViewAdapter);
        quickstartText = activity.findViewById(R.id.quick_start_text);
        quickstartButton = activity.findViewById(R.id.quick_start_button);
        quickstartButton.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        quickstartButton.setOnClickListener(v -> {
            if(!isGuest) {
                iGoScreenInterface.saveCustomWorkouts(customWorkoutFragment.getWorkoutItems());
            }
            long uptime = SystemClock.uptimeMillis();
            if (pressTime < uptime - 1500) {
                pressTime = SystemClock.uptimeMillis();
                iGoScreenInterface.onQuickStart();
            }
        });


        //Set first item as default
        activity.getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                .replace(R.id.fragment_container, categoryItems.get(0).fragment,
                        categoryItems.get(0).buttonTitle).commit();
    }

    public void addCategory(CategoryItem categoryItem) {
        categoryItems.add(categoryItem);
        categoryRecyclerViewAdapter.notifyDataSetChanged();
    }

    public void setMainFeaturedItems(List<FeaturedItem> mainFeaturedItems) {
        checkFeaturedItems(mainFeaturedItems);
        featuredWorkoutFragment.setMainFeaturedItems(mainFeaturedItems);
    }

    public void setFeaturedItems(List<FeaturedItem> featuredItems) {
        checkFeaturedItems(featuredItems);
        featuredWorkoutFragment.setFeaturedItems(featuredItems);
    }

    public void setWorkoutItems(List<GoScreenWorkoutDisplayItem> workoutItems) {
        workoutSelectionFragment.setWorkoutItems(workoutItems);
    }

    public void setVAWorkoutItems(List<GoScreenWorkoutDisplayItem> vaWorkoutItems) {
        vaSelectionFragment.setVaWorkoutItems(vaWorkoutItems);
    }

    public void setLandmarksWorkoutItems(List<GoScreenWorkoutDisplayItem> landmarksWorkoutItems) {
        landmarksSelectionFragment.setLandmarksWorkoutItems(landmarksWorkoutItems);
    }

    public void setCustomWorkoutItems(Context context, List<WorkoutParameter> customWorkoutParameters, List<CustomWorkoutItem> customWorkoutItems) {
        customWorkoutFragment.setCustomWorkoutItems(context, customWorkoutParameters, customWorkoutItems);
    }

    public void setCustomWorkoutLoading(Context context) {
        customWorkoutFragment.setCustomWorkoutLoading(context);
    }

    public void setCustomWorkoutIsGuest(boolean isGuest) {
        customWorkoutFragment.updateGuestUserLayout(isGuest);
        this.isGuest = isGuest;
    }

    public void setCustomWorkoutHasNetwork(boolean hasInternet) {
        customWorkoutFragment.setNetworkConnectedStatus(hasInternet);
    }

    public void setWorkoutParameters(List<WorkoutParameter> workoutParameters) {
        if (currentFragment != null) {
            currentFragment.setParameterValues(workoutParameters);
        }
    }

    public void showQuickStartButton(boolean show) {
        quickstartText.setVisibility(show ? View.VISIBLE : View.GONE);
        quickstartButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void hasCarouselItems(boolean hasItems) {
        featuredWorkoutFragment.setHasCarouselItems(hasItems);
    }

    public void setWorkoutModelInfo(WorkoutModel workoutModel) {
        workoutSelectionFragment.setWorkoutModel(workoutModel);
    }

    public void applyTiersMap(Map<String, Integer> workoutEnabledStates) {
        if(!workoutEnabledStates.isEmpty()) {
            workoutSelectionFragment.setWorkoutEnabledStates(workoutEnabledStates);
            featuredWorkoutFragment.setWorkoutEnabledStates(workoutEnabledStates, iGoScreenInterface);
        }
    }

    public int getCurrentIndex() {
        return currentPosition;
    }

    private void checkFeaturedItems(List<FeaturedItem> featuredItems) {
        for (FeaturedItem featuredItem : featuredItems) {
            switch (featuredItem.workoutType) {
                case WORKOUT_VA:
                    featuredItem.optionalFragment = vaSelectionFragment;
                    break;
                case WORKOUT_SPRINT8:
                    featuredItem.optionalFragment = FeaturePluginSetupFragment.forSprint8();
                    break;
                case WORKOUT_CUSTOM_HIIT:
                    featuredItem.optionalFragment = FeaturePluginSetupFragment.forHiit();
                    break;
                case WORKOUT_LANDMARKS:
                    featuredItem.optionalFragment = landmarksSelectionFragment;
                    break;
                case WORKOUT_IFIT:
                    featuredItem.optionalFragment = null;
                    break;

            }
            if (featuredItem.optionalFragment == null) {
                featuredItem.optionalFragment = featuredSetupFragment;
            }
        }
    }

    /**
     * Gets the fragment we are on so that we know which one to delete when updating the screen
     * @return
     */
    public Fragment getCurrentFragment(){
        return categoryItems.get(currentPosition).fragment;
    }

    /**
     * Removes old fragment from screen
     * @param activity
     */
    public void cleanGoScreenModule(FragmentActivity activity){
        if (currentPosition >= 0) {
            activity.getSupportFragmentManager().beginTransaction().remove(getCurrentFragment()).commit();
        }
    }
}
