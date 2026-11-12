package com.jht.chimera.ui.mainapp.goscreen.WorkoutSetup;

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
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.conversionlib.DistanceConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.ui.mainapp.GoScreenActivity;
import com.jht.chimera.ui.mainapp.goscreen.BitmapAsyncLoadTask;
import com.jht.chimera.ui.mainapp.goscreen.GenderRadioButton;
import com.jht.chimera.ui.mainapp.goscreen.GoScreenBaseFragment;
import com.jht.chimera.ui.mainapp.goscreen.GoScreenWorkoutDisplayItem;
import com.jht.chimera.ui.mainapp.goscreen.GoViewBuilder;
import com.jht.chimera.ui.mainapp.goscreen.MinMaxParameterLogic;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutParameter;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.gattserver2.WatchState;
import com.jht.workoutcontrollerlib.model.WorkoutModel;
import com.jht.workoutcontrollerlib.workoutcardio.WorkoutScriptVariables;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Created by AlexN on 12/29/2018.
 */

public class WorkoutSelectionFragment extends GoScreenBaseFragment {
    private static final String TAG = "WorkoutSelectionFrag...";
    private final DataMatrix dataMatrix = new DataMatrix();
    private RecyclerView recyclerView;
    private WorkoutSelectionAdapter workoutItemAdapter;
    private List<GoScreenWorkoutDisplayItem> workoutItems;
    private ConstraintLayout workoutSelectionBaseLayout;
    private ConstraintLayout maxHRInstructionsLayout;
    private GoScreenWorkoutDisplayItem selectedWorkoutItem;
    private List<WorkoutParameter> workoutParameters;
    private TextView workoutTitle;
    private TextView workoutDescription;
    private ImageView workoutBackground;
    private PillBox[] valueSelectorWidgets;
    private BitmapAsyncLoadTask bitmapAsyncLoadTask;
    private GenderRadioButton mGenderButton;
    private long pressTime = 0;
    private IWorkoutSelectionInterface iWorkoutSelectionInterface;
    private WorkoutInfoBox workoutInfoBox;

    private WorkoutModel workoutModel = null;

