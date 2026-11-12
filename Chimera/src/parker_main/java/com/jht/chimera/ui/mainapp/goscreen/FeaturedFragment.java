package com.jht.chimera.ui.mainapp.goscreen;

import static com.jht.chimera.lib.common.context.ContextUtil.getApplicationContext;
import static com.jht.chimera.lib.widget.CommonRoutines.getAssetUri;
import static com.jht.chimera.ui.mainapp.ParkerGoScreenActivity.WorkoutLabelProvider;
import static com.jht.chimera.ui.mainapp.goscreen.CarouselPagerAdapter.CarouselItemData;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.lib.common.datamatrixkeys.FeaturedWorkoutKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.workout.FavoriteIcon;
import com.jht.chimera.lib.workout.FavoriteWorkoutFile;
import com.jht.chimera.lib.workout.PreviousWorkout;
import com.jht.chimera.lib.workout.RecordedWorkoutDatabase;
import com.jht.chimera.lib.workout.WorkoutStartupParams;
import com.jht.chimera.lib.workout.workoutlist.workoutmap.ParkerWorkoutMap;
import com.jht.chimera.lib.workoutapps.AppIconManager;
import com.jht.chimera.lib.workoutapps.AppIds;
import com.jht.chimera.lib.workoutapps.AppInfo;
import com.jht.chimera.lib.workoutapps.AppManager;
import com.jht.chimera.service.ui.panels.DigitalSafetyPinDialog;
import com.jht.chimera.ui.mainapp.goscreen.Landmarks.LandmarksSelectionFragment;
import com.jht.chimera.ui.mainapp.goscreen.Landmarks.LandmarksSetupFragment;
import com.jht.chimera.ui.mainapp.goscreen.VA.VASelectionFragment;
import com.jht.chimera.ui.mainapp.goscreen.VA.VASetupFragment;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutSetup.WorkoutSetupFragment;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.progression.MmActivity;
import com.jhtp.dapiservice.wt.DapiProgramType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

@TargetApi(24)
public class FeaturedFragment extends Fragment {
    protected static final String TAG = "FeaturedFragment";
    private DigitalSafetyPinDialog safetyPrompt;

    ViewPager viewPager;
    CarouselPagerAdapter adapter;
    WorkoutSetupFragment workoutSetupFragment = new WorkoutSetupFragment();
    ParkerGoScreenModule.IGoScreenInterface iGoScreenInterface;

    VASelectionFragment vaSelectionFragment;
    VASetupFragment vaSetupFragment;
    LandmarksSelectionFragment landmarksSelectionFragment;
    LandmarksSetupFragment landmarksSetupFragment;
    AppInfo appInfo;
    DataMatrix mDataMatrix = new DataMatrix();

    public enum FeaturedExercise {
        VIRTUAL_ACTIVE,
        HULU,
        IFIT,
        LANDMARKS,
        SPRINT8,
        CUSTOM_WORKOUT,
        PROGRESSION_PROGRAMS;

        public String getImageViewSlug() {
            return this.name();
        }
    }

    // overriding constructor can cause fatal exception in go screen onCreate when changing dpi via command line
    public void initialize(ParkerGoScreenModule.IGoScreenInterface iGoScreenInterface) {
        this.iGoScreenInterface = iGoScreenInterface;
        initFragments();
    }

    /** Return the first non-null T in things */
    @SafeVarargs // should be safe, no type mutations, just pass through
    static <T> T coalesce(T...things) {
        for (T t : things) if (t != null) return t;
        return null;
    }

    private static boolean isIfitAllowed() {
        return DataMatrix.getBoolean(FeaturedWorkoutKeys.DATA_IFIT_APP_VISIBLE_BOOL, false);
    }

    static final ParkerWorkoutMap PARKER_WORKOUT_MAP = new ParkerWorkoutMap();
    static final Predicate<WorkoutStartupParams> WORKOUT_VALID = pw ->
            pw.programTypeId != null && PARKER_WORKOUT_MAP.getWorkout(pw.programTypeId) != null &&
            (!AppIds.IFIT_UUID.equals(pw.appUuid) || isIfitAllowed());
    static final Predicate<WorkoutStartupParams> WORKOUT_INVALID = WORKOUT_VALID.negate();

