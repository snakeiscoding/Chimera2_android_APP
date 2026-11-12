package com.jht.chimera.ui.mainapp.goscreen.Landmarks;

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
import androidx.fragment.app.Fragment;

import com.jht.androiduiwidgets.pillbox.PillBox;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.ui.mainapp.goscreen.BitmapLoader;
import com.jht.chimera.ui.mainapp.goscreen.GoScreenWorkoutDisplayItem;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutParameter;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutSetup.WorkoutSelectionFragment;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;

import java.util.List;

/**
 * Created by AlexN on 1/8/2019.
 */

public class LandmarksSetupFragment extends Fragment {

    private GoScreenWorkoutDisplayItem landmarksWorkoutItem;
    private List<WorkoutParameter> landmarksParameters;
    private ILandmarksSetupFragment ilandmarksSetupFragment;
    private PillBox[] landmarksSelectorWidgets;
    private DataMatrix dataMatrix = new DataMatrix();
    private long pressTime = 0;

    public interface ILandmarksSetupFragment{
        void onLandmarksWorkoutStart(String workoutIdentifier, List<WorkoutParameter> parameters);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.landmarks_setup_fragment, container, false);
        ImageView landmarksSetupBackground = view.findViewById(R.id.landmarks_setup_background);
        ImageView landmarksSetupPlaceBackground = view.findViewById(R.id.landmarks_setup_place_background);
        TextView landmarksSetupTitle = view.findViewById(R.id.landmarks_setup_title);
        TextView landmarksSetupSubTitle = view.findViewById(R.id.landmarks_setup_sub_title);
        TextView landmarksSetupDescription = view.findViewById(R.id.landmarks_setup_description);
        TextView landmarksSetupSubDescription = view.findViewById(R.id.landmarks_setup_sub_description);
        Button landmarksSetupBackButton = view.findViewById(R.id.landmarks_setup_back_button);
        landmarksSetupBackButton.setOnClickListener(v -> {
            if (getParentFragment() != null) {
                ((LandmarksSelectionFragment)getParentFragment()).closeLandmarksSetupFragment();
            }
        });

        landmarksSelectorWidgets = new PillBox[2];
        landmarksSelectorWidgets[0] = view.findViewById(R.id.landmarks_setup_value_selector_1);
        landmarksSelectorWidgets[1] = view.findViewById(R.id.landmarks_setup_value_selector_2);

        setParameters();

        Button landmarksStartButton = view.findViewById(R.id.start_button);
        landmarksStartButton.setOnClickListener(v -> {
            long uptime = SystemClock.uptimeMillis();
            if (pressTime < uptime - 1500) {
                pressTime = uptime;
                ilandmarksSetupFragment.onLandmarksWorkoutStart(landmarksWorkoutItem.identifier, landmarksParameters);
            }
        });

        BitmapLoader.loadSampledBitmap(getActivity(), landmarksSetupBackground, landmarksWorkoutItem.backgroundPath);
        BitmapLoader.loadSampledBitmap(getActivity(), landmarksSetupPlaceBackground, landmarksWorkoutItem.thumbnailPath);
        landmarksSetupTitle.setText(landmarksWorkoutItem.title);
        landmarksSetupSubTitle.setText(landmarksWorkoutItem.subTitle);
        landmarksSetupDescription.setText(landmarksWorkoutItem.description);
        landmarksSetupSubDescription.setText(landmarksWorkoutItem.subDescription);

        return view;
    }
    @NonNls private static String WEIGHT_VARIABLE = "WEIGHT";
    @Override
    public void onResume() {
        super.onResume();
        dataMatrix.createFloatAdapter(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT, weight -> {
            for(int i = 0; i < landmarksSelectorWidgets.length; i++){
                if(landmarksSelectorWidgets[i].parameterSettings != null && landmarksSelectorWidgets[i].parameterSettings.label().equalsIgnoreCase(WEIGHT_VARIABLE)) {
                    boolean isMetric = DataMatrix.getBoolean(WorkoutSelectionFragment.GoScreenKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, true);
                    landmarksSelectorWidgets[i].setValue(Math.round(isMetric ? (weight) : (weight / 0.453592f)));
                }
            }
        }, true);
    }

    void loadLandmarksWorkoutInfo(GoScreenWorkoutDisplayItem workoutItem) {
        landmarksWorkoutItem = workoutItem;
    }

    void landmarksSetupSetParameters(List<WorkoutParameter> parameters) {
        landmarksParameters = parameters;
        setParameters();
    }

    public void setILandmarksSetupFragment(ILandmarksSetupFragment ilandmarksSetupFragment){
        this.ilandmarksSetupFragment = ilandmarksSetupFragment;
    }

    private void setParameters(){
        if(landmarksParameters != null && landmarksSelectorWidgets!= null && isAdded()) {
            for (int i = 0; i < landmarksSelectorWidgets.length; i++) {
                if(i >= landmarksParameters.size()){
                    landmarksSelectorWidgets[i].setVisibility(View.INVISIBLE);
                }
                else {
                    landmarksSelectorWidgets[i].setVisibility(View.VISIBLE);
                    landmarksSelectorWidgets[i].initParameterSettings(landmarksParameters.get(i));
                }
            }
        }
    }

    public void setParameter(WorkoutParameter workoutParameter){
        if(landmarksSelectorWidgets != null && isAdded()) {
            for (PillBox valueSelectorWidget : landmarksSelectorWidgets)
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