    public interface IWorkoutSelectionInterface {
        void workoutSelected(String workoutIdentifier);
        void onStartWorkout(String workoutIdentifier, List<WorkoutParameter> parameters);
        void onDisabledWorkoutSelected(String workoutID);
        void getWorkoutInfo();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.workout_selection_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        workoutSelectionBaseLayout = view.findViewById(R.id.workout_selection_layout);
        maxHRInstructionsLayout = view.findViewById(R.id.max_hr_instructions);
        recyclerView = view.findViewById(R.id.list_recycler_view);
        workoutTitle = view.findViewById(R.id.workout_title);
        workoutDescription = view.findViewById(R.id.workout_text);
        workoutBackground = view.findViewById(R.id.workout_background);
        mGenderButton = view.findViewById(R.id.gender_button);
        workoutInfoBox = view.findViewById(R.id.workout_info_box);

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
        workoutItemAdapter = new WorkoutSelectionAdapter(workoutItems, position -> {
            if (workoutItems.get(position) != selectedWorkoutItem) {
                if (workoutItems.get(position).enabledState == GoScreenWorkoutDisplayItem.WorkoutStateTypes.ENABLED.ordinal()) {
                    selectedWorkoutItem = workoutItems.get(position);
                    ImageView backgroundView;
                        view.findViewById(R.id.workout_selection_setup).setVisibility(View.VISIBLE);
                        backgroundView = workoutBackground;
                    workoutTitle.setText(selectedWorkoutItem.title);
                    workoutDescription.setText(selectedWorkoutItem.description);

                    //load workout background
                    if (bitmapAsyncLoadTask != null) {
                        bitmapAsyncLoadTask.cancel(true);
                    }
                    bitmapAsyncLoadTask = new BitmapAsyncLoadTask(backgroundView, backgroundView.getWidth(),
                            backgroundView.getHeight());
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

    public enum GoScreenKeys {
        DATA_DISPLAY_METRIC_VALUES_BOOL,
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() instanceof GoScreenActivity) {
            DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.TRAINING_EXERCISES_SCREEN.name());
        }

        dataMatrix.createFloatAdapter(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT, weight -> {
            for (PillBox valueSelectorWidget : valueSelectorWidgets) {
                if (valueSelectorWidget.parameterSettings != null && valueSelectorWidget.parameterSettings.label().equalsIgnoreCase(WEIGHT_VARIABLE)) {
                    boolean isMetric = DataMatrix.getBoolean(GoScreenKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, true);
                    valueSelectorWidget.setValue(Math.round(isMetric ? (weight) : (weight / 0.453592f)));
                }
            }
        }, true);

        dataMatrix.createFloatAdapter(DAPIKeys.DATA_USER_HEIGHT_METERS_FLOAT, height -> {
            for (PillBox valueSelectorWidget : valueSelectorWidgets) {
                if (valueSelectorWidget.parameterSettings != null && valueSelectorWidget.parameterSettings.label().equalsIgnoreCase("HEIGHT")) {
                    boolean isMetric = DataMatrix.getBoolean(GoScreenKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, true);
                    //Widget takes height as cm or in
                    valueSelectorWidget.setValue(Math.round(isMetric ? (height*100) : (DistanceConvertUtil.convert(height*100, DistanceConvertUtil.CENTIMETERS_ID, DistanceConvertUtil.INCHES_ID))));
                }
            }
        }, true);

        dataMatrix.createIntAdapter(DAPIKeys.DATA_USER_GENDER_ENUM_INT, gender -> mGenderButton.setSelectedGender(gender), true);

        dataMatrix.createIntAdapter(DAPIKeys.DATA_USER_AGE_INT, age -> {
            for (PillBox valueSelectorWidget : valueSelectorWidgets) {
                if (valueSelectorWidget.parameterSettings != null && valueSelectorWidget.parameterSettings.label().equalsIgnoreCase("AGE")) {
                    valueSelectorWidget.setValue(age);
                }
            }
        }, true);

    }

    @Override
    public void onPause() {
        super.onPause();
        dataMatrix.finish();
    }

    public void setWorkoutSelectionInterface(IWorkoutSelectionInterface iWorkoutSelectionInterface) {
        this.iWorkoutSelectionInterface = iWorkoutSelectionInterface;
    }

    public void setWorkoutItems(List<GoScreenWorkoutDisplayItem> workoutItems) {
        this.workoutItems = workoutItems;
    }

    public void setWorkoutEnabledStates(Map<String, Integer> workoutEnabledStates) {
        for (GoScreenWorkoutDisplayItem item : workoutItems) {
            if (!item.identifier.isEmpty()) {
                try {
                    Integer state = workoutEnabledStates.get(item.identifier);
                    if (state != null)
                        item.enabledState = state;
                } catch (Exception e) {
                    Log.e(TAG, "Caught exception", e);
                }
            }
        }
    }

    private void setWorkoutParameters() {
        if (workoutParameters != null && valueSelectorWidgets != null) {
            if (isMaxHrTest()) {
                setMaxHrTestLayout();
            } else {
                List<WorkoutParameter> formattedParameters = getFormattedWorkoutParameters();
                if (!hasGenderInput()) mGenderButton.setVisibility(View.INVISIBLE);

                for (int i = 0; i < valueSelectorWidgets.length; i++) {
                    maxHRInstructionsLayout.setVisibility(View.GONE);

                    if (i < formattedParameters.size()) {
                        WorkoutParameter parameter = formattedParameters.get(i);
                        if (!parameter.identifier().equals(GENDER_VARIABLE)) {
                            valueSelectorWidgets[i].initParameterSettings(parameter);
                            valueSelectorWidgets[i].setVisibility(View.VISIBLE);
                            valueSelectorWidgets[i].setiValueSelectorInterface((value, identifier) -> {
                                // Changes the min/max values (eg so the min can't be higher than the max)
                                MinMaxParameterLogic.handleValueUpdated(value, identifier, valueSelectorWidgets);

                                // Update the level box info when we change the level
                                if (identifier.equals(LEVEL_VARIABLE)) {
                                    updateLevelInfo(Math.round(value) - 1);
                                }
                            });
                        } else {
                            valueSelectorWidgets[i].setVisibility(View.INVISIBLE);
                            setDefaultGender(parameter);
                            alignGenderButton(valueSelectorWidgets[i]);
                            mGenderButton.setVisibility(View.VISIBLE);
                        }
                    } else {
                        valueSelectorWidgets[i].setVisibility(View.INVISIBLE);
                    }
                }
            }
        }

       handleParameterSpecificLogic();
    }

    /**
     * Handles setting defaults and running logic for certain parameters
     */
    private void handleParameterSpecificLogic() {
        boolean hasLevel = false;
        if (valueSelectorWidgets != null) {
            for (PillBox valueSelectorWidget : valueSelectorWidgets) {
                if (valueSelectorWidget.parameterSettings == null) continue;

                String id = valueSelectorWidget.parameterSettings.identifier();
                switch (id) {
                    case HEIGHT_VARIABLE:
                        boolean isMetric = DataMatrix.getBoolean(GoScreenKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, true);
                        float height = DataMatrix.getFloat(DAPIKeys.DATA_USER_HEIGHT_METERS_FLOAT);
                        //Widget takes height as cm or in
                        valueSelectorWidget.setValue(Math.round(isMetric ? (height*100) : (DistanceConvertUtil.convert(height*100, DistanceConvertUtil.CENTIMETERS_ID, DistanceConvertUtil.INCHES_ID))));
                        break;
                    case AGE_VARIABLE:
                        valueSelectorWidget.setValue(DataMatrix.getInt(DAPIKeys.DATA_USER_AGE_INT,
                                DataMatrix.getInt(SettingKeys.DATA_DEFAULT_AGE_INT)));
                        break;
                    case LEVEL_VARIABLE:
                        if (iWorkoutSelectionInterface != null) {
                            iWorkoutSelectionInterface.getWorkoutInfo();
                            hasLevel = true;
                        }
                        break;
                    default:
                        // Do nothing
                }
            }
        }

        if (!hasLevel) { // If we don't have a level parameter we should not display this box
            workoutInfoBox.setVisibility(View.GONE);
        }
    }

    private boolean isMaxHrTest() {
        //TODO: This will check if there's only the weight field for inputs- we probably want something more certain than that in the future
        return workoutParameters.size() == 1 && workoutParameters.get(0).identifier().equals(WEIGHT_VARIABLE);
    }

    private void setDefaultGender(WorkoutParameter genderParameter) {
        if (DataMatrix.getInt(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, WatchState.Disconnected.ordinal()) == WatchState.Connecting.ordinal()) {
            mGenderButton.setSelectedGender(DataMatrix.getInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT));
        } else {
            mGenderButton.setSelectedGender((Math.round(genderParameter.defaultValue())));
        }
    }

    private void setMaxHrTestLayout() {
        // Instructions is visible
        valueSelectorWidgets[0].setVisibility(View.INVISIBLE);
        valueSelectorWidgets[1].setVisibility(View.INVISIBLE);
        valueSelectorWidgets[2].setVisibility(View.INVISIBLE);
        valueSelectorWidgets[3].setVisibility(View.INVISIBLE);
        valueSelectorWidgets[4].setVisibility(View.INVISIBLE);
        valueSelectorWidgets[5].setVisibility(View.INVISIBLE);
        mGenderButton.setVisibility(View.INVISIBLE);
        maxHRInstructionsLayout.setVisibility(View.VISIBLE);
    }

    /**
     * Moves the gender field (if it exists) to the end of the workout parameters
     * @return formatted workout parameter list
     */
    private List<WorkoutParameter> getFormattedWorkoutParameters() {
        List<WorkoutParameter> formattedList = new ArrayList<>();
        WorkoutParameter genderParameter = null;
        for (WorkoutParameter parameter : workoutParameters) {
            if (!parameter.identifier().equals(GENDER_VARIABLE)) {
                formattedList.add(parameter);
            } else {
                genderParameter = parameter;
            }
        }

        if (genderParameter != null) formattedList.add(genderParameter);
        return formattedList;
    }

    private enum SettingKeys {
        DATA_DEFAULT_HEIGHT_CM_FLOAT,
        DATA_DEFAULT_AGE_INT
    }

    @NonNls private static final String GENDER_VARIABLE = "GENDER";
    @NonNls private static final String WEIGHT_VARIABLE = "WEIGHT";
    @NonNls private static final String LEVEL_VARIABLE = "LEVEL";
    @NonNls private static final String AGE_VARIABLE = "AGE";
    @NonNls private static final String HEIGHT_VARIABLE = "HEIGHT";


    private boolean hasGenderInput() {
        for (WorkoutParameter parameter : workoutParameters) {
            if (parameter.identifier().equals(GENDER_VARIABLE)) {
                return true;
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
        for (WorkoutParameter parameter : workoutParameters) {
            if (parameter.identifier().equals(GENDER_VARIABLE)) {
                parameter.value(mGenderButton.getGenderValue());
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
                    valueSelectorWidget.setValue((float) workoutParameter.valueAsDouble());
                    break;
                }
            }
        }
    }

    /**
     * Sets the workout model box to display correct information
     * @param workoutModel The workout model for the current workout
     */
    public void setWorkoutModel(WorkoutModel workoutModel) {
        this.workoutModel = workoutModel;
        updateLevelInfo(getSelectedLevel());
    }

    private int getSelectedLevel() {
        for (PillBox pillBox : valueSelectorWidgets) {
            if (pillBox.parameterSettings != null && pillBox.parameterSettings.identifier().equals(LEVEL_VARIABLE)) {
                return (int) Math.round(pillBox.parameterSettings.valueAsDouble()) - 1;
            }
        }

        return 0;
    }

    private void updateLevelInfo(int level) {
        if (workoutModel == null) return;

        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        switch(frameConfig.machineClassType) {
            case Treadmill:
                setLevelInfoToStats(level, WorkoutScriptVariables.SPEED, WorkoutScriptVariables.INCLINE);
                break;
            case Elliptical:
            case RecumbentElliptical:
            case Bike:
            case Climbmill:
            case Stepper:
            case SpinBike:
                setLevelInfoToStats(level, WorkoutScriptVariables.RESISTANCE, null);
                break;
            case Ascent:
                setLevelInfoToStats(level, WorkoutScriptVariables.RESISTANCE, WorkoutScriptVariables.INCLINE);
                break;
            default:
                break;
        }
    }

    private void setLevelInfoToStats(int level, String stat1, String stat2) {
        List<WorkoutStatInfo> workoutInfoList = new ArrayList<>();
        List<Float> stageChanges1 = WorkoutInfoBoxHelperKt.getStatChangesForWorkout(level, stat1, workoutModel);
        WorkoutStatInfo stat1Info = WorkoutInfoBoxHelperKt.getWorkoutInfo(stat1, stageChanges1);
        if (stat1Info != null) workoutInfoList.add(stat1Info);

        if (stat2 != null) {
            List<Float> stageChanges2 = WorkoutInfoBoxHelperKt.getStatChangesForWorkout(level, stat2, workoutModel);
            WorkoutStatInfo stat2Info = WorkoutInfoBoxHelperKt.getWorkoutInfo(stat2, stageChanges2);
            if (stat2Info != null) workoutInfoList.add(stat2Info);
        }

        displayWorkoutInfo(level, workoutInfoList);
    }

    private void displayWorkoutInfo(int level, List<WorkoutStatInfo> workoutInfoList) {
        String levelTitle = getString(R.string.level_tc) + " " + (level + 1);

        switch (workoutInfoList.size()) {
            case 0: // There is no data to display
                workoutInfoBox.setVisibility(View.GONE);
                break;
            case 1: // There is only one stat to display
                workoutInfoBox.setStatInfo(levelTitle, workoutInfoList.get(0));
                workoutInfoBox.setVisibility(View.VISIBLE);
                break;
            case 2: // 2 or more stats (there should never be more than 2)
            default:
                workoutInfoBox.setStatInfo(levelTitle, workoutInfoList.get(0), workoutInfoList.get(1));
                workoutInfoBox.setVisibility(View.VISIBLE);
                break;
        }
    }
}