    /** Creates and returns a new stateful predicate that will only return true
     * for the first workout of any given programTypeId, appUuid, customWorkoutUuid */
    static Predicate<WorkoutStartupParams> newUniqueWorkoutPredicate() {
        final HashSet<String> workoutKeys = new HashSet<>();
        return pw -> {
            // if there is an app id that is third party interface, ignore other parameters
            AppInfo appInfo = AppManager.getInstance().getApp(pw.appUuid);
            if (appInfo != null && appInfo.useThirdPartyInterface) return workoutKeys.add(pw.appUuid);
            if (DapiUser.isGuestUser()) {
                // we don't save custom workouts for guest, so only report custom workouts generically without uuid
                return workoutKeys.add(pw.programTypeId + ","  + coalesce(pw.appUuid,""));
            } else {
                return workoutKeys.add(pw.programTypeId + ","  + coalesce(pw.appUuid,"") + ","  + coalesce(pw.customWorkoutUuid,""));
            }
        };
    }

    protected CarouselItemData getFeaturedExerciseIconUri(FeaturedExercise e) {
        switch (e) {
            case VIRTUAL_ACTIVE:
                return new CarouselItemData(getAssetUri("samples/feature_va.jpg"), null, e.getImageViewSlug());
            case LANDMARKS:
                return new CarouselItemData(getAssetUri("samples/feature_landmark.jpg"), null, e.getImageViewSlug());
            case SPRINT8:
                return new CarouselItemData(getAssetUri("samples/feature_sprint8.jpg"), null, e.getImageViewSlug());
            case HULU:
                return new CarouselItemData(Uri.parse("android.resource://com.jht.chimera/drawable/feature_hulu"), null, e.getImageViewSlug());
            case IFIT:
                return new CarouselItemData(Uri.parse("android.resource://com.jht.chimera/drawable/feature_ifit"), null, e.getImageViewSlug());
            case CUSTOM_WORKOUT:
                return new CarouselItemData(getAssetUri("samples/feature_custom.jpg"), getString(R.string.custom_workouts_uc), e.getImageViewSlug());
            case PROGRESSION_PROGRAMS:
                return new CarouselItemData(Uri.parse("android.resource://com.jht.chimera/drawable/featured_bg_matrix_method"), null, e.getImageViewSlug());
            default:
                throw new RuntimeException("Invalid FeaturedExercise "+e);
        }
    }

    private final List<FeaturedExercise> featuredExercises = new ArrayList<>();
    private final List<CarouselItemData> featuredExerciseIcons = new ArrayList<>();

    protected void setFeaturedExercises(List<FeaturedExercise> newFeaturedExercises) {
        if (newFeaturedExercises == this.featuredExercises) throw new RuntimeException("Don't call setFeaturedExercises(this.featuredExercises!");
        if (newFeaturedExercises.equals(this.featuredExercises)) {
            Log.d(TAG, "setFeaturedExercises: No change; skipping");
            return; // No change!
        }

        Log.d(TAG, "Featured exercise list has changed");
        this.featuredExercises.clear();
        this.featuredExercises.addAll(newFeaturedExercises);

        int ifitPosition = 0;
        featuredExerciseIcons.clear();
        for (FeaturedExercise e : featuredExercises) {
            if (e == FeaturedExercise.IFIT) ifitPosition = featuredExerciseIcons.size();
            featuredExerciseIcons.add(getFeaturedExerciseIconUri(e));
        }

        Log.d(TAG, "refreshFeaturedExercises: notifyDataSetChanged; featuredExerciseIcons has "+featuredExerciseIcons.size()+" items");
        adapter.notifyDataSetChanged();

        Log.d(TAG, "refreshFeaturedExercises: Initializing position to "+ifitPosition);
        viewPager.setCurrentItem(featuredExerciseIcons.size() * Math.floorDiv(CarouselPagerAdapter.LOOP_COUNT, 2) + ifitPosition);
    }

