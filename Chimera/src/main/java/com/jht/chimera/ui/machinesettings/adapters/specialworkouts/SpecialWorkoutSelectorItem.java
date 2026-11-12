package com.jht.chimera.ui.machinesettings.adapters.specialworkouts;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.workout.SpecialWorkouts;


/**
 * This contains the data needed for a row in the key test table.
 */
public class SpecialWorkoutSelectorItem extends SelectorItemBase {

    private SpecialWorkouts.SpecialWorkout workout;
    private IOnWorkoutDelete onDelete;

    public interface IOnWorkoutDelete {
        void onDelete(SpecialWorkoutSelectorItem item);
    }

    /**
     * Default constructor
     */
    public SpecialWorkoutSelectorItem(SpecialWorkouts.SpecialWorkout workout, IOnWorkoutDelete onDelete) {
        this.workout = workout;
        this.onDelete = onDelete;
    }

    public void delete() {
        if(onDelete != null) {
            onDelete.onDelete(this);
        }
    }

    /**
     * Used to get the item's label.
     *
     * @return Return the label
     */
    @Override
    public String toString() {
        return workout.toString();
    }

    public SpecialWorkouts.SpecialWorkout workout() {
        return workout;
    }

    @Override
    @NonNull
    public SelectorItemHolderBase createViewHolder(Context context, @NonNull ViewGroup parent) {

        View v = LayoutInflater.from(context).inflate(R.layout.special_workout_row, parent, false);
        return new SpecialWorkoutSelectorItemHolder(v);
    }
}
