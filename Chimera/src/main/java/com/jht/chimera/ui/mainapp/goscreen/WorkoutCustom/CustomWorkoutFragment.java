package com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom;

import static com.jht.chimera.lib.widget.CommonRoutines.hideKeyboard;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androiduiwidgets.pillbox.PillBox;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.lib.common.formatting.TimeFormat;
import com.jht.chimera.lib.widget.dialog.MessageBoxDialog;
import com.jht.chimera.ui.mainapp.GoScreenActivity;
import com.jht.chimera.ui.mainapp.goscreen.GoScreenWorkoutDisplayItem;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutParameter;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.translations.LanguageHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by AlexN on 12/29/2018.
 */

public class CustomWorkoutFragment extends Fragment {
    protected static final String TAG = "CustomWorkoutFragment";

    private CustomWorkoutItemAdapter customWorkoutItemAdapter;
    private List<CustomWorkoutItem> mCustomWorkoutItems;
    private PillBox[] valueSelectorWidgets;
    private CustomWorkoutGraph customWorkoutGraph;
    private CheckBox includeCooldownCheckbox;
    private List<WorkoutParameter> workoutParameters;
    private TextView totalTimeTextView;
    private View header;
    private View backButton;

    private TextView parameter1Value1;
    private TextView parameter1Value2;
    private TextView parameter1Value3;
    private TextView parameter1Value4;
    private View parameter1Color;
    private TextView parameter1Title;

    private TextView parameter2Value1;
    private TextView parameter2Value2;
    private TextView parameter2Value3;
    private TextView parameter2Value4;
    private View parameter2Color;
    private TextView parameter2Title;

    private TextView unableToSaveWorkoutTextView;
    private ImageView notConnectedIcon;

    private AppCompatEditText titleEditText;

    private View mLoadingOverlayView;

    private boolean mCooldownOptionality = false;
    private boolean mIsGuestUser = false;
    private boolean mIsOfflineUser = false;
    private boolean mIsNetworkConnected = false;
    private boolean mIsLoading = false;

    private boolean mUseBackButton;

    public interface ICustomWorkoutInterface {
        void saveWorkouts(List<CustomWorkoutItem> customWorkoutXmls);
        void onStartCustomWorkout(CustomWorkoutItem workoutXml);
        void onBackButtonShow(boolean isShown);
        void onBackButtonClicked();
        void onUnableToSaveShow(boolean isShown); // on 10 inch, use the message center for unable to save message
    }

    private ICustomWorkoutInterface customWorkoutInterface;
    private long pressTime = 0;


    public interface OnCreateViewListener {
        void onCreateView(View view);
    }
    public OnCreateViewListener onCreateViewListener;

    public interface CustomWorkoutSelectionListener {
        void customWorkoutSelected(CustomWorkoutItem item);
    }
    public CustomWorkoutSelectionListener customWorkoutSelectionListener;
    public interface CustomWorkoutRemovedListener {
        void customWorkoutRemoved(CustomWorkoutItem item);
    }
    public CustomWorkoutRemovedListener customWorkoutRemovedListener;

    protected void cooldownOptionalityUpdated() {
        if (includeCooldownCheckbox != null) {
            includeCooldownCheckbox.setVisibility(mCooldownOptionality ? View.VISIBLE : View.GONE);
        }
    }
    protected void cooldownClickHandler(View v, boolean checked) {
        CustomWorkoutItem cwi = getSelectedItem();
        if (cwi != null) {
            cwi.hasCooldown = checked;
        }
    }
    protected void cooldownSetWithoutEvent(boolean checked) {
        includeCooldownCheckbox.setOnCheckedChangeListener(null);
        includeCooldownCheckbox.setChecked(checked);
        includeCooldownCheckbox.setOnCheckedChangeListener(this::cooldownClickHandler);
    }