    protected void refreshFeaturedExercises() {
        List<FeaturedExercise> featuredExercises = new ArrayList<>();
        Log.d(TAG, "refreshFeaturedExercises...");
        Log.d(TAG, "refreshFeaturedExercises: "+FeaturedWorkoutKeys.DATA_IFIT_APP_VISIBLE_BOOL.name()+" = "+DataMatrix.getBoolean(FeaturedWorkoutKeys.DATA_IFIT_APP_VISIBLE_BOOL, false));
        if (isIfitAllowed()) {
            featuredExercises.add(FeaturedExercise.IFIT);
        }
        switch (FrameConfigManager.getInstance().getFrameConfig().machineClassType) {
            case Climbmill:
            case Stepper:
                featuredExercises.add(FeaturedExercise.LANDMARKS);
                break;
            default:
                featuredExercises.add(FeaturedExercise.SPRINT8);
                break;
        }
        //featuredExercises.add(FeaturedExercise.HULU);
        // the last item will appear to the left of the first item
        featuredExercises.add(FeaturedExercise.VIRTUAL_ACTIVE);

        JHTCommonTypes.MachineClassificationType frameType = FrameConfigManager.getInstance().getFrameConfig().machineClassType;
        if (frameType == JHTCommonTypes.MachineClassificationType.Treadmill) featuredExercises.add(FeaturedExercise.PROGRESSION_PROGRAMS);

        if (!isIfitAllowed()) {
            featuredExercises.add(FeaturedExercise.CUSTOM_WORKOUT);
        }

        setFeaturedExercises(featuredExercises);
    }

    protected void setupRecent(View view) {
        if (view == null) return;

        Predicate<WorkoutStartupParams> uniqueFilter = newUniqueWorkoutPredicate();
        Predicate<WorkoutStartupParams> recentWorkoutFilters = pw -> {
            if (WORKOUT_INVALID.test(pw)) return false;
            if (!DapiUser.isGuestUser() && isRemovedCustomWorkout(pw)) return false;
            if (!uniqueFilter.test(pw)) return false; // unique filter should be last
            return true;
        };
        List<PreviousWorkout> listRecent = RecordedWorkoutDatabase.getInstance().getRecentWorkouts(recentWorkoutFilters);

        if (listRecent.size() == 0) {
            view.findViewById(R.id.recent_text).setVisibility(View.GONE);
            view.findViewById(R.id.recent_list).setVisibility(View.GONE);
        } else {
            RecyclerView recyclerViewRecent = view.findViewById(R.id.recent_list);
            recyclerViewRecent.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            PreviousWorkoutRecyclerViewAdapter adapterRecent = new PreviousWorkoutRecyclerViewAdapter(getContext(), listRecent, "recent", workoutLabelProvider);
            recyclerViewRecent.setAdapter(adapterRecent);
            adapterRecent.setOnClickWorkoutListener(onClickWorkoutListener);
        }
    }

    protected void setupFavorites(View view) {
        if (view == null) return;
        List<WorkoutStartupParams> listFavorite = FavoriteWorkoutFile.getInstance().getList();
        listFavorite.removeIf(WORKOUT_INVALID);

        if (listFavorite.size() == 0) {
            view.findViewById(R.id.favorite_text).setVisibility(View.GONE);
            view.findViewById(R.id.favorite_list).setVisibility(View.GONE);
        } else {
            RecyclerView recyclerViewFavorite = view.findViewById(R.id.favorite_list);
            recyclerViewFavorite.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            PreviousWorkoutRecyclerViewAdapter adapterFavorite = new PreviousWorkoutRecyclerViewAdapter(getContext(), listFavorite, "favorite", workoutLabelProvider);
            recyclerViewFavorite.setAdapter(adapterFavorite);
            adapterFavorite.setOnClickWorkoutListener(onClickWorkoutListener);
        }
    }

