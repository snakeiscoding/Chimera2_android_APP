package com.jht.chimera.ui.mainapp.goscreen;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androiduiwidgets.pillbox.PillBox;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ConsoleType;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage;
import com.jht.chimera.lib.workout.FavoriteIcon;
import com.jht.chimera.lib.workout.FavoriteWorkoutFile;
import com.jht.chimera.lib.workout.WorkoutStartupParams;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom.CustomWorkoutFragment;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom.CustomWorkoutItem;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutSetup.WorkoutSelectionAdapter;
import com.jht.datamatrixlib.DataMatrix;
import com.jhtp.dapiservice.wt.DapiProgramType;

import java.util.List;
import java.util.Map;

public class ParkerWorkoutSelectionFragment extends GoScreenBaseFragment {
    private static final String TAG = "ParkerWorkoutSelecti...";
    private final String MANUAL_WORKOUT_ID = String.valueOf(DapiProgramType.Manual.Value);
    private final String CUSTOM_WORKOUT_ID = String.valueOf(DapiProgramType.CardioCustomWorkout.Value);

    public static int selectedWorkoutId = 0;

    private RecyclerView recyclerView;
    private WorkoutSelectionAdapter workoutItemAdapter;

    private List<GoScreenWorkoutDisplayItem> workoutItems;
    private ConstraintLayout workoutSelectionBaseLayout;
    private String previousSelectedWorkoutId = MANUAL_WORKOUT_ID;
    private GoScreenWorkoutDisplayItem selectedWorkoutItem;
    private List<WorkoutParameter> workoutParameters;
    private TextView workoutTitle;
    private TextView workoutDescription;
    private ImageView workoutBackground;
    private PillBox[] valueSelectorWidgets;
    private BitmapAsyncLoadTask bitmapAsyncLoadTask;
    private CustomWorkoutFragment customWorkoutFragment;
    private ParkerGoScreenModule.IGoScreenInterface iGoScreenInterface;
    private View customWorkoutView;
    private View horizontalBar;

    private GenderRadioButton mGenderButton;

    private long pressTime = 0;

    public interface IWorkoutSelectionInterface {
        void workoutSelected(String workoutIdentifier);
        void onStartWorkout(String workoutIdentifier, List<WorkoutParameter> parameters);
        void onDisabledWorkoutSelected(String workoutID);
    }

    private IWorkoutSelectionInterface iWorkoutSelectionInterface;

    @Override
    public void onResume() {
        super.onResume();
        // Check if the current selected workout is Custom
        if (selectedWorkoutItem != null && selectedWorkoutItem.identifier.equals(CUSTOM_WORKOUT_ID)) {
            // Reload the Custom Workout fragment
            addCustomWorkoutFragment();
            customWorkoutView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        removeCustomWorkoutFragment();
    }

    void addCustomWorkoutFragment() {
        if (customWorkoutFragment != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.workout_selection_container, customWorkoutFragment, "customWorkoutFragment").commit();
        }
    }

    void removeCustomWorkoutFragment() {
        if (customWorkoutFragment != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .remove(customWorkoutFragment).commit();
        }
    }