    public CustomWorkoutFragment() {
        this(false);
    }
    public CustomWorkoutFragment(boolean useBackButton) {
        super();
        mUseBackButton = useBackButton;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        // It is recommended to only inflate in onCreateView and operate within onViewCreated according to docs ?
        return inflater.inflate(R.layout.custom_workout_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView mRecyclerView = view.findViewById(R.id.custom_workout_recycler_view);

        parameter1Title = view.findViewById(R.id.custom_workout_parameter_1_text);
        parameter1Color = view.findViewById(R.id.custom_workout_parameter_1_color);
        parameter1Value1 = view.findViewById(R.id.custom_workout_parameter_1_value_1);
        parameter1Value2 = view.findViewById(R.id.custom_workout_parameter_1_value_2);
        parameter1Value3 = view.findViewById(R.id.custom_workout_parameter_1_value_3);
        parameter1Value4 = view.findViewById(R.id.custom_workout_parameter_1_value_4);

        parameter2Title = view.findViewById(R.id.custom_workout_parameter_2_text);
        parameter2Color = view.findViewById(R.id.custom_workout_parameter_2_color);
        parameter2Value1 = view.findViewById(R.id.custom_workout_parameter_2_value_1);
        parameter2Value2 = view.findViewById(R.id.custom_workout_parameter_2_value_2);
        parameter2Value3 = view.findViewById(R.id.custom_workout_parameter_2_value_3);
        parameter2Value4 = view.findViewById(R.id.custom_workout_parameter_2_value_4);
        setParameter2Visibility(View.INVISIBLE);

        valueSelectorWidgets = new PillBox[3];
        valueSelectorWidgets[0] = view.findViewById(R.id.custom_workout_value_selector_1);
        valueSelectorWidgets[1] = view.findViewById(R.id.custom_workout_value_selector_2);
        valueSelectorWidgets[2] = view.findViewById(R.id.custom_workout_value_selector_3);

        titleEditText = view.findViewById(R.id.title_edit_text);
        includeCooldownCheckbox = view.findViewById(R.id.include_cooldown_checkbox);
        totalTimeTextView = view.findViewById(R.id.total_time_text_view);
        unableToSaveWorkoutTextView = view.findViewById(R.id.unable_to_save_workout_text_view);
        notConnectedIcon = view.findViewById(R.id.no_wifi_icon);
        mLoadingOverlayView = view.findViewById(R.id.loading_overlay_view);
        setLoading(mIsLoading);

        PillBox.IValueSelectorInterface iValueSelectorInterface = (value, identifier) -> {
            customWorkoutGraph.updateParameter(value, identifier);
            updateTotalTime();
        };

        for (PillBox valueSelectorWidget : valueSelectorWidgets) {
            valueSelectorWidget.setiValueSelectorInterface(iValueSelectorInterface);
        }

        customWorkoutGraph = view.findViewById(R.id.custom_workout_graph);

        View deleteSegmentButton = view.findViewById(R.id.custom_workout_segment_delete);
        deleteSegmentButton.setOnClickListener(v -> {
            if (customWorkoutGraph.numColumns == 1) return;
            MessageBoxDialog dialog = new MessageBoxDialog(v.getContext(), R.layout.custom_workout_confirmation_dialog);
            dialog.setTitle(
                    getString(R.string.delete_segment_tc) + " " + customWorkoutGraph.getSelectedColumn()
            );
            dialog.setMessage(getString(R.string.delete_segment_pg));
            dialog.setButtonsText(
                    getString(R.string.cancel_tc),
                    getString(R.string.delete_segment_tc)
            );
            dialog.setButton0OnClickListener(v2 -> dialog.closeMessageBox());
            dialog.setButton1OnClickListener(v2 -> {
                dialog.closeMessageBox();
                customWorkoutGraph.deleteSegment();
                updateTotalTime();
                if (customWorkoutGraph.numColumns == 1) {
                    deleteSegmentButton.setActivated(true);
                    deleteSegmentButton.setClickable(false);
                }
            });
            dialog.show();
        });

        customWorkoutGraph.setCustomWorkoutGraphCallback(new CustomWorkoutGraph.ICustomWorkoutGraph() {
            @Override
            public void intervalSelected(List<Float> parameterValues) {
                setWorkoutParameterValues(parameterValues);
            }

            @Override
            public void canDeleteSegment(boolean canDelete) {
                deleteSegmentButton.setActivated(!canDelete);
                deleteSegmentButton.setClickable(canDelete);
            }
        });

        AppCompatImageButton previousIndexButton = view.findViewById(R.id.custom_workout_previous_index);
        previousIndexButton.setOnClickListener(v -> customWorkoutGraph.previousIndex());
        AppCompatImageButton nextIndexButton = view.findViewById(R.id.custom_workout_next_index);
        nextIndexButton.setOnClickListener(v -> customWorkoutGraph.nextIndex());

        View resetSegmentButton = view.findViewById(R.id.custom_workout_segment_reset);
        resetSegmentButton.setOnClickListener(v -> {
            MessageBoxDialog dialog = new MessageBoxDialog(v.getContext(), R.layout.custom_workout_confirmation_dialog);
            dialog.setTitle(
                    getString(R.string.reset_segment_tc) + " " + customWorkoutGraph.getSelectedColumn()
            );
            dialog.setMessage(getString(R.string.reset_segment_pg));
            dialog.setButtonsText(
                    getString(R.string.cancel_tc),
                    getString(R.string.reset_segment_tc)
            );
            dialog.setButton0OnClickListener(v2 -> dialog.closeMessageBox());
            dialog.setButton1OnClickListener(v2 -> {
                dialog.closeMessageBox();
                customWorkoutGraph.resetSegment();
                updateTotalTime();
            });
            dialog.show();
        });

        Button startButton = view.findViewById(R.id.start_button);
        startButton.setOnClickListener(v -> {
            long uptime = SystemClock.uptimeMillis();
            CustomWorkoutItem cwi = getSelectedItem();
            if (pressTime < uptime - 1500 && cwi != null) {
                pressTime = uptime;
                customWorkoutInterface.onStartCustomWorkout(cwi);
            }
        });

        backButton = view.findViewById(R.id.back_button);
        //backButton.setOnClickListener(this::onBackButton);
        header = view.findViewById(R.id.custom_workout_header); // use whole header to be easier to click on smaller screens
        header.setOnClickListener(this::onBackButton);

        titleEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                Editable titleEdit = titleEditText.getText();
                CustomWorkoutItem cwi = getSelectedItem();
                if (titleEdit != null && cwi != null) {
                    cwi.title = titleEdit.toString();
                }

                customWorkoutItemAdapter.notifyDataSetChanged();
            }
        });
        titleEditText.setSelectAllOnFocus(true);

        titleEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                titleEditText.clearFocus();
                hideKeyboard(getActivity());
            }
            return false;
        });

        updateGuestUserLayout(mIsGuestUser);
        setCustomWorkoutParameters(workoutParameters);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        customWorkoutItemAdapter = new CustomWorkoutItemAdapter(mCustomWorkoutItems, customWorkoutAdapter, mIsGuestUser);

        mRecyclerView.setAdapter(customWorkoutItemAdapter);
        mRecyclerView.setItemAnimator(null);

        cooldownOptionalityUpdated();
    }

    private void onBackButton(View v) {
        if (mUseBackButton && this.customWorkoutInterface != null) this.customWorkoutInterface.onBackButtonClicked();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() instanceof GoScreenActivity) {
            DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.CUSTOM_WORKOUTS_SCREEN.name());
        }

        if (mUseBackButton) {
            backButton.setVisibility(View.VISIBLE);
            if (customWorkoutInterface != null) customWorkoutInterface.onBackButtonShow(true);
        }

        // now that view is created, select an item if not already selected
        if (onCreateViewListener != null) onCreateViewListener.onCreateView(getView());
        if (customWorkoutItemAdapter.getSelectedPosition() < 0) customWorkoutItemAdapter.selectFirstItem();
    }

    public void selectCustomWorkout(String uuid) {
        for (int i = 0; i < mCustomWorkoutItems.size(); ++i) {
            if (mCustomWorkoutItems.get(i).uuid != null && mCustomWorkoutItems.get(i).uuid.toString().equals(uuid)) {
                customWorkoutItemAdapter.selectItem(i);
                return;
            }
        }
    }

    private CustomWorkoutItemAdapter.ICustomWorkoutAdapter customWorkoutAdapter = new CustomWorkoutItemAdapter.ICustomWorkoutAdapter() {
        @Override
        public void loadWorkoutAtPosition(int position) {
            CustomWorkoutItem workout = new CustomWorkoutItem(mCustomWorkoutItems.get(position)); // make a copy in case it mutates while ui is posting
            String title;
            // There is a chance that this is called when the fragment is no longer attached.
            try {
                title = workout.title.isEmpty() ? ContextUtil.getString(R.string.my_workout_tc) : workout.title;
            } catch(Exception ex) {
                return;
            }
            titleEditText.setText(title);
            cooldownSetWithoutEvent(workout.hasCooldown); // this also does strange things when outside of the post
            customWorkoutGraph.loadWorkout(workoutParameters, workout.intervalData);
            updateTotalTime();
            if (customWorkoutSelectionListener != null) customWorkoutSelectionListener.customWorkoutSelected(workout);
        }

        @Override
        public void addWorkout() {
            addDefaultWorkout();
        }

        @Override
        public void removeWorkout(CustomWorkoutItem item) {
            if (customWorkoutRemovedListener != null) customWorkoutRemovedListener.customWorkoutRemoved(item);
        }
    };

    private void updateTotalTime() {
        int totalSeconds = Math.round(customWorkoutGraph.computeTotalTime() * 60); // segment time is always minutes, but seconds can be represented as fractional component
        if (totalSeconds > 0) {
            String timeStr = TimeFormat.formatSeconds(totalSeconds);
            totalTimeTextView.setText(timeStr);
            totalTimeTextView.setVisibility(View.VISIBLE);
        } else {
            totalTimeTextView.setVisibility(View.GONE);
        }
    }

    private @Nullable
    CustomWorkoutItem getSelectedItem() {
        if (customWorkoutItemAdapter == null) return null;
        int position = customWorkoutItemAdapter.getSelectedPosition();
        return mCustomWorkoutItems.get(position);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (customWorkoutInterface != null) {
            if (mUseBackButton) customWorkoutInterface.onBackButtonShow(false);
            customWorkoutInterface.onUnableToSaveShow(false);

            if (!mIsLoading && !mIsGuestUser) {
                customWorkoutInterface.saveWorkouts(mCustomWorkoutItems);
            }
        }
    }

    public void setCooldownOptionality(boolean optional) {
        this.mCooldownOptionality = optional;
        this.cooldownOptionalityUpdated();
    }

    public void setCustomWorkoutInterface(ICustomWorkoutInterface iCustomWorkoutSelectionInterface) {
        this.customWorkoutInterface = iCustomWorkoutSelectionInterface;
    }

    public void setCustomWorkoutItems(Context context, List<WorkoutParameter> customWorkoutParameters, List<CustomWorkoutItem> customWorkoutItems) {
        setLoading(false);
        this.workoutParameters = customWorkoutParameters;
        setCustomWorkoutParameters(workoutParameters);

        if (mCustomWorkoutItems == null) {
            mCustomWorkoutItems = new ArrayList<>();
        }

        mCustomWorkoutItems.clear();
        if (customWorkoutItems != null) mCustomWorkoutItems.addAll(customWorkoutItems);

        if (mCustomWorkoutItems.isEmpty()) {
            addDefaultWorkout();
        }

        //this.mCustomWorkoutItems.add(0, new CustomWorkoutItem(context.getString(R.string.custom_workouts_tc), UUID.randomUUID(), GoScreenWorkoutDisplayItem.CATEGORY_TITLE_TYPE, true));

        if (!mIsGuestUser) {
            this.mCustomWorkoutItems.add(new CustomWorkoutItem(context.getString(R.string.new_exercise_tc), UUID.randomUUID(), GoScreenWorkoutDisplayItem.CATEGORY_SUBTITLE_TITLE, true));
        }

        if (customWorkoutItemAdapter != null) {
            customWorkoutItemAdapter.notifyDataSetChanged();
            customWorkoutItemAdapter.selectFirstItem();
        }

    }

    public void setCustomWorkoutLoading(Context context) {
        setLoading(true);
        if (this.mCustomWorkoutItems == null) {
            mCustomWorkoutItems = new ArrayList<>();
        }
        this.mCustomWorkoutItems.clear();
        //this.mCustomWorkoutItems.add(new CustomWorkoutItem(context.getString(R.string.custom_workouts_tc), UUID.randomUUID(),GoScreenWorkoutDisplayItem.CATEGORY_TITLE_TYPE, true));
        this.mCustomWorkoutItems.add(new CustomWorkoutItem("", UUID.randomUUID(), GoScreenWorkoutDisplayItem.LOADING_TYPE, true));
        if (customWorkoutItemAdapter != null) customWorkoutItemAdapter.notifyDataSetChanged();
    }

    private void setLoading(boolean isLoading) {
        mIsLoading = isLoading;
        if (mLoadingOverlayView != null) {
            mLoadingOverlayView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private void setCustomWorkoutParameters(List<WorkoutParameter> workoutParameter) {
        if (workoutParameter != null && valueSelectorWidgets != null) {
            for (int i = 0; i < valueSelectorWidgets.length; i++) {
                if (i < workoutParameter.size()) {
                    WorkoutParameter wp = new WorkoutParameter(workoutParameter.get(i));
                    if (wp.identifier().equals("TIME_GOAL_START")) {
                        wp.isTime(true);
                        wp.resolution(5.0 / 60); // change step to 5 sec per PM
                        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
                        if (config.WATTS_INACCURATE) {
                            wp.min(1); // for ECB resistance, minimum is 1 minute to avoid a lot of ECB value changing
                        } else {
                            wp.min(10.0 / 60); // PM wants 10 sec minimum
                        }
                        // original request was 3 separate labels in the dialog centered under each field, however this is much easier as long as no one complains
                        String units = ContextUtil.getString(R.string.two_digit_hours_uc) + " : " //NON-NLS
                                + ContextUtil.getString(R.string.two_digit_minutes_uc) + " : " //NON-NLS
                                + ContextUtil.getString(R.string.two_digit_seconds_uc); //NON-NLS
                        wp.units(units);
                    } else {
                        setParameterLabels(wp);
                    }
                    valueSelectorWidgets[i].initParameterSettings(wp);
                } else {
                    valueSelectorWidgets[i].setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private void setWorkoutParameterValues(List<Float> parameterValues) {
        for (int i = 0; i < valueSelectorWidgets.length; i++) {
            if (i < parameterValues.size() && i < workoutParameters.size()) {
                valueSelectorWidgets[i].setValue(parameterValues.get(i));
            }
        }
    }

    private void addDefaultWorkout() {
        Map<String,List<Float>> customWorkoutData = new HashMap<>();
        for (WorkoutParameter workoutParameter : workoutParameters) {
            List<Float> initialValues = new ArrayList<>();
            int defaultIntervals = 1;
            for (int i = 0; i < defaultIntervals; i++) {
                initialValues.add(workoutParameter.defaultValue());
            }
            customWorkoutData.put(workoutParameter.identifier(), initialValues);
        }
        mCustomWorkoutItems.add(Math.max(mCustomWorkoutItems.size() - 1, 0), new CustomWorkoutItem("", UUID.randomUUID(), customWorkoutData, true));
    }

    private void setParameterLabels(WorkoutParameter workoutParameter) {
        double value1 = workoutParameter.max();
        double value2 = (Math.round(workoutParameter.max() * 0.66 / workoutParameter.resolution()) * workoutParameter.resolution());
        double value3 = (Math.round(workoutParameter.max() * 0.33 / workoutParameter.resolution()) * workoutParameter.resolution());
        double value4 = workoutParameter.min();

        if (parameter1Title.getText().equals("")) {
            parameter1Title.setText(workoutParameter.label());
            parameter1Color.setBackgroundColor(workoutParameter.graphColor);
            parameter1Value1.setText(LanguageHelper.getDecimalFormat(workoutParameter.resolution()).format(value1));
            parameter1Value2.setText(LanguageHelper.getDecimalFormat(workoutParameter.resolution()).format(value2));
            parameter1Value3.setText(LanguageHelper.getDecimalFormat(workoutParameter.resolution()).format(value3));
            parameter1Value4.setText(LanguageHelper.getDecimalFormat(workoutParameter.resolution()).format(value4));
        } else if (parameter2Title.getText().equals("")) {
            setParameter2Visibility(View.VISIBLE);
            parameter2Title.setText(workoutParameter.label());
            parameter2Color.setBackgroundColor(workoutParameter.graphColor);
            parameter2Value1.setText(LanguageHelper.getDecimalFormat(workoutParameter.resolution()).format(value1));
            parameter2Value2.setText(LanguageHelper.getDecimalFormat(workoutParameter.resolution()).format(value2));
            parameter2Value3.setText(LanguageHelper.getDecimalFormat(workoutParameter.resolution()).format(value3));
            parameter2Value4.setText(LanguageHelper.getDecimalFormat(workoutParameter.resolution()).format(value4));
        }
    }

    private void setParameter2Visibility(int visibility) {
        parameter2Title.setVisibility(visibility);
        parameter2Color.setVisibility(visibility);
        parameter2Value1.setVisibility(visibility);
        parameter2Value2.setVisibility(visibility);
        parameter2Value3.setVisibility(visibility);
        parameter2Value4.setVisibility(visibility);
    }

    private void updateUnableToSave() {
        boolean isShown = (!mIsNetworkConnected && !mIsGuestUser && !mIsOfflineUser);
        //boolean isShown = true; // TEST
        int v = (isShown ? View.VISIBLE : View.INVISIBLE);
        if (getView() != null) { // do nothing if view is not created
            if (unableToSaveWorkoutTextView != null) { // view created AND has this element
                unableToSaveWorkoutTextView.setVisibility(v);
            } else { // view created AND does NOT have this element
                if (customWorkoutInterface != null) customWorkoutInterface.onUnableToSaveShow(isShown);
            }
            if (notConnectedIcon != null) notConnectedIcon.setVisibility(v);
        }
    }

    public void updateOfflineUser(boolean isOfflineUser) {
        mIsOfflineUser = isOfflineUser;
        updateUnableToSave();
    }
    public void updateGuestUserLayout(boolean isGuestUser) {
        mIsGuestUser = isGuestUser;

        if (titleEditText != null) {

            if (isGuestUser) {
                titleEditText.setVisibility(View.GONE);
            } else {
                titleEditText.setVisibility(View.VISIBLE);

                Context context = getContext();
                if (context != null && workoutParameters != null && mCustomWorkoutItems != null && !mIsLoading) {
                    addWorkoutAdditionButton(context);
                }
            }
        }

        updateUnableToSave();

        if (customWorkoutItemAdapter != null) {
            customWorkoutItemAdapter.setGuest(mIsGuestUser);
        }
    }

    private void addWorkoutAdditionButton(Context context) {
        boolean hasNewExerciseButton = false;
        for (CustomWorkoutItem customWorkoutItem : mCustomWorkoutItems) {
            if (customWorkoutItem.categoryType == GoScreenWorkoutDisplayItem.CATEGORY_SUBTITLE_TITLE) {
                hasNewExerciseButton = true;
                break;
            }
        }

        if (!hasNewExerciseButton) {
            this.mCustomWorkoutItems.add(new CustomWorkoutItem(context.getString(R.string.new_exercise_tc), UUID.randomUUID(), GoScreenWorkoutDisplayItem.CATEGORY_SUBTITLE_TITLE, true));
            if (customWorkoutItemAdapter != null) {
                customWorkoutItemAdapter.notifyDataSetChanged();
            }
        }
    }

    public void setNetworkConnectedStatus(boolean networkConnected) {
        mIsNetworkConnected = networkConnected;
        updateUnableToSave();
    }

    public List<CustomWorkoutItem> getWorkoutItems() {
        return mCustomWorkoutItems;
    }

}
