package com.jht.chimera.ui.mainapp.goscreen.VA;

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
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.ui.mainapp.goscreen.BitmapLoader;
import com.jht.chimera.ui.mainapp.goscreen.GoScreenBaseFragment;
import com.jht.chimera.ui.mainapp.goscreen.GoScreenWorkoutDisplayItem;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutParameter;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutSetup.WorkoutSelectionFragment;
import com.jht.datamatrixlib.DataMatrix;

import java.util.List;

/**
 * Created by AlexN on 1/8/2019.
 */

public class VASetupFragment extends GoScreenBaseFragment {

    private ImageView vaSetupBackground;
    private TextView vaSetupTitle;
    private TextView vaSetupDescription;
    private Button vaSetupBackButton;
    private Button vaStartButton;
    private PillBox[] valueSelectorWidgets;
    private GoScreenWorkoutDisplayItem vaWorkoutItem;
    private List<WorkoutParameter> vaParameters;
    private IVASetupFragment ivaSetupFragment;
    private boolean showBackButton = true;
    private DataMatrix dataMatrix = new DataMatrix();

    private long pressTime = 0;

    public interface IVASetupFragment{
        void onVAWorkoutStart(String workoutIdentifier, List<WorkoutParameter> parameters);
        void onVAGetParameters(String workoutIdentifier);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.va_setup_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vaSetupBackground = view.findViewById(R.id.va_setup_background);
        vaSetupTitle = view.findViewById(R.id.va_setup_title);
        vaSetupDescription = view.findViewById(R.id.va_setup_description);
        vaSetupBackButton = view.findViewById(R.id.va_setup_back_button);
        vaSetupBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getParentFragment() != null) {
                    ((VASelectionFragment) getParentFragment()).closeVaSetupFragment();
                }
            }
        });
        if(!showBackButton){
            vaSetupBackButton.setVisibility(View.GONE);
            view.findViewById(R.id.va_setup_back_text).setVisibility(View.GONE);
            view.findViewById(R.id.va_setup_back_arrow).setVisibility(View.GONE);
        }
        else{
            vaSetupBackButton.setVisibility(View.VISIBLE);
            view.findViewById(R.id.va_setup_back_text).setVisibility(View.VISIBLE);
            view.findViewById(R.id.va_setup_back_arrow).setVisibility(View.VISIBLE);
        }

        valueSelectorWidgets = new PillBox[4];
        valueSelectorWidgets[0] = view.findViewById(R.id.va_setup_value_selector_1);
        valueSelectorWidgets[1] = view.findViewById(R.id.va_setup_value_selector_2);
        valueSelectorWidgets[2] = view.findViewById(R.id.va_setup_value_selector_3);
        valueSelectorWidgets[3] = view.findViewById(R.id.va_setup_value_selector_4);

        setVaParameters();

        vaStartButton = view.findViewById(R.id.start_button);
        vaStartButton.setOnClickListener(v -> {
            long uptime = SystemClock.uptimeMillis();
            if (pressTime < uptime - 1000) {
                pressTime = uptime;
                ivaSetupFragment.onVAWorkoutStart(vaWorkoutItem.identifier, vaParameters);
            }
        });

        BitmapLoader.loadSampledBitmap(getActivity(), vaSetupBackground, vaWorkoutItem.backgroundPath);
        vaSetupTitle.setText(vaWorkoutItem.title);
        vaSetupDescription.setText(vaWorkoutItem.description);
    }
    private static String WEIGHT_VARIABLE = "WEIGHT"; //NON-NLS

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

    public void loadVAWorkoutInfo(GoScreenWorkoutDisplayItem workoutItem) {
        vaWorkoutItem = workoutItem;
        ivaSetupFragment.onVAGetParameters(workoutItem.identifier);
    }

    public void setIVASetupFragment(IVASetupFragment ivaSetupFragment){
        this.ivaSetupFragment = ivaSetupFragment;
    }

    public void hideBackButton(){
        showBackButton = false;
    }

    @Override
    public void setParameterValues(List<WorkoutParameter> inputConfigurationList) {
        super.setParameterValues(inputConfigurationList);
        vaParameters = inputConfigurationList;
        setVaParameters();
    }

    private void setVaParameters(){
        if(vaParameters != null && valueSelectorWidgets != null && isAdded()) {
            for (int i = 0; i < valueSelectorWidgets.length; i++) {
                if(i >= vaParameters.size()){
                    valueSelectorWidgets[i].setVisibility(View.INVISIBLE);
                }
                else {
                    valueSelectorWidgets[i].setVisibility(View.VISIBLE);
                    valueSelectorWidgets[i].initParameterSettings(vaParameters.get(i));
                }
            }
        }
    }

    @Override
    public void setParameter(WorkoutParameter workoutParameter)
    {
        if(valueSelectorWidgets != null && isAdded()) {
            for (PillBox valueSelectorWidget : valueSelectorWidgets)
            {
                if (valueSelectorWidget.parameterSettings.identifier().equals(workoutParameter.identifier()))
                {
                    valueSelectorWidget.setValue((float)workoutParameter.valueAsDouble());
                    break;
                }
            }
        }
    }
}
