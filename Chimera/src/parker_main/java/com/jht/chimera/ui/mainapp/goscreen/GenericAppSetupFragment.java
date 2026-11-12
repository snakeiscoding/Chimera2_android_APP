package com.jht.chimera.ui.mainapp.goscreen;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.androiduiwidgets.pillbox.PillBox;
import com.jht.chimera.R;
import com.jht.chimera.lib.workout.FavoriteIcon;
import com.jht.chimera.lib.workout.WorkoutStartupParams;
import com.jht.chimera.lib.workoutapps.AppIcon;
import com.jht.chimera.lib.workoutapps.AppIconManager;

import java.util.List;

public class GenericAppSetupFragment extends GoScreenBaseFragment {

    private ImageView setupBackground;
    private TextView setupTitle;
    private TextView setupDescription;
    private Button startButton;
    private PillBox[] valueSelectorWidgets;
    private GoScreenWorkoutDisplayItem workoutItem;
    private List<WorkoutParameter> parameters;
    private ISetupInterface iSetupInterface;

    private long pressTime = 0;

    public interface ISetupInterface {
        void onWorkoutStart(String workoutIdentifier, List<WorkoutParameter> parameters);

        void onGetParameters(String workoutIdentifier);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.generic_app_setup_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupBackground = view.findViewById(R.id.setup_background);
        setupTitle = view.findViewById(R.id.setup_title);
        setupDescription = view.findViewById(R.id.setup_description);

        valueSelectorWidgets = new PillBox[4];
        valueSelectorWidgets[0] = view.findViewById(R.id.setup_value_selector_1);
        valueSelectorWidgets[1] = view.findViewById(R.id.setup_value_selector_2);
        valueSelectorWidgets[2] = view.findViewById(R.id.setup_value_selector_3);
        valueSelectorWidgets[3] = view.findViewById(R.id.setup_value_selector_4);

        setParameters();

        startButton = view.findViewById(R.id.start_button);
        startButton.setOnClickListener(v -> {
            long uptime = SystemClock.uptimeMillis();
            if (pressTime < uptime - 1000) {
                pressTime = uptime;
                iSetupInterface.onWorkoutStart(workoutItem.identifier, parameters);
            }
        });

        // TODO we don't know how we are getting the images yet so hacking the description
        String appUuid = null;
        int workoutId = 0;
        try {
            appUuid = workoutItem.description;
            AppIcon appIcon = AppIconManager.getInstance().getAppIcon(appUuid);
            if (appIcon != null) {
                setupBackground.setImageDrawable(appIcon.getAppIcon());
            }

            workoutId = Integer.parseInt(workoutItem.identifier);
        } catch (Exception e) {
        }

        //setupBackground.setImageURI(workoutItem.backgroundPath); // this will load vector drawable correctly
        //BitmapLoader.loadSampledBitmap(getActivity(), setupBackground, workoutItem.backgroundPath);
        setupTitle.setText(workoutItem.title);
        //setupDescription.setText(workoutItem.description);

        // update favorite icon
        FavoriteIcon.populateIcon(view, WorkoutStartupParams.forProgramTypeAndApp(workoutId, appUuid));
    }

    public void loadWorkoutInfo(GoScreenWorkoutDisplayItem workoutItem) {
        this.workoutItem = workoutItem;
        iSetupInterface.onGetParameters(workoutItem.identifier);
    }

    public void setSetupInterface(ISetupInterface iSetupInterface) {
        this.iSetupInterface = iSetupInterface;
    }

    @Override
    public void setParameterValues(List<WorkoutParameter> inputConfigurationList) {
        super.setParameterValues(inputConfigurationList);
        parameters = inputConfigurationList;
        setParameters();
    }

    private void setParameters() {
        if (parameters != null && valueSelectorWidgets != null && isAdded()) {
            for (int i = 0; i < valueSelectorWidgets.length; i++) {
                if (i >= parameters.size()) {
                    valueSelectorWidgets[i].setVisibility(View.INVISIBLE);
                } else {
                    valueSelectorWidgets[i].setVisibility(View.VISIBLE);
                    valueSelectorWidgets[i].initParameterSettings(parameters.get(i));
                }
            }
        }
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
}
