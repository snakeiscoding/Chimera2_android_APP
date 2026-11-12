package com.jht.chimera.ui.mainapp.goscreen.VA;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
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

public class VASelectionFragment extends GoScreenBaseFragment {

    RecyclerView recyclerView;
    VASelectionRecyclerAdapter vaSelectionRecyclerAdapter;
    GridLayoutManager gridLayoutManager;

    List<GoScreenWorkoutDisplayItem> vaItems;
    VASetupFragment vaSetupFragment;

    public IVASelectionInterface ivaSelectionInterface;
    public interface IVASelectionInterface{
        void getVAWorkoutParameters(String vaIdentifier);
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
        return inflater.inflate(R.layout.va_selection_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ChimeraWorkoutManager chimeraWorkoutManager = new ChimeraWorkoutManager(getContext());
        vaItems = chimeraWorkoutManager.setVaWorkouts(getContext());

        recyclerView = view.findViewById(R.id.va_recycler_view);

        recyclerView.setHasFixedSize(true);
        gridLayoutManager = new GridLayoutManager(getActivity(), getResources().getInteger(R.integer.va_workout_selection_cols));
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (vaSelectionRecyclerAdapter.getItemViewType(position)) {
                    case GoScreenWorkoutDisplayItem.CATEGORY_TITLE_TYPE:
                        return getResources().getInteger(R.integer.va_workout_selection_cols);
                    default:
                        return 1;
                }
            }
        });
        recyclerView.setLayoutManager(gridLayoutManager);

        vaSelectionRecyclerAdapter = new VASelectionRecyclerAdapter(vaItems, position -> {
            vaSetupFragment.loadVAWorkoutInfo(vaItems.get(position));
            getChildFragmentManager().beginTransaction().replace(R.id.va_setup_fragment_container, vaSetupFragment).commit();
        });

        //Need getWidth as non-zero value after drawing, so observe layout binding
        final ViewTreeObserver observer = recyclerView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                vaSelectionRecyclerAdapter.setWidth(recyclerView.getWidth());
                recyclerView.setAdapter(vaSelectionRecyclerAdapter);
            }
        });

        if (onCreateViewListener != null) onCreateViewListener.onCreateView(view);
    }

    @Override
    public void onStart() {
        super.onStart();
        recyclerView.scrollToPosition(0);
    }

    public void setIVASelectionInterface(IVASelectionInterface ivaSelectionInterface) {
        this.ivaSelectionInterface = ivaSelectionInterface;
    }

    public void setVaWorkoutItems(List<GoScreenWorkoutDisplayItem> vaItems){
        this.vaItems = vaItems;
        this.vaItems.add(0, new GoScreenWorkoutDisplayItem("A series of fully interactive video based workouts designed to entertain and inspire users of all levels.", GoScreenWorkoutDisplayItem.CATEGORY_TITLE_TYPE)); //NON-NLS
    }

    public void setVaSetupFragment(VASetupFragment vaSetupFragment){
        this.vaSetupFragment = vaSetupFragment;
    }

    public VASetupFragment getVaSetupFragment(){
        return vaSetupFragment;
    }

    void closeVaSetupFragment(){
        getChildFragmentManager().beginTransaction().remove(vaSetupFragment).commit();
    }

    public boolean hasVaItems(){
        return vaItems !=null && !vaItems.isEmpty();
    }

    public List<GoScreenWorkoutDisplayItem> getVaItems(){
        return vaItems;
    }

    @Override
    public void setParameterValues(List<WorkoutParameter> inputConfigurationList) {
        if(isAdded())
        {
            vaSetupFragment.setParameterValues(inputConfigurationList);
            getChildFragmentManager().beginTransaction().replace(R.id.va_setup_fragment_container, vaSetupFragment).commit();
        }
    }

    @Override
    public void setParameter(WorkoutParameter workoutParameter)
    {
        vaSetupFragment.setParameter(workoutParameter);
    }
}