    private void exerciseSelectorVisible(boolean visible) {
        int visibility = (visible ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(visibility);
        horizontalBar.setVisibility(visibility);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.parker_workout_selection_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        workoutSelectionBaseLayout = view.findViewById(R.id.workout_selection_layout);
        recyclerView = view.findViewById(R.id.list_recycler_view);
        horizontalBar = view.findViewById(R.id.grey_bar);
        workoutTitle = view.findViewById(R.id.workout_title);
        workoutDescription = view.findViewById(R.id.workout_text);
        workoutBackground = view.findViewById(R.id.workout_background);
        customWorkoutView = view.findViewById(R.id.workout_selection_container);

        mGenderButton = view.findViewById(R.id.gender_button);

        valueSelectorWidgets = new PillBox[6];
        valueSelectorWidgets[0] = view.findViewById(R.id.stat_setup_1);
        valueSelectorWidgets[1] = view.findViewById(R.id.stat_setup_2);
        valueSelectorWidgets[2] = view.findViewById(R.id.stat_setup_3);
        valueSelectorWidgets[3] = view.findViewById(R.id.stat_setup_4);
        valueSelectorWidgets[4] = view.findViewById(R.id.stat_setup_5);
        valueSelectorWidgets[5] = view.findViewById(R.id.stat_setup_6);

        setWorkoutParameters();
        selectedWorkoutItem = null;
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemAnimator(null);

        // find if the workout id exists in the workout list
        // TODO This will not work for time, distance, calories, floor goal since it has the same workout id as manual
        int selectedIndex = -1;
        int counter = 0;
        if (workoutItems != null) {
            for (GoScreenWorkoutDisplayItem workoutItem : workoutItems) {
                if (workoutItem.identifier != null && workoutItem.identifier.equals(String.valueOf(selectedWorkoutId))) {
                    selectedIndex = counter;
                    break;
                }
                counter++;
            }
        }

        if (selectedIndex == -1) {
            Log.d(TAG, "Couldn't find workout with ID "+selectedWorkoutId+" in workoutItems (of which there are "+workoutItems.size()+")");
        }

        workoutItemAdapter = new WorkoutSelectionAdapter(workoutItems, selectedIndex, position -> {
            if (position < 0 || position >= workoutItems.size()) {
                Log.e(TAG, "Workout position ("+position+") is out of bounds (0.."+(workoutItems.size()-1)+")");
                return;
            }
            if (workoutItems.get(position) != selectedWorkoutItem) {
                if (workoutItems.get(position).enabledState == GoScreenWorkoutDisplayItem.WorkoutStateTypes.ENABLED.ordinal()) {
                    if (selectedWorkoutItem != null && !selectedWorkoutItem.identifier.equals(CUSTOM_WORKOUT_ID)) {
                        previousSelectedWorkoutId = selectedWorkoutItem.identifier; // on small screens custom hides the workout list and must return to previous workout selection
                    }
                    selectedWorkoutItem = workoutItems.get(position);

                    if (selectedWorkoutItem.identifier.equals(CUSTOM_WORKOUT_ID)) {
                        addCustomWorkoutFragment();
                        customWorkoutView.setVisibility(View.VISIBLE);
                        return;
                    }

                    // update favorite icon
                    try {
                        int programTypeId = Integer.parseInt(selectedWorkoutItem.identifier);
                        FavoriteIcon.populateIcon(view, WorkoutStartupParams.forProgramType(programTypeId));
                    } catch (Exception e) {
                        Log.w(TAG, e);
                    }

                    // save the workout if it was currently shown
                    if (customWorkoutView.getVisibility() == View.VISIBLE) {
                        ///if (customWorkoutFragment != null) customWorkoutFragment.saveWorkouts();
                        customWorkoutView.setVisibility(View.GONE);
                        removeCustomWorkoutFragment();
                    }

                    workoutTitle.setText(selectedWorkoutItem.title);
                    workoutDescription.setText(selectedWorkoutItem.description);

                    //load workout background
                    if (bitmapAsyncLoadTask != null) {
                        bitmapAsyncLoadTask.cancel(true);
                    }
                    bitmapAsyncLoadTask = new BitmapAsyncLoadTask(workoutBackground, workoutBackground.getWidth(),
                            workoutBackground.getHeight());
                    bitmapAsyncLoadTask.execute(selectedWorkoutItem.backgroundPath);

                    if (iWorkoutSelectionInterface != null) {
                        //Get parameters on selection of workout
                        iWorkoutSelectionInterface.workoutSelected(selectedWorkoutItem.identifier);
                    }
                } else {
                    if (iWorkoutSelectionInterface != null) {
                        iWorkoutSelectionInterface.onDisabledWorkoutSelected(workoutItems.get(position).identifier);
                    }
                }
            }

        });

        //Need getWidth as non-zero value after drawing, so observe layout binding
        final ViewTreeObserver observer = recyclerView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                workoutItemAdapter.setWidth(recyclerView.getWidth());
                recyclerView.setAdapter(workoutItemAdapter);
            }
        });


        Button startButton = view.findViewById(R.id.start_button);
        startButton.setOnClickListener(v -> {
            long uptime = SystemClock.uptimeMillis();
            if (pressTime < uptime - 1500 && iWorkoutSelectionInterface != null) {
                pressTime = uptime;
                updateGenderInputIfNeeded();
                iWorkoutSelectionInterface.onStartWorkout(selectedWorkoutItem.identifier, workoutParameters);
            }
        });
    }

    public void setGoScreenInterface(ParkerGoScreenModule.IGoScreenInterface goScreenInterface) {
        iGoScreenInterface = goScreenInterface;
    }

    // Not sure what customWorkoutView is!
    // But this one comes from CustomWorkoutFragment#onCreateViewListener
    View customWorkoutView2;
    CustomWorkoutItem selectedCustomWorkoutItem;
    void pokeCustomWorkoutFavoriteIcon() {
        if (customWorkoutView == null) return;
        if (selectedCustomWorkoutItem == null) return;
        FavoriteIcon.populateIcon(
            customWorkoutView2,
            WorkoutStartupParams.forCustomWorkout(selectedCustomWorkoutItem.uuid)
        );
    }

    public void setCustomWorkoutItems(Context context, List<WorkoutParameter> customWorkoutParameters, List<CustomWorkoutItem> customWorkoutItems) {

        if (customWorkoutFragment == null) {
            // do we go off dimensions or specific console types?
            ConsoleType consoleType = ProductConfig.getConsoleType();
            customWorkoutFragment = new CustomWorkoutFragment(consoleType.widthPx <= 1366);
            customWorkoutFragment.setCooldownOptionality(!ProductConfig.forcesCustomWorkoutsToHaveCooldowns());
            customWorkoutFragment.setNetworkConnectedStatus(true);
            customWorkoutFragment.setCustomWorkoutInterface(new CustomWorkoutFragment.ICustomWorkoutInterface() {
                @Override
                public void saveWorkouts(List<CustomWorkoutItem> customWorkouts) {
                    iGoScreenInterface.saveCustomWorkouts(customWorkouts);
                }

                @Override
                public void onStartCustomWorkout(CustomWorkoutItem customWorkout) {
                    iGoScreenInterface.onCustomWorkoutStart(customWorkout);
                }

                public void onBackButtonShow(boolean isShown) {
                    exerciseSelectorVisible(!isShown); // hide selector when back button is shown
                }
                public void onBackButtonClicked() {
                    if (!selectWorkout(previousSelectedWorkoutId)) selectWorkout(MANUAL_WORKOUT_ID);
                    exerciseSelectorVisible(true);
                }

                private MessageCenterMessage message = new MessageCenterMessage("", StringLookup.get(R.string.unable_to_save_workout_pg), 30);
                public void onUnableToSaveShow(boolean isShown) {
                    message.setHidden(!isShown);
                    DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, message);
                }
            });
            customWorkoutFragment.onCreateViewListener = view -> {
                customWorkoutView2 = view;
                pokeCustomWorkoutFavoriteIcon();
            };
            customWorkoutFragment.customWorkoutSelectionListener = customWorkoutItem -> {
                selectedCustomWorkoutItem = customWorkoutItem;
                pokeCustomWorkoutFavoriteIcon();
            };
            customWorkoutFragment.customWorkoutRemovedListener = customWorkoutItem -> {
                FavoriteWorkoutFile.getInstance().removeCustomWorkout(customWorkoutItem.uuid == null ? null : customWorkoutItem.uuid.toString());
            };
        }

        customWorkoutFragment.setCustomWorkoutItems(context, customWorkoutParameters, customWorkoutItems);
    }
    public void setCustomWorkoutIsGuest(boolean isGuest) {
        customWorkoutFragment.updateGuestUserLayout(isGuest);
    }

    public void setCustomWorkoutIsOfflineUser(boolean isOfflineUser) {
        customWorkoutFragment.updateOfflineUser(isOfflineUser);
    }

    public void setWorkoutSelectionInterface(IWorkoutSelectionInterface iWorkoutSelectionInterface) {
        this.iWorkoutSelectionInterface = iWorkoutSelectionInterface;
    }

    public void setWorkoutItems(List<GoScreenWorkoutDisplayItem> workoutItems) {
        this.workoutItems = workoutItems;
    }

    public void setWorkoutEnabledStates(Map<Integer, Integer> workoutEnabledStates) {
        for (GoScreenWorkoutDisplayItem item : workoutItems) {
            try {
                Integer state = workoutEnabledStates.get(Integer.parseInt(item.identifier));
                if (state != null)
                    item.enabledState = state;
            } catch (Exception e) {
                Log.e(TAG, "Caught exception", e);
            }
        }

        if (workoutItemAdapter != null)
            workoutItemAdapter.notifyDataSetChanged();
    }

    private void setWorkoutParameters() {
        boolean firstInvisibleItem = true;
        WorkoutParameter genderParameter = null;
        if (workoutParameters != null && valueSelectorWidgets != null) {
            int workoutParameterIndex = 0;
            for (int i = 0; i < valueSelectorWidgets.length; i++) {
                // Gender variable is a special case that needs to be handled specially at the end, skip it
                if (i < workoutParameters.size()) {
                    if (workoutParameters.get(i).identifier().equals(GENDER_VARIABLE)) {
                        genderParameter = workoutParameters.get(i);
                        workoutParameterIndex++;
                    }
                }

                if (workoutParameterIndex < workoutParameters.size()) {
                    valueSelectorWidgets[i].initParameterSettings(workoutParameters.get(workoutParameterIndex));
                    valueSelectorWidgets[i].setVisibility(View.VISIBLE);
                    valueSelectorWidgets[i].setiValueSelectorInterface((value, identifier) -> MinMaxParameterLogic.handleValueUpdated(value, identifier, valueSelectorWidgets));
                } else {
                    valueSelectorWidgets[i].setVisibility(View.INVISIBLE);
                    if (firstInvisibleItem ) {
                        firstInvisibleItem = false;

                        // Show gender input at the end over the top of the first invisible pill box
                        if (hasGenderInput() && genderParameter != null) {
                            mGenderButton.setSelectedGender(Math.round(genderParameter.defaultValue()));
                            alignGenderButton(valueSelectorWidgets[i]);
                            mGenderButton.setVisibility(View.VISIBLE);
                        }
                        else {
                            mGenderButton.setVisibility(View.INVISIBLE);
                        }
                    }
                }
                workoutParameterIndex++;
            }
        }
    }

    private static String GENDER_VARIABLE = "GENDER"; //NON-NLS

    private boolean hasGenderInput() {
        if (workoutParameters != null) {
            for (WorkoutParameter parameter : workoutParameters) {
                if (parameter.identifier().equals(GENDER_VARIABLE)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void alignGenderButton(View viewToAlignWith) {
        ConstraintSet set = new ConstraintSet();
        set.clone(workoutSelectionBaseLayout);
        set.connect(mGenderButton.getId(), ConstraintSet.START, viewToAlignWith.getId(), ConstraintSet.START, 0);
        set.connect(mGenderButton.getId(), ConstraintSet.TOP, viewToAlignWith.getId(), ConstraintSet.TOP, 0);
        set.applyTo(workoutSelectionBaseLayout);
    }

    private void updateGenderInputIfNeeded() {
        if (workoutParameters != null) {
            for (WorkoutParameter parameter : workoutParameters) {
                if (parameter.identifier().equals(GENDER_VARIABLE)) {
                    parameter.value(mGenderButton.getGenderValue());
                }
            }
        }
    }

    @Override
    public void setParameterValues(List<WorkoutParameter> inputConfigurationList) {
        this.workoutParameters = inputConfigurationList;
        setWorkoutParameters();
    }

    @Override
    public void setParameter(WorkoutParameter workoutParameter) {
        if (valueSelectorWidgets != null && isAdded()) {
            for (PillBox valueSelectorWidget : valueSelectorWidgets) {
                if (valueSelectorWidget.parameterSettings.identifier().equals(workoutParameter.identifier())) {
                    valueSelectorWidget.setValue((float)workoutParameter.valueAsDouble());
                    break;
                }
            }
        }
    }

    private boolean selectWorkout(String identifier) {
        for (int i=0; i < workoutItems.size(); ++i) {
            GoScreenWorkoutDisplayItem item = workoutItems.get(i);
            if (item.identifier.equals(identifier)) {
                workoutItemAdapter.selectItem(i);
                return true;
            }
        }
        return false;
    }
    public void selectCustomWorkout(String uuid) {
        if (selectWorkout(CUSTOM_WORKOUT_ID)) {
            customWorkoutFragment.selectCustomWorkout(uuid);
        }
    }
}
