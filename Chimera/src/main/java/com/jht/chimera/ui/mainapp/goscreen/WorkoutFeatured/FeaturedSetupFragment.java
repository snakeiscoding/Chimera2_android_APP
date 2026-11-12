package com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.jht.androiduiwidgets.pillbox.PillBox;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.ui.mainapp.goscreen.BitmapLoader;
import com.jht.chimera.ui.mainapp.goscreen.GoScreenBaseFragment;
import com.jht.chimera.ui.mainapp.goscreen.MigrosLevelRadioButton;
import com.jht.chimera.ui.mainapp.goscreen.MinMaxParameterLogic;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutParameter;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutSetup.WorkoutSelectionFragment;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;

import java.util.List;

/**
 * Created by AlexN on 1/8/2019.
 */

public class FeaturedSetupFragment extends GoScreenBaseFragment {

    private FeaturedItem featuredWorkoutItem;
    private List<WorkoutParameter> featuredParameters;
    private IFeaturedSetupFragment iFeaturedSetupFragment;
    private PillBox[] valueSelectorWidgets;
    private MigrosLevelRadioButton migrosLevelRadioButton;
    private ConstraintLayout featuredSetupBaseLayout;
    private ImageView goButtonScrollImageView;
    private ScrollView scrollView;
    private ConstraintLayout scrollLayout;

    private long pressTime = 0;

    private DataMatrix dataMatrix = new DataMatrix();

    public interface IFeaturedSetupFragment {
        void onFeaturedWorkoutStart(String workoutIdentifier, List<WorkoutParameter> parameters);
        void onFeaturedWorkoutGetParameters(String workoutIdentifier);
    }

    @NonNls private static String WEIGHT_VARIABLE = "WEIGHT";

    @Override
    public void onResume() {
        super.onResume();
        dataMatrix.createFloatAdapter(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT, weight -> {
            for(int i = 0; i < valueSelectorWidgets.length; i++){
                if(valueSelectorWidgets[i].parameterSettings != null && valueSelectorWidgets[i].parameterSettings.label().equalsIgnoreCase(WEIGHT_VARIABLE)) {
                    boolean isMetric = DataMatrix.getBoolean(WorkoutSelectionFragment.GoScreenKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, true);
                    valueSelectorWidgets[i].setValue(Math.round(isMetric ? (weight) : (weight / 0.453592f)));
                }
            }
        }, true);
    }

    @Override
    public void onPause() {
        super.onPause();
        dataMatrix.finish();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.featured_setup_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        featuredSetupBaseLayout = view.findViewById(R.id.featured_setup_layout);
        ImageView featuredSetupBackground = view.findViewById(R.id.featured_setup_background);
        TextView featuredSetupTitle = view.findViewById(R.id.featured_setup_title);
        TextView featuredSetupSubTitle = view.findViewById(R.id.featured_setup_sub_title);
        TextView featuredSetupSubTitle2 = view.findViewById(R.id.featured_setup_sub_title_2);
        TextView featuredSetupDescription = view.findViewById(R.id.featured_setup_description);

        migrosLevelRadioButton = view.findViewById(R.id.migros_button);

        valueSelectorWidgets = new PillBox[4];
        valueSelectorWidgets[0] = view.findViewById(R.id.featured_setup_value_selector_1);
        valueSelectorWidgets[1] = view.findViewById(R.id.featured_setup_value_selector_2);
        valueSelectorWidgets[2] = view.findViewById(R.id.featured_setup_value_selector_3);
        valueSelectorWidgets[3] = view.findViewById(R.id.featured_setup_value_selector_4);

        // Adjust the go button bg to be the full size so it lines up correctly with the normal diagonal bg
        goButtonScrollImageView = view.findViewById(R.id.go_button_scroll_bg);
        scrollView = view.findViewById(R.id.scroll_view);
        scrollLayout = view.findViewById(R.id.scroll_layout);
        ViewTreeObserver observer = scrollView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(() -> {
            int contentHeight = scrollLayout.getHeight();
            boolean isScrollable = scrollView.getHeight() < contentHeight + scrollView.getPaddingTop() + scrollView.getPaddingBottom();
            if (isScrollable) {
                goButtonScrollImageView.setVisibility(View.VISIBLE);
            } else {
                goButtonScrollImageView.setVisibility(View.INVISIBLE);
            }
        });
        setFeaturedParameters();

        Button featuredStartButton = view.findViewById(R.id.start_button);
        featuredStartButton.setOnClickListener(v -> {
            long uptime = SystemClock.uptimeMillis();
            if (pressTime < uptime - 1500) {
                pressTime = uptime;
                iFeaturedSetupFragment.onFeaturedWorkoutStart(featuredWorkoutItem.identifier, featuredParameters);
            }
        });

        BitmapLoader.loadSampledBitmap(getActivity(), featuredSetupBackground, featuredWorkoutItem.getSafeBackgroundItem(0));
        featuredSetupTitle.setText(featuredWorkoutItem.title);
        featuredSetupSubTitle.setText(featuredWorkoutItem.subtitle);
        featuredSetupSubTitle2.setText(featuredWorkoutItem.subtitle2);
        featuredSetupDescription.setText(featuredWorkoutItem.description);

        if (featuredSetupSubTitle.getText().toString().equals("")) {
            featuredSetupSubTitle.setVisibility(View.GONE);
        }

        if (featuredSetupSubTitle2.getText().toString().equals("")) {
            featuredSetupSubTitle2.setVisibility(View.GONE);
        }
    }