    protected void setupRecommended(View view) {
        if (view == null) return;
        // (Sprint 8, Virtual Active, Landmarks - if applicable, Rolling Hills, Fat Burn, Target Heart Rate, Calorie Goal)
        List<WorkoutStartupParams> listRecommended = new LinkedList<>();
        switch (FrameConfigManager.getInstance().getFrameConfig().machineClassType) {
            case Climbmill:
            case Stepper:
                listRecommended.add(WorkoutStartupParams.forProgramType(DapiProgramType.Landmark)); // landmark
                listRecommended.add(WorkoutStartupParams.forProgramType(DapiProgramType.VirtualActive)); // va
                listRecommended.add(WorkoutStartupParams.forProgramType(DapiProgramType.PercentTargetHeartRateResistance)); // target heart rate resistance
                listRecommended.add(WorkoutStartupParams.forProgramType(DapiProgramType.CalorieGoal)); // calories goal
                break;

            case Treadmill:
                listRecommended.add(WorkoutStartupParams.forProgramType(DapiProgramType.Sprint8)); // sprint 8
                listRecommended.add(WorkoutStartupParams.forProgramType(DapiProgramType.VirtualActive)); // va
                listRecommended.add(WorkoutStartupParams.forProgramType(DapiProgramType.PercentTargetHeartRateIncline)); // target heart rate incline
                listRecommended.add(WorkoutStartupParams.forProgramType(DapiProgramType.CalorieGoal)); // calories goal
                break;

            default:
                listRecommended.add(WorkoutStartupParams.forProgramType(DapiProgramType.Sprint8)); // sprint 8
                listRecommended.add(WorkoutStartupParams.forProgramType(DapiProgramType.VirtualActive)); // va
                listRecommended.add(WorkoutStartupParams.forProgramType(DapiProgramType.PercentTargetHeartRateResistance)); // target heart rate resistance
                listRecommended.add(WorkoutStartupParams.forProgramType(DapiProgramType.CalorieGoal)); // calories goal
                break;
        }
        listRecommended.removeIf(WORKOUT_INVALID);
        RecyclerView recyclerViewRecommended = view.findViewById(R.id.recommended_list);
        recyclerViewRecommended.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        PreviousWorkoutRecyclerViewAdapter adapterRecommended = new PreviousWorkoutRecyclerViewAdapter(getContext(), listRecommended, "recommended", workoutLabelProvider);
        recyclerViewRecommended.setAdapter(adapterRecommended);
        adapterRecommended.setOnClickWorkoutListener(onClickWorkoutListener);
    }

