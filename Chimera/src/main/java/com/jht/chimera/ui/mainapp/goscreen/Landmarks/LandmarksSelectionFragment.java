package com.jht.chimera.ui.mainapp.goscreen.Landmarks;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.chimera.R;
import com.jht.chimera.lib.workout.platform.ChimeraWorkoutManager;
import com.jht.chimera.ui.mainapp.goscreen.GoScreenBaseFragment;
import com.jht.chimera.ui.mainapp.goscreen.GoScreenWorkoutDisplayItem;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutParameter;

import java.util.List;

/**
 * Created by AlexN on 1/8/2019.
 */

public class LandmarksSelectionFragment extends GoScreenBaseFragment {
    private static final String TAG = "LandmarksSelectionFr...";
    RecyclerView recyclerView;
    LandmarksRecyclerAdapter landmarksSelectionRecyclerAdapter;

    List<GoScreenWorkoutDisplayItem> landmarksItems;
    LandmarksSetupFragment landmarksSetupFragment;

    ILandmarksSelectionInterface iLandmarksSelectionInterface;
    public interface ILandmarksSelectionInterface{
        void getLandmarksWorkoutParameters(String landmarksIdentifier);
    }

    public interface OnCreateViewListener {
        void onCreateView(View view);
    }

    OnCreateViewListener onCreateViewListener;
    public void setOnCreateViewListener(OnCreateViewListener onCreateViewListener) {
        this.onCreateViewListener = onCreateViewListener;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.landmarks_selection_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // IHRSA Temp Code Sprint8/Landmark Fragments
        // need this to be initialized with the view being created to work in compose
        ChimeraWorkoutManager chimeraWorkoutManager = new ChimeraWorkoutManager(getContext());
        landmarksItems = chimeraWorkoutManager.setLandmarksWorkouts(getContext());

        recyclerView = view.findViewById(R.id.landmarks_recycler_view);

        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);

        landmarksSelectionRecyclerAdapter = new LandmarksRecyclerAdapter(landmarksItems, new LandmarksRecyclerAdapter.ILandmarksRecyclerAdapter() {
            @Override
            public void onItemClicked(int position) {
                landmarksSetupFragment.loadLandmarksWorkoutInfo(landmarksItems.get(position));
                iLandmarksSelectionInterface.getLandmarksWorkoutParameters(landmarksItems.get(position).identifier);
            }
        });

        //Need getWidth as non-zero value after drawing, so observe layout binding
        final ViewTreeObserver observer = recyclerView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                landmarksSelectionRecyclerAdapter.setHeight((int) (recyclerView.getHeight()*1.48));
                recyclerView.setAdapter(landmarksSelectionRecyclerAdapter);
                Log.i(TAG, "layout listener: set adapter") ;
            }
        });

        if (onCreateViewListener != null) onCreateViewListener.onCreateView(view);
    }

    @Override
    public void onStart() {
        super.onStart();
        recyclerView.scrollToPosition(0);
    }

    public void setILandmarksSelectionInterface(ILandmarksSelectionInterface iLandmarksSelectionInterface) {
        this.iLandmarksSelectionInterface = iLandmarksSelectionInterface;
    }

    public void setLandmarksWorkoutItems(List<GoScreenWorkoutDisplayItem> landmarksItems){
        this.landmarksItems = landmarksItems;
    }

    public void setLandmarksSetupFragment(LandmarksSetupFragment landmarksSetupFragment){
        this.landmarksSetupFragment = landmarksSetupFragment;

    }

    void closeLandmarksSetupFragment(){
        getChildFragmentManager().beginTransaction().remove(landmarksSetupFragment).commit();
    }

    public boolean hasLandmarksItems(){
        return landmarksItems != null && !landmarksItems.isEmpty();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        recyclerView.setAdapter(null);
//        recyclerView = null;
    }

    @Override
    public void setParameterValues(List<WorkoutParameter> inputConfigurationList) {
        if(isAdded())
        {
            landmarksSetupFragment.landmarksSetupSetParameters(inputConfigurationList);
            getChildFragmentManager().beginTransaction().replace(R.id.landmarks_setup_fragment_container, landmarksSetupFragment).commit();
        }
    }

    @Override
    public void setParameter(WorkoutParameter workoutParameter)
    {
        landmarksSetupFragment.setParameter(workoutParameter);
    }
}