    void loadFeaturedWorkoutInfo(FeaturedItem featuredItem) {
        featuredWorkoutItem = featuredItem;
        iFeaturedSetupFragment.onFeaturedWorkoutGetParameters(featuredItem.identifier);
    }

    public void setiFeaturedSetupFragment(IFeaturedSetupFragment iFeaturedSetupFragment) {
        this.iFeaturedSetupFragment = iFeaturedSetupFragment;
    }

    @Override
    public void setParameterValues(List<WorkoutParameter> inputConfigurationList) {
        featuredParameters = inputConfigurationList;
        setFeaturedParameters();
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

    private boolean hasLevelInput() {
        for (WorkoutParameter parameter : featuredParameters) {
            if (parameter.identifier().equals("LEVEL")) {
                return true;
            }
        }

        return false;
    }

    private void alignLevelButton(View viewToAlignWith) {
        ConstraintSet set = new ConstraintSet();
        set.clone(featuredSetupBaseLayout);
        set.connect(migrosLevelRadioButton.getId(), ConstraintSet.START, viewToAlignWith.getId(), ConstraintSet.START, 0);
        set.connect(migrosLevelRadioButton.getId(), ConstraintSet.TOP, viewToAlignWith.getId(), ConstraintSet.TOP, 0);
        set.applyTo(featuredSetupBaseLayout);
    }


    private void setFeaturedParameters() {
        if (featuredParameters != null && valueSelectorWidgets != null && isAdded()) {
            if (featuredWorkoutItem.identifier.contains("migros")) {
                boolean firstInvisibleItem = true;
                WorkoutParameter levelParameter = null;


                int workoutParameterIndex = 0;
                for (int i = 0; i < valueSelectorWidgets.length; i++) {
                    // Gender variable is a special case that needs to be handled specially at the end, skip it
                    if (i < featuredParameters.size()) {
                        if (featuredParameters.get(i).identifier().equals("LEVEL")) {
                            levelParameter = featuredParameters.get(i);
                            migrosLevelRadioButton.setLevelParameter(levelParameter);
                            workoutParameterIndex++;
                        }
                    }

                    if (workoutParameterIndex < featuredParameters.size()) {
                        valueSelectorWidgets[i].initParameterSettings(featuredParameters.get(workoutParameterIndex));
                        valueSelectorWidgets[i].setVisibility(View.VISIBLE);
                        valueSelectorWidgets[i].setiValueSelectorInterface((value, identifier) -> MinMaxParameterLogic.handleValueUpdated(value, identifier, valueSelectorWidgets));
                    } else {
                        valueSelectorWidgets[i].setVisibility(View.INVISIBLE);
                        if (firstInvisibleItem) {
                            firstInvisibleItem = false;

                            // Show gender input at the end over the top of the first invisible pill box
                            if (hasLevelInput() && levelParameter != null) {
                                alignLevelButton(valueSelectorWidgets[i]);
                                migrosLevelRadioButton.setVisibility(View.VISIBLE);
                            } else {
                                migrosLevelRadioButton.setVisibility(View.INVISIBLE);
                            }
                        }
                    }
                    workoutParameterIndex++;
                }
            } else {
                for (int i = 0; i < valueSelectorWidgets.length; i++) {
                    if (i >= featuredParameters.size()) {
                        valueSelectorWidgets[i].setVisibility(View.INVISIBLE);
                    } else {
                        valueSelectorWidgets[i].setVisibility(View.VISIBLE);
                        valueSelectorWidgets[i].initParameterSettings(featuredParameters.get(i));
                        valueSelectorWidgets[i].setiValueSelectorInterface((value, identifier) ->
                                MinMaxParameterLogic.handleValueUpdated(value, identifier, valueSelectorWidgets)
                        );
                    }
                }
            }
        }
    }
}
