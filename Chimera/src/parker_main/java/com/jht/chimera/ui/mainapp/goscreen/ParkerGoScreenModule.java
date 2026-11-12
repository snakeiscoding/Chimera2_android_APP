package com.jht.chimera.ui.mainapp.goscreen;

import static com.jht.chimera.ui.mainapp.ParkerGoScreenActivity.WorkoutLabelProvider;

import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.Guideline;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom.CustomWorkoutItem;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured.FeaturedSetupFragment;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.List;

public class ParkerGoScreenModule {

    private CategoryRecyclerViewAdapter categoryRecyclerViewAdapter;
    private List<CategoryItem> categoryItems;
    private ParkerWorkoutSelectionFragment workoutSelectionFragment;
    private FeaturedSetupFragment featuredSetupFragment;
    private FeaturedFragment featuredFragment;
    private CalendarFragment calendarFragment;
    private AppsFragment appsFragment;
    private RecyclerView categoryRecycler;

    private GoScreenBaseFragment currentFragment;

    private ImageView quickstartButton;
    private TextView quickstartText;
    private Guideline guidelineRecycler;

    private IGoScreenInterface iGoScreenInterface;
    private int currentPosition = -1;
    private long pressTime = 0;

    public interface IGoScreenInterface {
        void onWorkoutSelected(String workoutIdentifier);

        void onWorkoutStart(String workoutIdentifier, List<WorkoutParameter> parameters);

        void onQuickStart();

        void saveCustomWorkouts(List<CustomWorkoutItem> customWorkoutXmls);

        void onCustomWorkoutStart(CustomWorkoutItem customWorkout);

        void onDisabledWorkoutSelected(String workoutID);

        void setCurrentFragment(GoScreenBaseFragment fragment);

        void selectCategory(int index);

        void selectCustomWorkout(String uuid);

        void launchThirdPartyInterface(String appUuid);
    }