    protected void refreshAll(boolean includeCarousel) {
        View view = getView(); // if there is no view yet, nothing needs to be refreshed
        if (view != null) {
            if (includeCarousel) refreshFeaturedExercises(); // carousel loads images sync, so could hold up loading the tab
            setupRecent(view);
            setupRecommended(view);
            setupFavorites(view);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.featured_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewPager = view.findViewById(R.id.view_pager);

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int pageMargin = (int) ((metrics.widthPixels / 4) * 1.5);
        viewPager.setPageMargin(-pageMargin);


        adapter = new CarouselPagerAdapter(getContext(), getChildFragmentManager(), viewPager.getId(), featuredExerciseIcons);
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(adapter);

        // Set current item to the middle page so we can fling to both
        // directions left and right; increment for the desired start index
        //viewPager.setCurrentItem(featuredExerciseIcons.size() * CarouselPagerAdapter.LOOP_COUNT / 2);
        // Lol actually pick the IFit one
        viewPager.setOffscreenPageLimit(3);

        adapter.setOnClickListener(position -> {
            if (position >= 0 && position < featuredExercises.size()) {
                FeaturedExercise featuredExercise = featuredExercises.get(position);
                switch (featuredExercise) {
                    case LANDMARKS:
                        showLandmarkSetup();
                        break;
                    case SPRINT8:
                        showSprint8Setup();
                        break;
                    case VIRTUAL_ACTIVE:
                        showVASetup();
                        break;
                    case HULU:
                        appInfo = AppManager.getInstance().getApp(AppIds.HULU_UUID); // load HULU
                        showGenericAppSetup();
                        break;
                    case IFIT:
                        appInfo = AppManager.getInstance().getApp(AppIds.IFIT_UUID);
                        iGoScreenInterface.launchThirdPartyInterface(appInfo.uuid);
                        break;
                    case CUSTOM_WORKOUT:
                        iGoScreenInterface.selectCustomWorkout(null);
                        break;
                    case PROGRESSION_PROGRAMS:
                        showProgressionProgramSetup();
                        break;
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        mDataMatrix.finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        mDataMatrix.createBoolAdapter(FeaturedWorkoutKeys.DATA_IFIT_APP_VISIBLE_BOOL, x -> {
            refreshAll(true);
        }, true);
    }

    void initFragments() {

        vaSetupFragment = new VASetupFragment();
        vaSetupFragment.setIVASetupFragment(new VASetupFragment.IVASetupFragment() {
            @Override
            public void onVAWorkoutStart(String workoutIdentifier, List<WorkoutParameter> parameters) {
                iGoScreenInterface.setCurrentFragment(vaSetupFragment);
                iGoScreenInterface.onWorkoutStart(workoutIdentifier, parameters);
            }

            @Override
            public void onVAGetParameters(String workoutIdentifier) {
                iGoScreenInterface.setCurrentFragment(vaSetupFragment);
                iGoScreenInterface.onWorkoutSelected(workoutIdentifier);
            }
        });

        vaSelectionFragment = new VASelectionFragment();
        vaSelectionFragment.setVaSetupFragment(vaSetupFragment);
        vaSelectionFragment.setIVASelectionInterface(new VASelectionFragment.IVASelectionInterface() {
            @Override
            public void getVAWorkoutParameters(String vaIdentifier) {
                iGoScreenInterface.setCurrentFragment(vaSelectionFragment);
                iGoScreenInterface.onWorkoutSelected(vaIdentifier);
            }
        });
        vaSelectionFragment.setOnCreateViewListener(view -> {
            FavoriteIcon.populateIcon(view, WorkoutStartupParams.forProgramType(DapiProgramType.VirtualActive));
        });

        landmarksSetupFragment = new LandmarksSetupFragment();
        landmarksSetupFragment.setILandmarksSetupFragment(new LandmarksSetupFragment.ILandmarksSetupFragment() {
            @Override
            public void onLandmarksWorkoutStart(String workoutIdentifier, List<WorkoutParameter> parameters) {
                iGoScreenInterface.onWorkoutStart(workoutIdentifier, parameters);
            }
        });
        landmarksSelectionFragment = new LandmarksSelectionFragment();
        landmarksSelectionFragment.setLandmarksSetupFragment(landmarksSetupFragment);
        landmarksSelectionFragment.setILandmarksSelectionInterface(new LandmarksSelectionFragment.ILandmarksSelectionInterface() {
            @Override
            public void getLandmarksWorkoutParameters(String landmarksIdentifier) {
                iGoScreenInterface.setCurrentFragment(landmarksSelectionFragment);
                iGoScreenInterface.onWorkoutSelected(landmarksIdentifier);
            }
        });
        landmarksSelectionFragment.setOnCreateViewListener(view -> {
            FavoriteIcon.populateIcon(view, WorkoutStartupParams.forProgramType(DapiProgramType.Landmark));
        });

    }

    public void setVAWorkoutItems(List<GoScreenWorkoutDisplayItem> vaWorkoutItems) {
        vaSelectionFragment.setVaWorkoutItems(vaWorkoutItems);
    }

    public void setLandmarksWorkoutItems(List<GoScreenWorkoutDisplayItem> landmarksWorkoutItems) {
        landmarksSelectionFragment.setLandmarksWorkoutItems(landmarksWorkoutItems);
    }

    private WorkoutLabelProvider workoutLabelProvider;
    public void setWorkoutLabelProvider(WorkoutLabelProvider workoutLabelProvider) {
        this.workoutLabelProvider = workoutLabelProvider;
        refreshAll(false); // carousel can hold up the load, just do lists
    }
    private boolean isRemovedCustomWorkout(WorkoutStartupParams workoutStartupParams) {
        if (workoutStartupParams == null || workoutStartupParams.programTypeId == null
                || workoutStartupParams.programTypeId != DapiProgramType.CardioCustomWorkout.Value) return false; // not a custom workout
        if (this.workoutLabelProvider == null) return false; // don't know yet
        return !(this.workoutLabelProvider.customWorkoutExists(workoutStartupParams.customWorkoutUuid)); // it doesn't exist anymore
    }

    PreviousWorkoutRecyclerViewAdapter.OnClickWorkoutListener onClickWorkoutListener = previousWorkout -> {

        // handle manual workout with app
        if (previousWorkout.appUuid != null) {
            AppInfo appInfo = AppManager.getInstance().getApp(previousWorkout.appUuid);
            if (appInfo != null) {
                if (appInfo.useThirdPartyInterface) {
                    LaunchAppUtil.initialLaunchAppByIcon(getContext(), appInfo, getActivity().getWindow().getDecorView().findViewById(android.R.id.content));
                    return;
                } else if (previousWorkout.programTypeId != null && previousWorkout.programTypeId == DapiProgramType.Manual.Value) { // or check if entertainment app?
                    FeaturedFragment.this.appInfo = appInfo;
                    showGenericAppSetup();
                    return;
                }
            }
        }

        // handle workout with special setup
        DapiProgramType dapiProgramType = DapiProgramType.fromInt(previousWorkout.programTypeId);
        if (dapiProgramType != null) {
            switch (DapiProgramType.fromInt(previousWorkout.programTypeId)) {
                case Sprint8:
                    showSprint8Setup();
                    return;
                case VirtualActive:
                    showVASetup();
                    return;
                case Landmark:
                    showLandmarkSetup();
                    return;
            }
        }

        // we assume the workout is in the list of exercises
        ParkerWorkoutSelectionFragment.selectedWorkoutId = previousWorkout.programTypeId;
        iGoScreenInterface.selectCategory(1);
        if (previousWorkout.programTypeId == DapiProgramType.CardioCustomWorkout.Value) {
            iGoScreenInterface.selectCustomWorkout(previousWorkout.customWorkoutUuid);
        }

    };

    void showGenericAppSetup() {
        GenericAppSetupFragment genericAppSetupFragment = new GenericAppSetupFragment();
        genericAppSetupFragment.setSetupInterface(new GenericAppSetupFragment.ISetupInterface() {
            @Override
            public void onWorkoutStart(String workoutIdentifier, List<WorkoutParameter> parameters) {
                iGoScreenInterface.setCurrentFragment(genericAppSetupFragment);
                iGoScreenInterface.onWorkoutStart(workoutIdentifier, parameters);
            }
            @Override
            public void onGetParameters(String workoutIdentifier) {
                iGoScreenInterface.setCurrentFragment(genericAppSetupFragment);
                if (appInfo != null) iGoScreenInterface.onWorkoutSelected(workoutIdentifier + ":" + appInfo.uuid);
                else iGoScreenInterface.onWorkoutSelected(workoutIdentifier);
            }
        });
        String appName = AppIconManager.getInstance().findAppName(appInfo);
        GoScreenWorkoutDisplayItem manualWorkout = new GoScreenWorkoutDisplayItem(appName, "", appInfo.uuid, "", null, null, "0");
        genericAppSetupFragment.loadWorkoutInfo(manualWorkout);
        workoutSetupFragment.loadNewSetupFragment(genericAppSetupFragment);
        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                .replace(android.R.id.content, workoutSetupFragment).commit();
    }

    void showLandmarkSetup() {
        workoutSetupFragment.loadNewSetupFragment(landmarksSelectionFragment);
        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                .replace(android.R.id.content, workoutSetupFragment).commit();
    }

    void showSprint8Setup() {
        // NOTE: custom Sprint8 profiles are no longer saved per user here (was CustomSprint8File),
        // the plugin has no equivalent persistence yet.
        // The plugin setup draws its own modal and sizes itself against the window, so it takes the
        // content view directly rather than sitting inside WorkoutSetupFragment's inset card.
        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                .replace(android.R.id.content, FeaturePluginSetupFragment.forSprint8()).commit();
    }

    void showVASetup() {
        workoutSetupFragment.loadNewSetupFragment(vaSelectionFragment);
        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                .replace(android.R.id.content, workoutSetupFragment).commit();
    }

    void showProgressionProgramSetup(){
        checkIfSafetyPinNeeded(()->{
            requireActivity().finish();
            Intent progressionProgram = new Intent(getActivity(), MmActivity.class);
            progressionProgram.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(progressionProgram);
        });
    }

    public void checkIfSafetyPinNeeded(DigitalSafetyPinDialog.Action action) {
        if (DataMatrix.getBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL)) {
            if (safetyPrompt == null) safetyPrompt = new DigitalSafetyPinDialog(getApplicationContext());
            safetyPrompt.checkIfNeeded(action);
        } else {
            action.doAction();
        }
    }
}
