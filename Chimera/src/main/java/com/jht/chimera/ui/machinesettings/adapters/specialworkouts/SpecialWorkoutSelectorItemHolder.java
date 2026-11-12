package com.jht.chimera.ui.machinesettings.adapters.specialworkouts;

import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageButton;

import com.jht.androiduiwidgets.selector.IOnItemSelected;
import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;
import com.jht.datamatrixlib.DataMatrix;


/**
 * This class is used map a keyCode test row to the view.
 */
class SpecialWorkoutSelectorItemHolder extends SelectorItemHolderBase {

    private View selectorView;
    private TextView workout;
    private AppCompatImageButton delete;

    /**
     * Cache the views in the holder.
     *
     * @param v The root view.
     */
    SpecialWorkoutSelectorItemHolder(View v) {
        super(v);
        selectorView = v;
        workout = selectorView.findViewById(R.id.eng_special_workout_name);
        delete = selectorView.findViewById(R.id.eng_special_workout_remove);
    }


    /**
     * The item has changed.  We need to update the views.
     *
     * @param adapter The adapter managing the views.
     * @param item    The item with the view data.
     */
    @Override
    protected void update(final SelectorAdapterBase adapter, final ISelectorItem item, final IOnItemSelected onItemSelected) {

        if (item instanceof SpecialWorkoutSelectorItem) {
            selectorView.setBackgroundColor(selectorView.getContext().getResources().getColor(item.getIndex() % 2 == 0 ? R.color.table_even_row_background_color : R.color.table_odd_row_background_color));
            SpecialWorkoutSelectorItem workoutItem = (SpecialWorkoutSelectorItem) item;
            workout.setText(workoutItem.workout().stringId());
            delete.setOnClickListener(v -> {
                DataMatrix.putBoolean(workoutItem.workout().key(), false);
                workoutItem.delete();
            });

        }

    }

}