    public ParkerGoScreenModule(final FragmentActivity activity, int defaultPosition, IGoScreenInterface goScreenInterface) {

        iGoScreenInterface = goScreenInterface;
        categoryRecycler = activity.findViewById(R.id.category_recycler);
        guidelineRecycler = activity.findViewById(R.id.category_guide);

        workoutSelectionFragment = new ParkerWorkoutSelectionFragment();
        workoutSelectionFragment.setGoScreenInterface(iGoScreenInterface);
        workoutSelectionFragment.setWorkoutSelectionInterface(new ParkerWorkoutSelectionFragment.IWorkoutSelectionInterface() {
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

        featuredFragment = new FeaturedFragment();
        featuredFragment.initialize(iGoScreenInterface);
        calendarFragment = new CalendarFragment();
        appsFragment = new AppsFragment(iGoScreenInterface);

        categoryItems = new ArrayList<>();

        boolean isGuest = DataMatrix.getInt(SettingKeys.Application.DATA_USER_PROFILE_SELECTED_SLOT_INT) == DAPIKeys.Constants.DAPI_GUEST_INDEX;

        //First Item loaded as default
        categoryItems.add(new CategoryItem(
                activity.getString(R.string.featured_uc),
                activity.getString(R.string.featured_exercises_tc),
                R.drawable.ic_star, featuredFragment));
        categoryItems.add(new CategoryItem(
                activity.getString(R.string.training_uc),
                activity.getString(R.string.training_workouts_tc),
                R.drawable.ic_running_man, workoutSelectionFragment));
        if (!isGuest) categoryItems.add(new CategoryItem(
                activity.getString(R.string.recent_uc),
                activity.getString(R.string.recent_workouts_tc),
                R.drawable.ic_setup_calendar, calendarFragment));
        categoryItems.add(new CategoryItem(
                activity.getString(R.string.apps_uc),
                activity.getString(R.string.entertainment_apps_tc),
                R.drawable.ic_setup_apps, appsFragment));

        categoryRecycler.setHasFixedSize(true);
        categoryRecycler.setLayoutManager(new LinearLayoutManager(activity));
        categoryRecyclerViewAdapter = new CategoryRecyclerViewAdapter(activity, categoryItems, position -> {
            if (currentPosition != position) {
                currentPosition = position;
                CategoryItem item = categoryItems.get(position);
                activity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                        .addToBackStack(null)
                        .replace(R.id.fragment_container, item.fragment,
                                item.buttonTitle).commit();
                activity.setTitle(item.pageTitle);
                activity.getSupportFragmentManager().executePendingTransactions();
            }
        });
        categoryRecyclerViewAdapter.setShowNames(true);
        categoryRecycler.setAdapter(categoryRecyclerViewAdapter);
        quickstartText = activity.findViewById(R.id.quick_start_text);
        quickstartButton = activity.findViewById(R.id.quick_start_button);
        quickstartButton.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        quickstartButton.setOnClickListener(v -> {

            /* TODO is this needed???
            if (!isGuest) {
                iGoScreenInterface.saveCustomWorkouts(customWorkoutFragment.getWorkoutItems());
            }
            */

            long uptime = SystemClock.uptimeMillis();
            if (pressTime < uptime - 1500) {
                pressTime = SystemClock.uptimeMillis();
                iGoScreenInterface.onQuickStart();
            }
        });

        //Set first item as default
        CategoryItem item = categoryItems.get(defaultPosition);
        activity.getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                .replace(R.id.fragment_container, item.fragment, item.buttonTitle).commit();
    }

    public void setWorkoutItems(List<GoScreenWorkoutDisplayItem> workoutItems) {
        workoutSelectionFragment.setWorkoutItems(workoutItems);
    }

    public void setVAWorkoutItems(List<GoScreenWorkoutDisplayItem> vaWorkoutItems) {
        featuredFragment.setVAWorkoutItems(vaWorkoutItems);
    }

    public void setLandmarksWorkoutItems(List<GoScreenWorkoutDisplayItem> landmarksWorkoutItems) {
        featuredFragment.setLandmarksWorkoutItems(landmarksWorkoutItems);
    }

    public void setCustomWorkoutItems(Context context, List<WorkoutParameter> customWorkoutParameters, List<CustomWorkoutItem> customWorkoutItems) {
        workoutSelectionFragment.setCustomWorkoutItems(context, customWorkoutParameters, customWorkoutItems);
    }
    public void setWorkoutLabelProvider(WorkoutLabelProvider workoutLabelProvider) {
        featuredFragment.setWorkoutLabelProvider(workoutLabelProvider);
        calendarFragment.setWorkoutLabelProvider(workoutLabelProvider);
    }

    public void setCustomWorkoutIsGuest(boolean isGuest) {
        workoutSelectionFragment.setCustomWorkoutIsGuest(isGuest);
    }

    public void setCustomWorkoutIsOfflineUser(boolean isOfflineUser) {
        workoutSelectionFragment.setCustomWorkoutIsOfflineUser(isOfflineUser);
    }


    public void setCurrentFragment(GoScreenBaseFragment fragment) {
        currentFragment = fragment;
    }

    public void setWorkoutParameters(List<WorkoutParameter> workoutParameters) {
        if (currentFragment != null) {
            currentFragment.setParameterValues(workoutParameters);
        }
    }

    public void selectCategory(int index) {
        categoryRecycler.findViewHolderForAdapterPosition(index).itemView.findViewById(R.id.category_background).performClick();
    }
    public void selectCustomWorkout(String uuid) {
        for (int i = 0; i < categoryItems.size(); ++i) {
            if (categoryItems.get(i).fragment == workoutSelectionFragment) {
                selectCategory(i);
                workoutSelectionFragment.selectCustomWorkout(uuid);
                return;
            }
        }
    }

    public void showQuickStartButton(boolean show) {
        quickstartText.setVisibility(show ? View.VISIBLE : View.GONE);
        quickstartButton.setVisibility(show ? View.VISIBLE : View.GONE);
        guidelineRecycler.setGuidelinePercent(show ? .1234f : 0f);
    }

    public void hasCarouselItems(boolean hasItems) {
        // added to chimera go module to handle hiding carousel in featured workout fragment during change workout, which operates differently from retail
    }

}
