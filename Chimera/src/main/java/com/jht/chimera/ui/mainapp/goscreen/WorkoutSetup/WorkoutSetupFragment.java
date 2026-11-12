package com.jht.chimera.ui.mainapp.goscreen.WorkoutSetup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.jht.chimera.R;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.datamatrixlib.DataMatrix;

/**
 * Created by AlexN on 1/8/2019.
 */
public class WorkoutSetupFragment extends Fragment {
    protected static final String TAG = "WorkoutSetupFragment";

    private Fragment workoutFragment;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.workout_setup_fragment, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ConstraintLayout layoutBackground = view.findViewById(R.id.setupFragment_background);
        layoutBackground.setOnClickListener(v -> closeFragment(true));
    }

    public void loadNewSetupFragment(Fragment fragment){
        workoutFragment = fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.OPEN_FEATURE_WORKOUT_SETUP.name());
        if (workoutFragment != null) {
            getChildFragmentManager().beginTransaction().replace(R.id.setup_fragment_container, workoutFragment).commit();
        }
    }

    private void closeFragment(boolean dismissedByUser) {
        if(workoutFragment != null) {
            getChildFragmentManager().beginTransaction()
                     .setCustomAnimations(R.animator.fade_in, R.animator.fade_same)
                    .remove(workoutFragment).commit();
            workoutFragment = null;
        }
        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.fade_in, R.animator.fade_out)
                .remove(WorkoutSetupFragment.this).commit();
    }

    @Override
    public void onPause() {
        super.onPause();
        closeFragment(false);
    }

    @Override
    public void onStop() {
        super.onStop();
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.CLOSE_FEATURE_WORKOUT_SETUP.name());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

}
